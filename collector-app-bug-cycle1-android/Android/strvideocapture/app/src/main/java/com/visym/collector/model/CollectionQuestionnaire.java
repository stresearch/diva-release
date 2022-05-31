package com.visym.collector.model;

public class CollectionQuestionnaire {

    private String id;
    private String collectionId;
    private String startFrame;
    private String endFrame;
    private String question;
    private String createdDate;
    private String updatedDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public String getStartFrame() {
        return startFrame;
    }

    public void setStartFrame(String startFrame) {
        this.startFrame = startFrame;
    }

    public String getEndFrame() {
        return endFrame;
    }

    public void setEndFrame(String endFrame) {
        this.endFrame = endFrame;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }
}
