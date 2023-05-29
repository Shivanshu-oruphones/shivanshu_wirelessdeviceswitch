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

import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMSimpleAsyncTask;
import com.pervacio.wds.sdk.CMDError;
import com.pervacio.wds.sdk.internal.CMDFileSystemInterface.CMDRemoteFileInfo;

public class CMDGoogleDriveUploadFileAsyncTask extends EMSimpleAsyncTask implements EMProgressHandler {

	// TODO: this is temporarily static because we want to re-use the DriveId for the backup folder because we don't currently support all the file path lookup stuff
	private static CMDGoogleUtility googleUtility = new CMDGoogleUtility();
	
	public CMDGoogleDriveUploadFileAsyncTask(String sourcePath, CMDRemoteFileInfo remoteFileInfo,
			CMDGoogleDriveAccess googleDriveAccess, int totalItems) {
		mSourceFilePath = sourcePath;
		mRemoteFileInfo = remoteFileInfo;
		mGoogleDriveAccess = googleDriveAccess;
		mTotalItems = totalItems;
	}
	
	@Override
	public void runTask() {
        EMProgressInfo progress = new EMProgressInfo();
        progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_SENDING_DATA;
        progress.mDataType = mRemoteFileInfo.mDataType;
        progress.mTotalItems = 0;
        progress.mCurrentItemNumber = 0;
        updateProgressFromWorkerThread(progress);
        
        String filename = null;
		
		String[] remotePathComponents = mRemoteFileInfo.mFilePath.split("/");
		
		if (remotePathComponents.length < 2) {
        	setFailed(CMDError.CMD_ERROR_CREATING_REMOTE_PATH); // We must have at least a backup folder and a file name
            return;
		}
		else {
			filename = remotePathComponents[remotePathComponents.length - 1];
		}

		String backupFolderId = googleUtility.makePath("/" + remotePathComponents[remotePathComponents.length - 2], mGoogleDriveAccess, this);

		if (backupFolderId == null) {
			setFailed(CMDError.CMD_ERROR_CREATING_REMOTE_PATH);
			return;
		}

		int result = mGoogleDriveAccess.copyFileFromLocal(backupFolderId, mSourceFilePath, filename, this, null);

		if (result == CMDError.CMD_GOOGLE_DRIVE_FULL_ERROR) {
			setFailed(CMDError.CMD_GOOGLE_DRIVE_FULL_ERROR);
			return;
		}
		if (result != CMDError.CMD_RESULT_OK) {
			setFailed(CMDError.FAILED_TO_CREATE_FILE_ON_CLOUD_SERVICE);
			return;
		}

		progress = new EMProgressInfo();
		progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_SENDING_DATA;
		progress.mDataType = mRemoteFileInfo.mDataType;
		progress.mTotalItems = mTotalItems;
		progress.mCurrentItemNumber = mTotalItems;
		updateProgressFromWorkerThread(progress);
	}
	
	private String mSourceFilePath;
	private CMDRemoteFileInfo mRemoteFileInfo;
	private CMDGoogleDriveAccess mGoogleDriveAccess;
	private int mTotalItems = 0;

	@Override
	public void taskComplete(boolean aSuccess) {
		// Not called
	}

	@Override
	public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
		// Not called
	}

	@Override
	public void progressUpdate(EMProgressInfo aProgressInfo) {
		// Used to update the caller paused / resumed connection status
		updateProgressFromWorkerThread(aProgressInfo);
	}
}
