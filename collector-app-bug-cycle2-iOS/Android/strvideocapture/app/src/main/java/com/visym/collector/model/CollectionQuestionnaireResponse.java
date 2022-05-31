package com.visym.collector.model;

public class CollectionQuestionnaireResponse {

    private String instanceId;
    private String questionId;
    private int questionResponse;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public int getQuestionResponse() {
        return questionResponse;
    }

    public void setQuestionResponse(int questionResponse) {
        this.questionResponse = questionResponse;
    }
}
