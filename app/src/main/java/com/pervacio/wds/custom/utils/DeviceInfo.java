package com.pervacio.wds.custom.utils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.CalendarContract;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMDataType;
import com.pervacio.wds.app.EMFileMetaData;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.app.EMMigrateStatus;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.custom.models.EDeviceInfo;
import com.pervacio.wds.sdk.internal.sdcard.CMDSDCardFileAccess;

import org.pervacio.onediaglib.diagtests.TestGoogleAccounts;
import org.pervacio.onediaglib.diagtests.TestSecurityLock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Scanner;

import static com.pervacio.wds.custom.utils.Constants.EXCLUDE_WHATSAPP_MEDIA;
import static com.pervacio.wds.custom.utils.Constants.EXCLUDE_SDCARD_MEDIA;
/**
 * Created by Surya Polasanapalli
 */
public class DeviceInfo {
    static EMGlobals emGlobals = new EMGlobals();
    private String _make = null;
    private String _imei = null;
    private String _carrierName = null;
    private String _serialnumber = null;
    private final String TAG = "Deviceinfo";
    private long mTotalStorage = -1;
    private long mAvailableStorage = -1;

    public synchronized Queue<EMFileMetaData> getFilesQueue() {
        return filesQueue;
    }

    synchronized public EMFileMetaData getMetaData() {
        try {
            return filesQueue.remove();
        } catch (Exception e) {
            return null;
        }
    }

    public EMFileMetaData getFileMetaData() {
        try {
            return documentsQueue.remove();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private Queue<EMFileMetaData> filesQueue= new LinkedList<>();
    private Queue<EMFileMetaData> documentsQueue = null;

    private static DeviceInfo mDeviceinfo = null;

    private DeviceInfo() {
    }

    public static DeviceInfo getInstance() {
        if (mDeviceinfo == null) {
            mDeviceinfo = new DeviceInfo();
        }
        return mDeviceinfo;
    }


    public String get_carrierName() {
        try {
            TelephonyManager telephonyManager = ((TelephonyManager) emGlobals.getmContext().getSystemService
                    (Context.TELEPHONY_SERVICE));
            _carrierName = telephonyManager.getNetworkOperatorName();
        } catch (Exception e) {
            _carrierName = "";
        }
        return _carrierName;
    }

    public static long parseRAMDataLine(String line, String prefix) {
        try {
            if (line.startsWith(prefix)) {
                Scanner scanner = new Scanner(line);
                String pre = scanner.next();
                String value = scanner.next();
                return Long.parseLong(value);
            }
        } catch (Exception e) {
        }
        return -1;
    }


    private long getRaMDetails(boolean isTotal) {
        long memory = -1;
        try {
            if (Build.VERSION.SDK_INT > 16) {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager) emGlobals.getmContext().getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager != null) {
                    activityManager.getMemoryInfo(memoryInfo);
                }
                if (!isTotal) {
                    memory = memoryInfo.availMem / 1024L;
                } else {
                    memory = memoryInfo.totalMem / 1024L;
                }
            }
        } catch (Exception e) {
            DLog.log("getRaMDetails : "+e);
        }
        return memory;
    }

    //returns in KB

    public long getTotalRAMMemory() {
        return getRaMDetails(true);//getMemoryDetails(true);
    }

    //returns in KB

    public long getAvailableRAMMemory() {
        return getRaMDetails(false);//getMemoryDetails(false);
    }

    private long getMemoryDetails(boolean isTotal) {
        long totalRAM = -1, availRAM = -1;
        RandomAccessFile reader = null;
        try {
            reader = new RandomAccessFile("/proc/meminfo", "r");
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("MemTotal:")) {
                    long value = parseRAMDataLine(line, "MemTotal:");
                    if (value >= 0) {
                        totalRAM = value;
                        DLog.log("Total RAM : " + totalRAM);
                    }
                } else if (line.startsWith("MemFree:")) {
                    long value = parseRAMDataLine(line, "MemFree:");
                    if (value >= 0) {
                        availRAM = value;
                        DLog.log("Available RAM : " + availRAM);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (isTotal)
            return totalRAM;
        return availRAM;
    }

    public int getBatteryLevel() {
        int level = 0;
        try {
            Intent batteryIntent = emGlobals.getmContext().registerReceiver(null, new IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL,
                        -1);
            }
        } catch (Exception e) {
            DLog.log(e);
        }
        return level;
    }

    public long getAvailableInternalStorage() {
        if (mAvailableStorage == -1) {
            getStorageInfo(Build.MANUFACTURER);
        }
        return mAvailableStorage;

    }

    public long getTotalInternalStroage() {
        if (mTotalStorage == -1) {
            getStorageInfo(Build.MANUFACTURER);
        }
        return mTotalStorage;

    }

    public String get_serialnumber() {

        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            _serialnumber = (String) get.invoke(c, "sys.serialnumber", "Error");
            if (_serialnumber.equals("Error")) {
                _serialnumber = (String) get.invoke(c, "ril.serialnumber", "Error");

            }
        } catch (Exception e) {
            DLog.log(e.toString());
        }
        if (_serialnumber.equals("Error"))
            _serialnumber = Build.SERIAL;


//        try {
//            TelephonyManager telephonyManager = ((TelephonyManager) context.getSystemService
//                    (Context.TELEPHONY_SERVICE));
//            _serialnumber = telephonyManager.getSimSerialNumber();
//        } catch (Exception e) {
//            _serialnumber = "";
//        }
        return _serialnumber;
    }


    public String get_make() {
        String manufacturer = "";

        try {
            manufacturer = Build.MANUFACTURER;
            if (manufacturer != null || manufacturer.length() == 0) {
                manufacturer = getProp(emGlobals.getmContext(), "ro.product.manufacturer");
            } else if (manufacturer == null || manufacturer.length() == 0) {
                manufacturer = getBuildFieldUsingReflection("MANUFACTURER");
            } else if (manufacturer == null || manufacturer.length() == 0) {
                manufacturer = "Unknown";
            }
        } catch (Exception ex) {
            Log.e(TAG + " " + getClass().getEnclosingMethod().getName(), ex.getMessage());
        }

        _make = manufacturer;
        return _make;
    }

    public String get_model() {
        if (Build.MODEL.equalsIgnoreCase("2014818")) {
            return "HM 2LTE-IN";
        }
        return Build.MODEL;
    }


    public String get_imei() {

        DLog.log("enter get_imei");

        if(Build.VERSION.SDK_INT >= Constants.IMEI_CHECK_MIN_APILEVEL ) {
            DLog.log("enter get_imei Build.VERSION.SDK_INT "+Build.VERSION.SDK_INT);
            return CommonUtil.getImeiFromPrefs();
        }
        TelephonyManager _telephonyMgr = (TelephonyManager) emGlobals.getmContext()
                .getSystemService(Context.TELEPHONY_SERVICE);

        if (Build.VERSION.SDK_INT < 20) {
            _imei = getSingleSimIMEINo(_telephonyMgr);
        } else {
            if (Build.VERSION.SDK_INT == 20 && Build.VERSION.RELEASE.equalsIgnoreCase("l")) {
                _imei = getSingleSimIMEINo(_telephonyMgr);
            } else {
//                DLog.log("enter get_imei else 1");
                try {
                    _imei = getMutiSimeIMEINo(_telephonyMgr);
                } catch (Exception e) {
//                    DLog.log("enter get_imei else exception case");
                    _imei = getSingleSimIMEINo(_telephonyMgr);
                }
            }
        }
        return _imei;
    }

    @TargetApi(23)
    private String getMutiSimeIMEINo(TelephonyManager telephonyManager) {
        String imei = "";
        int phoneCount;
        try {
            phoneCount = telephonyManager.getPhoneCount();
        } catch (Exception e) {
            phoneCount = 1;
        }
        switch (phoneCount) {
            case 1:
                imei = getSingleSimIMEINo(telephonyManager);
                break;
            case 2:
                try {

                    String imsiSIM1 = telephonyManager.getDeviceId(0);
                    String imsiSIM2 = telephonyManager.getDeviceId(1);
                    imei = imsiSIM1 + ", " + imsiSIM2;

                } catch (Exception ex) {
                    imei = getSingleSimIMEINo(telephonyManager);
                }
                break;
            case 3:
            default:
                break;
        }
        return imei;
    }

    public String getSingleIMEI(){
        TelephonyManager telephonyManager = (TelephonyManager) emGlobals.getmContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        return getSingleSimIMEINo(telephonyManager);
    }


    private String getSingleSimIMEINo(TelephonyManager telephonyManager) {
        String imei = "";
        try {
            imei = telephonyManager.getDeviceId();
        } catch (Exception ex) {
            ex.getMessage();
        }
        if (imei == null || imei.length() == 0) {
            try {
                imei = getBuildFieldUsingReflection("SERIAL");
            } catch (Exception ex) {
                ex.getMessage();
            }
        }
        return imei;
    }


    private int getAndroidSDK_INT() {
        try {
            Field sdkField = android.os.Build.VERSION.class
                    .getDeclaredField("SDK_INT");
            sdkField.setAccessible(true);
            int value = (int) sdkField.getInt(android.os.Build.VERSION.class);
            return value;
        } catch (Exception ex) {
            Log.i("PVAINFO", "Exception in getAndroidSDK_INT :" + ex.getMessage());
        }
        return 3;// for CUPCAKE
    }

    public String getOSversion() {
        String osVersion = Build.VERSION.RELEASE;
        try {
            if (Build.BRAND.equalsIgnoreCase(Constants.PLATFORM_BLACKBERRY) && "qnx".equalsIgnoreCase(System.getProperty("os.name"))) {
                osVersion = getFirmwareVersion();
            }
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
        return osVersion;
    }

    private String getBuildNumber(Context ctx, String manufacturer) {
        try {
            if ("TCT".equalsIgnoreCase(manufacturer)) {
                if ("ONE TOUCH 6012A".equalsIgnoreCase(Build.MODEL)) {
                    String buildNumber = getProp(ctx, "ro_def_software_version");
                    if (buildNumber != null && buildNumber.length() > 0) {
                        return buildNumber;
                    }
                }
                String buildNumber = getProp(ctx, "ro_def_build_number");
                if (buildNumber != null && buildNumber.length() > 0) {
                    return buildNumber;
                }
            } else if ("TCL ALCATEL ONETOUCH".equalsIgnoreCase(manufacturer)) {
                String buildNumber = getProp(ctx, "def.tctfw.build.number");
                if (buildNumber != null && buildNumber.length() > 0) {
                    return buildNumber;
                }
            } else if ("TCL".equalsIgnoreCase(manufacturer)) {
                if (Build.MODEL.equalsIgnoreCase("ALCATEL ONETOUCH 6050A")) {
                    if (getAndroidSDK_INT() == Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        return "7DAG-UCG3";
                    } else if (getAndroidSDK_INT() == Build.VERSION_CODES.KITKAT) {
                        return "7DSO-UCO7";
                    }
                }
            }
        } catch (Exception ex) {
            Log.i("PVAINFO", "Exception in getBuildNumber: " + ex.getMessage());
        }
        return Build.DISPLAY;
    }

    public String getFirmwareVersion() {
        String manufacturer = get_make();
        String buildNumber = getBuildNumber(emGlobals.getmContext(), manufacturer);
        if (manufacturer.equalsIgnoreCase("Samsung")) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(
                        "/system/build.prop"));
                String hidVerProperty = "ro.build.hidden_ver=";
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith(hidVerProperty)) {
                        String hidVer = line.substring(hidVerProperty
                                .length());
                        if (hidVer.trim().length() > 0) {
                            hidVer = hidVer.trim();
                            if ("samsung".equalsIgnoreCase(manufacturer)
                                    && "Verizon"
                                    .equalsIgnoreCase(Build.BRAND)) {
                                if (hidVer.contains("_")) {
                                    String[] sr = hidVer.split("_");
                                    if (sr.length == 2
                                            && sr[0].length() >= 10
                                            && sr[1].length() == 3) {
                                        hidVer = sr[0];
                                    }
                                } else if (buildNumber.contains(".")) {
                                    String[] sr = buildNumber.split("\\.");
                                    if (sr.length == 2
                                            && sr[1].length() >= 10
                                            && hidVer.startsWith(sr[1])) {
                                        hidVer = sr[1];
                                    }
                                }
                            }
                            buildNumber = hidVer;
                        }
                        break;
                    }
                }
                in.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        String baseband = getBaseband();
        String swVersion = buildNumber;
        if ("Samsung".equalsIgnoreCase(manufacturer)
                && "Galaxy Nexus".equalsIgnoreCase(Build.MODEL)) {
            swVersion = buildNumber + "_" + baseband;
        } else if ("HTC".equalsIgnoreCase(manufacturer)) {
            String prodVersion = getProp(emGlobals.getmContext(), "ro.product.version");
            if (prodVersion != null && prodVersion.length() > 0) {
                swVersion = prodVersion;
            }
        } else if ("LGE".equalsIgnoreCase(manufacturer)) {
            String lgeswVersion = getProp(emGlobals.getmContext(), "ro.lge.swversion");
            if (lgeswVersion != null && lgeswVersion.length() > 0) {
                swVersion = lgeswVersion;
            }
        } else if ("TCT".equalsIgnoreCase(manufacturer)) {
            String tctswVersion = getProp(emGlobals.getmContext(),
                    "ro.def.philips.software.svn");
            if (tctswVersion != null && tctswVersion.length() > 0) {
                swVersion = tctswVersion;
                buildNumber = tctswVersion;
            }
        }
        return swVersion;
    }


    private String getBaseband() {
        String baseband = getBuildFieldUsingReflection("RADIO");
        if (baseband == null || baseband.length() == 0 || baseband.equalsIgnoreCase("unknown")) {
            baseband = getProp(emGlobals.getmContext(), "gsm.version.baseband");
        }
        if (baseband == null || baseband.equalsIgnoreCase("unknown")) {
            baseband = "";
        }
        return baseband;
    }

    public String getDeviceLanguage(){
        return Locale.getDefault().getLanguage();
    }

    private String getBuildFieldUsingReflection(String field) {
        String value = "";
        try {
            Field buildField = Build.class.getDeclaredField(field);
            buildField.setAccessible(true);
            value = (String) buildField.get(Build.class);
        } catch (Exception ex) {
            DLog.log(ex);
        }
        return value;
    }

    private String getProp(Context ctx, String key) {
        String ret = "";
        try {
            ClassLoader cl = ctx.getClassLoader();
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");
            Class[] paramTypes = new Class[1];
            paramTypes[0] = String.class;
            Method get = SystemProperties.getMethod("get", paramTypes);
            Object[] params = new Object[1];
            params[0] = new String(key);
            ret = (String) get.invoke(SystemProperties, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public String getBuildNumber() {

        String manufacturer = this.get_make();
        try {
            if ("TCT".equalsIgnoreCase(manufacturer)) {
                if ("ONE TOUCH 6012A".equalsIgnoreCase(Build.MODEL)) {
                    String buildNumber = getProp(emGlobals.getmContext(), "ro_def_software_version");
                    if (buildNumber != null && buildNumber.length() > 0) {
                        return buildNumber;
                    }
                }
                String buildNumber = getProp(emGlobals.getmContext(), "ro_def_build_number");
                if (buildNumber != null && buildNumber.length() > 0) {
                    return buildNumber;
                }
            } else if ("TCL ALCATEL ONETOUCH".equalsIgnoreCase(manufacturer)) {
                String buildNumber = getProp(emGlobals.getmContext(), "def.tctfw.build.number");
                if (buildNumber != null && buildNumber.length() > 0) {
                    return buildNumber;
                }
            } else if ("TCL".equalsIgnoreCase(manufacturer)) {
                if (Build.MODEL.equalsIgnoreCase("ALCATEL ONETOUCH 6050A")) {
                    if (getAndroidSDK_INT() == Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        return "7DAG-UCG3";
                    } else if (getAndroidSDK_INT() == Build.VERSION_CODES.KITKAT) {
                        return "7DSO-UCO7";
                    }
                }
            }
        } catch (Exception ex) {
            Log.i("PVAINFO", "Exception in getBuildNumber: " + ex.getMessage());
        }
        return Build.DISPLAY;
    }

    private LinkedHashMap<String, String> getVoldMountPoints(LinkedHashMap<String, String> mountedPoints) {
        LinkedHashMap<String, String> voldMountPoints = new LinkedHashMap<String, String>();
        try {
            File voldFile = new File("/system/etc/internal_sd.fstab");// for
            // Huawei device(U8686-Prism II)
            if (!voldFile.exists()) {
                voldFile = new File("/system/etc/vold.fstab.nand");// ALCATEL
                // ONE TOUCH, Fierce/5020T
            }
            if (!voldFile.exists()) {
                voldFile = new File("/system/etc/vold.fstab");
            }
            if (voldFile.exists()) {
                Scanner scanner = new Scanner(voldFile);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("dev_mount")) {
                        String[] fields = line.split("\\s+");
                        if (fields.length > 4) {
                            String mntPath = fields[2];
                            if (mntPath.contains(":")) {
                                mntPath = mntPath.substring(0, mntPath.indexOf(":"));
                            }
                            if (!mountedPoints.containsKey(mntPath)) {
                                try {
                                    File file = new File(mntPath);
                                    if (!file.getAbsolutePath().equals(file.getCanonicalPath())) {
                                        mntPath = file.getCanonicalPath();
                                    }
                                } catch (Exception e) {
                                    DLog.log(e);
                                }
                            }
                            if (mountedPoints.containsKey(mntPath)) {
                                String mediaType = "";
                                for (int i = 4; i < fields.length; i++) {
                                    if (fields[i].contains("mmc_host")) {
                                        String type = getMediaPathType("/sys/" + fields[i]);
                                        if (type != null && type.length() > 0) {
                                            mediaType = type;
                                            break;
                                        }
                                    }
                                }
                                if (voldMountPoints.containsKey(mntPath)) {
                                    String type = voldMountPoints.get(mntPath);
                                    if (!"SD".equalsIgnoreCase(type) && mediaType.length() > 0) {
                                        voldMountPoints.put(mntPath, mediaType);
                                    }
                                } else {
                                    voldMountPoints.put(mntPath, mediaType);
                                }
                            }
                        }
                    }
                }
            } else {
                voldFile = new File("/system/etc/vold.conf");
                if (voldFile.exists()) {
                    Scanner scanner = new Scanner(voldFile);
                    boolean structStarted = false;
                    String mntPath = "";
                    String mediaPath = "";
                    while (scanner.hasNext()) {
                        String line = scanner.nextLine();
                        line = line.trim();
                        if (structStarted) {
                            if (line.endsWith("}")) {
                                if (mntPath.length() > 0 && mediaPath.length() > 0
                                        && mountedPoints.containsKey(mntPath)) {
                                    String mediaType = "";
                                    String type = getMediaPathType("/sys/" + mediaPath);
                                    if (type != null && type.length() > 0) {
                                        mediaType = type;
                                    }
                                    voldMountPoints.put(mntPath, mediaType);
                                }
                                structStarted = false;
                                mntPath = "";
                                mediaPath = "";
                            } else if (line.startsWith("media_path") || line.startsWith
                                    ("mount_point")) {
                                String[] fields = line.split("\\s+");
                                if (fields.length > 1) {
                                    if (fields[0].equals("media_path")) {
                                        mediaPath = fields[1];
                                    } else {
                                        mntPath = fields[1];
                                    }
                                }
                            }
                        } else if (line.startsWith("volume_") && line.endsWith("{")) {
                            structStarted = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return voldMountPoints;
    }

    private LinkedHashMap<String, String> getMountPoints() {
        LinkedHashMap<String, String> mountPoints = new LinkedHashMap<String, String>();
        try {
            File mountFile = new File("/proc/mounts");
            if (mountFile.exists()) {
                Scanner scanner = new Scanner(mountFile);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (!line.startsWith("/dev/block/vold/")) {
                        line = line.replaceAll("//", "/");
                    }
                    if (line.startsWith("/dev/block/vold/")) {
                        String[] fields = line.split("\\s+");
                        if (fields.length > 2) {
                            String mntPath = fields[1];
                            String fsType = fields[2];
                            //                            Log.i("PVA", "mntpath: " + mntPath + "
                            // " + fsType);
                            if (!mountPoints.containsKey(mntPath)) {
                                mountPoints.put(mntPath, fsType);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            DLog.log(e);
        }
        return mountPoints;
    }

    private void getStorageInfo(String manu) {
        LinkedHashMap<String, String> mountedPaths = getMountPoints();
        String internalPath = "";
        String usbStoragePath = "";
        String sdCardPath = "";
        boolean fuseFS = false;
        if (mountedPaths.size() > 0) {
            LinkedHashMap<String, String> voldMountPoints = getVoldMountPoints(mountedPaths);
            if (voldMountPoints.size() > 0) {
                Iterator<String> iter = voldMountPoints.keySet().iterator();
                while (iter.hasNext()) {
                    String path = iter.next();
                    String mediaType = voldMountPoints.get(path);
                    if ("SD".equalsIgnoreCase(mediaType) || "SDIO".equalsIgnoreCase(
                            mediaType)/* for samsung M919 - 4.3 */) {
                        sdCardPath = path;
                    } else {
                        usbStoragePath = path;
                        String fsType = mountedPaths.get(path);
                        if ("fuse".equalsIgnoreCase(fsType)) {
                            fuseFS = true;
                        }
                    }
                }
            }
        }
        if (!fuseFS) {
            internalPath = Environment.getDataDirectory().getAbsolutePath();
        }
        String model = get_model();
        String androidVersion = Build.VERSION.RELEASE;
        if ("SGH-M919".equalsIgnoreCase(model) && "4.3".equalsIgnoreCase(androidVersion)) {
            // workaround for GS4 4.3 sdcard issue
            if (sdCardPath.length() == 0 && usbStoragePath.equalsIgnoreCase("/storage/extSdCard")) {
                sdCardPath = usbStoragePath;
                usbStoragePath = "";
            }
        }
        // check sdcard path from devices
        if (sdCardPath.length() == 0) {
            File fileCur;
            for (String sPathCur : Arrays.asList("ext_card", "external_sd", "external_SD",
                    "ext_sd", "external", "extSdCard",
                    "externalSdCard", "sdcard0", "sdcard1",
                    "sdcard", "sdcard-ext")) // external sdcard
            {
                fileCur = new File("/storage/", sPathCur);
                if (!fileCur.exists())
                    fileCur = new File("/mnt/", sPathCur);
                if (fileCur.isDirectory() && fileCur.canWrite()) {
                    sdCardPath = fileCur.getAbsolutePath();
                    break;
                }
            }
        }
        if ("SHARP".equalsIgnoreCase(manu)) {
            usbStoragePath = "/internal_sd";
        }
        if ("Sony".equalsIgnoreCase(manu)) {
            usbStoragePath = "/mnt/int_storage";
        }
        long internalTotal = 0, internalFree = 0;
        if (internalPath.length() > 0) {
            internalTotal = getMemorySize(internalPath, true);
            internalFree = getMemorySize(internalPath, false);
//            internalTotal += getMemorySize(usbStoragePath, true);
//            internalFree += getMemorySize(usbStoragePath, false);
            mTotalStorage = internalTotal;
            mAvailableStorage = internalFree;
        }
    }

    private long getMemorySize(String pathName, boolean total) {
        long memorySize = 0;
        try {
            File path = new File(pathName);
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            if (total) {
                memorySize = stat.getBlockCount() * blockSize / 1024L;
            } else {
                memorySize = stat.getAvailableBlocks() * blockSize / 1024L;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return memorySize;
    }

    private String getMediaPathType(String mediaPath) {
        String type = "";
        try {
            if (mediaPath.endsWith("mmc_host")) {
                try {
                    File mmcHost = new File(mediaPath);
                    if (mmcHost != null && mmcHost.exists() && mmcHost.isDirectory()) {
                        File[] mmcFolders = mmcHost.listFiles();
                        String mmcSubFolder = "";
                        int mmcSubfoldersCount = 0;
                        if (mmcFolders != null) {
                            for (File mmcFolder : mmcFolders) {
                                String subFolderName = mmcFolder.getName();
                                if (mmcFolder.isDirectory()
                                        && subFolderName.startsWith("mmc")) {
                                    mmcSubFolder = subFolderName;
                                    mmcSubfoldersCount++;
                                }
                            }
                        }
                        if (mmcSubfoldersCount == 1) {
                            mediaPath += "/" + mmcSubFolder;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            File mmc = new File(mediaPath);
            if (!mmc.exists()) {
                int lastSlash = mediaPath.lastIndexOf('/');
                if (lastSlash >= 0) {
                    mediaPath = mediaPath.substring(0, lastSlash);
                }
                mmc = new File(mediaPath);
            }
            if (mmc != null) {
                if (mmc.isDirectory()) {
                    File[] subMmcs = mmc.listFiles();
                    for (File subMmc : subMmcs) {
                        String subMmcName = subMmc.getName();
                        if (subMmcName.startsWith("mmc")) {
                            String typeFileName = mediaPath + "/" + subMmcName
                                    + "/" + "type";
                            File typeFile = new File(typeFileName);
                            if (typeFile.exists()) {
                                StringBuilder sb = getFileContents(typeFile);
                                type = sb.toString().trim();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return type;
    }

    private StringBuilder getFileContents(File typeFile) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(typeFile));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }
            in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return sb;
    }


    private long getTotalMediaSize(Cursor mCursor, int mFilePathColumn)
    {
        if (! mCursor.moveToFirst()||mFilePathColumn==-1)
        {
            // Log.d(TAG, "=== getTotalMediaSize, Could not move to first item");
            return 0;
        }

        long totalSize = 0;

        do
        {
            String filePath = mCursor.getString(mFilePathColumn);
            File file = new File(filePath);

            if (file.exists())
            {
                totalSize += file.length();
            }

        } while(mCursor.moveToNext());

        return totalSize;
    }

    private long getTotalMediaSize(Cursor mCursor, int mFilePathColumn, boolean excludeWhatsAppMedia, boolean excludeSDCardMedia, String sdCardPath, boolean aCountOnly)
    {
        long fileCount = 0;
        if (! mCursor.moveToFirst()||mFilePathColumn==-1)
        {
            // Log.d(TAG, "=== getTotalMediaSize, Could not move to first item");
            return 0;
        }
        long totalSize = 0;
        do
        {
            String filePath = mCursor.getString(mFilePathColumn);
            File file = new File(filePath);
            if ((excludeWhatsAppMedia && (filePath.toLowerCase().contains("whatsapp"))) || (excludeSDCardMedia && (sdCardPath!=null) && filePath.contains(sdCardPath)))
            {
/*                if(excludeWhatsAppMedia && (filePath.toLowerCase().contains("whatsapp")))
                    DLog.log("excludeWhatsAppMedia case " + "excluding whatsapp media filepath "+filePath);
                if(excludeSDCardMedia && (sdCardPath!=null) && filePath.contains(sdCardPath))
                    DLog.log("excludesdCardMedia case " + "excluding sdcard media filepath "+filePath);*/
            }else {
//                DLog.log("excludesdCardMedia or excludeWhatsAppMedia  else case" + " filepath "+filePath);

                if (file.exists()) {
                    totalSize += file.length();
                    fileCount=fileCount+1;
                }
            }
        } while(mCursor.moveToNext());
        if (aCountOnly){
            return fileCount;
        }else{
            return totalSize;
        }
    }
    /**
     * This method to get the details of content,i.e; count or size.
     * Here user have to prase datatype and count or size (true or false)
     * size will be returned in KB for Media.
     * <p>
     * mDataType  - integer - selected data type integer.
     * aCountonly - false - retuns the size of the selected data in Bytes
     * aCountonly   - true  - retuns the count of the selected data
     *
     * @param mDataType
     * @param aCountOnly
     */

    public long getContentDetails(int mDataType, boolean aCountOnly) {
        Uri uri = null;
        int mFilePathColumn = -1;
        long value = -1;
        Cursor mCursor = null;
        String whereQuery = null;
        String sdCardPath="";

        if (mDataType == EMDataType.EM_DATA_TYPE_PHOTOS) {
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            whereQuery = MediaStore.Images.Media.SIZE + " != 0";
        } else if (mDataType == EMDataType.EM_DATA_TYPE_VIDEO) {
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            whereQuery = MediaStore.Video.Media.SIZE + " != 0";
        } else if (mDataType == EMDataType.EM_DATA_TYPE_MUSIC) {
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            whereQuery = MediaStore.Audio.Media.SIZE + " != 0";
        } else if (mDataType == EMDataType.EM_DATA_TYPE_CONTACTS)
            uri = android.provider.ContactsContract.Contacts.CONTENT_URI;
        else if (mDataType == EMDataType.EM_DATA_TYPE_CALL_LOGS)
            uri = CallLog.Calls.CONTENT_URI;
        else if (mDataType == EMDataType.EM_DATA_TYPE_CALENDAR) {
            uri = android.provider.CalendarContract.Events.CONTENT_URI;
            whereQuery = CalendarContract.Events.DELETED + " != 1";
        }
        else if (mDataType == EMDataType.EM_DATA_TYPE_SMS_MESSAGES) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Constants.MMS_SUPPORT) {
                uri = Telephony.MmsSms.CONTENT_CONVERSATIONS_URI;
            } else {
                uri = Uri.parse("content://sms");
            }
            String smsDbType = "type";
            whereQuery = smsDbType + " = '" + 1 + "' OR " +smsDbType + " = '" + 2 + "' OR " + smsDbType + " = '" + 3 + "'"; //filtering only inbox, sent and drafts. SMS_DB_TYPE_INBOX = 1;  SMS_DB_TYPE_SENT  = 2;  SMS_DB_TYPE_DRAFT  = 3;
        }
        sdCardPath  = CMDSDCardFileAccess.getSDCardpath(emGlobals.getmContext());
        try {
            mCursor = emGlobals.getmContext().getContentResolver().query(uri,
                    null,       // projection
                    whereQuery, // where - to get Specific rows.
                    null,       // arguments - none
                    null        // ordering - doesn't matter
            );
            if (mCursor != null) {
                if (mDataType == EMDataType.EM_DATA_TYPE_PHOTOS)
                    mFilePathColumn = mCursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                else if (mDataType == EMDataType.EM_DATA_TYPE_VIDEO)
                    mFilePathColumn = mCursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA);
                else if (mDataType == EMDataType.EM_DATA_TYPE_MUSIC)
                    mFilePathColumn = mCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
                if (mDataType == EMDataType.EM_DATA_TYPE_PHOTOS || mDataType == EMDataType.EM_DATA_TYPE_VIDEO || mDataType == EMDataType.EM_DATA_TYPE_MUSIC)
                value = getTotalMediaSize(mCursor, mFilePathColumn,EXCLUDE_WHATSAPP_MEDIA,EXCLUDE_SDCARD_MEDIA,sdCardPath,aCountOnly);
                else
                value = mCursor.getCount();
                if (aCountOnly) {
                    EMMigrateStatus.addContentDetails(mDataType, value);
                }
            }else{
                value = 0;
                DLog.log("cursor is null for " + mDataType + " returning value  as " + value);
            }
        } catch (Exception ex) {
            DLog.log(ex);
        } finally {
            if (mCursor != null)
                mCursor.close();
            return value;
        }
    }

    public long getDocumentDetails(boolean aCountly) {
        long value = 0L;
        if (documentsQueue == null) {
            documentsQueue = new LinkedList<>();
            getDocuments(Environment.getExternalStorageDirectory());
        }
        if (documentsQueue != null) {
            if (aCountly) {
                value = documentsQueue.size();
            } else {
                for (EMFileMetaData emFileMetaData : documentsQueue) {
                    value += emFileMetaData.mSize;
                }
            }
        }
        return value;
    }


    private boolean isDualBandSupported(WifiManager wifiManager) {
        boolean isDualBandSupported = false;
        if(Constants.PLATFORM.equalsIgnoreCase(Constants.PLATFORM_BLACKBERRY)){         //Returning true As BB cant host in WifiDirect.
            isDualBandSupported = true;
        }else {
            try {
                Method method = wifiManager.getClass().getDeclaredMethod("isDualBandSupported");
                method.setAccessible(true);
                isDualBandSupported = (Boolean) method.invoke(wifiManager);
                DLog.log("isDualBandSupported " + isDualBandSupported);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return isDualBandSupported;
    }

    public boolean is5GhzSupported() {//throws Exception {
        WifiManager wifiManager = (WifiManager) emGlobals.getmContext().getSystemService(Context.WIFI_SERVICE);
        if(wifiManager == null){
            return false;
            //throw new Exception("[CRITICAL] Unable to get Systemservice for WifiManager");
        }
        boolean is5GHZSupported = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                is5GHZSupported = wifiManager.is5GHzBandSupported() || isDualBandSupported(wifiManager);
            } else {
                is5GHZSupported = isDualBandSupported(wifiManager);
            }
            DLog.log("5GHZ supported " + is5GHZSupported);
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
        return is5GHZSupported;
    }


    /**
     * Get the WiFi-Direct feature availability
     * @return  {@code true} if the WiFi-Direct feature is available,{@code false} if the WiFi feature is not available
     */
    public boolean isP2PSupported() {
        boolean isP2PSupported = false;
        try {
            isP2PSupported = emGlobals.getmContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT);
            DLog.log("enter isP2PSupported isP2PSupported "+isP2PSupported);
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
        return isP2PSupported;
    }

    public void prepareFilesQueue(int mDataType){
        DLog.log("Enter prepareFilesQueue mDataType "+mDataType);
        Uri uri=null;
        Cursor mCursor=null;
        int mFilePathColumn=0;
        String whereQuery = null;
        String sdCardPath = CMDSDCardFileAccess.getSDCardpath(emGlobals.getmContext());
        if (mDataType == EMDataType.EM_DATA_TYPE_PHOTOS) {
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            whereQuery = MediaStore.Images.Media.SIZE +" != 0";
        }else if (mDataType == EMDataType.EM_DATA_TYPE_VIDEO){
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            whereQuery = MediaStore.Video.Media.SIZE +" != 0";
        }else if (mDataType == EMDataType.EM_DATA_TYPE_MUSIC) {
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            whereQuery = MediaStore.Audio.Media.SIZE+" != 0";
        }

        try {
            mCursor = emGlobals.getmContext().getContentResolver().query(uri,
                    null,       // projection
                    whereQuery, // where - to get Specific rows.
                    null,       // arguments - none
                    null        // orderng - doesn't matter
            );

            if (mCursor != null && mDataType == EMDataType.EM_DATA_TYPE_PHOTOS)
                mFilePathColumn = mCursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            else if (mCursor != null && mDataType == EMDataType.EM_DATA_TYPE_VIDEO)
                mFilePathColumn = mCursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA);
            else if (mCursor != null && mDataType == EMDataType.EM_DATA_TYPE_MUSIC)
                mFilePathColumn = mCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);

            if (mCursor != null) {
                while (mCursor.moveToNext()) {
                    String filePath = mCursor.getString(mFilePathColumn);
                    File file = new File(filePath);
                    if ((EXCLUDE_WHATSAPP_MEDIA && (filePath.toLowerCase().contains("whatsapp"))) || (EXCLUDE_SDCARD_MEDIA && (sdCardPath!=null) && filePath.contains(sdCardPath)))
                    {
/*                        if(EXCLUDE_WHATSAPP_MEDIA && (filePath.toLowerCase().contains("whatsapp")))
                            DLog.log("excludeWhatsAppMedia case prepareFilesQueue " + "excluding whatsapp media filepath "+filePath);
                        if(EXCLUDE_SDCARD_MEDIA && (sdCardPath!=null) && filePath.contains(sdCardPath))
                            DLog.log("excludesdCardMedia case prepareFilesQueue " + "excluding sdcard media filepath "+filePath);*/
                    } else {
                        //DLog.log("excludesdCardMedia or excludeWhatsAppMedia  else case prepareFilesQueue" + " filepath "+filePath);
                        try {
                            if (file.exists() && file.canRead()) {
                                filesQueue.add(getFileInfoString(file, mDataType));
                            }
                        } catch (Exception e) {
                            DLog.log("Exception while adding file to Queue : " + e);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            DLog.log("Exception in prepareFilesQueue : " + ex);
        } finally {
            if (mCursor != null)
                mCursor.close();
        }
    }


    private EMFileMetaData getFileInfoString(File file,int type){
        EMFileMetaData emFileMetaData = null;
        try {
            emFileMetaData = new EMFileMetaData();
            emFileMetaData.mDataType = type;
            emFileMetaData.mFileName = file.getName();
            emFileMetaData.mSize = file.length();
            emFileMetaData.mSourceFilePath = file.getAbsolutePath();
            String relativePath = file.getAbsolutePath();
            if (relativePath.startsWith(Environment.getExternalStorageDirectory().toString())) {
                relativePath = file.getAbsolutePath().replace(Environment.getExternalStorageDirectory().toString(), "");
            } else {
                //DLog.log("Folder structure Mismatch : " + file.getAbsolutePath());
                relativePath = null;
            }
            emFileMetaData.mRelativePath = relativePath;
        } catch (Exception e) {
            DLog.log("Exception while generating file meta data : " + e.getMessage());
        }
        return emFileMetaData;
    }

    public int getWifiFrequencyBand() {
        WifiManager wifiManager = (WifiManager) emGlobals.getmContext().getSystemService(Context.WIFI_SERVICE);
        Method method = null;
        int frequencyBand = 0;
        try {
            if (wifiManager != null) {
                method = wifiManager.getClass().getDeclaredMethod("getFrequencyBand");
            }
            Integer band = null;
            if (method != null) {
                method.setAccessible(true);
                band = (Integer) method.invoke(wifiManager);
            }
            DLog.log("Frequency band : " + band);
            if (band.intValue() > 2000 && band.intValue() < 3000) {
                frequencyBand = 2;
            } else if (band.intValue() > 3000) {
                frequencyBand = 1;
            } else {
                frequencyBand = band;
            }
        } catch (Exception e) {
            DLog.log("Exception in getWifiFrequencyBand "+e.getMessage());
        }
        DLog.log("Current Frequency band : "+frequencyBand);
        return frequencyBand;
    }


    public void getWifiFrequencyBandValue(){
        WifiManager wifiManager = (WifiManager) emGlobals.getmContext().getSystemService(Context.WIFI_SERVICE);
        Field field = null;
        try {
            field = wifiManager.getClass().getField("WIFI_FREQUENCY_BAND_2GHZ");
            Class clazzType = field.getType();
            if (clazzType.toString().equals("double")) {
                DLog.log("2GHZ "+field.getDouble(wifiManager));
            }
            else if (clazzType.toString().equals("int")) {
                DLog.log("2GHZ " + field.getInt(wifiManager));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            field = wifiManager.getClass().getField("WIFI_FREQUENCY_BAND_5GHZ");
            Class clazzType = field.getType();
            if (clazzType.toString().equals("double")) {
                DLog.log("5GHZ "+field.getDouble(wifiManager));
            }
            else if (clazzType.toString().equals("int")) {
                DLog.log("5GHZ " + field.getInt(wifiManager));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            field = wifiManager.getClass().getField("WIFI_FREQUENCY_BAND_AUTO");
            Class clazzType = field.getType();
            if (clazzType.toString().equals("double")) {
                DLog.log("Auto "+field.getDouble(wifiManager));
            }
            else if (clazzType.toString().equals("int")) {
                DLog.log("Auto " + field.getInt(wifiManager));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setWifiFrequencyBand(int band) {
        WifiManager wifiManager = (WifiManager) emGlobals.getmContext().getSystemService(Context.WIFI_SERVICE);
        DLog.log("Changing band to " + band);
        Method method = null;
        try {
            if (wifiManager != null) {
                method = wifiManager.getClass().getDeclaredMethod("setFrequencyBand", Integer.TYPE,
                        Boolean.TYPE);
            }
            if (method != null) {
                method.setAccessible(true);
                method.invoke(wifiManager, band, Boolean.TRUE);
            }
        } catch (Exception e) {
            DLog.log("Exception in setFrequencyBand "+e.getMessage());
        }
    }

    public boolean isGPSEnabled() {
        boolean status = false;
        LocationManager locManager = (LocationManager) emGlobals.getmContext().getSystemService(Context.LOCATION_SERVICE);
        if (locManager != null) {
            status = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        if (status && getLocationMode() == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {
            return true;
        }
        return false;
    }


    public int getLocationMode() {
        int mode;
        try {
            mode = Settings.Secure.getInt(emGlobals.getmContext().getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Exception e) {
            return Settings.Secure.LOCATION_MODE_HIGH_ACCURACY;
        }
        return mode;
    }

    /*
    0.75 - ldpi
    1.0 - mdpi
    ~1.3 - tvdpi
    1.5 - hdpi
    2.0 - xhdpi
    3.0 - xxhdpi
    4.0 - xxxhdpi
    */

    private String getDeviceDensity() {
        String deviceDensity = "";
        try {
            float density = emGlobals.getmContext().getResources().getDisplayMetrics().density;
            deviceDensity = String.valueOf(density);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deviceDensity;
    }

    public void logDeviceDetails() {
        try {
            DLog.log("===============DeviceInfo================");
            DLog.log("Make : " + get_make());
            DLog.log("Model : " + get_model());
            DLog.log("OSVersion : " + getOSversion());
            DLog.log("SDK Level : " + getAndroidSDK_INT());
            DLog.log("IMEI : " + get_imei());
            DLog.log("Build Number : " + getBuildNumber());
            DLog.log("Firmware : " + getFirmwareVersion());
            DLog.log("Serial Number : " + get_serialnumber());
            DLog.log("Carrier : " + get_carrierName());
            DLog.log("isP2PSupported : " + isP2PSupported());
            DLog.log("Battery Charging Level : " + getBatteryLevel());
            DLog.log("RAM -  Total : " + EMUtility.readableFileSize(getTotalRAMMemory()*1024) + ", Available : " + EMUtility.readableFileSize(getAvailableRAMMemory()*1024));
            DLog.log("Internal Storage - Total : " + EMUtility.readableFileSize(getTotalInternalStroage()*1024) + ", Available : " + EMUtility.readableFileSize(getAvailableInternalStorage()*1024));
            DLog.log("Device Timezone : "+ Time.getCurrentTimezone());
            DLog.log("Storage Directory : "+Environment.getExternalStorageDirectory());
            DLog.log("Device density : "+ getDeviceDensity());
            DLog.log("Mobile data enabled : "+NetworkUtil.isMobileDataEnabled());
            DLog.log("Supports touch : " + isTouchPhone());
            NetworkUtil.logNetworkDetails();
            DLog.log("===============================");
        } catch (Exception e) {
            DLog.log("Exception while getting DeviceInfo");
        }
    }

    public boolean isCarrierModel(String carrierName){
        boolean carrierMatched = false;
        String deviceCarrier = getProp(emGlobals.getmContext(),"sys.sbf.mnoname0");
        DLog.log("carrier model : "+deviceCarrier);
        if(deviceCarrier!=null && deviceCarrier.toLowerCase().contains(carrierName)){
            carrierMatched = true;
        }else {
            deviceCarrier = getProp(emGlobals.getmContext(),"ro.com.google.clientidbase.am");
            DLog.log("carrier model : "+deviceCarrier);
            if(deviceCarrier!=null && deviceCarrier.toLowerCase().contains(carrierName)){
                carrierMatched = true;
            }
        }
        return carrierMatched;
    }

    public boolean isSecurityLockPresent() {
        TestSecurityLock mTestSecurityLock = new TestSecurityLock();
        org.pervacio.onediaglib.diagtests.TestResult keyguardSecurityLock = mTestSecurityLock.getKeyguardSecurityLock();
        if (keyguardSecurityLock.getResultCode() == org.pervacio.onediaglib.diagtests.TestResult.RESULT_PASS) {
            return true;
        } else if (keyguardSecurityLock.getResultCode() == org.pervacio.onediaglib.diagtests.TestResult.RESULT_FAIL) {
            return false;
        }
        return false;
    }

    public boolean isGoogleAccountPresent() {
        TestGoogleAccounts testGoogleAccounts = new TestGoogleAccounts();
        org.pervacio.onediaglib.diagtests.TestResult result = testGoogleAccounts.checkGoogleAccountStatus();
        if (result.getResultCode() == org.pervacio.onediaglib.diagtests.TestResult.RESULT_PASS) {
            return false;
        } else if (result.getResultCode() == org.pervacio.onediaglib.diagtests.TestResult.RESULT_PERMISSION_NOT_GRANTED) {
            return true;
        } else if (result.getResultCode() == org.pervacio.onediaglib.diagtests.TestResult.RESULT_FAIL) {
            return true;
        }
        return false;
    }

    public EDeviceInfo getDeviceInfo(boolean isSource) {
        EDeviceInfo eDeviceInfo = new EDeviceInfo();
        try {
            eDeviceInfo.setMake(get_make());
            eDeviceInfo.setModel(get_model());
            eDeviceInfo.setOSVersion(getOSversion());
            eDeviceInfo.setPlatform(Constants.PLATFORM);
            if (isSource) {
                eDeviceInfo.setOperationType(Constants.OPERATION_TYPE.BACKUP.value());
            } else {
                eDeviceInfo.setOperationType(Constants.OPERATION_TYPE.RESTORE.value());
            }
            eDeviceInfo.setImei(get_imei());
//            eDeviceInfo.setFirmware(getFirmwareVersion());
            eDeviceInfo.setBuildNumber(getBuildNumber());
            eDeviceInfo.setTotalStorage(getTotalInternalStroage());
            eDeviceInfo.setFreeStorage(getAvailableInternalStorage());
            eDeviceInfo.setStartDateTime(Constants.UNINITIALIZED);
            eDeviceInfo.setEndDateTime(Constants.UNINITIALIZED);
//            eDeviceInfo.setSerialNumber(get_serialnumber());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return eDeviceInfo;
    }

    public int getSMSCountFromTime(long time) {
        int value = 0;
        try {
            String fromTime = "date>=" + time;
            Uri uri = null;
            if(getAndroidSDK_INT()>19 && Constants.MMS_SUPPORT){
                uri = Uri.parse("content://mms-sms/conversations/");
            }else {
                uri = Uri.parse("content://sms");
            }
            Cursor mCursor;
            String smsDbType = "type";
            String whereQuery = "( " + smsDbType + " = '" + 1 + "' OR " + smsDbType + " = '" + 2 + "' OR " + smsDbType + " = '" + 3 + "') AND " + fromTime; //filtering inbox, sent and drafts from specified time. SMS_DB_TYPE_INBOX = 1;  SMS_DB_TYPE_SENT  = 2;  SMS_DB_TYPE_DRAFT  = 3,;

            mCursor = emGlobals.getmContext().getContentResolver().query(uri,
                    null,       // projection
                    whereQuery, // where - to get Specific rows.
                    null,       // arguments - none
                    null        // ordering - doesn't matter
            );
            if (mCursor != null) {
                value = mCursor.getCount();
                mCursor.close();
            }
        } catch (Exception e) {
            DLog.log("Exception in getting SMS count from time "+e.getMessage());
        }

        return value;
    }

    public boolean isTouchPhone(){
        boolean hasSystemFeatureTouch = true;
        try {
            hasSystemFeatureTouch = emGlobals.getmContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
        } catch (Exception e) {
            DLog.log("Exception in isTouchPhone : " + e.getMessage());
        }
        return hasSystemFeatureTouch;
    }


    public void getDocuments(File dir) {
        try {
            List<String> fileExtensions = new ArrayList<>(Arrays.asList("pdf", "doc", "docx", "xls", "txt", "odt", "ppt", "pptx", "csv", "vcf", "xlsx", "zip", "rar", "html", "htm", "rtf"));
            File listFile[] = dir.listFiles();
            if (listFile != null) {
                for (int i = 0; i < listFile.length; i++) {
                    if (listFile[i].isDirectory()) {
                        getDocuments(listFile[i]);
                    } else {
                        int pos = listFile[i].getName().lastIndexOf(".");
                        String fileExtension = listFile[i].getName().substring(pos + 1).toLowerCase();
                        if (fileExtensions.contains(fileExtension) && listFile[i].exists() && listFile[i].length() != 0) {
                            EMFileMetaData emFileMetaData = new EMFileMetaData();
                            emFileMetaData.mDataType = EMDataType.EM_DATA_TYPE_DOCUMENTS;
                            emFileMetaData.mFileName = listFile[i].getName();
                            emFileMetaData.mSize = listFile[i].length();
                            emFileMetaData.mSourceFilePath = listFile[i].getAbsolutePath();
                            String relativePath = listFile[i].getAbsolutePath();
                            if (relativePath.startsWith(Environment.getExternalStorageDirectory().toString())) {
                                relativePath = listFile[i].getAbsolutePath().replace(Environment.getExternalStorageDirectory().toString(), "");
                            } else {
                                DLog.log("Folder structure Mismatch : " + listFile[i].getAbsolutePath());
                                relativePath = null;
                            }
                            emFileMetaData.mRelativePath = relativePath;
                            documentsQueue.add(emFileMetaData);
                        }
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}