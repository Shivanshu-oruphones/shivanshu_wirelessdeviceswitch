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

public class EMYouAreTargetCommandResponder implements EMCommandHandler {
	private EMCommandDelegate mCommandDelegate;
	private EMYouAreSourceOrTargetDelegate mYouAreTargetDelegate;
	boolean mCancelled = false;

	// Start this command handler
	@Override
	public void start(EMCommandDelegate aDelegate) {
	    mCommandDelegate = aDelegate;
	    mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
	}

	public void setYouAreTargetDelegate(EMYouAreSourceOrTargetDelegate aYouAreTargetDelegate)
	{
	    mYouAreTargetDelegate = aYouAreTargetDelegate;
	}

	// Returns true if this command hander handles the given command - only relevant to responders (as opposed to initiators)
	@Override
	public boolean handlesCommand(String aCommand)
	{
	    return aCommand.equalsIgnoreCase(EMStringConsts.EM_COMMAND_TEXT_YOU_ARE_TARGET);
	}

	// We have text (usually a command or a response)
	@Override
	public boolean gotText(String aText)
	{
	    // Ignore - we don't listen for any text (other than the initial command)
	    return true;
	}

	// We have received data
	// This will be a file path to a file containing the received data
	// This could be raw data, or it could be XML data
	@Override
	public boolean gotFile(String aDataPath)
	{
	    // Ignore - we don't listen for any files
	    return true;
	}

	// The data has been sent
	@Override
	public void sent()
	{
		if (mCancelled)
			return;

	    mCommandDelegate.commandComplete(true);
	    // We have sent our OK response, so notify the you-are-target delegate that it is now the target
	    mYouAreTargetDelegate.thisDeviceIsNowTheTarget();
	}

	@Override
	public void cancel() {
		mCancelled = true;
	}
}

