package com.visym.collector.model;

import android.util.Log;

import com.visym.collector.utils.Globals;

import org.json.JSONException;
import org.json.JSONObject;

public class UserVideoList {


  private String id;
  private  String raw_video_file_path;
  private String video_id;
  private String raw_json_file_path;
  private String activities_list;
  private String query_attribute;
  private String collection_name;
  private String rating;
  private String Thumbnail_small;
  private String status;



  private String display_duratiion;
  private String uploaded_date;

  private String duration;
  private String collectionId;



  private String video_sharing_link;
  private String json_sharing_link;



  private String video_state;



  public UserVideoList(JSONObject data){
    try {
      Log.e(Globals.TAG, "UserVideoList: "+data.toString());
      this.id = data.getString("id");
      this.raw_video_file_path = data.getString("raw_video_file_path");
      this.video_id = data.getString("video_id");
      this.raw_json_file_path = data.getString("raw_json_file_path");
      this.activities_list = data.getString("activities_list");
      this.query_attribute = data.getString("query_attribute");
      this.collection_name = data.getString("collection_name");
      if(!data.optString("rating").isEmpty()) {
        this.rating = data.getString("rating");
      }else {
        this.rating = "0";
      }
      if(!data.optString("Thumbnail_small").isEmpty()) {
        this.Thumbnail_small = data.getString("Thumbnail_small");
      }else{
        this.Thumbnail_small = "empty";
      }
      if(!data.optString("video_state").isEmpty()){
        this.video_state = data.getString("video_state");
      }else{
        this.video_state = "video_state";
      }
      if(!data.optString("display_duration").isEmpty()){
        this.display_duratiion = data.getString("display_duratiion");
      }else {
        this.display_duratiion = "00:00";
      }
      if(data.has("json_sharing_link") && !data.optString("json_sharing_link").isEmpty()){
        this.json_sharing_link = data.getString("json_sharing_link");
      }
      if(data.has("video_sharing_link") && !data.optString("video_sharing_link").isEmpty()){
        this.video_sharing_link = data.getString("video_sharing_link");
      }

      this.duration = data.getString("duration");
      this.uploaded_date = data.getString("uploaded_date");
      this.collectionId = data.getString("collection_id");

    }catch (JSONException e){
      e.printStackTrace();
      Log.e(Globals.TAG, "UserVideoList:exce "+e.toString());
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getRaw_video_file_path() {
    return raw_video_file_path;
  }

  public void setRaw_video_file_path(String raw_video_file_path) {
    this.raw_video_file_path = raw_video_file_path;
  }

  public String getVideo_id() {
    return video_id;
  }

  public void setVideo_id(String video_id) {
    this.video_id = video_id;
  }

  public String getRaw_json_file_path() {
    return raw_json_file_path;
  }

  public void setRaw_json_file_path(String raw_json_file_path) {
    this.raw_json_file_path = raw_json_file_path;
  }

  public String getActivities_list() {
    return activities_list;
  }

  public void setActivities_list(String activities_list) {
    this.activities_list = activities_list;
  }

  public String getQuery_attribute() {
    return query_attribute;
  }

  public void setQuery_attribute(String query_attribute) {
    this.query_attribute = query_attribute;
  }

  public String getCollection_name() {
    return collection_name;
  }

  public void setCollection_name(String collection_name) {
    this.collection_name = collection_name;
  }

  public String getRating() {
    return rating;
  }

  public void setRating(String rating) {
    this.rating = rating;
  }

  public String getThumbnail_small() {
    return Thumbnail_small;
  }

  public void setThumbnail_small(String thumbnail_small) {
    Thumbnail_small = thumbnail_small;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDuration() {
    return duration;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }
  public String getDisplay_duratiion() {
    return display_duratiion;
  }

  public void setDisplay_duratiion(String display_duratiion) {
    this.display_duratiion = display_duratiion;
  }

  public String getUploadedDate(){
    return uploaded_date;
  }
  public void setUploaded_date(String date){
    this.uploaded_date = date;
  }

  public String getCollectionId() {
    return collectionId;
  }

  public void setCollectionId(String collectionId) {
    this.collectionId = collectionId;
  }
  public String getUploaded_date() {
    return uploaded_date;
  }

  public String getVideo_sharing_link() {
    return video_sharing_link;
  }

  public void setVideo_sharing_link(String video_sharing_link) {
    this.video_sharing_link = video_sharing_link;
  }

  public String getJson_sharing_link() {
    return json_sharing_link;
  }

  public void setJson_sharing_link(String json_sharing_link) {
    this.json_sharing_link = json_sharing_link;
  }

  public String getVideo_state() {
    return video_state;
  }

  public void setVideo_state(String video_state) {
    this.video_state = video_state;
  }
}
