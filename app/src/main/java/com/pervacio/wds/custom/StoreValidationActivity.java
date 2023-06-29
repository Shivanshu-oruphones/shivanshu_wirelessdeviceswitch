package com.pervacio.wds.custom;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pervacio.crashreportlib.LogReporting;
import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.ui.EasyMigrateActivity;
import com.pervacio.wds.custom.asynctask.URLConnectionTask;
import com.pervacio.wds.custom.models.EDeviceLoggingInfo;
import com.pervacio.wds.custom.models.FeatureConfig;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.ContextWrapperHelper;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.DeviceInfo;
import com.pervacio.wds.custom.utils.NetworkUtil;
import com.pervacio.wds.custom.utils.PreferenceHelper;
import com.pervacio.wds.custom.utils.ServerCallBacks;
import com.pervacio.wds.custom.utils.startLocationAlert;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import static com.pervacio.wds.custom.utils.Constants.FLAVOUR_PLAYSTORE;
import static com.pervacio.wds.custom.utils.Constants.FLAVOUR_TMS;


/**
 * Created by Surya Polasanapalli on 25-05-2017.
 */

public class StoreValidationActivity extends AppCompatActivity implements supportDeviceCheck, ServerCallBacks {
    static EMGlobals emGlobals = new EMGlobals();
    PreferenceHelper preferenceHelper;
    private ProgressDialog mProgressDialog;
    private EDeviceLoggingInfo eDeviceLoggingInfo;
    private LocationManager locManager;
    private GpsListner gpsListner;
    static boolean locationRequested = false;
    private boolean mAuthorizedStore = false;
    private Context mContext;
    private TextView termsConditionsTv;
    private boolean isLocationValidationSuccess=false;
    private boolean isStoreValidation;
    GpsChangeReceiver gpsChangeReceiver;

    private boolean proceedOnValidation = false;
    private String storeID = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        proceedOnValidation = BuildConfig.FLAVOR.equalsIgnoreCase("privatelabel") || Constants.NEW_PLAYSTORE_FLOW;
        if("tms".equalsIgnoreCase(BuildConfig.FLAVOR))
        proceedOnValidation = false;
        //Stop the un-installation alarm whenever the app is launched and running.
        preferenceHelper = PreferenceHelper.getInstance(StoreValidationActivity.this);
        storeID = preferenceHelper.getStringItem(Constants.STORE_ID);
        if(getIntent().getBooleanExtra("isPrivacyPolicyScreen", false))
            getSupportActionBar().setTitle(getString(R.string.str_privacy_statement));
        else
            getSupportActionBar().setTitle(getString(R.string.terms_and_condition_dialog_header));
        setContentView(R.layout.activity_terms_and_conditions);

        boolean recreated = getIntent().getBooleanExtra("recreated", false);
        mAuthorizedStore = isStoreValidation = getIntent().getBooleanExtra("storevalidation", false);
        isLocationValidationSuccess = getIntent().getBooleanExtra("isLocationValidationSuccess",false);
        Button iAgree = (Button) findViewById(R.id.btn_postive);
        Button iDisagree = (Button) findViewById(R.id.btn_negative);
        termsConditionsTv = (TextView) findViewById(R.id.terms_conditions_tv);
        if(getIntent().getBooleanExtra("isPrivacyPolicyScreen", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                termsConditionsTv.setText(Html.fromHtml(getString(R.string.privacy_policy), Html.FROM_HTML_MODE_COMPACT));
                else
                termsConditionsTv.setText(Html.fromHtml(getString(R.string.privacy_policy)));
        }
        termsConditionsTv.setMovementMethod(LinkMovementMethod.getInstance());
        if(proceedOnValidation || Constants.NEW_PLAYSTORE_FLOW){
            termsConditionsTv.setText("");
            iAgree.setVisibility(View.INVISIBLE);
            iDisagree.setVisibility(View.INVISIBLE);
            getSupportActionBar().hide();
        }
        locManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if(BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT))
            isStoreValidation = true;
        if (Constants.IS_MMDS) {
            proceedToNext();
        } else if (!isStoreValidation && !recreated) {
            validateStoreLocation();
        }
        iAgree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((Build.VERSION.SDK_INT >= 23) && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(StoreValidationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    DLog.log("No Location Permission, requesting now...");
                    ActivityCompat.requestPermissions(StoreValidationActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            0);
                } else {
                    if (NetworkUtil.isOnline()) {
                        if (isStoreValidation) {
                            proceedToNext();
                        } else {
                            displayStoreIdDialogue(getString(R.string.location_validation_failed), null);
                        }
                    } else {
                        displayNetworkDialog(isStoreValidation ? 1 : 0);
                    }
                }
            }
        });

        iDisagree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                killApp();
            }
        });

        if(FeatureConfig.getInstance().getProductConfig().isShowPermissionsInstructions())
        {
            iDisagree.setVisibility(View.GONE);
            iAgree.setVisibility(View.GONE);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
            final boolean appInstallationLogged = preferenceHelper.getBooleanItem(Constants.INSTALLATION_LOGGING);

        Thread appInstallationLogging = new Thread(new Runnable() {
            @Override
            public void run() {
                logAppInstallationInfo();
            }
        });
        if (!appInstallationLogged && Constants.LOGGING_ENABLED) {
            appInstallationLogging.start();
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            EMUtility.makeActionOverflowMenuShown(this);
        }
        if (BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT)) {
            TextView read_more_tv = (TextView) findViewById(R.id.read_more_tv);
            read_more_tv.setVisibility(View.VISIBLE);
            readMoreTsAndCs(read_more_tv);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        List<String> languages = Arrays.asList(FeatureConfig.getInstance().getProductConfig().getSupportedLanguages().split(","));
        if (languages != null && languages.size() > 1) {
            getMenuInflater().inflate(R.menu.menu, menu);
            menu.findItem(R.id.select_polish).setVisible(languages.contains("pl"));
            menu.findItem(R.id.select_french).setVisible(languages.contains("fr"));
            menu.findItem(R.id.select_slovak).setVisible(languages.contains("sk"));
            menu.findItem(R.id.select_spanish).setVisible(languages.contains("es"));
            menu.findItem(R.id.select_portuguese).setVisible(languages.contains("pt")||languages.contains("pt-rBR")||languages.contains("pt-rPT"));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.select_polish) {
            PreferenceHelper.getInstance(mContext).putStringItem(Constants.LOCALE, "pl");
            changeLocale();
        } else if (id == R.id.select_english) {
            PreferenceHelper.getInstance(mContext).putStringItem(Constants.LOCALE, "en");
            changeLocale();
        } else if (id == R.id.select_french) {
            PreferenceHelper.getInstance(mContext).putStringItem(Constants.LOCALE, "fr");
            changeLocale();
        } else if (id == R.id.select_slovak) {
            PreferenceHelper.getInstance(mContext).putStringItem(Constants.LOCALE, "sk");
            changeLocale();
        }else if (id == R.id.select_spanish) {
            PreferenceHelper.getInstance(mContext).putStringItem(Constants.LOCALE, "es");
            changeLocale();
        }else if (id == R.id.select_portuguese) {
            PreferenceHelper.getInstance(mContext).putStringItem(Constants.LOCALE, "pt");
            changeLocale();
        }else if (id == android.R.id.home) {
           onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        String storedLocale = PreferenceHelper.getInstance(newBase).getStringItem(Constants.LOCALE);
        if(TextUtils.isEmpty(storedLocale)){
            storedLocale = FeatureConfig.getInstance().getProductConfig().getDefaultLanguage();
        }
        DLog.log("Read locale: " + storedLocale);
        super.attachBaseContext(ContextWrapperHelper.wrap(newBase, storedLocale));
    }

    void changeLocale() {
        Intent intent = new Intent(this, StoreValidationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("recreated", true);
        intent.putExtra("storevalidation", mAuthorizedStore);
        intent.putExtra("isLocationValidationSuccess", isLocationValidationSuccess);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
    private void killApp() {
        try {
            this.finish();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void proceedToNext() {
        if (NetworkUtil.isOnline() || Constants.IS_MMDS) {
            PreferenceHelper.getInstance(this).putBooleanItem(Constants.TERMS_AGREED, true);
            String company = PreferenceManager.getDefaultSharedPreferences(emGlobals.getmContext()).getString(LogReporting.COMPANY_NAME, Constants.COMPANY_NAME);
            if (FeatureConfig.getInstance().getProductConfig().isUserIdValidationEnabled()&& (!company.equalsIgnoreCase("TelefonicaBrazil"))) {
                startActivity(new Intent(StoreValidationActivity.this,LoginActivity.class));
            } else {
                if(hasImei()) {
                    startActivity(new Intent(StoreValidationActivity.this, EasyMigrateActivity.class));
                }
            }
            finish();
        } else {
            displayNetworkDialog(1);
        }
    }

    private void logAppInstallationInfo() {
        startServerCall(Constants.INSTALLATION_LOGGING,Constants.INSTALLATION_LOGGING_URL,getDeviceLoggingInfo());
    }

    private void startDeviceSupportCheck() {
        if(FeatureConfig.getInstance().getProductConfig().isCertificationCheck()){
            showProgressDialog("", getString(R.string.supporting_check));
            DeviceInfo deviceInfo = DeviceInfo.getInstance();
            EDeviceLoggingInfo eDeviceLoggingInfo = new EDeviceLoggingInfo();
            eDeviceLoggingInfo.setCarrier(deviceInfo.get_carrierName());
            eDeviceLoggingInfo.setFirmware(deviceInfo.getFirmwareVersion());
            eDeviceLoggingInfo.setMake(deviceInfo.get_make());
            eDeviceLoggingInfo.setModel(deviceInfo.get_model());
            eDeviceLoggingInfo.setProduct("DeviceSwitch_Wireless");
            eDeviceLoggingInfo.setProductVersion(BuildConfig.VERSION_NAME);
            eDeviceLoggingInfo.setProductFeature("ALL");
            startServerCall(Constants.SUPPORTED_CHECK,Constants.SUPPORTED_CHECK_URL, eDeviceLoggingInfo);
        }
    }

    @Override
    public void isSupportedDevice(boolean result) {
        hideProgressDialog();
        if (!result) {
            AlertDialog.Builder alert = new AlertDialog.Builder(StoreValidationActivity.this);
            alert.setTitle(getString(R.string.wds_alert));
            alert.setMessage(getString(R.string.not_supported_device));
            alert.setPositiveButton(getString(R.string.proceed_anyway), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //according to new changes we are still allowing to migrate.
                    //killApp();
                }
            });
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        }
    }

    private void showProgressDialog(String aTitle, String aText) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(aTitle);
        mProgressDialog.setMessage(aText);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    void hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    public void displayStoreIdDialogue(String title, final String[] storeList) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.storeid_dialogue);
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        TextView failreason = (TextView) dialog.findViewById(R.id.reason);
        TextView mHeaderMessage = (TextView) dialog.findViewById(R.id.textTitle);
        TextView failReasonRep = (TextView) dialog.findViewById(R.id.reason_repid);
        LinearLayout storeIDLyt = (LinearLayout) dialog.findViewById(R.id.lyt_storeID);
        LinearLayout repIDLyt = (LinearLayout) dialog.findViewById(R.id.lyt_repID);

        //Set fail reason in Polish
        failreason.setText(title);

        final Button BL_alert_submit = (Button) dialog.findViewById(R.id.BL_alert_submit);
        Button BL_alert_cancel = (Button) dialog.findViewById(R.id.BL_alert_cancel);
        TextView textTitle = dialog.findViewById(R.id.textTitle);
        final EditText ET_storeid = (EditText) dialog.findViewById(R.id.storeid);
        final Spinner storeIdList = (Spinner) dialog.findViewById(R.id.storeidList);
        TextView label_empid = (TextView) dialog.findViewById(R.id.text_empid);;
        final EditText ET_repid = (EditText) dialog.findViewById(R.id.repid);
        if(Constants.NEW_PLAYSTORE_FLOW && !"tms".equalsIgnoreCase(BuildConfig.FLAVOR)) {
           if(storeID != null && !"".equalsIgnoreCase(storeID)){
               ET_storeid.setText(storeID);
               ET_storeid.setEnabled(false);
               textTitle.setVisibility(View.GONE);
               ET_storeid.setVisibility(View.GONE);
           }
        }
        if(Constants.NEW_PLAYSTORE_FLOW ) {
            textTitle.setText(R.string.enter_your_id);
        }
        if("OrangePoland".equalsIgnoreCase(Constants.COMPANY_NAME)) {
            label_empid.setText(R.string.str_enter_bscs_code);
            BL_alert_submit.setEnabled(false);
            BL_alert_submit.setClickable(false);
            ET_repid.setTransformationMethod(null);
            ET_repid.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }

                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {
                    DLog.log("entered onTextChanged....................."+count);
                    if (ET_repid.getText() != null && ET_repid.getText().length() == 5) {
                            BL_alert_submit.setEnabled(true);
                            BL_alert_submit.setClickable(true);
                        } else {
                            BL_alert_submit.setEnabled(false);
                            BL_alert_submit.setClickable(false);
                        }
                }
            });
        }
        if (FeatureConfig.getInstance().getProductConfig().isRepIdValidationEnabled()||FeatureConfig.getInstance().getProductConfig().isRepIdLogin()) {
            if (isLocationValidationSuccess) {
                repIDLyt.setVisibility(View.VISIBLE);
                storeIDLyt.setVisibility(View.GONE);
            } else {
                repIDLyt.setVisibility(View.VISIBLE);
                storeIDLyt.setVisibility(View.VISIBLE);
            }
            failreason.setVisibility(View.GONE);
            failReasonRep.setVisibility(View.VISIBLE);
            failReasonRep.setText(title);
        }

        if (storeList != null) {
            mHeaderMessage.setText(getString(R.string.select_storeid));
            storeIdList.setVisibility(View.VISIBLE);
            ET_storeid.setVisibility(View.GONE);
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.spinner_child, storeList); //selected item will look like a spinner set from XML
            spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_child);
            storeIdList.setAdapter(spinnerArrayAdapter);
        }
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
                if (mAuthorizedStore) {
                    if (dialog != null)
                        dialog.dismiss();
                    startDeviceSupportCheck();
                    try {
                        if (storeList != null) {
                            preferenceHelper.putStringItem(Constants.STORE_ID, storeList[storeIdList.getSelectedItemPosition()]);
                            DashboardLog.getInstance().geteDeviceSwitchSession().setStoreId(storeList[storeIdList.getSelectedItemPosition()]);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    String enteredStoreId = ET_storeid.getText().toString().trim();
                    String enteredRepId = ET_repid.getText().toString().trim();

                    EDeviceLoggingInfo eDeviceLoggingInfo = getDeviceLoggingInfo();
                    eDeviceLoggingInfo.setLatitude(null);
                    eDeviceLoggingInfo.setLongitude(null);
                    eDeviceLoggingInfo.setCarrier("");
                    if (!isLocationValidationSuccess)
                        eDeviceLoggingInfo.setStoreCode(enteredStoreId);

                    if (!FeatureConfig.getInstance().getProductConfig().isRepIdValidationEnabled()) {
                        if (isLocationValidationSuccess) {
                            if (FeatureConfig.getInstance().getProductConfig().isRepIdLogin() && TextUtils.isEmpty(enteredRepId)) {
                                Toast.makeText(mContext, getString(R.string.str_pls_enter_emp_id), Toast.LENGTH_SHORT).show();
                            } else {
                                DashboardLog.getInstance().geteDeviceSwitchSession().setUserId(enteredRepId);
                                if (dialog != null)
                                    dialog.dismiss();
                                mAuthorizedStore = isStoreValidation = true;
                            }
                        } else {
                            if (!TextUtils.isEmpty(enteredStoreId)) {
                                if (FeatureConfig.getInstance().getProductConfig().isRepIdLogin() && TextUtils.isEmpty(enteredRepId)) {
                                    Toast.makeText(mContext, getString(R.string.str_pls_enter_emp_id), Toast.LENGTH_SHORT).show();
                                    return;
                                } else if (FeatureConfig.getInstance().getProductConfig().isRepIdLogin() && !TextUtils.isEmpty(enteredRepId)) {
                                    DashboardLog.getInstance().geteDeviceSwitchSession().setUserId(enteredRepId);
                                }
                                if (dialog != null)
                                    dialog.dismiss();
                                showProgressDialog(getString(R.string.connecting_to_server), getString(R.string.wds_please_wait));
                                startServerCall(Constants.STOREID_VALIDATION, Constants.STOREID_VALIDATION_URL, eDeviceLoggingInfo);
                            } else
                                Toast.makeText(mContext, getString(R.string.enter_storeid), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (isLocationValidationSuccess) {
                            if (enteredRepId != null && !enteredRepId.isEmpty()) {
                                if (dialog != null)
                                    dialog.dismiss();
                                showProgressDialog(getString(R.string.connecting_to_server), getString(R.string.wds_please_wait));
                                eDeviceLoggingInfo.setRepId(enteredRepId);
                                eDeviceLoggingInfo.setStoreCode(DashboardLog.getInstance().geteDeviceSwitchSession().getStoreId());
                                repId = enteredRepId;
                                startServerCall(Constants.REP_VALIDATION, Constants.REP_LOGIN_URL, eDeviceLoggingInfo);
                            } else
                                Toast.makeText(mContext, getString(R.string.str_pls_enter_emp_id), Toast.LENGTH_SHORT).show();

                        } else {
                            if (storeValidation(enteredStoreId, enteredRepId)) {
                                if (dialog != null)
                                    dialog.dismiss();
                                showProgressDialog(getString(R.string.connecting_to_server), getString(R.string.wds_please_wait));
                                repId = enteredRepId;
                                eDeviceLoggingInfo.setRepId(enteredRepId);
                                startServerCall(Constants.STORE_AND_REPVALIDATION, Constants.STORELOGINBYSTOREIDANDREPID_URL, eDeviceLoggingInfo);
                            }
                        }
                    }
                }
            }
        });
        BL_alert_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null)
                    dialog.dismiss();
                if(proceedOnValidation || Constants.NEW_PLAYSTORE_FLOW){
                    finish();
                }

            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    private boolean storeValidation(String enteredStoreId, String enteredRepId) {
        boolean isSuccess = true;
        try {
            if (enteredStoreId == null || enteredStoreId.isEmpty()) {
                Toast.makeText(mContext, getString(R.string.enter_storeid), Toast.LENGTH_SHORT).show();
                isSuccess = false;
            } else if (enteredRepId == null || enteredRepId.isEmpty()) {
                Toast.makeText(mContext, getString(R.string.str_pls_enter_emp_id), Toast.LENGTH_SHORT).show();
                isSuccess = false;
            }
        } catch (Exception e) {
            DLog.log("got exception in storeValidation : " + e.getMessage());
            isSuccess = false;
        }
        return isSuccess;
    }


    @Override
    public void gpsLocationGranted(boolean result) {
        DLog.log("gpsLocationGranted " + result);

        if (result) {
            if (this.locManager != null && locManager.isProviderEnabled("gps")) {
                DLog.log("isProviderEnabled: " + true);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    DLog.log("No Location Permission, requesting now...");
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            0);
                    return;
                }
                DLog.log("Location Permission Granted, requestLocationUpdates now...");
                gpsListner = new GpsListner();
                this.locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2 * 1000L, 10, gpsListner);
                this.locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2 * 1000L, 10, gpsListner);
                showProgressDialog(getString(R.string.connecting_to_server), getString(R.string.wds_please_wait));
                loggingHandler.postDelayed(logRunnable, 15000);
            }
        } else {
            if (!BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT)) {
                if ((Build.VERSION.SDK_INT >= 23) && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    DLog.log("No Location Permission, requesting now...");
                    ActivityCompat.requestPermissions(StoreValidationActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            0);
                } else {
                    displayStoreIdDialogue(getString(R.string.location_validation_failed), null);
                }
            }
            else
                isStoreValidation = true;
        }

    }

    @Override
    public void gpsLocationRequested(boolean result) {
        if (result) {
            locationRequested = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        DLog.log("on acitivty result " + requestCode + " " + resultCode);
        if (!isFinishing() && requestCode == 1 && (resultCode == 0 || resultCode == -1)) {
            if (!mAuthorizedStore) {
                locationRequested = true;
                validateStoreLocation();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void storeidValidation(boolean result, String response) {
        if (result) {
            isStoreValidation = mAuthorizedStore = true;
            hideProgressDialog();
            if (response != null) {
                String[] storeidList = response.split(",");
                if (storeidList.length > 1) {
                    displayStoreIdList(storeidList);
                } else {
                    preferenceHelper.putStringItem(Constants.STORE_ID, response);
                    DashboardLog.getInstance().geteDeviceSwitchSession().setStoreId(response);
                    //startDeviceSupportCheck();
                    if(proceedOnValidation || Constants.NEW_PLAYSTORE_FLOW){
                        startActivity(new Intent(this,EasyMigrateActivity.class));
                        finish();
                    }
                }
            }
        } else {
            hideProgressDialog();
            if (response == null|| response.equalsIgnoreCase("null")) {
                displayStoreIdDialogue(getString(R.string.invalid_storeid), null);
            }
            else {
                displayStoreIdDialogue(getString(R.string.no_internet), null);
            }
        }
    }

    @Override
    public void onserverCallCompleted(String response) {

    }

    @Override
    public void locationValidation(boolean result, String response) {
        hideProgressDialog();
        if (result) {
            if (!FeatureConfig.getInstance().getProductConfig().isRepIdValidationEnabled())
                isStoreValidation = mAuthorizedStore = true;
            if (response != null) {
                String[] storeidList = response.split(",");
                if (storeidList.length > 1) {
                    displayStoreIdList(storeidList);
                } else {
                    if (FeatureConfig.getInstance().getProductConfig().isRepIdValidationEnabled() || FeatureConfig.getInstance().getProductConfig().isRepIdLogin()) {
                        isLocationValidationSuccess = true;
                        EDeviceLoggingInfo eDeviceLoggingInfo = getDeviceLoggingInfo();
                        eDeviceLoggingInfo.setStoreCode(response);
                        displayStoreIdDialogue("", null);
                    }
                    preferenceHelper.putStringItem(Constants.STORE_ID, response);
                    DashboardLog.getInstance().geteDeviceSwitchSession().setStoreId(response);
                    startDeviceSupportCheck();
                }
            }
        } else {
            if (!BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT))
                displayStoreIdDialogue(getString(R.string.location_validation_failed), null);
            else
                isStoreValidation = true;
        }
    }

    @Override
    public void storeidAndRepidValidation(boolean result, String response) {
        hideProgressDialog();
        if (result) {
            DashboardLog.getInstance().geteDeviceSwitchSession().setStoreId(response);
            DashboardLog.getInstance().geteDeviceSwitchSession().setUserId(repId);
            isStoreValidation = mAuthorizedStore = true;
        } else
            displayStoreIdDialogue(getString(R.string.location_validation_failed), null);
    }

    @Override
    public void repLoginValidation(boolean result, String response) {
        hideProgressDialog();
        if (result) {
            DashboardLog.getInstance().geteDeviceSwitchSession().setUserId(repId);
            isStoreValidation = mAuthorizedStore = true;
        }
        else
            displayStoreIdDialogue(getString(R.string.invalid_repid), null);
    }

    private String repId = "-1";

    private class GpsListner implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            DLog.log("In GpsListner's onLocationChanged.............");
            if (location != null) {
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                locManager.removeUpdates(gpsListner);
                loggingHandler.removeCallbacks(logRunnable);
                DLog.log("onLocationChanged " + longitude + " " + latitude);
                EDeviceLoggingInfo eDeviceLoggingInfo = getDeviceLoggingInfo();
                eDeviceLoggingInfo.setLatitude(String.valueOf(latitude));
                eDeviceLoggingInfo.setLongitude(String.valueOf(longitude));
                DashboardLog.getInstance().geteDeviceSwitchSession().setLatitude(BigDecimal.valueOf(latitude));
                DashboardLog.getInstance().geteDeviceSwitchSession().setLongitude(BigDecimal.valueOf(longitude));
                validateGpsLogin(eDeviceLoggingInfo);
            } else {
                DLog.log("location is null.............");
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    private boolean isGoogleServicesAvailable(){
        int checkGooglePlayServices = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(StoreValidationActivity.this);
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

    public class GpsChangeReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive( Context context, Intent intent )
        {
            DLog.log("In GpsChangeReceiver ........++ ");
            if(gpsChangeReceiver != null)
                getApplicationContext().unregisterReceiver(gpsChangeReceiver);
            //Intent newIntent = new Intent(getApplicationContext(), StoreValidationActivity.class);
            locationRequested = true;
            //startActivity(getIntent());
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(getIntent());
                }
            }, 1000);
            DLog.log("Out GpsChangeReceiver ........++ ");
        }
    }

    public void showMustEnableLocationPopup() {
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(StoreValidationActivity.this);
            alert.setMessage(getText(R.string.location_must_turn_on_msg));
            String positiveString = getString(R.string.ept_ok);
            alert.setPositiveButton(positiveString, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (dialog != null) {
                        dialog.cancel();
                    }
                    if ("huawei".equalsIgnoreCase(DeviceInfo.getInstance().get_make()) && !isGoogleServicesAvailable()) {
                        showLocationPopup();
                    } else {
                        new startLocationAlert(StoreValidationActivity.this); //Display alert to enable GPS.
                    }
                }
            });
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    public void showLocationPopup() {
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(StoreValidationActivity.this);
/*            TextView dialogTextView = new TextView(SplashActivity.this);
            dialogTextView.setText(getText(R.string.location_turn_on_msg));
            dialogTextView.setGravity(Gravity.CENTER);
            alert.setView(dialogTextView);*/
            //alert.setTitle(" ");
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
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
/*          TextView messageView = (TextView) alertDialog.findViewById(android.R.id.message);
            messageView.setGravity(Gravity.CENTER);
            messageView.setPadding(5,0,5, 0);*/
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    private void validateStoreLocation() {
        if (FeatureConfig.getInstance().getProductConfig().isGeoFencingEnabled()) {
            if (NetworkUtil.isOnline()) {
                if (DeviceInfo.getInstance().isGPSEnabled()) {
                    this.gpsLocationGranted(true);
                } else if (locationRequested && !DeviceInfo.getInstance().isGPSEnabled()) {
                    showMustEnableLocationPopup();
                    //this.gpsLocationGranted(false);
                } else {
                    if ("huawei".equalsIgnoreCase(DeviceInfo.getInstance().get_make()) && !isGoogleServicesAvailable()) {
                        showLocationPopup();
                    } else {
                        new startLocationAlert(StoreValidationActivity.this); //Display alert to enable GPS.
                    }
                }
            } else {
                displayNetworkDialog(0);
            }
        } else if(FeatureConfig.getInstance().getProductConfig().isStoreIdValidationEnabled()){
            if (NetworkUtil.isOnline()) {
                displayStoreIdDialogue("", null);
            } else {
                displayNetworkDialog(0);
            }
        }else {
            isStoreValidation = mAuthorizedStore = true;
            startDeviceSupportCheck();
        }
    }

    private EDeviceLoggingInfo getDeviceLoggingInfo() {
        if (eDeviceLoggingInfo == null) {
            DeviceInfo deviceInfo = DeviceInfo.getInstance();
            eDeviceLoggingInfo = new EDeviceLoggingInfo();
            eDeviceLoggingInfo.setAddtionalInformation(null);
            eDeviceLoggingInfo.setBuildNumber(deviceInfo.getBuildNumber());
            eDeviceLoggingInfo.setCarrier(deviceInfo.get_carrierName());
            eDeviceLoggingInfo.setFirmware(deviceInfo.getFirmwareVersion());
            eDeviceLoggingInfo.setLatitude(null);
            eDeviceLoggingInfo.setLongitude(null);
            eDeviceLoggingInfo.setMake(deviceInfo.get_make());
            eDeviceLoggingInfo.setModel(deviceInfo.get_model());
            eDeviceLoggingInfo.setoSVersion(deviceInfo.getOSversion());
            eDeviceLoggingInfo.setPlatform(Constants.PLATFORM);
            eDeviceLoggingInfo.setUniqueDeviceId(deviceInfo.get_imei());
            eDeviceLoggingInfo.setSerialNumber(deviceInfo.get_serialnumber());
            eDeviceLoggingInfo.setStoreCode(null);
            eDeviceLoggingInfo.setCompanyName(Constants.COMPANY_NAME);
        }
        return eDeviceLoggingInfo;
    }

    private void startServerCall(String serviceCallFor, String url, EDeviceLoggingInfo eDeviceLoggingInfo) {
        Gson gson = new Gson();
        String loggingInfo = gson.toJson(eDeviceLoggingInfo);
        URLConnectionTask serviceCallTask = new URLConnectionTask(StoreValidationActivity.this, serviceCallFor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            serviceCallTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url, null, loggingInfo);
        else
            serviceCallTask.execute(url, null, loggingInfo);
    }

    private Handler loggingHandler = new Handler();
    private Runnable logRunnable = new Runnable() {
        @Override
        public void run() {
            loggingHandler.removeCallbacks(logRunnable);
            if (!mAuthorizedStore) {
                hideProgressDialog();
                locManager.removeUpdates(gpsListner);
                displayStoreIdDialogue(getString(R.string.location_validation_failed), null);
            }
        }
    };

    private void displayNetworkDialog(final int requestCode){
        AlertDialog.Builder alert = new AlertDialog.Builder(StoreValidationActivity.this);
        alert.setTitle(getString(R.string.no_network));
        alert.setMessage(getString(R.string.no_internet_message));
        alert.setPositiveButton(getString(R.string.wds_try_again), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (requestCode == 0)
                    validateStoreLocation();
                else
                    proceedToNext();
            }
        });
        alert.setNegativeButton(getString(R.string.wds_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(proceedOnValidation){
                    finish();
                }
            }
        });
        alert.setCancelable(false);
        AlertDialog alertDialog = alert.create();
        alertDialog.show();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            validateStoreLocation();
        } else {
            if (!BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT)) {
                if(Build.VERSION.SDK_INT >= 23){
                    selectedDontAskAgain = !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
                    if(selectedDontAskAgain)
                        displayLocationDialog(getString(R.string.str_alert),getString(R.string.location_permission_denied_msg));
                    else
                        displayLocationDialog(getString(R.string.str_alert),getString(R.string.location_permission_required_msg));
                } else {
                    displayStoreIdDialogue(getString(R.string.location_validation_failed), null);
                }
            }
            else {
                isStoreValidation = true;
            }
        }
    }

    boolean selectedDontAskAgain = false;
    public void displayLocationDialog(String title,String message) {
        try {
            android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(StoreValidationActivity.this);
            alert.setTitle(title);
            alert.setMessage(message);
            String positiveString = getString(R.string.ept_ok);
            if(selectedDontAskAgain)
                positiveString = getString(R.string.settings);

            alert.setPositiveButton(positiveString, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (NetworkUtil.isOnline() && dialog != null) {
                            dialog.cancel();
                            if(selectedDontAskAgain) {
                                Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS );
                                startActivity(intent);                            }
                            else
                                ActivityCompat.requestPermissions(StoreValidationActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                        0);
                        }
                    }
                });
            if(selectedDontAskAgain) {
                alert.setNegativeButton(getString(R.string.ept_exit_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        System.exit(0);
                    }
                });
            }
            alert.setCancelable(false);
            android.app.AlertDialog alertDialog = alert.create();
            alertDialog.show();
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    private void readMoreTsAndCs(final TextView textView) {
        SpannableString ss = new SpannableString(textView.getText().toString());
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                termsConditionsTv.setText(getResources().getString(R.string.terms_and_conditions_text_more));
                textView.setVisibility(View.GONE);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };
        ss.setSpan(clickableSpan, 0, 10, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),0,10,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }


    private void validateAuthenticationResponse(JsonObject jsonObj){
        try {
            if (jsonObj.has(Constants.RESPONSE_KEY_LOGIN_STATUS)) {
                String status = jsonObj.get(Constants.RESPONSE_KEY_LOGIN_STATUS).getAsString();
                if (!TextUtils.isEmpty(status) && status.equalsIgnoreCase("SUCCESS")) {
                    if (jsonObj.has(Constants.RESPONSE_KEY_COMPANY_ID)) {
                        int companyId = jsonObj.get(Constants.RESPONSE_KEY_COMPANY_ID).getAsInt();
                        DLog.log("Setting company ID as " + companyId);
                        DashboardLog.getInstance().geteDeviceSwitchSession().setCompanyId(String.valueOf(companyId));
                    } else {
                        DLog.log("Company ID not found.");
                    }
                    String[] storeidList = null;
                    if (jsonObj.has(Constants.RESPONSE_KEY_STORE_ID_LIST)) {
                        JsonArray storeidArray = jsonObj.get(Constants.RESPONSE_KEY_STORE_ID_LIST).getAsJsonArray();
                        storeidList = new String[storeidArray.size()];
                        for (int i = 0; i < storeidArray.size(); i++) {
                            storeidList[i] = storeidArray.get(i).getAsString();
                        }
                    }
                    if (storeidList != null && storeidList.length > 1) {
                        if (FeatureConfig.getInstance().getProductConfig().isRepIdValidationEnabled()) {
                            displayStoreIdDialogue("", null);
                        } else {
                            displayStoreIdList(storeidList);
                        }
                    } else if(storeidList !=null && storeidList.length ==1) {
                        preferenceHelper.putStringItem(Constants.STORE_ID, storeidList[0]);
                        DashboardLog.getInstance().geteDeviceSwitchSession().setStoreId(storeidList[0]);
                        if (FeatureConfig.getInstance().getProductConfig().isRepIdValidationEnabled() || FeatureConfig.getInstance().getProductConfig().isRepIdLogin()) {
                            isLocationValidationSuccess = true;
                            displayStoreIdDialogue("", null);
                        }else{
                            isStoreValidation = mAuthorizedStore = true;
                            if(proceedOnValidation){
                                startActivity(new Intent(this, EasyMigrateActivity.class));
                                finish();
                            }
                        }
                    }else if (jsonObj.has(Constants.RESPONSE_KEY_STORE_ID)) {
                        String storeID = jsonObj.get(Constants.RESPONSE_KEY_STORE_ID).getAsString();
                        preferenceHelper.putStringItem(Constants.STORE_ID, storeID);
                        DashboardLog.getInstance().geteDeviceSwitchSession().setStoreId(storeID);
                        if (FeatureConfig.getInstance().getProductConfig().isRepIdValidationEnabled()) {
                            isLocationValidationSuccess = true;
                            displayStoreIdDialogue("", null);
                        } else {
                            isStoreValidation = mAuthorizedStore = true;
                            if(proceedOnValidation){
                                startActivity(new Intent(this,EasyMigrateActivity.class));
                                finish();
                            }
                        }
                    }
                }else{
                    displayStoreIdDialogue(getString(R.string.location_validation_failed), null);
                }
            }
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    private void validateGpsLogin(EDeviceLoggingInfo eDeviceLoggingInfo) {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<JsonObject> call = apiService.validateGpsLogin(eDeviceLoggingInfo);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DLog.log("validateGpsLogin :: onResponse : Code :  " + response.code() + " ,Message : " + response.message() + " response : " + response.body());
                hideProgressDialog();
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    validateAuthenticationResponse(response.body());
                } else {
                    displayStoreIdDialogue(getString(R.string.location_validation_failed), null);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DLog.log("validateGpsLogin :: onFailure : " + t.getMessage());
                hideProgressDialog();
                displayStoreIdDialogue(getString(R.string.location_validation_failed), null);
            }
        });
    }


    AlertDialog multipleStoreListDialog;

    private void displayStoreIdList(final String[] storeidList) {
        try {
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.multiple_storeid_list, null);
            final AlertDialog.Builder builder2 = new AlertDialog.Builder(StoreValidationActivity.this)
                    .setCustomTitle(view)
                    .setCancelable(false)
                    .setSingleChoiceItems(storeidList, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (multipleStoreListDialog != null) {
                                multipleStoreListDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                            }
                            String storeId = storeidList[which];
                            DLog.log("Selected store id : " + storeId);
                            DashboardLog.getInstance().geteDeviceSwitchSession().setStoreId(storeId);
                        }
                    });
            builder2.setPositiveButton(getString(R.string.submit), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    if (FeatureConfig.getInstance().getProductConfig().isRepIdValidationEnabled()||FeatureConfig.getInstance().getProductConfig().isRepIdLogin()) {
                        isLocationValidationSuccess = true;
                        displayStoreIdDialogue("", null);
                    } else {
                        isStoreValidation = mAuthorizedStore = true;
                        if(proceedOnValidation){
                            startActivity(new Intent(StoreValidationActivity.this,EasyMigrateActivity.class));
                            finish();
                        }
                    }
                }
            });
            multipleStoreListDialog = builder2.create();
            multipleStoreListDialog.show();
            multipleStoreListDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        } catch (Exception e) {
            DLog.log("Exception in showing custom dialog: "+e.getMessage());
        }
    }

    private boolean hasImei(){

        DLog.log("Enter hasIMEI ");
        //For Android 9 and lower, IMEI is automatically obtained
        if((android.os.Build.VERSION.SDK_INT < Constants.IMEI_CHECK_MIN_APILEVEL))
            return true;

        //Check for allowed features based on customer name
//        if(AppUtils.PermissionsFlow.isAllowed(GlobalConfig.getInstance().getCompanyName(), AppUtils.PermissionsFlow.Features.IMEI_READ)){
//        if (BuildConfig.FLAVOR.equalsIgnoreCase(FLAVOUR_PLAYSTORE)){
        if (BuildConfig.FLAVOR.equalsIgnoreCase(FLAVOUR_PLAYSTORE) || BuildConfig.FLAVOR.equalsIgnoreCase(FLAVOUR_TMS) ){
            if(!CommonUtil.retrivedIMEIForAndroid10()) {
                DLog.log("Case of !CommonUtil.retrivedIMEIForAndroid10()");
                IMEIReadChildActivity.startActivity(this);
                return  false;
            }
        }else{
            DLog.log("getting getMACAddressInIMEIFormat");
            String imei = CommonUtil.getMACAddressInIMEIFormat(this);
            DLog.log("getMACAddressInIMEIFormat "+imei);
            CommonUtil.saveImeiInPrefs(imei);
//            com.pervacio.batterydiaglib.util.LogUtil.debug( "hasImei(): MAC :"+imei);
        }
        return true;
    }
}

interface supportDeviceCheck {
    void isSupportedDevice(boolean result);
}
