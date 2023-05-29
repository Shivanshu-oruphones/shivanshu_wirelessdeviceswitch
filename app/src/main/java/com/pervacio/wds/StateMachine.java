//package com.pervacio.wds;
//
//import android.content.Context;
//import android.net.wifi.WifiConfiguration;
//import android.os.Handler;
//import android.os.Message;
//
//import com.pervacio.vcard.Base64;
//import com.pervacio.wds.app.DLog;
//import com.pervacio.wds.app.EMConfig;
//import com.pervacio.wds.app.EMServer;
//import com.pervacio.wds.app.EMUtility;
//import com.pervacio.wds.app.HotspotServer;
//import com.pervacio.wds.custom.utils.CPServerCallBacks;
//import com.pervacio.wds.custom.utils.CommonUtil;
//import com.pervacio.wds.custom.utils.Constants;
//import com.pervacio.wds.custom.utils.DeviceInfo;
//import com.pervacio.wds.custom.utils.NetworkUtil;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.ByteArrayOutputStream;
//import java.io.DataOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InterruptedIOException;
//import java.io.UnsupportedEncodingException;
//import java.net.HttpURLConnection;
//import java.net.SocketTimeoutException;
//import java.net.URL;
//
//import static com.pervacio.wds.custom.utils.Constants.CLOUDPAIRING_URL;
//import static com.pervacio.wds.custom.utils.Constants.HTTP_AUTHENTICATION_PASSWORD;
//import static com.pervacio.wds.custom.utils.Constants.HTTP_AUTHENTICATION_USERNAME;
//
///**
// * Created by Surya Polasanapalli on 3/6/2018.
// */
//
//public class StateMachine {
//
//    public static final String DEVICE_INFO = "DEVICE_INFO";
//    public static final String WIFI_INFO = "WIFI_INFO";
//    private static StateMachine instance = null;
//    private final String POST = "POST";
//    private final String GET = "GET";
//    private final String SRC = "src";
//    private final String DEST = "dest";
//    private final String GETDATA = "dgd";
//    private final String PUTDATA = "dpd";
//    private final String DATA_TYPE = "dataType";
//    ServerThread mServerThread = null;
//    private Context mContext;
//    private String requestType = null;
//    private String mServerURL = null;
//    private String postData = null;
//    private CPServerCallBacks mCommandHandler = null;
//    private int mRole = 0;
//    private String sessionID = null;
//    private int dataRecordSeq = 0;
//    private String mPin = null;
//    private Handler mHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            String response = (String) msg.obj;
//            if (mCommandHandler != null) {
//                mCommandHandler.onresponseReceived(response);
//            }
//        }
//    };
//    private HttpURLConnection urlConnection;
//
//    private StateMachine(Context context) {
//        if (context != null) {
//            mCommandHandler = (CPServerCallBacks) context;
//            mContext = context;
//        }
//    }
//
//    public static StateMachine getInstance(Context context, int role) {
//        if (instance == null) {
//            instance = new StateMachine(context);
//        }
//        instance.mRole = role;
//        return instance;
//    }
//
//    private static String readStream(InputStream in) {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        byte[] buffer = new byte[1024];
//        int length = 0;
//        try {
//            while ((length = in.read(buffer)) != -1) {
//                baos.write(buffer, 0, length);
//            }
//        } catch (IOException e) {
//            DLog.log("Cloud Pairing :: "+e.getMessage());
//        }
//        try {
//            return baos.toString("UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            DLog.log("UnsupportedEncodingException UTF8.");
//            return baos.toString();
//        }
//    }
//
//    public int getDataRecordSeq() {
//        return dataRecordSeq;
//    }
//
//    public void setDataRecordSeq(int dataRecordSeq) {
//        this.dataRecordSeq = dataRecordSeq;
//    }
//
//    public String getSessionID() {
//        return sessionID;
//    }
//
//    public void setSessionID(String sessionID) {
//        this.sessionID = sessionID;
//    }
//
//    public String getmPin() {
//        return mPin;
//    }
//
//    public void setmPin(String mPin) {
//        this.mPin = mPin;
//    }
//
//    private String doServerCall(String serverUrl, String data, String requestType) {
//        String result = null;
//        if (!NetworkUtil.isInternetAvailable()) {
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "NO_INTERNET";
//        }
//
//        try {
//            URL url;
//            DLog.log("Cloud Pairing :: URL : " + serverUrl);
//            DLog.log("Cloud Pairing :: Request type : " + requestType);
//            DLog.log("Cloud Pairing :: Data : " + data);
//            //String credentials = HTTP_AUTHENTICATION_USERNAME + ":" + HTTP_AUTHENTICATION_PASSWORD;
//            String basicAuth = EMUtility.getAuthToken();//"Basic " + new String(new Base64().encode(credentials.getBytes()));
//            int timeout = 0;
//           /* if(requestType.equalsIgnoreCase(POST)){
//                timeout = 30000;
//            }*/
//            try {
//                url = new URL(serverUrl);
//                urlConnection = (HttpURLConnection) url.openConnection();
//                urlConnection.setConnectTimeout(0);
//                urlConnection.setReadTimeout(timeout);
//                urlConnection.setRequestMethod(requestType);
//                urlConnection.setDoOutput(true);
//                urlConnection.setDoInput(true);
//                urlConnection.setRequestProperty("Authorization", basicAuth);
////                urlConnection.setRequestProperty("uuid",DeviceInfo.getInstance().getSingleIMEI());
//                urlConnection.setRequestProperty("uuid",DeviceInfo.getInstance().get_imei());
//                urlConnection.setRequestProperty("Content-Type", "application/json");
//                urlConnection.setRequestProperty("Accept", "application/json");
//                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
//                if (requestType.equalsIgnoreCase(POST))
//                    wr.write(data.getBytes());
//
//                InputStream is;
//                boolean isSuccess = false;
//
//                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
//                    isSuccess = true;
//                    is = urlConnection.getInputStream();
//                } else {
//                    is = urlConnection.getErrorStream();
//                }
//
//                if (is == null) {
//                    DLog.log("Cloud Pairing :: Response Stream is null...");
//                } else {
//                    result = readStream(is);
//                }
//
//                DLog.log("Cloud Pairing :: Response: " + result);
//                DLog.log("Cloud Pairing :: Response Code: " + urlConnection.getResponseCode());
//                DLog.log("Cloud Pairing :: Response Message: " + urlConnection.getResponseMessage());
//                if (!isSuccess) {
//                    DLog.log("Getting Error from server, returning response as ERROR");
//                    result = "ERROR";
//                }
//            } catch (SocketTimeoutException e) {
//                DLog.log("SocketTimeoutException :" + e.getMessage());
//                return "TIMEOUT";
//            } catch (InterruptedIOException e) {
//                DLog.log("Interrupted " + e.getMessage());
//                return null;
//            } catch (Exception e) {
//                DLog.log("Exception  " + e.getMessage());
//                return "ERROR";
//            }
//        } catch (Exception e) {
//            DLog.log("In TransactionLogging. Encountered exception: " + e.getMessage());
//            return null;
//        }
//        return result;
//
//    }
//
//    private String getServerURL(String requestType) {
//        StringBuffer serverUrl = new StringBuffer(Constants.CLOUDPAIRING_URL);
//        String requestQueue;
//        if (requestType.equalsIgnoreCase(POST)) {
//            serverUrl.append("/dpd");
//            requestQueue = "/" + sessionID + "?dataType=" + WIFI_INFO;
//        } else {
//            serverUrl.append("/dgd");
//            requestQueue = "/" + sessionID + "?dataRecordSeq=" + dataRecordSeq;
//        }
//        if (mRole == 2) {
//            serverUrl.append("/dest");
//        } else {
//            serverUrl.append("/src");
//        }
//        serverUrl.append(requestQueue);
//        return String.valueOf(serverUrl);
//    }
//
//    public void getData() {
//        DLog.log("--getData--");
//        requestType = GET;
//        postData = "";
//        mServerURL = getServerURL(GET);
//        startServiceCall();
//    }
//
//    public void postData(String data) {
//        requestType = POST;
//        postData = getDetails(data);
//        mServerURL = getServerURL(POST);
//        startServiceCall();
//    }
//
//    public void createSession() {
//        DLog.log("--createSession--");
//        killExistingConnections();
//        requestType = POST;
//        postData = getDetails(DEVICE_INFO);
//        mServerURL = CLOUDPAIRING_URL + "/dcs" + "?dataType=" + DEVICE_INFO;
//        startServiceCall();
//    }
//
//    public void createSessionWithSessionID() {
//        DLog.log("--createSession--");
//        killExistingConnections();
//        requestType = POST;
//        postData = getDetails(DEVICE_INFO);
//        mServerURL = CLOUDPAIRING_URL + "/dcs" + "?sessionId=" + sessionID + "&dataType=" + DEVICE_INFO;
//        startServiceCall();
//    }
//
//     public void validateSession(String pin) {
//        DLog.log("--validateSession--");
//        killExistingConnections();
//        mPin = pin;
//        requestType = POST;
//        postData = getDetails(DEVICE_INFO);
//        mServerURL = CLOUDPAIRING_URL + "/das/" + mPin + "?dataType=" + DEVICE_INFO;
//        startServiceCall();
//    }
//
//    public void endSession() {
//        killExistingConnections();
//        if (sessionID != null) {
//            requestType = POST;
//            postData = "";
//            mServerURL = CLOUDPAIRING_URL + "/des/" + sessionID;
//            startServiceCall();
//        }
//    }
//
//    private void killExistingConnections() {
//        try {
//            if (mServerThread != null) {
//                mServerThread.stopExistingConnections();
//                mServerThread.interrupt();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void startServiceCall() {
//        DLog.log("--startServiceCall--");
//        mServerThread = new ServerThread();
//        mServerThread.start();
//    }
//
//    public String getDetails(String type) {
//        JSONObject jsonObject = new JSONObject();
//
//        if (type.equalsIgnoreCase(DEVICE_INFO))
//            try {
//                jsonObject.put("platform", Constants.PLATFORM);
//                jsonObject.put("dualBandSupported", DeviceInfo.getInstance().is5GhzSupported());
//                jsonObject.put("isP2PSupported", DeviceInfo.getInstance().isP2PSupported());
//                jsonObject.put("deviceAddress",CommonUtil.getInstance().getDeviceAddress());
//                DLog.log("enter getDetails deviceAddress "+CommonUtil.getInstance().getDeviceAddress()+" added to DEVICE_INFO");
//                jsonObject.put("deviceNameP2P", CommonUtil.getInstance().getDeviceNameP2P());
//                DLog.log("enter getDetails deviceNameP2P " + CommonUtil.getInstance().getDeviceNameP2P() + " added to DEVICE_INFO");
//                jsonObject.put("model",DeviceInfo.getInstance().get_model());
//                jsonObject.put("make", DeviceInfo.getInstance().get_make());
//                jsonObject.put("isP2PModel", Constants.P2P_MODELS.contains(DeviceInfo.getInstance().get_model()));
////                jsonObject.put("imei", DeviceInfo.getInstance().getSingleIMEI());
//                jsonObject.put("imei", DeviceInfo.getInstance().get_imei());
//                if (!CommonUtil.getInstance().isMigrationInterrupted())
//                    managePorts(jsonObject);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        else {
//            CommonUtil commonUtil = CommonUtil.getInstance();
//            if (commonUtil.getmCryptoEncryptPass().isEmpty()) {
//                WifiConfiguration details = new HotspotServer().getWifiApConfiguration();
//                try {
//                    jsonObject.put("TYPE", "hotspot");
//                    jsonObject.put("SSID", details.SSID);
//                    jsonObject.put("PASSWORD", details.preSharedKey);
//                    jsonObject.put("IP", "192.168.43.1");
//                    jsonObject.put("FREQUENCY", "2500");
//                    jsonObject.put("PIN", String.valueOf(Constants.DEFAULT_PIN));
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            } else {
//                try {
//                    jsonObject.put("TYPE",commonUtil.getNetworkType());
//                    jsonObject.put("SSID", commonUtil.getmWiFiPeerSSID());
//                    jsonObject.put("PASSWORD", commonUtil.getmWiFiPeerPassphrase());
//                    jsonObject.put("IP", commonUtil.getmWiFiPeerAddress());
//                    jsonObject.put("FREQUENCY", commonUtil.getmFrequency());
//                    jsonObject.put("PIN", commonUtil.getmCryptoEncryptPass());
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        return jsonObject.toString();
//    }
//
//    class ServerThread extends Thread {
//        @Override
//        public void run() {
//            try {
//                String response = doServerCall(mServerURL, postData, requestType);
//                Message msg = new Message();
//                msg.obj = response;
//                mHandler.sendMessage(msg);
//            } catch (Exception exception) {
//                DLog.log(exception);
//            }
//        }
//
//        void stopExistingConnections() {
//            try {
//                if (urlConnection != null) {
//                    urlConnection.disconnect();
//                }
//            } catch (Exception e) {
//                DLog.log("stopExistingConnections: "+e);
//            }
//        }
//    }
//
//    public void clean() {
//        killExistingConnections();
//        sessionID = null;
//        mPin = null;
//        dataRecordSeq = 0;
//    }
//
//
//    /**
//     * Method to check and get Available ports.
//     **/
//
//    private void managePorts(JSONObject jsonObject) {
//        try {
//            if (EMUtility.isLocalPortInUse(EMConfig.COMMUNICATION_PORT_NUMBER)) {
//                EMConfig.FIXED_PORT_NUMBER = EMUtility.getAvailblePort(EMConfig.FIXED_PORT_NUMBER, false);
//                jsonObject.put("fixedPort", EMConfig.FIXED_PORT_NUMBER);
//            }
//            if (EMUtility.isLocalPortInUse(EMConfig.DATA_TRANSFER_PORT_NUMBER)) {
//                EMServer.CONTENT_TRANSFER_PORT = EMUtility.getAvailblePort(EMServer.CONTENT_TRANSFER_PORT, true);
//                jsonObject.put("dataTransferPort", EMServer.CONTENT_TRANSFER_PORT);
//            }
//            DLog.log("managed fixed and transfer ports : " + EMConfig.FIXED_PORT_NUMBER + " , " + EMServer.CONTENT_TRANSFER_PORT);
//        } catch (Exception e) {
//            DLog.log("got exception while managing ports : " + e.getMessage());
//        }
//    }
//}

package com.pervacio.wds;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Handler;
import android.os.Message;

import com.pervacio.vcard.Base64;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMConfig;
import com.pervacio.wds.app.EMServer;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.HotspotServer;
import com.pervacio.wds.custom.utils.CPServerCallBacks;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DeviceInfo;
import com.pervacio.wds.custom.utils.NetworkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import static com.pervacio.wds.custom.utils.Constants.CLOUDPAIRING_URL;
import static com.pervacio.wds.custom.utils.Constants.HTTP_AUTHENTICATION_PASSWORD;
import static com.pervacio.wds.custom.utils.Constants.HTTP_AUTHENTICATION_USERNAME;

/**
 * Created by Surya Polasanapalli on 3/6/2018.
 */

public class StateMachine {

    public static final String DEVICE_INFO = "DEVICE_INFO";
    public static final String WIFI_INFO = "WIFI_INFO";
    private static StateMachine instance = null;
    private final String POST = "POST";
    private final String GET = "GET";
    private final String SRC = "src";
    private final String DEST = "dest";
    private final String GETDATA = "dgd";
    private final String PUTDATA = "dpd";
    private final String DATA_TYPE = "dataType";
    ServerThread mServerThread = null;
    private Context mContext;
    private String requestType = null;
    private String mServerURL = null;
    private String postData = null;
    private CPServerCallBacks mCommandHandler = null;
    private int mRole = 0;
    private String sessionID = null;
    private int dataRecordSeq = 0;
    private String mPin = null;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String response = (String) msg.obj;
            if (mCommandHandler != null) {
                mCommandHandler.onresponseReceived(response);
            }
        }
    };
    private HttpURLConnection urlConnection;

    private StateMachine(Context context) {
        if (context != null) {
            mCommandHandler = (CPServerCallBacks) context;
            mContext = context;
        }
    }

    public static StateMachine getInstance(Context context, int role) {
        if (instance == null) {
            instance = new StateMachine(context);
        }
        instance.mRole = role;
        return instance;
    }

    private static String readStream(InputStream in) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        try {
            while ((length = in.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
        } catch (IOException e) {
            DLog.log("Cloud Pairing :: "+e.getMessage());
        }
        try {
            return baos.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            DLog.log("UnsupportedEncodingException UTF8.");
            return baos.toString();
        }
    }

    public int getDataRecordSeq() {
        return dataRecordSeq;
    }

    public void setDataRecordSeq(int dataRecordSeq) {
        this.dataRecordSeq = dataRecordSeq;
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public String getmPin() {
        return mPin;
    }

    public void setmPin(String mPin) {
        this.mPin = mPin;
    }

    private String doServerCall(String serverUrl, String data, String requestType) {
        String result = null;
        if (!NetworkUtil.isInternetAvailable()) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "NO_INTERNET";
        }

//        try {
//            URL url;
//            DLog.log("Cloud Pairing :: URL : " + serverUrl);
//            DLog.log("Cloud Pairing :: Request type : " + requestType);
//            DLog.log("Cloud Pairing :: Data : " + data);
//            //String credentials = HTTP_AUTHENTICATION_USERNAME + ":" + HTTP_AUTHENTICATION_PASSWORD;
//            String basicAuth = EMUtility.getAuthToken();//"Basic " + new String(new Base64().encode(credentials.getBytes()));
//            int timeout = 0;
//           /* if(requestType.equalsIgnoreCase(POST)){
//                timeout = 30000;
//            }*/
//            try {
//                url = new URL(serverUrl);
//                urlConnection = (HttpURLConnection) url.openConnection();
//                urlConnection.setConnectTimeout(0);
//                urlConnection.setReadTimeout(timeout);
//                urlConnection.setRequestMethod(requestType);
//                urlConnection.setDoOutput(true);
//                urlConnection.setDoInput(true);
//                urlConnection.setRequestProperty("Authorization", basicAuth);
////                urlConnection.setRequestProperty("uuid",DeviceInfo.getInstance().getSingleIMEI());
//                urlConnection.setRequestProperty("uuid",DeviceInfo.getInstance().get_imei());
//                urlConnection.setRequestProperty("Content-Type", "application/json");
//                urlConnection.setRequestProperty("Accept", "application/json");
//                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
//                if (requestType.equalsIgnoreCase(POST))
//                    wr.write(data.getBytes());
//
//                InputStream is;
//                boolean isSuccess = false;
//
//                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
//                    isSuccess = true;
//                    is = urlConnection.getInputStream();
//                } else {
//                    is = urlConnection.getErrorStream();
//                }
//
//                if (is == null) {
//                    DLog.log("Cloud Pairing :: Response Stream is null...");
//                } else {
//                    result = readStream(is);
//                }
//
//                DLog.log("Cloud Pairing :: Response: " + result);
//                DLog.log("Cloud Pairing :: Response Code: " + urlConnection.getResponseCode());
//                DLog.log("Cloud Pairing :: Response Message: " + urlConnection.getResponseMessage());
//                if (!isSuccess) {
//                    DLog.log("Getting Error from server, returning response as ERROR");
//                    result = "ERROR";
//                }
//            } catch (SocketTimeoutException e) {
//                DLog.log("SocketTimeoutException :" + e.getMessage());
//                return "NO_INTERNET";
//            } catch (InterruptedIOException e) {
//                DLog.log("Interrupted " + e.getMessage());
//                return "NO_INTERNET";
//            } catch (Exception e) {
//                DLog.log("Exception  " + e.getMessage());
//                return "NO_INTERNET";
//            }
//        } catch (Exception e) {
//            DLog.log("In TransactionLogging. Encountered exception: " + e.getMessage());
//            return "NO_INTERNET";
//        }
        return "NO_INTERNET";

    }

    private String getServerURL(String requestType) {
        StringBuffer serverUrl = new StringBuffer(Constants.CLOUDPAIRING_URL);
        String requestQueue;
        if (requestType.equalsIgnoreCase(POST)) {
            serverUrl.append("/dpd");
            requestQueue = "/" + sessionID + "?dataType=" + WIFI_INFO;
        } else {
            serverUrl.append("/dgd");
            requestQueue = "/" + sessionID + "?dataRecordSeq=" + dataRecordSeq;
        }
        if (mRole == 2) {
            serverUrl.append("/dest");
        } else {
            serverUrl.append("/src");
        }
        serverUrl.append(requestQueue);
        return String.valueOf(serverUrl);
    }

    public void getData() {
        DLog.log("--getData--");
        requestType = GET;
        postData = "";
        mServerURL = getServerURL(GET);
        startServiceCall();
    }

    public void postData(String data) {
        requestType = POST;
        postData = getDetails(data);
        mServerURL = getServerURL(POST);
        startServiceCall();
    }

    public void createSession() {
        DLog.log("--createSession--");
        killExistingConnections();
        requestType = POST;
        postData = getDetails(DEVICE_INFO);
        mServerURL = CLOUDPAIRING_URL + "/dcs" + "?dataType=" + DEVICE_INFO;
        startServiceCall();
    }

    public void createSessionWithSessionID() {
        DLog.log("--createSession--");
        killExistingConnections();
        requestType = POST;
        postData = getDetails(DEVICE_INFO);
        mServerURL = CLOUDPAIRING_URL + "/dcs" + "?sessionId=" + sessionID + "&dataType=" + DEVICE_INFO;
        startServiceCall();
    }

    public void validateSession(String pin) {
        DLog.log("--validateSession--");
        killExistingConnections();
        mPin = pin;
        requestType = POST;
        postData = getDetails(DEVICE_INFO);
        mServerURL = CLOUDPAIRING_URL + "/das/" + mPin + "?dataType=" + DEVICE_INFO;
        startServiceCall();
    }

    public void endSession() {
        killExistingConnections();
        if (sessionID != null) {
            requestType = POST;
            postData = "";
            mServerURL = CLOUDPAIRING_URL + "/des/" + sessionID;
            startServiceCall();
        }
    }

    private void killExistingConnections() {
        try {
            if (mServerThread != null) {
                mServerThread.stopExistingConnections();
                mServerThread.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startServiceCall() {
        DLog.log("--startServiceCall--");
        mServerThread = new ServerThread();
        mServerThread.start();
    }

    public String getDetails(String type) {
        JSONObject jsonObject = new JSONObject();

        if (type.equalsIgnoreCase(DEVICE_INFO))
            try {
                jsonObject.put("platform", Constants.PLATFORM);
                jsonObject.put("dualBandSupported", DeviceInfo.getInstance().is5GhzSupported());
                jsonObject.put("isP2PSupported", DeviceInfo.getInstance().isP2PSupported());
                jsonObject.put("deviceAddress",CommonUtil.getInstance().getDeviceAddress());
                DLog.log("enter getDetails deviceAddress "+CommonUtil.getInstance().getDeviceAddress()+" added to DEVICE_INFO");
                jsonObject.put("deviceNameP2P", CommonUtil.getInstance().getDeviceNameP2P());
                DLog.log("enter getDetails deviceNameP2P " + CommonUtil.getInstance().getDeviceNameP2P() + " added to DEVICE_INFO");
                jsonObject.put("deviceAPILevel", CommonUtil.getInstance().getDeviceAPILevel());
                DLog.log("enter getDetails deviceAPILevel " + CommonUtil.getInstance().getDeviceAPILevel() + " added to DEVICE_INFO");
                jsonObject.put("model",DeviceInfo.getInstance().get_model());
                jsonObject.put("make", DeviceInfo.getInstance().get_make());
                jsonObject.put("isP2PModel", Constants.P2P_MODELS.contains(DeviceInfo.getInstance().get_model()));
//                jsonObject.put("imei", DeviceInfo.getInstance().getSingleIMEI());
                jsonObject.put("imei", DeviceInfo.getInstance().get_imei());
                if (!CommonUtil.getInstance().isMigrationInterrupted())
                    managePorts(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        else {
            CommonUtil commonUtil = CommonUtil.getInstance();
            if (commonUtil.getmCryptoEncryptPass().isEmpty()) {
                WifiConfiguration details = new HotspotServer().getWifiApConfiguration();
                try {
                    jsonObject.put("TYPE", "hotspot");
                    jsonObject.put("SSID", details.SSID);
                    jsonObject.put("PASSWORD", details.preSharedKey);
                    jsonObject.put("IP", "192.168.43.1");
                    jsonObject.put("FREQUENCY", "2500");
                    jsonObject.put("PIN", String.valueOf(Constants.DEFAULT_PIN));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    jsonObject.put("TYPE",commonUtil.getNetworkType());
                    jsonObject.put("SSID", commonUtil.getmWiFiPeerSSID());
                    jsonObject.put("PASSWORD", commonUtil.getmWiFiPeerPassphrase());
                    jsonObject.put("IP", commonUtil.getmWiFiPeerAddress());
                    jsonObject.put("FREQUENCY", commonUtil.getmFrequency());
                    jsonObject.put("PIN", commonUtil.getmCryptoEncryptPass());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return jsonObject.toString();
    }

    class ServerThread extends Thread {
        @Override
        public void run() {
            try {
                String response = doServerCall(mServerURL, postData, requestType);
                Message msg = new Message();
                msg.obj = response;
                mHandler.sendMessage(msg);
            } catch (Exception exception) {
                DLog.log(exception);
            }
        }

        void stopExistingConnections() {
            try {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                DLog.log("stopExistingConnections: "+e);
            }
        }
    }

    public void clean() {
        killExistingConnections();
        sessionID = null;
        mPin = null;
        dataRecordSeq = 0;
    }


    /**
     * Method to check and get Available ports.
     **/

    private void managePorts(JSONObject jsonObject) {
        try {
            if (EMUtility.isLocalPortInUse(EMConfig.COMMUNICATION_PORT_NUMBER)) {
                EMConfig.FIXED_PORT_NUMBER = EMUtility.getAvailblePort(EMConfig.FIXED_PORT_NUMBER, false);
                jsonObject.put("fixedPort", EMConfig.FIXED_PORT_NUMBER);
            }
            if (EMUtility.isLocalPortInUse(EMConfig.DATA_TRANSFER_PORT_NUMBER)) {
                EMServer.CONTENT_TRANSFER_PORT = EMUtility.getAvailblePort(EMServer.CONTENT_TRANSFER_PORT, true);
                jsonObject.put("dataTransferPort", EMServer.CONTENT_TRANSFER_PORT);
            }
            DLog.log("managed fixed and transfer ports : " + EMConfig.FIXED_PORT_NUMBER + " , " + EMServer.CONTENT_TRANSFER_PORT);
        } catch (Exception e) {
            DLog.log("got exception while managing ports : " + e.getMessage());
        }
    }
}
