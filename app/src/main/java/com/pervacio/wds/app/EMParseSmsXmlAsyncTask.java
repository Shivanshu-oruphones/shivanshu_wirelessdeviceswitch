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
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.pervacio.wds.custom.models.MMS;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.DeviceInfo;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

public class EMParseSmsXmlAsyncTask extends EMParseDataInThread {

	private int mTotalEntries = 0;
	private boolean ENABLE_BULKINSERST = true;
	private final int INSERT_COUNT = 28;
	private ContentValues[] contentValues = new ContentValues[INSERT_COUNT];
	private int stackedMessagesCount = 0;
	private final long uiUpdateIntervalInMs = 3*1000;
	private long lastUpdateTime= 0;
	private int mNumberOfEntries=0;
	private static final String TAG = "DeviceSwitchV2";
	private EMDuplicateFinder mDuplicateFinder;
	private ContentResolver mContentResolver = null;
	private boolean allowDuplicates = Constants.IS_MMDS;
	private long initialSMSCount;

	private Queue<ContentValues[]> valesQueue = new LinkedList<>();

	private messagesInsertionThread insertionThread = new messagesInsertionThread();


	@Override
	protected void runTask()
	{
		DLog.log("inside EMParseSmsXmlAsyncTask parseData");
		mContentResolver = mContext.getContentResolver();
		// Set up the duplicate finder (generate a hash for each existing note)
		List<String> duplicateKeys = new ArrayList<String>();

		duplicateKeys.add(SMS_DB_ADDRESS);
		duplicateKeys.add(SMS_DB_DATE);
		duplicateKeys.add(SMS_DB_DATE_SENT);
		duplicateKeys.add(SMS_DB_BODY);
		Uri smsUri  = Uri.parse(SMS_QUERY_URI);

		initialSMSCount = DeviceInfo.getInstance().getContentDetails(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, true);

		try {
			if (!allowDuplicates)
				mDuplicateFinder = new EMDuplicateFinder(duplicateKeys, smsUri, mContentResolver);
			DashboardLog.getInstance().setMessagesStartTime(CommonUtil.getInstance().getBackupStartedTime());
			DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.MESSAGE, -1, -1, Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, true);
			mTotalEntries = parse(true);
			parse(false);
			DashboardLog.getInstance().setMessagesEndTime(System.currentTimeMillis());
			DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.MESSAGE,  mNumberOfEntries,-1,  Constants.TRANSFER_STATUS.SUCCESS, Constants.TRANSFER_STATE.COMPLETED, true);
		} catch (Exception aException) {
			DLog.log(aException.getMessage());
			EMMigrateStatus.setTotalFailure(EMDataType.EM_DATA_TYPE_SMS_MESSAGES);
		}
	}

	int parse(boolean aCountOnly) throws IOException, XmlPullParserException {
		DLog.log("EMParseSmsXmlAsyncTask : parse");
		int entryNumber = 0;
		EMXmlPullParser pullParser = new EMXmlPullParser();
		pullParser.setFilePath(mFilePath);

		EMXmlPullParser.EMXmlNodeType nodeType = pullParser.readNode();
		if (!aCountOnly) {
			insertionThread.start();
		}

		while (nodeType != EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ROOT_ELEMENT) // While there is no error and we haven't reached the last node
		{
		/*	if (isCancelled())
				return 0;*/

			if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_START_ELEMENT)
			{
				String elementName = pullParser.name();

				if (elementName.equals(EMStringConsts.EM_XML_SMS_ENTRY))
				{
					++entryNumber;

					/*if (((System.currentTimeMillis() - lastUpdateTime) > uiUpdateIntervalInMs) && !aCountOnly)
					{
						lastUpdateTime = System.currentTimeMillis();
						EMProgressInfo progress = new EMProgressInfo();
						progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_INCOMING_DATA;
						progress.mDataType = EMDataType.EM_DATA_TYPE_SMS_MESSAGES;
						progress.mTotalItems = mTotalEntries;
						progress.mCurrentItemNumber = entryNumber;
						updateProgressFromWorkerThread(progress);
					}*/

					// TODO: init note object

					boolean endOfSMSEntry = false;

					int smsXmlLevel = 0;

					String dataTypeName = "";

					boolean messageIsInbound = false;
					String messageTimeStamp = null;
					String messageText = null;
					String messageContactNumber = null;
					String isSMSRead = "1";
					int messageType=0;

					while (!endOfSMSEntry)
					{
						nodeType = pullParser.readNode();

						if (smsXmlLevel == 0)
						{
							if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ELEMENT)
							{
								endOfSMSEntry = true;

								if (!aCountOnly)
								{
									saveSMS(messageText, messageContactNumber, messageType, messageTimeStamp,isSMSRead);
								}
							}
							else if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_START_ELEMENT)
							{
								// We have found a data element, so save the type
								dataTypeName = pullParser.name();
								smsXmlLevel += 1;
							}
						}

						if (smsXmlLevel == 1)
						{
							if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ELEMENT)
							{
								dataTypeName = "";
								smsXmlLevel -= 1;
							}
							else if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT)
							{
								String value = pullParser.value();

								if (dataTypeName.equals(EMStringConsts.EM_XML_SMS_ENTRY_FOLDER))
								{
									if (value.equals(EMStringConsts.EM_XML_SMS_ENTRY_FOLDER_INBOX))
										messageType=SMS_DB_TYPE_INBOX;
									else if (value.equals(EMStringConsts.EM_XML_SMS_ENTRY_FOLDER_SENT))
										messageType=SMS_DB_TYPE_SENT;
									else if (value.equals(EMStringConsts.EM_XML_SMS_ENTRY_FOLDER_DRAFT))
										messageType=SMS_DB_TYPE_DRAFT;
								}
								else if (dataTypeName.equals(EMStringConsts.EM_XML_SMS_ENTRY_DEVICE_DATE))
								{
									messageTimeStamp = value;
								}
								else if (dataTypeName.equals(EMStringConsts.EM_XML_SMS_ENTRY_DATA))
								{
									messageText = value;
								}
								else if (dataTypeName.equals(EMStringConsts.EM_XML_SMS_ENTRY_SENDER))
								{
									messageContactNumber = value;
								}
								else if (dataTypeName.equals(EMStringConsts.EM_XML_SMS_ENTRY_ADDRESSEE))
								{
									messageContactNumber = value;
								}else if (dataTypeName.equals(EMStringConsts.EM_XML_SMS_READ))
								{
									isSMSRead = value;
								}
							}
						}
					}

					// TODO: what should we do if it's not text? signal a bad-xml error probably?
				} else if (elementName.equals("mms")) {
					++entryNumber;
					if (!aCountOnly) {
						nodeType = pullParser.readNode();
						String mms = pullParser.value();
						mmsList.add(new Gson().fromJson(mms, MMS.class));
						DLog.log(mms);
					}

				}
			}

			nodeType = pullParser.readNode();
		}
		if (!aCountOnly) {
			DLog.log("Messages Queue prepared "+System.currentTimeMillis());
			if (stackedMessagesCount != 0) {
				ArrayList<ContentValues> list = new ArrayList<ContentValues>(Arrays.asList(contentValues));
				try {
					list.removeAll(Collections.singleton(null));
				} catch (Exception e) {
					e.printStackTrace();
				}
				ContentValues[] values = new ContentValues[list.size()];
				for (int i = 0; i < list.size(); i++) {
					values[i] = list.get(i);
				}
				addToQueue(values);
			}
			processingCompleted =true;
			while (!valesQueue.isEmpty()){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			EMMMSUtils emmmsUtils = new EMMMSUtils();
			for(MMS mms : mmsList){
				emmmsUtils.insertMMS(mms);
				mNumberOfEntries++;
			}
			EMProgressInfo progress = new EMProgressInfo();
			progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_INCOMING_DATA;
			progress.mDataType = EMDataType.EM_DATA_TYPE_SMS_MESSAGES;
			progress.mTotalItems = mTotalEntries;
			progress.mCurrentItemNumber = mTotalEntries;
			updateProgressFromWorkerThread(progress);
			DLog.log("Processing Messages Done >> ");
		}

		return entryNumber;
	}

	private List<MMS> mmsList = new ArrayList<>();

	void saveSMS(String bodyText, String contactNumber, int messageType, String aTimeStamp, String isSMSread) {
		try {
			long date = Long.parseLong(aTimeStamp);
			ContentValues values = new ContentValues();
			values.put(SMS_DB_TYPE, messageType);
			values.put(SMS_DB_ADDRESS, contactNumber);
			values.put(SMS_DB_DATE, Long.toString(date * 1000));
			values.put(SMS_DB_DATE_SENT, Long.toString(date * 1000));
			values.put(SMS_DB_BODY, bodyText);
			values.put(SMS_DB_READ, isSMSread);
			values.put(SMS_DB_SEEN, Integer.toString(1));
			boolean insertMessage = (allowDuplicates || (mDuplicateFinder!=null && !mDuplicateFinder.itemExists(values)));
			if (insertMessage) {
				contentValues[stackedMessagesCount++] = values;
				if (stackedMessagesCount == INSERT_COUNT) {
					addToQueue(contentValues);
				}
			} else {
				++mNumberOfEntries;
				EMMigrateStatus.setPreviouslyTransferredMessages(values.toString());
				DLog.log("Skipping Message as its already exists");
				sendProgressUpdate();
			}
			EMMigrateStatus.addItemTransferred(EMDataType.EM_DATA_TYPE_SMS_MESSAGES);
		} catch (Exception ex) {
			DLog.log(ex.getMessage());
			EMMigrateStatus.addItemNotTransferred(EMDataType.EM_DATA_TYPE_SMS_MESSAGES);
		}
	}



	private static final long   ONE_DAY = (24*60*60*1000);

	private static final String SMS_QUERY_URI = "content://sms";

	private static final String SMS_DB_MESSAGE_ID = "_ID";
	private static final String SMS_DB_ADDRESS    = "address";
	private static final String SMS_DB_PERSON     = "person";
	private static final String SMS_DB_DATE       = "date";
	private static final String SMS_DB_DATE_SENT       = "date_sent";
	private static final String SMS_DB_READ       = "read";
	private static final String SMS_DB_STATUS     = "status";
	private static final String SMS_DB_TYPE       = "type";
	private static final String SMS_DB_BODY       = "body";
	private static final String SMS_DB_SEEN       = "seen";

	public static final int SMS_DB_TYPE_INBOX = 1;
	public static final int SMS_DB_TYPE_SENT  = 2;
	public static final int SMS_DB_TYPE_DRAFT  = 3;

	public void createEntry(ContentResolver aResolver, int aFolder, String aAddress, long aDate, String aMessage,String isSMSread)
	{
		Uri smsUri  = Uri.parse(SMS_QUERY_URI);

		ContentValues values = new ContentValues();

		values.put(SMS_DB_TYPE,    aFolder);
		values.put(SMS_DB_ADDRESS, aAddress);
		values.put(SMS_DB_DATE,    Long.toString(aDate * 1000));
		values.put(SMS_DB_DATE_SENT,    Long.toString(aDate * 1000));
		values.put(SMS_DB_BODY,    aMessage);
		values.put(SMS_DB_READ,    isSMSread);
		values.put(SMS_DB_SEEN,    Integer.toString(1));


		/*boolean isDuplicate = mDuplicateFinder.itemExists(values);

		if (!isDuplicate) {
			Uri insertUri = aResolver.insert(smsUri, values);

			if (insertUri == null) {
				errorit("createEntry, Unable to insert new message");
			} else {
				logit("createEntry, Message inserted, Uri: " + insertUri);
			}
		}*/

		//We'll not check for duplicate record. Insert forcefully.
		Uri insertUri = aResolver.insert(smsUri, values);
	}

	public boolean bulkInsertMessages(ContentResolver aResolver, ContentValues[] contentValuesArray) {
		try {
			Uri smsUri = Uri.parse(SMS_QUERY_URI);
			int insertUri = aResolver.bulkInsert(smsUri, contentValuesArray);
			DLog.log("Bulk insert of messages, newly created rows : "+insertUri);
			if (insertUri != contentValuesArray.length) {
				DLog.log("Bulk Insertion of messages failed. Tried to Inserted " + contentValuesArray.length + "messages,but " + insertUri + " messages Insreted.");
				ENABLE_BULKINSERST = false;
				return false;
			}
			if(mNumberOfEntries == 0) { // cross checking bulk insertion
				long afterBulkInsertionSMSCount = DeviceInfo.getInstance().getContentDetails(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, true);
				DLog.log("SMS count before bulk insertion from query : " + initialSMSCount);
				DLog.log("SMS count after bulk insertion from query : " + afterBulkInsertionSMSCount);
				if ((afterBulkInsertionSMSCount - initialSMSCount) != contentValuesArray.length) {
					DLog.log("Messages are not inserted with bulk insertion, Cross checked with query");
					ENABLE_BULKINSERST = false;
					return false;
				}
			}
			mNumberOfEntries += insertUri;
		} catch (Exception e) {
			ENABLE_BULKINSERST = false;
			DLog.log("Exception in bulk Insertion of Messages. Exception : " + e.getMessage());
			return false;
		}
		return true;
	}


	static private void traceit(String aText)
	{
		Log.v(TAG, aText);
//		DLog.verbose(TAG, aText);
	}

	static void logit(String aText)
	{
		// Log.d(TAG, aText);
		//DLog.log(TAG, aText);
	}

	static void warnit(String aText)
	{
		Log.e(TAG, aText);
//		DLog.warn(TAG, aText);
	}

	static private void errorit(String aText)
	{
		Log.e(TAG, aText);
//		DLog.error(TAG, aText);
	}


	private class messagesInsertionThread extends Thread {
		@Override
		public void run() {
			super.run();
			while (!processingCompleted || !valesQueue.isEmpty()) {
				try {
					ContentValues[] contentValues = valesQueue.remove();
					if (ENABLE_BULKINSERST && !Constants.SMS_BULK_INSERTION_PROBLAMATIC_MODELS.contains(Build.MODEL)) {
						try {
							boolean inserted = bulkInsertMessages(mContext.getContentResolver(), contentValues);
							if (!inserted) {
								DLog.log("bulk insertion is failed & retrying normal insertion");
								Uri smsUri = Uri.parse(SMS_QUERY_URI);
								for (int i = 0; i < contentValues.length; i++) {
									mContext.getContentResolver().insert(smsUri, contentValues[i]);
									mNumberOfEntries++;
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						Uri smsUri = Uri.parse(SMS_QUERY_URI);
						for (int i = 0; i < contentValues.length; i++) {
							mContext.getContentResolver().insert(smsUri, contentValues[i]);
							mNumberOfEntries++;
							sendProgressUpdate();
						}
					}
					if (!valesQueue.isEmpty())
						sendProgressUpdate();
				} catch (NoSuchElementException ex) {
					DLog.log("Exception in getting element from Queue " + ex.getMessage());
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				} catch (Exception e) {
					DLog.log("Exception in getting element from Queue " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	private boolean processingCompleted = false;

	private void addToQueue(ContentValues[] contentValues) {
		ContentValues[] cvs = contentValues.clone();
		valesQueue.add(cvs);
		stackedMessagesCount = 0;
		this.contentValues = new ContentValues[INSERT_COUNT];
	}

	private void sendProgressUpdate() {
		if (((System.currentTimeMillis() - lastUpdateTime) > uiUpdateIntervalInMs)) {
			lastUpdateTime = System.currentTimeMillis();
			EMProgressInfo progress = new EMProgressInfo();
			progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_INCOMING_DATA;
			progress.mDataType = EMDataType.EM_DATA_TYPE_SMS_MESSAGES;
			progress.mTotalItems = mTotalEntries;
			progress.mCurrentItemNumber = mNumberOfEntries;
			updateProgressFromWorkerThread(progress);
		}
	}
}
