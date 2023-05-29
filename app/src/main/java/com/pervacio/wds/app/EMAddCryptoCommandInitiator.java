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

import com.pervacio.wds.sdk.internal.CMDCryptoSettings;

import java.io.File;

/**
 * Created by marcel on 10/10/2015.
 */
public class EMAddCryptoCommandInitiator implements EMCommandHandler {

    public EMAddCryptoCommandInitiator() {
        mState = ETaskState.EM_NOT_INITIALIZED;
        mCommandDelegate = null;
    }

    void init(byte[] aSalt) {
        String saltFilePath = EMUtility.temporaryFileName();
        mSaltFile = new File(saltFilePath);
        try {
            EMUtility.writeByteArrayToFile(aSalt, mSaltFile);
        } catch (Exception ex) {
            // TODO: handle error writing the salt file
        }
    }

    @Override
    public void start(EMCommandDelegate aDelegate) {
        mCommandDelegate  = aDelegate;

        mState = ETaskState.EM_SENDING_ADD_CRYPTO_COMMAND;
        mCommandDelegate.sendText(EMStringConsts.EM_COMMAND_TEXT_ADD_CRYPTO);
    }

    @Override
    public boolean handlesCommand(String aCommand) {
        return false;
    }

    @Override
    public boolean gotText(String aText) {

        if (mState == ETaskState.EM_WAITING_FOR_INITIAL_OK)
        {
            mState = ETaskState.EM_SENDING_ADD_SALT_COMMAND;
            long saltFileSize = mSaltFile.length();
            mCommandDelegate.sendText(EMStringConsts.EM_COMMAND_TEXT_ADD_SALT + " " + Long.toString(saltFileSize));
        }
        else if (mState == ETaskState.EM_WAITING_FOR_ADD_SALT_COMMAND_OK)
        {
            // Send the salt data
            mState = ETaskState.EM_SENDING_SALT_FILE;
            mCommandDelegate.sendFile(mSaltFile.toString(), true, null); // Delete the file
        }
        else if (mState == ETaskState.EM_WAITING_FOR_ADD_REFERENCE_DATA_COMMAND_OK)
        {
            // Send the reference data
            mState = ETaskState.EM_SENDING_REFERENCE_DATA;
            CMDCryptoSettings.setEnabled(true);
            mCommandDelegate.sendFile(mReferenceDataFilePath, true, null);
        }
        else if (mState == ETaskState.EM_WAITING_FOR_FINAL_OK)
        {
            mCommandDelegate.commandComplete(true);
        }
        else if (mState == ETaskState.EM_WAITING_FOR_SALT_FILE_OK)
        {
            mState = ETaskState.EM_SENDING_ADD_REFERENCE_DATA_COMMAND;
            mReferenceDataFilePath = EMUtility.createReferenceFileWithSendingDeviceInfo();
            File referenceFileInfo = new File(mReferenceDataFilePath);
            long referenceFileSize = referenceFileInfo.length();
            mCommandDelegate.sendText(EMStringConsts.EM_COMMAND_TEXT_ADD_REFERENCE_DATA + " " + Long.toString(referenceFileSize));
        }
        return true;
    }

    @Override
    public boolean gotFile(String aDataPath) {
        return false;
    }

    @Override
    public void sent() {
        if (mState == ETaskState.EM_SENDING_ADD_CRYPTO_COMMAND)
        {
            mState = ETaskState.EM_WAITING_FOR_INITIAL_OK;
            mCommandDelegate.getText();
        }
        else if (mState == ETaskState.EM_SENDING_ADD_SALT_COMMAND)
        {
            mState = ETaskState.EM_WAITING_FOR_ADD_SALT_COMMAND_OK;
            mCommandDelegate.getText();
        }
        else if (mState == ETaskState.EM_SENDING_SALT_FILE)
        {
            mState = ETaskState.EM_WAITING_FOR_SALT_FILE_OK;
            mCommandDelegate.getText();
        }
        else if (mState == ETaskState.EM_SENDING_ADD_REFERENCE_DATA_COMMAND)
        {
            mState = ETaskState.EM_WAITING_FOR_ADD_REFERENCE_DATA_COMMAND_OK;
            mCommandDelegate.getText();
        }
        else if (mState == ETaskState.EM_SENDING_REFERENCE_DATA)
        {
            mState = ETaskState.EM_WAITING_FOR_FINAL_OK;
            mCommandDelegate.getText();
        }
    }

    @Override
    public void cancel() {
        mState = ETaskState.EM_CANCELLED;
    }

    enum ETaskState
    {
        EM_NOT_INITIALIZED,
        EM_SENDING_ADD_CRYPTO_COMMAND,
        EM_WAITING_FOR_INITIAL_OK,
        EM_SENDING_ADD_SALT_COMMAND,
        EM_WAITING_FOR_ADD_SALT_COMMAND_OK,
        EM_SENDING_SALT_FILE,
        EM_WAITING_FOR_SALT_FILE_OK,
        EM_SENDING_ADD_REFERENCE_DATA_COMMAND,
        EM_WAITING_FOR_ADD_REFERENCE_DATA_COMMAND_OK,
        EM_SENDING_REFERENCE_DATA,
        EM_WAITING_FOR_FINAL_OK,
        EM_CANCELLED
    };


    private EMCommandDelegate mCommandDelegate;

    ETaskState mState;

    File mSaltFile;

    String mReferenceDataFilePath;
}
