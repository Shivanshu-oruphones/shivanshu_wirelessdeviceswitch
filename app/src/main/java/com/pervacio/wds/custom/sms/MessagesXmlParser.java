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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.custom.utils.AppUtils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class MessagesXmlParser extends DefaultHandler {
	private static final String ADDRESS = "address";
	private static final String PERSON = "person";
	private static final String DATE = "date";
	private static final String READ = "read";
	private static final String STATUS = "status";
	private static final String TYPE = "type";
	private static final String BODY = "body";
	public static final int MESSAGE_TYPE_INBOX = 1;
	public static final int MESSAGE_TYPE_SENT = 2;
	private static final int MESSAGE_TYPE_DRAFT = 3;
	public static final int MESSAGE_TYPE_OUTBOX = 4;
	public static final int MESSAGE_TYPE_FAILED = 5;
	public static final int MESSAGE_TYPE_QUEUED = 6;
	private final OutputStream aOutputStream;
	private int counter;
    Handler smsHandler;

	public MessagesXmlParser(Handler restoredSmsHangler, Context aContext, OutputStream aOutputstream, int counter) {
		smsHandler = restoredSmsHangler;
		mContext = aContext;
		this.aOutputStream = aOutputstream;
		this.counter = counter;
	}

	private static final String TAG = "MessagesXmlParser";

	@Override
	public void startDocument() {
		// Nothing to do
	}

	@Override
	public void endDocument() {
	}

	private void handleMessageElement(String aElementName) {
		try {
			if (aElementName.equalsIgnoreCase(XmlConsts.MESSAGES_ADDRESS)) {
				mAddress = mLevel3Text;
			} else if (aElementName.equalsIgnoreCase(XmlConsts.MESSAGES_TEXT)) {
				mText = mLevel3Text;
			} else if (aElementName.equalsIgnoreCase(XmlConsts.MESSAGES_DATE)) {
				mDateText = mLevel3Text;
			} else if (aElementName.equalsIgnoreCase(XmlConsts.MESSAGES_READ)) {
				mRead = mLevel3Text.compareToIgnoreCase("true") == 0;
			} else if (aElementName.equalsIgnoreCase(XmlConsts.MESSAGES_ATTACHMENT)) {
				Attachment attachment = new Attachment();
				attachment.mData = Base64.decode(mLevel3Text, Base64.DEFAULT);
				attachment.mMimeType = mAttachmentMimeType;
				mAttachments.add(attachment);
			}
		} catch (Exception ex) {
			// Ignore - some bad parameter
			DLog.log(ex);
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
//		// Log.d(TAG, ">> startElement Uri=" + uri + " localName=" + localName + "qName=" + qName);

		mXmlLevel++;

		try {
			if (mXmlLevel == 3) {
				mLevel3Text = "";

				if (localName.compareToIgnoreCase(XmlConsts.MESSAGES_ATTACHMENT) == 0)
				{
					mAttachmentMimeType = attributes.getValue(XmlConsts.MESSAGES_MIME_TYPE);
				}
			}
		} catch (Exception ex)
		{
			// Ignore
			DLog.log(ex);
		}

		if (mXmlLevel == 2) {
			// Level 2 element should always be <sent_message> or <received_message> - otherwise flag a fatal error
			mText = "";
			mAddress = "";
			mRead = true;
			mAttachments = new ArrayList<Attachment>();
			mAttachmentMimeType = "";
		}

		if (mXmlLevel == 1) {
			// Ignore
		}

		mInElement = true;

//		// Log.d(TAG, "<< startElement");
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {

		// Throw an exception if we're at the end of the document.
		// This is grim - ultimately this code should be moved to a pull parser in an async task then we wouldn't have to do this
		if ((localName.equalsIgnoreCase("messages")) || (localName.equalsIgnoreCase(XmlConsts.COMMON_ROOT_ELEMENT))) {
			smsHandler.sendEmptyMessageDelayed(1,500);
			AppUtils.printLog(TAG," End of messages XML",null, AppUtils.LogType.INFO);
			throw new SAXException();
		}

		// // Log.d(TAG, "endElement Uri=" + uri + " localName=" + localName + "qName=" + qName);
		if (mXmlLevel == 3) {
			// // Log.d(TAG, "End element level 3: " + localName + ", " + mLevel3Text);

			// Handle message elements:
			handleMessageElement(localName);
		}

		if (mXmlLevel == 2) {
			try {
				// // Log.d(TAG, "Getting date");
				counter++;
/*				if(counter % 50 == 0){
					try {
						sendResponse(aOutputStream, "INPROGRESS");
					}catch (Exception ex){

					}
				}*/

				long unixDate = Long.valueOf(mDateText);
				final Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(unixDate);
				Date date = cal.getTime();
				String androidTime = String.valueOf(date.getTime() * 1000);

				int messageType = 0;

				if (localName.equalsIgnoreCase(XmlConsts.RECEIVED_MESSAGE_ELEMENT)) {
					messageType = 1;
				} else if (localName.equalsIgnoreCase(XmlConsts.SENT_MESSAGE_ELEMENT)) {
					messageType = 2;
				} else if (localName.equalsIgnoreCase(XmlConsts.DRAFT_MESSAGE_ELEMENT)) {
					messageType = 3;
				} else if (localName.equalsIgnoreCase(XmlConsts.OUTBOX_MESSAGE_ELEMENT)) {
					messageType = 4;
				} else if (localName.equalsIgnoreCase(XmlConsts.FAILED_MESSAGE_ELEMENT)) {
					messageType = 5;
				} else if (localName.equalsIgnoreCase(XmlConsts.QUEUED_MESSAGE_ELEMENT)) {
					messageType = 6;
				}

				if ((messageType > 0) && (mAttachments.isEmpty())) { // if this is an SMS message then add it

					// We are removing content provider query temporarily, this query taking too much time while msg insertion
					// need to find one good solution for message insertion
					/*// Query message content provider to see if we have a matching message there already
					String url = "content://sms";
					Uri smsUri = Uri.parse(url);

					String where = ADDRESS + "=? AND " + DATE + "=? AND " + TYPE + "=? AND " + BODY + "=?";
					String[] args = {mAddress, androidTime, String.valueOf(messageType), mText};

					Cursor cur = mContext.getContentResolver().query(smsUri,
							null, // Column projection
							where, // WHERE
							args, // WHERE arguments
							null); // Order-by

					// If there is no existing matching message then add this one...
					if(cur != null) {
						if (cur.getCount() == 0) {*/
							ContentValues values = new ContentValues();
							if (mAddress != null && !mAddress.isEmpty()) {
								values.put(ADDRESS, mAddress);
							}

							values.put(DATE, androidTime);

							if (mRead)
								values.put(READ, 1);
							else
								values.put(READ, 0);

							values.put(STATUS, -1);

							values.put(TYPE, messageType);

							values.put(BODY, mText);

							// Log.d(TAG, "Message Address: " + mAddress);

							// Log.d(TAG, "Text Message: " + mText);

							if ((mAddress != null) && (!mAddress.isEmpty())
									&& (mText != null) && (!mText.isEmpty())) {
								mContext.getContentResolver().insert(Uri.parse("content://sms"), values);
								// Log.d(TAG, "Added a message");
							} else if (messageType == MESSAGE_TYPE_DRAFT //in case of draft message any one may be still null
									&& ((mText != null) && (!mText.isEmpty()))) {
								values.put(ADDRESS, " "); // in order to have that message accessible, in case of drafts
								//putting empty value if not entered.
								Uri respUri = mContext.getContentResolver().insert(Uri.parse("content://sms"), values);
								// Log.d(TAG, "Added a message");
							}
//						}
//
//						cur.close();
//					}
				}

				if ((messageType > 0) && (!mAttachments.isEmpty())) { // this looks like an MMS message, so add it

					boolean sent = false;
					if (messageType == 1)
						sent = true;

					ArrayList<String> textParts = new ArrayList<String>();
					textParts.add(mText);

					ArrayList<byte[]> imageParts = new ArrayList<byte[]>();

					Iterator<Attachment> attachmentsIter = mAttachments.iterator();

					for (Attachment object : mAttachments) {
						if (object.mMimeType.compareToIgnoreCase("image/jpeg") == 0)
						{
							imageParts.add(object.mData);
						}
					}

					addMMS(sent,
							textParts,
							imageParts,
							date.getTime(),
							null, // for now we ignore the subject
							mRead,
							mAddress);

				}
				smsHandler.sendEmptyMessage(0);
			}
			catch (Exception ex)
			{
				AppUtils.printLog(TAG," Exception adding message ",ex, AppUtils.LogType.EXCEPTION);
				DLog.log(ex);
			}
		}

		mXmlLevel--;

		mInElement = false;
	}



	@Override
	public void characters(char[] ch, int start, int length) {
		// // Log.d(TAG, ">> characters");
		if (mInElement) { // Only use the initial text from an element, ignore any text between elements, or after any nested elements
			if (mXmlLevel == 3) {
				mLevel3Text += new String(ch, start, length);
			}
		}
		// // Log.d(TAG, "<< characters");
	}

	private int mXmlLevel = 0; // Incremented every time we start a tag, decremented when we end one	

	private String mLevel3Text; // The text for the element directly under the <message> element

	private boolean mInElement = false;

	private String mAddress;
	private String mText;
	private String mDateText;
	private boolean mRead;
	private String mAttachmentMimeType;

	class Attachment {
		public String mMimeType;
		public byte[] mData;
	}

	private ArrayList<Attachment> mAttachments;

	private final Context mContext;


	private String generateSmilText(int aNumberOfImages,
                                    int aNumberOfTextEntries) {
		String smil = "<smil><head><layout><root-layout width=\"320px\" height=\"480px\"/><region id=\"Text\" left=\"0\" top=\"320\" width=\"320px\" height=\"160px\" fit=\"meet\"/><region id=\"Image\" left=\"0\" top=\"0\" width=\"320px\" height=\"320px\" fit=\"meet\"/></layout></head><body>";

		int numberOfSlides = aNumberOfImages;
		if (aNumberOfTextEntries > aNumberOfImages) {
			numberOfSlides = aNumberOfTextEntries;
		}

		for (int index = 0; index < numberOfSlides; index++) {
			smil += "<par dur=\"5000ms\">";

			if (index < aNumberOfTextEntries)
				smil += "<text src=\"cid:text_" + Integer.toString(index) + ".txt\" region=\"Text\"/>";

			if (index < aNumberOfImages)
				smil += "<img src=\"image_" + Integer.toString(index) + ".jpg\" region=\"Image\"/>";

			smil += "</par>";
		}

		smil += "</body></smil>";

		return smil;
	}

	private void sendResponse(OutputStream aOutputStream, String aString) {
		byte[] bytesWithCrLf = new byte["INPROGRESS".length() + 2];
		try {
			byte[] bytesWithoutCrLf = "INPROGRESS".getBytes("US-ASCII");

			System.arraycopy(bytesWithoutCrLf,
					0,
					bytesWithCrLf,
					0,
					bytesWithoutCrLf.length);

			bytesWithCrLf[bytesWithoutCrLf.length] = '\r';
			bytesWithCrLf[bytesWithoutCrLf.length + 1] = '\n';

			try {
				aOutputStream.write(bytesWithCrLf);
			} catch (IOException e) {
				// Fail
				e.printStackTrace();
				AppUtils.printLog(TAG," sendResponse ",e, AppUtils.LogType.EXCEPTION);
				DLog.log(e);
			}

		} catch (UnsupportedEncodingException e) {
			// Fail and log
			e.printStackTrace();
			AppUtils.printLog(TAG," sendResponse ",e, AppUtils.LogType.EXCEPTION);
			DLog.log(e);
		}
	}

	void logMmsMessages(String aUri) {
		try {
			Uri smsUri = Uri.parse(aUri);
			Cursor cur = mContext.getContentResolver().query(smsUri,
					null, // Column projection
					null, // WHERE
					null, // WHERE arguments
					null); // Order-by


			int count = 0;

			if(cur != null) {
				count = cur.getCount();
				String[] columnNames = cur.getColumnNames();
				cur.moveToFirst();
				if (cur.moveToFirst()) {
					do {
						// Log the main message
						// Log.d(TAG, "***************************************************");
						for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {
							String columnName = columnNames[columnIndex];
							// Log.d(TAG, "values.put(\"" + columnName + "\", \"" + cur.getString(columnIndex) + "\");");
						}

						int messageIdIndex = cur.getColumnIndexOrThrow("_id");
						String messageId = cur.getString(messageIdIndex);

						// Log the parts for this message
						Uri mmsPartUri = Uri.parse("content://mms/part");
						String selectionPart = "mid=" + messageId;
						Cursor partCursor = mContext.getContentResolver().query(mmsPartUri, null, selectionPart, null, null);

						if (partCursor != null) {
							String[] partColumnNames = partCursor.getColumnNames();
							if (partCursor.moveToFirst()) {
								do {
									// Log.d(TAG, "***************************************************");
									for (int columnIndex = 0; columnIndex < partColumnNames.length; columnIndex++) {
										String columnName = partColumnNames[columnIndex];
										// Log.d(TAG, "part>>       values.put(\"" + columnName + "\", \"" + partCursor.getString(columnIndex) + "\");");
									}
								} while (partCursor.moveToNext());
							}
							partCursor.close();
						}

						Uri addrUri = Uri.parse("content://mms/" + messageId + "/addr");
						Cursor addrCursor = mContext.getContentResolver().query(addrUri, null, null, null, null);

						if (addrCursor != null) {
							String[] addrColumnNames = addrCursor.getColumnNames();
							if (addrCursor.moveToFirst()) {
								do {
									// Log.d(TAG, "***************************************************");
									for (int columnIndex = 0; columnIndex < addrColumnNames.length; columnIndex++) {
										String columnName = addrColumnNames[columnIndex];
										// Log.d(TAG, "addr>>       values.put(\"" + columnName + "\", \"" + addrCursor.getString(columnIndex) + "\");");
									}
								} while (addrCursor.moveToNext());
								addrCursor.close();
							}
						}
					}
					while (cur.moveToNext()) ;


					cur.close();
				}
			}
		} catch (Exception ex) {
			Log.e(TAG, ex.toString());
			DLog.log(ex);
		}
	}

	private void addMmsAddress(String aMessageId, // The message to attach this address to
                               String aContactId,
                               String aAddress,
                               boolean aSent) {

		try {
			boolean has_mms_addr_msg_id = false;
			boolean has_mms_addr_contact_id = false;
			boolean has_mms_addr_address = false;
			boolean has_mms_addr_type = false;
			boolean has_mms_addr_charset = false;

			Uri uri = Uri.parse("content://mms/" + aMessageId + "/addr");
			Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);

			if(cursor != null) {
				String[] columnNames2 = cursor.getColumnNames();

				for (String columnName : columnNames2) {
					if (columnName.equals("msg_id"))
						has_mms_addr_msg_id = true;

					if (columnName.equals("contact_id"))
						has_mms_addr_contact_id = true;

					if (columnName.equals("address"))
						has_mms_addr_address = true;

					if (columnName.equals("type"))
						has_mms_addr_type = true;

					if (columnName.equals("charset"))
						has_mms_addr_charset = true;
				}
				cursor.close();
			}

			ContentValues mmsAddrValue = new ContentValues();

			if ((has_mms_addr_contact_id) && (aContactId != null))
				mmsAddrValue.put("contact_id", aContactId);

			if (has_mms_addr_address)
				mmsAddrValue.put("address", aAddress);

			if (aSent) {
				mmsAddrValue.put("type", "137");
			} else {
				mmsAddrValue.put("type", "151");
			}

			if (has_mms_addr_charset)
				mmsAddrValue.put("charset", "106");

			Uri addrUri = Uri.parse("content://mms/" + aMessageId + "/addr");
			Uri mmsAddrUri = mContext.getContentResolver().insert(addrUri, mmsAddrValue);
			// // Log.d(TAG, mmsAddrUri.toString());


		} catch (Exception ex) {
			Log.e(TAG, ex.toString());
			DLog.log(ex);
		}
	}

	private void addMmsPart(boolean aIsSmil, // True if this is the main SMIL part of the message
                            String aMessageId, // The message to attach to
                            byte[] aData, // Can be null if there is no data part
                            String aText, // Can be null if there is no text
                            String aMimeType,
                            String aContentId, // The cid as used in the SMIL
                            String aContentLink) // content link - usually the filename - could maybe be referenced in the SMIL
	{
		try {
			boolean has_mms_part_seq = false;
			boolean has_mms_part_ct = false;
			boolean has_mms_part_chset = false;
			boolean has_mms_part_cid = false;
			boolean has_mms_part_cl = false;
			boolean has_mms_part_text = false;

			Uri uri = Uri.parse("content://mms/part");
			String selectionPart = "mid=" + aMessageId;
			Cursor cursor = mContext.getContentResolver().query(uri, null, selectionPart, null, null);

			if(cursor != null) {
				String[] columnNames2 = cursor.getColumnNames();

				for (String columnName : columnNames2) {
					if (columnName.equals("seq"))
						has_mms_part_seq = true;

					if (columnName.equals("ct"))
						has_mms_part_ct = true;

					if (columnName.equals("chset"))
						has_mms_part_chset = true;

					if (columnName.equals("cid"))
						has_mms_part_cid = true;

					if (columnName.equals("cl"))
						has_mms_part_cl = true;

					if (columnName.equals("text"))
						has_mms_part_text = true;
				}
				cursor.close();
			}
			ContentValues mmsPartValue = new ContentValues();

			if (has_mms_part_seq) {
				if (aIsSmil)
					mmsPartValue.put("seq", "-1");
				else
					mmsPartValue.put("seq", "0");
			}

			if (has_mms_part_ct)
				mmsPartValue.put("ct", aMimeType);

			if (has_mms_part_cid)
				mmsPartValue.put("cid", aContentId);

			if (has_mms_part_cl)
				mmsPartValue.put("cl", aContentLink);

			if ((has_mms_part_chset) && (!aIsSmil))
				mmsPartValue.put("chset", "106");

			if (has_mms_part_text) {
				if (aText != null) {
					mmsPartValue.put("text", aText);
				}
			}

			Uri partUri = Uri.parse("content://mms/" + aMessageId + "/part");
			Uri mmsPartUri = mContext.getContentResolver().insert(partUri, mmsPartValue);
			// // Log.d(TAG, mmsPartUri.toString());

			// Add data if available, e.g. picture data / video data
			if (aData != null) {
				OutputStream os = null;
				if (mmsPartUri != null) {
					os = mContext.getContentResolver().openOutputStream(mmsPartUri);
					if (os != null)
						os.write(aData, 0, aData.length);
				}
			}

		} catch (Exception ex) {
			Log.e(TAG, ex.toString());
			DLog.log(ex);
		}
	}

	@SuppressWarnings("ConstantConditions")
	private void addMMS(boolean aSent,
						List<String> aTextParts,
						List<byte[]> aImageParts,
						long aDate,
						String aSubject,
						boolean aRead,
						String aAddress) {
		boolean has_mms_thread_id = false;
		boolean has_mms_date = false;
		boolean has_mms_msg_box = false;
		boolean has_mms_read = false;
		boolean has_mms_sub = false;
		boolean has_mms_ct_t = false;
		boolean has_mms_exp = false;
		boolean has_mms_m_id = false;
		boolean has_mms_m_cls = false;
		boolean has_mms_m_type = false;
		boolean has_mms_v = false;
		boolean has_mms_m_size = false;
		boolean has_mms_pri = false;
		boolean has_mms_rr = false;
		boolean has_mms_resp_st = false;
		boolean has_mms_tr_id = false;
		boolean has_mms_d_rpt = false;

//		logMmsMessages("content://mms/sent");

		try {

			// We need the following in order to add a message
			// An address
			// At least one text part or one attachment

			if ((aAddress == null) || (aAddress.isEmpty()))
				return;

			if ((aTextParts.size() == 0) && (aImageParts.size() == 0))
				return;

			Uri smsUri = Uri.parse("content://mms");
			String[] args = {Long.toString(aDate)};
			Cursor cur = mContext.getContentResolver().query(smsUri,
					null, // Column projection
					"date=?", // WHERE
					args, // WHERE arguments
					null); // Order-by


			boolean alreadyExists = false;
			if (cur.getCount() > 0) {
				alreadyExists = true;
			}

			String[] columnNames = cur.getColumnNames();
			cur.moveToFirst();

			for (String columnName : columnNames) {
				if (columnName.equals("thread_id"))
					has_mms_thread_id = true;

				if (columnName.equals("date"))
					has_mms_date = true;

				if (columnName.equals("msg_box"))
					has_mms_msg_box = true;

				if (columnName.equals("read"))
					has_mms_read = true;

				if (columnName.equals("sub"))
					has_mms_sub = true;

				if (columnName.equals("ct_t"))
					has_mms_ct_t = true;

				if (columnName.equals("exp"))
					has_mms_exp = true;

				if (columnName.equals("m_id"))
					has_mms_m_id = true;

				if (columnName.equals("m_cls"))
					has_mms_m_cls = true;

				if (columnName.equals("m_type"))
					has_mms_m_type = true;

				if (columnName.equals("v"))
					has_mms_v = true;

				if (columnName.equals("m_size"))
					has_mms_m_size = true;

				if (columnName.equals("pri"))
					has_mms_pri = true;

				if (columnName.equals("rr"))
					has_mms_rr = true;

				if (columnName.equals("resp_st"))
					has_mms_resp_st = true;

				if (columnName.equals("tr_id"))
					has_mms_tr_id = true;

				if (columnName.equals("d_rpt"))
					has_mms_d_rpt = true;
			}
			/*for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {
				String columnName = columnNames[columnIndex];					

				if (columnName.equals("thread_id"))
					has_mms_thread_id = true;
				
				if (columnName.equals("date"))
			        has_mms_date = true;
			        					        
				if (columnName.equals("msg_box"))
			        has_mms_msg_box = true;
			        
				if (columnName.equals("read"))					        
			        has_mms_read = true;
			        
				if (columnName.equals("sub"))					        
			        has_mms_sub = true;
			        
				if (columnName.equals("ct_t"))					        
			        has_mms_ct_t = true;
			        
				if (columnName.equals("exp"))					        
			        has_mms_exp = true;
			        
				if (columnName.equals("m_id"))					        
			        has_mms_m_id = true;
			        
				if (columnName.equals("m_cls"))					        
			        has_mms_m_cls = true;
			        
				if (columnName.equals("m_type"))					        
			        has_mms_m_type = true;
			        
				if (columnName.equals("v"))					        
			        has_mms_v = true;
			        
				if (columnName.equals("m_size"))					        
			        has_mms_m_size = true;
			        
				if (columnName.equals("pri"))					        
			        has_mms_pri = true;
			        
				if (columnName.equals("rr"))					        
			        has_mms_rr = true;
			        
				if (columnName.equals("resp_st"))					        
			        has_mms_resp_st = true;
			        
				if (columnName.equals("tr_id"))					        
			        has_mms_tr_id = true;
			        
				if (columnName.equals("d_rpt"))					        
			        has_mms_d_rpt = true;
			}*/

			cur.close();

			if (!alreadyExists) {

				int dummySmsType = 1; // sent message

				// Create a dummy SMS
				final String ADDRESS = "address";
				final String PERSON = "person";
				final String DATE = "date";
				final String READ = "read";
				final String STATUS = "status";
				final String TYPE = "type";
				final String BODY = "body";
				ContentValues dummySmsValues = new ContentValues();
				dummySmsValues.put(ADDRESS, aAddress);
				dummySmsValues.put(DATE, aDate);
				dummySmsValues.put(READ, 1);
				dummySmsValues.put(STATUS, -1);
				dummySmsValues.put(TYPE, dummySmsType);
				dummySmsValues.put(BODY, "MMS");
				Uri dummySmsUri = mContext.getContentResolver().insert(Uri.parse("content://sms"), dummySmsValues);

				try {

					String threadId = null;
					Cursor dummySmsCursor = mContext.getContentResolver().query(dummySmsUri, null, null, null, null);
					int smsThreadIdColumn = dummySmsCursor.getColumnIndexOrThrow("thread_id");
					if (dummySmsCursor.moveToFirst()) {
						threadId = dummySmsCursor.getString(smsThreadIdColumn);
					}
					dummySmsCursor.close();

					ContentValues values = new ContentValues();

					if (has_mms_thread_id)
						values.put("thread_id", threadId);
					if (has_mms_date)
						values.put("date", aDate);
					if (has_mms_msg_box) {
						if (aSent)
							values.put("msg_box", "2");
						else
							values.put("msg_box", "1");
					}
					if (has_mms_read) {
						if (aRead)
							values.put("read", "1");
						else
							values.put("read", "0");
					}
					if (has_mms_sub)
						values.put("sub", aSubject);
					if (has_mms_ct_t)
						values.put("ct_t", "application/vnd.wap.multipart.related");
					if (has_mms_exp)
						values.put("exp", "604800");
					if (has_mms_m_id)
						values.put("m_id", "weudehdjkwehdkjwhdkjhw");
					if (has_mms_m_cls)
						values.put("m_cls", "personal");
					if (has_mms_m_type)
						if (aSent) {
							values.put("m_type", "128");
						} else {
							values.put("m_type", "132");
						}
					if (has_mms_v)
						values.put("v", "18");
					if (has_mms_m_size)
						values.put("m_size", "178370"); // Assume this doesn't really matter?
					if (has_mms_pri)
						values.put("pri", "129");
					if (has_mms_rr)
						values.put("rr", "129");
					if (has_mms_resp_st) {
						if (aSent)
							values.put("resp_st", "128");
					}
					if (has_mms_tr_id)
						values.put("tr_id", "T1736812636"); // Assume this doesn't matter?
					if (has_mms_d_rpt)
						values.put("d_rpt", "129");

					Uri mmsUri = mContext.getContentResolver().insert(Uri.parse("content://mms"), values);

					// // Log.d(TAG, mmsUri.toString());

					String messageId = mmsUri.getLastPathSegment().trim();

					int imageParts = 0;
					int textParts = 0;

					if (aImageParts != null)
						imageParts = aImageParts.size();

					if (aTextParts != null)
						textParts = aTextParts.size();

					addMmsPart(true, // it is the main smil part
							messageId,
							null, // no data
							generateSmilText(imageParts, textParts),
							"application/smil",
							"<smil>", // The cid as used in the SMIL
							"smil.xml"); // File name - probably not used

					// Enumerate through the text parts and add them
					Iterator<String> textItemIter = aTextParts.iterator();
					int textPartIndex = 0;
					while (textItemIter.hasNext()) {
						addMmsPart(false, // this isn't the main smil part, it's just a text part
								messageId,
								null, // no data
								textItemIter.next(),
								"text/plain",
								"<text_" + Integer.toString(textPartIndex) + ".txt>", // The cid as used in the SMIL
								"text_" + Integer.toString(textPartIndex) + ".txt"); // File name - probably not used
						textPartIndex++;
					}

					// Enumerate through the image parts and add them
					Iterator<byte[]> imageItemIter = aImageParts.iterator();
					int imagePartIndex = 0;
					while (imageItemIter.hasNext()) {
						addMmsPart(false, // this isn't the main smil part, it's just a text part
								messageId,
								imageItemIter.next(),
								null,
								"image/jpeg",
								"<image_" + Integer.toString(imagePartIndex) + ".jpg>", // The cid as used in the SMIL
								"image_" + Integer.toString(imagePartIndex) + ".jpg"); // File name - probably not used
						imagePartIndex++;
					}

					// Add the address part
					addMmsAddress(messageId, // The message to attach this address to
							null, // Should we add the contact ID from the SMS message, if we have it?
							aAddress,
							aSent);

				} catch (Exception ex) {
					// Ignore, but still continue to delete the dummy SMS
					DLog.log(ex);
				}

				/////////////////////////////////////////////////////////

				// delete the dummy SMS
				int deletedRows = mContext.getContentResolver().delete(dummySmsUri, null, null);
				// // Log.d(TAG, "deletedRows " + deletedRows);

			}
		}
		catch (Exception ex2) {
			Log.e(TAG, ex2.toString());
			DLog.log(ex2);
		}
	}
}