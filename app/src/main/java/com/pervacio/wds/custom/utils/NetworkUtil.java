package com.pervacio.wds.custom.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.custom.APPI;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Darpan Dodiya on 23-Oct-17.
 */

public class NetworkUtil {
    static EMGlobals emGlobals = new EMGlobals();
    private static final int TYPE_WIFI = 1;
    private static final int TYPE_MOBILE = 2;
    private static final int TYPE_NOT_CONNECTED = 0;
    private static final int NETWORK_STATUS_NOT_CONNECTED=0,NETWORK_STAUS_WIFI=1,NETWORK_STATUS_MOBILE=2;

    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;
        }
        return TYPE_NOT_CONNECTED;
    }

    //Returns the connected Wifi Network name if any.
    public static String getConnectedNetworkName() {
        String networkName = null;
        try {
            ConnectivityManager cm = (ConnectivityManager) emGlobals.getmContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI && netInfo.isConnectedOrConnecting()) {
                networkName = netInfo.getExtraInfo();
                if (networkName != null && networkName.startsWith(("\"")) && networkName.endsWith("\"")) {
                    networkName = networkName.substring(1, networkName.length() - 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return networkName;
    }


    public static int getConnectedNetworkId() {
        int connectedId = -1;
        try {
            WifiManager wifiManager = (WifiManager) emGlobals.getmContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                connectedId = wifiInfo.getNetworkId();
            }
            DLog.log("getConnectedNetworkId : Connected WifiNetwork id : " + connectedId);
        } catch (Exception e) {
            DLog.log("Exception in getConnectedNetworkId : " + connectedId);
        }
        return connectedId;
    }

    //Returns the ip address from the connected wifi Network
    public static String getIpAddress() {
        try {
            WifiManager wm = (WifiManager) emGlobals.getmContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wm.getConnectionInfo();
            if (wifiInfo != null) {
                return Formatter.formatIpAddress(wifiInfo.getIpAddress());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static int getConnectivityStatusString(Context context) {
        int conn = NetworkUtil.getConnectivityStatus(context);
        int status = 0;
        if (conn == NetworkUtil.TYPE_WIFI) {
            status = NETWORK_STAUS_WIFI;
        } else if (conn == NetworkUtil.TYPE_MOBILE) {
            status =NETWORK_STATUS_MOBILE;
        } else if (conn == NetworkUtil.TYPE_NOT_CONNECTED) {
            status = NETWORK_STATUS_NOT_CONNECTED;
        }
        return status;
    }

    public static boolean isOnline() {
        boolean isOnline = false;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Boolean> callable = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return isInternetAvailable();
            }
        };
        Future<Boolean> future = executor.submit(callable);
        try {
            isOnline = future.get();
            executor.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isOnline;
    }

    public static boolean isInternetAvailable() {
        boolean isInternetAvailable = false;
        try {
            InetAddress ipAddress = InetAddress.getByName("www.google.com");
            if (ipAddress != null && !ipAddress.equals("")) {
                isInternetAvailable = true;
            }
        } catch (Exception e) {
            DLog.log("Exception while checking internet availability : " + e.toString());
        }
        DLog.log("isInternetAvailable :  "+isInternetAvailable);
        return isInternetAvailable;
    }

    public static void logNetworkDetails() {
        try {
            WifiManager wifiManager = (WifiManager) emGlobals.getmContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                DLog.log("Connected Network Details : " + wifiInfo.toString());
            }
        } catch (Exception e) {
            DLog.log("Exception in logNetworkDetails : " + e.getMessage());
        }
    }

    public static void enableAllNetworks(boolean enable, int networkIdToConnect) {
        DLog.log("enableAllNetworks >>  : " + enable+" ,Networkid : "+networkIdToConnect);
        try {
            WifiManager mWifiManager = (WifiManager) emGlobals.getmContext().getSystemService(Context.WIFI_SERVICE);
            if (mWifiManager == null || mWifiManager.getConfiguredNetworks() == null) {
                DLog.log("Unable to get list of configured wifi networks");
            } else {
                List<WifiConfiguration> wifiNetworks = mWifiManager.getConfiguredNetworks();
                for (WifiConfiguration wifiConfiguration : wifiNetworks) {
                    if (enable) {
                        mWifiManager.enableNetwork(wifiConfiguration.networkId, wifiConfiguration.networkId == networkIdToConnect);
                    } else {
                        mWifiManager.disableNetwork(wifiConfiguration.networkId);
                    }
                }
                if (enable) {
                    mWifiManager.reconnect();
                }
            }
        } catch (Exception e) {
            DLog.log("enableAllNetworks >> Exception : " + e);
        }
    }

    public static boolean isWifiConnected() {
        boolean isConnected = false;
        try {
            ConnectivityManager connManager = (ConnectivityManager) emGlobals.getmContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            isConnected = mWifi.isConnected();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isConnected;
    }

    public static boolean isMobileDataEnabled() {
        TelephonyManager telephonyManager = (TelephonyManager) emGlobals.getmContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            return false;
        }
        int simState = telephonyManager.getSimState();
        if (simState != TelephonyManager.SIM_STATE_READY) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return telephonyManager.isDataEnabled();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Method getDataEnabled = telephonyManager.getClass().getDeclaredMethod("getDataEnabled");
                getDataEnabled.setAccessible(true);
                Object invoke = getDataEnabled.invoke(telephonyManager);
                return (boolean) invoke;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ConnectivityManager cm = (ConnectivityManager) emGlobals.getmContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            try {
                Method m = cm.getClass().getDeclaredMethod("getMobileDataEnabled");
                m.setAccessible(true);
                return (Boolean) m.invoke(cm);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}