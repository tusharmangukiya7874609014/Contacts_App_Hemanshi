package com.contactshandlers.contactinfoall.helper;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {

    private static SharedPreferencesManager instance = null;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    private SharedPreferencesManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static void init(Context context) {
        instance = new SharedPreferencesManager(context);
    }

    public static SharedPreferencesManager getInstance(){
        if (instance == null){
            instance = new SharedPreferencesManager(getInstance().context);
        }
        return instance;
    }

    public int getIntValue(final String key, final int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public void setIntValue(final String key, final int value) {
        editor.putInt(key, value);
        editor.commit();
    }

    public String getStringValue(final String key, final String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public void setStringValue(final String key, final String value) {
        editor.putString(key, value);
        editor.commit();
    }

    public boolean getBooleanValue(final String key, final boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public void setBooleanValue(final String key, final boolean value) {
        editor.putBoolean(key, value);
        editor.commit();
    }
}