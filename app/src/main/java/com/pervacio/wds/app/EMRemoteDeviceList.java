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

import java.util.ArrayList;

public class EMRemoteDeviceList {
	public ArrayList<EMDeviceInfo> mRemoteDevices = new ArrayList<EMDeviceInfo>();

	// Return the found device info that corresponds to the service name
	// Returns null if no matching device is found
	public EMDeviceInfo findDeviceByServiceName(String aServiceName)
	{
	    EMDeviceInfo matchingDeviceInfo = null;
	    
	    for (EMDeviceInfo existingDeviceInfo : mRemoteDevices) {
	        if (existingDeviceInfo.mServiceName.contentEquals(aServiceName))
	        {
	            // We have found a matching device
	            matchingDeviceInfo = existingDeviceInfo;
	            break;
	        }
	    }
	    
	    return matchingDeviceInfo;
	}

	// Remove the service from the list
	void removeService(String aServiceName)
	{
	    for (EMDeviceInfo existingDeviceInfo : mRemoteDevices) {
	    	if (existingDeviceInfo.mServiceName.contentEquals(aServiceName))
	    	{
	    		// TODO: should this remove all matches? If so, is it safe to do in this loop after we've already removed one?
	    		mRemoteDevices.remove(existingDeviceInfo);
	    		break;
	    	}
	    }
	}

	private void addDeviceToList(EMDeviceInfo emDeviceInfo) {
		for (EMDeviceInfo existingDeviceInfo : mRemoteDevices) {
			if (existingDeviceInfo.mDeviceUniqueId.contentEquals(emDeviceInfo.mDeviceUniqueId)) {
				// We have found this device is alrdy exists in list, so removing it before adding again.
				mRemoteDevices.remove(existingDeviceInfo);
				break;
			}
		}
		mRemoteDevices.add(emDeviceInfo);
	}

	// Adds the device to the list (assuming it's not there already)
	// Updates the existing device with additional info if it already exists
	// Returns true if a new device is added
	boolean addDevice(EMDeviceInfo aDeviceInfo)
	{
	    boolean deviceAdded = false;
	    
	    EMDeviceInfo matchingDeviceInfo = findDeviceByServiceName(aDeviceInfo.mServiceName);

	    if (matchingDeviceInfo != null)
	    {
			if (aDeviceInfo.mThisDeviceIsTargetAutoConnect && !matchingDeviceInfo.mThisDeviceIsTargetAutoConnect)
			{
				// Clear any existing IP addresses - use the one that the other device wants us to connect on
				matchingDeviceInfo.mIpV4Address = null;
				matchingDeviceInfo.mIpV6Address = null;
				matchingDeviceInfo.mThisDeviceIsTargetAutoConnect = true; // Set this so we don't try to clear it with subseqent requests
			}

	        // Update the existing device
	        // Update the IP address field if we have more info
	        if (matchingDeviceInfo.mIpV4Address == null)
	            matchingDeviceInfo.mIpV4Address = aDeviceInfo.mIpV4Address;

			if (matchingDeviceInfo.mIpV6Address == null)
				matchingDeviceInfo.mIpV6Address = aDeviceInfo.mIpV6Address;
	        
	        // Update the host name if we have more info
//	        if (matchingDeviceInfo.mHostName == null)
//	            matchingDeviceInfo.mHostName = aDeviceInfo.mHostName;
	    }
	    else
	    {
	        // Add this as a new device
			addDeviceToList(aDeviceInfo);
	        mDelegate.deviceListChanged();
	    }
	    
	    return deviceAdded;
	}

	public void setDelegate(EMDeviceListDelegate aDelegate)
	{
		mDelegate = aDelegate;
	}

	private EMDeviceListDelegate mDelegate;
}
