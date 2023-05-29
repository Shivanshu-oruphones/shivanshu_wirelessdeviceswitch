package com.pervacio.wds.app;

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

import android.webkit.WebView;

import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.sdk.internal.CMDCryptoSettings;

public class EMAddCryptoSrpCommandResponder implements EMCommandHandler, SRPBridge.Observer {

    public EMAddCryptoSrpCommandResponder(EMAddCryptoCommandDelegate aAddCryptoCommandDelegate) {
        mState = ETaskState.EM_NOT_INITIALIZED;
        mCommandDelegate = null;
        mAddCryptoCommandDelegate = aAddCryptoCommandDelegate;
    }

    /*
    // Requires a WebView to run the SRP JavaScript code
    void initialize(WebView aWebView) {
        mWebView = aWebView;
    }
    */

    void setPassword(String aPassword) {
        DLog.log("***** EMAddCryptoSrpCommandResponder:setPassword: " + aPassword);
        mState = ETaskState.EM_INITIALIZING_SRP;
        mSrp.srpServerInitialise(mSaltString, "", aPassword);
    }

    @Override
    public void start(EMCommandDelegate aDelegate) {
        mCommandDelegate  = aDelegate;
        mSuccess = false;
        //Disabling SRP if remote device is windows platform.
        boolean disableSRP= DashboardLog.getInstance().sourceEMDeviceInfo.dbDevicePlatform.equalsIgnoreCase("windows");
        if (disableSRP) {
            mSuccess=true;
            mState = ETaskState.EM_SENDING_SERVER_PROOF;
            mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
        }
        else {
            mState = ETaskState.EM_LOADING_SRP;
            mSrp = new SRPBridgeWebView(EMGlobals.getJavascriptWebView(), this);
        }
    }

    @Override
    public boolean handlesCommand(String aCommand) {
        boolean commandHandled = false;
        if (aCommand.startsWith(EMStringConsts.EM_COMMAND_TEXT_ADD_CRYPTO_SRP + " ")) {
            String[] commandAndParameters = aCommand.split(" ");

            if (commandAndParameters.length > 2) {
                mSaltString = commandAndParameters[1];
                mClientPublicKeyString = commandAndParameters[2];
            }
            commandHandled=true;
        }
        return commandHandled;
    }

    @Override
    public boolean gotText(String aText) {

        if (mState == ETaskState.EM_WAITING_FOR_CLIENT_PROOF)
        {
            mState = ETaskState.EM_CHECKING_CLIENT_PROOF;
            mSrp.srpServerPerformClientProofCheck(mClientPublicKeyString, aText); // aText is the client proof
        }

        return true;
    }

    @Override
    public boolean gotFile(String aDataPath) {
        return false;
    }

    @Override
    public void sent() {
        if (mState == ETaskState.EM_SENDING_SERVER_PUBLIC_KEY)
        {
            mState = ETaskState.EM_WAITING_FOR_CLIENT_PROOF;
            mCommandDelegate.getText();
        }
        else if (mState == ETaskState.EM_SENDING_SERVER_PROOF)
        {
            mCommandDelegate.commandComplete(mSuccess);
        }
    }

    @Override
    public void cancel() {
        mState = ETaskState.EM_CANCELLED;
    }

    @Override
    public void onSRPLoaded() {
        mState = ETaskState.EM_REQUESTING_PASSWORD;
        mAddCryptoCommandDelegate.cryptoPasswordRequested();
    }

    @Override
    public void onSRPClientInitialiseResponse(String aSalt, String aClientPublicKey) {
        // Ignore - this is a server
    }

    @Override
    public void onSRPClientCreateProofResponse(String aClientProof) {
        // Ignore - this is a server
    }

    @Override
    public void onSRPClientPerformServerProofCheckResponse(boolean aSuccess, String aClientSharedKey) {
        // Ignore - this a server
    }

    @Override
    public void onSRPServerInitialiseResponse(String aServerPublicKey) {
        mState = ETaskState.EM_SENDING_SERVER_PUBLIC_KEY;
        mCommandDelegate.sendText(aServerPublicKey);
    }

    @Override
    public void onSRPServerPerformClientProofCheckResponse(boolean aSuccess, String aServerSharedKey, String aServerProof) {
        mSuccess = aSuccess;
        mState = ETaskState.EM_SENDING_SERVER_PROOF;
        if (aSuccess) {
            byte[] serverKeyBytes = EMUtility.hexStringToByteArray(aServerSharedKey);
            CMDCryptoSettings.setKeyBytes(serverKeyBytes);
            mCommandDelegate.sendText(aServerProof);
        }
        else {
            // An empty response means failure
            // TODO: xxx - we should do something to ensure that we don't accept unencrypted traffic, or a default key
            mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_FAILED);
        }
    }

    enum ETaskState
    {
        EM_NOT_INITIALIZED,
        EM_LOADING_SRP,
        EM_REQUESTING_PASSWORD,
        EM_INITIALIZING_SRP,
        EM_SENDING_SERVER_PUBLIC_KEY,
        EM_WAITING_FOR_CLIENT_PROOF,
        EM_CHECKING_CLIENT_PROOF,
        EM_SENDING_SERVER_PROOF,
        EM_CANCELLED
    };

    private EMCommandDelegate mCommandDelegate;
    private ETaskState mState;
    private SRPBridgeWebView mSrp;
    private WebView mWebView;
    private String mClientPublicKeyString;
    private String mSaltString;
    private boolean mSuccess;
    private String mPassword;
    private EMAddCryptoCommandDelegate mAddCryptoCommandDelegate;
}
