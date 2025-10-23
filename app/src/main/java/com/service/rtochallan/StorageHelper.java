package com.service.rtochallan;

import android.content.Context;
import android.content.SharedPreferences;

public class StorageHelper {

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public StorageHelper(Context context) {
        Helper helper = new Helper();
        String PREFS_NAME = helper.StorageName;
//        //Log.d(helper.TAG, "Storage Name : " + PREFS_NAME);
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Save a string value
    public void saveString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    // Retrieve a string value
    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    // Save an integer value
    public void saveInt(String key, int value) {
        editor.putInt(key, value);
        editor.apply();
    }

    // Save a long value
    public void saveLong(String key, long value) {
        editor.putLong(key, value);
        editor.apply();
    }

    // Retrieve a long value safely (handles old int values)
    public long getLong(String key, long defaultValue) {
        try {
            return sharedPreferences.getLong(key, defaultValue);
        } catch (ClassCastException e) {
            int oldValue = sharedPreferences.getInt(key, (int) defaultValue);
            saveLong(key, (long) oldValue);
            return (long) oldValue;
        }
    }


    // Retrieve an integer value
    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    // Save a boolean value
    public void saveBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    // Retrieve a boolean value
    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }
}

