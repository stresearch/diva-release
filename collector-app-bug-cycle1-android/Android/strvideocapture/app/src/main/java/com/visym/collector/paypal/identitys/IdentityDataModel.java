package com.visym.collector.paypal.identitys;

import java.util.ArrayList;

public class IdentityDataModel {
    private String user_id;
    private ArrayList<SubIdentityDataModel>emails;
    private String message;
    private String apiStatus;
    private int code;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getApiStatus() {
        return apiStatus;
    }

    public void setApiStatus(String apiStatus) {
        this.apiStatus = apiStatus;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public ArrayList<SubIdentityDataModel> getEmails() {
        return emails;
    }

    public void setEmails(ArrayList<SubIdentityDataModel> emails) {
        this.emails = emails;
    }
}
