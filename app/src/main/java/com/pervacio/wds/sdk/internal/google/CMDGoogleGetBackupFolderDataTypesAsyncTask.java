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

import com.pervacio.wds.app.EMDataType;
import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMSimpleAsyncTask;
import com.pervacio.wds.sdk.CMDError;
import com.pervacio.wds.sdk.internal.CMDStringConsts;

import java.util.ArrayList;
import java.util.HashMap;

public class CMDGoogleGetBackupFolderDataTypesAsyncTask
        extends    EMSimpleAsyncTask
        implements EMProgressHandler
{
    private static final HashMap<String, Integer> mDataTypeByFileName = new HashMap<String, Integer>()
    {{
        put(CMDStringConsts.EM_CONTACTS_FILE_NAME,    EMDataType.EM_DATA_TYPE_CONTACTS);
        put(CMDStringConsts.EM_CALENDAR_FILE_NAME,    EMDataType.EM_DATA_TYPE_CALENDAR);
        put(CMDStringConsts.EM_PHOTOS_FOLDER_NAME,    EMDataType.EM_DATA_TYPE_PHOTOS);
        put(CMDStringConsts.EM_VIDEOS_FOLDER_NAME,    EMDataType.EM_DATA_TYPE_VIDEO);
        put(CMDStringConsts.EM_SMS_FILE_NAME,         EMDataType.EM_DATA_TYPE_SMS_MESSAGES);
        put(CMDStringConsts.EM_NOTES_FILE_NAME,       EMDataType.EM_DATA_TYPE_NOTES);
        put(CMDStringConsts.EM_TASKS_FILE_NAME,       EMDataType.EM_DATA_TYPE_TASKS);
        put(CMDStringConsts.EM_DOCUMENTS_FOLDER_NAME, EMDataType.EM_DATA_TYPE_DOCUMENTS);
        put(CMDStringConsts.EM_MUSIC_FOLDER_NAME,     EMDataType.EM_DATA_TYPE_MUSIC);
        put(CMDStringConsts.EM_ACCOUNTS_FILE_NAME,    EMDataType.EM_DATA_TYPE_ACCOUNTS);
    }};

    private CMDGoogleDriveAccess mGoogleDriveAccess;
    private String               mRemoteFolderPath;

    private final static String TAG = "GetBackupFolderTypes";

    CMDGoogleGetBackupFolderDataTypesAsyncTask(String aRemoteFolderPath,
                                               CMDGoogleDriveAccess aGoogleDriveAccess)
    {
        // Log.d(TAG, "+++ CMDGoogleGetBackupFolderDataTypesAsyncTask, Remote: " +aRemoteFolderPath);

        mRemoteFolderPath  = aRemoteFolderPath;
        mGoogleDriveAccess = aGoogleDriveAccess;
    }

    @Override
    public void runTask()
    {
        // Recursively copy the folder contents

        // Log.d(TAG, "+++ runTask");

        // Get google item ID of the folder that we want to copy from
        String parentId = CMDGoogleUtility.getDriveIdForPathBlocking(mRemoteFolderPath,
                                                                        mGoogleDriveAccess,
                                                                        this);
        if (parentId == null)
        {
            dispatchProgressUpdate(-1);
            return;
        }

        ArrayList<CMDGoogleDriveAccess.CMDGoogleDriveItem> childItems = new ArrayList<CMDGoogleDriveAccess.CMDGoogleDriveItem>();

        int result = mGoogleDriveAccess.listChildren(parentId, childItems, this);

        if (result != CMDError.CMD_RESULT_OK)
        {
            dispatchProgressUpdate(-1);
            return;
        }

        int foundDataTypes = 0;

        for (CMDGoogleDriveAccess.CMDGoogleDriveItem childItem : childItems)
        {
            String fileName = childItem.mName;

            Integer thisTypeId = mDataTypeByFileName.get(fileName);

            if (thisTypeId == null)
            {
                // Log.d(TAG, "+++ runTask, Ignoring File: " +fileName);
                continue;
            }

            int thisId = thisTypeId;

            // Log.d(TAG, "+++ runTask, Found Data Item: " +fileName+ ", Type: " +thisId);

            foundDataTypes |= thisId;
        }

        // Log.d(TAG, "+++ runTask, Data Types Found: " + foundDataTypes);

        dispatchProgressUpdate(foundDataTypes);
    }

    private void dispatchProgressUpdate(int aDataTypes)
    {
        EMProgressInfo progressInfo = new EMProgressInfo();
        progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_RESTORING_FROM_BACKUP;
        progressInfo.mDataType      = aDataTypes;

        updateProgressFromWorkerThread(progressInfo);
    }


    @Override
    public void taskComplete(boolean aSuccess)
    {
        // Log.d(TAG, "+++ CMDGoogleGetBackupFolderDataTypesAsyncTask::taskComplete, Success: " +aSuccess);
    }

    @Override
    public void taskError(int errorCode, boolean alreadyDisplayedDialog)
    {
        // Log.d(TAG, "+++ CMDGoogleGetBackupFolderDataTypesAsyncTask::taskError, error: " +errorCode);
    }

    @Override
    public void progressUpdate(EMProgressInfo aProgressInfo)
    {
        // Log.d(TAG, "+++ CMDGoogleGetBackupFolderDataTypesAsyncTask::progressUpdate");

        updateProgressFromWorkerThread(aProgressInfo);
    }

}
