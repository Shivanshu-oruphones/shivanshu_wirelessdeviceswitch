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

import android.util.Log;

import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;

public class EMHandshakeCommandInitiator implements EMCommandHandler {

    enum EMHandshakeInitiatorState {
        EM_SENDING_COMMAND,
        EM_WAITING_FOR_RESPONSE_TO_COMMAND,
        EM_SENDING_XML,
        EM_WAITING_FOR_XML_FROM_RESPONDER,
        EM_HANDSHAKE_COMPLETE,
        EM_CANCELLED
    };

    EMCommandDelegate mCommandDelegate;
    EMHandshakeInitiatorState mState;
    EMDeviceInfo mDeviceInfo;
    EMDeviceInfo mRemoteDeviceInfo;
    EMHandshakeDelegate mHandshakeDelegate;

    void setHandshakeDelegate(EMHandshakeDelegate aHandshakeDelegate)
    {
        DLog.log(">> EMHandshakeCommandInitiator::setHandshakeDelegate");
        mHandshakeDelegate = aHandshakeDelegate;
        DLog.log("<< EMHandshakeCommandInitiator::setHandshakeDelegate");
    }

    @Override
    public boolean handlesCommand(String aCommand)
    {
        DLog.log(">> handlesCommand");
        // This is an initiator, not a responder, it does not handle imcoming commands
        DLog.log("<< handlesCommand");
        return false;
    }

    // Must be called before the handshake command is used
    void setDeviceInfo(EMDeviceInfo aDeviceInfo)
    {
        DLog.log(">> setDeviceInfo");
        mDeviceInfo = aDeviceInfo;
        DLog.log("<< setDeviceInfo");
    }

    // Can only be called after the command is complete (returns the device info for the remote device)
    EMDeviceInfo getRemoteDeviceInfo()
    {
        DLog.log(">> getRemoteDeviceInfo");
        DLog.log("<< getRemoteDeviceInfo");
        return mRemoteDeviceInfo;
    }

    // Start this command handler
    @Override
    public void start(EMCommandDelegate aDelegate)
    {
        // Send the handshake command to the remote device and wait for the response
        DLog.log(">> start");
        mCommandDelegate = aDelegate;
        mState = EMHandshakeInitiatorState.EM_SENDING_COMMAND;
        mCommandDelegate.sendText(EMStringConsts.EM_COMMAND_TEXT_HANDSHAKE);
        DLog.log("<< start");
    }

    // We have text (usually a command or a response)
    @Override
    public boolean gotText(String aText)
    {
        DLog.log(">> gotText");
        boolean ok = false;
        switch (mState)
        {
            case EM_WAITING_FOR_RESPONSE_TO_COMMAND:
                if (EMStringConsts.EM_TEXT_RESPONSE_OK.equals(aText))
                {
                    // If the response is okay then send the handshake XML
                    mState = EMHandshakeInitiatorState.EM_SENDING_XML;
                    EMHandshakeUtility.sendHandshakeXml(mDeviceInfo, mCommandDelegate);
                }
                else
                {
                    ok = false;
                }
                break;
            case EM_WAITING_FOR_XML_FROM_RESPONDER:
            case EM_SENDING_COMMAND:
            case EM_SENDING_XML:
            case EM_HANDSHAKE_COMPLETE:
                // TODO:
                // EM_EXCEPTION_RAISE_BAD_STATE(mState);
                break;
        }

        DLog.log("<< gotText");

        return ok;
    }

    // We have received data
    // This will be a file path to a file containing the received data
    // This could be raw data, or it could be XML data
    @Override
    public boolean gotFile(String aDataPath)
    {
        DLog.log(">> gotFile");

        boolean ok = false;

        EMDeviceInfo remoteDeviceInfo = null;

        switch (mState)
        {
            case EM_SENDING_COMMAND:
            case EM_SENDING_XML:
            case EM_WAITING_FOR_RESPONSE_TO_COMMAND:
            case EM_HANDSHAKE_COMPLETE:
                // TODO: EM_EXCEPTION_RAISE_BAD_STATE(mState);
                break;
            case EM_WAITING_FOR_XML_FROM_RESPONDER:
                mState = EMHandshakeInitiatorState.EM_HANDSHAKE_COMPLETE;
                remoteDeviceInfo = EMHandshakeUtility.processHandshakeXml(aDataPath, true);

                DLog.log("EM_WAITING_FOR_XML_FROM_RESPONDER in CommandInitiator");

                if (remoteDeviceInfo != null) {
                    DLog.log("*** EMHandshakeCommandInitiator handshake complete");
                    remoteDeviceInfo.log();
                    DashboardLog.getInstance().setDevicesInfo(mDeviceInfo,remoteDeviceInfo);
                    mHandshakeDelegate.handshakeComplete(remoteDeviceInfo);
                }
                mCommandDelegate.commandComplete(true);
                break;
        }

        DLog.log("<< gotFile");

        return ok;
    }

    // The data has been sent
    @Override
    public void sent()
    {
        DLog.log(">> sent");

        switch (mState)
        {
            case EM_SENDING_COMMAND:
                mState = EMHandshakeInitiatorState.EM_WAITING_FOR_RESPONSE_TO_COMMAND;
                mCommandDelegate.getText();
                break;
            case EM_SENDING_XML:
                mState = EMHandshakeInitiatorState.EM_WAITING_FOR_XML_FROM_RESPONDER;
                mCommandDelegate.getXmlAsFile();
                break;
            case EM_WAITING_FOR_XML_FROM_RESPONDER:
            case EM_WAITING_FOR_RESPONSE_TO_COMMAND:
            case EM_HANDSHAKE_COMPLETE:
                break;
        }

        DLog.log("<< sent");
    }

    @Override
    public void cancel() {
        mState = EMHandshakeInitiatorState.EM_CANCELLED;
    }
}
