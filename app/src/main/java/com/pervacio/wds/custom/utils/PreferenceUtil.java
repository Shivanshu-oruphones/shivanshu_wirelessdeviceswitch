package com.pervacio.wds.custom.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.pervacio.wds.app.EMGlobals;

import org.pervacio.onediaglib.APPI;

public class PreferenceUtil {
    static EMGlobals emGlobals = new EMGlobals();
    private static SharedPreferences prefs = emGlobals.getmContext().getSharedPreferences(emGlobals.getmContext().getPackageName(), Context.MODE_PRIVATE);
    public static String IS_CLOUD_VISION = "is_cloud_vision";




    public static void putString(String key,
                               String value) {
        prefs.edit().putString(key,value).commit();
    }

    public static String getString(String key){
        return prefs.getString(key,null);
    }

    public static void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key,value).commit();
    }

    public static boolean getBoolean(String key){
        return prefs.getBoolean(key,false);
    }

}
