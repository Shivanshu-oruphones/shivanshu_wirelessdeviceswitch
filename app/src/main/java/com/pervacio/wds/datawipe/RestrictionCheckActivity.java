package com.pervacio.wds.datawipe;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.pervacio.wds.R;
import org.pervacio.onediaglib.diagtests.SdCardInsertionTest;
import org.pervacio.onediaglib.diagtests.TestResult;
import org.pervacio.onediaglib.diagtests.TestSdCardResult;
import org.pervacio.onediaglib.diagtests.TestSim;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

public class RestrictionCheckActivity extends AppCompatActivity {

    ImageView DeviceRooted;
    TextView DeviceRootedText;
    ImageView Airplane;
    TextView AirplaneText;
    ImageView FactoryReset;
    TextView FactoryResetText;
    ImageView battery_percent;
    TextView battery_percentText;
    ImageView SIM_present;
    TextView SIM_presentText;
    ImageView SD_present;
    TextView SD_presentText;
    TestResult testResult;
    TestSim testSim;
    SdCardInsertionTest sdCardInsertionTest;
    TestSdCardResult testSdCardResult;
    private ModeReceiverClass modeReceiverClass;
    public AccountManager accountManager;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restriction_check);

        //Getting ID's
        getIDs();

        //Check if rooted
        if (!isDeviceRooted()) {
            DeviceRooted.setImageResource(R.drawable.ic_pass);
        }

        //Mode Receiver Class Checking for AIRPLANE_MODE
        modeReceiverClass = new ModeReceiverClass(this); // Pass the activity reference
        IntentFilter filter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(modeReceiverClass, filter);


        //Mode Receiver Class Checking for BATTERY_CHANGED
        IntentFilter filter2 = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(modeReceiverClass, filter2);


        //Initiating TestSim Class
        testSim = new TestSim();

        //Initiating SdCardInsertionTest Class
        sdCardInsertionTest = new SdCardInsertionTest();

        //Google accounts fetching
        accountManager= AccountManager.get(this);

        //Check few settings few 1 second
        startUpdatingTextView();

        //When Google Accounts are not logged out you can click the given TEXT and it will redirect to google account settings.
        FactoryResetText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                @SuppressLint("UseCompatLoadingForDrawables") Drawable desiredDrawable = getResources().getDrawable(R.drawable.ic_fail);
                Drawable currentDrawable = FactoryReset.getDrawable();
                if (currentDrawable != null && currentDrawable.getConstantState() != null) {
                    if (currentDrawable.getConstantState().equals(desiredDrawable.getConstantState())) {

                        openGoogleAccountsSettings(getApplicationContext());
                    } else {

                        //Toast.makeText(RestrictionCheckActivity.this, "ALREADY TICKED", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });

    }

    public boolean isGoogleAccountSignedIn() {
        Account[] accounts = accountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        return accounts.length > 0;

    }
    private void startUpdatingTextView() {

        // Create a periodic task to update the TextView at a desired interval
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        //SIM Fetching
                        testResult = testSim.checkCurrentSimState();
                        if (testResult.getResultCode() == 0) {
                            SIM_present.setImageResource(R.drawable.ic_fail);
                        } else {
                            SIM_present.setImageResource(R.drawable.ic_pass);
                        }

                        //SD fetching
                        testSdCardResult = sdCardInsertionTest.performSdCardInsertionTest();
                        if (testSdCardResult.getResultCode() == 512) {
                            //var2 = "PASS";
                            SD_present.setImageResource(R.drawable.ic_pass);
                        } else {
                            //var2 = "FAIL";
                            SD_present.setImageResource(R.drawable.ic_fail);

                        }

                        //CheckAccounts
                        if(isGoogleAccountSignedIn())
                        {
                            FactoryReset.setImageResource(R.drawable.ic_fail);
                        }
                        else {
                            FactoryReset.setImageResource(R.drawable.ic_pass);
                        }



                    }
                });

            }
        };

        // Schedule the periodic task to run every desired interval
        Timer timer = new Timer();
        timer.schedule(timerTask, 0, 1000); // Update every 1 second (adjust the interval as needed)

    }

    public static void openGoogleAccountsSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
        intent.putExtra(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, new String[]{"com.google"});
        context.startActivity(intent);
    }

    private void getIDs() {

        //Getting ID's
        DeviceRooted = findViewById(R.id.rooted_img);
        DeviceRootedText = findViewById(R.id.rooted_txt);

        Airplane = findViewById(R.id.airplane_img);
        AirplaneText=findViewById(R.id.airplane_txt);

        FactoryReset=findViewById(R.id.factory_protection_off_img);
        FactoryResetText=findViewById(R.id.factory_protection_off_txt);

        battery_percent=findViewById(R.id.battery_img);
        battery_percentText=findViewById(R.id.battery_txt);

        SIM_present=findViewById(R.id.sim_img);
        SIM_presentText=findViewById(R.id.sim_txt);

        SD_present = findViewById(R.id.sd_img);
        SD_presentText=findViewById(R.id.sd_txt);
    }
    public static boolean isDeviceRooted() {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3();
    }

    private static boolean checkRootMethod1() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private static boolean checkRootMethod2() {
        String[] paths = { "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"};
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private static boolean checkRootMethod3() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "/system/xbin/which", "su" });
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (in.readLine() != null) return true;
            return false;
        } catch (Throwable t) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the receiver
        unregisterReceiver(modeReceiverClass);
    }

}

