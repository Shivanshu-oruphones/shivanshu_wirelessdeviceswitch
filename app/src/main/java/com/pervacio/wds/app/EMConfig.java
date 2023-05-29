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

import java.util.Arrays;
import java.util.List;

public class EMConfig {
	public static final int DEVICE_CAPABILITIES = EMDeviceInfo.EM_SUPPORTS_CONTACTS
			| EMDeviceInfo.EM_SUPPORTS_CALENDAR
			| EMDeviceInfo.EM_SUPPORTS_PHOTOS
			| EMDeviceInfo.EM_SUPPORTS_VIDEOS
			| EMDeviceInfo.EM_SUPPORTS_SMS_MESSAGES
			| EMDeviceInfo.EM_SUPPORTS_NOTES
			| EMDeviceInfo.EM_SUPPORTS_TASKS
			| EMDeviceInfo.EM_SUPPORTS_ACCOUNTS
			| EMDeviceInfo.EM_SUPPORTS_DOCUMENTS
			| EMDeviceInfo.EM_SUPPORTS_MUSIC
			| EMDeviceInfo.EM_SUPPORTS_APP;

	public static final int SUPPORTED_ROLES = EMDeviceInfo.EM_SUPPORTS_ROLE_MIGRATION_SOURCE
			| EMDeviceInfo.EM_SUPPORTS_ROLE_MIGRATION_TARGET;

	public static final boolean LOG_CONTACTS_XML = false;

	public static final boolean SKIP_CLOUD_CALENDARS = false;

	public static final boolean PC_INITIATED_WIFI_MODE = false;
	public static final boolean USE_FIXED_PORT = true;

	public static final int COMMUNICATION_PORT_NUMBER = 31313;
	public static final int DATA_TRANSFER_PORT_NUMBER = 31315;

	public static int FIXED_PORT_NUMBER = COMMUNICATION_PORT_NUMBER;

	public static final boolean REQUIRES_WIFI_PIN = false;

	public static final boolean ENABLE_DECRYPTION = true;
	public static final boolean ENABLE_ENCRYPTION = true;

	public static final boolean ASK_FOR_WRITE_ACCESS_TO_SMS = false;

	public static final String TAG = "deviceswitch";

	public static final String CALENDAR_ACCOUNT_NAME = "Content Transfer";
	public static final String CALENDAR_DISPLAY_NAME = "Content Transfer";

	public static final int EM_WAIT_FOR_WIFI_TIME_MS = 5000; // Polling time to check if WiFi has been enabled
	public static final int EM_WAIT_FOR_RE_AUTH_TIME_MS = 5000;
	public static final int EM_WAIT_FOR_WIFI_GLITCH_TIME_MS = 1000; // Polling time to check if WiFi has been enabled
	public static final int LOOK_AGAIN_FOR_BACKUP_STARTED_DELAY = 5000; // Polling interval (in MS) when waiting for the backup-started file
	public static final int LOOK_AGAIN_FOR_BACKUP_FINISHED_DELAY = 10000; // Polling interval (in MS) when waiting for the backup-finished file

	// Account types to be ignored when fetching contacts and calendar (note that if the values below occur as a substring in the ACCOUNT_TYPE then they will be considered a match)
	final static public List<String> kAccountTypesToIgnore = Arrays.asList("com.google",
			"com.android.exchange", // Modern hotmail
			"com.google.android.gm.exchange", // Exchange (note this will get filtered anyway due to the com.google substring
			"com.aol",
			"com.facebook",
			"com.hotmail",
			"com.outlook",
			"com.yahoo");

	public static final boolean ALLOW_UNENCRYPTED_WIFI_TRANSFER = true; // Leave this set to false, otherwise it's a security risk (unverified clients can push data)

	public static final long SPACE_CHECK_SAFETY_MARGIN = 1024 * 1024 * 20; // ensure there's an extra X MBs of data before starting a transfer

	public static final int WIFI_TIMEOUT_IN_SECONDS = 10;
	public static final int NOOP_TIMER_IN_SECONDS = 2;

	public static final String DESTINATION_DEVICE_SOFTWARE_ID = "02095"; // Used so the source device can get info about the destination device - this can be used to modify the behavior of the source device depending on the destination device

	public static final int MINIMUM_DESTINATION_APP_VERSION = 2093;

	public static final boolean DISABLE_INITIAL_SPACE_CHECK_ERROR = false; // Set to true for TESTING ONLY

	public static final int CELLULAR_POLLING_TIMEOUT_MILLISECONDS = 8 * 60 * 60 * 1000;
	public static final int EM_GOOGLE_DRIVE_READ_BUFFER_SIZE = 1024 * 256;
	public static final int EM_GOOGLE_DRIVE_WRITE_BUFFER_SIZE = 1024 * 1024;
//	public static int CELLULAR_POLLING_TIMEOUT_MILLISECONDS = 60 * 1000; // Short timeout for testing only

	public static final int MAXIMUM_NUMBER_OF_SMS_MESSAGES_TO_SEND = 3500;

	public static final boolean LOGGING_ON = true; // Should be off for release (it can significantly slow down the app)
	public static final boolean RESTORE_FROM_INTERNAL_STORAGE = false; // true for testing only (restores from interal storage instead of SD card)

	public static final int VERSION = 219;


	public static final boolean FORCE_RESTORE_OF_ALL_CONTENT = false;
	public static final boolean WAIT_FOR_BACKUP_STARTED_AND_FINISHED_FILES = false;
	public static final boolean REQUIRES_SPECIFIC_REMOTE_DEVICE_APP_VERSION = false;

	public static final boolean ENABLE_GOOGLE_DRIVE = true;
	public static final boolean ENABLE_SD_CARD = false;
	public static final boolean ENABLE_ADS = false;

	public static final int kNetworkConnectCheckTimeout = 1; // Seconds. Time to wait before checking if we have a connection
	public static final int kNetworkConnectCheckTimeouts = 15; // Number of times to cycle through the network check timeouts before we give up a try reconnecting
	public static final int kNetworkConnectMaxAttempts = 4;     // Total number of times to try and connect to peer network
	final static public String EM_PRIVATE_DIR = "cmd"; // In applications private area

	public static final int PEER_CONNECTION_ATTEMPTS = 5;
	public static final int WIFI_DIRECT_RECONNECTION_TIMER_IN_SECONDS = 10;
	public static final int DELAY_BETWEEN_CONNECTION_ATTEMPTS_IN_SECONDS = 1;
	public static final int PENDING_RECONNECTION_TIMER_IN_SECONDS = 5; // The number of seconds to wait before reconnection attempts

	public static final boolean QR_CODE_MODE_BY_DEFAULT = false; // Use QR code pairing for WiFi (also enables WiFi direct transfers)

	public static final boolean ALLOW_CONTINUE_WITH_WIFI_OFF = false;

	public enum EMWiFiConnectionMode {
		LAN,
		DIRECT
	}

	public static final EMWiFiConnectionMode[] WIFI_CONNECTION_PREFERENCE_ORDER = {
			EMWiFiConnectionMode.DIRECT,
			EMWiFiConnectionMode.LAN
	};

	public static final boolean USE_JMDNS_DISCOVERY = false;
	public static final boolean USE_NSD_DISCOVERY = true;

	public static final boolean LOG_RAW_ASYNC_STREAM_DATA = false;

	public static final int GOOGLE_DRIVE_AUTHENTICATION_ATTEMPTS = 3;

	public static final boolean SKIP_WELCOME_SCREEN = true;

	public static final boolean DEFAULT_TO_CONTACTS_ONLY_IN_DIRECT_MODE = false;

	public static final boolean NEW_DATA_TRANSFER_PROTOCOL = true;

	public static boolean SEQUENCE_TRANSFER_FOR_IOS= false;

}
