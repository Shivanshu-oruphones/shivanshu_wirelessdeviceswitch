/*************************************************************************
 *
 * Media Mushroom Limited CONFIDENTIAL
 * __________________
 *
 *  Copyright 2017 Media Mushroom Limited
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Media Mushroom Limited.
 *
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Media Mushroom Limited.
 */

package com.pervacio.wds.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony.Sms;
import android.util.Log;

import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.custom.utils.PreferenceHelper;

import java.io.File;

public class EMUtilsDefaultSmsApp
{
	static EMGlobals emGlobals = new EMGlobals();
	static final private String EM_PRIVATE_DIR = "data";
	static final private String EM_DEFAULT_SMS_APP_FILE = "smsapp.txt";

    private String mDefaultSmsAppStoragePath = null;

    private Context  mAppContext  = null;
    private Activity mAppActivity = null;
	EMUtilsDefaultSmsApp emUtilsDefaultSmsApp;
	public static final  int REQUEST_CODE_SMSAPP_DEFAULT = 101;
	public static final int REQUEST_CODE_CHANGEBACK_SMSAPP = 220;
    
    public EMUtilsDefaultSmsApp(Context aContext, Activity aActivity)
    {
    	mAppContext  = aContext;
    	mAppActivity = aActivity;
    	
        File storagePath  = mAppContext.getDir(EM_PRIVATE_DIR, Context.MODE_PRIVATE);        
        
        storagePath.mkdirs();

        mDefaultSmsAppStoragePath = storagePath.getAbsolutePath() + File.separator + EM_DEFAULT_SMS_APP_FILE;
    }

	public void becomeDefaultSmsApp() {
    	DLog.log("becomeDefaultSmsApp");

		String defaultSmsApp = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			defaultSmsApp = Sms.getDefaultSmsPackage(mAppContext);
		}
		// Log.d("SatyaTest","enter becomeDefaultSmsApp defaultSmsApp "+defaultSmsApp);
		String thisPackage = mAppContext.getPackageName();
		// Log.d("SatyaTest","enter becomeDefaultSmsApp thisPackage "+thisPackage);
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT || thisPackage.equalsIgnoreCase(defaultSmsApp)) {
			traceit("<< becomeDefaultSmsApp, We are already the default SMS App");

			return;
		}

		File smsAppNameFile = new File(mDefaultSmsAppStoragePath);
		if (!smsAppNameFile.exists()) {
			traceit("<< becomeDefaultSmsApp, Saving default SMS App: " + defaultSmsApp);
			EMUtilsFileIO.setFileContents(mDefaultSmsAppStoragePath, defaultSmsApp.getBytes());
			PreferenceHelper.getInstance(emGlobals.getmContext()).putStringItem("default_sms_app",mDefaultSmsAppStoragePath);
		}
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			DLog.log("enter becomeDefaultSmsApp and calling createRequestRoleIntent case");
			@SuppressLint("WrongConstant") RoleManager rm = (RoleManager) mAppContext.getSystemService(Context.ROLE_SERVICE);
			mAppActivity.startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_SMS), REQUEST_CODE_SMSAPP_DEFAULT);
			DLog.log("enter becomeDefaultSmsApp and called createRequestRoleIntent case");

		}else{
			DLog.log("enter becomeDefaultSmsApp starting ACTION_CHANGE_DEFAULT");
			Intent intent = new Intent(Sms.Intents.ACTION_CHANGE_DEFAULT);
			intent.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, thisPackage);

			mAppActivity.startActivityForResult(intent, REQUEST_CODE_SMSAPP_DEFAULT);
		}
		traceit("<< becomeDefaultSmsApp");
	}
	
	
	public void restoreOriginalSmsApp()
	{
		traceit(">> restoreOriginalSmsApp");

		mDefaultSmsAppStoragePath = PreferenceHelper.getInstance(emGlobals.getmContext()).getStringItem("default_sms_app");
        File smsAppPackageNameFile = new File(mDefaultSmsAppStoragePath);
        
        if (! smsAppPackageNameFile.exists())
        {
			traceit("<< restoreOriginalSmsApp, No Default SMS App File: " +mDefaultSmsAppStoragePath);			
			return;        	
        }

		byte[] smsAppPackageNameData = EMUtilsFileIO.getFileContents(mDefaultSmsAppStoragePath);		
		
		smsAppPackageNameFile.delete();							
		
		if (smsAppPackageNameData == null)
		{
			logit("restoreOriginalSmsApp, Unable to read Default SMS App file: " +mDefaultSmsAppStoragePath);
			traceit("<< restoreOriginalSmsApp");
			return;
		}
		
		String defaultSmsAppPackageName = null;
		
		try
		{			
			defaultSmsAppPackageName = new String(smsAppPackageNameData, "UTF-8");

			traceit("restoreOriginalSmsApp, Original SMS App: " +defaultSmsAppPackageName);
		}
		catch (Exception e)
		{
			errorit("restoreOriginalSmsApp, Exception: " +e);
		}
		
		traceit("restoreOriginalSmsApp, Original SMS App: " +defaultSmsAppPackageName);		

		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		}else {
			DLog.log("enter restoreOriginalSmsApp and calling ACTION_CHANGE_DEFAULT case");
			Intent intent = new Intent(Sms.Intents.ACTION_CHANGE_DEFAULT);
			intent.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, defaultSmsAppPackageName);
			mAppActivity.startActivityForResult(intent, REQUEST_CODE_CHANGEBACK_SMSAPP);
		}
		
		traceit("<< restoreOriginalSmsApp");								
	}

    static final private String TAG = "EMUtilsDefaultSmsApp";

    static private void traceit(String aText)
    {
        Log.v(TAG, aText);
//        DLog.verbose(TAG, aText);
    }

    static void logit(String aText)
    {
        // Log.d(TAG, aText);
        //DLog.log(TAG, aText);
    }

    static void warnit(String aText)
    {
        Log.e(TAG, aText);
//        DLog.warn(TAG, aText);
    }

    private void errorit(String aText)
    {
        Log.e(TAG, aText);
//        DLog.error(TAG, aText);
    }	
}
