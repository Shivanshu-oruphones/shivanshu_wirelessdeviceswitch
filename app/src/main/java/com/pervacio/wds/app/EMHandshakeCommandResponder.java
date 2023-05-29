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

import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.DashboardLog;

public class EMHandshakeCommandResponder implements EMCommandHandler {

    private final static int EM_SENDING_RESPONSE_TO_COMMAND = 0;
    private final static int EM_WAITING_FOR_XML_FROM_INITIATOR = 1;
    private final static int EM_SENDING_XML = 2;
    private final static int EM_HANDSHAKE_COMPLETE = 3;
	private final static int EM_CANCELLED = 4;

	private EMCommandDelegate mCommandDelegate;
	private int mState;
	private EMDeviceInfo mDeviceInfo;
	private EMDeviceInfo mRemoteDeviceInfo;
	private EMHandshakeDelegate mHandshakeDelegate;
	
	public void setHandshakeDelegate(EMHandshakeDelegate aHandshakeDelegate)
	{
	    DLog.log(">> EMHandshakeCommandResponder::setHandshakeDelegate");
	    mHandshakeDelegate = aHandshakeDelegate;
	    DLog.log("<< EMHandshakeCommandResponder::setHandshakeDelegate");
	}
	
	public boolean handlesCommand(String aCommand)
	{
	    DLog.log(">> handlesCommand");
	    DLog.log("<< handlesCommand");
	    return aCommand.equals(EMStringConsts.EM_COMMAND_TEXT_HANDSHAKE);
	}

	// Must be called before the handshake command is used
	public void setDeviceInfo(EMDeviceInfo aDeviceInfo)
	{
	    DLog.log(">> EMHandshakeCommandResponder::setDeviceInfo");
	    mDeviceInfo = aDeviceInfo;
	    DLog.log("<< EMHandshakeCommandResponder::setDeviceInfo");
	}

	// Can only be called after the command is complete (returns the device info for the remote device)
	public EMDeviceInfo getRemoteDeviceInfo()
	{
	    DLog.log(">> EMHandshakeCommandResponder::getRemoteDeviceInfo");
	    DLog.log("<< EMHandshakeCommandResponder::getRemoteDeviceInfo");
	    return mRemoteDeviceInfo;
	}

	// Start this command handler
	public void start(EMCommandDelegate aDelegate)
	{
	    // Send the handshake command to the remote device and wait for the response
	    DLog.log(">> EMHandshakeCommandResponder::start");
	    mCommandDelegate = aDelegate;
	    mState = EM_SENDING_RESPONSE_TO_COMMAND;
	    mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
	    DLog.log("<< EMHandshakeCommandResponder::start");
	}

	// We have text (usually a command or a response)
	public boolean gotText(String aText)
	{
	    DLog.log(">> gotText");
	    boolean result = true;
	    
	    switch (mState)
	    {
	        case EM_SENDING_RESPONSE_TO_COMMAND:
	        case EM_WAITING_FOR_XML_FROM_INITIATOR:
	        case EM_SENDING_XML:
	        case EM_HANDSHAKE_COMPLETE:
	            // TODO: EM_EXCEPTION_RAISE_BAD_STATE(mState);
	            result = false;
	            break;
	    }
	    DLog.log("<< gotText");
	    return result;
	}

	// We have received data
	// This will be a file path to a file containing the received data
	// This could be raw data, or it could be XML data
	public boolean gotFile(String aDataPath)
	{
	    DLog.log(">> gotFile");
	    boolean result = true;
	    // EMDeviceInfo* remoteDeviceInfo = NULL;
	    
	    switch (mState)
	    {
	        case EM_SENDING_RESPONSE_TO_COMMAND:
	        case EM_SENDING_XML:
	        case EM_HANDSHAKE_COMPLETE:
	            // TODO: EM_EXCEPTION_RAISE_BAD_STATE(mState);
	            result = false;
	            break;
	        case EM_WAITING_FOR_XML_FROM_INITIATOR:
				DLog.log("EM_WAITING_FOR_XML_FROM_INITIATOR in CommandResponder");
				mRemoteDeviceInfo = EMHandshakeUtility.processHandshakeXml(aDataPath, true);

				// START – Pervacio
	            /*
	            Now we have received and processed device info sent by source

	            Since we'll be logging the transaction from destination, update devices info in
	            DashboardLog

	            From destination device's perspective, remote device = source device
	             */
	            DashboardLog.getInstance().setDevicesInfo(mDeviceInfo, mRemoteDeviceInfo);
				// END – Pervacio

	            mState = EM_SENDING_XML;
	            EMHandshakeUtility.sendHandshakeXml(mDeviceInfo, mCommandDelegate);
	            
	            break;
	    }
	    
	    DLog.log("<< gotFile");

	    return result;
	}

	// The data has been sent
	public void sent()
	{
	    DLog.log(">> sent");

	    switch (mState)
	    {
	        case EM_SENDING_RESPONSE_TO_COMMAND:
	            mState = EM_WAITING_FOR_XML_FROM_INITIATOR;
	            mCommandDelegate.getXmlAsFile();
	            break;
	        case EM_WAITING_FOR_XML_FROM_INITIATOR:
	        case EM_HANDSHAKE_COMPLETE:
	            // TODO: EM_EXCEPTION_RAISE_BAD_STATE(mState);
	            break;
	        case EM_SENDING_XML:     
	            mState = EM_HANDSHAKE_COMPLETE;
	            if (mRemoteDeviceInfo != null) {
					DLog.log("*** EMHandshakeCommandResponder handshake complete");
					mRemoteDeviceInfo.log();
					if (mRemoteDeviceInfo.mIpV4Address == null) {
						DLog.log("mIpV4Address is null, setting remoteipAdress : " + CommonUtil.getInstance().getRemoteDeviceIpAddress());
						mRemoteDeviceInfo.mIpV4Address = CommonUtil.getInstance().getRemoteDeviceIpAddress();
					}

					// TODO: extend to support IPv6
					if (mRemoteDeviceInfo.mIpV4Address != null) {
						mHandshakeDelegate.handshakeComplete(mRemoteDeviceInfo);
					}
					else {
						DLog.log("*** Ignoring handshake because IP address is not supplied");
					}
				}
	            mCommandDelegate.commandComplete(true);
	            break;
	    }
	    
	    DLog.log("<< sent");
	}

	@Override
	public void cancel() {
		mState = EM_CANCELLED;
	}
}
