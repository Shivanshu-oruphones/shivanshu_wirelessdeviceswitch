/*************************************************************************
 /**
 * Class that implements EMCommandHanlder to send command and data(Text) to the remote Devices
 * <p>
 * Created by: Surya Polasanapalli on 31/5/2017.
 * Contributors:
 * Last updated on: 5/26/2017
 */

package com.pervacio.wds.app;

import com.pervacio.wds.custom.utils.Constants;

public class EMTextCommandInitiator implements EMCommandHandler {

    EMDataCommandDelegate mDataCommandDelegate;
    EMCommandDelegate mCommandDelegate;
    boolean mCancelled;

    // Start this command handler
    @Override
    public void start(EMCommandDelegate aDelegate) {
        mCancelled = false;
        mCommandDelegate = aDelegate;
        StringBuilder sb = new StringBuilder(EMStringConsts.EM_COMMAND_TEXT);
        sb.append(Constants.EM_TEXT_COMMAND);
        mCommandDelegate.sendText(sb.toString());
    }

    // Returns true if this command hander handles the given command - only relevant to responders (as opposed to initiators)
    @Override
    public boolean handlesCommand(String aCommand) {
        // This is an initiator, so we don't handle any commands
        return false;
    }

    // We have text (usually a command or a response)
    @Override
    public boolean gotText(String aText) {
        if (mCancelled)
            return false;

        mCommandDelegate.commandComplete(true);

        // Notify the delegate that we've got incoming command.
        EMProgressInfo progressInfo = new EMProgressInfo();
        progressInfo.mTextCommand=aText;
        progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_TEXT_COMMAND_SENT;
        mDataCommandDelegate.progressUpdate(progressInfo);

        return (EMStringConsts.EM_TEXT_RESPONSE_OK.equals(aText));
    }

    // We have received data
    // This will be a file path to a file containing the received data
    // This could be raw data, or it could be XML data
    @Override
    public boolean gotFile(String aDataPath) {
        // Igore - we don't expect to receive a file
        return false;
    }

    // The data has been sent
    @Override
    public void sent() {
        if (mCancelled)
            return;

        // This means we've sent the command, so wait for the response
        mCommandDelegate.getText();
    }

    // Set the delegate to receive notifications about data sending progress
    public void setDataCommandDelegate(EMDataCommandDelegate aDataCommandDelegate) {
        mDataCommandDelegate = aDataCommandDelegate;
    }

    @Override
    public void cancel() {
        mCancelled = true;
    }
}
