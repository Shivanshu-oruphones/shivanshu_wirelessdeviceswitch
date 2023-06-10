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

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.Toast;

import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.DeviceInfo;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import static android.content.Context.WIFI_SERVICE;

public class EMRemoteDeviceManager implements EMServerDelegate, EMSessionDelegate
{
	public EMRemoteDeviceList mRemoteDeviceList;
	public EMDeviceInfo mSelectedDevice;
	static EMGlobals emGlobals = new EMGlobals();
	private Context mContext = emGlobals.getmContext();
	public EMRemoteDeviceManager(Context aContext)
	{
		EMUtility.setContext(emGlobals.getmContext());
		mContext = emGlobals.getmContext();
		mServer = new EMServer(EMConfig.FIXED_PORT_NUMBER);
		mSessionList = new ArrayList<EMSession>();
		mRemoteDeviceList = new EMRemoteDeviceList();
	}

	public void resetMainSession() {
		mMainSession = null;
	}

	public void resetDeviceList() {
		// unpublishService();
		ArrayList<EMDeviceInfo> oldRemoteDevices = mRemoteDeviceList.mRemoteDevices;
		mRemoteDeviceList = new EMRemoteDeviceList();
		// boolean result = publishService(mServer.mName);

		for (EMDeviceInfo oldDeviceInfo:
				oldRemoteDevices) {
			// Re-handshake with any old devices we have a referece to - see if they are still around
			if (!oldDeviceInfo.mServiceName.equals(mServer.mName))
				handshakeWithResolvedService(oldDeviceInfo.mIpV4Address, oldDeviceInfo.mPort, oldDeviceInfo.mServiceName);
		}
	}

	public void cancelSessions() {
		for (EMSession session: mSessionList) {
			session.stop();
		}
	}

	public void sendQuit(){
		mMainSession.sendQuit();
	}
	
	// Start listening for new connections and scanning for new devices
	public void start()
	{
		mServer.setDelegate(this);
	    mServer.start();

		if (!EMMigrateStatus.qrCodeWifiDirectMode()) { // Don't do device discovery in QR code mode
            boolean result = publishService(mServer.mName);
        }
	/*
			mCustomDiscoveryListener = new EMCustomDiscoveryListener(mContext);

			mCustomDiscoveryListener.setDelegate(new EMCustomDiscoveryListenerDelegate() {
				@Override
				void deviceFound(InetAddress aAddress, int aPort, String aServiceId) {
					if (!aServiceId.equals(mServer.mName)) { // Check that we haven't discovered ourselves before handshaking
						handshakeWithResolvedService(aAddress, aPort, aServiceId);
					}
				}
			});

			mCustomDiscoveryListener.execute();
	*/
		//}
	}

	// Stop listening for new connections and scanning for new devices
	public void stop()
	{
		// TODO: xxx - cancel any running sessions

		if (mServer != null) {
			mServer.stop();
			mServer = null;
		}

		unpublishService();
		// TODO: when (if?) we add support for service publishing then unpublish here
	}
	
	AsyncTask<String, EMPublishServerTaskUpdate, Void> mPublishServerTask;

	private InetAddress mThisDeviceIpV4Address = null;
	private InetAddress mThisDeviceIpV6Address = null;

	class EMPublishServerTaskUpdate // Discovered service info
	{
		public InetAddress mDiscoveredServiceAddress;
		public int mDiscoveredServicePort;
		public String mDiscoveredServiceName;
	}

	// EMCustomDiscoveryListener mCustomDiscoveryListener;

	// TODO: this is now a bad name for the class as we're discovering services as well as publishing our service
    private class PublishServerTask extends AsyncTask<String, EMPublishServerTaskUpdate, Void> {
    	@Override
    	protected void onProgressUpdate (EMPublishServerTaskUpdate... values)
    	{
			try {
				if (!values[0].mDiscoveredServiceName.equals(mServer.mName)) // Check that we haven't discovered ourselves before handshaking
                        handshakeWithResolvedService(values[0].mDiscoveredServiceAddress, values[0].mDiscoveredServicePort, values[0].mDiscoveredServiceName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Note this function retries, if the Wi-Fi address cannot be retreived. Is should ONLY EVER BE USED IN A BACKGROUND TASK
		protected InetAddress wifiInetAddress(WifiManager aWiFiManager) {
			boolean gotAddress = false;
			InetAddress inetAddress = null;

			while (!gotAddress) {
				int ipAddress = aWiFiManager.getConnectionInfo().getIpAddress();

				if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
					ipAddress = Integer.reverseBytes(ipAddress);
				}

				byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();


				try {
					inetAddress = InetAddress.getByAddress(ipByteArray);
					// Cache the IPv4 address for later (to be included in the handshake message)
					mThisDeviceIpV4Address = inetAddress; // TODO: also get and return the IPv6 address?
					gotAddress = true;
				} catch (UnknownHostException ex) {
					DLog.log("EMRemoteDeviceManager: unable to get Wi-Fi host address");
					inetAddress = null;
				}

				if (!gotAddress) {
					try {
						DLog.log("EMRemoteDeviceManager: could not get Wi-Fi address - retrying...");
						Thread.sleep(5000); // Sleep, then try again
					} catch (InterruptedException e) {
						inetAddress = null;
						gotAddress = true;
					}
				}
			}

			return inetAddress;
		}
    	
		@Override
		protected Void doInBackground(String... aName) {
	    	ServiceInfo serviceInfo;
	    	WifiManager wifi = (WifiManager) emGlobals.getmContext().getSystemService(WIFI_SERVICE);
	    	WifiManager.MulticastLock lock = wifi.createMulticastLock("CMD_MULTICAST_LOCK"); // TODO: release this lock when we're done
	    	lock.setReferenceCounted(true);
	    	lock.acquire();

			// final String serviceName = aName[0];

			final Set<String> serviceNames = new HashSet<>();
			serviceNames.add(aName[0]);

			// Publish and listen using EMBonjourController
			if (EMConfig.USE_NSD_DISCOVERY) {
				try {
					// WifiInfo wifiInfo = wifi.getConnectionInfo();
					DLog.log("EMRemoteDeviceManager: about to publish and listen using EMBonjourController");
					final InetAddress wifiInetAddress = wifiInetAddress(wifi);
					try {
						if (wifiInetAddress == null) {
							DLog.log("EMRemoteDeviceManager: no WiFi address");
						} else {
							mBonjourController = new EMBonjourControllerNsd(wifiInetAddress.toString(), emGlobals.getmContext());
							mBonjourController.setDelegate(new EMBonjourController.Observer() {
								@Override
								public void onServiceFound(String aServiceName, InetAddress aHost, int aPort) {
                                    if (serviceNames.contains(aServiceName)|| aHost.toString().contentEquals(wifiInetAddress.toString())) {
										DLog.log("EMBonjourController: Found my own service - ignore it");
									} else {

										DLog.log("EMRemoteDeviceManager: found service with EMBonjourController");
										DLog.log("EMRemoteDeviceManager: EMBonjourController: aServiceName:" + aServiceName);
										DLog.log("EMRemoteDeviceManager: EMBonjourController: aHost:" + aHost.toString());
										DLog.log("EMRemoteDeviceManager: EMBonjourController: aPort: " + aPort);
										EMPublishServerTaskUpdate discoveredServiceInfo = new EMPublishServerTaskUpdate();
										discoveredServiceInfo.mDiscoveredServiceAddress = aHost;
										discoveredServiceInfo.mDiscoveredServicePort = aPort;
										discoveredServiceInfo.mDiscoveredServiceName = aServiceName;
										publishProgress(discoveredServiceInfo);
									}
								}

								@Override
								public void onServiceRegistered(String aServiceName) {
									serviceNames.add(aServiceName);
								}
							});
							mBonjourController.publishService(aName[0], mServer.mPort);
							mBonjourController.listenForService();
						}
					} catch (Exception ex) {
						DLog.log("EMRemoteDeviceManager: EMBonjourController exception", ex);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					DLog.log("EMRemoteDeviceManager", e);
				}
			}
	        
			return null;
		}
    }

	private boolean publishService(String aName) {
		DLog.log(">> EMRemoteDeviceManager: publishService");

		mPublishServerTask = new PublishServerTask();
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			mPublishServerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, aName);
		else
			mPublishServerTask.execute(aName);
		
		// mPublishServerTask.execute(aName);

		DLog.log("<< EMRemoteDeviceManager: publishService");

		return true;
	}
	
	private void unpublishService() {
		if (EMConfig.USE_NSD_DISCOVERY) {
			if (mBonjourController != null) {
				mBonjourController.unpublishService();
			}
		}
		else if (EMConfig.USE_JMDNS_DISCOVERY) {
			mJmDNS.unregisterAllServices();
		}
	}

	// Select the remote device that we will probably connect to later
	public void selectRemoteDevice(EMDeviceInfo aDeviceInfo)
	{
		DLog.log("--selectRemoteDevice--");
		mSelectedDevice = aDeviceInfo.clone();
		mPreviouslyTransferredContentRegistry = new EMPreviouslyTransferredContentRegistry();

		// TODO: is this potentially too slow to be doing here? It could have 1000s of entries
		new Thread(){
			@Override
			public void run() {
				super.run();
				mPreviouslyTransferredContentRegistry.initialize(mSelectedDevice.mDeviceUniqueId, emGlobals.getmContext());
			}
		}.start();
	}

	// Connect to the selected remote device
	public void connectToRemoteDevice()
	{
		boolean bindToWiFiLan = true;
		// TODO: use IPv4 & IPv6?
		if (mSelectedDevice.mThisDeviceIsTargetAutoConnect)
			bindToWiFiLan = false; // Don't bind to the Wi-Fi lan - we assume an auto connect request has come from an iOS device connected to our hosted Wi-Fi Direct interface

	    mMainSession = new EMSession(mSelectedDevice.mIpV4Address, EMConfig.FIXED_PORT_NUMBER, this, emGlobals.getmContext(), mPreviouslyTransferredContentRegistry, bindToWiFiLan);
	    // mMainSession.setDelegate(this);
	    mSessionList.add(mMainSession);
	}

	public void reconnectToRemoteDevice(){
		//TODO FIXED BUG
		if(mSelectedDevice.mIpV4Address != null){
			mMainSession = new EMSession(mSelectedDevice.mIpV4Address, EMConfig.FIXED_PORT_NUMBER, this, emGlobals.getmContext(), mPreviouslyTransferredContentRegistry, true);
			mMainSession.startFileServer();
			mMainSession.reCreateConnection();
			sendTextCommand(Constants.RECOVERED);
		}else {
			Toast.makeText(mContext, "Something Went wrong!", Toast.LENGTH_SHORT).show();
		}
	}

	// The connected remote device should become the source (and this device becomes the target)
	// Must only be called when this device is the client (and the remote device is the server)
	public void remoteToBecomeSource()
	{
	    stop();
	    mMainSession.remoteToBecomeSource();
	}

	// The connected remote device should become the target (and this device becomes the source)
	// Must only be called when this device is the client (and the remote device is the server)
	public void remoteToBecomeTarget()
	{
//	    stop();
	    mMainSession.remoteToBecomeTarget();
	}

	// Send the selected data to the other device
	// Sends the commands, generates + sends the data payload
	// Asynchronous operation - Notifies the delegate of progress
	public void sendData(int aDataTypes)
	{
	    mMainSession.sendData(aDataTypes);
	}

	public boolean anyItemsPreviouslyTransferred() {
		if (mPreviouslyTransferredContentRegistry == null)
			return false;
		else
			return mPreviouslyTransferredContentRegistry.anyItemsPreviouslyTransferred();
	}

	public void clearPreviouslyTransferredItems() {
		DLog.log("--clearPreviouslyTransferredItems--" + mPreviouslyTransferredContentRegistry);
		if (mPreviouslyTransferredContentRegistry != null) {
			mPreviouslyTransferredContentRegistry.clearPreviouslyTransferredItems();
		}
	}

	public void setDelegate(EMRemoteDeviceManagerDelegate aDelegate)
	{
	    mDelegate = aDelegate;
	}
	

 	// TODO: not needed for now as we don't resolve services (other devices look for us)
 	 
	public void handshakeWithResolvedService(InetAddress aResolvedServiceAddress, int aPort, String aServiceId)
	{
	    // Create a session with the server and save it on the session list
	    EMSession newSession = new EMSession(aResolvedServiceAddress, aPort, this, emGlobals.getmContext(), null, true);
	    mSessionList.add(newSession);
	    newSession.handshakeWithServer();
	}

	// Notification that the PIN is okay (either the entered PIN or the pin received from the remote device, depending on the context)
	public void pinOk()
	{
	    mDelegate.pinOk();
	}
	
    private ArrayList<EMSession> mSessionList;
    private EMSession mMainSession;
    private EMRemoteDeviceManagerDelegate mDelegate;
    private EMServer mServer;

    // From EMServerDelegate
	@Override
	public void clientConnected(EMConnection aIncomingConnection) {
	    // State a new server session - wait for commands from the client
		DLog.log(">> EMserver::clientConnected");
		InetAddress remoteAddress = aIncomingConnection.mRemoteIpAddress;
//		if (remoteAddress != null)
//			DLog.log(remoteAddress.toString());
//		else
//			DLog.log("mRemoteIpAddress is null");
	    EMSession newSession = new EMSession(aIncomingConnection, this, emGlobals.getmContext());
//	    newSession.setDelegate(this);
	    mSessionList.add(newSession);

		DLog.log("<< EMserver::clientConnected");
	}
	
    // From EMServerDelegate
	@Override
	public void serverError(int aError) {
		// TODO Auto-generated method stub
		
	}
	
//	private Context mContext;

	@Override
	public void disconnected(EMSession aSession) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void progressUpdate(EMProgressInfo aProgressInfo) {
		// TODO Auto-generated method stub
	    mDelegate.progressUpdate(aProgressInfo);
	}

	@Override
	public void handshakeComplete(EMDeviceInfo aRemoteDeviceInfo) {
		mRemoteDeviceList.addDevice(aRemoteDeviceInfo);

		if (aRemoteDeviceInfo.mThisDeviceIsTargetAutoConnect) {
			if (mMainSession == null) { // Only do the auto-connect if we don't already have a session
				// The remote device has asked us to connect to it
				selectRemoteDevice(aRemoteDeviceInfo);
				connectToRemoteDevice();
				remoteToBecomeTarget();
			}
		} else selectRemoteDevice(aRemoteDeviceInfo);
	}

	@Override
	public void haveBecomeSource(EMSession aMainSession) {
	    mMainSession = aMainSession;

		// mMainSession.setMainSession();

		// TODO: is this needed any more? It looks a bit horrible...
		// Set the selected remote device so we can retrieve the appropriate device info later
		/*
		if (mSelectedDevice == null) {
			InetAddress remoteDeviceAddress = mMainSession.getRemoteDeviceAddress();
			for (EMDeviceInfo deviceInListInfo : mRemoteDeviceList.mRemoteDevices) {
				if (deviceInListInfo.mIpAddress.equals(remoteDeviceAddress)) {
					mSelectedDevice = deviceInListInfo;
				}
			}
		}
		*/
		
	    mDelegate.haveBecomeSource();
	}

	@Override
	public void haveBecomeTarget(EMSession aMainSession) {
	    mMainSession = aMainSession;
		// mMainSession.setMainSession();
	    mDelegate.haveBecomeTarget();
	}

	@Override
	public void nonHandshakeConnectionFromRemoteDeviceOnNewSession(EMSession aReconnectedSession) {
		// If a main session already exists, then this is a reconnection event, so cancel the old main session and replace it with this one
		// TODO: later it might be good to add an explicit RECONNECT command
		if (mMainSession != null) {
			if (mMainSession != aReconnectedSession) {
				mMainSession.cancel();
				mMainSession = aReconnectedSession;
			}
		}
	}

	@Override
	public void error(int aError) {
		mDelegate.remoteDeviceManagerError(aError);
	}

	@Override
	public EMDeviceInfo getDeviceInfo(boolean isSource) {
		DLog.log("getDeviceInfo of RemoteDeviceManager helper called. is Source: " + isSource);

        EMDeviceInfo deviceInfo = null;
        try {
            deviceInfo = new EMDeviceInfo();
            DeviceInfo mDeviceInfo= DeviceInfo.getInstance();
            if (!Build.MANUFACTURER.isEmpty()) {
                deviceInfo.mDeviceName = Build.MANUFACTURER + " " + Build.MODEL;
            }
            else
                deviceInfo.mDeviceName = Build.MODEL;

            deviceInfo.mIpV4Address = mThisDeviceIpV4Address;
            deviceInfo.mPort = mServer.mPort;
            deviceInfo.mCapabilities = EMConfig.DEVICE_CAPABILITIES;
            deviceInfo.mRoles = EMConfig.SUPPORTED_ROLES;
            deviceInfo.mServiceName = mServer.mName;
            deviceInfo.mKeyboardShortcutImporterAvailable = false;
            deviceInfo.mDeviceUniqueId = mDeviceInfo.get_serialnumber();

            /* Set fields required for Dashboard Logging */
            deviceInfo.dbDeviceBuildNumber = mDeviceInfo.getBuildNumber();
            deviceInfo.dbDeviceFirmware = mDeviceInfo.getFirmwareVersion();
            deviceInfo.dbDeviceMake = Build.MANUFACTURER;
            deviceInfo.dbDeviceModel = Build.MODEL;
            deviceInfo.dbDeviceOSVersion = mDeviceInfo.getOSversion();
            deviceInfo.dbDevicePlatform = Constants.PLATFORM;
            deviceInfo.dbDeviceIMEI = mDeviceInfo.get_imei();
            deviceInfo.dbDeviceTotalStorage = mDeviceInfo.getTotalInternalStroage();
            deviceInfo.dbDeviceFreeStorage = mDeviceInfo.getAvailableInternalStorage();
            deviceInfo.appVersion= BuildConfig.VERSION_NAME;
            if(DashboardLog.getInstance().isThisDest()) {
                deviceInfo.dbOperationType = Constants.OPERATION_TYPE.RESTORE.value();
            }
            else {
                deviceInfo.dbOperationType = Constants.OPERATION_TYPE.BACKUP.value();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return deviceInfo;
	}

	@Override
	public void cryptoPasswordRequested()
	{
		mDelegate.cryptoPasswordRequested();
	}

	// START – Pervacio

	public void sendTextCommand(String command){
		Constants.EM_TEXT_COMMAND= command;
		mMainSession.sendTextCommand();
	}


	public void sendDataForEstimation(EMCommandDelegate emCommandDelegate){
		mMainSession.sendDataForEstimation(emCommandDelegate);
	}

	// END – Pervacio

	public boolean setCryptoPassword(String aPassword) {
		if (aPassword.isEmpty()){
			return  false;
		}else {
			return mMainSession.setCryptoPassword(aPassword);
		}

	}
	
	private JmDNS mJmDNS;
	private EMBonjourController mBonjourController = null;

	private EMPreviouslyTransferredContentRegistry mPreviouslyTransferredContentRegistry;
}
