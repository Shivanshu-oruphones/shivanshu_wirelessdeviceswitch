package com.pervacio.wds.app;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import com.pervacio.wds.custom.APPI;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by Surya Polasanapalli on 2/10/2018.
 */

public class HotspotServer {

    static EMGlobals emGlobals = new EMGlobals();

    private WifiManager wifiManager = null;

    public HotspotServer() {
        wifiManager = (WifiManager) emGlobals.getmContext().getSystemService(Context.WIFI_SERVICE);
    }


    private InetAddress getHotspotHostAddress() {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName("192.168.43.1");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return inetAddress;
    }

    public WifiConfiguration getWifiApConfiguration() {
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("getWifiApConfiguration");
            method.setAccessible(true);
            WifiConfiguration configuration = (WifiConfiguration) method.invoke(wifiManager);
            return configuration;
        } catch (Exception e) {
            DLog.log(e.getMessage());
            return null;
        }
    }

    public boolean toggleHotspot(boolean status) {
        if (status && getWifiState()) {
            setWifiState(false);
        }
        return changeHotspotState(status);
    }

    private boolean changeHotspotState(boolean status) {
        DLog.log("<<<< changeHotspotState " + status);
        boolean isSuccess = false;
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("setWifiApEnabled", WifiConfiguration.class,
                    Boolean.TYPE);
            method.setAccessible(true);
            WifiConfiguration configuration = status ? getWifiApConfiguration() : null;
            isSuccess = (Boolean) method.invoke(wifiManager, configuration, status);
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
        DLog.log("changeHotspotState >>>>> " + isSuccess);
        return isSuccess;
    }

    /**
     * Get the WiFi current state
     *
     * @return {@code true} if the WiFi state is enabled,{@code false} if the WiFi state is disabled
     */
    public boolean getWifiState() {
        boolean isEnabled = false;
        if (wifiManager != null) {
            isEnabled = wifiManager.isWifiEnabled();
        }
        return isEnabled;
    }

    /**
     * Set WiFi state change
     *
     * @param state {@code true} to enable, {@code false} to disable.
     * @return {@code true} if the operation succeeds,{@code false} if the operation is failed
     */
    public boolean setWifiState(boolean state) {
        boolean isChanged = false;
        if (wifiManager != null) {
            isChanged = wifiManager.setWifiEnabled(state);
        }
        return isChanged;
    }
}
