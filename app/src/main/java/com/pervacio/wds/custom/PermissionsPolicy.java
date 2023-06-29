package com.pervacio.wds.custom;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pervacio.crashreportlib.LogReporting;

import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.ui.EasyMigrateActivity;
import com.pervacio.wds.custom.models.FeatureConfig;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.DeviceInfo;
import com.pervacio.wds.custom.utils.NetworkUtil;
import com.pervacio.wds.custom.utils.PreferenceHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.pervacio.wds.custom.utils.Constants.CLOUD_PAIRING_ENABLED;
import static com.pervacio.wds.custom.utils.Constants.FLAVOUR_PLAYSTORE;
import static com.pervacio.wds.custom.utils.Constants.FLAVOUR_TMS;

public class PermissionsPolicy extends AppCompatActivity {
    TextView pp_header_tv, pp_body_tv;
    private boolean isStoreConfigSuccess = false;
    private String storeID = "";
    ProgressBar action_spinner;
    LinearLayout btn_layout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions_policy);
        btn_layout = findViewById(R.id.btn_layout);
        action_spinner = findViewById(R.id.action_spinner1);
        action_spinner.setVisibility(View.GONE);
        pp_header_tv = (TextView) findViewById(R.id.pp_header);
        pp_body_tv = (TextView) findViewById(R.id.pp_body);
        pp_body_tv.setMovementMethod(new ScrollingMovementMethod());
        //SpannableString spannablePolicyBody = new SpannableString(getString(R.string.permissions_policy_body));
        SpannableString spannablePolicyBody;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            pp_header_tv.setText(Html.fromHtml(getString(R.string.permissions_policy_header), Html.FROM_HTML_MODE_COMPACT));
            spannablePolicyBody = new SpannableString(Html.fromHtml(getString(R.string.permissions_policy_body), Html.FROM_HTML_MODE_COMPACT));
        } else {
            pp_header_tv.setText(Html.fromHtml(getString(R.string.permissions_policy_header)));
            spannablePolicyBody = new SpannableString(Html.fromHtml(getString(R.string.permissions_policy_body)));
        }
//        ClickableSpan clickableTandC = new ClickableSpan() {
//            @Override
//            public void onClick(View textView) {
//                startActivity(new Intent(getApplicationContext(), TermsAndConditionsActivity.class));
//            }
//        };
//        ClickableSpan clickablePrivacy = new ClickableSpan() {
//            @Override
//            public void onClick(View textView) {
//                Intent policiIntent = new Intent(getApplicationContext(), TermsAndConditionsActivity.class);
//                policiIntent.putExtra("isPrivacyPolicyScreen", true);
//                startActivity(policiIntent);
//            }
//        };

        String linkText;
        DLog.log("Normal Text: "+getString(R.string.str_terms_condition));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            linkText = (new SpannableString(Html.fromHtml(getString(R.string.str_terms_condition), Html.FROM_HTML_MODE_COMPACT))).toString();//getString(R.string.str_terms_condition);
        else
            linkText = (new SpannableString(Html.fromHtml(getString(R.string.str_terms_condition)))).toString();//getString(R.string.str_terms_condition);
        DLog.log("Spanned Text: "+linkText);
        String strPolicyBody = spannablePolicyBody.toString();
        DLog.log("Body Text: "+strPolicyBody);
//        spannablePolicyBody.setSpan(clickableTandC, strPolicyBody.indexOf(linkText),
//                strPolicyBody.indexOf(linkText) + linkText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
//            linkText = (new SpannableString(Html.fromHtml(getString(R.string.str_privacy_statement), Html.FROM_HTML_MODE_COMPACT))).toString();//getString(R.string.str_terms_condition);
//        else
//        linkText = (new SpannableString(Html.fromHtml(getString(R.string.str_privacy_statement)))).toString();//getString(R.string.str_terms_condition);
//        spannablePolicyBody.setSpan(clickablePrivacy, strPolicyBody.indexOf(linkText),
//                strPolicyBody.indexOf(linkText) + linkText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        pp_body_tv.setText(spannablePolicyBody, TextView.BufferType.SPANNABLE);
        pp_body_tv.setMovementMethod(LinkMovementMethod.getInstance());
        Button iAgree = (Button) findViewById(R.id.btn_postive);
        Button iDisagree = (Button) findViewById(R.id.btn_negative);
        iAgree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceHelper.getInstance(PermissionsPolicy.this).putBooleanItem(Constants.TERMS_AGREED, true);
                proceedToNext();
/*              if(Constants.NEW_PLAYSTORE_FLOW) {
                    btn_layout.setVisibility(View.GONE);
                    pp_header_tv.setVisibility(View.GONE);
                    pp_body_tv.setVisibility(View.GONE);
                    if("tms".equalsIgnoreCase(BuildConfig.FLAVOR)){
                        action_spinner.setVisibility(View.VISIBLE);
                        startConfigServiceCall();
                    } else{
                        displayStoreIdDialogue(getString(R.string.enter_your_id), null);
                    }
                } else {
                    if (FeatureConfig.getInstance().getProductConfig().isStoreIdValidationEnabled()) {
                            startActivity(new Intent(getApplicationContext(), StoreValidationActivity.class));
                    } else {
                        startActivity(new Intent(getApplicationContext(), EasyMigrateActivity.class));
                    }
                    finish();
                }*/
            }
        });

        iDisagree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    private void proceedToNext() {
//        String company = PreferenceManager.getDefaultSharedPreferences(emGlobals.getmContext()).getString(LogReporting.COMPANY_NAME, Constants.COMPANY_NAME);
//        if (FeatureConfig.getInstance().getProductConfig().isUserIdValidationEnabled() && (!company.equalsIgnoreCase("TelefonicaBrazil"))) {
//            //PreferenceHelper.getInstance(PermissionsPolicy.this).putStringItem("userID", storeID);
//            startActivity(new Intent(getApplicationContext(),LoginActivity.class));
//
//        } else if (FeatureConfig.getInstance().getProductConfig().isRepIdLogin() ||
//                   FeatureConfig.getInstance().getProductConfig().isRepIdValidationEnabled() ||
//                   "tms".equalsIgnoreCase(BuildConfig.FLAVOR)) {
//            startActivity(new Intent(getApplicationContext(), StoreValidationActivity.class));
//
//        } else {
//            if(hasImei()) {
//                startActivity(new Intent(getApplicationContext(), EasyMigrateActivity.class));
//            }
//        }
        Intent intent = new Intent(PermissionsPolicy.this, EasyMigrateActivity.class);
        startActivity(intent);
        finish();
    }
        public void displayStoreIdDialogue(String title, String failReason) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.storeid_dialogue);
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;

        final Button BL_alert_submit = (Button) dialog.findViewById(R.id.BL_alert_submit);
        Button BL_alert_cancel = (Button) dialog.findViewById(R.id.BL_alert_cancel);
        TextView  textTitle = (TextView) dialog.findViewById(R.id.textTitle);
            textTitle.setText(title);
        final EditText ET_storeid = (EditText) dialog.findViewById(R.id.storeid);
        TextView failreasonTV = (TextView) dialog.findViewById(R.id.reason);
        if(failReason == null || "".equalsIgnoreCase(failReason)) {
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
                System.exit(0);
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    public void displayNetworkDialog(String title,String message) {
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(PermissionsPolicy.this);
            alert.setTitle(title);
            alert.setMessage(message);
            String positiveString = getString(R.string.wds_try_again);
                alert.setPositiveButton(positiveString, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (NetworkUtil.isOnline() && dialog != null) {
                            dialog.cancel();
                                if (Constants.NEW_PLAYSTORE_FLOW) {
                                    if (isStoreConfigSuccess)
                                        startConfigServiceCall();
                                    else
                                        displayStoreIdDialogue(getString(R.string.enter_your_id), null);
                                }
                            }
                        else {
                            displayNetworkDialog(getString(R.string.no_network), getString(R.string.no_internet_message));
                        }
                    }
                });
            alert.setNegativeButton(getString(R.string.ept_exit_button), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    System.exit(0);
                }
            });
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    public void  startStoreConfigServiceCall(final String storeID) {
        DLog.log("Store Config Calling Api Service: ");
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<JsonObject> call = apiService.getStoreConfig(EMUtility.getAuthToken(), storeID, "wds");
        DLog.log("Store Config Calling Api Service call enqueue: ");
        try {
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject>call, Response<JsonObject> response) {
                    try {
                        DLog.log("Requested URL: "+response.raw().request().url());
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
                                        DLog.log("From Response Json got storeCode.........."+store_id);
                                        if(store_id == null || "".equalsIgnoreCase(store_id)) {
                                            store_id = storeID;
                                            DLog.log("Taken entred storeID as storeCode.........."+store_id);
                                        }
                                        PreferenceHelper.getInstance(PermissionsPolicy.this).putStringItem(Constants.STORE_ID, store_id);
                                        DashboardLog.getInstance().geteDeviceSwitchSession().setStoreId(store_id);
                                    } else{
                                        DLog.log("Taken entred storeID as storeCode.........."+storeID);
                                        PreferenceHelper.getInstance(PermissionsPolicy.this).putStringItem(Constants.STORE_ID, storeID);
                                        DashboardLog.getInstance().geteDeviceSwitchSession().setStoreId(storeID);
                                    }
                                    if (storeConfig.has("companyName")) {
                                        DLog.log("Response Json has companyName..........");
                                        String companyName = storeConfig.get("companyName").getAsString();
                                        DLog.log("From Response Json got companyName.........."+companyName);
                                            if(companyName != null && !"".equalsIgnoreCase(companyName)) {
                                                Constants.COMPANY_NAME = storeConfig.get("companyName").getAsString();
                                                PreferenceManager.getDefaultSharedPreferences(PermissionsPolicy.this).edit().putString(LogReporting.COMPANY_NAME, Constants.COMPANY_NAME).apply();
                                            }
                                        }
                                    if (storeConfig.has("companyId")) {
                                        String companyId = storeConfig.get("companyId").getAsString();
                                        if(companyId != null && !"".equalsIgnoreCase(companyId)) {
                                            DashboardLog.getInstance().geteDeviceSwitchSession().setCompanyId(companyId);
                                        }
                                    }
                                    if (storeConfig.has("password")) {
                                        String password = storeConfig.get("password").getAsString();
                                        if(password != null && !"".equalsIgnoreCase(password)) {
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
                                            PreferenceManager.getDefaultSharedPreferences(PermissionsPolicy.this).edit().putString(LogReporting.END_POINT, Constants.LOG_UPLOAD_URL).apply();
                                            startConfigServiceCall();
                                        }
                                    }

                                }
                            } else {
                                displayStoreIdDialogue(getString(R.string.enter_your_id), getString(R.string.invalid_id));
                            }
                        } else {
                            displayStoreIdDialogue(getString(R.string.enter_your_id), getString(R.string.invalid_id));
                        }
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(Call<JsonObject>call, Throwable t) {
                    DLog.log("Config Details error : "+t.getMessage());
                    displayStoreIdDialogue(getString(R.string.enter_your_id), getString(R.string.ept_timeout));
                }
            });
        } catch (Exception e) {
            DLog.log("Store Config Details error : "+e.getMessage());
            displayStoreIdDialogue(getString(R.string.enter_your_id), getString(R.string.ept_timeout));
        }
    }

    public void  startConfigServiceCall() {
        DLog.log("Config Calling Api Service: ");
        ApiInterface apiService =
                ApiClient.getClient().create(ApiInterface.class);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("make", DeviceInfo.getInstance().get_make());
        jsonObject.addProperty("model",DeviceInfo.getInstance().get_model());
        jsonObject.addProperty("company",Constants.COMPANY_NAME.toLowerCase());
        jsonObject.addProperty("mergeCompanyConfigData",true);
        jsonObject.addProperty("platform",Constants.PLATFORM);
        jsonObject.addProperty("product","wds");

        Call<FeatureConfig> call = apiService.getConfigDetails(EMUtility.getAuthToken(),jsonObject);
        DLog.log("Config Calling Api Service call enqueue: ");
        try {
            call.enqueue(new Callback<FeatureConfig>() {
                @Override
                public void onResponse(Call<FeatureConfig>call, Response<FeatureConfig> response) {
                    //DLog.log("Requested URL: "+response.raw().request().url());
                    if (response.body() != null) {
                        DLog.log("Config Details: " + new Gson().toJson(response.body(), FeatureConfig.class));
                        FeatureConfig featureConfig = response.body();
                        if("privatelabel".equalsIgnoreCase(BuildConfig.FLAVOR))
                            featureConfig.getProductConfig().setShowPermissionsInstructions(true);
                        if(Constants.NEW_PLAYSTORE_FLOW) {
                            featureConfig.getProductConfig().setShowPermissionsInstructions(true);
                            if(!"tms".equalsIgnoreCase(BuildConfig.FLAVOR)) {
                                featureConfig.getProductConfig().setGeoFencingEnabled(false); //For other flavors except TMS force GeoFencing false
                            }
                        }
                        FeatureConfig.setInstance(featureConfig);
                        CLOUD_PAIRING_ENABLED = FeatureConfig.getInstance().getProductConfig().isCloudpairingRequired();
                        Constants.LOGGING_ENABLED = FeatureConfig.getInstance().getProductConfig().isTransactionLoggingEnabled();
                        EMUtility.updateSupportingDataTypes();
                        proceedToNext();
                        action_spinner.setVisibility(View.GONE);
                    } else {
                        displayNetworkDialog(getString(R.string.ept_timeout),getString(R.string.ept_server_response));
                    }
                }
                @Override
                public void onFailure(Call<FeatureConfig>call, Throwable t) {
                    DLog.log("Config Details error : "+t.getMessage());
                    displayNetworkDialog(getString(R.string.ept_timeout),getString(R.string.ept_server_response));
                }
            });
        } catch (Exception e) {
            DLog.log("Store Config Details error : "+e.getMessage());
            displayStoreIdDialogue(getString(R.string.enter_your_id), getString(R.string.ept_timeout));
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

