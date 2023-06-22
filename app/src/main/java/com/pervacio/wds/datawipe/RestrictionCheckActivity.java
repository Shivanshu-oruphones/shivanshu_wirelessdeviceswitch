package com.pervacio.wds.datawipe;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.pervacio.wds.R;



import org.pervacio.onediaglib.diagtests.SdCardInsertionTest;
import org.pervacio.onediaglib.diagtests.TestResult;
import org.pervacio.onediaglib.diagtests.TestSdCardResult;
import org.pervacio.onediaglib.diagtests.TestSim;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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
    // SdCardInsertionTest sdCardInsertionTest;
    TestSdCardResult testSdCardResult;
    private ModeReceiverClass modeReceiverClass;
    public AccountManager accountManager;

    Button GoBack;
    Button Confirm;
    private AlertDialog alertDialog;

    Drawable decideRoot_img_current ;
    Drawable AirplaneMode_img_current ;
    Drawable Factory_img_current ;
    Drawable Battery_img_current ;
    Drawable SIM_img_current;
    Drawable SD_img_current ;
    private static final int PERMISSION_REQUEST_CODE = 1;


    Handler handler;
    String additionalResponse;
    public static String remotePlatform = "";

    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
            // Add more permissions here as needed
    };



    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restriction_check);

        checkPermissions();
        /*

        USED EXTERNAL LIBRARY AND IT'S CLASSES ONLY FOR TESTING SIM AND SD CARD

        TestResult testResult;
        TestSim testSim;
        SdCardInsertionTest sdCardInsertionTest;
        TestSdCardResult testSdCardResult;

        */

        //Getting Permissions for checking account



        //Getting ID's
        getIDs();

        //Creating ConfirmDialog While Checking
        createConfirmLoadingDialog();

        //Initiating TestSim Class
        testSim = new TestSim();

        //Initiating SdCardInsertionTest Class
        // sdCardInsertionTest = new SdCardInsertionTest();

        //performing All the checks
        performChecks();

        //For delaying time to display user the dialog
        handler = new Handler(Looper.getMainLooper());

        //Return boolean values of whether all checks are green or not.
        getStatusOfAllChecks();


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

        //Setting Airplane Mode for 1st time
        if(isAirplaneModeOn(this)){
            Airplane.setImageResource(R.drawable.ic_fail);
            AirplaneText.setText("Airplane Mode ON!");
        }
        else {
            AirplaneText.setText("Airplane Mode OFF");
            Airplane.setImageResource(R.drawable.ic_pass);

        }
        //Go_Back Button Logic
        GoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();

            }
        });

        //Confirm Button Logic
        Confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfirmButtonClicked();
            }
        });

    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Check if the GET_ACCOUNTS permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.GET_ACCOUNTS);
        }

        // Check if the GET_ACCOUNTS permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_SMS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SYNC_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_SYNC_SETTINGS);
        }



        // Request permissions if any are needed
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            // Permissions are already granted, you can proceed with getting the number of Google accounts
            isGoogleAccountSignedIn();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all permissions are granted
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // All permissions are granted, proceed with getting the number of Google accounts
                isGoogleAccountSignedIn();
            } else {
                // Some permissions are not granted, handle the situation accordingly
            }
        }
    }






    private Boolean getStatusOfAllChecks() {

        @SuppressLint("UseCompatLoadingForDrawables")
        Drawable pass_img = getResources().getDrawable(R.drawable.ic_pass);
        @SuppressLint("UseCompatLoadingForDrawables")
        Drawable fail_img = getResources().getDrawable(R.drawable.ic_fail);

        decideRoot_img_current = DeviceRooted.getDrawable();
        AirplaneMode_img_current = Airplane.getDrawable();
        Factory_img_current = FactoryReset.getDrawable();
        Battery_img_current = battery_percent.getDrawable();
        SIM_img_current = SIM_present.getDrawable();
        SD_img_current = SD_present.getDrawable();

        return decideRoot_img_current.getConstantState().equals(pass_img.getConstantState()) &&
                AirplaneMode_img_current.getConstantState().equals(pass_img.getConstantState()) &&
                Factory_img_current.getConstantState().equals(pass_img.getConstantState()) &&
                Battery_img_current.getConstantState().equals(pass_img.getConstantState()) &&
                SIM_img_current.getConstantState().equals(pass_img.getConstantState()) && ///////////////TESTING SIM AS FAIL ICON
                SD_img_current.getConstantState().equals(pass_img.getConstantState());

    }

    private void createConfirmLoadingDialog() {

        AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.datawipeconfirmprogressdialog,null));
        builder.setCancelable(false);
        alertDialog = builder.create();

    }

    private void ConfirmButtonClicked() {
        alertDialog.show();

        performChecks();

        if(getStatusOfAllChecks())
        {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    alertDialog.hide();

                    Intent myIntent = new Intent(getApplicationContext(), ConfirmDataWipeActivity.class);
                    startActivity(myIntent);

                }
            }, 1200);

        }
        else {
            alertDialog.hide();
            Toast.makeText(getApplicationContext(), "Check all the Validations!", Toast.LENGTH_SHORT).show();
        }
    }

    private void performChecks() {

        //Check if rooted
        if (!isDeviceRooted()) {
            DeviceRooted.setImageResource(R.drawable.ic_pass);
            DeviceRootedText.setText("Device Not Rooted");

        }
        else {
            DeviceRooted.setImageResource(R.drawable.ic_fail);
            DeviceRootedText.setText("Device Rooted!");
        }

        //Mode Receiver Class Checking for AIRPLANE_MODE
        modeReceiverClass = new ModeReceiverClass(this); // Pass the activity reference
        IntentFilter filter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(modeReceiverClass, filter);


        //Mode Receiver Class Checking for BATTERY_CHANGED
        IntentFilter filter2 = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(modeReceiverClass, filter2);

        //Google accounts fetching
        accountManager= AccountManager.get(this);

        //Check few settings few 1 second
        startUpdatingTextView();

    }


    public static boolean isAirplaneModeOn(Context context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

    public boolean isGoogleAccountSignedIn() {
        Account[] accounts = accountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        return accounts.length > 0;

    }

    public static boolean isSdCardPresent(Context context) {
        File[] externalStoragePaths = new File[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            externalStoragePaths = context.getExternalFilesDirs(null);
        }
        for (File path : externalStoragePaths) {
            if (path != null && !path.toString().equals(context.getExternalFilesDir(null).getAbsolutePath())) {
                return true;
            }
        }
        return false;
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
                            SIM_presentText.setText("SIM Card Present!");
                        } else {
                            SIM_present.setImageResource(R.drawable.ic_pass);
                            SIM_presentText.setText("SIM Card Not Present");
                        }

                        //SD fetching
                        // testSdCardResult = sdCardInsertionTest.performSdCardInsertionTest();

                        if (isSdCardPresent(getApplicationContext())) {
                            // SD card is present
                            // Perform your desired operations
                            Log.d("TARAKK", "run: "+"YOOOOOOOOOOOO");
                            SD_present.setImageResource(R.drawable.ic_fail);
                            SD_presentText.setText("SD Card Present!");
                        } else {
                            // SD card is not present
                            // Handle the absence of SD card
                            SD_present.setImageResource(R.drawable.ic_pass);
                            SD_presentText.setText("SD Card Not Present");
                        }


                        //CheckAccounts
                        if(isGoogleAccountSignedIn())
                        {
                            FactoryReset.setImageResource(R.drawable.ic_fail);
                            FactoryResetText.setText("Factory Reset Protection ON!");
                            FactoryResetText.setEnabled(true);
                            FactoryResetText.setClickable(true);

                        }
                        else {
                            FactoryReset.setImageResource(R.drawable.ic_pass);
                            FactoryResetText.setText("Factory Reset Protection OFF");
                            FactoryResetText.setEnabled(false);
                            FactoryResetText.setClickable(false);

                        }

                        //Setting COMFIRM Button
                        if(getStatusOfAllChecks())
                        {
                            Confirm.setClickable(true);
                            Confirm.setEnabled(true);
                            //Confirm.setBackgroundColor(getResources().getColor(R.color.backgroundColor));
                            Confirm.setAlpha(1);
                            Confirm.setBackgroundResource(R.color.backgroundColor);


                        }
                        else{
                            Confirm.setClickable(false);
                            Confirm.setEnabled(false);

                            Confirm.setBackgroundResource(R.color.backgroundColor_NotClickable);
                            Confirm.setAlpha(0.6f); // Set the transparency of the button to 60%


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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        context.startActivity(intent);
    }

    private void getIDs() {

        Log.d("TARAK", "getIDs: "+"NEW UPDATED DATA WIPE");

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

        GoBack = findViewById(R.id.goBack_button);
        Confirm = findViewById(R.id.confirm_button);
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

