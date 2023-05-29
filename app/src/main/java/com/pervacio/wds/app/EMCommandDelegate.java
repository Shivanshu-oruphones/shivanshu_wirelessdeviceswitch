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

public interface EMCommandDelegate {
	// Send the string, plus the line ending
	// Used for sending commands and responses
	// Sending is asynchronous - the command is notified when it is complete
	void sendText(String text);

	// Send a file
	// Sending is asynchronous - the command is notified when it is complete
	void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate);

	// Starts listening for a line of text
	// The command is notified when it is received
	void getText();

	// Starts listening for XML data and saves it into a temporary file
	// The command is notified when the XML is ready (when we have the </root> element)
	void getXmlAsFile();

	// Starts listening for raw data and saves it into a file
	// The command is notified when the data is ready (when we have read [length] bytes)
	void getRawDataAsFile(long aLength, String aTargetFilePath);

	// The command is complete - passes YES if it has succedded, NO otherwise
	void commandComplete(boolean aSuccess);

	void setSharedObject(String aKey, Object aObject);
	
	Object getSharedObject(String aKey);

	public void startNoopTimer();

	public void stopNoopTimer();

	public void disableTimeoutTimer();

	public void enableTimeoutTimer();

	public void addToPreviouslyTransferredItems(String aItem);

	public boolean itemHasBeenPreviouslyTransferred(String aItem);
}
