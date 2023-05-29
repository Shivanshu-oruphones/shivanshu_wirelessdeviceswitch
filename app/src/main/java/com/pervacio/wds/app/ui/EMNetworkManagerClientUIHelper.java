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

package com.pervacio.wds.app.ui;

import static com.pervacio.wds.app.ui.EasyMigrateActivity.TAG;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMConfig;
import com.pervacio.wds.app.EMDeviceInfo;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMSession;
import com.pervacio.wds.app.EMSessionDelegate;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.EMWifiPeerConnector;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.DeviceInfo;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class EMNetworkManagerClientUIHelper implements EMSessionDelegate {

    enum State {
        CONNECTING_TO_WIFI_DIRECT_NETWORK,
        TRYING_CONNECTION_TO_LAN_PEER,
        TRYING_CONNECTION_TO_WIFI_DIRECT_PEER,
        CONNECTED_TO_WIFI_DIRECT_NETWORK
    }

    static abstract class ProgressObserver {
        abstract void remoteDeviceFound(EMDeviceInfo aDeviceInfo, EMConfig.EMWiFiConnectionMode aConnectionMode);

        abstract void connectionError(int aError);

        abstract void progressUpdate(State aState);
    }

    ;

    static private EMNetworkManagerClientUIHelper mInstance = new EMNetworkManagerClientUIHelper();

    static void connectToClientNetwork(DecoratedBarcodeView aBarcodeView,
                                       ProgressObserver aObserver,
                                       Context aContext
    ) {
        mInstance.connectToHostNetwork(aBarcodeView, aObserver, aContext);
    }

    static void connectToClientNetwork(
            ProgressObserver aObserver,
            Context aContext
    ) {
        mInstance.connectToHostNetwork(aObserver, aContext);
    }

    static public void reset() {
        mInstance = new EMNetworkManagerClientUIHelper();
    }

    static public void disconnectFromAnyNetworks() {
        if (mInstance != null) {
            mInstance.disconnectFromNetwork();
        }
    }

    EMNetworkManagerClientUIHelper() {

    }

    private void disconnectFromNetwork() {
        if (mWiFiPeerConnector != null) {
            mWiFiPeerConnector.disconnect();
        }
    }

    void connectToHostNetwork(DecoratedBarcodeView aBarcodeView,
                              ProgressObserver aObserver,
                              Context aContext
    ) {
        mRemoteDevicePortNumber = EMConfig.FIXED_PORT_NUMBER;
        mBarcodeView = aBarcodeView;
        mObserver = aObserver;
        mContext = aContext;
        Log.d(TAG, "connectToHostNetwork: ");
        if (aBarcodeView != null) {
            scanQRCode(aBarcodeView);
        } else {
            //Instead of scaning we are using previously received command from ME
            getHostNetworkDetails();
        }
    }

    void connectToHostNetwork(ProgressObserver aObserver, Context aContext) {
        mRemoteDevicePortNumber = EMConfig.FIXED_PORT_NUMBER;
        mObserver = aObserver;
        mContext = aContext;
        //Instead of scaning we are using previously received command from ME
        getHostNetworkDetails();
    }

    private void getHostNetworkDetails() {
        CommonUtil commonUtil = CommonUtil.getInstance();
        mWiFiDirectPeerSSID = commonUtil.getmWiFiPeerSSID();
        mWiFiDirectPeerPassphrase = commonUtil.getmWiFiPeerPassphrase();
        mEncryptionPasscode = commonUtil.getmCryptoEncryptPass();
        mWiFiDirectPeerAddress = commonUtil.getmWiFiPeerAddress();
        mFrequency = String.valueOf(commonUtil.getmFrequency());
        mObserver.progressUpdate(State.TRYING_CONNECTION_TO_WIFI_DIRECT_PEER);
        Log.d(TAG, "getHostNetworkDetails: " + mWiFiDirectPeerSSID + " " + mWiFiDirectPeerPassphrase + " " + mEncryptionPasscode + " " + mWiFiDirectPeerAddress + " " + mFrequency);
        //If mode is wlan establishing the connection (BB-BB,BB-IOS)
        startPairing();
        connectToWiFiDirectGroup();
        if (Constants.mTransferMode.equalsIgnoreCase(Constants.P2P_MODE)) {
            //this connection part will be handled in OnConnectionInfoAvailable() method
        } else if (Constants.mTransferMode.equalsIgnoreCase("WLAN")) {
            startPairing();
        } else {
            if (canConnect()) {
                if (Constants.PLATFORM.equalsIgnoreCase(Constants.PLATFORM_BLACKBERRY)) {
                    mObserver.connectionError(11);
                } else {
                    connectToWiFiDirectGroup();
                }
            } else {
                mInstance.mEncryptionPasscode = null;
                mObserver.connectionError(3);
            }
        }
    }

    public static EMNetworkManagerClientUIHelper getInstance() {
        if (mInstance == null) {
            mInstance = new EMNetworkManagerClientUIHelper();
        }
        return mInstance;
    }

    public static void startPairing() {
        mInstance.mObserver.progressUpdate(EMNetworkManagerClientUIHelper.State.TRYING_CONNECTION_TO_WIFI_DIRECT_PEER);
        mInstance.attemptConnectionToPeer(mInstance.mWiFiDirectPeerAddress);
    }

    private void scanQRCode(DecoratedBarcodeView aBarcodeView) {
        aBarcodeView.resume();
        aBarcodeView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult barcodeResult) {
                String messageString = barcodeResult.getText();
                DLog.log("Got QR code message string: " + messageString);
                try {
                    String base64String = messageString.substring(0, 12);
                    byte[] messageBytes = Base64.decode(base64String, Base64.DEFAULT);

                    if (messageBytes[0] != 1) {
                        // It should always be 1 - at least for now
                        // TODO: handle this
                    }

                    byte[] lanIpV4Bytes = new byte[4];
                    lanIpV4Bytes[0] = messageBytes[1];
                    lanIpV4Bytes[1] = messageBytes[2];
                    lanIpV4Bytes[2] = messageBytes[3];
                    lanIpV4Bytes[3] = messageBytes[4];
                    try {
                        mLanPeerAddress = Inet4Address.getByAddress(lanIpV4Bytes);
                    } catch (UnknownHostException e) {
                        // Leave as null if there are any problems
                    }

                    byte[] wifiDirectIpV4Bytes = new byte[4];
                    wifiDirectIpV4Bytes[0] = messageBytes[5];
                    wifiDirectIpV4Bytes[1] = messageBytes[6];
                    wifiDirectIpV4Bytes[2] = messageBytes[7];
                    wifiDirectIpV4Bytes[3] = messageBytes[8];
                    try {
                        mWiFiDirectPeerAddress = Inet4Address.getByAddress(wifiDirectIpV4Bytes);
                    } catch (UnknownHostException e) {
                        // Leave as null if there are any problems
                    }

                    String ssidAndPassphrase = messageString.substring(12, messageString.length());
                    String[] ssidAndPassphraseSplit = ssidAndPassphrase.split("\n");
                    if (ssidAndPassphraseSplit.length > 2) {
                        mWiFiDirectPeerSSID = ssidAndPassphraseSplit[0];
                        mWiFiDirectPeerPassphrase = ssidAndPassphraseSplit[1];
                        mEncryptionPasscode = ssidAndPassphraseSplit[2];
                        mFrequency = ssidAndPassphraseSplit[3];
                    }

                    if (mLanPeerAddress != null)
                        DLog.log("EMQRCodeScanner: lanIpV4Address " + mLanPeerAddress.toString());

                    if (mWiFiDirectPeerAddress != null)
                        DLog.log("EMQRCodeScanner: wifiDirectIpV4Address " + mWiFiDirectPeerAddress.toString());

                    if (mWiFiDirectPeerSSID != null)
                        DLog.log("EMQRCodeScanner: ssid " + mWiFiDirectPeerSSID);

                    if (mWiFiDirectPeerPassphrase != null)
                        DLog.log("EMQRCodeScanner: passphrase " + mWiFiDirectPeerPassphrase);

                    if (mFrequency != null)
                        DLog.log("EMQRCodeScanner: Frequency " + mFrequency);

                    CommonUtil commonUtil = CommonUtil.getInstance();
                    commonUtil.setmWiFiPeerSSID(mWiFiDirectPeerSSID);
                    commonUtil.setmWiFiPeerPassphrase(mWiFiDirectPeerPassphrase);
                    EMUtility.copyTextToClipboard("password", mWiFiDirectPeerPassphrase);
                    Toast.makeText(mContext, "Wifi Password copied to clipboard", Toast.LENGTH_LONG).show();
                    commonUtil.setmCryptoEncryptPass(mEncryptionPasscode);
                    commonUtil.setmWiFiPeerAddress(mWiFiDirectPeerAddress);
                    commonUtil.setmFrequency(Integer.parseInt(mFrequency));

                    if (canConnect()) {
                        if (Constants.PLATFORM.equalsIgnoreCase(Constants.PLATFORM_BLACKBERRY)) {
                            mObserver.connectionError(11);
                        } else {
                            tryConnection();
                        }
                    } else {
                        mEncryptionPasscode = null;
                        mObserver.connectionError(3);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    DLog.log("Got exception in QR Code scan : " + e);
                    mObserver.connectionError(4);
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> list) {
                // Ignore
            }
        });
    }

    private void connectToWiFiDirectGroup() {
        disconnectFromAnyNetworks();

        abstract class WiFiPeerConnectionListener implements EMWifiPeerConnector.Listener {
        }
        ;

        mWiFiPeerConnector = new EMWifiPeerConnector(new WiFiPeerConnectionListener() {
            @Override
            public void onWifiPeerConnection(boolean aSuccess) {
                Log.d(TAG, "onWifiPeerConnection: " + mWiFiDirectPeerAddress);
                DLog.log("attemptConnectionToPeer : " + aSuccess);
                if (aSuccess) {
                    if ((CommonUtil.getInstance().getMigrationStatus() == Constants.REVIEW_INPROGRESS)) {
                        attemptConnectionToPeer(mWiFiDirectPeerAddress);
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                            attemptConnectionToPeer(mWiFiDirectPeerAddress);
                        }
                    } else {
                        mObserver.progressUpdate(State.CONNECTED_TO_WIFI_DIRECT_NETWORK);
                    }
                } else {
                    attemptConnectionToPeer(mWiFiDirectPeerAddress);
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        for (int i = 0; i < 20; i++) {
                            attemptConnectionToPeer(mWiFiDirectPeerAddress);
                        }
                    }
                    // Could not connect to peer, so try next conneciton method
                    // tryNextConnectionMethod();
                    // TODO: should we record that it was not possible to connect to the WiFi network? It might be useful to report it later if LAN connection doesn't work either
//                    mEncryptionPasscode = null;
//                    mObserver.connectionError(3);
                }
            }
        });

        if ((mWiFiDirectPeerSSID != null) && (!mWiFiDirectPeerSSID.equals(""))) {
            mObserver.progressUpdate(State.CONNECTING_TO_WIFI_DIRECT_NETWORK);
            mWiFiPeerConnector.start(mWiFiDirectPeerSSID, mWiFiDirectPeerPassphrase);
        } else {
            // There is no valid SSID to connect to, so try the next connection method if there is one
            tryNextConnectionMethod();
        }
    }

    private Timer mTimeoutTimer = new Timer();

    private Handler mTimeoutHandler = new Handler() {
        public void handleMessage(Message msg) { // Dispatch timeout on the main thread
            DLog.log("*** Handshake to WiFi Direct peer failed - trying again");
            doAttemptConnectionToPeer();
        }
    };

    private void startOrResetTimeoutTimer() {
        stopTimeoutTimer();

        try {
            mTimeoutTimer = new Timer();
            mTimeoutTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mTimeoutHandler.obtainMessage(1).sendToTarget();
                }
            }, EMConfig.WIFI_DIRECT_RECONNECTION_TIMER_IN_SECONDS * 1000);
        } catch (Exception ex) {
            // If we can't set the timeout timer for some reason then just ignore
        }
    }

    private void stopTimeoutTimer() {
        if (mTimeoutTimer != null) {
            mTimeoutTimer.cancel();
        }
    }

    private void doAttemptConnectionToPeer() {
        if (mHandshakeSession != null) {
            mHandshakeSession.cancel();
            mHandshakeSession = null;
        }
        CommonUtil.getInstance().setGroupOwner(false);

        startOrResetTimeoutTimer();

        mHandshakeSession = new EMSession(mWiFiDirectPeerAddress, EMConfig.FIXED_PORT_NUMBER, this, mContext, null, true);
        mHandshakeSession.handshakeWithServer();
    }

    public void attemptConnectionToPeer(final InetAddress aRemoteDeviceAddress) {
        mRemoteDeviceAddress = aRemoteDeviceAddress;
        mWiFiDirectPeerAddress = aRemoteDeviceAddress;
        doAttemptConnectionToPeer();
    }

    private EMConfig.EMWiFiConnectionMode currentConnectionAttemptMode() {
        return EMConfig.WIFI_CONNECTION_PREFERENCE_ORDER[mConnectionTypeIndex];
    }

    private void tryConnection() {
        switch (currentConnectionAttemptMode()) {
            case LAN:
                mObserver.progressUpdate(State.TRYING_CONNECTION_TO_LAN_PEER);
                attemptConnectionToPeer(mLanPeerAddress);
                break;
            case DIRECT:
                mObserver.progressUpdate(State.TRYING_CONNECTION_TO_WIFI_DIRECT_PEER);
                connectToWiFiDirectGroup();
                break;
        }
    }

    private boolean tryNextConnectionMethod() {
        mConnectionTypeIndex++;
        if (mConnectionTypeIndex < EMConfig.WIFI_CONNECTION_PREFERENCE_ORDER.length) {
            tryConnection();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void disconnected(EMSession aSession) {

    }

    @Override
    public void progressUpdate(EMProgressInfo aProgressInfo) {

    }

    @Override
    public void handshakeComplete(EMDeviceInfo aRemoteDeviceInfo) {
        stopTimeoutTimer();
        EMDeviceInfo deviceInfo = new EMDeviceInfo();
        deviceInfo.mDeviceName = "QR-Code-Device";
        deviceInfo.mCapabilities = 0xFFFFFFFF;
        deviceInfo.mIpV4Address = mRemoteDeviceAddress; // TODO: should we also support IPv6?
        deviceInfo.mPort = mRemoteDevicePortNumber;
        deviceInfo.mRoles = aRemoteDeviceInfo.mRoles;
        deviceInfo.mServiceName = "QR-Code-Service";
        deviceInfo.mDeviceUniqueId = aRemoteDeviceInfo.mDeviceUniqueId;

        mObserver.remoteDeviceFound(deviceInfo, EMConfig.EMWiFiConnectionMode.DIRECT);
    }

    @Override
    public void haveBecomeSource(EMSession aMainSession) {

    }

    @Override
    public void haveBecomeTarget(EMSession aMainSession) {

    }

    @Override
    public void error(int aError) {
        // TODO: handle connection failures
        mObserver.connectionError(3);
    }

    @Override
    public EMDeviceInfo getDeviceInfo(boolean isSource) {
        DLog.log("getDeviceInfo of ClientUI helper called. isSource: " + isSource);
        EMDeviceInfo deviceInfo = new EMDeviceInfo();
        DeviceInfo mDeviceInfo = DeviceInfo.getInstance();
        deviceInfo.mDeviceName = Build.MODEL;
        try {
            deviceInfo.mIpV4Address = InetAddress.getByName("0.0.0.0");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        deviceInfo.mPort = 0;
        deviceInfo.mCapabilities = EMConfig.DEVICE_CAPABILITIES;
        deviceInfo.mRoles = EMConfig.SUPPORTED_ROLES;
        deviceInfo.mServiceName = "QR-CODE-SERVICE";
        deviceInfo.mKeyboardShortcutImporterAvailable = false;
        deviceInfo.mDeviceUniqueId = mDeviceInfo.get_serialnumber();
        deviceInfo.dbDeviceBuildNumber = mDeviceInfo.getBuildNumber();
//        deviceInfo.dbDeviceFirmware = mDeviceInfo.getFirmwareVersion();
        deviceInfo.dbDeviceMake = Build.MANUFACTURER;
        deviceInfo.dbDeviceModel = Build.MODEL;
        deviceInfo.dbDeviceOSVersion = mDeviceInfo.getOSversion();
        deviceInfo.dbDevicePlatform = Constants.PLATFORM;
        deviceInfo.dbDeviceIMEI = mDeviceInfo.get_imei();
        deviceInfo.dbDeviceTotalStorage = mDeviceInfo.getTotalInternalStroage();
        deviceInfo.dbDeviceFreeStorage = mDeviceInfo.getAvailableInternalStorage();
        deviceInfo.appVersion = "V1.0.23";
        if (DashboardLog.getInstance().isThisDest()) {
            deviceInfo.dbOperationType = Constants.OPERATION_TYPE.RESTORE.value();
        } else {
            deviceInfo.dbOperationType = Constants.OPERATION_TYPE.BACKUP.value();
        }

        return deviceInfo;
    }

    @Override
    public void pinOk() {

    }

    @Override
    public void cryptoPasswordRequested() {

    }

    @Override
    public void nonHandshakeConnectionFromRemoteDeviceOnNewSession(EMSession aReconnectedSession) {

    }

    public static String getEncryptionPasscode() {
        return mInstance.mEncryptionPasscode;
    }

    public static void setEncryptionPasscode(String password) {
        mInstance.mEncryptionPasscode = password;
    }

    private DecoratedBarcodeView mBarcodeView;
    private String mWiFiDirectPeerSSID;
    private String mWiFiDirectPeerPassphrase;
    private InetAddress mLanPeerAddress;
    private InetAddress mWiFiDirectPeerAddress;
    private int mRemoteDevicePortNumber = EMConfig.FIXED_PORT_NUMBER;
    private int mConnectionTypeIndex = 0;
    private ProgressObserver mObserver;
    private String mEncryptionPasscode;
    private EMWifiPeerConnector mWiFiPeerConnector;

    private EMSession mHandshakeSession;

    private InetAddress mRemoteDeviceAddress;

    private Context mContext;

    private String mFrequency;

    private boolean canConnect() {
        try {
            DLog.log("<--canConnect");
            boolean is5GHZSupported = DeviceInfo.getInstance().is5GhzSupported();
            if (TextUtils.isEmpty(mWiFiDirectPeerSSID)) {
                DLog.log("Cant connect to this network, network name is null : " + mWiFiDirectPeerSSID);
                return false;
            } else if (Constants.PLATFORM.equalsIgnoreCase(Constants.PLATFORM_BLACKBERRY)) {
                return true;
            } else if (Constants.SWITCH_TO_SORUCE_5GHZ && CommonUtil.getInstance().getNetworkType().equalsIgnoreCase("P2P") && CommonUtil.getInstance().isSource() && is5GHZSupported && CommonUtil.getInstance().isRemoteDeviceDualBand() && Integer.parseInt(mFrequency) < 3000 && Build.VERSION.SDK_INT < 30) {
                DLog.log("Not connecting to this network,Lower frequency,network Frequency : " + mFrequency);
                return false;
            } else if (!TextUtils.isEmpty(mFrequency) && Integer.parseInt(mFrequency) > 3000 && !is5GHZSupported) {
                DLog.log("Cant connect to this network, Higher frequency,network Frequency : " + mFrequency);
                return false;
            }
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
        DLog.log("Connecting to network : " + mWiFiDirectPeerSSID + "  canConnect -->");
        return true;
    }
}