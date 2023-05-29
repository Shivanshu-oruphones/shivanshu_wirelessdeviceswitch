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

import com.pervacio.wds.app.EMConfig;
import com.pervacio.wds.app.EMDataType;
import com.pervacio.wds.app.EMFileFinder;
import com.pervacio.wds.app.EMFileMetaData;
import com.pervacio.wds.app.EMGenerateCalendarXmlAsyncTask;
import com.pervacio.wds.app.EMGenerateContactsXmlAsyncTask;
import com.pervacio.wds.app.EMGenerateSmsMessagesXmlAsyncTask;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMStringConsts;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.EMXmlGenerator;
import com.pervacio.wds.sdk.CMDError;

import java.io.File;
import java.util.ArrayList;

public class CMDCreateBackupAndWriteDataHelper implements EMProgressHandler {

	public CMDCreateBackupAndWriteDataHelper(EMProgressHandler clientProgressHandler,
														CMDFileSystemInterface cloudServiceFileInterface,
														int dataTypes) {
		mClientProgressHandler = clientProgressHandler;
		mDataTypes = dataTypes;
		mCloudFileService = cloudServiceFileInterface;
	}
	
	// Start the helper
	// Calls the client progress handler to notify it of progress, completion and / or errors
	public void start() {
		mState = CMD_STATE_NOT_STARTED;
		moveToNextState();
	}
	
	// Local states - must be kept in order of execution
	// These must be done in the order that they occur in EMDataType (otherwise progress reporting can be messed up)
	static private final int CMD_STATE_NOT_STARTED = 0;
	static private final int CMD_STATE_CALCULATE_SELECTED_DATA_SIZE = 1;
	static private final int CMD_STATE_UPLOADING_SALT_FILE = 2;
    static private final int CMD_STATE_UPLOADING_BACKUP_STARTED_FILE = 3;
	static private final int CMD_STATE_CREATING_CONTACTS_FILE = 4;
	static private final int CMD_STATE_UPLOADING_CONTACTS_FILE = 5;
	static private final int CMD_STATE_CREATING_CALENDAR_FILE = 6;
	static private final int CMD_STATE_UPLOADING_CALENDAR_FILE = 7;
	static private final int CMD_STATE_CREATING_SMS_FILE = 8;
	static private final int CMD_STATE_UPLOADING_SMS_FILE = 9;
	static private final int CMD_STATE_FINDING_FILES = 10;
	static private final int CMD_STATE_UPLOADING_FILES = 11;
	static private final int CMD_STATE_UPLOADING_MANIFEST_FILE = 12;
    static private final int CMD_STATE_UPLOADING_BACKUP_FINISHED_FILE = 13;
	static private final int CMD_STATE_DONE = 14;
	
	private EMProgressHandler mClientProgressHandler;
	private int mDataTypes; // Bit-field of CMDDataTypes to backup
	private int mState = CMD_STATE_NOT_STARTED;
	private CMDFileSystemInterface mCloudFileService; // The cloud service that started this help - the help will call back into the service to run subtasks
	
	private EMGenerateContactsXmlAsyncTask mGenerateContactsTask;
	private EMGenerateCalendarXmlAsyncTask mGenerateCalendarTask;
	private EMGenerateSmsMessagesXmlAsyncTask mGenerateSmsTask;

	private EMFileFinder mFileFinderTask;
	private ArrayList<EMFileMetaData> mFilesToTransfer;

	private EMFileFinder mFileFinder; // Used to calculate the space of the selected content
	private ArrayList<EMFileMetaData> mFoundFiles  = new ArrayList<EMFileMetaData>();

	// Moves to the next state
	// Completes the client progress handler when done
	private void moveToNextState() {
		boolean startedOperation = false;
		
		while ((!startedOperation)
					&& (mState != CMD_STATE_DONE))
		{
			mState++;

			if (mState == CMD_STATE_CALCULATE_SELECTED_DATA_SIZE) {
				mFileFinder = new EMFileFinder(mDataTypes, mFoundFiles);
				EMProgressInfo progressInfo = new EMProgressInfo();
				progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_CHECKING_REMOTE_STORAGE_SPACE;
				mClientProgressHandler.progressUpdate(progressInfo);
				mFileFinder.startTask(this);
			}

			// Skip to the next state if any aren't appropriate to the current state
			if ((mState == CMD_STATE_CREATING_CONTACTS_FILE)
					&& ((mDataTypes & EMDataType.EM_DATA_TYPE_CONTACTS) == 0)) {
				continue;
			}
			
			if ((mState == CMD_STATE_UPLOADING_CONTACTS_FILE)
					&& ((mDataTypes & EMDataType.EM_DATA_TYPE_CONTACTS) == 0)) {
				continue;
			}
			
			if ((mState == CMD_STATE_CREATING_CALENDAR_FILE)
					&& ((mDataTypes & EMDataType.EM_DATA_TYPE_CALENDAR) == 0)) {
				continue;
			}

			if ((mState == CMD_STATE_CREATING_SMS_FILE)
					&& ((mDataTypes & EMDataType.EM_DATA_TYPE_SMS_MESSAGES) == 0)) {
				continue;
			}
			
			if ((mState == CMD_STATE_UPLOADING_CALENDAR_FILE)
					&& ((mDataTypes & EMDataType.EM_DATA_TYPE_CALENDAR) == 0)) {
				continue;
			}

			if ((mState == CMD_STATE_UPLOADING_SMS_FILE)
					&& ((mDataTypes & EMDataType.EM_DATA_TYPE_SMS_MESSAGES) == 0)) {
				continue;
			}
			
			if (mState == CMD_STATE_CREATING_CONTACTS_FILE) {
				mGenerateContactsTask = new EMGenerateContactsXmlAsyncTask();
				mGenerateContactsTask.startTask(this);
			}
			
			if (mState == CMD_STATE_UPLOADING_CONTACTS_FILE) {
				CMDCloudServiceFileInterface.CMDRemoteFileInfo remoteFileInfo = new CMDCloudServiceFileInterface.CMDRemoteFileInfo();
				remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/contacts.xml";
				remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_CONTACTS;
				mCloudFileService.uploadFileAsync(mGenerateContactsTask.getFilePath(), remoteFileInfo, this, mGenerateContactsTask.getNumberOfEntries());
				// TODO: delete the local file after it has been uploaded
			}

			if (mState == CMD_STATE_CREATING_CALENDAR_FILE) {
				mGenerateCalendarTask = new EMGenerateCalendarXmlAsyncTask();
				mGenerateCalendarTask.startTask(this);
			}

			if (mState == CMD_STATE_CREATING_SMS_FILE) {
				mGenerateSmsTask = new EMGenerateSmsMessagesXmlAsyncTask();
				mGenerateSmsTask.startTask(this);
			}

			if (mState == CMD_STATE_UPLOADING_CALENDAR_FILE) {
				CMDCloudServiceFileInterface.CMDRemoteFileInfo remoteFileInfo = new CMDCloudServiceFileInterface.CMDRemoteFileInfo();
				remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/calendar.xml";
				remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_CALENDAR;
				mCloudFileService.uploadFileAsync(mGenerateCalendarTask.getFilePath(), remoteFileInfo, this, mGenerateCalendarTask.getNumberOfEntries());
				// TODO: delete the local file after it has been uploaded
			}

			if (mState == CMD_STATE_UPLOADING_SMS_FILE) {
				CMDCloudServiceFileInterface.CMDRemoteFileInfo remoteFileInfo = new CMDCloudServiceFileInterface.CMDRemoteFileInfo();
				remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/sms.xml";
				remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_SMS_MESSAGES;
				mCloudFileService.uploadFileAsync(mGenerateSmsTask.getFilePath(), remoteFileInfo, this, mGenerateSmsTask.getNumberOfEntries());
				// TODO: delete the local file after it has been uploaded
			}

			if (mState == CMD_STATE_FINDING_FILES) {
				mFilesToTransfer = new ArrayList<EMFileMetaData>();
				mFileFinderTask = new EMFileFinder(mDataTypes, mFilesToTransfer);
				EMProgressInfo progressInfo = new EMProgressInfo();
				progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_FINDING_FILES;
				mClientProgressHandler.progressUpdate(progressInfo);
				mFileFinderTask.startTask(this);
			}

			if (mState == CMD_STATE_UPLOADING_FILES) {
				mCloudFileService.uploadFilesAsync(mFilesToTransfer, "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME, this);
			}

	        if (mState == CMD_STATE_UPLOADING_MANIFEST_FILE) {
	            // Generate the handshake XML
	            EMXmlGenerator xmlGenerator = new EMXmlGenerator();
	            
	            try {
		            xmlGenerator.startDocument();
		            
		            if ((mDataTypes & EMDataType.EM_DATA_TYPE_CALENDAR) != 0)
		            {
		                xmlGenerator.startElement(EMStringConsts.EM_XML_MANIFEST_CONTAINS_CONTENT);
		                xmlGenerator.writeText(EMStringConsts.EM_XML_CALENDAR);
		                xmlGenerator.endElement(EMStringConsts.EM_XML_MANIFEST_CONTAINS_CONTENT);
		            }
		            
		            if ((mDataTypes & EMDataType.EM_DATA_TYPE_CONTACTS) != 0)
		            {
		                xmlGenerator.startElement(EMStringConsts.EM_XML_MANIFEST_CONTAINS_CONTENT);
		                xmlGenerator.writeText(EMStringConsts.EM_XML_CONTACTS);
		                xmlGenerator.endElement(EMStringConsts.EM_XML_MANIFEST_CONTAINS_CONTENT);
	
		            }
		            
		            String manifestFilePath = xmlGenerator.endDocument();

					CMDFileSystemInterface.CMDRemoteFileInfo remoteFileInfo = new CMDFileSystemInterface.CMDRemoteFileInfo();
		            remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/manifest.xml";
		            remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_MANIFEST;
		            
		            mCloudFileService.uploadFileAsync(manifestFilePath,
		            									remoteFileInfo,
		            									this,
		            									0);
		            
	            }
	            catch (Exception ex) {
	            	mClientProgressHandler.taskError(CMDError.LOCAL_FILE_FAILURE, false);
	            }
	        }

			if (mState == CMD_STATE_UPLOADING_SALT_FILE) {
				if (!CMDCryptoSettings.isConfigured()) {
					mState = CMD_STATE_UPLOADING_BACKUP_STARTED_FILE;
				}
				else {
					byte[] saltData = CMDCryptoSettings.getSalt();
					String saltFilePath = EMUtility.temporaryFileName();
					File tempSaltFile = new File(saltFilePath);
					try {
						EMUtility.writeByteArrayToFile(saltData, tempSaltFile);
						CMDFileSystemInterface.CMDRemoteFileInfo remoteFileInfo = new CMDFileSystemInterface.CMDRemoteFileInfo();
						remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/salt";
						remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_MANIFEST;
						mCloudFileService.uploadFileAsync(saltFilePath,
								remoteFileInfo,
								this,
								0);

					} catch (Exception ex) {
						// TODO: handle error writing the salt file
					}
				}
			}

			if (mState == CMD_STATE_UPLOADING_BACKUP_STARTED_FILE) {
                try {
                    String filePath = EMUtility.createReferenceFileWithSendingDeviceInfo();
                    CMDFileSystemInterface.CMDRemoteFileInfo remoteFileInfo = new CMDFileSystemInterface.CMDRemoteFileInfo();
                    remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/backup-started.xml";
                    remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_MANIFEST;
                    mCloudFileService.uploadFileAsync(filePath,
                            remoteFileInfo,
                            this,
                            0);
                } catch (Exception ex) {
                    // TODO:
                }
            }

            if (mState == CMD_STATE_UPLOADING_BACKUP_FINISHED_FILE) {
                EMXmlGenerator xmlGenerator = new EMXmlGenerator();
                try {
                    xmlGenerator.startDocument();
                    String filePath = xmlGenerator.endDocument();
                    CMDFileSystemInterface.CMDRemoteFileInfo remoteFileInfo = new CMDFileSystemInterface.CMDRemoteFileInfo();
                    remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/" + EMStringConsts.EM_BACKUP_FINISHED_FILE;
                    remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_MANIFEST;
                    mCloudFileService.uploadFileAsync(filePath,
                            remoteFileInfo,
                            this,
                            0);
                } catch (Exception ex) {
                    // TODO:
                }
            }

			startedOperation = true;
		}
		
		if (mState == CMD_STATE_DONE) {
			// TODO: check for errors?
			mClientProgressHandler.taskComplete(true);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////
	// From CMDProgressHander
	
	@Override
	public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
			mClientProgressHandler.taskError(errorCode, alreadyDisplayedDialog);
	}

	@Override
	public void taskComplete(boolean aSuccess) {
	    if (mState == CMD_STATE_UPLOADING_SALT_FILE)
			CMDCryptoSettings.setEnabled(true); // Turn encryption on now that we have sent the salt file

		if (mState == CMD_STATE_CALCULATE_SELECTED_DATA_SIZE) {
			long totalSizeOfSelectedData = 0;
			for(EMFileMetaData fileMetadata : mFoundFiles) {
				totalSizeOfSelectedData += fileMetadata.mSize;
			}

			CMDFileSystemInterface.CMDFileSystemInfo targetFileSystemInfo = mCloudFileService.getCachedFileSystemInfo();
			long freeSpaceOnTargetFileSystem = 0;
			if (targetFileSystemInfo != null) {
				freeSpaceOnTargetFileSystem = targetFileSystemInfo.mTotalSpaceBytes - targetFileSystemInfo.mUsedSpaceBytes;
			}

		if ((totalSizeOfSelectedData + EMConfig.SPACE_CHECK_SAFETY_MARGIN) > freeSpaceOnTargetFileSystem)
			if (!EMConfig.DISABLE_INITIAL_SPACE_CHECK_ERROR) {
				taskError(CMDError.CMD_ERROR_NOT_ENOUGH_SPACE_ON_TARGET_FILE_SYSTEM, false);
				return;
			}
		}

		// Move to the next state - sends a completion message to the client when done
		moveToNextState();
	}

	@Override
	public void progressUpdate(EMProgressInfo progressInfo) {
		mClientProgressHandler.progressUpdate(progressInfo);
	}
}
