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

import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import java.io.File;

public class CMDUtilsDevice
{
    private final static String kDeviceIdFileName = "device.uid";

    public static String getDeviceId(Context aContext)
    {
        String deviceId = "";

        try
        {
            ContentResolver resolver = aContext.getContentResolver();
            deviceId = Settings.Secure.getString(resolver, Settings.Secure.ANDROID_ID);
        }
        catch (Exception e)
        {
            errorit("getDeviceId, Could not get Device's ANDROID_ID" +e);
            deviceId = "";
        }

        // If we have got a device ID - return it

        if (! deviceId.equals(""))
        {
            logit("getDeviceId, ANDROID_ID: " +deviceId);
            return deviceId;
        }

        // Otherwise, make one up using the time and store it for future use
        //
        try
        {
            File storagePath  = aContext.getDir(EMConfig.EM_PRIVATE_DIR, Context.MODE_PRIVATE);

            storagePath.mkdirs();

            String deviceIdPath = storagePath.getAbsolutePath() + File.separator + kDeviceIdFileName;

            File deviceIdFile = new File(deviceIdPath);

            if (! deviceIdFile.exists())
            {
                long time = System.currentTimeMillis();

                String tempDeviceId = Long.toString(time);

                tempDeviceId = new StringBuffer(tempDeviceId).reverse().toString(); // Reverse the string

                EMUtilsFileIO.setFileContents(deviceIdPath, tempDeviceId.getBytes());
            }

            byte[] deviceIdBytes = EMUtilsFileIO.getFileContents(deviceIdPath);

            deviceId = new String(deviceIdBytes, "UTF8");
        }
        catch (Exception e)
        {
            errorit("getDeviceId, Could not get Device's ANDROID_ID" +e);

            deviceId = "";
        }

        logit("getDeviceId, Generated: " +deviceId);

        return deviceId;
    }

    public static void switchWifiOn(Context aAppContext, boolean aTurnOn)
    {
        try
        {
            WifiManager wifiManager = (WifiManager) aAppContext.getSystemService(Context.WIFI_SERVICE);

            boolean wifiOn = wifiManager.isWifiEnabled();

            logit("switchWifiOn, Current Enabled Status: " +wifiOn);

            if (aTurnOn)
            {
                if (! wifiOn)
                {
                    logit("switchWifiOn, Turning Wifi On");
                    wifiManager.setWifiEnabled(true);
                }
            }
            else // turn off
            {
                if (wifiOn)
                {
                    logit("switchWifiOn, Turning Wifi Off");
                    wifiManager.setWifiEnabled(false);
                }
            }
        }
        catch (Exception e)
        {
            errorit("switchWifiOn, Exception: " +e);
        }
    }
    //=========================================================================
    // Logging Methods
    //=========================================================================
    private final static String TAG = "EMUtilsDevice";

    @SuppressWarnings("unused")
    static private void traceit(String aText)
    {
        //Log.v(TAG, aText);
        DLog.log(aText);
    }

    static private void logit(String aText)
    {
        //// Log.d(TAG, aText);
        DLog.log(aText);
    }

    @SuppressWarnings("unused")
    static private void warnit(String aText)
    {
        //Log.e(TAG, aText);
        DLog.log(aText);
    }

    static private void errorit(String aText)
    {
        //Log.e(TAG, aText);
        DLog.log(aText);
    }
}