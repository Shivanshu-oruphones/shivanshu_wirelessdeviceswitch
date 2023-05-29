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

import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.pervacio.wds.sdk.internal.CMDUtility;

import java.io.File;
import java.util.ArrayList;

public class EMFileFinder extends EMSimpleAsyncTask {

    public EMFileFinder(int aDataTypes,
                        ArrayList<EMFileMetaData> aFileList) { // The file list that will be added to. Note that this should not be used while the operation is running because it is not threadsafe
        mDataTypes = aDataTypes;
        mFileList = aFileList;
    }

    long mTotalFoundFileSize;

    public long totalFoundFileSize() {
        return mTotalFoundFileSize;
    }

    @Override
    protected void runTask() {
        mTotalFoundFileSize = 0;
        addFilesToListForType(mDataTypes, mFileList);
    }

    void addFilesToListForType(int aDataTypes,
                               ArrayList<EMFileMetaData> aFileList) {
        if ((mDataTypes & EMDataType.EM_DATA_TYPE_PHOTOS) != 0) {
            addMediaFilesToListForType(EMDataType.EM_DATA_TYPE_PHOTOS, mFileList);
        }

        if ((mDataTypes & EMDataType.EM_DATA_TYPE_VIDEO) != 0) {
            addMediaFilesToListForType(EMDataType.EM_DATA_TYPE_VIDEO, mFileList);
        }

        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= 19) {
            if ((aDataTypes & EMDataType.EM_DATA_TYPE_DOCUMENTS) != 0) {
                String filePath = Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOCUMENTS;
                File rootFolderForDataType = new File(filePath);
                CMDUtility.buildFileListRecursive(rootFolderForDataType, EMDataType.EM_DATA_TYPE_DOCUMENTS, aFileList);

                for (EMFileMetaData metaData : aFileList) {
                    if (isCancelled()) {
                        return;
                    }

                    mTotalFoundFileSize += metaData.mSize;
                }
            }
        }

        if ((mDataTypes & EMDataType.EM_DATA_TYPE_MUSIC) != 0) {
            addMediaFilesToListForType(EMDataType.EM_DATA_TYPE_MUSIC, mFileList);
        }
    }

    void addMediaFilesToListForType(int aDataType,
                                    ArrayList<EMFileMetaData> aFileList) {
        int foundFilesTotal = 0;

        ArrayList<EMFileMetaData> addedFiles = new ArrayList<EMFileMetaData>();

        try {
            Uri uri = null;

            if (aDataType == EMDataType.EM_DATA_TYPE_PHOTOS)
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            else if (aDataType == EMDataType.EM_DATA_TYPE_VIDEO)
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            else if (aDataType == EMDataType.EM_DATA_TYPE_MUSIC)
                uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            Cursor cursor = EMUtility.Context().getContentResolver().query(uri,
                    null,        // projection : TODO: optimize this
                    null,       // where - get all rows
                    null,       // arguments - none
                    null        // orderng - doesn't matter
            );

            int filePathColumn = -1;
            int currentFileNumber = 0;

            if (aDataType == EMDataType.EM_DATA_TYPE_PHOTOS)
                filePathColumn = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            else if (aDataType == EMDataType.EM_DATA_TYPE_VIDEO)
                filePathColumn = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA);
            else if (aDataType == EMDataType.EM_DATA_TYPE_MUSIC) {
                filePathColumn = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
            }

            if (!cursor.moveToFirst())
                return;

            do {
                // TODO:
                try {
                    if (isCancelled()) {
                        cursor.close();
                        return;
                    }

                    String filePath = cursor.getString(filePathColumn);
                    File file = new File(filePath);

                    if (!file.exists())
                        continue;
                    else {
                        EMFileMetaData metaData = new EMFileMetaData();
                        metaData.mSourceFilePath = filePath;
                        metaData.mSize = file.length();
                        metaData.mFileName = file.getName();
                        metaData.mDataType = aDataType;
                        metaData.mCurrentFileNumber = ++currentFileNumber;
                        metaData.mRelativePath = EMUtility.relativePathForFilePath(filePath);
                        mTotalFoundFileSize += metaData.mSize;
                        foundFilesTotal++;

                        // Log.d(TAG, "=== sendFile, Current: " + currentFileNumber + ", List: " + mFileList.size());

                        aFileList.add(metaData);
                        addedFiles.add(metaData);
                    }
                } catch (Exception ex) {
                    // TODO: handle the exception
                }

            } while (cursor.moveToNext());
        }
        catch (Exception exception) {
            DLog.log(exception);
            EMMigrateStatus.setTotalFailure(aDataType);
        }

        // Go through the list of file metadata and fix up the total files count
        for (EMFileMetaData metaData : addedFiles) {
            // only set the mTotalFiles on the files we've just added (there might be other files of other types in there)
            metaData.mTotalFiles = foundFilesTotal;
            // Log.d(TAG, "Fixed up total items: mDataType: " + metaData.mDataType
//                    + " mCurrentFileNumber: " + metaData.mCurrentFileNumber
//                    + " mTotalFiles: " + metaData.mTotalFiles);
        }
    }

    private int mDataTypes;
    ArrayList<EMFileMetaData> mFileList;

    private static final String TAG = "EMFileFinder";
}
