package com.pervacio.wds.custom;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMStringConsts;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.PreferenceHelper;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class CrashWorkManager extends Worker {

    private Socket mSocket;
    private InputStream mSocketRawInputStream;
    private OutputStream mSocketRawOutputStream;

    public CrashWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    @NonNull
    @Override
    public Result doWork() {
        DLog.log("<---CrashWorkManager :: Do work");

        boolean isConnected = initialiseConnection();
        if (isConnected) {
            sendText(EMStringConsts.EM_COMMAND_TEXT + Constants.COMMAND_APP_CRASHED);
        }
        DLog.log("CrashWorkManager :: Do work -->");
        return Result.success();
    }


    public boolean initialiseConnection() {
        try {
            DLog.log("----initialiseConnection----");
            if (mSocket == null || !mSocket.isConnected()) {
                int mPort = PreferenceHelper.getInstance(getApplicationContext()).getIntegerItem("fixed_port");
                InetAddress mHostName = PreferenceHelper.getInstance(getApplicationContext()).getObjectFromJson("remote_ipaddress", InetAddress.class);
                if (mPort == 0 || mHostName == null) {
                    DLog.log("Not sending app_crashed command , port " + mPort + " Hostname : " + mHostName);
                    return false;
                }
                DLog.log("connect to host " + mHostName.toString() + " on port : " + mPort);
                mSocket = new Socket();
                InetSocketAddress remoteAddr = new InetSocketAddress(mHostName, mPort);
                mSocket.setTcpNoDelay(true);
                if (EMUtility.shouldBindSocketToWifi()) {
                    EMUtility.bindSocketToWiFiNetwork(mSocket);
                }
                mSocket.connect(remoteAddr, 10 * 1000);
                if (mSocket.isConnected()) {
                    DLog.log(String.format("***** Socket connected: %s on port %d", mHostName.toString(), mPort));
                } else {
                    DLog.log(String.format("***** Socket not connected: %s on port %d", mHostName.toString(), mPort));
                }
            }
            DLog.log("Socket SendBufferSize:" + mSocket.getSendBufferSize());
            DLog.log("Socket ReceiveBufferSize:" + mSocket.getReceiveBufferSize());
            mSocketRawInputStream = mSocket.getInputStream();
            mSocketRawOutputStream = mSocket.getOutputStream();
            return true;
        } catch (Exception ex) {
            DLog.log("Exception : " + ex.getMessage());
        }
        return false;
    }

    boolean sendText(String text) {
        String finalText = text + "\r\n";
        DLog.log("sendText >> " + text);
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
}