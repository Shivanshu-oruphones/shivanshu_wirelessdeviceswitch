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

public class CMDError {
	static public final int CMD_RESULT_OK = 0;
	static public final int GOOGLE_SERVICES_NOT_AVAILABLE = 1;
	static public final int RESOLVE_CONNECTION_REQUEST_CODE = 2; // Not fatal error - the main activity should retry the authentication on the cloud service
	static public final int GOOGLE_AUTHENTICATION_FAILED = 3;
	static public final int GOOGLE_FAILED_TO_CREATE_CONTENT = 4;
	static public final int FAILED_TO_WRITE_DATA_TO_CLOUD = 5;
	static public final int INVALID_REMOTE_FILE_PATH = 6;
	static public final int FAILED_TO_CREATE_FILE_ON_CLOUD_SERVICE = 7;
	static public final int FAILED_TO_COMPLETE_SYNC_TO_CLOUD = 8;
	static public final int UNHANDLED_EXCEPTION_IN_ASYNC_TASK = 9;
	static public final int FAILED_TO_FIND_REMOTE_OBJECT = 10;
	static public final int FAILED_TO_OPEN_REMOTE_OBJECT = 11;
	static public final int FAILED_TO_READ_DATA_FROM_CLOUD = 12;
	static public final int SELECTING_ACCOUNT = 13; // Not really an error - move this?
	static public final int LOCAL_FILE_FAILURE = 14;
	static public final int ERROR_HANDLING_MANIFEST = 15;
	static public final int CMD_ERROR_LOGING_IN_CANCELED = 16;
	static public final int ERROR_GETTING_SALT = 17;
	static public final int ERROR_GENERATING_KEY = 18;
	static public final int ERROR_DECRYPTION_FAILED = 19; // Most likely an incorrect password
	static public final int ERROR_GETTING_BACKUP_STARTED_FILE = 20;
	static public final int CMD_UTILITY_ERROR_CRYPTO = 21;
	static public final int CMD_ERROR_NOT_ENOUGH_SPACE_ON_LOCAL_DEVICE = 22;
	static public final int CMD_ERROR_UNABLE_TO_CREATE_DEVICE_INFO_FILE = 23;

	static public final int CMD_ERROR_REMOTE_SERVICE_ERROR = 1001; // An error using the remote service - maybe an internet or service issue
	static public final int CMD_ERROR_EXPECTED_OBJECT_NOT_PRESENT_ON_REMOTE_SERVICE = 1002;
	static public final int CMD_ERROR_UNABLE_TO_CREATE_OBJECT_ON_REMOTE_SERVICE = 1003;

	static public final int CMD_ERROR_LOGING_IN_FAILED = 2000; // Not possible to log in to the remote service
	static public final int CMD_ERROR_DOWNLOADING_FILE = 2001;
	static public final int CMD_ERROR_FINDING_REMOTE_FILE = 2002;
	static public final int CMD_ERROR_CREATING_REMOTE_PATH = 2003;
	static public final int CMD_ERROR_UPLOADING_FILE = 2004;
	static public final int CMD_ERROR_NOT_ENOUGH_SPACE_ON_TARGET_FILE_SYSTEM = 2005;

	static public final int CMD_GOOGLE_DRIVE_ACCESS_RECOVERABLE_AUTHENTICATION_ERROR = 3000; // The error is recoverable and steps have been taken, for example displaying a dialog to the user
	static public final int CMD_GOOGLE_DRIVE_ACCESS_TRANSIENT_AUTHENTICATION_ERROR = 3001; // Loss of network, server error, or quota exceeded - don't retry immediately
	static public final int CMD_GOOGLE_DRIVE_ACCESS_AUTHENTICATION_FAILED = 3002; // Fatal authentication error - there's no point retrying with the same parameters
	static public final int CMD_GOOGLE_DRIVE_ACCESS_AUTHENTICATION_RECOVERY_COMPLETE = 3003;
	static public final int CMD_GOOGLE_DRIVE_ACCESS_ERROR_LISTING_CHILDREN = 3004;
	static public final int CMD_GOOGLE_DRIVE_ACCESS_UNABLE_TO_COPY_FILE_FROM_LOCAL = 3005;
	static public final int CMD_ERROR_NO_WIFI_AND_CELLULAR_NOT_PERMITTED = 3006;
	static public final int CMD_GOOGLE_DRIVE_FULL_ERROR = 3007; // Loss of network, server error, or quota exceeded - don't retry immediately
	static public final int CMD_GOOGLE_DRIVE_DESTINATION_DEVICE_REQUIRES_UPDATE = 3008;
	static public final int CMD_GOOGLE_DRIVE_ACCESS_UNABLE_TO_COPY_FILE_TO_LOCAL = 3009;

	static public final int CMD_SD_CARD_ERROR_COPYING_TO_LOCAL = 4000;
	static public final int CMD_SD_CARD_ERROR_COPYING_FOLDER_TO_LOCAL = 4001;
	static public final int CMD_SD_CARD_ERROR_CRYPTO = 4002;
	static public final int CMD_SD_CARD_ERROR_COPYING_FILE_TO_SD_CARD = 4003;
	static public final int CMD_SD_CARD_ERROR_COPYING_DIRETORY = 4004;

	static public final int CMD_WIFI_TIMEOUT_ERROR = 5000;
	static public final int CMD_WIFI_MAIN_CONNECTION_ERROR = 5001;

	static public final int CMD_PERMISSION_FAILURES  = 6000;
	static public final int CMD_GENERAL_FAILURES  = 6001;

	static public final int CMD_UNABLE_TO_REACH_PEER = 7000;
}
