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

import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.custom.models.MigrationStats;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.DeviceInfo;

import java.net.InetAddress;
import java.util.Arrays;

public class EMMediaSenderNew implements EMCommandDelegate, EMCommandHandler {

	EMMediaSenderNew(EMDataCommandDelegate aDataCommandDelegate, int aDataType) {
		mDataCommandDelegate = aDataCommandDelegate;
		mDataType = aDataType;
	}

	private EMDataCommandDelegate mDataCommandDelegate;
	private int mDataType;
	boolean mCancelled = false;
	private int timeOutRetry = 0;
	private final int RETRY = 1;

	private boolean isMigrationResumed = false;
	public InetAddress mHostName = null;
	EMDataTransferHelper mediaTransferHelper = null;
	private EMFileMetaData fileMetaData;

	public void start(EMCommandDelegate aDelegate)
	{
		mCancelled = false;
		mDelegate = aDelegate;
		isMigrationResumed = Constants.IS_MIGRATION_RESUMED;
		if (!mCancelled) {
			sendNextFile();
		} else {
			mDelegate.commandComplete(true);
		}
	}

	private EMCommandDelegate mDelegate;

	@Override
	public void sendText(String text) {
		if (!mCancelled)
			mDelegate.sendText(text);
	}

	@Override
	public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {
		if (!mCancelled)
			mDelegate.sendFile(aFilePath, aDeleteFileWhenDone, aFileSendingProgressDelegate);
	}

	@Override
	public void getText() {
		if (!mCancelled)
			mDelegate.getText();
	}

	@Override
	public void getXmlAsFile() {
		if (!mCancelled)
			mDelegate.getXmlAsFile();
	}

	@Override
	public void commandComplete(boolean aSuccess) {
		if (mCancelled) {
			return;
		}

		if (aSuccess) {
			timeOutRetry = 0;
			EMMigrateStatus.addItemTransferred(fileMetaData.mDataType);
			EMMigrateStatus.addTransferedFilesSize(fileMetaData.mDataType,fileMetaData.mSize);
			mDelegate.addToPreviouslyTransferredItems(fileMetaData.mSourceFilePath);
			sendNextFile();
		}
		else {
			boolean canBeRecovered = false;
			if (EMConfig.NEW_DATA_TRANSFER_PROTOCOL) {
				int errorCode = mediaTransferHelper.getErrorCode();
				if (errorCode == EMDataTransferHelper.ERROR_CODE_UNSUPPORTED || errorCode == EMDataTransferHelper.ERROR_CODE_FILE_NOTFOUND || errorCode == EMDataTransferHelper.ERROR_CODE_READ_FAILED) {
					EMMigrateStatus.addItemNotTransferred(fileMetaData.mDataType);
					EMMigrateStatus.addTransferredFailedSize(fileMetaData.mDataType, fileMetaData.mSize);
					sendNextFile();
					return;
				} else if (errorCode == EMDataTransferHelper.ERROR_CODE_TIMEOUT) {
					canBeRecovered = true;
					timeOutRetry++;
				} else {
					canBeRecovered = true;            //TODO: Need to handle other error codes as well.
					timeOutRetry++;
				}
			}
			DLog.log("Media file sending failed : " + timeOutRetry);
			if (canBeRecovered && timeOutRetry < 3) {
				mHandler.sendEmptyMessageDelayed(RETRY, 2 * 1000L);
			} else if (timeOutRetry == 3 && !isRemoteDeviceIOS()) {
				timeOutRetry = 0;
				sendFile();
			} else {
				mDelegate.commandComplete(false);
			}
		}
	}

	// From Stack Overflow
	private static final int[] illegalChars = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};
	static {
		Arrays.sort(illegalChars);
	}

	// From Stack Overflow
	public static String cleanFileName(String badFileName) {
		StringBuilder cleanName = new StringBuilder();
		for (int i = 0; i < badFileName.length(); i++) {
			int c = (int)badFileName.charAt(i);
			if (Arrays.binarySearch(illegalChars, c) < 0) {
				cleanName.append((char)c);
			}
		}
		return cleanName.toString();
	}

	private void sendFile() {
		if (mCancelled)
			return;

		boolean go = true;
		while (go) {
			try {
				go = false;
				if (mediaTransferHelper == null) {
					mediaTransferHelper = new EMDataTransferHelper(mHostName);
					mediaTransferHelper.setCommandDelegate(this);
					mediaTransferHelper.init();
				}
				mediaTransferHelper.sendFile(fileMetaData);

			} catch (Exception ex) {
				DLog.log(ex);
			}
		}
	}

	private void sendNextFile() {
		if (mCancelled || CommonUtil.getInstance().isMigrationInterrupted())
			return;
		if (mDataType == EMDataType.EM_DATA_TYPE_DOCUMENTS) {
			fileMetaData = DeviceInfo.getInstance().getFileMetaData();
		} else {
			fileMetaData = DeviceInfo.getInstance().getMetaData();
		}
		if (fileMetaData == null) {
			if (mediaTransferHelper != null) {
				mediaTransferHelper.clean();
			}
			mDelegate.commandComplete(true);
		} else if (isMigrationResumed && itemHasBeenPreviouslyTransferred(fileMetaData.mSourceFilePath)) {
			DLog.log("Skipping file "+fileMetaData.mSourceFilePath);
			sendNextFile();
		}else {
			EMMigrateStatus.addItemTransferStarted(fileMetaData.mDataType);
			sendFile();
		}
	}


	private EMAddFileCommandInitiator mAddFileCommandInitiator;

	@Override
	public Object getSharedObject(String aObject) {
		// Ignore
		return null;
	}

	@Override
	public void startNoopTimer() {
		//Ignore
	}

	@Override
	public void stopNoopTimer() {
		//Ignore
	}

	@Override
	public void setSharedObject(String aKey, Object aObject) {
		// Ignore
	}

	// Starts listening for raw data and saves it into a file
	// The command is notified when the data is ready (when we have read [length] bytes)
	@Override
	public void getRawDataAsFile(long aLength, String aTargetFilePath) {
		if (!mCancelled)
			mDelegate.getRawDataAsFile(aLength, aTargetFilePath);
	}

	@Override
	public boolean handlesCommand(String aCommand) {
		return false;
	}

	@Override
	public boolean gotText(String aText) {
		if (mCancelled)
			return false;
		if (mAddFileCommandInitiator != null)
			return mAddFileCommandInitiator.gotText(aText);
		return false;
	}

	@Override
	public boolean gotFile(String aDataPath) {
		if (mCancelled)
			return false;

		if (mAddFileCommandInitiator != null)
			return mAddFileCommandInitiator.gotFile(aDataPath);
		return false;
	}

	@Override
	public void sent() {
		if (mCancelled)
			return;

		if (mAddFileCommandInitiator != null)
			mAddFileCommandInitiator.sent();
	}

	@Override
	public void cancel() {
		mCancelled = true;
		if (mAddFileCommandInitiator != null)
			mAddFileCommandInitiator.cancel();
	}

	@Override
	public void disableTimeoutTimer() {
		// Ignore
	}

	@Override
	public void enableTimeoutTimer() {
		// Ignore
	}

	@Override
	public void addToPreviouslyTransferredItems(String aItem) {
		mDelegate.addToPreviouslyTransferredItems(aItem);
	}

	@Override
	public boolean itemHasBeenPreviouslyTransferred(String aItem) {
		return mDelegate.itemHasBeenPreviouslyTransferred(aItem);
	}

	private boolean isRemoteDeviceIOS() {
		boolean isRemoteDeviceIOS = false;
		String remoteDevicePlatform = null;
		try {
			remoteDevicePlatform = DashboardLog.getInstance().destinationEMDeviceInfo.dbDevicePlatform;
		} catch (Exception e) {
			DLog.log(e.getMessage());
		}
		if (Constants.PLATFORM_IOS.equalsIgnoreCase(remoteDevicePlatform)) {
			isRemoteDeviceIOS = true;
		}
		return isRemoteDeviceIOS;
	}

	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(msg.what == RETRY){
				DLog.log("sending file - retrying");
				fileMetaData.retry = true; // Adds retry when resending the file- To avoid count mismatch
				sendFile();
			}
		}
	};
}