package com.visym.collector.capturemodule.model;

import com.google.gson.annotations.SerializedName;
import com.visym.collector.model.BoundingBox;

import java.util.List;

public class FrameObject {

    private String label;

    @SerializedName("bounding_box")
    private List<BoundingBox> boundingBox;

    public FrameObject(String label, List<BoundingBox> boundingBox) {
        this.label = label;
        this.boundingBox = boundingBox;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public List<BoundingBox> getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(List<BoundingBox> boundingBox) {
        this.boundingBox = boundingBox;
    }
}
