package com.visym.collector.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

import java.util.List;

public class CollectionModel implements Parcelable {

    private String trainingVideosOverlay;
    private String id;
    private String collectionName;
    private String collectionId;
    private String projectId;
    private String projectName;
    private String programName;
    private String programId;
    private String collectionDescription;
    private String activities;

    private String defaultObject;
    private String collectorEmail;
    private String trainingVideoURL;

    private String trainingVideoJsonUrl;
    private boolean active;
    private String objectList;
    private String trainingVideos;
    private String trainingVideosLow;

    private String activityShortNames;
    private boolean isPlayTrainingVideo;

    private  String consentOverLayText;
    private boolean isConsentRequired;

    public CollectionModel() {
    }

    public CollectionModel(String collectionName, String collectionId, String projectId, String activities) {
        this.collectionName = collectionName;
        this.collectionId = collectionId;
        this.projectId = projectId;
        this.activities = activities;
    }

    public CollectionModel(JSONObject data) {
        this.id = data.optString("id");
        this.collectionName = data.optString("collection_name");
        this.collectionId = data.optString("collection_id");
        this.projectId = data.optString("project_id");
        this.projectName = data.optString("project_name");
        this.programName = data.optString("program_name");
        this.programId = data.optString("program_id");
        this.collectionDescription = data.optString("collection_description");

        this.activities = data.optString("activities");
        this.defaultObject = data.optString("default_object");
        this.collectorEmail = data.optString("collector_email");
        this.trainingVideoURL = data.optString("training_videos");
        this.objectList = data.optString("object_list");
        this.active = data.optBoolean("active");
        this.activityShortNames = data.optString("activity_short_names");
        this.isPlayTrainingVideo = data.optBoolean("play_training_video");
        this.consentOverLayText = data.optString("consent_overlay_text");
        this.trainingVideosOverlay = data.optString("training_videos_overlay");
        this.isConsentRequired = data.optBoolean("isConsentRequired");

    }

    protected CollectionModel(Parcel in) {
        id = in.readString();
        collectionName = in.readString();
        collectionId = in.readString();
        projectId = in.readString();
        projectName = in.readString();
        programName = in.readString();
        programId = in.readString();
        collectionDescription = in.readString();
        activities = in.readString();
        defaultObject = in.readString();
        collectorEmail = in.readString();
        trainingVideoURL = in.readString();
        trainingVideoJsonUrl = in.readString();
        active = in.readByte() != 0;
        objectList = in.readString();
        trainingVideos = in.readString();
        trainingVideosLow = in.readString();
        activityShortNames = in.readString();
        trainingVideosOverlay = in.readString();
        isPlayTrainingVideo = in.readByte() != 0;
        consentOverLayText = in.readString();
        isConsentRequired = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(collectionName);
        dest.writeString(collectionId);
        dest.writeString(projectId);
        dest.writeString(projectName);
        dest.writeString(programName);
        dest.writeString(programId);
        dest.writeString(collectionDescription);
        dest.writeString(activities);
        dest.writeString(defaultObject);
        dest.writeString(collectorEmail);
        dest.writeString(trainingVideoURL);
        dest.writeString(trainingVideoJsonUrl);
        dest.writeByte((byte) (active ? 1 : 0));
        dest.writeString(objectList);
        dest.writeString(trainingVideos);
        dest.writeString(trainingVideosLow);
        dest.writeString(activityShortNames);
        dest.writeString(trainingVideosOverlay);
        dest.writeByte((byte) (isPlayTrainingVideo ? 1 : 0));
        dest.writeString(consentOverLayText);
        dest.writeByte((byte) (isConsentRequired ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CollectionModel> CREATOR = new Creator<CollectionModel>() {
        @Override
        public CollectionModel createFromParcel(Parcel in) {
            return new CollectionModel(in);
        }

        @Override
        public CollectionModel[] newArray(int size) {
            return new CollectionModel[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
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

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getCollectionDescription() {
        return collectionDescription;
    }

    public void setCollectionDescription(String collectionDescription) {
        this.collectionDescription = collectionDescription;
    }

    public String getActivities() {
        return activities;
    }

    public void setActivities(String activities) {
        this.activities = activities;
    }

    public String getDefaultObject() {
        return defaultObject;
    }

    public void setDefaultObject(String defaultObject) {
        this.defaultObject = defaultObject;
    }

    public String getCollectorEmail() {
        return collectorEmail;
    }

    public void setCollectorEmail(String collectorEmail) {
        this.collectorEmail = collectorEmail;
    }

    public String getTrainingVideoURL() {
        return trainingVideoURL;
    }

    public void setTrainingVideoURL(String trainingVideoURL) {
        this.trainingVideoURL = trainingVideoURL;
    }

    public String getTrainingVideoJsonUrl() {
        return trainingVideoJsonUrl;
    }

    public void setTrainingVideoJsonUrl(String trainingVideoJsonUrl) {
        this.trainingVideoJsonUrl = trainingVideoJsonUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getObjectList() {
        return objectList;
    }

    public void setObjectList(String objectList) {
        this.objectList = objectList;
    }

    public String getTrainingVideos() {
        return trainingVideos;
    }

    public void setTrainingVideos(String trainingVideos) {
        this.trainingVideos = trainingVideos;
    }

    public String getTrainingVideosLow() {
        return trainingVideosLow;
    }

    public void setTrainingVideosLow(String trainingVideosLow) {
        this.trainingVideosLow = trainingVideosLow;
    }

    public String getActivityShortNames() {
        return activityShortNames;
    }

    public void setActivityShortNames(String activityShortNames) {
        this.activityShortNames = activityShortNames;
    }

    public boolean isPlayTrainingVideo() {
        return isPlayTrainingVideo;
    }

    public void setPlayTrainingVideo(boolean playTrainingVideo) {
        isPlayTrainingVideo = playTrainingVideo;
    }

    public String getConsentOverLayText() {
        return consentOverLayText;
    }

    public void setConsentOverLayText(String consentOverLayText) {
        this.consentOverLayText = consentOverLayText;
    }

    public boolean isConsentRequired() {
        return isConsentRequired;
    }

    public void setConsentRequired(boolean consentRequired) {
        isConsentRequired = consentRequired;
    }

    public String getTrainingVideosOverlay() {
        return trainingVideosOverlay;
    }
}
