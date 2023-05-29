/**
 * This is class is used to generate call logs backup.
 *
 * Created by Ravikumar D on 29-Nov-17.
 */

package com.pervacio.wds.app;

import android.database.Cursor;
import android.provider.CallLog;

import java.io.IOException;
import java.util.Date;

public class EMGenerateCallLogsBackupTask extends EMGenerateDataInThread {
    public String filePath = null;

    @Override
    protected void runTask() {
        String msg = null;
        // call log backup code
        DLog.log(">> EMGenerateCallLogdBackupTaskTask::run()");

        EMXmlGenerator xmlGenerator = null;
        long lastUpdateTime = 0;
        long uiUpdateIntervalInMs = 3 * 1000;
        int currentItemNumber = 0;
        int mNumberOfEntries = 0;

        // Generate the handshake XML
        try {
            xmlGenerator = new EMXmlGenerator();
            xmlGenerator.startDocument();

            DLog.log("About to query call logs");
            Cursor managedCursor = EMUtility.Context().getContentResolver().query(CallLog.Calls.CONTENT_URI, null,
                    null, null, null);
            mNumberOfEntries = managedCursor.getCount();
            int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
            int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
            int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
            int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
            while (managedCursor.moveToNext()) {
                String phNumber = managedCursor.getString(number);
                String callType = managedCursor.getString(type);
                String callDate = managedCursor.getString(date);
                Date callDayTime = new Date(Long.valueOf(callDate));
                String callDuration = managedCursor.getString(duration);
                String dir = null;
                int dircode = Integer.parseInt(callType);
                switch (dircode) {
                    case CallLog.Calls.OUTGOING_TYPE:
                        dir = EMStringConsts.CALLLOG_OUTGOING;
                        break;

                    case CallLog.Calls.INCOMING_TYPE:
                        dir = EMStringConsts.CALLLOG_INCOMING;
                        break;

                    case CallLog.Calls.MISSED_TYPE:
                        dir = EMStringConsts.CALLLOG_MISSED;
                        break;

                    case CallLog.Calls.REJECTED_TYPE:
                        dir = EMStringConsts.CALLLOG_REJECTED;
                        break;

                    case CallLog.Calls.VOICEMAIL_TYPE:
                        dir = EMStringConsts.CALLLOG_VOICEMAIL;
                        break;

                    case CallLog.Calls.BLOCKED_TYPE:
                        dir = EMStringConsts.CALLLOG_BLOCKED;
                        break;
                }
                // Write the vCard as an XML entry
                xmlGenerator.startElement(EMStringConsts.EM_XML_CALL_LOG_ENTRY);
                xmlGenerator.startElement(EMStringConsts.EM_XML_PHONE_NUMBER);
                xmlGenerator.writeText(phNumber);
                xmlGenerator.endElement(EMStringConsts.EM_XML_PHONE_NUMBER);

                xmlGenerator.startElement(EMStringConsts.EM_XML_CALL_TYPE);
                xmlGenerator.writeText(dir);
                xmlGenerator.endElement(EMStringConsts.EM_XML_CALL_TYPE);

                xmlGenerator.startElement(EMStringConsts.EM_XML_CALL_DATE);
                xmlGenerator.writeText(callDate);
                xmlGenerator.endElement(EMStringConsts.EM_XML_CALL_DATE);

                xmlGenerator.startElement(EMStringConsts.EM_XML_CALL_DURATION);
                xmlGenerator.writeText(callDuration);
                xmlGenerator.endElement(EMStringConsts.EM_XML_CALL_DURATION);
                xmlGenerator.endElement(EMStringConsts.EM_XML_CALL_LOG_ENTRY);
                ++currentItemNumber;
                if ((System.currentTimeMillis() - lastUpdateTime) > uiUpdateIntervalInMs) {
                    lastUpdateTime = System.currentTimeMillis();
                    EMProgressInfo progress = new EMProgressInfo();
                    progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_OUTGOING_DATA;
                    progress.mDataType = EMDataType.EM_DATA_TYPE_CALL_LOGS;
                    progress.mTotalItems = mNumberOfEntries;
                    progress.mCurrentItemNumber = currentItemNumber;
                    updateProgressFromWorkerThread(progress);
                }
                EMMigrateStatus.addItemTransferred(EMDataType.EM_DATA_TYPE_CALL_LOGS);
            }
            managedCursor.close();
        } catch (Exception ex) {
            DLog.log("Exception while reading Call logs : " + ex);
        } finally {
            EMProgressInfo progress = new EMProgressInfo();
            progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_OUTGOING_DATA;
            progress.mDataType = EMDataType.EM_DATA_TYPE_CALL_LOGS;
            progress.mTotalItems = mNumberOfEntries;
            progress.mCurrentItemNumber = currentItemNumber;
            updateProgressFromWorkerThread(progress);
            try {
                xmlGenerator.endElement(EMStringConsts.EM_XML_ROOT);
            } catch (Exception ex) {
                // nothing we can do here
            }
            try {
                setFilePath(xmlGenerator.endDocument());
            } catch (IOException e) {
                // Nothing we can do here
            }
        }
    }
}
