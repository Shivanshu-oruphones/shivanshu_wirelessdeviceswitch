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

import java.io.IOException;
import java.io.File;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMConfig;
import com.pervacio.wds.app.EMMigrateStatus;
import com.pervacio.wds.app.EMParseCalendarXmlAsyncTask;
import com.pervacio.wds.app.EMParseContactsXmlAsyncTask;
import com.pervacio.wds.app.EMParseSmsXmlAsyncTask;
import com.pervacio.wds.app.EMDataType;
import com.pervacio.wds.app.EMPermissionChecker;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMStringConsts;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.EMXmlPullParser;
import com.pervacio.wds.sdk.CMDError;
import com.pervacio.wds.sdk.internal.CMDFileSystemInterface.CMDRemoteFileInfo;
import com.pervacio.wds.sdk.internal.sdcard.CMDSDCardFileAccess;

public class CMDRestoreBackupToDeviceHelper implements EMProgressHandler
{
	
	// Start the helper
	// Calls the client progress handler to notify it of progress, completion and / or errors
	public void start() {
		EMMigrateStatus.setStartTransfer();
		mState = CMD_STATE_NOT_STARTED;
		waitForBackupStartedFile();
		moveToNextState();
	}

	// Local states - must be kept in order of execution
	static private final int CMD_STATE_NOT_STARTED = 0;
    // static private final int CMD_STATE_WAIT_FOR_BACKUP_STARTED_FILE = 1;

	static private final int CMD_STATE_DOWNLOADING_SALT_FILE = 1;
	static private final int CMD_STATE_DOWNLOADING_BACKUP_STARTED_FILE = 2;
    static private final int CMD_STATE_WAIT_FOR_BACKUP_FINISHED_FILE = 3;
	static private final int CMD_STATE_DOWNLOADING_MANIFEST = 4;
	static private final int CMD_STATE_DOWNLOADING_CONTACTS_FILE = 5;
	static private final int CMD_STATE_PARSING_CONTACTS_FILE = 6;
	static private final int CMD_STATE_DOWNLOADING_CALENDAR_FILE = 7;
	static private final int CMD_STATE_PARSING_CALENDAR_FILE = 8;
	static private final int CMD_STATE_DOWNLOADING_SMS_FILE = 9;
	static private final int CMD_STATE_PARSING_SMS_FILE = 10;
    static private final int CMD_STATE_COPYING_PHOTOS = 11;
    static private final int CMD_STATE_COPYING_VIDEOS = 12;
    static private final int CMD_STATE_COPYING_DOCUMENTS = 13;
    static private final int CMD_STATE_COPYING_MUSIC = 14;
	static private final int CMD_STATE_DONE = 15;

//    static private final int CMD_STATE_REFRESHING_FILES_BEFORE_RETRYING_BACKUP_STARTED_FILE = 20;
//    static private final int CMD_STATE_REFRESHING_FILES_BEFORE_RETRYING_BACKUP_FINISHED_FILE = 21;

	private EMProgressHandler mClientProgressHandler;
	private int mDataTypes; // Bit-field of CMDDataTypes to backup
	private int mState = CMD_STATE_NOT_STARTED;

	private CMDFileSystemInterface mCloudFileInterface;
	private CMDSDCardFileAccess mSDCardFileInterface;
	private CMDFileSystemInterface mSourceFileInterface;

	private EMParseContactsXmlAsyncTask mParseContactsTask;
	private EMParseCalendarXmlAsyncTask mParseCalendarTask;
	private EMParseSmsXmlAsyncTask mParseSmsTask;
	private Context mContext;
	
	private String mTempLocalPath;
	private boolean mFoundManifest;
	private int mManifestContentTypes;
	private boolean mForceGoogleDriveOnly;
	private Set<Integer> mDataTypesWithMissingPermissions;

	static final String TAG = "EasyMigrate";
	
	public CMDRestoreBackupToDeviceHelper(EMProgressHandler clientProgressHandler,
						CMDCloudServiceFileInterface cloudServiceFileInterface,
						int dataTypes,
						Context context,
						boolean aForceGoogleDriveOnly) {
		mClientProgressHandler = clientProgressHandler;
		mDataTypes = dataTypes;
		mCloudFileInterface = cloudServiceFileInterface;
		mContext = context;
		mSDCardFileInterface = new CMDSDCardFileAccess(context);
		mForceGoogleDriveOnly = aForceGoogleDriveOnly;

		if ((cloudServiceFileInterface != null) || (mForceGoogleDriveOnly)) {
			mSourceFileInterface = cloudServiceFileInterface;
		}
		else {
			mSourceFileInterface = mSDCardFileInterface;
		}

		EMPermissionChecker permissionChecker = new EMPermissionChecker(mContext);
		mDataTypesWithMissingPermissions = permissionChecker.getDataTypesWithMissingPermissions(true);
	}
	
	// Moves to the next state
	// Completes the client progress handler when done
	private void moveToNextState() {
		if (mFatalError) // If we're in a fatal error condition then exit
			return;

		boolean startedOperation = false;

		// Log.d(TAG, "+++ >> moveToNextState: mState: " + mState);
		
		while ((!startedOperation)
					&& (mState != CMD_STATE_DONE))
		{
			mState++;

			// Check whether we have the appropriate permissions for this state, if not then skip it
			int dataType = -1;
			switch (mState) {
				case CMD_STATE_DOWNLOADING_CONTACTS_FILE:
				case CMD_STATE_PARSING_CONTACTS_FILE:
					dataType = EMDataType.EM_DATA_TYPE_CONTACTS;
					break;

				case CMD_STATE_DOWNLOADING_CALENDAR_FILE:
				case CMD_STATE_PARSING_CALENDAR_FILE:
					dataType = EMDataType.EM_DATA_TYPE_CALENDAR;
					break;

				case CMD_STATE_DOWNLOADING_SMS_FILE:
				case CMD_STATE_PARSING_SMS_FILE:
					dataType = EMDataType.EM_DATA_TYPE_SMS_MESSAGES;
					break;

				case CMD_STATE_COPYING_PHOTOS:
					dataType = EMDataType.EM_DATA_TYPE_PHOTOS;
					break;
				case CMD_STATE_COPYING_VIDEOS:
					dataType = EMDataType.EM_DATA_TYPE_VIDEO;
					break;
				case CMD_STATE_COPYING_DOCUMENTS:
					dataType = EMDataType.EM_DATA_TYPE_DOCUMENTS;
					break;
				case CMD_STATE_COPYING_MUSIC:
					dataType = EMDataType.EM_DATA_TYPE_MUSIC;
					break;
			}

			if (mDataTypesWithMissingPermissions.contains(dataType)) {
				EMMigrateStatus.setTotalFailure(dataType);
				continue;
			}

			// Log.d(TAG, "moveToNextState: mState++: to " + mState);

			// Skip to the next state if any aren't appropriate to the current state
	        if ((mState == CMD_STATE_DOWNLOADING_CONTACTS_FILE)
	                && (((mDataTypes & EMDataType.EM_DATA_TYPE_CONTACTS) == 0)
	                        || (mManifestContentTypes & EMDataType.EM_DATA_TYPE_CONTACTS) == 0)) {
	                continue;
	            }
			else if (mState == CMD_STATE_DOWNLOADING_SALT_FILE) {
				CMDCloudServiceFileInterface.CMDRemoteFileInfo remoteFileInfo = new CMDCloudServiceFileInterface.CMDRemoteFileInfo();
				remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/salt";
				remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_NOT_SET;
				mTempLocalPath = CMDUtility.temporaryFileName();
				mSourceFileInterface.downloadFileAsync(mTempLocalPath, remoteFileInfo, this);
			}
			else if (mState == CMD_STATE_DOWNLOADING_BACKUP_STARTED_FILE) {
				if (EMConfig.WAIT_FOR_BACKUP_STARTED_AND_FINISHED_FILES) {
					CMDCloudServiceFileInterface.CMDRemoteFileInfo remoteFileInfo = new CMDCloudServiceFileInterface.CMDRemoteFileInfo();
					remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/backup-started.xml";
					remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_NOT_SET;
					mTempLocalPath = CMDUtility.temporaryFileName();
					mSourceFileInterface.downloadFileAsync(mTempLocalPath, remoteFileInfo, this);
				}
				else
					continue;
			}
			else if (mState == CMD_STATE_DOWNLOADING_MANIFEST) {
				// Log.d(TAG, "+++ moveToNextState: Doing Manifest");
				if (EMConfig.FORCE_RESTORE_OF_ALL_CONTENT) {
					mDataTypes = 0xFFFF;
				}

                mManifestContentTypes = 0xFFFF;

				//EMProgressInfo progressInfo = new EMProgressInfo();
                //progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_RESTORING_FROM_BACKUP;
                //mClientProgressHandler.progressUpdate(progressInfo);

				String remoteFolder = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME;

				mSourceFileInterface.getBackupFolderDataTypesAsync(remoteFolder, this);

				//continue; // TODO: For now we will ignore the manifest and user content selection - download everything in the backup
            }
			else if ((mState == CMD_STATE_PARSING_CONTACTS_FILE)
	                && (((mDataTypes & EMDataType.EM_DATA_TYPE_CONTACTS) == 0)
	                    || (mManifestContentTypes & EMDataType.EM_DATA_TYPE_CONTACTS) == 0)) {
	                continue;
	            }
			else if ((mState == CMD_STATE_DOWNLOADING_CALENDAR_FILE)
	                && (((mDataTypes & EMDataType.EM_DATA_TYPE_CALENDAR) == 0)
	                    || (mManifestContentTypes & EMDataType.EM_DATA_TYPE_CALENDAR) == 0)) {
	                continue;
	            }
			else if ((mState == CMD_STATE_PARSING_CALENDAR_FILE)
	                && (((mDataTypes & EMDataType.EM_DATA_TYPE_CALENDAR) == 0)
	                    || (mManifestContentTypes & EMDataType.EM_DATA_TYPE_CALENDAR) == 0)) {
	                continue;
	            }
			else if ((mState == CMD_STATE_COPYING_PHOTOS)
                    && ((mDataTypes & EMDataType.EM_DATA_TYPE_PHOTOS) == 0)) {
                continue;
            }
			else if ((mState == CMD_STATE_COPYING_VIDEOS)
                    && ((mDataTypes & EMDataType.EM_DATA_TYPE_VIDEO) == 0)) {
                continue;
            }
			else if ((mState == CMD_STATE_COPYING_DOCUMENTS)
                    && ((mDataTypes & EMDataType.EM_DATA_TYPE_DOCUMENTS) == 0)) {
                continue;
            }
			else if ((mState == CMD_STATE_COPYING_MUSIC)
                    && ((mDataTypes & EMDataType.EM_DATA_TYPE_MUSIC) == 0)) {
                continue;
            }
			else if (mState == CMD_STATE_WAIT_FOR_BACKUP_FINISHED_FILE) {
				if (EMConfig.WAIT_FOR_BACKUP_STARTED_AND_FINISHED_FILES) {
					EMProgressInfo progressInfo = new EMProgressInfo();
					progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_WAITING_FOR_REMOTE_BACKUP_FINISH;
					mClientProgressHandler.progressUpdate(progressInfo);
					waitForBackupFinishedFile();
				}
				else
					continue;
	        }
			else if (mState == CMD_STATE_DOWNLOADING_CONTACTS_FILE) {
				CMDCloudServiceFileInterface.CMDRemoteFileInfo remoteFileInfo = new CMDCloudServiceFileInterface.CMDRemoteFileInfo();
				remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/" +CMDStringConsts.EM_CONTACTS_FILE_NAME;
				remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_CONTACTS;
				mTempLocalPath = CMDUtility.temporaryFileName();
				mSourceFileInterface.downloadFileAsync(mTempLocalPath, remoteFileInfo, this);
			}
			else if (mState == CMD_STATE_PARSING_CONTACTS_FILE) {
				mParseContactsTask = new EMParseContactsXmlAsyncTask();
				mParseContactsTask.startTask(mTempLocalPath, true, mContext, this);
			}
			else if (mState == CMD_STATE_DOWNLOADING_CALENDAR_FILE) {
				CMDCloudServiceFileInterface.CMDRemoteFileInfo remoteFileInfo = new CMDCloudServiceFileInterface.CMDRemoteFileInfo();
				remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/" +CMDStringConsts.EM_CALENDAR_FILE_NAME;
				remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_CALENDAR;
				mTempLocalPath = CMDUtility.temporaryFileName();
				mSourceFileInterface.downloadFileAsync(mTempLocalPath, remoteFileInfo, this);
			}
			else if (mState == CMD_STATE_PARSING_CALENDAR_FILE) {
				mParseCalendarTask = new EMParseCalendarXmlAsyncTask();
				mParseCalendarTask.startTask(mTempLocalPath, true, mContext, this);
			}
			else if (mState == CMD_STATE_COPYING_PHOTOS) {
                String localFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                String remoteFolder = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/" + CMDStringConsts.EM_PHOTOS_FOLDER_NAME;
				mSourceFileInterface.copyRemoteFolderContentsToLocalAsync(remoteFolder, localFolder, true, EMDataType.EM_DATA_TYPE_PHOTOS, this);
            }
			else if (mState == CMD_STATE_COPYING_VIDEOS) {
                String localFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString();
                String remoteFolder = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/" + CMDStringConsts.EM_VIDEOS_FOLDER_NAME;
				mSourceFileInterface.copyRemoteFolderContentsToLocalAsync(remoteFolder, localFolder, true, EMDataType.EM_DATA_TYPE_VIDEO, this);
            }
			else if (mState == CMD_STATE_COPYING_DOCUMENTS) {
				String localFolder = null;
				if (Build.VERSION.SDK_INT >= 19) {
					File documentsFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

					if (documentsFile != null)
						localFolder = documentsFile.toString();
				}

				if (localFolder == null)
					localFolder = Environment.getExternalStorageDirectory().toString();

				String remoteFolder = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/" + CMDStringConsts.EM_DOCUMENTS_FOLDER_NAME;
				mSourceFileInterface.copyRemoteFolderContentsToLocalAsync(remoteFolder, localFolder, true, EMDataType.EM_DATA_TYPE_DOCUMENTS, this);
            }
			else if (mState == CMD_STATE_COPYING_MUSIC) {
                String localFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString();
                String remoteFolder = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/" + CMDStringConsts.EM_MUSIC_FOLDER_NAME;
				mSourceFileInterface.copyRemoteFolderContentsToLocalAsync(remoteFolder, localFolder, true, EMDataType.EM_DATA_TYPE_MUSIC, this);
            }
			else if (mState == CMD_STATE_DOWNLOADING_SMS_FILE) {
				CMDCloudServiceFileInterface.CMDRemoteFileInfo remoteFileInfo = new CMDCloudServiceFileInterface.CMDRemoteFileInfo();
				remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/" +CMDStringConsts.EM_SMS_FILE_NAME;
				remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_SMS_MESSAGES;
				mTempLocalPath = CMDUtility.temporaryFileName();
				mSourceFileInterface.downloadFileAsync(mTempLocalPath, remoteFileInfo, this);
			}
			else if (mState == CMD_STATE_PARSING_SMS_FILE) {
				mParseSmsTask = new EMParseSmsXmlAsyncTask();
				mParseSmsTask.startTask(mTempLocalPath, true, mContext, this);
			}
			
			startedOperation = true;
		}

		if (mState == CMD_STATE_DONE) {
			// TODO: check for errors?
			mClientProgressHandler.taskComplete(true);
		}

		// Log.d(TAG, "+++ << moveToNextState: mState: " + mState);
	}

    void waitForBackupStartedFile() {
		if (EMConfig.WAIT_FOR_BACKUP_STARTED_AND_FINISHED_FILES) {
			boolean backupFolderExists = false;
			while (!backupFolderExists) {
				boolean hasReadExternalStorage = (EMUtility.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

				if (hasReadExternalStorage) {

					if ((mSDCardFileInterface != null) && (!mForceGoogleDriveOnly)) {
						String[] possibleSDCardPaths = mSDCardFileInterface.getPossibleExternalSDCardPaths();

						for (String possibleSDCardPath : possibleSDCardPaths) {
							try {
								String backupStartedPath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/backup-started.xml";
								mSDCardFileInterface.setSDCardPath(possibleSDCardPath);

								if (mSDCardFileInterface.itemExistsBlocking("/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/backup-started.xml", this)) {
									DLog.log("*** Starting SD card restore");
									backupFolderExists = true;
									mSourceFileInterface = mSDCardFileInterface;
									EMMigrateStatus.setTransportMode(EMProgressInfo.EMTransportMode.EM_SD_CARD);
									break;
								}
							} catch (Exception exception) {
								// Either this isn't the SD card, or we don't have permission to access it
								// Just continue
							}
							// if SD backup found then move state to CMD_STATE_WAIT_FOR_BACKUP_FINISHED_FILE (when this function exists we will move to the next state)
							// mState = CMD_STATE_WAIT_FOR_BACKUP_FINISHED_FILE;
						}
					}
				}

				if (!backupFolderExists) {
					DLog.log("*** Starting google restore");
					if (mCloudFileInterface.itemExistsBlocking("/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/backup-started.xml", this)) {
						backupFolderExists = true;
						mSourceFileInterface = mCloudFileInterface;
						EMMigrateStatus.setTransportMode(EMProgressInfo.EMTransportMode.EM_CLOUD);
					}
				}

				if (!backupFolderExists) {
					DLog.log("*** No backup - waiting");
					SystemClock.sleep(EMConfig.LOOK_AGAIN_FOR_BACKUP_STARTED_DELAY); // Wait then look again...
				}
			}
		}

		// moveToNextState();
    }

    void waitForBackupFinishedFile() {
		if (EMConfig.WAIT_FOR_BACKUP_STARTED_AND_FINISHED_FILES) {
			mState = CMD_STATE_WAIT_FOR_BACKUP_FINISHED_FILE;
			CMDRemoteFileInfo remoteFileInfo = new CMDRemoteFileInfo();
			remoteFileInfo.mFilePath = "/" + EMStringConsts.EM_BACKUP_FOLDER_NAME + "/backup-finished.xml";
			remoteFileInfo.mDataType = EMDataType.EM_DATA_TYPE_CONTACTS;
			mTempLocalPath = CMDUtility.temporaryFileName();

			mSourceFileInterface.downloadFileAsync(mTempLocalPath, remoteFileInfo, this);
		}
    }

	void parseManifest() throws XmlPullParserException, IOException
	{
		// Log.d(TAG, ">> parseManifest");
	    mFoundManifest = true;
	    
	    String manifestFilePath = mTempLocalPath;
	    
	    EMXmlPullParser pullParser = new EMXmlPullParser();
	    pullParser.setFilePath(manifestFilePath);
	    
	    EMXmlPullParser.EMXmlNodeType nodeType = pullParser.readNode();
	    
	    while (nodeType != EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ROOT_ELEMENT ) // While there is no error and we haven't reached the last node
	    {
	        if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_START_ELEMENT)
	        {
	            String elementName = pullParser.name();
	            
	            if (elementName.equalsIgnoreCase(EMStringConsts.EM_XML_MANIFEST_CONTAINS_CONTENT))
	            {
	                nodeType = pullParser.readNode();
	                if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT)
	                {
	                    String contentType = pullParser.value();
	                    
	                    if (contentType.equalsIgnoreCase(EMStringConsts.EM_XML_CONTACTS))
	                    {
	                        mManifestContentTypes |= EMDataType.EM_DATA_TYPE_CONTACTS;
	                    }
	                    
	                    if (contentType.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR))
	                    {
	                        mManifestContentTypes |= EMDataType.EM_DATA_TYPE_CALENDAR;
	                    }
	                    
	                    if (contentType.equalsIgnoreCase(EMStringConsts.EM_XML_PHOTOS))
	                    {
	                        mManifestContentTypes |= EMDataType.EM_DATA_TYPE_PHOTOS;
	                    }
	                    
	                    if (contentType.equalsIgnoreCase(EMStringConsts.EM_XML_VIDEO))
	                    {
	                        mManifestContentTypes |= EMDataType.EM_DATA_TYPE_VIDEO;
	                    }
	                }
	            }
	        }

	        nodeType = pullParser.readNode();
	    }
	}
	
	////////////////////////////////////////////////////////////////////////////////////////
	// From CMDProgressHander

	@Override
	public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
//        static private final int  = 20;
//        static private final int CMD_STATE_REFRESHING_FILES_BEFORE_RETRYING_BACKUP_FINISHED_FILE = 21;

		if (errorCode == CMDError.CMD_ERROR_NOT_ENOUGH_SPACE_ON_LOCAL_DEVICE) {
			fatalError(errorCode);
		}
        else if (mState == CMD_STATE_WAIT_FOR_BACKUP_FINISHED_FILE) {
            SystemClock.sleep(EMConfig.LOOK_AGAIN_FOR_BACKUP_FINISHED_DELAY); // TODO: temp value - later we should implement some kind of incrememntal pause
            File file = new File(mTempLocalPath);
            boolean deleted = file.delete();

            waitForBackupFinishedFile();
        }
		else if (mState == CMD_STATE_DOWNLOADING_SALT_FILE) {
			// If we can't get the salt file then the backup is probably not encrypted, so continue
			taskComplete(true);
		}
        else {
//            mClientProgressHandler.cmdError(errorCode, alreadyDisplayedDialog);
			taskComplete(true); // TODO: assume that the operation completed and continue. It may be the case that the requested file did not exist in the backup - is this correct?
        }
	}

	private boolean mFatalError = false;

	private void fatalError(int aError) {
		mClientProgressHandler.taskError(aError, false);
		mFatalError = true;
	}

	@Override
	public void taskComplete(boolean aSuccess) {
		// Log.d(TAG, "+++ >> taskComplete: mState: " + mState);

		/*
		if (mState == CMD_STATE_DOWNLOADING_MANIFEST_FILE) {
			try {

				// Log.d(TAG, "=== >> taskComplete: Manifest");

				//parseManifest();
			} catch (Exception ex) {
				taskError(CMDError.ERROR_HANDLING_MANIFEST, false);
				return;
			}
		} else
		*/

		if (mState == CMD_STATE_DOWNLOADING_BACKUP_STARTED_FILE) {
			try {
				if (CMDCryptoSettings.enabled()) {
					byte[] backupStartedData = EMUtility.readFileToByteArray(new File(mTempLocalPath));
					if (!CMDCryptoSettings.testDecryptionWithReferenceXML(backupStartedData, true)) { // Save this data, we will use it later to verify that we are decrypting the data correctly (that we have the right password)
						fatalError(CMDError.ERROR_DECRYPTION_FAILED);
						return;
					}

				} else {
					// If not encrypted then parse the reference data anyway to get the sending device type (share parsing)
					EMUtility.parseSendingDeviceInfo(mTempLocalPath);
				}
			}
			catch (Exception ex) {
				fatalError(CMDError.ERROR_GETTING_BACKUP_STARTED_FILE);
				return;
			}
		} else if (mState == CMD_STATE_DOWNLOADING_SALT_FILE)
		{
			try {
				byte[] saltData = EMUtility.readFileToByteArray(new File(mTempLocalPath));
				if (saltData.length > 0) {
					CMDCryptoSettings.setSalt(saltData);
					// Don't set the password - we should already have set that when entering the main service
					CMDCryptoSettings.setEnabled(true);
				}
			} catch (IOException e) {
				taskError(CMDError.ERROR_GETTING_SALT, false);
				return;
			}
			catch (Exception e) {
				taskError(CMDError.ERROR_GENERATING_KEY, false);
				return;
			}
		}

		// Log.d(TAG, "++ << cmdOperationComplete: mState: " + mState);

		/*
		if ((EMMigrateStatus.getTransportMode() == EMProgressInfo.EMTransportMode.EM_SD_CARD)
            && (mState >= CMD_STATE_DOWNLOADING_CONTACTS_FILE)) {
                try {
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    // Ignore
            }
		}
		*/

		// Move to the next state - sends a completion message to the client when done
		moveToNextState();
	}

	@Override
	public void progressUpdate(EMProgressInfo progressInfo) {
		mClientProgressHandler.progressUpdate(progressInfo);
	}
}
