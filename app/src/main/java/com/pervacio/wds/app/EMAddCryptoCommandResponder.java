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

public class EMAddCryptoCommandResponder implements EMCommandHandler {

    EMAddCryptoCommandResponder(EMAddCryptoCommandDelegate aAddCryptoCommandDelegate)
    {
        mAddCryptoCommandDelegate = aAddCryptoCommandDelegate;
    }

    // Start this command handler
    public void start(EMCommandDelegate aDelegate)
    {
        DLog.log(">> EMAddCryptoCommandResponder::start");
        mCommandDelegate = aDelegate;

        /*
        // Don't sent the OK initially, instead request the password from the crypto delegate, we'll get a call back when we have the password
        mState = EMAddCryptoState.EM_WAITING_FOR_USER_PASSWORD;
        mAddCryptoCommandDelegate.cryptoPasswordRequested();
        */

        mState = EMAddCryptoState.EM_FILE_SENDING_INITIAL_OK;

        DLog.log("*** EMAddCryptoCommandResponder: sending initial OK response");
        mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
        DLog.log("<< EMAddCryptoCommandResponder::setPassword");
    }

    // Returns true if this command hander handles the given command - only relevant to responders (as opposed to initiators)
    @Override
    public boolean handlesCommand(String aCommand)
    {
        return (aCommand.equals(EMStringConsts.EM_COMMAND_TEXT_ADD_CRYPTO));
    }

    // We have text (usually a command or a response)
    @Override
    public boolean gotText(String aText)
    {
        String[] commandAndParameters = aText.split(" ");

        boolean result = true;
        if (commandAndParameters.length < 2)
            result = false;

        if (result) {
            mExpectedFileSize = Long.parseLong(commandAndParameters[1], 10);
        }

        if (result)
        {
            if (aText.startsWith(EMStringConsts.EM_COMMAND_TEXT_ADD_SALT))
            {
                mState = EMAddCryptoState.EM_SENDING_ADD_SALT_COMMAND_OK;
                mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
            }
            else if (aText.startsWith(EMStringConsts.EM_COMMAND_TEXT_ADD_REFERENCE_DATA))
            {
                mState = EMAddCryptoState.EM_SENDING_ADD_REFERENCE_DATA_COMMAND_OK;
                mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
            }
        }

        return result;
    }

    // We have received data
    // This will be a file path to a file containing the received data
    // This could be raw data, or it could be XML data
    @Override
    public boolean gotFile(String aFilePath)
    {
//  if (true) {
        DLog.log(">> EMAddCryptoCommandResponder::gotFile");
        DLog.log("aFilePath: " + aFilePath);

        boolean result = true;

        if (mState == EMAddCryptoState.EM_WAITING_FOR_SALT_FILE) {
            DLog.log("Got salt file");
            File saltFile = new File(aFilePath);
            try {
                byte salt[] = EMUtility.readFileToByteArray(saltFile);
                CMDCryptoSettings.setSalt(salt);
                // TODO: delete salt file
            }
            catch (Exception ex) {
                result = false;
            }

            mState = EMAddCryptoState.EM_SENDING_SALT_FILE_OK;
            mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
        }
        else if (mState == EMAddCryptoState.EM_WAITING_FOR_REFERENCE_DATA) {
            DLog.log("Got reference data");

            File referenceDataFile = new File(aFilePath);

            try {
                mReferenceData = EMUtility.readFileToByteArray(referenceDataFile);
//                boolean passwordOk = CMDCryptoSettings.testDecryptionWithReferenceXML(mReferenceData, true);
//                if (!passwordOk)
//                    result = false;
            }
            catch (Exception ex) {
                result = false;
            }

            // TODO: delete reference data file

            if (result) {
                /*
                mState = EMAddCryptoState.EM_SENDING_FINAL_OK;
                mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
                */
                mAddCryptoCommandDelegate.cryptoPasswordRequested();
            }
        }
        else
        {
            // TODO: bad state
        }

        return result;
    }

    // The data has been sent
    @Override
    public void sent()
    {
        DLog.log(">> EMAddCryptoCommandResponder::sent");

        if (mState == EMAddCryptoState.EM_FILE_SENDING_INITIAL_OK)
        {
            mState = EMAddCryptoState.EM_WAITING_FOR_ADD_SALT_COMMAND;
            DLog.log("Waiting for ADD_SALT command");
            mCommandDelegate.getText();
        }
        else if (mState == EMAddCryptoState.EM_SENDING_ADD_SALT_COMMAND_OK)
        {
            mState = EMAddCryptoState.EM_WAITING_FOR_SALT_FILE;

            DLog.log("About to wait for salt file");
            mTemporaryFileName = EMUtility.temporaryFileName();
            mCommandDelegate.getRawDataAsFile(mExpectedFileSize, mTemporaryFileName);
        }
        else if (mState == EMAddCryptoState.EM_SENDING_SALT_FILE_OK) {
            mState = EMAddCryptoState.EM_WAITING_FOR_ADD_REFERENCE_DATA_COMMAND;
            DLog.log("Waiting for ADD_REFERENCE_DATA command");
            mCommandDelegate.getText();
        }
        else if (mState == EMAddCryptoState.EM_SENDING_ADD_REFERENCE_DATA_COMMAND_OK)
        {
            mState = EMAddCryptoState.EM_WAITING_FOR_REFERENCE_DATA;

            DLog.log("About to wait for reference data");
            mTemporaryFileName = EMUtility.temporaryFileName();
            CMDCryptoSettings.setEnabled(true);

            // fix size (reference data will be encrypted, but we haven't yet enabled decryption on the stream)
            long extraBytesNeeded = (16 - (mExpectedFileSize % 16));
            long encryptedExpectedFileSize = mExpectedFileSize + extraBytesNeeded;

            mCommandDelegate.getRawDataAsFile(encryptedExpectedFileSize, mTemporaryFileName);
        }
        else if (mState == EMAddCryptoState.EM_SENDING_FINAL_OK) {
            DLog.log("EMAddCryptoCommandResponder - commandComplete");
            mCommandDelegate.commandComplete(true);
        }
        else {
            // TODO: invalid state
        }

        DLog.log("<< EMAddCryptoCommandResponder::sent");
    }

    boolean setPassword(String aPassword)
    {
        DLog.log(">> EMAddCryptoCommandResponder::setPassword");
        CMDCryptoSettings.setPassword(aPassword);
        try {
            CMDCryptoSettings.generateKey();
        }
        catch (Exception ex) {
            // Not much we can do here
            // The password check will fail
        }
        boolean passwordOk = CMDCryptoSettings.testDecryptionWithReferenceXML(mReferenceData, false);

        if (passwordOk) {
            mState = EMAddCryptoState.EM_SENDING_FINAL_OK;
            mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
        }
        // If the password is not okay we return false, and we expect to be called again by the UI with the correct password

        DLog.log("<< EMAddCryptoCommandResponder::setPassword");

        return passwordOk; // TODO: test password against the saved reference data, return false if bad
    }

    @Override
    public void cancel() {
        mState = EMAddCryptoState.EM_CANCELLED;
    }

    private EMCommandDelegate mCommandDelegate;

    private enum EMAddCryptoState
    {
        EM_WAITING_FOR_USER_PASSWORD,
        EM_FILE_SENDING_INITIAL_OK,
        EM_WAITING_FOR_ADD_SALT_COMMAND,
        EM_SENDING_ADD_SALT_COMMAND_OK,
        EM_WAITING_FOR_SALT_FILE,
        EM_SENDING_SALT_FILE_OK,
        EM_WAITING_FOR_ADD_REFERENCE_DATA_COMMAND,
        EM_SENDING_ADD_REFERENCE_DATA_COMMAND_OK,
        EM_WAITING_FOR_REFERENCE_DATA,
        EM_SENDING_FINAL_OK,
        EM_CANCELLED
    }

    private EMAddCryptoState mState;

    private long mExpectedFileSize;

    private EMAddCryptoCommandDelegate mAddCryptoCommandDelegate;

    private String mTemporaryFileName;

    private byte mReferenceData[];
}
