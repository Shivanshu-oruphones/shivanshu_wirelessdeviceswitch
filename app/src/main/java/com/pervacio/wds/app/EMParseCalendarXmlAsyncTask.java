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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.text.format.Time;
import android.util.Log;

import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;

import java.util.ArrayList;

public class EMParseCalendarXmlAsyncTask extends EMParseDataInThread {
    private int mNumberOfEntries;
    private String mCalendarId;
    private String calendarAccountName = EMConfig.CALENDAR_ACCOUNT_NAME;
    private String calendarDisplayName = EMConfig.CALENDAR_DISPLAY_NAME;
	static EMGlobals emGlobals = new EMGlobals();
	private int parseXml(boolean aCountOnly)
	{
	    int numberOfEntriesRet = 0;

		if (BuildConfig.FLAVOR.equalsIgnoreCase("privatelabel")) {
			calendarAccountName = "MobileCopy";
			calendarDisplayName = "MobileCopy";
		}


	    EMXmlPullParser pullParser = new EMXmlPullParser();

	    EMXmlPullParser.EMXmlNodeType nodeType;
	    try {
	    	pullParser.setFilePath(mFilePath);
	    	nodeType = pullParser.readNode();

		    int currentItem = 0;

		    if (!aCountOnly)
		    {
				mCalendarId = getOrCreateLocalCalendar(mContext);
		    }
		    long lastUpdateTime = 0;
			long uiUpdateIntervalInMs = 3*1000;
			while (nodeType != EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ROOT_ELEMENT) // While there is no error and we haven't reached the last node
			{
				/*if (isCancelled())
					return 0;*/

				if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_START_ELEMENT) {
					String elementName = pullParser.name();

					if (elementName.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR_ENTRY)) {
						numberOfEntriesRet++;

						if (!aCountOnly) {
							++currentItem;
							if ((System.currentTimeMillis() - lastUpdateTime) > uiUpdateIntervalInMs) {
								lastUpdateTime = System.currentTimeMillis();
								EMProgressInfo progress = new EMProgressInfo();
								progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_INCOMING_DATA;
								progress.mDataType = EMDataType.EM_DATA_TYPE_CALENDAR;
								progress.mTotalItems = mNumberOfEntries;
								progress.mCurrentItemNumber = currentItem;
								updateProgressFromWorkerThread(progress);
								DLog.log("Processing Calender >> " + currentItem);
							}

							if (!aCountOnly) {
								mTitle = "";
								mEndDate = "";
								mStartDate = "";
								mLocation = "";
								mDescription = "";
								mTimeZone = "";
								mAllDay = false;
								mUrl = "";
								mFrequency = "";
								mInterval = "";
								mRepeatEndDate = "";
								mAlarms = new ArrayList<String>();
								mRrule = null;
							}
						}

						boolean endOfCalendarEntry = false;

						int calendarEntryXmlLevel = 0;

						String dataTypeName = "";

						while (!endOfCalendarEntry) {
							nodeType = pullParser.readNode();

							if (calendarEntryXmlLevel == 0) {
								if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ELEMENT) {
									endOfCalendarEntry = true;

									int error = 0;

									// Add the calendar event to the calendar
									if (!aCountOnly) {
										addCurrentDetailsToCalendar();
									}
								} else if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_START_ELEMENT) {
									// We have found a data element, so save the type
									dataTypeName = pullParser.name();
									calendarEntryXmlLevel += 1;
								}
							}

							if (calendarEntryXmlLevel == 1) {
								String value = pullParser.value();

								if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ELEMENT) {
									dataTypeName = "";
									calendarEntryXmlLevel -= 1;
								} else if ((nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT)
										&& (!aCountOnly)
										&& (value != null)) {
									handleCalendarElement(dataTypeName, value);
								}
							}
						}
					}
				}

				nodeType = pullParser.readNode();
			}
		} catch (Exception ex) {
			EMMigrateStatus.setTotalFailure(EMDataType.EM_DATA_TYPE_CALENDAR);
		}
		if (!aCountOnly) {
			EMProgressInfo progress = new EMProgressInfo();
			progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_INCOMING_DATA;
			progress.mDataType = EMDataType.EM_DATA_TYPE_CALENDAR;
			progress.mTotalItems = mNumberOfEntries;
			progress.mCurrentItemNumber = mNumberOfEntries;
			updateProgressFromWorkerThread(progress);
			DLog.log("Processing Calender Done >> ");
		}
		return numberOfEntriesRet;
	}

	static final String SEMCTAG = "SEMC";
	// To be overridden by the derived task
	// This function is run in the new thread
	@Override
	public void runTask()
	{
		try {
			Log.i(SEMCTAG, "Adding Semc calendar");
			SemcCalendarUtil.insertInitialzationRecordForSemcCalendar(emGlobals.getmContext());
		} catch (Exception e) {
			DLog.log("Exception adding Semc calendar", e);
		}

		DashboardLog.getInstance().setCalendarStartTime(CommonUtil.getInstance().getBackupStartedTime());
        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.CALENDAR, -1, -1, Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, true);
	    mNumberOfEntries = parseXml(true);
	    parseXml(false);
		DashboardLog.getInstance().setCalendarEndTime(System.currentTimeMillis());
        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.CALENDAR, mNumberOfEntries, -1, Constants.TRANSFER_STATUS.SUCCESS, Constants.TRANSFER_STATE.COMPLETED, true);

		Log.i(SEMCTAG, "Done Adding Semc calendar. Count: " + getCalendarcount(emGlobals.getmContext()));
	}

	public static int getCalendarcount(Context mContext){
		int calendarCount = 0;
		try {
			Cursor calendarCursor = null;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				Log.e(SEMCTAG, "ICS detected");
				// ICS: query for CalendarContract.Events.TITLE=mTitle and
				// CalendarContract.Events.DTSTART=startMillis
				String permission = "android.permission.READ_CALENDAR";
				int res = mContext.checkCallingOrSelfPermission(permission);
				if(res == PackageManager.PERMISSION_GRANTED){
					calendarCursor = mContext.getContentResolver().query(
							CalendarContract.Events.CONTENT_URI, null, // Column
							// projection
							null, // WHERE
							null, // WHERE arguments
							null); // Order-by
				}

			} else {
				Uri calendarUri;
				// Gingerbread: query for title=mTitle dtstart=startMillis
				if (Build.VERSION.SDK_INT >= 8) {
					// Log.d(SEMCTAG,
//							"Trying: content://com.android.calendar/events");
					calendarUri = Uri
							.parse("content://com.android.calendar/events");
				} else {
					// Log.d(SEMCTAG, "Trying: content://calendar/events");
					calendarUri = Uri.parse("content://calendar/events");
				}

				calendarCursor = mContext.getContentResolver().query(
						calendarUri, null, // Column projection
						null, // WHERE
						null, // WHERE arguments
						null); // Order-by

				if (calendarCursor == null) {
					// Log.d(SEMCTAG, "Got null calendarCursor");
				} else {
					// Log.d(SEMCTAG, "Got a calendarCursor");
				}
			}

			if (calendarCursor != null) {
				calendarCount = calendarCursor.getCount();
				calendarCursor.close();
			}

		} catch (Exception ex) {
			// Log.d(SEMCTAG, "Method : Calendar Count Exception: ", ex);
		}
		return calendarCount;
	}

	private void validateTimezone() {
		if (mTimeZone == null || mTimeZone.isEmpty()) {
			if (mAllDay)
				mTimeZone = "UTC";
			else
				mTimeZone = Time.getCurrentTimezone();
		}
		if (mAllDay && !"UTC".equalsIgnoreCase(mTimeZone)) {
			mStartDate = String.valueOf(Long.parseLong(mStartDate) + (EMUtility.getTimeZoneDiff(mTimeZone, "UTC") / 1000));
			mEndDate = String.valueOf(Long.parseLong(mEndDate) + (EMUtility.getTimeZoneDiff(mTimeZone, "UTC") / 1000));
			mTimeZone = "UTC";
		}
	}


	private void addCurrentDetailsToCalendar() {
        // TODO: check for any existing matching calendar entries
		try
		{
			validateTimezone();
			long startMillis = 0;
			try {
				startMillis = Long.valueOf(mStartDate) * 1000; 
			} catch (Exception ex)
			{
				// Do nothing
			}
			
			long endMillis = 0;
			try {
				endMillis = Long.valueOf(mEndDate) * 1000; 
			} catch (Exception ex)
			{
				// Do nothing
			}  
			
			long secondsDuration = (endMillis - startMillis) / 1000;
			
			if (secondsDuration < 0)
				secondsDuration = 0;
			
			String durationString = "P" + Long.toString(secondsDuration) + "S";
			
			String rrule = null;
			long repeatUntilMillis = 0;

			// If there's no rrule in the xml then try to make one
			if (mRrule == null) {
				// Ignore
			} else {
				// If we have an explicit rrule then use it
				String[] rruleComponents = mRrule.split(";");
				rrule = "";
				for(String rruleComponent : rruleComponents) {
					if (!rruleComponent.startsWith("START=")) { // Strip out the START= field if there is one (it's invalid and the Android parser rejects it)
						if (!rrule.isEmpty())
							rrule += ";";

						rrule += rruleComponent;
					}
				}
			}
				
			ContentResolver cr = mContext.getContentResolver();
			ContentValues values = new ContentValues();
								
			Cursor cur;
			String[] args = {mTitle, Long.toString(startMillis),Integer.toString(1)};
			if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH) {					
				// ICS: query for CalendarContract.Events.TITLE=mTitle and CalendarContract.Events.DTSTART=startMillis
				String where = CalendarContract.Events.TITLE + "=? AND " + CalendarContract.Events.DTSTART + "=? AND "+CalendarContract.Events.DELETED +"!=?";

				cur = mContext.getContentResolver().query(CalendarContract.Events.CONTENT_URI, 
						null, // Column projection
						where, // WHERE
						args, // WHERE arguments
						null); // Order-by
			}
			else {
				Uri calendarUri;
				// Gingerbread: query for title=mTitle dtstart=startMillis
			    if (Build.VERSION.SDK_INT >= 8 ) {
			    	calendarUri = Uri.parse("content://com.android.calendar/events");
			    } else {
			    	calendarUri = Uri.parse("content://calendar/events");
			    }

				String where = "title=? AND " + "dtstart=?";

				cur = mContext.getContentResolver().query(calendarUri, 
						null, // Column projection
						where, // WHERE
						args, // WHERE arguments
						null); // Order-by
			}
			
			
			Uri calendarUri;
			if ((cur != null) && (cur.getCount() == 0)) { // Don't add a calendar entry if there's already a matching one there

				if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH) {					
					// TODO: set transparency?
					values.put(CalendarContract.Events.DTSTART, startMillis);
					values.put(CalendarContract.Events.TITLE, mTitle);
					values.put(CalendarContract.Events.DESCRIPTION, mDescription);
					values.put(CalendarContract.Events.CALENDAR_ID, mCalendarId);
					values.put(CalendarContract.Events.EVENT_LOCATION, mLocation);
					values.put(CalendarContract.Events.EVENT_TIMEZONE, mTimeZone);
					
					if (mAlarms.size() != 0)
						values.put(CalendarContract.Events.HAS_ALARM, true);
					
					// TODO: ensure timezone is UTC for all-day events
					if (mAllDay) {
						values.put(CalendarContract.Events.ALL_DAY, "1");
					}

					if (rrule == null) {
						values.put(CalendarContract.Events.DTEND, endMillis);
					}
					else {
						values.put(CalendarContract.Events.DURATION, durationString); // TODO: do for gingerbread?
						values.put(CalendarContract.Events.RRULE, rrule);
						if (repeatUntilMillis == 0)
						{
							values.putNull(CalendarContract.Events.LAST_DATE); // TODO: do for gingerbread?
						}
						else
							values.put(CalendarContract.Events.LAST_DATE, repeatUntilMillis);
					}

					calendarUri = cr.insert(CalendarContract.Events.CONTENT_URI, values);				
				}
				else
				{
				    values.put("calendar_id", mCalendarId);
				    values.put("title", mTitle);
				    values.put("description", mDescription);
				    values.put("eventLocation", mLocation);
				    values.put("dtstart", startMillis);
				    values.put("dtend", endMillis);
				    values.put("duration", durationString);
				    values.put("eventTimezone", mTimeZone);
				    
					if (mAllDay)
						values.put("allDay", 1);

					if (rrule != null)
					{
						values.put("rrule", rrule);
						if (repeatUntilMillis == 0)
						{
							values.putNull(CalendarContract.Events.LAST_DATE); // TODO: do for gingerbread?
						}
						else
							values.put(CalendarContract.Events.LAST_DATE, repeatUntilMillis);
					}
					
					values.put("visibility", 0);
				    values.put("transparency", 1);
				    
				    if (mAlarms.size() != 0)
				    	values.put("hasAlarm", 1);
				    else
				    	values.put("hasAlarm", 0);
				    
				    Uri eventUri;
				    if (Build.VERSION.SDK_INT >= 8 ) {
				    	eventUri = Uri.parse("content://com.android.calendar/events");
					} else {
				    	eventUri = Uri.parse("content://calendar/events");
				    }
				    
				    // Log.e(DASHBOARD_TAG, mTitle);
				    // Log.e(DASHBOARD_TAG, Long.toString(endMillis));
				    
				    calendarUri = cr.insert(eventUri, values);
				}
				
				String idString = calendarUri.getLastPathSegment();
				try {
					int idNumber = Integer.parseInt(idString);

				    for (String alarmText : mAlarms) {
						int alarmXmlTime = Integer.parseInt(alarmText); // Seconds prior to event
						int androidAlarmTime = 0 - alarmXmlTime;
						androidAlarmTime = androidAlarmTime / 60;
						if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH) {					
							ContentValues alarmValues = new ContentValues();
							alarmValues.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
							alarmValues.put(CalendarContract.Reminders.EVENT_ID, idNumber);
							alarmValues.put(CalendarContract.Reminders.MINUTES, androidAlarmTime);
							Uri alarmUrl = cr.insert(CalendarContract.Reminders.CONTENT_URI, alarmValues);
						}
						else
						{
						    Uri remindersUri;
						    if (Build.VERSION.SDK_INT >= 8 ) {
						    	remindersUri = Uri.parse("content://com.android.calendar/reminders");
						    } else {
						    	remindersUri = Uri.parse("content://calendar/reminders");
						    }
						    
						    ContentValues alarmValues = new ContentValues();
						    alarmValues.put( "event_id", idNumber);
						    alarmValues.put( "method", 1 );
						    alarmValues.put( "minutes", androidAlarmTime );
					        cr.insert( remindersUri, alarmValues );
						}
				    }
				} catch (Exception ex)
				{
					DLog.log("Exception adding alarm", ex);
				}
			}else{
				DLog.log("Skipping calendar event as its exists already");
			}

			EMMigrateStatus.addItemTransferred(EMDataType.EM_DATA_TYPE_CALENDAR);
			cur.close();
		}
		catch (Exception ex) {
			EMMigrateStatus.addItemNotTransferred(EMDataType.EM_DATA_TYPE_CALENDAR);
			DLog.log(ex);
		}
	}
	
	private void handleCalendarElement(String aElementName, String aValue) {
		try {
			// Add the calendar entry to the content provider
			if (aElementName.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR_ENTRY_TITLE)) {
				mTitle = aValue;
			}
			
			if (aElementName.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR_ENTRY_LOCATION)) {
				mLocation = aValue;
			}
			
			if (aElementName.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR_ENTRY_DESCRIPTION)) {
				mDescription = aValue;
			}
			
			if (aElementName.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR_ENTRY_START_DATE)) {
				mStartDate = aValue;
			}
			
			if (aElementName.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR_ENTRY_END_DATE)) {
				mEndDate = aValue;
			}
			
			if (aElementName.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR_ENTRY_TIME_ZONE)) {
				mTimeZone = aValue;
			}
			
			if (aElementName.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR_ENTRY_ALL_DAY)) {
				if (aValue.compareToIgnoreCase(EMStringConsts.EM_XML_XML_TRUE) == 0)
					mAllDay = true;
			}
			
			if (aElementName.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR_ENTRY_URL)) {
				mUrl = aValue;
			}
			
			/*// No longer needed - we always use the RRULE now
			if (aElementName.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR_ENTRY_FREQUENCY)) {
				mFrequency = mLevel3Text;
			}
			
			if (aElementName.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR_ENTRY_INTERVAL)) {
				mInterval = mLevel3Text;
			}
			
			if (aElementName.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR_ENTRY_REPEAT_END_DATE)) {
				mRepeatEndDate = mLevel3Text;
			}	
			*/		
			
			if (aElementName.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR_ENTRY_ALARM)){
				mAlarms.add(aValue);
			}
			
			if (aElementName.equalsIgnoreCase(EMStringConsts.EM_XML_CALENDAR_ENTRY_RRULE)){
				mRrule = aValue;
			}

		} catch (Exception ex)
		{
			// Ignore any errors
		}
	}

	 String getOrCreateLocalCalendar(Context aContext) {
		String calendarId = null;

		Uri calendarUri;
		ContentResolver cr = aContext.getContentResolver();
		Cursor calendarCursor;

		String calendarDisplayName = this.calendarDisplayName;

		int accountNameColumn = -1;

		try {
			cr = aContext.getContentResolver();
			if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				calendarUri = CalendarContract.Calendars.CONTENT_URI;
				String whereQuery = CalendarContract.Events.DELETED + " != 1";
				String[] projection = new String[] {
						CalendarContract.Calendars._ID,
						CalendarContract.Calendars.ACCOUNT_NAME};

				calendarCursor = cr.query(calendarUri, projection, whereQuery, null, null);
				accountNameColumn = calendarCursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME);
//				calendarCursor = cr.query(calendarUri, projection, null, null, null);
			}
			else
			{
				if (Build.VERSION.SDK_INT >= 8 ) {
					calendarUri = Uri.parse("content://com.android.calendar/calendars");
				} else {
					calendarUri = Uri.parse("content://calendar/calendars");
				}
				String[] projection = new String[] {"_ID", "_sync_account"};

				calendarCursor = cr.query(calendarUri, projection, null, null, null);
				accountNameColumn = calendarCursor.getColumnIndex("_sync_account");
			}

			if (calendarCursor != null)
			{
				if (calendarCursor.moveToFirst()) {
					do {
						int calendarColumn = calendarCursor.getColumnIndexOrThrow("_ID");

						String fetchedAccountName = calendarCursor.getString(accountNameColumn);

						if (fetchedAccountName.equals(calendarAccountName))
							calendarId = calendarCursor.getString(calendarColumn);
					} while (calendarCursor.moveToNext());
				}

				calendarCursor.close();
			}
		}
		catch (Exception ex) {
			// Couldn't get existing calendar, so create a new one
		}

		if (calendarId == null) {
			ContentValues values = new ContentValues();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				String accountType = CalendarContract.ACCOUNT_TYPE_LOCAL;
				values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, calendarDisplayName);
				values.put(CalendarContract.Calendars.ACCOUNT_NAME, calendarAccountName);
				values.put(CalendarContract.Calendars.ACCOUNT_TYPE, accountType);
				values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
				values.put(CalendarContract.Calendars.OWNER_ACCOUNT, calendarAccountName);
				values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
				Uri uri = CalendarContract.Calendars.CONTENT_URI
						.buildUpon()
						.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
						.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, calendarAccountName)
						.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
						.build();
				calendarUri = cr.insert(uri, values);
				calendarId = calendarUri.getLastPathSegment();
			} else {
				if (Build.VERSION.SDK_INT >= 8) {
					calendarUri = Uri.parse("content://com.android.calendar/calendars");
				} else {
					calendarUri = Uri.parse("content://calendar/calendars");
				}

				// values.put(Calendar.Calendars.URL, calendarUrl);
				values.put("name", calendarAccountName);
				values.put("displayName", calendarDisplayName);
				values.put("_sync_account", calendarAccountName);
				values.put("_sync_account_type", "com.home.local_type");
				values.put("sync_events", 1);
				values.put("selected", 1);
				values.put("hidden", 0);
				values.put("organizerCanRespond", 0);
				values.put("color", 0xFF888888);
				values.put("timezone", Time.getCurrentTimezone());
				values.put("access_level", "700");
				values.put("ownerAccount", calendarAccountName);

				calendarUri = cr.insert(calendarUri, values);
				calendarId = calendarUri.getLastPathSegment();
			}
		}

		return calendarId;
	}
	
	
    String mTitle;
    String mEndDate;
    String mLocation;
    String mDescription;
    String mStartDate;
    String mTimeZone;
    boolean mAllDay;
    String mUrl;
    String mFrequency;
    String mInterval;
    String mRepeatEndDate;
    ArrayList<String> mAlarms;
    String mRrule;
    
    public static final String CALENDAR_ENTRY_FREQUENCY_ONCE = "once";
    
    private String TAG = "EMParseCalendarXmlAsyncTask";
}
