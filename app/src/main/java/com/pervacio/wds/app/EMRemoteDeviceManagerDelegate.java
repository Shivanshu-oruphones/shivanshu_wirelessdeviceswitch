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

public interface EMRemoteDeviceManagerDelegate {
	// Notification that data is being sent, received or processed
	void progressUpdate(EMProgressInfo aProgressInfo);

	// Notification that we have become the source device
	void haveBecomeSource();

	// Notification that we have become the target device
	void haveBecomeTarget();

	// Notification that the PIN is okay (either the entered PIN or the pin received from the remote device, depending on the context)
	void pinOk();

	// Notification that the user must enter a password to decrypt the data received from the remote device
	void cryptoPasswordRequested();

	void remoteDeviceManagerError(int aError);
}
