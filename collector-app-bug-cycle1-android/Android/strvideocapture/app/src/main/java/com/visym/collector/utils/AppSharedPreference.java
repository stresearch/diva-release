package com.visym.collector.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSharedPreference {

    public static final String ORIENTATION_KEY = "orientation";
    public static final String FRAME_OBJECT_LABEL = "OBJECT_LABEL";
    private static AppSharedPreference appSharedPreference;
    private final String PREF_NAME = "str";
    private SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    private AppSharedPreference(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // JEBYRNE: this will create globally accessible, mutable state with strong consistency guarantees on write
    // - This is used by com.visym.collector.utils.FrameUtil to access the most recent video annotations using keys defined in com.visym.collector.utils.Constant.*
    // - From the docs:  Note: This class provides strong consistency guarantees. It is using expensive operations which might slow down an app. Frequently changing properties or properties where loss can be tolerated should use other mechanisms.
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
        editor.putString(key, value);
        editor.apply();
    }

    public void storeValue(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    public void storeValue(String key, Integer value) {
        editor.putInt(key, value);
        editor.apply();
    }

    public void clearValue(String key) {
        editor.remove(key);
        editor.apply();
    }

    public void clearAll() {
        editor.clear().apply();
    }
}
