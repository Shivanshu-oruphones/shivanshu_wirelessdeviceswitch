package com.pervacio.wds.custom;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.pervacio.wds.CustomProgressDialog;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.custom.utils.DashboardLog;
import java.net.HttpURLConnection;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class EmailSummaryActivity extends AppCompatActivity {
    private Button exit, email_btn;
    private EditText cust_email_et;
    private TextView invalidEmailText;
    LinearLayout store_email_layout;
    private CustomProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_p_summary);
        setTitle(getResources().getString(R.string.email_summary));
        store_email_layout = (LinearLayout) findViewById(R.id.store_email_layout);
        email_btn = (Button) findViewById(R.id.email_btn);
        exit = (Button) findViewById(R.id.email_back_btn);
        exit.setSelected(false);
        cust_email_et = (EditText) findViewById(R.id.cust_email);
        invalidEmailText = (TextView) findViewById(R.id.invalid_email_tv);
        mProgressDialog = new CustomProgressDialog(this);

        cust_email_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable email) {
                boolean isValidEmail = isEmailValid(email);
                email_btn.setEnabled(isValidEmail);
                if (!isValidEmail) {
                    exit.setSelected(false);
                }
            }
        });
        email_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emailSummery();
            }
        });
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void emailSummery(){
        String email = cust_email_et.getText().toString().trim();
        if (!email.isEmpty()) {
            if (isEmailValid(email)) {
                invalidEmailText.setVisibility(View.GONE);
                hideKeyboard(EmailSummaryActivity.this);
                if (isOnline()) {
                    sendEmaiId(email);
                } else{
                    twoButtonDialog(EmailSummaryActivity.this, getResources().getString(R.string.wds_alert), getResources().getString(R.string.network_msz), new String[]{getString(R.string.wds_cancel), getString(R.string.retry)},
                            null, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    emailSummery();
                                }
                            }
                    );
                }
            } else {
                setTextVisible();
                invalidEmailText.setText(R.string.enter_valid_email);
            }
        } else {
            setTextVisible();
            invalidEmailText.setText(R.string.plz_enter_email);
        }
    }

    private void setTextVisible() {
        invalidEmailText.setVisibility(View.VISIBLE);
//        cust_email_et.setBackground(getResources().getDrawable(R.drawable.rectangular_edit_text));
    }

    private void sendEmaiId(String emailId) {

        mProgressDialog.setInfo(R.string.sending_email_summary,R.string.wds_please_wait);
        mProgressDialog.show();
        String sessionId = DashboardLog.getInstance().geteDeviceSwitchSession().getDeviceSwitchSessionId();
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);

        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("sessionId", Long.parseLong(sessionId));
        jsonObject.addProperty("locale", getResources().getString(R.string.locale));
        jsonObject.addProperty("emailTo", emailId);

        Call<JsonPrimitive> sendSumamryEmailCall = apiService.sendSummaryEmail(EMUtility.getAuthToken(),jsonObject);
        sendSumamryEmailCall.enqueue(new Callback<JsonPrimitive>() {
            @Override
            public void onResponse( Call<JsonPrimitive> call,  Response<JsonPrimitive> response) {
                DLog.log("enter sendSumamryEmailCall onResponse "+response);
                DLog.log("sendSumamryEmailCall Response body : "+response.body());
                JsonPrimitive jsonPrimitive = response.body();
                try {
                    DLog.log("sendSumamryEmailCall Requested URL: "+response.raw().request().url());
                    DLog.log("sendSumamryEmailCall Requested Body: "+response.raw().request());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mProgressDialog.hide();
                exit.setSelected(true);
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    if(jsonPrimitive.getAsString().equalsIgnoreCase("SUCCESS")) {
                        Toast.makeText(EmailSummaryActivity.this, getString(R.string.email_sent_success), Toast.LENGTH_LONG).show();
                    }else{
                        DLog.log("Email is not sent, there is an error");
                    }
                }else{
                    DLog.log("Email is not sent, there is an error");
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonPrimitive> call, @NonNull Throwable t) {
                DLog.log("sendSumamryEmailCall exception: " + t.getMessage());
                mProgressDialog.hide();
            }
        });
    }

    private boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // check internet connection
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) EmailSummaryActivity.this
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }


        public void twoButtonDialog(Activity activity,String title, String message, String[] buttonText,final View.OnClickListener firstButton,final View.OnClickListener secondButton) {
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.cutom_alert_dialog);
            dialog.setCancelable(false);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            TextView BL_alert_head = (TextView) dialog
                    .findViewById(R.id.BL_alert_head);
            TextView BL_alert_text = (TextView) dialog
                    .findViewById(R.id.BL_alert_text);
            BL_alert_head.setText(title);
            BL_alert_text.setText(message);
            Button BL_alert_ok = (Button) dialog.findViewById(R.id.BL_alert_yes);
            Button BL_alert_no = (Button) dialog.findViewById(R.id.BL_alert_no);
            /*Buttons as per requirements*/
            String firstButtonText = buttonText[0];
            String SecondButtonText = null;
            if(buttonText.length>1){
                SecondButtonText = buttonText[1];
            }
            BL_alert_ok.setVisibility(View.GONE);
            BL_alert_no.setVisibility(View.GONE);
            if(!TextUtils.isEmpty(firstButtonText)){
                BL_alert_ok.setText(firstButtonText);
                BL_alert_ok.setVisibility(View.VISIBLE);
            }
            if(!TextUtils.isEmpty(SecondButtonText)){
                BL_alert_no.setText(SecondButtonText);
                BL_alert_no.setVisibility(View.VISIBLE);
            }
            BL_alert_ok.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.dismiss();
                    if(firstButton!=null){
                        firstButton.onClick(v);
                    }
                }
            });
            BL_alert_no.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.dismiss();
                    if(secondButton!=null){
                        secondButton.onClick(v);
                    }
                }
            });

            dialog.show();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);

        }

        private void setTitle(String title){
            ActionBar supportActionBar = getSupportActionBar();
            if (supportActionBar != null ){
                supportActionBar.setDisplayHomeAsUpEnabled(false);
                supportActionBar.setTitle(title);
            }
        }
}
