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

package com.pervacio.wds.sdk.internal;

import com.pervacio.wds.app.EMFileMetaData;
import com.pervacio.wds.app.EMProgressHandler;

import java.util.ArrayList;

public class CMDLocalFileSystemAccess implements CMDFileSystemInterface {

    // Uploads a file to the cloud service
    // The destination path must be in Unix format
    // If the folders are not present on the remote service then they will be created
    // TODO: should the mkpath bit be optional?
    public void uploadFileAsync(String sourcePath,
                         CMDRemoteFileInfo remoteFileInfo,
                         EMProgressHandler progressHandler,
                         int totalSubItems) // Used to provide smoother progress information when sending large files (value should be the number of items in the file, e.g. number of contacts)
    {
        // TODO:
    }

    // Downloads the file to the specified local path
    // The path must include the target file name
    // The specified folder must exist
    public void downloadFileAsync(String localPath,
                           CMDRemoteFileInfo remoteFileInfo,
                           EMProgressHandler progressHandler)
    {
        // TODO:
    }

    // Deletes a folder and all its contents on the cloud service
    public void deleteFolderAsync(String remoteFolderPath,
                           EMProgressHandler progressHandler)
    {
        // TODO:
    }

    // Deletes a file from the cloud service
    public void deleteFileAsync(String remoteFilePath,
                         EMProgressHandler progressHandler)
    {
        // TODO:
    }

    // Get a list of CMDRemoteFileInfo objects for all files under the specified path
    // The searching is recursive
    public CMDRemoteFileInfo[] getRemoteFilesInfoAsync(String path,
                                                EMProgressHandler progressHandler)
    {
        // TODO:
        return new CMDRemoteFileInfo[]{};
    }

    // Copy the contents of the remote folder (folder and files) to the local filesystem
    public void copyRemoteFolderContentsToLocalAsync(String remoteFolderPath,
                                              String localFolderPath,
                                              boolean runMediaScannerOnFiles, // Set to true if the Android media scanner should be run on copied files
                                              int dataType,
                                              EMProgressHandler progressHandler)
    {
        // TODO:
    }

    public boolean itemExistsBlocking(String aPath, EMProgressHandler aProgressHandler)
    {
        // TODO:
        return false;
    }

    public void getBackupFolderDataTypesAsync(String aRemoteFolderPath, EMProgressHandler aProgressHandler)
    {
        // TODO ****
    }

    @Override
    public CMDFileSystemInfo getCachedFileSystemInfo() {
        return null;
    }

    @Override
    public void uploadFilesAsync(ArrayList<EMFileMetaData> filesToUpload,
                                 String remoteBasePath,
                                 EMProgressHandler progressHandler) {
        // TODO:
    }

    }
