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

import com.google.gson.Gson;
import com.pervacio.wds.custom.models.MMS;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;

import java.io.IOException;
import java.util.List;

public class EMGenerateSmsMessagesXmlAsyncTask extends EMGenerateDataTask {
    public void init(boolean aCountOnly) {
        mCountOnly = aCountOnly;
    }

    void sendProgressUpdate(int aCurrentItem, int aTotalItems)
    {
        EMProgressInfo progress = new EMProgressInfo();
        progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_OUTGOING_DATA;
        progress.mDataType      = EMDataType.EM_DATA_TYPE_SMS_MESSAGES;
        progress.mTotalItems        = aTotalItems;
        progress.mCurrentItemNumber = aCurrentItem;
        updateProgressFromWorkerThread(progress);
    }

    private boolean mCountOnly = false;

    @Override
    protected void runTask() {

        long lastUpdateTime = 0;
        long uiUpdateIntervalInMs = 3*1000;
        int currentItemNumber = 0;

        EMXmlGenerator xmlGenerator = new EMXmlGenerator();

        try {
            xmlGenerator.startDocument();
            xmlGenerator.startElement(EMStringConsts.EM_XML_SMS);

            ContentResolver cr = EMUtility.Context().getContentResolver();
            ContentValues values = new ContentValues();

            int addressColumn = -1;
            int dateColumn = -1;
            int textColumn = -1;
            int readColumn = -1;
            int typeColumn = -1;

            // TODO: add attachment & mime type columns for MMS

            String url = "content://sms";
            Uri smsUri = Uri.parse(url);
            String fromDate = "date>=" + CommonUtil.getInstance().getMessageSelectionFrom();

            String whereQuery = "( " +SMS_DB_TYPE + " = '" + SMS_DB_TYPE_SENT + "' OR " +SMS_DB_TYPE + " = '" + SMS_DB_TYPE_INBOX + "' OR " + SMS_DB_TYPE + " = '" + SMS_DB_TYPE_DRAFT + "') AND "+ fromDate;


            Cursor messagesCursor = null;

            messagesCursor = EMUtility.Context().getContentResolver().query(smsUri,
                    null, // Column projection : TODO: optimize this later
                    whereQuery, // WHERE
                    null, // WHERE arguments
                    SMS_DB_DATE +" DESC"); // Order-by - most recent messages first

            if (messagesCursor == null) {
                EMMigrateStatus.setTotalFailure(EMDataType.EM_DATA_TYPE_SMS_MESSAGES);
                return; // It could be a tablet with no SMS provider
            }

            mNumberOfEntries = messagesCursor.getCount();

            addressColumn = messagesCursor.getColumnIndex(SMS_DB_ADDRESS);
            dateColumn = messagesCursor.getColumnIndex(SMS_DB_DATE);
            textColumn = messagesCursor.getColumnIndex(SMS_DB_BODY);
            readColumn = messagesCursor.getColumnIndex(SMS_DB_READ);
            typeColumn = messagesCursor.getColumnIndex(SMS_DB_TYPE);

            if (!mCountOnly) {

                //Currently disabling the Max messages count check.

               /* if (mNumberOfEntries > EMConfig.MAXIMUM_NUMBER_OF_SMS_MESSAGES_TO_SEND)
                    mNumberOfEntries = EMConfig.MAXIMUM_NUMBER_OF_SMS_MESSAGES_TO_SEND;
*/              // Fix for 50292
                for (messagesCursor.moveToLast(); !messagesCursor.isBeforeFirst(); messagesCursor.moveToPrevious()) {
                //if (messagesCursor.moveToFirst()) {
                   // do {
                        if (isCancelled()) {
                            messagesCursor.close();
                            // TODO: remove temporary file
                            return;
                        }

                        String folder = null; // Will be either sent_message or received_message

                        long type = messagesCursor.getLong(typeColumn);

                        if (type == SMS_DB_TYPE_INBOX)
                            folder = EMStringConsts.EM_XML_SMS_ENTRY_FOLDER_INBOX;

                        if (type == SMS_DB_TYPE_SENT)
                            folder = EMStringConsts.EM_XML_SMS_ENTRY_FOLDER_SENT;

                        if (type == SMS_DB_TYPE_DRAFT)
                            folder = EMStringConsts.EM_XML_SMS_ENTRY_FOLDER_DRAFT;

                        if (folder != null) {
                            ++currentItemNumber;
                            xmlGenerator.startElement(EMStringConsts.EM_XML_SMS_ENTRY);

                            try {
                                xmlGenerator.startElement(EMStringConsts.EM_XML_SMS_ENTRY_FOLDER);
                                xmlGenerator.writeText(folder);
                                xmlGenerator.endElement(EMStringConsts.EM_XML_SMS_ENTRY_FOLDER);

                                if (type == SMS_DB_TYPE_INBOX)
                                    writeColumnToXml(messagesCursor, xmlGenerator, EMStringConsts.EM_XML_SMS_ENTRY_SENDER, addressColumn);
                                else
                                    writeColumnToXml(messagesCursor, xmlGenerator, EMStringConsts.EM_XML_SMS_ENTRY_ADDRESSEE, addressColumn);

                                writeTimeColumnToXml(messagesCursor, xmlGenerator, EMStringConsts.EM_XML_SMS_ENTRY_DEVICE_DATE, dateColumn);
                                writeColumnToXml(messagesCursor, xmlGenerator, EMStringConsts.EM_XML_SMS_ENTRY_DATA, textColumn);
                                writeColumnToXml(messagesCursor, xmlGenerator, EMStringConsts.EM_XML_SMS_READ, readColumn);
                                if ((System.currentTimeMillis() - lastUpdateTime) > uiUpdateIntervalInMs) {
                                    lastUpdateTime = System.currentTimeMillis();
                                    sendProgressUpdate(currentItemNumber, mNumberOfEntries);
                                }
                                EMMigrateStatus.addItemTransferred(EMDataType.EM_DATA_TYPE_SMS_MESSAGES);
                            } catch (Exception ex) {
                                DLog.log(ex);
                                EMMigrateStatus.addItemNotTransferred(EMDataType.EM_DATA_TYPE_SMS_MESSAGES);
                            } finally {
                                xmlGenerator.endElement(EMStringConsts.EM_XML_SMS_ENTRY);
                            }

                            /*if (currentItemNumber > mNumberOfEntries) {
                                break; // Only copy up to the limit of SMS messages
                            }*/
                        }
                   // } while (messagesCursor.moveToNext());
                }

                if(Constants.MMS_SUPPORT) {
                    EMMMSUtils emmmsUtils = new EMMMSUtils();
                    List<MMS> mmsList = emmmsUtils.getMMSDetails(null);
                    for (MMS mms : mmsList) {
                        xmlGenerator.startElement("mms");
                        xmlGenerator.writeText(new Gson().toJson(mms));
                        xmlGenerator.endElement("mms");
                    }
                }

            }

            messagesCursor.close();
        } catch (Exception ex) {
            EMMigrateStatus.setTotalFailure(EMDataType.EM_DATA_TYPE_SMS_MESSAGES);
        }
        finally {
            try {
                xmlGenerator.endElement(EMStringConsts.EM_XML_SMS);
            } catch (IOException e) {
                // Nothing we can do here
            }

            try {
                setFilePath(xmlGenerator.endDocument());
            } catch (IOException e) {
                // Nothing we can do here
            }
            sendProgressUpdate(currentItemNumber,mNumberOfEntries);
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
                // DLog.log(ex);
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
                // DLog.log(ex);
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
                // DLog.log(ex);
                // TODO:
            } finally {
                aXmlSerializer.endElement(aTag);
            }
        }
    }

    public int getNumberOfEntries() {
        return mNumberOfEntries;
    }

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

    private int mNumberOfEntries = 0;
}
