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

package com.pervacio.wds.sdk.internal.sdcard;

import com.pervacio.wds.app.EMSimpleAsyncTask;

import java.io.File;

public class CMDSDCardRecursiveDeleteAsyncTask extends EMSimpleAsyncTask {

    private void DeleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                DeleteRecursive(child);

        fileOrDirectory.delete();
    }

    public CMDSDCardRecursiveDeleteAsyncTask(String aPath) {
        mPath = aPath;
    }

    @Override
    public void runTask() {
        DeleteRecursive(new File(mPath));
    }

    String mPath;
}
