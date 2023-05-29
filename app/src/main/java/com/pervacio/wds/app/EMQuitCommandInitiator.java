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

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.pervacio.wds.custom.models.EDeviceSwitchSession;
import com.pervacio.wds.custom.utils.DashboardLog;

import java.io.StringReader;

public class EMQuitCommandInitiator implements EMCommandHandler {

	EMDataCommandDelegate mDataCommandDelegate;
	EMCommandDelegate mCommandDelegate;
	boolean mCancelled;

	// Start this command handler
	@Override
	public void start(EMCommandDelegate aDelegate)
	{
		mCancelled = false;
	    mCommandDelegate = aDelegate;
	    mCommandDelegate.sendText(EMStringConsts.EM_COMMAND_TEXT_QUIT);
	}

	// Returns true if this command hander handles the given command - only relevant to responders (as opposed to initiators)
	@Override
	public boolean handlesCommand(String aCommand)
	{
	    // This is an initiator, so we don't handle any commands
	    return false;
	}

	// We have text (usually a command or a response)
	@Override
	public boolean gotText(String aText)
	{
		if (mCancelled)
			return false;

		try {
			if (aText.startsWith(EMStringConsts.EM_TEXT_RESPONSE_OK) && aText.length() > EMStringConsts.EM_TEXT_RESPONSE_OK.length()) {
				String jsonString = aText.substring(EMStringConsts.EM_TEXT_RESPONSE_OK.length() + 1);
				DLog.log("substring : "+jsonString);
				JsonReader reader = new JsonReader(new StringReader(jsonString));
				reader.setLenient(true);
				EDeviceSwitchSession eDeviceSwitchSession = new Gson().fromJson(reader, EDeviceSwitchSession.class);
				DashboardLog.getInstance().seteDeviceSwitchSession(eDeviceSwitchSession);
				DashboardLog.getInstance().setUpdateDBdetails(true);
				aText = EMStringConsts.EM_TEXT_RESPONSE_OK;    //removing json to command  validation.
			}
		} catch (Exception e) {
			DLog.log(e);
		}

		mCommandDelegate.commandComplete(true);
		// Notify the delegate that we've quit. The delegate will probably close the session and connection.
		EMProgressInfo progressInfo = new EMProgressInfo();
		progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_QUIT_COMMAND_SENT;
		mDataCommandDelegate.progressUpdate(progressInfo);

	    return (EMStringConsts.EM_TEXT_RESPONSE_OK.equals(aText));
	}

	// We have received data
	// This will be a file path to a file containing the received data
	// This could be raw data, or it could be XML data
	@Override
	public boolean gotFile(String aDataPath)
	{
	    // Igore - we don't expect to receive a file
	    return false;
	}

	// The data has been sent
	@Override
	public void sent()
	{
		if (mCancelled)
			return;

	    // This means we've sent the command, so wait for the response
	    mCommandDelegate.getText();
	}

	// Set the delegate to receive notifications about data sending progress
	public void setDataCommandDelegate(EMDataCommandDelegate aDataCommandDelegate)
	{
	    mDataCommandDelegate = aDataCommandDelegate;
	}

	@Override
	public void cancel() {
		mCancelled = true;
	}
}
