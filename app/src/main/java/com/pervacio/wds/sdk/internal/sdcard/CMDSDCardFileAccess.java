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
import android.os.StatFs;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMDataType;
import com.pervacio.wds.app.EMFileMetaData;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMSimpleAsyncTask;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.sdk.CMDError;
import com.pervacio.wds.sdk.internal.CMDCryptoSettings;
import com.pervacio.wds.sdk.internal.CMDFileSystemInterface;
import com.pervacio.wds.sdk.internal.CMDStringConsts;
import com.pervacio.wds.sdk.internal.CMDUtilsStorage;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;

public class CMDSDCardFileAccess implements CMDFileSystemInterface {
    static EMGlobals emGlobals = new EMGlobals();
    private EMProgressHandler mCurrentProgressHandler;

    private static final String TAG = "EasyMigrate";
    private String mSDCardPath;
    private Context mContext;

    private String getFullPath(String aRelativePath) {
        return mSDCardPath + aRelativePath;
    }

    public void setSDCardPath(String aPath) {
        mSDCardPath = aPath;
    }

    public CMDSDCardFileAccess(Context aContext) {
        mContext = emGlobals.getmContext();

        String[] storagePaths = CMDUtilsStorage.getSDCardStoragePaths(mContext, true);

        if (storagePaths.length > 0)
            setSDCardPath(storagePaths[0]);
    }

    static public boolean sdCardExists(Context aContext) {
        String[] storagePaths = CMDUtilsStorage.getSDCardStoragePaths(emGlobals.getmContext(), false);

        boolean exists = false;

        if (storagePaths == null)
            exists = false;
        else if (storagePaths.length < 1)
            exists = false;
        else if (storagePaths[0] == null)
            exists = false;
        else {
            File sdCardPathFile = new File(storagePaths[0]);
            if (sdCardPathFile.exists())
                exists = true;
            else
                exists = false;
        }

        return exists;
    }

    static public String  getSDCardpath(Context aContext) {
        String[] storagePaths = CMDUtilsStorage.getSDCardStoragePaths(emGlobals.getmContext(), false);
        String sdCardPath = null;

        boolean exists = false;

        if (storagePaths == null)
            exists = false;
        else if (storagePaths.length < 1)
            exists = false;
        else if (storagePaths[0] == null)
            exists = false;
        else {
            File sdCardPathFile = new File(storagePaths[0]);
            if (sdCardPathFile.exists()) {
                exists = true;
                DLog.log("sdCardExists true storagePaths[0] "+storagePaths[0]);
                sdCardPath = storagePaths[0];
            }
            else {
                exists = false;
                DLog.log("sdCardExists false storagePaths[0] " + storagePaths[0]);
            }
        }

        return sdCardPath;
    }
    @Override
    public void uploadFileAsync(String sourcePath,
                                CMDRemoteFileInfo remoteFileInfo,
                                EMProgressHandler progressHandler,
                                int totalSubItems) {
        String fullSDCardFilePath = getFullPath(remoteFileInfo.mFilePath);
        copyFile(progressHandler, sourcePath, fullSDCardFilePath);
    }

    @Override
    public void uploadFilesAsync(ArrayList<EMFileMetaData> filesToUpload,
                                 String remoteBasePath,
                                 EMProgressHandler progressHandler) {
        String backupFolderPath = mSDCardPath + remoteBasePath;
        CMDSDCardUploadMultipleFilesAsyncTask fileCopier = new CMDSDCardUploadMultipleFilesAsyncTask(filesToUpload, backupFolderPath);
        fileCopier.startTask(progressHandler);
    }

    @Override
    public void downloadFileAsync(String localPath,
                                  CMDRemoteFileInfo remoteFileInfo,
                                  EMProgressHandler progressHandler) {
        String fullSDCardFilePath = getFullPath(remoteFileInfo.mFilePath);
        copyFile(progressHandler, fullSDCardFilePath, localPath);
    }

    private void copyFile(EMProgressHandler aProgressHandler,
                    String aSourceFilePath,
                    String aDestinationFilePath) {
        final String sourceFilePath = aSourceFilePath;
        final String destinationFilePath = aDestinationFilePath;

        EMSimpleAsyncTask copyFileAsync = new EMSimpleAsyncTask() {
            @Override
            protected void runTask() {
                boolean copyResult;

                EMUtility.makePathForLocalFile(destinationFilePath);

                if ((sourceFilePath == null) || (destinationFilePath == null))
                    copyResult = false;
                else {
                    if (CMDCryptoSettings.enabled())
                        copyResult = EMUtility.copyFileWithDecrypt(new File(sourceFilePath), new File(destinationFilePath));
                    else
                        copyResult = EMUtility.copyFile(new File(sourceFilePath), new File(destinationFilePath));
                }

                if (copyResult) {
                    File sourceFile = new File(sourceFilePath);
                    File destinationFile = new File(destinationFilePath);
                    destinationFile.setLastModified(sourceFile.lastModified());
                }
                else
                    setFailed(CMDError.CMD_SD_CARD_ERROR_COPYING_TO_LOCAL);
            }
        };
        copyFileAsync.startTask(aProgressHandler);
    }

    @Override
    public void deleteFolderAsync(String remoteFolderPath,
                                  EMProgressHandler progressHandler) {
        String fullSDCardFilePath = getFullPath(remoteFolderPath);
        CMDSDCardRecursiveDeleteAsyncTask deleteAsyncTask = new CMDSDCardRecursiveDeleteAsyncTask(fullSDCardFilePath);
        deleteAsyncTask.startTask(progressHandler);
    }

    @Override
    public void deleteFileAsync(String remoteFilePath,
                                EMProgressHandler progressHandler) {
        // TODO Auto-generated method stub
    }

    @Override
    public CMDRemoteFileInfo[] getRemoteFilesInfoAsync(String path,
                                                       EMProgressHandler progressHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
    private int mCurrentCopyItemNumber;
    private int mTotalItems;

    public boolean copyDirectory(File sourceLocation ,
                                          File targetLocation,
                                          boolean runMediaScannerOnFiles, // Set to true if the Android media scanner should be run on copied files
                                          int dataType,
                                          EMProgressHandler progressHandler) throws IOException {
        mCurrentCopyItemNumber = 0;
        mTotalItems = 0;

        copyDirectoryRecursive(sourceLocation,
                targetLocation,
                runMediaScannerOnFiles,
                dataType,
                progressHandler,
                true); // TODO: handle error?

        mTotalItems = mCurrentCopyItemNumber;
        mCurrentCopyItemNumber = 0;

        return copyDirectoryRecursive(sourceLocation,
                targetLocation,
                runMediaScannerOnFiles,
                dataType,
                progressHandler,
                false);
    }

    // Based on StackOverflow example
    // Returns the number of files copied (or number of files counted if countOnly mode)
    public boolean copyDirectoryRecursive(File sourceLocation ,
                                    File targetLocation,
                                    boolean runMediaScannerOnFiles, // Set to true if the Android media scanner should be run on copied files
                                    int dataType,
                                    EMProgressHandler progressHandler,
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
                        progressHandler,
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
                progressHandler.progressUpdate(progressInfo);

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
    */

    // Copy the contents of the remote folder (folder and files) to the local filesystem
    @Override
    public void copyRemoteFolderContentsToLocalAsync(String remoteFolderPath,
                                                     String localFolderPath,
                                                     boolean runMediaScannerOnFiles, // Set to true if the Android media scanner should be run on copied files
                                                     int dataType,
                                                     EMProgressHandler progressHandler) {
        boolean success = true;

        String fullRemotePath = getFullPath(remoteFolderPath);

        CMDCopyDirectoryAsyncTask directoryCopier = new CMDCopyDirectoryAsyncTask(new File(fullRemotePath),
                                                                                        new File(localFolderPath),
                                                                                        runMediaScannerOnFiles,
                                                                                        dataType,
                                                                                        mContext);
        directoryCopier.startTask(progressHandler);

        /*
        try {
            copyDirectory(new File(fullRemotePath),
                    new File(localFolderPath),
                    runMediaScannerOnFiles,
                    dataType,
                    progressHandler);
        }
        catch (Exception ex) {
            success = false;
        }

        if (success)
            progressHandler.taskComplete(true);
        else
            progressHandler.taskError(CMDError.CMD_SD_CARD_ERROR_COPYING_FOLDER_TO_LOCAL, false);
        */
    }

    @Override
    public void getBackupFolderDataTypesAsync(String aRemoteFolderPath, EMProgressHandler aProgressHandler) {
        int foundDataTypes = 0;

        HashMap<String, Integer> dataTypeByFileName = new HashMap<String, Integer>() {{
            put(CMDStringConsts.EM_CONTACTS_FILE_NAME, EMDataType.EM_DATA_TYPE_CONTACTS);
            put(CMDStringConsts.EM_CALENDAR_FILE_NAME, EMDataType.EM_DATA_TYPE_CALENDAR);
            put(CMDStringConsts.EM_PHOTOS_FOLDER_NAME, EMDataType.EM_DATA_TYPE_PHOTOS);
            put(CMDStringConsts.EM_VIDEOS_FOLDER_NAME, EMDataType.EM_DATA_TYPE_VIDEO);
            put(CMDStringConsts.EM_SMS_FILE_NAME, EMDataType.EM_DATA_TYPE_SMS_MESSAGES);
            put(CMDStringConsts.EM_NOTES_FILE_NAME, EMDataType.EM_DATA_TYPE_NOTES);
            put(CMDStringConsts.EM_TASKS_FILE_NAME, EMDataType.EM_DATA_TYPE_TASKS);
            put(CMDStringConsts.EM_DOCUMENTS_FOLDER_NAME, EMDataType.EM_DATA_TYPE_DOCUMENTS);
            put(CMDStringConsts.EM_MUSIC_FOLDER_NAME, EMDataType.EM_DATA_TYPE_MUSIC);
            put(CMDStringConsts.EM_ACCOUNTS_FILE_NAME, EMDataType.EM_DATA_TYPE_ACCOUNTS);
        }};

        String fullFolderPath = getFullPath(aRemoteFolderPath);

        // Populate foundDataTypes
        File folderFile = new File(fullFolderPath);
        File[] files = folderFile.listFiles();
        for (File file : files) {
            String filename = file.getName();
            if (dataTypeByFileName.containsKey(filename)) {
                int dataType = dataTypeByFileName.get(filename);
                foundDataTypes |= dataType;
            }
        }

        EMProgressInfo progressInfo = new EMProgressInfo();
        progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_RESTORING_FROM_BACKUP;
        progressInfo.mDataType      = foundDataTypes;
        aProgressHandler.progressUpdate(progressInfo);
        aProgressHandler.taskComplete(true);
    }


    @Override
    public boolean itemExistsBlocking(String aPath, EMProgressHandler aProgressHandler) {
        String fullSDCardFilePath = getFullPath(aPath);
        File file = new File(fullSDCardFilePath);
        return file.exists();
    }

    public CMDFileSystemInterface.CMDFileSystemInfo getCachedFileSystemInfo()
    {
        File sdCardPathFile = new File(mSDCardPath);
        boolean folderCreated = sdCardPathFile.mkdirs();
        StatFs statFs = new StatFs(mSDCardPath);

        long free;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            free = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());
        }
        else {
            free = (((long) statFs.getAvailableBlocks()) * ((long)statFs.getBlockSize()));
        }

        long total;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            total = (statFs.getBlockCountLong() * statFs.getBlockSizeLong());
        }
        else {
            total = (statFs.getBlockCount() * statFs.getBlockSize());
        }

        CMDFileSystemInfo fileSystemInfo = new CMDFileSystemInfo();
        fileSystemInfo.mTotalSpaceBytes = total;
        fileSystemInfo.mUsedSpaceBytes = total - free;

        return fileSystemInfo;
    }

    public String[] getPossibleExternalSDCardPaths() {
        File pathFile = new File("/storage");
        File[] directories = pathFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });

        ArrayList<String> subdirectoryPaths = new ArrayList<String>();
        for (File directoryFile : directories) {
            subdirectoryPaths.add(directoryFile.getAbsolutePath());
        }

        return subdirectoryPaths.toArray(new String[subdirectoryPaths.size()]);
    }

    private int mConnectionAttempts = 0;
}
