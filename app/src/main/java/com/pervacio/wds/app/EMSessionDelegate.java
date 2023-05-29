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

public interface EMSessionDelegate {
	// This session has been disconnected
	// This can be used by the owner 
	void disconnected(EMSession aSession);

	// Notification that data is being sent, received or processed
	void progressUpdate(EMProgressInfo aProgressInfo);

	void handshakeComplete(EMDeviceInfo aRemoteDeviceInfo);

	// Notification the this end of the session has become the source
	// Also signifies that this is a main session
	// Used to update the UI appropriately
	// Sent in client and server mode
	void haveBecomeSource(EMSession aMainSession);

	// Notification that this end of the session has become the target
	// Also signifies that this is a main session
	// Used to update the UI appropriately
	// Sent in client and server mode
	void haveBecomeTarget(EMSession aMainSession);

	void error(int aError);

	// A request from the session for the device info for this device
	// The session needs this information for handshaking
	EMDeviceInfo getDeviceInfo(boolean isSource);

	// Notification that the PIN is okay (either the entered PIN or the pin received from the remote device, depending on the context)
	void pinOk();

	// Notification that the user must enter a password to decrypt the data received from the remote device
	void cryptoPasswordRequested();

	// Notification that a remove device has reconnected
	void nonHandshakeConnectionFromRemoteDeviceOnNewSession(EMSession aReconnectedSession);
}
