package com.visym.collector.capturemodule.model;

public class Questionnaire {

    private String questionId;

    private String responseType;

    private String question;

    private String timeOfappearence;

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getTimeOfappearence() {
        return timeOfappearence;
    }

    public void setTimeOfappearence(String timeOfappearence) {
        this.timeOfappearence = timeOfappearence;
    }
}
