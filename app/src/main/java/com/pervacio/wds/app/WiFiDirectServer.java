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

import android.annotation.TargetApi;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DeviceInfo;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;

/*
public class WiFiDirectServer implements ATimer.Callback
{
    private static final int BUFFER_SIZE = 1024 * 4;

	public interface Observer
	{
		void onServerStatus(int aError);
		void onServerTrace(String aText);
	}

	public class NetworkInfo
	{
		String mSSID;
		String mPass;
		String mIPAddress;
		int    mPort;
	}

	private enum NetworkState
	{
		ENetworkIdle,
		ENetworkCreated,
		ENetworkNamed,
		ENetworkReady,
		ENetworkError
	}

	private final int mDefaultPort = 8888;

	Observer mObserver = null;

	NetworkState mNetworkState = NetworkState.ENetworkIdle;

	WifiP2pManager mManager = null;
	WifiP2pManager.Channel mChannel = null;

	Context mAppContext = null;

	String mSSID = "";
	String mPass = "";
	String mAddr = "";
	int    mPort = -1;

	int mNumNameAttempts = 0;
	int mNumAddressAttempts = 0;

	private final int mMaxNameAttempts    = 5;
	private final int mMaxAddressAttempts = 5;

	ATimer mTimer = null;

	WiFiDirectServer(Observer aObserver, Context aContext)
	{
		note("WiFiDirectServer");

		mObserver      = aObserver;

		mAppContext    = aContext;
		mNetworkState = NetworkState.ENetworkIdle;

		mTimer = new ATimer(this, 1, 100);
	}

	private void reportTrace(String aText)
	{
		if (mObserver != null)
		{
			mObserver.onServerTrace(aText);
		}
	}

	private void reportServerStatus(int aCode)
	{
		if (mObserver != null)
		{
			mObserver.onServerStatus(aCode);
		}
	}

	public void onATimerTick(int aId)
	{
		note("onATimerTick, Network Status: " +mNetworkState);

		mTimer.cancel();

		switch (mNetworkState)
		{
		case ENetworkIdle:
			break;

		case ENetworkCreated:
			doNetworkNameInfo();
			break;

		case ENetworkNamed:
			doNetworkAddressInfo();
			break;

		case ENetworkReady:
			break;

		case ENetworkError:
			break;

		default:
			error("onATimerTick, Unknown Status: " +mNetworkState);
			break;
		}
	}

	public NetworkInfo getNetworkInfo()
	{
		note("getNetworkInfo");

		if (mNetworkState != NetworkState.ENetworkReady)
		{
			note("getNetworkInfo, Network Not Ready, Status: " +mNetworkState);

			return null;
		}

		note("getNetworkInfo, Ready - SSID: " +mSSID+ ", Pass: " +mPass+ ", Addr: " +mAddr+ ", Port: " +mPort);

		NetworkInfo info = new NetworkInfo();

		info.mSSID      = mSSID;
		info.mPass      = mPass;
		info.mIPAddress = mAddr;
		info.mPort      = mPort;

		return info;
	}

	public void networkDestroy()
	{
		note("networkDestroy");

		mTimer.cancel();

		mManager.removeGroup(mChannel, null);

		mNetworkState = NetworkState.ENetworkIdle;
	}

	public void networkCreate()
	{
		note("networkCreate");
		initialisePeerManager();

		doNetworkGroupCreate();
	}

	public void networkProcess()
	{
		note("networkProcess");

		ReceiveDataTask receiver = new ReceiveDataTask();

		receiver.execute("some,page,name");

		note("networkProcess, Done");
	}

	protected void initialisePeerManager()
	{
		note("initialisePeerManager");

		try
		{
			Looper mainLooper = mAppContext.getMainLooper();

			mManager = (WifiP2pManager) mAppContext.getSystemService(Context.WIFI_P2P_SERVICE);
			mChannel = mManager.initialize(mAppContext, mainLooper, null);

			mSSID = "";
			mPass = "";
			mAddr = "";
			mPort = mDefaultPort;

			mNumNameAttempts    = 0;
			mNumAddressAttempts = 0;

			mNetworkState = NetworkState.ENetworkIdle;
		}
		catch (Exception e)
		{
			note("initialisePeerManager, Exception: " +e);
		}
	}

	protected void doNetworkGroupCreate()
	{
		note("doNetworkGroupCreate");

		mNetworkState = NetworkState.ENetworkIdle;

		mManager.createGroup(mChannel, new WifiP2pManager.ActionListener()
		{

		//@Override
	    public void onSuccess()
		{
	    	note("doNetworkGroupCreate::onSuccess");

			mNetworkState = NetworkState.ENetworkCreated;
			mTimer.start();
	    }

	    //@Override
	    public void onFailure(int aCode)
	    {
	    	note("doNetworkGroupCreate::onFailure, Code: " +aCode);

	    	if (aCode == WifiP2pManager.BUSY)
	    	{
				mNetworkState = NetworkState.ENetworkCreated;
				mTimer.start();
				return;
	    	}

			mNetworkState = NetworkState.ENetworkError;
			reportServerStatus(-1);
	    }
		});
	}

	protected void doNetworkNameInfo()
	{
    	note("doNetworkNameInfo");

		mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener()
		{
			public void onGroupInfoAvailable(WifiP2pGroup aGroup)
			{
		    	note("doNetworkNameInfo, onGroupInfoAvailable");

		    	if (aGroup == null)
		    	{
			    	note("doNetworkNameInfo, Group is null");

		    		mNumNameAttempts++;

		    		if (mNumNameAttempts < mMaxNameAttempts)
		    		{
		    			error("doNetworkNameInfo, Trying Again. Attempts: " +mNumNameAttempts);
		    			mTimer.start();
		    		}
		    		else
		    		{
				    	mNetworkState = NetworkState.ENetworkError;

		    			error("doNetworkNameInfo, Unable to get network name. Attempts: " +mNumNameAttempts);
		    			reportServerStatus(-1);
		    		}

			    	return;
		    	}

		    	mSSID = aGroup.getNetworkName();
		    	mPass = aGroup.getPassphrase();

		    	note("doNetworkNameInfo, onGroupInfoAvailable, SSID: " +mSSID+ ", Pass: " +mPass);

		    	mNetworkState = NetworkState.ENetworkNamed;
		    	mTimer.start();
			}

		});
	}

	protected void doNetworkAddressInfo()
	{
    	note("doNetworkAddressInfo");

		mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener()
		{
			public void onConnectionInfoAvailable(WifiP2pInfo aInfo)
			{
		    	note("doNetworkAddressInfo, onConnectionInfoAvailable");

		    	if (aInfo == null)
		    	{
			    	note("doNetworkAddressInfo, onConnectionInfoAvailable, Info is null");

		    		mNumAddressAttempts++;

		    		if (mNumAddressAttempts < mMaxAddressAttempts)
		    		{
		    			error("doNetworkAddressInfo, Trying Again. Attempts: " +mNumAddressAttempts);
		    			mTimer.start();
		    		}
		    		else
		    		{
				    	mNetworkState = NetworkState.ENetworkError;

		    			error("doNetworkAddressInfo, Unable to get network address. Attempts: " +mNumAddressAttempts);
		    			reportServerStatus(-1);
		    		}

			    	return;
		    	}


		    	InetAddress ownerAddress = aInfo.groupOwnerAddress;

		    	if (ownerAddress == null)
		    	{
			    	mNetworkState = NetworkState.ENetworkError;

			    	error("doNetworkAddressInfo, onConnectionInfoAvailable, Owner address null");
	    			reportServerStatus(-1);
			    	return;
		    	}

		    	mAddr = ownerAddress.getHostAddress();

				note("onConnectionInfoAvailable, Owner address:  " + mAddr);

		    	mNetworkState = NetworkState.ENetworkReady;
    			reportServerStatus(0);
			}
		});
	}

	protected class ReceiveDataTask extends AsyncTask<String, String, Long>
	{
		private Long receiveData()
		{
	        long totalSize = 0;

	        ServerSocket serverSocket = null;
	        Socket       clientSocket = null;
	        InputStream  inputStream  = null;
	        BufferedInputStream input = null;

	        try
			{
	            publishProgress("receiveData, Server Waiting for Connection");

				serverSocket = new ServerSocket(mPort);

				clientSocket = serverSocket.accept();

				publishProgress("receiveData, Got a Connection");

	            inputStream  = clientSocket.getInputStream();

	            input = new BufferedInputStream(inputStream);

	            byte[] buffer = new byte[BUFFER_SIZE];

	            int count;

	            publishProgress("Start");

	            while ((count = input.read(buffer, 0, BUFFER_SIZE)) != -1)
	            {
	            	totalSize += count;

	            	String str = new String(buffer, 0, count);

	            	publishProgress("receiveData, Got: '" +str+ "'");

	            	if (isCancelled()) break;
	            }

	            publishProgress("Completed");

			}
			catch (Exception e)
			{
				publishProgress("receiveData, Exception: " +e);
				e.printStackTrace();
			}

			try
			{
				if (input != null) input.close();
				if (clientSocket != null && clientSocket.isConnected()) clientSocket.close();
				if (serverSocket != null) serverSocket.close();
			}
			catch (Exception e)
			{
				publishProgress("receiveData, Exception on Closing: " +e);
				e.printStackTrace();
			}

			return totalSize;
		}

		protected Long doInBackground(String... aParams)
	    {
			publishProgress("doInBackground, Enter");

	        //int count = aParams.length;

	        long totalSize = receiveData();

	    	publishProgress("doInBackground, Exit");

	        return totalSize;
	    }

	    protected void onProgressUpdate(String... aProgress)
	    {
	        note("onProgressUpdate, Progress: " +aProgress[0]);
	    }

	    protected void onPostExecute(Long result)
	    {
	    	note("onPostExecute, Downloaded " + result + " bytes");
	    }
	}


	//=============================================================================
	//=============================================================================

	final static String kTagModule = "WiFiDirectServer";

	static private void trace(String aText)
	{
		Log.v(kTagModule, aText);
	}

	static private void log(String aText)
	{
		// Log.d(kTagModule, aText);
	}

	private void note(String aText)
	{
		// Log.d(kTagModule, aText);
	}

	private void warn(String aText)
	{
		Log.w(kTagModule, "WARN: " + aText);
	}

	private void error(String aText)
	{
		Log.e(kTagModule, "ERROR: " + aText);
	}
}
*/

public class WiFiDirectServer implements ATimer.Callback {
    private static final int BUFFER_SIZE = 1024 * 4;
    static EMGlobals emGlobals = new EMGlobals();
    public interface Observer {
        void onServerStatus(int aError);

        void onServerTrace(String aText);
    }

    public class NetworkInfo {
        public String mSSID = "";
        public String mPass = "";
        public String mIPAddress = "0.0.0.0";
        int mPort;
        public String mFreqency = "0";
    }

    private enum NetworkState {
        ENetworkIdle,
        ENetworkCreated,
        ENetworkNamed,
        ENetworkReady,
        ENetworkError
    }

    private final int mDefaultPort = 8888;

    Observer mObserver = null;

    NetworkState mNetworkState = NetworkState.ENetworkIdle;

    WifiP2pManager mManager = null;
    WifiP2pManager.Channel mChannel = null;

    Context mAppContext = null;

    String mSSID = "";
    String mPass = "";
    String mAddr = "";
    int mPort = -1;
    String mFreqency = "";

    int mNumNameAttempts = 0;
    int mNumAddressAttempts = 0;

    private final int mMaxNameAttempts = 20;
    private final int mMaxAddressAttempts = 20;

    ATimer mTimer = null;

    public WiFiDirectServer(WiFiDirectServer.Observer aObserver, Context aContext) {
        note("WiFiDirectServer");

        mObserver = aObserver;

        mAppContext = aContext;
        mNetworkState = NetworkState.ENetworkIdle;

        mTimer = new ATimer(this, 1, 1000);
    }

    private void reportTrace(String aText) {
        if (mObserver != null) {
            mObserver.onServerTrace(aText);
        }
    }

    private void reportServerStatus(int aCode) {
        if (mObserver != null) {
            mTimer.cancel();
            mObserver.onServerStatus(aCode);
        }
    }

    public void onATimerTick(int aId) {
        note("onATimerTick, Network Status: " + mNetworkState);

        mTimer.cancel();

        switch (mNetworkState) {
            case ENetworkIdle:
                break;

            case ENetworkCreated:
                doNetworkNameInfo();
                break;

            case ENetworkNamed:
                doNetworkAddressInfo();
                break;

            case ENetworkReady:
                break;

            case ENetworkError:
                break;

            default:
                error("onATimerTick, Unknown Status: " + mNetworkState);
                break;
        }
    }

    public NetworkInfo getNetworkInfo() {
        note("getNetworkInfo");

        if (mNetworkState != NetworkState.ENetworkReady) {
            note("getNetworkInfo, Network Not Ready, Status: " + mNetworkState);

            return new NetworkInfo();
        }

        note("getNetworkInfo, Ready - SSID: " + mSSID + ", Pass: " + mPass + ", Addr: " + mAddr + ", Port: " + mPort + ", Frequncey: " + mFreqency);

        NetworkInfo info = new NetworkInfo();

        info.mSSID = mSSID;
        info.mPass = mPass;
        info.mIPAddress = mAddr;
        info.mPort = mPort;
        info.mFreqency = mFreqency;
        return info;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void networkDestroy() {
        note("networkDestroy");

        mTimer.cancel();

        if (mChannel != null)
            mManager.removeGroup(mChannel, null);
        else
            changeHotspot(false);

        mNetworkState = NetworkState.ENetworkIdle;
    }

    public void networkCreate() {
        note("networkCreate");
//        if ((Constants.CLOUD_PAIRING_ENABLED && Constants.WIFI_PROBLAMATIC_MODELS.contains(Build.MODEL)) || Constants.WIFI_DIRECT_PROBLAMATIC_MODELS.contains(Build.MODEL)) {
//            changeHotspot(true);
//            sendHotSpotInfo();
//        } else {
        initialisePeerManager();

//            doNetworkGroupCreate();
        doChangeGroupname();
//        }
    }

    public void networkProcess() {
        note("networkProcess");

        ReceiveDataTask receiver = new ReceiveDataTask();

        receiver.execute("some,page,name");

        note("networkProcess, Done");
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected void initialisePeerManager() {
        note("initialisePeerManager");

        try {
            Looper mainLooper = mAppContext.getMainLooper();

            mManager = (WifiP2pManager) mAppContext.getSystemService(Context.WIFI_P2P_SERVICE);
            mChannel = mManager.initialize(mAppContext, mainLooper, null);

            mSSID = "";
            mPass = "";
            mAddr = "";
            mPort = mDefaultPort;

            mNumNameAttempts = 0;
            mNumAddressAttempts = 0;

            mNetworkState = NetworkState.ENetworkIdle;
        } catch (Exception e) {
            note("initialisePeerManager, Exception: " + e);
        }
    }

    /*
     * Method to change the Wifi-Direct Server Name.
     *
     * */
    void doChangeGroupname() {
        mNetworkState = NetworkState.ENetworkIdle;

        try {

            Class[] paramTypes = new Class[3];
            paramTypes[0] = WifiP2pManager.Channel.class;
            paramTypes[1] = String.class;
            paramTypes[2] = WifiP2pManager.ActionListener.class;
            Method setDeviceName = mManager.getClass().getMethod(
                    "setDeviceName", paramTypes);
            setDeviceName.setAccessible(true);

            SecureRandom secureRandom = new SecureRandom();
            float randomFloat = secureRandom.nextFloat();
            float pinFloat = randomFloat * 9999;
            int pinInt = (int) pinFloat;

            Object arglist[] = new Object[3];
            arglist[0] = mChannel;
            arglist[1] = String.format("%s %04d", Build.MODEL, pinInt);
            arglist[2] = new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    DLog.log("setDeviceName succeeded");
                    note("doNetworkGroupCreate::onSuccess");
                    doNetworkGroupCreate();
                }

                @Override
                public void onFailure(int aCode) {
                    DLog.log("setDeviceName failed");
                    note("doNetworkGroupCreate::onFailure, Code: " + aCode);
                    doNetworkGroupCreate();
                }
            };
            setDeviceName.invoke(mManager, arglist);

        } catch (Exception e) {
            DLog.log("Exception on changing wifidirect name "+e.getMessage());
            doNetworkGroupCreate();
        }

    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected void doNetworkGroupCreate() {
        note("doNetworkGroupCreate");

        mNetworkState = NetworkState.ENetworkIdle;
        if (!retryConnection) {
            mHandler.sendEmptyMessageDelayed(CHANGE_WIFI_STATE, 20 * 1000);
        }
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            //@Override
            public void onSuccess() {
                DLog.log("doNetworkGroupCreate::onSuccess");
                mHandler.removeMessages(CHANGE_WIFI_STATE);
                mNetworkState = NetworkState.ENetworkCreated;
                mTimer.start();
            }

            //@Override
            public void onFailure(int aCode) {
                mHandler.removeMessages(CHANGE_WIFI_STATE);
                DLog.log("doNetworkGroupCreate::onFailure, Code: " +aCode);
                if (aCode == WifiP2pManager.BUSY) {
                    mNetworkState = NetworkState.ENetworkCreated;
                    mTimer.start();
                    return;
                } else if (aCode == WifiP2pManager.ERROR) {
                    if (Constants.CLOUD_PAIRING_ENABLED) {
                        sendHotSpotInfo();
                    } else {
                        changeHotspot(true);
                        sendHotSpotInfo();
                    }
                    return;
                }

                mNetworkState = NetworkState.ENetworkError;
                reportServerStatus(-1);
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected void doNetworkNameInfo() {
        note("doNetworkNameInfo");

        mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            public void onGroupInfoAvailable(WifiP2pGroup aGroup) {
                note("doNetworkNameInfo, onGroupInfoAvailable");

                if (aGroup == null) {
                    note("doNetworkNameInfo, Group is null");

                    mNumNameAttempts++;

                    if (mNumNameAttempts < mMaxNameAttempts) {
                        error("doNetworkNameInfo, Trying Again. Attempts: " + mNumNameAttempts);
                        mTimer.start();
                    } else {
                        mNetworkState = NetworkState.ENetworkError;

                        error("doNetworkNameInfo, Unable to get network name. Attempts: " + mNumNameAttempts);
                        reportServerStatus(-1);
                    }

                    return;
                }
                DLog.log("GroupInfo "+aGroup.toString());
                String[] info = aGroup.toString().split("\n");
                for (int i = 0; i < info.length; i++) {
                    String str = info[i];
                    if (str.contains("frequency") || str.contains("mGoOperFreq")) {
                        mFreqency = str.split(":")[1].trim();
                    }
                }
                if (mFreqency == null || mFreqency.isEmpty()) {
                    mFreqency = getFrequency();
                }
                mSSID = aGroup.getNetworkName();
                mPass = aGroup.getPassphrase();
                CommonUtil.getInstance().setNetworkType("P2P");

                //note("doNetworkNameInfo, onGroupInfoAvailable, SSID: " +mSSID+ ", Pass: " +mPass +" Frequncey: "+mFreqency);
                DLog.log("doNetworkNameInfo, onGroupInfoAvailable, SSID: " +mSSID+ ", Pass: " +mPass +" Frequency: "+mFreqency);

                mNetworkState = NetworkState.ENetworkNamed;
                mTimer.start();
            }

        });
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected void doNetworkAddressInfo() {
        note("doNetworkAddressInfo");

        mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            public void onConnectionInfoAvailable(WifiP2pInfo aInfo) {
                note("doNetworkAddressInfo, onConnectionInfoAvailable");

                if (aInfo == null) {
                    note("doNetworkAddressInfo, onConnectionInfoAvailable, Info is null");

                    mNumAddressAttempts++;

                    if (mNumAddressAttempts < mMaxAddressAttempts) {
                        error("doNetworkAddressInfo, Trying Again. Attempts: " + mNumAddressAttempts);
                        mTimer.start();
                    } else {
                        mNetworkState = NetworkState.ENetworkError;

                        error("doNetworkAddressInfo, Unable to get network address. Attempts: " + mNumAddressAttempts);
                        reportServerStatus(-1);
                    }

                    return;
                }


                InetAddress ownerAddress = aInfo.groupOwnerAddress;

                if (ownerAddress == null) {
                    mNetworkState = NetworkState.ENetworkError;

                    error("doNetworkAddressInfo, onConnectionInfoAvailable, Owner address null");
                    reportServerStatus(-1);
                    return;
                }

                mAddr = ownerAddress.getHostAddress();

                note("onConnectionInfoAvailable, Owner address:  " + mAddr);

                mNetworkState = NetworkState.ENetworkReady;
                reportServerStatus(0);
            }
        });
    }

    protected class ReceiveDataTask extends AsyncTask<String, String, Long> {
        private Long receiveData() {
            long totalSize = 0;

            ServerSocket serverSocket = null;
            Socket clientSocket = null;
            InputStream inputStream = null;
            BufferedInputStream input = null;

            try {
                publishProgress("receiveData, Server Waiting for Connection");

                serverSocket = new ServerSocket(mPort);

                clientSocket = serverSocket.accept();

                publishProgress("receiveData, Got a Connection");

                inputStream = clientSocket.getInputStream();

                input = new BufferedInputStream(inputStream);

                byte[] buffer = new byte[BUFFER_SIZE];

                int count;

                publishProgress("Start");

                while ((count = input.read(buffer, 0, BUFFER_SIZE)) != -1) {
                    totalSize += count;

                    String str = new String(buffer, 0, count);

                    publishProgress("receiveData, Got: '" + str + "'");

                    if (isCancelled()) break;
                }

                publishProgress("Completed");

            } catch (Exception e) {
                publishProgress("receiveData, Exception: " + e);
                e.printStackTrace();
            }

            try {
                if (input != null) input.close();
                if (clientSocket != null && clientSocket.isConnected()) clientSocket.close();
                if (serverSocket != null) serverSocket.close();
            } catch (Exception e) {
                publishProgress("receiveData, Exception on Closing: " + e);
                e.printStackTrace();
            }

            return totalSize;
        }

        protected Long doInBackground(String... aParams) {
            publishProgress("doInBackground, Enter");

            //int count = aParams.length;

            long totalSize = receiveData();

            publishProgress("doInBackground, Exit");

            return totalSize;
        }

        protected void onProgressUpdate(String... aProgress) {
            note("onProgressUpdate, Progress: " + aProgress[0]);
        }

        protected void onPostExecute(Long result) {
            note("onPostExecute, Downloaded " + result + " bytes");
        }
    }

    private boolean changeWifiState() {
        try {
            WifiManager wifiManager = (WifiManager) emGlobals.getmContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                if (wifiManager.isWifiEnabled()) {
                    int networkId = wifiManager.getConnectionInfo().getNetworkId();
                    wifiManager.enableNetwork(networkId, true);
                    wifiManager.disableNetwork(networkId);
                    wifiManager.disconnect();
                    if (wifiManager.getConnectionInfo() != null) {
                        while (wifiManager.getConnectionInfo().getNetworkId() != -1) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return wifiManager.disconnect();
                } else {
                    return wifiManager.setWifiEnabled(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            DLog.log("handler message: "+msg.what);
            if (msg.what == CHANGE_WIFI_STATE) {
                retryConnection = true;
                mHandler.removeMessages(CHANGE_WIFI_STATE);
                boolean changed = changeWifiState();
                mHandler.sendEmptyMessageDelayed(RETRY, 5 * 1000);
            } else if (msg.what == RETRY) {
                mHandler.removeMessages(RETRY);
                networkCreate();
            }
        }
    };

    private void sendHotSpotInfo() {
        try {
            HotspotServer hotspotServer = new HotspotServer();
            DLog.log("sending hotspot");
            if (mManager != null)
                mManager.removeGroup(mChannel, null);
            mChannel = null;
            WifiConfiguration details = hotspotServer.getWifiApConfiguration();
            if (details != null) {
                mSSID = details.SSID;
                mPass = details.preSharedKey;
                mAddr = "192.168.43.1";
                mFreqency = getFrequency();
                CommonUtil.getInstance().setNetworkType("hotspot");
                mNetworkState = NetworkState.ENetworkReady;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        reportServerStatus(0);
    }

    private void changeHotspot(boolean status) {
        HotspotServer hotspotServer = new HotspotServer();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (status) {
                hotspotServer.toggleHotspot(true);
            } else {
                hotspotServer.toggleHotspot(false);
            }
        }
    }

    private String getFrequency() {
        if (DeviceInfo.getInstance().is5GhzSupported()) {
            return String.valueOf(5000);
        }
        return String.valueOf(2500);
    }

    //=============================================================================
    //=============================================================================

    final static String kTagModule = "WiFiDirectServer";

    static private void trace(String aText) {
        Log.v(kTagModule, aText);
    }

    static private void log(String aText) {
        // Log.d(kTagModule, aText);
    }

    private void note(String aText) {
        DLog.log(aText);
    }

    private void warn(String aText) {
        Log.w(kTagModule, "WARN: " + aText);
    }

    private void error(String aText) {
        DLog.log("ERROR: " + aText);
    }

    private final int CHANGE_WIFI_STATE = 11;
    private final int RETRY = 12;

    private boolean retryConnection = false;
}
