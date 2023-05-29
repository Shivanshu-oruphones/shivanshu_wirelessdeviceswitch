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

public class CMDStringConsts {
	final static public String EM_SERVICE_NAME = "_easymigrate._tcp.";
	final static public String EM_COMMAND_TEXT_HANDSHAKE = "HANDSHAKE";
	final static public String EM_COMMAND_TEXT_YOU_ARE_TARGET = "YOU_ARE_TARGET";
	final static public String EM_COMMAND_TEXT_YOU_ARE_SOURCE = "YOU_ARE_SOURCE";
	final static public String EM_COMMAND_TEXT_PIN_REQUEST = "PIN_REQUEST";
	final static public String EM_COMMAND_TEXT_PIN_OK = "PIN_OK";
	final static public String EM_COMMAND_TEXT_ADD_CONTACTS = "ADD_CONTACTS";
	final static public String EM_COMMAND_TEXT_ADD_CALENDAR = "ADD CALENDAR";
	final static public String EM_COMMAND_TEXT_ADD_FILE = "ADD_FILE";
	final static public String EM_COMMAND_TEXT_QUIT = "QUIT";

	final static public String EM_TEXT_RESPONSE_OK = "OK";
	     
	final static public String EM_XML_ROOT = "root";
	     
	final static public String EM_XML_DEVICE_INFO = "device_info";
	     
	final static public String EM_XML_DEVICE_NAME = "name";
	final static public String EM_XML_DEVICE_PORT = "port";
	final static public String EM_XML_CAN_ADD = "can_add";
	final static public String EM_XML_SUPPORTS_ROLE = "supports_role";
	final static public String EM_XML_SERVICE_NAME = "service_name";
	     
	final static public String EM_XML_CONTACTS = "contacts";
	final static public String EM_XML_CALENDAR = "calendar";
	final static public String EM_XML_FILE = "file";
	final static public String EM_XML_PHOTOS = "photos";
	final static public String EM_XML_VIDEO = "video";

	final static public String EM_XML_ROLE_MIGRATION_SOURCE = "migration_source";
	final static public String EM_XML_ROLE_MIGRATION_TARGET = "migration_target";

	final static public String EM_XML_CONTACT_ENTRY = "contact";
    final static public String EM_XML_CONTACT_VCARD_ENTRY = "vcard";
	final static public String EM_XML_CALENDAR_ENTRY = "calendar_entry";

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

    final static public String EM_PHOTOS_FOLDER_NAME    = "photos";
    final static public String EM_VIDEOS_FOLDER_NAME    = "videos";
    final static public String EM_DOCUMENTS_FOLDER_NAME = "documents";
    final static public String EM_MUSIC_FOLDER_NAME     = "music";

	final static public String EM_CONTACTS_FILE_NAME    = "contacts.xml";
	final static public String EM_CALENDAR_FILE_NAME    = "calendar.xml";
	final static public String EM_SMS_FILE_NAME         = "sms.xml";
	final static public String EM_NOTES_FILE_NAME       = "notes.xml";
	final static public String EM_TASKS_FILE_NAME       = "tasks.xml";
	final static public String EM_ACCOUNTS_FILE_NAME    = "accounts.xml";
}
