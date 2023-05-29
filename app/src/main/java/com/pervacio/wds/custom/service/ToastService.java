package com.pervacio.wds.custom.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;

import com.pervacio.wds.app.DLog;

/**
 * Created by Sathish on 7/4/2018.
 * <p>
 * This service runs in the another process
 * used to displaying the "Please wait" toast to the user while start over the app.
 */

public class ToastService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "Please wait few seconds while restarting...", Toast.LENGTH_SHORT).show();
        DLog.log("ToastService : " + "Service created");
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DLog.log("ToastService : " + "Service destroyed");
    }
}
