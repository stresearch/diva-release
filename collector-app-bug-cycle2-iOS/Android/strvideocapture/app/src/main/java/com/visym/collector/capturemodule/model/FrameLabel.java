package com.visym.collector.capturemodule.model;

public class FrameLabel {

    private String label;
    private Long startTime;
    private Long endTime;

    public FrameLabel(String label, Long startTime) {
        this.label = label;
        this.startTime = startTime;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
}
