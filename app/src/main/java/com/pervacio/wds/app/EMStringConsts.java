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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class EMStringConsts {
    final static public String EM_SERVICE_NAME = "_easymigrate._tcp.";
    final static public String EM_COMMAND_TEXT_HANDSHAKE = "HANDSHAKE";
    final static public String EM_COMMAND_TEXT_YOU_ARE_TARGET = "YOU_ARE_TARGET";
    final static public String EM_COMMAND_TEXT_YOU_ARE_SOURCE = "YOU_ARE_SOURCE";
    final static public String EM_COMMAND_TEXT_PIN_REQUEST = "PIN_REQUEST";
    final static public String EM_COMMAND_TEXT_PIN_OK = "PIN_OK";
    final static public String EM_COMMAND_TEXT_ADD_CONTACTS = "ADD_CONTACTS";
    final static public String EM_COMMAND_TEXT_ADD_CALENDAR = "ADD CALENDAR";
    final static public String EM_COMMAND_TEXT_ADD_FILE = "ADD_FILE";
    final static public String EM_COMMAND_TEXT_ADD_SMS_MESSAGES = "ADD_SMS_MESSAGES";
    final static public String EM_COMMAND_TEXT_ADD_NOTES = "ADD_NOTES";
    final static public String EM_COMMAND_TEXT_ADD_TASKS = "ADD_TASKS";
    final static public String EM_COMMAND_TEXT_ADD_ACCOUNTS = "ADD_ACCOUNTS";
    final static public String EM_COMMAND_TEXT_ADD_DATA = "ADD_DATA";
    final static public String EM_COMMAND_TEXT_ADD_CRYPTO_SRP = "ADD_CRYPTO_SRP";
    final static public String EM_COMMAND_TEXT_QUIT = "QUIT";
    final static public String EM_COMMAND_TEXT = "TEXTCOMMAND:";  //PERVACIO ADDED
    final static public String EM_COMMAND_TEXT_NOOP = "NOOP";
    final static public String EM_COMMAND_UPDATE_DB_STATUS_INPROGRESS = "DB_LOG_STATUS_INPROGRESS";
    final static public String EM_COMMAND_UPDATE_DB_STATUS_FINAL = "DB_LOG_STATUS_FINAL";
    final static public String EM_COMMAND_SERVER_RESPONSE = "SERVER_RESPONSE";
    final static public String EM_COMMAND_START_ESTIMATION = "START_ESTIMATION";
    final static public String EM_COMMAND_RESTORE_COMPLETED = "RESTORE_COMPLETED";

    final static public String DATA_TYPE_CONTACTS = "CONTACTS";
    final static public String DATA_TYPE_CALENDAR = "CALENDAR";
    final static public String DATA_TYPE_SMS_MESSAGES = "SMS_MESSAGES";
    final static public String DATA_TYPE_CALL_LOGS = "CALL_LOGS";
    final static public String DATA_TYPE_SETTINGS = "SETTINGS";
    final static public String DATA_TYPE_NOTES = "NOTES";
    final static public String DATA_TYPE_TASKS = "TASKS";
    final static public String DATA_TYPE_ACCOUNTS = "ACCOUNTS";
    final static public String DATA_TYPE_KEYBOARD_SHORTCUTS = "KEYBOARD_SHORTCUTS";

    final static public String EM_COMMAND_TEXT_ADD_CRYPTO = "ADD_CRYPTO";
    final static public String EM_COMMAND_TEXT_ADD_SALT = "SET_SALT";
    final static public String EM_COMMAND_TEXT_ADD_REFERENCE_DATA = "SET_REFERENCE_DATA";

    final static public String EM_COMMAND_TEXT_CREATE_WIFI_NETWORK = "CREATE_WIFI_NETWORK";
    final static public String EM_COMMAND_TEXT_GET_WIFI_DIRECT_GROUP_DETAILS = "GET_WIFI_DIRECT_GROUP_DETAILS";
    final static public String EM_COMMAND_TEXT_STOP_WIFI_NETWORK = "STOP_WIFI_NETWORK";
    final static public String EM_COMMAND_TEXT_JOIN_WIFI_NETWORK = "JOIN_WIFI_NETWORK";
    final static public String EM_COMMAND_TEXT_REMOVE_WIFI_NETWORK = "REMOVE_WIFI_NETWORK";

    final static public String EM_TEXT_RESPONSE_OK = "OK";
    final static public String EM_TEXT_RESPONSE_ALREADY_EXISTS = "ALREADY_EXISTS";
    final static public String EM_TEXT_RESPONSE_UNSUPPORTED = "UNSUPPORTED";
    final static public String EM_TEXT_RESPONSE_ERROR = "ERROR";
    final static public String EM_TEXT_RESPONSE_FAILED = "FAILED";

    final static public String EM_XML_ROOT = "root";

    final static public String EM_XML_DEVICE_INFO = "device_info";

    final static public String EM_XML_DEVICE_NAME = "name";
    final static public String EM_XML_DEVICE_PORT = "port";
    final static public String EM_XML_CAN_ADD = "can_add";
    final static public String EM_XML_SUPPORTS_ROLE = "supports_role";
    public static final String EM_XML_LINK_SPEED = "link_speed";
    public static final String EM_XML_SESSION_ID = "session_id";
    final static public String EM_XML_SERVICE_NAME = "service_name";
    final static public String EM_XML_DEVICE_UNIQUE_ID = "device_uid";
    final static public String EM_XML_KEYBOARD_SHORTCUT_IMPORTER_AVAILABLE = "keyboard_shortcut_importer_available";
    final static public String EM_XML_THIS_DEVICE_IS_TARGET_AUTO_CONNECT = "this_device_is_target_auto_connect";

    final static public String EM_XML_CONTACTS = "contacts";
    final static public String EM_XML_CALENDAR = "calendar";
    final static public String EM_XML_FILE = "file";
    final static public String EM_XML_PHOTOS = "photos";
    final static public String EM_XML_VIDEO = "video";
    final static public String EM_XML_NOTES = "notes";
    final static public String EM_XML_UNKNOWN_FILE_TYPE = "unknown";
    final static public String EM_XML_SMS_MESSAGES = "sms";
    final static public String EM_XML_ACCOUNTS = "accounts";
    final static public String EM_XML_DOCUMENTS = "documents";
    final static public String EM_XML_APP = "app";
    final static public String EM_XML_MUSIC = "music";
    final static public String EM_XML_KEYBOARD_SHORTCUTS = "keyboard_shortcuts";
    final static public String DENIED_PERMISSIONS = "denied_permissions";

    final static public String EM_XML_MANIFEST_CONTAINS_CONTENT = "contains_content";

    final static public String EM_XML_ROLE_MIGRATION_SOURCE = "migration_source";
    final static public String EM_XML_ROLE_MIGRATION_TARGET = "migration_target";

    final static public String EM_XML_CONTACT_ENTRY = "contact";
    final static public String EM_XML_CONTACT_VCARD_ENTRY = "vcard";
    final static public String EM_XML_CALENDAR_ENTRY = "calendar_entry";
    final static public String EM_XML_CALL_LOG_ENTRY = "call_log";
    final static public String EM_XML_PHONE_NUMBER = "phone_number";
    final static public String EM_XML_CALL_TYPE = "call_type";
    final static public String EM_XML_CALL_DATE = "call_date";
    final static public String EM_XML_CALL_DURATION = "call_duration";
    final static public String EM_XML_CALENDAR_ENTRY_TITLE = "title";
    final static public String EM_XML_CALENDAR_ENTRY_LOCATION = "location";
    final static public String EM_XML_CALENDAR_ENTRY_DESCRIPTION = "description";
    final static public String EM_XML_CALENDAR_ENTRY_START_DATE = "start_date";
    final static public String EM_XML_CALENDAR_ENTRY_END_DATE = "end_date";
    final static public String EM_XML_CALENDAR_ENTRY_DURATION = "duration";
    final static public String EM_XML_CALENDAR_ENTRY_TIME_ZONE = "time_zone";
    final static public String EM_XML_CALENDAR_ENTRY_ALL_DAY = "all_day";
    final static public String EM_XML_CALENDAR_ENTRY_URL = "url";
    final static public String EM_XML_CALENDAR_ENTRY_ORGANIZER = "organizer";

    final static public String EM_XML_XML_TRUE = "true";
    final static public String EM_XML_XML_FALSE = "false";

    final static public String EM_XML_CALENDAR_ENTRY_ALARM = "alarm";
    final static public String EM_XML_CALENDAR_ENTRY_RRULE = "rrule";
    final static public String EM_XML_CALENDAR_ENTRY_ATTENDEE = "attendee";
    final static public String EM_XML_CALENDAR_CREATION_DATE = "creation_date";
    final static public String EM_XML_CALENDAR_LAST_MODIFIED_DATE = "last_modified_date";
    final static public String EM_XML_CALENDAR_EXTERNAL_UID = "external_uid";

    final static public String EM_XML_FILE_SIZE = "size";
    final static public String EM_XML_FILE_NAME = "file_name";
    final static public String EM_XML_FILE_TYPE = "file_type";
    final static public String EM_XML_FILE_TOTAL_MEDIA_SIZE = "total_media_size";

    final static public String EM_XML_WIFI_DIRECT_GROUP_DETAILS = "wifi_direct_group_details";
    final static public String EM_XML_WIFI_NETWORK_SSID = "network_ssid";
    final static public String EM_XML_WIFI_NETWORK_PASS = "network_passphrase";
    final static public String EM_XML_WIFI_SERVER_IP4_ADDRESS = "server_ip4_address";
    final static public String EM_XML_WIFI_SERVER_IP6_ADDRESS = "server_ip6_address";
    final static public String EM_XML_WIFI_SERVER_PORT = "server_port";

    final static public String EM_XML_RELATIVE_PATH = "relative_path";
    public static final String EM_XML_RETRY = "retry";

    final static public String EM_XML_NOTE_ENTRY = "note";
    final static public String EM_XML_NOTE_CREATION_DATE = "creation_date";
    final static public String EM_XML_NOTE_TITLE = "title";
    final static public String EM_XML_NOTE_MODIFICATION_DATE = "modification_date";
    final static public String EM_XML_NOTE_DATA = "data";
    final static public String EM_XML_NOTE_SUMMARY = "summary";
    final static public String EM_XML_NOTE_FOLDER = "folder";
    final static public String EM_XML_NOTE_DUE_DATE = "due_date";
    final static public String EM_XML_NOTE_REMINDER_DATE = "reminder_date";
    final static public String EM_XML_NOTE_COMPLETE = "complete";

    final static public String EM_XML_SMS = "sms";
    final static public String EM_XML_SMS_ENTRY = "sms_message";

    final static public String EM_XML_SMS_ENTRY_FOLDER = "folder";
    final static public String EM_XML_SMS_ENTRY_FOLDER_INBOX = "inbox";
    final static public String EM_XML_SMS_ENTRY_FOLDER_SENT = "sent";
    final static public String EM_XML_SMS_ENTRY_FOLDER_DRAFT = "draft";

    final static public String EM_XML_SMS_ENTRY_DEVICE_DATE = "device_date";
    final static public String EM_XML_SMS_ENTRY_NETWORK_DATE = "network_date";
    final static public String EM_XML_SMS_ENTRY_DATA = "data";
    final static public String EM_XML_SMS_READ = "read";
    final static public String EM_XML_SMS_ENTRY_SENDER = "sender";
    final static public String EM_XML_SMS_ENTRY_ADDRESSEE = "addressee";

    final static public String EM_XML_EMAIL_ACCOUNT_ADDRESS = "email_account_address";

    final static public String EM_BACKUP_FOLDER_NAME = "content-transfer";

    public static final String EM_XML_DESTINATION_INFO_SOFTWARE_VERSION_ID = "software-version-id";
    public static final String EM_DEVICE_INFO_FILENAME = "content-transfer.deviceinfo";

    public static final String EM_BACKUP_FINISHED_FILE = "backup-finished.xml";
    public static final String EM_BACKUP_STARTED_FILE = "backup-started.xml";

    public static final String EM_VERSION_CHECK_SUPPORTED_VERSION = "supported_version";
    public static final String EM_VERSION_CHECK_MINIMUM_SUPPORTED_VERSION = "minimum_supported_version";

    public static final String EM_XML_DEVICE_TYPE = "device_type";
    public static final String EM_DEVICE_TYPE_ANDROID = "android";
    public static final String EM_DEVICE_TYPE_IOS = "ios";
    public static final String EM_DEVICE_TYPE_UNKNOWN = "unknown";
    // public static final String EM_DEVICE_TYPE_IOS = "android";
    // public static final String EM_DEVICE_TYPE_BBOS = "android";
    // public static final String EM_DEVICE_TYPE_BB10 = "android";
    // public static final String EM_DEVICE_TYPE_WINDOWS_PHONE = "windows_phone";
    public static final String CALLLOG_INCOMING = "INCOMING";
    public static final String CALLLOG_OUTGOING = "OUTGOING";
    public static final String CALLLOG_MISSED = "MISSED";
    public static final String CALLLOG_REJECTED = "REJECTED";
    public static final String CALLLOG_BLOCKED = "BLOCKED";
    public static final String CALLLOG_VOICEMAIL = "VOICEMAIL";



    public static final String EM_XML_SETTINGS = "settings";
    public static final String SETTINGS_WALLPAPER = "wallpaper";
    public static final String SETTINGS_SCREEN_TIMEOUT = "screentimeout";
    public static final String SETTINGS_BRIGHTNESS = "brightness";
    public static final String SETTINGS_RINGMODE="ringmode";


    public static final String EM_XML_NAME = "name";
    public static final String EM_XML_VALUE = "value";


    public static final ArrayList<String> SETTINGS_LIST= new ArrayList<String>(Arrays.asList(SETTINGS_WALLPAPER,
            SETTINGS_SCREEN_TIMEOUT,SETTINGS_BRIGHTNESS,SETTINGS_RINGMODE));

    public static final HashMap<String,Integer> DATATYPE_MAP=new HashMap<>();

    public static final int WIRELESS_TRANSFER = 0;
    public static final int EXTERNAL_STORAGE_SDCARD = 1;
    public static final int EXTERNAL_STORAGE_USB = 2;
    public static final int OPERATION_TYPE_BACKUP = 1;
    public static final int OPERATION_TYPE_RESTORE = 2;

    public static final String OPERATION_TYPE = "operation_type";
    public static final String TRANSFER_TYPE = "transfer_type";


    static{
        DATATYPE_MAP.put(EMStringConsts.DATA_TYPE_CONTACTS,EMDataType.EM_DATA_TYPE_CONTACTS);
        DATATYPE_MAP.put(EMStringConsts.DATA_TYPE_CALENDAR,EMDataType.EM_DATA_TYPE_CALENDAR);
        DATATYPE_MAP.put(EMStringConsts.DATA_TYPE_SMS_MESSAGES,EMDataType.EM_DATA_TYPE_SMS_MESSAGES);
        DATATYPE_MAP.put(EMStringConsts.DATA_TYPE_CALL_LOGS,EMDataType.EM_DATA_TYPE_CALL_LOGS);
        DATATYPE_MAP.put(EMStringConsts.DATA_TYPE_SETTINGS,EMDataType.EM_DATA_TYPE_SETTINGS);
        DATATYPE_MAP.put(EMStringConsts.EM_XML_PHOTOS,EMDataType.EM_DATA_TYPE_PHOTOS);
        DATATYPE_MAP.put(EMStringConsts.EM_XML_VIDEO,EMDataType.EM_DATA_TYPE_VIDEO);
        DATATYPE_MAP.put(EMStringConsts.EM_XML_MUSIC,EMDataType.EM_DATA_TYPE_MUSIC);
        DATATYPE_MAP.put(EMStringConsts.EM_XML_APP,EMDataType.EM_DATA_TYPE_APP);
        DATATYPE_MAP.put(EMStringConsts.EM_XML_DOCUMENTS,EMDataType.EM_DATA_TYPE_DOCUMENTS);
    }



}

