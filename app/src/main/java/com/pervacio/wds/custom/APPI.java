package com.pervacio.wds.custom;

import android.app.Application;
import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.pervacio.crashreportlib.LogReporting;
import com.pervacio.wds.BuildConfig;
/**
 * Created by Surya Polasanapalli on 08-03-2016.
 */

public class APPI extends org.pervacio.onediaglib.APPI {
    private static Context applicationContext;

    public APPI() {
    }

    public void onCreate() {
        super.onCreate();
        applicationContext = this;
        //LogReporting.initialize(applicationContext, "WDS", BuildConfig.FLAVOR, "http://182.75.101.100:8080/CommonServices/fileupload");
        //FirebaseApp.initializeApp(this);
    }

    public static Context getAppContext() {
        return applicationContext;
    }
}