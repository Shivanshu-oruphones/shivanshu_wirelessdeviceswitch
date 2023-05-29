package com.pervacio.wds.custom.asynctask;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.pervacio.vcard.Base64;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.custom.StoreValidationActivity;
import com.pervacio.wds.custom.models.EDeviceSwitchSession;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.NetworkUtil;
import com.pervacio.wds.custom.utils.PreferenceHelper;
import com.pervacio.wds.custom.utils.ServerCallBacks;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Class that extends AsyncTask to perform network operations.
 * <p>
 * It's a simple class that leverages URLConnection of Android framework to make network calls.
 * <p>
 * If you want to make this class configurable, modify the constructor.
 * <p>
 * Created by: Darpan Dodiya on 5/24/2017.
 * Contributors:
 * Last updated on: 5/26/2017
 */

public class URLConnectionTask extends AsyncTask<String, Void, String> {
    private static final String TAG = "DLU";
    private static final String NO_INTERNET = "Internet connection not available on the device.";
    HttpURLConnection urlConnection;
    URL url;
    private Context mContext;
    private String mTrasactionCall = null;
    private ServerCallBacks mCommunicator =null;
    private PreferenceHelper preferenceHelper;
    private String response = null;

    public URLConnectionTask(Context context, String returnCallBack) {
        //Constructor.

        this.mContext = context;
        this.mTrasactionCall = returnCallBack;
        preferenceHelper = PreferenceHelper.getInstance(mContext);
    }

    @Override
    public void onPreExecute() {
        //Do nothing
    }

    @Override
    /*
    param[0] = URL
    param[1] = Username:Password
    param[2] = Body
     */
    public String doInBackground(String... params) {

        if (NetworkUtil.isOnline()) {
            DLog.log("Internet connection is available, moving ahead to call the API.");
        } else {
            DLog.log("No internet connection available on the device. Returning.");
            response = NO_INTERNET;
            return NO_INTERNET;
        }

        String result = null;
        try {
            String userCredentials = params[1];
            String basicAuth = null;
            if (userCredentials != null) {
                basicAuth = "Basic " + new String(new Base64().encode(userCredentials.getBytes()));
            }
            DLog.log("Logging info at URL: " + params[0] + " with data: " + params[2]);
            try {
                url = new URL(params[0]);
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
                if (Constants.FEEDBACK.equalsIgnoreCase(mTrasactionCall)) {
                    wr.write(EMUtility.gzip(params[2]));
                } else {
                    wr.write(params[2].getBytes());
                }
                InputStream is =null;
                if(urlConnection.getResponseCode()==HttpURLConnection.HTTP_OK) {
                    is = urlConnection.getInputStream();
                }
                else {
                    is = urlConnection.getErrorStream();
                }

                if (is == null)
                    DLog.log("Response Stream is null...");
                else
                    result = readStream(is);

                DLog.log("Response: " + result);
                DLog.log("Response Code: " + urlConnection.getResponseCode());
                DLog.log("Response Message: " + urlConnection.getResponseMessage());

            } catch (java.net.SocketTimeoutException e) {
                e.printStackTrace();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return result;

    }

    @Override
    public void onPostExecute(String str) {
        super.onPostExecute(str);

        DLog.log("response from server: " + str);
        try {
            boolean status = false;
            if (!isNullOrEmpty(str) && !str.equalsIgnoreCase(NO_INTERNET)) {
                if (!isNullOrEmpty(mTrasactionCall) && mTrasactionCall.equalsIgnoreCase(Constants.INSTALLATION_LOGGING)) {
                    status = Boolean.parseBoolean(str);
                } else if (!isNullOrEmpty(mTrasactionCall) && mTrasactionCall.equalsIgnoreCase(Constants.TRANSACTION_LOGGING)) {
                    DLog.log("Converting JSON to object");
                    Gson gson = new Gson();
                    DashboardLog.getInstance().seteDeviceSwitchSession(gson.fromJson(str, EDeviceSwitchSession.class));
                    String gsonString = gson.toJson(DashboardLog.getInstance().geteDeviceSwitchSession());
                    DLog.log("Converted JSON response: " + gsonString);
                } else if (!isNullOrEmpty(mTrasactionCall) && mTrasactionCall.equalsIgnoreCase(Constants.SUPPORTED_CHECK)) {
                    response = str;
                } else if (!isNullOrEmpty(mTrasactionCall)) {
                    status = parseResponse(str);
                }

            }
            /*else if(!isNullOrEmpty(str)&& str.equalsIgnoreCase(NO_INTERNET)){
                response= NO_INTERNET;
                Toast.makeText(mContext, "Internet Connection is Not Available", Toast.LENGTH_SHORT).show();
            }*/
            sendCallBacks(status, response);
            DLog.log("Server call for : "+mTrasactionCall+ " , status : " +status+" , response : "+response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {

        }
    }


    private void sendCallBacks(boolean result, String response) {
        if (mTrasactionCall != null && mContext != null) {
            if (mTrasactionCall.equalsIgnoreCase(Constants.LOCATION_VALIDATION)) {
                mCommunicator = (ServerCallBacks) mContext;
                mCommunicator.locationValidation(result, response);
            } else if (mTrasactionCall.equalsIgnoreCase(Constants.STOREID_VALIDATION)) {
                mCommunicator = (ServerCallBacks) mContext;
                mCommunicator.storeidValidation(result, response);
            } else if (mTrasactionCall.equalsIgnoreCase(Constants.INSTALLATION_LOGGING)) {
                preferenceHelper.putBooleanItem(Constants.INSTALLATION_LOGGING, result);
            } else if (mTrasactionCall.equalsIgnoreCase(Constants.SUPPORTED_CHECK)) {
                if (response != null && response.contains("NOT_CERTIFIED")) {
                    result = false;
                } else {
                    result = true;
                }
                StoreValidationActivity storeValidationActivity = (StoreValidationActivity) mContext;
                storeValidationActivity.isSupportedDevice(result);
            } else if (mTrasactionCall.equalsIgnoreCase(Constants.STORE_AND_REPVALIDATION)) {
                mCommunicator = (ServerCallBacks) mContext;
                mCommunicator.storeidAndRepidValidation(result, response);
            } else if (mTrasactionCall.equalsIgnoreCase(Constants.REP_VALIDATION)) {
                mCommunicator = (ServerCallBacks) mContext;
                mCommunicator.repLoginValidation(result, response);
            }
        }
    }

    private boolean parseResponse(String jsonResponse){
        try {
            if (jsonResponse != null) {
                JSONObject jsonObj = new JSONObject(jsonResponse);
                String status = jsonObj.getString(Constants.RESPONSE_KEY_LOGIN_STATUS);
                String storeID = jsonObj.getString(Constants.RESPONSE_KEY_STORE_ID);

                if (jsonObj.has(Constants.RESPONSE_KEY_COMPANY_ID)) {
                    int companyId = jsonObj.getInt(Constants.RESPONSE_KEY_COMPANY_ID);
                    DLog.log("Setting company ID as " + companyId);
                    DashboardLog.getInstance().geteDeviceSwitchSession().setCompanyId(String.valueOf(companyId));
                }else{
                    DLog.log("Company ID not found.");
                }
                response = storeID;
                if (!isNullOrEmpty(status) && status.equalsIgnoreCase("SUCCESS")) {
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
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

    public static boolean isNullOrEmpty(String str) {
        if (str == null || "".equalsIgnoreCase(str) || "null".equalsIgnoreCase(str)) {
            return true;
        } else {
            return false;
        }
    }

}

