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

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.DiscoveryListener;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdManager.ResolveListener;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class EMBonjourControllerNsd implements EMBonjourController
{	
    private static final String SERVICE_REG_TYPE = "_easymigrate._tcp.";
	
    private NsdManager           mNsdManager;
    //private ResolveListener      mResolveListener;
    private DiscoveryListener    mDiscoveryListener;
    private RegistrationListener mRegistrationListener;
  
    private String   mPublishedServiceName;    
    private String   mMyIpAddress;
    private Observer mDelegate;
    
    public EMBonjourControllerNsd(String aMyIpAddress, Context aContext)
	{
		traceit(">> EMBonjourControllerNsd");
		
		mDelegate    = null;		
		mMyIpAddress = aMyIpAddress;
		mNsdManager  = (NsdManager) aContext.getSystemService(Context.NSD_SERVICE);

		traceit("<< EMBonjourControllerNsd");
	}
    
    public void setDelegate(Observer aDelegate)
    {
    	mDelegate = aDelegate;
    }
    
	public void unpublishService()
	{				
		logit("unpublishService");
		
		try
		{
			if (mRegistrationListener != null)
			{
				mNsdManager.unregisterService(mRegistrationListener);
			}
            if(mDiscoveryListener!=null){
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            }
		}
		catch (Exception e)
		{
			warnit("unpublishService, Exception: " +e);
		}
	} 
    
	public void publishService(String aServiceName, int aPort)
	{
		logit("publishService, Name: " +aServiceName+ ", Port: " +aPort);
		
		mPublishedServiceName = aServiceName;
		
		if (mRegistrationListener == null)
		{
			logit("publishService, Initialising Registration Listener");
			initializeRegistrationListener();
		}
		
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        
        serviceInfo.setPort(aPort);
        serviceInfo.setServiceName(aServiceName);
        serviceInfo.setServiceType(SERVICE_REG_TYPE);
        
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);        
    }
	
    private void initializeRegistrationListener()
    {
        mRegistrationListener = new RegistrationListener()
        {
            @Override
            public void onServiceRegistered(NsdServiceInfo aServiceInfo)
            {
                logit("onServiceRegistered, Name: " +aServiceInfo.getServiceName());
                mPublishedServiceName = aServiceInfo.getServiceName();
                mDelegate.onServiceRegistered(mPublishedServiceName);
            }
            
            @Override
            public void onRegistrationFailed(NsdServiceInfo aServiceInfo, int aErrorCode)
            {
               logit("onRegistrationFailed, Name: " +aServiceInfo.getServiceName());
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo aServiceInfo)
            {
               logit("onServiceUnregistered, Name: " +aServiceInfo.getServiceName());
            }
                        
            @Override
            public void onUnregistrationFailed(NsdServiceInfo aServiceInfo, int aErrorCode)
            {
               logit("onUnregistrationFailed, Name: " +aServiceInfo.getServiceName());
            }            
        };
    }

    private void initializeDiscoveryListener()
    {
		logit("initializeDiscoveryListener");

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new DiscoveryListener()
        {
            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String aRegType)
            {
               logit("onDiscoveryStarted, RegType: " +aRegType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo aService)
            { 
            	try
            	{
	            	logit("onServiceFound, Name: " + aService.getServiceName());
	            	logit("onServiceFound, Type: " + aService.getServiceType());
	            	                
	                if (! aService.getServiceType().equals(SERVICE_REG_TYPE))
	                {
	                	logit("onServiceFound, Different Service Type: " + aService.getServiceType());
	                    return;
	                }
	                
	                if (aService.getServiceName().equals(mPublishedServiceName))                
	                {
	                    logit("onServiceFound, Ignoring - Its my service: " + mPublishedServiceName);
	                    return;
	                }
	                
	                logit("onServiceFound, Attempting to resolve service info");
	                
	                ResolveListener resolveListener = createResolveListener(aService);
	                
	                mNsdManager.resolveService(aService, resolveListener);
            	}
            	catch (Exception e)
            	{
	            	logit("onServiceFound, Exception: " + e);
            	}
            }

            @Override
            public void onServiceLost(NsdServiceInfo aService)
            {
                // When the network service is no longer available.
                logit("onServiceLost, Service: " + aService);
            }

            @Override
            public void onDiscoveryStopped(String aServiceType)
            {
                logit("onDiscoveryStopped, Service: " + aServiceType);
            }

            @Override
            public void onStartDiscoveryFailed(String aServiceType, int aErrorCode)
            {
                logit("onStartDiscoveryFailed, Error code: " + aErrorCode);
                try {
                    mNsdManager.stopServiceDiscovery(this);
                }
                catch (Exception ex) {
                    DLog.log("onStartDiscoveryFailed", ex);
                }
            }

            @Override
            public void onStopDiscoveryFailed(String aServiceType, int aErrorCode)
            {
                logit("onStopDiscoveryFailed, Error code: " + aErrorCode);
                try {
                    mNsdManager.stopServiceDiscovery(this);
                }
                catch (Exception ex) {
                    DLog.log("onStopDiscoveryFailed", ex);
                }
            }
        };
    }
	   
	public void listenForService()
	{
		traceit(">> listenForService");

		//initializeResolveListener();
		initializeDiscoveryListener();	
		
		try
		{		
			mNsdManager.discoverServices(SERVICE_REG_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
		}
		catch (Exception e)
		{
			logit("listenForService, Exception: " +e);
		}		

		traceit("<< listenForService");

	}

    private ResolveListener createResolveListener(final NsdServiceInfo aService)
    {
    	ResolveListener listener = new ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
                //Log.e(TAG, "Resolve Failed: " + serviceInfo + "\tError Code: " + errorCode);
                if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE) {
                    //DLog.log("Trying again...");
                    mNsdManager.resolveService(aService, this);
                }
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo)
            {
            	String      serviceName = serviceInfo.getServiceName();
            	InetAddress serviceHost = serviceInfo.getHost();
            	int         servicePort = serviceInfo.getPort();
            	
                logit("onServiceResolved, Name: " + serviceName);
                logit("onServiceResolved, Type: " + serviceInfo.getServiceType());
                logit("onServiceResolved, Host: " + serviceHost);
                logit("onServiceResolved, Port: " + servicePort);

                if (mMyIpAddress.equals(serviceHost.getHostAddress()))
                {
                    logit("onServiceResolved, IP address same as mine - Ignoring");
                	return;
                }
                
                if (mDelegate != null)
                {
                	mDelegate.onServiceFound(serviceName, serviceHost, servicePort);
                }
            }
        };
        
        return listener;
    }
    
    /*
	private String getLocalIpAddress()
	  {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
            {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                    {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        }
        catch (Exception e)
        {
            logit("getLocalIpAddress, Exception: " +e.toString());
        }
        
        return "";
    }
    */
	
	
	private final static String TAG = "EMBonjourControllerNsd";
	
    static private void traceit(String aText)
    {
        //Log.v(TAG, aText);
        DLog.log(TAG + ": " + aText);
    }

    static private void logit(String aText)
    {
        //// Log.d(TAG, aText);
        DLog.log(TAG + ": " + aText);
    }

    static private void warnit(String aText)
    {
        //Log.e(TAG, aText);
        DLog.log(TAG + ": " + aText);
    }

    static private void errorit(String aText)
    {
        //Log.e(TAG, aText);
        DLog.log(TAG + ": " + aText);
    }
}
