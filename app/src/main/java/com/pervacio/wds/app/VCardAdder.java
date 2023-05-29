/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pervacio.wds.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import com.pervacio.pim.vcard.EntryCommitter;
import com.pervacio.pim.vcard.VCardBuilder;
import com.pervacio.pim.vcard.VCardBuilderCollection;
import com.pervacio.pim.vcard.VCardConfig;
import com.pervacio.pim.vcard.VCardDataBuilder;
import com.pervacio.pim.vcard.VCardEntryCounter;
import com.pervacio.pim.vcard.VCardParser;
import com.pervacio.pim.vcard.VCardParser_V21;
import com.pervacio.pim.vcard.VCardParser_V30;
import com.pervacio.pim.vcard.VCardSourceDetector;
import com.pervacio.pim.vcard.exception.VCardException;
import com.pervacio.pim.vcard.exception.VCardNestedException;
import com.pervacio.pim.vcard.exception.VCardNotSupportedException;
import com.pervacio.pim.vcard.exception.VCardVersionException;

public class VCardAdder {

	private static final String LOG_TAG = "VCardAdder";
	private Context mContext;
	private ContentResolver mResolver;
	private VCardParser mVCardParser;
	private boolean mCanceled = false;
	private PowerManager.WakeLock mWakeLock;
	private List<String> mErrorFileNameList;

	public VCardAdder(Context aActivityContext) {
		// TODO: initialize with whatever we can share between vCard adds
		mContext = aActivityContext;
		mResolver = mContext.getContentResolver();
        PowerManager powerManager = (PowerManager)mContext.getSystemService(
                Context.POWER_SERVICE);
		mWakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK |
                PowerManager.ON_AFTER_RELEASE, LOG_TAG);
	}
	
	public void Close() {
		// TODO: clear up anything that isn't needed
	}
	
	public void ProcessVCard(InputStream aInputStream) {
		//// Log.d(LOG_TAG, ">> ProcessVCard");
		mErrorFileNameList = new ArrayList<String>();
        boolean shouldCallFinish = true;
        mWakeLock.acquire();
        // Some malicious vCard data may make this thread broken
        // (e.g. OutOfMemoryError).
        // Even in such cases, some should be done.
        try {
            // Count the number of VCard entries
            long start;
            VCardEntryCounter counter = new VCardEntryCounter();
            VCardSourceDetector detector = new VCardSourceDetector();
            VCardBuilderCollection builderCollection = new VCardBuilderCollection(
                    Arrays.asList(counter, detector));

            boolean result;
            try {
                result = readOneVCardFile(aInputStream,
                        VCardConfig.DEFAULT_CHARSET, builderCollection, null, true, null);
            } catch (VCardNestedException e) {
                try {
                    // Assume that VCardSourceDetector was able to detect the source.
                    // Try again with the detector.
                    result = readOneVCardFile(aInputStream,
                            VCardConfig.DEFAULT_CHARSET, counter, detector, false, null);
                } catch (VCardNestedException e2) {
                    result = false;
                    Log.e(LOG_TAG, "Must not reach here. " + e2);
                }
            }
            if (!result) {
                shouldCallFinish = false;
                return;
            }

            String charset = detector.getEstimatedCharset();
            try {
            	aInputStream.reset();
            } catch (Exception ex) {
            	Log.e(LOG_TAG, "Error resetting input stream");
            }
            doActuallyReadOneVCard(aInputStream, null, charset, true, detector, mErrorFileNameList);

        } finally {
            mWakeLock.release();
            // finish() is called via mCancelListener, which is used in DialogDisplayer.
//            if (shouldCallFinish && !isFinishing()) {
//                if (mErrorFileNameList == null || mErrorFileNameList.isEmpty()) {
//                    finish();
//                } else {
//                    StringBuilder builder = new StringBuilder();
//                    boolean first = true;
                   
                    /*
                    for (String fileName : mErrorFileNameList) {
                        if (first) {
                            first = false;
                        } else {
                            builder.append(", ");
                        }
                    
                        builder.append(fileName);
                    }
*/
 
                
                    
                    // TODO: error here: just log?
//                }
//            }
        }
        
		//// Log.d(LOG_TAG, "<< ProcessVCard");
    }

    private boolean doActuallyReadOneVCard(InputStream aInputStream, Account account,
            String charset, boolean showEntryParseProgress,
            VCardSourceDetector detector, List<String> errorFileNameList) {
//        final Context context = ImportVCardActivity.this;
    	
        VCardDataBuilder builder;
        final String currentLanguage = Locale.getDefault().getLanguage();
        int vcardType = VCardConfig.getVCardTypeFromString("default");
        // Account dummyAccount = new Account(null, null);
        if (charset != null) {
            builder = new VCardDataBuilder(charset, charset, false, vcardType, null);
        } else {
            charset = VCardConfig.DEFAULT_CHARSET;
            builder = new VCardDataBuilder(null, null, false, vcardType, null);
        }
        builder.addEntryHandler(new EntryCommitter(mResolver));

        try {
            if (!readOneVCardFile(aInputStream, charset, builder, detector, false, null)) {
                return false;
            }
        } catch (VCardNestedException e) {
            Log.e(LOG_TAG, "Never reach here.");
        }
        return true;
    }

    private boolean readOneVCardFile(InputStream is, String charset,
            VCardBuilder builder, VCardSourceDetector detector,
            boolean throwNestedException, List<String> errorFileNameList)
            throws VCardNestedException {
        try {
            mVCardParser = new VCardParser_V21(detector);

            try {
                mVCardParser.parse(is, charset, builder, mCanceled);
            } catch (VCardVersionException e1) {
                try {
                    is.close();
                } catch (IOException e) {
                }
                is.reset();

                try {
                    mVCardParser = new VCardParser_V30();
                    mVCardParser.parse(is, charset, builder, mCanceled);
                } catch (VCardVersionException e2) {
                    throw new VCardException("vCard with unspported version.");
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException was emitted: " + e.getMessage());

            if (errorFileNameList != null) {
//              errorFileNameList.add(canonicalPath);
            } else {
            	// TODO: log error?
            }
            return false;
        } catch (VCardNotSupportedException e) {
            if ((e instanceof VCardNestedException) && throwNestedException) {
                throw (VCardNestedException)e;
            }
            if (errorFileNameList != null) {
//                errorFileNameList.add(canonicalPath);
            } else {
            	// TODO: log error?
            }
            return false;
        } catch (VCardException e) {
            if (errorFileNameList != null) {
            	// TODO: log error?
            } else {
            	// TODO: log error?
            }
            return false;
        }
        return true;
    }

}
