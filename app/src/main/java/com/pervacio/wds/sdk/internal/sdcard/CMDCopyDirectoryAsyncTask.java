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

import android.content.Context;
import android.media.MediaScannerConnection;

import com.pervacio.wds.app.EMMigrateStatus;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMSimpleAsyncTask;
import com.pervacio.wds.sdk.CMDError;
import com.pervacio.wds.sdk.internal.CMDCryptoSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CMDCopyDirectoryAsyncTask extends EMSimpleAsyncTask {
        public CMDCopyDirectoryAsyncTask(File sourceLocation,
            File targetLocation,
            boolean runMediaScannerOnFiles, // Set to true if the Android media scanner should be run on copied files
            int dataType,
            Context aContext) {
            mSourceLocation = sourceLocation;
            mTargetLocation = targetLocation;
            mRunMediaScannerOnFiles = runMediaScannerOnFiles; // Set to true if the Android media scanner should be run on copied files
            mDataType = dataType;
            mContext = aContext;
        }

    @Override
    protected void runTask() {
        mCurrentCopyItemNumber = 0;
        mTotalItems = 0;

        EMProgressInfo progressInfo = new EMProgressInfo();
        progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_FINDING_FILES;
        progressInfo.mDataType = mDataType;
        updateProgressFromWorkerThread(progressInfo);

        try {
            copyDirectoryRecursive(mSourceLocation,
                    mTargetLocation,
                    mRunMediaScannerOnFiles,
                    mDataType,
                    true); // TODO: handle error?

            mTotalItems = mCurrentCopyItemNumber;
            mCurrentCopyItemNumber = 0;

            copyDirectoryRecursive(mSourceLocation,
                    mTargetLocation,
                    mRunMediaScannerOnFiles,
                    mDataType,
                    false);
        }
        catch (Exception exception) {
            setFailed(CMDError.CMD_SD_CARD_ERROR_COPYING_DIRETORY);
        }
    }

    // Based on StackOverflow example
    // Returns the number of files copied (or number of files counted if countOnly mode)
    public boolean copyDirectoryRecursive(File sourceLocation,
                                          File targetLocation,
                                          boolean runMediaScannerOnFiles, // Set to true if the Android media scanner should be run on copied files
                                          int dataType,
                                          boolean countOnly) throws IOException {
        if (!sourceLocation.exists())
            return true; // Do nothing if the folder doesn't exist

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists() && !targetLocation.mkdirs()) {
                throw new IOException("Cannot create dir " + targetLocation.getAbsolutePath());
            }

            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                copyDirectoryRecursive(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]),
                        runMediaScannerOnFiles,
                        dataType,
                        countOnly);
            }
        } else {
            String filename = sourceLocation.getName();
            if ((filename.equalsIgnoreCase("BBThumbs.dat"))
                    || (filename.startsWith("."))){
                return true; // This is a file insterted on the SD card by BBOS. It should be ignored.
            }

            mCurrentCopyItemNumber++;

            if (!countOnly) {
                // make sure the directory we plan to store the recording in exists
                File directory = targetLocation.getParentFile();
                if (directory != null && !directory.exists() && !directory.mkdirs()) {
                    throw new IOException("Cannot create dir " + directory.getAbsolutePath());
                }

                EMProgressInfo progressInfo = new EMProgressInfo();
                progressInfo.mDataType = dataType;
                progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_RECEIVING_DATA;
                progressInfo.mCurrentItemNumber = mCurrentCopyItemNumber;
                progressInfo.mTotalItems = mTotalItems;
                updateProgressFromWorkerThread(progressInfo);

//                progressHandler.progressUpdate(progressInfo);

                InputStream in = new FileInputStream(sourceLocation);
                OutputStream out = new FileOutputStream(targetLocation);

                if (CMDCryptoSettings.enabled())
                    try {
                        out = CMDCryptoSettings.getCipherDecryptOutputStream(out); // Decrypt when writing to the output file (if crypto is enabled)
                    } catch (Exception e) {
                        return false;
                    }

                // Copy the bits from instream to outstream
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                    EMMigrateStatus.addBytesTransferred(len);
                }
                in.close();
                out.close();

                EMMigrateStatus.addItemTransferred(dataType);

                if (runMediaScannerOnFiles) {
                    // Run the media scanner on the file if mRunMediaScannerOnFiles
                    String[] paths = {targetLocation.getAbsolutePath()};
                    MediaScannerConnection.scanFile(mContext, paths, null, null);
                }
            }
        }

        return true;
    }

    private File mSourceLocation;
    private File mTargetLocation;
    private boolean mRunMediaScannerOnFiles; // Set to true if the Android media scanner should be run on copied files
    private int mDataType;
    private int mCurrentCopyItemNumber;
    private int mTotalItems;
    private Context mContext;
}
