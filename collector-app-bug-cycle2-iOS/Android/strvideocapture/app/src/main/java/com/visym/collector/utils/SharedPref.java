package com.visym.collector.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPref {

  private SharedPreferences sharedPreferences;
  private SharedPreferences.Editor editor;

  public SharedPref(Context context) {
    String MY_PREFS_NAME = "SRTVC_PREF";
    sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE);
    editor = sharedPreferences.edit();
  }

  public void setValueToSharedPref(String key, String value) {
    editor.putString(key, value);
    editor.commit();
  }
  public void setBooleanToSharedPref(String key,boolean value){
    editor.putBoolean(key,value);
    editor.commit();
  }

  public void setRefreshTokenToSharedPref(String value) {
    editor.putString("refreshToken", value);
    editor.commit();
  }

  public void clearSharedPref() {
    editor.clear();
    editor.commit();
  }
  public String getUserId(){
    return  sharedPreferences.getString("userId",null);
  }
  public boolean isSyncOverWifi(){
    return sharedPreferences.getBoolean("syncOverWifi",false);
  }

  public String getToken() {
    return sharedPreferences.getString("accessToken", null);
  }

  public String getRefreshToken() {
    return sharedPreferences.getString("refreshToken", null);
  }

}
