package com.pervacio.wds.custom.asynctask;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.pervacio.vcard.Base64;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.custom.models.EDeviceSwitchSession;
import com.pervacio.wds.custom.service.TransactionLogService;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.NetworkUtil;
import com.pervacio.wds.custom.utils.ServerCallBacks;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;


/**
 * Class that extends AsyncTask to perform network operations.
 * <p>
 * It's a simple class that leverages URLConnection of Android framework to make network calls.
 * <p>
 * If you want to make this class configurable, modify the constructor.
 * <p>
 * Created by: Surya Polasanapalli on 5/24/2017.
 * Contributors:
 * Last updated on: 01/08/2017
 */

public class TransactionLogging {
    private static final String TAG = "servertask";
    private static final String NO_INTERNET = "NO_INTERNET";
    HttpURLConnection urlConnection;
    URL url;
    private String serverURL = null;
    private String logData = null;
    private String credentials = null;
    private ServerCallBacks serverCallBacks;
    private boolean isBackgroundCall = false;
    private boolean serverResult = false;
    private String TIMEOUT="TIMEOUT";

    public TransactionLogging() {

    }

    public TransactionLogging(String serverUrl, String credentials, String data, ServerCallBacks serverCallBacks) {
        this.serverURL = serverUrl;
        this.credentials = credentials;
        this.logData = data;
        this.serverCallBacks= serverCallBacks;
    }

    public void startServiceCall() {
        mServerThread = new ServerThread();
        mServerThread.start();
    }


    /*
    param[0] = URL
    param[1] = Username:Password
    param[2] = Body
     */
    public String doInBackground(String serverUrl, String credentials, String data) {

        if (NetworkUtil.isInternetAvailable()) {
            DLog.log("Internet connection is available, moving ahead to call the API.");
        } else {
            DLog.log("No internet connection available on the device. Returning.");
            return NO_INTERNET;
        }

        String result = null;
        try {
            String userCredentials = credentials;
            String basicAuth = EMUtility.getAuthToken();
/*            if (userCredentials != null) {
                basicAuth = "Basic " + new String(new Base64().encode(userCredentials.getBytes()));
            }*/
            DLog.log("Logging info at URL: " + serverUrl + " with data: " + data);
            try {
                url = new URL(serverUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(15000);
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                if (basicAuth != null)
                    urlConnection.setRequestProperty("Authorization", basicAuth);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");
                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.write(data.getBytes());

                InputStream is;

                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    is = urlConnection.getInputStream();
                    serverResult = true;
                }
                else {
                    is = urlConnection.getErrorStream();
                }

                if (is == null) {
                    DLog.log("Response Stream is null...");
                }
                else {
                    result = readStream(is);
                }

                DLog.log("Response: " + result);
                DLog.log("Response Code: " + urlConnection.getResponseCode());
                DLog.log("Response Message: " + urlConnection.getResponseMessage());
            } catch (SocketTimeoutException e){
                DLog.log("SocketTimeoutException :"+e.getMessage());
                return TIMEOUT;
            }
            catch (Exception e) {
                DLog.log("Exception  "+e.getMessage());
                return "ERROR";
            }
        } catch (Exception e) {
            DLog.log("In TransactionLogging. Encountered exception: " + e.getMessage() );
            return null;
        }
        return result;
    }


    private boolean onPostExecute(String str) {
        DLog.log("onPostExecute: " + str);
        boolean status = false;
        try {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }

            if (!isNullOrEmpty(str) && !str.equalsIgnoreCase(NO_INTERNET)&& !str.equalsIgnoreCase(TIMEOUT)&&!str.equalsIgnoreCase("ERROR")) {
                DLog.log("Converting JSON to object");
                Gson gson = new Gson();
                try {
                    EDeviceSwitchSession remoteSession = gson.fromJson(str, EDeviceSwitchSession.class);
                    if (remoteSession != null && !TextUtils.isEmpty(remoteSession.getDeviceSwitchSessionId())) {
                        EDeviceSwitchSession localSession = DashboardLog.getInstance().geteDeviceSwitchSession();
                        DashboardLog.mergeObjects(localSession, remoteSession);
                        status = true;
                    }
                } catch (Exception ex) {
                    DLog.log("Encountered exception while converting JSON response. Moving ahead." + ex.getMessage());
                }
            }
        } catch (Exception e) {
            DLog.log("Exception in postExecute: " + e);
        }
        return status;
    }


    // Converting InputStream to String
    private static String readStream(InputStream in) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        try {
            while ((length = in.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
        } catch (IOException e) {
            // Log.d(TAG, "IOException while reading response.");
        }
        try {
            return baos.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Log.d(TAG, "UnsupportedEncodingException UTF8.");
            return baos.toString();
        }
    }

   /* private boolean isOnline() {
        try {
            //Google servers are blazing fast, so timeout of 1.5 secs is more than enough.
            int timeoutMs = 1500;
            Socket sock = new Socket();
            //We will call Google DNS to check if internet is available on the device or not.
            SocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 53);
            sock.connect(sockaddr, timeoutMs);
            sock.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }*/

    private static boolean isNullOrEmpty(String str) {
        if (str == null || "".equalsIgnoreCase(str) || "null".equalsIgnoreCase(str)) {
            return true;
        } else {
            return false;
        }
    }

    ServerThread mServerThread = null;

    class ServerThread extends Thread {
        @Override
        public void run() {
            try {
                String response = doInBackground(serverURL, credentials, logData);
                if(serverCallBacks!=null){
                    serverCallBacks.onserverCallCompleted(response);
                }
               //No need to call post execute while in background
                else if(isBackgroundCall) {
                    DLog.log("Sending callback to service");
                    tlsCallBack.sendCallBack(TransactionLogService.CALLBACK_TYPE.TRANSACTION_LOG, serverResult);
                }
                else {
                    onPostExecute(response);
                }

            } catch (Exception exception) {
                DLog.log(exception);
            }
            try {
                DashboardLog.getInstance().setLastServerCallReturned(true);
                DashboardLog.getInstance().performPendingTransactions();
            } catch (Exception e) {
                DLog.log(e.getMessage());
            }
        }
    }

    public HttpURLConnection getUrlConnection() {
        return urlConnection;
    }

    public void setIsBackgroundCall(Boolean isBackgroundCall) {
        this.isBackgroundCall = isBackgroundCall;
    }

    TransactionLogService tlsCallBack = null;

    public void setTlsCallBack(TransactionLogService tlsCallBack) {
        this.tlsCallBack = tlsCallBack;
    }
}

