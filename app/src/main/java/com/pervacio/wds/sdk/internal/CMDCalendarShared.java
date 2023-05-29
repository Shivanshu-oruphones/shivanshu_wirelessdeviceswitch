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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.util.Log;

public class CMDCalendarShared {

   public static int findDefaultCalendar(Context aContext) {
		Cursor calendarCursor = null;
		int calendarId = 1;
		
	    Uri calendarUri;
		
		try {
			ContentResolver cr = aContext.getContentResolver();
			if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH) {					
				calendarUri = CalendarContract.Calendars.CONTENT_URI;
				String[] projection = new String[] {
				       CalendarContract.Calendars._ID,
				       CalendarContract.Calendars.ACCOUNT_NAME,
				       CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
				       CalendarContract.Calendars.NAME,
				       CalendarContract.Calendars.CALENDAR_COLOR};
				
				calendarCursor = cr.query(calendarUri, projection, null, null, null);
			}
			else
			{
			    if (Build.VERSION.SDK_INT >= 8 ) {
			    	calendarUri = Uri.parse("content://com.android.calendar/calendars");
			    } else {
			    	calendarUri = Uri.parse("content://calendar/calendars");
			    }
				String[] projection = new String[] {"_ID"};
					
					calendarCursor = cr.query(calendarUri, projection, null, null, null);
			}
			
			if (calendarCursor != null)
			{
				if (calendarCursor.getCount() == 0)
				{
					// If there are no calendars on the device then create a new one
					ContentValues calendarValues = new ContentValues();
					Uri addedCalendarUri = null;
					if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH) {	
					       calendarValues.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "iPhone");
					       calendarValues.put(CalendarContract.Calendars.NAME, "iPhone");
					}
					else
					{
					       calendarValues.put("calendar_displayName", "iPhone");
					       calendarValues.put("name", "iPhone");
					}
					
			       addedCalendarUri = cr.insert(calendarUri, calendarValues);

			       // Extract the created calendar ID and remember it
			       if (addedCalendarUri != null) {
				       String idString = addedCalendarUri.getLastPathSegment();
				       calendarId = Integer.parseInt(idString);
			       }

				} else if (calendarCursor.moveToNext()) {
				    if (calendarCursor.getCount() != 0) {
				    	 int calendarColumn = calendarCursor.getColumnIndexOrThrow("_ID");
				    	 calendarId = calendarCursor.getInt(calendarColumn);
				    }
			    }
			    calendarCursor.close();
			}
		} catch (Exception ex)
		{
			Log.e(TAG, ex.toString());
		}
		
		return calendarId;
	}

   static final String TAG = "CalendarShared";
}
