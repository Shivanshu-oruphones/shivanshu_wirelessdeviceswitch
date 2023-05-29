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

public abstract class EMGenerateDataTask extends EMSimpleAsyncTask {
    public String getFilePath()
    {
        return mFilePath;
    }

    public void setFilePath(String aFilePath)
    {
        mFilePath = aFilePath;
    }

    private String mFilePath;
    private EMCommandDelegate mCommandDelegate;

    public void setCommandDelegate(EMCommandDelegate aCommandDelegate) {
        mCommandDelegate = aCommandDelegate;
    }

    protected void addToPreviouslyTransferredItems(String aItem) {
        if (mCommandDelegate != null)
            mCommandDelegate.addToPreviouslyTransferredItems(aItem);
    }

    protected boolean itemHasBeenPreviouslyTransferred(String aItem) {
        if (mCommandDelegate != null)
            return mCommandDelegate.itemHasBeenPreviouslyTransferred(aItem);
        else
            return false;
    }
}
