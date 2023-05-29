/*************************************************************************
 * 
 * Media Mushroom Limited CONFIDENTIAL
 * __________________
 * 
 *  Copyright 2015 Media Mushroom Limited
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

import java.net.InetAddress;

public interface EMBonjourController
{
    interface Observer
    {
    	void onServiceFound(String aServiceName, InetAddress aHost, int aPort);
        void onServiceRegistered(String aServiceName); // Note that this might be different to the requested name
    }
    
    void setDelegate(Observer aDelegate);

	void publishService(String aServiceName, int aPort);

	void unpublishService();
    
	void listenForService();	
}
