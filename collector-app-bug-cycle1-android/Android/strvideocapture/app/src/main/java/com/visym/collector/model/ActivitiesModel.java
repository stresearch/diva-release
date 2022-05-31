package com.visym.collector.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.visym.collector.utils.Globals;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.jar.JarException;

public class ActivitiesModel implements Parcelable {
    private String activitiesName;
    private String activitiesId;
    private String activitiesShortName;

    public ActivitiesModel(JSONObject data){
        try{
            this.activitiesId = data.getString("id");
            this.activitiesName = data.getString("name");
            this.activitiesShortName = data.getString("short_name");
        }catch (JSONException e){
            Log.e(Globals.TAG, "ActivitiesModel: "+e.toString());
        }
    }

    public static final Creator<ActivitiesModel> CREATOR = new Creator<ActivitiesModel>() {
        @Override
        public ActivitiesModel createFromParcel(Parcel in) {
            return new ActivitiesModel(in);
        }

        @Override
        public ActivitiesModel[] newArray(int size) {
            return new ActivitiesModel[size];
        }
    };

    public String getActivitiesName() {
        return activitiesName;
    }

    public void setActivitiesName(String activitiesName) {
        this.activitiesName = activitiesName;
    }

    public String getActivitiesId() {
        return activitiesId;
    }

    public void setActivitiesId(String activitiesId) {
        this.activitiesId = activitiesId;
    }

    public String getActivitiesShortName() {
        return activitiesShortName;
    }

    public void setActivitiesShortName(String activitiesShortName) {
        this.activitiesShortName = activitiesShortName;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(activitiesId);
        dest.writeString(activitiesName);
        dest.writeString(activitiesShortName);
    }

    private ActivitiesModel(Parcel in){
        activitiesId = in.readString();
        activitiesName = in.readString();
        activitiesShortName = in.readString();
    }
}
