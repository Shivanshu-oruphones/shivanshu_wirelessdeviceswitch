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

import com.pervacio.wds.sdk.internal.CMDCryptoSettings;

public class EMAddCryptoSrpCommandInitiator implements EMCommandHandler, SRPBridge.Observer {

    public EMAddCryptoSrpCommandInitiator(EMAddCryptoCommandDelegate aAddCryptoCommandDelegate) {
        mState = ETaskState.EM_NOT_INITIALIZED;
        mAddCryptoCommandDelegate = aAddCryptoCommandDelegate;
        mCommandDelegate = null;
    }

    // Requires a WebView to run the SRP JavaScript code
    void initialize(WebView aWebView) {
        mWebView = aWebView;
    }

    boolean setPassword(String aPassword) {
        DLog.log("***** EMAddCryptoSrpCommandInitiator:setPassword: " + aPassword);
        mSrp.srpClientInitialise("", aPassword);
        return true;
    }

    @Override
    public void start(EMCommandDelegate aDelegate) {
        mCommandDelegate  = aDelegate;
        mState = ETaskState.EM_LOADING_SRP;
        mSrp = new SRPBridgeWebView(mWebView, this);
    }

    @Override
    public boolean handlesCommand(String aCommand) {
        return false;
    }

    @Override
    public boolean gotText(String aText) {

        if (mState == ETaskState.EM_WAITING_FOR_SERVER_PUBLIC_KEY)
        {
            mState = ETaskState.EM_CREATING_CLIENT_PROOF;
            mSrp.srpClientCreateProof(aText); // aText is the server public key
        }
        else if (mState == ETaskState.EM_WAITING_WAITING_FOR_SERVER_PROOF)
        {
            if (aText.equalsIgnoreCase(EMStringConsts.EM_TEXT_RESPONSE_FAILED)) {
                mCommandDelegate.commandComplete(false);
            }
            else {
                mState = ETaskState.EM_CHECKING_SERVER_PROOF;
                mSrp.srpClientPerformServerProofCheck(aText); // aTest is the server proof
            }
        }
        return true;
    }

    @Override
    public boolean gotFile(String aDataPath) {
        return false;
    }

    @Override
    public void sent() {
        if (mState == ETaskState.EM_SENDING_ADD_CRYPTO_SRP_COMMAND)
        {
            mState = ETaskState.EM_WAITING_FOR_SERVER_PUBLIC_KEY;
            mCommandDelegate.getText();
        }
        else if (mState == ETaskState.EM_SENDING_CLIENT_PROOF)
        {
            mState = ETaskState.EM_WAITING_WAITING_FOR_SERVER_PROOF;
            mCommandDelegate.getText();
        }
    }

    @Override
    public void onSRPLoaded() {
        mState = ETaskState.EM_REQUESTING_PASSWORD;
        mAddCryptoCommandDelegate.cryptoPasswordRequested();
    }

    @Override
    public void onSRPClientInitialiseResponse(String aSalt, String aClientPublicKey) {
        mState = ETaskState.EM_SENDING_ADD_CRYPTO_SRP_COMMAND;
        mCommandDelegate.sendText(EMStringConsts.EM_COMMAND_TEXT_ADD_CRYPTO_SRP + " " + aSalt + " " + aClientPublicKey);
    }

    @Override
    public void onSRPClientCreateProofResponse(String aClientProof) {
        mState = ETaskState.EM_SENDING_CLIENT_PROOF;
        mCommandDelegate.sendText(aClientProof);
    }

    @Override
    public void onSRPClientPerformServerProofCheckResponse(boolean aSuccess, String aClientSharedKey) {
        if (aSuccess) {
            byte[] clientSharedKeyBytes = EMUtility.hexStringToByteArray(aClientSharedKey);
            CMDCryptoSettings.setKeyBytes(clientSharedKeyBytes);
            mCommandDelegate.commandComplete(true);
        }
        else {
            // TODO: xxx - handle failure case up the stack
            mCommandDelegate.commandComplete(false);
        }
    }

    @Override
    public void onSRPServerInitialiseResponse(String aServerPublicKey) {
        // Ignore - this is a client
    }

    @Override
    public void onSRPServerPerformClientProofCheckResponse(boolean aSuccess, String aServerSharedKey, String aServerProof) {
        // Ignore - this is a client
    }

    @Override
    public void cancel() {
        mState = ETaskState.EM_CANCELLED;
    }

    enum ETaskState
    {
        EM_NOT_INITIALIZED,
        EM_LOADING_SRP,
        EM_REQUESTING_PASSWORD,
        EM_INITIALIZING_SRP,
        EM_SENDING_ADD_CRYPTO_SRP_COMMAND,
        EM_WAITING_FOR_SERVER_PUBLIC_KEY,
        EM_CREATING_CLIENT_PROOF,
        EM_SENDING_CLIENT_PROOF,
        EM_WAITING_WAITING_FOR_SERVER_PROOF,
        EM_CHECKING_SERVER_PROOF,
        EM_CANCELLED
    };

    private EMCommandDelegate mCommandDelegate;
    private ETaskState mState;
    private SRPBridgeWebView mSrp;
    private WebView mWebView;
    private EMAddCryptoCommandDelegate mAddCryptoCommandDelegate;
}
