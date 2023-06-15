package com.pervacio.wds.datawipe;

import androidx.appcompat.app.AppCompatActivity;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.pervacio.wds.R;

public class ConfirmDataWipeActivity extends AppCompatActivity {

    private static final int REQUEST_ADMIN_PERMISSION = 123;

    Button GoBack;
    Button Confirm;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdminReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_data_wipe);

        getIDs();

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
                DataWipe();
            }
        });

    }

    private void DataWipe() {

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        boolean isSamsungS9 = manufacturer.equalsIgnoreCase("Samsung") && model.equalsIgnoreCase("SM-G960F");

        Log.d("DI_ALL", "DataWipe: +\n manufacturer -> "+manufacturer+"\n model -> "+model);

        if (isSamsungS9) {
            // Device is a Samsung Galaxy S9
            // Add your code here

            ///

            Log.d("DI_ALL", "DataWipe: Device :- S9");
            //Toast.makeText(this, "Device :- S9"+"\n", Toast.LENGTH_SHORT).show();
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminComponentName = new ComponentName(this, MyDeviceAdminReceiver.class);
            boolean isAdminActive = devicePolicyManager.isAdminActive(adminComponentName);

            if (!isAdminActive) {

                // Request device admin permission
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$DeviceAdminSettingsActivity"));
                startActivityForResult(intent, REQUEST_ADMIN_PERMISSION);

            } else {
                performDataWipe();
                // Device admin permission already granted
                // Proceed with your code here
            }
            
            

        }

        //All other devices
        else
        {

            //Toast.makeText(this, manufacturer+" "+model,Toast.LENGTH_LONG).show();
            devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            deviceAdminReceiver = new ComponentName(this, MyDeviceAdminReceiver.class);

            // Check if the app has been granted device admin privileges
            if (!devicePolicyManager.isAdminActive(deviceAdminReceiver))
            {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminReceiver);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable admin access to perform data wipe");

                startActivityForResult(intent, 1);
            } else {
                performDataWipe();
            }

        }




    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                performDataWipe();
            } else {
                Toast.makeText(this, "Admin access not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void performDataWipe() {
        if (devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
            devicePolicyManager.wipeData(0);
        }
    }

    private void getIDs() {
        GoBack = findViewById(R.id.goBack_button);
        Confirm = findViewById(R.id.confirm_button);
    }
}