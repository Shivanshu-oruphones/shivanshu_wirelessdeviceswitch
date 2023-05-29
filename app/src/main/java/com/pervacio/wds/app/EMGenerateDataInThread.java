/**
 * This is class is used to start backup process for any data type.
 *
 * If we want to start backup of any non media data type,it should start from this class(in separate thread)
 *
 * Created by Ravikumar D on 29-Nov-17.
 */

package com.pervacio.wds.app;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public abstract class EMGenerateDataInThread {
    EMProgressInfo aProgressInfo = null;
    int progressUpdate = 1;
    int taskCompleteUpdate = 2;

    public String getFilePath() {
        return mFilePath;
    }

    private EMProgressHandler mDelegate;

    public void setFilePath(String aFilePath) {
        mFilePath = aFilePath;
    }

    // To be overridden by the derived task
    // This function is run in the new thread
    abstract protected void runTask();

    // To be called by the client, from their own thread
    public void startTask(EMProgressHandler aDelegate) {
        mDelegate = aDelegate;
        new WorkerThread().start();
    }

    public class WorkerThread extends Thread {
        @Override
        public void run() {
            super.run();
            runTask();
            mDelegate.taskComplete(true);
        }
    }

    private boolean mFailed = false;
    private int mError;

    public void setFailed(int aResult) {
        mFailed = true;
        mError = aResult;
    }

    private String mFilePath;
    private EMCommandDelegate mCommandDelegate;

    public void setCommandDelegate(EMCommandDelegate aCommandDelegate) {
        mCommandDelegate = aCommandDelegate;
    }

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
