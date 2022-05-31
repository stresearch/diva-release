package com.visym.collector.model;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONException;
import org.json.JSONObject;

public class UserModel implements Parcelable {
    private String account;
    private String videoStats;
    private String payment;

    public UserModel(JSONObject userData){
        try {
            this.account = userData.getString("account");
            this.videoStats = userData.getString("videoStats");
            this.payment = userData.getString("payment");
        }catch (JSONException e){
            e.printStackTrace();
        }

    }

    protected UserModel(Parcel in) {
        account = in.readString();
        videoStats = in.readString();
        payment = in.readString();
    }

    public static final Creator<UserModel> CREATOR = new Creator<UserModel>() {
        @Override
        public UserModel createFromParcel(Parcel in) {
            return new UserModel(in);
        }

        @Override
        public UserModel[] newArray(int size) {
            return new UserModel[size];
        }
    };

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getVideos() {
        return videoStats;
    }

    public void setVideos(String videoStats) {
        this.videoStats = videoStats;
    }

    public String getPayment() {
        return payment;
    }

    public void setPayment(String payment) {
        this.payment = payment;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(account);
            dest.writeString(videoStats);
            dest.writeString(payment);
    }
}
