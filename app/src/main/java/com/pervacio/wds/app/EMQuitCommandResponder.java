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
import android.os.Message;

import com.google.gson.Gson;
import com.pervacio.wds.app.ui.EasyMigrateActivity;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;

public class EMQuitCommandResponder implements EMCommandHandler {
	
    private EMCommandDelegate mCommandDelegate;
    private EMDataCommandDelegate mDataCommandDelegate;
	private boolean mCancelled;
	
	// Start this command handler
	@Override
	public void start(EMCommandDelegate aDelegate)
	{
		mCancelled = false;
	    mCommandDelegate = aDelegate;
	    sendCommand();
	}

	private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			handler.removeMessages(msg.what);

			if(Constants.IS_MMDS && EasyMigrateActivity.iosOnlySMSSelected && !EasyMigrateActivity.iosSMSRestored) {
				DLog.log("In Quit Command pre check");
				handler.sendEmptyMessageDelayed(msg.what, 2000);
				return;
			} else {
				CommonUtil.getInstance().setMigrationStatus(Constants.MIGRATION_SUCCEEDED);
			}

			if (CommonUtil.getInstance().isMigrationDone()) {
				StringBuilder response = new StringBuilder(EMStringConsts.EM_TEXT_RESPONSE_OK);
				if (msg.what == 1) {
					response.append("#");
					response.append(new Gson().toJson(DashboardLog.getInstance().geteDeviceSwitchSession()));
				}
				mCommandDelegate.sendText(response.toString());
			} else {
				handler.sendEmptyMessageDelayed(msg.what, 2000);
			}
		}
	};

	private void sendCommand() {
		if(Constants.IS_MMDS){
			if( EasyMigrateActivity.iosOnlySMSSelected && !EasyMigrateActivity.iosSMSRestored) {
               //Skip sending status as success to ME as messages are still pending
			} else {
				CommonUtil.getInstance().setMigrationStatus(Constants.MIGRATION_SUCCEEDED);
			}
			handler.sendEmptyMessageDelayed(0, 2000);
		}else {
			try {
				if (DashboardLog.getInstance().sourceEMDeviceInfo.dbDevicePlatform.equalsIgnoreCase(Constants.PLATFORM_ANDROID)) {
					EMProgressInfo progressInfo = new EMProgressInfo();
					progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_UPDATE_DB_DETAILS;
					mDataCommandDelegate.progressUpdate(progressInfo);
					handler.sendEmptyMessageDelayed(1, 2000);
				} else {
					mCommandDelegate.sendText(EMStringConsts.EM_TEXT_RESPONSE_OK);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// Returns true if this command hander handles the given command - only relevant to responders (as opposed to initiators)
	@Override
	public boolean handlesCommand(String aCommand)
	{
	    return aCommand.equalsIgnoreCase(EMStringConsts.EM_COMMAND_TEXT_QUIT);
	}

	// We have text (usually a command or a response)
	@Override
	public boolean gotText(String aText)
	{
	    // Ignore - we don't listen for any text (other than the initial command)
	    return true;
	}

	// We have received data
	// This will be a file path to a file containing the received data
	// This could be raw data, or it could be XML data
	@Override
	public boolean gotFile(String aDataPath)
	{
	    // Ignore - we don't listen for any files
	    return true;
	}

	// The data has been sent
	@Override
	public void sent()
	{
		if (mCancelled)
			return;

	    mCommandDelegate.commandComplete(true);
	    
	    // We have sent our OK response, so notify the you-are-target delegate that it is now the target
	    EMProgressInfo progressInfo = new EMProgressInfo();
	    progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_QUIT_COMMAND_RECEIVED;
	    mDataCommandDelegate.progressUpdate(progressInfo);
	}

	// Set the delegate to receive notifications about the quitting operating
	public void setDataCommandDelegate(EMDataCommandDelegate aDataCommandDelegate)
	{
	    mDataCommandDelegate = aDataCommandDelegate;
	}

	@Override
	public void cancel() {
		mCancelled = true;
	}
}
