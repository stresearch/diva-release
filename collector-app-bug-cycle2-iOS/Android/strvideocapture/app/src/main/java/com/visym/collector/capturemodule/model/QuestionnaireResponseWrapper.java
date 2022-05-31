package com.visym.collector.capturemodule.model;

import com.google.gson.annotations.SerializedName;
import com.visym.collector.model.ResponseWrapper;

import java.util.List;

public class QuestionnaireResponseWrapper implements ResponseWrapper<List<Questionnaire>> {

    @SerializedName("questionnaire")
    private List<Questionnaire> questionnaires;

    @Override
    public List<Questionnaire> getContent() {
        return questionnaires;
    }
}
