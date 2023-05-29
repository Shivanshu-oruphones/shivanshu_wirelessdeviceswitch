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

import com.pervacio.wds.app.EMProgressHandler;

public interface CMDCloudServiceFileInterface extends CMDFileSystemInterface{

    public void startUserLoginAsync(EMProgressHandler progressHandler);

    public void startLogInWithAccountNameAsync(String accountName, EMProgressHandler progressHandler, boolean aIsDestinationDevice);

    void startUserAccountCreationAndLoginAsync(EMProgressHandler progressHandler);

    void userLogoutAsyncAsync(EMProgressHandler progressHandler);

    // Initialize the cloud service with an access token (for cases where the login has been done elsewhere)
    // void initWithAccessToken(String aAccessToken);

    void initWithUserName(String aAccessToken, String aUserName);

    // Get the access token, for cases where it will be used with a different instance
    String getAccessToken();

    String getUserName();

    // Uploads a file to the cloud service
    // The destination path must be in Unix format
    // If the folders are not present on the remote service then they will be created
    // TODO: should the mkpath bit be optional?
    void uploadFileAsync(String sourcePath,
                         CMDRemoteFileInfo remoteFileInfo,
                         EMProgressHandler progressHandler,
                         int totalSubItems); // Used to provide smoother progress information when sending large files (value should be the number of items in the file, e.g. number of contacts)

    // Downloads the file to the specified local path
    // The path must include the target file name
    // The specified folder must exist
    void downloadFileAsync(String localPath,
                           CMDRemoteFileInfo remoteFileInfo,
                           EMProgressHandler progressHandler);

    // Deletes a folder and all its contents on the cloud service
    void deleteFolderAsync(String remoteFolderPath,
                           EMProgressHandler progressHandler);

    // Deletes a file from the cloud service
    void deleteFileAsync(String remoteFilePath,
                         EMProgressHandler progressHandler);

    // Get a list of CMDRemoteFileInfo objects for all files under the specified path
    // The searching is recursive
    CMDRemoteFileInfo[] getRemoteFilesInfoAsync(String path,
                                                EMProgressHandler progressHandler);

    // Copy the contents of the remote folder (folder and files) to the local filesystem
    void copyRemoteFolderContentsToLocalAsync(String remoteFolderPath,
                                              String localFolderPath,
                                              boolean runMediaScannerOnFiles, // Set to true if the Android media scanner should be run on copied files
                                              int dataType,
                                              EMProgressHandler progressHandler);

    // Find which datasets are present on the backup folder
    public void getBackupFolderDataTypesAsync(String aRemoteFolderPath, EMProgressHandler aProgressHandler);

}
