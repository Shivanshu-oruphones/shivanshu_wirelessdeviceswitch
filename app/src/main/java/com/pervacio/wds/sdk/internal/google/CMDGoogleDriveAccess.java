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

package com.pervacio.wds.sdk.internal.google;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.pervacio.wds.app.EMConfig;
import com.pervacio.wds.app.EMMigrateStatus;
import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMStringConsts;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.sdk.CMDError;
import com.pervacio.wds.sdk.internal.CMDCopyFileProgressDelegate;
import com.pervacio.wds.sdk.internal.CMDCryptoSettings;
import com.pervacio.wds.sdk.internal.CMDFileSystemInterface;
import com.pervacio.wds.sdk.internal.CMDMultipart;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

// TODO: add caching of paths -> item IDs, to prevent unnecessary REST calls (especially useful when polling)

/**
 * Provides low level access to Google authentication and Google Drive.
 * All functions are blocking/synchronous and many can take a long time to complete.
 * It is assumed that all calls to functions in this class will be made from a background thread/async task.
 *
 * This class is thread safe and a single instance may be used in different threads (after the initial authentication is complete).
 *
 * All functions return an int for the error code (see CMDError) for more the details
 */
public class CMDGoogleDriveAccess
{
    static private final String SCOPE = "drive.file";
    static private final String ROOT_ID_ALIAS = "root";

    static private final int CHUNKED_SIZE = 65536;

    static private final int READ_BUFFER_SIZE = 1024 * 64;
    static private final int MINIMUM_STORAGE_SPACE = 1024 * 1024; // Pause the transfer if free storage drops below threshold

    private static final String GOOGLE_REST_FILES = "https://www.googleapis.com/drive/v2/files";
    private static final String GOOGLE_REST_FILE_SEARCH_FOR_ITEMS_WITH_PARENT_PREFIX = "?q='";
    private static final String GOOGLE_REST_FILE_SEARCH_FOR_ITEMS_WITH_PARENT_POSTFIX = "' in parents";

    private static final String GOOGLE_REST_ABOUT_BASE = "https://www.googleapis.com/drive/v2/about";
    private static final String GOOGLE_REST_FILES_BASE = "https://www.googleapis.com/drive/v2/files/";
    private static final String GOOGLE_REST_FILE_DOWNLOAD_POSTFIX = "?alt=media";
    private static final String GOOGLE_REST_CHILDREN = "/children";

    private static final String GOOGLE_DRIVE_URL_UPLOAD_FILE = "https://www.googleapis.com/upload/drive/v2/files?uploadType=multipart";

    private static final String GOOGLE_JSON_ITEMS = "items";

    private static final String GOOGLE_DRIVE_JSON_NAME_KIND = "kind";
    private static final String GOOGLE_DRIVE_JSON_VALUE_FILE_LINK = "drive#fileLink";
    private static final String GOOGLE_DRIVE_JSON_NAME_ID = "id";
    private static final String GOOGLE_DRIVE_JSON_NAME_PARENTS = "parents";

    private static final String GOOGLE_DRIVE_JSON_NAME_TITLE = "title";
    private static final String GOOGLE_DRIVE_JSON_NAME_MIME_TYPE = "mimeType";
    private static final String GOOGLE_DRIVE_JSON_VALUE_MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";

    private static final String HTTP_CONTENT_TYPE = "Content-Type";
    private static final String MIME_MULTIPART_RELATED_WITH_BOUNDARY = "multipart/related; boundary=";

    private static final String GOOGLE_DRIVE_URL_CREATE_FOLDER = "https://www.googleapis.com/drive/v2/files";
    private static final String GOOGLE_DRIVE_MIME_VALUE_JSON = "application/json; charset=UTF-8";
    private static final String GOOGLE_DRIVE_MIME_VALUE_OCTET_STREAM = "application/octet-stream";

    private static final int HTTP_RESULT_OK = 200;

    private static final int MAX_CELLULAR_RETRIES = 50;

    private static final String TAG = "CMDGoogleDriveAccess";

    enum CMDGoogleDriveItemType {
        EGoogleDriveFolder,
        EGoogleDriveFile
    }

    public class CMDGoogleDriveItem {
        public String mGoogleDriveId;
        public CMDGoogleDriveItemType mType;
        public String mName;
        public java.util.Date mModifiedDate;
        public long mSize;
        boolean mIsTrashed;
    }

    private Context mContext;

    public CMDGoogleDriveAccess(Context aContext) {
        mContext = aContext;
        mConnectivityManager = (ConnectivityManager) aContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private boolean cacheDriveInfo(){
        EMProgressHandler dummyProgressHandler = new EMProgressHandler() {
            @Override
            public void taskComplete(boolean aSuccess) {
                // Ignore
            }

            @Override
            public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
                // Ignore
            }

            @Override
            public void progressUpdate(EMProgressInfo aProgressInfo) {
                // Ignore
            }
        };

        try {
            JSONObject jObject2 = getJsonObjectFromUrlWithoutRetry(GOOGLE_REST_ABOUT_BASE, dummyProgressHandler);

            if (jObject2 == null)
                return false;

            CMDFileSystemInterface.CMDFileSystemInfo info = new CMDFileSystemInterface.CMDFileSystemInfo();
            String data = (String)jObject2.get("quotaBytesUsed");
            if(data != null && data.length() > 0) {
                 info.mUsedSpaceBytes = Long.parseLong(data);
            }

            data = (String)jObject2.get("quotaBytesTotal");
            if(data != null && data.length() > 0) {
                info.mTotalSpaceBytes = Long.parseLong(data);
            }

            mCachedDriveInfo = info;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private CMDFileSystemInterface.CMDFileSystemInfo mCachedDriveInfo;

    public CMDFileSystemInterface.CMDFileSystemInfo getCachedDriveInfo() {
        return mCachedDriveInfo;
    }

    // Authenticate for drive.file scope
    // This function is blocking and could be long running. It is expected that it would be called from a background thread/task
    // This function may be called more than once, for example if the REST server starts returning errors (re-authentication may be required)
    // The passed in aActivity may be used to invoke recovering actions, e.g. prompting the user to select or create an account
    // aActivity.onActivityResult will be called with one of the following once the recovering action is complete:
        // CMDError.CMD_GOOGLE_DRIVE_ACCESS_AUTHENTICATION_RECOVERY_COMPLETE
    public int authenticate(String aUserName,
                                Activity aActivity) {
        int result = CMDError.CMD_RESULT_OK;
        boolean go = true;

        int remainingRetryAttempts = EMConfig.GOOGLE_DRIVE_AUTHENTICATION_ATTEMPTS;

        while ((go) && (remainingRetryAttempts-- > 0)) {
            mUserName = aUserName;
            go = false;

            if (mAccessToken != null) {
                // If we already have an access token then assume that we want to invalidate it and get a new one
                GoogleAuthUtil.invalidateToken(aActivity, mAccessToken);
                mAccessToken = null;
            }

            try {
                // Get the access token with a synchronous API call (could be a long running operation)
                // String scope = "oauth2:server:client_id:" + CLIENT_ID + ":api_scope:https://www.googleapis.com/auth/" + SCOPE;
                String scope = "oauth2:" + "https://www.googleapis.com/auth/drive.file";
                mAccessToken = GoogleAuthUtil.getToken(aActivity, aUserName, scope);
                if (!cacheDriveInfo()) {
                    go = true; // Try this again - it sometimes fails the first time - not sure why
                    result = CMDError.CMD_GOOGLE_DRIVE_ACCESS_AUTHENTICATION_FAILED;
                }
            } catch (GooglePlayServicesAvailabilityException playEx) {
                // Log.d(TAG, "*** Exception");
                // Log.d(TAG, Log.getStackTraceString(playEx));
                result = CMDError.GOOGLE_SERVICES_NOT_AVAILABLE;
            } catch (UserRecoverableAuthException userAuthEx) {
                // Start the user recoverable action using the intent returned by getIntent()
                // Log.d(TAG, "*** Exception");
                // Log.d(TAG, Log.getStackTraceString(userAuthEx));
                aActivity.startActivityForResult(
                        userAuthEx.getIntent(),
                        CMDError.SELECTING_ACCOUNT);
                // Set result: note that it is recoverable with initiated user interaction
                result = CMDError.CMD_GOOGLE_DRIVE_ACCESS_RECOVERABLE_AUTHENTICATION_ERROR;
            } catch (IOException transientEx) {
                // Log.d(TAG, "*** Exception");
                // Log.d(TAG, Log.getStackTraceString(transientEx));

                // Maybe a network error, server rejection, e.g. quotas exceeded, either way - don't retry immediately (maybe try a backoff handling mechanism)
                result = CMDError.CMD_GOOGLE_DRIVE_ACCESS_TRANSIENT_AUTHENTICATION_ERROR;
            } catch (GoogleAuthException authEx) {
                // Log.d(TAG, "*** Exception");
                // Log.d(TAG, Log.getStackTraceString(authEx));

                result = CMDError.CMD_GOOGLE_DRIVE_ACCESS_AUTHENTICATION_FAILED;
            }
        }

        return result;
    }

    public void setAccessToken(String aAccessToken) {
        mAccessToken = aAccessToken;
    }

    public int copyFileFromLocal(String aParentGoogleDriveId,   // The folder that this file will be created under
                                    String aLocalFilePath, // The local file path
                                    String aRemoteFileName,
                                    EMProgressHandler aProgressHandler,
                                    CMDCopyFileProgressDelegate aCopyFileProgressDelegate) {

        // Log.d(TAG, ">> copyFileFromLocal");
        // Log.d(TAG, "aLocalFilePath: " + aLocalFilePath);
        // Log.d(TAG, "aRemoteFileName: " + aRemoteFileName);
        int result = CMDError.CMD_RESULT_OK;

        CMDMultipart multipart = null;
        try {
            JSONObject mainJsonObject = new JSONObject();
            mainJsonObject.put(GOOGLE_DRIVE_JSON_NAME_TITLE, aRemoteFileName);
            addParentIdToJson(mainJsonObject, aParentGoogleDriveId);

            multipart = new CMDMultipart();
            multipart.addBody(mainJsonObject.toString().getBytes("UTF-8"), GOOGLE_DRIVE_MIME_VALUE_JSON, false);
            multipart.addBody(new File(aLocalFilePath), GOOGLE_DRIVE_MIME_VALUE_OCTET_STREAM, CMDCryptoSettings.enabled());

            boolean forceUploadOnCellular = false;
            if (aRemoteFileName.equals(EMStringConsts.EM_BACKUP_FINISHED_FILE)) {
                forceUploadOnCellular = true; // Write the backup-finished file even if we're on cellular - this is to maximize the chances of writing the file
            }

            if (aRemoteFileName.equals(EMStringConsts.EM_BACKUP_STARTED_FILE)) {
                EMMigrateStatus.setGoogleBackupInProgress(mContext, aParentGoogleDriveId, mUserName);
            }

            HttpResponse response = postRequest(GOOGLE_DRIVE_URL_UPLOAD_FILE,
                    MIME_MULTIPART_RELATED_WITH_BOUNDARY + "\"" + multipart.getBoundary() + "\"",
                    multipart,
                    aProgressHandler,
                    forceUploadOnCellular,
                    aCopyFileProgressDelegate);

            if (response.mHttpResult == 403) { // If we get a 403 error then return failure - it probably means Google Drive is full
                result = CMDError.CMD_GOOGLE_DRIVE_FULL_ERROR;
            }
        }
        catch (Exception ex) {
            // Log.d(TAG, "*** Exception in copyFileFromLocal");
            // Log.d(TAG, Log.getStackTraceString(ex));
            result = CMDError.CMD_GOOGLE_DRIVE_ACCESS_UNABLE_TO_COPY_FILE_FROM_LOCAL;
        }

        // Log.d(TAG, "<< copyFileFromLocal" + result);

        return result;
    }

    void addParentIdToJson(JSONObject aJsonObject, // The Json object to add the parent to
                            String aParentId)
    {
        // Log.d(TAG, ">> addParentIdToJson");
        try {
            JSONObject parentFolderObject  = new JSONObject();
            parentFolderObject.put(GOOGLE_DRIVE_JSON_NAME_KIND, GOOGLE_DRIVE_JSON_VALUE_FILE_LINK);
            parentFolderObject.put(GOOGLE_DRIVE_JSON_NAME_ID, aParentId);
            JSONArray parentFolderArray = new JSONArray();
            parentFolderArray.put(parentFolderObject);
            aJsonObject.put(GOOGLE_DRIVE_JSON_NAME_PARENTS, parentFolderArray);
        }
        catch (Exception ex) {
            // TODO: handle this
            // TODO: report error - is it likely?
            // Log.d(TAG, "*** Exception in addParentIdToJson");
            // Log.d(TAG, Log.getStackTraceString(ex));
        }
        // Log.d(TAG, "<< addParentIdToJson");
    }

    private class HttpResponse {
        public int mHttpResult;
        public byte[] mResponseBytes;
    }

    private HttpResponse postRequest(String aUrl,
                                        String aContentType,
                                        InputStream aInputStream,
                                        EMProgressHandler aProgressHandler) {
        // Log.d(TAG, ">> postRequest");

        HttpResponse response = new HttpResponse();

        boolean tryCopy = true;
//        boolean paused = false;
        boolean copyComplete = false;

        while (tryCopy) {
            // Log.d(TAG, "Attempting copy to post from input stream:");

            int result = CMDError.CMD_RESULT_OK;

            // TODO: review: add retry mechanism
            // TODO: review: return error if retry fails or is not enabled
            try {
                EMMigrateStatus.raiseExceptionIfNetworkNotAllowed(mConnectivityManager, aProgressHandler);

                HttpsURLConnection urlConnection = null;

                URL url = new URL(aUrl);
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(CHUNKED_SIZE);
                urlConnection.setRequestProperty(GOOGLE_DRIVE_REST_AUTHORIZATION, "Bearer" + " " + mAccessToken);
                urlConnection.setRequestProperty(HTTP_CONTENT_TYPE, aContentType);
                // urlConnection.connect();

                int n = -1;
                byte[] buffer = new byte[READ_BUFFER_SIZE];
                OutputStream connectionOutputStream = urlConnection.getOutputStream();
                while ((n = aInputStream.read(buffer)) != -1) {
                    if (n > 0) {
                        connectionOutputStream.write(buffer, 0, n);
                    }

                    /*
                    if (paused) {
                        // Log.d(TAG, "unpausing ");
                        paused = false;
                        EMProgressInfo progressInfo = new EMProgressInfo();
                        progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TRANSFER_RESUMED;
                        aProgressHandler.progressUpdate(progressInfo);
                    }
                    */
                }

                connectionOutputStream.flush();
                connectionOutputStream.close();

                int responseCode = urlConnection.getResponseCode();
                // Log.d(TAG, "responseCode: " + responseCode);

                String responseMessage = urlConnection.getResponseMessage();
                // Log.d(TAG, "responseMessage: " + responseMessage);

                handleHttpResult(responseCode);

                InputStream httpResponseInputStream = urlConnection.getInputStream();
                ByteArrayOutputStream httpResponseByteArrayOutputStream = new ByteArrayOutputStream();

                buffer = new byte[READ_BUFFER_SIZE];
                n = -1;

                // Log.d(TAG, "About to write to stream");
                while ((n = httpResponseInputStream.read(buffer)) != -1) {
                    if (n > 0) {
                        httpResponseByteArrayOutputStream.write(buffer, 0, n);
                    }

                    /*
                    if (paused) {
                        // Log.d(TAG, "unpausing ");
                        paused = false;
                        EMProgressInfo progressInfo = new EMProgressInfo();
                        progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TRANSFER_RESUMED;
                        aProgressHandler.progressUpdate(progressInfo);
                    }
                    */
                }
                copyComplete = true;
                // Log.d(TAG, "Read from stream complete");

                httpResponseInputStream.close();

                response.mResponseBytes = httpResponseByteArrayOutputStream.toByteArray();
                response.mHttpResult = responseCode;
            }
            catch (EMRecoverableAuthErrorException ex) {
                // Log.d(TAG, "*** Exception: " + ex.toString());
                // Log.d(TAG, Log.getStackTraceString(ex));
                result = CMDError.CMD_GOOGLE_DRIVE_ACCESS_TRANSIENT_AUTHENTICATION_ERROR;
            }
            catch (Exception ex) {
                // Log the exception and continue - we know if we have not written the whole file and will retry if needed
                // Log.d(TAG, "*** Exception: " + ex.toString());
                // Log.d(TAG, Log.getStackTraceString(ex));
                ex.printStackTrace();
            }

            // Log.d(TAG, "result: " + result);
            // Log.d(TAG, "copyComplete: " + copyComplete);
            if (result == CMDError.CMD_GOOGLE_DRIVE_ACCESS_TRANSIENT_AUTHENTICATION_ERROR) {
                // Log.d(TAG, "CMD_GOOGLE_DRIVE_ACCESS_TRANSIENT_AUTHENTICATION_ERROR: sleeping");
                SystemClock.sleep(EMConfig.EM_WAIT_FOR_RE_AUTH_TIME_MS); // Wait then try again...
                result = CMDError.CMD_RESULT_OK;
                // Log.d(TAG, "resuming"); // Try again, but don't report any error
            }
            else if ((result != CMDError.CMD_RESULT_OK) || (copyComplete)) {
                tryCopy = false; // Don't try again if we have an error code (bad response from Google Drive), or if we have copied the file successfully
            }
            else {
                /*
                if (!paused) {
                    paused = true;
                    EMProgressInfo progressInfo = new EMProgressInfo();
                    progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TRANSFER_PAUSED; // TODO: we're assuming this is a network issue (so pausing)
                    aProgressHandler.progressUpdate(progressInfo);
                }
                */

                // Log.d(TAG, "sleeping");
                SystemClock.sleep(EMConfig.EM_WAIT_FOR_WIFI_TIME_MS); // Wait then try again...
                // Log.d(TAG, "resuming");
            }
        }

        // Log.d(TAG, "<< postRequest" + response);

        return response;
    }

    private HttpResponse postRequest(String aUrl,
                                     String aContentType,
                                     CMDMultipart aMultipart,
                                     EMProgressHandler aProgressHandler,
                                     boolean aForceUploadEvenOnCellular,
                                     CMDCopyFileProgressDelegate aCopyFileProgressDelegate) {
        // Log.d(TAG, ">> postRequest");

        HttpResponse response = new HttpResponse();

        boolean tryCopy = true;
//        boolean paused = false;
        boolean copyComplete = false;
        int cellularRetryCountdown = MAX_CELLULAR_RETRIES;

        long totalDataTransferred = 0;

        while (tryCopy) {
            int result = CMDError.CMD_RESULT_OK;
            // Log.d(TAG, "Attempting copy to post from multipart:");

            try {
                if (!aForceUploadEvenOnCellular) { // Only try uploading on cellular a limited number of times
                    EMMigrateStatus.raiseExceptionIfNetworkNotAllowed(mConnectivityManager, aProgressHandler);
                }

                HttpsURLConnection urlConnection = null;

                URL url = new URL(aUrl);
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setUseCaches(false);
                urlConnection.setChunkedStreamingMode(CHUNKED_SIZE);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty(GOOGLE_DRIVE_REST_AUTHORIZATION, "Bearer" + " " + mAccessToken);
                urlConnection.setRequestProperty(HTTP_CONTENT_TYPE, aContentType);

                OutputStream connectionOutputStream = urlConnection.getOutputStream();

                aMultipart.writeToOutputStream(connectionOutputStream, aProgressHandler, aCopyFileProgressDelegate, EMMigrateStatus.getPausedStatus()); // Write to output stream and close any input streams
                // paused = false;

                connectionOutputStream.flush();
                connectionOutputStream.close();

                int responseCode = urlConnection.getResponseCode();

                // If we have an HTTP error (>=300) and we're on wifi then decrement a counter (we don't want to retry on cellular forever, even if we're forcing the writing of something important like the backup-finished file)
                if ((responseCode >= 300) && (aForceUploadEvenOnCellular)) {
                    NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
                    boolean connectedToWiFi = (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
                    if (!connectedToWiFi) {
                        cellularRetryCountdown--;
                        if (cellularRetryCountdown <= 0) {
                            aForceUploadEvenOnCellular = false; // Disable retrying on cellular - wait until we have Wi-Fi back
                        }
                    }
                }

                // Log.d(TAG, "responseCode: " + responseCode);

                String responseMessage = urlConnection.getResponseMessage();
                // Log.d(TAG, "responseMessage: " + responseMessage);

                response.mHttpResult = responseCode;

                handleHttpResult(responseCode);

                InputStream httpResponseInputStream = urlConnection.getInputStream();
                ByteArrayOutputStream httpResponseByteArrayOutputStream = new ByteArrayOutputStream();

                byte[] buffer = new byte[READ_BUFFER_SIZE];
                int n = -1;
                while ((n = httpResponseInputStream.read(buffer)) != -1) {
                    if (n > 0) {
                        httpResponseByteArrayOutputStream.write(buffer, 0, n);
                    }

                    /*
                    if (paused) {
                        // Log.d(TAG, "unpausing ");
                        paused = false;
                        EMProgressInfo progressInfo = new EMProgressInfo();
                        progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TRANSFER_RESUMED;
                        aProgressHandler.progressUpdate(progressInfo);
                    }
                    */
                }

                // Log.d(TAG, "Copy complete");

                copyComplete = true;

                httpResponseInputStream.close();

                response.mResponseBytes = httpResponseByteArrayOutputStream.toByteArray();
            }
            catch (EMRecoverableAuthErrorException ex) {
                // Log.d(TAG, "*** Exception: " + ex.toString());
                // Log.d(TAG, Log.getStackTraceString(ex));
                result = CMDError.CMD_GOOGLE_DRIVE_ACCESS_TRANSIENT_AUTHENTICATION_ERROR;
            }
            catch (EMGoogleForbiddenErrorException ex) {
                // Log.d(TAG, "*** Exception: " + ex.toString());
                // Log.d(TAG, Log.getStackTraceString(ex));
                result = CMDError.CMD_GOOGLE_DRIVE_FULL_ERROR;
                tryCopy = false; // Don't retry after a forbidden error - return the HTTP result instead
            }
            catch (Exception ex) {
                // Log the exception and continue - we know if we have not written the whole file and will retry if needed
                // Log.d(TAG, "*** Exception: " + ex.toString());
                // Log.d(TAG, Log.getStackTraceString(ex));
                ex.printStackTrace();
            }

            // Log.d(TAG, "result: " + result);
            // Log.d(TAG, "copyComplete: " + copyComplete);
            if (result == CMDError.CMD_GOOGLE_DRIVE_ACCESS_TRANSIENT_AUTHENTICATION_ERROR) {
                // Log.d(TAG, "CMD_GOOGLE_DRIVE_ACCESS_TRANSIENT_AUTHENTICATION_ERROR: sleeping");
                SystemClock.sleep(EMConfig.EM_WAIT_FOR_RE_AUTH_TIME_MS); // Wait then try again...
                result = CMDError.CMD_RESULT_OK;
                // Log.d(TAG, "resuming"); // Try again, but don't report any error
            }
            else if ((result != CMDError.CMD_RESULT_OK) || (copyComplete)) {
                tryCopy = false; // Don't try again if we have an error code (bad response from Google Drive), or if we have copied the file successfully
            }
            else {
                /*
                if (!paused) {
                    paused = true;
                    EMProgressInfo progressInfo = new EMProgressInfo();
                    progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TRANSFER_PAUSED; // TODO: we're assuming this is a network issue (so pausing)
                    aProgressHandler.progressUpdate(progressInfo);
                }
                */

                // Log.d(TAG, "sleeping");
                SystemClock.sleep(EMConfig.EM_WAIT_FOR_WIFI_TIME_MS); // Wait then try again...
                // Log.d(TAG, "resuming");
            }
        }

        // Log.d(TAG, "<< postRequest");

        return response;
    }

    public HttpResponse deleteRequest(String aGoogleDriveId) {
        HttpResponse response = new HttpResponse();

        // TODO: review: add retry mechanism
        // TODO: review: return error if retry fails or is not enabled
        try {
            HttpsURLConnection urlConnection = null;

            String uriString = "https://www.googleapis.com/drive/v2/files/" + aGoogleDriveId;

            URL url = new URL(uriString);
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("DELETE");
            urlConnection.setRequestProperty(GOOGLE_DRIVE_REST_AUTHORIZATION, "Bearer" + " " + mAccessToken);
            urlConnection.connect(); // Do the delete

            int responseCode = urlConnection.getResponseCode();

            response.mHttpResult = responseCode;
        }
        catch (Exception ex) {
            // TODO: handle errors
            Log.e(TAG, ex.toString());
            // Log.d(TAG, "*** Exception");
            // Log.d(TAG, Log.getStackTraceString(ex));
        }

        return response;
    }

    // Returns the created folder details (or null if the folder could not be created)
    public CMDGoogleDriveItem createFolder(String aParentGoogleDriveId,
                                            String aFolderName,
                                            EMProgressHandler aProgressHandler) {
        CMDGoogleDriveItem createdFolder = null;

        JSONObject mainJsonObject = new JSONObject();
        addParentIdToJson(mainJsonObject, aParentGoogleDriveId);

        try {
            String createdFolderId = null;
            mainJsonObject.put(GOOGLE_DRIVE_JSON_NAME_TITLE, aFolderName);
            mainJsonObject.put(GOOGLE_DRIVE_JSON_NAME_MIME_TYPE, GOOGLE_DRIVE_JSON_VALUE_MIME_TYPE_FOLDER);

            ByteArrayInputStream postDataInputStream = new ByteArrayInputStream(mainJsonObject.toString().getBytes("UTF-8"));

            HttpResponse httpResponse = postRequest(GOOGLE_DRIVE_URL_CREATE_FOLDER, GOOGLE_DRIVE_MIME_VALUE_JSON, postDataInputStream, aProgressHandler);
            if (httpResponse.mHttpResult == HTTP_RESULT_OK) {
                String responseJsonString = new String(httpResponse.mResponseBytes, "UTF-8");
                JSONObject responseJson = new JSONObject(responseJsonString);
                createdFolderId = responseJson.getString(GOOGLE_DRIVE_JSON_NAME_ID);
            }

            if (createdFolderId != null) {
                createdFolder = getDriveItemFromId(createdFolderId, aProgressHandler); // Ensures we get all the correct details, including timestamp. Also ensures the item is added to the cache
            }
        }
        catch (Exception ex) {
            // Ignore, we'll just return null
            // Log.d(TAG, "*** Exception");
            // Log.d(TAG, Log.getStackTraceString(ex));
        }
        // TODO: return created folder details
        return createdFolder;
    }

    // Copy a file from Google Drive to the local file system
    // The requested local file will be created
    // If a local file name with the same path already exists then it will be replaced
    public int copyFileToLocal(String aGoogleDriveId,
                                    String aLocalFilePath,
                                    EMProgressHandler aProgressHandler, // For progress reporting (in case the transfer is paused)
                                    CMDCopyFileProgressDelegate aCopyFileProgressDelegate) {
        // Log.d(TAG, ">> copyFileToLocal");
        int result = CMDError.CMD_RESULT_OK;

        // boolean paused = false;

        boolean tryCopy = true;

        while (tryCopy) {
            // Log.d(TAG, "Attempting copy to local:");

            boolean copyComplete = false;

            String urlString = GOOGLE_REST_FILES_BASE + aGoogleDriveId + GOOGLE_REST_FILE_DOWNLOAD_POSTFIX;

            InputStream inputStream = null;
            HttpsURLConnection urlConnection = null;
            JSONObject jsonObject = null;

            // TODO: review: return error if retry fails or is not enabled
            OutputStream output = null;
            try {
                EMMigrateStatus.raiseExceptionIfNetworkNotAllowed(mConnectivityManager, aProgressHandler);
                EMMigrateStatus.raiseExceptionIfInsufficientStorageSpace(aLocalFilePath, MINIMUM_STORAGE_SPACE);

                URL url = new URL(urlString);
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty(GOOGLE_DRIVE_REST_AUTHORIZATION, "Bearer" + " " + mAccessToken);
                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
                // Log.d(TAG, "responseCode: " + responseCode);

                String responseMessage = urlConnection.getResponseMessage();
                // Log.d(TAG, "responseMessage: " + responseMessage);

                handleHttpResult(responseCode);

                inputStream = urlConnection.getInputStream();

                byte[] buffer = new byte[EMConfig.EM_GOOGLE_DRIVE_READ_BUFFER_SIZE];
                int n = -1;

                // Log.d(TAG, "opening local file: " + aLocalFilePath);

                File localFile = new File(aLocalFilePath);
                output = new FileOutputStream(localFile);

                if (CMDCryptoSettings.enabled())
                    output = CMDCryptoSettings.getCipherDecryptOutputStream(output); // Decrypt when writing to the output file (if crypto is enabled)

                long totalDataCopied = 0;

                // Log.d(TAG, "entering : inputStream.read(buffer) loop ");
                while ((n = inputStream.read(buffer)) != -1) {
                    EMMigrateStatus.raiseExceptionIfNetworkNotAllowed(mConnectivityManager, aProgressHandler);
                    EMMigrateStatus.raiseExceptionIfInsufficientStorageSpace(aLocalFilePath, MINIMUM_STORAGE_SPACE);
                    if (n > 0) {
                        output.write(buffer, 0, n);

                        /*
                        if (paused) {
                            // Log.d(TAG, "unpausing ");
                            paused = false;
                            EMProgressInfo progressInfo = new EMProgressInfo();
                            progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TRANSFER_RESUMED;
                            aProgressHandler.progressUpdate(progressInfo);
                        }
                        */

                        if (aCopyFileProgressDelegate != null)
                        {
                            totalDataCopied += n;
                            aCopyFileProgressDelegate.onCopyFileProgress(totalDataCopied);
                        }

                        EMMigrateStatus.addBytesTransferred(n);
                    }
                }
                // Log.d(TAG, "exiting : inputStream.read(buffer) loop ");


                // Log.d(TAG, "Closing output file");
                output.close();
                copyComplete = true;
                // Log.d(TAG, "Output file closed");
            }
            catch (EMRecoverableAuthErrorException ex) {
                // Log.d(TAG, "*** Exception: " + ex.toString());
                // Log.d(TAG, Log.getStackTraceString(ex));
                result = CMDError.CMD_GOOGLE_DRIVE_ACCESS_TRANSIENT_AUTHENTICATION_ERROR;
            }
            catch (EMMigrateStatus.EMLowLocalStorageException ex) {
                // Log.d(TAG, "*** Exception: " + ex.toString());
                // Log.d(TAG, Log.getStackTraceString(ex));
                tryCopy = false;
                result = CMDError.CMD_ERROR_NOT_ENOUGH_SPACE_ON_LOCAL_DEVICE;
            }
            catch (Exception ex) {
                // Log the exception and continue - we know if we have not written the whole file and will retry if needed
                // Log.d(TAG, "*** Exception: " + ex.toString());
                // Log.d(TAG, Log.getStackTraceString(ex));
                ex.printStackTrace();

                boolean hasWriteExternalStorage = (EMUtility.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

                if (hasWriteExternalStorage) {
                    tryCopy = true;
                }
                else {
                    result = CMDError.CMD_GOOGLE_DRIVE_ACCESS_UNABLE_TO_COPY_FILE_TO_LOCAL;
                    tryCopy = false; // Don't retry - don't have permissions, so we expect to fail
                }
            } finally {
                // Log.d(TAG, "Final closing");
                if (urlConnection != null)
                    urlConnection.disconnect();

                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        // Ignore: nothing we can do here
                        e.printStackTrace();
                        // Log.d(TAG, "*** Exception");
                        // Log.d(TAG, Log.getStackTraceString(e));
                    }
                }

                if (output != null) {
                    try {
                        output.close();

                        if (!copyComplete) {
                            // Delete output file if not fully written (it will be incomplete)
                            File incompleteFile = new File(aLocalFilePath);
                            incompleteFile.delete();
                        }
                    } catch (IOException e) {
                        // Ignore: nothing we can do here
                        e.printStackTrace();
                        // Log.d(TAG, "*** Exception");
                        // Log.d(TAG, Log.getStackTraceString(e));
                    }
                }
            }

            // Log.d(TAG, "result: " + result);
            // Log.d(TAG, "copyComplete: " + copyComplete);
            if (result == CMDError.CMD_GOOGLE_DRIVE_ACCESS_TRANSIENT_AUTHENTICATION_ERROR) {
                // Log.d(TAG, "CMD_GOOGLE_DRIVE_ACCESS_TRANSIENT_AUTHENTICATION_ERROR: sleeping");
                SystemClock.sleep(EMConfig.EM_WAIT_FOR_RE_AUTH_TIME_MS); // Wait then try again...
                result = CMDError.CMD_RESULT_OK;
                // Log.d(TAG, "resuming"); // Try again, but don't report any error
            }
            else if ((result != CMDError.CMD_RESULT_OK) || (copyComplete)) {
                tryCopy = false; // Don't try again if we have an error code (bad response from Google Drive), or if we have copied the file successfully
            }
            else {
                /*
                if (!paused) {
                    paused = true;
                    EMProgressInfo progressInfo = new EMProgressInfo();
                    progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TRANSFER_PAUSED; // TODO: we're assuming this is a network issue (so pausing)
                    aProgressHandler.progressUpdate(progressInfo);
                }
                */

                // Log.d(TAG, "sleeping");
                SystemClock.sleep(EMConfig.EM_WAIT_FOR_WIFI_TIME_MS); // Wait then try again...
                // Log.d(TAG, "resuming");
            }
        }

        // Log.d(TAG, "<< copyFileToLocal: aLocalFilePath:" + aLocalFilePath);

        return result;
    }

    private static final String GOOGLE_JSON_ID = "id"; // value: String
    private static final String GOOGLE_JSON_TITLE = "title"; // value: String
    private static final String GOOGLE_JSON_MIME_TYPE = "mimeType"; // value: String
    private static final String GOOGLE_JSON_SIZE = "fileSize"; // value: String
    private static final String GOOGLE_JSON_MODIFIED_DATE = "modifiedDate"; // value: datetime
    private static final String GOOGLE_JSON_LABELS = "labels"; // value: JSON Object
    private static final String GOOGLE_JSON_LABELS_TRASHED = "trashed"; // value: boolean
    private static final String GOOGLE_JSON_NEXT_LINK = "nextLink"; // value: string // Link to the next page of child items

    private static final String GOOGLE_JSON_MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";

    private static final String GOOGLE_DRIVE_REST_AUTHORIZATION = "Authorization";

    public static java.util.Date parseDateString(String aDateString) {
        java.util.Date date = null;
        try {
            TimeZone utc = TimeZone.getTimeZone("UTC");
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            f.setTimeZone(utc);
            GregorianCalendar cal = new GregorianCalendar(utc);
            cal.setTime(f.parse(aDateString));
            date = cal.getTime();
        }
        catch (Exception ex) {
            // Ignore, just return a null date
            // Log.d(TAG, "*** Exception");
            // Log.d(TAG, Log.getStackTraceString(ex));
        }

        return date;
    }

    private Map<String, CMDGoogleDriveItem> mIdToDriveItemCacheMap = new HashMap<String, CMDGoogleDriveItem>();

    private CMDGoogleDriveItem getDriveItemFromFullJsonItem(JSONObject aJsonObject) {
        CMDGoogleDriveItem driveItem = new CMDGoogleDriveItem();

        try {
            driveItem.mGoogleDriveId = aJsonObject.getString(GOOGLE_JSON_ID);
        }
        catch (JSONException ex) {
            // Ignore any values that we can't get
        }

        try {
            driveItem.mName = aJsonObject.getString(GOOGLE_JSON_TITLE);
        } catch (JSONException e) {
            // Ignore any values that we can't get
        }

        try {
            driveItem.mSize = aJsonObject.getLong(GOOGLE_JSON_SIZE);
        } catch (JSONException e) {
            // Ignore any values that we can't get
        }

        driveItem.mType = CMDGoogleDriveItemType.EGoogleDriveFile;
        String mimeType = null;
        try {
            mimeType = aJsonObject.getString(GOOGLE_JSON_MIME_TYPE);
            if (mimeType.equalsIgnoreCase(GOOGLE_JSON_MIME_TYPE_FOLDER))
                driveItem.mType = CMDGoogleDriveItemType.EGoogleDriveFolder;
        } catch (JSONException e) {
            // Ignore any values that we can't get
        }

        String dateString = null;
        try {
            dateString = aJsonObject.getString(GOOGLE_JSON_MODIFIED_DATE);
            driveItem.mModifiedDate = parseDateString(dateString);
        } catch (JSONException e) {
            // Ignore any values that we can't get
        }

        JSONObject labelsObject = null;
        try {
            labelsObject = aJsonObject.getJSONObject(GOOGLE_JSON_LABELS);
            if (labelsObject != null) {
                driveItem.mIsTrashed = labelsObject.getBoolean(GOOGLE_JSON_LABELS_TRASHED);
            }
        } catch (JSONException e) {
            // Ignore any values that we can't get
        }

        mIdToDriveItemCacheMap.put(driveItem.mGoogleDriveId, driveItem);

        return driveItem;
    }

    private CMDGoogleDriveItem getDriveItemFromId(String aDriveItemId, EMProgressHandler aProgressHandler) {
        if (mIdToDriveItemCacheMap.containsKey(aDriveItemId))
            return mIdToDriveItemCacheMap.get(aDriveItemId);

        JSONObject driveItemJson = getJsonObjectFromUrl(GOOGLE_REST_FILES_BASE + aDriveItemId, aProgressHandler);

        return getDriveItemFromFullJsonItem(driveItemJson);
    }

    private JSONObject getJsonObjectFromUrl(String aUrlString,
                                            EMProgressHandler aProgressHandler) {
        return getJsonObjectFromUrl(aUrlString, aProgressHandler, true);
    }

    private JSONObject getJsonObjectFromUrlWithoutRetry(String aUrlString,
                                                     EMProgressHandler aProgressHandler) {
        return getJsonObjectFromUrl(aUrlString, aProgressHandler, false);
    }

    private JSONObject getJsonObjectFromUrl(String aUrlString,
                                            EMProgressHandler aProgressHandler,
                                            boolean aRetryOnFailure) {
        // Log.d(TAG, ">> getJsonObjectFromUrl: " + aUrlString);

        InputStream inputStream = null;
        HttpsURLConnection urlConnection = null;
        JSONObject jsonObject = null;

        boolean tryGet = true;
        // boolean paused = false;

        while (tryGet) {
            try {
                EMMigrateStatus.raiseExceptionIfNetworkNotAllowed(mConnectivityManager, aProgressHandler);

                URL url = new URL(aUrlString);
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty(GOOGLE_DRIVE_REST_AUTHORIZATION, "Bearer" + " " + mAccessToken);

                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
                // Log.d(TAG, "responseCode: " + responseCode);

                handleHttpResult(responseCode);

                String responseMessage = urlConnection.getResponseMessage();
                // Log.d(TAG, "responseMessage: " + responseMessage);

                inputStream = new BufferedInputStream(urlConnection.getInputStream());

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder jsonStringBuilder = new StringBuilder();

                String line = null;
                // Log.d(TAG, "entering line reader");
                while ((line = reader.readLine()) != null) {
                    jsonStringBuilder.append(line + "\n");
                }
                // Log.d(TAG, "existing line reader");

                String jsonString = jsonStringBuilder.toString();
                // Log.d(TAG, "Got object data, about to parse...");

                tryGet = false; // We have the data now, so don't retry

                /*
                if (paused) {
                    // Log.d(TAG, "unpausing ");
                    paused = false;
                    EMProgressInfo progressInfo = new EMProgressInfo();
                    progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TRANSFER_RESUMED;
                    aProgressHandler.progressUpdate(progressInfo);
                }
                */

                jsonObject = new JSONObject(jsonString);
            } catch (EMRecoverableAuthErrorException ex) {
                // Log.d(TAG, "*** Exception: " + ex.toString());
                // Log.d(TAG, Log.getStackTraceString(ex));
                // Retry ...
            } catch (Exception ex) {
                String temp = "";
                // Log.d(TAG, "*** Exception in getJsonObjectFromUrl");
                // Log.d(TAG, Log.getStackTraceString(ex));
                // Retry (assuming we haven't already got the data) ...
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();

                if (inputStream != null) {
                    try {
                        // TODO: will this close the underlying stream?
                        inputStream.close();
                    } catch (IOException e) {
                        // Ignore: nothing we can do here
                        // Log.d(TAG, "*** Exception");
                        // Log.d(TAG, Log.getStackTraceString(e));
                        e.printStackTrace();
                    }
                }
            }

            if (!aRetryOnFailure)
                tryGet = false;

            if (tryGet) { // If we're retrying then wait a short time first
                /*
                if (!paused) {
                    paused = true;
                    EMProgressInfo progressInfo = new EMProgressInfo();
                    progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TRANSFER_PAUSED; // TODO: we're assuming this is a network issue (so pausing)
                    aProgressHandler.progressUpdate(progressInfo);
                }
                */

                SystemClock.sleep(EMConfig.EM_WAIT_FOR_RE_AUTH_TIME_MS); // Wait then try again...
            }
        }

        // Log.d(TAG, "<< getJsonObjectFromUrl");

        return jsonObject;
    }

    public int listChildren(String aParentGoogleDriveId,
                            ArrayList<CMDGoogleDriveItem> aChildItems,
                            EMProgressHandler aProgressHandler) { // aChildItems is populated with a list of child item IDs
        // Log.d(TAG, ">> listChildren");
        int result = CMDError.CMD_RESULT_OK;

        aChildItems.clear();

        String queryValueString = "'"
                + aParentGoogleDriveId
                + "' in parents";

        String urlString = null;
        try {
            urlString = GOOGLE_REST_FILES + "?q=" + URLEncoder.encode(queryValueString, "UTF-8");
        }
        catch (Exception ex) {
            // TODO:
        }

        while (urlString != null) {
            JSONObject jObject = getJsonObjectFromUrl(urlString, aProgressHandler);

            urlString = null;

            try {
                try {
                    urlString = jObject.getString(GOOGLE_JSON_NEXT_LINK);
                }
                catch (Exception ex) {
                    // TODO: handle next link exception
                }

                JSONArray childItems = jObject.getJSONArray(GOOGLE_JSON_ITEMS);

                for (int childItemIndex = 0; childItemIndex < childItems.length(); childItemIndex++) {
                    JSONObject childItemJson = childItems.getJSONObject(childItemIndex);

                    // Process the full child items (not just the child summary like we would have got)
                    CMDGoogleDriveItem childItem = getDriveItemFromFullJsonItem(childItemJson);
                    if (childItem != null) {
                        if (!childItem.mIsTrashed) {
                            aChildItems.add(childItem);
                        }
                    } else {
                        // TODO: handle case where we can't extract the child item from the JSON, or just ignore?
                    }
                }
            } catch (Exception ex) {
                // TODO: handle this
                ex.printStackTrace();
            }
        }

        // Log.d(TAG, "<< listChildren");

        return result;
    }

    public int getRootFolder(CMDGoogleDriveItem aRootFolder) { // aRootFolder is populated with the details for the root folder
        int result = CMDError.CMD_RESULT_OK;

        aRootFolder.mGoogleDriveId = ROOT_ID_ALIAS;
        aRootFolder.mType = CMDGoogleDriveItemType.EGoogleDriveFolder;
        aRootFolder.mIsTrashed = false;
        aRootFolder.mName = "";
        aRootFolder.mSize = 0;

        return result;
    }

    /*
    // Initialize the cloud service with an access token (for cases where the login has been done elsewhere)
    void initWithAccessToken(String aAccessToken) {
        mAccessToken = aAccessToken;
    }
    */

    public void initWithUserName(String aAccessToken, String aUserName) {
        // Log.d(TAG, ">> initWithUserName: " + aUserName);
        mUserName = aUserName;
        mAccessToken = aAccessToken;

        if (mAccessToken == null)
            silentlyGenerateAccessToken();

        // Log.d(TAG, "<< initWithUserName");
    }

    void handleHttpResult(int aResultCode) throws EMRecoverableAuthErrorException, EMGoogleForbiddenErrorException {
        // Log.d(TAG, ">> handleHttpResult: " + aResultCode);
        if (aResultCode == 401) {
            silentlyGenerateAccessToken();
            throw(new EMRecoverableAuthErrorException("401 from Google. Have attempted to silently regenerate the access token. So try again..."));
        }

        if (aResultCode == 403) {
            // Log.d(TAG, "Google 403 error - Google Drive is probably full");
            throw(new EMGoogleForbiddenErrorException("403 from Google. Google Drive is probably full."));
        }

        // Log.d(TAG, "<< handleHttpResult: " + aResultCode);
    }

    public static class EMRecoverableAuthErrorException extends Exception {
        public EMRecoverableAuthErrorException(String aMessage) {
            super(aMessage);
        }
    }

    public static class EMGoogleForbiddenErrorException extends Exception {
        public EMGoogleForbiddenErrorException(String aMessage) {
            super(aMessage);
        }
    }

    // Get or re-get the access token from Google
    // Do this without asking the user for permission confirmation
    // Useful for cases where
    boolean silentlyGenerateAccessToken() {
        // Log.d(TAG, ">> silentlyGenerateAccessToken");
        boolean generatedToken = false;
        if (mAccessToken != null) {
            // If we already have an access token then assume that we want to invalidate it and get a new one
            // Log.d(TAG, "invalidating existing token");
            GoogleAuthUtil.invalidateToken(mContext, mAccessToken);
            mAccessToken = null;
            // Log.d(TAG, "invalidated existing token");
        }

        try {
            // Log.d(TAG, "Requesting new access token");
            // Get the access token with a synchronous API call (could be a long running operation)
            // String scope = "oauth2:server:client_id:" + CLIENT_ID + ":api_scope:https://www.googleapis.com/auth/" + SCOPE;
            String scope = "oauth2:" + "https://www.googleapis.com/auth/drive.file";
            mAccessToken = GoogleAuthUtil.getToken(mContext, mUserName, scope);

//            if (mAccessToken != null)
                // Log.d(TAG, "Got access token");
//            else
                // Log.d(TAG, "Not got access token");

            if (mAccessToken != null)
                generatedToken = true;
        } catch (Exception ex) {
            // Log.d(TAG, "*** Exception generating access token");
            Log.e(TAG, "exception", ex);
        }

        // Log.d(TAG, "<< silentlyGenerateAccessToken");

        return generatedToken;
    }

    private String mUserName = null;

    // Get the access token, for cases where it will be used with a different instance
    String getAccessToken() {
        return mAccessToken;
    }

    String getUserName() {
        return mUserName;
    }

    public boolean itemExists(String aPath, EMProgressHandler aProgressHandler) {
        String id = CMDGoogleUtility.getDriveIdForPathBlocking(aPath, this, aProgressHandler);

        if (id == null)
            return false;
        else
            return true;
    }

    // TODO: other access + writing functions

    public class CMDGoogleDriveInfo {
        public long mAvailableSpaceBytes = -1;
        public long mUsedSpaceBytes = -1;
    }

    private String mAccessToken;
    private ConnectivityManager mConnectivityManager;
}
