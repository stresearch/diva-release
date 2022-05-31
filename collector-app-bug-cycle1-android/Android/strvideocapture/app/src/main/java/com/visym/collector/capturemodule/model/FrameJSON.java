package com.visym.collector.capturemodule.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FrameJSON {

    @SerializedName("metadata")
    private FrameMetaData metaData;

    private List<ActivityLabel> activity;

    private List<FrameObject> object;

    public FrameJSON() {
    }

    public FrameJSON(FrameMetaData metaData, List<ActivityLabel> activity, List<FrameObject> object) {
        this.metaData = metaData;
        this.activity = activity;
        this.object = object;
    }

    public FrameMetaData getMetaData() {
        return metaData;
    }

    public List<ActivityLabel> getActivity() {
        return activity;
    }

    public void setActivity(List<ActivityLabel> activities) {
        this.activity = activities;
    }

    public List<FrameObject> getObject() {
        return object;
    }

    public void setObject(List<FrameObject> object) {
        this.object = object;
    }
}
