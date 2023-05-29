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

public class EMProgressInfo {
	public enum EMOperationType
	{
	    EM_OPERATION_SENDING_DATA,
	    EM_OPERATION_SENT_DATA,
	    EM_OPERATION_RECEIVING_DATA,
	    EM_OPERATION_PROCESSING_OUTGOING_DATA,
	    EM_OPERATION_PROCESSING_INCOMING_DATA,
	    EM_QUIT_COMMAND_RECEIVED,
	    EM_QUIT_COMMAND_SENT,
		EM_RECEIVED_DATA,
		EM_USER_LOGGING_IN,
		EM_USER_LOGGING_OUT,
		EM_USER_LOGGED_OUT,
		EM_GETTING_BACKUP_INFO,
		EM_WAITING_FOR_REMOTE_BACKUP_START,
		EM_WAITING_FOR_REMOTE_BACKUP_FINISH,
		EM_RESTORING_FROM_BACKUP,
		EM_TRANSFER_PAUSED,
		EM_TRANSFER_RESUMED,
		EM_FINDING_FILES,
		EM_CHECKING_REMOTE_STORAGE_SPACE,
		EM_NULL_OPERATION,
		//START PERVACIO
        EM_TEXT_COMMAND_SENT,
        EM_TEXT_COMMAND_RECEIVED,
		EM_TEXT_RESTORE_COMPLETED,
		EM_UPDATE_DB_DETAILS

		//END PERVACIO
	};

	public enum EMTransportMode {
		EM_UNKNOWN,
		EM_WIFI,
		EM_SD_CARD,
		EM_CLOUD
	}

	public EMOperationType mOperationType;
	public int mDataType;
	public int mCurrentItemNumber;
	public int mTotalItems;
	public String mItemDescription;
	public boolean mFailed;
	public EMTransportMode mTransportMode = EMTransportMode.EM_UNKNOWN;
	public long mFileSize        = -1;
	public long mTotalMediaSize  = -1;
	public int  mProgressPercent = -1;
	public String mTextCommand ;	//PERVACIO ADDED.
}
