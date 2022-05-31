package com.visym.collector.model;

import org.json.JSONException;
import org.json.JSONObject;

public class ConsentDataQuestionnaire {

  String questionId;
  String responseType;
  String shortDescription;
  String longDescription;
  String agreeDestinationId;
  String disagreeDestinationId;
  Boolean isAnswered;
  String q_category;
  String q_category_response;
  String moreInfo;

  public Boolean getAnswered() {
    return isAnswered;
  }

  public void setAnswered(Boolean answered) {
    isAnswered = answered;
  }

  public String getQ_category() {
    return q_category;
  }

  public void setQ_category(String q_category) {
    this.q_category = q_category;
  }

  public String getQ_category_response() {
    return q_category_response;
  }

  public void setQ_category_response(String q_category_response) {
    this.q_category_response = q_category_response;
  }

  public ConsentDataQuestionnaire(JSONObject qObject){
    try {
      this.questionId = qObject.getString("id");
      //this.responseType = qObject.getString("responseType");
      this.shortDescription = qObject.getString("short_description");
      this.isAnswered = false;
      // this.longDescription = qObject.getString("longDescription");
      if (qObject.has("agree_question_id") && qObject.get("agree_question_id") != null) {


      this.agreeDestinationId = qObject.getString("agree_question_id");
    }
      if(qObject.has("disagree_question_id") && qObject.get("disagree_question_id") != null) {
        this.disagreeDestinationId = qObject.getString("disagree_question_id");
      }
      if(qObject.has("q_category") && qObject.get("q_category") != null){
        this.q_category = qObject.getString("q_category");
      }
      if(qObject.has("q_category_response") && qObject.get("q_category_response") != null){
        this.q_category_response = qObject.getString("q_category_response");
      }
      if (qObject.has("more_info") && qObject.get("more_info") != null){
        moreInfo = qObject.getString("more_info");
      }
    }catch (JSONException e){
      e.printStackTrace();
    }
  }
  public String getQuestionId() {
    return questionId;
  }
  public Boolean getIsAnswered(){
    return isAnswered;
  }
  public void setIsAnswered(boolean value){
    this.isAnswered = value;
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

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  public String getLongDescription() {
    return longDescription;
  }

  public void setLongDescription(String longDescription) {
    this.longDescription = longDescription;
  }

  public String getAgreeDestinationId() {
    return agreeDestinationId;
  }

  public void setAgreeDestinationId(String agreeDestinationId) {
    this.agreeDestinationId = agreeDestinationId;
  }

  public String getDisagreeDestinationId() {
    return disagreeDestinationId;
  }

  public void setDisagreeDestinationId(String disagreeDestinationId) {
    this.disagreeDestinationId = disagreeDestinationId;
  }

  public String getMoreInfo() {
    return moreInfo;
  }
}
