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

public interface EMCommandHandler {
	// Start this command handler
	void start(EMCommandDelegate aDelegate);

	// Returns true if this command hander handles the given command - only relevant to responders (as opposed to initiators)
	boolean handlesCommand(String aCommand);

	// We have text (usually a command or a response)
	boolean gotText(String aText);

	// We have received data
	// This will be a file path to a file containing the received data
	// This could be raw data, or it could be XML data
	boolean gotFile(String aDataPath);

	// The data has been sent
	void sent();

	// Cancel any ongoing operations with this command handler
	// The command handler MUST NOT make any calls back to the delegate after it has been cancelled
	void cancel();
}
