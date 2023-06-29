package com.pervacio.wds.custom;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pervacio.crashreportlib.LogReporting;
import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.ui.EasyMigrateActivity;
import com.pervacio.wds.custom.imeireader.IMEIReadActivity;
import com.pervacio.wds.custom.models.FeatureConfig;
import com.pervacio.wds.custom.receivers.UninstallBroadcastReceiver;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.ContextWrapperHelper;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.DeviceInfo;
import com.pervacio.wds.custom.utils.NetworkUtil;
import com.pervacio.wds.custom.utils.PreferenceHelper;
import com.pervacio.wds.custom.utils.startLocationAlert;

import org.json.JSONException;
import org.json.JSONObject;
import org.pervacio.onediaglib.atomicfunctions.AFGPS;
import org.pervacio.onediaglib.atomicfunctions.AFHotspot;
import org.pervacio.onediaglib.atomicfunctions.AFWiFi;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.Collections;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.pervacio.wds.custom.utils.Constants.CLOUD_PAIRING_ENABLED;
import static com.pervacio.wds.custom.utils.Constants.FLAVOUR_PLAYSTORE;
import static com.pervacio.wds.custom.utils.Constants.FLAVOUR_TMS;
import static com.pervacio.wds.custom.utils.Constants.GET_PRELOADED_APPS_NAMES_FROM_SERVER;
import static com.pervacio.wds.custom.utils.Constants.IS_MMDS;
import static com.pervacio.wds.custom.utils.Constants.preLoadedAppNamesS3ListServer;
import static com.pervacio.wds.custom.utils.Constants.preLoadedAppsListServer;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SplashActivity extends Activity {

    boolean startOver = false;
    Handler handler = new Handler();
    Button letsgetStartedTextView;
    public static String storePin = null;
    public static boolean isStoreConfigSuccess = false;
    ProgressBar action_spinner;
    boolean selectedDontAskAgain = false;
    ImageView powered_by_logo;
    private String storeID = "";
    GpsChangeReceiver gpsChangeReceiver;
    static boolean locationRequested = false;
    private boolean isHotSpotSettingsCalled = false;
    private boolean isWiFiSettingsCalled = false;
    AFHotspot afHotspot;
    AFWiFi afWiFi;
    AFGPS afgps;
    NetworkStatesHandlr networkStatesHandlr;
    public static final int MSG_TURNOFF_HOTSPOT = 300;
    public static final int MSG_TURNON_WIFI = 301;
    public boolean locationPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DLog.log("SplashActivity onCreate() : " + DashboardLog.getInstance().isTransferFinished());
        if (DashboardLog.getInstance().isTransferFinished()) {
            DLog.log("SplashActivity onCreate() : transfer finished finishing activity ");
            finish();
            return;
        }
        /*if (!isTaskRoot()) {
            DLog.log("SplashActivity onCreate() !isTaskRoot finish: ");
            finish();
            return;
        }*/
        setContentView(R.layout.activity_splash);
        setTitle("");
        powered_by_logo = findViewById(R.id.powered_by_logo);
        if ("playstore".equalsIgnoreCase(BuildConfig.FLAVOR) ||
                //"tms".equalsIgnoreCase(BuildConfig.FLAVOR)       ||
                "o2".equalsIgnoreCase(BuildConfig.FLAVOR)) {
            Constants.NEW_PLAYSTORE_FLOW = true; //set true for New Playstore Flow
        }
        if ("tms".equalsIgnoreCase(BuildConfig.FLAVOR))
            powered_by_logo.setVisibility(View.VISIBLE);
        else
            powered_by_logo.setVisibility(View.GONE);
        loadProductConfig();
        DeviceInfo.getInstance().logDeviceDetails();
        changeBand(0);
        startOver = getIntent().getBooleanExtra(Constants.START_OVER, false);
        if (Build.VERSION.SDK_INT >= 23) {
            selectedDontAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
        }
        DLog.log("selectedDontAskAgain: " + selectedDontAskAgain);
        action_spinner = findViewById(R.id.action_spinner);
        action_spinner.setVisibility(View.GONE);
        TextView appVersionTextView = (TextView) findViewById(R.id.app_version);
        appVersionTextView.setText(BuildConfig.VERSION_NAME);
        letsgetStartedTextView = (Button) findViewById(R.id.letsgetstarted);
        letsgetStartedTextView.setVisibility(View.GONE);
        letsgetStartedTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable, 100);
            }
        });
        networkStatesHandlr = new NetworkStatesHandlr();
        afHotspot = new AFHotspot();
        afWiFi = new AFWiFi();
        afgps = new AFGPS();
        PreferenceHelper.getInstance(this).putBooleanItem(Constants.PREF_FINISH_CLICKED, false);
        PreferenceHelper.getInstance(this).putBooleanItem(Constants.PREF_UPLOAD_FINISHED, false);
        UninstallBroadcastReceiver.deleteUninstallAlarm(this);

        //To launch activity even when the lock screen is on/turn on screen/keep screen turn on.
        Window window = this.getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (IS_MMDS) {
            String languageToSet = EMUtility.readPropertyFile();
            DLog.log("Got language to set : " + languageToSet);
            if (!TextUtils.isEmpty(languageToSet)) {
                PreferenceHelper.getInstance(getApplicationContext()).putStringItem(Constants.LOCALE, languageToSet);
            }
        }
        if (IS_MMDS || startOver) {
            startActivity(new Intent(this, EasyMigrateActivity.class));
            finish();
        } else {
            //enable bluetooth if it is disabled
//            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
//                BluetoothAdapter.getDefaultAdapter().enable();
//            }
//            if (ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
//            {
//                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R)
//                {
//                    ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
//                    return;
//                }
//            }
            //intent to enable location service
            if (!afgps.getState()) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
            if (afHotspot.getState()) {
                networkStatesHandlr.sendEmptyMessage(MSG_TURNOFF_HOTSPOT);
            }
            else if (!afWiFi.getState()) {
                networkStatesHandlr.sendEmptyMessage(MSG_TURNON_WIFI);
//            }else if(NetworkUtil.isOnline() ) {
//               if(Constants.NEW_PLAYSTORE_FLOW) {
//                   letsgetStartedTextView.setVisibility(View.VISIBLE);
//               }
//               else
//                   handler.postDelayed(runnableConfigCall, 3000);
//            }else{
//                displayNetworkDialog(getString(R.string.no_network),getString(R.string.no_internet_message));
            }
        }
        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!afHotspot.getState() && afWiFi.getState() && afgps.getState()) {
                    handler.postDelayed(runnable, 3000);
                }
                locationPermissionRequired();
            }
        };

        registerReceiver(br, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        DLog.log("SplashActivity onCreate() END: ");
    }

    /*    Runnable runnableStorePin = new Runnable() {
            @Override
            public void run() {
                displayStoreIdDialogue(getString(R.string.enter_store_id), null);
            }
        };*/
    Runnable runnableConfigCall = new Runnable() {
        @Override
        public void run() {
            if (Constants.NEW_PLAYSTORE_FLOW) {
                //displayStoreIdDialogue(getString(R.string.enter_your_id), null);
                action_spinner.setVisibility(View.VISIBLE);
                storeID = "wdsdemo1";
                startStoreConfigServiceCall(storeID);
            } else {
                action_spinner.setVisibility(View.VISIBLE);
                startConfigServiceCall();
            }
        }
    };

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            DLog.log("Enter run and checking locationPermissionRequired()");
//            if (!locationPermissionRequired() ||
//                ("bell".equalsIgnoreCase(BuildConfig.FLAVOR)) ||
//                ("tms".equalsIgnoreCase(BuildConfig.FLAVOR))) {
            proceedToNext();
//            }
        }
    };

    public void locationAlertResultCallback() {
        handler.postDelayed(runnable, 100);
    }

    private void proceedToNext() {
//        DLog.log("SplashActivity proceedToNext()");
//        if (!isFinishing()) {
        Intent intent;
//            if (startOver || (BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT) && PreferenceHelper.getInstance(this).getBooleanItem(Constants.TERMS_AGREED))) {
//                DLog.log("calling EasyMigrateActivity 1");
//                intent = new Intent(SplashActivity.this, EasyMigrateActivity.class);
//                startActivity(intent);
//            } else {
//                if(Constants.NEW_PLAYSTORE_FLOW) {
//                    if("TelefonicaO2UK".equalsIgnoreCase(Constants.COMPANY_NAME)) {
//                        if(CommonUtil.getInstance().isTermsAndConditionsAccepted(SplashActivity.this)) {
//                            if(hasImei()) {
//                                DLog.log("hasIMEI calling EasyMigrateActivity 2");
//                                intent = new Intent(SplashActivity.this, EasyMigrateActivity.class);
//                                startActivity(intent);
//                            }
//                        }
//                        else {
//                            intent = new Intent(SplashActivity.this, TFTermsAndConditionsActivity.class);
//                            startActivity(intent);
//                        }
//                    }
//                    else {
//                        intent = new Intent(SplashActivity.this, PermissionsPolicy.class);
//                        startActivity(intent);
//                    }
//                    SplashActivity.this.finish();
//                } else if (FeatureConfig.getInstance().getProductConfig().isShowPermissionsInstructions()) {
//                        if (PreferenceHelper.getInstance(this).getBooleanItem(Constants.TERMS_AGREED)) {
//                            if (FeatureConfig.getInstance().getProductConfig().isStoreIdValidationEnabled() ) {
//                                intent = new Intent(SplashActivity.this, StoreValidationActivity.class);
//                                startActivity(intent);
//                            } else {
//                                if(hasImei()) {
//                                    DLog.log("hasIMEI calling EasyMigrateActivity 3");
//                                    intent = new Intent(SplashActivity.this, EasyMigrateActivity.class);
//                                    startActivity(intent);
//                                }
//                            }
//                        } else {
        PreferenceHelper.getInstance(SplashActivity.this).putBooleanItem(Constants.TERMS_AGREED, true);
        intent = new Intent(SplashActivity.this, EasyMigrateActivity.class);
        startActivity(intent);
//                        }
//                    SplashActivity.this.finish();
//                } else {
//                    if (FeatureConfig.getInstance().getProductConfig().isStoreIdValidationEnabled()) {
//                        intent = new Intent(SplashActivity.this, StoreValidationActivity.class);
//                        startActivity(intent);
//                    } else {
//                        if(hasImei()) {
//                            DLog.log("hasIMEI calling EasyMigrateActivity 4");
//                            intent = new Intent(SplashActivity.this, EasyMigrateActivity.class);
//                            startActivity(intent);
//                        }
//                    }
        SplashActivity.this.finish();
//                }
//            }
//            startActivity(intent);
        //finish();
//        }
    }

    private boolean locationPermissionRequired() {
        boolean required = false;
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                required = true;
                DLog.log("@ selectedDontAskAgain: " + selectedDontAskAgain);
                if (selectedDontAskAgain)
                    displayNetworkDialog(getString(R.string.str_alert), getString(R.string.location_permission_denied_msg), true);
                else {
                    DLog.log("enter locationPermissionRequired requestPermissions ACCESS_FINE_LOCATION");
                    ActivityCompat.requestPermissions(SplashActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            0);
                }
            }
            else {
                LocationManager locManager_ = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                DLog.log("locManager.isProviderEnabled(gps) ---  " + locManager_.isProviderEnabled("gps"));
                if ("huawei".equalsIgnoreCase(DeviceInfo.getInstance().get_make()) && !isGoogleServicesAvailable() && !locManager_.isProviderEnabled("gps")) {
                    DLog.log("calling showLocationPopup ----- ");
                    LocationManager locManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                    showLocationPopup();
                    required = true;
                } else {
                    LocationManager locManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                    if (locManager.isProviderEnabled("gps")) {
                        DLog.log("enter locationPermissionRequired isProviderEnabled case 2 required " + required);
                        required = false;
                    } else {
                        required = true;
                        if (!locationRejected)
                            new startLocationAlert(SplashActivity.this);
                        else
                            showMustEnableLocationPopup();
                    }
                }
            }
        }
        return required;
    }

    private void loadProductConfig() {
        try {
            String product = "WDS";
            String server_url = "";
            String mode = "WLAN";
            boolean authentication_required = false;
            boolean transaction_logging = false;
            String companyName = "Home";
            boolean certification_required = false;
            boolean estimation_required = false;
            boolean cloud_pairing = false;
            boolean dynamic_estimation = false;
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            InputStream is = getApplicationContext().getAssets().open(Constants.productConfig);
            xpp.setInput(is, null);
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equalsIgnoreCase("product")) {
                        product = xpp.nextText();
                    } else if (xpp.getName().equalsIgnoreCase("transfer_mode")) {
                        mode = xpp.nextText();
                    } else if (xpp.getName().equalsIgnoreCase("authentication_required")) {
                        authentication_required = Boolean.parseBoolean(xpp.nextText());
                    } else if (xpp.getName().equalsIgnoreCase("server_url")) {
                        server_url = xpp.nextText();
                    } else if (xpp.getName().equalsIgnoreCase("transaction_logging")) {
                        transaction_logging = Boolean.parseBoolean(xpp.nextText());
                    } else if (xpp.getName().equalsIgnoreCase("customer")) {
                        companyName = xpp.nextText();
                    } else if (xpp.getName().equalsIgnoreCase("certification_required")) {
                        certification_required = Boolean.parseBoolean(xpp.nextText());
                    } else if (xpp.getName().equalsIgnoreCase("estimation_required")) {
                        estimation_required = Boolean.parseBoolean(xpp.nextText());
                    } else if (xpp.getName().equalsIgnoreCase("cloud_pairing")) {
                        cloud_pairing = Boolean.parseBoolean(xpp.nextText());
                    } else if (xpp.getName().equalsIgnoreCase("dynamic_estimation")) {
                        dynamic_estimation = Boolean.parseBoolean(xpp.nextText());
                    }

                }
                eventType = xpp.next(); // move to next element
            }

            DLog.log("Application Version : " + BuildConfig.VERSION_NAME);
            DLog.log("Application Release date : " + BuildConfig.BUILD_DATE);
            DLog.log("Debug Application : " + BuildConfig.DEBUG);
            DLog.log("Product : " + product);
            DLog.log("Server_url : " + server_url);
            DLog.log("Transfer mode : " + mode);
            DLog.log("Authentication Enabled : " + authentication_required);
            DLog.log("Transaction logging Enabled: " + transaction_logging);
            DLog.log("Customer : " + companyName);
            DLog.log("Certification Enabled : " + certification_required);
            DLog.log("Estimationtime Enabled : " + estimation_required);
            DLog.log("Cloud pairing Enabled : " + cloud_pairing);
            DLog.log("Dynamic estimation Enabled : " + dynamic_estimation);
            if (product != null && !product.isEmpty()) {
                /*if (!product.equalsIgnoreCase("WDS")) {
                    IS_MMDS = true;
                }*/
            }
            if (mode != null && !mode.isEmpty()) {
                Constants.mTransferMode = mode;
            }
            FeatureConfig.getInstance().getProductConfig().setTransactionLoggingEnabled(transaction_logging);
            FeatureConfig.getInstance().getProductConfig().setGeoFencingEnabled(authentication_required);
            FeatureConfig.getInstance().getProductConfig().setStoreIdValidationEnabled(authentication_required);
            FeatureConfig.getInstance().getProductConfig().setCertificationCheck(certification_required);
            Constants.COMPANY_NAME = companyName;
            FeatureConfig.getInstance().getProductConfig().setCompanyName(companyName);
            FeatureConfig.getInstance().getProductConfig().setEstimationtimeRequired(estimation_required);
            FeatureConfig.getInstance().getProductConfig().setCloudpairingRequired(cloud_pairing);
            Constants.REESTIMATION_REQUIRED = dynamic_estimation;
            if (server_url != null && !server_url.isEmpty()) {
                Constants.setServerAddress(server_url);
            }
            PreferenceManager.getDefaultSharedPreferences(SplashActivity.this).edit().putString(LogReporting.COMPANY_NAME, companyName).apply();
            PreferenceManager.getDefaultSharedPreferences(SplashActivity.this).edit().putString(LogReporting.END_POINT, Constants.LOG_UPLOAD_URL).apply();
        } catch (Exception e) {
            DLog.log("Config Exception : " + Log.getStackTraceString(e));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        DLog.log("on acitivty result " + requestCode + " " + resultCode);
        if (!isFinishing() && requestCode == startLocationAlert.REQUEST_CHECK_SETTINGS && resultCode == -1) {
            handler.postDelayed(runnable, 100);
        } else if (!isFinishing() && requestCode == startLocationAlert.REQUEST_CHECK_SETTINGS && resultCode == 0) {
            locationRejected = true;
        }
        switch (requestCode) {
//            case TermsAndConditionsActivity.RC_TERMS_CONDITIONS:
            case IMEIReadActivity.RC_IMEI_READ:
                if (resultCode == Activity.RESULT_OK) {
//                    decideAppFlow();
                    proceedToNext();
                    return;
                }/*else if(resultCode == Activity.RESULT_CANCELED){
                    BaseUtils.triggerUninstall(this,true,BuildConfig.APPLICATION_ID);
                }*/
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DLog.log("enter onResume SpalshActivity");
        DLog.log("isHotSpotSettingsCalled " + isHotSpotSettingsCalled);
        DLog.log("isWiFiSettingsCalled " + isWiFiSettingsCalled);
        if (isHotSpotSettingsCalled) {
            isHotSpotSettingsCalled = false;
            boolean status = afHotspot.getState();
            DLog.log("Hotspot Status " + status);
            if (status) {
                networkStatesHandlr.sendEmptyMessage(MSG_TURNOFF_HOTSPOT);
            } else {
                networkStatesHandlr.sendEmptyMessage(MSG_TURNON_WIFI);
            }
        } else if (isWiFiSettingsCalled) {
            isWiFiSettingsCalled = false;
            boolean status = afWiFi.getState();
            DLog.log("Wifi Status " + status);
            if (!status) {
                DLog.log("WiFi is Off");
                networkStatesHandlr.sendEmptyMessage(MSG_TURNON_WIFI);
            } else {
                DLog.log("WiFi is ON");
//                if(NetworkUtil.isOnline() ) {
//                    handler.postDelayed(runnableConfigCall, 1000);
//                }else{
//                    displayNetworkDialog(getString(R.string.no_network),getString(R.string.no_internet_message));
//                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        DLog.log("$ selectedDontAskAgain: " + selectedDontAskAgain +"-------grantResults[0]= "+grantResults[0]);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            DLog.log("calling runnable from onRequestPermissionsResult");
            handler.postDelayed(runnable, 100);
            if ("huawei".equalsIgnoreCase(DeviceInfo.getInstance().get_make()) && !isGoogleServicesAvailable()) {
                DLog.log("calling showLocationPopup");
                LocationManager locManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                DLog.log("locManager.isProviderEnabled(gps)  ++++  " + locManager.isProviderEnabled("gps"));
                showLocationPopup();
            }
        } else {
            if (Build.VERSION.SDK_INT >= 23) {
                DLog.log("calling shouldShowRequestPermissionRationale from onRequestPermissionsResult");
                selectedDontAskAgain = !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
                DLog.log("called shouldShowRequestPermissionRationale from onRequestPermissionsResult selectedDontAskAgain " + selectedDontAskAgain);
            }
            if (selectedDontAskAgain)
                displayNetworkDialog(getString(R.string.str_alert), getString(R.string.location_permission_denied_msg), true);
            else
                displayNetworkDialog(getString(R.string.str_alert), getString(R.string.location_permission_required_msg), true);
        }
    }

    boolean locationRejected = false;

    public void showMustEnableLocationPopup() {
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(SplashActivity.this);
            alert.setMessage(getText(R.string.location_must_turn_on_msg));
            String positiveString = getString(R.string.ept_ok);
            alert.setPositiveButton(positiveString, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (dialog != null) {
                        dialog.cancel();
                    }
                    new startLocationAlert(SplashActivity.this); //Display alert to enable GPS.
                }
            });
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        String storedLocale = PreferenceHelper.getInstance(newBase).getStringItem(Constants.LOCALE);
        DLog.log("Read locale: " + storedLocale);
        super.attachBaseContext(ContextWrapperHelper.wrap(newBase, storedLocale));
    }

    private void changeBand(int band) {
        try {
            DeviceInfo deviceInfo = DeviceInfo.getInstance();
            if (deviceInfo.is5GhzSupported() && (deviceInfo.getWifiFrequencyBand() == 2)) {
                deviceInfo.setWifiFrequencyBand(band);
            }
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    public void displayNetworkDialog(String title, String message) {
        displayNetworkDialog(title, message, false);
    }

    public void displayNetworkDialog(String title, String message, final boolean fromPermissions) {
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(SplashActivity.this);
            alert.setTitle(title);
            alert.setMessage(message);
            String positiveString = getString(R.string.wds_try_again);
            if (fromPermissions)
                positiveString = getString(R.string.ept_ok);
            if (!(fromPermissions && selectedDontAskAgain)) {
                alert.setPositiveButton(positiveString, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (NetworkUtil.isOnline() && dialog != null) {
                            dialog.cancel();
                            if (fromPermissions) {
                                handler.postDelayed(runnable, 100);
                            } else {
                                if (Constants.NEW_PLAYSTORE_FLOW) {
                                    handler.postDelayed(runnable, 100);
/*                                    if (isStoreConfigSuccess)
                                        startConfigServiceCall();
                                    else
                                        displayStoreIdDialogue(getString(R.string.enter_store_id), null);*/
                                } else
                                    startConfigServiceCall();
                            }
                        } else {
                            displayNetworkDialog(getString(R.string.no_network), getString(R.string.no_internet_message));
                        }
                    }
                });
            }
            alert.setNegativeButton(getString(R.string.ept_exit_button), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
//                    System.exit(0);
                    finish();
                }
            });
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    public void startStoreConfigServiceCall(final String storeID) {
        DLog.log("Store Config Calling Api Service: ");
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<JsonObject> call = apiService.getStoreConfig(EMUtility.getAuthToken(), storeID, "wds");
        DLog.log("Store Config Calling Api Service call enqueue: ");
        try {
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    try {
                        DLog.log("Requested URL: " + response.raw().request().url());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        if (response.body() != null) {
                            JsonObject responseJson = response.body();
                            DLog.log("Store Config Details: " + responseJson.toString());
                            if (responseJson.has("status") && "PASS".equalsIgnoreCase(responseJson.get("status").getAsString())) {
                                DLog.log("status: " + responseJson.get("status").getAsString());
                                if (responseJson.has("resultData")) {
                                    DLog.log("Response Json has resultData..........");
                                    JsonObject storeConfig = responseJson.get("resultData").getAsJsonObject();
                                    if (storeConfig.has("storeCode")) {
                                        DLog.log("Response Json has storeCode..........");
                                        String store_id = storeConfig.get("storeCode").getAsString();
                                        DLog.log("From Response Json got storeCode.........." + store_id);
                                        if (store_id == null || "".equalsIgnoreCase(store_id)) {
                                            store_id = storeID;
                                            DLog.log("Taken entred storeID as storeCode.........." + store_id);
                                        }
                                        PreferenceHelper.getInstance(SplashActivity.this).putStringItem(Constants.STORE_ID, store_id);
                                        DashboardLog.getInstance().geteDeviceSwitchSession().setStoreId(store_id);
                                        PreferenceHelper.getInstance(SplashActivity.this).putStringItem("userID", store_id);
                                        DashboardLog.getInstance().geteDeviceSwitchSession().setUserId(store_id); // Satya add
                                    } else {
                                        DLog.log("Taken entred storeID as storeCode.........." + storeID);
                                        PreferenceHelper.getInstance(SplashActivity.this).putStringItem(Constants.STORE_ID, storeID);
                                        PreferenceHelper.getInstance(SplashActivity.this).putStringItem("userID", storeID);
                                        DashboardLog.getInstance().geteDeviceSwitchSession().setStoreId(storeID);
                                    }
                                    if (storeConfig.has("companyName")) {
                                        DLog.log("Response Json has companyName..........");
                                        String companyName = storeConfig.get("companyName").getAsString();
                                        DLog.log("From Response Json got companyName.........." + companyName);
                                        if (companyName != null && !"".equalsIgnoreCase(companyName)) {
                                            Constants.COMPANY_NAME = storeConfig.get("companyName").getAsString();
                                            PreferenceManager.getDefaultSharedPreferences(SplashActivity.this).edit().putString(LogReporting.COMPANY_NAME, Constants.COMPANY_NAME).apply();
                                            if (companyName.equalsIgnoreCase("TelefonicaBrazil")) {
                                                DashboardLog.getInstance().geteDeviceSwitchSession().setUserId(DashboardLog.getInstance().geteDeviceSwitchSession().getStoreId()); // Satya add
                                            }
                                        }
                                    }
                                    if (storeConfig.has("companyId")) {
                                        String companyId = storeConfig.get("companyId").getAsString();
                                        if (companyId != null && !"".equalsIgnoreCase(companyId)) {
                                            DashboardLog.getInstance().geteDeviceSwitchSession().setCompanyId(companyId);
                                        }
                                    }
                                    if (storeConfig.has("password")) {
                                        String password = storeConfig.get("password").getAsString();
                                        if (password != null && !"".equalsIgnoreCase(password)) {
                                            DLog.log("Store Config password: " + password);
                                            EMUtility.getAuthToken();
                                            EMUtility.setAuthToken(password);
                                            EMUtility.getAuthToken();
                                        }
                                    }
                                    if (storeConfig.has("serverUrl")) {
                                        DLog.log("Response Json has serverUrl..........");
                                        String server_url = storeConfig.get("serverUrl").getAsString();
                                        DLog.log("Store Config server_url: " + server_url);
                                        if (server_url != null && !server_url.isEmpty()) {
                                            isStoreConfigSuccess = true;
                                            Constants.setServerAddress(server_url);
                                            PreferenceManager.getDefaultSharedPreferences(SplashActivity.this).edit().putString(LogReporting.END_POINT, Constants.LOG_UPLOAD_URL).apply();
                                            startConfigServiceCall();
                                        }
                                    }
                                    if (GET_PRELOADED_APPS_NAMES_FROM_SERVER) {
                                        if (storeConfig.has("country")) {
                                            DLog.log("Response Json has country..........");
                                            String country = storeConfig.get("country").getAsString();
                                            DLog.log("Store Config country: " + country);

                                            if (country != null && !country.isEmpty()) {
                                                Constants.COUNTRY_NAME = storeConfig.get("country").getAsString();
//                                                COUNTRY_NAME = "Uruguay";
                                            }
                                        } else {
                                            DLog.log("Response Json do not has country name ----- ");
                                        }
                                    }

                                }
                            } else {
                                displayStoreIdDialogue(getString(R.string.enter_your_id), getString(R.string.invalid_id));
                            }
                        } else {
                            displayStoreIdDialogue(getString(R.string.enter_your_id), getString(R.string.invalid_id));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    DLog.log("Config Details error : " + t.getMessage());
                    displayStoreIdDialogue(getString(R.string.enter_your_id), getString(R.string.ept_timeout));
                }
            });
        } catch (Exception e) {
            DLog.log("Store Config Details error : " + e.getMessage());
            displayStoreIdDialogue(getString(R.string.enter_your_id), getString(R.string.ept_timeout));
        }
    }

    public void displayStoreIdDialogue(String title, String failReason) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.storeid_dialogue);
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;

        final Button BL_alert_submit = (Button) dialog.findViewById(R.id.BL_alert_submit);
        Button BL_alert_cancel = (Button) dialog.findViewById(R.id.BL_alert_cancel);
        TextView textTitle = (TextView) dialog.findViewById(R.id.textTitle);
        textTitle.setText(title);
        final EditText ET_storeid = (EditText) dialog.findViewById(R.id.storeid);
        TextView failreasonTV = (TextView) dialog.findViewById(R.id.reason);
        if (failReason == null || "".equalsIgnoreCase(failReason)) {
            failreasonTV.setVisibility(View.GONE);
        } else {
            failreasonTV.setVisibility(View.VISIBLE);
            failreasonTV.setText(failReason);
        }
        action_spinner.setVisibility(View.GONE);
        ET_storeid.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    BL_alert_submit.callOnClick();
                }
                return false;
            }
        });

        BL_alert_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storeID = ET_storeid.getText().toString().trim();
                if (dialog != null)
                    dialog.dismiss();
                action_spinner.setVisibility(View.VISIBLE);
                startStoreConfigServiceCall(storeID);
/*                Intent resultIntent = new Intent();
                resultIntent.putExtra("pin",mPin);
                setResult(2002, resultIntent);
                finish();*/
            }
        });
        BL_alert_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null)
                    dialog.dismiss();
//                System.exit(0);
                finish();
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    public void startConfigServiceCall() {
        DLog.log("Config Calling Api Service: ");
        ApiInterface apiService =
                ApiClient.getClient().create(ApiInterface.class);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("make", DeviceInfo.getInstance().get_make());
        jsonObject.addProperty("model", DeviceInfo.getInstance().get_model());
        jsonObject.addProperty("company", Constants.COMPANY_NAME.toLowerCase());
        jsonObject.addProperty("mergeCompanyConfigData", true);
        jsonObject.addProperty("platform", Constants.PLATFORM);
        jsonObject.addProperty("product", "wds");

        Call<FeatureConfig> call = apiService.getConfigDetails(EMUtility.getAuthToken(), jsonObject);
        DLog.log("Config Calling Api Service:  with data : " + jsonObject.toString());

        call.enqueue(new Callback<FeatureConfig>() {
            @Override
            public void onResponse(Call<FeatureConfig> call, Response<FeatureConfig> response) {
                DLog.log("Config Calling Api Service: Requested URL: " + response.raw().request().url());
                if (response.body() != null) {
                    DLog.log("Config Details: " + new Gson().toJson(response.body(), FeatureConfig.class));
                    FeatureConfig featureConfig = response.body();
                    if ("privatelabel".equalsIgnoreCase(BuildConfig.FLAVOR) || Constants.NEW_PLAYSTORE_FLOW)
                        featureConfig.getProductConfig().setShowPermissionsInstructions(true);
                    FeatureConfig.setInstance(featureConfig);
                    CLOUD_PAIRING_ENABLED = FeatureConfig.getInstance().getProductConfig().isCloudpairingRequired();
                    Constants.LOGGING_ENABLED = FeatureConfig.getInstance().getProductConfig().isTransactionLoggingEnabled();
                    EMUtility.updateSupportingDataTypes();
                    if (GET_PRELOADED_APPS_NAMES_FROM_SERVER) {
                        String preLoadedApps = FeatureConfig.getInstance().getProductConfig().getPreloadedApps();
                        DLog.log(" Config Details: preloadedApps data " + preLoadedApps);
                        String preLoadedAppNamesS3 = FeatureConfig.getInstance().getProductConfig().getPreloadedAppNamesS3();
                        DLog.log(" Config Details: preLoadedAppNamesS3 data " + preLoadedAppNamesS3);
                        udpdatePreLoadedAppsDetailsFromServre(preLoadedApps, preLoadedAppNamesS3);
                    }
                    handler.postDelayed(runnable, 1000);
                    action_spinner.setVisibility(View.GONE);
                    letsgetStartedTextView.setVisibility(View.VISIBLE);
                } else {
                    displayNetworkDialog(getString(R.string.ept_timeout), getString(R.string.ept_server_response));
                }
            }

            @Override
            public void onFailure(Call<FeatureConfig> call, Throwable t) {
                DLog.log("Config Details error : " + t.getMessage());
                displayNetworkDialog(getString(R.string.ept_timeout), getString(R.string.ept_server_response));
            }
        });
    }

    private boolean hasImei() {
        DLog.log("Enter hasIMEI ");
        //For Android 9 and lower, IMEI is automatically obtained
        if ((android.os.Build.VERSION.SDK_INT < Constants.IMEI_CHECK_MIN_APILEVEL))
            return true;

        //Check for allowed features based on customer name
//        if(AppUtils.PermissionsFlow.isAllowed(GlobalConfig.getInstance().getCompanyName(), AppUtils.PermissionsFlow.Features.IMEI_READ)){
//        if (BuildConfig.FLAVOR.equalsIgnoreCase(FLAVOUR_PLAYSTORE)){
        if (BuildConfig.FLAVOR.equalsIgnoreCase(FLAVOUR_PLAYSTORE) || BuildConfig.FLAVOR.equalsIgnoreCase(FLAVOUR_TMS)) {
            if (!CommonUtil.retrivedIMEIForAndroid10()) {
                DLog.log("Case of !CommonUtil.retrivedIMEIForAndroid10()");
                IMEIReadChildActivity.startActivity(this);
                return false;
            }
        } else {
            DLog.log("getting getMACAddressInIMEIFormat");
            String imei = CommonUtil.getMACAddressInIMEIFormat(this);
            DLog.log("getMACAddressInIMEIFormat " + imei);
            CommonUtil.saveImeiInPrefs(imei);
//            com.pervacio.batterydiaglib.util.LogUtil.debug( "hasImei(): MAC :"+imei);
        }

        return true;
    }

    private void udpdatePreLoadedAppsDetailsFromServre(String preLoadedApps, String preLoadedAppNamesS3) {
        if (preLoadedApps != null) {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(preLoadedApps);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (jsonObject.has(Constants.COUNTRY_NAME)) {
                try {
                    DLog.log("Constants.COUNTRY_NAME : " + Constants.COUNTRY_NAME + " , " + "App List :" + jsonObject.getString(Constants.COUNTRY_NAME));
                    String[] appList = jsonObject.getString(Constants.COUNTRY_NAME).split(",");
                    Collections.addAll(preLoadedAppsListServer, appList);
                    Constants.movistarappsList = preLoadedAppsListServer;
                    DLog.log("Constants.COUNTRY_NAME : " + Constants.COUNTRY_NAME + " , " + "App List size:" + Constants.movistarappsList.size());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            DLog.log("Pre Loaded Apps Data not present in server");
        }
        if (preLoadedAppNamesS3 != null) {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(preLoadedAppNamesS3);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (jsonObject.has(Constants.COUNTRY_NAME)) {
                try {
                    DLog.log("Constants.COUNTRY_NAME : " + Constants.COUNTRY_NAME + " , " + "App Names S3 List :" + jsonObject.getString(Constants.COUNTRY_NAME));
                    String[] appList = jsonObject.getString(Constants.COUNTRY_NAME).split(",");
                    Collections.addAll(preLoadedAppNamesS3ListServer, appList);
                    Constants.movistarappsListS3 = preLoadedAppNamesS3ListServer;
                    DLog.log("New AppsListS3 Constants.COUNTRY_NAME : " + Constants.COUNTRY_NAME + " , " + "App Names S3 List size :" + Constants.movistarappsListS3.size());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        } else {
            DLog.log("Pre Loaded Apps Names S3 Data not present in server");
        }

    }

    private boolean isGoogleServicesAvailable() {
        int checkGooglePlayServices = GooglePlayServicesUtil.isGooglePlayServicesAvailable(SplashActivity.this);
        if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
            /*
             * Google Play Services is missing or update is required
             *  return code could be
             * SUCCESS,
             * SERVICE_MISSING, SERVICE_VERSION_UPDATE_REQUIRED,
             * SERVICE_DISABLED, SERVICE_INVALID.
             */
/*            GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices,
                    mContext, REQUEST_CODE_RECOVER_PLAY_SERVICES).show();*/

            return false;
        }

        return true;
    }

    public class GpsChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DLog.log("In GpsChangeReceiver ........++ ");
            if (gpsChangeReceiver != null)
                getApplicationContext().unregisterReceiver(gpsChangeReceiver);
            //Intent newIntent = new Intent(getApplicationContext(), StoreValidationActivity.class);
            locationRequested = true;
            //startActivity(getIntent());
            /*(new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(getIntent());
                }
            }, 1000);*/
            handler.postDelayed(runnable, 100);
            DLog.log("Out GpsChangeReceiver ........++ ");
        }
    }

    public void showLocationPopup() {
        try {
            androidx.appcompat.app.AlertDialog.Builder alert = new androidx.appcompat.app.AlertDialog.Builder(SplashActivity.this);
            alert.setMessage(getText(R.string.location_turn_on_msg));
            String positiveString = getString(R.string.ept_ok);
            alert.setPositiveButton(positiveString, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (dialog != null) {
                        dialog.cancel();
                    }
                    gpsChangeReceiver = new GpsChangeReceiver();
                    getApplicationContext().registerReceiver(gpsChangeReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
                    locationRequested = true;
                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            alert.setCancelable(false);
            androidx.appcompat.app.AlertDialog alertDialog = alert.create();
            alertDialog.show();
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    public void displayNetworkSettingsDialog(String title, String message, final int id) {
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(SplashActivity.this);
            alert.setTitle(title);
            alert.setMessage(message);
            String positiveString = getString(R.string.ept_ok);
            alert.setPositiveButton(positiveString, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (dialog != null) {
                        dialog.cancel();
                        switch (id) {
                            case MSG_TURNOFF_HOTSPOT:
                                DLog.log("Case MSG_TURNOFF_HOTSPOT alert dialog");
                                final Intent intent = new Intent(Intent.ACTION_MAIN, null);
                                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                                final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
                                intent.setComponent(cn);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                isHotSpotSettingsCalled = true;
                                break;
                            case MSG_TURNON_WIFI:
                                DLog.log("Case MSG_TURNON_WIFI alert dialog");
                                final Intent wifisettingsIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                wifisettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(wifisettingsIntent);
                                isWiFiSettingsCalled = true;
                                break;
                            default:
                                break;
                        }
                    }
                }
            });
//            }
            alert.setNegativeButton(getString(R.string.ept_exit_button), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
//                    System.exit(0);
                    finish();
                }
            });
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    class NetworkStatesHandlr extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_TURNOFF_HOTSPOT:
//                    displayNetworkSettingsDialog("Turn Off HotSpot", "Please click on OK to turn off hotspot",MSG_TURNOFF_HOTSPOT);
                    displayNetworkSettingsDialog(getString(R.string.turn_off_hotspot_title), getString(R.string.turn_off_hotspot_message), MSG_TURNOFF_HOTSPOT);
                    break;
                case MSG_TURNON_WIFI:
//                    displayNetworkSettingsDialog("Turn On WiFi", "Please click on OK to turn on WiFi",MSG_TURNON_WIFI);
                    displayNetworkSettingsDialog(getString(R.string.turn_on_wifi_title), getString(R.string.turn_on_wifi_message), MSG_TURNON_WIFI);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DLog.log("Splash Activit onDestroy");
    }
}
