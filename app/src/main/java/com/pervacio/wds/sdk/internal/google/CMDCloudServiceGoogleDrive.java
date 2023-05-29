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

// import android.R;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.pervacio.wds.app.EMFileFinder;
import com.pervacio.wds.app.EMFileMetaData;
import com.pervacio.wds.app.EMGenerateCalendarXmlAsyncTask;
import com.pervacio.wds.app.EMGenerateContactsXmlAsyncTask;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMStringConsts;
import com.pervacio.wds.sdk.CMDError;
import com.pervacio.wds.sdk.internal.CMDCloudServiceFileInterface;
import com.pervacio.wds.sdk.internal.CMDFileSystemInterface;

import java.util.ArrayList;

public class CMDCloudServiceGoogleDrive implements CMDCloudServiceFileInterface, EMProgressHandler {
	
	private enum CMDMainGoogleOperation
	{
		CMD_GOOGLE_NO_OPERATION,
		CMD_GOOGLE_AUTHENTICATING,
		CMD_GOOGLE_SELECT_ACCOUNT
	}
	
	private CMDMainGoogleOperation mMainOperation;
	private EMProgressHandler mCurrentProgressHandler;
	private CMDGoogleDriveUploadFileAsyncTask mUploadFileTask;
	private CMDGoogleDriveUploadMultipleFilesAsyncTask mUploadFilesTask;
	private CMDGoogleDriveDownloadFileAsyncTask mDownloadFileTask;
	private Activity mParentActivity;
    private Context mContext;
	private CMDGoogleDriveAccess mGoogleDriveAccess;
	private CMDGoogleAuthenticateAsyncTask mAuthenticateAsyncTask;

	public CMDCloudServiceGoogleDrive(Activity parentActivity, Context aContext) {
		mParentActivity = parentActivity;
        mContext = aContext;
		mGoogleDriveAccess = new CMDGoogleDriveAccess(aContext);
	}
	
	private static final String TAG = "EasyMigrate";
	
	@Override
	public void startLogInWithAccountNameAsync(String accountName,
												EMProgressHandler progressHandler,
											   	boolean aIsDestinationDevice) {
		// Log.d(TAG, "startLogInWithAccountNameAsync: accountName: " + accountName + " progressHandler: " + progressHandler.toString());

        if (mParentActivity != null)
    		// Log.d(TAG, "parentActivity: " + mParentActivity.toString());
				
		mMainOperation = CMDMainGoogleOperation.CMD_GOOGLE_AUTHENTICATING;
		mCurrentProgressHandler = progressHandler;
		
        EMProgressInfo progress = new EMProgressInfo();
        progress.mOperationType = EMProgressInfo.EMOperationType.EM_USER_LOGGING_IN;
        mCurrentProgressHandler.progressUpdate(progress);
		
		// Log.d(TAG, "Checking if play services are available");
		int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
		if (result == ConnectionResult.SUCCESS)
		{
			// Log.d(TAG, "Google services are available");

			mAuthenticateAsyncTask = new CMDGoogleAuthenticateAsyncTask(accountName, mParentActivity, mGoogleDriveAccess, aIsDestinationDevice);
			mAuthenticateAsyncTask.startTask(this);

			// Log.d(TAG, "Connection to Google services requested...");
		}
	}
	
	@Override
	public void startUserLoginAsync(EMProgressHandler progressHandler) {
		// Log.d(TAG, ">> startUserLoginAsync");
		// mMainOperation = CMDMainGoogleOperation.CMD_GOOGLE_AUTHENTICATING;
		mMainOperation = CMDMainGoogleOperation.CMD_GOOGLE_SELECT_ACCOUNT;
		mCurrentProgressHandler = progressHandler;
		
		int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mParentActivity);
		if (result == ConnectionResult.SUCCESS)
		{
			// Log.d(TAG, "About to start account picker ...");
			Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"},
			         true, null, null, null, null);
			 mParentActivity.startActivityForResult(intent, CMDError.SELECTING_ACCOUNT);
		}
		else
		{
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(result, mParentActivity, 1);
			errorDialog.show();
			progressHandler.taskError(CMDError.GOOGLE_SERVICES_NOT_AVAILABLE, true);
		}
	}

	@Override
	public void startUserAccountCreationAndLoginAsync(EMProgressHandler progressHandler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void userLogoutAsyncAsync(EMProgressHandler progressHandler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void uploadFileAsync(String sourcePath,
									CMDRemoteFileInfo remoteFileInfo,
									EMProgressHandler progressHandler,
									int totalSubItems) {
		mUploadFileTask = new CMDGoogleDriveUploadFileAsyncTask(sourcePath, remoteFileInfo, mGoogleDriveAccess, totalSubItems);
		mUploadFileTask.startTask(progressHandler);
	}

	@Override
	public void uploadFilesAsync(ArrayList<EMFileMetaData> filesToUpload,
						  String remoteBasePath,
						  EMProgressHandler progressHandler) {
		mUploadFilesTask = new CMDGoogleDriveUploadMultipleFilesAsyncTask(filesToUpload, remoteBasePath, mGoogleDriveAccess);
		mUploadFilesTask.startTask(progressHandler);
	}

	@Override
	public void downloadFileAsync(String localPath,
									CMDRemoteFileInfo remoteFileInfo,
									EMProgressHandler progressHandler) {
		mDownloadFileTask = new CMDGoogleDriveDownloadFileAsyncTask(localPath, remoteFileInfo, mGoogleDriveAccess);
		mDownloadFileTask.startTask(progressHandler);
	}

	@Override
	public void deleteFolderAsync(String remoteFolderPath,
									EMProgressHandler progressHandler) {
		CMDGoogleDeleteFolderContentsAsyncTask deleteFolderAsyncTask = new CMDGoogleDeleteFolderContentsAsyncTask("/" + EMStringConsts.EM_BACKUP_FOLDER_NAME, mGoogleDriveAccess);
		deleteFolderAsyncTask.startTask(progressHandler);
	}

	@Override
	public void deleteFileAsync(String remoteFilePath,
									EMProgressHandler progressHandler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public CMDRemoteFileInfo[] getRemoteFilesInfoAsync(String path,
														EMProgressHandler progressHandler) {
		// TODO Auto-generated method stub
		return null;
	}

    // Copy the contents of the remote folder (folder and files) to the local filesystem
    @Override
    public void copyRemoteFolderContentsToLocalAsync(String remoteFolderPath,
                                              String localFolderPath,
                                              boolean runMediaScannerOnFiles, // Set to true if the Android media scanner should be run on copied files
                                              int dataType,
                                              EMProgressHandler progressHandler) {
        CMDGoogleCopyFolderContentsToLocalAsyncTask copyFolderContentsToLocalAsyncTask = new CMDGoogleCopyFolderContentsToLocalAsyncTask(remoteFolderPath,
                    localFolderPath,
                    runMediaScannerOnFiles,
                    dataType,
					mGoogleDriveAccess,
                    mContext);

        copyFolderContentsToLocalAsyncTask.startTask(progressHandler);
    }

	public void getBackupFolderDataTypesAsync(String aRemoteFolderPath, EMProgressHandler aProgressHandler)
	{
		CMDGoogleGetBackupFolderDataTypesAsyncTask task = new CMDGoogleGetBackupFolderDataTypesAsyncTask(aRemoteFolderPath, mGoogleDriveAccess);
		task.startTask(aProgressHandler);
	}

	private EMFileFinder mFileFinderTask;
	private ArrayList<EMFileMetaData> mFilesToTransfer;
	private EMGenerateContactsXmlAsyncTask mGenerateContactsTask;
	private EMGenerateCalendarXmlAsyncTask mGenerateCalendarTask;
	private int state = 0;

	/*
	// Initialize the cloud service with an access token (for cases where the login has been done elsewhere)
	@Override
	public void initWithAccessToken(String aAccessToken) {
		mGoogleDriveAccess.initWithAccessToken(aAccessToken);
	}
	*/

	@Override
	public void initWithUserName(String aAccessToken, String aUserName) {
		mGoogleDriveAccess.initWithUserName(aAccessToken, aUserName);
	}

	// Get the access token, for cases where it will be used with a different instance
	@Override
	public String getAccessToken() {
		return mGoogleDriveAccess.getAccessToken();
	}

	@Override
	public String getUserName() {
		return mGoogleDriveAccess.getUserName();
	}

	@Override
	public boolean itemExistsBlocking(String aPath, EMProgressHandler aProgressHandler) {
		return mGoogleDriveAccess.itemExists(aPath, aProgressHandler);
	}

	@Override
	public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
		mCurrentProgressHandler.taskError(errorCode, alreadyDisplayedDialog);
	}

	@Override
	public void taskComplete(boolean aSuccess) {
		mCurrentProgressHandler.taskComplete(aSuccess);
	}

	@Override
	public void progressUpdate(EMProgressInfo progressInfo) {
		mCurrentProgressHandler.progressUpdate(progressInfo);
	}

	public CMDFileSystemInterface.CMDFileSystemInfo getCachedFileSystemInfo()
	{
		return mGoogleDriveAccess.getCachedDriveInfo();
	}

	private int mConnectionAttempts = 0;
}
