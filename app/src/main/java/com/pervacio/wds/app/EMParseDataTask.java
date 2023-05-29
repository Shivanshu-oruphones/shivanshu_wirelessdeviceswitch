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

import android.content.ContentResolver;
import android.content.Context;

import java.io.File;

abstract public class EMParseDataTask extends EMSimpleAsyncTask {
    public void startTask(String aFilePath,
                boolean aDeleteFileAfterParsing,
                Context aContext,
                EMProgressHandler aProgressHandler) {
        mFilePath = aFilePath;
        mDeleteFileAfterParsing = aDeleteFileAfterParsing;
        mContext = aContext;
        startTask(aProgressHandler);
    }

    @Override
    protected void runTask() {
        parseData();
        if (mDeleteFileAfterParsing) {
            File file = new File(mFilePath);
            file.delete();
        }
    }

    abstract protected void parseData();

    protected Context mContext;
    protected boolean mDeleteFileAfterParsing;
    protected String mFilePath;
}
