package com.visym.collector.utils;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

public class AwsResponse {

    public static final int FILE_TYPE_VIDEO = 1;
    public static final int FILE_TYPE_JSON = 2;
    public static final int FILE_TYPE_CONSENT = 3;
    public static final int FILE_TYPE_UPDATED_JSON = 4;

    private TransferState state;
    private int progress;
    private String videoUrl;
    private int fileType;
    private String errorMessage;

    public TransferState getState() {
        return state;
    }

    public void setState(TransferState state) {
        this.state = state;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
