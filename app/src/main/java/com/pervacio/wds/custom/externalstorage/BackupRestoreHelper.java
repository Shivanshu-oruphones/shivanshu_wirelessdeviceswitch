package com.pervacio.wds.custom.externalstorage;

import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.github.mjdev.libaums.fs.UsbFileOutputStream;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMCommandDelegate;
import com.pervacio.wds.app.EMDataCommandDelegate;
import com.pervacio.wds.app.EMDataCompleteDelegate;
import com.pervacio.wds.app.EMDataType;
import com.pervacio.wds.app.EMFileMetaData;
import com.pervacio.wds.app.EMGenerateCalendarXmlAsyncTask;
import com.pervacio.wds.app.EMGenerateCallLogsBackupTask;
import com.pervacio.wds.app.EMGenerateContactsXmlAsyncTask;
import com.pervacio.wds.app.EMGenerateDataInThread;
import com.pervacio.wds.app.EMGenerateDataTask;
import com.pervacio.wds.app.EMGenerateSettingsBackupTask;
import com.pervacio.wds.app.EMGenerateSmsMessagesXmlAsyncTask;
import com.pervacio.wds.app.EMMigrateStatus;
import com.pervacio.wds.app.EMParseCalendarXmlAsyncTask;
import com.pervacio.wds.app.EMParseCallLogsXmlInThread;
import com.pervacio.wds.app.EMParseContactsXmlAsyncTask;
import com.pervacio.wds.app.EMParseDataInThread;
import com.pervacio.wds.app.EMParseDataTask;
import com.pervacio.wds.app.EMParseSettingsXmlInThread;
import com.pervacio.wds.app.EMParseSmsXmlAsyncTask;
import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMStringConsts;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.ui.EasyMigrateActivity;
import com.pervacio.wds.app.ui.PermissionHandler;
import com.pervacio.wds.custom.appmigration.AppMigrateUtils;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DeviceInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/*Created by Surya Polasanapalli*/

public class BackupRestoreHelper {
    private static final int READ_BUFFER_SIZE = 1024 * 128;
    private static final int EMFTUReqBackupMedia = 1;
    private static final int EMFTUReqRestoreMedia = 2;
    private static final int EMFTUReqBackupPIM = 3;
    private static final int EMFTUReqRestorePIM = 4;
    private static final int EMFTURespSendFileSuccess = 1;
    private static final int EMFTURespSendFileError = 2;
    private static final int EMFTURespBackupFile = 3;
    private static final int EMFTURespRestoredFile = 4;
    private static final int EMFTURespProgressInfo = 5;
    Handler mMainThreadHandler = null;
    Messenger mMainThreadMessenger = null;
    BackupRestoreThread mWorkerThread = new BackupRestoreThread();
    private String mCurrentDataType;
    private EMDataCompleteDelegate mDataCompleteDelegate;
    private EMDataCommandDelegate mDataCommandDelegate;
    private EMCommandDelegate mCommandDelegate;
    private int operationType = EMStringConsts.OPERATION_TYPE_BACKUP;
    private int STORAGE_DEVICE_TYPE = 0;
    BackupRestoreHelper() {
        init();
        STORAGE_DEVICE_TYPE = CommonUtil.getInstance().getExternalStorageType();
    }

    private static synchronized boolean backupToUSB(EMFileMetaData emFileMetaData) {
        FileInputStream fileInputStream = null;
        OutputStream outputStream = null;
        boolean result = true;
        long totalBytes = 0;
        try {
            UsbFile root = CommonUtil.getInstance().getUsbDevice().getPartitions().get(0).getFileSystem().getRootDirectory();
            UsbFile newDir = null;
            try {
                newDir = root.search(Constants.EXTERNAL_STORAGE_BACKUP_FOLDER);
            } catch (Exception e) {
                newDir = root.createDirectory(Constants.EXTERNAL_STORAGE_BACKUP_FOLDER);
            }
            if (newDir == null) {
                newDir = root.createDirectory(Constants.EXTERNAL_STORAGE_BACKUP_FOLDER);
            }
            UsbFile file = newDir.createFile(emFileMetaData.mFileName);
            fileInputStream = new FileInputStream(new File(emFileMetaData.mSourceFilePath));
            outputStream = new UsbFileOutputStream(file);
            byte[] buffer = new byte[READ_BUFFER_SIZE];
            int bytesRead = 0;
            boolean isLiveUpdateRequired = EMMigrateStatus.isLiveUpdateRequired(emFileMetaData.mDataType);
            long chunkReadBytes = 0;

            while ((bytesRead = fileInputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                if (isLiveUpdateRequired) {
                    chunkReadBytes += bytesRead;
                    //Update current bytes transferred
                    if (chunkReadBytes > EMMigrateStatus.LIVE_CHUNK_UPDATE_SIZE) {
                        EMMigrateStatus.addLiveTransferredSize(emFileMetaData.mDataType, chunkReadBytes);
                        chunkReadBytes = 0;
                    }
                }
            }
            //Add last remaining chunk of file
            if (chunkReadBytes != 0) {
                EMMigrateStatus.addLiveTransferredSize(emFileMetaData.mDataType, chunkReadBytes);
            }
        } catch (Exception ex) {
            DLog.log("backupToUSB : Exception " + ex.getMessage());
            result = false;
        } finally {
            try {
                fileInputStream.close();
                outputStream.close();
            } catch (Exception ex) {
                // Ignore
            }
            DLog.log("finally backupToUSB : " + emFileMetaData.mFileName + " ,isSuccess : " + result + ", total Bytes Written " + totalBytes);
        }
        return result;
    }

    public void setCommandDelegate(EMCommandDelegate aCommandDelegate) {
        mCommandDelegate = aCommandDelegate;
    }

    public void setDataCommandDelegate(EMDataCommandDelegate aDataCommandDelegate) {
        mDataCommandDelegate = aDataCommandDelegate;
    }

    private boolean copyFile(EMFileMetaData aSource, String aDestination) {
        boolean result = true;
        InputStream input = null;
        OutputStream output = null;
        DLog.log("Copying file : " + aSource.mSourceFilePath + " >> " + aDestination);
        try {
            File file = new File(aDestination + "/" + aSource.mFileName);
            File fileDirectory = new File(file.getParent());
            try {
                if (!fileDirectory.exists()) {
                    fileDirectory.mkdirs();
                }
                if (!file.exists()) {
                    file.createNewFile();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            input = new FileInputStream(new File(aSource.mSourceFilePath));
            output = new FileOutputStream(file);
            byte[] buffer = new byte[READ_BUFFER_SIZE];
            int bytesRead;
            boolean isLiveUpdateRequired = EMMigrateStatus.isLiveUpdateRequired(aSource.mDataType);
            long chunkReadBytes = 0;
            while ((bytesRead = input.read(buffer)) > 0) {
                output.write(buffer, 0, bytesRead);
                if (isLiveUpdateRequired) {
                    chunkReadBytes += bytesRead;
                    //Update current bytes transferred
                    if (chunkReadBytes > EMMigrateStatus.LIVE_CHUNK_UPDATE_SIZE) {
                        EMMigrateStatus.addLiveTransferredSize(aSource.mDataType, chunkReadBytes);
                        chunkReadBytes = 0;
                    }
                }
            }
            //Add last remaining chunk of file
            if (chunkReadBytes != 0) {
                EMMigrateStatus.addLiveTransferredSize(aSource.mDataType, chunkReadBytes);
            }
        } catch (Exception ex) {
            DLog.log("Exception in copyfile : " + ex.getMessage());
            result = false;
        } finally {
            try {
                input.close();
                output.close();
            } catch (Exception ex) {
                // Ignore
            }
        }

        return result;
    }

    private void progressUpdate(EMProgressInfo aProgressInfo) {
        if (mDataCommandDelegate != null) {
            mDataCommandDelegate.progressUpdate(aProgressInfo);
        }
    }

    public boolean backupMedia() {
        Message msg = new Message();
        EMFTURequestMessage message = new EMFTURequestMessage();
        message.mId = EMFTUReqBackupMedia;
        msg.obj = message;
        mWorkerThread.getHandler().sendMessage(msg);
        return true;
    }

    public boolean restoreMedia() {
        Message msg = new Message();
        EMFTURequestMessage message = new EMFTURequestMessage();
        message.mId = EMFTUReqRestoreMedia;
        msg.obj = message;
        mWorkerThread.getHandler().sendMessage(msg);
        return true;
    }

    public boolean backupPIM(String dataType) {
        mCurrentDataType = dataType;
        Message msg = new Message();
        EMFTURequestMessage message = new EMFTURequestMessage();
        message.mId = EMFTUReqBackupPIM;
        msg.obj = message;
        mWorkerThread.getHandler().sendMessage(msg);
        return true;
    }

    public boolean restorePIM(String dataType) {
        mCurrentDataType = dataType;
        operationType = EMStringConsts.OPERATION_TYPE_RESTORE;
        Message msg = new Message();
        EMFTURequestMessage message = new EMFTURequestMessage();
        message.mId = EMFTUReqRestorePIM;
        msg.obj = message;
        mWorkerThread.getHandler().sendMessage(msg);
        return true;
    }

    public void init() {
        mMainThreadHandler = new Handler() {
            public void handleMessage(Message msg) {
                EMFTUResponseMessage message = (EMFTUResponseMessage) msg.obj;
                switch (message.mId) {
                    case EMFTURespSendFileSuccess:
                        mCommandDelegate.commandComplete(true);
                        break;
                    case EMFTURespSendFileError:
                        mCommandDelegate.commandComplete(false);
                        break;
                    case EMFTURespRestoredFile: {
                        EMFileMetaData fileInfo = (EMFileMetaData) message.obj;
                        EMMigrateStatus.addItemTransferred(fileInfo.mDataType);
                        CommonUtil.getInstance().setRestoredMediaCount(1);
                        EMMigrateStatus.addTransferedFilesSize(fileInfo.mDataType, fileInfo.mSize);
                        String[] paths = {fileInfo.mSourceFilePath};
                        MediaScannerConnection.scanFile(EMUtility.Context(), paths, null, null);
                        break;
                    }
                    case EMFTURespBackupFile: {
                        EMFileMetaData fileInfo = (EMFileMetaData) message.obj;
                        EMMigrateStatus.addItemTransferred(fileInfo.mDataType);
                        EMMigrateStatus.addTransferedFilesSize(fileInfo.mDataType, fileInfo.mSize);
                        break;
                    }
                    case EMFTURespProgressInfo: {
                        EMProgressInfo progressInfo = (EMProgressInfo) message.obj;
                        progressUpdate(progressInfo);
                        break;
                    }
                }
            }
        };
        mMainThreadMessenger = new Messenger(mMainThreadHandler);
        mWorkerThread.start();
    }

    class EMFTURequestMessage {
        public int mId;
    }

    class EMFTUResponseMessage {
        public int mId;
        public Object obj;
    }

    class BackupRestoreThread extends Thread {
        Handler mInThreadHandler;
        boolean mCancel = false;
        EMParseDataTask emParseDataTask = null;
        EMParseDataInThread emParseDataInThread = null;
        private EMGenerateDataTask mGenerateDataTask;
        private EMGenerateDataInThread mGenerateDataThread;
        private EMProgressHandler pimProgressHandler = new EMProgressHandler() {
            @Override
            public void taskComplete(boolean aSuccess) {
                if (aSuccess) {
                    DLog.log(mCurrentDataType + " task Completed");
                }
                if (operationType == EMStringConsts.OPERATION_TYPE_BACKUP) {
                    String filePath = null;
                    if (mGenerateDataTask != null) {
                        filePath = mGenerateDataTask.getFilePath();
                    } else {
                        filePath = mGenerateDataThread.getFilePath();
                    }

                    EMFileMetaData emFileMetaData = new EMFileMetaData();
                    emFileMetaData.mDataType = EMStringConsts.DATATYPE_MAP.get(mCurrentDataType);
                    emFileMetaData.mFileName = mCurrentDataType.toLowerCase() + ".xml";
                    emFileMetaData.mSourceFilePath = filePath;

                    checkMigrationStatus();

                    if (STORAGE_DEVICE_TYPE == EMStringConsts.EXTERNAL_STORAGE_USB) {
                        boolean isCopied = BackupRestoreHelper.backupToUSB(emFileMetaData);
                        int retryCount = 0;
                        while (!isCopied && retryCount < 3) {
                            isCopied = BackupRestoreHelper.backupToUSB(emFileMetaData);
                            ++retryCount;
                        }
                    } else {
                        File destination = new File(new SdcardUtils().getSdcardPath() + "/" + Constants.EXTERNAL_STORAGE_BACKUP_FOLDER);
                        boolean isCopied = copyFile(emFileMetaData, destination.getAbsolutePath());
                        int retryCount = 0;
                        while (!isCopied && retryCount < 3) {
                            isCopied = copyFile(emFileMetaData, destination.getAbsolutePath());
                            ++retryCount;
                        }
                    }
                    try {
                        File file = new File(filePath);
                        file.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                sendTransferComplete(true);
            }

            @Override
            public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
                DLog.log(mCurrentDataType + " taskError");
            }

            @Override
            public void progressUpdate(EMProgressInfo aProgressInfo) {
                sendProgressUpdate(aProgressInfo);
            }
        };
        private EMFileMetaData currentMetaData = new EMFileMetaData();

        synchronized Handler getHandler() {
            while (mInThreadHandler == null) {
                try {
                    wait();
                } catch (Exception e) {
                    DLog.log(e);
                }
            }

            return mInThreadHandler;
        }

        @Override
        public void run() {
            DLog.log(">>BackupRestoreThread");
            Looper.prepare();
            synchronized (this) {
                mInThreadHandler = new Handler() {
                    public void handleMessage(Message msg) {
                        EMFTURequestMessage message = (EMFTURequestMessage) msg.obj;
                        switch (message.mId) {
                            case EMFTUReqBackupMedia:
                                backupMedia();
                                break;
                            case EMFTUReqRestoreMedia:
                                restoreMedia();
                                break;
                            case EMFTUReqBackupPIM:
                                backupPIMData(mCurrentDataType);
                                break;
                            case EMFTUReqRestorePIM:
                                restorePIMData(mCurrentDataType);
                                break;
                            default:
                                break;
                        }

                    }
                };
                notifyAll();
            }
            Looper.loop();
        }

        private void sendTransferComplete(boolean aSuccess) {
            Message msg = new Message();
            EMFTUResponseMessage resp = new EMFTUResponseMessage();
            if (aSuccess) {
                resp.mId = EMFTURespSendFileSuccess;
            } else {
                resp.mId = EMFTURespSendFileError;
            }
            msg.obj = resp;
            try {
                mMainThreadMessenger.send(msg);
            } catch (RemoteException e1) {
                e1.printStackTrace();
                DLog.log(e1);
            }
        }

        private void processedFile(EMFileMetaData aFileInfo) {
            Message msg = new Message();
            EMFTUResponseMessage resp = new EMFTUResponseMessage();
            resp.mId = EMFTURespBackupFile;
            resp.obj = aFileInfo;
            msg.obj = resp;
            try {
                mMainThreadHandler.sendMessage(msg);
            } catch (Exception e1) {
                e1.printStackTrace();
                DLog.log(e1);
            }
        }

        private void restoredAFile(EMFileMetaData aFileInfo) {
            Message msg = new Message();
            EMFTUResponseMessage resp = new EMFTUResponseMessage();
            resp.mId = EMFTURespRestoredFile;
            resp.obj = aFileInfo;
            msg.obj = resp;
            try {
                mMainThreadMessenger.send(msg);
            } catch (RemoteException e1) {
                e1.printStackTrace();
                DLog.log(e1);
            }
        }

        private void sendProgressUpdate(EMProgressInfo aProgressInfo) {
            Message msg = new Message();
            EMFTUResponseMessage resp = new EMFTUResponseMessage();
            resp.mId = EMFTURespProgressInfo;
            resp.obj = aProgressInfo;
            msg.obj = resp;
            try {
                mMainThreadHandler.sendMessage(msg);
            } catch (Exception e1) {
                DLog.log(e1);
            }
        }

        void writePIMXml() {
            DLog.log("Generating PIMData XML " + mCurrentDataType);
            if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CONTACTS)) {
                mGenerateDataTask = new EMGenerateContactsXmlAsyncTask();
            } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CALENDAR)) {
                mGenerateDataTask = new EMGenerateCalendarXmlAsyncTask();
            } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_SMS_MESSAGES)) {
                mGenerateDataTask = new EMGenerateSmsMessagesXmlAsyncTask();
            } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CALL_LOGS)) {
                mGenerateDataThread = new EMGenerateCallLogsBackupTask();
            } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_SETTINGS)) {
                mGenerateDataThread = new EMGenerateSettingsBackupTask();
            }
            if (mGenerateDataTask != null) {
                // mGenerateDataTask.setCommandDelegate(mDataCommandDelegate);
                CommonUtil.getInstance().setRunningAsyncTask(mGenerateDataTask);
                mGenerateDataTask.startTask(pimProgressHandler);
            } else if (mGenerateDataThread != null) {
                mGenerateDataThread.startTask(pimProgressHandler);
            }
        }

        void processPIMXML(final String aDataPath) {
            DLog.log("Parsing PIMData XML " + mCurrentDataType + " " + aDataPath);
            if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CONTACTS)) {
                emParseDataInThread = new EMParseContactsXmlAsyncTask();
                startParsingXML(aDataPath);
            } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CALENDAR)) {
                emParseDataInThread = new EMParseCalendarXmlAsyncTask();
                startParsingXML(aDataPath);
            } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_SMS_MESSAGES)) {
                emParseDataInThread = new EMParseSmsXmlAsyncTask();
                EasyMigrateActivity.getDefaultSMSAppPermission(new PermissionHandler() {
                    @Override
                    public void userAccepted() {
                        DLog.log(mCurrentDataType + " User Accepted ");
                        startParsingXML(aDataPath);
                    }

                    @Override
                    public void userDenied() {
                        DLog.log(mCurrentDataType + " User Denied ");
                        if (mDataCompleteDelegate != null) {
                            mDataCompleteDelegate.restoreCompleted(mCurrentDataType, false);
                        }
                    }
                });
            } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_CALL_LOGS)) {
                emParseDataInThread = new EMParseCallLogsXmlInThread();
                startParsingXML(aDataPath);
            } else if (mCurrentDataType.equalsIgnoreCase(EMStringConsts.DATA_TYPE_SETTINGS)) {
                emParseDataInThread = new EMParseSettingsXmlInThread();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
               /* EasyMigrateActivity.enableWritePermissionForSettings(new PermissionHandler() {
                    @Override
                    public void userAccepted() {
                        DLog.log(mCurrentDataType + " User enabled ");
                        startParsingXML(aDataPath);
                    }

                    @Override
                    public void userDenied() {
                        DLog.log(mCurrentDataType + " User denied ");
                        startParsingXML(aDataPath);
                    }
                });*/
                    startParsingXML(aDataPath);
                } else {
                    startParsingXML(aDataPath);
                }
            }
        }

        private void startParsingXML(String aDataPath) {
            if (emParseDataTask != null) {
                CommonUtil.getInstance().setRunningAsyncTask(emParseDataTask);
                emParseDataTask.startTask(aDataPath, true, EMUtility.Context(), pimProgressHandler);
            } else if (emParseDataInThread != null) {
                emParseDataInThread.startTask(aDataPath, true, EMUtility.Context(), pimProgressHandler);
            }
        }

        private synchronized boolean restoreFromUSB(UsbFile sourceUSBFile, String destinationPath) {
            boolean result = true;
            InputStream input = null;
            OutputStream output = null;
            try {
                input = new UsbFileInputStream(sourceUSBFile);
                output = new FileOutputStream(new File(destinationPath));
                byte[] buffer = new byte[READ_BUFFER_SIZE];
                int bytesRead = 0;
                boolean isLiveUpdateRequired = EMMigrateStatus.isLiveUpdateRequired(currentMetaData.mDataType);
                long chunkReadBytes = 0;
                while ((bytesRead = input.read(buffer)) > 0) {
                    output.write(buffer, 0, bytesRead);
                    if (isLiveUpdateRequired) {
                        chunkReadBytes += bytesRead;
                        //Update current bytes transferred
                        if (chunkReadBytes > EMMigrateStatus.LIVE_CHUNK_UPDATE_SIZE) {
                            EMMigrateStatus.addLiveTransferredSize(currentMetaData.mDataType, chunkReadBytes);
                            chunkReadBytes = 0;
                        }
                    }
                }
                //Add last remaining chunk of file
                if (chunkReadBytes != 0) {
                    EMMigrateStatus.addLiveTransferredSize(currentMetaData.mDataType, chunkReadBytes);
                }
            } catch (Exception ex) {
                result = false;
            } finally {
                try {
                    input.close();
                    output.close();
                } catch (Exception ex) {
                    // Ignore
                }
            }
            return result;
        }

        private void restoreMedia() {
            while (!RestoreUtility.getInstance(STORAGE_DEVICE_TYPE).getFileList().isEmpty()) {
                checkMigrationStatus();
                EMFileMetaData emFileMetaData = new EMFileMetaData();
                UsbFile usbFile = null;
                if (STORAGE_DEVICE_TYPE == EMStringConsts.EXTERNAL_STORAGE_USB) {
                    usbFile = (UsbFile) RestoreUtility.getInstance(STORAGE_DEVICE_TYPE).getMetaData();
                    if (usbFile == null) {
                        sendTransferComplete(true);
                        return;
                    }
                    emFileMetaData.mDataType = getFileType(usbFile.getName());
                    emFileMetaData.mFileName = usbFile.getName();
                    emFileMetaData.mSize = usbFile.getLength();
                } else {
                    emFileMetaData = (EMFileMetaData) (RestoreUtility.getInstance(STORAGE_DEVICE_TYPE).getMetaData());
                }
                String filePathToCopy = getTargetFilePath(emFileMetaData);
                File tempFile = new File(filePathToCopy);
                boolean isFileExists = false;
                if (tempFile.exists()) {
                    if (tempFile.length() == emFileMetaData.mSize) {
                        isFileExists = true;
                    } else {
                        try {
                            int pos = tempFile.getName().lastIndexOf(".");
                            int fileNo = 0;
                            String fileExtension = tempFile.getName().substring(pos + 1);
                            String fileName = pos > 0 ? tempFile.getName().substring(0, pos) : tempFile.getName();
                            File modifiedFile = null;
                            do {
                                fileNo++;
                                String newFileName = fileName + "(" + fileNo + ")." + fileExtension;
                                modifiedFile = new File(tempFile.getParent(), newFileName);
                                filePathToCopy = modifiedFile.getAbsolutePath();
                            } while (modifiedFile.exists());
                        } catch (Exception e) {
                            DLog.log("Exception while creating the new file name : " + e.toString());
                        }
                        DLog.log("*** modified file name " + filePathToCopy);
                    }
                }
                if (isFileExists) {
                    //File alrdy exists. No need to copy.
                    DLog.log("file already exists. SKIPPING - " + emFileMetaData.mFileName);
                    restoredAFile(emFileMetaData);
                } else {
                    currentMetaData = emFileMetaData;
                    boolean isFileRestored = false;
                    if (STORAGE_DEVICE_TYPE == EMStringConsts.EXTERNAL_STORAGE_SDCARD) {
                        File filePathDirectory = new File(new File(filePathToCopy).getParent());
                        if (!filePathDirectory.exists()) {
                            DLog.log("Trying to create file Directory : " + filePathDirectory);
                            filePathDirectory.mkdirs();
                        }
                        isFileRestored = copyFile(emFileMetaData, filePathDirectory.getAbsolutePath());
                    } else {
                        isFileRestored = restoreFromUSB(usbFile, filePathToCopy);
                    }
                    if (isFileRestored) {
                        emFileMetaData.mSourceFilePath = filePathToCopy; // to refresh the gallery setting path.
                        restoredAFile(emFileMetaData);
                    } else {
                        EMMigrateStatus.addItemNotTransferred(currentMetaData.mDataType);
                        EMMigrateStatus.addTransferredFailedSize(currentMetaData.mDataType, currentMetaData.mSize);
                    }
                }
            }
            sendTransferComplete(true);
        }

        private void backupMedia() {
            EMFileMetaData emFileMetaData = DeviceInfo.getInstance().getMetaData();
            String mPath = new SdcardUtils().getSdcardPath() + "/" + Constants.EXTERNAL_STORAGE_BACKUP_FOLDER;
            while (emFileMetaData != null) {
                checkMigrationStatus();
                currentMetaData = emFileMetaData;
                boolean fileBackedup = false;
                if (STORAGE_DEVICE_TYPE == EMStringConsts.EXTERNAL_STORAGE_USB) {
                    fileBackedup = BackupRestoreHelper.backupToUSB(emFileMetaData);
                } else {
                    File file = null;
                    if (emFileMetaData.mRelativePath != null) {
                        file = new File(mPath, emFileMetaData.mRelativePath);
                    } else {
                        file = new File(mPath + "/" + emFileMetaData.mFileName);
                    }
                    File filePathDirectory = new File(file.getParent());
                    if (!filePathDirectory.exists()) {
                        DLog.log("Trying to create file Directory : " + filePathDirectory);
                        filePathDirectory.mkdirs();
                    }
                    fileBackedup = copyFile(emFileMetaData, filePathDirectory.getAbsolutePath());
                }
                if (fileBackedup) {
                    processedFile(emFileMetaData);
                } else {
                    EMMigrateStatus.addItemNotTransferred(currentMetaData.mDataType);
                    EMMigrateStatus.addTransferredFailedSize(currentMetaData.mDataType, currentMetaData.mSize);
                }
                emFileMetaData = DeviceInfo.getInstance().getMetaData();
            }
            sendTransferComplete(true);
        }

        private void restorePIMData(String mCurrentDataType) {
            processPIMXML(EMUtility.createTempFile(mCurrentDataType.toLowerCase() + ".xml"));
        }

        private void backupPIMData(String mCurrentDataType) {
            writePIMXml();
        }

        private String getTargetFilePath(EMFileMetaData aFileInfo) {
            String filePath = null; // The path of the file to be created on the file system

            // Determine the target file name
            if (aFileInfo.mRelativePath != null && !aFileInfo.mRelativePath.equalsIgnoreCase("")) {
                try {
                    File completeFilePath = new File(Environment.getExternalStorageDirectory().toString() + "/" + aFileInfo.mRelativePath);
                    File filePathDirectory = new File(completeFilePath.getParent());
                    if (!filePathDirectory.exists()) {
                        DLog.log("Trying to create file Directory : " + filePathDirectory);
                        filePathDirectory.mkdirs();
                    }
                    if (filePathDirectory.exists()) {
                        filePath = completeFilePath.getAbsolutePath();
                    } else {
                        DLog.log("Failed to create Directory : " + filePathDirectory);
                        filePath = null;
                    }
                } catch (Exception e) {
                    filePath = null;
                    DLog.log("Exception while creating the directory : " + e.getMessage());
                }
            }
            if (TextUtils.isEmpty(filePath)) {
                filePath = Environment.getExternalStorageDirectory().toString() + "/";
                int mFileType = aFileInfo.mDataType;
                if (mFileType == EMDataType.EM_DATA_TYPE_PHOTOS) {
                    filePath += Environment.DIRECTORY_PICTURES + "/" + aFileInfo.mFileName;
                } else if (mFileType == EMDataType.EM_DATA_TYPE_VIDEO) {
                    filePath += Environment.DIRECTORY_MOVIES + "/" + aFileInfo.mFileName;
                } else if (mFileType == EMDataType.EM_DATA_TYPE_MUSIC) {
                    filePath += Environment.DIRECTORY_MUSIC + "/" + aFileInfo.mFileName;
                } else if (mFileType == EMDataType.EM_DATA_TYPE_APP) {
                    filePath += Constants.APP_MIGRATION_DIRECTORY + "/" + aFileInfo.mFileName;
                    AppMigrateUtils.addRestoreApp(filePath);
                } else {
                    filePath += Constants.DOCUMENTS_MIGRATION_DIRECTORY + "/" + aFileInfo.mFileName;
                }
            }
            if (filePath != null) {
                File file = new File(filePath);
                file.getParentFile().mkdirs();
            }
            return filePath;
        }

        private int getFileType(String fileName) {
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
            String fileType = null;
            int dataType = 0;
            fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (fileType != null) {
                if (fileType.startsWith("image")) {
                    dataType = EMDataType.EM_DATA_TYPE_PHOTOS;
                } else if (fileType.startsWith("video")) {
                    dataType = EMDataType.EM_DATA_TYPE_VIDEO;
                } else if (fileType.startsWith("audio")) {
                    dataType = EMDataType.EM_DATA_TYPE_MUSIC;
                }
            }
            return dataType;
        }

        private void checkMigrationStatus() {
            while (Constants.stopMigration) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}