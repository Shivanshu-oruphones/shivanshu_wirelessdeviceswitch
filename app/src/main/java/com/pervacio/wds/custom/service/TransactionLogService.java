package com.pervacio.wds.custom.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import com.google.gson.Gson;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.ui.EasyMigrateActivity;
import com.pervacio.wds.custom.asynctask.MultipartUtility;
import com.pervacio.wds.custom.asynctask.TransactionLogging;
import com.pervacio.wds.custom.models.EDeviceSwitchSession;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.NetworkUtil;
import com.pervacio.wds.custom.utils.PreferenceHelper;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.work.Data;

import static com.pervacio.wds.custom.utils.Constants.HTTP_AUTHENTICATION_PASSWORD;
import static com.pervacio.wds.custom.utils.Constants.HTTP_AUTHENTICATION_USERNAME;
import static com.pervacio.wds.custom.utils.Constants.LOGGING_API_ENDPOINT;
import static com.pervacio.wds.custom.utils.Constants.LOG_UPLOAD_URL;

/**
 * In the event of cancellation or at the end of migration, a service will be started in background.
 *
 * This service will remain active even when the app has been closed and perform:
 * 1. Transaction Logging
 * 2. Debug log upload to S3 server
 *
 * This service approach was conceived to make sure that we do not miss any transactions on server side.
 *
 * @author Darpan Dodiya <darpan.dodiya@pervacio.com>
 */

public class TransactionLogService extends Service {
    static EMGlobals emGlobals = new EMGlobals();
    private static final String STR_SESSION_OBJ = "STR_SESSION_OBJ";
    private static final String STR_SESSION_DATA = "STR_SESSION_DATA";
    private static final String STR_SESSION_ID = "STR_SESSION_ID";
    private static final String STR_LOGGING_ENDPOINT = "STR_LOGGING_ENDPOINT";
    private static final String STR_S3_ENDPOINT = "STR_S3_ENDPOINT";
    private static final String STR_IS_DEST = "STR_IS_DEST";
    private static final String STR_UPDATE_DB = "STR_UPDATE_DB";
    private static final String STR_DEVICES_PAIRED = "STR_DEVICES_PAIRED";
    private static final String STR_DEVICES_DETAILS = "STR_DEVICES_DETAILS";

    private static final int S3_UPLOAD=2;
    private static final int TRANSACTION_UPLOAD =4;
    private static int SERVER_CALLS = 0;

    public enum CALLBACK_TYPE {
        TRANSACTION_LOG,
        S3_LOG
    }

    private boolean updateDBDetails;
    private boolean isDest;
    private boolean devicesPaired;
    private String deviceDetails;
    private String loggingEndpoint;
    private String s3Endpoint;
    private String gsonString;
    private EDeviceSwitchSession eds;
    private boolean serviceInitialized = false;
    private boolean networkObserverRegistered =false;


    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public TransactionLogService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);
        InitializeServerCall(intent);
        return START_REDELIVER_INTENT;
    }

    private void InitializeServerCall(Intent intent){
        try {
            DLog.log("In TransactionLogService.");
            SERVER_CALLS |= S3_UPLOAD;
            SERVER_CALLS |= TRANSACTION_UPLOAD;

            //Initialize the service
            serviceInitialized = true;

            //Get data from intent
            eds = (EDeviceSwitchSession) intent.getExtras().getSerializable(STR_SESSION_OBJ);
            updateDBDetails = intent.getExtras().getBoolean(STR_UPDATE_DB);
            isDest = intent.getExtras().getBoolean(STR_IS_DEST);
            deviceDetails = intent.getExtras().getString(STR_DEVICES_DETAILS);
            devicesPaired = intent.getExtras().getBoolean(STR_DEVICES_PAIRED);
            loggingEndpoint = intent.getExtras().getString(STR_LOGGING_ENDPOINT);
            s3Endpoint = intent.getExtras().getString(STR_S3_ENDPOINT);

            //Convert string to JSON object
            Gson gson = new Gson();
            gsonString = gson.toJson(eds);
            DLog.log("JSON: " + gsonString);
            //Call the server if network is available.
            if (NetworkUtil.isOnline()) {
                doServerCalls();
            } else {
                //Register network change listener
                registerNetworkListener();
            }
        } catch (Exception ex) {
            DLog.log(ex);
        }
    }

    private void registerNetworkListener() {
        if (!networkObserverRegistered) {
            networkObserverRegistered = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(networkObserver, filter);
        }
    }


    /**
     * Actually perform network operation. Two calls will be made, one for transaction logging and
     * one for S3 log upload.
     * <p>
     * In case of source device, the transaction logging call will not be made.
     */
    private void doServerCalls() {
        DLog.log("server calls " + SERVER_CALLS);
        updateTransactionDetails();
        if(Constants.ENABLE_LOGS_UPLOAD) {
            uploadLogFile();
        }
    }

    private void updateTransactionDetails() {
        if ((SERVER_CALLS & TRANSACTION_UPLOAD) != 0) {
            //Upload transaction log only for destination. Call only when there's no outstanding request.
            if ((!devicesPaired || updateDBDetails)) {
                //Create new instance and start service call
                TransactionLogging transactionLogging = new TransactionLogging(loggingEndpoint,
                        HTTP_AUTHENTICATION_USERNAME + ":" + HTTP_AUTHENTICATION_PASSWORD, gsonString, null);
                transactionLogging.setIsBackgroundCall(true);
                transactionLogging.setTlsCallBack(this);
                transactionLogging.startServiceCall();
            } else {
                SERVER_CALLS = SERVER_CALLS ^ TRANSACTION_UPLOAD;
            }
        }
    }
    private void uploadLogFile() {
        //Upload debug logs to S3 for both devices.
        DLog.log("Enter uploadLogFile");
        if ((SERVER_CALLS & S3_UPLOAD) != 0) {
            String mRole = isDest ? "DST" : "SRC";
            MultipartUtility s3Upload = new MultipartUtility(s3Endpoint,
                    Constants.HTTP_AUTHENTICATION_USERNAME + ":" + Constants.HTTP_AUTHENTICATION_PASSWORD, new String[]{mRole, deviceDetails, eds.getDeviceSwitchSessionId()});

            s3Upload.setIsBackgroundCall(true);
            s3Upload.setTlsCallBack(this);
            s3Upload.uploadLogToServer(new File(emGlobals.getmContext().getFilesDir() + "/log.txt"));
        }
    }

    @Override
    public void onDestroy() {
        DLog.log("Service destroyed");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Creating this method as utility so that code will not have to be duplicated whenever we want
     * to call the service.
     *
     * @return Intent of this service type.
     */
    public static Intent getServiceIntent() {
        DLog.log("Trying to getServiceIntent");
        Intent intent = new Intent(emGlobals.getmContext(), TransactionLogService.class);
        Bundle b = new Bundle();
        b.putSerializable(STR_SESSION_OBJ, DashboardLog.getInstance().geteDeviceSwitchSession());
        b.putString(STR_LOGGING_ENDPOINT, LOGGING_API_ENDPOINT);
        b.putString(STR_S3_ENDPOINT, LOG_UPLOAD_URL);
        b.putBoolean(STR_IS_DEST, DashboardLog.getInstance().isThisDest());
        b.putBoolean(STR_UPDATE_DB, DashboardLog.getInstance().isThisDest()||DashboardLog.getInstance().isUpdateDBdetails());
        b.putBoolean(STR_DEVICES_PAIRED, EasyMigrateActivity.devicesPaired);
        b.putString(STR_DEVICES_DETAILS, EMUtility.getDevicesCombinationDetails());
        intent.putExtras(b);

        //Kill all existing connections before starting service
        DashboardLog.getInstance().killExistingConnections();

        return intent;
    }

    public static Data getDataForServiceCall() {
        DLog.log("Trying to getData for final servere call");
        //Convert string to JSON object
        Gson gson = new Gson();
        String gsonString = gson.toJson(DashboardLog.getInstance().geteDeviceSwitchSession());

        Data data = new Data.Builder()
                .putString(STR_LOGGING_ENDPOINT, LOGGING_API_ENDPOINT)
                .putString(STR_S3_ENDPOINT, LOG_UPLOAD_URL)
                .putBoolean(STR_IS_DEST, DashboardLog.getInstance().isThisDest())
                .putBoolean(STR_UPDATE_DB, DashboardLog.getInstance().isThisDest() || DashboardLog.getInstance().isUpdateDBdetails())
                .putBoolean(STR_DEVICES_PAIRED, EasyMigrateActivity.devicesPaired)
                .putString(STR_SESSION_DATA, gsonString)
                .putString(STR_SESSION_ID, DashboardLog.getInstance().geteDeviceSwitchSession().getDeviceSwitchSessionId())
                .putString(STR_DEVICES_DETAILS, EMUtility.getDevicesCombinationDetails())
                .build();
        //Kill all existing connections before starting service
        DashboardLog.getInstance().killExistingConnections();
        return data;
    }

    /**
     * Register network observer to listen for network change events.
     *
     * This will happen in background and whenever there's an network connectivity change, we'll
     * try to call the servers.
     */
    private BroadcastReceiver networkObserver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            DLog.log("Network connectivity change");

            //Listen for the connectivity change only when the service is initialized
            if (serviceInitialized && intent.getExtras() != null) {
                final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

                if (ni != null && ni.isConnectedOrConnecting()) {
                    DLog.log("Network " + ni.getTypeName() + " connected.");

                    doServerCalls();

                } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                    DLog.log("There's no network connectivity");
                }
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                updateTransactionDetails();
            } else {
                uploadLogFile();
            }
        }
    };

    /**
     * From the network calls, call this method to notify about result of network operation.
     *
     * @param callbackType Whether it was S3 or transaction
     * @param result Whether or not the call was successful or not
     */
    public void sendCallBack(CALLBACK_TYPE callbackType, boolean result) {
        DLog.log("In sendCallBack. Type: " + callbackType + " Result: " + result );
        if (!result && !NetworkUtil.isOnline()) {
            registerNetworkListener();
        }
        switch (callbackType) {
            case TRANSACTION_LOG:
                DLog.log("TRANSACTION_LOG callback with result: " + result);
                if (result) {
                    SERVER_CALLS = SERVER_CALLS ^TRANSACTION_UPLOAD;
                } else if (NetworkUtil.isOnline()) {
                    mHandler.sendEmptyMessageDelayed(0, 5 * 1000L);
                }
                break;
            case S3_LOG:
                DLog.log("S3_LOG callback with result: " + result);
                if (result) {
                    SERVER_CALLS = SERVER_CALLS ^S3_UPLOAD;
                    DLog.resetLogFile();
                } else if (NetworkUtil.isOnline()) {
                    mHandler.sendEmptyMessageDelayed(1, 5 * 1000L);
                }
                break;

            default:
                break;
        }
        DLog.log("server calls "+ SERVER_CALLS);
        if (SERVER_CALLS == 0)
            stopService();
    }


    private void stopService(){
        DLog.log("Server calls finished, closing service.");
        serviceInitialized = false;
        mHandler.removeMessages(0);
        //Unregister the network change listener once everything's done.
        try {
            if (networkObserverRegistered) {
                networkObserverRegistered = false;
                unregisterReceiver(networkObserver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        PreferenceHelper.getInstance(getApplicationContext()).putBooleanItem(Constants.PREF_UPLOAD_FINISHED, true);
        //Start uninstall popup only when clicked on finished button
/*        if(PreferenceHelper.getInstance(getApplicationContext()).getBooleanItem(Constants.PREF_FINISH_CLICKED)) {
            if (!BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT)) {
                DLog.log("Finished button has been clicked. Starting uninstall alarm.");
                UninstallBroadcastReceiver.startUninstallAlarm(getApplicationContext());
            }
        }*/
        stopSelf();
    }
}
