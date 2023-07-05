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

import java.io.File;
import java.net.InetAddress;
import java.util.Arrays;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.sdk.internal.sdcard.CMDSDCardFileAccess;

import static com.pervacio.wds.custom.utils.Constants.EXCLUDE_SDCARD_MEDIA;
import static com.pervacio.wds.custom.utils.Constants.EXCLUDE_WHATSAPP_MEDIA;

public class EMMediaSender implements EMCommandDelegate, EMCommandHandler {

	private static final String TAG = "EMMediaSender";
	static EMGlobals emGlobals = new EMGlobals();
Context context;

	EMMediaSender(EMDataCommandDelegate aDataCommandDelegate, int aDataType, Context mContext) {
		mDataCommandDelegate = aDataCommandDelegate;
		mDataType = aDataType;
		context = mContext;
	}

	private Cursor mCursor;
	private int mFilePathColumn;
	// private int mTitleColumn;
	private int mAlbumColumn = -1;
	private int mArtistColumn = -1;
	// private int mDateColumn;
	private EMDataCommandDelegate mDataCommandDelegate;
	private int mDataType;
	int mCurrentFileNumber = 0;
	long mTotalMediaSize   = 0;
	long mTotalMediaCount   = 0;
	long mTotalBytesSent = 0;
	boolean mCancelled = false;
	private String mFilePath = null;
	private int timeOutRetry = 0;
	ProgressBar mProgressBar;
	ProgressDialog mProgressDialog;
	int PROGRESS_BAR_MAX = 60;
	public InetAddress mHostName = null;
	EMDataTransferHelper mediaTransferHelper = null;
	private String mSdCardPath = null;


	public void start(EMCommandDelegate aDelegate)
	{


		mCancelled = false;
		mDelegate = aDelegate;
		mTotalBytesSent = 0;
		// TODO: start indexing the photos
		// Get and write the list of images
		Uri uri = null;
		if (mDataType == EMDataType.EM_DATA_TYPE_PHOTOS)
			uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		else if (mDataType == EMDataType.EM_DATA_TYPE_VIDEO)
			uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
		else if (mDataType == EMDataType.EM_DATA_TYPE_MUSIC)
			uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

		try {
			mCursor = EMUtility.Context().getContentResolver().query(uri,
					null,		// projection : TODO: optimize this
					null,       // where - get all rows
					null,       // arguments - none
					null        // orderng - doesn't matter
			);

			if (mDataType == EMDataType.EM_DATA_TYPE_PHOTOS)
				mFilePathColumn = mCursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
			else if (mDataType == EMDataType.EM_DATA_TYPE_VIDEO)
				mFilePathColumn = mCursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA);
			else if (mDataType == EMDataType.EM_DATA_TYPE_MUSIC)
			{
				mFilePathColumn = mCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
//				mFilePathColumn = mCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
//				mArtistColumn = mCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
//				mAlbumColumn = mCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM);
			}
			//mTotalMediaSize = getTotalMediaSize();
			mSdCardPath = CMDSDCardFileAccess.getSDCardpath(emGlobals.getmContext());
			mTotalMediaSize = getTotalMediaSize( mCursor, mFilePathColumn, EXCLUDE_WHATSAPP_MEDIA, EXCLUDE_SDCARD_MEDIA, mSdCardPath, false);
			mTotalMediaCount = getTotalMediaSize( mCursor, mFilePathColumn, EXCLUDE_WHATSAPP_MEDIA, EXCLUDE_SDCARD_MEDIA, mSdCardPath, true);

		} catch (Exception ex) {
			ex.printStackTrace();
			EMMigrateStatus.setTotalFailure(mDataType);
		}

		// Log.d(TAG, "=== start, Total Dataset Size: " +mTotalMediaSize);

		if (!mCancelled) {
			if ((mCursor != null) && (mCursor.moveToFirst()) && mTotalMediaCount != 0) {


				sendFile();
			}
			else
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

//	@Override
//	public void getRawDataAsFile(long aLength) {
//		//mDelegate.getRawDataAsFile(aLength);
//	}

	@Override
	public void commandComplete(boolean aSuccess) {
		if (mCancelled) {
			mCursor.close();
			return;
		}

		if (aSuccess) {
			mDelegate.addToPreviouslyTransferredItems(mFilePath);
			sendNextFile();
		}
		else
		{
			boolean canBeRecovered = false;
			if(EMConfig.NEW_DATA_TRANSFER_PROTOCOL) {
				int errorCode = mediaTransferHelper.getErrorCode();
				if(errorCode == EMDataTransferHelper.ERROR_CODE_TIMEOUT) {
					canBeRecovered = true;
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					timeOutRetry++;
				}
			}
			if(canBeRecovered && timeOutRetry<3) {
				sendNextFile();
			} else {
				mCursor.close();
				mDelegate.commandComplete(false);
			}
		}
	}

	// From Stack Overflow
	final static int[] illegalChars = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};
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

				String filePath = mCursor.getString(mFilePathColumn);
				File file = new File(filePath);
				if ((EXCLUDE_WHATSAPP_MEDIA && (filePath.toLowerCase().contains("whatsapp"))) || (EXCLUDE_SDCARD_MEDIA && (mSdCardPath!=null) && filePath.contains(mSdCardPath)))
				{
/*					if(EXCLUDE_WHATSAPP_MEDIA && (filePath.toLowerCase().contains("whatsapp")))
						DLog.log("excludeWhatsAppMedia case " + "excluding whatsapp media filepath "+filePath);
					if(EXCLUDE_SDCARD_MEDIA && (mSdCardPath!=null) && filePath.contains(mSdCardPath))
						DLog.log("excludesdCardMedia case " + "excluding sdcard media filepath "+filePath);*/
				}
				else if (!file.exists()) {
					// Do nothing, we will continue to the next file
					DLog.log("Skipping not-found file: " + filePath);
				}
				else  if (mDelegate.itemHasBeenPreviouslyTransferred(filePath)) {
					// Do nothing, we will continue to the next file
					DLog.log("Skipping previously send file: " + filePath);
				}
				else {
					go = false;
					final EMFileMetaData metaData = new EMFileMetaData();
					metaData.mSourceFilePath = filePath;
					metaData.mSize = file.length();
					metaData.mTotalMediaSize = mTotalMediaSize;
					metaData.mFileName = file.getName();
					metaData.mTotalFiles = (int) mTotalMediaCount; //mCursor.getCount();
					metaData.mDataType = mDataType;
					metaData.mCurrentFileNumber = ++mCurrentFileNumber;
					metaData.mRelativePath = "";

//					if (mArtistColumn != -1) {
//						String artistString = mCursor.getString(mArtistColumn);
//						if (artistString != null) {
//							if (!artistString.equalsIgnoreCase("")) {
//								artistString = cleanFileName(artistString);
//								metaData.mRelativePath += "/" + artistString;
//							}
//						}
//					}
//
//					if (mAlbumColumn != -1) {
//						String albumString = mCursor.getString(mAlbumColumn);
//						if (albumString != null) {
//							if (!albumString.equalsIgnoreCase("")) {
//								albumString = cleanFileName(albumString);
//								metaData.mRelativePath += "/" + albumString;
//							}
//						}
//					}

					EMProgressInfo progressInfo = new EMProgressInfo();
					progressInfo.mDataType = mDataType;
					progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_SENDING_DATA;
					progressInfo.mTotalItems = (int) mTotalMediaCount;//mCursor.getCount();
					progressInfo.mCurrentItemNumber = mCurrentFileNumber;
					progressInfo.mFileSize = metaData.mSize;
					progressInfo.mTotalMediaSize = metaData.mTotalMediaSize;
					progressInfo.mProgressPercent = 0;
					mDataCommandDelegate.progressUpdate(progressInfo);

					mFilePath = filePath;

					if(EMConfig.NEW_DATA_TRANSFER_PROTOCOL) {
						if(mediaTransferHelper == null) {
							mediaTransferHelper = new EMDataTransferHelper(mHostName);
							mediaTransferHelper.setCommandDelegate(this);
							mediaTransferHelper.init();
						}
						mediaTransferHelper.sendFile(metaData);
					} else {

						mAddFileCommandInitiator = new EMAddFileCommandInitiator(metaData, new EMFileSendingProgressDelegate() {
							@Override
							public void fileSendingProgressUpdate(long mFileBytesSent) {
								mTotalBytesSent += mFileBytesSent;
								float percentCompleteFloat = ((((float) mTotalBytesSent) / ((float) metaData.mTotalMediaSize)) * ((float) 100));
								int percentComplete = (int) percentCompleteFloat;
								if (percentComplete != mPreviousPercentComplete) {
									mPreviousPercentComplete = percentComplete;
									EMProgressInfo progressInfo = new EMProgressInfo();
									progressInfo.mDataType = mDataType;
									progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_SENDING_DATA;
									progressInfo.mTotalItems = (int) mTotalMediaCount;//mCursor.getCount();
									progressInfo.mCurrentItemNumber = mCurrentFileNumber;
									progressInfo.mFileSize = metaData.mSize;
									progressInfo.mTotalMediaSize = metaData.mTotalMediaSize;
									progressInfo.mProgressPercent = (int) percentCompleteFloat;
									mDataCommandDelegate.progressUpdate(progressInfo);
								}
							}
						});
						//		    	mAddFileCommandInitiator.setDataCommandDelegate(mDataCommandDelegate);
						//mFilePath = filePath;
						mAddFileCommandInitiator.start(this);
					}

				}
			} catch (Exception ex) {
				ex.printStackTrace();
				DLog.log(ex);
			}

			if (go) {
				// The file wasn't sent, maybe it doesn't exist or has been sent before
				// So move to the next one
				if (!mCursor.moveToNext()) {
					go = false;
					mCursor.close();
					mDelegate.commandComplete(true);
				}
				else {
					mCurrentFileNumber++;
				}
			}
		}
	}

	int mPreviousPercentComplete = 0;

	// Sends the next photo, or completes our delegate if there are no more photos
	private void sendNextFile() {
		if (mCancelled)
			return;

		if (!mCursor.moveToNext()) {
			mCursor.close();
			//TODO
			if(EMConfig.NEW_DATA_TRANSFER_PROTOCOL) {
				if(mediaTransferHelper != null) {
					mediaTransferHelper.clean();
				}
			}
			mDelegate.commandComplete(true);
		}
		else {
			sendFile();
		}
	}

	private long getTotalMediaSize()
	{
		if (! mCursor.moveToFirst())
		{
			// Log.d(TAG, "=== getTotalMediaSize, Could not move to first item");
			return 0;
		}

		long totalSize = 0;

		do
		{
			if (mCancelled)
				return 0;

			String filePath = mCursor.getString(mFilePathColumn);
			File file = new File(filePath);

			if (file.exists())
			{
				totalSize += file.length();
			}

		} while(mCursor.moveToNext());

		return totalSize;
	}

	private long getTotalMediaSize(Cursor mCursor, int mFilePathColumn, boolean excludeWhatsAppMedia, boolean excludeSDCardMedia, String sdCardPath, boolean aCountOnly)
	{
		long fileCount = 0;
		if (! mCursor.moveToFirst()||mFilePathColumn==-1)
		{
			// Log.d(TAG, "=== getTotalMediaSize, Could not move to first item");
			return 0;
		}
		long totalSize = 0;
		do
		{
			String filePath = mCursor.getString(mFilePathColumn);
			File file = new File(filePath);
			if ((excludeWhatsAppMedia && (filePath.toLowerCase().contains("whatsapp"))) || (excludeSDCardMedia && (sdCardPath!=null) && filePath.contains(sdCardPath)))
			{
/*				if(excludeWhatsAppMedia && (filePath.toLowerCase().contains("whatsapp")))
					DLog.log("excludeWhatsAppMedia case " + "excluding whatsapp media filepath "+filePath);
				if(excludeSDCardMedia && (sdCardPath!=null) && filePath.contains(sdCardPath))
					DLog.log("excludesdCardMedia case " + "excluding sdcard media filepath "+filePath);*/
			}else {
				//DLog.log("excludesdCardMedia or excludeWhatsAppMedia  else case" + " filepath "+filePath);

				if (file.exists()) {
					totalSize += file.length();
					fileCount=fileCount+1;
				}
			}
		} while(mCursor.moveToNext());
		if (aCountOnly){
			return fileCount;
		}else{
			return totalSize;
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

	}

	@Override
	public void stopNoopTimer() {

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
		// TODO: check the initiator is valid
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

		// TODO: check the initiator is valid
		if (mAddFileCommandInitiator != null)
			return mAddFileCommandInitiator.gotFile(aDataPath);
		return false;
	}

	@Override
	public void sent() {
		if (mCancelled)
			return;

		// TODO: check the initiator is valid
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
}