/**
 * This is class is used to generate call logs restoration.
 *
 * Created by Ravikumar D on 29-Nov-17.
 */
package com.pervacio.wds.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.provider.CallLog;

import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class EMParseCallLogsXmlInThread extends EMParseDataInThread {
    private int mTotalEntries = 0;
    EMPreviouslyTransferredContentRegistry emPreviouslyTransferredContentRegistry = null;

    @Override
    protected void runTask() {
        DLog.log("inside EMParseCallLogsXmlInThread runTask");
        emPreviouslyTransferredContentRegistry = EMPreviouslyTransferredContentRegistry.getInstanceOfEMPreviouslyTransferredContentRegistry();
        DashboardLog.getInstance().setCallLogsStartTime(CommonUtil.getInstance().getBackupStartedTime());
        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.CALLLOG, -1, -1, Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, true);
        try {
            mTotalEntries = parseXml(true);
            parseXml(false);
        } catch (Exception ex) {
            DLog.log("exception in runtask of EMParseCallLogsXmlInThread " + ex);
        }
        DashboardLog.getInstance().setCallLogsEndTime(System.currentTimeMillis());
        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.CALLLOG, mTotalEntries, -1, Constants.TRANSFER_STATUS.SUCCESS, Constants.TRANSFER_STATE.COMPLETED, true);
    }

    private int parseXml(boolean aCountOnly) throws IOException, XmlPullParserException {
        long lastUpdateTime = 0;
        int entryNumber = 0;
        long uiUpdateIntervalInMs = 3 * 1000;
        EMXmlPullParser pullParser = new EMXmlPullParser();
        pullParser.setFilePath(mFilePath);

        EMXmlPullParser.EMXmlNodeType nodeType = pullParser.readNode();

        while (nodeType != EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ROOT_ELEMENT) // While there is no error and we haven't reached the last node
        {

            if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_START_ELEMENT) {
                String elementName = pullParser.name();

                if (elementName.equals(EMStringConsts.EM_XML_CALL_LOG_ENTRY)) {
                    ++entryNumber;

                    if ((System.currentTimeMillis() - lastUpdateTime) > uiUpdateIntervalInMs && !aCountOnly) {
                        lastUpdateTime = System.currentTimeMillis();
                        EMProgressInfo progress = new EMProgressInfo();
                        progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_INCOMING_DATA;
                        progress.mDataType = EMDataType.EM_DATA_TYPE_CALL_LOGS;
                        progress.mTotalItems = mTotalEntries;
                        progress.mCurrentItemNumber = entryNumber;
                        DLog.log("Processing call logs >> " + entryNumber);
                        updateProgressFromWorkerThread(progress);
                    }
                    boolean endOfCallLogEntry = false;
                    int callLogXmlLevel = 0;
                    String dataTypeName = "";
                    String mPhoneNumber = null;
                    String mCallType = null;
                    String mCallDate = null;
                    String mCallDuration = null;
                    while (!endOfCallLogEntry) {
                        nodeType = pullParser.readNode();

                        if (callLogXmlLevel == 0) {
                            if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ELEMENT) {
                                endOfCallLogEntry = true;
                                if (!aCountOnly) {
                                    saveCallLogs(mPhoneNumber, mCallDate, mCallDuration, mCallType);
                                }
                            } else if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_START_ELEMENT) {
                                // We have found a data element, so save the type
                                dataTypeName = pullParser.name();
                                callLogXmlLevel += 1;
                            }
                        }

                        if (callLogXmlLevel == 1) {
                            if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ELEMENT) {
                                dataTypeName = "";
                                callLogXmlLevel -= 1;
                            } else if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                                String value = pullParser.value();
                                if (dataTypeName.equals(EMStringConsts.EM_XML_PHONE_NUMBER)) {
                                    mPhoneNumber = value;
                                } else if (dataTypeName.equals(EMStringConsts.EM_XML_CALL_DATE)) {
                                    mCallDate = value;
                                } else if (dataTypeName.equals(EMStringConsts.EM_XML_CALL_DURATION)) {
                                    mCallDuration = value;
                                } else if (dataTypeName.equals(EMStringConsts.EM_XML_CALL_TYPE)) {
                                    mCallType = value;
                                }
                            }
                        }
                    }
                }
            }

            nodeType = pullParser.readNode();
        }
        if (!aCountOnly) {
            EMProgressInfo progress = new EMProgressInfo();
            progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_INCOMING_DATA;
            progress.mDataType = EMDataType.EM_DATA_TYPE_CALL_LOGS;
            progress.mTotalItems = mTotalEntries;
            progress.mCurrentItemNumber = entryNumber;
            updateProgressFromWorkerThread(progress);
            DLog.log("Processing CallLogs Done >> ");

        }
        return entryNumber;
    }

    private void saveCallLogs(String mPhoneNumber, String mCallDate, String mCallDuration, String mCallType) {
        //call logs restoration code
        try {
            long date = Long.parseLong(mCallDate);
            long callDuration = Long.parseLong(mCallDuration);
            createEntry(mContext.getContentResolver(), mPhoneNumber, date, callDuration, mCallType);
            EMMigrateStatus.addItemTransferred(EMDataType.EM_DATA_TYPE_CALL_LOGS);
        } catch (Exception ex) {
            EMMigrateStatus.addItemNotTransferred(EMDataType.EM_DATA_TYPE_CALL_LOGS);
        }
    }

    private void createEntry(ContentResolver contentResolver, String mPhoneNumber, long date, long mCallDuration, String mCallType) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CallLog.Calls.NUMBER, mPhoneNumber);
        contentValues.put(CallLog.Calls.DATE, date);
        contentValues.put(CallLog.Calls.DURATION, mCallDuration);
        contentValues.put(CallLog.Calls.TYPE, getCallType(mCallType));
        if (emPreviouslyTransferredContentRegistry.itemHasBeenPreviouslyTransferred(contentValues.toString())) {
            DLog.log("Skipping previously transferred call log");
        } else {
            //TODO Logs need to be added
            contentResolver.insert(CallLog.Calls.CONTENT_URI, contentValues);
            emPreviouslyTransferredContentRegistry.addToPreviouslyTransferredItem(contentValues.toString());

        }
    }

    private int getCallType(String mCallType) {
        if (mCallType != null && mCallType.equalsIgnoreCase(EMStringConsts.CALLLOG_OUTGOING)) {
            return CallLog.Calls.OUTGOING_TYPE;
        } else if (mCallType != null && mCallType.equalsIgnoreCase(EMStringConsts.CALLLOG_INCOMING)) {
            return CallLog.Calls.INCOMING_TYPE;
        } else if (mCallType != null && mCallType.equalsIgnoreCase(EMStringConsts.CALLLOG_MISSED)) {
            return CallLog.Calls.MISSED_TYPE;
        } else if (mCallType != null && mCallType.equalsIgnoreCase(EMStringConsts.CALLLOG_REJECTED)) {
            return CallLog.Calls.REJECTED_TYPE;
        } else if (mCallType != null && mCallType.equalsIgnoreCase(EMStringConsts.CALLLOG_VOICEMAIL)) {
            return CallLog.Calls.VOICEMAIL_TYPE;
        } else if (mCallType != null && mCallType.equalsIgnoreCase(EMStringConsts.CALLLOG_BLOCKED)) {
            return CallLog.Calls.BLOCKED_TYPE;
        }
        return -1;
    }

    private String TAG = "EMParseCallLogsXmlInThread";
}
