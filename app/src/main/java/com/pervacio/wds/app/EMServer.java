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

import com.pervacio.wds.custom.utils.CommonUtil;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public class EMServer {

    public static int CONTENT_TRANSFER_PORT = EMConfig.DATA_TRANSFER_PORT_NUMBER;
    private int mServerPort = 0;

    EMServer(int aServerPort) {
        mServerPort = aServerPort;
    }


    String uuid() {
        return UUID.randomUUID().toString();
    }

    // Publish the service over Bonjour
    // Listen for incoming connections (which could be handshake sessions, or main sessions)
    boolean start() {
        boolean result = createServer();

        if (result) {
            mName = uuid();
        }

        return result;
    }

    // Un-plublish the service and stop listening for incoming connections
    void stop() {
        stopServer();
    }

    ServerThread mServerThread = null;
    private Handler handler = new Handler();

    class ServerThread extends Thread {
        ServerSocket serverSocket = null;
        @Override
        public void run() {
            try {
                mKeepRunning = true;
                DLog.log("EMServer:start server socket");
                serverSocket = new ServerSocket(mServerPort);
                mPort = serverSocket.getLocalPort();
                DLog.log("server established on  : "+mPort);
                while (mKeepRunning) {
                    Socket socket = serverSocket.accept();
                    DLog.log("EMServer:Client connected on port " + mPort);
                    publishProgress(socket);
                }
                serverSocket.close();
            } catch (Exception exception) {
                // TODO: handle
                DLog.log(exception);
            }
        }

        private void publishProgress(final Socket aSocket) {
            handler.post(new Runnable() {
                public void run() {
                    DLog.log("Handler run");
                    EMConnection incomingConnection = new EMConnection(aSocket);
                    CommonUtil.getInstance().setRemoteDeviceIpAddress(incomingConnection.mSocket.getInetAddress());
                    mDelegate.clientConnected(incomingConnection);
                }
            });
        }

        private void cancel() {
            mKeepRunning = false;
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private boolean mKeepRunning = true;
    }

    private boolean createServer() {
        mServerThread = new ServerThread();
        mServerThread.start();
        return true;
    }

    private void stopServer() {
        mServerThread.cancel();
    }

    void setDelegate(EMServerDelegate aDelegate) {
        mDelegate = aDelegate;
    }

    public int mPort;

    public String mName;

    private EMServerDelegate mDelegate;

}
