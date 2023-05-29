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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import com.pervacio.wds.custom.appmigration.AppMigrateUtils;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.sdk.internal.CMDCryptoSettings;

public class EMAddFileCommandResponder implements EMCommandHandler {

	private static final String TAG = "EMAddFileCommandResp";

	private enum EMAddFileState
	{
	    EM_SENDING_INITIAL_OK,
	    EM_WAITING_FOR_ADD_FILE_XML,
	    EM_SENDING_XML_OK,
	    EM_WAITING_FOR_RAW_FILE_DATA,
	    EM_SENDING_FINAL_OK,
		EM_CANCELLED
	};
	
	class FileInfo {
		int mFileType        = EMDataType.EM_DATA_TYPE_NOT_SET;
		String mFileName     = "unknown";
		String mRelativePath = "";
		long mFileSize       = -1;
		long mTotalMediaSize = -1;
	}
	
	EMAddFileCommandResponder(Context aContext) {
		mContext = aContext;
	}
	
	// Set the delegate to receive notifications about data sending progress
	public void setDataCommandDelegate(EMDataCommandDelegate aDataCommandDelegate)
	{
	    mDataCommandDelegate = aDataCommandDelegate;
	}
	
	@Override
	public void start(EMCommandDelegate aDelegate) {
		EMMigrateStatus.setStartTransfer(); // It's okay to call this multiple times

		mCommandDelegate = aDelegate;

		if (!EMConfig.ALLOW_UNENCRYPTED_WIFI_TRANSFER) {
			if (!CMDCryptoSettings.encryptionVerified()) {
				mState = EMAddFileState.EM_SENDING_FINAL_OK;
				mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_ERROR);
				return;
			}
		}

	    mState = EMAddFileState.EM_SENDING_INITIAL_OK;
	    mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
	}

	@Override
	public boolean handlesCommand(String aCommand) {
		if (aCommand.startsWith(EMStringConsts.EM_COMMAND_TEXT_ADD_FILE + " ")) {
			String[] commandAndParameters = aCommand.split(" ");

			if (commandAndParameters.length > 1) {
				try {
					mMetaDataSize = Long.parseLong(commandAndParameters[1]);
					return true;
				}
				catch (Exception ex) {
					// Ignore, just return false
				}
			}
		}

		return false;
	}

	@Override
	public boolean gotText(String aText) {
		// TODO not expecting any test: should we raise an exception?
		return true;
	}

	@Override
	public boolean gotFile(String aDataPath) {
		if (mState == EMAddFileState.EM_WAITING_FOR_ADD_FILE_XML) {
			// Parse the XML file
			mFileInfo = parseFileInfo(aDataPath);
			mState = EMAddFileState.EM_SENDING_XML_OK;
			mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
		} else if (mState == EMAddFileState.EM_WAITING_FOR_RAW_FILE_DATA) {
			if (aDataPath == null) {
				EMMigrateStatus.addItemNotTransferred(mFileInfo.mFileType);
			}
			else {
				String[] paths = {aDataPath};
				MediaScannerConnection.scanFile(mContext, paths, null, null);
				EMMigrateStatus.addItemTransferred(mFileInfo.mFileType);
				EMMigrateStatus.addBytesTransferred(mFileInfo.mFileSize);
			}

			mState = EMAddFileState.EM_SENDING_FINAL_OK;
			mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
		}

		return true;
	}

	@Override
	public void sent() {
		if (mState == EMAddFileState.EM_SENDING_INITIAL_OK) {
			mState = EMAddFileState.EM_WAITING_FOR_ADD_FILE_XML;
			String tempFilePath = EMUtility.temporaryFileName();
			mCommandDelegate.getRawDataAsFile(mMetaDataSize, tempFilePath);
		}
		else if (mState == EMAddFileState.EM_SENDING_XML_OK) {
			// Create the file to read in to, then get the raw file
			mState = EMAddFileState.EM_WAITING_FOR_RAW_FILE_DATA;
			String filePath = getTargetFilePath();

			// TODO: temp code
			File tempFile = new File(filePath);
			if (tempFile.exists()) {
				DLog.log("*** The file already exists: " + tempFile);
			}

			mCommandDelegate.getRawDataAsFile(mFileInfo.mFileSize, filePath);
		}
		else if (mState == EMAddFileState.EM_SENDING_FINAL_OK) {
			// Complete the delegate
			mCommandDelegate.commandComplete(true);
		}
	}
	
	private String getTargetFilePath() {
		String filePath = null; // The path of the file to be created on the file system
		
		// Determine the target file name
		filePath = Environment.getExternalStorageDirectory().toString() + "/";
		if (mFileInfo.mFileType == EMDataType.EM_DATA_TYPE_PHOTOS) {
			filePath += Environment.DIRECTORY_PICTURES + "/" + mFileInfo.mFileName;
		/*
		} else if (aFileInfo.fileType.equalsIgnoreCase(XmlConsts.MEDIA_MUSIC_ELEMENT)) {
			filePath += Environment.DIRECTORY_MUSIC + "/" + mFileInfo.fileName;
		*/
		} else if (mFileInfo.mFileType == EMDataType.EM_DATA_TYPE_VIDEO) {
			filePath += Environment.DIRECTORY_MOVIES + "/" + mFileInfo.mFileName;
		}
		else if (mFileInfo.mFileType == EMDataType.EM_DATA_TYPE_MUSIC) {
			filePath += Environment.DIRECTORY_MUSIC + "/" + mFileInfo.mFileName;
		}
        else if (mFileInfo.mFileType == EMDataType.EM_DATA_TYPE_APP) {
            filePath += Environment.getExternalStorageDirectory() + "/" + Constants.APP_MIGRATION_DIRECTORY + "/" + mFileInfo.mFileName;

            //In case of app migration, we need to keep track of each restored apk so that we can
			//install them at the end of migration
			AppMigrateUtils.addRestoreApp(filePath);
        }
        else {
			filePath += Environment.getExternalStorageDirectory() + "/" + Constants.DOCUMENTS_MIGRATION_DIRECTORY + "/" + mFileInfo.mFileName;
		}
		
		if (filePath != null) {
			File file = new File(filePath);
			file.getParentFile().mkdirs();
		}
		
		// TODO: check that the filename doesn't already exist (if it does then add a number before the extension to prevent it being overwritten)
		
		return filePath;
	}
	
	private FileInfo parseFileInfo(String aFilePath) {
		// TODO: parsing this in the main thread - not great, but it should be very fast as we already have the file and it's only a few lines long
		
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(aFilePath);
		}
		catch (Exception ex) {
			return null;
		}
		
		FileInfo fileInfo = null;
		
		// Initialize the parser
		XmlPullParser xmlParser = Xml.newPullParser();
		
		try {
			xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			xmlParser.setInput(new InputStreamReader(inputStream));
			xmlParser.nextTag();
			
			int xmlLevel = 1;
			
			String text = "";
						
			// Parse XML file - currently very, very basic
			while (xmlParser.next() != XmlPullParser.END_DOCUMENT) {
				
				String tagName = xmlParser.getName();
				
				if (xmlParser.getEventType() == XmlPullParser.START_TAG) {
					
					if (xmlLevel == 1) {
						fileInfo = new FileInfo();
					}
					
					xmlLevel++;
				}
	
				if (xmlParser.getEventType() == XmlPullParser.TEXT) {
					text = xmlParser.getText();
				}
	
				if (xmlParser.getEventType() == XmlPullParser.END_TAG) {
					xmlLevel--;
					
					if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_ROOT)) {
						break;
					}
					
					if (xmlLevel == 2) {
						if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_FILE_SIZE)) {
							fileInfo.mFileSize = Long.valueOf(text);
						} else if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_FILE_NAME)) {
							fileInfo.mFileName = text;
						}
						else if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_FILE_TOTAL_MEDIA_SIZE))
						{
							fileInfo.mTotalMediaSize = Long.valueOf(text);
							// Log.d(TAG, "parseFileInfo, Total Dataset Size: " +fileInfo.mTotalMediaSize);
						}
						else if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_FILE_TYPE)) {
							if (text.equalsIgnoreCase(EMStringConsts.EM_XML_PHOTOS)) {
								fileInfo.mFileType = EMDataType.EM_DATA_TYPE_PHOTOS;
							}
							else if (text.equalsIgnoreCase(EMStringConsts.EM_XML_VIDEO)) {
								fileInfo.mFileType = EMDataType.EM_DATA_TYPE_VIDEO;
							}
							else if (text.equalsIgnoreCase(EMStringConsts.EM_XML_DOCUMENTS)) {
								fileInfo.mFileType = EMDataType.EM_DATA_TYPE_DOCUMENTS;
							}
							else if (text.equalsIgnoreCase(EMStringConsts.EM_XML_MUSIC)) {
								fileInfo.mFileType = EMDataType.EM_DATA_TYPE_MUSIC;
							}
							else if (text.equalsIgnoreCase(EMStringConsts.EM_XML_APP)) {
								fileInfo.mFileType = EMDataType.EM_DATA_TYPE_APP;
							}
							
							if (fileInfo.mFileType != EMDataType.EM_DATA_TYPE_NOT_SET) {
								EMProgressInfo progressInfo = new EMProgressInfo();
								progressInfo.mDataType       = fileInfo.mFileType;
								progressInfo.mOperationType  = EMProgressInfo.EMOperationType.EM_OPERATION_RECEIVING_DATA;
								progressInfo.mFileSize       = fileInfo.mFileSize;
								progressInfo.mTotalMediaSize = fileInfo.mTotalMediaSize;
								mDataCommandDelegate.progressUpdate(progressInfo);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			DLog.log(ex);
		}
		
		try {
			inputStream.close();
		} catch (Exception e) {
			// Ignore
		}
		
		return fileInfo;
	}

	@Override
	public void cancel() {
		mState = EMAddFileState.EM_CANCELLED;
	}

	private EMCommandDelegate mCommandDelegate;
	private EMAddFileState mState;
	private FileInfo mFileInfo;
	private Context mContext;
	private EMDataCommandDelegate mDataCommandDelegate;

	Long mMetaDataSize;
}
