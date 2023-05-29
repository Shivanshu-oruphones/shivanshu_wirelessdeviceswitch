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

package com.pervacio.wds.sdk;


import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.sdk.internal.CMDCloudServiceFileInterface;
import com.pervacio.wds.sdk.internal.CMDCreateBackupAndWriteDataHelper;
import com.pervacio.wds.sdk.internal.CMDFileSystemInterface;
import com.pervacio.wds.sdk.internal.CMDRestoreBackupToDeviceHelper;
import com.pervacio.wds.sdk.internal.CMDUtility;
import com.pervacio.wds.sdk.internal.google.CMDCloudServiceGoogleDrive;
import com.pervacio.wds.sdk.internal.sdcard.CMDSDCardFileAccess;

public class CMDBackupAndRestoreEngine {
	final static String TAG = "EasyMigrate";

	public CMDBackupAndRestoreEngine(int serviceType, // CMDCloudServiceType
									 Activity parentActivity, // Used for user dialogs - can be null if no dialogs are expected
									 Context aContext)
						// TODO: does this need to take a context? for the authentication?
	{
		CMDUtility.setContext(parentActivity);

        mContext = aContext;
		
		mParentActivity = parentActivity;
		
		switch (serviceType)
		{
		case CMDBackupAndRestoreServiceType.CMD_GOOGLE_DRIVE:
			mCloudServiceFileInterface = new CMDCloudServiceGoogleDrive(mParentActivity, aContext);
		case CMDBackupAndRestoreServiceType.CMD_DROPBOX:
		case CMDBackupAndRestoreServiceType.CMD_WINDOWS_LIVE:
		case CMDBackupAndRestoreServiceType.CMD_BOX:
		case CMDBackupAndRestoreServiceType.CMD_PRODUCT_SPECIFIC:
		default:
			// TODO: fail
			break;
		}
	}

	/*
	public void initWithAccessToken(String aAccessToken) {
		mCloudServiceFileInterface.initWithAccessToken(aAccessToken);
	}
	*/

	public void initWithUserName(String aAccessToken, String aUserName) {
		mCloudServiceFileInterface.initWithUserName(aAccessToken, aUserName);
	}

	public String getAccessToken() {
		return mCloudServiceFileInterface.getAccessToken();
	}

	public String getUserName() {
		return mCloudServiceFileInterface.getUserName();
	}
	
	public void startUserLoginAsync(EMProgressHandler progressHandler)
	{
		mCloudServiceFileInterface.startUserLoginAsync(progressHandler);
	}
	
	public void startLogInWithAccountNameAsync(String accountName, EMProgressHandler progressHandler, boolean aIsDestinationDevice)
	{
		// Log.d(TAG, "startLogInWithAccountNameAsync: accountName: " + accountName + " progressHandler: " + progressHandler.toString());
		mCloudServiceFileInterface.startLogInWithAccountNameAsync(accountName, progressHandler, aIsDestinationDevice);
	}
	
	public void startUserAccountCreationAndLoginAsync(EMProgressHandler progressHandler)
	{
		// TODO:
	}
	
	public void userLogoutAsyncAsync(EMProgressHandler progressHandler)
	{
		// TODO:
	}
	
	public void fetchDetailsAsync(EMProgressHandler progressHandler)
	{
		// TODO:
	}
	
	public CMDCloudServiceDetails details() // Must only be called after fetchDetails has completed
	{
		// TODO:
		return new CMDCloudServiceDetails();
	}
	
	public void deleteBackupAsync(String backupName,
									EMProgressHandler progressHandler)
	{
		// TODO:
	}
	
	// Create a backup with the given name and backup the datatypes to it
	public void createBackupWithDataFromDeviceAsync(String backupName,
													int dataTypes, // bitfield of CMDDataType
													EMProgressHandler progressHandler)
	{
		// TODO: the backup name is hard coded further down the stack - we need to change this
		// The helper manages the state machine to accomplish the subtasks - it notifies the progressHandler when it is complete

		CMDFileSystemInterface fileSystemInterface = mCloudServiceFileInterface;
		if (fileSystemInterface == null) {
			fileSystemInterface = new CMDSDCardFileAccess(mContext);
		}

		mCreateBackupWithDataFromDeviceHelper = new CMDCreateBackupAndWriteDataHelper(progressHandler, fileSystemInterface, dataTypes);

		mCreateBackupWithDataFromDeviceHelper.start();
	}

	public CMDFileSystemInterface.CMDFileSystemInfo getDriveInfo() {
		return mCloudServiceFileInterface.getCachedFileSystemInfo();
	}
	
	public void restoreDeviceDataFromBackupAsync(String backupName,
														int dataTypes,
														EMProgressHandler progressHandler,
												 		boolean aForceCloudRestore)
	{
		// TODO: the backup name is hard coded further down the stack - we need to change this
		// The helper manages the state machine to accomplish the subtasks - it notifies the progressHandler when it is complete
		mRestoreBackupToDeviceHelper = new CMDRestoreBackupToDeviceHelper(progressHandler, mCloudServiceFileInterface, dataTypes, mContext, aForceCloudRestore);
        CMDUtility.setContext(mContext);
		mRestoreBackupToDeviceHelper.start();
	}

	public boolean itemExistsBlocking(String aPath, EMProgressHandler aProgressHandler) {
		return mCloudServiceFileInterface.itemExistsBlocking(aPath, aProgressHandler);
	}

	/*
	public class ContentSizeInfo {
		public int mCalendarCount;
		public long mCalendarFileSize;
		public int mContactsCount;
		public long mContactsFileSize;
		public int mPictureCount;
		public long mPictureFileSize;
		public int mVideoCount;
		public long mVideoFileSize;
		public int mMusicCount;
		public long mMusicFileSize;
		public int mDocumentsCount;
		public long mDocumentsFileSize;
	}

	private EMFileFinder mFileFinderTask;
	private ArrayList<EMFileMetaData> mFilesToTransfer;
	private EMGenerateContactsXmlAsyncTask mGenerateContactsTask;
	private EMGenerateCalendarXmlAsyncTask mGenerateCalendarTask;
	private EMProgressHandler handler;
	private int mCompleteCount = 0;

	public void startDriveContentSizeLookupAsync(EMProgressHandler progressHandler)
	{
		EMProgressHandler dummyProgressHandler = new EMProgressHandler() {
			@Override
			public void taskComplete(boolean aSuccess) {
				// Ignore
				mCompleteCount++;
				if(mCompleteCount == 3) {
					handler.taskComplete(aSuccess);
				}
			}

			@Override
			public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
				// Ignore
				handler.taskError(errorCode,alreadyDisplayedDialog);
			}

			@Override
			public void progressUpdate(EMProgressInfo aProgressInfo) {
				// Ignore
			}
		};

		handler = progressHandler;
		mFilesToTransfer = new ArrayList<EMFileMetaData>();
		int dtypes = EMDataType.EM_DATA_TYPE_DOCUMENTS | EMDataType.EM_DATA_TYPE_PHOTOS | EMDataType.EM_DATA_TYPE_DOCUMENTS | EMDataType.EM_DATA_TYPE_VIDEO | EMDataType.EM_DATA_TYPE_MUSIC;
		mFileFinderTask = new EMFileFinder(dtypes, mFilesToTransfer);
		mFileFinderTask.startTask(dummyProgressHandler);

		mGenerateCalendarTask = new EMGenerateCalendarXmlAsyncTask();
		mGenerateCalendarTask.startTask(dummyProgressHandler);

		mGenerateContactsTask = new EMGenerateContactsXmlAsyncTask();
		mGenerateContactsTask.startTask(dummyProgressHandler);
	}

	public ContentSizeInfo getContentFileDataSizeInfo() {
		ContentSizeInfo info = new ContentSizeInfo();
		info.mCalendarCount = mGenerateCalendarTask.getNumberOfEntries();
		File f;
		if(mGenerateCalendarTask.getFilePath() != null) {
			f = new File(mGenerateCalendarTask.getFilePath());
			info.mCalendarFileSize = f.length();
		}

		info.mContactsCount = mGenerateContactsTask.getNumberOfEntries();
		if(mGenerateContactsTask.getFilePath() != null) {
			f = new File(mGenerateContactsTask.getFilePath());
			info.mContactsFileSize = f.length();
		}

		info.mPictureCount = getFileContentCount(EMDataType.EM_DATA_TYPE_PHOTOS);
		info.mPictureFileSize = getFileContentSize(EMDataType.EM_DATA_TYPE_PHOTOS);

		info.mVideoCount = getFileContentCount(EMDataType.EM_DATA_TYPE_VIDEO);
		info.mVideoFileSize = getFileContentSize(EMDataType.EM_DATA_TYPE_VIDEO);

		info.mMusicCount = getFileContentCount(EMDataType.EM_DATA_TYPE_MUSIC);
		info.mMusicFileSize = getFileContentSize(EMDataType.EM_DATA_TYPE_MUSIC);

		info.mDocumentsCount = getFileContentCount(EMDataType.EM_DATA_TYPE_DOCUMENTS);
		info.mDocumentsFileSize = getFileContentSize(EMDataType.EM_DATA_TYPE_DOCUMENTS);
		return info;
	}

	private int getFileContentCount(int dataType) {
		int count = 0;
		for(EMFileMetaData mData : mFilesToTransfer){
			if (mData.mDataType == dataType) {
				count++;
			}
		}
		return count;
	}

	private int getFileContentSize(int dataType) {
		int size = 0;
		for(EMFileMetaData mData : mFilesToTransfer){
			if (mData.mDataType == dataType) {
				size+=mData.mSize;
			}
		}
		return size;
	}
	*/

	private CMDCloudServiceFileInterface mCloudServiceFileInterface;
	private Activity mParentActivity;
	private CMDCreateBackupAndWriteDataHelper mCreateBackupWithDataFromDeviceHelper;
    private Context mContext;
    private String mTempLocalPath;
	CMDRestoreBackupToDeviceHelper mRestoreBackupToDeviceHelper;
}
