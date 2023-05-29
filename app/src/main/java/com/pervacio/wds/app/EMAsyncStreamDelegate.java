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

public interface EMAsyncStreamDelegate {
	final static int EventHasSpaceAvailable = 0;
	final static int EventOpenCompleted = 1;
	final static int EventHasBytesAvailable = 2;
	final static int EventErrorOccurred = 3;
	final static int EventEndEncountered = 4;
	final static int EventFileDone = 5;
    
	void handleEvent(int aEvent);
}
