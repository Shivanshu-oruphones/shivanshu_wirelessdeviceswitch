/*************************************************************************
 /**
 * Class that implements EMCommandHanlder to respond for the commands from remote device and get the command data(TextCommand).
 * <p>
 * Created by: Surya Polasanapalli on 31/5/2017.
 * Contributors:
 * Last updated on: 5/26/2017
 */

package com.pervacio.wds.app;

import android.util.Log;

public class EMTextCommandResponder implements EMCommandHandler {

    private EMCommandDelegate mCommandDelegate;
    private EMDataCommandDelegate mDataCommandDelegate;
    private boolean mCancelled;
    private String receivedCommand = null;

    // Start this command handler
    @Override
    public void start(EMCommandDelegate aDelegate) {
        mCancelled = false;
        mCommandDelegate = aDelegate;
        mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
    }

    // Returns true if this command hander handles the given command - only relevant to responders (as opposed to initiators)
    @Override
    public boolean handlesCommand(String aCommand) {
        receivedCommand = aCommand;
        if (aCommand!=null && aCommand.contains(EMStringConsts.EM_COMMAND_TEXT)) {
            receivedCommand = receivedCommand.split(EMStringConsts.EM_COMMAND_TEXT)[1];
            return true;
        }
        return false;
    }

    // We have text (usually a command or a response)
    @Override
    public boolean gotText(String aText) {
        // Ignore - we don't listen for any text (other than the initial command)
        return true;
    }

    // We have received data
    // This will be a file path to a file containing the received data
    // This could be raw data, or it could be XML data
    @Override
    public boolean gotFile(String aDataPath) {
        // Ignore - we don't listen for any files
        return true;
    }

    // The data has been sent
    @Override
    public void sent() {
        if (mCancelled)
            return;

        mCommandDelegate.commandComplete(true);

        // We have sent our OK response, so notify the you-are-target delegate that it is now the target
        EMProgressInfo progressInfo = new EMProgressInfo();
        progressInfo.mTextCommand = receivedCommand;
        progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TEXT_COMMAND_RECEIVED;
        mDataCommandDelegate.progressUpdate(progressInfo);
    }

    // Set the delegate to receive notifications about the quitting operating
    public void setDataCommandDelegate(EMDataCommandDelegate aDataCommandDelegate) {
        mDataCommandDelegate = aDataCommandDelegate;
    }

    @Override
    public void cancel() {
        mCancelled = true;
    }
}
