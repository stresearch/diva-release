package com.visym.collector.model;

public class RatingQuestionResponse {
    private String ID;
    private String Reviewer_ID;
    private int BAD_Alignment;
    private int Bad_Box_Small;
    private int Bad_Box_big;
    private int Bad_Label;
    private int Bad_Timing;
    private int Bad_Video;
    private int Bad_Viewpoint;

    public void setID(String ID) {
        this.ID = ID;
    }

    public void setReviewer_ID(String reviewer_ID) {
        Reviewer_ID = reviewer_ID;
    }

    public void setBAD_Alignment(int BAD_Alignment) {
        this.BAD_Alignment = BAD_Alignment;
    }

    public void setBad_Box_Small(int bad_Box_Small) {
        Bad_Box_Small = bad_Box_Small;
    }

    public void setBad_Box_big(int bad_Box_big) {
        Bad_Box_big = bad_Box_big;
    }

    public void setBad_Label(int bad_Label) {
        Bad_Label = bad_Label;
    }

    public void setBad_Timing(int bad_Timing) {
        Bad_Timing = bad_Timing;
    }

    public void setBad_Video(int bad_Video) {
        Bad_Video = bad_Video;
    }

    public void setBad_Viewpoint(int bad_Viewpoint) {
        Bad_Viewpoint = bad_Viewpoint;
    }
}
