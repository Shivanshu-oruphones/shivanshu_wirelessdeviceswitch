package com.pervacio.wds.datawipe;

import static android.telephony.TelephonyManager.SIM_STATE_ABSENT;
import static android.telephony.TelephonyManager.SIM_STATE_READY;
import static android.telephony.TelephonyManager.SIM_STATE_UNKNOWN;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.pervacio.wds.R;

import org.pervacio.onediaglib.diagtests.TestResult;
import org.pervacio.onediaglib.diagtests.TestSim;

public class ModeReceiverClass extends BroadcastReceiver {

    private Activity activity;
    static int ii=1;

    public ModeReceiverClass(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        //Initiating Airplane Mode For 1st Time only
        if(ii==1)
        {
            boolean isAirplaneModeOn = isAirplaneModeOn(activity);
            updateSymbolAirplane(isAirplaneModeOn);
            ii=2;
        }

        //Checking Airplane Mode For n Times
        if (intent.getAction().equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
            updateSymbolAirplane(isAirplaneModeOn);
        }

        //Checking Battery For n Times
        if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {

            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPercentage = (level / (float) scale) * 100;
            updateSymbolBattery(batteryPercentage);

        }

    }



    private void updateSymbolBattery(float batteryPercentage) {
        TextView batteryIMG_text = activity.findViewById(R.id.battery_txt);

        ImageView batteryIMG = activity.findViewById(R.id.battery_img);
        if (batteryPercentage<20) {
            batteryIMG_text.setText("Battery Charge Below 20%!");
            batteryIMG.setImageResource(R.drawable.ic_fail);
        } else {
            batteryIMG_text.setText("Minimum 20% Battery Charge Available");
            batteryIMG.setImageResource(R.drawable.ic_pass);
        }
    }


    private void updateSymbolAirplane(boolean isAirplaneModeOn) {
        ImageView airplane = activity.findViewById(R.id.airplane_img);
        TextView airplane_text = activity.findViewById(R.id.airplane_txt);
        if (isAirplaneModeOn) {
            airplane.setImageResource(R.drawable.ic_fail);
            airplane_text.setText("Airplane Mode ON!");
        } else {
            airplane_text.setText("Airplane Mode OFF");
            airplane.setImageResource(R.drawable.ic_pass);
        }
    }

    public static boolean isAirplaneModeOn(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Prior to Jelly Bean MR1, airplane mode setting was stored in Settings.System
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            // On Jelly Bean MR1 and above, airplane mode setting is stored in Settings.Global
            return Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

}

