package com.pervacio.wds.app;

import android.os.Looper;

import com.pervacio.wds.app.EMDataTransferHelper;

/**
 * Created by Ravikumar D on 22-03-2018.
 */

public class WorkerThreadForSocketConection implements Runnable {
    EMDataTransferHelper emDataTransferHelper = null;
    WorkerThreadForSocketConection(EMDataTransferHelper emDataTransferHelper){
        DLog.log("In WorkerThreadConstructor");
        this.emDataTransferHelper = emDataTransferHelper;
    }
    @Override
    public void run() {
        DLog.log("WorkerThreadForSocketConnection : inside run method");
        emDataTransferHelper.initialiseConnection();
    }
}
