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

import android.media.MediaScannerConnection;
import android.content.Context;
import android.util.Log;

import com.pervacio.wds.app.EMMigrateStatus;
import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMSimpleAsyncTask;
import com.pervacio.wds.sdk.CMDError;
import com.pervacio.wds.sdk.internal.CMDCopyFileProgressDelegate;

import java.io.File;
import java.util.ArrayList;

public class CMDGoogleCopyFolderContentsToLocalAsyncTask
        extends    EMSimpleAsyncTask
        implements EMProgressHandler, CMDCopyFileProgressDelegate
{

    private final static String TAG = "CopyFolderContentsToLoc";

    CMDGoogleCopyFolderContentsToLocalAsyncTask(String remoteFolderPath,
                    String localFolderPath,
                    boolean runMediaScannerOnFiles,
                    int dataType, // CMDDataType - used for progress updates
                    CMDGoogleDriveAccess googleDriveAccess,
                    Context context)
    {
        // Log.d(TAG, "+++ CMDGoogleCopyFolderContentsToLocalAsyncTask, Remote: " +remoteFolderPath+ ", Local: " +localFolderPath);

        mRemoteFolderPath = remoteFolderPath;
        mLocalFolderPath = localFolderPath;
        mRunMediaScannerOnFiles = runMediaScannerOnFiles;
        mDataType = dataType;
        mGoogleDriveAccess = googleDriveAccess;
        mContext = context;
    }

    @Override
    public void runTask() {
        // Recursively copy the folder contents

        // Log.d(TAG, "+++ runTask");

        // Get google item ID of the folder that we want to copy from
        String parentId = CMDGoogleUtility.getDriveIdForPathBlocking(mRemoteFolderPath,
                                                                        mGoogleDriveAccess,
                                                                        this);

        if (parentId == null) {
            // TODO: handle error
        }
        else {
            findAndCountRemoteFiles(parentId, mLocalFolderPath);

            // TODO: [later] space check on local
            mPreviousPercentProgress = -1;
            mCurrentFileNumber       = 0;

            // TODO: enumerate through remote files
            for(CMDGoogleFileInfo fileInfo : mGoogleFileInfoList) {

                mCurrentFileNumber++;

                int progressPercent = 0;
                if (mTotalSizeOfRemoteFiles > 0)
                {
                    progressPercent = (int) ((mTotalDataTransferred * 100.0) / mTotalSizeOfRemoteFiles);
                }

                dispatchReceiveProgress(progressPercent);

                // Log.d(TAG, "+++ runTask, Item: " + mCurrentFileNumber + ", Size: " + fileInfo.mFileSize + ", Total: " + mTotalSizeOfRemoteFiles);

                // TODO: move this to utility function and share with fetching an individual file
                // Make local path
                if (fileInfo.mLocalFilePath != null) {
                    File file = new File(fileInfo.mLocalFilePath);
                    file.getParentFile().mkdirs();
                }

                // Copy the remote file to local
                int result = mGoogleDriveAccess.copyFileToLocal(fileInfo.mGoogleItemId,
                        fileInfo.mLocalFilePath,
                        this, this);

                mTotalDataTransferred  += fileInfo.mFileSize;

                if (result != CMDError.CMD_RESULT_OK) {
                    EMMigrateStatus.addItemNotTransferred(mDataType);
                    // setFailed(result);
                    // break;
                }
                else {
                    EMMigrateStatus.addItemTransferred(mDataType);
                    // Run the media scanner on the file if mRunMediaScannerOnFiles
                    String[] paths = {fileInfo.mLocalFilePath};
                    MediaScannerConnection.scanFile(mContext, paths, null, null);
                }
            }
        }
    }

    private void dispatchReceiveProgress(int aPercent)
    {
        EMProgressInfo progressInfo = new EMProgressInfo();
        progressInfo.mDataType          = mDataType;
        progressInfo.mOperationType     = EMProgressInfo.EMOperationType.EM_OPERATION_RECEIVING_DATA;
        progressInfo.mCurrentItemNumber = mCurrentFileNumber;
        progressInfo.mTotalItems        = mGoogleFileInfoList.size();
        progressInfo.mProgressPercent   = aPercent;

        // Log.d(TAG, "+++ dispatchReceiveProgress, Dispatching Percentage Progress: " +aPercent);

        updateProgressFromWorkerThread(progressInfo);
    }

    @Override
    public void onCopyFileProgress(long aTotalFileDataCopied)
    {
        // Log.d(TAG, "+++ onCopyFileProgress, Total: " +aTotalFileDataCopied);

        int progressPercent = 0;

        if (mTotalSizeOfRemoteFiles > 0)
        {
            long transferredTotal = mTotalDataTransferred + aTotalFileDataCopied;
            progressPercent = (int) ((transferredTotal * 100.0) / mTotalSizeOfRemoteFiles);
        }

        if (progressPercent != mPreviousPercentProgress)
        {
            dispatchReceiveProgress(progressPercent);

            mPreviousPercentProgress = progressPercent;
        }
    }

    // Build the list of remote file paths
    // Calculate the total size of the remote files
    // Designed to be called recursively
    private void findAndCountRemoteFiles(String parentId,
                                            String localFolderPath) {
        // Log.d(TAG, "+++ findAndCountRemoteFiles, Parent: " +parentId+ ", Local Path: " +localFolderPath);

        ArrayList<CMDGoogleDriveAccess.CMDGoogleDriveItem> childItems = new ArrayList<CMDGoogleDriveAccess.CMDGoogleDriveItem>();
        int result = mGoogleDriveAccess.listChildren(parentId, childItems, this);

        // Enumerate child files and folders
        for (CMDGoogleDriveAccess.CMDGoogleDriveItem childItem : childItems) {
            String title = childItem.mName;
            String itemId = childItem.mGoogleDriveId;
            String itemPath = localFolderPath + "/" + title;
            // if (childMetadata.isFolder()) {
            if (childItem.mType == CMDGoogleDriveAccess.CMDGoogleDriveItemType.EGoogleDriveFolder) {
                // For folders: recurse (using the nested localFolderPath)
                findAndCountRemoteFiles(itemId, itemPath);
            }
            else {
                // for files:
                long fileSize = childItem.mSize;
                mTotalSizeOfRemoteFiles += fileSize;

                CMDGoogleFileInfo fileInfo = new CMDGoogleFileInfo();
                fileInfo.mGoogleItemId  = itemId;
                fileInfo.mLocalFilePath = itemPath;
                fileInfo.mFileSize      = fileSize;

                mGoogleFileInfoList.add(fileInfo);

                // Log.d(TAG, "+++ findAndCountRemoteFiles, File Size: " +fileSize+ ", Total: " +mTotalSizeOfRemoteFiles);
            }
        }
    }

    private String mRemoteFolderPath;
    private String mLocalFolderPath;
    private boolean mRunMediaScannerOnFiles;
    private int mDataType; // CMDDataType

    private long mTotalSizeOfRemoteFiles;
    private long mTotalDataTransferred;

    @Override
    public void taskComplete(boolean aSuccess)
    {
        // Log.d(TAG, "+++ taskComplete, Success: " +aSuccess);
    }

    @Override
    public void taskError(int errorCode, boolean alreadyDisplayedDialog) {

    }

    @Override
    public void progressUpdate(EMProgressInfo aProgressInfo)
    {
        // Log.d(TAG, "+++ progressUpdate");

        updateProgressFromWorkerThread(aProgressInfo);
    }

    public class CMDGoogleFileInfo {
        public String mGoogleItemId; // The item ID on Google Drive
        public String mLocalFilePath; // The file path to copy the file to
        public long   mFileSize = -1;
    };

    private ArrayList<CMDGoogleFileInfo> mGoogleFileInfoList = new ArrayList<CMDGoogleFileInfo>();

    private CMDGoogleDriveAccess mGoogleDriveAccess;

    private Context mContext;

    int mCurrentFileNumber        = 0;
    int mPreviousPercentProgress = -1;

}
