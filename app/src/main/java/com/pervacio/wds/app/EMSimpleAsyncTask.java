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

import android.os.AsyncTask;
import android.os.Build;

abstract public class EMSimpleAsyncTask extends AsyncTask<Void, EMProgressInfo, Void> {
	
	private EMProgressHandler mDelegate;

	// To be called by the client, from their own thread
	public void startTask(EMProgressHandler aDelegate) {

		mDelegate = aDelegate;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		else
			execute();
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

	// To be called from within the runTask implementation on the derived class to update the progress in the parent thread
	protected void updateProgressFromWorkerThread(EMProgressInfo aProgressInfo)
	{
		publishProgress(aProgressInfo);
	}

	@Override
	protected Void doInBackground(Void... params) {
		runTask();
		return null;
	}
	
	protected void onProgressUpdate (EMProgressInfo... values)
	{
		mDelegate.progressUpdate(values[0]);
	}
	
	@Override
	protected void onPostExecute(Void result) {
		if (!mFailed)
			mDelegate.taskComplete(true);
		else
			mDelegate.taskError(mError, false);
	}
}
