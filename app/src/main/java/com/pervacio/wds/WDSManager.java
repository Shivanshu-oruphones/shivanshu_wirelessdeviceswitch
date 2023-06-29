package com.pervacio.wds;

import android.content.Context;
import android.content.Intent;

import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.custom.SplashActivity;
import com.pervacio.wds.custom.utils.DashboardLog;

public class WDSManager {
    private static WDSManager wdsManager;
    private WDSStateListener wdsStateListener;
    private WDSManager() {

    }

    public static WDSManager getInstance() {
        if(wdsManager == null) {
           wdsManager = new WDSManager();
        }
        return wdsManager;
    }

    public interface WDSStateListener {
        void onWDSCompleted();
        void onWDSStarted();
    }

    public void setWDSStateListner(WDSStateListener listner) {
        wdsStateListener = listner;
    }

    public WDSStateListener getWdsStateListener() {
        return wdsStateListener;
    }

    public void startWDS(Context context) {
        EMGlobals emGlobals = new EMGlobals();
        emGlobals.setmContext(context);
        DashboardLog.getInstance().setTransferFinished(false);
        Intent intent = new Intent(context, SplashActivity.class);
        context.startActivity(intent);
    }

}
