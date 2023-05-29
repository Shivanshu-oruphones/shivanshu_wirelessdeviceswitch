/**
 * This is class is used to start restoration process of any data type.
 *
 * If we want to start restoration of any non media data type,it should start from this class (in separate thread).
 *
 * Created by Ravikumar D on 29-Nov-17.
 */
package com.pervacio.wds.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;

abstract public class EMParseDataInThread {

    EMProgressInfo aProgressInfo = null;
    int progressUpdate = 1;
    int taskCompleteUpdate = 2;

    public void startTask(String aFilePath,
                          boolean aDeleteFileAfterParsing,
                          Context aContext,
                          EMProgressHandler aProgressHandler) {
        mFilePath = aFilePath;
        mDeleteFileAfterParsing = aDeleteFileAfterParsing;
        mContext = aContext;
        startTask(aProgressHandler);
    }


    protected Context mContext;
    protected boolean mDeleteFileAfterParsing;
    protected String mFilePath;
    public EMProgressHandler mDelegate;

    // To be called by the client, from their own thread
    public void startTask(EMProgressHandler aDelegate) {

        mDelegate = aDelegate;
        new EMParseDataInThread.WorkerThread().start();
    }

    public class WorkerThread extends Thread {
        @Override
        public void run() {
            super.run();
            runTask();
            if (mDeleteFileAfterParsing) {
                File file = new File(mFilePath);
                file.delete();
            }
            handler.sendEmptyMessage(taskCompleteUpdate);
        }
    }

    private boolean mFailed = false;
    private int mError;

    public void setFailed(int aResult) {
        mFailed = true;
        mError = aResult;
    }

    // To be overridden by the derived task
    // This function is run in the new thread
    abstract protected void runTask();

    protected void updateProgressFromWorkerThread(EMProgressInfo aProgressInfo) {
        this.aProgressInfo = aProgressInfo;
        handler.sendEmptyMessage(progressUpdate);
    }

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == progressUpdate) {
                mDelegate.progressUpdate(aProgressInfo);
            } else if (msg.what == taskCompleteUpdate) {
                mDelegate.taskComplete(true);
            }
        }
    };
}
