package com.visym.collector.model;

import android.util.Log;

import com.visym.collector.utils.Globals;

import org.json.JSONException;
import org.json.JSONObject;

public class CollectionActivitiesModel {

  String activitiesName;
  String activitiesId;
  private String activitiesShortName;

  public CollectionActivitiesModel(){

  }
  public CollectionActivitiesModel(JSONObject data){
    try{
      this.activitiesId = data.getString("id");
      this.activitiesName = data.getString("name");
      this.activitiesShortName = data.getString("short_name");
    }catch (JSONException e){
      Log.e(Globals.TAG, "ActivitiesModel: "+e.toString());
    }
  }

  public String getActivitiesName() {
    return activitiesName;
  }

  public void setActivitiesName(String activitiesName) {
    this.activitiesName = activitiesName;
  }

  public String getActivitiesId() {
    return activitiesId;
  }
  public String getActivitiesShortName() {
    return activitiesShortName;
  }
  public void setActivitiesId(String activitiesId) {
    this.activitiesId = activitiesId;
  }
  public void setActivitiesShortName(String activitiesShortName){
    this.activitiesShortName = activitiesShortName;
  }
}
