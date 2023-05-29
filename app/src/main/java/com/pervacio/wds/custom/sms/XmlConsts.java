/*************************************************************************
 * 
 * Media Mushroom Limited CONFIDENTIAL
 * __________________
 * 
 *  Copyright 2014 Media Mushroom Limited
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Media Mushroom Limited and its suppliers,
 * if any. 
 *
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Media Mushroom Limited.
 */

package com.pervacio.wds.custom.sms;

class XmlConsts {
	final static String COMMON_ROOT_ELEMENT = "root";
	
	final static String MEDIA_IMAGE_ELEMENT = "image";
	final static String MEDIA_MUSIC_ELEMENT = "music";
	final static String MEDIA_VIDEO_ELEMENT = "video";
	
//	final static String MEDIA_SOURCE_FILE_PATH = "source_filepath";
	final static String MEDIA_TITLE = "title";
	final static String MEDIA_ALBUM = "album";
	final static String MEDIA_ARTIST = "artist";
	final static String MEDIA_FILE_SIZE = "size";
	final static String MEDIA_DATE = "date";
	
	final static String FILE_ELEMENT = "file";
	final static String FILE_SIZE = "size";
	final static String FILE_SOURCE_PATH = "source_filepath";
//	final static String FILE_DESTINATION_PATH = "destination_filepath";
	final static String FILE_NAME = "filename";
	
//	final static String MAIN_ADDRESS_BOOK_ELEMENT = "address_book";

//	final static String PERSON_ELEMENT = "person";

	final static String FIRST_NAME_ELEMENT = "first_name";
	final static String MIDDLE_NAME_ELEMENT = "middle_name";
	final static String LAST_NAME_ELEMENT = "last_name";
	final static String ORGANIZATION_ELEMENT = "organization";
	final static String PHOTO_ELEMENT = "photo";
	final static String BIRTHDAY_ELEMENT = "birthday";
	final static String ANNIVERSARY_ELEMENT = "anniversary";
	final static String NICK_NAME_ELEMENT = "nickname";
	final static String PHONETIC_NAME_ELEMENT = "phonetic_name";
//	final static String OTHER_ELEMENT = "other";

	
	final static String TYPE_ATTRIBUTE = "type";
	final static String VALUE_ATTRIBUTE = "value";

	final static String CONTACT_TYPE_HOME = "home";
	final static String CONTACT_TYPE_WORK = "work";
	final static String CONTACT_TYPE_HOMEPAGE = "homepage";
	final static String CONTACT_TYPE_MAIN = "main";
	final static String CONTACT_TYPE_MOBILE = "mobile";

//	final static String PROPERTY_UNKNOWN = "unknown";
	final static String PROPERTY_EMAIL = "email";
	final static String PROPERTY_PHONE = "phone";
	final static String PROPERTY_ADDRESS= "address";
	final static String PROPERTY_URL = "url";

//	final static String ENTRY_UNKNOWN = "unknown";
	final static String ENTRY_COUNTRY = "country"; // Country
	final static String ENTRY_STREET = "street"; // Street
	final static String ENTRY_ZIP = "zip"; // ZIP
	final static String ENTRY_CITY = "city"; // City
	final static String ENTRY_COUNTRY_CODE = "country_code"; // CountryCode
	final static String ENTRY_STATE = "state"; // State

//	final static String MAIN_MESSAGES_ELEMENT = "messages";
	final static String SENT_MESSAGE_ELEMENT = "sent_message";
	final static String RECEIVED_MESSAGE_ELEMENT = "received_message";
	final static String DRAFT_MESSAGE_ELEMENT = "draft_message";
	final static String OUTBOX_MESSAGE_ELEMENT = "outbox_message";
	final static String FAILED_MESSAGE_ELEMENT = "failed_message";
	final static String QUEUED_MESSAGE_ELEMENT = "queued_message";

	final static String MESSAGES_ADDRESS = "address";
	final static String MESSAGES_DATE = "date";
	final static String MESSAGES_TEXT = "text";
	final static String MESSAGES_READ = "read";
	
	final static String MESSAGES_ATTACHMENT = "attachment";
	final static String MESSAGES_MIME_TYPE = "mime_type"; // attribute
	
//	final static String MAIN_NOTES_ELEMENT = "notes";
	final static String NOTES_ENTRY = "note";

//	final static String NOTE_CREATION_DATE = "creation_date";
//	final static String NOTE_TITLE = "title";
//	final static String NOTE_MODIFICATION_DATE = "modification_date";
//	final static String NOTE_DATA = "data";
//	final static String NOTE_SUMMARY = "summary";
	
   public static final String CALENDAR_ENTRY_TITLE = "title";
   public static final String CALENDAR_ENTRY_LOCATION = "location";
   public static final String CALENDAR_ENTRY_DESCRIPTION = "description";
   public static final String CALENDAR_ENTRY_START_DATE = "start_date";
   public static final String CALENDAR_ENTRY_END_DATE = "end_date";
   public static final String CALENDAR_ENTRY_TIME_ZONE = "time_zone";
   public static final String CALENDAR_ENTRY_ALL_DAY = "all_day";
   public static final String CALENDAR_ENTRY_URL = "url";
   public static final String CALENDAR_ENTRY_RRULE = "rrule";

   public static final String XML_TRUE = "true";
   public static final String XML_FALSE = "false";
   
   public static final String CALENDAR_ENTRY_FREQUENCY = "frequency";
   public static final String CALENDAR_ENTRY_FREQUENCY_ONCE = "once";
//   public static final String CALENDAR_ENTRY_FREQUENCY_DAILY = "DAILY";
//   public static final String CALENDAR_ENTRY_FREQUENCY_WEEKLY = "WEEKLY";
//   public static final String CALENDAR_ENTRY_FREQUENCY_MONTHLY = "MONTHLY";
//   public static final String CALENDAR_ENTRY_FREQUENCY_YEARLY = "YEARLY";
   public static final String CALENDAR_ENTRY_INTERVAL = "interval";
   public static final String CALENDAR_ENTRY_REPEAT_END_DATE = "repeat_end_date";
   public static final String CALENDAR_ENTRY_ALARM = "alarm";

   //Availability
   public static final String CALENDAR_ENTRY_AVAILABILITY = "availability";
   //Access Level
   public static final String CALENDAR_ENTRY_ACCESS_LEVEL = "access_level";


	final static String TYPE_ASSISTANT = "assistant";
	final static String TYPE_BROTHER = "brother";
	final static String TYPE_CHILD = "child";
	final static String TYPE_DOMESTIC_PARTNER = "domestic partner";
	final static String TYPE_FATHER = "father";
	final static String TYPE_FRIEND = "friend";
	final static String TYPE_MANAGER = "manager";
	final static String TYPE_MOTHER = "mother";
	final static String TYPE_PARENT = "parent";
	final static String TYPE_PARTNER = "partner";
	final static String TYPE_REFERRED_BY = "referred by";
	final static String TYPE_RELATIVE = "relative";
	final static String TYPE_SISTER = "sister";
	final static String TYPE_SPOUSE = "spouse";



}
