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

package com.pervacio.wds.sdk.internal;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMConfig;
import com.pervacio.wds.app.EMDataType;
import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMStringConsts;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.sdk.CMDError;
import com.pervacio.wds.sdk.internal.google.CMDCloudServiceGoogleDrive;
import com.pervacio.wds.sdk.internal.sdcard.CMDSDCardFileAccess;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

// Scans for backups in the background
// Notifies the observer when any of the following conditions are met:
    // An initial scan has been completed and no backups are found
    // A backup has been found including:
        // any salt that may be needed for decryption
        // the backup-started.xml data that can be used to verify any passwords
public class CMDBackupMonitor implements EMProgressHandler {

    static private final int CMD_STATE_NOT_STARTED = 0;
    static private final int CMD_STATE_DOWNLOADING_BACKUP_STARTED_FILE = 1; // This state retries until a backup-started file has been found
    static private final int CMD_STATE_DOWNLOADING_SALT_FILE = 2;

    private CMDCloudServiceFileInterface mCloudFileInterface;
    private CMDSDCardFileAccess mSDCardFileInterface;
    private CMDFileSystemInterface mSourceFileInterface;
    private boolean mForceCloudOnly = false;

    int mState = CMD_STATE_DOWNLOADING_BACKUP_STARTED_FILE;

    private Context mContext;
    private String mTempLocalPath;

    private byte[] mSaltData = null;
    private byte[] mBackupStartedData = null;

    public enum CMDBackupMonitorMode {
        EMSingleCheck, // Just check once for backups, not continuously
        EMContinuousCheck
    }

    CMDBackupMonitorMode mMode;

    private CMDBackupMonitorDelegate mDelegate;
    private CMDBackupDetails mBackupDetails;

    public CMDBackupMonitor(String aAccessToken, // Set to null if we're not looking for Google backups
                            String aUserName, // Set to null if we're not looking for Google backups
                            Context aContext,
                            CMDBackupMonitorDelegate aDelegate,
                            CMDBackupMonitorMode aMode,
                            boolean aForceCloudOnly) {
        mContext = aContext;
        mAccessToken = aAccessToken;
        mUserName = aUserName;
        mDelegate = aDelegate;
        mMode = aMode;
        mForceCloudOnly = aForceCloudOnly;
    }

    public void start() {
        mSDCardFileInterface = new CMDSDCardFileAccess(mContext);

        /*
        if (mAccessToken != null) {
            mCloudFileInterface = new CMDCloudServiceGoogleDrive(null, mContext); // TODO: assume it's google for now
            mCloudFileInterface.initWithAccessToken(mAccessToken);

        }
        */

        if (mUserName != null) {
            mCloudFileInterface = new CMDCloudServiceGoogleDrive(null, mContext); // TODO: assume it's google for now
            mCloudFileInterface.initWithUserName(mAccessToken, mUserName);

        }

        checkForBackup();
    }

    void moveToNextState() {
        mState++;

        if (mState == CMD_STATE_DOWNLOADING_SALT_FILE) {
            CMDCloudServiceFileInterface.CMDRemoteFileInfo remoteFileInfo = new CMDCloudServiceFileInterface.CMDRemoteFileInfo();
            remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/salt";
            remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_NOT_SET;
            mTempLocalPath = CMDUtility.temporaryFileName();
            mSourceFileInterface.downloadFileAsync(mTempLocalPath, remoteFileInfo, this);
        }
        else if (mState == CMD_STATE_DOWNLOADING_BACKUP_STARTED_FILE) {
            CMDCloudServiceFileInterface.CMDRemoteFileInfo remoteFileInfo = new CMDCloudServiceFileInterface.CMDRemoteFileInfo();
            remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/backup-started.xml";
            remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_NOT_SET;
            mTempLocalPath = CMDUtility.temporaryFileName();
            mSourceFileInterface.downloadFileAsync(mTempLocalPath, remoteFileInfo, this);
        }
    }

    private String mAccessToken;
    private String mUserName;

    @Override
    public void taskComplete(boolean aSuccess) {
        // Log.d(EMConfig.TAG, ">> taskComplete: mState: " + mState);

    if (mState == CMD_STATE_DOWNLOADING_BACKUP_STARTED_FILE) {
        try {
            mBackupStartedData = EMUtility.readFileToByteArray(new File(mTempLocalPath));
            File tempFile = new File(mTempLocalPath);
            long modifedTimeStamp = tempFile.lastModified();
            Date modifiedDate = new Date(modifedTimeStamp);
            mBackupDetails.mBackupTimeStamp = modifiedDate;
        } catch (IOException e) {
            // Ignore: nothing much we can do if we can't read the file
        }
    } else if (mState == CMD_STATE_DOWNLOADING_SALT_FILE)
        {
            try {
                // Populate the backup details with the encryption data, and the modified date of the file
                mSaltData = EMUtility.readFileToByteArray(new File(mTempLocalPath));
                mBackupDetails.mSalt = mSaltData;
                mBackupDetails.mIsEncrypted = true;
                mBackupDetails.mReferenceData = mBackupStartedData;
                mDelegate.backupFound(mBackupDetails);
            } catch (IOException e) {
                taskError(CMDError.ERROR_GETTING_SALT, false);
                return;
            }
        }

        // Log.d(EMConfig.TAG, "<< cmdOperationComplete: mState: " + mState);

        // Move to the next state - sends a completion message to the client when done
        moveToNextState();
    }

    void checkForBackup() {
        boolean sdCardBackupFolderExists = false;

        if ((mSDCardFileInterface != null) && (!mForceCloudOnly)) {
            boolean hasReadExternalStorage = (EMUtility.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

            if (hasReadExternalStorage) {
                DLog.log("*** Starting SD card restore");

                String[] possibleSDCardPaths = mSDCardFileInterface.getPossibleExternalSDCardPaths();

                for (String possibleSDCardPath : possibleSDCardPaths) {
                    try {
                        String backupStartedPath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/backup-started.xml";
                        mSDCardFileInterface.setSDCardPath(possibleSDCardPath);
                        if (mSDCardFileInterface.itemExistsBlocking(backupStartedPath, this)) {
                            sdCardBackupFolderExists = true;
                            mSourceFileInterface = mSDCardFileInterface;
                            // EMMigrateStatus.setTransportMode(EMProgressInfo.EMTransportMode.EM_SD_CARD);
                            // if SD backup found then move state to CMD_STATE_WAIT_FOR_BACKUP_FINISHED_FILE (when this function exists we will move to the next state)
                            // mState = CMD_STATE_WAIT_FOR_BACKUP_FINISHED_FILE;
                            mBackupDetails = new CMDBackupDetails();
                            mBackupDetails.mBackupType = CMDBackupDetails.CMDBackupType.CMD_SD_CARD;
                            mSourceFileInterface = mSDCardFileInterface;

                            // Get the backup-started file from the SD card
                            CMDCloudServiceFileInterface.CMDRemoteFileInfo remoteFileInfo = new CMDCloudServiceFileInterface.CMDRemoteFileInfo();
                            remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/backup-started.xml";
                            remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_ACCOUNTS;
                            mTempLocalPath = CMDUtility.temporaryFileName();
                            mSourceFileInterface.downloadFileAsync(mTempLocalPath, remoteFileInfo, this);
                            break;
                        }
                    }
                    catch(Exception aException){
                        // Ignore: It's probably not an SD card, or we don't have permission to access it
                    }
                }
            }
        }

        if (sdCardBackupFolderExists) {
            moveToNextState();
        }
        else {
            if (mCloudFileInterface == null) {
                // If there's no cloud file interface, and nothing on the SD card then wait a while and check the SD card again
                waitAndThenCheckAgainOrComplete();
            }
            else {
                mBackupDetails = new CMDBackupDetails();
                mBackupDetails.mBackupType = CMDBackupDetails.CMDBackupType.CMD_GOOGLE_DRIVE;

                mSourceFileInterface = mCloudFileInterface;

                // Get the backup-started file from Google (if we have an access token)
                CMDCloudServiceFileInterface.CMDRemoteFileInfo remoteFileInfo = new CMDCloudServiceFileInterface.CMDRemoteFileInfo();
                remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/backup-started.xml";
                remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_ACCOUNTS;
                mTempLocalPath = CMDUtility.temporaryFileName();
                mSourceFileInterface.downloadFileAsync(mTempLocalPath, remoteFileInfo, this);
            }
        }
    }

    private void waitAndThenCheckAgainOrComplete() {
        if (mMode == CMDBackupMonitorMode.EMSingleCheck)
            mDelegate.noBackupFound();
        else {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mHandler.obtainMessage(1).sendToTarget();
                }
            }, EMConfig.LOOK_AGAIN_FOR_BACKUP_STARTED_DELAY);
        }
    }

    public Handler mHandler = new Handler() { // This is needed so that checkForBackup is run on the main thread, because there's a chance we could end up modifying the UI
        public void handleMessage(Message msg) {
            checkForBackup();
        }
    };

    @Override
    public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
        if (errorCode == CMDError.CMD_ERROR_NOT_ENOUGH_SPACE_ON_LOCAL_DEVICE) {
            // This is not recoverable so we need to fail
            mDelegate.backupMonitorError(errorCode);
            return;
        }

        if (mState == CMD_STATE_DOWNLOADING_SALT_FILE) {
            // This probably just means the backup isn't encrypted, so complete with the data we already have (i.e. without the salt or encrypted flag set)
            mDelegate.backupFound(mBackupDetails);
        }
        else if (mState == CMD_STATE_DOWNLOADING_BACKUP_STARTED_FILE) {
            waitAndThenCheckAgainOrComplete();
        }
    }

    @Override
    public void progressUpdate(EMProgressInfo progressInfo) {
        if (progressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_TRANSFER_PAUSED) {
            mDelegate.backupMonitorPaused();
        }
        else if (progressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_TRANSFER_RESUMED) {
            mDelegate.backupMonitorResumed();
        }
    }
}
