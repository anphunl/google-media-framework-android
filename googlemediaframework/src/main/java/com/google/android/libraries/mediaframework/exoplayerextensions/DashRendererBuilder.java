/**
 * Copyright 2014 Google Inc. All rights reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * This file has been taken from the ExoPlayer demo project with minor modifications.
 * https://github.com/google/ExoPlayer/
 */

/**
 * This file has been taken from the ExoPlayer demo project with minor modifications.
 * https://github.com/google/ExoPlayer/
 */
package com.google.android.libraries.mediaframework.exoplayerextensions;

import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.ChunkSource;
import com.google.android.exoplayer.chunk.FormatEvaluator.AdaptiveEvaluator;
import com.google.android.exoplayer.dash.DashChunkSource;
import com.google.android.exoplayer.dash.DefaultDashTrackSelector;
import com.google.android.exoplayer.dash.mpd.AdaptationSet;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescriptionParser;
import com.google.android.exoplayer.dash.mpd.Period;
import com.google.android.exoplayer.dash.mpd.UtcTimingElement;
import com.google.android.exoplayer.dash.mpd.UtcTimingElementResolver;
import com.google.android.exoplayer.dash.mpd.UtcTimingElementResolver.UtcTimingCallback;
import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.Util;

import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper.RendererBuilder;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;

/**
 * A {@link RendererBuilder} for DASH.
 */
public class DashRendererBuilder implements RendererBuilder {

    private static final String TAG = "DashRendererBuilder";

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int VIDEO_BUFFER_SEGMENTS = 200;
    private static final int AUDIO_BUFFER_SEGMENTS = 54;
    private static final int TEXT_BUFFER_SEGMENTS = 2;
    private static final int LIVE_EDGE_LATENCY_MS = 30000;

    private static final int SECURITY_LEVEL_UNKNOWN = -1;
    private static final int SECURITY_LEVEL_1 = 1;
    private static final int SECURITY_LEVEL_3 = 3;

    private final Context context;
    private final Video video;
    private final MediaDrmCallback drmCallback;

    private AsyncRendererBuilder currentAsyncBuilder;

    public DashRendererBuilder(Context context, Video video, MediaDrmCallback drmCallback) {
        this.context = context;
        this.video = video;
        this.drmCallback = drmCallback;
    }

    @Override
    public void buildRenderers(ExoplayerWrapper player) {
        currentAsyncBuilder = new AsyncRendererBuilder(context, video, drmCallback, player);
        currentAsyncBuilder.init();
    }

    @Override
    public void cancel() {
        if (currentAsyncBuilder != null) {
            currentAsyncBuilder.cancel();
            currentAsyncBuilder = null;
        }
    }

    private static final class AsyncRendererBuilder
            implements ManifestFetcher.ManifestCallback<MediaPresentationDescription>,
            UtcTimingCallback {

        private final Video video;
        private final Context context;
        private final MediaDrmCallback drmCallback;
        private final ExoplayerWrapper player;
        private final ManifestFetcher<MediaPresentationDescription> manifestFetcher;
        private final UriDataSource manifestDataSource;

        private boolean canceled;
        private MediaPresentationDescription manifest;
        private long elapsedRealtimeOffset;

        public AsyncRendererBuilder(Context context, Video video,
                                    MediaDrmCallback drmCallback, ExoplayerWrapper player) {
            this.context = context;
            this.video = video;
            this.drmCallback = drmCallback;
            this.player = player;
            MediaPresentationDescriptionParser parser = new MediaPresentationDescriptionParser();
            manifestDataSource = new DefaultUriDataSource(context, video.getUserAgent());
            manifestFetcher = new ManifestFetcher<>(video.getUrl(), manifestDataSource, parser);
        }

        public void init() {
            manifestFetcher.singleLoad(player.getMainHandler().getLooper(), this);
        }

        public void cancel() {
            canceled = true;
        }

        @Override
        public void onSingleManifest(MediaPresentationDescription manifest) {
            if (canceled) {
                return;
            }

            this.manifest = manifest;
            if (manifest.dynamic && manifest.utcTiming != null) {
                UtcTimingElementResolver.resolveTimingElement(manifestDataSource, manifest.utcTiming,
                        manifestFetcher.getManifestLoadCompleteTimestamp(), this);
            } else {
                buildRenderers();
            }
        }

        @Override
        public void onSingleManifestError(IOException e) {
            if (canceled) {
                return;
            }

            player.onRenderersError(e);
        }

        @Override
        public void onTimestampResolved(UtcTimingElement utcTiming, long elapsedRealtimeOffset) {
            if (canceled) {
                return;
            }

            this.elapsedRealtimeOffset = elapsedRealtimeOffset;
            buildRenderers();
        }

        @Override
        public void onTimestampError(UtcTimingElement utcTiming, IOException e) {
            if (canceled) {
                return;
            }

            Log.e(TAG, "Failed to resolve UtcTiming element [" + utcTiming + "]", e);
            // Be optimistic and continue in the hope that the device clock is correct.
            buildRenderers();
        }

        private void buildRenderers() {
            Period period = manifest.getPeriod(0);
            Handler mainHandler = player.getMainHandler();
            LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(mainHandler, player);

            boolean hasContentProtection = false;
            for (int i = 0; i < period.adaptationSets.size(); i++) {
                AdaptationSet adaptationSet = period.adaptationSets.get(i);
                if (adaptationSet.type != AdaptationSet.TYPE_UNKNOWN) {
                    hasContentProtection |= adaptationSet.hasContentProtection();
                }
            }

            // Check drm support if necessary.
            boolean filterHdContent = false;
            StreamingDrmSessionManager drmSessionManager = null;
            if (hasContentProtection) {
                if (Util.SDK_INT < 18) {
                    player.onRenderersError(
                            new UnsupportedDrmException(UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME));
                    return;
                }
                try {
                    drmSessionManager = StreamingDrmSessionManager.newWidevineInstance(
                            player.getPlaybackLooper(), drmCallback, null, player.getMainHandler(), player);
                    filterHdContent = getWidevineSecurityLevel(drmSessionManager) != SECURITY_LEVEL_1;
                } catch (UnsupportedDrmException e) {
                    player.onRenderersError(e);
                    return;
                }
            }

            // Build the video renderer.
            DataSource videoDataSource = new DefaultUriDataSource(context, bandwidthMeter, video.getUserAgent());
            ChunkSource videoChunkSource = new DashChunkSource(manifestFetcher,
                    DefaultDashTrackSelector.newVideoInstance(context, true, filterHdContent),
                    videoDataSource, new AdaptiveEvaluator(bandwidthMeter), LIVE_EDGE_LATENCY_MS,
                    elapsedRealtimeOffset, mainHandler, player, ExoplayerWrapper.TYPE_VIDEO);
            ChunkSampleSource videoSampleSource = new ChunkSampleSource(videoChunkSource, loadControl,
                    VIDEO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mainHandler, player,
                    ExoplayerWrapper.TYPE_VIDEO);
            TrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context, videoSampleSource,
                    MediaCodecSelector.DEFAULT, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000,
                    drmSessionManager, true, mainHandler, player, 50);

            // Build the audio renderer.
            DataSource audioDataSource = new DefaultUriDataSource(context, bandwidthMeter, video.getUserAgent());
            ChunkSource audioChunkSource = new DashChunkSource(manifestFetcher,
                    DefaultDashTrackSelector.newAudioInstance(), audioDataSource, null, LIVE_EDGE_LATENCY_MS,
                    elapsedRealtimeOffset, mainHandler, player, ExoplayerWrapper.TYPE_AUDIO);
            ChunkSampleSource audioSampleSource = new ChunkSampleSource(audioChunkSource, loadControl,
                    AUDIO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mainHandler, player,
                    ExoplayerWrapper.TYPE_AUDIO);
            TrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(audioSampleSource,
                    MediaCodecSelector.DEFAULT, drmSessionManager, true, mainHandler, player,
                    AudioCapabilities.getCapabilities(context), AudioManager.STREAM_MUSIC);

            // Build the text renderer.
            TrackRenderer textRenderer = RendererBuilderFactory.getSubtitleTrack(video, context, bandwidthMeter, player);

            // Invoke the callback.
            TrackRenderer[] renderers = new TrackRenderer[ExoplayerWrapper.RENDERER_COUNT];
            renderers[ExoplayerWrapper.TYPE_VIDEO] = videoRenderer;
            renderers[ExoplayerWrapper.TYPE_AUDIO] = audioRenderer;
            renderers[ExoplayerWrapper.TYPE_TEXT] = textRenderer;
            player.onRenderers(renderers, bandwidthMeter);
        }

        private static int getWidevineSecurityLevel(StreamingDrmSessionManager sessionManager) {
            String securityLevelProperty = sessionManager.getPropertyString("securityLevel");
            return securityLevelProperty.equals("L1") ? SECURITY_LEVEL_1 : securityLevelProperty
                    .equals("L3") ? SECURITY_LEVEL_3 : SECURITY_LEVEL_UNKNOWN;
        }

    }

}