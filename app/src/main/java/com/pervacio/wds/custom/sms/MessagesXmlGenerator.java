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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.Xml;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.custom.utils.AppUtils;

import org.xmlpull.v1.XmlSerializer;

import java.io.OutputStream;

public class MessagesXmlGenerator {
	   
	   private final Context mContext;

	   final static private String TAG = "MessagesXmlGenerator";

	   public MessagesXmlGenerator(Context aContext) {
			mContext = aContext;
		}
		
		public void generateXml(OutputStream aOutputStream) {
	    	XmlSerializer xmlSerializer = Xml.newSerializer();

	    	try {
		    	xmlSerializer.setOutput(aOutputStream, "UTF-8");
		    	xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		    	xmlSerializer.startDocument("UTF-8", true);
		    	xmlSerializer.startTag(null, XmlConsts.COMMON_ROOT_ELEMENT);
		    			
//				ContentResolver cr = mContext.getContentResolver();
//				ContentValues values = new ContentValues();
				
				int addressColumn;
				int dateColumn;
				int textColumn;
				int readColumn;
				int typeColumn;

				// TODO: add attachment & mime type columns for MMS
				
				String url = "content://sms";
				Uri smsUri = Uri.parse(url);
				Cursor messagesCursor = mContext.getContentResolver().query(smsUri,
																	null, // Column projection : TODO: optimize this later
																	null, // WHERE
																	null, // WHERE arguments
																	null); // Order-by

				if(messagesCursor != null) {
					addressColumn = messagesCursor.getColumnIndex(MessagesShared.SMS_DB_ADDRESS);
					dateColumn = messagesCursor.getColumnIndex(MessagesShared.SMS_DB_DATE);
					textColumn = messagesCursor.getColumnIndex(MessagesShared.SMS_DB_BODY);
					readColumn = messagesCursor.getColumnIndex(MessagesShared.SMS_DB_READ);
					typeColumn = messagesCursor.getColumnIndex(MessagesShared.SMS_DB_TYPE);
					// Fix for #26184
					for (messagesCursor.moveToLast(); !messagesCursor.isBeforeFirst(); messagesCursor.moveToPrevious()) {
					//if (messagesCursor.moveToFirst()) {
						//do {
							String messageTag = null; // Will be either sent_message or received_message

							long type = messagesCursor.getLong(typeColumn);

							if (type == MessagesShared.SMS_DB_TYPE_INBOX) {
								messageTag = XmlConsts.RECEIVED_MESSAGE_ELEMENT;
							} else if (type == MessagesShared.SMS_DB_TYPE_SENT) {
								messageTag = XmlConsts.SENT_MESSAGE_ELEMENT;
							} else if (type == MessagesShared.SMS_DB_TYPE_DRAFT) {
								messageTag = XmlConsts.DRAFT_MESSAGE_ELEMENT;
							} else if (type == MessagesShared.SMS_DB_TYPE_OUTBOX) {
								messageTag = XmlConsts.OUTBOX_MESSAGE_ELEMENT;
							} else if (type == MessagesShared.SMS_DB_TYPE_QUEUED) {
								messageTag = XmlConsts.QUEUED_MESSAGE_ELEMENT;
							} else if (type == MessagesShared.SMS_DB_TYPE_FAILED) {
								messageTag = XmlConsts.FAILED_MESSAGE_ELEMENT;
							}
							if (messageTag != null) {
								xmlSerializer.startTag("", messageTag);

								try {
									XmlWritingUtilities.writeColumnToXml(messagesCursor, xmlSerializer, XmlConsts.MESSAGES_ADDRESS, addressColumn);
									XmlWritingUtilities.writeTimeColumnToXml(messagesCursor, xmlSerializer, XmlConsts.MESSAGES_DATE, dateColumn);
									XmlWritingUtilities.writeColumnToXml(messagesCursor, xmlSerializer, XmlConsts.MESSAGES_TEXT, textColumn);
									XmlWritingUtilities.writeBooleanColumnToXml(messagesCursor, xmlSerializer, XmlConsts.MESSAGES_READ, readColumn);
								} catch (Exception ex) {
									Log.e(TAG, ex.toString());
									DLog.log(ex);
								} finally {
									xmlSerializer.endTag("", messageTag);
								}
							}


						//} while (messagesCursor.moveToNext());
					}

					messagesCursor.close();
				}
			    
		    	xmlSerializer.endTag(null, XmlConsts.COMMON_ROOT_ELEMENT);
		    	xmlSerializer.endDocument();
		    	xmlSerializer.flush();
		    	aOutputStream.flush();
		    	
			} catch (Exception ex) {
				AppUtils.printLog(TAG," generateXml ",ex, AppUtils.LogType.EXCEPTION);
				DLog.log(ex);
			} finally {
		    	// Write XML footer and close the stream
				AppUtils.printLog(TAG,"Method : generateXML Finally block ",null, AppUtils.LogType.INFO);
			}
		}
}
