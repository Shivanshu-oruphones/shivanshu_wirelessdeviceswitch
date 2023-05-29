package com.pervacio.wds.custom.receivers;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.BatteryManager;

import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;

/**
 * This class used to inform to the user,when battery goes down
 * <p>
 * Created by Ravikumar D on 02-April-2018.
 */

public class BatteryStatusBroadcastReceiver extends BroadcastReceiver {
    private final static String BATTERY_LEVEL = "level";
    private final static int MIN_BATTERY_LEVEL = 40;
    public Context mContext;
    private AlertDialog batteryDialog;

    public BatteryStatusBroadcastReceiver(Context context) {
        mContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (intent != null) {
                int level = intent.getIntExtra(BATTERY_LEVEL, 0);
                if (level <= MIN_BATTERY_LEVEL) {
                    String connectedStatus = intent.getAction();
                    boolean isCharging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING;
                    DLog.log(connectedStatus + "Battery Level : " + level);
                    if (Intent.ACTION_BATTERY_CHANGED.equalsIgnoreCase(connectedStatus) && !isCharging) {
                        batteryLowEventHandler(level, true, this);
                    } else if (Intent.ACTION_POWER_CONNECTED.equals(connectedStatus)) {
                        batteryLowEventHandler(level, false, this);
                    }
                }
            }
        } catch (Exception e) {
            DLog.log(e);
        }
    }

    /**
     * Called if the battery level is below 40
     *
     * @param value shows the battery level
     * @param show  it returns true if the battery level is below 40.else return false.
     */
    public void batteryLowEventHandler(int value, boolean show, final BatteryStatusBroadcastReceiver batteryStatusBroadcastReceiver) {
        DLog.log("Battery Event Handler : " + value + " " + show);
        if (show && batteryDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(mContext.getString(R.string.low_battery));
            builder.setCancelable(false);
            builder.setMessage(mContext.getString(R.string.battery_level) + value + "%" + mContext.getString(R.string.low_battery_text_message));
            builder.setPositiveButton(mContext.getResources().getString(R.string.ept_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        mContext.unregisterReceiver(batteryStatusBroadcastReceiver);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            batteryDialog = builder.create();
            batteryDialog.show();
        } else if (!show) {
            if (batteryDialog != null) {
                batteryDialog.cancel();
                try {
                    mContext.unregisterReceiver(batteryStatusBroadcastReceiver);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
