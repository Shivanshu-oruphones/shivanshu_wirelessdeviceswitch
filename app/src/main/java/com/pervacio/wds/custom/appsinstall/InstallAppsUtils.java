//package com.pervacio.wds.custom.appsinstall;
//
//import android.content.Intent;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.graphics.drawable.Drawable;
//import android.net.Uri;
//
//import androidx.core.content.FileProvider;
//
//import com.pervacio.wds.BuildConfig;
//import com.pervacio.wds.app.DLog;
//import com.pervacio.wds.custom.APPI;
//import com.pervacio.wds.custom.appmigration.AppInfoModel;
//import com.pervacio.wds.custom.appmigration.AppMigrateUtils;
//import com.pervacio.wds.custom.appmigration.AppRestoreModel;
//import com.pervacio.wds.custom.utils.AppsDetails;
//
//import org.pervacio.onediaglib.diagtests.TestGoogleAccounts;
//import org.pervacio.onediaglib.diagtests.TestResult;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//
//import static com.pervacio.wds.custom.appmigration.AppMigrateUtils.prepareRestoreAppFullData;
//
//public class InstallAppsUtils {
//
//    public static boolean returningFromAppInstallFlow;
//
//    public static void installApps(String appPathName,String packageName, boolean fromPlayStore)
//    {
//        returningFromAppInstallFlow = true;
//
//        DLog.log("Enter installApps appPathName : "+appPathName+" packageName "+packageName+" fromPlayStore "+fromPlayStore);
//        if(fromPlayStore){
//            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
//            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET|Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
//            APPI.getAppContext().startActivity(marketIntent);
//        }else {
//            Uri apkUri = FileProvider.getUriForFile(APPI.getAppContext(), BuildConfig.APPLICATION_ID + ".fileprovider", new File(appPathName));
//
//            Intent intent = new Intent(Intent.ACTION_VIEW, apkUri);
//            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
//            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //dont forget add this line
//            APPI.getAppContext().startActivity(intent);
//        }
//
//    }
//
//    public static void installAppFromPlayStore(String packageName){
//        Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
//        marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET|Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
//        APPI.getAppContext().startActivity(marketIntent);
//    /*        installledAppsCount = installledAppsCount + 1;
//            installFromPlayStore = true;*/
//    }
//
//    public static List<AppsDetails> getCompleteAppsDetails(){
//
//        List<AppInfoModel> appInfoModelFailedList = getInstallationFailedList();
//        List<AppInfoModel> appInfoModelNotInstalledList = getNotInstalledList();
//        List<AppInfoModel> appInfoModelPassedList = getInstallationPassedList();
//        DLog.log("enter contentNotifyDatasetChanged failed size "+appInfoModelFailedList.size()+" , not installed size "+appInfoModelNotInstalledList.size()+" , installed size "+appInfoModelPassedList.size());
//        List<AppInfoModel> appInfoModelCompleteList = new ArrayList<>();
//        appInfoModelCompleteList.addAll(appInfoModelFailedList);
//        appInfoModelCompleteList.addAll(appInfoModelNotInstalledList);
//        appInfoModelCompleteList.addAll(appInfoModelPassedList);
//
//
//        List<AppsDetails> appsDetailsList = new ArrayList<>();
//        if(appInfoModelFailedList.size()!=0) {
//            //            appsDetailsList.add(new AppsDetails("Installation Failed Apps", appInfoModelFailedList, true, "Failed"));
//            appsDetailsList.add(new AppsDetails("Installation Failed Apps", appInfoModelFailedList, true, "Failed"));
//        }
//        if(appInfoModelNotInstalledList.size()!=0) {
//            appsDetailsList.add(new AppsDetails("Installation Pending Apps", appInfoModelNotInstalledList, true, "Not Installed"));
//        }
//        if(appInfoModelPassedList.size()!=0) {
//            appsDetailsList.add(new AppsDetails("Installed Apps", appInfoModelPassedList, true, "Installed"));
//        }
//
//        return appsDetailsList;
//
//
//    }
//
//    public static List<AppInfoModel> getInstallationFailedList(){
//        List<AppInfoModel> appInfoFailedList = new ArrayList<>();
//        for (AppInfoModel appInfoModel: AppMigrateUtils.appInfoList){
//            if(appInfoModel.isInstallationDone()==false){
//                if(appInfoModel.isLocalInstallationAttempted()){
//                    appInfoFailedList.add(appInfoModel);
//                }
//            }
//               /* if(appInfoModel.isLocalInstallationAttempted()){
//                    DLog.log("");
//                    appInfoFailedList.add(appInfoModel);
//                }else{
//
//                }*/
//        }
//        return appInfoFailedList;
//    }
//
//    public static List<AppInfoModel> getNotInstalledList(){
//        List<AppInfoModel> appInfoNotInstalledList = new ArrayList<>();
//        for (AppInfoModel appInfoModel: AppMigrateUtils.appInfoList){
//                /*if(appInfoModel.isLocalInstallationAttempted()){
//                    DLog.log("");
//
//                }else{
//                    appInfoNotInstalledList.add(appInfoModel);
//                }*/
//            if(appInfoModel.isInstallationDone()==false){
//                if(appInfoModel.isLocalInstallationAttempted()==false)
//                    appInfoNotInstalledList.add(appInfoModel);
//            }
//        }
//        return appInfoNotInstalledList;
//    }
//
//    public static List<AppInfoModel> getInstallationPassedList(){
//        List<AppInfoModel> appInfoPassedList = new ArrayList<>();
//        for (AppInfoModel appInfoModel: AppMigrateUtils.appInfoList){
//            if(appInfoModel.isInstallationDone() ){
//                DLog.log("");
//                appInfoPassedList.add(appInfoModel);
//            }
//        }
//        return appInfoPassedList;
//    }
//
//    public static void prepareRestoredAppData() {
//        DLog.log("enter prepareAppData  AppMigrateUtils.appInfoList size "+ AppMigrateUtils.appInfoList.size());
//        List<AppInfoModel> appInfoListLocal = new ArrayList<>();
//        AppMigrateUtils.appInfoList.clear();
//
//        DLog.log("enter prepareAppData first time");
//        for (AppRestoreModel appRestoreModel : AppMigrateUtils.restoreAppList){
//
//            AppInfoModel appInfoModel = prepareRestoreAppFullData(appRestoreModel.getPath());
//            String appVersonName = getAppVersion(appInfoModel.getPackageName());
//            if(appVersonName.equalsIgnoreCase("NotAvailable"))
//            {
//                appInfoModel.setInstallationDone(false);
//            }else{
//                appInfoModel.setLocalInstallationAttempted(true);
//                appInfoModel.setInstallationDone(true);
//            }
//
//
//
//               /* String appVersonName = getAppVersion(appRestoreModel.getPackageName());
//                AppInfoModel appInfoModel = new AppInfoModel();
//                appInfoModel.setVersionName(appVersonName);
//                appInfoModel.setPackageName(appRestoreModel.getPackageName());
//                appInfoModel.setName(appRestoreModel.getName());
//                appInfoModel.setIcon(getAppIcon(appRestoreModel.getPackageName()));
//                if(appVersonName.equalsIgnoreCase("NotAvailable"))
//                {
//                    appInfoModel.setInstallationDone(false);
//                }else{
//                    appInfoModel.setInstallationDone(true);
//                }
//                appInfoListMap.put(appRestoreModel.getPackageName(),appInfoModel);
//    */
//                /*for (Map.Entry mapElement : appInfoListMap.entrySet()) {
//                    AppInfoModel appInfoModel = (AppInfoModel) mapElement.getValue();
//                    DLog.log("enter onPostExecute package name : "+appInfoModel.getPackageName()+" version : "+appInfoModel.getVersionName()+" playstore version : "+appInfoModel.getPlayStoreVersionName());
//                    AppMigrateUtils.appInfoList.add(appInfoModel);
//                }*/
//            AppMigrateUtils.appInfoList.add(appInfoModel);
//            //            appInfoListLocal.add(appInfoModel);
//
//            DLog.log("enter prepareAppData + appInfoModel.getPackageName() "+appInfoModel.getPackageName() +" appVersonName "+appVersonName);
//        }
//        //        AppMigrateUtils.appInfoList = appInfoListLocal;
//        //        DLog.log("exit prepareAppData "+appInfoListMap.size());
//        DLog.log("exit prepareAppData  AppMigrateUtils.appInfoList size "+ AppMigrateUtils.appInfoList.size());
//    }
//    public static String getAppVersion(String appName) {
//        String version = "NotAvailable";
//        DLog.log("enter getAppVersion appName "+appName);
//        PackageManager pm = APPI.getAppContext().getPackageManager();
//        try {
//            //            PackageInfo packageInfo = pm.getPackageInfo(appName, PackageManager.GET_ACTIVITIES);
//            PackageInfo packageInfo = pm.getPackageInfo(appName, 0);
//            String versionName = packageInfo.versionName;
//            version = versionName;
//        } catch (PackageManager.NameNotFoundException e) {
//            DLog.log("getAppVersion Exception "+e);
//            version = "NotAvailable";
//        }
//        return version;
//    }
//
//    public Drawable getAppIcon(String appName) {
//        Drawable appIcon = null;
//        PackageManager pm = APPI.getAppContext().getPackageManager();
//        try {
//            PackageInfo packageInfo = pm.getPackageInfo(appName, PackageManager.GET_ACTIVITIES);
//            appIcon = packageInfo.applicationInfo.loadIcon(pm);
//        } catch (PackageManager.NameNotFoundException e) {
//
//        }
//        return appIcon;
//    }
//
//    public static boolean isGoogleAccountPresent(){
//        DLog.log("enter isGoogleAccountPresent");
//        boolean googleAccountPresent = false;
//        TestGoogleAccounts testGoogleAccounts;
//        testGoogleAccounts = new TestGoogleAccounts();
//        TestResult testResult = testGoogleAccounts.checkGoogleAccountStatus();
//        DLog.log("checkGoogleAccountStatus testResult " + testResult.getResultCode());
//        if (TestResult.RESULT_PASS == testResult.getResultCode()) {
//            DLog.log("checkGoogleAccountStatus testResult : PASS and googleAccountPresent false ");
//            googleAccountPresent=false;
//        } else {
//            DLog.log("checkGoogleAccountStatus testResult : FAIL and googleAccountPresent true ");
//            googleAccountPresent=true;
//        }
//        return googleAccountPresent;
//    }
//}

package com.pervacio.wds.custom.appsinstall;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.custom.appmigration.AppInfoModel;
import com.pervacio.wds.custom.appmigration.AppMigrateUtils;
import com.pervacio.wds.custom.appmigration.AppRestoreModel;
import com.pervacio.wds.custom.utils.AppsDetails;

import org.pervacio.onediaglib.diagtests.TestGoogleAccounts;
import org.pervacio.onediaglib.diagtests.TestResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.pervacio.wds.custom.appmigration.AppMigrateUtils.prepareRestoreAppFullData;

public class InstallAppsUtils {
    static EMGlobals emGlobals = new EMGlobals();

    public static boolean returningFromAppInstallFlow;

    public static void installApps(String appPathName,String packageName, boolean fromPlayStore)
    {
        returningFromAppInstallFlow = true;

        DLog.log("Enter installApps appPathName : "+appPathName+" packageName "+packageName+" fromPlayStore "+fromPlayStore);
        if(fromPlayStore){
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET|Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            emGlobals.getmContext().startActivity(marketIntent);
        }else {
            Uri apkUri = FileProvider.getUriForFile(emGlobals.getmContext(), BuildConfig.APPLICATION_ID + ".fileprovider", new File(appPathName));
//            Uri apkUri = null;
            Intent intent = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(emGlobals.getmContext(), BuildConfig.APPLICATION_ID + ".fileprovider", new File(appPathName));
            } else {
                apkUri = Uri.fromFile(new File(appPathName));
            }
            intent = new Intent(Intent.ACTION_VIEW, apkUri);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //dont forget add this line
            emGlobals.getmContext().startActivity(intent);
        }

    }

    public static void installAppFromPlayStore(String packageName){
        Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
        marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET|Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        emGlobals.getmContext().startActivity(marketIntent);
    }

    public static List<AppsDetails> getCompleteAppsDetails(){

        List<AppInfoModel> appInfoModelFailedList = getInstallationFailedList();
        List<AppInfoModel> appInfoModelNotInstalledList = getNotInstalledList();
        List<AppInfoModel> appInfoModelPassedList = getInstallationPassedList();
        DLog.log("enter contentNotifyDatasetChanged failed size "+appInfoModelFailedList.size()+" , not installed size "+appInfoModelNotInstalledList.size()+" , installed size "+appInfoModelPassedList.size());
        List<AppInfoModel> appInfoModelCompleteList = new ArrayList<>();
        appInfoModelCompleteList.addAll(appInfoModelFailedList);
        appInfoModelCompleteList.addAll(appInfoModelNotInstalledList);
        appInfoModelCompleteList.addAll(appInfoModelPassedList);


        List<AppsDetails> appsDetailsList = new ArrayList<>();
        if(appInfoModelFailedList.size()!=0) {
            appsDetailsList.add(new AppsDetails("Installation Failed Apps", appInfoModelFailedList, true, "Failed"));
        }
        if(appInfoModelNotInstalledList.size()!=0) {
            appsDetailsList.add(new AppsDetails("Installation Pending Apps", appInfoModelNotInstalledList, true, "Not Installed"));
        }
        if(appInfoModelPassedList.size()!=0) {
            appsDetailsList.add(new AppsDetails("Installed Apps", appInfoModelPassedList, true, "Installed"));
        }
        return appsDetailsList;

    }

    public static List<AppInfoModel> getInstallationFailedList(){
        List<AppInfoModel> appInfoFailedList = new ArrayList<>();
        for (AppInfoModel appInfoModel: AppMigrateUtils.appInfoList){
            if(appInfoModel.isInstallationDone()==false){
                if(appInfoModel.isLocalInstallationAttempted()){
                    appInfoFailedList.add(appInfoModel);
                }
            }
        }
        return appInfoFailedList;
    }

    public static List<AppInfoModel> getNotInstalledList(){
        List<AppInfoModel> appInfoNotInstalledList = new ArrayList<>();
        for (AppInfoModel appInfoModel: AppMigrateUtils.appInfoList){
            if(appInfoModel.isInstallationDone()==false){
                if(appInfoModel.isLocalInstallationAttempted()==false)
                    appInfoNotInstalledList.add(appInfoModel);
            }
        }
        return appInfoNotInstalledList;
    }

    public static List<AppInfoModel> getInstallationPassedList(){
        List<AppInfoModel> appInfoPassedList = new ArrayList<>();
        for (AppInfoModel appInfoModel: AppMigrateUtils.appInfoList){
            if(appInfoModel.isInstallationDone() ){
                DLog.log("");
                appInfoPassedList.add(appInfoModel);
            }
        }
        return appInfoPassedList;
    }

    public static void prepareRestoredAppData() {
        DLog.log("enter prepareAppData  AppMigrateUtils.appInfoList size "+ AppMigrateUtils.appInfoList.size());
        List<AppInfoModel> appInfoListLocal = new ArrayList<>();
        AppMigrateUtils.appInfoList.clear();

        DLog.log("enter prepareAppData first time AppMigrateUtils.restoreAppList size "+AppMigrateUtils.restoreAppList.size());
        for (AppRestoreModel appRestoreModel : AppMigrateUtils.restoreAppList){

            AppInfoModel appInfoModel = prepareRestoreAppFullData(appRestoreModel.getPath());
            DLog.log("enter prepareAppData first time appInfoModel "+appInfoModel);
            if(appInfoModel!=null) {
                String appVersonName = getAppVersion(appInfoModel.getPackageName());
                if (appVersonName.equalsIgnoreCase("NotAvailable")) {
                    appInfoModel.setInstallationDone(false);
                } else {
                    appInfoModel.setLocalInstallationAttempted(true);
                    appInfoModel.setInstallationDone(true);
                }
                AppMigrateUtils.appInfoList.add(appInfoModel);
                DLog.log("enter prepareAppData + appInfoModel.getPackageName() " + appInfoModel.getPackageName() + " appVersonName " + appVersonName);
            }
        }
        DLog.log("exit prepareAppData  AppMigrateUtils.appInfoList size "+ AppMigrateUtils.appInfoList.size());
    }
    public static String getAppVersion(String appName) {
        String version = "NotAvailable";
        DLog.log("enter getAppVersion appName "+appName);
        PackageManager pm = emGlobals.getmContext().getPackageManager();
        try {
            //            PackageInfo packageInfo = pm.getPackageInfo(appName, PackageManager.GET_ACTIVITIES);
            PackageInfo packageInfo = pm.getPackageInfo(appName, 0);
            String versionName = packageInfo.versionName;
            version = versionName;
            DLog.log("enter getAppVersion appName "+appName+" version "+version);
        } catch (PackageManager.NameNotFoundException e) {
            DLog.log("getAppVersion Exception "+e);
            version = "NotAvailable";
        }
        return version;
    }

    public Drawable getAppIcon(String appName) {
        Drawable appIcon = null;
        PackageManager pm = emGlobals.getmContext().getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(appName, PackageManager.GET_ACTIVITIES);
            appIcon = packageInfo.applicationInfo.loadIcon(pm);
        } catch (PackageManager.NameNotFoundException e) {

        }
        return appIcon;
    }

    public static boolean isGoogleAccountPresent(){
        DLog.log("enter isGoogleAccountPresent");
        boolean googleAccountPresent = false;
        TestGoogleAccounts testGoogleAccounts;
        testGoogleAccounts = new TestGoogleAccounts();
        TestResult testResult = testGoogleAccounts.checkGoogleAccountStatus();
        DLog.log("checkGoogleAccountStatus testResult " + testResult.getResultCode());
        if (TestResult.RESULT_PASS == testResult.getResultCode()) {
            DLog.log("checkGoogleAccountStatus testResult : PASS and googleAccountPresent false ");
            googleAccountPresent=false;
        } else {
            DLog.log("checkGoogleAccountStatus testResult : FAIL and googleAccountPresent true ");
            googleAccountPresent=true;
        }
        return googleAccountPresent;
    }
}
