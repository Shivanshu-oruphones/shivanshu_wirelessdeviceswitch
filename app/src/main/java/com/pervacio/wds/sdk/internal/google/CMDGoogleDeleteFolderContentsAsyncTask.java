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

import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMSimpleAsyncTask;

import java.util.ArrayList;

// TODO: can we share some of the recursive code with CMDGoogleCopyFolderContentsToLocalAsyncTask?
public class CMDGoogleDeleteFolderContentsAsyncTask extends EMSimpleAsyncTask implements EMProgressHandler {
                    public CMDGoogleDeleteFolderContentsAsyncTask(String remoteFolderPath,
                                                                  CMDGoogleDriveAccess googleDriveAccess) {
        mRemoteFolderPath = remoteFolderPath;
        mGoogleDriveAccess = googleDriveAccess;
    }

    @Override
    public void runTask() {
        // Recursively delete the folder contents

        // Get google item ID of the folder that we want to copy from
        String parentId = CMDGoogleUtility.getDriveIdForPathBlocking(mRemoteFolderPath,
                                                                        mGoogleDriveAccess,
                                                                        this);

        if (parentId == null) {
            // TODO: handle error
        }
        else {
            findAndCountRemoteItems(parentId);

            // Dnumerate through remote files
            for(CMDGoogleFileInfo fileInfo : mGoogleFileInfoList) {
                mGoogleDriveAccess.deleteRequest(fileInfo.mGoogleItemId);
            }

            // Dnumerate through remote files
            for(CMDGoogleFileInfo fileInfo : mGoogleFolderInfoList) {
                mGoogleDriveAccess.deleteRequest(fileInfo.mGoogleItemId);
            }
        }

        mGoogleDriveAccess.deleteRequest(parentId);
    }

    // Build the list of remote file paths
    // Calculate the total size of the remote files
    // Designed to be called recursively
    private void findAndCountRemoteItems(String parentId) {
        ArrayList<CMDGoogleDriveAccess.CMDGoogleDriveItem> childItems = new ArrayList<CMDGoogleDriveAccess.CMDGoogleDriveItem>();
        int result = mGoogleDriveAccess.listChildren(parentId, childItems,this);

        // Enumerate child files and folders
        for (CMDGoogleDriveAccess.CMDGoogleDriveItem childItem : childItems) {
            String itemId = childItem.mGoogleDriveId;

            CMDGoogleFileInfo fileInfo = new CMDGoogleFileInfo();
            fileInfo.mGoogleItemId = itemId;

            if (childItem.mType == CMDGoogleDriveAccess.CMDGoogleDriveItemType.EGoogleDriveFolder) {
                // For folders: recurse (using the nested localFolderPath)
                findAndCountRemoteItems(itemId);
                mGoogleFolderInfoList.add(fileInfo);
            }
            else {
                mGoogleFileInfoList.add(fileInfo);
            }
        }
    }

    private String mRemoteFolderPath;

    @Override
    public void taskComplete(boolean aSuccess) {

    }

    @Override
    public void taskError(int errorCode, boolean alreadyDisplayedDialog) {

    }

    @Override
    public void progressUpdate(EMProgressInfo aProgressInfo) {
        // Used for reporting loss of connectivity
        // TODO: how should we report this for deletes? Is there anything we can do?
    }

    public class CMDGoogleFileInfo {
        public String mGoogleItemId; // The item ID on Google Drive
    };

    private ArrayList<CMDGoogleFileInfo> mGoogleFileInfoList = new ArrayList<CMDGoogleFileInfo>();
    private ArrayList<CMDGoogleFileInfo> mGoogleFolderInfoList = new ArrayList<CMDGoogleFileInfo>();

    private CMDGoogleDriveAccess mGoogleDriveAccess;
}
