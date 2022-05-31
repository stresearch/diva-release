package com.visym.collector.model;

import org.json.JSONException;
import org.json.JSONObject;

public class ConsentDataList {
  String version;
  String questionnaire;

  public ConsentDataList(JSONObject consentData){
    try {
      this.version = consentData.getString("version");
      this.questionnaire = consentData.getString("questionnaire");
    }catch (JSONException e){
      e.printStackTrace();
    }
  }
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getQuestionnaire() {
    return questionnaire;
  }

  public void setQuestionnaire(String questionnaire) {
    this.questionnaire = questionnaire;
  }
}
