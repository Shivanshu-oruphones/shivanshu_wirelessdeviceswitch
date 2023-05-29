package com.pervacio.wds.custom.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.pervacio.wds.app.EMGlobals;

import java.lang.reflect.Type;

/**
 *
 * Created by Surya Polasanapalli on 08-03-2016.
 *
 * Manage the preferences
 */

public class PreferenceHelper {

    private static SharedPreferences prefs;
    private static PreferenceHelper instance;
    static EMGlobals emGlobals = new EMGlobals();
    private PreferenceHelper() {
    }

    public static PreferenceHelper getInstance(Context ctx) {
        Context context = emGlobals.getmContext();
        prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        if (instance == null) {
            instance = new PreferenceHelper();
        }
        return instance;
    }

    public Boolean getBooleanItem(String key) {
        String value = getStringItem(key);
        if (value != null) {
            try {
                Boolean parsedValue = Boolean.valueOf(value);
                return parsedValue;
            } catch (Exception e) {
                // If the value cannot be parsed then just return default
            }
        }
        return Boolean.FALSE;
    }

    public void putIntegerItem(String key,
                               Integer value) {

        Integer intValue = value!=null?value:null;
        prefs.edit().putInt(key, intValue).commit();
    }

    public int getIntegerItem(String key
                               ) {
        if (prefs.contains(key)) {
            try {
                Integer item;
                item = prefs.getInt(key, 0);
                return item;
            } catch (Exception e) {

            }
        }
        return 0;
    }

    public String getStringItem(String key) {

        if (prefs.contains(key)) {
            try {
                String item;
                item = prefs.getString(key, null);
                return item;
            } catch (Exception e) {
                // If the item cannot be retrieved from the preferences then it is older version and needs to be reset.
            }
        }
        return null;
    }

    public void putStringItem(String key, String value) {
        if (value != null) {
            String valueToPut = value;
            prefs.edit().putString(key, valueToPut).commit();
        } else {
            prefs.edit().remove(key).commit();
        }
    }

    public void putObjectAsJson(String key,
                                Object obj) {
        String json = new Gson().toJson(obj);
        putStringItem(key, json);
    }

    public <T> T getObjectFromJson(String key,
                                   Type objClass) {
        String json = getStringItem(key);
        return new Gson().fromJson(json, objClass);
    }


    public void putBooleanItem(String key,
                               Boolean value) {
        String stringValue = value != null ? value.toString() : null;
        putStringItem(key, stringValue);
    }

    public void putLongItem(String key,
                            Long value) {
        String stringValue = value != null ? value.toString() : null;
        putStringItem(key, stringValue);
    }

    public void clearPreferences() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
    }

}
