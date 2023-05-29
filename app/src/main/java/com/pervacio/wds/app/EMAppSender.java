package com.pervacio.wds.app;

import android.util.Log;

import com.pervacio.wds.custom.appmigration.AppMigrateUtils;
import com.pervacio.wds.custom.appmigration.AppBackupModel;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Driver class to transfer apps.
 *
 * @author : Darpan Dodiya <darpan.dodiya@pervacio.com>
 */

public class EMAppSender implements EMCommandDelegate, EMCommandHandler {
    private static final String TAG = "EMAppSender";

    EMAppSender(EMDataCommandDelegate aDataCommandDelegate, int aDataType) {
        mDataCommandDelegate = aDataCommandDelegate;
        mDataType = aDataType;
    }

    private EMDataCommandDelegate mDataCommandDelegate;
    private int mDataType;
    private int mCurrentFileNumber = 0;
    private long mTotalMediaSize   = 0;
    private long mTotalBytesSent = 0;
    boolean mCancelled = false;
    private String mFilePath = null;
    private int timeOutRetry = 0;

    private ArrayList<EMFileMetaData> mAppList = new ArrayList<>();
    private EMFileMetaData fileMetaData = new EMFileMetaData();

    public InetAddress mHostName = null;
    private EMDataTransferHelper mediaTransferHelper = null;

    public void start(EMCommandDelegate aDelegate)
    {
        mCancelled = false;
        mDelegate = aDelegate;
        mTotalBytesSent = 0;
        mTotalMediaSize = AppMigrateUtils.totalAppSize;

        int index = 0;

        for (AppBackupModel currentApp : AppMigrateUtils.backupAppList) {

            if(currentApp.isChecked()) {
            EMFileMetaData appMetaData = new EMFileMetaData();
            appMetaData.mCurrentFileNumber = index++;
            appMetaData.mDataType = mDataType;
            appMetaData.mFileName = currentApp.getAppName() + ".apk";
            appMetaData.mRelativePath = currentApp.getFile().getPath();
            appMetaData.mSize = currentApp.getAppMemory();
            appMetaData.mSourceFilePath = currentApp.getFile().getAbsolutePath();
            appMetaData.mTotalFiles = AppMigrateUtils.totalAppCount;
            appMetaData.mTotalMediaSize = AppMigrateUtils.totalAppSize;
            mAppList.add(appMetaData);
            }
        }

        DLog.log("=== start, Total Dataset app Size: " + mTotalMediaSize);

        if (!mAppList.isEmpty()) {
            sendFile();
        } else {
            mDelegate.commandComplete(true);
        }
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
        if (mCancelled) {
            return;
        }

        if (aSuccess) {
            mDelegate.addToPreviouslyTransferredItems(mFilePath);
            EMMigrateStatus.addItemTransferred(EMDataType.EM_DATA_TYPE_APP);
            EMMigrateStatus.addTransferedFilesSize(EMDataType.EM_DATA_TYPE_APP,fileMetaData.mSize);
            sendNextFile();
        }
        else
        {
            boolean canBeRecovered = false;
            if(EMConfig.NEW_DATA_TRANSFER_PROTOCOL) {
                int errorCode = mediaTransferHelper.getErrorCode();
                if(errorCode == EMDataTransferHelper.ERROR_CODE_TIMEOUT) {
                    canBeRecovered = true;
                    timeOutRetry++;
                }
            }
            if(canBeRecovered && timeOutRetry<3) {
                sendNextFile();
            } else {
                mDelegate.commandComplete(false);
            }
        }
    }

    // From Stack Overflow
    private static final  int[] illegalChars = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};
    static {
        Arrays.sort(illegalChars);
    }

    // From Stack Overflow
    public static String cleanFileName(String badFileName) {
        StringBuilder cleanName = new StringBuilder();
        for (int i = 0; i < badFileName.length(); i++) {
            int c = (int)badFileName.charAt(i);
            if (Arrays.binarySearch(illegalChars, c) < 0) {
                cleanName.append((char)c);
            }
        }
        return cleanName.toString();
    }

    private void sendFile() {
        if (mCancelled)
            return;

        boolean go = true;
        while (go) {
            try {
                String filePath = mAppList.get(mCurrentFileNumber).mSourceFilePath;
                fileMetaData = mAppList.get(mCurrentFileNumber);
                File file = new File(filePath);

                if (!file.exists()) {
                    // Do nothing, we will continue to the next file
                    DLog.log("Skipping not-found file: " + filePath);
                }
                else  if (mDelegate.itemHasBeenPreviouslyTransferred(filePath)) {
                    // Do nothing, we will continue to the next file
                    DLog.log("Skipping previously send file: " + filePath);
                }
                else {
                    go = false;
                    final EMFileMetaData metaData = new EMFileMetaData();
                    metaData.mSourceFilePath = filePath;
                    metaData.mSize = file.length();
                    metaData.mTotalMediaSize = mTotalMediaSize;
                    metaData.mFileName = mAppList.get(mCurrentFileNumber).mFileName;
                    metaData.mTotalFiles = mAppList.size();
                    metaData.mDataType = mDataType;
                    metaData.mCurrentFileNumber = ++mCurrentFileNumber;
                    metaData.mRelativePath = "";


                    EMProgressInfo progressInfo = new EMProgressInfo();
                    progressInfo.mDataType = mDataType;
                    progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_SENDING_DATA;
                    progressInfo.mTotalItems = mAppList.size();
                    progressInfo.mCurrentItemNumber = mCurrentFileNumber;
                    progressInfo.mFileSize = metaData.mSize;
                    progressInfo.mTotalMediaSize = metaData.mTotalMediaSize;
                    progressInfo.mProgressPercent = 0;
                    mDataCommandDelegate.progressUpdate(progressInfo);

                    EMMigrateStatus.addItemTransferStarted(EMDataType.EM_DATA_TYPE_APP);

                    mFilePath = filePath;

                    if(EMConfig.NEW_DATA_TRANSFER_PROTOCOL) {
                        if(mediaTransferHelper == null) {
                            mediaTransferHelper = new EMDataTransferHelper(mHostName);
                            mediaTransferHelper.setCommandDelegate(this);
                            mediaTransferHelper.init();
                        }
                        mediaTransferHelper.sendFile(metaData);
                    } else {

                        mAddFileCommandInitiator = new EMAddFileCommandInitiator(metaData, new EMFileSendingProgressDelegate() {
                            @Override
                            public void fileSendingProgressUpdate(long mFileBytesSent) {
                                mTotalBytesSent += mFileBytesSent;
                                float percentCompleteFloat = ((((float) mTotalBytesSent) / ((float) metaData.mTotalMediaSize)) * ((float) 100));
                                int percentComplete = (int) percentCompleteFloat;
                                if (percentComplete != mPreviousPercentComplete) {
                                    mPreviousPercentComplete = percentComplete;
                                    EMProgressInfo progressInfo = new EMProgressInfo();
                                    progressInfo.mDataType = mDataType;
                                    progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_SENDING_DATA;
                                    progressInfo.mTotalItems = mAppList.size();
                                    progressInfo.mCurrentItemNumber = mCurrentFileNumber;
                                    progressInfo.mFileSize = metaData.mSize;
                                    progressInfo.mTotalMediaSize = metaData.mTotalMediaSize;
                                    progressInfo.mProgressPercent = (int) percentCompleteFloat;
                                    mDataCommandDelegate.progressUpdate(progressInfo);
                                }
                            }
                        });
                        mAddFileCommandInitiator.start(this);
                    }

                }
            } catch (Exception ex) {
                DLog.log(ex);
            }

            if (go) {
                // The file wasn't sent, maybe it doesn't exist or has been sent before
                // So move to the next one
                if (mCurrentFileNumber >= mAppList.size()) {
                    go = false;
                    mDelegate.commandComplete(true);
                }
                else {
                    mCurrentFileNumber++;
                }
            }
        }
    }

   private int mPreviousPercentComplete = 0;

    // Sends the next photo, or completes our delegate if there are no more photos
    private void sendNextFile() {
        if (mCancelled)
            return;

        if (mCurrentFileNumber >= mAppList.size()) {
            if (EMConfig.NEW_DATA_TRANSFER_PROTOCOL && mediaTransferHelper != null) {
                mediaTransferHelper.clean();
            }
            mDelegate.commandComplete(true);
        } else {
            sendFile();
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
        // Not using timer now
    }

    @Override
    public void stopNoopTimer() {
        // Not using timer now
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
            return false;
        if (mAddFileCommandInitiator != null)
            return mAddFileCommandInitiator.gotText(aText);
        return false;
    }

    @Override
    public boolean gotFile(String aDataPath) {
        if (mCancelled)
            return false;
        if (mAddFileCommandInitiator != null)
            return mAddFileCommandInitiator.gotFile(aDataPath);
        return false;
    }

    @Override
    public void sent() {
        if (mCancelled)
            return;
        if (mAddFileCommandInitiator != null)
            mAddFileCommandInitiator.sent();
    }

    @Override
    public void cancel() {
        mCancelled = true;
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
