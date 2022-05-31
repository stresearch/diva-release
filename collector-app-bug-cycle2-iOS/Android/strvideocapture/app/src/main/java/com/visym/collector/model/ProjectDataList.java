package com.visym.collector.model;

import org.json.JSONException;
import org.json.JSONObject;

public class ProjectDataList {
  String id;
  String projectId;
  String projectName;
  String collectionsCount;

  public ProjectDataList(String id, String projectId, String projectName, String collectionsCount) {
    this.id = id;
    this.projectId = projectId;
    this.projectName = projectName;
    this.collectionsCount = collectionsCount;
  }

  public ProjectDataList(JSONObject projectList){
    try {
      this.id = projectList.getString("id");
      this.projectId = projectList.getString("project_id");
      this.projectName = projectList.getString("name");
      this.collectionsCount = projectList.getString("collectionsCount");
    }catch (JSONException e){
      e.printStackTrace();
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getCollectionsCount() {
    return collectionsCount;
  }

  public void setCollectionsCount(String collectionsCount) {
    this.collectionsCount = collectionsCount;
  }
}
