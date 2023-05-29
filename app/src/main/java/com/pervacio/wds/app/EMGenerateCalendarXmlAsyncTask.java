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
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EMGenerateCalendarXmlAsyncTask extends EMGenerateDataTask {
	EMXmlGenerator mXmlGenerator = new EMXmlGenerator();
	
	int mNumberOfEntries = 0;

	String mTempFilePath;

	private class Reminder {
		public int id;
		public int eid;
		public int seconds;
	}
	final List<Reminder> reminders = new ArrayList<Reminder>();
	
	void writeTextElement(String aName, String aText) throws IllegalArgumentException, IllegalStateException, IOException
	{
	    mXmlGenerator.startElement(aName);
	    if (aText != null)
	    {
	        mXmlGenerator.writeText(aText);
	    }
	    else
	    {
	        mXmlGenerator.writeText("");
	        DLog.log("Writing empty element");
	    }
	    
	    mXmlGenerator.endElement(aName);
	}

	void writeNumberElement(String aName, long aNumber) throws IllegalArgumentException, IllegalStateException, IOException
	{
	    mXmlGenerator.startElement(aName);
	    String numberText = Long.toString(aNumber); // [NSString stringWithFormat:@"%ld", number];
	    mXmlGenerator.writeText(numberText);
	    mXmlGenerator.endElement(aName);
	}

	void writeBoolElement(String aName, boolean aValue) throws IllegalArgumentException, IllegalStateException, IOException
	{
	    mXmlGenerator.startElement(aName);
	    
	    if (aValue)
	    {
	        mXmlGenerator.writeText(EMStringConsts.EM_XML_XML_TRUE);
	    }
	    else
	    {
	        mXmlGenerator.writeText(EMStringConsts.EM_XML_XML_FALSE);
	    }
	    
	    mXmlGenerator.endElement(aName);
	}

	String getValueForColumn(Cursor aCursor, int aColumn) {
		String value = "";
		try {
			value = aCursor.getString(aColumn);
		} catch (Exception ex) {
			// Ignore
		}

		if (value == null)
			value = "";

		return value;
	}

	// To be overridden by the derived task
	// This function is run in the new thread
	@Override
	public void runTask()
	{	    
		try {
		    mXmlGenerator.startDocument();
		    
			ContentResolver cr = EMUtility.Context().getContentResolver();
			ContentValues values = new ContentValues();
								
			Cursor calendarCursor;
			
			int titleColumn = -1;
			int startDateColumn = -1;
			int endDateColumn = -1;
			int durationColumn = -1;
			int descriptionColumn = -1;
			int timezoneColumn = -1;
			int locationColumn = -1;
			int hasAlarmColumn = -1;
			int rruleColumn = -1;
			int allDayEventColumn = -1;
			int repeatUntilDateColumn = -1;
			int eventIdColumn = -1;
			int deletedColumn = -1;
			int accountTypeColumn = -1;
			int isDirtyColumn = -1;
			
			Uri attendeeTableUri;
			String eventIdColumnName;
			String attendeeColumnName;
			String attendeeEmailColumnName;
			long lastUpdateTime = 0;
			long uiUpdateIntervalInMs = 3*1000;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                // ICS: query for CalendarContract.Events.TITLE=mTitle and CalendarContract.Events.DTSTART=startMillis
				String whereQuery = CalendarContract.Events.DELETED + " != 1";
                calendarCursor = EMUtility.Context().getContentResolver().query(CalendarContract.Events.CONTENT_URI,
                        null, // Column projection
						whereQuery, // WHERE
                        null, // WHERE arguments
                        null); // Order-by

                titleColumn = calendarCursor.getColumnIndex(CalendarContract.Events.TITLE);
                startDateColumn = calendarCursor.getColumnIndex(CalendarContract.Events.DTSTART);
                endDateColumn = calendarCursor.getColumnIndex(CalendarContract.Events.DTEND);
                durationColumn = calendarCursor.getColumnIndex(CalendarContract.Events.DURATION);
                descriptionColumn = calendarCursor.getColumnIndex(CalendarContract.Events.DESCRIPTION);
                timezoneColumn = calendarCursor.getColumnIndex(CalendarContract.Events.EVENT_TIMEZONE);
                locationColumn = calendarCursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION);
                hasAlarmColumn = calendarCursor.getColumnIndex(CalendarContract.Events.HAS_ALARM);
                rruleColumn = calendarCursor.getColumnIndex(CalendarContract.Events.RRULE);
                allDayEventColumn = calendarCursor.getColumnIndex(CalendarContract.Events.ALL_DAY);
                repeatUntilDateColumn = calendarCursor.getColumnIndex(CalendarContract.Events.LAST_DATE);
                eventIdColumn = calendarCursor.getColumnIndex(CalendarContract.Events._ID);
                deletedColumn = calendarCursor.getColumnIndex(CalendarContract.Events.DELETED);
                accountTypeColumn = calendarCursor.getColumnIndex(CalendarContract.Events.ACCOUNT_TYPE);
                isDirtyColumn = calendarCursor.getColumnIndex(CalendarContract.Events.DIRTY);

                attendeeTableUri = Attendees.CONTENT_URI;
                eventIdColumnName = Attendees.EVENT_ID;
                attendeeColumnName = Attendees.ATTENDEE_NAME;
                attendeeEmailColumnName = Attendees.ATTENDEE_EMAIL;
            } else {
                Uri calendarUri;
                // Gingerbread: query for title=mTitle dtstart=startMillis
                if (Build.VERSION.SDK_INT >= 8) {
                    calendarUri = Uri.parse("content://com.android.calendar/events");
                } else {
                    calendarUri = Uri.parse("content://calendar/events");
                }

                calendarCursor = EMUtility.Context().getContentResolver().query(calendarUri,
                        null, // Column projection
                        null, // WHERE
                        null, // WHERE arguments
                        null); // Order-by

                titleColumn = calendarCursor.getColumnIndex("title");
                startDateColumn = calendarCursor.getColumnIndex("dtstart");
                endDateColumn = calendarCursor.getColumnIndex("dtend");
                durationColumn = calendarCursor.getColumnIndex("duration");
                descriptionColumn = calendarCursor.getColumnIndex("description");

                timezoneColumn = calendarCursor.getColumnIndex("timezone");

                isDirtyColumn = calendarCursor.getColumnIndex("_sync_dirty");
                accountTypeColumn = calendarCursor.getColumnIndex("_sync_account_type");

                int alternativeTimezoneColumn = calendarCursor.getColumnIndex("eventTimezone");
                if (alternativeTimezoneColumn != -1)
                    timezoneColumn = alternativeTimezoneColumn;

                locationColumn = calendarCursor.getColumnIndex("eventLocation");
                hasAlarmColumn = calendarCursor.getColumnIndex("hasAlarm");
                rruleColumn = calendarCursor.getColumnIndex("rrule");
                allDayEventColumn = calendarCursor.getColumnIndex("allDay");

                eventIdColumn = calendarCursor.getColumnIndex("_id");

                if (Build.VERSION.SDK_INT >= 8) {
                    attendeeTableUri = Uri.parse("content://com.android.calendar/attendees");
                } else {
                    attendeeTableUri = Uri.parse("content://calendar/attendees");
                }

                eventIdColumnName = "event_id";
                attendeeColumnName = "attendeeName";
                attendeeEmailColumnName = "attendeeEmail";
            }

            int currentItemNumber = 1;

            createAlarmList();

            if (calendarCursor.moveToFirst()) {
                do {
                    String calendarHashInputString = getValueForColumn(calendarCursor, titleColumn)
                            + getValueForColumn(calendarCursor, startDateColumn)
                            + getValueForColumn(calendarCursor, endDateColumn)
                            + getValueForColumn(calendarCursor, durationColumn)
                            + getValueForColumn(calendarCursor, descriptionColumn)
                            + getValueForColumn(calendarCursor, timezoneColumn)
                            + getValueForColumn(calendarCursor, locationColumn)
                            + getValueForColumn(calendarCursor, rruleColumn)
                            + getValueForColumn(calendarCursor, allDayEventColumn);

                   /* String calendarHashString = EMUtility.md5(calendarHashInputString);

                    if (itemHasBeenPreviouslyTransferred(calendarHashString)) {
                        // TODO: log that we have skipped this entry?
                        DLog.log("Skipping previously trasferred calendar event");
                        continue;
                    } else {
                        addToPreviouslyTransferredItems(calendarHashString);
                    }*/


                    try {
                        if (isCancelled()) {
                            calendarCursor.close();
                            // TODO: remove temporary file
                            return;
                        }

                        // Skip deleted items
                        if (deletedColumn != -1 && calendarCursor.getInt(deletedColumn) == 1) {
                            continue;
                        }

                        boolean isDirty = true;
                        String accountType = null;

                        try {
                            String stringValue = calendarCursor.getString(isDirtyColumn);
                            if (stringValue.equalsIgnoreCase("0")) {
                                isDirty = false;
                            }
                        } catch (Exception ex) {
                            // Ignore if we can't get the isDirty value
                        }

                        try {
                            accountType = calendarCursor.getString(accountTypeColumn);
                        } catch (Exception ex) {
                            // Ignore if we can't get the accountType value
                        }

                        if (EMConfig.SKIP_CLOUD_CALENDARS) {
                            if (accountType != null) {
                                boolean inAccountToIgnore = EMUtility.itemInListIsSubstringInString(accountType, EMConfig.kAccountTypesToIgnore);

                                //						if ((inAccountToIgnore) && (!isDirty))
                                if (inAccountToIgnore) // Ignore the isDirty flag, it seems to be sent on all calendar events on some devices
                                    continue; // If this is a cloud calendar event that has not been modified since the last sync then skip it (we're assuming that the user will get their cloud content another way)
                            }
                        }

                        mXmlGenerator.startElement(EMStringConsts.EM_XML_CALENDAR_ENTRY);
                        if ((System.currentTimeMillis() - lastUpdateTime) > uiUpdateIntervalInMs) {
                            lastUpdateTime = System.currentTimeMillis();
                            EMProgressInfo progress = new EMProgressInfo();
                            progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_OUTGOING_DATA;
                            progress.mDataType = EMDataType.EM_DATA_TYPE_CALENDAR;
                            progress.mTotalItems = calendarCursor.getCount();
                            progress.mCurrentItemNumber = currentItemNumber;
                            updateProgressFromWorkerThread(progress);
                            DLog.log("Processing Calender >> " + currentItemNumber);
                        }
                        currentItemNumber++;
                        mNumberOfEntries++;

                        writeColumnToXml(calendarCursor, mXmlGenerator, EMStringConsts.EM_XML_CALENDAR_ENTRY_TITLE, titleColumn);
						writeBooleanColumnToXml(calendarCursor, mXmlGenerator, EMStringConsts.EM_XML_CALENDAR_ENTRY_ALL_DAY, allDayEventColumn);
                        writeTimeColumnToXml(calendarCursor, mXmlGenerator, EMStringConsts.EM_XML_CALENDAR_ENTRY_START_DATE, startDateColumn);

                        long longEndDate = 0;

                        if (endDateColumn != -1) {
                            try {
                                longEndDate = calendarCursor.getLong(endDateColumn);
                                longEndDate = longEndDate / 1000;
                            } catch (Exception ex) {
                                // TODO:
                            }
                        }

                        if (longEndDate == 0) {
                            if (durationColumn != -1) {
                                try {
                                    long longDuration = 0;
                                    String durationString;
                                    String truncatedDurationString;
                                    durationString = calendarCursor.getString(durationColumn);
                                    if (durationString.length() > 2) {
                                        truncatedDurationString = durationString.substring(1, durationString.length() - 1);
                                        longDuration = Long.parseLong(truncatedDurationString);
                                    }

                                    if (startDateColumn != -1) {
                                        Long startTime = calendarCursor.getLong(startDateColumn);
                                        startTime = startTime / 1000;
                                        longEndDate = startTime + longDuration;
                                    }
                                } catch (Exception ex) {
                                    // TODO:
                                }
                            }
                        }

                        if (longEndDate != 0) {
                            mXmlGenerator.startElement(EMStringConsts.EM_XML_CALENDAR_ENTRY_END_DATE);
                            String text = String.valueOf(longEndDate);
                            mXmlGenerator.writeText(text);
                            mXmlGenerator.endElement(EMStringConsts.EM_XML_CALENDAR_ENTRY_END_DATE);
                        }

                        //	                writeColumnToXml(calendarCursor, mXmlGenerator, EMStringConsts.EM_XML_CALENDAR_ENTRY_DURATION, durationColumn);

                        writeColumnToXml(calendarCursor, mXmlGenerator, EMStringConsts.EM_XML_CALENDAR_ENTRY_DESCRIPTION, descriptionColumn);
                        writeColumnToXml(calendarCursor, mXmlGenerator, EMStringConsts.EM_XML_CALENDAR_ENTRY_TIME_ZONE, timezoneColumn);
                        writeColumnToXml(calendarCursor, mXmlGenerator, EMStringConsts.EM_XML_CALENDAR_ENTRY_LOCATION, locationColumn);
                        writeColumnToXml(calendarCursor, mXmlGenerator, EMStringConsts.EM_XML_CALENDAR_ENTRY_RRULE, rruleColumn);
                        // writeTimeColumnToXml(calendarCursor, mXmlGenerator, EMStringConsts.EM_XML_CALENDAR_ENTRY_REPEAT_END_DATE, repeatUntilDateColumn);

                        String eventId = calendarCursor.getString(eventIdColumn);

                        // TODO: query attendee table
                        Cursor attendeeCursor = EMUtility.Context().getContentResolver().query(attendeeTableUri,
                                null, // Column projection
                                eventIdColumnName + "=" + eventId, // WHERE
                                null, // WHERE arguments
                                null); // Order-by

                        int attendeeNameColumn = attendeeCursor.getColumnIndex(attendeeColumnName);
                        int attendeeEmailColumn = attendeeCursor.getColumnIndex(attendeeEmailColumnName);

                        if (attendeeCursor.moveToFirst()) {
                            do {
                                //				    		writeColumnToXml(attendeeCursor, mXmlGenerator, EMStringConsts.EM_XML_CALENDAR_ENTRY_ATTENDEE, attendeeNameColumn);

                                String attendeeName = attendeeCursor.getString(attendeeNameColumn);
                                String attendeeEmail = attendeeCursor.getString(attendeeEmailColumn);

                                mXmlGenerator.startElement(EMStringConsts.EM_XML_CALENDAR_ENTRY_ATTENDEE);

                                if ((attendeeName != null) && (!attendeeName.equals("")))
                                    mXmlGenerator.writeText(attendeeName);
                                else if (attendeeEmail != null)
                                    mXmlGenerator.writeText(attendeeEmail);

                                mXmlGenerator.endElement(EMStringConsts.EM_XML_CALENDAR_ENTRY_ATTENDEE);
                            } while (attendeeCursor.moveToNext());
                        }

                        if (attendeeCursor != null)
                            attendeeCursor.close();

                        if (hasAlarmColumn >= 0) {
                            for (Reminder r : reminders) {
                                if (r.eid == Integer.parseInt(eventId)) {
                                    mXmlGenerator.startElement(EMStringConsts.EM_XML_CALENDAR_ENTRY_ALARM);
                                    mXmlGenerator.writeText(Integer.toString(r.seconds));
                                    mXmlGenerator.endElement(EMStringConsts.EM_XML_CALENDAR_ENTRY_ALARM);
                                }
                            }
                        }

                        mXmlGenerator.endElement(EMStringConsts.EM_XML_CALENDAR_ENTRY);
                        EMMigrateStatus.addItemTransferred(EMDataType.EM_DATA_TYPE_CALENDAR);
                    } catch (Exception exception) {
                        // Nothing we can do here so just flag the error
                        EMMigrateStatus.addItemNotTransferred(EMDataType.EM_DATA_TYPE_CALENDAR);
                    }
                } while (calendarCursor.moveToNext());
            }
            EMProgressInfo progress = new EMProgressInfo();
            progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_OUTGOING_DATA;
            progress.mDataType = EMDataType.EM_DATA_TYPE_CALENDAR;
            progress.mTotalItems = calendarCursor.getCount();
            progress.mCurrentItemNumber = calendarCursor.getCount();
            updateProgressFromWorkerThread(progress);
            DLog.log("Processing Calender Completed >> ");

            calendarCursor.close();

        } catch (Exception ex) {
            DLog.log(ex);
            EMMigrateStatus.setTotalFailure(EMDataType.EM_DATA_TYPE_CALENDAR);
        } finally {
            try {
                mXmlGenerator.endElement(EMStringConsts.EM_XML_ROOT);
            } catch (IOException e) {
                // Nothing we can do here
                DLog.log(e);
            }

            try {
                setFilePath(mXmlGenerator.endDocument());
            } catch (IOException e) {
                // Nothing we can do here
                DLog.log(e);
            }
        }
    }

	private void createAlarmList() {
		int secondsBefore = 0;

		if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			final String[] attendeeProjection = new String[]{
					CalendarContract.Reminders._ID,
					CalendarContract.Reminders.EVENT_ID,
					CalendarContract.Reminders.MINUTES,
					CalendarContract.Reminders.METHOD
			};
			final Cursor cursor = EMUtility.Context().getContentResolver().query(CalendarContract.Reminders.CONTENT_URI, attendeeProjection, null, null, null);
			cursor.moveToFirst();
			reminders.clear();

			while (cursor.moveToNext()) {
				final Reminder reminder = new Reminder();
				int id = cursor.getInt(0);
				int eid = cursor.getInt(1);
				secondsBefore = cursor.getInt(2)*-60;
				//	int method = cursor.getInt(3);
				reminder.id = id;
				reminder.eid = eid;
				reminder.seconds = secondsBefore;
				reminders.add(reminder);
			}
			cursor.close();
		}
		else
		{
			final String[] attendeeProjection = new String[]{
					"event_id",
					"method",
					"minutes",
			};
			Uri remindersUri;
			if (Build.VERSION.SDK_INT >= 8 ) {
				remindersUri = Uri.parse("content://com.android.calendar/reminders");
			} else {
				remindersUri = Uri.parse("content://calendar/reminders");
			}
			final Cursor cursor = EMUtility.Context().getContentResolver().query(CalendarContract.Reminders.CONTENT_URI, attendeeProjection, null, null, null);
			cursor.moveToFirst();
			reminders.clear();

			while (cursor.moveToNext()) {
				final Reminder reminder = new Reminder();
				int eid = cursor.getInt(0);
				int method = cursor.getInt(1);
				secondsBefore = cursor.getInt(2)*-60;
				reminder.id = -1;
				reminder.eid = eid;
				reminder.seconds = secondsBefore;
				reminders.add(reminder);
			}
			cursor.close();
		}
	}
	
	static public void writeBooleanColumnToXml(Cursor cursor, EMXmlGenerator aXmlSerializer, String aTag, int aColumnNumber) throws IllegalArgumentException, IllegalStateException, IOException {
	    if (aColumnNumber != -1) {
		aXmlSerializer.startElement(aTag);
		try {
		    	String stringValue = cursor.getString(aColumnNumber);
		    	boolean boolValue = false;
		    	if (stringValue.equalsIgnoreCase("1")) {
		    		boolValue = true;
		    	}
		    	
		    	if (boolValue)
		    		aXmlSerializer.writeText(EMStringConsts.EM_XML_XML_TRUE);
		    	else
		    		aXmlSerializer.writeText(EMStringConsts.EM_XML_XML_FALSE);
			} catch (Exception ex) {
				DLog.log(ex);
				// TODO:
			} finally {
		    	aXmlSerializer.endElement(aTag);
			}
	    }
	}

	
	static public void writeTimeColumnToXml(Cursor cursor, EMXmlGenerator aXmlSerializer, String aTag, int aColumnNumber) throws IllegalArgumentException, IllegalStateException, IOException {
	    if (aColumnNumber != -1) {
	    	aXmlSerializer.startElement(aTag);
			try {
			    	Long time = cursor.getLong(aColumnNumber);
			    	time = time / 1000;
			    	String text = String.valueOf(time);
			    	aXmlSerializer.writeText(text);
			} catch (Exception ex) {
				DLog.log(ex);
				// TODO:
			} finally {
		    	aXmlSerializer.endElement(aTag);
			}
	    }
	}
	
	static public void writeColumnToXml(Cursor cursor, EMXmlGenerator aXmlSerializer, String aTag, int aColumnNumber) throws IllegalArgumentException, IllegalStateException, IOException {
	    if (aColumnNumber != -1) {
	    	aXmlSerializer.startElement(aTag);
			try {
			    	String text = cursor.getString(aColumnNumber);
			    	aXmlSerializer.writeText(text);
			} catch (Exception ex) {
				DLog.log(ex);
				// TODO:
			} finally {
		    	aXmlSerializer.endElement(aTag);
			}
	    }
	}
	
	// Note the aColumnNumber is the number of the field containing the file path
	static public void writeFileSizeToXml(Cursor cursor, EMXmlGenerator aXmlSerializer, String aTag, int aColumnNumber) throws IllegalArgumentException, IllegalStateException, IOException {
	    if (aColumnNumber != -1) {
	    	aXmlSerializer.startElement(aTag);
			try {
			    	String filePath = cursor.getString(aColumnNumber);
			    	
			    	long fileSize = 0;
			    	
			    	if (filePath != null) {
			    		File file= new File(filePath);
			    		fileSize = file.length();
			    	}
			    	
			    	aXmlSerializer.writeText(Long.toString(fileSize));
			} catch (Exception ex) {
				DLog.log(ex);
				// TODO:
			} finally {
		    	aXmlSerializer.endElement(aTag);
			}
	    }
	}
	
	// Note the aColumnNumber is the number of the field containing the file path
	static public void writeFileNameToXml(Cursor cursor, EMXmlGenerator aXmlSerializer, String aTag, int aColumnNumber) throws IllegalArgumentException, IllegalStateException, IOException {
	    if (aColumnNumber != -1) {
	    	aXmlSerializer.startElement(aTag);
			try {
			    	String filePath = cursor.getString(aColumnNumber);
			    	
			    	String fileName = "";
			    	
			    	if (filePath != null) {
			    		File file= new File(filePath);
			    		fileName = file.getName();
			    	}
			    	
			    	aXmlSerializer.writeText(fileName);
			} catch (Exception ex) {
				DLog.log(ex);
				// TODO:
			} finally {
		    	aXmlSerializer.endElement(aTag);
			}
	    }
	}

	/*
	public String getFilePath() {
		return mTempFilePath;
	}
	*/

	public int getNumberOfEntries() {
		return mNumberOfEntries;
	}
}
