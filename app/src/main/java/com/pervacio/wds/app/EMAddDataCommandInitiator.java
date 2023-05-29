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

import java.io.File;

public class EMAddDataCommandInitiator implements EMCommandHandler, EMProgressHandler, EMFileSendingProgressDelegate {

    EMAddDataCommandInitiator(int aDataType) {
        mState =  EMAddDataState.EM_STATE_NONE;
        mDataType = aDataType;
    }

    @Override
    public void start(EMCommandDelegate aDelegate) {
        mCommandDelegate = aDelegate;

        mState = EMAddDataState.EM_WAITING_FOR_DATA_GENERATION;

        mGenerateDataTask = null;

        if (mDataType == EMDataType.EM_DATA_TYPE_CONTACTS)
        {
            EMGenerateContactsXmlAsyncTask generateDataTask = new EMGenerateContactsXmlAsyncTask();
            // generateDataTask.init();
            mGenerateDataTask = generateDataTask;
        }
        else if (mDataType == EMDataType.EM_DATA_TYPE_CALENDAR)
        {
            EMGenerateCalendarXmlAsyncTask generateDataTask = new EMGenerateCalendarXmlAsyncTask();
            // generateDataTask.init();
            mGenerateDataTask = generateDataTask;
        }
        else if (mDataType == EMDataType.EM_DATA_TYPE_SMS_MESSAGES)
        {
            EMGenerateSmsMessagesXmlAsyncTask generateDataTask = new EMGenerateSmsMessagesXmlAsyncTask();
            // generateDataTask.init();
            mGenerateDataTask = generateDataTask;
        }
        else {
            // TODO: unexpected / unsupported data type
        }

        if (mGenerateDataTask != null)
        {
            mGenerateDataTask.setCommandDelegate(mCommandDelegate);
            sendProgressUpdate(EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_OUTGOING_DATA);
            mCommandDelegate.disableTimeoutTimer(); // Disable the timeout timer, because this could be a long running operation - if the connection has gone down we will spot it when we try to send (the timeout timer will be on again then)
            mGenerateDataTask.startTask(this);
        }
    }

    @Override
    public void sent()
    {
        if (mState == EMAddDataState.EM_SENDING_ADD_DATA_COMMAND)
        {
            mState = EMAddDataState.EM_WAITING_FOR_ADD_DATA_RESPONSE;
            mCommandDelegate.getText();
            return;
        }
        else if (mState == EMAddDataState.EM_SENDING_DATA)
        {
            sendProgressUpdate(EMProgressInfo.EMOperationType.EM_OPERATION_SENT_DATA);
            mState = EMAddDataState.EM_WAITING_FOR_FINAL_OK;
            mCommandDelegate.getText();
            return;
        }
    }

    @Override
    public boolean gotText(String aText)
    {
        if (mState == EMAddDataState.EM_WAITING_FOR_ADD_DATA_RESPONSE)
        {
            mState = EMAddDataState.EM_SENDING_DATA;
            String tempFilePath = mGenerateDataTask.getFilePath();
            mCommandDelegate.sendFile(tempFilePath, true, null);
            return true;
        }

        if (mState == EMAddDataState.EM_WAITING_FOR_FINAL_OK)
        {
            mCommandDelegate.commandComplete(true);
            return true;
        }

        return true;
    }

    @Override
    public void taskComplete(boolean aSuccess)
    {
        mCommandDelegate.enableTimeoutTimer();
        if (mState == EMAddDataState.EM_WAITING_FOR_DATA_GENERATION)
        {
            sendProgressUpdate(EMProgressInfo.EMOperationType.EM_OPERATION_SENDING_DATA);

            mState = EMAddDataState.EM_SENDING_ADD_DATA_COMMAND;

            String dataTypeString;
            switch (mDataType) {
                case EMDataType.EM_DATA_TYPE_CONTACTS:
                    dataTypeString = EMStringConsts.DATA_TYPE_CONTACTS;
                    break;
                case EMDataType.EM_DATA_TYPE_CALENDAR:
                    dataTypeString = EMStringConsts.DATA_TYPE_CALENDAR;
                    break;
                case EMDataType.EM_DATA_TYPE_SMS_MESSAGES:
                    dataTypeString = EMStringConsts.DATA_TYPE_SMS_MESSAGES;
                    break;
                case EMDataType.EM_DATA_TYPE_ACCOUNTS:
                    dataTypeString = EMStringConsts.DATA_TYPE_ACCOUNTS;
                    break;
                default:
                    // TODO: error case
                    dataTypeString = "NULL";
                    break;
            }

            File fileInfo = new File(mGenerateDataTask.getFilePath());
            long fileSize = fileInfo.length();
            String dataSizeString = Long.toString(fileSize);

            String commandString = EMStringConsts.EM_COMMAND_TEXT_ADD_DATA + " " + dataTypeString + " " + dataSizeString;

            mCommandDelegate.sendText(commandString);

            return;
        }
    }

    @Override
    public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
        // TODO:
    }

    // Set the delegate to receive notifications about data sending progress
    void setDataCommandDelegate(EMDataCommandDelegate aDelegate)
    {
        mDataCommandDelegate = aDelegate;
    }


    @Override
    public boolean handlesCommand(String aCommand)
    {
        return false;
    }

    @Override
    public boolean gotFile(String aFilePath)
    {
        return false;
    }

//=============================================================================
// From Simple Async Task
// Apart from taskComplete which is placed above for clarity of state
// transitions
//=============================================================================

    @Override
    public void progressUpdate(EMProgressInfo aProgressInfo)
    {
        sendProgressUpdate(aProgressInfo);
    }

//=============================================================================
// Utilities
//=============================================================================

    void sendProgressUpdate(EMProgressInfo aProgressInfo)
    {
        if (mDataCommandDelegate == null)
        {
            return;
        }

        mDataCommandDelegate.progressUpdate(aProgressInfo);
    }


    void sendProgressUpdate(EMProgressInfo.EMOperationType aOperationType)
    {
        if (mState != EMAddDataState.EM_CANCELLED) {
            EMProgressInfo progressInfo = new EMProgressInfo();
            progressInfo.mDataType = mDataType;
            progressInfo.mOperationType = aOperationType;
            progressInfo.mCurrentItemNumber = 0;
            progressInfo.mTotalItems = 0;
            sendProgressUpdate(progressInfo);
        }
    }

    @Override
    public void cancel() {
        mState = EMAddDataState.EM_CANCELLED;
        if (mGenerateDataTask != null)
            mGenerateDataTask.cancel(true);
    }


    private EMGenerateDataTask mGenerateDataTask;

    private EMCommandDelegate mCommandDelegate;
    private EMDataCommandDelegate mDataCommandDelegate;

    @Override
    public void fileSendingProgressUpdate(long mFileBytesSent) {
        // Ignore
    }

    private enum EMAddDataState
    {
        EM_STATE_NONE,
        EM_WAITING_FOR_DATA_GENERATION, // Waiting the the async thread to generate the data
        EM_SENDING_ADD_DATA_COMMAND,
        EM_WAITING_FOR_ADD_DATA_RESPONSE,
        EM_SENDING_DATA,
        EM_WAITING_FOR_FINAL_OK,
        EM_CANCELLED
    };

    private EMAddDataState mState;

    private int mDataType;
}
