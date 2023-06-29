package com.pervacio.wds.custom.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.ui.EasyMigrateActivity;
import com.pervacio.wds.custom.models.FeatureConfig;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.PreferenceHelper;

import java.util.Calendar;

/**
 * In WDS, once the transaction is over, we need to annoy the user to uninstall application.
 * And keep annoying him till he removes application.
 *
 * We achieve this by registering an PendingIntent through AlarmManager.
 *
 * Whenever the Intent is broadcast, we receive it and fire one more install to uninstall our app.
 *
 *
 * Created by Darpan Dodiya on 19-Sep-17.
 */

public class UninstallBroadcastReceiver extends BroadcastReceiver {

    private Context mContext;

    public UninstallBroadcastReceiver(Context context){
        this.mContext=context;
    }

    public UninstallBroadcastReceiver(){

    }

    /**
     * Receive PendingIntent broadcast and forcefully start new activity to show uninstall dialog.
     * @param context
     * @param intent
     */

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && action.equalsIgnoreCase(Intent.ACTION_POWER_DISCONNECTED)) {
                DLog.log("USB cable disconnected ");
                try {
                    EasyMigrateActivity easyMigrateActivity= (EasyMigrateActivity) mContext;
                    easyMigrateActivity.usbCableDisconnectionHandler(EasyMigrateActivity.DEVICE_DISCONNECTED,true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Constants.stopMigration = true;
            } else if (action != null && action.equalsIgnoreCase(Intent.ACTION_POWER_CONNECTED)) {
                DLog.log("USB cable connected ");
                try {
                    EasyMigrateActivity easyMigrateActivity= (EasyMigrateActivity) mContext;
                    easyMigrateActivity.usbCableDisconnectionHandler(EasyMigrateActivity.DEVICE_DISCONNECTED,false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Constants.stopMigration = false;
                Object lock = EMUtility.getLockObject();
                synchronized (lock) {
                    lock.notifyAll();
                }
            } else {
                int value = PreferenceHelper.getInstance(context).getIntegerItem("alarmCount");
                DLog.log("Uninstallation intent trigger count "+value);
                if (value < 3) {
                    PreferenceHelper.getInstance(context).putIntegerItem("alarmCount", ++value);
                    Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
                    uninstallIntent.setData(Uri.parse("package:" + context.getPackageName()));
                    uninstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(uninstallIntent);
                } else {
                    deleteUninstallAlarm(context);
                }
            }
        }}



    /**
     * Get AlarmManager instance from Context and register PendingIntent.
     *
     * The PendingIntent will be repeating since we want user to annoy every 5 minutes until he
     * uninstalls the application.
     * @param context
     */
    public static void startUninstallAlarm(Context context) {
        DLog.log("In startUninstallAlarm");

        if(!FeatureConfig.getInstance().getProductConfig().isUninstallRequired()){
            return;
        }

        //Start un-installation prompt only when we have uploaded logs
        if(PreferenceHelper.getInstance(context).getBooleanItem(Constants.PREF_UPLOAD_FINISHED)) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, UninstallBroadcastReceiver.class);
            PreferenceHelper.getInstance(context).putIntegerItem("alarmCount", 0);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
            //After one minute. First alarm will fire at 3 second later than current time.
            am.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis()+3*1000, 60*1000, pi);

            DLog.log("Started un-installation alarm");
        } else {
            DLog.log("Log upload not yet finished. Not starting alarm.");
        }
    }

    public static void deleteUninstallAlarm(Context context) {
//        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        Intent intent = new Intent(context, UninstallBroadcastReceiver.class);
//
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        am.cancel(pendingIntent);

        DLog.log("Cancelled un-installation alarm");
    }
}
