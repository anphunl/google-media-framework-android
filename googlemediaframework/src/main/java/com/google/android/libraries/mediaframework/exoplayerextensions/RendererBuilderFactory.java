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
 */

package com.google.android.libraries.mediaframework.exoplayerextensions;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.SingleSampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.MimeTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Generate a renderer builder appropriate for rendering a video.
 */
public class RendererBuilderFactory {

    /**
     * Create a renderer builder which can build the given video.
     * @param ctx The context (ex {@link android.app.Activity} in whicb the video has been created.
     * @param video The video which will be played.
     */
    public static ExoplayerWrapper.RendererBuilder createRendererBuilder(Context ctx,
                                                                         Video video) {
        switch (video.getVideoType()) {
            case HLS:
                return new HlsRendererBuilder(ctx, video);
            case DASH:
                return new DashRendererBuilder(ctx, video, new WidevineTestMediaDrmCallback(video.getContentId()));
            case MP4:
                return new ExtractorRendererBuilder(ctx, video);
            case OTHER:
                return new ExtractorRendererBuilder(ctx, video);
            default:
                return null;
        }
    }

    public static TrackRenderer getSubtitleTrack(Video video, Context context, DefaultBandwidthMeter bandwidthMeter, ExoplayerWrapper player) {
        TrackRenderer textRenderer = null;
        int i = 0;
        if (video.getNumberOfSubtitles() > 0) {
            DataSource textDataSource = new DefaultUriDataSource(context, bandwidthMeter, video.getUserAgent());

            SingleSampleSource[] sources = new SingleSampleSource[video.getNumberOfSubtitles()];
            for (Video.Subtitle subtitle : video.getSubtitlesList()) {
                MediaFormat mediaFormat = MediaFormat.createTextFormat(subtitle.getLanguage(), MimeTypes.APPLICATION_SUBRIP, MediaFormat.NO_VALUE, C.MATCH_LONGEST_US, null);
                Uri uri = Uri.parse(subtitle.getUrl());
                sources[i++] = new SingleSampleSource(uri, textDataSource, mediaFormat);
            }

            textRenderer = new TextTrackRenderer(sources, player, player.getMainHandler().getLooper());
        }

        return textRenderer;
    }
}
