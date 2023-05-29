package com.pervacio.wds.app;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;

import java.util.List;
/**
 * We need to do some special initialization for SemcCalender.
 * This class provides utilities for that
 * @author Pervacio
 *
 */
class SemcCalendarUtil {

	private static final String[] CALENDAR_PROJECTION = { "_id", "calendar_color", "calendar_displayName", "calendar_access_level", "calendar_timezone" };
	
	/**
	 * inserts initialization record for semc Calender
	 * @param context
	 */
	public static void insertInitialzationRecordForSemcCalendar(Context context){
		String str = "Device calendar";
		int i = 0xffb9db68;
	    ContentResolver localContentResolver = context.getContentResolver();
	    Cursor localCursor = null;
	    localCursor = doCalendarsQuery(localContentResolver, CALENDAR_PROJECTION, "calendar_access_level>=500 AND sync_events=1", null);
	    if ((localCursor != null) && (localCursor.getCount() < 1))
	      {
	        ContentValues localContentValues = new ContentValues();
	        localContentValues.put("name", str);
	        localContentValues.put("calendar_displayName", str);
	        localContentValues.put("calendar_color", i);
	        localContentValues.put("account_name", "SYNCML-ORPHANED");
	        localContentValues.put("ownerAccount", " ");
	        localContentValues.put("account_type", "LOCAL");
	        localContentValues.put("sync_events", "1");
	        localContentValues.put("calendar_access_level", 5000);
			  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				  localContentResolver.insert(asSyncAdapter(CalendarContract.Calendars.CONTENT_URI, "SYNCML-ORPHANED", "LOCAL"), localContentValues);
			  }
		  }
	}
	/**
	 * To check if calendar is existing or not. 
	 * 
	 * @param paramContentResolver
	 * @param paramArrayOfString
	 * @param paramString1
	 * @param paramString2
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private static Cursor doCalendarsQuery(ContentResolver paramContentResolver, String[] paramArrayOfString, String paramString1, String paramString2)
	  {
	    Uri localUri = CalendarContract.Calendars.CONTENT_URI;
	    if (paramString2 == null);
	    //Code is copied from semc calendar initialization as it is. It can be improved but not changed so it will be easy to find in future.
	    for (String str = "calendar_displayName"; ; str = paramString2)
	      return paramContentResolver.query(localUri, paramArrayOfString, paramString1, null, str);
	  }
	/**
	 * Returns URI which can be used as a sync adapter
	 * @param paramUri
	 * @param paramString1
	 * @param paramString2
	 * @return
	 */
	private static Uri asSyncAdapter(Uri paramUri, String paramString1, String paramString2)
	  {
	    return paramUri.buildUpon().appendQueryParameter("caller_is_syncadapter", "true").appendQueryParameter("account_name", paramString1).appendQueryParameter("account_type", paramString2).build();
	  }
	/**
	 * Utility method to check if semc calendar is installed  or not.
	 * @param context
	 * @return
	 */
	public static boolean isSemcCalenderInstalled(Context context){
		List<PackageInfo> pkgInfos = context.getPackageManager().getInstalledPackages(0);
		boolean installed = false;
		for (PackageInfo packageInfo : pkgInfos) {
			if(packageInfo != null &&  packageInfo.applicationInfo != null 
					&& packageInfo.applicationInfo.publicSourceDir != null
					&& packageInfo.applicationInfo.sourceDir.contains("SemcCalendar.apk")){
				installed = true;
				break;
			}
		}
		return installed;

	}
}
