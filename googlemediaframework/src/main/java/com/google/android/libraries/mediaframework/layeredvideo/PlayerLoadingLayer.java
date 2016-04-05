package com.google.android.libraries.mediaframework.layeredvideo;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.libraries.mediaframework.R;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper;

/**
 * Created by nlaphu on 4/5/2016.
 */
public class PlayerLoadingLayer implements Layer, ExoplayerWrapper.PlaybackListener {

    private ProgressBar progressBar;
    private FrameLayout view;

    public PlayerLoadingLayer() {

    }

    public void hideLoading() {
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public FrameLayout createView(LayerManager layerManager) {
        LayoutInflater inflater = layerManager.getActivity().getLayoutInflater();

        view = (FrameLayout) inflater.inflate(R.layout.player_loading_layer, null);
        progressBar = (ProgressBar) view.findViewById(R.id.controls_video_loading);

        layerManager.getExoplayerWrapper().addListener(this);

        return view;
    }

    @Override
    public void onLayerDisplayed(LayerManager layerManager) {

    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_READY) {
            hideLoading();
        }
    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

    }
}
