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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMFileMetaData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;


public class CMDUtility {

	static private Context mContext;

	static public void setContext(Context aContext) {
		mContext = aContext;
	}

	public static Context Context() {
		return mContext;
	}

	public static String temporaryFileName() {
		String filepath = null;
		try {
			File tempDir = mContext.getFilesDir();
			File tempFile = File.createTempFile("emtemp", "tmp", tempDir);
			filepath = tempFile.getPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			DLog.log(e);
		}
		return filepath;
	}

	// Only call this from the main thread
	public static void displayAlert(String aTitle, String aMessage) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

		// set title
		alertDialogBuilder.setTitle(aTitle); // TODO: internationalize this

		// set dialog message
		alertDialogBuilder
				.setMessage(aMessage)
				.setCancelable(false)
				.setPositiveButton(mContext.getString(R.string.ept_ok), new DialogInterface.OnClickListener() { // TODO: localize the OK
					public void onClick(DialogInterface dialog, int id) {
						// Ignore
					}
				});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
	}

	public static boolean copyFile(File aSource, File aDestination) {
		boolean result = true;
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new FileInputStream(aSource);
			output = new FileOutputStream(aDestination);
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buffer)) > 0) {
				output.write(buffer, 0, bytesRead);
			}
		} catch (Exception ex) {
			result = false;
		} finally {
			try {
				input.close();
				output.close();
			} catch (Exception ex) {
				// Ignore
			}
		}

		return result;
	}

	static public void buildFileListRecursive(File sourceLocation,
									   int dataType,
									   Integer currentItemNumber,
									   List<EMFileMetaData> aFileList) /*throws IOException*/ {
		if (!sourceLocation.exists())
			return; // Do nothing if the folder doesn't exist

		if (sourceLocation.isDirectory()) {
			String[] children = sourceLocation.list();
			if (children != null) {
				for (int i = 0; i < children.length; i++) {
					buildFileListRecursive(new File(sourceLocation, children[i]),
							dataType, currentItemNumber, aFileList);
				}
			}
		} else {
			currentItemNumber++;

			EMFileMetaData metaData = new EMFileMetaData();
			metaData.mSourceFilePath = sourceLocation.toString();
			metaData.mSize = sourceLocation.length();
			metaData.mFileName = sourceLocation.getName();
			// metaData.mTotalFiles = cursor.getCount();
			metaData.mDataType = dataType;
			metaData.mCurrentFileNumber = currentItemNumber;
			metaData.mRelativePath = "";

			aFileList.add(metaData);
		}
	}

	static public void buildFileListRecursive(File sourceLocation,
									   int dataType,
									   List<EMFileMetaData> aFileList) /*throws IOException*/ {
		Integer currentItemNumber = new Integer(0);
		buildFileListRecursive(sourceLocation, dataType, currentItemNumber, aFileList);

		int itemCount = 0;
		for (EMFileMetaData fileMetadata: aFileList) {
			if (fileMetadata.mDataType == dataType) {
				itemCount++;
			}
		}

		for (EMFileMetaData fileMetadata: aFileList) {
			if (fileMetadata.mDataType == dataType) {
				fileMetadata.mTotalFiles = itemCount;
			}
		}
	}
}