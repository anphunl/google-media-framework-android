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

package com.google.android.libraries.mediaframework.layeredvideo;

import android.annotation.TargetApi;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.accessibility.CaptioningManager;
import android.widget.FrameLayout;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.google.android.libraries.mediaframework.R;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper;

import java.util.List;

/**
 * Creates a view which displays subtitleLayout.
 */
public class SubtitleLayer implements Layer, ExoplayerWrapper.CaptionListener {

    /**
     * The text view that displays the subtitleLayout.
     */
    private SubtitleLayout subtitleLayout;

    private AspectRatioFrameLayout aspectRatioFrameLayout;

    /**
     * The view that is created by this layer (it contains SubtitleLayer#subtitleLayout).
     */
    private FrameLayout view;

    private ExoplayerWrapper.PlaybackListener playbackListener
            = new ExoplayerWrapper.PlaybackListener() {
        @Override
        public void onStateChanged(boolean playWhenReady, int playbackState) {
            // Do nothing. VideoSurfaceLayer doesn't care about state changes.
        }

        @Override
        public void onError(Exception e) {
            // Do nothing. VideoSurfaceLayer doesn't care about errors here.
        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthAspectRatio) {
            aspectRatioFrameLayout.setAspectRatio(height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);
        }
    };

    @Override
    public FrameLayout createView(LayerManager layerManager) {
        LayoutInflater inflater = layerManager.getActivity().getLayoutInflater();

        view = (FrameLayout) inflater.inflate(R.layout.subtitle_layer, null);
        subtitleLayout = (SubtitleLayout) view.findViewById(R.id.subtitles);
        aspectRatioFrameLayout = (AspectRatioFrameLayout) view.findViewById(R.id.video_subtitles_frame);

        layerManager.getExoplayerWrapper().addListener(playbackListener);

        return view;
    }

    public void configureSubtitleView(CaptionStyleCompat style, float fontScale) {
        subtitleLayout.setStyle(style);
        subtitleLayout.setFractionalTextSize(SubtitleLayout.DEFAULT_TEXT_SIZE_FRACTION * fontScale);
    }

    @Override
    public void onLayerDisplayed(LayerManager layerManager) {

    }


    /**
     * Show or hide the subtitleLayout.
     * @param visibility One of {@link android.view.View#INVISIBLE},
     *                   {@link android.view.View#VISIBLE}, {@link android.view.View#GONE}.
     */
    public void setVisibility(int visibility) {
        view.setVisibility(visibility);
    }

    @Override
    public void onCues(List<Cue> cues) {
        subtitleLayout.setCues(cues);
    }
}
