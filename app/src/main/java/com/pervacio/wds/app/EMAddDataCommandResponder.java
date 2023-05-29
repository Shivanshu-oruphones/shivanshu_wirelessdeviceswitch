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

import android.content.Context;

import com.pervacio.wds.sdk.internal.CMDCryptoSettings;

public class EMAddDataCommandResponder implements EMCommandHandler, EMProgressHandler {

    EMAddDataCommandResponder(EMDataCommandDelegate aDataCommandDelegate,
                                Context aContext) {
        mContext = aContext;
        mDataCommandDelegate = aDataCommandDelegate;
    }

    @Override
    public void start(EMCommandDelegate aDelegate) {

        EMMigrateStatus.setStartTransfer(); // It's okay to call this multiple times

        mDelegate = aDelegate;

        if (!EMConfig.ALLOW_UNENCRYPTED_WIFI_TRANSFER) {
            if (!CMDCryptoSettings.encryptionVerified()) {
                mState = EMAddDataState.EM_SENDING_FINAL_OK;
                mDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_ERROR);
                return;
            }
        }

        boolean parametersAreOkay = true;

        if (mCommandAndParameters.length < 3)  // We must have at least 3 strings: ADD_DATA + [type] + [size]
            parametersAreOkay = false;

        if (parametersAreOkay) {
            try {
                mFileSize = Long.parseLong(mCommandAndParameters[2], 10);
            } catch (Exception ex) {
                parametersAreOkay = false;
            }
        }

        int dataType = -1;

        if (parametersAreOkay) {
            String dataTypeString = mCommandAndParameters[1];

            /*** Darpan: Below is the piece of code where the trasnfer happens ***/
            if (dataTypeString.equals(EMStringConsts.DATA_TYPE_CONTACTS)) {
                mParseDataTask = new EMParseContactsXmlAsyncTask();
                dataType = EMDataType.EM_DATA_TYPE_CONTACTS;
            }
            else if (dataTypeString.equals(EMStringConsts.DATA_TYPE_CALENDAR)) {
                mParseDataTask = new EMParseCalendarXmlAsyncTask();
                dataType = EMDataType.EM_DATA_TYPE_CALENDAR;
            }
            else if (dataTypeString.equals(EMStringConsts.DATA_TYPE_SMS_MESSAGES)) {
                mParseDataTask = new EMParseSmsXmlAsyncTask();
                dataType = EMDataType.EM_DATA_TYPE_SMS_MESSAGES;
            }
            else
                parametersAreOkay = false; // We have an unsupported data type string
        }

        if (parametersAreOkay) {
            EMProgressInfo progressInfo = new EMProgressInfo();
            progressInfo.mDataType = dataType;
            progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_RECEIVING_DATA;
            mDataCommandDelegate.progressUpdate(progressInfo);

            mState = EMAddDataState.EM_SENDING_INITIAL_OK;
            mDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
        }
        else {
            mDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_ERROR);
            mDelegate.commandComplete(false);
        }
    }

    @Override
    public boolean handlesCommand(String aCommand) {
        if (aCommand.startsWith(EMStringConsts.EM_COMMAND_TEXT_ADD_DATA + " ")) {
            mCommandAndParameters = aCommand.split(" ");
            return true;
        }
        else
            return false;
    }

    @Override
    public boolean gotText(String aText) {
        // We don't expect any text after the initial command
        return false;
    }

    @Override
    public boolean gotFile(String aDataPath) {
        if (mState == EMAddDataState.EM_WAITING_FOR_RAW_FILE_DATA) {
            mState = EMAddDataState.EM_PROCESSING_RECEIVED_DATA;
            mDelegate.startNoopTimer();
            mParseDataTask.startTask(aDataPath, true, mContext, this);
        }
        else {
            // TODO: we don't expect to get here. The only possible state is EM_WAITING_FOR_RAW_FILE_DATA
        }

        return true;
    }

    @Override
    public void sent() {
        if (mState == EMAddDataState.EM_SENDING_INITIAL_OK) {
            mState = EMAddDataState.EM_WAITING_FOR_RAW_FILE_DATA;
            String tempFilePath = EMUtility.temporaryFileName();
            mDelegate.getRawDataAsFile(mFileSize, tempFilePath);
        }
        else if (mState == EMAddDataState.EM_SENDING_FINAL_OK) {
            mDelegate.commandComplete(true);
        }
    }

    @Override
    public void taskComplete(boolean aSuccess)
    {
        /*** Darpan: Below is the piece of code where the trasnfer happens ***/
        if (mState == EMAddDataState.EM_PROCESSING_RECEIVED_DATA) {
            mDelegate.stopNoopTimer();
            mState = EMAddDataState.EM_SENDING_FINAL_OK;
            mDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
        }
        else {
            // TODO: unexpected
        }
    }

    @Override
    public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
        mDelegate.commandComplete(false);
    }

    @Override
    public void progressUpdate(EMProgressInfo aProgressInfo)
    {
        if (mState != EMAddDataState.EM_CANCELLED) {
            // Forward any progress updates from the async task
            mDataCommandDelegate.progressUpdate(aProgressInfo);
        }
    }

    @Override
    public void cancel() {
        mState = EMAddDataState.EM_CANCELLED;
        /*if (mParseDataTask != null)
            mParseDataTask.cancel(true);*/
    }

    private Context mContext;
    private EMCommandDelegate mDelegate;
    private EMDataCommandDelegate mDataCommandDelegate;

    private enum EMAddDataState
    {
        EM_SENDING_INITIAL_OK,
        EM_WAITING_FOR_RAW_FILE_DATA,
        EM_PROCESSING_RECEIVED_DATA,
        EM_SENDING_FINAL_OK,
        EM_CANCELLED
    };

    private String[] mCommandAndParameters;
    long mFileSize;
    EMParseDataInThread mParseDataTask;
    EMAddDataState mState;
}
