package com.visym.collector.model;

import android.util.Log;

import com.visym.collector.utils.Globals;

import org.json.JSONException;
import org.json.JSONObject;

public class GalleryVideoList {



    String id;
    String raw_video_file_path;
    String video_id;
    String raw_json_file_path;
    String activities_list;
    String query_attribute;
    String collection_name;
    String rating_score;
    String status;
    String uploaded_date;
    String duration;
    String collectorId;



    String annotation_file_path;
    String collection_id;
    String collector_id;
    String instance_ids;
    String program_id;
    String program_name;
    String project_id;
    String project_name;
    String review_status;
    String thumbnail;
    String thumbnail_small;
    String week;


  public GalleryVideoList(JSONObject data) {
        this.raw_video_file_path = data.optString("raw_video_file_path");
        this.video_id = data.optString("video_id");
        this.annotation_file_path = data.optString("annotation_file_path");
        this.collection_id = data.optString("collection_id");
        this.collector_id = data.optString("collector_id");
        this.collection_name = data.optString("collection_name");
        this.instance_ids = data.optString("instance_ids");
        this.thumbnail_small = data.optString("thumbnail_small");
        this.duration = data.optString("duration");
        this.uploaded_date = data.optString("uploaded_date");
        this.instance_ids = data.optString("instance_ids");
        this.program_id = data.optString("program_id");
        this.program_name = data.optString("program_name");
        this.project_id = data.optString("project_id");
        this.project_name = data.optString("project_name");
        this.review_status = data.optString("review_status");
        this.thumbnail = data.optString("thumbnail");
        this.week = data.optString("week");

        this.collectorId = data.optString("collector_id");
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

    public String getRatingScore() {
        return rating_score;
    }

    public void setRatingScore(String rating_score) {
        this.rating_score = rating_score;
    }

    public String getThumbnail_small() {
        return thumbnail_small;
    }

    public void setThumbnail_small(String thumbnail_small) {
        thumbnail_small = thumbnail_small;
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

    public String getUploaded_date() {
        return uploaded_date;
    }

    public void setUploaded_date(String uploaded_date) {
        this.uploaded_date = uploaded_date;
    }
    public String getRating_score() {
        return rating_score;
    }

    public void setRating_score(String rating_score) {
        this.rating_score = rating_score;
    }

    public String getAnnotation_file_path() {
        return annotation_file_path;
    }

    public void setAnnotation_file_path(String annotation_file_path) {
        this.annotation_file_path = annotation_file_path;
    }

    public String getCollection_id() {
        return collection_id;
    }

    public void setCollection_id(String collection_id) {
        this.collection_id = collection_id;
    }

    public String getCollector_id() {
        return collector_id;
    }

    public void setCollector_id(String collector_id) {
        this.collector_id = collector_id;
    }

    public String getInstance_ids() {
        return instance_ids;
    }

    public void setInstance_ids(String instance_ids) {
        this.instance_ids = instance_ids;
    }

    public String getProgram_id() {
        return program_id;
    }

    public void setProgram_id(String program_id) {
        this.program_id = program_id;
    }

    public String getProgram_name() {
        return program_name;
    }

    public void setProgram_name(String program_name) {
        this.program_name = program_name;
    }

    public String getProject_id() {
        return project_id;
    }

    public void setProject_id(String project_id) {
        this.project_id = project_id;
    }

    public String getProject_name() {
        return project_name;
    }

    public void setProject_name(String project_name) {
        this.project_name = project_name;
    }

    public String getReview_status() {
        return review_status;
    }

    public void setReview_status(String review_status) {
        this.review_status = review_status;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public String getCollectorId() {
        return collectorId;
    }

    public void setCollectorId(String collectorId) {
        this.collectorId = collectorId;
    }
}
