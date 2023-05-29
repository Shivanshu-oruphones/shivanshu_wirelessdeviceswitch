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

public interface EMConnectionDelegate {
	// We have text (usually a command or a response)
	void gotText(String aText);

	// We have received data
	// This will be a file path to a file containing the received data
	// This could be raw data, or it could be XML data
	void gotFile(String aDataPath);

	// Data has been sent
	void sent();

	// Something has gone wrong
	void error(int aError);

	void disconnected();
}
