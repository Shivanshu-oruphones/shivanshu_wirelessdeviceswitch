package com.pervacio.wds.app;

import android.content.Context;
import android.webkit.WebView;

import org.pervacio.onediaglib.APPI;

// Grim class for sharing globals about the app
// Avoids passing random objects all over the place that are used extensively
// USE WITH CARE - avoid adding stuff here unless if possible

public class EMGlobals {
    static EMGlobals emGlobals = new EMGlobals();
    public static void initialize() {
        mInstance = new EMGlobals();
    }

    public static void setJavascriptWebView(WebView aWebView) {
        if (mInstance != null)
            mInstance.setJavascriptWebViewInstance(aWebView);
    }

    public static void setCancelled(WebView aWebView) {
        if (mInstance != null)
            mInstance.setJavascriptWebViewInstance(aWebView);
    }

    public void setJavascriptWebViewInstance(WebView aWebView) {
        mJavascriptWebView = aWebView;
    }

    static public WebView getJavascriptWebView() {
        if (mInstance != null)
            return mInstance.getJavascriptWebViewInstance();
        else
            return null;
    }

    public WebView getJavascriptWebViewInstance() {
        return mJavascriptWebView;
    }

    private static EMGlobals mInstance;
    private WebView mJavascriptWebView;

    public Context getmContext() {
        if (mContext != null) {
            return mContext;
        } else {
            return APPI.getAppContext().getApplicationContext();
        }
    }

    public void setmContext(Context mContext) {
        this.mContext = mContext;
    }

    private Context mContext;
}
