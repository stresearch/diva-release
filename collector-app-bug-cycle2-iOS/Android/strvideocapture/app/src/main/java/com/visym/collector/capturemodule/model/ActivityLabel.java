package com.visym.collector.capturemodule.model;

import com.google.gson.annotations.SerializedName;

public class ActivityLabel implements Comparable<ActivityLabel>{

    private String label;

    @SerializedName("start_frame")
    private int startFrame;

    @SerializedName("end_frame")
    private int endFrame;

    @SerializedName("object_index")
    private int[] objectIndex = {0};

    public ActivityLabel() {
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getStartFrame() {
        return startFrame;
    }

    public void setStartFrame(int startFrame) {
        this.startFrame = startFrame;
    }

    public int getEndFrame() {
        return endFrame;
    }

    public void setEndFrame(int endFrame) {
        this.endFrame = endFrame;
    }

    @Override
    public int compareTo(ActivityLabel o) {
        return getStartFrame() - o.getStartFrame();
    }

    public ActivityLabel(String label, int startFrame, int endFrame) {
        this.label = label;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
    }
}
