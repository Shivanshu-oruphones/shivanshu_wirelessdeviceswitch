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

import android.util.Log;

import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMSimpleAsyncTask;
import com.pervacio.wds.sdk.CMDError;
import com.pervacio.wds.sdk.internal.CMDFileSystemInterface.CMDRemoteFileInfo;

import java.io.File;
import java.util.Date;

public class CMDGoogleDriveDownloadFileAsyncTask extends EMSimpleAsyncTask implements EMProgressHandler {

	private static final String TAG = "DeviceSwitchV2";

	private int mDataType;
	private int mCurrentItemNumber;
	private int mTotalItems;

	public CMDGoogleDriveDownloadFileAsyncTask(String localDestinationPath, CMDRemoteFileInfo remoteFileInfo,
													CMDGoogleDriveAccess googleDriveAccess) {
		mLocalDestinationFilePath = localDestinationPath;
		mRemoteFileInfo = remoteFileInfo;
		mGoogleDriveAccess = googleDriveAccess;
	}
	
	@Override
	public void runTask() {
		// Log.d(TAG, ">> CMDGoogleDriveDownloadFileAsyncTask::runTask");

        CMDGoogleUtility googleUtility = new CMDGoogleUtility();

        // Synchronously find the DriveId of the file we want to download
        String remoteItemDriveId = null;
		CMDGoogleDriveAccess.CMDGoogleDriveItem googleDriveItem = googleUtility.getDriveItemForPathBlocking(mRemoteFileInfo.mFilePath, mGoogleDriveAccess, this);
		if (googleDriveItem != null) {
			remoteItemDriveId = googleDriveItem.mGoogleDriveId;
		}

		if (remoteItemDriveId == null) {
        	setFailed(CMDError.CMD_ERROR_FINDING_REMOTE_FILE);
        }
		else {
			// Log.d(TAG, "About to copy to local: CMDGoogleDriveDownloadFileAsyncTask.mLocalDestinationFilePath" + mLocalDestinationFilePath);
			int result = mGoogleDriveAccess.copyFileToLocal(remoteItemDriveId,
																mLocalDestinationFilePath,
																this, null);

			Date modifiedDateInLocalTimezone = googleDriveItem.mModifiedDate;
			File locallyCopiedFile = new File(mLocalDestinationFilePath);
			long timestamp = modifiedDateInLocalTimezone.getTime();
			locallyCopiedFile.setLastModified(timestamp);

			if (result != CMDError.CMD_RESULT_OK) {
				// Log.d(TAG, "Setting failed: " + result);
				setFailed(result);
			}
		}

		// Log.d(TAG, "<< CMDGoogleDriveDownloadFileAsyncTask::runTask");
	}
	
	private String mLocalDestinationFilePath;
	private CMDRemoteFileInfo mRemoteFileInfo;
	private CMDGoogleDriveAccess mGoogleDriveAccess;

	@Override
	public void taskComplete(boolean aSuccess) {

	}

	@Override
	public void taskError(int errorCode, boolean alreadyDisplayedDialog) {

	}

	@Override
	public void progressUpdate(EMProgressInfo aProgressInfo) {
		updateProgressFromWorkerThread(aProgressInfo);
	}
}
