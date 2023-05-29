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
import android.util.Log;

public class ATimer extends Object implements Handler.Callback
{
	public interface Callback
	{
		void onATimerTick(int aId);
	}
	
	private Callback mCallback;
	private int  mId;
	private long mTick;
	
	private Handler mHandler = null;
	
	private boolean mIsRunning = false;
	
	public ATimer(Callback aCallback, int aId, long aTick)
	{
		super();
		log("ATimer, Id: " + aId + ", Tick: " + aTick);
		
		mIsRunning  = false;
		
		mId       = aId;
		mTick     = aTick;
		mCallback = aCallback;
		
		mHandler = new Handler(this);
	}

	public void start()
	{
		log("start");
		mHandler.removeMessages(mId);
		mHandler.sendEmptyMessageDelayed(mId, mTick);
		
		mIsRunning = true;
	}
	
	public void cancel()
	{
		log("cancel");
		mHandler.removeMessages(mId);
		
		mIsRunning = false;
	}
	
	public boolean handleMessage(Message aMessage)
	{
		log("handleMessage");
	
		mHandler.removeMessages(mId);

		mCallback.onATimerTick(mId);
		
		if (mIsRunning)
		{
			mHandler.sendEmptyMessageDelayed(mId, mTick);
		}
		
		return true;
	}
	
	//=============================================================================
	//=============================================================================
	private final static String kTagModule = "ATimer";

	static private void trace(String aText)
	{
		Log.v(kTagModule, aText);
	}
	
	static private void log(String aText)
	{
		// Log.d(kTagModule, aText);
	}
	
	private void note(String aText)
	{
		// Log.d(kTagModule, aText);
	}
	
	private void warn(String aText)
	{
		Log.w(kTagModule, "WARN: " + aText);
	}
	
	private void error(String aText)
	{
		Log.e(kTagModule, "ERROR: " + aText);
	}
}
