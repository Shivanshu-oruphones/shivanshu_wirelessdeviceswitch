package com.pervacio.wds.custom;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.ui.EasyMigrateActivity;
import com.pervacio.wds.custom.models.FeatureConfig;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.ContextWrapperHelper;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.NetworkUtil;
import com.pervacio.wds.custom.utils.PreferenceHelper;

import java.net.HttpURLConnection;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.pervacio.wds.custom.utils.Constants.FLAVOUR_PLAYSTORE;
import static com.pervacio.wds.custom.utils.Constants.FLAVOUR_TMS;

/**
 * A login screen that offers login via user id/password.
 */
public class LoginActivity extends AppCompatActivity {

    Button mLoginButton;
    private EditText mUserName, mStoreId;
    private EditText mPasswordView;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_activity_login);
        }
        mUserName = (EditText) findViewById(R.id.username);
        if(Constants.NEW_PLAYSTORE_FLOW) {
            mUserName.setText(PreferenceHelper.getInstance(LoginActivity.this).getStringItem("userID"));
            mUserName.setVisibility(View.GONE);
            mUserName.setEnabled(false);
        }
            mStoreId = (EditText) findViewById(R.id.storeid);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mLoginButton = (Button) findViewById(R.id.email_sign_in_button);
        mLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mStoreId.setError(null);
        if(Constants.NEW_PLAYSTORE_FLOW) {
            mUserName.setText(PreferenceHelper.getInstance(LoginActivity.this).getStringItem("userID"));
        } else {
            mUserName.setError(null);
        }
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String storeId = mStoreId.getText().toString();
        String userName = mUserName.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid userName address.
       if (TextUtils.isEmpty(userName)) {
           if(Constants.NEW_PLAYSTORE_FLOW) {
               mUserName.setText(PreferenceHelper.getInstance(LoginActivity.this).getStringItem("userID"));
           } else {
               mUserName.setError(getString(R.string.error_field_userid_required));
           }
            focusView = mUserName;
            cancel = true;
        } else if (!isValidUserName(userName)) {
           if(Constants.NEW_PLAYSTORE_FLOW) {
               mUserName.setText(PreferenceHelper.getInstance(LoginActivity.this).getStringItem("userID"));
           } else {
               mUserName.setError(getString(R.string.error_invalid_username));
           }
            focusView = mUserName;
            cancel = true;
        } else if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_password_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(userName)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }


        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {

            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mPasswordView.getWindowToken(), 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            validateUserLogin();
        }
    }

    private boolean isValidUserName(String userName) {
        return userName.length() > 0;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 0;
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

    private void validateUserLogin() {
        if (NetworkUtil.isOnline()) {
            showProgressDialog("", getString(R.string.wds_please_wait));
            ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
            final JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("userName", mUserName.getText().toString());
            jsonObject.addProperty("password", mPasswordView.getText().toString());
            jsonObject.addProperty("companyName", Constants.COMPANY_NAME);

            Call<JsonObject> call = apiService.validateUserLogin(jsonObject);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    DLog.log("validateUserLogin :: onResponse : Code :  " + response.code() + " ,Message : " + response.message() + " response : " + response.body());
                    hideProgressDialog();
                    try {
                        if (response.code() == HttpURLConnection.HTTP_OK) {
                            parseResponse(response.body());

                        } else {
                            if (response.body() != null && response.body().isJsonObject()) {
                                parseResponse(response.body());
                            } else if (response.errorBody() != null) {
                                JsonObject responseObj = new Gson().getAdapter(JsonObject.class).fromJson(response.errorBody().string());
                                parseResponse(responseObj);
                            } else {
                                displayFailDialog(getString(R.string.error_invalid_credentials));
                            }
                        }
                    } catch (Exception e) {
                        DLog.log("onResponse :: Exception : " + Log.getStackTraceString(e));
                        displayFailDialog(getString(R.string.error_invalid_credentials));
                    }

                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    DLog.log("validateUserLogin :: onFailure : " + t.getMessage());
                    hideProgressDialog();
                    displayFailDialog(getString(R.string.error_invalid_credentials));
                }
            });
        } else {
            displayNetworkDialog();
        }
    }

    private void parseResponse(JsonObject result) {
        if (result != null && result.isJsonObject()) {
            DLog.log("User Login Result Json: "+result.toString());
            if (result.has("loginStatus") && result.get("loginStatus").getAsString().equalsIgnoreCase("SUCCESS")) {
                DashboardLog.getInstance().geteDeviceSwitchSession().setUserId(mUserName.getText().toString());
                if (result.has("companyId")) {
                    DashboardLog.getInstance().geteDeviceSwitchSession().setCompanyId(result.get("companyId").getAsString());
                }
                if (result.has("storeCode")) {
                    PreferenceHelper.getInstance(LoginActivity.this).putStringItem(Constants.STORE_ID, result.get("storeCode").getAsString());
                    DashboardLog.getInstance().geteDeviceSwitchSession().setStoreId(result.get("storeCode").getAsString());
                }
                if(hasImei()) {
                    startActivity(new Intent(LoginActivity.this, EasyMigrateActivity.class));
                    finish();
                }
            } else if (result.has("loginStatus") && result.get("loginStatus").getAsString().equalsIgnoreCase("INVALID_STORE")) {
                displayFailDialog(getString(R.string.error_invalid_storeid));
            } else if (result.has("loginStatus") && result.get("loginStatus").getAsString().equalsIgnoreCase("WRONG_CREDENTIAL")) {
                displayFailDialog(getString(R.string.error_invalid_credentials));
            } else {
                displayFailDialog(getString(R.string.error_invalid_credentials));
            }
        } else {
            displayFailDialog(getString(R.string.error_invalid_credentials));
        }
    }

    private void displayFailDialog(String title) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
                .setMessage(title)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ept_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        Dialog interruptedDialog = alertDialogBuilder.create();
        interruptedDialog.setCanceledOnTouchOutside(false);
        interruptedDialog.show();
    }


    private void displayNetworkDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(getString(R.string.no_network));
        alert.setMessage(getString(R.string.no_internet_message));
        alert.setPositiveButton(getString(R.string.wds_try_again), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null)
                    dialog.dismiss();
                validateUserLogin();
            }
        });
        alert.setCancelable(false);
        AlertDialog alertDialog = alert.create();
        alertDialog.show();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        String storedLocale = PreferenceHelper.getInstance(newBase).getStringItem(Constants.LOCALE);
        if (TextUtils.isEmpty(storedLocale)) {
            storedLocale = FeatureConfig.getInstance().getProductConfig().getDefaultLanguage();
        }
        DLog.log("Read locale: " + storedLocale);
        super.attachBaseContext(ContextWrapperHelper.wrap(newBase, storedLocale));
    }

    public void showSnackbar(String message, int duration) {
        final Snackbar snackbar = Snackbar.make(findViewById(R.id.login_rootView), message, duration);
        snackbar.setAction(getString(R.string.ept_exit_button), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
                System.exit(0);
            }
        });
        snackbar.show();
    }

    private static final int TIME_INTERVAL = 3000;
    private long mBackPressed;

    @Override
    public void onBackPressed() {
        if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
            super.onBackPressed();
            return;
        } else {
            showSnackbar(getString(R.string.press_again_to_exit), TIME_INTERVAL);
        }
        mBackPressed = System.currentTimeMillis();
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