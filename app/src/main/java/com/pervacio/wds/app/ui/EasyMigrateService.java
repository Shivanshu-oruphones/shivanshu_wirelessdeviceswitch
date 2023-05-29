package com.pervacio.wds.app.ui;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.app.EMRemoteDeviceManager;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.custom.models.FeatureConfig;
import com.pervacio.wds.custom.service.TransactionLogService;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.NetworkUtil;
import com.pervacio.wds.custom.utils.PreferenceHelper;
import com.pervacio.wds.sdk.internal.CMDCryptoSettings;

public class EasyMigrateService extends Service {
    static EMGlobals emGlobals = new EMGlobals();
    public EasyMigrateService() {
    }

    int mStartMode;       // indicates how to behave if the service is killed
    private final IBinder mBinder = new LocalBinder();
    boolean mAllowRebind; // indicates whether onRebind should be used
    private EMRemoteDeviceManager mRemoteDeviceManager;

    public class LocalBinder extends Binder {
        EasyMigrateService getService() {
            // Return this instance of LocalService so clients can call public methods
            return EasyMigrateService.this;
        }
    }

    EMRemoteDeviceManager getRemoteDeviceManager() {
        return mRemoteDeviceManager;
    }

    @Override
    public void onCreate() {
        DLog.log("onCreate >> EasyMigrateService");
        try {
            boolean isFinished = PreferenceHelper.getInstance(emGlobals.getmContext()).getBooleanItem(Constants.PREF_FINISH_CLICKED);
            if (isFinished) {
                stopSelf();
            } else {
                EMGlobals.initialize();
                CMDCryptoSettings.initialize();
                mRemoteDeviceManager = new EMRemoteDeviceManager(emGlobals.getmContext());
                mRemoteDeviceManager.start();
            }
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        return START_NOT_STICKY;
    }
    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        DLog.log("onUnbind >> EasyMigrateService");
        stopSelf();
        return mAllowRebind;
    }
    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }
    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed

        DLog.log("onDestroy >> EasyMigrateService");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        DLog.log("******** ON TASK REMOVED - APP FORCE CLOSED ********");
        try {
            if (CommonUtil.getInstance().getMigrationStatus() != Constants.MIGRATION_SUCCEEDED && mRemoteDeviceManager != null) {
                mRemoteDeviceManager.sendTextCommand(Constants.COMMAND_APP_KILLED);
            }
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                DLog.log(e.getMessage());
            }
            EMNetworkManagerClientUIHelper.disconnectFromAnyNetworks();
            EMNetworkManagerHostUIHelper.stopAnyExistingHostNetwork();
            boolean logsUploaded = PreferenceHelper.getInstance(getApplicationContext()).getBooleanItem(Constants.PREF_UPLOAD_FINISHED);
            if(CommonUtil.getInstance().getMigrationStatus()!= Constants.MIGRATION_SUCCEEDED && !logsUploaded && !Constants.IS_MMDS){
                DLog.log("Uploading the logs >>>>>>>>>>");
                DashboardLog.getInstance().geteDeviceSwitchSession().setCancellationReason(Constants.CANCEL_REASON.FORCE_CLOSED.value());
                DashboardLog.getInstance().geteDeviceSwitchSession().setSessionStatus(Constants.SESSION_STATUS.CANCELLED.value());
                DashboardLog.getInstance().geteDeviceSwitchSession().setSessionStage(Constants.SESSION_STAGE.TRANSFER_CLOSED.value());
                DashboardLog.getInstance().geteDeviceSwitchSession().setEndDateTime(String.valueOf(System.currentTimeMillis()));
                if (CommonUtil.getInstance().getBackupStartedTime() != 0) {
                    DashboardLog.getInstance().geteDeviceSwitchSession().setActualTimeInMS(String.valueOf(System.currentTimeMillis() - CommonUtil.getInstance().getBackupStartedTime()));
                }
                if (FeatureConfig.getInstance().getProductConfig().isTransactionLoggingEnabled()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        EMUtility.createWorkerForDBUpdate();
                    } else {
                        startService(TransactionLogService.getServiceIntent());
                    }
                }
            }
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NetworkUtil.enableAllNetworks(true, -1);
            if (mNotificationManager != null) {
                mNotificationManager.cancel(0);
            }
            onUnbind(rootIntent);
            stopSelf();
        } catch (Exception e){
            DLog.log(e.getMessage());
        }
    }
}
