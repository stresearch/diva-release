package com.visym.collector.capturemodule.model;

import android.os.Build;

import com.google.gson.annotations.SerializedName;
import com.visym.collector.BuildConfig;

public class FrameMetaData {

    @SerializedName("project_id")
    private String projectId;

    @SerializedName("collection_id")
    private String collectionId;

    @SerializedName("collector_id")
    private String collectorId;

    @SerializedName("subject_ids")
    private String[] subjectIds;

    @SerializedName("video_id")
    private String videoId;

    @SerializedName("collection_name")
    private String collectionName;

    @SerializedName("project_name")
    private String projectName;

    @SerializedName("program_name")
    private String programName;

    @SerializedName("collected_date")
    private String collectedDate;

    @SerializedName("blurred_faces")
    private int blurredFaces;

    @SerializedName("frame_rate")
    private double frameRate;

    @SerializedName("frame_width")
    private int frameWidth;

    @SerializedName("frame_height")
    private int frameHeight;

    private float duration;

    private String ipAddress = "";

    private String orientation;

    private String category;

    @SerializedName("shortname")
    private String shortName;

    @SerializedName("device_type")
    private String deviceType = Build.MODEL;

    @SerializedName("app_version")
    private String appVersion = BuildConfig.VERSION_NAME;

    @SerializedName("device_identifier")
    private String deviceIdentifier = "android";

    @SerializedName("os_version")
    private String osVersion = String.valueOf(Build.VERSION.SDK_INT);

    private int deviceOrientation;
    private int sensorOrientation;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public String getCollectorId() {
        return collectorId;
    }

    public void setCollectorId(String collectorId) {
        this.collectorId = collectorId;
    }

    public String[] getSubjectIds() {
        return subjectIds;
    }

    public void setSubjectIds(String[] subjectIds) {
        this.subjectIds = subjectIds;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getCollectedDate() {
        return collectedDate;
    }

    public void setCollectedDate(String collectedDate) {
        this.collectedDate = collectedDate;
    }

    public int getBlurredFaces() {
        return blurredFaces;
    }

    public void setBlurredFaces(int blurredFaces) {
        this.blurredFaces = blurredFaces;
    }

    public double getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(double frameRate) {
        this.frameRate = frameRate;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public void setFrameHeight(int frameHeight) {
        this.frameHeight = frameHeight;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public int getDeviceOrientation() {
        return deviceOrientation;
    }

    public void setDeviceOrientation(int deviceOrientation) {
        this.deviceOrientation = deviceOrientation;
    }

    public int getSensorOrientation() {
        return sensorOrientation;
    }

    public void setSensorOrientation(int sensorOrientation) {
        this.sensorOrientation = sensorOrientation;
    }
}
