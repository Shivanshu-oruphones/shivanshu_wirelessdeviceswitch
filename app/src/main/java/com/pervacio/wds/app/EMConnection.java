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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.sdk.CMDError;
import com.pervacio.wds.sdk.internal.CMDCryptoSettings;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class EMConnection implements EMAsyncStreamDelegate {
    public static EMGlobals emGlobals = new EMGlobals();

    enum EMConnectionState {
        EM_CONNECTION_STATE_DISCONNECTED,
        EM_CONNECTION_STATE_CONNECTED,
        EM_CONNECTION_STATE_SENDING_FILE,
        EM_CONNECTION_STATE_GETTING_XML_FILE,
        EM_CONNECTION_STATE_GETTING_RAW_FILE,
        EM_CONNECTION_STATE_SENDING_TEXT,
        EM_CONNECTION_STATE_GETTING_TEXT,
        EM_CONNECTION_STATE_FINISHED,
        EM_CONNECTION_STATE_SENDING_NOOP,
    }

    ;

    static final int BUFFER_SIZE = 1024 * 128;

// -(EMConnection*) initWithNetService:(NSNetService*)netService;
// TODO: implement the above if we add browsing for mDNS services

    void clean() {
        mWriteBuffer = new byte[BUFFER_SIZE + 1]; // + 1 for any null that we might want to add
        mReadBuffer = new byte[BUFFER_SIZE + 1]; // + 1 for any null we might want to add
        String rootString = "</root>";
        String altRootString = "<root/>";
        try {
            mRootEndCharArray = rootString.getBytes("UTF-8");
            mAltRootEndCharArray = altRootString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            DLog.log(e);
        }
    }

    EMConnection(InetAddress aHostName, int aPortNumber, boolean aBindToWiFiLan)
    // -(EMConnection*) initWithHostNameAndPort:(NSString*)hostName port:(int)portNumber;
    {
        clean();
        mBindToWiFiLan = aBindToWiFiLan;
        mHostName = aHostName;
        mPort = aPortNumber;
        mRemoteIpAddress = mHostName;
        connect();
    }

    public EMConnection(Socket aSocket) {
        clean();
        mSocket = aSocket;
        connect();
    }

    public void close() {
        if (mSocket != null) {
            disableTimeoutTimer();
            try {
                mSocket.close();
            } catch (IOException e) {
                DLog.log("Failed to close connection");
            }
        }
    }

    void connect() {
        try {
            if (mSocket == null) {
                if (Build.VERSION.SDK_INT > 9) {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                }

                // Create an unconnected socket
                mSocket = new Socket();
//                CommonUtil commonUtil = CommonUtil.getInstance();
//                String mWiFiDirectPeerSSID = commonUtil.getmWiFiPeerSSID();
//                String mWiFiDirectPeerPassphrase = commonUtil.getmWiFiPeerPassphrase();
//                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P && ) {
//                    WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
//                            .setSsid(mWiFiDirectPeerSSID)
////                        .setBssid(MacAddress.fromString(bssid))
//                            .setWpa2Passphrase(mWiFiDirectPeerPassphrase)
//                            .build();
//
//                    NetworkRequest request = new NetworkRequest.Builder()
//                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//                            .setNetworkSpecifier(specifier)
//                            .build();
//
//                    final ConnectivityManager manager = (ConnectivityManager) emGlobals.getmContext().getSystemService(Context.CONNECTIVITY_SERVICE);
//                    manager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
//
//                                @Override
//                                public void onAvailable(@NonNull Network network) {
//                                    super.onAvailable(network);
//                                    manager.bindProcessToNetwork(network);
//                                    NetworkInfo info = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//                                    if (info != null && info.isConnectedOrConnecting()) {
//                                        Toast.makeText(emGlobals.getmContext(), "Connected!!!", Toast.LENGTH_SHORT).show();
//                                    } else {
//                                        Toast.makeText(emGlobals.getmContext(), "Connecting...", Toast.LENGTH_SHORT).show();
//                                    }
//
//                                }
//                            }
//                    );
//                }

//                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                    CommonUtil commonUtil = CommonUtil.getInstance();
//                    String mWiFiDirectPeerSSID = commonUtil.getmWiFiPeerSSID();
//                    String mWiFiDirectPeerPassphrase = commonUtil.getmWiFiPeerPassphrase();
//                    WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
//                    builder.setSsid(mWiFiDirectPeerSSID);
//                    builder.setWpa2Passphrase(mWiFiDirectPeerPassphrase);
//
//                    WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();
//                    NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
//                    networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
//                    networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier);
//                    NetworkRequest networkRequest = networkRequestBuilder.build();
//                    final ConnectivityManager cm = (ConnectivityManager) emGlobals.getmContext().getSystemService(Context.CONNECTIVITY_SERVICE);
//                    if (cm != null) {
//                        cm.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
//                            @Override
//                            public void onAvailable(@NonNull Network network) {
//                                super.onAvailable(network);
//                                cm.bindProcessToNetwork(network);
//                            }
//                        });
//                    }
//                }

                if (EMUtility.shouldBindSocketToWifi())
                    EMUtility.bindSocketToWiFiNetwork(mSocket);

                // TODO: read / write stream open() is redundant

                // TODO: sharing the same socket between the read and write stream is dangerous if both try
                // to read and write for the first time at the same time, in this case an attempt to open the
                // socket could happen in both threads and we'd get a race condition
                // For now this isn't a massive issue as we're either reading, or writing, not both.

                mReadStream = new EMAsyncStream(this, mHostName, mPort, mSocket, EMAsyncStream.EM_READ_MODE);
                mReadStream.open();
                mWriteStream = new EMAsyncStream(this, mHostName, mPort, mSocket, EMAsyncStream.EM_WRITE_MODE);
                mReadStream.open();
            } else {
                mReadStream = new EMAsyncStream(this, mSocket, EMAsyncStream.EM_READ_MODE);
                mReadStream.open();
                mWriteStream = new EMAsyncStream(this, mSocket, EMAsyncStream.EM_WRITE_MODE);
                mReadStream.open();
            }

            if (mHostName == null) {
                // Don't use any of these, we can't connect back to the provided address
                // mRemoteIpAddress = ((InetSocketAddress) mSocket.getRemoteSocketAddress()).getAddress();
                // mRemoteIpAddress = mSocket.getInetAddress();
            }
        } catch (Exception ex) {
            // TODO: throw connection exception
            ex.printStackTrace();
            DLog.log(ex);
        }
    }

    void setMainConnection() {
        mIsMainConnection = true;
    }

    private boolean mIsMainConnection = false;

    // Used to inform the connection that this is a hand-shake session - errors aren't reported to the user when in this state
    void setHandshakeConnection() {
        mIsHandshakeConnection = true;
    }

    void finish() {
        mState = EMConnectionState.EM_CONNECTION_STATE_FINISHED;
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                DLog.log("Unable to close socket", e);
            }
        }
    }

    void setDelegate(EMConnectionDelegate aDelegate) {
        mDelegate = aDelegate;
    }

    private void doSendText() {
        DLog.log(">> doSendText");

        DLog.log(String.format("doSendText: mCurrentTextString %s", mCurrentTextString));

        String commandWithCrLf = new String(mCurrentTextString);
        commandWithCrLf = commandWithCrLf + "\r\n";

        byte utf8String[];
        try {
            utf8String = commandWithCrLf.getBytes("UTF-8");

            mBytesLeftToWrite += utf8String.length;

            if (utf8String.length > BUFFER_SIZE) {
                // TODO:
//	            EM_EXCEPTION_RAISE(EM_EXCEPTION_COMMAND_TOO_LONG);
            }

            for (int index = 0; index < utf8String.length; index++) {
                mWriteBuffer[index] = utf8String[index];
            }

            mWriteBufferPosition = 0;

            // Try to write the data now, but it might not get written until later
            writeTextBuffer();

        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            DLog.log(e);
        }

        DLog.log("<< doSendText");
    }

    // Send the string, plus the line ending
    // Used for sending commands and responses
    // Sending is asynchronous - the delegate is notified when it is complete
    void sendText(String aText) {
        DLog.log("*** sendText: " + aText);

        if (mState == EMConnectionState.EM_CONNECTION_STATE_SENDING_NOOP) {
            mQueuedTextToSend = aText;
            DLog.log("*** Already sending NOOP, so queue the text for later");
        } else {
            if (aText.equalsIgnoreCase(EMStringConsts.EM_COMMAND_TEXT_NOOP)) {
                mState = EMConnectionState.EM_CONNECTION_STATE_SENDING_NOOP;
                DLog.log("EM_CONNECTION_STATE_SENDING_NOOP");
            } else {
                mState = EMConnectionState.EM_CONNECTION_STATE_SENDING_TEXT;
                DLog.log("EM_CONNECTION_STATE_SENDING_TEXT");
            }

            mCurrentTextString = aText;

            DLog.log(String.format("*** mCurrentTextString: %s", mCurrentTextString));


            if ((mHostName == null) && (mSocket == null)) {
                // TODO: we don't resolve in Android - at present
                // We should have the hostname and socket at this point
            } else {
                doSendText();
            }
        }

        DLog.log("<< sendText");
    }

    private void raiseError(int aError) {
        if (mState != EMConnectionState.EM_CONNECTION_STATE_FINISHED) { // Ignore any errors if this connection is finished (for example if it has been closed because it has become unresponsive)
            mDelegate.error(aError);
        }
    }

    private Timer mNoopTimer = new Timer();
    boolean mNoopsEnabled = false;

    // Send a NOOP - if there is nothing in the buffer already
    private void sendNoop() {
        // Only send the NOOP if there's nothing in the buffer, and we're not waiting on anything or sending anything
        if ((mBytesLeftToWrite == 0) && (mState == EMConnectionState.EM_CONNECTION_STATE_CONNECTED)) {
            sendText(EMStringConsts.EM_COMMAND_TEXT_NOOP);
        }
    }

    private Handler mNoopHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mNoopsEnabled) {
                sendNoop();
                startNoopTimer(); // Repost the NOOP timer
            }
        }
    };

    public void startNoopTimer() {
        cancelNoopTimer();
        try {
            mNoopsEnabled = true;
            mNoopTimer = new Timer();
            mNoopTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mNoopHandler.obtainMessage(1).sendToTarget();
                }
            }, EMConfig.NOOP_TIMER_IN_SECONDS * 1000);
        } catch (Exception ex) {
            // If we can't set the noop timer for some reason then just ignore
        }
    }

    public void stopNoopTimer() {
        mNoopsEnabled = false;
        cancelNoopTimer();
    }

    private void cancelNoopTimer() {
        if (mNoopTimer != null) {
            mNoopTimer.cancel();
        }
    }

    private Timer mTimeoutTimer = new Timer();
    boolean mTimedOut = false;
    boolean mTimeoutTimerEnabled = false;

    private Handler mTimeoutHandler = new Handler() {
        public void handleMessage(Message msg) { // Dispatch timeout on the main thread
            if (mState != EMConnectionState.EM_CONNECTION_STATE_FINISHED) {
                DLog.log("*** Connection timed out");
                raiseError(CMDError.CMD_WIFI_TIMEOUT_ERROR);
            }
        }
    };

    public void enableTimeoutTimer() {
        DLog.log("*** Timeout timer enabled");
        //mTimeoutTimerEnabled = true;
    }

    public void disableTimeoutTimer() {
        DLog.log("*** Timeout timer disabled");
        stopTimeoutTimer();
        mTimeoutTimerEnabled = false;
    }

    private void startOrResetTimeoutTimer() {
        if (mState != EMConnectionState.EM_CONNECTION_STATE_FINISHED) {
            if ((mIsMainConnection) && (mTimeoutTimerEnabled)) { // Only timeout if we're the main connection
                DLog.log("*** Resetting timeout timer");
                stopTimeoutTimer();

                try {
                    mTimeoutTimer = new Timer();
                    mTimeoutTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (mTimeoutTimerEnabled) {
                                mTimedOut = true;
                                mTimeoutHandler.obtainMessage(1).sendToTarget();
                            }
                        }
                    }, EMConfig.WIFI_TIMEOUT_IN_SECONDS * 1000);
                } catch (Exception ex) {
                    // If we can't set the timeout timer for some reason then just ignore
                }
            }
        }
    }

    private void stopTimeoutTimer() {
        if (mTimeoutTimer != null) {
            mTimeoutTimer.cancel();
        }
    }

    private void sendFileDone() {
        if (mDelegate != null) {
            mDelegate.sent();
        }
    }

    private void doSendFile() {
        try {
            int bytesReadFromFile = 0;

            int bytesWritten = 0;

            do {
                if (mBytesLeftToWrite != 0) {
                    bytesWritten = writeBuffer();
                }

                if (mBytesLeftToWrite == 0) {
                    boolean endOfFile = false;
                    bytesReadFromFile = 0;
                    while ((!endOfFile) && (bytesReadFromFile < BUFFER_SIZE)) {
                        int bytesReadFromFileThisTime = mFileReadStream.read(mWriteBuffer, bytesReadFromFile, BUFFER_SIZE - bytesReadFromFile);

                        if ((bytesReadFromFileThisTime == -1) || (bytesReadFromFileThisTime == 0)) { // Should == 0 signify an end of file?
//							bytesReadFromFileThisTime = 0; // If we read 0 bytes from the file then we assume we're at the end (TODO: safe assumption?)
                            endOfFile = true;
                        } else {
                            bytesReadFromFile += bytesReadFromFileThisTime;
                        }
                    }

                    mBytesLeftToWrite = bytesReadFromFile;
                    mWriteBufferPosition = 0;
                    bytesWritten = writeBuffer();
                }

                // If there are no bytes read from the file, and no bytes left to write then we're done sending this file
                if ((bytesReadFromFile == 0) && (mBytesLeftToWrite == 0)) {
                    mState = EMConnectionState.EM_CONNECTION_STATE_CONNECTED;
                    mFileReadStream.close();

                    if (mDeleteFileWhenDone) {
                        //	                [[NSFileManager defaultManager] removeItemAtPath:mTempFileName error:NULL];
                        // TODO: delete the sent file
                    }

                    if (mDelegate != null) {
                        mDelegate.sent();
                    }
                }

            } while (bytesWritten != 0);
        } catch (Exception ex) {
            // TODO: some kind of file error - how to handle this?
            DLog.log(ex);
        }
    }

    private void doSendFileEx() {
        try {
            mWriteStream.writeEx(mFileReadStream, mBytesLeftToWrite);
        } catch (Exception ex) {
            // TODO: some kind of file error - how to handle this?
            DLog.log(ex);
        }
    }


    // Send a file
    // Sending is asynchronous - the delegate is notified when it is complete
    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {
        mFileProgressDelegate = aFileSendingProgressDelegate;
        try {
            mEndOfFile = false;

            mDeleteFileWhenDone = aDeleteFileWhenDone;
            mTempFileName = aFilePath;

            mFileReadStream = new FileInputStream(aFilePath); // [NSInputStream inputStreamWithFileAtPath:filePath];

            // If crypto is enabled then read through a crypto stream
            if (CMDCryptoSettings.enabled()) {
                DLog.log("*** Reading crypto file");
                mFileReadStream = CMDCryptoSettings.getCipherEncryptInputStream(mFileReadStream);
            }

		    /* if (mFileReadStream == nil)
		    {
		        EM_EXCEPTION_RAISE_NO_FILE(filePath);
		    }
		    else */
            {
                mState = EMConnectionState.EM_CONNECTION_STATE_SENDING_FILE;
                DLog.log("EM_CONNECTION_STATE_SENDING_FILE");

                File file = new File(aFilePath);
                mFileBytesTotal = file.length();

                mFileBytesSent = 0;
                mWriteBufferPosition = 0;
                mBytesLeftToWrite = 0;

                //doSendFile();
                doSendFileEx();
            }
        } catch (Exception ex) {
            // TODO: a file error - how should we handle this?
            DLog.log(ex);
        }
    }

    boolean isWhitespace(byte aByte) {
        boolean whitespace = false;

        switch (aByte) {
            case ' ':
            case '\n':
            case '\r':
            case 9: // tab
                whitespace = true;
            default:
                break;
        }

        return whitespace;
    }

    void doGetText() {
        DLog.log(">> doGetText");
        int maxBytesToRead = BUFFER_SIZE - mReadBufferPosition;

        int bytesRead = 0;

        if (mReadStream.hasBytesAvailable()) {
            DLog.log("Bytes available...");

            bytesRead = mReadStream.read(mReadBuffer, mReadBufferPosition, maxBytesToRead);

            DLog.log(String.format("Bytes read: %d", bytesRead));

            if (bytesRead > 0) {
                startOrResetTimeoutTimer();
                mReadBufferPosition += bytesRead;

                if (mReadBufferPosition > 2) {
                    if ((mReadBuffer[mReadBufferPosition - 2] == '\r')
                            && (mReadBuffer[mReadBufferPosition - 1] == '\n')) {
                        // If we have a cr lf at the end of the buffer, if we do then notify the client and drop back to the connected state
                        // mReadBuffer[mReadBufferPosition - 2] = 0; // Add a null (the real buffer is a byte longer than the const size)

                        int leadingWhitespaceCharacters = 0;

                        if (mReadBufferPosition > 0) {
                            while ((isWhitespace(mReadBuffer[leadingWhitespaceCharacters])) && (mReadBuffer[leadingWhitespaceCharacters] != 0)) {
                                leadingWhitespaceCharacters++;
                            }
                        }

                        String string = new String(mReadBuffer, leadingWhitespaceCharacters, mReadBufferPosition - 2 - leadingWhitespaceCharacters);
                        DLog.log(String.format("Got string: %s", string));

                        // We have copied everything in this buffer, so clear it
                        mReadBufferPosition = 0;

                        // Split strings, in case we have NOOP\r\n[RESPONSE]\r\n
                        // We could get the case where we might have NOOP\r\n\[RESPO
                        // If so, wait for NSE]\r\n, it is imminent if the first part of the command has already been received
                        boolean gotRealCommand = false; // Have we got a real command? i.e. something that is not a NOOP
                        String[] responses = string.split("\r\n");
                        for (String response : responses) {
                            if (response.equalsIgnoreCase(EMStringConsts.EM_COMMAND_TEXT_NOOP)) {
                                // Reset the timeout but otherwise ignore
                                continue;
                            }

                            mState = EMConnectionState.EM_CONNECTION_STATE_CONNECTED;
                            DLog.log("EM_CONNECTION_STATE_CONNECTED");
                            mDelegate.gotText(response);
                            gotRealCommand = true;
                        }

                        if (!gotRealCommand) {
                            doGetText();
                        }
                    }
                }
            }

            // If we don't have the cr lf at the end of the buffer then wait for more data ...
        } else {
            DLog.log("No bytes available...");
        }

        DLog.log("<< doGetText");
    }

    // Starts listening for a line of text
// The delegate is notified when it is received
    void getText() {
        mState = EMConnectionState.EM_CONNECTION_STATE_GETTING_TEXT;
        DLog.log("EM_CONNECTION_STATE_GETTING_TEXT");
        mReadBufferPosition = 0;
        doGetText();
    }

    void doGetXmlAsFile() {
        DLog.log(">> doGetXmlAsFile");

        try {

            mReadBufferPosition = 0;
            int bytesRead = 0;

            boolean foundRootEnd = false;

            do {
                if (mReadStream.hasBytesAvailable()) {
                    // bytesRead = [mReadStream read:(mReadBuffer + mReadBufferPosition) maxLength:BUFFER_SIZE];
                    bytesRead = mReadStream.read(mReadBuffer, mReadBufferPosition, BUFFER_SIZE - mReadBufferPosition);

                    if (bytesRead > 0) {
                        startOrResetTimeoutTimer();
                    }
		            /*
		            if (bytesRead == -1)
		            {
		                // TODO: assert?
		                DLog("Error reading xml");
		            }
		            // Errors are handled differently on Android
		            */
                } else {
                    bytesRead = 0;
                    DLog.log("No bytes available");
                }

                // Look for the </root>
                int lastCharacterOfRootEndInBuffer = 0;
                for (int index = 0; index < bytesRead; index++) {
                    if (mReadBuffer[index] == mRootEndCharArray[mRootEndCounter++]) {
                        if (mRootEndCounter == 7) {
                            lastCharacterOfRootEndInBuffer = index;
                            foundRootEnd = true;
                            break;
                        }
                    } else {
                        if (mReadBuffer[index] == mRootEndCharArray[0]) {
                            mRootEndCounter = 1;
                        } else {
                            mRootEndCounter = 0;
                        }
                    }

                    if (mReadBuffer[index] == mAltRootEndCharArray[mAltRootEndCounter++]) {
                        if (mAltRootEndCounter == 7) {
                            lastCharacterOfRootEndInBuffer = index;
                            foundRootEnd = true;
                            break;
                        }
                    } else {
                        if (mReadBuffer[index] == mAltRootEndCharArray[0]) {
                            mAltRootEndCounter = 1;
                        } else {
                            mAltRootEndCounter = 0;
                        }
                    }

                }

                if (bytesRead > 0) {
                    if (foundRootEnd) {
                        // int tempBytesToWrite = lastCharacterOfRootEndInBuffer + 1;
                        // If we have the </root> then only write up to this point, close the file then notify the delegate
                        mFileOutputStream.write(mReadBuffer, 0, lastCharacterOfRootEndInBuffer + 1);
                        // TODO: check we have written the right number of file bytes - adjust the mReadBufferPosition accordingly - should we loop until we have written all of the buffer

                        // NSLog(@"File bytes to write: %d, bytes written: %d", tempBytesToWrite, fileBytesWritten);

                        mReadBufferPosition = 0;

                        mFileOutputStream.close();

                        // NSError* nsError;

                        // NSString* fileContents = [NSString stringWithContentsOfFile:mTempFileName encoding:NSUTF8StringEncoding error:&nsError];

                        // NSLog(@"File contents: %@", fileContents);

                        mState = EMConnectionState.EM_CONNECTION_STATE_CONNECTED;
                        DLog.log("EM_CONNECTION_STATE_CONNECTED");

                        mDelegate.gotFile(mTempFileName);
                    } else {
                        // If we don't have the </root> then write what we have and read some more from the socket
                        mFileOutputStream.write(mReadBuffer, 0, bytesRead);
                        // TODO: check we have written the correct amount of data

                        // NSData* bytes = [[NSData alloc] initWithBytes:mReadBuffer length:bytesRead];
                        // NSString* bytesString = [[NSString alloc] initWithData:bytes encoding:NSUTF8StringEncoding];
                        // NSLog(@"Bytes read: %@", bytesString);

                        // NSLog(@"File bytes to write: %d, bytes written: %d", bytesRead, fileBytesWritten);

                        // TODO: we're assuming that we're written the whole socket read buffer to the file - maybe we shouldn't?
                        mReadBufferPosition = 0;
                    }


                }

                //	        mReadBufferPosition += bytesRead;

            } while ((bytesRead > 0) && (!foundRootEnd));
        } catch (Exception ex) {
            ex.printStackTrace();
            DLog.log(ex);
            // TODO: handle errors, most likely file errors
        }
        DLog.log("<< doGetXmlAsFile");
    }

    // Starts listening for XML data and saves it into a temporary file
// The delegate is notified when the XML is ready (when we have the </root> element)
    void getXmlAsFile() {
        mRootEndCounter = 0;
        mAltRootEndCounter = 0;

        mTempFileName = EMUtility.temporaryFileName();

        mReadBufferPosition = 0;

        try {
            mFileOutputStream = new FileOutputStream(mTempFileName);

            mState = EMConnectionState.EM_CONNECTION_STATE_GETTING_XML_FILE;
            DLog.log("EM_CONNECTION_STATE_GETTING_XML_FILE");

            // Start reading from the socket and writing to the XML file
            doGetXmlAsFile();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            DLog.log(e);
        } // [[NSOutputStream alloc] initToFileAtPath:mTempFileName append:NO];
    }

    // Starts listening for raw data and saves it into a file
// The delegate is notified when the data is ready (when we have read [length] bytes)
    void getRawDataAsFile(long aLength, String aTargetFilePath) {
        DLog.log(">> getRawDataAsFile: " + aTargetFilePath);
        mState = EMConnectionState.EM_CONNECTION_STATE_GETTING_RAW_FILE;
        mTempFileName = aTargetFilePath;

        String filePath = aTargetFilePath; // The path of the file to be created on the file system

        if (filePath != null) {
            File file = new File(filePath);
            file.getParentFile().mkdirs();

            // TODO: handle other file types, or only these for now?
            // TODO: later we could also use other information, e.g. album name, artist (for music), etc. (and create subpaths as appropriate)
            // TODO: check that the file doesn't already exist?? or just overwrite for now? or create unique file name? Could cause issues for music

            try {
                // TODO: open the file for writing
                mRawDataFileOutputStream = new FileOutputStream(file);
            } catch (Exception ex) {
                // TODO: handle this
                DLog.log(ex);
            }

            mOutputStreamWrapper = mRawDataFileOutputStream;

            if (CMDCryptoSettings.enabled()) {
                try {
                    DLog.log("*** Writing crypto file");
                    // Wraper the file output stream with a decrypting cipher stream (so we decrypt data on the fly as we're writing it to the file)
                    mOutputStreamWrapper = CMDCryptoSettings.getCipherDecryptOutputStream(mRawDataFileOutputStream);
                    mOutputStreamWrapper = new BufferedOutputStream(mOutputStreamWrapper);
                    // Adjust aLength for encrypted size
                    long extraBytesNeeded = (16 - (aLength % 16));
                    aLength = aLength + extraBytesNeeded;
                } catch (Exception ex) {
                    // TODO: handle crypto exception // TODO: throw fatal exception
                    DLog.log(ex);
                }
            }

            mBytesLeftToRead = aLength;

            doGetRawDataAsFile();
        }
        DLog.log("<< getRawDataAsFile");
    }

    void doGetRawDataAsFile() {
        DLog.log(">> doGetRawDataAsFile");
        mReadBufferPosition = 0;
        int bytesRead = 0;

        boolean foundRootEnd = false;
        boolean readZeroBytes = false;

        int error = CMDError.CMD_RESULT_OK;

        try {
            while ((mReadStream.hasBytesAvailable())
                    && (!readZeroBytes)) {
                int numberOfBytesToRead = BUFFER_SIZE;

                if (numberOfBytesToRead > mBytesLeftToRead)
                    numberOfBytesToRead = (int) mBytesLeftToRead;

                bytesRead = mReadStream.read(mReadBuffer, 0, numberOfBytesToRead);

                if (bytesRead == 0)
                    readZeroBytes = true;
                else if (bytesRead > 0) {
                    startOrResetTimeoutTimer();
                }

                mBytesLeftToRead -= bytesRead;
                mOutputStreamWrapper.write(mReadBuffer, 0, bytesRead);
            }
        } catch (Exception ex) {
            DLog.log(ex);
            // Continue reading data (there's no way to stop it), but flag an error so we can report it at the end
            error = CMDError.CMD_ERROR_DOWNLOADING_FILE;
        }

        if (mBytesLeftToRead <= 0) {
            try {
                mOutputStreamWrapper.close();
            } catch (Exception ex) {
                DLog.log(ex);
                error = CMDError.CMD_ERROR_DOWNLOADING_FILE; // We need to flag this as a failure, the error could have come when we flush the file
            }

            mState = EMConnectionState.EM_CONNECTION_STATE_CONNECTED;
            DLog.log("EM_CONNECTION_STATE_CONNECTED");

            if (error == CMDError.CMD_RESULT_OK)
                mDelegate.gotFile(mTempFileName);
            else
                mDelegate.gotFile(null); // Signal that there was a problem downloading the file
        }

        DLog.log("<< doGetRawDataAsFile");
    }

    boolean isConnected() {
        // TODO:
        return true;
    }

/////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////
//From EMConnectionDelegate

    public void handleEvent(int aStreamEvent) {
        DLog.log(">> handleEvent");

        if (mTimedOut) {
            DLog.log("Already timed out - ignore event");
            return; // Don't handle any events if we've timeout out already (otherwise the delegate could get multiple callbacks)
        }
        try {

            if (mState == EMConnectionState.EM_CONNECTION_STATE_DISCONNECTED) {
                // Ignore any stream events if we've already disconnected
                DLog.log("Disconnected - ignore event");
                return;
            }

            if ((aStreamEvent == EventHasSpaceAvailable)
                    || (aStreamEvent == EventOpenCompleted)
                    || (aStreamEvent == EventHasBytesAvailable)
                    || (aStreamEvent == EventFileDone)) {
                if (mRemoteIpAddress == null) {
                    // Don't use any of these, we can't connect back to the provided address
                    // mRemoteIpAddress = mSocket.getInetAddress();
                    // mRemoteIpAddress = ((InetSocketAddress) mSocket.getRemoteSocketAddress()).getAddress();
                }

                DLog.log("Space available, open completed or bytes available");
                if ((mState == EMConnectionState.EM_CONNECTION_STATE_SENDING_TEXT)
                        || ((mState == EMConnectionState.EM_CONNECTION_STATE_SENDING_NOOP))) {
                    DLog.log("EM_CONNECTION_STATE_SENDING_TEXT");

                    // Continue sending data from the buffer
                    writeTextBuffer();
                } else if (mState == EMConnectionState.EM_CONNECTION_STATE_SENDING_FILE) {
                    DLog.log("EM_CONNECTION_STATE_SENDING_FILE");
                    //doSendFile();
                    sendFileDone();
                } else if (mState == EMConnectionState.EM_CONNECTION_STATE_GETTING_XML_FILE) {
                    DLog.log("EM_CONNECTION_STATE_GETTING_XML_FILE");
                    doGetXmlAsFile();
                } else if (mState == EMConnectionState.EM_CONNECTION_STATE_GETTING_RAW_FILE) {
                    DLog.log("EM_CONNECTION_STATE_GETTING_RAW_FILE");
                    doGetRawDataAsFile();
                } else if (mState == EMConnectionState.EM_CONNECTION_STATE_GETTING_TEXT) {
                    DLog.log("EM_CONNECTION_STATE_GETTING_TEXT");
                    doGetText();
                } else {
                    DLog.log("Unexpected state: " + mState + " - ignore event. This could be cancelled.");
                    doGetText();
                }
            } else if (aStreamEvent == EventErrorOccurred) {
                mDelegate.error(CMDError.CMD_WIFI_MAIN_CONNECTION_ERROR);
            }
        } catch (Exception ex) {
            DLog.log(ex);
            if (!mReportedConnectionError) {
                if (!mHandshakeConnection) {
                    mReportedConnectionError = true;
                    // TODO: handle exception
                    // [EMExceptionHandler handleException:exception];
                }
            }
        }

        DLog.log("<< handleEvent");
    }

    int writeBuffer() {
        DLog.log(">> writeBuffer");

        DLog.log(String.format("mBytesLeftToWrite: %d", mBytesLeftToWrite));

        boolean noBytes = false;
        if (mBytesLeftToWrite <= 0) {
            DLog.log("*** No bytes to write");
            noBytes = true;
        }

        int bytesWritten = 0;
        if ((mWriteStream.hasSpaceAvailable()) && (!noBytes)) {
            DLog.log("Has space available...");
            bytesWritten = mWriteStream.write(mWriteBuffer, mWriteBufferPosition, mBytesLeftToWrite);

            if (bytesWritten > 0) {
                startOrResetTimeoutTimer();
            }

            // DLog.log(String.format("Bytes written: %d", bytesWritten));
	        /* if (bytesWritten < 0)
	        {
	        	
	            // EM_EXCEPTION_RAISE(EM_EXCEPTION_CONNECTION_ERROR);
	        } // Errors are handled differently for Android
	        else */
            {
                mBytesLeftToWrite -= bytesWritten;
                mWriteBufferPosition += bytesWritten;
                mFileBytesSent += bytesWritten;

                if (mFileProgressDelegate != null)
                    mFileProgressDelegate.fileSendingProgressUpdate(bytesWritten);
            }
        } else {
            DLog.log("No space available...");
        }


        // DLog.log(String.format("Bytes written %d", bytesWritten));

        DLog.log("<< writeBuffer");

        return bytesWritten;
    }

    private void writeTextBuffer() {
        DLog.log(">> writeTextBuffer");

        // Don't set this here - we might be sending a NOOP which has a different state
//	    mState = EMConnectionState.EM_CONNECTION_STATE_SENDING_TEXT;
//	    DLog.log("EM_CONNECTION_STATE_SENDING_TEXT");

        writeBuffer();

        if (mBytesLeftToWrite <= 0) {
            // All of the data has been written, so notify the client and go back to the connected state
            if (mDelegate != null) {
                if (mState == EMConnectionState.EM_CONNECTION_STATE_SENDING_NOOP) {
                    mState = EMConnectionState.EM_CONNECTION_STATE_CONNECTED;
                    DLog.log("EM_CONNECTION_STATE_CONNECTED");
                    // This was a NOOP, so don't complete the delegate, instead see if we have any queued commands to send
                    if (mQueuedTextToSend != null) {
                        String textToSend = mQueuedTextToSend;
                        mQueuedTextToSend = null;
                        sendText(textToSend);
                    }
                } else {
                    mState = EMConnectionState.EM_CONNECTION_STATE_CONNECTED;
                    DLog.log("EM_CONNECTION_STATE_CONNECTED");
                    mDelegate.sent();
                }
            }
        }

        DLog.log("<< writeTextBuffer");
    }

    public InetAddress mRemoteIpAddress;
    public InetAddress mHostName;

    private boolean mIsHandshakeConnection = false;
    int mPort;

    private String mCurrentTextString;

    private EMAsyncStream mReadStream;
    private boolean mReadStreamOpen;

    private EMAsyncStream mWriteStream;
    private boolean mWriteStreamOpen;

    private String mTempFileName;

    private InputStream mFileReadStream;
    private OutputStream mFileOutputStream;

    Socket mSocket;

    private byte mWriteBuffer[]; // + 1 for any null that we might want to add
    private byte mReadBuffer[]; // + 1 for any null we might want to add
    private byte mRootEndCharArray[]; // = "</root>";
    private byte mAltRootEndCharArray[]; // = "<root/>";
    private int mWriteBufferPosition;
    private int mReadBufferPosition;
    private int mBytesLeftToWrite;
    private boolean mDeleteFileWhenDone;
    private boolean mReportedConnectionError;

    private EMConnectionState mState;

    private EMConnectionDelegate mDelegate;

    private int mRootEndCounter;
    private int mAltRootEndCounter;

    private boolean mEndOfFile;

    EMFileSendingProgressDelegate mFileProgressDelegate;

    private long mFileBytesTotal;
    private long mFileBytesSent;

    private long mBytesLeftToRead; // The number of bytes left to read for a raw-data-read

    boolean mHandshakeConnection;

    FileOutputStream mRawDataFileOutputStream;

    private OutputStream mOutputStreamWrapper; // Either the raw file output stream, or a cipher stream wrapper around the raw output stream (used to decrypt data as we're writing it to a file)

    private String mQueuedTextToSend; // Queued text, used in cases where we are in the middle of sending a NOOP

    private boolean mBindToWiFiLan;
}
