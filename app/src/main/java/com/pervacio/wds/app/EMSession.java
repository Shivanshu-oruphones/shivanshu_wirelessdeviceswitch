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
import android.os.Handler;
import android.os.Message;

import com.pervacio.wds.custom.models.MigrationStats;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.DeviceInfo;
import com.pervacio.wds.sdk.CMDError;

import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class EMSession implements EMConnectionDelegate, EMCommandDelegate, EMHandshakeDelegate, EMYouAreSourceOrTargetDelegate,
        EMDataCommandDelegate, EMAddCryptoCommandDelegate, EMDataCompleteDelegate {


    private int CHANNEL_COUNT = 3;
    private int totalChannels = 3;

    enum EMSessionState {
        EM_SESSION_NOT_INITIALIZED,
        EM_SESSION_WAITING_FOR_COMMAND,
        EM_SESSION_IN_COMMAND,
        EM_SESSION_HAS_QUIT
    }

    ;

    enum EMSessionType {
        EM_COMMAND_INITIATOR,
        EM_COMMAND_RESPONDER
    }

    ;

    enum EMSessionRole {
        EM_SESSION_SOURCE_ROLE,
        EM_SESSION_TARGET_ROLE,
        EM_SESSION_UNKNOWN_ROLE
    }

    ;

    // Initialize with a netService - if the net service is not yet resolved then the only valid method to call is handshakeWithServer
    // -(EMSession*) initAsClientWithNetService:(NSNetService*)netService;

    void clean() {
        mDataTypesToSend = 0; // The data types we need to send (EMDataType or'ed together)
        mState = EMSessionState.EM_SESSION_NOT_INITIALIZED;
        mConnection = null;
        mDelegate = null;
        mPin = null;
        mSessionRole = EMSessionRole.EM_SESSION_UNKNOWN_ROLE;
        mCurrentDataType = EMDataType.EM_DATA_TYPE_NOT_SET;
        mSendingData = false;
    }

    public EMSession(InetAddress aHostName, int aPort, EMSessionDelegate aDelegate, Context aContext, EMPreviouslyTransferredContentRegistry aPreviouslyTransferredContentRegistry, boolean aBindToWiFiLan)
//	-(EMSession*) initAsClientWithHostNameAndPort:(NSString*)hostName port:(int)portNumber;
    {
        mContext = aContext;
        clean();
        setDelegate(aDelegate);
        mHostName = aHostName;
        mPort = aPort;
        mPreviouslyTransferredContentRegistry = aPreviouslyTransferredContentRegistry;
        mBindToWiFiLan = aBindToWiFiLan;
        createConnection();
    }

    private void createConnection() {
        mConnection = new EMConnection(mHostName, mPort, mBindToWiFiLan);
        mConnection.setDelegate(this);
    }

    public void reCreateConnection() {
        mConnection.close();
        mConnection = new EMConnection(mHostName, mPort, mBindToWiFiLan);
        mConnection.setDelegate(this);
    }

    private boolean mBindToWiFiLan = true;

    // Set the session up as a server and start listening for commands from the client
    // Note that we may end up as the migration source or the target, we don't know yet
    // -(EMSession*) initAsServerWithSocket:(CFSocketNativeHandle)socket;
    EMSession(EMConnection aIncomingConnection, EMSessionDelegate aDelegate, Context aContext) {
        mContext = aContext;
        clean();
        setDelegate(aDelegate);
        mConnection = aIncomingConnection;
        mConnection.setDelegate(this);
        handleIncomingCommands();
    }

    void handleIncomingCommands() {
        // Listen for and handle any incoming commands
        mState = EMSessionState.EM_SESSION_WAITING_FOR_COMMAND;
        mCurrentCommandHandler = null;
        mConnection.getText();
    }

    // Close this session
    void close() {
        // TODO:
    }

    private boolean mMainSession = false;
    private EMServer mFileServer = null;

    private void initFileReceiver(Socket aSocket) {
        EMDataTransferHelper fileTransferUtility = new EMDataTransferHelper(aSocket);
        fileTransferUtility.setDataCommandDelegate(this); //  addded
        fileTransferUtility.setCommandDelegate(this);
        fileTransferUtility.setDataCompleteDelegate(this);
        fileTransferUtility.init();
        fileTransferUtility.startReceivingFiles();
        //TODO - clear the object once done
    }

    public void startFileServer() {
        if (mFileServer != null) {
            DLog.log("EMSession:: stop file receiver");
            mFileServer.stop();
        }
        DLog.log("EMSession::init file receiver");
        mFileServer = new EMServer(EMServer.CONTENT_TRANSFER_PORT);
        mFileServer.setDelegate(new EMServerDelegate() {
            @Override
            public void clientConnected(EMConnection aIncomingConnection) {
                DLog.log("EMSession::start receiving files");
                initFileReceiver(aIncomingConnection.mSocket);
            }

            @Override
            public void serverError(int aError) {
                //
            }
        });
        mFileServer.start();
    }

    private void setMainSession() {
        mMainSession = true;
        mConnection.setMainConnection();
    }

    // Send a handshake message to the server and handle the response
    // This may result in a notification of a new device for the delegate
    public void handshakeWithServer() {
        mConnection.setHandshakeConnection();

        // Start the handshake command initiator
        mHandshakeInitiator = new EMHandshakeCommandInitiator();
        mHandshakeInitiator.setDeviceInfo(mDelegate.getDeviceInfo(true));
        mHandshakeInitiator.setHandshakeDelegate(this);
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mHandshakeInitiator;
        mHandshakeInitiator.start(this);
    }

    private void setDelegate(EMSessionDelegate aDelegate) {
        mDelegate = aDelegate;
        mCommandResponders = new ArrayList<EMCommandHandler>();

        // Add handshake responder to the list of command responders
        EMHandshakeCommandResponder handshakeCommandResponder = new EMHandshakeCommandResponder();
        handshakeCommandResponder.setHandshakeDelegate(this);
        mCommandResponders.add(handshakeCommandResponder);
        handshakeCommandResponder.setDeviceInfo(mDelegate.getDeviceInfo(false));

        // Add you-are-target responder to the list of command responders
        EMYouAreTargetCommandResponder youAreTargetCommandResponder = new EMYouAreTargetCommandResponder();
        youAreTargetCommandResponder.setYouAreTargetDelegate(this);
        mCommandResponders.add(youAreTargetCommandResponder);

        // Add you-are-source responder to the list of command responders
        EMYouAreSourceCommandResponder youAreSourceCommandResponder = new EMYouAreSourceCommandResponder();
        youAreSourceCommandResponder.setYouAreSourceDelegate(this);
        mCommandResponders.add(youAreSourceCommandResponder);

        // Add the command handler for contacts, calendar, etc..
        EMAddDataCommandResponder addDataResponder = new EMAddDataCommandResponder(this, mContext);
        mCommandResponders.add(addDataResponder);

        // Add the quit responder to the list of command responders
        EMQuitCommandResponder quitResponder = new EMQuitCommandResponder();
        quitResponder.setDataCommandDelegate(this);
        mCommandResponders.add(quitResponder);

        EMAddFileCommandResponder addFileResponder = new EMAddFileCommandResponder(mContext);
        addFileResponder.setDataCommandDelegate(this);
        mCommandResponders.add(addFileResponder);

        mAddCryptoCommandResponder = new EMAddCryptoSrpCommandResponder(this);
//		mAddCryptoCommandResponder.initialize(EMGlobals.getJavascriptWebView());
        mCommandResponders.add(mAddCryptoCommandResponder);

        // START – Pervacio

        EMTextCommandResponder mTextCommandResponder = new EMTextCommandResponder();
        mTextCommandResponder.setDataCommandDelegate(this);
        mCommandResponders.add(mTextCommandResponder);

        // END – Pervacio
    }

    // Send the selected data to the other device
    // Sends the commands, generates + sends the data payload
    // Asynchronous operation - Notifies the delegate of progress
    void sendData(int aDataTypes) {
        mDataTypesToSend = aDataTypes;
        mCurrentDataType = EMDataType.EM_DATA_TYPE_NOT_SET;
        mSendingData = true;
        CommonUtil.getInstance().setDatatypesTobeTransferred(aDataTypes);
        if (mHostName == null) {
            mHostName = CommonUtil.getInstance().getRemoteDeviceIpAddress();
        }
        modifyChannelCount();

        if (EMConfig.NEW_DATA_TRANSFER_PROTOCOL) {
            doSendAllData();
        } else {
            mConnection.enableTimeoutTimer();
            doSendNextData();
        }
    }

    public void sendDataForEstimation(EMCommandDelegate emCommandDelegate) {
        DLog.log("EMSession:: Estimation START");
        if (mHostName == null) {
            mHostName = CommonUtil.getInstance().getRemoteDeviceIpAddress();
        }
        EMDataTransferHelper estimationTimeHelper = new EMDataTransferHelper(mHostName);
        estimationTimeHelper.setCommandDelegate(emCommandDelegate);
        estimationTimeHelper.setDataCommandDelegate(EMSession.this);
        estimationTimeHelper.init();
        estimationTimeHelper.startEstimation();
    }

    private void modifyChannelCount() {
        long transferSpeed = CommonUtil.getInstance().getTransferSpeed();
        transferSpeed = transferSpeed / (1024 * 1024);
        boolean remoteDeviceAndroid = false;
        try {
            remoteDeviceAndroid = DashboardLog.getInstance().destinationEMDeviceInfo.dbDevicePlatform.equalsIgnoreCase(Constants.PLATFORM_ANDROID);
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
        if (remoteDeviceAndroid) {
            if (transferSpeed >= 25) {
                CHANNEL_COUNT = 10;
            } else if (transferSpeed > 10) {
                CHANNEL_COUNT = 5;
            } else {
                CHANNEL_COUNT = 5;
            }
        } else {
            CHANNEL_COUNT = 5;
        }
        int totalFiles = DeviceInfo.getInstance().getFilesQueue().size();
        if (totalFiles < CHANNEL_COUNT) {
            CHANNEL_COUNT = totalFiles;
        }
        totalChannels = CHANNEL_COUNT;
        DLog.log("Starting Migration. Transfer speed: " + transferSpeed + ", Channel count: " + CHANNEL_COUNT);
    }


    private synchronized void commandCompleted(int dataType, boolean status) {
        long mDataTypesTobeTransferred = CommonUtil.getInstance().getDatatypesTobeTransferred();
        DLog.log("Command Completed " + dataType + " " + status + " Pending " + mDataTypesTobeTransferred);
        if (dataType == EMDataType.EM_DATA_TYPE_MEDIA) {
            if ((mDataTypesTobeTransferred & EMDataType.EM_DATA_TYPE_PHOTOS) != 0) {
                mDataTypesTobeTransferred = mDataTypesTobeTransferred ^ EMDataType.EM_DATA_TYPE_PHOTOS;
                DLog.log("EM_DATA_TYPE_PHOTOS: " + mDataTypesTobeTransferred);
            }

            if ((mDataTypesTobeTransferred & EMDataType.EM_DATA_TYPE_VIDEO) != 0) {
                mDataTypesTobeTransferred = mDataTypesTobeTransferred ^ EMDataType.EM_DATA_TYPE_VIDEO;
                DLog.log("EM_DATA_TYPE_VIDEO: " + mDataTypesTobeTransferred);
            }

            if ((mDataTypesTobeTransferred & EMDataType.EM_DATA_TYPE_MUSIC) != 0) {
                mDataTypesTobeTransferred = mDataTypesTobeTransferred ^ EMDataType.EM_DATA_TYPE_MUSIC;
                DLog.log("EM_DATA_TYPE_MUSIC: " + mDataTypesTobeTransferred);
            }
        } else {
            if ((mDataTypesTobeTransferred & dataType) != 0) {
                mDataTypesTobeTransferred = mDataTypesTobeTransferred ^ dataType;
                DLog.log("EM_DATA_TYPE "+ dataType+": " + mDataTypesTobeTransferred);
            }
        }
        CommonUtil.getInstance().setDatatypesTobeTransferred(mDataTypesTobeTransferred);
        DLog.log("Command Completed, Pending " + mDataTypesTobeTransferred);
        MigrationStats.getInstance().setDataTypesTobeCompleted((int) mDataTypesTobeTransferred);
        if (mDataTypesTobeTransferred == 0 || !status) {
            // TODO - handle overall status
            DLog.log("MIGRATION Completed- SENDING QUIT");
            EMProgressInfo progressInfo = new EMProgressInfo();
            progressInfo.mDataType = EMDataType.EM_DATA_TYPE_NO_MORE_DATA;
            mSendingData = false;
            progressInfo.mFailed = !status;
            if (mDataTypesTobeTransferred == 0) {
                progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TEXT_COMMAND_SENT;
                progressInfo.mTextCommand = EMStringConsts.EM_COMMAND_TEXT_QUIT;
            }
            mDelegate.progressUpdate(progressInfo);
            // We're done now - send the quit command
            sendQuit();
            //sending quit from EasyMigrateActivity from main session.
        }
    }


    class ParallelDataManager {
        int mDataTypesToSend = 0;
        int mDataTypesTransferStarted = 0;

        public ParallelDataManager(int mDataTypesToSend) {
            this.mDataTypesToSend = mDataTypesToSend;
        }


        void doSendNextData() {
            // Move to the next selected data type
            boolean sendThisData = false;
            while ((mCurrentDataType != EMDataType.EM_DATA_TYPE_NO_MORE_DATA) && (!sendThisData)) {
                mCurrentDataType = mCurrentDataType << 1;
                if ((mCurrentDataType & mDataTypesToSend) != 0)
                    sendThisData = true;
            }
            if (mCurrentDataType == EMDataType.EM_DATA_TYPE_NO_MORE_DATA) {
                // Notify the UI that all data has been sent
                EMProgressInfo progressInfo = new EMProgressInfo();
                progressInfo.mDataType = EMDataType.EM_DATA_TYPE_NO_MORE_DATA;
                mSendingData = false;
                mDelegate.progressUpdate(progressInfo);
                mConnection.disableTimeoutTimer();
                // We're done now - send the quit command
                sendQuit();
            } else {
                doSendData();
            }
        }

        void doSendData() {
            switch (mCurrentDataType) {
                case EMDataType.EM_DATA_TYPE_CONTACTS:
                    sendContacts();
                    break;
                case EMDataType.EM_DATA_TYPE_CALENDAR:
                    sendCalendar();
                    break;
                case EMDataType.EM_DATA_TYPE_SMS_MESSAGES:
                    sendSms();
                    break;
                case EMDataType.EM_DATA_TYPE_ACCOUNTS:
                    sendAccounts();
                    break;
                case EMDataType.EM_DATA_TYPE_PHOTOS:
                    sendPhotos();
                    break;
                case EMDataType.EM_DATA_TYPE_VIDEO:
//					sendVideo();
                    sendVideos();
                    break;
                case EMDataType.EM_DATA_TYPE_MUSIC:
                    sendMusic();
                    break;
                case EMDataType.EM_DATA_TYPE_DOCUMENTS:
                    sendDocuments();
                    break;
                case EMDataType.EM_DATA_TYPE_APP:
                    sendApps();
                    break;
                case EMDataType.EM_DATA_TYPE_CALL_LOGS:
                    sendCallLogs();
                default:
                    // Ignore others;
                    break;
            }
        }


        void sendVideos() {
            if ((mDataTypesToSend & EMDataType.EM_DATA_TYPE_VIDEO) != 0) {
                mDataTypesTransferStarted |= EMDataType.EM_DATA_TYPE_VIDEO;
                DLog.log("EMSession::sendVideos START");
                mVideoSender = new EMMediaSender(EMSession.this, EMDataType.EM_DATA_TYPE_VIDEO);
                mState = EMSessionState.EM_SESSION_IN_COMMAND;
                mCurrentCommandHandler = mVideoSender;
                mVideoSender.mHostName = mHostName;
                mVideoSender.start(new EMCommandDelegate() {
                    @Override
                    public void sendText(String text) {
                    }

                    @Override
                    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {
                    }

                    @Override
                    public void getText() {

                    }

                    @Override
                    public void getXmlAsFile() {
                    }

                    @Override
                    public void getRawDataAsFile(long aLength, String aTargetFilePath) {
                    }

                    @Override
                    public void commandComplete(boolean aSuccess) {
                        DLog.log("command complete Videos " + aSuccess);
                        commandCompleted(EMDataType.EM_DATA_TYPE_VIDEO, aSuccess);
                    }

                    @Override
                    public void setSharedObject(String aKey, Object aObject) {
                    }

                    @Override
                    public Object getSharedObject(String aKey) {
                        return null;
                    }

                    @Override
                    public void startNoopTimer() {
                    }

                    @Override
                    public void stopNoopTimer() {
                    }

                    @Override
                    public void disableTimeoutTimer() {
                    }

                    @Override
                    public void enableTimeoutTimer() {
                    }

                    @Override
                    public void addToPreviouslyTransferredItems(String aItem) {
                    }

                    @Override
                    public boolean itemHasBeenPreviouslyTransferred(String aItem) {
                        return false;
                    }
                });
            }
        }

        void sendPhotos() {
            if ((mDataTypesToSend & EMDataType.EM_DATA_TYPE_PHOTOS) != 0) {
                mDataTypesTransferStarted |= EMDataType.EM_DATA_TYPE_PHOTOS;
                DLog.log("EMSession::sendPhotos START");
                mPhotoSender = new EMMediaSender(EMSession.this, EMDataType.EM_DATA_TYPE_PHOTOS);
                mState = EMSessionState.EM_SESSION_IN_COMMAND;
                mCurrentCommandHandler = mPhotoSender;
                mPhotoSender.mHostName = mHostName;
                mPhotoSender.start(new EMCommandDelegate() {
                    @Override
                    public void sendText(String text) {
                    }

                    @Override
                    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {
                    }

                    @Override
                    public void getText() {

                    }

                    @Override
                    public void getXmlAsFile() {
                    }

                    @Override
                    public void getRawDataAsFile(long aLength, String aTargetFilePath) {
                    }

                    @Override
                    public void commandComplete(boolean aSuccess) {
                        DLog.log("command complete Photos " + aSuccess);
                        commandCompleted(EMDataType.EM_DATA_TYPE_PHOTOS, aSuccess);
                    }

                    @Override
                    public void setSharedObject(String aKey, Object aObject) {
                    }

                    @Override
                    public Object getSharedObject(String aKey) {
                        return null;
                    }

                    @Override
                    public void startNoopTimer() {
                    }

                    @Override
                    public void stopNoopTimer() {
                    }

                    @Override
                    public void disableTimeoutTimer() {
                    }

                    @Override
                    public void enableTimeoutTimer() {
                    }

                    @Override
                    public void addToPreviouslyTransferredItems(String aItem) {
                    }

                    @Override
                    public boolean itemHasBeenPreviouslyTransferred(String aItem) {
                        return false;
                    }
                });
            }
        }

        void sendMusic() {
            if ((mDataTypesToSend & EMDataType.EM_DATA_TYPE_MUSIC) != 0) {
                mDataTypesTransferStarted |= EMDataType.EM_DATA_TYPE_MUSIC;
                DLog.log("EMSession::send Audio START");
                mMusicSender = new EMMediaSender(EMSession.this, EMDataType.EM_DATA_TYPE_MUSIC);
                mState = EMSessionState.EM_SESSION_IN_COMMAND;
                mCurrentCommandHandler = mMusicSender;
                mMusicSender.mHostName = mHostName;
                mMusicSender.start(new EMCommandDelegate() {
                    @Override
                    public void sendText(String text) {
                    }

                    @Override
                    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {
                    }

                    @Override
                    public void getText() {

                    }

                    @Override
                    public void getXmlAsFile() {
                    }

                    @Override
                    public void getRawDataAsFile(long aLength, String aTargetFilePath) {
                    }

                    @Override
                    public void commandComplete(boolean aSuccess) {
                        DLog.log("command complete Audio " + aSuccess);
                        commandCompleted(EMDataType.EM_DATA_TYPE_MUSIC, aSuccess);
                    }

                    @Override
                    public void setSharedObject(String aKey, Object aObject) {
                    }

                    @Override
                    public Object getSharedObject(String aKey) {
                        return null;
                    }

                    @Override
                    public void startNoopTimer() {
                    }

                    @Override
                    public void stopNoopTimer() {
                    }

                    @Override
                    public void disableTimeoutTimer() {
                    }

                    @Override
                    public void enableTimeoutTimer() {
                    }

                    @Override
                    public void addToPreviouslyTransferredItems(String aItem) {
                    }

                    @Override
                    public boolean itemHasBeenPreviouslyTransferred(String aItem) {
                        return false;
                    }
                });
            }
        }

        void sendDocuments() {
            if ((mDataTypesToSend & EMDataType.EM_DATA_TYPE_DOCUMENTS) != 0) {
                mDataTypesTransferStarted |= EMDataType.EM_DATA_TYPE_DOCUMENTS;
                DLog.log("EMSession::send Documents START");
                EMMediaSenderNew documentSender = new EMMediaSenderNew(EMSession.this, EMDataType.EM_DATA_TYPE_DOCUMENTS);
                mState = EMSessionState.EM_SESSION_IN_COMMAND;
                documentSender.mHostName = mHostName;
                documentSender.start(new EMCommandDelegate() {
                    @Override
                    public void sendText(String text) {
                    }

                    @Override
                    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {
                    }

                    @Override
                    public void getText() {

                    }

                    @Override
                    public void getXmlAsFile() {
                    }

                    @Override
                    public void getRawDataAsFile(long aLength, String aTargetFilePath) {
                    }

                    @Override
                    public void commandComplete(boolean aSuccess) {
                        DLog.log("command complete Documents " + aSuccess);
                        commandCompleted(EMDataType.EM_DATA_TYPE_DOCUMENTS, aSuccess);
                    }

                    @Override
                    public void setSharedObject(String aKey, Object aObject) {
                    }

                    @Override
                    public Object getSharedObject(String aKey) {
                        return null;
                    }

                    @Override
                    public void startNoopTimer() {
                    }

                    @Override
                    public void stopNoopTimer() {
                    }

                    @Override
                    public void disableTimeoutTimer() {
                    }

                    @Override
                    public void enableTimeoutTimer() {
                    }

                    @Override
                    public void addToPreviouslyTransferredItems(String aItem) {
                    }

                    @Override
                    public boolean itemHasBeenPreviouslyTransferred(String aItem) {
                        return false;
                    }
                });
            }
        }

        void sendCalendar() {
            if ((mDataTypesToSend & EMDataType.EM_DATA_TYPE_CALENDAR) != 0) {
                mDataTypesTransferStarted |= EMDataType.EM_DATA_TYPE_CALENDAR;
                DLog.log("EMSession::sendCalendar START");
                mCalendarTransferHelper = new EMDataTransferHelper(mHostName);
                mCalendarTransferHelper.setCommandDelegate(new EMCommandDelegate() {
                    @Override
                    public void sendText(String text) {

                    }

                    @Override
                    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {

                    }

                    @Override
                    public void getText() {

                    }

                    @Override
                    public void getXmlAsFile() {

                    }

                    @Override
                    public void getRawDataAsFile(long aLength, String aTargetFilePath) {

                    }

                    @Override
                    public void commandComplete(boolean aSuccess) {
                        DLog.log("command complete CALENDAR " + aSuccess);
                        mCalendarTransferHelper.clean();
                        if (!aSuccess) {
                            DLog.log("CALENDAR failed");
                            commandCompleted(EMDataType.EM_DATA_TYPE_CALENDAR, true);
                        }
                    }

                    @Override
                    public void setSharedObject(String aKey, Object aObject) {

                    }

                    @Override
                    public Object getSharedObject(String aKey) {
                        return null;
                    }

                    @Override
                    public void startNoopTimer() {

                    }

                    @Override
                    public void stopNoopTimer() {

                    }

                    @Override
                    public void disableTimeoutTimer() {

                    }

                    @Override
                    public void enableTimeoutTimer() {

                    }

                    @Override
                    public void addToPreviouslyTransferredItems(String aItem) {

                    }

                    @Override
                    public boolean itemHasBeenPreviouslyTransferred(String aItem) {
                        return false;
                    }
                });
                mCalendarTransferHelper.setDataCommandDelegate(EMSession.this);
                mCalendarTransferHelper.init();
                mCalendarTransferHelper.sendPIM(EMStringConsts.DATA_TYPE_CALENDAR);
            }
        }

        void sendMessages() {
            if ((mDataTypesToSend & EMDataType.EM_DATA_TYPE_SMS_MESSAGES) != 0) {
                mDataTypesTransferStarted |= EMDataType.EM_DATA_TYPE_SMS_MESSAGES;
                DLog.log("EMSession::sendMessages START");
                mMessagesTransferHelper = new EMDataTransferHelper(mHostName);
                mMessagesTransferHelper.setCommandDelegate(new EMCommandDelegate() {
                    @Override
                    public void sendText(String text) {

                    }

                    @Override
                    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {

                    }

                    @Override
                    public void getText() {

                    }

                    @Override
                    public void getXmlAsFile() {

                    }

                    @Override
                    public void getRawDataAsFile(long aLength, String aTargetFilePath) {

                    }

                    @Override
                    public void commandComplete(boolean aSuccess) {
                        DLog.log("command complete MESSAGES " + aSuccess);
                        mMessagesTransferHelper.clean();
                        if (!aSuccess) {
                            DLog.log("MESSAGES failed");
                            commandCompleted(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, true);
                        }
                    }

                    @Override
                    public void setSharedObject(String aKey, Object aObject) {

                    }

                    @Override
                    public Object getSharedObject(String aKey) {
                        return null;
                    }

                    @Override
                    public void startNoopTimer() {

                    }

                    @Override
                    public void stopNoopTimer() {

                    }

                    @Override
                    public void disableTimeoutTimer() {

                    }

                    @Override
                    public void enableTimeoutTimer() {

                    }

                    @Override
                    public void addToPreviouslyTransferredItems(String aItem) {

                    }

                    @Override
                    public boolean itemHasBeenPreviouslyTransferred(String aItem) {
                        return false;
                    }
                });
                mMessagesTransferHelper.setDataCommandDelegate(EMSession.this);
                mMessagesTransferHelper.init();
                mMessagesTransferHelper.sendPIM(EMStringConsts.DATA_TYPE_SMS_MESSAGES);
            }
        }

        void sendContacts() {
            if ((mDataTypesToSend & EMDataType.EM_DATA_TYPE_CONTACTS) != 0) {
                mDataTypesTransferStarted |= EMDataType.EM_DATA_TYPE_CONTACTS;
                DLog.log("EMSession::sendContacts START");
                mContactsTransferHelper = new EMDataTransferHelper(mHostName);
                mContactsTransferHelper.setCommandDelegate(new EMCommandDelegate() {
                    @Override
                    public void sendText(String text) {

                    }

                    @Override
                    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {

                    }

                    @Override
                    public void getText() {

                    }

                    @Override
                    public void getXmlAsFile() {

                    }

                    @Override
                    public void getRawDataAsFile(long aLength, String aTargetFilePath) {

                    }

                    @Override
                    public void commandComplete(boolean aSuccess) {
                        DLog.log("command complete CONTACTS " + aSuccess);
                        mContactsTransferHelper.clean();
                        if (!aSuccess) {
                            DLog.log("CONTACTS failed");
                            commandCompleted(EMDataType.EM_DATA_TYPE_CONTACTS, true);
                        }
                    }

                    @Override
                    public void setSharedObject(String aKey, Object aObject) {

                    }

                    @Override
                    public Object getSharedObject(String aKey) {
                        return null;
                    }

                    @Override
                    public void startNoopTimer() {

                    }

                    @Override
                    public void stopNoopTimer() {

                    }

                    @Override
                    public void disableTimeoutTimer() {

                    }

                    @Override
                    public void enableTimeoutTimer() {

                    }

                    @Override
                    public void addToPreviouslyTransferredItems(String aItem) {

                    }

                    @Override
                    public boolean itemHasBeenPreviouslyTransferred(String aItem) {
                        return false;
                    }
                });
                mContactsTransferHelper.setDataCommandDelegate(EMSession.this);
                mContactsTransferHelper.init();
                mContactsTransferHelper.sendPIM(EMStringConsts.DATA_TYPE_CONTACTS);
            }
        }

        private void sendCallLogs() {
            if ((mDataTypesToSend & EMDataType.EM_DATA_TYPE_CALL_LOGS) != 0) {
                mDataTypesTransferStarted |= EMDataType.EM_DATA_TYPE_CALL_LOGS;
                DLog.log("EMSession::sendCallLogs START");
                mCallLogsTransferHelper = new EMDataTransferHelper(mHostName);
                mCallLogsTransferHelper.setCommandDelegate(new EMCommandDelegate() {
                    @Override
                    public void sendText(String text) {

                    }

                    @Override
                    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {

                    }

                    @Override
                    public void getText() {

                    }

                    @Override
                    public void getXmlAsFile() {

                    }

                    @Override
                    public void getRawDataAsFile(long aLength, String aTargetFilePath) {

                    }

                    @Override
                    public void commandComplete(boolean aSuccess) {
                        DLog.log("command complete CALL LOGS " + aSuccess);
                        mCallLogsTransferHelper.clean();
                        if (!aSuccess) {
                            DLog.log("CALL LOGS failed");
                            commandCompleted(EMDataType.EM_DATA_TYPE_CALL_LOGS, true);
                        }
                    }

                    @Override
                    public void setSharedObject(String aKey, Object aObject) {

                    }

                    @Override
                    public Object getSharedObject(String aKey) {
                        return null;
                    }

                    @Override
                    public void startNoopTimer() {

                    }

                    @Override
                    public void stopNoopTimer() {

                    }

                    @Override
                    public void disableTimeoutTimer() {

                    }

                    @Override
                    public void enableTimeoutTimer() {

                    }

                    @Override
                    public void addToPreviouslyTransferredItems(String aItem) {

                    }

                    @Override
                    public boolean itemHasBeenPreviouslyTransferred(String aItem) {
                        return false;
                    }
                });

                mCallLogsTransferHelper.setDataCommandDelegate(EMSession.this);
                mCallLogsTransferHelper.init();
                mCallLogsTransferHelper.sendPIM(EMStringConsts.DATA_TYPE_CALL_LOGS);
            }
        }

        private void sendSettings() {
            if ((mDataTypesToSend & EMDataType.EM_DATA_TYPE_SETTINGS) != 0) {
                mDataTypesTransferStarted |= EMDataType.EM_DATA_TYPE_SETTINGS;
                DLog.log("EMSession::sendSettings START");
                mSettingsTransferHelper = new EMDataTransferHelper(mHostName);
                mSettingsTransferHelper.setCommandDelegate(new EMCommandDelegate() {
                    @Override
                    public void sendText(String text) {

                    }

                    @Override
                    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {

                    }

                    @Override
                    public void getText() {

                    }

                    @Override
                    public void getXmlAsFile() {

                    }

                    @Override
                    public void getRawDataAsFile(long aLength, String aTargetFilePath) {

                    }

                    @Override
                    public void commandComplete(boolean aSuccess) {
                        DLog.log("command complete Settings " + aSuccess);
                        mSettingsTransferHelper.clean();
                        if (!aSuccess) {
                            DLog.log("Settings failed");
                            commandCompleted(EMDataType.EM_DATA_TYPE_SETTINGS, true);
                        }
                    }

                    @Override
                    public void setSharedObject(String aKey, Object aObject) {

                    }

                    @Override
                    public Object getSharedObject(String aKey) {
                        return null;
                    }

                    @Override
                    public void startNoopTimer() {

                    }

                    @Override
                    public void stopNoopTimer() {

                    }

                    @Override
                    public void disableTimeoutTimer() {

                    }

                    @Override
                    public void enableTimeoutTimer() {

                    }

                    @Override
                    public void addToPreviouslyTransferredItems(String aItem) {

                    }

                    @Override
                    public boolean itemHasBeenPreviouslyTransferred(String aItem) {
                        return false;
                    }
                });

                mSettingsTransferHelper.setDataCommandDelegate(EMSession.this);
                mSettingsTransferHelper.init();
                mSettingsTransferHelper.sendPIM(EMStringConsts.DATA_TYPE_SETTINGS);
            }
        }

        void sendApps() {
            if ((mDataTypesToSend & EMDataType.EM_DATA_TYPE_APP) != 0) {
                mDataTypesTransferStarted |= EMDataType.EM_DATA_TYPE_APP;
                DLog.log("EMSession::sendApps START");
                mAppSender = new EMAppSender(EMSession.this, EMDataType.EM_DATA_TYPE_APP);
                mState = EMSessionState.EM_SESSION_IN_COMMAND;
                mCurrentCommandHandler = mAppSender;
                mAppSender.mHostName = mHostName;

                mAppSender.start(new EMCommandDelegate() {
                    @Override
                    public void sendText(String text) {
                    }

                    @Override
                    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {
                    }

                    @Override
                    public void getText() {

                    }

                    @Override
                    public void getXmlAsFile() {
                    }

                    @Override
                    public void getRawDataAsFile(long aLength, String aTargetFilePath) {
                    }

                    @Override
                    public void commandComplete(boolean aSuccess) {
                        DLog.log("command complete Apps " + aSuccess);
                        commandCompleted(EMDataType.EM_DATA_TYPE_APP, aSuccess);
                    }

                    @Override
                    public void setSharedObject(String aKey, Object aObject) {
                    }

                    @Override
                    public Object getSharedObject(String aKey) {
                        return null;
                    }

                    @Override
                    public void startNoopTimer() {
                    }

                    @Override
                    public void stopNoopTimer() {
                    }

                    @Override
                    public void disableTimeoutTimer() {
                    }

                    @Override
                    public void enableTimeoutTimer() {
                    }

                    @Override
                    public void addToPreviouslyTransferredItems(String aItem) {
                    }

                    @Override
                    public boolean itemHasBeenPreviouslyTransferred(String aItem) {
                        return false;
                    }
                });
            }
        }

        private void sendMedia() {
            if ((mDataTypesToSend & EMDataType.EM_DATA_TYPE_PHOTOS) != 0)
                mDataTypesTransferStarted |= EMDataType.EM_DATA_TYPE_PHOTOS;
            if ((mDataTypesToSend & EMDataType.EM_DATA_TYPE_VIDEO) != 0)
                mDataTypesTransferStarted |= EMDataType.EM_DATA_TYPE_VIDEO;
            if ((mDataTypesToSend & EMDataType.EM_DATA_TYPE_MUSIC) != 0)
                mDataTypesTransferStarted |= EMDataType.EM_DATA_TYPE_MUSIC;
            for (int i = 0; i < CHANNEL_COUNT; i++) {
                DLog.log("EMSession::sending Media START");
                EMMediaSenderNew mMediaSender = new EMMediaSenderNew(EMSession.this, EMDataType.EM_DATA_TYPE_MEDIA);
                mState = EMSessionState.EM_SESSION_IN_COMMAND;
                mMediaSender.mHostName = mHostName;
                mMediaSender.start(new EMCommandDelegate() {
                    @Override
                    public void sendText(String text) {
                    }

                    @Override
                    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {
                    }

                    @Override
                    public void getText() {

                    }

                    @Override
                    public void getXmlAsFile() {
                    }

                    @Override
                    public void getRawDataAsFile(long aLength, String aTargetFilePath) {
                    }

                    @Override
                    public void commandComplete(boolean aSuccess) {
                        --totalChannels;
                        DLog.log("Media channel ended. Pending "+ totalChannels);
                        if (totalChannels == 0)
                            commandCompleted(EMDataType.EM_DATA_TYPE_MEDIA, aSuccess);
                    }

                    @Override
                    public void setSharedObject(String aKey, Object aObject) {
                    }

                    @Override
                    public Object getSharedObject(String aKey) {
                        return null;
                    }

                    @Override
                    public void startNoopTimer() {
                    }

                    @Override
                    public void stopNoopTimer() {
                    }

                    @Override
                    public void disableTimeoutTimer() {
                    }

                    @Override
                    public void enableTimeoutTimer() {
                    }

                    @Override
                    public void addToPreviouslyTransferredItems(String aItem) {
                        if (mPreviouslyTransferredContentRegistry != null)
                            mPreviouslyTransferredContentRegistry.addToPreviouslyTransferredItem(aItem);
                    }

                    @Override
                    public boolean itemHasBeenPreviouslyTransferred(String aItem) {
                        if (mPreviouslyTransferredContentRegistry != null)
                            return mPreviouslyTransferredContentRegistry.itemHasBeenPreviouslyTransferred(aItem);
                        else
                            return false;
                    }
                });
            }
        }

        void sendAllData() {
            DLog.log("--sendAllData--");
            if (EMConfig.SEQUENCE_TRANSFER_FOR_IOS) {
                doSendNextData();
            } else {
                sendContacts();
                sendCalendar();
                sendMessages();
//				sendMedia();
                sendDocuments();
                sendPhotos();
//				sendVideo();
                sendVideos();
                sendMusic();
                sendApps();
                sendCallLogs();
                sendSettings();
            }
        }
    }


    ParallelDataManager parallelDataManager = null;

    void doSendAllData() {
        parallelDataManager = new ParallelDataManager(mDataTypesToSend);
        parallelDataManager.sendAllData();
    }

    private void sendRestoreCompleted(String dataType) {
        if (mHostName == null) {
            mHostName = CommonUtil.getInstance().getRemoteDeviceIpAddress();
        }
        final EMDataTransferHelper restoreHelper = new EMDataTransferHelper(mHostName);
        restoreHelper.setCommandDelegate(new EMCommandDelegate() {
            @Override
            public void sendText(String text) {

            }

            @Override
            public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {

            }

            @Override
            public void getText() {

            }

            @Override
            public void getXmlAsFile() {

            }

            @Override
            public void getRawDataAsFile(long aLength, String aTargetFilePath) {

            }

            @Override
            public void commandComplete(boolean aSuccess) {
                DLog.log("Restore sending completed " + aSuccess);
                restoreHelper.clean();
            }

            @Override
            public void setSharedObject(String aKey, Object aObject) {

            }

            @Override
            public Object getSharedObject(String aKey) {
                return null;
            }

            @Override
            public void startNoopTimer() {

            }

            @Override
            public void stopNoopTimer() {

            }

            @Override
            public void disableTimeoutTimer() {

            }

            @Override
            public void enableTimeoutTimer() {

            }

            @Override
            public void addToPreviouslyTransferredItems(String aItem) {

            }

            @Override
            public boolean itemHasBeenPreviouslyTransferred(String aItem) {
                return false;
            }
        });
        restoreHelper.setDataCommandDelegate(EMSession.this);
        restoreHelper.init();
        restoreHelper.sendRestoreCompleted(dataType);
    }


    // Send the PIN to the remote device (should only be done if the remote device has asked for it)
    // -(void)sendPin:(NSString*)pin;

    // The remote device should become a source session
    void remoteToBecomeSource() {
        if (mSessionRole == EMSessionRole.EM_SESSION_TARGET_ROLE)
            return; // If we're already in the source role then just continue

        setMainSession();

        mSessionRole = EMSessionRole.EM_SESSION_TARGET_ROLE;

        // Start the you-are-target command initiator
        mYouAreSourceInitiator = new EMYouAreSourceCommandInitiator();
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mYouAreSourceInitiator;
        mYouAreSourceInitiator.setYouAreTargetDelegate(this); // So that we are notified when we become the source (when the other device becomes the target)
        mYouAreSourceInitiator.start(this);

        mConnection.setMainConnection();
    }

    // The remote device should become the target
    void remoteToBecomeTarget() {
        if (mSessionRole == EMSessionRole.EM_SESSION_SOURCE_ROLE)
            return; // If we're already in the target role then just continue

        setMainSession();

        mSessionRole = EMSessionRole.EM_SESSION_SOURCE_ROLE;

        // Start the you-are-target command initiator
        mYouAreTargetInitiator = new EMYouAreTargetCommandInitiator();
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mYouAreTargetInitiator;
        mYouAreTargetInitiator.setYouAreSourceDelegate(this); // So that we are notified when we become the source (when the other device becomes the target)
        mYouAreTargetInitiator.start(this);
    }

    void startCommandHandler(String aText) {
        DLog.log("In start command handler");
        boolean commandHandled = false;

        if (mSessionRole == EMSessionRole.EM_SESSION_UNKNOWN_ROLE) {
            if (!aText.equalsIgnoreCase(EMStringConsts.EM_COMMAND_TEXT_HANDSHAKE)) {
                // Assume that this is a reconnection event - we don't have a role, but we are getting
                // TODO: later it might be nicer to make this reconnection explicit - perhaps with a RECONNECT command?
                mSessionRole = EMSessionRole.EM_SESSION_TARGET_ROLE;
                mDelegate.nonHandshakeConnectionFromRemoteDeviceOnNewSession(this);
            }
        }

        for (EMCommandHandler commandHandler : mCommandResponders) {
            if (commandHandler.handlesCommand(aText)) {
                mCurrentCommandHandler = commandHandler;
                mCurrentCommandHandler.start(this);
                commandHandled = true;
                break;
            }
        }

        DLog.log("commandHandled " + commandHandled);

        if (!commandHandled) {
            // If the command was not handled then return an error
            sendText(EMStringConsts.EM_TEXT_RESPONSE_ERROR);
            handleIncomingCommands();
        }
    }

    InetAddress getRemoteDeviceAddress() {
        return mConnection.mRemoteIpAddress;
    }

    boolean setCryptoPassword(String aPassword) {
        if (mCurrentCommandHandler == mAddCryptoInitiator)
            return mAddCryptoInitiator.setPassword(aPassword);
        else if (mCurrentCommandHandler == mAddCryptoCommandResponder) {
            mAddCryptoCommandResponder.setPassword(aPassword);
            return true;
        } else
            return false;
    }

/////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////
// From EMConnectionDelegate

    // We have text (usually a command or a response)
    @Override
    public void gotText(String aText) {
        DLog.log("EMSession::gotText: " + aText);
        if (mCurrentCommandHandler == null || aText.contains(EMStringConsts.EM_COMMAND_TEXT)) {
            DLog.log("New command. Starting command handler.");
            // This is a new command - so find the appropriate command handler to handle it
            startCommandHandler(aText);
        } else {
            mCurrentCommandHandler.gotText(aText);
        }
    }

    // From EMConnectionDelegate
    // We have received data
    // This will be a file path to a file containing the received data
    // This could be raw data, or it could be XML data
    public void gotFile(String aDataPath) {
        DLog.log("EMSession::gotFile");
        if (mCurrentCommandHandler != null) {
            mCurrentCommandHandler.gotFile(aDataPath);
        } else {
            // Ignore if we don't have a command handler (we should have one)
        }
    }

    // From EMConnectionDelegate
    // Data has been sent
    public void sent() {
        if (mCurrentCommandHandler != null) {
            mCurrentCommandHandler.sent();
        } else {
            // Ignore if we don't have a command handler (we should have one)
        }
    }

    private boolean mStopped = false;

    public void stop() {
        cancel();
        mConnection.close();
        mStopped = true;
        mSendingData = false;
    }

    public void cancel() {
        if (mCurrentCommandHandler != null) {
            mCurrentCommandHandler.cancel();
            mCurrentCommandHandler = null;
        }
    }

    private Timer mPendingReconnectionTimer = new Timer();
    boolean mPendingReconnection = false;

    private Handler mPendingReconnectionHandler = new Handler() {
        public void handleMessage(Message msg) { // Dispatch timeout on the main thread
            DLog.log("*** Attempting reconnection");
            mPendingReconnection = false;

            // Recreate the connection
            createConnection();

            mConnection.setMainConnection();

            // Restart the current command handler
            enableTimeoutTimer();
            doSendData();
        }
    };

    private void startPendingReconnectionTimer() {
        if ((!mPendingReconnection) && (!mStopped)) {
            mPendingReconnection = true;
            DLog.log("*** Pending reconnection timer");

            try {
                mPendingReconnectionTimer = new Timer();
                mPendingReconnectionTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mPendingReconnectionHandler.obtainMessage(1).sendToTarget();
                    }
                }, EMConfig.PENDING_RECONNECTION_TIMER_IN_SECONDS * 1000);
            } catch (Exception ex) {
                // If we can't set the timeout timer for some reason then just ignore
            }
        }
    }

    // From EMConnectionDelegate
    // Something has gone wrong
    public void error(int aError) {
        // Cancel any active command handlers on the main session (initiators or responders)
        cancel();

        if ((aError == CMDError.CMD_WIFI_TIMEOUT_ERROR) || (aError == CMDError.CMD_WIFI_MAIN_CONNECTION_ERROR)) {

            // Ignore failures on the receiving side - but if we get them on the sending side then we should try to reconnect
            if ((mMainSession) && (mSessionRole == EMSessionRole.EM_SESSION_SOURCE_ROLE)) {
                // Is this a Wi-Fi timeout error or a critical connection failure? If so, try establishing the connection again
                mConnection.finish();

                // Wait a few seconds then try to connect again
                startPendingReconnectionTimer();
            }
        } else {
            if (mMainSession) {
                // Only report errors on the main connection - otherwise we get lots of problems on unreachable discovered devices (maybe on unreachable IPv6 addresses)
                mDelegate.error(aError);
            }
        }
    }

    public void disconnected() {
        mCurrentCommandHandler = null;
        mDelegate.disconnected(this);
        mState = EMSessionState.EM_SESSION_NOT_INITIALIZED;
    }

/////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////
//From EMCommandDelegate

    private Map<String, Object> mSharedObjectMap = null;

    public void setSharedObject(String aKey, Object aObject) {
        if (mSharedObjectMap == null) {
            mSharedObjectMap = new HashMap<String, Object>();
        }

        mSharedObjectMap.put(aKey, aObject);
    }

    public Object getSharedObject(String aKey) {
        if (mSharedObjectMap == null) {
            return null;
        }

        Object object = mSharedObjectMap.get(aKey);
        return object;
    }

    // Request from the active command to send some text
    public void sendText(String aText) {
        mConnection.sendText(aText);
    }

    // Request from the active command to send a file
    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {
        mConnection.sendFile(aFilePath, aDeleteFileWhenDone, aFileSendingProgressDelegate);
    }

    // Request from the active command to get some text
    public void getText() {
        mConnection.getText();
    }

    // Request from the active command to get some XML data
    public void getXmlAsFile() {
        mConnection.getXmlAsFile();
    }

    // Request from the active command to get some raw data
    public void getRawDataAsFile(long aLength, String aTargetFilePath) {
        mConnection.getRawDataAsFile(aLength, aTargetFilePath);
    }

    void sendContacts() {
        // Start the handshake command initiator
        mAddContactsInitiator = new EMAddDataCommandInitiator(EMDataType.EM_DATA_TYPE_CONTACTS);
        mAddContactsInitiator.setDataCommandDelegate(this);
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mAddContactsInitiator;
        mAddContactsInitiator.start(this);
    }

    void sendCalendar() {
        // Start the handshake command initiator
        mAddCalendarInitiator = new EMAddDataCommandInitiator(EMDataType.EM_DATA_TYPE_CALENDAR);
        mAddCalendarInitiator.setDataCommandDelegate(this);
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mAddCalendarInitiator;
        mAddCalendarInitiator.start(this);
    }

    void sendSms() {
        // Start the handshake command initiator
        mAddSmsInitiator = new EMAddDataCommandInitiator(EMDataType.EM_DATA_TYPE_SMS_MESSAGES);
        mAddSmsInitiator.setDataCommandDelegate(this);
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mAddSmsInitiator;
        mAddSmsInitiator.start(this);
    }

    void sendAccounts() {
        // Start the handshake command initiator
        mAddAccountsInitiator = new EMAddDataCommandInitiator(EMDataType.EM_DATA_TYPE_ACCOUNTS);
        mAddAccountsInitiator.setDataCommandDelegate(this);
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mAddAccountsInitiator;
        mAddAccountsInitiator.start(this);
    }

    private EMMediaSender mPhotoSender;
    private EMMediaSender mVideoSender;
    private EMFileSender mDocumentsSender;
    private EMMediaSender mMusicSender;
    private EMAppSender mAppSender;

    private EMDataTransferHelper mContactsTransferHelper;
    private EMDataTransferHelper mCalendarTransferHelper;
    private EMDataTransferHelper mMessagesTransferHelper;
    private EMDataTransferHelper mCallLogsTransferHelper;
    private EMDataTransferHelper mSettingsTransferHelper;

    void sendPhotos() {
        DLog.log("EMSession::sendPhotos START");
        mPhotoSender = new EMMediaSender(this, EMDataType.EM_DATA_TYPE_PHOTOS);
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mPhotoSender;
        mPhotoSender.mHostName = mHostName;
        mPhotoSender.start(this);
    }

    void sendVideo() {
        DLog.log("EMSession::sendVideo START");
        mVideoSender = new EMMediaSender(this, EMDataType.EM_DATA_TYPE_VIDEO);
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mVideoSender;
        mVideoSender.mHostName = mHostName;
        mVideoSender.start(this);
    }

    void sendMusic() {
        mMusicSender = new EMMediaSender(this, EMDataType.EM_DATA_TYPE_MUSIC);
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mMusicSender;
        mMusicSender.start(this);
    }

    void sendDocuments() {
        mDocumentsSender = new EMFileSender(this, EMDataType.EM_DATA_TYPE_DOCUMENTS);
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mDocumentsSender;
        mDocumentsSender.start(this);
    }

    void sendApp() {
        mAppSender = new EMAppSender(this, EMDataType.EM_DATA_TYPE_APP);
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mAppSender;
        mAppSender.start(this);
    }

    void sendQuit() {
        // Start the handshake command initiator
        mQuitCommandInitiator = new EMQuitCommandInitiator();
        mQuitCommandInitiator.setDataCommandDelegate(this);
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mQuitCommandInitiator;
        mQuitCommandInitiator.start(this);
    }


    void sendTextCommand() {
        // Start the handshake command initiator
        mSendTextCommandInitiator = new EMTextCommandInitiator();
        mSendTextCommandInitiator.setDataCommandDelegate(this);
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mSendTextCommandInitiator;
        mSendTextCommandInitiator.start(this);
    }


    /**
     * Darpan: This is the method to keep a watch for.
     * <p>
     * Transfer of each content type will start/end here. We can measure the time each datatype took,
     * their status (success/fail) etc.
     */
    void doSendData() {
        switch (mCurrentDataType) {
            case EMDataType.EM_DATA_TYPE_CONTACTS:
                //Start
                sendContacts();
                //End
                break;
            case EMDataType.EM_DATA_TYPE_CALENDAR:
                sendCalendar();
                break;
            case EMDataType.EM_DATA_TYPE_SMS_MESSAGES:
                sendSms();
                break;
            case EMDataType.EM_DATA_TYPE_ACCOUNTS:
                sendAccounts();
                break;
            case EMDataType.EM_DATA_TYPE_PHOTOS:
                sendPhotos();
                break;
            case EMDataType.EM_DATA_TYPE_VIDEO:
                sendVideo();
//				sendVideos();
                break;
            case EMDataType.EM_DATA_TYPE_MUSIC:
                sendMusic();
                break;
            case EMDataType.EM_DATA_TYPE_DOCUMENTS:
                sendDocuments();
                break;
            case EMDataType.EM_DATA_TYPE_APP:
                sendApp();
                break;
            default:
                // Ignore others;
                break;
        }
    }

    void doSendNextData() {
        // Move to the next selected data type
        boolean sendThisData = false;
        while ((mCurrentDataType != EMDataType.EM_DATA_TYPE_NO_MORE_DATA) && (!sendThisData)) {
            mCurrentDataType = mCurrentDataType << 1;
            if ((mCurrentDataType & mDataTypesToSend) != 0)
                sendThisData = true;
        }


        if (mCurrentDataType == EMDataType.EM_DATA_TYPE_NO_MORE_DATA) {
            // Notify the UI that all data has been sent
            EMProgressInfo progressInfo = new EMProgressInfo();
            progressInfo.mDataType = EMDataType.EM_DATA_TYPE_NO_MORE_DATA;
            mSendingData = false;
            mDelegate.progressUpdate(progressInfo);

            mConnection.disableTimeoutTimer();

            // We're done now - send the quit command
            sendQuit();
        }

        doSendData();
    }

    @Override
    public void commandComplete(String datatype, boolean status) {
        DLog.log("Migration completed "+datatype);
        commandCompleted(EMStringConsts.DATATYPE_MAP.get(datatype), true);
    }


    @Override
    public void restoreCompleted(String datatype, boolean status) {
        DLog.log("Restore Completed on destination. Sending command to source.");
        sendRestoreCompleted(datatype);
    }

    // Notification from the active command that it is complete
    public void commandComplete(boolean aSuccess) {
        // TODO: how to handle a failed command?
        boolean inCryptoResponder = (mCurrentCommandHandler == mAddCryptoCommandResponder);
        mCurrentCommandHandler = null;

        // Listen for more commands (if we're in target mode)
        if (mSessionRole == EMSessionRole.EM_SESSION_TARGET_ROLE) {
            if (inCryptoResponder && aSuccess) {
                mDelegate.pinOk();
            }
            handleIncomingCommands();
        } else {
            if (aSuccess == true) {
                if (mInitiatingCrypto) {
                    mInitiatingCrypto = false;
                    mDelegate.pinOk();
                }
                // below code is usful when we are doing sequencial transfer, so not req.
				/*else if (mSendingData)
				{
					doSendNextData();
				}*/
            } else {
                if (mInitiatingCrypto) {
                    // We failed while initiating crypto - try again
                    startCryptoInitiator();
                } else {
                    // Notify the UI that we've stopped sending data due to a failure
                    EMProgressInfo progressInfo = new EMProgressInfo();
                    progressInfo.mDataType = EMDataType.EM_DATA_TYPE_NO_MORE_DATA;
                    progressInfo.mFailed = true;
                    mSendingData = false;
                    mDelegate.progressUpdate(progressInfo);

                    // We're done now - send the quit command
                    sendQuit();
                }
            }
        }
    }

    @Override
    public void startNoopTimer() {
        mConnection.startNoopTimer();
    }

    @Override
    public void stopNoopTimer() {
        mConnection.stopNoopTimer();
    }

    @Override
    public void disableTimeoutTimer() {
        if (mConnection != null) {
            mConnection.disableTimeoutTimer();
        }
    }

    @Override
    public void enableTimeoutTimer() {
        if (mConnection != null) {
            mConnection.enableTimeoutTimer();
        }
    }

    @Override
    synchronized public void addToPreviouslyTransferredItems(String aItem) {
        if (mPreviouslyTransferredContentRegistry != null)
            mPreviouslyTransferredContentRegistry.addToPreviouslyTransferredItem(aItem);
    }

    @Override
    synchronized public boolean itemHasBeenPreviouslyTransferred(String aItem) {
        if (mPreviouslyTransferredContentRegistry != null)
            return mPreviouslyTransferredContentRegistry.itemHasBeenPreviouslyTransferred(aItem);
        else
            return false;
    }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//From EMHandshakeDelegate

    public void handshakeComplete(EMDeviceInfo aRemoteDeviceInfo) {
		/*
		InetAddress connectionRemoteIpAddress = mConnection.mRemoteIpAddress;
		if (connectionRemoteIpAddress != null)
		{
			aRemoteDeviceInfo.mIpAddress = connectionRemoteIpAddress;
		}
		*/

		/*
		String connectionRemoteHostName = mConnection.mHostName;
		if (connectionRemoteHostName != null)
		{
			aRemoteDeviceInfo.mHostName = connectionRemoteHostName;
		}
		else
		{
			// If we don't have the host name then just use the IP address from the device that is connecting to us
			aRemoteDeviceInfo.mHostName = mConnection.mRemoteIpAddress;
		}
*/

        mState = EMSessionState.EM_SESSION_HAS_QUIT;

        DLog.log("*** EMSession::handshakeComplete");
        aRemoteDeviceInfo.log();

        mDelegate.handshakeComplete(aRemoteDeviceInfo);

        close();
    }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//From EMYouAreSourceOrTargetDelegate

    @Override
    public void thisDeviceIsNowTheTarget() {
        DLog.log("EMSession: thisDeviceIsNowTheTarget");
        EMMigrateStatus.setTransportMode(EMProgressInfo.EMTransportMode.EM_WIFI);

        mSessionRole = EMSessionRole.EM_SESSION_TARGET_ROLE;

        // Notify the device manager that we have become the target
        mDelegate.haveBecomeTarget(this);

        if (EMConfig.NEW_DATA_TRANSFER_PROTOCOL) {
            startFileServer();
        }
        // Start listening for commands from the source (we are now the target)
        handleIncomingCommands();
    }

    @Override
    public void thisDeviceIsNowTheSource() {
        DLog.log("EMSession: thisDeviceIsNowTheSource");
        //Trying to log from Android device when IOS as Destination.
        //Starting a server to listen incoming request
		/*EMDeviceInfo mRemoteDeviceInfo = DashboardLog.getInstance().destinationEMDeviceInfo;
		if (mRemoteDeviceInfo.dbDevicePlatform.equalsIgnoreCase(EMStringConsts.EM_DEVICE_TYPE_IOS) && !Constants.IS_MMDS) {
			DLog.log("Starting a server for db logging IOS as destination");
		}*/

        startFileServer();
        EMMigrateStatus.setTransportMode(EMProgressInfo.EMTransportMode.EM_WIFI);

        mSessionRole = EMSessionRole.EM_SESSION_SOURCE_ROLE;
/*
		if (EMMigrateStatus.qrCodeWifiDirectMode()) {
			// The PIN request is no longer used, instead we use the crypto method as opposed to a plaintext PIN
			mDelegate.pinOk();
		}
		else */
        {
            mDelegate.haveBecomeSource(this);
            startCryptoInitiator();
        }
    }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//From EMDataCommandDelegate

    @Override
    public void progressUpdate(EMProgressInfo aProgressInfo) {
        if ((aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_QUIT_COMMAND_RECEIVED)
                || (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_QUIT_COMMAND_SENT)) {
            mState = EMSessionState.EM_SESSION_HAS_QUIT;
            close();
        }
        // Forward the progress upwards
        mDelegate.progressUpdate(aProgressInfo);
    }

    public void startCryptoInitiator() {
        mAddCryptoInitiator = new EMAddCryptoSrpCommandInitiator(this);
        mState = EMSessionState.EM_SESSION_IN_COMMAND;
        mCurrentCommandHandler = mAddCryptoInitiator;
        mInitiatingCrypto = true;
        mAddCryptoInitiator.initialize(EMGlobals.getJavascriptWebView());
        mAddCryptoInitiator.start(this);
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//From EMAddCryptoCommandDelegate

    @Override
    public void cryptoPasswordRequested() {
        mDelegate.cryptoPasswordRequested();
    }

    EMSessionState mState;

    int mDataTypesToSend; // = 0; // The data types we need to send (EMDataType or'ed together)
    int mCurrentDataType;
    EMSessionType mSessionType; // = EM_SESSION_NOT_INITIALIZED;

    EMConnection mConnection;
    EMSessionDelegate mDelegate;

    ArrayList<EMCommandHandler> mCommandResponders;

    private EMAddCryptoSrpCommandResponder mAddCryptoCommandResponder;

    EMHandshakeCommandInitiator mHandshakeInitiator;
    EMYouAreTargetCommandInitiator mYouAreTargetInitiator;
    EMYouAreSourceCommandInitiator mYouAreSourceInitiator;

    // EMYouAreSourceCommandInitiator* mYouAreSourceInitiator;

    EMAddDataCommandInitiator mAddContactsInitiator;
    EMAddDataCommandInitiator mAddCalendarInitiator;
    EMAddDataCommandInitiator mAddSmsInitiator;
    EMAddDataCommandInitiator mAddAccountsInitiator;
    EMQuitCommandInitiator mQuitCommandInitiator;
    EMTextCommandInitiator mSendTextCommandInitiator;  //PERVACIO ADDED

    // EMPinRequestCommandResponder* mPinRequestCommandResponder;

    EMCommandHandler mCurrentCommandHandler;
    EMAddCryptoSrpCommandInitiator mAddCryptoInitiator;

    String mPin;

    EMSessionRole mSessionRole;

    boolean mSendingData;

    private Context mContext;

    private boolean mInitiatingCrypto;

    private InetAddress mHostName;
    private int mPort;
    private EMPreviouslyTransferredContentRegistry mPreviouslyTransferredContentRegistry;
}
