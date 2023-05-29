package com.pervacio.wds.custom;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.custom.models.EmailServiceDto;
import com.pervacio.wds.custom.models.FeatureConfig;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.ContextWrapperHelper;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.DeviceInfo;
import com.pervacio.wds.custom.utils.NetworkUtil;
import com.pervacio.wds.custom.utils.PreferenceHelper;

import java.io.File;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ReportAProblem extends AppCompatActivity {
    static EMGlobals emGlobals = new EMGlobals();
    private ProgressDialog mProgressDialog;
    TextView privacy_policy, txt_transaction_id;
    Button btn_negative, btn_postive;
    EditText structured_edittext_emaiid, structured_edittext_phonenumber, structured_edittext_answer;
    String email_Text, phonenumber_Text, description_Text, transaction_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_aproblem);
        privacy_policy = (TextView) findViewById(R.id.privacy_policy);
        txt_transaction_id = (TextView) findViewById(R.id.txt_transaction_id);
        structured_edittext_emaiid = (EditText) findViewById(R.id.structured_edittext_emaiid);
        structured_edittext_phonenumber = (EditText) findViewById(R.id.structured_edittext_phonenumber);
        structured_edittext_answer = (EditText) findViewById(R.id.structured_edittext_answer);

        btn_negative = (Button) findViewById(R.id.btn_negative);
        btn_postive = (Button) findViewById(R.id.btn_postive);
        //privacy_policy.setMovementMethod(LinkMovementMethod.getInstance());


        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeAsUpIndicator(R.drawable.back);
            actionBar.setTitle(R.string.title_activity_report_aproblem);
        }
        btn_postive.setText(R.string.submit);
        btn_negative.setText(R.string.str_skip);
        transaction_id = DashboardLog.getInstance().geteDeviceSwitchSession().getDeviceSwitchSessionId();
        txt_transaction_id.setText(getString(R.string.uniqueTransactionId) + transaction_id);
        btn_negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btn_postive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email_Text = structured_edittext_emaiid.getText().toString().trim();
                phonenumber_Text = structured_edittext_phonenumber.getText().toString().trim();
                description_Text = structured_edittext_answer.getText().toString();

                if (!email_Text.isEmpty() && !phonenumber_Text.isEmpty() && !description_Text.isEmpty()) {
                    if (isEmailValid(email_Text)) {
                        reportProblem();
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.valid_email), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.required_fields), Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    public static boolean isEmailValid(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public void reportProblem() {
        if (NetworkUtil.isOnline()) {
            showProgressDialog("", getString(R.string.wds_please_wait));

            Runnable runnable = new Runnable() {
                public void run() {
                    ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
                    final File logfile = new File(emGlobals.getmContext().getFilesDir().toString() + "/" + "reportlog.txt");
                    byte[] log = null;
                    try {
                        log = EMUtility.readFileToByteArray(logfile);
                    } catch (Exception e) {
                        DLog.log(e);
                    }
                    EmailServiceDto emailServiceDto = new EmailServiceDto();
                    emailServiceDto.setEmailTo(new String[]{FeatureConfig.getInstance().getProductConfig().getRapEmail()});
                    emailServiceDto.setEmailBody("<html><body><b>Phone Number : </b>" + phonenumber_Text + "<br><b>EmailID : </b>" + email_Text + "</br><b>Description : </b>" + description_Text + "</body></html>");
                    emailServiceDto.setSubject(getSubject());
                    emailServiceDto.addAttachement(getFileName(), log);
                    emailServiceDto.addDispostion("test", "attachment");
                    emailServiceDto.setUserName(Constants.HTTP_AUTHENTICATION_USERNAME);
                    emailServiceDto.setPassword(Constants.HTTP_AUTHENTICATION_PASSWORD);
                    emailServiceDto.setStoreCode(DashboardLog.getInstance().geteDeviceSwitchSession().getStoreId());
                    Call<JsonObject> call = apiService.reportAProblemService(EMUtility.getAuthToken(), emailServiceDto);
                    call.enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            DLog.log(" onResponse report problem Details: " + response.code() + " " + response.body());
                            hideProgressDialog();
                            if (response.code() == HttpURLConnection.HTTP_OK) {
                                try {
                                    logfile.delete();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                displayAlertDialog(0);
                            } else {
                                displayAlertDialog(1);
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            DLog.log(" onFailure report problem Details: " + t.getMessage());
                            hideProgressDialog();
                            displayAlertDialog(1);
                        }
                    });
                }
            };
            new Thread(runnable).start();
        } else {
            displayAlertDialog(2);
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

    private void displayAlertDialog(final int responseCode) {
        AlertDialog.Builder alert = new AlertDialog.Builder(ReportAProblem.this);
        String buttonName = getString(R.string.transfer_interrupted_action);
        if (responseCode == 0) {
            alert.setTitle(getString(R.string.reportproblem_title));
            alert.setMessage(getString(R.string.reportproblem_message));
        } else if(responseCode == 1) {
            alert.setTitle(getString(R.string.reportproblem_error_title));
            alert.setMessage(getString(R.string.reportproblem_error_message));
        }else {
            alert.setTitle(getString(R.string.no_network));
            alert.setMessage(getString(R.string.no_internet_message));
            buttonName = getString(R.string.wds_try_again);
        }
        alert.setPositiveButton(buttonName, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null)
                    dialog.dismiss();
                if (responseCode == 0) {
                    finish();
                } else if (responseCode == 2) {
                    reportProblem();
                }
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // If home icon is clicked return to main Activity
            case android.R.id.home:
                finish();
                break;

            default:
                break;
        }

        return true;
    }

    private String getFileName() {
        StringBuilder fileName = new StringBuilder();
        try {
//            String identifier = transaction_id == null ? DeviceInfo.getInstance().getSingleIMEI() : transaction_id;
            String identifier = transaction_id == null ? DeviceInfo.getInstance().get_imei() : transaction_id;
            if (DashboardLog.getInstance().isThisDest()) {
                fileName.append("DST");
            } else {
                fileName.append("SRC");
            }
            String timeStamp = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss").format(new Date());
            fileName.append("_" + identifier);
            fileName.append("_" + EMUtility.getDevicesCombinationDetails());
            fileName.append("_" + timeStamp);
            fileName.append(".txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName.toString().replaceAll(" ", "");
    }

    private String getSubject() {
        StringBuilder subject = new StringBuilder(FeatureConfig.getInstance().getProductConfig().getRapSubject());
        try {
            String userID = DashboardLog.getInstance().geteDeviceSwitchSession().getUserId();
            if (!TextUtils.isEmpty(userID) && !"-1".equalsIgnoreCase(userID)) {
                subject.append(" - " + DashboardLog.getInstance().geteDeviceSwitchSession().getUserId());
            }
            String storeID = DashboardLog.getInstance().geteDeviceSwitchSession().getStoreId();
            if (!TextUtils.isEmpty(storeID)&& !"-1".equalsIgnoreCase(storeID)) {
                subject.append(" - " + DashboardLog.getInstance().geteDeviceSwitchSession().getStoreId());
            }
            subject.append(" - " + Constants.COMPANY_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return subject.toString();
    }
}
