package com.pervacio.wds.custom;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.custom.models.FeatureConfig;
import com.pervacio.wds.custom.receivers.UninstallBroadcastReceiver;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DeviceInfo;
import com.pervacio.wds.custom.utils.PreferenceHelper;

public class ThankYouActivity extends AppCompatActivity {
    Button exitButton;
    TextView tv;
    Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thankyoupage);
        exitButton = (Button) findViewById(R.id.thanks_exit_button);
        tv = (TextView) findViewById(R.id.step);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
//        mToolbar.setTitleTextAppearance(this, R.style.textStyle_title);
        mToolbar.setTitleTextAppearance(this, R.style.MyActionBarTitleText);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle(getString(R.string.app_name));
        mToolbar.setTitle(getString(R.string.app_name));

        if(isMessageAppLaunchNeeded()){
            tv.setText(R.string.text_suggestionDest2);
        }else{
            tv.setText(R.string.text_suggestionDest);
        }
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DLog.log("Enter onCLick ThankYouActivity");



                if (FeatureConfig.getInstance().getProductConfig().isUninstallRequired() &&
                        (!Constants.IS_MMDS && !BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT))) {
                    UninstallBroadcastReceiver.startUninstallAlarm(ThankYouActivity.this);
                }

                if(isMessageAppLaunchNeeded()){
                    Intent intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, "android.intent.category.APP_MESSAGING");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//Min SDK 15
                    startActivity(intent);
                }else{

                }

                finish();
//                System.exit(0);
            }
        });
    }

    @Override
    public void onBackPressed() {
    }
    private boolean isMessageAppLaunchNeeded() {
        DLog.log("Enter isMessageAppLaunchNeeded");
        boolean needed = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(getApplicationContext());
            String thisPackage = getPackageName();
            DLog.log("Enter isMessageAppLaunchNeeded thisPackage "+thisPackage+" defaultSmsApp "+defaultSmsApp);
            if(defaultSmsApp.equals(thisPackage)) {
                needed = true;
            }
        }
        return needed;
    }
}
