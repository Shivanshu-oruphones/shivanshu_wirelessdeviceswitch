package com.pervacio.wds.custom;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.ui.EasyMigrateActivity;
import com.pervacio.wds.custom.imeireader.IMEIReadActivity;
import com.pervacio.wds.custom.utils.CommonUtil;


public class IMEIReadChildActivity extends IMEIReadActivity {

    private static final String TAG = "IMEIReadChildActivity";

    public static void startActivity(Activity activity) {
        DLog.log("enter startActivity in IMEIReadChildActivity");
       startActivity(activity,IMEIReadChildActivity.class);
    }

    public void startDiagnostics(String imei) {
        CommonUtil.saveImeiInPrefs(imei);
//        LogUtil.printLog(TAG, " After result : " + imei);
        DLog.log( " After result : " + imei);
//        PinValidationActivity.startActivity(this,true,false);

//        Intent intent = new Intent(this, EasyMigrateActivity.class);
////        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
//        startActivity(intent);
//        finish();

        /*LoginActivity.startActivity(this,true,false);
        finish();*/

        startActivity(new Intent(IMEIReadChildActivity.this, EasyMigrateActivity.class));
        finish();

    }
}

