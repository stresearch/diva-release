package com.visym.collector.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSharedPreference {

    public static final String ORIENTATION_KEY = "orientation";
    public static final String FRAME_OBJECT_LABEL = "OBJECT_LABEL";
    private static AppSharedPreference appSharedPreference;
    private final String PREF_NAME = "str";
    private SharedPreferences sharedPreferences;

    private AppSharedPreference(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static AppSharedPreference getInstance() {
        if (appSharedPreference == null) {
            appSharedPreference = new AppSharedPreference(Globals.getAppContext());
        }
        return appSharedPreference;
    }

    public String readString(String value) {
        return sharedPreferences.getString(value, null);
    }

    public int readInt(String value) {
        return sharedPreferences.getInt(value, 0);
    }

    public boolean readBoolean(String value) {
        return sharedPreferences.getBoolean(value, false);
    }

    public void storeValue(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void storeValue(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public void storeValue(String key, Integer value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public void clearValue(String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

    public void clearAll() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear().apply();
    }
}
