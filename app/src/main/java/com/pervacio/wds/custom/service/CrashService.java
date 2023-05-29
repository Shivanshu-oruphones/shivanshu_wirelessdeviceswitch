package com.pervacio.wds.custom.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.work.Configuration;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.custom.CrashWorkManager;

/**
 * Created by Surya Polasapalli on 14/05/2019.
 * <p>
 * This service runs in the another process
 * used to send the command to other device when app crashed.
 */

public class CrashService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        DLog.log("<--createWorkerCrashUpdate");
        try {
            final OneTimeWorkRequest simpleRequest = new OneTimeWorkRequest.Builder(CrashWorkManager.class)
                    .addTag("simple_crash_work")
                    .build();
            WorkManager.initialize(this,new Configuration.Builder().build());
            if (WorkManager.getInstance() != null) {
                WorkManager.getInstance().enqueue(simpleRequest);
            }
            DLog.log("createWorkerCrashUpdate-->");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    }


}
