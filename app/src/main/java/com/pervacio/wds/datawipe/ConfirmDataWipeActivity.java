package com.pervacio.wds.datawipe;

import androidx.appcompat.app.AppCompatActivity;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.pervacio.wds.R;

public class ConfirmDataWipeActivity extends AppCompatActivity {

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

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceAdminReceiver = new ComponentName(this, MyDeviceAdminReceiver.class);

        // Check if the app has been granted device admin privileges
        if (!devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminReceiver);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable admin access to perform data wipe");
            startActivityForResult(intent, 1);
        } else {
            performDataWipe();
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