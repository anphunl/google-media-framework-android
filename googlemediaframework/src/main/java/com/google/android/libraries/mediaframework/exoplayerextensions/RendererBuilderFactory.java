/**
 Copyright 2014 Google Inc. All rights reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
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
//      case DASH:
//        return new DashRendererBuilder(ctx, ExoplayerUtil.getUserAgent(ctx),
//                                       video.getUrl(),
//                                       new WidevineTestMediaDrmCallback(video.getContentId()));
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
        if (video.getNumberOfSubtitles() > 0) {
            DataSource textDataSource = new DefaultUriDataSource(context, bandwidthMeter, video.getUserAgent());
            MediaFormat mediaFormat = MediaFormat.createTextFormat("0", MimeTypes.APPLICATION_SUBRIP, MediaFormat.NO_VALUE, C.MATCH_LONGEST_US, null);

            Uri uri = Uri.parse("http://s.vn-hd.com:8080/store_06_2013/15062013/Rio_2011_1080p_2D_Bluray_DTS_x264_DON/Rio_2011_1080p_2D_Bluray_DTS_x264_DON_VIE.srt");

            SingleSampleSource textSampleSource = new SingleSampleSource(uri, textDataSource, mediaFormat);
            textRenderer = new TextTrackRenderer(textSampleSource, player, player.getMainHandler().getLooper());
        }

        return textRenderer;
    }
}
