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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

public class EMAsyncStream {

    private static final int READ_BUFFER_SIZE = 1024 * 128;
    private byte[] mReadBuffer = new byte[READ_BUFFER_SIZE];
    private int mReadBufferStart = 0;
    private int mReadBufferEnd = 0;

    private static String TAG = "EMAsyncStream";

    public static final int EM_READ_MODE = 0;
    public static final int EM_WRITE_MODE = 1;

    private int mReadWriteMode;

    private void init(EMAsyncStreamDelegate aDelegate, int aReadWriteMode) {
        mDelegate = aDelegate;
        mReadWriteMode = aReadWriteMode;
        mMainThreadHandler = new Handler() {
            public void handleMessage(Message msg) {
                mActive = false;

                EMAsyncStreamMessageResponse response = (EMAsyncStreamMessageResponse) msg.obj;

                switch (response.mId) {
                    case EMStreamMessageResponseOpened:
                        mDelegate.handleEvent(EMAsyncStreamDelegate.EventOpenCompleted);
                        break;
                    case EMStreamMessageHasBytesAvailable:
                        //DLog.log("EMAsyncStream: handleMessage: EMStreamMessageHasBytesAvailable");
                        mDelegate.handleEvent(EMAsyncStreamDelegate.EventHasBytesAvailable);
                        break;
                    case EMStreamMessageResponseAllBytesWritten:
                        mDelegate.handleEvent(EMAsyncStreamDelegate.EventHasSpaceAvailable);
                        break;
                    case EMStreamMessageResponseError:
                        mDelegate.handleEvent(EMAsyncStreamDelegate.EventErrorOccurred);
                        break;
                    case EMStreamMessageResponseAllBytesWrittenForFile:
                        mDelegate.handleEvent(EMAsyncStreamDelegate.EventFileDone);
                        break;
                    default:
                        // Ignore
                        break;
                }
            }
        };

        mMainThreadMessenger = new Messenger(mMainThreadHandler);

        mWorkerThread.start();
    }

    public EMAsyncStream(EMAsyncStreamDelegate aDelegate, InetAddress aHostName, int aPort, Socket aSocket, int aReadWriteMode) {
        mHostName = aHostName;
        mPort = aPort;
        mSocket = aSocket;
        init(aDelegate, aReadWriteMode);
    }

    public EMAsyncStream(EMAsyncStreamDelegate aDelegate, Socket aSocket, int aReadWriteMode) {
        mSocket = aSocket;
        init(aDelegate, aReadWriteMode);
    }

    static final int EMStreamMessageOpen = 0;
    static final int EMStreamMessageClose = 1;
    static final int EMStreamMessageRead = 2;
    static final int EMStreamMessageWrite = 3;
    static final int EMStreamMessageReadInAndWriteOut = 4;

    class EMAsyncStreamMessage {
        public int mId;
        byte[] mBytes;
        int mOffset;
        int mTotalBytes;
    }

    static final int EMStreamMessageResponseOpened = 0;
    static final int EMStreamMessageHasBytesAvailable = 1;
    static final int EMStreamMessageResponseAllBytesWritten = 2;
    static final int EMStreamMessageResponseError = 3;
    static final int EMStreamMessageClosed = 4;
    static final int EMStreamMessageResponseAllBytesWrittenForFile = 5;

    class EMAsyncStreamMessageResponse {
        public int mId;
        public int mNumberBytes;
    }

    private Handler mMainThreadHandler;
    private Messenger mMainThreadMessenger;

    EMStreamThread mWorkerThread = new EMStreamThread();

    class EMStreamThread extends Thread {

        public Handler mInThreadHandler;
        private byte[] mRealReadBuffer = new byte[READ_BUFFER_SIZE];
        private InputStream mRawInputStream;
        private OutputStream mOutputStream;

        EMStreamThread() {
        }

        synchronized Handler getHandler() {
            while (mInThreadHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    //Ignore and try again.
                }
            }

            return mInThreadHandler;
        }

        @Override
        public void run() {
            DLog.log(">>WoEMAsyncString: openInThread:");
            Looper.prepare();

            synchronized (this) {

                mInThreadHandler = new Handler() {
                    public void handleMessage(Message msg) {
                        EMAsyncStreamMessage message = (EMAsyncStreamMessage) msg.obj;
                        switch (message.mId) {
                            case EMStreamMessageOpen:
                                openInThread(msg.replyTo);
                                break;
                            case EMStreamMessageClose:
                                closeInThread(msg.replyTo);
                                break;
                            case EMStreamMessageRead:
                                readInThread(msg.replyTo);
                                break;
                            case EMStreamMessageWrite:
                                writeInThread(message.mBytes, message.mOffset, message.mTotalBytes, msg.replyTo);
                                break;
                            case EMStreamMessageReadInAndWriteOut:
                                readInAndWriteOutInThread(msg.replyTo);
                                break;
                            default:
                                // Ignore
                        }
                    }
                };

                notifyAll();
            }

            Looper.loop();
        }

        void openInThread(Messenger aReplyTo) {
            DLog.log(">>EMAsyncString: openInThread:");
            Message msg = new Message();
            EMAsyncStreamMessageResponse message = new EMAsyncStreamMessageResponse();
            msg.obj = message;

            synchronized (mSocket) {
                try {
                    // TODO: put a limit on the number of retries?
                    while (!mSocket.isConnected()) {
                        if (mHostName != null)
                            DLog.log(String.format("***** Connecting socket to: %s on port %d", mHostName.toString(), mPort));
                        else
                            DLog.log(String.format("***** Connecting socket to: %s on port %d", "null", mPort));
                        InetSocketAddress remoteAddr = new InetSocketAddress(mHostName, mPort);
                        DLog.log("socket established port on : " + mPort);
                        DLog.log("socket established remoteAddr on : " + remoteAddr);
                        mSocket.connect(remoteAddr, 1000);
                        if (mSocket.isConnected())
                            DLog.log(String.format("***** Socket connected: %s on port %d", mHostName.toString(), mPort));
                        else {
                            DLog.log(String.format("***** Socket not connected: %s on port %d", mHostName.toString(), mPort));
                        }
                    }

                    if (mReadWriteMode == EM_READ_MODE) {
                        if (mRawInputStream != null) {
                            DLog.log(">>EMAsyncString: openInThread: mRawInputStream is already open");
                            return;
                        }

                        mRawInputStream = mSocket.getInputStream();
                    } else if (mReadWriteMode == EM_WRITE_MODE) {
                        if (mOutputStream != null) {
                            // We're already open so just return
                            return;
                        }

                        mOutputStream = mSocket.getOutputStream();
                    }

                    message.mId = EMStreamMessageResponseOpened;

                } catch (Exception ex) {
                    ex.printStackTrace();
                    DLog.log(ex);
                    message.mId = EMStreamMessageResponseError;
                }

                try {
                    aReplyTo.send(msg);
                } catch (RemoteException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    DLog.log(e1);
                }

                //	if (mReadWriteMode == EM_READ_MODE)
                //		readInThread(aReplyTo);
            }
            DLog.log("<<EMAsyncString: openInThread:");
        }

        ;

        void closeInThread(Messenger aReplyTo) {
            try {
                if (mRawInputStream != null)
                    mRawInputStream.close();

                if (mOutputStream != null)
                    mOutputStream.close();

                synchronized (mSocket) {
                    if (mSocket != null)
                        mSocket.close();
                }

                // TODO: send a quit message to the worker thread

            } catch (Exception ex) {
                // TODO: ?
                ex.printStackTrace();
                DLog.log(ex);
            }

        }

        void readInThread(Messenger aReplyTo) {
            DLog.log("EMAsyncString: readInThread:");

            if (mRawInputStream == null) {
                DLog.log("EMAsyncStream: readInThread: stream not open - opening now");
                openInThread(aReplyTo);
            }

            if (mRawInputStream == null)
                return;

            synchronized (mReadBuffer) {
                if (mReadBufferStart < mReadBufferEnd)
                    return; // We already have data in the buffer and have already reported it to the client
                // In this case we don't want to read again otherwise we might overwrite the data before the client has read it
            }


            Message msg = new Message();
            EMAsyncStreamMessageResponse message = new EMAsyncStreamMessageResponse();
            msg.obj = message;

            int bytesRead = 0;

            try {
                while ((bytesRead == 0) && (mSocket != null) && (mSocket.isConnected())) {
                    try {
                        bytesRead = mRawInputStream.read(mRealReadBuffer);
                        if (EMConfig.LOG_RAW_ASYNC_STREAM_DATA) {
                            Charset UTF8_CHARSET = Charset.forName("UTF-8");
                            String logString = new String(mRealReadBuffer, 0, bytesRead, UTF8_CHARSET);
                            DLog.log("Bytes_Read: " + logString);
                        }
                    } catch (Exception ex) {
						/*
						// Log and retry
						// Log.d(TAG, "*** Socket read exception - retrying");
						DLog.log(ex);
						SystemClock.sleep(EMConfig.EM_WAIT_FOR_WIFI_GLITCH_TIME_MS); // Wait then try again...
						*/

                        message.mId = EMStreamMessageResponseError;
                        try {
                            aReplyTo.send(msg);
                        } catch (RemoteException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                            DLog.log(e1);
                        }

                        return;
                    }

                    // TODO: temp logging
                    // DLog.log(new String(mRealReadBuffer, 0, bytesRead, "UTF-8"));
                }

                // If the socket has been closed then exit from the read loop - don't complete anything
                if (mSocket == null)
                    return;

                if (!mSocket.isConnected())
                    return;

                if (bytesRead < 0) {
                    // TODO: handle this - closed message?
                    DLog.log("EMAsyncStream: Stream closed");
                }

                synchronized (mReadBuffer) {
                    for (int index = 0; index < bytesRead; index++) {
                        mReadBuffer[index] = mRealReadBuffer[index];
                    }

                    mReadBufferStart = 0;
                    mReadBufferEnd = bytesRead;
                }

                if (bytesRead > 0) {
                    DLog.log(new String("EMAsyncStream: readInThread: EMStreamMessageHasBytesAvailable: ") + bytesRead);
                    message.mId = EMStreamMessageHasBytesAvailable;
                } else if (bytesRead < 0)
                    message.mId = EMStreamMessageClosed;

            } catch (Exception ex) {
                // TODO:
            }

            try {
                aReplyTo.send(msg);
            } catch (RemoteException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                DLog.log(e1);
            }
        }

        void writeInThread(byte[] aByteArray, int aOffset, int aBytesToWrite, Messenger aReplyTo) {
            if (mOutputStream == null || !mSocket.isConnected()) {
                openInThread(aReplyTo);
            }

            // TODO: write ALL bytes to thread - may need to loop until this is done
            Message msg = new Message();
            EMAsyncStreamMessageResponse message = new EMAsyncStreamMessageResponse();
            msg.obj = message;

            try {
                if (mOutputStream == null)
                    openInThread(aReplyTo);

                if (mOutputStream != null) // TODO: how should we report this error - most likely it is a device at an unreachable address (e.g. an ipv6 broadcast on an ipv4 network)
                    mOutputStream.write(aByteArray, aOffset, aBytesToWrite);

                if (EMConfig.LOG_RAW_ASYNC_STREAM_DATA) {
                    Charset UTF8_CHARSET = Charset.forName("UTF-8");
                    String logString = new String(aByteArray, aOffset, aBytesToWrite, UTF8_CHARSET);
                    DLog.log("Bytes_Written: " + logString);
                }

                message.mId = EMStreamMessageResponseAllBytesWritten;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                message.mId = EMStreamMessageResponseError;
                e.printStackTrace();
                DLog.log(e);
            }

            try {
                aReplyTo.send(msg);
            } catch (RemoteException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                DLog.log(e1);
            }
        }

        void readInAndWriteOutInThread(Messenger aReplyTo) {
            if (mOutputStream == null) {
                openInThread(aReplyTo);
            }

            // TODO: write ALL bytes to thread - may need to loop until this is done
            Message msg = new Message();
            EMAsyncStreamMessageResponse message = new EMAsyncStreamMessageResponse();
            msg.obj = message;

            try {
                if (mOutputStream == null)
                    openInThread(aReplyTo);

                if (mOutputStream != null) // TODO: how should we report this error - most likely it is a device at an unreachable address (e.g. an ipv6 broadcast on an ipv4 network)
                {
                    byte[] buffer = new byte[READ_BUFFER_SIZE];
                    int len = 0;
                    while ((len = mInputStreamForFile.read(buffer)) > 0) {
                        mOutputStream.write(buffer, 0, len);
                    }
                }
                mInputStreamForFile.close();
                mInputStreamForFile = null;
                message.mId = EMStreamMessageResponseAllBytesWrittenForFile;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                message.mId = EMStreamMessageResponseError;
                e.printStackTrace();
                DLog.log(e);
                try {
                    mInputStreamForFile.close();
                    mInputStreamForFile = null;
                } catch (Exception ex) {
                }
            }
            mActive = false;

            try {
                aReplyTo.send(msg);
            } catch (RemoteException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                DLog.log(e1);
            }
        }
    }

    public void open() {
	/*
		try {
			Thread.currentThread().sleep(100);
		 // TODO: this is horrible, but if we don't do it then the worker thread does not receive our messages - need to work out why this is the case & fix
		} catch (Exception ex)
		{

		}

		mDelegate.handleEvent(EMAsyncStreamDelegate.EventOpenCompleted);

		// We will open the stream when we do the first read / write


		mActive = true;
		EMAsyncStreamMessage message = new EMAsyncStreamMessage();
		message.mId = EMStreamMessageOpen;
		Message msg = new Message();
		msg.obj = message;
		msg.replyTo = mMainThreadMessenger;
		mInThreadHandler.sendMessage(msg);
		*/
    }

    public void close() {
        mActive = true;
        EMAsyncStreamMessage message = new EMAsyncStreamMessage();
        message.mId = EMStreamMessageClose;
        Message msg = new Message();
        msg.obj = message;
        msg.replyTo = mMainThreadMessenger;
        mWorkerThread.getHandler().sendMessage(msg);
    }

    boolean hasBytesAvailable() {
        boolean bytesAvailable = false;
        synchronized (mReadBuffer) {
//			bytesAvailable = (mReadBufferEnd > mReadBufferStart);
            bytesAvailable = true;
            if ((!bytesAvailable) && (!mActive)) {
                // Trigger a new read in the worker thread if we have no data
                EMAsyncStreamMessage message = new EMAsyncStreamMessage();
                message.mId = EMStreamMessageRead;
                Message msg = new Message();
                msg.obj = message;
                msg.replyTo = mMainThreadMessenger;
                mWorkerThread.getHandler().sendMessage(msg);
                mActive = true;
            }
        }
        return bytesAvailable;
    }

    int read(byte[] aByteArray, int aOffset, int aBytesToRead) {
        int bytesCopied = 0;
        synchronized (mReadBuffer) {
            while ((mReadBufferEnd > mReadBufferStart) && (bytesCopied < aBytesToRead)) {
                aByteArray[bytesCopied + aOffset] = mReadBuffer[mReadBufferStart];
                bytesCopied++;
                mReadBufferStart++;
            }

            if (aByteArray.length > 0) {
                DLog.log(new String("EMAsyncStream: read: bytes already available: ") + (mReadBufferEnd - mReadBufferStart));
            }

            if (mReadBufferStart == mReadBufferEnd) {
                DLog.log("EMAsyncStream: No bytes available - requesting more");

                mReadBufferStart = 0;
                mReadBufferEnd = 0;
                // Trigger a new read in the worker thread
                EMAsyncStreamMessage message = new EMAsyncStreamMessage();
                message.mId = EMStreamMessageRead;
                Message msg = new Message();
                msg.obj = message;
                msg.replyTo = mMainThreadMessenger;
                mWorkerThread.getHandler().sendMessage(msg);
                mActive = true;
            }
        }

        if (bytesCopied != 0) {
            if (DLog.loggingIsOn()) {
                // Charset UTF8_CHARSET = Charset.forName("UTF-8");
                // String logString = new String(aByteArray, 0, bytesCopied, UTF8_CHARSET);
                // DLog.log(logString);
            }
        }

        DLog.log(new String("EMAsyncStream: read: bytesCopied:") + bytesCopied);

        return bytesCopied;
    }

    boolean hasSpaceAvailable() {
        return (!mActive);
    }

    int write(byte[] aByteArray, int aOffset, int aBytesToWrite) {
        // If we're already writing data then don't try to write any more
        if (mActive)
            return 0;

        try {
            mActive = true;
            EMAsyncStreamMessage message = new EMAsyncStreamMessage();
            message.mId = EMStreamMessageWrite;
            message.mBytes = aByteArray.clone();
            message.mOffset = aOffset;
            message.mTotalBytes = aBytesToWrite;
            Message msg = new Message();
            msg.obj = message;
            msg.replyTo = mMainThreadMessenger;
            mWorkerThread.getHandler().sendMessage(msg);
        } catch (Exception ex) {
            DLog.log("***** Exception sending data");
            ex.printStackTrace();
            DLog.log(ex);
        }

        return aBytesToWrite;
    }

    int writeEx(InputStream aInputStreamForFile, long aBytesToWrite) {
        try {
            mInputStreamForFile = aInputStreamForFile;
            mTotalBytesToWrite = aBytesToWrite;

            mActive = true;
            EMAsyncStreamMessage message = new EMAsyncStreamMessage();
            message.mId = EMStreamMessageReadInAndWriteOut;
            message.mBytes = null;
            message.mOffset = 0;
            message.mTotalBytes = 0;
            Message msg = new Message();
            msg.obj = message;
            msg.replyTo = mMainThreadMessenger;
            mWorkerThread.getHandler().sendMessage(msg);
        } catch (Exception ex) {
            DLog.log("***** Exception sending data");
            ex.printStackTrace();
            DLog.log(ex);
        }

        return 0;
    }

    InputStream mInputStreamForFile = null;
    long mTotalBytesToWrite = 0;
    private InetAddress mHostName = null;
    private int mPort = -1;
    private Socket mSocket = null;
    private EMAsyncStreamDelegate mDelegate = null;
    private boolean mActive = false;
}
