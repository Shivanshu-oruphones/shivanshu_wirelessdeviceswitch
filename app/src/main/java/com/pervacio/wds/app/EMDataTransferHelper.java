//package com.pervacio.wds.app;
//
//import android.media.MediaScannerConnection;
//import android.os.Build;
//import android.os.Environment;
//import android.os.Handler;
//import android.os.Looper;
//import android.os.Message;
//import android.os.Messenger;
//import android.os.RemoteException;
//import android.text.TextUtils;
//import android.util.Xml;
//
//import com.google.gson.Gson;
//import com.google.gson.stream.JsonReader;
//import com.pervacio.wds.app.ui.EasyMigrateActivity;
//import com.pervacio.wds.app.ui.PermissionHandler;
//import com.pervacio.wds.custom.appmigration.AppMigrateUtils;
//import com.pervacio.wds.custom.asynctask.TransactionLogging;
//import com.pervacio.wds.custom.models.EDeviceSwitchSession;
//import com.pervacio.wds.custom.models.MigrationStats;
//import com.pervacio.wds.custom.utils.CommonUtil;
//import com.pervacio.wds.custom.utils.Constants;
//import com.pervacio.wds.custom.utils.DashboardLog;
//import com.pervacio.wds.custom.utils.ServerCallBacks;
//
//import org.xmlpull.v1.XmlPullParser;
//
//import java.io.ByteArrayInputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.StringReader;
//import java.net.InetAddress;
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.util.Random;
//import java.util.Scanner;
//import java.util.Timer;
//import java.util.TimerTask;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.concurrent.TimeUnit;
//
//import static com.pervacio.wds.custom.utils.Constants.ESTIMATION_TIME_FILESIZE;
//import static com.pervacio.wds.custom.utils.Constants.HTTP_AUTHENTICATION_PASSWORD;
//import static com.pervacio.wds.custom.utils.Constants.HTTP_AUTHENTICATION_USERNAME;
//import static com.pervacio.wds.custom.utils.Constants.IS_MMDS;
//import static com.pervacio.wds.custom.utils.Constants.LOGGING_API_ENDPOINT;
//
///**
// * Created by Shyam on 14-07-2017.
// */
//
//public class EMDataTransferHelper{
//
//    private static final int READ_BUFFER_SIZE = 1024 * 128;
//
//    private InetAddress mHostName = null;
//    private int mPort = EMServer.CONTENT_TRANSFER_PORT;
//    Socket mSocket = null;
//    private InputStream mSocketRawInputStream = null;
//    private OutputStream mSocketRawOutputStream = null;
//    private byte[] mBuffer = new byte[READ_BUFFER_SIZE];
//    private String mCurrentDataType = null;
//    private EMGenerateDataTask mGenerateDataTask;
//    private EMGenerateDataInThread mGenerateDataThread;
//    private int mRetryCount=0;
//    private int socketTimeout = 0;
//
//
//    private EMSendFileState sendFileState = EMSendFileState.EM_SEND_STATE_NONE;
//    private EMReceiveFileState receiveFileState = EMReceiveFileState.EM_RECEIVE_STATE_NONE;
//    private EMSendPIMState sendPIMState = EMSendPIMState.EM_SEND_STATE_NONE;
//    private EMEstimationState estimationState = EMEstimationState.EM_ESTIMATION_STATE_NONE;
//    private EMReceivePIMState receivePIMState = EMReceivePIMState.EM_RECEIVE_STATE_NONE;
//
//    private EMSendPIMState restoreCompletedState = EMSendPIMState.EM_SEND_STATE_NONE;
//
//    private EMCommandDelegate mCommandDelegate = null;
//    private EMDataCommandDelegate mDataCommandDelegate = null;
//
//
//    public void setDataCompleteDelegate(EMDataCompleteDelegate mDataCompleteDelegate) {
//        this.mDataCompleteDelegate = mDataCompleteDelegate;
//    }
//
//    private EMDataCompleteDelegate mDataCompleteDelegate=null;
//
//    private Handler mMainThreadHandler = null;
//    private Messenger mMainThreadMessenger = null;
//
//    private Timer mTimeoutTimer = new Timer();
//    private long mLastActivityTime = System.currentTimeMillis();
//    private static final long TRANSFER_INACTIVE_TIMEOUT = 60*1000L;
//
//    public static final int ERROR_CODE_NONE = -1;
//    public static final int ERROR_CODE_SUCESS = 0;
//    public static final int ERROR_CODE_FAIL = 1;
//    public static final int ERROR_CODE_TIMEOUT = 2;
//    public static final int ERROR_CODE_UNSUPPORTED = 3;
//    public static final int ERROR_CODE_FILE_NOTFOUND = 4;
//    public static final int ERROR_CODE_READ_FAILED = 5;;
//
//    private int mErrorCode = -1;
//
//    private boolean mUpdateProgress =true;
//
//    private enum EMSendFileState {
//        EM_SEND_STATE_NONE,
//        EM_SENDING_ADD_FILE_COMMAND,
//        EM_WAITING_FOR_ADD_FILE_RESPONSE,
//        EM_SENDING_ADD_FILE_XML,
//        EM_WAITING_FOR_XML_OK,
//        EM_SENDING_RAW_FILE_DATA,
//        EM_WAITING_FOR_FINAL_OK,
//        EM_CANCELLED
//    }
//
//    private enum EMReceiveFileState {
//        EM_RECEIVE_STATE_NONE,
//        EM_SENDING_INITIAL_OK,
//        EM_WAITING_FOR_ADD_FILE_XML,
//        EM_SENDING_XML_OK,
//        EM_WAITING_FOR_RAW_FILE_DATA,
//        EM_SENDING_FINAL_OK,
//        EM_CANCELLED
//    }
//
//
//    private enum EMSendPIMState {
//        EM_SEND_STATE_NONE,
//        EM_WAITING_FOR_DATA_GENERATION,
//        EM_SENDING_ADD_DATA_COMMAND,
//        EM_WAITING_FOR_ADD_DATA_RESPONSE,
//        EM_SENDING_DATA,
//        EM_WAITING_FOR_FINAL_OK,
//        EM_CANCELLED
//    }
//
//    private enum EMReceivePIMState {
//        EM_RECEIVE_STATE_NONE,
//        EM_SENDING_INITIAL_OK,
//        EM_WAITING_FOR_RAW_FILE_DATA,
//        EM_PROCESSING_RECEIVED_DATA,
//        EM_SENDING_FINAL_OK,
//        EM_CANCELLED
//    }
//
//    private enum EMEstimationState{
//        EM_ESTIMATION_STATE_NONE,
//        EM_ESTIMATION_WRITE_DATA,
//        EM_ESTIMATION_READ_DATA,
//        EM_ESTIMATION_STATE_DONE;
//
//    }
//
//
//    class FileInfo {
//        int mFileType = EMDataType.EM_DATA_TYPE_NOT_SET;
//        String mDataType = "";
//        String mFileName = "unknown";
//        String mFilePath = "";
//        String mRelativePath = "";
//        long mFileSize = -1;
//        long mTotalMediaSize = -1;
//        boolean retry = false;
//    }
//
//    public EMDataTransferHelper(InetAddress aHostName) {
//        mHostName = aHostName;
//    }
//
//    public EMDataTransferHelper(Socket aSocket) {
//        mSocket = aSocket;
//    }
//
//    public void setCommandDelegate(EMCommandDelegate aCommandDelegate) {
//        mCommandDelegate = aCommandDelegate;
//    }
//
//    public void setDataCommandDelegate(EMDataCommandDelegate aDataCommandDelegate) {
//        mDataCommandDelegate = aDataCommandDelegate;
//    }
//
//    private void progressUpdate(EMProgressInfo aProgressInfo) {
//        if (mDataCommandDelegate != null) {
//            mDataCommandDelegate.progressUpdate(aProgressInfo);
//        }
//    }
//
//    public boolean init() {
//        mMainThreadHandler = new Handler() {
//            public void handleMessage(Message msg) {
//                EMFTUResponseMessage message = (EMFTUResponseMessage) msg.obj;
//                switch (message.mId) {
//                    case EMFTURespSendFileSuccess:
//                        mCommandDelegate.commandComplete(true);
//                        break;
//                    case EMFTURespSendFileError:
//                        clean();
//                        mCommandDelegate.commandComplete(false);
//                        break;
//                    case EMFTURespReceivedFile: {
//                        FileInfo fileInfo = (FileInfo) message.obj;
//                        String[] paths = {fileInfo.mFilePath};
//                        EMMigrateStatus.addItemTransferred(fileInfo.mFileType);
//                        CommonUtil.getInstance().setRestoredMediaCount(1);
//                        EMMigrateStatus.addTransferedFilesSize(fileInfo.mFileType,fileInfo.mFileSize);
//                        MediaScannerConnection.scanFile(EMUtility.Context(), paths, null, null);
//                        if (EMMigrateStatus.isMediaRestorationCompleted()) {
//                            //Once media completes, update to top layer
//                            EMProgressInfo progressInfo = new EMProgressInfo();
//                            progressInfo.mDataType = EMDataType.EM_DATA_TYPE_MEDIA;
//                            progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TEXT_RESTORE_COMPLETED;
//                            progressUpdate(progressInfo);
//                        }
//                        break;
//                    }
//                    case EMFTURespProgressInfo: {
//                        EMProgressInfo progressInfo = (EMProgressInfo) message.obj;
//                        progressUpdate(progressInfo);
//                        break;
//                    }
//                }
//            }
//        };
//        ExecutorService executorService = Executors.newFixedThreadPool(2);
//        WorkerThreadForSocketConection workerThreadForSocketConection = new WorkerThreadForSocketConection(this);
//        Future future = executorService.submit(workerThreadForSocketConection);
//        boolean connection = false;
//        try {
//            if (future.get(15, TimeUnit.SECONDS) == null) {
//                connection = true;
//            } else
//                connection = false;
//            DLog.log("Socket connection established : " + connection);
//            executorService.shutdownNow();
//        } catch (Exception ex) {
//            DLog.log("Exception while getting connection status or closing executor : " + ex);
//        }
//
//        mMainThreadMessenger = new Messenger(mMainThreadHandler);
//        mWorkerThread.start();
//        return connection;
//    }
//
//    public void clean() {
//        try {
//            if(mSocket!=null) {
//                synchronized (mSocket) {
//                    try {
//                        mSocketRawInputStream.close();
//                    } catch (Exception ex) {
//                    } finally {
//                        mSocketRawInputStream = null;
//                    }
//                    try {
//                        mSocketRawOutputStream.close();
//                    } catch (Exception ex) {
//                    } finally {
//                        mSocketRawOutputStream = null;
//                    }
//                    try {
//                        mSocket.close();
//                    } catch (Exception ex) {
//                    } finally {
//                        mSocket = null;
//                    }
//                    try {
//                        // TODO: send a quit message to the worker thread
//                        mWorkerThread.interrupt();
//                    } catch (Exception ex) {
//                        DLog.log(ex);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private boolean isConnected(){
//        boolean isConnected = false;
//        try {
//            if(mSocket != null && mSocket.isConnected()){
//                isConnected = true;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return isConnected;
//    }
//
//    public boolean initialiseConnection() {
//        try {
//            DLog.log("----initialiseConnection----");
//            if (mSocket == null || !mSocket.isConnected()) {
//                DLog.log("connect to host " + mHostName.toString()+ " on port : "+mPort);
//                mSocket = new Socket();
//                InetSocketAddress remoteAddr = new InetSocketAddress(mHostName, mPort);
//                mSocket.setTcpNoDelay(true);
//                if (EMUtility.shouldBindSocketToWifi()) {
//                    EMUtility.bindSocketToWiFiNetwork(mSocket);
//                }
//                mSocket.connect(remoteAddr,socketTimeout);
//                if (mSocket.isConnected()) {
//                    DLog.log(String.format("***** Socket connected: %s on port %d", mHostName.toString(), mPort));
//                }
//                else {
//                    DLog.log(String.format("***** Socket not connected: %s on port %d", mHostName.toString(), mPort));
//                }
//            }
//            DLog.log("Socket SendBufferSize:" + mSocket.getSendBufferSize());
//            DLog.log("Socket ReceiveBufferSize:" + mSocket.getReceiveBufferSize());
//            mSocketRawInputStream = mSocket.getInputStream();
//            mSocketRawOutputStream = mSocket.getOutputStream();
//            return true;
//        } catch (Exception ex) {
//            DLog.log(ex.getMessage());
//        }
//        return false;
//    }
//
//    /*private boolean reconnect() {
//        DLog.log("reconnect for file transfer");
//        try {
//            if (mSocket != null && !mSocket.isConnected()) {
//                DLog.log("start reconnect");
//                InetSocketAddress remoteAddr = new InetSocketAddress(mHostName, mPort);
//                mSocket.connect(remoteAddr);
//                if (mSocket.isConnected())
//                    DLog.log(String.format("***** Socket reconnected: %s on port %d", mHostName.toString(), mPort));
//                else {
//                    DLog.log(String.format("***** Socket not reconnected: %s on port %d", mHostName.toString(), mPort));
//                }
//            }
//            mSocketRawInputStream = mSocket.getInputStream();
//            mSocketRawOutputStream = mSocket.getOutputStream();
//            return true;
//        } catch (Exception ex) {
//            DLog.log(ex);
//        }
//        return false;
//    }*/
//
//    private void checkConnection() {
//        if (mSocket == null || !mSocket.isConnected()) {
//            initialiseConnection();
//        }
//    }
//
//    public int getErrorCode() {
//        return mErrorCode;
//    }
//
//    private Handler mTimeoutHandler = new Handler() {
//        public void handleMessage(Message msg) {
//            mErrorCode = ERROR_CODE_TIMEOUT;
//            DLog.log("Timed out");
//            try {
//                mSocket.close();
//                clean();
//            }catch (Exception ex) {
//                DLog.log(ex);
//            }finally {
//                mSocket = null;
//            }
//        }
//    };
//
//    private void createNewTimer(final long timeoutInMillis) {
//        try {
//            mTimeoutTimer = new Timer();
//            mTimeoutTimer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    long currentTimeInMillis = System.currentTimeMillis();
//                    long elapsedInMillis = currentTimeInMillis - mLastActivityTime;
//                    if( elapsedInMillis > timeoutInMillis) {
//                        mTimeoutHandler.obtainMessage(1).sendToTarget();
//                    } else {
//                        restartTimeoutTimer(timeoutInMillis);
//                    }
//                }
//            }, timeoutInMillis);
//        } catch (Exception ex) {
//            // If we can't set the timeout timer for some reason then just ignore
//        }
//    }
//
//    private void restartTimeoutTimer(long timeoutInMillis) {
//        stopTimeoutTimer();
//        createNewTimer(timeoutInMillis);
//    }
//
//    private void stopTimeoutTimer() {
//        if (mTimeoutTimer != null) {
//            mTimeoutTimer.cancel();
//        }
//    }
//
//    private void updateLastActivityTime() {
//        mLastActivityTime = System.currentTimeMillis();
//    }
//
//    private static final int EMFTUReqSendFile = 1;
//    private static final int EMFTUReqReceiveFiles = 2;
//    private static final int EMFTUReqSendPIM = 3;
//    private static final int EMFTUReqEstimation = 4;
//    private static final int EMFTUReqRestoreDone = 5;
//
//
//    class EMFTURequestMessage {
//        public int mId;
//    }
//
//    private static final int EMFTURespSendFileSuccess = 1;
//    private static final int EMFTURespSendFileError = 2;
//    private static final int EMFTURespReceivedFile = 3;
//    private static final int EMFTURespProgressInfo = 4;
//
//    class EMFTUResponseMessage {
//        public int mId;
//        public Object obj;
//    }
//
//    public void cancel() {
//        mWorkerThread.cancelOperation();
//    }
//
//    public boolean sendFile(EMFileMetaData aFileMetaData) {
//        mMetaData = aFileMetaData;
//        Message msg = new Message();
//        EMFTURequestMessage message = new EMFTURequestMessage();
//        message.mId = EMFTUReqSendFile;
//        msg.obj = message;
//        mWorkerThread.getHandler().sendMessage(msg);
//        return true;
//    }
//
//    public boolean startReceivingFiles() {
//        Message msg = new Message();
//        EMFTURequestMessage message = new EMFTURequestMessage();
//        message.mId = EMFTUReqReceiveFiles;
//        msg.obj = message;
//        mWorkerThread.getHandler().sendMessage(msg);
//        return true;
//    }
//
//    public boolean sendPIM(String dataType) {
//        mCurrentDataType = dataType;
//        Message msg = new Message();
//        EMFTURequestMessage message = new EMFTURequestMessage();
//        message.mId = EMFTUReqSendPIM;
//        msg.obj = message;
//        mWorkerThread.getHandler().sendMessage(msg);
//        return true;
//    }
//
//
//    public boolean startEstimation() {
//        socketTimeout = 10*1000;
//        Message msg = new Message();
//        EMFTURequestMessage message = new EMFTURequestMessage();
//        message.mId = EMFTUReqEstimation;
//        msg.obj = message;
//        mWorkerThread.getHandler().sendMessage(msg);
//        return true;
//    }
//
//    public boolean sendRestoreCompleted(String dataType){
//        socketTimeout = 10*1000;
//        mCurrentDataType = dataType;
//        Message msg = new Message();
//        EMFTURequestMessage message = new EMFTURequestMessage();
//        message.mId = EMFTUReqRestoreDone;
//        msg.obj = message;
//        mWorkerThread.getHandler().sendMessage(msg);
//        return true;
//    }
//
//    private EMFileMetaData mMetaData = null;
//    private String mGeneratedMetadataXmlFilePath = null;
//
//    EMDataTransferThread mWorkerThread = new EMDataTransferThread();
//
//    class EMDataTransferThread extends Thread {
//        Handler mInThreadHandler;
//        boolean keepRunning = false;
//        boolean mCancel = false;
//
//        synchronized Handler getHandler() {
//            while (mInThreadHandler == null) {
//                try {
//                    wait();
//                } catch (Exception e) {
//                    DLog.log(e);
//                }
//            }
//
//            return mInThreadHandler;
//        }
//
//        @Override
//        public void run() {
//            DLog.log(">>EMDataTransferThread");
//            Looper.prepare();
//
//            synchronized (this) {
//
//                mInThreadHandler = new Handler() {
//                    public void handleMessage(Message msg) {
//                        EMFTURequestMessage message = (EMFTURequestMessage) msg.obj;
//                        checkConnection();
//                        switch (message.mId) {
//                            case EMFTUReqSendFile:
//                                sendFileInThread();
//                                break;
//                            case EMFTUReqReceiveFiles:
//                                receiveFilesInThread();
//                                break;
//                            case EMFTUReqSendPIM:
//                                sendPIMData();
//                                break;
//                            case EMFTUReqEstimation:
//                                sendDataForEstimation();
//                                break;
//                            case EMFTUReqRestoreDone:
//                                sendRestoreCompleted();
//                                break;
//                            default:
//                                break;
//                        }
//
//                    }
//                };
//                notifyAll();
//            }
//            Looper.loop();
//        }
//
//        public void cancelOperation() {
//            mCancel = true;
//            //TODO - cancel the ongoing peration
//        }
//
//        private void sendTransferComplete(boolean aSuccess) {
//            stopTimeoutTimer();
//            Message msg = new Message();
//            EMFTUResponseMessage resp = new EMFTUResponseMessage();
//            if (aSuccess) {
//                resp.mId = EMFTURespSendFileSuccess;
//            } else {
//                resp.mId = EMFTURespSendFileError;
//            }
//            msg.obj = resp;
//            try {
//                mMainThreadMessenger.send(msg);
//            } catch (RemoteException e1) {
//                e1.printStackTrace();
//                DLog.log(e1);
//            }
//        }
//
//        private void sendProgressUpdate(EMProgressInfo aProgressInfo) {
//            Message msg = new Message();
//            EMFTUResponseMessage resp = new EMFTUResponseMessage();
//            resp.mId = EMFTURespProgressInfo;
//            resp.obj = aProgressInfo;
//            msg.obj = resp;
//            try {
//                mMainThreadMessenger.send(msg);
//            } catch (RemoteException e1) {
//                e1.printStackTrace();
//                DLog.log(e1);
//            }
//        }
//
//        private void receivedAFile(FileInfo aFileInfo) {
//            Message msg = new Message();
//            EMFTUResponseMessage resp = new EMFTUResponseMessage();
//            resp.mId = EMFTURespReceivedFile;
//            resp.obj = aFileInfo;
//            msg.obj = resp;
//            try {
//                mMainThreadMessenger.send(msg);
//            } catch (RemoteException e1) {
//                e1.printStackTrace();
//                DLog.log(e1);
//            }
//        }
//
//        private void sendFileInThread() {
//            checkMigrationStatus();
//            keepRunning = true;
//            sendFileState = EMSendFileState.EM_SEND_STATE_NONE;
//            boolean isSuccess = false;
//            mErrorCode = -1; // resetting Error code before sending a new file.
//            while (keepRunning) {
//                updateLastActivityTime();
//                switch (sendFileState) {
//                    case EM_SEND_STATE_NONE: {
//                        createNewTimer(TRANSFER_INACTIVE_TIMEOUT);
//                        generateMetaData();
//                        File metadataFile = new File(mGeneratedMetadataXmlFilePath);
//                        sendFileState = EMSendFileState.EM_SENDING_ADD_FILE_COMMAND;
//                        DLog.log(">>trying to send " + mMetaData.mFileType + " of size: " + mMetaData.mSize);
//                        String metadataXML = "";
//                        try {
//                            byte[] metadataXMLArr = EMUtility.readFileToByteArray(metadataFile);
//                            metadataXML = new String(metadataXMLArr);
//                        } catch (Exception e) {
//                            DLog.log(e);
//                        }
//                        boolean isMetaDataSent = sendText(EMStringConsts.EM_COMMAND_TEXT_ADD_FILE + " " + Long.toString(metadataFile.length()) + " " + metadataXML);
//                        if (!isMetaDataSent) {
//                            keepRunning = false;
//                            DLog.log("error while sending the meta data");
//                        }
//                        try {
//                            metadataFile.delete();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                        break;
//                    }
//                    case EM_SENDING_ADD_FILE_COMMAND: {
//                        sendFileState = EMSendFileState.EM_WAITING_FOR_XML_OK;
//                        String text = getText();
//                        if (text.equalsIgnoreCase(EMStringConsts.EM_TEXT_RESPONSE_ALREADY_EXISTS)) {
//                            EMMigrateStatus.addLiveTransferredSize(mMetaData.mDataType, mMetaData.mSize);
//                            keepRunning = false;
//                            isSuccess = true;
//                            break;
//                        } else if (text.equalsIgnoreCase(EMStringConsts.EM_TEXT_RESPONSE_UNSUPPORTED)) {
//                            mErrorCode = ERROR_CODE_UNSUPPORTED;
//                            keepRunning = false;
//                            break;
//                        }
//                        boolean ok = text.equals(EMStringConsts.EM_TEXT_RESPONSE_OK);
//                        if (!ok) {
//                            keepRunning = false;
//                        }
//                        break;
//                    }
//                    case EM_WAITING_FOR_XML_OK: {
//                        sendFileState = EMSendFileState.EM_SENDING_RAW_FILE_DATA;
//                        DLog.log("start writing " + mMetaData.mFileType + " of size: " + mMetaData.mSize);
//                        FileInfo writeFileInfo = new FileInfo();
//                        writeFileInfo.mFileType = mMetaData.mDataType;
//                        writeFileInfo.mFilePath = mMetaData.mSourceFilePath;
//                        boolean fileSentOk = writeFile(writeFileInfo);
//                        if (!fileSentOk) {
//                            keepRunning = false;
//                        }
//                        //DLog.log("wrote file");
//                        break;
//                    }
//                    case EM_SENDING_RAW_FILE_DATA: {
//                        String text = getText();
//                        sendFileState = EMSendFileState.EM_WAITING_FOR_FINAL_OK;
//                        if (text.equalsIgnoreCase(EMStringConsts.EM_TEXT_RESPONSE_UNSUPPORTED)) {
//                            mErrorCode = ERROR_CODE_UNSUPPORTED;
//                            keepRunning = false;
//                        }else if (!text.equals(EMStringConsts.EM_TEXT_RESPONSE_OK)) {
//                            keepRunning = false;
//                        }
//                        break;
//                    }
//                    case EM_WAITING_FOR_FINAL_OK: {
//                        isSuccess = true;
//                        keepRunning = false;
//                        break;
//                    }
//                }
//            }
//            sendTransferComplete(isSuccess);
//        }
//
//
//        private void sendDataForEstimation() {
//            keepRunning = true;
//            boolean isSuccess = false;
//            while (keepRunning) {
//                switch (estimationState) {
//                    case EM_ESTIMATION_STATE_NONE:
//                        DLog.log("Sending start estimation command ");
//                        boolean sent = sendText(EMStringConsts.EM_COMMAND_START_ESTIMATION);
//                        if (sent) {
//                            String ok = getText();
//                            estimationState = EMEstimationState.EM_ESTIMATION_WRITE_DATA;
//                        }else {
//                            DLog.log("some thing went wrong,making keep running false");
//                            handleReadError();
//                        }
//                        break;
//                    case EM_ESTIMATION_WRITE_DATA:
//                        long totalBytesWritten = writeFile(Constants.ESTIMATION_CALCULATION_TIME);
//                        isSuccess = true;
//                        mCommandDelegate.addToPreviouslyTransferredItems(String.valueOf(totalBytesWritten));
//                        if(isSuccess){
//                            estimationState = EMEstimationState.EM_ESTIMATION_STATE_DONE;
//                        }else {
//                            handleReadError();
//                        }
//                        break;
//                    case EM_ESTIMATION_STATE_DONE:
//                       // String ok = getText();
//                        keepRunning = false;
//                        isSuccess = true;
//                        break;
//                    default:
//                        break;
//                }
//            }
//            sendTransferComplete(isSuccess);
//            clean();
//        }
//
//        private void receiveDataForEstimation() {
//            keepRunning = true;
//            boolean isSuccess = false;
//            while (keepRunning) {
//                switch (estimationState) {
//                    case EM_ESTIMATION_STATE_NONE:
//                        boolean sent = sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
//                        if (sent) {
//                            estimationState = EMEstimationState.EM_ESTIMATION_READ_DATA;
//                        }
//                        break;
//                    case EM_ESTIMATION_READ_DATA:
//                        String path = EMUtility.temporaryFileName();
//                        boolean readFile = readToFile(path, ESTIMATION_TIME_FILESIZE*100);
//                        if (readFile)
//                            estimationState = EMEstimationState.EM_ESTIMATION_STATE_DONE;
//                        else
//                            keepRunning = false;
//                        try {
//                            new File(path).delete();
//                        } catch (Exception ex) {
//                            DLog.log(ex.getMessage());
//                        }
//                        break;
//                    case EM_ESTIMATION_STATE_DONE:
//                        //sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
//                        keepRunning = false;
//                        isSuccess = true;
//                        break;
//                    default:
//                        break;
//                }
//            }
//            sendTransferComplete(isSuccess);
//            clean();
//        }
//
//        private void sendRestoreCompleted() {
//            keepRunning = true;
//            restoreCompletedState = EMSendPIMState.EM_SEND_STATE_NONE;
//            boolean isSuccess = false;
//            checkMigrationStatus();
//            while (keepRunning) {
//                switch (restoreCompletedState) {
//                    case EM_SEND_STATE_NONE: {
//                        if(IS_MMDS && !"Android".equalsIgnoreCase(EasyMigrateActivity.remotePlatform) && !EasyMigrateActivity.iosSMSRestored){
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            break;
//                        }
//                        DLog.log("RESTORE COMPLETED FOR "+ ":" + mCurrentDataType);
//                        boolean sendText = sendText(EMStringConsts.EM_COMMAND_RESTORE_COMPLETED + ":" + mCurrentDataType);
//                        if (sendText) {
//                            restoreCompletedState = EMSendPIMState.EM_WAITING_FOR_FINAL_OK;
//                        } else {
//                            if(mRetryCount<3) {
//                                checkMigrationStatus();
//                                if(!isConnected()){
//                                    initialiseConnection();
//                                }
//                                mRetryCount++;
//                                DLog.log("sendRestoreTryCount = "+mRetryCount);
//                                try {
//                                    Thread.sleep(3000);
//                                } catch (Exception e) {
//                                    DLog.log(e.getMessage());
//                                }
//                                restoreCompletedState = EMSendPIMState.EM_SEND_STATE_NONE;
//                                break;
//                            } else {
//                                handleReadError();
//                                DLog.log("sendRestore Failed after the count= "+mRetryCount);
//                            }
//                        }
//                        EMProgressInfo progressInfo = new EMProgressInfo();
//                        progressInfo.mDataType = EMStringConsts.DATATYPE_MAP.get(mCurrentDataType);
//                        progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TEXT_RESTORE_COMPLETED;
//                        sendProgressUpdate(progressInfo);
//                        break;
//                    }
//                    case EM_WAITING_FOR_FINAL_OK: {
//                        String text = getText();
//                        // At this point treat migration as success irrespective of return values
//                        isSuccess = true;
//                        keepRunning = false;
//                        break;
//                    }
//                    default:
//                        break;
//                }
//            }
//            sendTransferComplete(isSuccess);
//        }
//
//
//        private void sendPIMData() {
//            keepRunning = true;
//            sendPIMState = EMSendPIMState.EM_SEND_STATE_NONE;
//            boolean isSuccess = false;
//            while (keepRunning) {
//                updateLastActivityTime();
//                switch (sendPIMState) {
//                    case EM_SEND_STATE_NONE: {
//                        mGeneratedMetadataXmlFilePath = MigrationStats.getInstance().getPimBackupFile(mCurrentDataType);
//                        if (mGeneratedMetadataXmlFilePath != null && new File(mGeneratedMetadataXmlFilePath).exists()) {
//                            sendPIMState = EMSendPIMState.EM_SENDING_ADD_DATA_COMMAND;
//                        } else {
//                            writePIMXml();
//                            sendPIMState = EMSendPIMState.EM_WAITING_FOR_DATA_GENERATION;
//                        }
//                        break;
//                    }
//                    case EM_WAITING_FOR_DATA_GENERATION:
//                        try {
//                            Thread.sleep(100);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                        break;
//                    case EM_SENDING_ADD_DATA_COMMAND: {
//                        String filePath ;
//                        checkMigrationStatus();
//                        reInitiateConnection();
//                        if (mGeneratedMetadataXmlFilePath != null) {
//                            filePath = mGeneratedMetadataXmlFilePath;
//                        } else if (mGenerateDataTask != null) {
//                            filePath = mGenerateDataTask.getFilePath();
//                        } else {
//                            filePath = mGenerateDataThread.getFilePath();
//                        }
//                        MigrationStats.getInstance().addToBackupList(mCurrentDataType,filePath);
//                        File fileInfo = new File(filePath);
//                        long fileSize = fileInfo.length();
//                        String dataSizeString = Long.toString(fileSize);
//                        String commandString = EMStringConsts.EM_COMMAND_TEXT_ADD_DATA + " " + mCurrentDataType + " " + dataSizeString;
//                        boolean ok = sendText(commandString);
//                        if (!ok) {
//                            handleReadError();
//                        }
//                        sendPIMState = EMSendPIMState.EM_WAITING_FOR_ADD_DATA_RESPONSE;
//                        break;
//                    }
//                    case EM_WAITING_FOR_ADD_DATA_RESPONSE: {
//                        String text = getText();
//                        boolean ok = text.equals(EMStringConsts.EM_TEXT_RESPONSE_OK);
//                        if (!ok) {
//                            keepRunning = false;
//                        }
//                        sendPIMState = EMSendPIMState.EM_SENDING_DATA;
//                        break;
//                    }
//                    case EM_SENDING_DATA: {
//                        String filePath = null;
//                        if (mGeneratedMetadataXmlFilePath != null) {
//                            filePath = mGeneratedMetadataXmlFilePath;
//                        }
//                        else if(mGenerateDataTask != null){
//                            filePath = mGenerateDataTask.getFilePath();
//                        }else {
//                            filePath = mGenerateDataThread.getFilePath();
//                        }
//                        boolean fileSentOk = writeFile(filePath);
//                        if (!fileSentOk) {
//                            keepRunning = false;
//                        }
//                        DLog.log(mCurrentDataType + " XML Sent");
//                        sendPIMState = EMSendPIMState.EM_WAITING_FOR_FINAL_OK;
//                        break;
//                    }
//                    case EM_WAITING_FOR_FINAL_OK: {
//                        String text = getText();
//                        if (text.length() == 0) {
//                            handleReadError();
//                        }
//                        //changing to contains as sometimes gettting accumulated strings.(temporary fix).
//                        //Need to analyze Issue.
//                        boolean ok = text.contains(EMStringConsts.EM_TEXT_RESPONSE_OK);
//                        if (ok) {
//                            isSuccess = true;
//                            keepRunning = false;
//                        } else {
//                            boolean noop = text.contains(EMStringConsts.EM_COMMAND_TEXT_NOOP);
//                            if(noop) {
//                                updateLastActivityTime();
//                            }
//                        }
//                        break;
//                    }
//                    default:
//                        break;
//                }
//            }
//            if (isSuccess || mRetryCount > 3) {
//                sendTransferComplete(isSuccess);
//            } else {
//                mRetryCount++;
//                sendPIMState = EMSendPIMState.EM_SENDING_ADD_DATA_COMMAND;
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                sendPIMData();
//            }
//        }
//
//
//        boolean sendText(String text) {
//            try {
//                if(!TextUtils.isEmpty(text) && text.startsWith(EMStringConsts.EM_COMMAND_TEXT_ADD_FILE)){
//                    DLog.log(">>send text: " + EMStringConsts.EM_COMMAND_TEXT_ADD_FILE + " " + EMMigrateStatus.getItemTransferStarted(mMetaData.mDataType) + "$$" + mMetaData.mFileType + "$$" + mMetaData.mSize);
//                }else {
//                    DLog.log(">>send text: " + text);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            String finalText = text + "\r\n";
//            try {
//                byte[] utf8String = finalText.getBytes("UTF-8");
//                if (mSocketRawOutputStream != null) {
//                    mSocketRawOutputStream.write(utf8String, 0, utf8String.length);
//                    mSocketRawOutputStream.flush();
//                    return true;
//                }
//            } catch (Exception ex) {
//                DLog.log(">>got exception in sendText: " + ex.getMessage());
//            }
//            return false;
//        }
//
//        private boolean isWhitespace(byte aByte) {
//            boolean whitespace = false;
//
//            switch (aByte) {
//                case ' ':
//                case '\n':
//                case '\r':
//                case 9: // tab
//                    whitespace = true;
//                    break;
//                default:
//                    break;
//            }
//
//            return whitespace;
//        }
//
//        String getText() {
//            String text = "";
//            try {
//                int bytesRead = 0;
//                while(true) {
//                    int thisRead = mSocketRawInputStream.read(mBuffer, bytesRead, mBuffer.length-bytesRead);
//                    //DLog.log("thisRead : "+thisRead);
//                    bytesRead += thisRead;
//                    if (bytesRead > 2 && mBuffer[bytesRead - 2] == '\r'  && mBuffer[bytesRead - 1] == '\n') {
//                        int leadingWhitespaceCharacters = 0;
//                        while ((isWhitespace(mBuffer[leadingWhitespaceCharacters])) && (mBuffer[leadingWhitespaceCharacters] != 0)) {
//                            leadingWhitespaceCharacters++;
//                        }
//                        String string = new String(mBuffer, leadingWhitespaceCharacters, bytesRead - 2 - leadingWhitespaceCharacters);
//                        if (!TextUtils.isEmpty(string) && string.startsWith(EMStringConsts.EM_COMMAND_TEXT_ADD_FILE)) {
//                            DLog.log("Got string: " + EMStringConsts.EM_COMMAND_TEXT_ADD_FILE + " **MetaData**");
//                        } else {
//                            DLog.log(String.format("Got string: %s", string));
//                        }
//                        text = string;
//                        break;
//                    } else if(bytesRead>=mBuffer.length) {
//                        break;
//                    }
//                }
//            }catch (ArrayIndexOutOfBoundsException e){
//                //Ignore
//            }
//            catch (Exception ex) {
//                DLog.log(ex);
//            }
//            //DLog.log("<<got text: ");
//            return text;
//        }
//
//        boolean writeFile(String aFilePath) {
//            FileInfo fileInfo = new FileInfo();
//            fileInfo.mFileType = -1;
//            fileInfo.mFilePath = aFilePath;
//            return writeFile(fileInfo);
//        }
//
//        boolean writeFile(FileInfo aFileInfo) {
//            FileInputStream fileInputStream = null;
//            boolean isFileWrote = false;
//            long totalBytes = 0;
//            try {
//                fileInputStream = new FileInputStream(aFileInfo.mFilePath);
//                if (fileInputStream == null || mSocketRawOutputStream == null) {
//                    DLog.log("stream is null while writing from file");
//                } else {
//                    int len = 0;
//                    long totalFileReadTime = 0;
//                    long writeStart = System.nanoTime();
//                    long fileReadStartTime = System.nanoTime();
//
//                    boolean isLiveUpdateRequired = EMMigrateStatus.isLiveUpdateRequired(aFileInfo.mFileType);
//
//                    long chunkReadBytes = 0;
//
//                    while ((len = fileInputStream.read(mBuffer)) > 0) {
//                        totalFileReadTime += (System.nanoTime() - fileReadStartTime);
//                        //DLog.log("Read bytes from file: " + len);
//                        mSocketRawOutputStream.write(mBuffer, 0, len);
//                        //DLog.log("Wrote bytes to socket: " + len);
//                        totalBytes += len;
//                        fileReadStartTime = System.nanoTime();
//                        updateLastActivityTime();
//
//                        if(isLiveUpdateRequired) {
//                            chunkReadBytes += len;
//
//                            //Update current bytes transferred
//                            if (chunkReadBytes > EMMigrateStatus.LIVE_CHUNK_UPDATE_SIZE) {
//                                EMMigrateStatus.addLiveTransferredSize(aFileInfo.mFileType, chunkReadBytes);
//                                chunkReadBytes = 0;
//                            }
//                        }
//                    }
//
//                    //Add last remaining chunk of file
//                    if(chunkReadBytes != 0) {
//                        EMMigrateStatus.addLiveTransferredSize(aFileInfo.mFileType, chunkReadBytes);
//                    }
//
//                    isFileWrote = true;
//                    DLog.log("Wrote file." + "\ttotalTime in Nanos: " + (System.nanoTime() - writeStart) + "\ttotalFileReadTime in Nanos: " + totalFileReadTime + "\tTotal bytes written: " + totalBytes);
//                }
//            } catch (FileNotFoundException e) {
//                DLog.log("FileNotFoundException : " + aFileInfo.mFilePath + " " + e);
//                mErrorCode = ERROR_CODE_FILE_NOTFOUND;
//            }catch (IOException e){
//                DLog.log("Exception : Total bytes written:" + totalBytes + ", Exception : " + e);
//                if(e.toString().contains("java.io.IOException: read failed")){
//                    mErrorCode = ERROR_CODE_READ_FAILED;
//                }
//            }
//            catch (Exception e) {
//                DLog.log("Exception : Total bytes written:" + totalBytes + ", Exception : " + e);
//            } finally {
//                try {
//                    if (fileInputStream != null)
//                        fileInputStream.close();
//                } catch (Exception ex) {
//                    DLog.log(ex);
//                }
//            }
//            return isFileWrote;
//        }
//
//        long writeFile(long timeTowrite) {
//            long totalBytes = 0;
//            try {
//                new Random().nextBytes(mBuffer);
//                int len = mBuffer.length;
//                long startTime = System.currentTimeMillis();
//                while (timeTowrite > (System.currentTimeMillis() - startTime)) {
//                    mSocketRawOutputStream.write(mBuffer);
//                    totalBytes += len;
//                }
//                mBuffer = new byte[READ_BUFFER_SIZE];
//            } catch (IOException e) {
//                DLog.log(e);
//            }
//            return totalBytes;
//        }
//
//
//
//        private void receiveFilesInThread() {
//            //DLog.log(">>start receiveFilesInThread");
//            keepRunning = true;
//            while (keepRunning) {
//                String text = getText();
//                if (text.length() == 0) {
//                    handleReadError();
//                }
//                if (text.startsWith(EMStringConsts.EM_COMMAND_TEXT_ADD_FILE + " ")) {
//                    FileInfo fileInfo = null;
//                    Scanner scanner = null;
//                    try {
//                        scanner = new Scanner(text);
//                        String cmd = scanner.next();
//                        String metasize = scanner.next();
//                        String prefix = cmd + " " + metasize + " ";
//                        String metadata = text.substring(prefix.length());
//                        InputStream inputStream = null;
//                        try {
//                            inputStream = new ByteArrayInputStream(metadata.getBytes("UTF-8"));
//                        } catch (Exception ex) {
//                            DLog.log(ex);
//                        }
//
//                        fileInfo = parseFileInfo(inputStream);
//                        DLog.log("read Metadata file");
//                        scanner.close();
//                    } catch (Exception e) {
//                        //
//                    }finally {
//                        if(scanner!=null) {
//                            scanner.close();
//                        }
//                    }
//                    if (fileInfo != null) {
//                        receiveFile(fileInfo);
//                    } else {
//                        keepRunning = false;
//                    }
//                } else if (text.startsWith(EMStringConsts.EM_COMMAND_TEXT_ADD_DATA + " ")) {
//                    String[] commandAndParameters = text.split(" ");
//                    if (commandAndParameters.length > 1) {
//                        try {
//                            mCurrentDataType = commandAndParameters[1];
//                            long metaDataSize = Long.parseLong(commandAndParameters[2]);
//                            receivePIMState = EMReceivePIMState.EM_SENDING_INITIAL_OK;
//                            receivePIMFile(metaDataSize);
//                        } catch (Exception ex) {
//                            //
//                        }
//                    }
//                }
//                // Logging from Android device when IOS as Destination
//                else if (text.contains(EMStringConsts.EM_COMMAND_UPDATE_DB_STATUS_INPROGRESS)) {
//                    int index = text.indexOf(':');
//                    String jsonString = text.substring(index + 1);
//                    startServerCall(jsonString,true);
//                } else if (text.contains(EMStringConsts.EM_COMMAND_UPDATE_DB_STATUS_FINAL)) {
//                    try {
//                        int index = text.indexOf(':');
//                        String jsonString = text.substring(index + 1);
//                        JsonReader reader = new JsonReader(new StringReader(jsonString));
//                        reader.setLenient(true);
//                        EDeviceSwitchSession eDeviceSwitchSession = new Gson().fromJson(reader, EDeviceSwitchSession.class);
//                        DashboardLog.getInstance().seteDeviceSwitchSession(eDeviceSwitchSession);
//                        DashboardLog.getInstance().setUpdateDBdetails(true);
//                        startServerCall(jsonString, false);
//                    } catch (Exception e) {
//                        DLog.log(e);
//                    }
//                    sendText(EMStringConsts.EM_COMMAND_SERVER_RESPONSE + ":" + EMStringConsts.EM_TEXT_RESPONSE_OK);
//                }
//
//                else if(text.equalsIgnoreCase(EMStringConsts.EM_COMMAND_START_ESTIMATION)){
//                    receiveDataForEstimation();
//                }
//                else if (text.contains(EMStringConsts.EM_COMMAND_RESTORE_COMPLETED)) {
//                    int index = text.indexOf(':');
//                    String dataType = text.substring(index + 1);
//                    String backupFile = MigrationStats.getInstance().getPimBackupFile(dataType);
//                    try {
//                        File file = new File(backupFile);
//                        if (file.exists())
//                            file.delete();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }finally {
//                        MigrationStats.getInstance().removeBackupFile(backupFile);
//                    }
//                    boolean sent = sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
//                    if(sent) {
//                        mDataCompleteDelegate.commandComplete(dataType, true);
//                        keepRunning = false;
//                    }
//                }
//            }
//            //DLog.log("<<end receiveFilesInThread");
//        }
//
//        void handleReadError() {
//            keepRunning = false;
//            DLog.log("handleReadError: making keep running false, to handle the error");
//        }
//
//        private void receiveFile(FileInfo fileInfo) {
//            receiveFileState = EMReceiveFileState.EM_RECEIVE_STATE_NONE;
//            String filePath = null;
//            boolean isSuccess = false;
//            boolean isFileSkipped = false;
//            while (keepRunning) {
//                switch (receiveFileState) {
//                    case EM_RECEIVE_STATE_NONE: {
//                        filePath = getTargetFilePath(fileInfo);
//                        fileInfo.mFilePath = filePath;
//                        if (filePath == null || filePath.length() == 0) {
//                            handleReadError();
//                            break;
//                        }
//                        receiveFileState = EMReceiveFileState.EM_WAITING_FOR_ADD_FILE_XML;
//                        File tempFile = new File(filePath);
//                        if (tempFile.exists()) {
//                            if (tempFile.length() == fileInfo.mFileSize) {
//                                DLog.log("*** The file already exists: >>" + fileInfo.mDataType + " size: " + fileInfo.mFileSize);
//                                EMMigrateStatus.addLiveTransferredSize(fileInfo.mFileType, fileInfo.mFileSize);
//                                boolean ok = sendText(EMStringConsts.EM_TEXT_RESPONSE_ALREADY_EXISTS);
//                                if (!ok) {
//                                    handleReadError();
//                                    break;
//                                }
//                                isSuccess = true;
//                                keepRunning = false;
//                            } else {
//                                try {
//                                    int pos = tempFile.getName().lastIndexOf(".");
//                                    int fileNo = 0;
//                                    String fileExtension = tempFile.getName().substring(pos + 1);
//                                    String fileName = pos > 0 ? tempFile.getName().substring(0, pos) : tempFile.getName();
//                                    File modifiedFile = null;
//                                    do {
//                                        fileNo++;
//                                        String newFileName = fileName + "(" + fileNo + ")." + fileExtension;
//                                        modifiedFile = new File(tempFile.getParent(), newFileName);
//                                        filePath = modifiedFile.getAbsolutePath();
//                                    } while (modifiedFile.exists());
//                                } catch (Exception e) {
//                                    DLog.log("Exception while creating the new file name : " + e.toString());
//                                }
//                                fileInfo.mFilePath = filePath;          //Setting modified file path.
//                            }
//                        }
//                        break;
//                    }
//                    case EM_WAITING_FOR_ADD_FILE_XML: {
//                        receiveFileState = EMReceiveFileState.EM_SENDING_XML_OK;
//                        boolean ok;
//                        if (fileInfo.mFileSize == 0) {
//                            EMMigrateStatus.addItemNotTransferred(fileInfo.mFileType);
//                            CommonUtil.getInstance().setRestoredMediaCount(1);
//                            sendText(EMStringConsts.EM_TEXT_RESPONSE_UNSUPPORTED);
//                            keepRunning = false;
//                            isFileSkipped = true;
//                            break;
//                        } else {
//                            ok = sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
//                        }
//                        if (!ok) {
//                            handleReadError();
//                            break;
//                        }
//                        break;
//                    }
//                    case EM_SENDING_XML_OK: {
//                        receiveFileState = EMReceiveFileState.EM_WAITING_FOR_RAW_FILE_DATA;
//                        DLog.log("start reading " + fileInfo.mDataType + " file of size: " + fileInfo.mFileSize);
//                        boolean ok = readToFile(fileInfo, fileInfo.mFileSize);
//                        if (!ok) {
//                            DLog.log("Error while reading " +fileInfo.mDataType + " file of size: " + fileInfo.mFileSize);
//                            handleReadError();
//                            break;
//                        }
//                        DLog.log("read file: " + fileInfo.mDataType + " of size: " + fileInfo.mFileSize);
//                        break;
//                    }
//                    case EM_WAITING_FOR_RAW_FILE_DATA: {
//                        receiveFileState = EMReceiveFileState.EM_SENDING_FINAL_OK;
//                        boolean ok = sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
//                        if (!ok) {
//                            handleReadError();
//                        }
//                        DLog.log("received file: " + fileInfo.mDataType + " of size: " + fileInfo.mFileSize);
//                        break;
//                    }
//                    case EM_SENDING_FINAL_OK: {
//                        isSuccess = true;
//                        keepRunning = false;
//                        break;
//                    }
//                    default: {
//                        break;
//                    }
//                }
//            }
//            if (isSuccess) {
//                receivedAFile(fileInfo);
//                keepRunning = true;//for next file
//            }else if(isFileSkipped){
//                keepRunning = true ; //for next file
//            }
//        }
//
//
//        private void receivePIMFile(long aMetaDataSize) {
//            keepRunning = true;
//            boolean isSuccess = false;
//            while (keepRunning) {
//                switch (receivePIMState) {
//                    case EM_SENDING_INITIAL_OK: {
//                        DLog.log(">>PIM ["+mCurrentDataType+ "] OK");
//                        boolean ok = sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
//                        if (!ok) {
//                            handleReadError();
//                        }
//                        receivePIMState = EMReceivePIMState.EM_WAITING_FOR_RAW_FILE_DATA;
//                        break;
//                    }
//                    case EM_WAITING_FOR_RAW_FILE_DATA: {
//                        String metadataFilePath = EMUtility.temporaryFileName();
//                        boolean fileOk = readToFile(metadataFilePath, aMetaDataSize);
//                        if (!fileOk) {
//                            handleReadError();
//                            break;
//                        }
//                        DLog.log(">>Read PIM["+mCurrentDataType+ "] file "+metadataFilePath);
//                        //receivePIMState = EMReceivePIMState.EM_PROCESSING_RECEIVED_DATA;
//                        receivePIMState=EMReceivePIMState.EM_SENDING_FINAL_OK;
//                        processPIMXML(metadataFilePath);
//                        break;
//                    }
//                    case EM_PROCESSING_RECEIVED_DATA: {
//                        sendText(EMStringConsts.EM_COMMAND_TEXT_NOOP);
//                        try {
//                            Thread.sleep(2000);
//                        } catch (Exception e) {
//                            DLog.log(e.getMessage());
//                        }
//                        break;
//                    }
//                    case EM_SENDING_FINAL_OK: {
//                        boolean ok = sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
//                        if (ok) {
//                            isSuccess = true;
//                            keepRunning = false;
//                        }
//                        break;
//                    }
//                    default: {
//                        break;
//                    }
//                }
//            }
//            sendTransferComplete(isSuccess);
//        }
//
//        boolean readToFile(String aFilePath, long size) {
//            FileInfo fileInfo = new FileInfo();
//            fileInfo.mFileType = -1;
//            fileInfo.mFilePath = aFilePath;
//            return readToFile(fileInfo, size);
//        }
//
//        boolean readToFile(FileInfo fileInfo, long size) {
//            FileOutputStream fileOutputStream = null;
//            long totalBytes = 0;
//
//            boolean isFileRead = false;
//            boolean isLiveUpdateRequired = EMMigrateStatus.isLiveUpdateRequired(fileInfo.mFileType);
//
//            try {
//                fileOutputStream = new FileOutputStream(fileInfo.mFilePath);
//                if (fileOutputStream == null || mSocketRawInputStream == null) {
//                    DLog.log("stream is null while reading data");
//                } else {
//                    int len = 0;
//                    long totalFileWriteTime = 0;
//                    long readStart = System.nanoTime();
//                    long chunkReadBytes = 0;
//                    if (size > 0) {
//                        while ((len = mSocketRawInputStream.read(mBuffer)) > 0) {
//                            //DLog.log("Read bytes from socket: " + len);
//                            long fileWriteStartTime = System.nanoTime();
//                            fileOutputStream.write(mBuffer, 0, len);
//                            //DLog.log("Wrote bytes to file: " + len);
//                            totalFileWriteTime += (System.nanoTime() - fileWriteStartTime);
//                            totalBytes += len;
//
//                            if(isLiveUpdateRequired) {
//                                chunkReadBytes += len;
//
//                                //Update current bytes transferred
//                                if (chunkReadBytes >= EMMigrateStatus.LIVE_CHUNK_UPDATE_SIZE) {
//                                    EMMigrateStatus.addLiveTransferredSize(fileInfo.mFileType, chunkReadBytes);
//                                    chunkReadBytes = 0;
//                                }
//                            }
//
//                            if (totalBytes >= size) {
//                                if(chunkReadBytes != 0) {
//                                    EMMigrateStatus.addLiveTransferredSize(fileInfo.mFileType, chunkReadBytes);
//                                }
//                                isFileRead = true;
//                                break;
//                            }
//                        }
//                        DLog.log("totalTime in Nanos: " + (System.nanoTime() - readStart)
//                                + "\ttotalFileWriteTime in Nanos: " + totalFileWriteTime
//                                + "\tTotal bytes read: " + totalBytes);
//                    }
//                }
//            } catch (Exception e) {
//                DLog.log("Exception : Total bytes read: " + totalBytes + ", Exception : " + e);
//            } finally {
//                try {
//                    fileOutputStream.close();
//                } catch (Exception ex) {
//                    //No need to handle
//                }
//            }
//            return isFileRead;
//        }
//
//        private void generateMetaData() {
//            try {
//
//                EMXmlGenerator xmlGenerator = new EMXmlGenerator();
//                xmlGenerator.startDocument();
//
//                File file = new File(mMetaData.mSourceFilePath);
//                // TODO: check file exists
//
//                // TODO: later we need to say what type of file?
//                xmlGenerator.startElement(EMStringConsts.EM_XML_FILE);
//
//                {
//                    xmlGenerator.startElement(EMStringConsts.EM_XML_FILE_TYPE);
//                    String fileType = "";
//                    switch (mMetaData.mDataType) {
//                        case EMDataType.EM_DATA_TYPE_PHOTOS:
//                            fileType = EMStringConsts.EM_XML_PHOTOS;
//                            break;
//                        case EMDataType.EM_DATA_TYPE_VIDEO:
//                            fileType = EMStringConsts.EM_XML_VIDEO;
//                            break;
//                        case EMDataType.EM_DATA_TYPE_MUSIC:
//                            fileType = EMStringConsts.EM_XML_MUSIC;
//                            break;
//                        case EMDataType.EM_DATA_TYPE_DOCUMENTS:
//                            fileType = EMStringConsts.EM_XML_DOCUMENTS;
//                            break;
//                        case EMDataType.EM_DATA_TYPE_APP:
//                            fileType = EMStringConsts.EM_XML_APP;
//                            break;
//                        default:
//                            // TODO: unknown file type
//                            break;
//                    }
//                    xmlGenerator.writeText(fileType);
//                    mMetaData.mFileType = fileType;
//                    xmlGenerator.endElement(EMStringConsts.EM_XML_FILE_TYPE);
//
//                    xmlGenerator.startElement(EMStringConsts.EM_XML_FILE_SIZE);
//                    xmlGenerator.writeText(String.valueOf(file.length()));
//                    xmlGenerator.endElement(EMStringConsts.EM_XML_FILE_SIZE);
//
//                    xmlGenerator.startElement(EMStringConsts.EM_XML_FILE_NAME);
//                    xmlGenerator.writeText(mMetaData.mFileName);
//                    xmlGenerator.endElement(EMStringConsts.EM_XML_FILE_NAME);
//
//                   /* xmlGenerator.startElement(EMStringConsts.EM_XML_FILE_TOTAL_MEDIA_SIZE);
//                    xmlGenerator.writeText(String.valueOf(mMetaData.mTotalMediaSize));
//                    xmlGenerator.endElement(EMStringConsts.EM_XML_FILE_TOTAL_MEDIA_SIZE);*/  //stopping as we are not using this filed.
//
//                    if (mMetaData.mRelativePath != null && !mMetaData.mRelativePath.equalsIgnoreCase("")) {
//                        xmlGenerator.startElement(EMStringConsts.EM_XML_RELATIVE_PATH);
//                        xmlGenerator.writeText(mMetaData.mRelativePath);
//                        xmlGenerator.endElement(EMStringConsts.EM_XML_RELATIVE_PATH);
//                    }
//                    if(mMetaData.retry){
//                        xmlGenerator.startElement(EMStringConsts.EM_XML_RETRY);
//                        xmlGenerator.writeText(String.valueOf(mMetaData.retry));
//                        xmlGenerator.endElement(EMStringConsts.EM_XML_RETRY);
//                    }
//                }
//
//                xmlGenerator.endElement(EMStringConsts.EM_XML_FILE);
//
//                mGeneratedMetadataXmlFilePath = xmlGenerator.endDocument();
//            } catch (Exception ex) {
//                // TODO: handle error
//            }
//        }
//
//        private String getTargetFilePath(FileInfo aFileInfo) {
//            String filePath = null; // The path of the file to be created on the file system
//
//            // Determine the target file name
//            if (aFileInfo.mRelativePath != null && !aFileInfo.mRelativePath.equalsIgnoreCase("")) {
//                try {
//                    File completeFilePath = new File(Environment.getExternalStorageDirectory().toString() + "/" + aFileInfo.mRelativePath);
//                    File filePathDirectory = new File(completeFilePath.getParent());
//                    if (!filePathDirectory.exists()) {
//                        DLog.log("Trying to create file Directory : " + filePathDirectory);
//                        filePathDirectory.mkdirs();
//                    }
//                    if (filePathDirectory.exists()) {
//                        filePath = completeFilePath.getAbsolutePath();
//                    } else {
//                        DLog.log("Failed to create Directory : " + filePathDirectory);
//                        filePath = null;
//                    }
//                } catch (Exception e) {
//                    filePath = null;
//                    DLog.log("Exception while creating the directory : " + e.getMessage());
//                }
//            }
//            if(TextUtils.isEmpty(filePath)) {
//                filePath = Environment.getExternalStorageDirectory().toString() + "/";
//                if (aFileInfo.mFileType == EMDataType.EM_DATA_TYPE_PHOTOS) {
//                    filePath += Environment.DIRECTORY_PICTURES + "/" + aFileInfo.mFileName;
//                } else if (aFileInfo.mFileType == EMDataType.EM_DATA_TYPE_VIDEO) {
//                    filePath += Environment.DIRECTORY_MOVIES + "/" + aFileInfo.mFileName;
//                } else if (aFileInfo.mFileType == EMDataType.EM_DATA_TYPE_MUSIC) {
//                    filePath += Environment.DIRECTORY_MUSIC + "/" + aFileInfo.mFileName;
//                }else if (aFileInfo.mFileType == EMDataType.EM_DATA_TYPE_DOCUMENTS) {
//                    filePath += Environment.DIRECTORY_DOCUMENTS + "/" + aFileInfo.mFileName;
//                } else if (aFileInfo.mFileType == EMDataType.EM_DATA_TYPE_APP) {
//                    filePath += Constants.APP_MIGRATION_DIRECTORY + "/" + aFileInfo.mFileName;
//                    AppMigrateUtils.addRestoreApp(filePath);
//                } else {
//                    filePath += Constants.DOCUMENTS_MIGRATION_DIRECTORY + "/" + aFileInfo.mFileName;
//                }
//            }
//            if (!aFileInfo.retry)
//                EMMigrateStatus.addItemTransferStarted(aFileInfo.mFileType);
//
//            if (filePath != null) {
//                File file = new File(filePath);
//                file.getParentFile().mkdirs();
//            }
//
//            // TODO: check that the filename doesn't already exist (if it does then add a number before the extension to prevent it being overwritten)
//
//            return filePath;
//        }
//
//        private FileInfo parseFileInfo(InputStream inputStream) {
//            // TODO: parsing this in the main thread - not great, but it should be very fast as we already have the file and it's only a few lines long
//
//            FileInfo fileInfo = null;
//
//            // Initialize the parser
//            XmlPullParser xmlParser = Xml.newPullParser();
//
//            try {
//                xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
//                xmlParser.setInput(new InputStreamReader(inputStream));
//                xmlParser.nextTag();
//
//                int xmlLevel = 1;
//
//                String text = "";
//
//                // Parse XML file - currently very, very basic
//                while (xmlParser.next() != XmlPullParser.END_DOCUMENT) {
//
//                    String tagName = xmlParser.getName();
//
//                    if (xmlParser.getEventType() == XmlPullParser.START_TAG) {
//
//                        if (xmlLevel == 1) {
//                            fileInfo = new FileInfo();
//                        }
//
//                        xmlLevel++;
//                    }
//
//                    if (xmlParser.getEventType() == XmlPullParser.TEXT) {
//                        text = xmlParser.getText();
//                    }
//
//                    if (xmlParser.getEventType() == XmlPullParser.END_TAG) {
//                        xmlLevel--;
//
//                        if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_ROOT)) {
//                            break;
//                        }
//
//                        if (xmlLevel == 2) {
//                            if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_FILE_SIZE)) {
//                                fileInfo.mFileSize = Long.valueOf(text);
//                            } else if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_FILE_NAME)) {
//                                fileInfo.mFileName = text;
//                            } else if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_FILE_TOTAL_MEDIA_SIZE)) {
//                                fileInfo.mTotalMediaSize = Long.valueOf(text);
//                            } else if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_RELATIVE_PATH)) {
//                                fileInfo.mRelativePath = text;
//                            } else if(tagName.equalsIgnoreCase(EMStringConsts.EM_XML_RETRY)){
//                                fileInfo.retry = Boolean.parseBoolean(text);
//                            } else if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_FILE_TYPE)) {
//                                fileInfo.mDataType = text;
//                                if (text.equalsIgnoreCase(EMStringConsts.EM_XML_PHOTOS)) {
//                                    fileInfo.mFileType = EMDataType.EM_DATA_TYPE_PHOTOS;
//                                } else if (text.equalsIgnoreCase(EMStringConsts.EM_XML_VIDEO)) {
//                                    fileInfo.mFileType = EMDataType.EM_DATA_TYPE_VIDEO;
//                                } else if (text.equalsIgnoreCase(EMStringConsts.EM_XML_DOCUMENTS)) {
//                                    fileInfo.mFileType = EMDataType.EM_DATA_TYPE_DOCUMENTS;
//                                } else if (text.equalsIgnoreCase(EMStringConsts.EM_XML_MUSIC)) {
//                                    fileInfo.mFileType = EMDataType.EM_DATA_TYPE_MUSIC;
//                                } else if (text.equalsIgnoreCase(EMStringConsts.EM_XML_APP)) {
//                                    fileInfo.mFileType = EMDataType.EM_DATA_TYPE_APP;
//                                }
//
//                                if (fileInfo.mFileType != EMDataType.EM_DATA_TYPE_NOT_SET) {
//                                    EMProgressInfo progressInfo = new EMProgressInfo();
//                                    progressInfo.mDataType = fileInfo.mFileType;
//                                    progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_RECEIVING_DATA;
//                                    progressInfo.mFileSize = fileInfo.mFileSize;
//                                    progressInfo.mTotalMediaSize = fileInfo.mTotalMediaSize;
//                                    progressInfo.mCurrentItemNumber=EMMigrateStatus.getItemsTransferred(fileInfo.mFileType)+1;
//                                    sendProgressUpdate(progressInfo);
//                                }
//                            }
//                        }
//                    }
//                }
//            } catch (Exception ex) {
//                ex.printStackTrace();
//                DLog.log(ex);
//            }
//
//            try {
//                inputStream.close();
//            } catch (Exception e) {
//                // Ignore
//            }
//
//            return fileInfo;
//        }
//
//    }
//
//    void writePIMXml() {
//        DLog.log("Generating PIMData XML " + mCurrentDataType);
//        if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CONTACTS)) {
//            mGenerateDataTask = new EMGenerateContactsXmlAsyncTask();
//        } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CALENDAR)) {
//            mGenerateDataTask = new EMGenerateCalendarXmlAsyncTask();
//        } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_SMS_MESSAGES)) {
//            mGenerateDataTask = new EMGenerateSmsMessagesXmlAsyncTask();
//        } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CALL_LOGS)) {
//            mGenerateDataThread = new EMGenerateCallLogsBackupTask();
//        }else if(mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_SETTINGS)){
//            mGenerateDataThread = new EMGenerateSettingsBackupTask();
//        }
//        if (mGenerateDataTask != null) {
//            mGenerateDataTask.setCommandDelegate(mCommandDelegate);
//            CommonUtil.getInstance().setRunningAsyncTask(mGenerateDataTask);
//            mGenerateDataTask.startTask(pimProgressHandler);
//        }else if(mGenerateDataThread!=null){
//            mGenerateDataThread.startTask(pimProgressHandler);
//        }
//    }
//    EMParseDataTask emParseDataTask = null;
//    EMParseDataInThread emParseDataInThread = null;
//
//    void processPIMXML(final String aDataPath) {
//        DLog.log("Parsing PIMData XML " + mCurrentDataType + " " + aDataPath + " " + Thread.currentThread().getId());
//        if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CONTACTS)) {
//            emParseDataInThread = new EMParseContactsXmlAsyncTask();
//            startParsingXML(aDataPath);
//        } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CALENDAR)) {
//            emParseDataInThread = new EMParseCalendarXmlAsyncTask();
//            startParsingXML(aDataPath);
//        }else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_SMS_MESSAGES)) {
//            emParseDataInThread = new EMParseSmsXmlAsyncTask();
//            EasyMigrateActivity.getDefaultSMSAppPermission(new PermissionHandler() {
//                @Override
//                public void userAccepted() {
//                    DLog.log(mCurrentDataType + " User Accepted ");
//                    startParsingXML(aDataPath);
//                }
//                @Override
//                public void userDenied() {
//                    receivePIMState = EMReceivePIMState.EM_SENDING_FINAL_OK;
//                    DLog.log(mCurrentDataType + " User Denied ");
//                    if(mDataCompleteDelegate!=null){
//                        mDataCompleteDelegate.restoreCompleted(mCurrentDataType,false);
//                    }
//                }
//            });
//        }
//        else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CALL_LOGS)) {
//            emParseDataInThread = new EMParseCallLogsXmlInThread();
//            startParsingXML(aDataPath);
//        }else if(mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_SETTINGS)){
//            emParseDataInThread = new EMParseSettingsXmlInThread();
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                EasyMigrateActivity.enableWritePermissionForSettings(new PermissionHandler() {
//                    @Override
//                    public void userAccepted() {
//                        DLog.log(mCurrentDataType + " User enabled ");
//                        startParsingXML(aDataPath);
//                    }
//
//                    @Override
//                    public void userDenied() {
//                        DLog.log(mCurrentDataType + " User denied ");
//                        // Allowing to change settings as some of them not required permission.
//                        /*if (mDataCompleteDelegate != null) {
//                            mDataCompleteDelegate.restoreCompleted(mCurrentDataType, false);
//                        }*/
//                        startParsingXML(aDataPath);
//                    }
//                });
//            } else {
//                startParsingXML(aDataPath);
//            }
//        }
//    }
//
//    private void startParsingXML(String aDataPath){
//        if (emParseDataTask != null) {
//            CommonUtil.getInstance().setRunningAsyncTask(emParseDataTask);
//            emParseDataTask.startTask(aDataPath, true, EMUtility.Context(), pimProgressHandler);
//        }else if(emParseDataInThread != null){
//            emParseDataInThread.startTask(aDataPath, true, EMUtility.Context(), pimProgressHandler);
//        }
//    }
//
//    private EMProgressHandler pimProgressHandler = new EMProgressHandler() {
//        @Override
//        public void taskComplete(boolean aSuccess) {
//            if (aSuccess) {
//                DLog.log(mCurrentDataType + " task Completed");
//                if (receivePIMState == EMReceivePIMState.EM_PROCESSING_RECEIVED_DATA) {
//                    receivePIMState = EMReceivePIMState.EM_SENDING_FINAL_OK;
//                } else if(sendPIMState == EMSendPIMState.EM_WAITING_FOR_DATA_GENERATION){
//                    sendPIMState = EMSendPIMState.EM_SENDING_ADD_DATA_COMMAND;
//                }
//            }
//            if(mDataCompleteDelegate!=null){
//                mDataCompleteDelegate.restoreCompleted(mCurrentDataType,aSuccess);
//            }
//        }
//
//        @Override
//        public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
//            DLog.log(mCurrentDataType + " taskError");
//            if (receivePIMState == EMReceivePIMState.EM_PROCESSING_RECEIVED_DATA)
//                receivePIMState = EMReceivePIMState.EM_SENDING_FINAL_OK;
//        }
//
//        @Override
//        public void progressUpdate(EMProgressInfo aProgressInfo) {
//            if (mDataCommandDelegate != null)
//                mDataCommandDelegate.progressUpdate(aProgressInfo);
//        }
//    };
//
//
//    private void startServerCall(String gsonString, final boolean respondBack){
//        TransactionLogging transactionLogging = new TransactionLogging(LOGGING_API_ENDPOINT,
//                HTTP_AUTHENTICATION_USERNAME + ":" + HTTP_AUTHENTICATION_PASSWORD, gsonString, new ServerCallBacks() {
//            @Override
//            public void gpsLocationGranted(boolean result) {
//                //Ignore
//            }
//
//            @Override
//            public void gpsLocationRequested(boolean requested) {
//                //Ignore
//            }
//
//            @Override
//            public void locationValidation(boolean result, String response) {
//                //Ignore
//            }
//
//            @Override
//            public void storeidValidation(boolean result, String response) {
//                //Ignore
//            }
//
//            @Override
//            public void onserverCallCompleted(String response) {
//                DLog.log("in onserverCallCompleted response:" + response);
//                if(respondBack) {
//                    mWorkerThread.sendText(EMStringConsts.EM_COMMAND_SERVER_RESPONSE + ":" + response);
//                }
//            }
//
//            @Override
//            public void storeidAndRepidValidation(boolean result, String response) {
//
//            }
//
//            @Override
//            public void repLoginValidation(boolean result, String response) {
//
//            }
//
//        });
//        transactionLogging.startServiceCall();
//    }
//
//    private void checkMigrationStatus(){
//       boolean restoreConnection = false;
//        while (Constants.stopMigration) {
//            if (!restoreConnection) {
//                DLog.log("***Migration is paused***");
//            }
//            restoreConnection = true;
//            try {
//                Thread.sleep(1000);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        if (restoreConnection) {
//            reInitiateConnection();
//        }
//    }
//
//    private void  reInitiateConnection(){
//        try {
//            DLog.log("----reInitiateConnection----");
//            if (mSocket != null) {
//                mSocket.close();
//                mSocket = null;
//            }
//        } catch (Exception e) {
//            DLog.log(e.getMessage());
//        }
//        initialiseConnection();
//    }
//
//}

package com.pervacio.wds.app;

import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Xml;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.pervacio.wds.app.ui.EasyMigrateActivity;
import com.pervacio.wds.app.ui.PermissionHandler;
import com.pervacio.wds.custom.appmigration.AppMigrateUtils;
import com.pervacio.wds.custom.asynctask.TransactionLogging;
import com.pervacio.wds.custom.models.EDeviceSwitchSession;
import com.pervacio.wds.custom.models.MigrationStats;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.ServerCallBacks;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.pervacio.wds.custom.utils.Constants.ESTIMATION_TIME_FILESIZE;
import static com.pervacio.wds.custom.utils.Constants.HTTP_AUTHENTICATION_PASSWORD;
import static com.pervacio.wds.custom.utils.Constants.HTTP_AUTHENTICATION_USERNAME;
import static com.pervacio.wds.custom.utils.Constants.IS_MMDS;
import static com.pervacio.wds.custom.utils.Constants.LOGGING_API_ENDPOINT;

/**
 * Created by Shyam on 14-07-2017.
 */

public class EMDataTransferHelper{

    private static final int READ_BUFFER_SIZE = 1024 * 128;

    private InetAddress mHostName = null;
    private int mPort = EMServer.CONTENT_TRANSFER_PORT;
    Socket mSocket = null;
    private InputStream mSocketRawInputStream = null;
    private OutputStream mSocketRawOutputStream = null;
    private byte[] mBuffer = new byte[READ_BUFFER_SIZE];
    private String mCurrentDataType = null;
    private EMGenerateDataTask mGenerateDataTask;
    private EMGenerateDataInThread mGenerateDataThread;
    private int mRetryCount=0;
    private int socketTimeout = 0;


    private EMSendFileState sendFileState = EMSendFileState.EM_SEND_STATE_NONE;
    private EMReceiveFileState receiveFileState = EMReceiveFileState.EM_RECEIVE_STATE_NONE;
    private EMSendPIMState sendPIMState = EMSendPIMState.EM_SEND_STATE_NONE;
    private EMEstimationState estimationState = EMEstimationState.EM_ESTIMATION_STATE_NONE;
    private EMReceivePIMState receivePIMState = EMReceivePIMState.EM_RECEIVE_STATE_NONE;

    private EMSendPIMState restoreCompletedState = EMSendPIMState.EM_SEND_STATE_NONE;

    private EMCommandDelegate mCommandDelegate = null;
    private EMDataCommandDelegate mDataCommandDelegate = null;


    public void setDataCompleteDelegate(EMDataCompleteDelegate mDataCompleteDelegate) {
        this.mDataCompleteDelegate = mDataCompleteDelegate;
    }

    private EMDataCompleteDelegate mDataCompleteDelegate=null;

    private Handler mMainThreadHandler = null;
    private Messenger mMainThreadMessenger = null;

    private Timer mTimeoutTimer = new Timer();
    private long mLastActivityTime = System.currentTimeMillis();
    private static final long TRANSFER_INACTIVE_TIMEOUT = 60*1000L;

    public static final int ERROR_CODE_NONE = -1;
    public static final int ERROR_CODE_SUCESS = 0;
    public static final int ERROR_CODE_FAIL = 1;
    public static final int ERROR_CODE_TIMEOUT = 2;
    public static final int ERROR_CODE_UNSUPPORTED = 3;
    public static final int ERROR_CODE_FILE_NOTFOUND = 4;
    public static final int ERROR_CODE_READ_FAILED = 5;;

    private int mErrorCode = -1;

    private boolean mUpdateProgress =true;

    private enum EMSendFileState {
        EM_SEND_STATE_NONE,
        EM_SENDING_ADD_FILE_COMMAND,
        EM_WAITING_FOR_ADD_FILE_RESPONSE,
        EM_SENDING_ADD_FILE_XML,
        EM_WAITING_FOR_XML_OK,
        EM_SENDING_RAW_FILE_DATA,
        EM_WAITING_FOR_FINAL_OK,
        EM_CANCELLED
    }

    private enum EMReceiveFileState {
        EM_RECEIVE_STATE_NONE,
        EM_SENDING_INITIAL_OK,
        EM_WAITING_FOR_ADD_FILE_XML,
        EM_SENDING_XML_OK,
        EM_WAITING_FOR_RAW_FILE_DATA,
        EM_SENDING_FINAL_OK,
        EM_CANCELLED
    }


    private enum EMSendPIMState {
        EM_SEND_STATE_NONE,
        EM_WAITING_FOR_DATA_GENERATION,
        EM_SENDING_ADD_DATA_COMMAND,
        EM_WAITING_FOR_ADD_DATA_RESPONSE,
        EM_SENDING_DATA,
        EM_WAITING_FOR_FINAL_OK,
        EM_CANCELLED
    }

    private enum EMReceivePIMState {
        EM_RECEIVE_STATE_NONE,
        EM_SENDING_INITIAL_OK,
        EM_WAITING_FOR_RAW_FILE_DATA,
        EM_PROCESSING_RECEIVED_DATA,
        EM_SENDING_FINAL_OK,
        EM_CANCELLED
    }

    private enum EMEstimationState{
        EM_ESTIMATION_STATE_NONE,
        EM_ESTIMATION_WRITE_DATA,
        EM_ESTIMATION_READ_DATA,
        EM_ESTIMATION_STATE_DONE;

    }


    class FileInfo {
        int mFileType = EMDataType.EM_DATA_TYPE_NOT_SET;
        String mDataType = "";
        String mFileName = "unknown";
        String mFilePath = "";
        String mRelativePath = "";
        long mFileSize = -1;
        long mTotalMediaSize = -1;
        boolean retry = false;
    }

    public EMDataTransferHelper(InetAddress aHostName) {
        mHostName = aHostName;
    }

    public EMDataTransferHelper(Socket aSocket) {
        mSocket = aSocket;
    }

    public void setCommandDelegate(EMCommandDelegate aCommandDelegate) {
        mCommandDelegate = aCommandDelegate;
    }

    public void setDataCommandDelegate(EMDataCommandDelegate aDataCommandDelegate) {
        mDataCommandDelegate = aDataCommandDelegate;
    }

    private void progressUpdate(EMProgressInfo aProgressInfo) {
        if (mDataCommandDelegate != null) {
            mDataCommandDelegate.progressUpdate(aProgressInfo);
        }
    }

    public boolean init() {
        mMainThreadHandler = new Handler() {
            public void handleMessage(Message msg) {
                EMFTUResponseMessage message = (EMFTUResponseMessage) msg.obj;
                switch (message.mId) {
                    case EMFTURespSendFileSuccess:
                        mCommandDelegate.commandComplete(true);
                        break;
                    case EMFTURespSendFileError:
                        clean();
                        mCommandDelegate.commandComplete(false);
                        break;
                    case EMFTURespReceivedFile: {
                        FileInfo fileInfo = (FileInfo) message.obj;
                        String[] paths = {fileInfo.mFilePath};
                        EMMigrateStatus.addItemTransferred(fileInfo.mFileType);
                        CommonUtil.getInstance().setRestoredMediaCount(1);
                        EMMigrateStatus.addTransferedFilesSize(fileInfo.mFileType,fileInfo.mFileSize);
                        MediaScannerConnection.scanFile(EMUtility.Context(), paths, null, null);
                        if (EMMigrateStatus.isMediaRestorationCompleted()) {
                            //Once media completes, update to top layer
                            EMProgressInfo progressInfo = new EMProgressInfo();
                            progressInfo.mDataType = EMDataType.EM_DATA_TYPE_MEDIA;
                            progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TEXT_RESTORE_COMPLETED;
                            progressUpdate(progressInfo);
                        }
                        break;
                    }
                    case EMFTURespProgressInfo: {
                        EMProgressInfo progressInfo = (EMProgressInfo) message.obj;
                        progressUpdate(progressInfo);
                        break;
                    }
                }
            }
        };
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        WorkerThreadForSocketConection workerThreadForSocketConection = new WorkerThreadForSocketConection(this);
        Future future = executorService.submit(workerThreadForSocketConection);
        boolean connection = false;
        try {
            if (future.get(15, TimeUnit.SECONDS) == null) {
                connection = true;
            } else
                connection = false;
            DLog.log("Socket connection established : " + connection);
            executorService.shutdownNow();
        } catch (Exception ex) {
            DLog.log("Exception while getting connection status or closing executor : " + ex);
        }

        mMainThreadMessenger = new Messenger(mMainThreadHandler);
        mWorkerThread.start();
        return connection;
    }

    public void clean() {
        try {
            if(mSocket!=null) {
                synchronized (mSocket) {
                    try {
                        mSocketRawInputStream.close();
                    } catch (Exception ex) {
                    } finally {
                        mSocketRawInputStream = null;
                    }
                    try {
                        mSocketRawOutputStream.close();
                    } catch (Exception ex) {
                    } finally {
                        mSocketRawOutputStream = null;
                    }
                    try {
                        mSocket.close();
                    } catch (Exception ex) {
                    } finally {
                        mSocket = null;
                    }
                    try {
                        // TODO: send a quit message to the worker thread
                        mWorkerThread.interrupt();
                    } catch (Exception ex) {
                        DLog.log(ex);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isConnected(){
        boolean isConnected = false;
        try {
            if(mSocket != null && mSocket.isConnected()){
                isConnected = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isConnected;
    }

    public boolean initialiseConnection() {
        try {
            DLog.log("----initialiseConnection----");
            if (mSocket == null || !mSocket.isConnected()) {
                DLog.log("connect to host " + mHostName.toString()+ " on port : "+mPort);
                mSocket = new Socket();
                InetSocketAddress remoteAddr = new InetSocketAddress(mHostName, mPort);
                mSocket.setTcpNoDelay(true);
                if (EMUtility.shouldBindSocketToWifi()) {
                    EMUtility.bindSocketToWiFiNetwork(mSocket);
                }
                mSocket.connect(remoteAddr,socketTimeout);
                if (mSocket.isConnected()) {
                    DLog.log(String.format("***** Socket connected: %s on port %d", mHostName.toString(), mPort));
                }
                else {
                    DLog.log(String.format("***** Socket not connected: %s on port %d", mHostName.toString(), mPort));
                }
            }
            DLog.log("Socket SendBufferSize:" + mSocket.getSendBufferSize());
            DLog.log("Socket ReceiveBufferSize:" + mSocket.getReceiveBufferSize());
            mSocketRawInputStream = mSocket.getInputStream();
            mSocketRawOutputStream = mSocket.getOutputStream();
            return true;
        } catch (Exception ex) {
            DLog.log(ex.getMessage());
        }
        return false;
    }

    /*private boolean reconnect() {
        DLog.log("reconnect for file transfer");
        try {
            if (mSocket != null && !mSocket.isConnected()) {
                DLog.log("start reconnect");
                InetSocketAddress remoteAddr = new InetSocketAddress(mHostName, mPort);
                mSocket.connect(remoteAddr);
                if (mSocket.isConnected())
                    DLog.log(String.format("***** Socket reconnected: %s on port %d", mHostName.toString(), mPort));
                else {
                    DLog.log(String.format("***** Socket not reconnected: %s on port %d", mHostName.toString(), mPort));
                }
            }
            mSocketRawInputStream = mSocket.getInputStream();
            mSocketRawOutputStream = mSocket.getOutputStream();
            return true;
        } catch (Exception ex) {
            DLog.log(ex);
        }
        return false;
    }*/

    private void checkConnection() {
        if (mSocket == null || !mSocket.isConnected()) {
            initialiseConnection();
        }
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    private Handler mTimeoutHandler = new Handler() {
        public void handleMessage(Message msg) {
            mErrorCode = ERROR_CODE_TIMEOUT;
            DLog.log("Timed out");
            try {
                mSocket.close();
                clean();
            }catch (Exception ex) {
                DLog.log(ex);
            }finally {
                mSocket = null;
            }
        }
    };

    private void createNewTimer(final long timeoutInMillis) {
        try {
            mTimeoutTimer = new Timer();
            mTimeoutTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    long currentTimeInMillis = System.currentTimeMillis();
                    long elapsedInMillis = currentTimeInMillis - mLastActivityTime;
                    if( elapsedInMillis > timeoutInMillis) {
                        mTimeoutHandler.obtainMessage(1).sendToTarget();
                    } else {
                        restartTimeoutTimer(timeoutInMillis);
                    }
                }
            }, timeoutInMillis);
        } catch (Exception ex) {
            // If we can't set the timeout timer for some reason then just ignore
        }
    }

    private void restartTimeoutTimer(long timeoutInMillis) {
        stopTimeoutTimer();
        createNewTimer(timeoutInMillis);
    }

    private void stopTimeoutTimer() {
        if (mTimeoutTimer != null) {
            mTimeoutTimer.cancel();
        }
    }

    private void updateLastActivityTime() {
        mLastActivityTime = System.currentTimeMillis();
    }

    private static final int EMFTUReqSendFile = 1;
    private static final int EMFTUReqReceiveFiles = 2;
    private static final int EMFTUReqSendPIM = 3;
    private static final int EMFTUReqEstimation = 4;
    private static final int EMFTUReqRestoreDone = 5;


    class EMFTURequestMessage {
        public int mId;
    }

    private static final int EMFTURespSendFileSuccess = 1;
    private static final int EMFTURespSendFileError = 2;
    private static final int EMFTURespReceivedFile = 3;
    private static final int EMFTURespProgressInfo = 4;

    class EMFTUResponseMessage {
        public int mId;
        public Object obj;
    }

    public void cancel() {
        mWorkerThread.cancelOperation();
    }

    public boolean sendFile(EMFileMetaData aFileMetaData) {
        mMetaData = aFileMetaData;
        Message msg = new Message();
        EMFTURequestMessage message = new EMFTURequestMessage();
        message.mId = EMFTUReqSendFile;
        msg.obj = message;
        mWorkerThread.getHandler().sendMessage(msg);
        return true;
    }

    public boolean startReceivingFiles() {
        Message msg = new Message();
        EMFTURequestMessage message = new EMFTURequestMessage();
        message.mId = EMFTUReqReceiveFiles;
        msg.obj = message;
        mWorkerThread.getHandler().sendMessage(msg);
        return true;
    }

    public boolean sendPIM(String dataType) {
        mCurrentDataType = dataType;
        Message msg = new Message();
        EMFTURequestMessage message = new EMFTURequestMessage();
        message.mId = EMFTUReqSendPIM;
        msg.obj = message;
        mWorkerThread.getHandler().sendMessage(msg);
        return true;
    }


    public boolean startEstimation() {
        socketTimeout = 10*1000;
        Message msg = new Message();
        EMFTURequestMessage message = new EMFTURequestMessage();
        message.mId = EMFTUReqEstimation;
        msg.obj = message;
        mWorkerThread.getHandler().sendMessage(msg);
        return true;
    }

    public boolean sendRestoreCompleted(String dataType){
        socketTimeout = 10*1000;
        mCurrentDataType = dataType;
        Message msg = new Message();
        EMFTURequestMessage message = new EMFTURequestMessage();
        message.mId = EMFTUReqRestoreDone;
        msg.obj = message;
        mWorkerThread.getHandler().sendMessage(msg);
        return true;
    }

    private EMFileMetaData mMetaData = null;
    private String mGeneratedMetadataXmlFilePath = null;

    EMDataTransferThread mWorkerThread = new EMDataTransferThread();

    class EMDataTransferThread extends Thread {
        Handler mInThreadHandler;
        boolean keepRunning = false;
        boolean mCancel = false;

        synchronized Handler getHandler() {
            while (mInThreadHandler == null) {
                try {
                    wait();
                } catch (Exception e) {
                    DLog.log(e);
                }
            }

            return mInThreadHandler;
        }

        @Override
        public void run() {
            DLog.log(">>EMDataTransferThread");
            Looper.prepare();

            synchronized (this) {

                mInThreadHandler = new Handler() {
                    public void handleMessage(Message msg) {
                        EMFTURequestMessage message = (EMFTURequestMessage) msg.obj;
                        checkConnection();
                        switch (message.mId) {
                            case EMFTUReqSendFile:
                                sendFileInThread();
                                break;
                            case EMFTUReqReceiveFiles:
                                receiveFilesInThread();
                                break;
                            case EMFTUReqSendPIM:
                                sendPIMData();
                                break;
                            case EMFTUReqEstimation:
                                sendDataForEstimation();
                                break;
                            case EMFTUReqRestoreDone:
                                sendRestoreCompleted();
                                break;
                            default:
                                break;
                        }

                    }
                };
                notifyAll();
            }
            Looper.loop();
        }

        public void cancelOperation() {
            mCancel = true;
            //TODO - cancel the ongoing peration
        }

        private void sendTransferComplete(boolean aSuccess) {
            stopTimeoutTimer();
            Message msg = new Message();
            EMFTUResponseMessage resp = new EMFTUResponseMessage();
            if (aSuccess) {
                resp.mId = EMFTURespSendFileSuccess;
            } else {
                resp.mId = EMFTURespSendFileError;
            }
            msg.obj = resp;
            try {
                mMainThreadMessenger.send(msg);
            } catch (RemoteException e1) {
                e1.printStackTrace();
                DLog.log(e1);
            }
        }

        private void sendProgressUpdate(EMProgressInfo aProgressInfo) {
            Message msg = new Message();
            EMFTUResponseMessage resp = new EMFTUResponseMessage();
            resp.mId = EMFTURespProgressInfo;
            resp.obj = aProgressInfo;
            msg.obj = resp;
            try {
                mMainThreadMessenger.send(msg);
            } catch (RemoteException e1) {
                e1.printStackTrace();
                DLog.log(e1);
            }
        }

        private void receivedAFile(FileInfo aFileInfo) {
            Message msg = new Message();
            EMFTUResponseMessage resp = new EMFTUResponseMessage();
            resp.mId = EMFTURespReceivedFile;
            resp.obj = aFileInfo;
            msg.obj = resp;
            try {
                mMainThreadMessenger.send(msg);
            } catch (RemoteException e1) {
                e1.printStackTrace();
                DLog.log(e1);
            }
        }

        private void sendFileInThread() {
            checkMigrationStatus();
            keepRunning = true;
            sendFileState = EMSendFileState.EM_SEND_STATE_NONE;
            boolean isSuccess = false;
            mErrorCode = -1; // resetting Error code before sending a new file.
            while (keepRunning) {
                updateLastActivityTime();
                switch (sendFileState) {
                    case EM_SEND_STATE_NONE: {
                        createNewTimer(TRANSFER_INACTIVE_TIMEOUT);
                        generateMetaData();
                        File metadataFile = new File(mGeneratedMetadataXmlFilePath);
                        sendFileState = EMSendFileState.EM_SENDING_ADD_FILE_COMMAND;
                        DLog.log(">>trying to send " + mMetaData.mFileType + " of size: " + mMetaData.mSize);
                        String metadataXML = "";
                        try {
                            byte[] metadataXMLArr = EMUtility.readFileToByteArray(metadataFile);
                            metadataXML = new String(metadataXMLArr);
                        } catch (Exception e) {
                            DLog.log(e);
                        }
                        boolean isMetaDataSent = sendText(EMStringConsts.EM_COMMAND_TEXT_ADD_FILE + " " + Long.toString(metadataFile.length()) + " " + metadataXML);
                        if (!isMetaDataSent) {
                            keepRunning = false;
                            DLog.log("error while sending the meta data");
                        }
                        try {
                            metadataFile.delete();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case EM_SENDING_ADD_FILE_COMMAND: {
                        sendFileState = EMSendFileState.EM_WAITING_FOR_XML_OK;
                        String text = getText();
                        if (text.equalsIgnoreCase(EMStringConsts.EM_TEXT_RESPONSE_ALREADY_EXISTS)) {
                            EMMigrateStatus.addLiveTransferredSize(mMetaData.mDataType, mMetaData.mSize);
                            keepRunning = false;
                            isSuccess = true;
                            break;
                        } else if (text.equalsIgnoreCase(EMStringConsts.EM_TEXT_RESPONSE_UNSUPPORTED)) {
                            mErrorCode = ERROR_CODE_UNSUPPORTED;
                            keepRunning = false;
                            break;
                        }
                        boolean ok = text.equals(EMStringConsts.EM_TEXT_RESPONSE_OK);
                        if (!ok) {
                            keepRunning = false;
                        }
                        break;
                    }
                    case EM_WAITING_FOR_XML_OK: {
                        sendFileState = EMSendFileState.EM_SENDING_RAW_FILE_DATA;
                        DLog.log("start writing " + mMetaData.mFileType + " of size: " + mMetaData.mSize);
                        FileInfo writeFileInfo = new FileInfo();
                        writeFileInfo.mFileType = mMetaData.mDataType;
                        writeFileInfo.mFilePath = mMetaData.mSourceFilePath;
                        boolean fileSentOk = writeFile(writeFileInfo);
                        if (!fileSentOk) {
                            keepRunning = false;
                        }
                        //DLog.log("wrote file");
                        break;
                    }
                    case EM_SENDING_RAW_FILE_DATA: {
                        String text = getText();
                        sendFileState = EMSendFileState.EM_WAITING_FOR_FINAL_OK;
                        if (text.equalsIgnoreCase(EMStringConsts.EM_TEXT_RESPONSE_UNSUPPORTED)) {
                            mErrorCode = ERROR_CODE_UNSUPPORTED;
                            keepRunning = false;
                        }else if (!text.equals(EMStringConsts.EM_TEXT_RESPONSE_OK)) {
                            keepRunning = false;
                        }
                        break;
                    }
                    case EM_WAITING_FOR_FINAL_OK: {
                        isSuccess = true;
                        keepRunning = false;
                        break;
                    }
                }
            }
            sendTransferComplete(isSuccess);
        }


        private void sendDataForEstimation() {
            keepRunning = true;
            boolean isSuccess = false;
            while (keepRunning) {
                switch (estimationState) {
                    case EM_ESTIMATION_STATE_NONE:
                        DLog.log("Sending start estimation command ");
                        boolean sent = sendText(EMStringConsts.EM_COMMAND_START_ESTIMATION);
                        if (sent) {
                            String ok = getText();
                            estimationState = EMEstimationState.EM_ESTIMATION_WRITE_DATA;
                        }else {
                            DLog.log("some thing went wrong,making keep running false");
                            handleReadError();
                        }
                        break;
                    case EM_ESTIMATION_WRITE_DATA:
                        long totalBytesWritten = writeFile(Constants.ESTIMATION_CALCULATION_TIME);
                        isSuccess = true;
                        mCommandDelegate.addToPreviouslyTransferredItems(String.valueOf(totalBytesWritten));
                        if(isSuccess){
                            estimationState = EMEstimationState.EM_ESTIMATION_STATE_DONE;
                        }else {
                            handleReadError();
                        }
                        break;
                    case EM_ESTIMATION_STATE_DONE:
                        // String ok = getText();
                        keepRunning = false;
                        isSuccess = true;
                        break;
                    default:
                        break;
                }
            }
            sendTransferComplete(isSuccess);
            clean();
        }

        private void receiveDataForEstimation() {
            keepRunning = true;
            boolean isSuccess = false;
            while (keepRunning) {
                switch (estimationState) {
                    case EM_ESTIMATION_STATE_NONE:
                        boolean sent = sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
                        if (sent) {
                            estimationState = EMEstimationState.EM_ESTIMATION_READ_DATA;
                        }
                        break;
                    case EM_ESTIMATION_READ_DATA:
                        String path = EMUtility.temporaryFileName();
                        boolean readFile = readToFile(path, ESTIMATION_TIME_FILESIZE*100);
                        if (readFile)
                            estimationState = EMEstimationState.EM_ESTIMATION_STATE_DONE;
                        else
                            keepRunning = false;
                        try {
                            new File(path).delete();
                        } catch (Exception ex) {
                            DLog.log(ex.getMessage());
                        }
                        break;
                    case EM_ESTIMATION_STATE_DONE:
                        //sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
                        keepRunning = false;
                        isSuccess = true;
                        break;
                    default:
                        break;
                }
            }
            sendTransferComplete(isSuccess);
            clean();
        }

        private void sendRestoreCompleted() {
            keepRunning = true;
            restoreCompletedState = EMSendPIMState.EM_SEND_STATE_NONE;
            boolean isSuccess = false;
            checkMigrationStatus();
            while (keepRunning) {
                switch (restoreCompletedState) {
                    case EM_SEND_STATE_NONE: {
                        if(IS_MMDS && !"Android".equalsIgnoreCase(EasyMigrateActivity.remotePlatform) && !EasyMigrateActivity.iosSMSRestored){
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                        DLog.log("RESTORE COMPLETED FOR "+ ":" + mCurrentDataType);
                        boolean sendText = sendText(EMStringConsts.EM_COMMAND_RESTORE_COMPLETED + ":" + mCurrentDataType);
                        if (sendText) {
                            restoreCompletedState = EMSendPIMState.EM_WAITING_FOR_FINAL_OK;
                        } else {
                            if(mRetryCount<3) {
                                checkMigrationStatus();
                                if(!isConnected()){
                                    initialiseConnection();
                                }
                                mRetryCount++;
                                DLog.log("sendRestoreTryCount = "+mRetryCount);
                                try {
                                    Thread.sleep(3000);
                                } catch (Exception e) {
                                    DLog.log(e.getMessage());
                                }
                                restoreCompletedState = EMSendPIMState.EM_SEND_STATE_NONE;
                                break;
                            } else {
                                handleReadError();
                                DLog.log("sendRestore Failed after the count= "+mRetryCount);
                            }
                        }
                        EMProgressInfo progressInfo = new EMProgressInfo();
                        progressInfo.mDataType = EMStringConsts.DATATYPE_MAP.get(mCurrentDataType);
                        progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TEXT_RESTORE_COMPLETED;
                        sendProgressUpdate(progressInfo);
                        break;
                    }
                    case EM_WAITING_FOR_FINAL_OK: {
                        String text = getText();
                        // At this point treat migration as success irrespective of return values
                        isSuccess = true;
                        keepRunning = false;
                        break;
                    }
                    default:
                        break;
                }
            }
            sendTransferComplete(isSuccess);
        }


        private void sendPIMData() {
            keepRunning = true;
            sendPIMState = EMSendPIMState.EM_SEND_STATE_NONE;
            boolean isSuccess = false;
            while (keepRunning) {
                updateLastActivityTime();
                switch (sendPIMState) {
                    case EM_SEND_STATE_NONE: {
                        mGeneratedMetadataXmlFilePath = MigrationStats.getInstance().getPimBackupFile(mCurrentDataType);
                        if (mGeneratedMetadataXmlFilePath != null && new File(mGeneratedMetadataXmlFilePath).exists()) {
                            sendPIMState = EMSendPIMState.EM_SENDING_ADD_DATA_COMMAND;
                        } else {
                            writePIMXml();
                            sendPIMState = EMSendPIMState.EM_WAITING_FOR_DATA_GENERATION;
                        }
                        break;
                    }
                    case EM_WAITING_FOR_DATA_GENERATION:
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case EM_SENDING_ADD_DATA_COMMAND: {
                        String filePath ;
                        checkMigrationStatus();
                        reInitiateConnection();
                        if (mGeneratedMetadataXmlFilePath != null) {
                            filePath = mGeneratedMetadataXmlFilePath;
                        } else if (mGenerateDataTask != null) {
                            filePath = mGenerateDataTask.getFilePath();
                        } else {
                            filePath = mGenerateDataThread.getFilePath();
                        }
                        MigrationStats.getInstance().addToBackupList(mCurrentDataType,filePath);
                        File fileInfo = new File(filePath);
                        long fileSize = fileInfo.length();
                        String dataSizeString = Long.toString(fileSize);
                        String commandString = EMStringConsts.EM_COMMAND_TEXT_ADD_DATA + " " + mCurrentDataType + " " + dataSizeString;
                        boolean ok = sendText(commandString);
                        if (!ok) {
                            handleReadError();
                        }
                        sendPIMState = EMSendPIMState.EM_WAITING_FOR_ADD_DATA_RESPONSE;
                        break;
                    }
                    case EM_WAITING_FOR_ADD_DATA_RESPONSE: {
                        String text = getText();
                        boolean ok = text.equals(EMStringConsts.EM_TEXT_RESPONSE_OK);
                        if (!ok) {
                            keepRunning = false;
                        }
                        sendPIMState = EMSendPIMState.EM_SENDING_DATA;
                        break;
                    }
                    case EM_SENDING_DATA: {
                        String filePath = null;
                        if (mGeneratedMetadataXmlFilePath != null) {
                            filePath = mGeneratedMetadataXmlFilePath;
                        }
                        else if(mGenerateDataTask != null){
                            filePath = mGenerateDataTask.getFilePath();
                        }else {
                            filePath = mGenerateDataThread.getFilePath();
                        }
                        boolean fileSentOk = writeFile(filePath);
                        if (!fileSentOk) {
                            keepRunning = false;
                        }
                        DLog.log(mCurrentDataType + " XML Sent");
                        sendPIMState = EMSendPIMState.EM_WAITING_FOR_FINAL_OK;
                        break;
                    }
                    case EM_WAITING_FOR_FINAL_OK: {
                        String text = getText();
                        if (text.length() == 0) {
                            handleReadError();
                        }
                        //changing to contains as sometimes gettting accumulated strings.(temporary fix).
                        //Need to analyze Issue.
                        boolean ok = text.contains(EMStringConsts.EM_TEXT_RESPONSE_OK);
                        if (ok) {
                            isSuccess = true;
                            keepRunning = false;
                        } else {
                            boolean noop = text.contains(EMStringConsts.EM_COMMAND_TEXT_NOOP);
                            if(noop) {
                                updateLastActivityTime();
                            }
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
            if (isSuccess || mRetryCount > 3) {
                sendTransferComplete(isSuccess);
            } else {
                mRetryCount++;
                sendPIMState = EMSendPIMState.EM_SENDING_ADD_DATA_COMMAND;
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sendPIMData();
            }
        }


        boolean sendText(String text) {
            try {
                if(!TextUtils.isEmpty(text) && text.startsWith(EMStringConsts.EM_COMMAND_TEXT_ADD_FILE)){
                    DLog.log(">>send text: " + EMStringConsts.EM_COMMAND_TEXT_ADD_FILE + " " + EMMigrateStatus.getItemTransferStarted(mMetaData.mDataType) + "$$" + mMetaData.mFileType + "$$" + mMetaData.mSize);
                }else {
                    DLog.log(">>send text: " + text);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String finalText = text + "\r\n";
            try {
                byte[] utf8String = finalText.getBytes("UTF-8");
                if (mSocketRawOutputStream != null) {
                    mSocketRawOutputStream.write(utf8String, 0, utf8String.length);
                    mSocketRawOutputStream.flush();
                    return true;
                }
            } catch (Exception ex) {
                DLog.log(">>got exception in sendText: " + ex.getMessage());
            }
            return false;
        }

        private boolean isWhitespace(byte aByte) {
            boolean whitespace = false;

            switch (aByte) {
                case ' ':
                case '\n':
                case '\r':
                case 9: // tab
                    whitespace = true;
                    break;
                default:
                    break;
            }

            return whitespace;
        }

        String getText() {
            String text = "";
            try {
                int bytesRead = 0;
                while(true) {
                    int thisRead = mSocketRawInputStream.read(mBuffer, bytesRead, mBuffer.length-bytesRead);
                    //DLog.log("thisRead : "+thisRead);
                    bytesRead += thisRead;
                    if (bytesRead > 2 && mBuffer[bytesRead - 2] == '\r'  && mBuffer[bytesRead - 1] == '\n') {
                        int leadingWhitespaceCharacters = 0;
                        while ((isWhitespace(mBuffer[leadingWhitespaceCharacters])) && (mBuffer[leadingWhitespaceCharacters] != 0)) {
                            leadingWhitespaceCharacters++;
                        }
                        String string = new String(mBuffer, leadingWhitespaceCharacters, bytesRead - 2 - leadingWhitespaceCharacters);
                        if (!TextUtils.isEmpty(string) && string.startsWith(EMStringConsts.EM_COMMAND_TEXT_ADD_FILE)) {
                            DLog.log("Got string: " + EMStringConsts.EM_COMMAND_TEXT_ADD_FILE + " **MetaData**");
                        } else {
                            DLog.log(String.format("Got string: %s", string));
                        }
                        text = string;
                        break;
                    } else if(bytesRead>=mBuffer.length) {
                        break;
                    }
                }
            }catch (ArrayIndexOutOfBoundsException e){
                //Ignore
            }
            catch (Exception ex) {
                DLog.log(ex);
            }
            //DLog.log("<<got text: ");
            return text;
        }

        boolean writeFile(String aFilePath) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.mFileType = -1;
            fileInfo.mFilePath = aFilePath;
            return writeFile(fileInfo);
        }

        boolean writeFile(FileInfo aFileInfo) {
            FileInputStream fileInputStream = null;
            boolean isFileWrote = false;
            long totalBytes = 0;
            try {
                fileInputStream = new FileInputStream(aFileInfo.mFilePath);
                if (fileInputStream == null || mSocketRawOutputStream == null) {
                    DLog.log("stream is null while writing from file");
                } else {
                    int len = 0;
                    long totalFileReadTime = 0;
                    long writeStart = System.nanoTime();
                    long fileReadStartTime = System.nanoTime();

                    boolean isLiveUpdateRequired = EMMigrateStatus.isLiveUpdateRequired(aFileInfo.mFileType);

                    long chunkReadBytes = 0;

                    while ((len = fileInputStream.read(mBuffer)) > 0) {
                        totalFileReadTime += (System.nanoTime() - fileReadStartTime);
                        //DLog.log("Read bytes from file: " + len);
                        mSocketRawOutputStream.write(mBuffer, 0, len);
                        //DLog.log("Wrote bytes to socket: " + len);
                        totalBytes += len;
                        fileReadStartTime = System.nanoTime();
                        updateLastActivityTime();

                        if(isLiveUpdateRequired) {
                            chunkReadBytes += len;

                            //Update current bytes transferred
                            if (chunkReadBytes > EMMigrateStatus.LIVE_CHUNK_UPDATE_SIZE) {
                                EMMigrateStatus.addLiveTransferredSize(aFileInfo.mFileType, chunkReadBytes);
                                chunkReadBytes = 0;
                            }
                        }
                    }

                    //Add last remaining chunk of file
                    if(chunkReadBytes != 0) {
                        EMMigrateStatus.addLiveTransferredSize(aFileInfo.mFileType, chunkReadBytes);
                    }

                    isFileWrote = true;
                    DLog.log("Wrote file." + "\ttotalTime in Nanos: " + (System.nanoTime() - writeStart) + "\ttotalFileReadTime in Nanos: " + totalFileReadTime + "\tTotal bytes written: " + totalBytes);
                }
            } catch (FileNotFoundException e) {
                DLog.log("FileNotFoundException : " + aFileInfo.mFilePath + " " + e);
                mErrorCode = ERROR_CODE_FILE_NOTFOUND;
            }catch (IOException e){
                DLog.log("Exception : Total bytes written:" + totalBytes + ", Exception : " + e);
                if(e.toString().contains("java.io.IOException: read failed")){
                    mErrorCode = ERROR_CODE_READ_FAILED;
                }
            }
            catch (Exception e) {
                DLog.log("Exception : Total bytes written:" + totalBytes + ", Exception : " + e);
            } finally {
                try {
                    if (fileInputStream != null)
                        fileInputStream.close();
                } catch (Exception ex) {
                    DLog.log(ex);
                }
            }
            return isFileWrote;
        }

        long writeFile(long timeTowrite) {
            long totalBytes = 0;
            try {
                new Random().nextBytes(mBuffer);
                int len = mBuffer.length;
                long startTime = System.currentTimeMillis();
                while (timeTowrite > (System.currentTimeMillis() - startTime)) {
                    mSocketRawOutputStream.write(mBuffer);
                    totalBytes += len;
                }
                mBuffer = new byte[READ_BUFFER_SIZE];
            } catch (IOException e) {
                DLog.log(e);
            }
            return totalBytes;
        }



        private void receiveFilesInThread() {
            //DLog.log(">>start receiveFilesInThread");
            keepRunning = true;
            while (keepRunning) {
                String text = getText();
                if (text.length() == 0) {
                    handleReadError();
                }
                if (text.startsWith(EMStringConsts.EM_COMMAND_TEXT_ADD_FILE + " ")) {
                    FileInfo fileInfo = null;
                    Scanner scanner = null;
                    try {
                        scanner = new Scanner(text);
                        String cmd = scanner.next();
                        String metasize = scanner.next();
                        String prefix = cmd + " " + metasize + " ";
                        String metadata = text.substring(prefix.length());
                        InputStream inputStream = null;
                        try {
                            inputStream = new ByteArrayInputStream(metadata.getBytes("UTF-8"));
                        } catch (Exception ex) {
                            DLog.log(ex);
                        }

                        fileInfo = parseFileInfo(inputStream);
                        DLog.log("read Metadata file");
                        scanner.close();
                    } catch (Exception e) {
                        //
                    }finally {
                        if(scanner!=null) {
                            scanner.close();
                        }
                    }
                    if (fileInfo != null) {
                        receiveFile(fileInfo);
                    } else {
                        keepRunning = false;
                    }
                } else if (text.startsWith(EMStringConsts.EM_COMMAND_TEXT_ADD_DATA + " ")) {
                    String[] commandAndParameters = text.split(" ");
                    if (commandAndParameters.length > 1) {
                        try {
                            mCurrentDataType = commandAndParameters[1];
                            long metaDataSize = Long.parseLong(commandAndParameters[2]);
                            receivePIMState = EMReceivePIMState.EM_SENDING_INITIAL_OK;
                            receivePIMFile(metaDataSize);
                        } catch (Exception ex) {
                            //
                        }
                    }
                }
                // Logging from Android device when IOS as Destination
                else if (text.contains(EMStringConsts.EM_COMMAND_UPDATE_DB_STATUS_INPROGRESS)) {
                    int index = text.indexOf(':');
                    String jsonString = text.substring(index + 1);
                    startServerCall(jsonString,true);
                } else if (text.contains(EMStringConsts.EM_COMMAND_UPDATE_DB_STATUS_FINAL)) {
                    try {
                        int index = text.indexOf(':');
                        String jsonString = text.substring(index + 1);
                        JsonReader reader = new JsonReader(new StringReader(jsonString));
                        reader.setLenient(true);
                        EDeviceSwitchSession eDeviceSwitchSession = new Gson().fromJson(reader, EDeviceSwitchSession.class);
                        DashboardLog.getInstance().seteDeviceSwitchSession(eDeviceSwitchSession);
                        DashboardLog.getInstance().setUpdateDBdetails(true);
                        startServerCall(jsonString, false);
                    } catch (Exception e) {
                        DLog.log(e);
                    }
                    sendText(EMStringConsts.EM_COMMAND_SERVER_RESPONSE + ":" + EMStringConsts.EM_TEXT_RESPONSE_OK);
                }

                else if(text.equalsIgnoreCase(EMStringConsts.EM_COMMAND_START_ESTIMATION)){
                    receiveDataForEstimation();
                }
                else if (text.contains(EMStringConsts.EM_COMMAND_RESTORE_COMPLETED)) {
                    int index = text.indexOf(':');
                    String dataType = text.substring(index + 1);
                    String backupFile = MigrationStats.getInstance().getPimBackupFile(dataType);
                    try {
                        File file = new File(backupFile);
                        if (file.exists())
                            file.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }finally {
                        MigrationStats.getInstance().removeBackupFile(backupFile);
                    }
                    boolean sent = sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
                    if(sent) {
                        mDataCompleteDelegate.commandComplete(dataType, true);
                        keepRunning = false;
                    }
                }
            }
            //DLog.log("<<end receiveFilesInThread");
        }

        void handleReadError() {
            keepRunning = false;
            DLog.log("handleReadError: making keep running false, to handle the error");
        }

        private void receiveFile(FileInfo fileInfo) {
            receiveFileState = EMReceiveFileState.EM_RECEIVE_STATE_NONE;
            String filePath = null;
            boolean isSuccess = false;
            boolean isFileSkipped = false;
            while (keepRunning) {
                switch (receiveFileState) {
                    case EM_RECEIVE_STATE_NONE: {
                        filePath = getTargetFilePath(fileInfo);
                        fileInfo.mFilePath = filePath;
                        if (filePath == null || filePath.length() == 0) {
                            handleReadError();
                            break;
                        }
                        receiveFileState = EMReceiveFileState.EM_WAITING_FOR_ADD_FILE_XML;
                        File tempFile = new File(filePath);
                        if (tempFile.exists()) {
                            if (tempFile.length() == fileInfo.mFileSize) {
                                DLog.log("*** The file already exists: >>" + fileInfo.mDataType + " size: " + fileInfo.mFileSize);
                                EMMigrateStatus.addLiveTransferredSize(fileInfo.mFileType, fileInfo.mFileSize);
                                boolean ok = sendText(EMStringConsts.EM_TEXT_RESPONSE_ALREADY_EXISTS);
                                if (!ok) {
                                    handleReadError();
                                    break;
                                }
                                isSuccess = true;
                                keepRunning = false;
                            } else {
                                try {
                                    int pos = tempFile.getName().lastIndexOf(".");
                                    int fileNo = 0;
                                    String fileExtension = tempFile.getName().substring(pos + 1);
//                                    if (fileExtension.equalsIgnoreCase("m4a")){
//                                        fileExtension = "mp3";
//                                    }
                                    String fileName = pos > 0 ? tempFile.getName().substring(0, pos) : tempFile.getName();
                                    File modifiedFile = null;
                                    do {
                                        fileNo++;
                                        String newFileName = fileName + "(" + fileNo + ")." + fileExtension;
                                        modifiedFile = new File(tempFile.getParent(), newFileName);
                                        filePath = modifiedFile.getAbsolutePath();
                                    } while (modifiedFile.exists());
                                } catch (Exception e) {
                                    DLog.log("Exception while creating the new file name : " + e.toString());
                                }
                                fileInfo.mFilePath = filePath;          //Setting modified file path.
                            }
                        }
                        break;
                    }
                    case EM_WAITING_FOR_ADD_FILE_XML: {
                        receiveFileState = EMReceiveFileState.EM_SENDING_XML_OK;
                        boolean ok;
                        if (fileInfo.mFileSize == 0) {
                            EMMigrateStatus.addItemNotTransferred(fileInfo.mFileType);
                            CommonUtil.getInstance().setRestoredMediaCount(1);
                            sendText(EMStringConsts.EM_TEXT_RESPONSE_UNSUPPORTED);
                            keepRunning = false;
                            isFileSkipped = true;
                            break;
                        } else {
                            ok = sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
                        }
                        if (!ok) {
                            handleReadError();
                            break;
                        }
                        break;
                    }
                    case EM_SENDING_XML_OK: {
                        receiveFileState = EMReceiveFileState.EM_WAITING_FOR_RAW_FILE_DATA;
                        DLog.log("start reading " + fileInfo.mDataType + " file of size: " + fileInfo.mFileSize);
                        boolean ok = readToFile(fileInfo, fileInfo.mFileSize);
                        if (!ok) {
                            DLog.log("Error while reading " +fileInfo.mDataType + " file of size: " + fileInfo.mFileSize);
                            handleReadError();
                            break;
                        }
                        DLog.log("read file: " + fileInfo.mDataType + " of size: " + fileInfo.mFileSize);
                        break;
                    }
                    case EM_WAITING_FOR_RAW_FILE_DATA: {
                        receiveFileState = EMReceiveFileState.EM_SENDING_FINAL_OK;
                        boolean ok = sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
                        if (!ok) {
                            handleReadError();
                        }
                        DLog.log("received file: " + fileInfo.mDataType + " of size: " + fileInfo.mFileSize);
                        break;
                    }
                    case EM_SENDING_FINAL_OK: {
                        isSuccess = true;
                        keepRunning = false;
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
            if (isSuccess) {
                receivedAFile(fileInfo);
                keepRunning = true;//for next file
            }else if(isFileSkipped){
                keepRunning = true ; //for next file
            }
        }


        private void receivePIMFile(long aMetaDataSize) {
            keepRunning = true;
            boolean isSuccess = false;
            while (keepRunning) {
                switch (receivePIMState) {
                    case EM_SENDING_INITIAL_OK: {
                        DLog.log(">>PIM ["+mCurrentDataType+ "] OK");
                        boolean ok = sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
                        if (!ok) {
                            handleReadError();
                        }
                        receivePIMState = EMReceivePIMState.EM_WAITING_FOR_RAW_FILE_DATA;
                        break;
                    }
                    case EM_WAITING_FOR_RAW_FILE_DATA: {
                        String metadataFilePath = EMUtility.temporaryFileName();
                        boolean fileOk = readToFile(metadataFilePath, aMetaDataSize);
                        if (!fileOk) {
                            handleReadError();
                            break;
                        }
                        DLog.log(">>Read PIM["+mCurrentDataType+ "] file "+metadataFilePath);
                        //receivePIMState = EMReceivePIMState.EM_PROCESSING_RECEIVED_DATA;
                        receivePIMState=EMReceivePIMState.EM_SENDING_FINAL_OK;
                        processPIMXML(metadataFilePath);
                        break;
                    }
                    case EM_PROCESSING_RECEIVED_DATA: {
                        sendText(EMStringConsts.EM_COMMAND_TEXT_NOOP);
                        try {
                            Thread.sleep(2000);
                        } catch (Exception e) {
                            DLog.log(e.getMessage());
                        }
                        break;
                    }
                    case EM_SENDING_FINAL_OK: {
                        boolean ok = sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
                        if (ok) {
                            isSuccess = true;
                            keepRunning = false;
                        }
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
            sendTransferComplete(isSuccess);
        }

        boolean readToFile(String aFilePath, long size) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.mFileType = -1;
            fileInfo.mFilePath = aFilePath;
            return readToFile(fileInfo, size);
        }

        boolean readToFile(FileInfo fileInfo, long size) {
            FileOutputStream fileOutputStream = null;
            long totalBytes = 0;

            boolean isFileRead = false;
            boolean isLiveUpdateRequired = EMMigrateStatus.isLiveUpdateRequired(fileInfo.mFileType);

            try {
                fileOutputStream = new FileOutputStream(fileInfo.mFilePath);
                if (fileOutputStream == null || mSocketRawInputStream == null) {
                    DLog.log("stream is null while reading data");
                } else {
                    int len = 0;
                    long totalFileWriteTime = 0;
                    long readStart = System.nanoTime();
                    long chunkReadBytes = 0;
                    if (size > 0) {
                        while ((len = mSocketRawInputStream.read(mBuffer)) > 0) {
                            //DLog.log("Read bytes from socket: " + len);
                            long fileWriteStartTime = System.nanoTime();
                            fileOutputStream.write(mBuffer, 0, len);
                            //DLog.log("Wrote bytes to file: " + len);
                            totalFileWriteTime += (System.nanoTime() - fileWriteStartTime);
                            totalBytes += len;

                            if(isLiveUpdateRequired) {
                                chunkReadBytes += len;

                                //Update current bytes transferred
                                if (chunkReadBytes >= EMMigrateStatus.LIVE_CHUNK_UPDATE_SIZE) {
                                    EMMigrateStatus.addLiveTransferredSize(fileInfo.mFileType, chunkReadBytes);
                                    chunkReadBytes = 0;
                                }
                            }

                            if (totalBytes >= size) {
                                if(chunkReadBytes != 0) {
                                    EMMigrateStatus.addLiveTransferredSize(fileInfo.mFileType, chunkReadBytes);
                                }
                                isFileRead = true;
                                break;
                            }
                        }
                        DLog.log("totalTime in Nanos: " + (System.nanoTime() - readStart)
                                + "\ttotalFileWriteTime in Nanos: " + totalFileWriteTime
                                + "\tTotal bytes read: " + totalBytes);
                    }
                }
            } catch (Exception e) {
                DLog.log("Exception : Total bytes read: " + totalBytes + ", Exception : " + e);
            } finally {
                try {
                    fileOutputStream.close();
                } catch (Exception ex) {
                    //No need to handle
                }
            }
            return isFileRead;
        }

        private void generateMetaData() {
            try {

                EMXmlGenerator xmlGenerator = new EMXmlGenerator();
                xmlGenerator.startDocument();

                File file = new File(mMetaData.mSourceFilePath);
                // TODO: check file exists

                // TODO: later we need to say what type of file?
                xmlGenerator.startElement(EMStringConsts.EM_XML_FILE);

                {
                    xmlGenerator.startElement(EMStringConsts.EM_XML_FILE_TYPE);
                    String fileType = "";
                    switch (mMetaData.mDataType) {
                        case EMDataType.EM_DATA_TYPE_PHOTOS:
                            fileType = EMStringConsts.EM_XML_PHOTOS;
                            break;
                        case EMDataType.EM_DATA_TYPE_VIDEO:
                            fileType = EMStringConsts.EM_XML_VIDEO;
                            break;
                        case EMDataType.EM_DATA_TYPE_MUSIC:
                            fileType = EMStringConsts.EM_XML_MUSIC;
                            break;
                        case EMDataType.EM_DATA_TYPE_DOCUMENTS:
                            fileType = EMStringConsts.EM_XML_DOCUMENTS;
                            break;
                        case EMDataType.EM_DATA_TYPE_APP:
                            fileType = EMStringConsts.EM_XML_APP;
                            break;
                        default:
                            // TODO: unknown file type
                            break;
                    }
                    xmlGenerator.writeText(fileType);
                    mMetaData.mFileType = fileType;
                    xmlGenerator.endElement(EMStringConsts.EM_XML_FILE_TYPE);

                    xmlGenerator.startElement(EMStringConsts.EM_XML_FILE_SIZE);
                    xmlGenerator.writeText(String.valueOf(file.length()));
                    xmlGenerator.endElement(EMStringConsts.EM_XML_FILE_SIZE);

                    xmlGenerator.startElement(EMStringConsts.EM_XML_FILE_NAME);
                    xmlGenerator.writeText(mMetaData.mFileName);
                    xmlGenerator.endElement(EMStringConsts.EM_XML_FILE_NAME);

                    xmlGenerator.startElement(EMStringConsts.EM_XML_FILE_TOTAL_MEDIA_SIZE);
                    xmlGenerator.writeText(String.valueOf(mMetaData.mTotalMediaSize));
                    xmlGenerator.endElement(EMStringConsts.EM_XML_FILE_TOTAL_MEDIA_SIZE);  //stopping as we are not using this filed.

                    if (mMetaData.mRelativePath != null && !mMetaData.mRelativePath.equalsIgnoreCase("")) {
                        xmlGenerator.startElement(EMStringConsts.EM_XML_RELATIVE_PATH);
                        xmlGenerator.writeText(mMetaData.mRelativePath);
                        xmlGenerator.endElement(EMStringConsts.EM_XML_RELATIVE_PATH);
                    }
                    if(mMetaData.retry){
                        xmlGenerator.startElement(EMStringConsts.EM_XML_RETRY);
                        xmlGenerator.writeText(String.valueOf(mMetaData.retry));
                        xmlGenerator.endElement(EMStringConsts.EM_XML_RETRY);
                    }
                }

                xmlGenerator.endElement(EMStringConsts.EM_XML_FILE);

                mGeneratedMetadataXmlFilePath = xmlGenerator.endDocument();
            } catch (Exception ex) {
                // TODO: handle error
            }
        }

        private String getTargetFilePath(FileInfo aFileInfo) {
            String filePath = null; // The path of the file to be created on the file system

            // Determine the target file name
            if (aFileInfo.mRelativePath != null && !aFileInfo.mRelativePath.equalsIgnoreCase("")) {
                try {
                    File completeFilePath = new File(Environment.getExternalStorageDirectory().toString() + "/" + aFileInfo.mRelativePath);
                    File filePathDirectory = new File(completeFilePath.getParent());
                    if (!filePathDirectory.exists()) {
                        DLog.log("Trying to create file Directory : " + filePathDirectory);
                        filePathDirectory.mkdirs();
                    }
                    if (filePathDirectory.exists()) {
                        filePath = completeFilePath.getAbsolutePath();
                    } else {
                        DLog.log("Failed to create Directory : " + filePathDirectory);
                        filePath = null;
                    }
                } catch (Exception e) {
                    filePath = null;
                    DLog.log("Exception while creating the directory : " + e.getMessage());
                }
            }
            if(TextUtils.isEmpty(filePath)) {
                filePath = Environment.getExternalStorageDirectory().toString() + "/";
                if (aFileInfo.mFileType == EMDataType.EM_DATA_TYPE_PHOTOS) {
                    filePath += Environment.DIRECTORY_PICTURES + "/" + aFileInfo.mFileName;
                } else if (aFileInfo.mFileType == EMDataType.EM_DATA_TYPE_VIDEO) {
                    filePath += Environment.DIRECTORY_MOVIES + "/" + aFileInfo.mFileName;
                } else if (aFileInfo.mFileType == EMDataType.EM_DATA_TYPE_MUSIC) {
                    filePath += Environment.DIRECTORY_DOWNLOADS + "/" + aFileInfo.mFileName;
                }else if (aFileInfo.mFileType == EMDataType.EM_DATA_TYPE_DOCUMENTS) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        filePath += Environment.DIRECTORY_DOCUMENTS + "/" + aFileInfo.mFileName;
                    }
                } else if (aFileInfo.mFileType == EMDataType.EM_DATA_TYPE_APP) {
                    filePath += Constants.APP_MIGRATION_DIRECTORY + "/" + aFileInfo.mFileName;
                    AppMigrateUtils.addRestoreApp(filePath);
                } else {
                    filePath += Constants.DOCUMENTS_MIGRATION_DIRECTORY + "/" + aFileInfo.mFileName;
                }
            }
            if (!aFileInfo.retry)
                EMMigrateStatus.addItemTransferStarted(aFileInfo.mFileType);

            if (filePath != null) {
                File file = new File(filePath);
                file.getParentFile().mkdirs();
            }

            // TODO: check that the filename doesn't already exist (if it does then add a number before the extension to prevent it being overwritten)

            return filePath;
        }

        private FileInfo parseFileInfo(InputStream inputStream) {
            // TODO: parsing this in the main thread - not great, but it should be very fast as we already have the file and it's only a few lines long

            FileInfo fileInfo = null;

            // Initialize the parser
            XmlPullParser xmlParser = Xml.newPullParser();

            try {
                xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                xmlParser.setInput(new InputStreamReader(inputStream));
                xmlParser.nextTag();

                int xmlLevel = 1;

                String text = "";

                // Parse XML file - currently very, very basic
                while (xmlParser.next() != XmlPullParser.END_DOCUMENT) {

                    String tagName = xmlParser.getName();

                    if (xmlParser.getEventType() == XmlPullParser.START_TAG) {

                        if (xmlLevel == 1) {
                            fileInfo = new FileInfo();
                        }

                        xmlLevel++;
                    }

                    if (xmlParser.getEventType() == XmlPullParser.TEXT) {
                        text = xmlParser.getText();
                    }

                    if (xmlParser.getEventType() == XmlPullParser.END_TAG) {
                        xmlLevel--;

                        if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_ROOT)) {
                            break;
                        }

                        if (xmlLevel == 2) {
                            if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_FILE_SIZE)) {
                                fileInfo.mFileSize = Long.valueOf(text);
                            } else if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_FILE_NAME)) {
                                fileInfo.mFileName = text;
                            } else if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_FILE_TOTAL_MEDIA_SIZE)) {
                                fileInfo.mTotalMediaSize = Long.valueOf(text);
                            } else if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_RELATIVE_PATH)) {
                                fileInfo.mRelativePath = text;
                            } else if(tagName.equalsIgnoreCase(EMStringConsts.EM_XML_RETRY)){
                                fileInfo.retry = Boolean.parseBoolean(text);
                            } else if (tagName.equalsIgnoreCase(EMStringConsts.EM_XML_FILE_TYPE)) {
                                fileInfo.mDataType = text;
                                if (text.equalsIgnoreCase(EMStringConsts.EM_XML_PHOTOS)) {
                                    fileInfo.mFileType = EMDataType.EM_DATA_TYPE_PHOTOS;
                                } else if (text.equalsIgnoreCase(EMStringConsts.EM_XML_VIDEO)) {
                                    fileInfo.mFileType = EMDataType.EM_DATA_TYPE_VIDEO;
                                } else if (text.equalsIgnoreCase(EMStringConsts.EM_XML_DOCUMENTS)) {
                                    fileInfo.mFileType = EMDataType.EM_DATA_TYPE_DOCUMENTS;
                                } else if (text.equalsIgnoreCase(EMStringConsts.EM_XML_MUSIC)) {
                                    fileInfo.mFileType = EMDataType.EM_DATA_TYPE_MUSIC;
                                } else if (text.equalsIgnoreCase(EMStringConsts.EM_XML_APP)) {
                                    fileInfo.mFileType = EMDataType.EM_DATA_TYPE_APP;
                                }

                                if (fileInfo.mFileType != EMDataType.EM_DATA_TYPE_NOT_SET) {
                                    EMProgressInfo progressInfo = new EMProgressInfo();
                                    progressInfo.mDataType = fileInfo.mFileType;
                                    progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_RECEIVING_DATA;
                                    progressInfo.mFileSize = fileInfo.mFileSize;
                                    progressInfo.mTotalMediaSize = fileInfo.mTotalMediaSize;
                                    progressInfo.mCurrentItemNumber=EMMigrateStatus.getItemsTransferred(fileInfo.mFileType)+1;
                                    sendProgressUpdate(progressInfo);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                DLog.log(ex);
            }

            try {
                inputStream.close();
            } catch (Exception e) {
                // Ignore
            }

            return fileInfo;
        }

    }

    void writePIMXml() {
        DLog.log("Generating PIMData XML " + mCurrentDataType);
        if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CONTACTS)) {
            mGenerateDataTask = new EMGenerateContactsXmlAsyncTask();
        } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CALENDAR)) {
            mGenerateDataTask = new EMGenerateCalendarXmlAsyncTask();
        } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_SMS_MESSAGES)) {
            mGenerateDataTask = new EMGenerateSmsMessagesXmlAsyncTask();
        } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CALL_LOGS)) {
            mGenerateDataThread = new EMGenerateCallLogsBackupTask();
        }else if(mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_SETTINGS)){
            mGenerateDataThread = new EMGenerateSettingsBackupTask();
        }
        if (mGenerateDataTask != null) {
            mGenerateDataTask.setCommandDelegate(mCommandDelegate);
            CommonUtil.getInstance().setRunningAsyncTask(mGenerateDataTask);
            mGenerateDataTask.startTask(pimProgressHandler);
        }else if(mGenerateDataThread!=null){
            mGenerateDataThread.startTask(pimProgressHandler);
        }
    }
    EMParseDataTask emParseDataTask = null;
    EMParseDataInThread emParseDataInThread = null;

    void processPIMXML(final String aDataPath) {
        DLog.log("Parsing PIMData XML " + mCurrentDataType + " " + aDataPath + " " + Thread.currentThread().getId());
        if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CONTACTS)) {
            emParseDataInThread = new EMParseContactsXmlAsyncTask();
            startParsingXML(aDataPath);
        } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CALENDAR)) {
            emParseDataInThread = new EMParseCalendarXmlAsyncTask();
            startParsingXML(aDataPath);
        }else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_SMS_MESSAGES)) {
            emParseDataInThread = new EMParseSmsXmlAsyncTask();
            EasyMigrateActivity.getDefaultSMSAppPermission(new PermissionHandler() {
                @Override
                public void userAccepted() {
                    DLog.log(mCurrentDataType + " User Accepted ");
                    startParsingXML(aDataPath);
                }
                @Override
                public void userDenied() {
                    receivePIMState = EMReceivePIMState.EM_SENDING_FINAL_OK;
                    DLog.log(mCurrentDataType + " User Denied ");
                    if(mDataCompleteDelegate!=null){
                        mDataCompleteDelegate.restoreCompleted(mCurrentDataType,false);
                    }
                }
            });
        }
        else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CALL_LOGS)) {
            emParseDataInThread = new EMParseCallLogsXmlInThread();
            startParsingXML(aDataPath);
        }else if(mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_SETTINGS)){
            emParseDataInThread = new EMParseSettingsXmlInThread();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                EasyMigrateActivity.enableWritePermissionForSettings(new PermissionHandler() {
                    @Override
                    public void userAccepted() {
                        DLog.log(mCurrentDataType + " User enabled ");
                        startParsingXML(aDataPath);
                    }

                    @Override
                    public void userDenied() {
                        DLog.log(mCurrentDataType + " User denied ");
                        // Allowing to change settings as some of them not required permission.
                        /*if (mDataCompleteDelegate != null) {
                            mDataCompleteDelegate.restoreCompleted(mCurrentDataType, false);
                        }*/
                        startParsingXML(aDataPath);
                    }
                });
            } else {
                startParsingXML(aDataPath);
            }
        }
    }

    private void startParsingXML(String aDataPath){
        if (emParseDataTask != null) {
            CommonUtil.getInstance().setRunningAsyncTask(emParseDataTask);
            emParseDataTask.startTask(aDataPath, true, EMUtility.Context(), pimProgressHandler);
        }else if(emParseDataInThread != null){
            emParseDataInThread.startTask(aDataPath, true, EMUtility.Context(), pimProgressHandler);
        }
    }

    private EMProgressHandler pimProgressHandler = new EMProgressHandler() {
        @Override
        public void taskComplete(boolean aSuccess) {
            if (aSuccess) {
                DLog.log(mCurrentDataType + " task Completed");
                if (receivePIMState == EMReceivePIMState.EM_PROCESSING_RECEIVED_DATA) {
                    receivePIMState = EMReceivePIMState.EM_SENDING_FINAL_OK;
                } else if(sendPIMState == EMSendPIMState.EM_WAITING_FOR_DATA_GENERATION){
                    sendPIMState = EMSendPIMState.EM_SENDING_ADD_DATA_COMMAND;
                }
            }
            if(mDataCompleteDelegate!=null){
                mDataCompleteDelegate.restoreCompleted(mCurrentDataType,aSuccess);
            }
        }

        @Override
        public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
            DLog.log(mCurrentDataType + " taskError");
            if (receivePIMState == EMReceivePIMState.EM_PROCESSING_RECEIVED_DATA)
                receivePIMState = EMReceivePIMState.EM_SENDING_FINAL_OK;
        }

        @Override
        public void progressUpdate(EMProgressInfo aProgressInfo) {
            if (mDataCommandDelegate != null)
                mDataCommandDelegate.progressUpdate(aProgressInfo);
        }
    };


    private void startServerCall(String gsonString, final boolean respondBack){
        TransactionLogging transactionLogging = new TransactionLogging(LOGGING_API_ENDPOINT,
                HTTP_AUTHENTICATION_USERNAME + ":" + HTTP_AUTHENTICATION_PASSWORD, gsonString, new ServerCallBacks() {
            @Override
            public void gpsLocationGranted(boolean result) {
                //Ignore
            }

            @Override
            public void gpsLocationRequested(boolean requested) {
                //Ignore
            }

            @Override
            public void locationValidation(boolean result, String response) {
                //Ignore
            }

            @Override
            public void storeidValidation(boolean result, String response) {
                //Ignore
            }

            @Override
            public void onserverCallCompleted(String response) {
                DLog.log("in onserverCallCompleted response:" + response);
                if(respondBack) {
                    mWorkerThread.sendText(EMStringConsts.EM_COMMAND_SERVER_RESPONSE + ":" + response);
                }
            }

            @Override
            public void storeidAndRepidValidation(boolean result, String response) {

            }

            @Override
            public void repLoginValidation(boolean result, String response) {

            }

        });
        transactionLogging.startServiceCall();
    }

    private void checkMigrationStatus(){
        boolean restoreConnection = false;
        while (Constants.stopMigration) {
            if (!restoreConnection) {
                DLog.log("***Migration is paused***2");
            }
            restoreConnection = true;
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (restoreConnection) {
            reInitiateConnection();
        }
    }

    private void  reInitiateConnection(){
        try {
            DLog.log("----reInitiateConnection----");
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
        initialiseConnection();
    }

}