package com.visym.collector.model;

import com.google.gson.annotations.SerializedName;

public class BoundingBox {

    private Frame frame;

    @SerializedName("frame_index")
    private int frameIndex;

    public BoundingBox(Frame frame, int frameIndex) {
        this.frame = frame;
        this.frameIndex = frameIndex;
    }

    public Frame getFrame() {
        return frame;
    }

    public int getFrameIndex() {
        return frameIndex;
    }
}
