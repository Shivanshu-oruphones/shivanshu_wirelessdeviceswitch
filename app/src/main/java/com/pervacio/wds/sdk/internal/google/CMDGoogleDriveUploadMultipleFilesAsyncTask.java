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

package com.pervacio.wds.sdk.internal.google;

import android.util.Log;
import android.util.SparseArray;

import com.pervacio.wds.app.EMDataType;
import com.pervacio.wds.app.EMFileMetaData;
import com.pervacio.wds.app.EMMigrateStatus;
import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMSimpleAsyncTask;
import com.pervacio.wds.sdk.CMDError;
import com.pervacio.wds.sdk.internal.CMDCopyFileProgressDelegate;
import com.pervacio.wds.sdk.internal.CMDStringConsts;

import java.util.ArrayList;

public class CMDGoogleDriveUploadMultipleFilesAsyncTask
        extends    EMSimpleAsyncTask
        implements EMProgressHandler, CMDCopyFileProgressDelegate
{

    private ArrayList<EMFileMetaData> mFilesToUpload;
    private int mCurrentFileIndex;

    private CMDGoogleDriveAccess mGoogleDriveAccess;
    private String mRemoteBasePath;

    private final static String TAG = "UploadMultFilesAsync";

    private SparseArray<Long>    mDatasetSizeByType;
    private SparseArray<Long>    mTotalTransferByType;
    private SparseArray<Integer> mProgressByType;


    CMDGoogleDriveUploadMultipleFilesAsyncTask(ArrayList<EMFileMetaData> aFilesToUpload,
                                               String aRemoteBasePath,
                                               CMDGoogleDriveAccess aGoogleDriveAccess) {
        mFilesToUpload     = aFilesToUpload;
        mCurrentFileIndex = 0;

        mGoogleDriveAccess = aGoogleDriveAccess;
        mRemoteBasePath = aRemoteBasePath;

        mDatasetSizeByType   = getTotalSizeOfDatasets(aFilesToUpload);
        mTotalTransferByType = new SparseArray<>();
        mProgressByType      = new SparseArray<>();
    }

    // To be overridden by the derived task
    // This function is run in the new thread
    @Override
    public void runTask() {
        for(EMFileMetaData fileToUpload : mFilesToUpload)
        {
            String subFolderBase = "";

            if (fileToUpload.mDataType == EMDataType.EM_DATA_TYPE_PHOTOS)
                subFolderBase = CMDStringConsts.EM_PHOTOS_FOLDER_NAME + "/";
            if (fileToUpload.mDataType == EMDataType.EM_DATA_TYPE_VIDEO)
                subFolderBase = CMDStringConsts.EM_VIDEOS_FOLDER_NAME + "/";
            if (fileToUpload.mDataType == EMDataType.EM_DATA_TYPE_MUSIC)
                subFolderBase = CMDStringConsts.EM_MUSIC_FOLDER_NAME + "/";
            if (fileToUpload.mDataType == EMDataType.EM_DATA_TYPE_DOCUMENTS)
                subFolderBase = CMDStringConsts.EM_DOCUMENTS_FOLDER_NAME + "/";

            int dataType = fileToUpload.mDataType;

            // Log.d(TAG, "+++ runTask, Index: " +mCurrentFileIndex+ "Type: " +dataType+ ", Name: " +fileToUpload.mFileName);

            long dataTransferred = mTotalTransferByType.get(dataType, (long) 0);
            long datasetSize     = mDatasetSizeByType.get(dataType,   (long) 0);

            int progressPercent = 0;

            if (datasetSize > 0)
            {
                progressPercent = (int) ((dataTransferred * 100.0) / datasetSize);
            }

            dispatchSendingProgress(fileToUpload, progressPercent);

            String remoteFolderPath = mRemoteBasePath + "/" + subFolderBase + fileToUpload.mRelativePath;
            String parentId = CMDGoogleUtility.makePath(remoteFolderPath, mGoogleDriveAccess, this);

            int result = mGoogleDriveAccess.copyFileFromLocal(parentId, fileToUpload.mSourceFilePath, fileToUpload.mFileName, this, this);

            dataTransferred += fileToUpload.mSize;

            mTotalTransferByType.put(dataType, dataTransferred);

            if (result == CMDError.CMD_RESULT_OK) {
                EMMigrateStatus.addItemTransferred(fileToUpload.mDataType);
            }
            else {
                EMMigrateStatus.addItemNotTransferred(fileToUpload.mDataType);

                if (result == CMDError.CMD_GOOGLE_DRIVE_FULL_ERROR) {
                    setFailed(result); // This probably means the drive is full - propagate the error up and fail the transfer
                    break;
                }
            }

            mCurrentFileIndex++;
        }
    }

    private void dispatchSendingProgress(EMFileMetaData aCurrentFile, int aPercent)
    {
        int dataType = aCurrentFile.mDataType;

        Integer prevProgress = mProgressByType.get(dataType);

        if (prevProgress != null)
        {
            if (prevProgress == aPercent)
            {
                // Log.d(TAG, "+++ dispatchSendingProgress, No Progress: " +aPercent);
                return;
            }
        }

        // Log.d(TAG, "+++ dispatchSendingProgress, Dispatching Percentage Progress: " +aPercent);

        EMProgressInfo progressInfo = new EMProgressInfo();
        progressInfo.mOperationType     = EMProgressInfo.EMOperationType.EM_OPERATION_SENDING_DATA;

        progressInfo.mDataType          = aCurrentFile.mDataType;
        progressInfo.mCurrentItemNumber = aCurrentFile.mCurrentFileNumber;
        progressInfo.mTotalItems        = aCurrentFile.mTotalFiles;
        progressInfo.mProgressPercent   = aPercent;

        mProgressByType.put(dataType, aPercent);

        updateProgressFromWorkerThread(progressInfo);
    }

    public void onCopyFileProgress(long aTotalFileDataCopied)
    {
        // Log.d(TAG, "+++ onCopyFileProgress, Total: " + aTotalFileDataCopied);

        if (mCurrentFileIndex >= mFilesToUpload.size())
        {
            return;
        }

        EMFileMetaData currentFile = mFilesToUpload.get(mCurrentFileIndex);

        int dataType = currentFile.mDataType;

        long dataTransferred = mTotalTransferByType.get(dataType, (long) 0);
        long datasetSize     = mDatasetSizeByType.get(dataType,   (long) 0);

        dataTransferred += aTotalFileDataCopied;

        int progressPercent = 0;

        if (datasetSize > 0)
        {
            progressPercent = (int) ((dataTransferred * 100.0) / datasetSize);
        }

        dispatchSendingProgress(currentFile, progressPercent);
    }

    private SparseArray<Long> getTotalSizeOfDatasets(ArrayList<EMFileMetaData> aFiles)
    {
        SparseArray<Long> sizeByType = new SparseArray<>();

        for (EMFileMetaData thisFile : aFiles)
        {
            // Log.d(TAG, "+++ getTotalSizeOfLocalFiles, File: " + thisFile.mFileName+ ", Size: " +thisFile.mSize);

            int dataType  = thisFile.mDataType;
            long thisSize = thisFile.mSize;

            long total = sizeByType.get(dataType, (long) 0);

            total += thisSize;

            sizeByType.put(dataType, total);
        }

        return sizeByType;
    }

    @Override
    public void taskComplete(boolean aSuccess) {
        // Not called
    }

    @Override
    public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
        // Not called
    }

    @Override
    public void progressUpdate(EMProgressInfo aProgressInfo) {
        // Used to report changes to the connection status
        updateProgressFromWorkerThread(aProgressInfo);
    }
}
