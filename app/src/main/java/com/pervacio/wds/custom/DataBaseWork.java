package com.pervacio.wds.custom;


import android.content.Context;
import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.custom.asynctask.MultipartUtility;
import com.pervacio.wds.custom.asynctask.TransactionLogging;
import com.pervacio.wds.custom.receivers.UninstallBroadcastReceiver;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.PreferenceHelper;

import java.io.File;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import static com.pervacio.wds.custom.utils.Constants.HTTP_AUTHENTICATION_PASSWORD;
import static com.pervacio.wds.custom.utils.Constants.HTTP_AUTHENTICATION_USERNAME;

public class DataBaseWork extends Worker {
    public static final String DEBUG_LOG_SERVER_CALL = "debugLog";
    static EMGlobals emGlobals = new EMGlobals();
    private static final String STR_LOGGING_ENDPOINT = "STR_LOGGING_ENDPOINT";
    private static final String STR_S3_ENDPOINT = "STR_S3_ENDPOINT";
    private static final String STR_IS_DEST = "STR_IS_DEST";
    private static final String STR_UPDATE_DB = "STR_UPDATE_DB";
    private static final String STR_DEVICES_PAIRED = "STR_DEVICES_PAIRED";
    private static final String STR_DEVICES_DETAILS = "STR_DEVICES_DETAILS";
    private static final String STR_SESSION_DATA = "STR_SESSION_DATA";
    private static final String STR_SESSION_ID = "STR_SESSION_ID";
    private boolean isDest;
    private String s3Endpoint;
    private String deviceDetails;
    private String deviceSwitchSessionID;
    private String gsonString;
    private String loggingEndpoint;

    public DataBaseWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    @NonNull
    @Override
    public Result doWork() {
        DLog.log("<--- Do work");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
/*        loggingEndpoint = getInputData().getString(STR_LOGGING_ENDPOINT,"");
        s3Endpoint = getInputData().getString(STR_S3_ENDPOINT, "");
        isDest = getInputData().getBoolean(STR_IS_DEST,false);
        deviceDetails = getInputData().getString(STR_DEVICES_DETAILS, "");
        gsonString = getInputData().getString(STR_SESSION_DATA, "");
        deviceSwitchSessionID = getInputData().getString(STR_SESSION_ID,"");*/

        loggingEndpoint = getInputData().getString(STR_LOGGING_ENDPOINT);
        s3Endpoint = getInputData().getString(STR_S3_ENDPOINT);
        isDest = getInputData().getBoolean(STR_IS_DEST,false);
        deviceDetails = getInputData().getString(STR_DEVICES_DETAILS);
        gsonString = getInputData().getString(STR_SESSION_DATA);
        deviceSwitchSessionID = getInputData().getString(STR_SESSION_ID);
        sendTransactionDetailsToServer();
        if(Constants.ENABLE_LOGS_UPLOAD) {
            sendDebugLogsToServer();
        }
        PreferenceHelper.getInstance(getApplicationContext()).putBooleanItem(Constants.PREF_UPLOAD_FINISHED, true);
        //Start uninstall popup only when clicked on finished button
/*        if(PreferenceHelper.getInstance(getApplicationContext()).getBooleanItem(Constants.PREF_FINISH_CLICKED)) {
            if (!BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT)) {
                DLog.log("Finished button has been clicked. Starting uninstall alarm.");
                UninstallBroadcastReceiver.startUninstallAlarm(getApplicationContext());
            }
        }*/
        DLog.log("Do work -->");
        return Result.success();
    }

    private void sendDebugLogsToServer() {
        DLog.log("<--sendDebugLogsToServer");
        String mRole = isDest ? "DST" : "SRC";
        MultipartUtility s3Upload = new MultipartUtility(s3Endpoint,
                Constants.HTTP_AUTHENTICATION_USERNAME + ":" + Constants.HTTP_AUTHENTICATION_PASSWORD, new String[]{mRole, deviceDetails, deviceSwitchSessionID});
        s3Upload.setIsBackgroundCall(true);
        boolean result = s3Upload.uploadLogFileToServer(new File(emGlobals.getmContext().getFilesDir() + "/log.txt"));
        DLog.log("File upload status : " + result);
        if (!result) {
            try {
                Thread.sleep(5000);
            } catch (Exception ex) {
                DLog.log("exception while sleeping : " + ex.getMessage());
            }
            sendDebugLogsToServer();
        } else {
            DLog.resetLogFile();
        }
        DLog.log("sendDebugLogsToServer-->");
    }

    private void sendTransactionDetailsToServer() {
        DLog.log("<--sendTransactionDetailsToServer");
        TransactionLogging transactionLogging = new TransactionLogging();
        String result = transactionLogging.doInBackground(loggingEndpoint, HTTP_AUTHENTICATION_USERNAME + ":" + HTTP_AUTHENTICATION_PASSWORD, gsonString);
        DLog.log("Server call result : " + result);
        if (EMUtility.isNullOrEmpty(result) || result.equalsIgnoreCase("NO_INTERNET") || result.equalsIgnoreCase("TIMEOUT") || result.equalsIgnoreCase("ERROR")) {
            try {
                Thread.sleep(5000);
            } catch (Exception ex) {
                DLog.log("exception while sleeping : " + ex.getMessage());
            }
            sendTransactionDetailsToServer();
        }
        DLog.log("sendTransactionDetailsToServer-->");
    }
}