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

public class EMYouAreTargetCommandInitiator implements EMCommandHandler {

	EMCommandDelegate mCommandDelegate;
	EMYouAreSourceOrTargetDelegate mYouAreSourceDelegate;
	boolean mCancelled;

	// Start this command handler
	public void start(EMCommandDelegate aDelegate)
	{
		mCancelled = false;
	    mCommandDelegate = aDelegate;
	    mCommandDelegate.sendText(EMStringConsts.EM_COMMAND_TEXT_YOU_ARE_TARGET);
	}

	void setYouAreSourceDelegate(EMYouAreSourceOrTargetDelegate aYouAreSourceDelegate)
	{
	    mYouAreSourceDelegate = aYouAreSourceDelegate;
	}

	// Returns true if this command hander handles the given command - only relevant to responders (as opposed to initiators)
	public boolean handlesCommand(String aCommand)
	{
	    // This is an initiator, so we don't handle any commands
	    return false;
	}

	// We have text (usually a command or a response)
	public boolean gotText(String aText)
	{
		if (mCancelled)
			return false;

	    mCommandDelegate.commandComplete(true);
	    mYouAreSourceDelegate.thisDeviceIsNowTheSource();
	    return(EMStringConsts.EM_TEXT_RESPONSE_OK.equals(aText));
	}

	// We have received data
	// This will be a file path to a file containing the received data
	// This could be raw data, or it could be XML data
	public boolean gotFile(String aDataPath)
	{
	    // Igore - we don't expect to receive a file
	    return true;
	}

	// The data has been sent
	public void sent()
	{
		if (mCancelled)
			return;

	    // This means we've sent the command, so wait for the response
	    mCommandDelegate.getText();
	}

	@Override
	public void cancel() {
		mCancelled = true;
	}
}
