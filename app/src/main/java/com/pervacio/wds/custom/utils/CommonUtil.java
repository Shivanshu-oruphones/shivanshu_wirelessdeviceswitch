package com.pervacio.wds.custom.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.custom.APPI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by Pervacio on 19-06-2017.
 */

public class CommonUtil implements Serializable {

    private static CommonUtil commonUtil = null;
    ArrayList<AsyncTask> runningAsyncTask = new ArrayList<>();
    private String mWiFiPeerPassphrase;
    private String mWiFiPeerSSID;
    private InetAddress mWiFiPeerAddress;
    private String mCryptoEncryptPass;
    private int mFrequency;
    private InetAddress remoteDeviceIpAddress;
    private long backupStartedTime = 0;
    private int linkSpeed = 0;
    private long transferSpeed = 0;
    private int selectedMediaCount = 0;
    private int selectedDataTypeCount = 0;
    private int restoredMediaCount = 0;
    private int migrationStatus = Constants.REVIEW_INPROGRESS;
    private boolean isMigrationDone = false;
    private boolean isGroupOwner = true;
    private boolean remoteDeviceDualBand = false;
    private boolean isSource = false;
    private String networkType = "P2P";
    private long initialEstimation = 0;
    static EMGlobals emGlobals = new EMGlobals();
    private String deviceAddress;

    private String deviceNameP2P = "";
    private String remoteDeviceNameP2P = "";
    private String deviceAPILevel = "";

    public String getRemoteDeviceAPILevel() {
        return remoteDeviceAPILevel;
    }

    public void setRemoteDeviceAPILevel(String remoteDeviceAPILevel) {
        this.remoteDeviceAPILevel = remoteDeviceAPILevel;
    }

    private String remoteDeviceAPILevel = "";

    public String getDeviceAPILevel() {
        return deviceAPILevel;
    }

    public void setDeviceAPILevel(String deviceAPILevel) {
        this.deviceAPILevel = deviceAPILevel;
    }

    public String getRemoteDeviceModel() {
        return remoteDeviceModel;
    }

    public void setRemoteDeviceModel(String remoteDeviceModel) {
        this.remoteDeviceModel = remoteDeviceModel;
    }

    private String remoteDeviceModel="";

    public String getRemoteDeviceAddress() {
        return remoteDeviceAddress;
    }

    public void setRemoteDeviceAddress(String remoteDeviceAddress) {
        this.remoteDeviceAddress = remoteDeviceAddress;
    }

    private String remoteDeviceAddress;
    private long datatypesTobeTransferred = 0;
    private long messageSelectionFrom = 0;

    private CommonUtil() {
    }

    public static CommonUtil getInstance() {
        if (commonUtil == null)
            commonUtil = new CommonUtil();
        return commonUtil;
    }

    public long getInitialEstimation() {
        return initialEstimation;
    }

    public void setInitialEstimation(long initialEstimation) {
        this.initialEstimation = initialEstimation;
    }

    public void addToDataTobeTransferred(int count) {
        datatypesTobeTransferred |= count;
    }


    public void setSelectedMediaCount(int count) {
        selectedMediaCount += count;
        DLog.log(" selectedMediaCount : "+selectedMediaCount);
    }

    public int getSelectedMediaCount() {
        return selectedMediaCount;
    }

    public synchronized void setRestoredMediaCount(int count) {
        restoredMediaCount += count;
    }

    public int getRestoredMediaCount() {
        return restoredMediaCount;
    }
    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }
    public boolean isSource() {
        return isSource;
    }

    public void setSource(boolean source) {
        isSource = source;
    }

    public boolean isRemoteDeviceDualBand() {
        return remoteDeviceDualBand;
    }

    public void setRemoteDeviceDualBand(boolean remoteDeviceDualBand) {
        this.remoteDeviceDualBand = remoteDeviceDualBand;
    }

    public boolean isGroupOwner() {
        return isGroupOwner;
    }

    public void setGroupOwner(boolean groupOwner) {
        isGroupOwner = groupOwner;
    }

    public long getTransferSpeed() {
        return transferSpeed;
    }

    //Linkspeed in bytes/sec
    public void setTransferSpeed(long transferSpeed) {
        this.transferSpeed = transferSpeed;
    }

    public int getMigrationStatus() {
        return migrationStatus;
    }

    public void setMigrationStatus(int migrationStatus) {
        DLog.log(" setMigrationStatus : " + migrationStatus);
        this.migrationStatus = migrationStatus;
    }

    public boolean isMigrationDone() {
        return isMigrationDone;
    }

    public void setMigrationDone(boolean migrationDone) {
        isMigrationDone = migrationDone;
    }

    public void setRunningAsyncTask(AsyncTask asyncTask) {
        runningAsyncTask.add(asyncTask);
    }

    public void cleanRunningAsyncTask() {
        DLog.log("<< cleanRunningAsyncTask");
        for (AsyncTask asyncTask : runningAsyncTask) {
            try {
                asyncTask.cancel(true);
            } catch (Exception ex) {
                DLog.log("Exception while closing Task : " + ex);
            }
        }
        DLog.log("cleanRunningAsyncTask >>");
    }

    public void cleanRunningThreads(){
        DLog.log("<< cleanRunningThreads");
        try {
            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
            for (Thread thread : threadArray) {
                try {
                    thread.interrupt();
                } catch (Exception e) {
                    DLog.log(e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        DLog.log("cleanRunningThreads >>");
    }

    public int getLinkSpeed() {
        return linkSpeed;
    }

    //Linkspeed in Mbps

    public void setLinkSpeed(int linkSpeed) {
        this.linkSpeed = linkSpeed;
    }

    public InetAddress getRemoteDeviceIpAddress() {
        return remoteDeviceIpAddress;
    }

    public void setRemoteDeviceIpAddress(InetAddress remoteDeviceIpAddress) {
        this.remoteDeviceIpAddress = remoteDeviceIpAddress;
        PreferenceHelper.getInstance(emGlobals.getmContext()).putObjectAsJson("remote_ipaddress", remoteDeviceIpAddress);
    }

    public long getBackupStartedTime() {
        return backupStartedTime;
    }

    public void setBackupStartedTime(long backupStartedTime) {
        this.backupStartedTime = backupStartedTime;
    }

    public int getmFrequency() {
        return mFrequency;
    }

    public void setmFrequency(int mFrequency) {
        this.mFrequency = mFrequency;
    }

    public String getmWiFiPeerPassphrase() {
        return mWiFiPeerPassphrase;
    }

    public void setmWiFiPeerPassphrase(String mWiFiPeerPassphrase) {
        this.mWiFiPeerPassphrase = mWiFiPeerPassphrase;
    }

    public String getmWiFiPeerSSID() {
        return mWiFiPeerSSID;
    }

    public void setmWiFiPeerSSID(String mWiFiPeerSSID) {
        this.mWiFiPeerSSID = mWiFiPeerSSID;
    }

    public InetAddress getmWiFiPeerAddress() {
        return mWiFiPeerAddress;
    }

    public void setmWiFiPeerAddress(InetAddress mWiFiPeerAddress) {
        this.mWiFiPeerAddress = mWiFiPeerAddress;
        PreferenceHelper.getInstance(emGlobals.getmContext()).putObjectAsJson("remote_ipaddress", mWiFiPeerAddress);
    }

    public String getmCryptoEncryptPass() {
        return mCryptoEncryptPass;
    }

    public void setmCryptoEncryptPass(String mCryptoEncryptPass) {
        this.mCryptoEncryptPass = mCryptoEncryptPass;
    }

    public long getDatatypesTobeTransferred() {
        return datatypesTobeTransferred;
    }

    public void setDatatypesTobeTransferred(long datatypesTobeTransferred) {
        this.datatypesTobeTransferred = datatypesTobeTransferred;
    }

    public long getMessageSelectionFrom() {
        return messageSelectionFrom;
    }

    public void setMessageSelectionFrom(long messageSelectionFrom) {
        this.messageSelectionFrom = messageSelectionFrom;
    }

    public void pauseMigration(){
        DLog.log("***Migration is paused***");
//        Constants.stopMigration = true;
    }

    public void resumeMigration(){
        DLog.log("***Migration is resumed***");
        Constants.stopMigration = false;
        Object lock = EMUtility.getLockObject();
        synchronized (lock){
            lock.notifyAll();
        }
    }

    boolean autoPairingStarted = false;

    public boolean isAutoPairingStarted() {
        return autoPairingStarted;
    }

    public void setAutoPairingStatus(boolean value) {
        autoPairingStarted = value;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public boolean isMigrationInterrupted() {
        return mIsInterrupted;
    }

    private boolean mIsInterrupted = false;

    public void setMigrationInterrupted(boolean value) {
        mIsInterrupted = value;
    }


    public UsbMassStorageDevice getUsbDevice() {
        return usbDevice;
    }

    public void setUsbDevice(UsbMassStorageDevice usbDevice) {
        this.usbDevice = usbDevice;
    }

    private UsbMassStorageDevice usbDevice;


    public String getUsbBackupPath() {
        return usbBackupPath;
    }

    public void setUsbBackupPath(String usbBackupPath) {
        this.usbBackupPath = usbBackupPath;
    }

    private String usbBackupPath;

    public int getExternalStorageType() {
        return externalStorageType;
    }

    public void setExternalStorageType(int externalStorageType) {
        this.externalStorageType = externalStorageType;
    }

    private int externalStorageType;

    public String getDeviceNameP2P() {
        return deviceNameP2P;
    }

    public void setDeviceNameP2P(String deviceNameP2P) {
        this.deviceNameP2P = deviceNameP2P;
    }

    public String getRemoteDeviceNameP2P() {
        return remoteDeviceNameP2P;
    }

    public void setRemoteDeviceNameP2P(String remoteDeviceNameP2P) {
        this.remoteDeviceNameP2P = remoteDeviceNameP2P;
    }

    public void setTermsAndConditionsAccepted(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("TermsAndConditions", 1);
        editor.commit();
    }

    public boolean isTermsAndConditionsAccepted(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int status = sharedPreferences.getInt("TermsAndConditions", 0);
        return (status == 1);
    }
    public static void saveImeiInPrefs(String imei) {
        DLog.log("enter saveImeiInPrefs imei "+imei);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(emGlobals.getmContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("IMEI", imei);
        editor.commit();
        String imei1 = sharedPreferences.getString("IMEI", "");
        DLog.log("in saveImeiInPrefs red imei : "+imei1);
    }

    public static String getImeiFromPrefs() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(emGlobals.getmContext());
        String imei = sharedPreferences.getString("IMEI", "");
//        return sharedPreferences.getString("IMEI", "");
        DLog.log("in getImeiFromPrefs imei : "+imei);
        return imei;
    }

    public static boolean retrivedIMEIForAndroid10(){
        String imei = getImeiFromPrefs();
        DLog.log("Enter retrivedIMEIForAndroid10 imei "+imei);
        return imei != null && !imei.isEmpty();
    }

    public static GIFMovieView getNewGIFMovieView(Context context, String name) {
        InputStream stream = null;
        try {
            stream = context.getAssets().open(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        GIFMovieView gifMovieView = new GIFMovieView(context, stream);
        gifMovieView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        return gifMovieView;
    }


    /*---------MAC --*/
    public static  String getMACAddress(Context context) {
        WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = manager.getConnectionInfo();

        if("02:00:00:00:00:00".equals(wifiInf.getMacAddress())){
            String ret = null;
            try {
                ret= getAdressMacByInterface();
                if (ret != null){
                    return ret;
                } else {
                    ret = getAddressMacByFile(manager);
                    return ret;
                }
            } catch (IOException e) {
//                LogUtil.error("IOException in getMACAddress : "+e.getMessage());
                DLog.log("IOException in getMACAddress : "+e.getMessage());
            } catch (Exception e) {
//                LogUtil.error("Exception in getMACAddress : "+e.getMessage());
                DLog.log("Exception in getMACAddress : "+e.getMessage());
            }
        } else{
            return wifiInf.getMacAddress();
        }
        return "";
    }
    public static  String getMACAddressInIMEIFormat(Context context){
        String mac =  getMACAddress( context);
        if(!TextUtils.isEmpty(mac)){
            return mac.replaceAll(":","");
        }
        return null;
    }

    private static String getAdressMacByInterface(){
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (nif.getName().equalsIgnoreCase("wlan0")) {
                    byte[] macBytes = nif.getHardwareAddress();
                    if (macBytes == null) {
                        return "";
                    }

                    StringBuilder res1 = new StringBuilder();
                    for (byte b : macBytes) {
                        res1.append(String.format("%02X:",b));
                    }

                    if (res1.length() > 0) {
                        res1.deleteCharAt(res1.length() - 1);
                    }
                    return res1.toString();
                }
            }

        } catch (Exception e) {
//            LogUtil.error("MAC Address : Exception in getAdressMacByInterface : ");
            DLog.log("MAC Address : Exception in getAdressMacByInterface : ");
        }
        return null;
    }

    private static String getAddressMacByFile(WifiManager wifiMan) throws Exception {
        String ret;
        int wifiState = wifiMan.getWifiState();

        wifiMan.setWifiEnabled(true);
        File fl = new File("/sys/class/net/wlan0/address");
        FileInputStream fin = new FileInputStream(fl);
        ret = convertStreamToString(fin);
        fin.close();

        boolean enabled = WifiManager.WIFI_STATE_ENABLED == wifiState;
        wifiMan.setWifiEnabled(enabled);
        return ret;
    }
    public static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /* MAC - END --*/

    public static boolean isForManualIMEI() {
        for (String item : Constants.manualDevice) {
            if (item.toLowerCase().equalsIgnoreCase(Build.MODEL.toLowerCase().trim())) {
                return true;
            }
        }
        return isAndroidGoEdition(APPI.getAppContext());
    }
    public static class  DialogUtil{

        public static AlertDialog.Builder getAlert(Context context){
            return new AlertDialog.Builder(context, ThemeUtil.getColorsFromAttrs(context, R.attr.c_alertDialogTheme));
        }

        public static AlertDialog showAlertWithList(Context context , View customTitle, String[] list, DialogInterface.OnClickListener singleChoice, String positiveButtonText, DialogInterface.OnClickListener positiveListener){
            final AlertDialog.Builder builder2 = getAlert( context)
                    .setCustomTitle(customTitle)
                    .setCancelable(false)
                    .setSingleChoiceItems(list, -1, singleChoice);
            builder2.setPositiveButton(positiveButtonText,positiveListener);
            return builder2.create();
        }

        public static AlertDialog showAlert(Context context ,String title, String message,String positiveButtonText, DialogInterface.OnClickListener positiveListener){
            AlertDialog dialog = getAlert( context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(positiveButtonText, positiveListener).show();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }
        public static AlertDialog getAlert(Context context ,String title, String message, String positiveButtonText, DialogInterface.OnClickListener positiveListener,String negativeButtonText, DialogInterface.OnClickListener negativeListener){
            AlertDialog.Builder builder = getAlert( context)
                    .setTitle(title)
                    .setMessage(message);

            if(positiveListener!=null)
                builder.setPositiveButton(positiveButtonText, positiveListener);

            if(negativeListener!=null)
                builder.setNegativeButton(negativeButtonText, negativeListener);

            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }
    }

    public static boolean isAndroidGoEdition(Context context) {
        final String GMAIL_GO = "com.google.android.gm.lite";
        final String YOUTUBE_GO = "com.google.android.apps.youtube.mango";
        final String GOOGLE_GO = "com.google.android.apps.searchlite";
        final String ASSISTANT_GO = "com.google.android.apps.assistant";

        boolean isGmailGoPreInstalled = isPreInstalledApp(context, GMAIL_GO);
        boolean isYoutubeGoPreInstalled = isPreInstalledApp(context, YOUTUBE_GO);
        boolean isGoogleGoPreInstalled = isPreInstalledApp(context, GOOGLE_GO);
        boolean isAssistantGoPreInstalled = isPreInstalledApp(context, ASSISTANT_GO);

        if(isGoogleGoPreInstalled | isAssistantGoPreInstalled){
            return true;
        }
        if(isGmailGoPreInstalled && isYoutubeGoPreInstalled){
            return true;
        }

        return false;
    }

    private static boolean isPreInstalledApp(Context context, String packageName){
        try {
            PackageManager pacMan = context.getPackageManager();
            PackageInfo packageInfo = pacMan.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            if(packageInfo != null){
                //Check if comes with the image OS
                int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
                return (packageInfo.applicationInfo.flags & mask) != 0;
            }
        } catch (PackageManager.NameNotFoundException e) {
            //The app isn't installed
        }
        return false;
    }
}
