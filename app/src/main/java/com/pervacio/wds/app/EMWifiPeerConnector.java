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
import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.custom.utils.CommonUtil;

public class EMWifiPeerConnector extends BroadcastReceiver implements ATimer.Callback
{
    static EMGlobals emGlobals = new EMGlobals();
    public interface Listener
    {
        void onWifiPeerConnection(boolean aSucces);
    }

    static private final String kWifiNetworksStateFile   = "WifiNetworks.json";
    //static private final String kPeerNetworksListFile    = "PeerNetworks.txt";
    static private final int    kWifiNetworkStateVersion = 1;

    static private final String PEER_NETWORK_PATTERN = "^\"iOS ([0-9a-f]{5})\"$";

    static private boolean     mWifiIsConnected = false;
    static private WifiManager mWifiManager     = null;
    static private Listener    mDelegate        = null;

    static private String mPeerNetworkWanted = null;
    static public int    mPeerNetworkId     = -1;

    static private String mWifiStateStoragePath    = null;

    static private final int  kConnectTimerId = 1;
    static private final int  kSettleTimerId  = 2;

    static private ATimer mConnectTimer    = null;
    //>>static private ATimer mSettleTimer     = null;

    static private int    mNumNetworkConnectAttempts = 0;
    static private int    mNumNetworkConnectChecks = 0;
    private static long   mLastConnectAttemptTime    = 0;

    // Public default constructor required for onReceive callback
    //
    public EMWifiPeerConnector()
    {
        //traceit(">> EMWifiPeerConnector");
        //traceit("<< EMWifiPeerConnector");
    }

    public EMWifiPeerConnector(Listener aDelegate)
    {
        traceit(">> EMWifiPeerConnector");

        Context appContext = EMUtility.Context();

        mDelegate    = aDelegate;
        mWifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);

        File storagePath  = appContext.getDir(EMConfig.EM_PRIVATE_DIR, Context.MODE_PRIVATE);

        storagePath.mkdirs();

        mWifiStateStoragePath = storagePath.getAbsolutePath() + File.separator + kWifiNetworksStateFile;

        logit("EMWifiPeerConnector, Making sure Wifi is switched on");

        //errorit("EMWifiPeerConnector, **** NOT CHECKING WIFI IS TURNED ON ****");
        CMDUtilsDevice.switchWifiOn(appContext, true); // Comment out to replicate DEC-108. MUST BE CALLED FOR RELEASE

        traceit("<< EMWifiPeerConnector");
    }

    // Start setting up the peer-2-peer network saving the current network configuration
    // and the name of the new peer network. The peer network will be deleted when
    // the app closes and the original networks restored.
    // If still have a state file, the app must have exited abnormally on the previous
    // run. As the state file contains the original network setup, so we do not want to
    // delete it. If the app did crash, the current networks may be in an incorrect
    // state.
    //
    // Because of problems encountered deleting and then re-using peer networks
    // we do NOT remove the new network when finished. This means the device will
    // be left with a peer network entry when finished.
    // Instead, we keep track of the peer networks that have been used previously
    // and delete those old ones. This keeps the network table reasonably clean
    // even after multiple runs. The network table should only contain one peer
    // network entry - the last one used.
    //
    public void start(String aWifiSSID, String aWifiPass)
    {
        traceit(">> start, SSID: " +aWifiSSID+ ", Pass: " +aWifiPass);

        mNumNetworkConnectAttempts = 0;
        mNumNetworkConnectChecks = 0;

        String peerSSID = String.format("\"%s\"", aWifiSSID);
        String peerPass = String.format("\"%s\"", aWifiPass);

        // Clean up any old peer networks from previous runs.
        //
        deleteOldPeerNetworks(null); //peerSSID);

        File wifiStateFile = new File(mWifiStateStoragePath);

        // Save the current network configuration if we do not already have one stored
        //
        if (! wifiStateFile.exists())
        {
            saveWifiNetworksState();
        }

        mPeerNetworkWanted = peerSSID;

        joinPeerNetwork(peerSSID, peerPass);

        traceit("<< start");
    }

	/*
		private static ConnectivityChangeReceiver mConnectivityReceiver = null;

		Context appContext = EMUtility.Context();
    	mConnectivityReceiver = new ConnectivityChangeReceiver();
    	IntentFilter connectivityFilter = new IntentFilter();
    	connectivityFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    	appContext.registerReceiver(mConnectivityReceiver, connectivityFilter);
	 */

    public void disconnect()
    {
        stopConnectionTimer();

        restoreWifiNetworks();
    }

    static private void stopConnectionTimer()
    {
        traceit(">> stopConnectionTimer");

        if (mConnectTimer != null)
        {
            logit("stopConnectionTimer, Stopping connection timer");
            mConnectTimer.cancel();
            mConnectTimer = null;
        }
        else
        {
            logit("stopConnectionTimer, No connection timer running");
        }

        traceit("<< stopConnectionTimer");
    }

    //==========================================================================
    // ATimer.Callback Methods
    //==========================================================================

    // Test our wifi connection status on a regular basis
    // This checks the wifi status to see if we are connected. Normally, the app will receive an intent
    // broadcast about the network becoming available but this we provide a double check here in case
    // that was missed for some reason. This appears to have happened on a Nexus 6.
    //
    // If we are not connected after kNetworkConnectTimeout, the function will attempt to re-connect
    // to the desired network but only to a maximum of kNetworkConnectMaxAttempts tries.
    // If there is still no connection after multiple attempts we give up and signal to our observer
    // about the failure.
    //
    public void onATimerTick(int aId)
    {
        logit("onATimerTick, Id: " +aId);

        if (aId == kConnectTimerId)
        {
            processConnectTimer();
            return;
        }

		/*
		if (aId == kSettleTimerId)
		{
			traceit("onATimerTick, Processing Settle Timer");

			mSettleTimer.cancel();

			if (mDelegate != null)
			{
				mDelegate.onWifiPeerConnection(true);
			}

			return;
		}
		*/
    }

    private void processConnectTimer()
    {
        logit(">> processConnectTimer");

        if (mConnectTimer == null)
        {
            warnit("processConnectTimer, Strange - Connect Timer is null");
            return;
        }

        if (mWifiIsConnected)
        {
            errorit("processConnectTimer, Strange - Got Timer Tick but network already connected");
            stopConnectionTimer();
            return;
        }

        if (mWifiError)
        {
            errorit("processConnectTimer, Wifi Error, Network ID: " +mPeerNetworkId);

            stopConnectionTimer();

            if (mDelegate != null)
            {
                restoreWifiNetworks();
                mDelegate.onWifiPeerConnection(false);
            }

            return;
        }

        Context appContext = EMUtility.Context();

        boolean wifiIsConnected = attemptWifiConnect(appContext);

        if (wifiIsConnected)
        {
            warnit("<< processConnectTimer, Strange - Poll found that Wifi is now Connected");
            return;
        }

        if (mNumNetworkConnectChecks < EMConfig.kNetworkConnectCheckTimeouts) {
            // If we haven't used all of our network checks then just wait, we might still connect
            mNumNetworkConnectChecks++;
        }
        else {
            long timeNow = System.currentTimeMillis();

            long connectElapsed = (timeNow - mLastConnectAttemptTime) / 1000;

            mNumNetworkConnectChecks = 0;
            mNumNetworkConnectAttempts++;

            warnit("processConnectTimer, Attempt: " + mNumNetworkConnectAttempts + " No connection within: " + connectElapsed);

            if (mNumNetworkConnectAttempts < EMConfig.kNetworkConnectMaxAttempts) {
                logit("processConnectTimer, Attempting to re-connect to network ID: " + mPeerNetworkId);

                mLastConnectAttemptTime = System.currentTimeMillis();

                mWifiManager.disconnect();
                mWifiManager.enableNetwork(mPeerNetworkId, true); // true == disable all other networks //>>
                mWifiManager.reconnect();

                logit("<< processConnectTimer");
                return;
            }

            errorit("processConnectTimer, Giving Up - Unable to connect to network ID: " + mPeerNetworkId);

            stopConnectionTimer();

            if (mDelegate != null) {
                restoreWifiNetworks();
                mDelegate.onWifiPeerConnection(false);
            }
        }

        logit("<< processConnectTimer");
    }

    // Join the specified network
    // If an network entry already exists with the same SSID we re-use that network.
    // Otherwise, a new network entry is created.
    // If we are already connected to the required network we simply inform the
    // delegate we are ready to go via the "onWifiConnection" callback.
    // If no, we disable all other networks and start scanning for the peer network.

    private boolean mWifiError = false;

    private void joinPeerNetwork(String aSSID, String aKey)
    {
        mWifiError = false;

        int peerNetworkId = findWifiNetwork(aSSID);

        if (peerNetworkId == -1)
        {
            peerNetworkId = createNetworkEntry(aSSID, aKey);
        }

        if (peerNetworkId == -1)
        {
            errorit("joinPeerNetwork, Could not find or create network entry: " +aSSID);

            mWifiError = true;

            if (mDelegate != null)
            {
                mDelegate.onWifiPeerConnection(false);
            }

            //mConnectTimer = new ATimer(this, kConnectTimerId, 2*1000); //>> BG: Ignore, Test code
            //mConnectTimer.start();

            return;
        }

        WifiInfo currentWifiInfo = mWifiManager.getConnectionInfo();

        int currentNetworkId = currentWifiInfo.getNetworkId();

        // Check if we are already connected to the required network
        //
        if (peerNetworkId == currentNetworkId)
        {
            mWifiIsConnected = true;

            if (mDelegate != null)
            {
                mDelegate.onWifiPeerConnection(true);
            }

            return;
        }

        logit("joinPeerNetwork, Starting connection poll timer");

        mConnectTimer = new ATimer(this, kConnectTimerId, EMConfig.kNetworkConnectCheckTimeout*1000);
        mConnectTimer.start();

        // We are joining a different network - save its Id so we can clean up later
        mPeerNetworkId = peerNetworkId;

        logit("joinPeerNetwork, Scanning for SSID: " +aSSID+ ", Network ID: " +peerNetworkId);

        mLastConnectAttemptTime = System.currentTimeMillis();

        //>>mSettleTimer = new ATimer(this, kSettleTimerId, 5000);

        mWifiManager.disconnect();
        disableOtherNetworks(peerNetworkId);
        mWifiManager.enableNetwork(mPeerNetworkId, true); // true == disable other networks //>>
        mWifiManager.reconnect();
    }

    // Start scanning for the peer to peer network
    // The intent method "onReceive" will be called when there is a change to
    // the network status. This will indicate when we have connected to the
    // the peer network.
    private int createNetworkEntry(String aSSID, String aKey)
    {
        logit("createNetworkEntry, SSID: " +aSSID+ ", Key: " +aKey);

        WifiConfiguration peerWifiInfo = new WifiConfiguration();

        peerWifiInfo.SSID         = aSSID;
        peerWifiInfo.preSharedKey = aKey;

        logit("createNetworkEntry, before calling addNetwork peer peerWifiInfo: " + peerWifiInfo);
        logit("createNetworkEntry, before calling addNetwork peer peerWifiInfo.SSID: " + peerWifiInfo.SSID);
        logit("createNetworkEntry, before calling addNetwork peer peerWifiInfo.preSharedKey: " + peerWifiInfo.preSharedKey);
        int peerNetworkId = mWifiManager.addNetwork(peerWifiInfo);

        logit("createNetworkEntry, peer Network ID: " +peerNetworkId);

        if (peerNetworkId == -1)
        {
            warnit("createNetworkEntry, Unable to add peer-2-peer network: " +aSSID+ "  [" +aKey+ "]");
        }

        return peerNetworkId;
    }


    //=========================================================================
    // In Android 5.0 there is a defect in enableNetwork, this should
    // disable all other networks when the second parameter is true.
    // However, it does not.
    // This routine simply iterates through all networks and disables them
    // provided its not the one we are about to use.
    //=========================================================================
    //
    private void disableOtherNetworks(int aCurrentNetworkId)
    {
        try
        {
            List<WifiConfiguration> wifiNetworks = mWifiManager.getConfiguredNetworks();

            if (wifiNetworks == null)
            {
                warnit("disableOtherNetworks, Unable to get list of configured wifi networks");
                return;
            }

            int numNetworks = wifiNetworks.size();

            for (int index = 0; index < numNetworks; index++)
            {
                WifiConfiguration thisNetwork = wifiNetworks.get(index);

                int thisNetworkId = thisNetwork.networkId;

                if (thisNetworkId != aCurrentNetworkId)
                {
                    mWifiManager.disableNetwork(thisNetworkId);
                }
            }

        }
        catch (Exception e)
        {
            warnit("disableOtherNetworks, Exception: " +e);
        }
    }

    //=========================================================================
    // onNetworkChangeReceived is called from the onRecieve intent which is
    // called when there has been a change in the network status.
    // We use this to watch out a connection with the peer network being established
    //=========================================================================
    public void onNetworkChangeReceived(Context aContext, Intent aIntent)
    {
        traceit(">> onNetworkChangeReceived");

        if (mPeerNetworkWanted == null)
        {
            traceit("<< onNetworkChangeReceived, Not scanning for a network - Ignoring broadcast");
            return;
        }

        ConnectivityManager conMan = (ConnectivityManager) emGlobals.getmContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo netInfo = conMan.getActiveNetworkInfo();

        // Following is for debugging - to see what types of broadcast events we are getting
        if (netInfo == null)
        {
            logit("onNetworkChangeReceived, No network information available");
        }
        else
        {
            logit("onNetworkChangeReceived, Message is about network type " +netInfo.getType());
        }

        attemptWifiConnect(aContext);

        traceit("<< onNetworkChangeReceived");
    }

    public void onReceive(Context aContext, Intent aIntent)
    {
        logit("onReceive");
        onNetworkChangeReceived(aContext, aIntent);
    }

    /*
    class ConnectivityChangeReceiver extends BroadcastReceiver
    {
    	@Override
    	public void onReceive(Context aContext, Intent aIntent)
    	{
    		logit("ConnectivityChangeReceiver::onReceive");
    		onNetworkChangeReceived(aContext, aIntent);
    	}
    }
    */

    // The following method is called from the onRecieve() method as a result of
    // a network change broadcast AND processConnectTimer() to check if we have
    // missed any broadcasts.

    synchronized static private boolean attemptWifiConnect(Context aContext)
    {
        traceit(">> attemptWifiConnect");

        if (mWifiIsConnected)
        {
            logit("attemptWifiConnect, Wifi already connected");
            return true;
        }

        ConnectivityManager conMan = (ConnectivityManager) emGlobals.getmContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (conMan == null)
        {
            errorit("attemptWifiConnect, Unable to obtains Connectivity Manager!!!");
            return false;
        }

        NetworkInfo netInfo = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (netInfo == null)
        {
            errorit("attemptWifiConnect, Could not get network info for WiFi!!!");
            return false;
        }

        NetworkInfo.State wifiState = netInfo.getState();

        traceit("attemptWifiConnect, Got Wifi State: " +wifiState);

        if (wifiState == NetworkInfo.State.CONNECTING)
        {
            logit("attemptWifiConnect, Wifi data connection not ready yet (still connecting)");
            return false;
        }

    	/*
    	 * Commented out this code because some Android versions (definitely 6.x) report that the network is not connected if there is no internet access
        if (! netInfo.isConnected())
        {
        	logit("attemptWifiConnect, Wifi data connection not ready yet");
        	return false;
        }
        */

        WifiManager wifiManager = (WifiManager) emGlobals.getmContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager == null)
        {
            errorit("attemptWifiConnect, Unable to obtain Wifi Manager!!!");
            return false;
        }

        WifiInfo currentWifiInfo = wifiManager.getConnectionInfo();

        if (currentWifiInfo == null)
        {
            errorit("attemptWifiConnect, Unable to get Wifi Connection Info!!!");
            return false;
        }

        int ipAddress = currentWifiInfo.getIpAddress();
        if (ipAddress == 0) {
            errorit("No IP address yet, so we're not connected");
            return false;
        }

        SupplicantState connectionState = currentWifiInfo.getSupplicantState();

        if (connectionState != SupplicantState.COMPLETED)
        {
            errorit("attemptWifiConnect, Strange - Wifi CONNECTED but not COMPLETED yet (" +connectionState+ ")");
            return false;
        }

        String currentNetwork = currentWifiInfo.getSSID();
        int currentNetWorkId = currentWifiInfo.getNetworkId();

        logit("attemptWifiConnect, SSID Now:   " +currentWifiInfo.getSSID());
        logit("attemptWifiConnect, Network ID: " +currentWifiInfo.getNetworkId());

        // Defect in 2.3.3 strips the quotes from the SSID name
        // Workaround is to remove the quotes from both the desired and current network SSIdDs
        String unquotedWantedName    = mPeerNetworkWanted.replace("\"", "");
        String unquotedConnectedName = currentWifiInfo.getSSID().replace("\"", "");

        DLog.log("ConnectedNetworkName : "+unquotedConnectedName+" ConnectedNetworkId : "+currentWifiInfo.getNetworkId());
        DLog.log("WantedNetworkName : "+unquotedWantedName+" WantedNetworkId : "+mPeerNetworkId);
        // Test if we are connected to the desired peer network.
        // If not, hopefully the reconnect retry timer will sort this out.
        //
        //if (! unquotedWantedName.equals(unquotedConnectedName))
        if (currentNetWorkId != -1 && currentNetWorkId != mPeerNetworkId) // Pairing issue for Pixel XL devices, comparing network ID instead of SSID
        {
            warnit("attemptWifiConnect, Connected to the Wrong Network - Wanted: " +mPeerNetworkId+ ", Now: " +currentWifiInfo.getNetworkId());
            mWifiManager.disableNetwork(currentNetWorkId);
            mWifiManager.disconnect();
            mWifiManager.enableNetwork(mPeerNetworkId,true);
            mWifiManager.reconnect();
            return false;
        }

        traceit("attemptWifiConnect, Peer Network Now Connected");
        int linkSpeed= currentWifiInfo.getLinkSpeed();
        DLog.log("WifiPeerConnector Link speed : "+linkSpeed);
        CommonUtil.getInstance().setLinkSpeed(linkSpeed);
        CommonUtil.getInstance().setGroupOwner(false);
        stopConnectionTimer();

        mWifiIsConnected = true;

        //traceit("attemptWifiConnect, Starting Settle Timer");
        //mSettleTimer.start();

        if (mDelegate != null)
        {
            mDelegate.onWifiPeerConnection(true);
        }

        traceit("<< attemptWifiConnect");

        return true;
    }

    //=========================================================================
    // Save the current network configuration
    //=========================================================================

    private void saveWifiNetworksState()
    {
        String stateData = getWifiNetworksInJson(mWifiManager);

        if (stateData != null)
        {
            EMUtilsFileIO.setFileContents(mWifiStateStoragePath, stateData.getBytes());
        }
    }


    private String getWifiNetworksInJson(WifiManager aWifiManager)
    {
        traceit(">> getWifiNetworksInJson");

        String data = null;

        try
        {
            JSONArray wifiList = new JSONArray();

            List<WifiConfiguration> wifiNetworks = aWifiManager.getConfiguredNetworks();

            if (wifiNetworks == null)
            {
                warnit("getWifiNetworksInJson, Unable to get list of configured wifi networks");
                return null;
            }

            int numNetworks = wifiNetworks.size();

            for (int index = 0; index < numNetworks; index++)
            {
                JSONObject networkInfo = new JSONObject();

                WifiConfiguration thisNetwork = wifiNetworks.get(index);

                String ssid      = thisNetwork.SSID;
                int    status    = thisNetwork.status;
                int    networkId = thisNetwork.networkId;

                networkInfo.put("SSID",      ssid);
                networkInfo.put("networkId", networkId);
                networkInfo.put("status",    status);

                wifiList.put(networkInfo);
            }

            JSONObject wifiState = new JSONObject();

            WifiInfo currentWifiInfo = mWifiManager.getConnectionInfo();

            int currentNetworkId = currentWifiInfo.getNetworkId();

            wifiState.put("version",           kWifiNetworkStateVersion);
            wifiState.put("originalNetworkId", currentNetworkId);
            wifiState.put("networkList",       wifiList);

            data = wifiState.toString();
        }
        catch (Exception e)
        {
            warnit("getWifiNetworksInJson, Exception: " +e);
            data = null;
        }

        logit("getWifiNetworksInJson, Data: " +data);

        traceit("<< getWifiNetworksInJson");

        return data;
    }

    //=========================================================================
    // Restore the original network configuration
    //=========================================================================
    private void restoreWifiNetworks()
    {
        traceit(">> restoreWifiNetworks");

        if (mWifiManager == null)
        {
            traceit("<< restoreWifiNetworks, No WifiManager");
            return;
        }

        File wifiStateFile = new File(mWifiStateStoragePath);

        if (! wifiStateFile.exists())
        {
            traceit("<< restoreWifiNetworks, No Wifi State File: " +mWifiStateStoragePath);
            return;
        }

        byte[] networkStateData = EMUtilsFileIO.getFileContents(mWifiStateStoragePath);

        wifiStateFile.delete();

        if (networkStateData == null)
        {
            logit("restoreWifiNetworks, Unable to network state file: " +mWifiStateStoragePath);
            traceit("<< restoreWifiNetworks");
            return;
        }

        int originalNetworkId = -1;
        JSONArray networkList = null;

        try
        {
            String networkStateStr  = new String(networkStateData, "UTF-8");

            traceit("restoreWifiNetworks, State JSON: " +networkStateStr);

            JSONObject wifiState = new JSONObject(networkStateStr);

            networkList       = wifiState.getJSONArray("networkList");
            originalNetworkId = wifiState.getInt("originalNetworkId");

			/*
			if (mConnectivityReceiver != null)
			{
				Context appContext = EMUtility.Context();
				appContext.unregisterReceiver(mConnectivityReceiver);
				mConnectivityReceiver = null;
			}
			*/
        }
        catch (Exception e)
        {
            errorit("restoreWifiNetworks, Exception: " +e);
        }

        traceit("restoreWifiNetworks, Original Network Id: " +originalNetworkId);

        mPeerNetworkWanted = null;
        mWifiIsConnected   = false;

        mWifiManager.disconnect();

        // On Android 5.1, trying to enable a network without disabling all the others
        // does not work. It does not enable the desired network. Here we temporarily
        // disable all the others and then individually re-enable them below.
        // On 5.1, this at least restores the primary network but leaves the others
        // reporting a connection error. However, that appears to be a different
        // Android bug!
        //
        if (originalNetworkId != -1)
        {
            mWifiManager.enableNetwork(originalNetworkId, true); // false == do not disable other networks //>>
        }

        // We joined a new peer network disable it

        if (mPeerNetworkId != -1)
        {
            mWifiManager.removeNetwork(mPeerNetworkId);
        }

        deleteOldPeerNetworks(null);

        // Individually re-enable all the other networks
        //
        if (networkList != null)
        {
            restoreNetworksState(networkList);
        }

        mWifiManager.reconnect();

        traceit("<< restoreWifiNetworks");
    }

    // Loop through all the networks we have state for and enable them if required
    //
    private void restoreNetworksState(JSONArray aNetworkList)
    {
        traceit(">> restoreNetworksState");

        try
        {
            int numNetworks = aNetworkList.length();

            for (int index = 0; index < numNetworks; index++)
            {
                JSONObject thisNetwork = aNetworkList.getJSONObject(index);

                String ssid      = thisNetwork.getString("SSID");
                int    networkId = thisNetwork.getInt("networkId");
                int    status    = thisNetwork.getInt("status");

                // Check the original state and re-enable this network if necessary
                //
                if (status != WifiConfiguration.Status.DISABLED)
                {
                    logit("restoreNetworksState, Enabling Network: " +ssid+ " (ID: " +networkId+ ")");
                    mWifiManager.enableNetwork(networkId, false);
                }
            }
        }
        catch (Exception e)
        {
            errorit("restoreNetworksState, Exception: " +e);
        }

        traceit("<< restoreNetworksState");
    }

    //=========================================================================
    // Delete any WiFi network that looks like an old peer network
    // This is decided by matching its SSID name against the pattern "iOS xxxxx"
    // where x is a lower case hexadecimal digit.
    //=========================================================================
    //
    private void deleteOldPeerNetworks(String aCurrentSSID)
    {
        traceit(">> deleteOldPeerNetworks");

        try
        {
            Pattern pattern = Pattern.compile(PEER_NETWORK_PATTERN);

            if (pattern == null)
            {
                warnit("<< deleteOldPeerNetworks, Unable to compile peer network pattern");
                return;
            }

            List<WifiConfiguration> wifiNetworks = mWifiManager.getConfiguredNetworks();

            if (wifiNetworks == null)
            {
                warnit("deleteOldPeerNetworks, Unable to get list of configured wifi networks");
                return;
            }

            int numNetworks = wifiNetworks.size();

            for (int index = 0; index < numNetworks; index++)
            {
                WifiConfiguration thisNetwork = wifiNetworks.get(index);

                String thisSSID      = thisNetwork.SSID;
                int    thisNetworkId = thisNetwork.networkId;

                if (thisSSID == null || thisSSID.length() == 0)
                {
                    traceit("deleteOldPeerNetworks, Existing Network Has No SSID");
                    continue;
                }

                traceit(">> deletePeerNetworks, Found Network: " +thisSSID);

                if (aCurrentSSID != null && aCurrentSSID.equals(thisSSID))
                {
                    traceit("deleteOldPeerNetworks, Skipping Current Network: " +thisSSID);
                    continue;
                }

                Matcher matcher = pattern.matcher(thisSSID);

                boolean isOldPeer = matcher.matches();

                if (isOldPeer)
                {
                    logit("deleteOldPeerNetworks, Found Old Peer - Removing: " +thisNetworkId+ ", SSID: " +thisSSID);
                    mWifiManager.removeNetwork(thisNetworkId);
                }
            }
        }
        catch (Exception e)
        {
            errorit("deleteOldPeerNetworks, Exception: " +e);
        }

        traceit("<< deleteOldPeerNetworks");
    }


    private int findWifiNetwork(String aTargetSSID)
    {
        traceit("<< findWifiNetwork, SSID: " +aTargetSSID);

        List<WifiConfiguration> wifiNetworks = mWifiManager.getConfiguredNetworks();

        if (wifiNetworks == null)
        {
            warnit("findWifiNetwork, Unable to get list of configured wifi networks");
            return -1;
        }

        int numNetworks = wifiNetworks.size();

        int targetNetworkId = -1;

        for (int index = 0; index < numNetworks; index++)
        {
            WifiConfiguration thisNetwork = wifiNetworks.get(index);

            String thisSSID      = thisNetwork.SSID;
            int    thisNetworkId = thisNetwork.networkId;

            //traceit(">> findWifiNetwork, Network: " +thisSSID);

            if (aTargetSSID.equals(thisSSID))
            {
                logit("findWifiNetwork, Found: " +thisNetworkId+ ", SSID: " +thisSSID);
                targetNetworkId = thisNetworkId;
                break;
            }
        }

        traceit("<< findWifiNetwork, Found: " +targetNetworkId);

        return targetNetworkId;
    }



    //=========================================================================


    private final static String TAG = "EMWifiPeerConnector";

    static private void traceit(String aText)
    {
        //Log.v(TAG, aText);
        DLog.log(aText);
    }

    static private void logit(String aText)
    {
        //// Log.d(TAG, aText);
        DLog.log(aText);
    }

    static private void warnit(String aText)
    {
        //Log.e(TAG, aText);
        DLog.log(aText);
    }

    static private void errorit(String aText)
    {
        //Log.e(TAG, aText);
        DLog.log(aText);
    }
}