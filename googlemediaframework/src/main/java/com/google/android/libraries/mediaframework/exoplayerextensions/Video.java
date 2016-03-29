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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a video that can be played by Exoplayer.
 */
public class Video {

    /**
     * A list of available video formats which Exoplayer can play.
     */
    public static enum VideoType {
        DASH,
        MP4,
        HLS,
        OTHER
    }

    /**
     * The URL pointing to the video.
     */
    private final String url;

    /**
     * The video format of the video.
     */
    private final VideoType videoType;

    /**
     * ID of content (for DASH).
     */
    private final String contentId;

    private Map<String, String> subtitlesMap;

    private String userAgent = "Coichua";

    /**
     * @param url The URL pointing to the video.
     * @param videoType The video format of the video.
     */
    public Video(String url, VideoType videoType) {
        this(url, videoType, null);
    }

    /**
     * @param url The URL pointing to the video.
     * @param videoType The video format of the video.
     * @param contentId ID of content (for DASH).
     */
    public Video(String url, VideoType videoType, String contentId) {
        this.url = url;
        this.videoType = videoType;
        this.contentId = contentId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Returns ID of content (for DASH).
     */
    public String getContentId() {
        return contentId;
    }

    /**
     * Returns the URL pointing to the video.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the video format of the video.
     */
    public VideoType getVideoType() {
        return videoType;
    }

    public void addSubtitle(String language, String url) {
        if (subtitlesMap == null) {
            subtitlesMap = new HashMap<String, String>();
        }

        subtitlesMap.put(language, url);
    }

    public int getNumberOfSubtitles() {
        if (subtitlesMap == null) {
            return 0;
        }

        return subtitlesMap.size();
    }

    public String getSubtilteUrl(String language) {
        return subtitlesMap.get(language);
    }

    public Map<String, String> getSubtitlesMap() {
        return subtitlesMap;
    }
}
