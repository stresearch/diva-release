package com.visym.collector.capturemodule.model;

public class ConsentDetail {

    private String email;
    private String questions;
    private String consentVideoURL;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getQuestions() {
        return questions;
    }

    public void setQuestions(String questions) {
        this.questions = questions;
    }

    public String getConsentVideoURL() {
        return consentVideoURL;
    }

    public void setConsentVideoURL(String consentVideoURL) {
        this.consentVideoURL = consentVideoURL;
    }
}
