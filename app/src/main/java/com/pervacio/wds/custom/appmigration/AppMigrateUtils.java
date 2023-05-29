package com.pervacio.wds.custom.appmigration;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.custom.APPI;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.custom.utils.Constants;

import static com.pervacio.wds.custom.utils.Constants.ENABLE_INSTALL_MOVISTAR_APPS;
import static com.pervacio.wds.custom.utils.Constants.ENABLE_INSTALL_NON_MOVISTAR_APPS;
import static com.pervacio.wds.custom.utils.Constants.moviStarAppsMap;
import static com.pervacio.wds.custom.utils.Constants.movistarappsList;
import static com.pervacio.wds.custom.utils.Constants.movistarappsListS3;

/**
 * Utility class to support feature of app migration.
 *
 * Use cases for particular methods have been documented at their own places.
 *
 * @author : Darpan Dodiya <darpan.dodiya@pervacio.com>
 */

public class AppMigrateUtils {
    public static int totalAppCount = 0;
    public static long totalAppSize = 0;
    public static long appsCountTest = 20;
    public static String TAG = "AppMigrateUtils";
    static EMGlobals emGlobals = new EMGlobals();
    //This list will be used in source device while backing up
    public static List<AppBackupModel> backupAppList = new ArrayList<>();

    //This list will be used in destination device while restoring
    public static List<AppRestoreModel> restoreAppList = new ArrayList<>();
    public static LinkedHashMap<Integer, AppBackupModel> appDetailsMap = new LinkedHashMap<>();
    public static List<AppInfoModel> appInfoList = new ArrayList<>();

    /**
     * Method to get information of all the apps present on device.
     *
     * The retrieved info will be stored in backupAppList. It will ignore system applications
     * and own WDS app while iterating through.
     */
    public static void procureAppsDetails() {
        Log.i(TAG,"Enter procureAppsDetails backupAppList.size() "+backupAppList.size());
        int installCount = 5;
        List<String> movistarAppsList_ = new ArrayList<>();
        if (backupAppList.size()==0){

            if(ENABLE_INSTALL_MOVISTAR_APPS == true) {

               /* for (String appName : Constants.movistarappsList) {
                    if (checkAppPresence(appName)) {
                        Log.i("SatyaTest", "Enter procureAppsDetails movistar app already present " + appName);
                    } else {

                        AppBackupModel app = new AppBackupModel();
                        app.setAppName(appName);
                        app.setChecked(true);
                        backupAppList.add(app);
                        totalAppCount++;
                        totalAppSize = totalAppSize + 0;
                        Log.i("SatyaTest", "Enter procureAppsDetails movistar app added " + appName);
                    }
                }*/


                try {
                    PackageManager pm = (PackageManager) emGlobals.getmContext().getPackageManager();

                    List<PackageInfo> packs = pm.getInstalledPackages(0);
                    for (int i = 0; i < packs.size(); i++) {
                        if (true) {
                            PackageInfo p = packs.get(i);
                            if ((p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                                continue;
                            }
                            //Skip our own app
                            if (p.packageName.equalsIgnoreCase(emGlobals.getmContext().getPackageName())) {
                                continue;
                            }
                            AppBackupModel app = new AppBackupModel();
                            if (movistarappsList.contains(p.applicationInfo.loadLabel(pm).toString())) {
                                app.setAppName(p.applicationInfo.loadLabel(pm).toString());
                                app.setPackageName(p.packageName);
                                app.setVersionName(p.versionName);
                                app.setVersionCode(p.versionCode);
                                app.setAppIcon(p.applicationInfo.loadIcon(pm));
                                app.setFile(new File(p.applicationInfo.publicSourceDir));

                                app.setChecked(true);
                                backupAppList.add(app);


                                totalAppCount++;
                                totalAppSize += app.getAppMemory();
                                Log.i(TAG, "Enter procureAppsDetails app added " + totalAppCount);
                                Log.i(TAG, "Enter procureAppsDetails movistart app present " + p.applicationInfo.loadLabel(pm).toString());
                                movistarAppsList_.add(p.applicationInfo.loadLabel(pm).toString());
                            }

                        }
                    }
                } catch (Exception e) {
                    DLog.log(e);
                }
                if (!ENABLE_INSTALL_MOVISTAR_APPS) {
                    for (String appName : movistarappsList) {

                        if (!movistarAppsList_.contains(appName)) {
                            AppBackupModel app = new AppBackupModel();
                            app.setAppName(appName);
                            app.setChecked(true);
                            backupAppList.add(app);
                            totalAppCount++;
                            totalAppSize = totalAppSize + 0;
                            Log.i(TAG, "Enter procureAppsDetails app " + appName + " not present and added");
                        }
                    }
                }
            }
        if(ENABLE_INSTALL_NON_MOVISTAR_APPS) {
            try {
                PackageManager pm = (PackageManager) emGlobals.getmContext().getPackageManager();

                List<PackageInfo> packs = pm.getInstalledPackages(0);
                for (int i = 0; i < packs.size(); i++) {
                    if (true) {
                        PackageInfo p = packs.get(i);
                        if ((p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                            continue;
                        }
                        //Skip our own app
                        if (p.packageName.equalsIgnoreCase(emGlobals.getmContext().getPackageName())) {
                            continue;
                        }
                        AppBackupModel app = new AppBackupModel();
                        app.setAppName(p.applicationInfo.loadLabel(pm).toString());
                        app.setPackageName(p.packageName);
                        app.setVersionName(p.versionName);
                        app.setVersionCode(p.versionCode);
                        app.setAppIcon(p.applicationInfo.loadIcon(pm));
                        app.setFile(new File(p.applicationInfo.publicSourceDir));
                        app.setChecked(true);
                        backupAppList.add(app);


                        totalAppCount++;
                        totalAppSize += app.getAppMemory();
                        Log.i(TAG, "Enter procureAppsDetails app added " + totalAppCount);

                    }
                }
            } catch (Exception e) {
                DLog.log(e);
            }
        }
        }
    }

    /**
     * During restoration, this method will be used to keep track of all the apk files being transferred.
     * It will be stored in restoreAppList, which will be later used to show installation prompts.
     *
     * @param apkPath Absolute file path to the apk
     */
    public static void addRestoreApp(String apkPath) {
        DLog.log("In addRestoreApp for apkPath: " + apkPath);

        try {
            AppRestoreModel app = new AppRestoreModel();
            app.setPath(apkPath);

            restoreAppList.add(app);
        }
        catch(Exception ex) {
            DLog.log(ex);
        }
    }
    public static boolean checkAppPresence(String appName) {
        boolean isAppPresent = false;
        try {
            PackageManager pm = (PackageManager) emGlobals.getmContext().getPackageManager();

            List<PackageInfo> packs = pm.getInstalledPackages(0);
            for (int i = 0; i < packs.size(); i++) {
                PackageInfo p = packs.get(i);
                if ((p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    continue;
                }
                //Skip our own app
                if(p.packageName.equalsIgnoreCase(emGlobals.getmContext().getPackageName())) {
                    continue;
                }
                AppBackupModel app = new AppBackupModel();
                String applicationName = p.applicationInfo.loadLabel(pm).toString();
                String appPackageName = p.packageName;
                DLog.log("checkAppPresence appName "+applicationName);
                DLog.log("checkAppPresence packageName "+appPackageName);

                if( applicationName.equals(appName)){
                    isAppPresent = true;
                    break;
                }

            }
        } catch (Exception e) {
            DLog.log(e);
        }

        return isAppPresent;
    }
    public static void updateSelectedAppsMap(int appNumber, boolean isChecked) {
        AppBackupModel app = appDetailsMap.get(appNumber);
        app.setChecked(isChecked);
        appDetailsMap.put(appNumber,app);
    }


    public static LinkedHashMap<Integer, AppBackupModel> procureAppsDetailsMap() {

        if (appDetailsMap.size()!=0)
        {
            return appDetailsMap;
        }else {
            int appNumber = 0;
            try {
                PackageManager pm = (PackageManager) emGlobals.getmContext().getPackageManager();

                List<PackageInfo> packs = pm.getInstalledPackages(0);
                for (int i = 0; i < packs.size(); i++) {
                    PackageInfo p = packs.get(i);
                    if ((p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                        continue;
                    }
                    //Skip our own app
                    if (p.packageName.equalsIgnoreCase(emGlobals.getmContext().getPackageName())) {
                        continue;
                    }
                    AppBackupModel app = new AppBackupModel();
                    app.setAppName(p.applicationInfo.loadLabel(pm).toString());
                    app.setPackageName(p.packageName);
                    app.setVersionName(p.versionName);
                    app.setVersionCode(p.versionCode);
                    app.setAppIcon(p.applicationInfo.loadIcon(pm));
                    app.setFile(new File(p.applicationInfo.publicSourceDir));
                    app.setChecked(true);
/*              backupAppList.add(app);
                totalAppCount++;
                totalAppSize += app.getAppMemory();*/
                    appNumber = appNumber + 1;
                    appDetailsMap.put(appNumber, app);
                }
            } catch (Exception e) {
                DLog.log(e);
            }

            return appDetailsMap;
        }
    }


    public static int getSelectedTotalAppsCount(){
        int appsCount =0;
        for (Map.Entry mapElement : appDetailsMap.entrySet()) {
            AppBackupModel app = (AppBackupModel) mapElement.getValue();
            if(app.isChecked()){
                appsCount = appsCount+1;
            }
        }
        return appsCount;
    }

    public static int getSelectedTotalAppsCountList(){
        int appsCount =0;
        /*for (Map.Entry mapElement : appDetailsMap.entrySet()) {
            AppBackupModel app = (AppBackupModel) mapElement.getValue();
            if(app.isChecked()){
                appsCount = appsCount+1;
            }
        }*/

        for (AppBackupModel appBackupModel : backupAppList){
            if(appBackupModel.isChecked()){
                appsCount = appsCount+1;
            }
        }
        return appsCount;
    }

    public static long getSelectedTotalAppsSize(){

        long appsSize =0;

        for (Map.Entry mapElement : appDetailsMap.entrySet()) {
            AppBackupModel app = (AppBackupModel) mapElement.getValue();
            if(app.isChecked()){
                appsSize = appsSize+app.getAppMemory();
            }
        }
        return appsSize;
    }

    public static long getSelectedTotalAppsSizeList(){

        long appsSize =0;

       /* for (Map.Entry mapElement : appDetailsMap.entrySet()) {
            AppBackupModel app = (AppBackupModel) mapElement.getValue();
            if(app.isChecked()){
                appsSize = appsSize+app.getAppMemory();
            }
        }*/

        for (AppBackupModel appBackupModel : backupAppList){
            if(appBackupModel.isChecked()){
                appsSize = appsSize+ appBackupModel.getAppMemory();
            }
        }
        return appsSize;
    }

    public static void updateBackupAppsCountSize(int index,boolean isChecked){
        if(!isChecked) {
            totalAppSize = totalAppSize - backupAppList.get(index).getAppMemory();
            totalAppCount = totalAppCount -1;
        }
        else{
            totalAppSize = totalAppSize + backupAppList.get(index).getAppMemory();
            totalAppCount = totalAppCount +1;
        }
    }

    public static void selectOrUnselectAllApps(boolean isSelected){
        DLog.log("enter selectOrUnselectAllApps AppMigrateUtils.backupAppList size "+AppMigrateUtils.backupAppList.size()+" isSelected "+isSelected);
        int position=0;
        int totalCount=0;
        long totalSize=0;
        List<AppBackupModel> backupAppListLocal = new ArrayList<>();

        for(AppBackupModel appBackupModel : AppMigrateUtils.backupAppList){
            appBackupModel.setChecked(isSelected);
            backupAppListLocal.add(position,appBackupModel);
            DLog.log("enter loop selectOrUnselectAllApps added to local list size "+backupAppListLocal.size());

            position++;
        }
        AppMigrateUtils.backupAppList = backupAppListLocal;
        DLog.log("after loop selectOrUnselectAllApps AppMigrateUtils.backupAppList.size() "+AppMigrateUtils.backupAppList.size());

        position =0;
        while(position<AppMigrateUtils.backupAppList.size()){
            DLog.log("selectOrUnselectAllApps pos "+position+" selected "+isSelected);
            if(isSelected){
                totalSize = totalSize + backupAppList.get(position).getAppMemory();
                totalCount = totalCount +1;
            }else{
                totalCount=0;
                totalSize=0;
            }
            position++;
        }
        totalAppSize = totalSize;
        totalAppCount = totalCount;
        DLog.log("selectOrUnselectAllApps totalAppCount "+totalAppCount+" totalAppSize "+totalAppSize);
    }

    public static void addAppBackupList(AppBackupModel app){

        backupAppList.add(app);


        totalAppCount++;
        totalAppSize += app.getAppMemory();
    }

    public static void clearBackupAppsList(){
        backupAppList.clear();
        totalAppCount = 0;
        totalAppSize=0;
    }

    public static void  prepareMoviStarAppsListMap(){
        int i =0;
        for (String name : movistarappsListS3){
            moviStarAppsMap.put(name, movistarappsList.get(i));
            i = i+1;

        }
    }

    public static AppInfoModel prepareRestoreAppFullData(String mFullPath) {
        AppInfoModel appInfoModel  = null;
        File file = new File(mFullPath);
        int file_size = Integer.parseInt(String.valueOf(file.length() / 1024));
        DLog.log("App file_size of " + mFullPath + " " + file_size + " KB");
        try {
            final PackageManager pm = emGlobals.getmContext().getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(mFullPath, 0);
            DLog.log("app versionName " + info.versionName);
            info.applicationInfo.publicSourceDir = mFullPath;
            File appFile = new File(info.applicationInfo.publicSourceDir);
            //info.applicationInfo.publicSourceDir = mFullPath;
            long appMemory = appFile.length();
            DLog.log("appMemory : " + appMemory);
            Drawable appIcon = info.applicationInfo.loadIcon(pm);
            String appName = info.applicationInfo.loadLabel(pm).toString();
            DLog.log("appName : " + appName);
            String packageName = info.packageName;
            DLog.log("packageName : " + packageName);
            String versionName = info.versionName;
            DLog.log("versionName : " + versionName);
            int versionCode = info.versionCode;
            DLog.log("versionCode : " + versionCode);

            appInfoModel = new AppInfoModel();
            appInfoModel.setName(appName);
            appInfoModel.setPackageName(packageName);
            appInfoModel.setPath(mFullPath);
//            app.setVersionName(versionName);
//            app.setVersionCode(versionCode);
            appInfoModel.setIcon(appIcon);
            appInfoModel.setFile(appFile);
            appInfoModel.setChecked(true);
            appInfoModel.setLocalInstallationAttempted(false);
//            AppMigrateUtils.addAppBackupList(app);

        } catch (Exception e) {
            appInfoModel = null;

        }
        return appInfoModel;
    }
}
