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

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class to send files from the file system over WiFi
 * <p>
 * Note that this class should only be used when sending the contents of a folder.
 * For sending content from the Media Store the EMMediaSender class should be used instead.
 */
public class EMFileSender implements EMCommandDelegate, EMCommandHandler, EMProgressHandler {

    private static final String TAG = "EMFileSender";

    boolean mCancelled = false;

    EMFileSender(EMDataCommandDelegate aDataCommandDelegate, int aDataType) {
        mDataCommandDelegate = aDataCommandDelegate;
        mDataType = aDataType;
    }

    private EMDataCommandDelegate mDataCommandDelegate;
    private int mDataType;
    int mCurrentFileNumber = 0;
    private EMFileFinder mFileFinder;
    private ArrayList<EMFileMetaData> mFileList;
    long mTotalSizeOfFiles = 0;
    long mTotalBytesSent = 0;

    public void start(EMCommandDelegate aDelegate) {
        mCancelled = false;
        mDelegate = aDelegate;
        mFileList = new ArrayList<EMFileMetaData>();
        mFileFinder = new EMFileFinder(mDataType, mFileList);
        mFileFinder.startTask(this);
    }

    private EMCommandDelegate mDelegate;

    @Override
    public void sendText(String text) {
        if (!mCancelled)
            mDelegate.sendText(text);
    }

    @Override
    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {
        if (!mCancelled)
            mDelegate.sendFile(aFilePath, aDeleteFileWhenDone, aFileSendingProgressDelegate);
    }

    @Override
    public void getText() {
        if (!mCancelled)
            mDelegate.getText();
    }

    @Override
    public void getXmlAsFile() {
        if (!mCancelled)
            mDelegate.getXmlAsFile();
    }

    @Override
    public void commandComplete(boolean aSuccess) {
        if (!mCancelled) {
            if (aSuccess)
                sendNextFile();
            else {
                mDelegate.commandComplete(false);
            }
        }
    }

    // From Stack Overflow
    final static int[] illegalChars = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};

    static {
        Arrays.sort(illegalChars);
    }

    // From Stack Overflow
    public static String cleanFileName(String badFileName) {
        StringBuilder cleanName = new StringBuilder();
        for (int i = 0; i < badFileName.length(); i++) {
            int c = (int) badFileName.charAt(i);
            if (Arrays.binarySearch(illegalChars, c) < 0) {
                cleanName.append((char) c);
            }
        }
        return cleanName.toString();
    }

    private void sendFile() {
        if (mCancelled)
            return;

        try {
            Log.d(TAG, "=== sendFile, Current: " + mCurrentFileNumber + ", List: " + mFileList.size());

            if (mCurrentFileNumber >= mFileList.size()) {
                mDelegate.commandComplete(true);
                return;
            }

            String filePath = mFileList.get(mCurrentFileNumber).mSourceFilePath;
            File file = new File(filePath);

            if (!file.exists())
                sendNextFile();
            else {
                EMFileMetaData metaData = new EMFileMetaData();
                metaData.mSourceFilePath = filePath;
                metaData.mSize = file.length();
                metaData.mFileName = file.getName();
                metaData.mTotalFiles = mFileList.size();
                metaData.mDataType = mDataType;
                metaData.mCurrentFileNumber = mCurrentFileNumber + 1;
                metaData.mRelativePath = "";

                EMProgressInfo progressInfo = new EMProgressInfo();
                progressInfo.mDataType = mDataType;
                progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_SENDING_DATA;
                progressInfo.mTotalItems = mFileList.size();
                progressInfo.mCurrentItemNumber = mCurrentFileNumber;
                progressInfo.mProgressPercent = 0;
                progressInfo.mTotalMediaSize = mTotalSizeOfFiles;
                mDataCommandDelegate.progressUpdate(progressInfo);

                mAddFileCommandInitiator = new EMAddFileCommandInitiator(metaData, new EMFileSendingProgressDelegate() {
                    @Override
                    public void fileSendingProgressUpdate(long mFileBytesSent) {
                        mTotalBytesSent += mFileBytesSent;
                        float percentCompleteFloat = ((((float) mTotalBytesSent) / ((float) mTotalSizeOfFiles)) * ((float) 100));
                        int percentComplete = (int) percentCompleteFloat;
                        if (percentComplete != mPreviousPercentComplete) {
                            mPreviousPercentComplete = percentComplete;
                            EMProgressInfo progressInfo = new EMProgressInfo();
                            progressInfo.mDataType = mDataType;
                            progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_SENDING_DATA;
                            progressInfo.mTotalItems = mFileList.size();
                            progressInfo.mCurrentItemNumber = mCurrentFileNumber;
                            progressInfo.mProgressPercent = percentComplete;
                            mDataCommandDelegate.progressUpdate(progressInfo);
                        }
                    }
                });
                mAddFileCommandInitiator.start(this);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            DLog.log(ex);
        }
    }

    private int mPreviousPercentComplete = 0;

    // Sends the next photo, or completes our delegate if there are no more photos
    private void sendNextFile() {
        if (!mCancelled) {
            mCurrentFileNumber++;
            if (mCurrentFileNumber >= mFileList.size()) {
                mDelegate.commandComplete(true);
            } else {
                sendFile();
            }
        }
    }

    private EMAddFileCommandInitiator mAddFileCommandInitiator;

    @Override
    public Object getSharedObject(String aObject) {
        // Ignore
        return null;
    }

    @Override
    public void startNoopTimer() {
        // Ignore
    }

    @Override
    public void stopNoopTimer() {

    }

    @Override
    public void setSharedObject(String aKey, Object aObject) {
        // Ignore
    }

    // Starts listening for raw data and saves it into a file
    // The command is notified when the data is ready (when we have read [length] bytes)
    @Override
    public void getRawDataAsFile(long aLength, String aTargetFilePath) {
        if (!mCancelled)
            mDelegate.getRawDataAsFile(aLength, aTargetFilePath);
    }

    @Override
    public boolean handlesCommand(String aCommand) {
        return false;
    }

    @Override
    public boolean gotText(String aText) {
        if (mCancelled)
            return true;

        // TODO: check the initiator is valid
        return mAddFileCommandInitiator.gotText(aText);
    }

    @Override
    public boolean gotFile(String aDataPath) {
        if (mCancelled)
            return true;

        // TODO: check the initiator is valid
        return mAddFileCommandInitiator.gotFile(aDataPath);
    }

    @Override
    public void sent() {
        if (mCancelled)
            return;

        // TODO: check the initiator is valid
        mAddFileCommandInitiator.sent();
    }

    @Override
    public void taskComplete(boolean aSuccess) {
        if (mCancelled)
            return;

        // The file finder has completed, so start sending the files
        mCurrentFileNumber = 0;
        mTotalSizeOfFiles = mFileFinder.totalFoundFileSize();
        sendFile();
    }

    @Override
    public void taskError(int errorCode, boolean alreadyDisplayedDialog) {

    }

    @Override
    public void progressUpdate(EMProgressInfo aProgressInfo) {

    }

    @Override
    public void cancel() {
        mCancelled = true;
        mFileFinder.cancel(true);
        if (mAddFileCommandInitiator != null)
            mAddFileCommandInitiator.cancel();
    }

    @Override
    public void disableTimeoutTimer() {
        // Ignore
    }

    @Override
    public void enableTimeoutTimer() {
        // Ignore
    }

    @Override
    public void addToPreviouslyTransferredItems(String aItem) {
        mDelegate.addToPreviouslyTransferredItems(aItem);
    }

    @Override
    public boolean itemHasBeenPreviouslyTransferred(String aItem) {
        return mDelegate.itemHasBeenPreviouslyTransferred(aItem);
    }
}