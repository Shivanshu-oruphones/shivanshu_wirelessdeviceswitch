package com.pervacio.wds.custom;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.custom.utils.DeviceInfo;

public class LockRemoveinstructionActivity extends AppCompatActivity {

    private final static String GOOGLE_LOCK = "google lock";
    private final static String DEVICE_LOCK = "device lock";
    Toolbar mToolbar;
    private TextView head_tv;
    private TextView instruction_tv;
    private Button go_to_settings;
    private String lockType = null;
    private DeviceInfo mDeviceInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDeviceInfo = DeviceInfo.getInstance();
        setContentView(R.layout.activity_lock_removeinstruction);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitleTextAppearance(this, R.style.textStyle_title);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
        DLog.log("Lock onCreate middle");
        if (DeviceInfo.getInstance().isGoogleAccountPresent()) {
            ((TextView) mToolbar.findViewById(R.id.toolbar_title)).setText(getString(R.string.remove_google_account));
        } else {
            ((TextView) mToolbar.findViewById(R.id.toolbar_title)).setText(getString(R.string.remove_pin_lock));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        go_to_settings = findViewById(R.id.settings_btn_lr_inst);
        head_tv = findViewById(R.id.head_lr_inst);
        instruction_tv = findViewById(R.id.inst_view);
        if (mDeviceInfo.isGoogleAccountPresent()) {
            lockType = GOOGLE_LOCK;
        } else if (mDeviceInfo.isSecurityLockPresent()) {
            lockType = DEVICE_LOCK;
        } else {
            lockType = null;
        }
        if (lockType == null) {
            Intent intent = new Intent(LockRemoveinstructionActivity.this, ThankYouActivity.class);
            startActivity(intent);
            finish();
        } else {
            if (lockType.equalsIgnoreCase(GOOGLE_LOCK)) {
                ((TextView) mToolbar.findViewById(R.id.toolbar_title)).setText(getString(R.string.remove_google_account));
            } else if (lockType.equalsIgnoreCase(DEVICE_LOCK)) {
                ((TextView) mToolbar.findViewById(R.id.toolbar_title)).setText(getString(R.string.remove_pin_lock));
                head_tv.setText(R.string.device_lock_remove_head);
                instruction_tv.setText(R.string.device_lock_remove_instructions);
            }
            go_to_settings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    goToSettings();
                }
            });
        }
    }

    private void goToSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
    }

}
