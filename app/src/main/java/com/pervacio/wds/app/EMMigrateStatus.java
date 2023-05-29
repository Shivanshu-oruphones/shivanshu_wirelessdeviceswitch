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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.sdk.CMDError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EMMigrateStatus {
    public static void setmInstance(EMMigrateStatus instance) {
        mInstance = instance;
    }

    public static EMMigrateStatus mInstance;

    private static final String KEY_INDICATOR_WIFI_LINKSPEED = "WifiLinkSpeed";

    private HashMap<String, String> keyIndicators = new HashMap<>();
    private ArrayList<Object> dataTransferParams = new ArrayList<Object>();
    private ArrayList<Object> dataReadWriteParams = new ArrayList<Object>();
    private long migrationStartTime=System.currentTimeMillis();
    private long totalSizeTobeTransfered=0;
    private long lastSpeed=0;

    public static void initialize() {
        if (mInstance == null)
            mInstance = new EMMigrateStatus();
    }

    public static void setTransportMode(EMProgressInfo.EMTransportMode aTransportMode) {
        mInstance.mTransportMode = aTransportMode;
    }

    public static EMProgressInfo.EMTransportMode getTransportMode() {
        return mInstance.mTransportMode;
    }

    static public void setError(int aErrorCode) {
        mInstance.mErrorCode = aErrorCode;
    }

    static public int getError(Context aContext, boolean aIsTarget) {
        int errorCode = mInstance.mErrorCode;

        EMPermissionChecker permissionChecker = new EMPermissionChecker(aContext);
        permissionChecker.getMissingPermissions(aIsTarget);
        Set<Integer> dataTypesWithMissingPermissions = permissionChecker.getDataTypesWithMissingPermissions(aIsTarget);
        EMTransferStatus status = EMTransferStatus.SUCCESS;
        int dataType = EMDataType.EM_DATA_TYPE_NOT_SET;
        while ((dataType <<= 1) < EMDataType.EM_DATA_TYPE_NO_MORE_DATA) {
            if ((EMMigrateStatus.getTotalFailure(dataType))
                    || (EMMigrateStatus.getItemsNotTransferred(dataType) > 0)) {
                if (dataTypesWithMissingPermissions.contains(dataType)) {
                    errorCode = CMDError.CMD_PERMISSION_FAILURES;
                }
                else {
                    if (errorCode == CMDError.CMD_RESULT_OK) {
                        errorCode = CMDError.CMD_GENERAL_FAILURES;
                    }
                }
            }
        }

        return errorCode;
    }

    int mErrorCode = CMDError.CMD_RESULT_OK;

    public static void addAddedAccount() {
        mInstance.mAddedAccountTotal++;
    }

    public static int getAddedAccountCount() {
        return mInstance.mAddedAccountTotal;
    }

    public static void addEmailAccountAddress(String aEmailAddress) {
        mInstance.mEmailAccountAddresses.add(aEmailAddress);
    }

    static public List<String> getEmailAccountAddresses() {
        return mInstance.mEmailAccountAddresses;
    }

    // TODO: this should really be synchronized - but not really an issue as writing and reading happen at different times
    private ArrayList<String> mEmailAccountAddresses = new ArrayList<String>();

    private EMProgressInfo.EMTransportMode mTransportMode = EMProgressInfo.EMTransportMode.EM_UNKNOWN;

    private int mAddedAccountTotal = 0;

    boolean mUserAllowedDataOverCellular = false;
    boolean mAllowDataOverCellular = false;

    // Allow the user of cellular data automatically, e.g. for logging in to Google
    public static void setAutoAllowCellularData(boolean aAllowCellularData) {
        mInstance.mAllowDataOverCellular = aAllowCellularData;
    }

    // Allow the user of cellular with user consent, e.g. for polling for backups, uploading backups, downloading backups, etc.
    public static void setUserAllowCellularData(boolean aAllowCellularData) {
        mInstance.mUserAllowedDataOverCellular = aAllowCellularData;
    }

    // Returns true if the user has explicitly allowed the user of cellular data
    public static boolean userAllowedDataOverCellular() {
        return mInstance.mUserAllowedDataOverCellular;
    }

    // Returns true if cellular data use is allowed at this time (e.g. if user has given consent, or setAutoAllowCellularData has been set to true)
    public static boolean allowDataOverCellular() {
        if (mInstance.mUserAllowedDataOverCellular)
            return true;
        else
            return mInstance.mAllowDataOverCellular;
    }

    static public void setItemsTransferred(int aDataType, int aItemsTransferred) {
        mInstance.mTransferredFilesMap.put(aDataType, aItemsTransferred);
    }

    static public void setItemsNotTransferred(int aDataType, int aItemsNotTransferred) {
        mInstance.mNotTransferredFilesMap.put(aDataType, aItemsNotTransferred);
    }

    synchronized static public void addItemTransferred(int aDataType) {
        Integer filesOfDataType = mInstance.mTransferredFilesMap.get(new Integer(aDataType));
        if (filesOfDataType != null) {
            mInstance.mTransferredFilesMap.put(aDataType, filesOfDataType + 1);
        }
        else {
            mInstance.mTransferredFilesMap.put(aDataType, 1);
        }
    }

    static public boolean isMediaRestorationCompleted() {
        return ((CommonUtil.getInstance().getSelectedMediaCount() - CommonUtil.getInstance().getRestoredMediaCount()) == 0);
    }

    static public void setPreviouslyTransferredContacts(String item) {
        mInstance.mNotTransferredContactsSet.add(item);
    }

    static public ArrayList<String> getPreviouslyTransferredContacts() {
        return mInstance.mNotTransferredContactsSet;
    }

    static public void setPreviouslyTransferredMessages(String item) {
        mInstance.mNotTransferredMessagesSet.add(item);
    }

    static public ArrayList<String> getPreviouslyTransferredMessages() {
        return mInstance.mNotTransferredMessagesSet;
    }

    static public void addContentDetails(int aDataType, long count) {

        mInstance.mContentDetailsMap.put(aDataType, count);
    }

    static public long getContentDetails(int aDataType) {
        long count = 0;
        try {
            count = mInstance.mContentDetailsMap.get(aDataType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }


    synchronized static public void addItemTransferStarted(int aDataType) {
        Integer filesOfDataType = mInstance.mTransferStartedMap.get(new Integer(aDataType));
        if (filesOfDataType != null) {
            mInstance.mTransferStartedMap.put(aDataType, filesOfDataType + 1);
        }
        else {
            mInstance.mTransferStartedMap.put(aDataType, 1);
        }
    }

    static public int getItemTransferStarted(int aDataType) {
        int filesTransferred = 0;

        Integer filesOfDataType = mInstance.mTransferStartedMap.get(new Integer(aDataType));
        if (filesOfDataType != null)
            filesTransferred = filesOfDataType.intValue();

        return filesTransferred;
    }

    static public void addItemNotTransferred(int aDataType) {
        Integer filesOfDataType = mInstance.mNotTransferredFilesMap.get(new Integer(aDataType));
        if (filesOfDataType != null) {
            mInstance.mNotTransferredFilesMap.put(aDataType, filesOfDataType + 1);
        }
        else {
            mInstance.mNotTransferredFilesMap.put(aDataType, 1);
        }
    }

    public static void setTotalFailure(int aDataType) {
        mInstance.mTotalFailureMap.put(aDataType, true);
    }

    public static void clearTotalFailures() {
        mInstance.mTotalFailureMap.clear();
    }

    public static boolean getTotalFailure(int aDataType) {
        boolean totalFailure = false;
        Boolean totalFailureObject = mInstance.mTotalFailureMap.get(aDataType);
        if (totalFailureObject != null) {
            if (totalFailureObject) {
                totalFailure = true;
            }
        }

        return totalFailure;
    }

    synchronized public static void addTransferedFilesSize(int aDataType,long size) {
        Long filesOfDataType = mInstance.mTransferredFilesSizeMap.get(new Integer(aDataType));
        if (filesOfDataType != null) {
            mInstance.mTransferredFilesSizeMap.put(aDataType, filesOfDataType + size);
        }
        else {
            mInstance.mTransferredFilesSizeMap.put(aDataType, size);
        }
    }

    public static long getTransferedFilesSize(int aDataType){
        long fileSize=0;
        Long transferedFilesSize= mInstance.mTransferredFilesSizeMap.get(aDataType);
        if(transferedFilesSize!=null){
            fileSize= transferedFilesSize.longValue();
        }
        return fileSize;
    }

    synchronized public static void addTransferredFailedSize(int aDataType,long size) {
        Long filesOfDataType = mInstance.mTransferredFailedSizeMap.get(new Integer(aDataType));
        if (filesOfDataType != null) {
            mInstance.mTransferredFailedSizeMap.put(aDataType, filesOfDataType + size);
        }
        else {
            mInstance.mTransferredFailedSizeMap.put(aDataType, size);
        }
    }

    public static long getTransferredFailedSize(int aDataType){
        long fileSize=0;
        Long transferedFilesSize= mInstance.mTransferredFailedSizeMap.get(aDataType);
        if(transferedFilesSize!=null){
            fileSize= transferedFilesSize.longValue();
        }
        return fileSize;
    }

    public static long getTransferredFailedSize() {
        long fileSize = 0;
        for (int aDataType : mInstance.mTransferredFailedSizeMap.keySet()) {
            Long transferredFilesSize = mInstance.mTransferredFailedSizeMap.get(aDataType);
            if (transferredFilesSize != null) {
                fileSize += transferredFilesSize.longValue();
            }
        }
        return fileSize;
    }


    synchronized public static void addLiveTransferredSize(int aDataType,long size) {
        Long currentTransferred = mInstance.mLiveTransferredSizeMap.get(aDataType);
        if (currentTransferred != null) {
            mInstance.mLiveTransferredSizeMap.put(aDataType, currentTransferred + size);
        }
        else {
            mInstance.mLiveTransferredSizeMap.put(aDataType, size);
        }
    }

    public static long getLiveTransferredSize(int aDataType){
        Long liveTransferredSize = mInstance.mLiveTransferredSizeMap.get(aDataType);

        if(liveTransferredSize == null) {
            return -1;
        }
        else {
            return liveTransferredSize;
        }
    }

    private static long getTransferredBytes(int dataType){
        long mTransferredBytes = getLiveTransferredSize(dataType);
        if(mTransferredBytes == 0)
            mTransferredBytes = getTransferedFilesSize(dataType);
        return mTransferredBytes;
    }


    public static long getTransferredBytesSofar() {
        long mTransferredBytes = 0;
        mTransferredBytes += getTransferredBytes(EMDataType.EM_DATA_TYPE_PHOTOS);
        mTransferredBytes += getTransferredBytes(EMDataType.EM_DATA_TYPE_VIDEO);
        mTransferredBytes += getTransferredBytes(EMDataType.EM_DATA_TYPE_MUSIC);
        mTransferredBytes += getTransferredBytes(EMDataType.EM_DATA_TYPE_APP);
        mTransferredBytes += getTransferredBytes(EMDataType.EM_DATA_TYPE_DOCUMENTS);
        return mTransferredBytes;
    }

    //This is flag to enable/disable live update for any datatype
    public static boolean isLiveUpdateRequired(int dataType) {
        return dataType == EMDataType.EM_DATA_TYPE_VIDEO ||
                dataType == EMDataType.EM_DATA_TYPE_MUSIC ||
                dataType == EMDataType.EM_DATA_TYPE_PHOTOS ||
                dataType == EMDataType.EM_DATA_TYPE_DOCUMENTS ||
                dataType == EMDataType.EM_DATA_TYPE_APP;
    }

    public static long getTransferredFilesSize() {
        long audio = getTransferedFilesSize(EMDataType.EM_DATA_TYPE_MUSIC);
        long video = getTransferedFilesSize(EMDataType.EM_DATA_TYPE_VIDEO);
        long image = getTransferedFilesSize(EMDataType.EM_DATA_TYPE_PHOTOS);
        long app = getTransferedFilesSize(EMDataType.EM_DATA_TYPE_APP);
        long docs = getTransferedFilesSize(EMDataType.EM_DATA_TYPE_DOCUMENTS);
        return (audio + video + image + app + docs);
    }

    public static void setMigrationStartTime(long migrationStartTime){
        mInstance.migrationStartTime=migrationStartTime;
    }

    public static long getMigrationStartTime(){
        return mInstance.migrationStartTime;
    }
    static public int getItemsTransferred(int aDataType) {
        int filesTransferred = 0;
        Integer filesOfDataType = mInstance.mTransferredFilesMap.get(new Integer(aDataType));
        if (filesOfDataType != null)
            filesTransferred = filesOfDataType.intValue();

        return filesTransferred;
    }

    static public int getItemsNotTransferred(int aDataType) {
        int filesTransferred = 0;

        Integer filesOfDataType = mInstance.mNotTransferredFilesMap.get(new Integer(aDataType));
        if (filesOfDataType != null)
            filesTransferred = filesOfDataType.intValue();

        return filesTransferred;
    }

    private Map<Integer, Long> mTransferredFilesSizeMap = new HashMap<Integer, Long>();
    private Map<Integer, Integer> mTransferredFilesMap = new HashMap<Integer, Integer>();
    private Map<Integer,Long> mTransferredFailedSizeMap = new HashMap<>();
    private ArrayList<String> mNotTransferredContactsSet = new ArrayList<>();
    private ArrayList<String> mNotTransferredMessagesSet = new ArrayList<>();
    private Map<Integer, Integer> mTransferStartedMap = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> mNotTransferredFilesMap = new HashMap<Integer, Integer>();
    private Map<Integer, Boolean> mTotalFailureMap = new HashMap<Integer, Boolean>();
    private Map<Integer, Long> mContentDetailsMap = new HashMap<>();
    private boolean mPaused = false;

    //To store live progress of media files
    public static final long LIVE_CHUNK_UPDATE_SIZE = 1024 * 1024 * 5;
    private Map<Integer, Long> mLiveTransferredSizeMap = new HashMap<Integer, Long>();

    static public boolean getPausedStatus() {
        return mInstance.mPaused;
    }

    public static void raiseExceptionIfNetworkNotAllowed(ConnectivityManager aConnectivityManager, EMProgressHandler aProgressHandler) throws EMNoWiFiException {
        // // Log.d(EMConfig.TAG, ">> raiseExceptionIfNetworkNotAllowed");
        NetworkInfo info = aConnectivityManager.getActiveNetworkInfo();
        boolean connectedToWiFi = (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
        if (!connectedToWiFi) {
            // // Log.d(EMConfig.TAG, "Not connected to wifi");

            boolean allowDataOverCellular = allowDataOverCellular();

            // // Log.d(EMConfig.TAG, "allowDataOverCellular=" + allowDataOverCellular);

            if (!allowDataOverCellular) {
                // Log.d(EMConfig.TAG, "Cellular not permitted");
                if (!mInstance.mPaused) {
                    // Log.d(EMConfig.TAG, "Sending paused status");
                    mInstance.mPaused = true;
                    EMProgressInfo progressInfo = new EMProgressInfo();
                    progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TRANSFER_PAUSED; // TODO: we're assuming this is a network issue (so pausing)
                    aProgressHandler.progressUpdate(progressInfo);
                }

                // Log.d(EMConfig.TAG, "*** Network not allowed - raising exception");
                throw new EMNoWiFiException("No WiFi connection and user not allowed transfer over cellular");
            }
        }

        if (mInstance.mPaused) {
            // Log.d(EMConfig.TAG, "Sending unpaused status");
            mInstance.mPaused = false;
            EMProgressInfo progressInfo = new EMProgressInfo();
            progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TRANSFER_RESUMED;
            aProgressHandler.progressUpdate(progressInfo);
        }

        // // Log.d(EMConfig.TAG, "<< raiseExceptionIfNetworkNotAllowed");
    }

    public static void raiseExceptionIfInsufficientStorageSpace(String aDestinationFilePath, long aMinimumStorageSpace) throws EMLowLocalStorageException {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
        if (bytesAvailable < aMinimumStorageSpace) {
            throw new EMLowLocalStorageException("Free storage space is below the threshold - fail with low disk space");
        }
    }

    public static void setSourceDeviceType(String aSourceDeviceType) {
        mInstance.mSourceDeviceType = aSourceDeviceType;
    }

    public static void setStartTransfer() {
        if (mInstance.mStartTimeStamp == 0) // This allows us to call it multiple times without resetting the timestamp
            mInstance.mStartTimeStamp = System.currentTimeMillis();
    }

    public static void addBytesTransferred(long aBytes) {
        mInstance.mBytesTransferred += aBytes;
    }

    String mSourceDeviceType = EMStringConsts.EM_DEVICE_TYPE_UNKNOWN;
    long mBytesTransferred = 0;
    long mStartTimeStamp = 0;

    public static long getTotalSizeTobeTransfered() {
        return mInstance.totalSizeTobeTransfered;
    }

    public static void setTotalSizeTobeTransfered(long totalSizeTobeTransfered) {
        mInstance.totalSizeTobeTransfered = totalSizeTobeTransfered;
    }

    public static class EMNoWiFiException extends Exception {
        public EMNoWiFiException(String aMessage) {
            super(aMessage);
        }
    }

    public static class EMLowLocalStorageException extends Exception {
        public EMLowLocalStorageException(String aMessage) {
            super(aMessage);
        }
    }

    private boolean mGoogleBackupInProgress = false;
    private String mGoogleBackupFolderId;
    private String mGoogleUserName;

    public static void setGoogleBackupInProgress(Context aContext,
                                                    String aBackupFolderId,
                                                    String aUsername) {
        if (!mInstance.mGoogleBackupInProgress) {
            mInstance.mGoogleBackupInProgress = true;
            mInstance.mGoogleBackupFolderId = aBackupFolderId;
            mInstance.mGoogleUserName = aUsername;
        }
    }

    enum EMTransferStatus {
        SUCCESS,
        GENERAL_FAILURES,
        PERMISSION_FAILURES
    }

    /*
    enum EMSourceDeviceType {
        EUnknown,
        EIOS,
        EAndroid,
        EBBOS,
        EBB10,
        EWindowsPhone
    }
    */

    public static boolean getGoogleBackupStarted() {
        return mInstance.mGoogleBackupInProgress;
    }

    public static String getGoogleBackupFolderId() {
        return mInstance.mGoogleBackupFolderId;
    }

    public static boolean qrCodeWifiDirectMode() {
        return mInstance.mQrCodeWifiDirectMode;
    }

    public static void setQrCodeWifiDirectMode(boolean aEnable) {
        mInstance.mQrCodeWifiDirectMode = aEnable;
    }

    public static void udpateWifiLinkSpeed(String speed) {
        mInstance.keyIndicators.put(KEY_INDICATOR_WIFI_LINKSPEED, speed);
    }

    public static void updateDataTransferParams(long bytes, long timeInNanos) {
        mInstance.dataTransferParams.add(new long[]{bytes, timeInNanos});
    }

    public static void updateDataReadWriteParams(long bytes, long timeInNanos) {
        mInstance.dataReadWriteParams.add(new long[]{bytes, timeInNanos});
    }

    public static void logKeyIndicators() {
        DLog.log("*** Key Indicators ***");
        DLog.log("WifiLinkSpeed: "+mInstance.keyIndicators.get(KEY_INDICATOR_WIFI_LINKSPEED));
        long totalReadWriteBytes = 0;
        long totalReadWriteNanos = 0;
        for(int i=0;i<mInstance.dataReadWriteParams.size();i++) {
            long[] data = (long[])mInstance.dataReadWriteParams.get(i);
            totalReadWriteBytes += data[0];
            totalReadWriteNanos += data[1];
        }
        DLog.log("Total MB read/write: " + totalReadWriteBytes/(1024*1024));
        DLog.log("Total time took to read/write(in secs): " + totalReadWriteNanos/(1000*1000*1000));
        long totalTransferedBytes = 0;
        long totalTrasferedTimeNanos = 0;
        for(int i=0;i<mInstance.dataReadWriteParams.size();i++) {
            long[] data = (long[])mInstance.dataReadWriteParams.get(i);
            totalTransferedBytes += data[0];
            totalTrasferedTimeNanos += data[1];
        }
        DLog.log("Total MB transferred: " + totalTransferedBytes/(1024*1024));
        DLog.log("Total time took to transfer data(in secs): " + totalTrasferedTimeNanos/(1000*1000*1000));
        DLog.log("**********************");
    }

    private boolean mQrCodeWifiDirectMode = EMConfig.QR_CODE_MODE_BY_DEFAULT;

}
