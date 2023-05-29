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

import android.util.Log;

import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.custom.APPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DLog {
	static EMGlobals emGlobals = new EMGlobals();
	
	static class DLogInstance {
		
		static boolean mLoggingOn = EMConfig.LOGGING_ON;
		
		public boolean loggingIsOn() {
			return mLoggingOn;
		}
		
		private OutputStream mDataLogStream;
		
		DLogInstance() {
			try {
				File externalStorage = emGlobals.getmContext().getFilesDir();
				String logFilePath = externalStorage.toString() + "/log.txt";
	
				if (mLoggingOn)
				{
					File logFile = new File(logFilePath);
					if (!logFile.exists()) {
						// Log.d(EMConfig.TAG, "Created new file");
						logFile.createNewFile();
					}
					try {
						//Append to existing file
						mDataLogStream = new FileOutputStream(logFile, true);
						mDataLogStream.write("===========================================================================================================".getBytes());
						mDataLogStream.write("===========================================================================================================".getBytes());
						mDataLogStream.write("===========================================================================================================".getBytes());
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}				
				}
			} catch (Exception ex)
			{
				// TODO:
				DLog.log("Exception : "+ex.toString());
			}
		}
		
		synchronized void log(String aString)
		{
			try {
				byte[] logBytes = (aString + "\r\n").getBytes();

				if (mDataLogStream != null) {
					mDataLogStream.write(logBytes);
					mDataLogStream.flush();
				}
//				if (BuildConfig.DEBUG)  // Some of logs are missing in release build apk, so enabling logs for release also.
					// Log.d(EMConfig.TAG, aString);

			} catch (Exception ex)
			{
				// TODO:
			}
		}
	};
	
	static DLogInstance mLogSingleton; // = new DLogInstance();
		
	 public static void log(String aLogString) {
		 try {
			 if (DLogInstance.mLoggingOn) {
				 if (mLogSingleton == null)
					 mLogSingleton = new DLogInstance();
				 // // Log.d(EMConfig.TAG, aLogString);
				 SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				 Calendar cal = Calendar.getInstance();
				 mLogSingleton.log("["+dateFormat.format(cal.getTime()) + "] : [" +Thread.currentThread().getId()+"] "+ aLogString);
			 }
		 } catch (Exception e) {
			 e.printStackTrace();
		 }
	 }

	public static void resetLogFile() {
		mLogSingleton = null;
	}

	private static String exceptionToString(Exception aException) {
		Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		aException.printStackTrace(printWriter);
		return writer.toString();
	}

	public static void log(Exception aException) {
		log("Exception: " + exceptionToString(aException));
	}

	public static void log(String aTitle, Exception aException) {
		log("Exception: " + aTitle + ": " + exceptionToString(aException));
	}

	public static boolean loggingIsOn() {
		/*
		if (mLogSingleton == null)
			mLogSingleton = new DLogInstance();
		
		return mLogSingleton.loggingIsOn();
		*/
		return true;
	}
}
