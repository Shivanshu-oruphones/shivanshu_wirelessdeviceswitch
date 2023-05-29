package com.pervacio.wds.app;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Created by Billy on 17/01/2017.
 */

public abstract class MessengerBase {
    private Handler mHandler;

    public MessengerBase() {
        MessageReceiver receiver = new MessageReceiver();
        mHandler = new Handler(Looper.myLooper(), receiver);
    }

    protected void removeMessage(int aMessageId) {
        if (mHandler != null) {
            mHandler.removeMessages(aMessageId, null);
        }
    }

    public Message queueDelayedMessage(int aMessageId, Object aPayload, long aMilliSeconds) {
        Message message = null;

        try {
            message = mHandler.obtainMessage();

            if (message == null) {
                error("queueDelayedMessage, Cannot obtain a message");
                return null;
            }

            message.what = aMessageId;
            message.obj = aPayload;

            mHandler.sendMessageDelayed(message, aMilliSeconds);
        } catch (Exception e) {
            error("queueDelayedMessage, Exception: " + e);
            message = null;
        }

        return message;
    }

    public void queueMessage(int aMessageId, Object aPayload) {
        try {
            Message message = mHandler.obtainMessage();

            if (message == null) {
                error("sendMessage, Cannot obtain a message");
                return;
            }

            message.what = aMessageId;
            message.obj = aPayload;

            mHandler.sendMessage(message);
        } catch (Exception e) {
            error("sendMessage, Exception: " + e);
        }
    }

    abstract protected void processMessage(int aMessageId, Object aPayload);

    private class MessageReceiver implements Handler.Callback {
        public boolean handleMessage(Message aMessage) {
            //trace(">> handleMessage");

            int messageId = aMessage.what;
            Object payload = aMessage.obj;

            processMessage(messageId, payload);

            //trace("<< handleMessage");

            return true;
        }
    }


    //==============================================================================================
    // Logging
    //==============================================================================================

    protected static String TAG = "Messenger";

    protected static void trace(String aMessage) {
        Log.v(TAG, aMessage);
    }

    protected static void log(String aMessage) {
        Log.d(TAG, aMessage);
    }

    protected static void warn(String aMessage) {
        Log.w(TAG, aMessage);
    }

    protected static void error(String aMessage) {
        Log.e(TAG, aMessage);
    }
}