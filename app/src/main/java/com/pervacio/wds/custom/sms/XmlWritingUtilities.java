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

import android.database.Cursor;

import com.pervacio.wds.app.DLog;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.IOException;

public class XmlWritingUtilities {
	
	static private final String TAG = "XmlWritingUtilities";
	
	static public void writeBooleanColumnToXml(Cursor cursor, XmlSerializer aXmlSerializer, String aTag, int aColumnNumber) throws IllegalArgumentException, IllegalStateException, IOException {
	    if (aColumnNumber != -1) {
		aXmlSerializer.startTag("", aTag);
		try {
		    	String stringValue = cursor.getString(aColumnNumber);
		    	boolean boolValue = false;
		    	if (stringValue.equalsIgnoreCase("1")) {
		    		boolValue = true;
		    	}
		    	
		    	if (boolValue)
		    		aXmlSerializer.text(XmlConsts.XML_TRUE);
		    	else
		    		aXmlSerializer.text(XmlConsts.XML_FALSE);
			} catch (Exception ex) {
				DLog.log(ex);
			} finally {
		    	aXmlSerializer.endTag("", aTag);
			}
	    }
	}

	
	static public void writeTimeColumnToXml(Cursor cursor, XmlSerializer aXmlSerializer, String aTag, int aColumnNumber) throws IllegalArgumentException, IllegalStateException, IOException {
	    if (aColumnNumber != -1) {
	    	aXmlSerializer.startTag("", aTag);
			try {
			    	Long time = cursor.getLong(aColumnNumber);
			    	time = time / 1000;
			    	String text = String.valueOf(time);
			    	aXmlSerializer.text(text);
			} catch (Exception ex) {
				DLog.log(ex);
			} finally {
		    	aXmlSerializer.endTag("", aTag);
			}
	    }
	}
	
	static public void writeColumnToXml(Cursor cursor, XmlSerializer aXmlSerializer, String aTag, int aColumnNumber) throws IllegalArgumentException, IllegalStateException, IOException {
	    if (aColumnNumber != -1) {
	    	aXmlSerializer.startTag("", aTag);
			try {
			    	String text = cursor.getString(aColumnNumber);
			    	aXmlSerializer.text(text);
			} catch (Exception ex) {
				DLog.log(ex);
			} finally {
		    	aXmlSerializer.endTag("", aTag);
			}
	    }
	}
	
	
	// Note the aColumnNumber is the number of the field containing the file path
	static public void writeFileSizeToXml(Cursor cursor, XmlSerializer aXmlSerializer, String aTag, int aColumnNumber) throws IllegalArgumentException, IllegalStateException, IOException {
	    if (aColumnNumber != -1) {
	    	aXmlSerializer.startTag("", aTag);
			try {
			    	String filePath = cursor.getString(aColumnNumber);
			    	
			    	long fileSize = 0;
			    	
			    	if (filePath != null) {
			    		File file= new File(filePath);
			    		fileSize = file.length();
			    	}
			    	
			    	aXmlSerializer.text(Long.toString(fileSize));
			} catch (Exception ex) {
				DLog.log(ex);
			} finally {
		    	aXmlSerializer.endTag("", aTag);
			}
	    }
	}
	
	// Note the aColumnNumber is the number of the field containing the file path
	static public void writeFileNameToXml(Cursor cursor, XmlSerializer aXmlSerializer, String aTag, int aColumnNumber) throws IllegalArgumentException, IllegalStateException, IOException {
	    if (aColumnNumber != -1) {
	    	aXmlSerializer.startTag("", aTag);
			try {
			    	String filePath = cursor.getString(aColumnNumber);
			    	
			    	String fileName = "";
			    	
			    	if (filePath != null) {
			    		File file= new File(filePath);
			    		fileName = file.getName();
			    	}
			    	
			    	aXmlSerializer.text(fileName);
			} catch (Exception ex) {
				DLog.log(ex);
			} finally {
		    	aXmlSerializer.endTag("", aTag);
			}
	    }
	}
}
