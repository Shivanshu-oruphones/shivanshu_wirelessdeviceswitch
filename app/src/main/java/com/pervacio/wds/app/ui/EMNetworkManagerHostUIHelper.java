///*************************************************************************
// *
// * Media Mushroom Limited CONFIDENTIAL
// * __________________
// *
// *  Copyright 2017 Media Mushroom Limited
// *  All Rights Reserved.
// *
// * NOTICE:  All information contained herein is, and remains
// * the property of Media Mushroom Limited.
// *
// * Dissemination of this information or reproduction of this material
// * is strictly forbidden unless prior written permission is obtained
// * from Media Mushroom Limited.
// */
//
//package com.pervacio.wds.app.ui;
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.net.wifi.WifiManager;
//import android.os.Build;
//import android.util.Base64;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import com.google.zxing.BarcodeFormat;
//import com.google.zxing.WriterException;
//import com.journeyapps.barcodescanner.BarcodeEncoder;
//import com.pervacio.wds.app.EMRemoteDeviceManager;
//import com.pervacio.wds.app.WiFiDirectServer;
//import com.pervacio.wds.custom.APPI;
//import com.pervacio.wds.custom.utils.CommonUtil;
//import com.pervacio.wds.custom.utils.Constants;
//
//import java.math.BigInteger;
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.nio.ByteOrder;
//import java.security.SecureRandom;
//
//import static android.content.Context.WIFI_SERVICE;
//
//public class EMNetworkManagerHostUIHelper implements WiFiDirectServer.Observer {
//
//    enum State {
//        CREATING_WIFI_DIRECT_NETWORK,
//        WAITING_FOR_CONNECTION
//    }
//
//    static abstract class ProgressObserver {
//        abstract void progressUpdate(State aState);
//    }
//
//    // static private EMNetworkManagerHostUIHelper mInstance = new EMNetworkManagerHostUIHelper();
//    static private EMNetworkManagerHostUIHelper mInstance;
//
//    static public void stopAnyExistingHostNetwork() {
//        if (mInstance != null) {
//            mInstance.stopHostNetwork();
//        }
//    }
//
//    static public void reset() {
//        mInstance = new EMNetworkManagerHostUIHelper();
//    }
//
//    public static void createHostNetwork(EMRemoteDeviceManager aRemoteDeviceManager,
//                                         ImageView aQRCodeImageView, // set to null if no QR code is required
//                                         TextView aSSIDTextView, // set to null if the SSID should not be displayed
//                                         TextView aPassPhrase, // set to null if the passphrase should not be displayed
//                                         Context aContext,
//                                         ProgressObserver aProgressObserver
//    ) {
//        stopAnyExistingHostNetwork();
//
//        mInstance = new EMNetworkManagerHostUIHelper();
//
//        mInstance.createHostNetworkInstance(aRemoteDeviceManager, aQRCodeImageView, aSSIDTextView, aPassPhrase, aContext, aProgressObserver);
//    }
//
//    private void stopHostNetwork() {
//        if (mWiFiServer != null) {
//            mWiFiServer.networkDestroy();
//        }
//    }
//
//    public static String getEncryptionPasscode() {
//        if (mInstance != null)
//            return mInstance.mEncryptionPasscode;
//        else
//            return null;
//    }
//
//    void createHostNetworkInstance(EMRemoteDeviceManager aRemoteDeviceManager,
//                                   ImageView aQRCodeImageView, // set to null if no QR code is required
//                                   TextView aSSIDTextView, // set to null if the SSID should not be displayed
//                                   TextView aPassPhraseTextView, // set to null if the passphrase should not be displayed
//                                   Context aContext,
//                                   ProgressObserver aProgressObserver
//    ) {
//        mContext = aContext;
//        mProgressObserver = aProgressObserver;
//        mRemoteDeviceManager = aRemoteDeviceManager;
//        mQRCodeImageView = aQRCodeImageView;
//
//        mSSIDTextView = aSSIDTextView;
//        mPassPhraseTextView = aPassPhraseTextView;
//
//        mWiFiServer = new WiFiDirectServer(this, aContext);
//
//        mProgressObserver.progressUpdate(State.CREATING_WIFI_DIRECT_NETWORK);
//        mWiFiServer.networkCreate();
//    }
//
//    @Override
//    public void onServerStatus(int aError) {
//        byte[] lanIpV4Address;
//        CommonUtil commonUtil= CommonUtil.getInstance();
//        WifiManager wifiManager = (WifiManager) emGlobals.getmContext().getSystemService(WIFI_SERVICE);
//        int lanIpAddressInt = wifiManager.getConnectionInfo().getIpAddress();
//
//        // Swap byte order if required
//        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
//            lanIpAddressInt = Integer.reverseBytes(lanIpAddressInt);
//        }
//
//        lanIpV4Address = BigInteger.valueOf(lanIpAddressInt).toByteArray();
//        byte[] zeroLanAddress = {0,0,0,0};
//        if (lanIpV4Address.length < 4)
//            lanIpV4Address = zeroLanAddress;
//
//        byte[] directIpV4Address = {0,0,0,0};
//
//        String ssid = "";
//        String pass = "";
//        String frequency="";
//
//        if (aError != 0) {
//            // Leave direct IP address as 0s
//        }
//        else {
//            WiFiDirectServer.NetworkInfo wifiDirectNetworkInfo = mWiFiServer.getNetworkInfo();
//            try {
//                InetAddress ip = InetAddress.getByName(wifiDirectNetworkInfo.mIPAddress);
//                commonUtil.setmWiFiPeerAddress(ip);
//                directIpV4Address = ip.getAddress();
//                pass = wifiDirectNetworkInfo.mPass;
//                ssid = wifiDirectNetworkInfo.mSSID;
//                frequency=wifiDirectNetworkInfo.mFreqency;
//                commonUtil.setmWiFiPeerPassphrase(pass);
//                commonUtil.setmWiFiPeerSSID(ssid);
//                if (mSSIDTextView != null)
//                    mSSIDTextView.setText(ssid);
//
//                if (mPassPhraseTextView != null)
//                    mPassPhraseTextView.setText(pass);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                // Ignore - bad IP address
//            }
//        }
//
//        // Generate a 4 digit numeric PIN to use as the encryption passphase
//        // TODO: this is consistent with the LAN mode PIN, but we may want to make it stronger in QR code mode because it is targeted at in-store use
//        SecureRandom secureRandom = new SecureRandom();
//        float randomFloat = secureRandom.nextFloat();
//        float pinFloat = randomFloat * 9999;
//        int pinInt = (int) pinFloat;
//        if (Constants.WIFI_PROBLAMATIC_MODELS.contains(Build.MODEL)) {
//            mEncryptionPasscode = String.format("%04d", Constants.DEFAULT_PIN);
//        } else {
//            mEncryptionPasscode = String.format("%04d", pinInt);
//        }
//        commonUtil.setmCryptoEncryptPass(mEncryptionPasscode);
//        if (frequency != null && !frequency.isEmpty())
//            commonUtil.setmFrequency(Integer.parseInt(frequency));
//        // mRemoteDeviceManager.start();
//
//        encodeQRCode(mQRCodeImageView,
//                lanIpV4Address,
//                directIpV4Address,
//                ssid,
//                pass,
//                mEncryptionPasscode,frequency);
//
//        mProgressObserver.progressUpdate(State.WAITING_FOR_CONNECTION);
//    }
//
//    private String mEncryptionPasscode;
//
//    @Override
//    public void onServerTrace(String aText) {
//        // Ignore
//    }
//
//    static private void encodeQRCode(ImageView aImageView,
//                      byte[] aLanIpV4,
//                      byte[] aWiFiDirectIpV4,
//                      String aSSID,
//                      String aWiFiPassPhrase,
//                      String aEncryptionPasscode,
//                      String aFrequency) {
//        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
//
//        byte[] formatAndIpAddressesBytes = new byte[9];
//        formatAndIpAddressesBytes[0] = 1; // Message format version
//        formatAndIpAddressesBytes[1] = aLanIpV4[0];
//        formatAndIpAddressesBytes[2] = aLanIpV4[1];
//        formatAndIpAddressesBytes[3] = aLanIpV4[2];
//        formatAndIpAddressesBytes[4] = aLanIpV4[3];
//        formatAndIpAddressesBytes[5] = aWiFiDirectIpV4[0];
//        formatAndIpAddressesBytes[6] = aWiFiDirectIpV4[1];
//        formatAndIpAddressesBytes[7] = aWiFiDirectIpV4[2];
//        formatAndIpAddressesBytes[8] = aWiFiDirectIpV4[3];
//
//        String formatAndIpAddressesB64 = Base64.encodeToString(formatAndIpAddressesBytes, Base64.DEFAULT);
//        formatAndIpAddressesB64 = formatAndIpAddressesB64.replace("\n", "").replace("\r", "").replace(" ", "").replace("\t", "");
//
//        String messageString = formatAndIpAddressesB64;
//        messageString += aSSID;
//        messageString += "\n";
//        messageString += aWiFiPassPhrase;
//        messageString += "\n";
//        messageString += aEncryptionPasscode;
//        messageString += "\n";
//        messageString += aFrequency;
//
//        try {
//            Bitmap qrCodeBitmap = barcodeEncoder.encodeBitmap(messageString, BarcodeFormat.QR_CODE, 1024, 1024);
//            if (aImageView != null) {
//                aImageView.setImageBitmap(qrCodeBitmap);
//            }
//        } catch (WriterException e) {
//            // TODO: handle this
//            e.printStackTrace();
//        }
//    }
//
//    private WiFiDirectServer mWiFiServer;
//    private ImageView mQRCodeImageView;
//    private Context mContext;
//    private EMRemoteDeviceManager mRemoteDeviceManager;
//    private ProgressObserver mProgressObserver;
//    private TextView mSSIDTextView;
//    private TextView mPassPhraseTextView;
//}

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

import android.content.Context;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.app.EMRemoteDeviceManager;
import com.pervacio.wds.app.WiFiDirectServer;
import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.security.SecureRandom;

import static android.content.Context.WIFI_SERVICE;

public class EMNetworkManagerHostUIHelper implements WiFiDirectServer.Observer {
    static EMGlobals emGlobals = new EMGlobals();
    enum State {
        CREATING_WIFI_DIRECT_NETWORK,
        WAITING_FOR_CONNECTION
    }

    static abstract class ProgressObserver {
        abstract void progressUpdate(State aState);
    }

    // static private EMNetworkManagerHostUIHelper mInstance = new EMNetworkManagerHostUIHelper();
    static private EMNetworkManagerHostUIHelper mInstance;

    static public void stopAnyExistingHostNetwork() {
        if (mInstance != null) {
            mInstance.stopHostNetwork();
        }
    }

    static public void reset() {
        mInstance = new EMNetworkManagerHostUIHelper();
    }

    public static void createHostNetwork(EMRemoteDeviceManager aRemoteDeviceManager,
                                         ImageView aQRCodeImageView, // set to null if no QR code is required
                                         TextView aSSIDTextView, // set to null if the SSID should not be displayed
                                         TextView aPassPhrase, // set to null if the passphrase should not be displayed
                                         Context aContext,
                                         ProgressObserver aProgressObserver
    ) {
        stopAnyExistingHostNetwork();

        mInstance = new EMNetworkManagerHostUIHelper();

        mInstance.createHostNetworkInstance(aRemoteDeviceManager, aQRCodeImageView, aSSIDTextView, aPassPhrase, aContext, aProgressObserver);
    }

    private void stopHostNetwork() {
        if (mWiFiServer != null) {
            mWiFiServer.networkDestroy();
        }
    }

    public static String getEncryptionPasscode() {
        if (mInstance != null)
            return mInstance.mEncryptionPasscode;
        else
            return null;
    }

    void createHostNetworkInstance(EMRemoteDeviceManager aRemoteDeviceManager,
                                   ImageView aQRCodeImageView, // set to null if no QR code is required
                                   TextView aSSIDTextView, // set to null if the SSID should not be displayed
                                   TextView aPassPhraseTextView, // set to null if the passphrase should not be displayed
                                   Context aContext,
                                   ProgressObserver aProgressObserver
    ) {
        mContext = aContext;
        mProgressObserver = aProgressObserver;
        mRemoteDeviceManager = aRemoteDeviceManager;
        mQRCodeImageView = aQRCodeImageView;

        mSSIDTextView = aSSIDTextView;
        mPassPhraseTextView = aPassPhraseTextView;

        mWiFiServer = new WiFiDirectServer(this, aContext);

        mProgressObserver.progressUpdate(State.CREATING_WIFI_DIRECT_NETWORK);
        mWiFiServer.networkCreate();
    }

    @Override
    public void onServerStatus(int aError) {
        byte[] lanIpV4Address;
        CommonUtil commonUtil= CommonUtil.getInstance();
//        DLog.log("Sourabh "+emGlobals.getmContext());
        WifiManager wifiManager = (WifiManager) emGlobals.getmContext().getSystemService(WIFI_SERVICE);
//        WifiManager wifiManager = (WifiManager) APPI.getAppContext().getApplicationContext().getSystemService(WIFI_SERVICE);
        int lanIpAddressInt = wifiManager.getConnectionInfo().getIpAddress();

        // Swap byte order if required
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            lanIpAddressInt = Integer.reverseBytes(lanIpAddressInt);
        }

        lanIpV4Address = BigInteger.valueOf(lanIpAddressInt).toByteArray();
        byte[] zeroLanAddress = {0,0,0,0};
        if (lanIpV4Address.length < 4)
            lanIpV4Address = zeroLanAddress;

        byte[] directIpV4Address = {0,0,0,0};

        String ssid = "";
        String pass = "";
        String frequency="";

        if (aError != 0) {
            // Leave direct IP address as 0s
        }
        else {
            WiFiDirectServer.NetworkInfo wifiDirectNetworkInfo = mWiFiServer.getNetworkInfo();
            try {
                InetAddress ip = InetAddress.getByName(wifiDirectNetworkInfo.mIPAddress);
                commonUtil.setmWiFiPeerAddress(ip);
                directIpV4Address = ip.getAddress();
                pass = wifiDirectNetworkInfo.mPass;
                ssid = wifiDirectNetworkInfo.mSSID;
                frequency=wifiDirectNetworkInfo.mFreqency;
                commonUtil.setmWiFiPeerPassphrase(pass);
                commonUtil.setmWiFiPeerSSID(ssid);
                if (mSSIDTextView != null)
                    mSSIDTextView.setText(ssid);

                if (mPassPhraseTextView != null)
                    mPassPhraseTextView.setText(pass);

                Log.d("Sourabh","Frequency "+ssid +" "+pass+" "+frequency);

            } catch (Exception e) {
                e.printStackTrace();
                // Ignore - bad IP address
            }
        }

        // Generate a 4 digit numeric PIN to use as the encryption passphase
        // TODO: this is consistent with the LAN mode PIN, but we may want to make it stronger in QR code mode because it is targeted at in-store use
        SecureRandom secureRandom = new SecureRandom();
        float randomFloat = secureRandom.nextFloat();
        float pinFloat = randomFloat * 9999;
        int pinInt = (int) pinFloat;
//        if (Constants.WIFI_PROBLAMATIC_MODELS.contains(Build.MODEL)) {
//            mEncryptionPasscode = String.format("%04d", Constants.DEFAULT_PIN);
//        } else {
        mEncryptionPasscode = String.format("%04d", pinInt);
//        }
        commonUtil.setmCryptoEncryptPass(mEncryptionPasscode);
        if (frequency != null && !frequency.isEmpty())
            commonUtil.setmFrequency(Integer.parseInt(frequency));
        // mRemoteDeviceManager.start();

        encodeQRCode(mQRCodeImageView,
                lanIpV4Address,
                directIpV4Address,
                ssid,
                pass,
                mEncryptionPasscode,frequency);

        mProgressObserver.progressUpdate(State.WAITING_FOR_CONNECTION);
    }

    private String mEncryptionPasscode;

    @Override
    public void onServerTrace(String aText) {
        // Ignore
    }

    static private void encodeQRCode(ImageView aImageView,
                                     byte[] aLanIpV4,
                                     byte[] aWiFiDirectIpV4,
                                     String aSSID,
                                     String aWiFiPassPhrase,
                                     String aEncryptionPasscode,
                                     String aFrequency) {
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

        byte[] formatAndIpAddressesBytes = new byte[9];
        formatAndIpAddressesBytes[0] = 1; // Message format version
        formatAndIpAddressesBytes[1] = aLanIpV4[0];
        formatAndIpAddressesBytes[2] = aLanIpV4[1];
        formatAndIpAddressesBytes[3] = aLanIpV4[2];
        formatAndIpAddressesBytes[4] = aLanIpV4[3];
        formatAndIpAddressesBytes[5] = aWiFiDirectIpV4[0];
        formatAndIpAddressesBytes[6] = aWiFiDirectIpV4[1];
        formatAndIpAddressesBytes[7] = aWiFiDirectIpV4[2];
        formatAndIpAddressesBytes[8] = aWiFiDirectIpV4[3];

        String formatAndIpAddressesB64 = Base64.encodeToString(formatAndIpAddressesBytes, Base64.DEFAULT);
        formatAndIpAddressesB64 = formatAndIpAddressesB64.replace("\n", "").replace("\r", "").replace(" ", "").replace("\t", "");

        String messageString = formatAndIpAddressesB64;
//        String messageString = "";
        messageString += aSSID;
        messageString += "\n";
        messageString += aWiFiPassPhrase;
        messageString += "\n";
        messageString += aEncryptionPasscode;
        messageString += "\n";
        messageString += aFrequency;

        try {
            Bitmap qrCodeBitmap = barcodeEncoder.encodeBitmap(messageString, BarcodeFormat.QR_CODE, 1024, 1024);
            if (aImageView != null) {
                aImageView.setImageBitmap(qrCodeBitmap);
            }
        } catch (WriterException e) {
            // TODO: handle this
            e.printStackTrace();
        }
    }

    private WiFiDirectServer mWiFiServer;
    private ImageView mQRCodeImageView;
    private Context mContext;
    private EMRemoteDeviceManager mRemoteDeviceManager;
    private ProgressObserver mProgressObserver;
    private TextView mSSIDTextView;
    private TextView mPassPhraseTextView;
}