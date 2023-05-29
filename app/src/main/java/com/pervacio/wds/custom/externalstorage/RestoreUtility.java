package com.pervacio.wds.custom.externalstorage;

import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMDataType;
import com.pervacio.wds.app.EMFileMetaData;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.app.EMMigrateStatus;
import com.pervacio.wds.app.EMStringConsts;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.EMXmlPullParser;
import com.pervacio.wds.custom.APPI;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/*Created by Surya Polasanapalli*/

public class RestoreUtility {
    static EMGlobals emGlobals = new EMGlobals();
    private static RestoreUtility restoreUtility;
    private final HashMap<String, String> dataTypeTagMap = new HashMap<>();
    private Queue<Object> filesQueue = new LinkedList<>();
    private long photosSize = 0;
    private long videosSize = 0;
    private long audiosSize = 0;
    private List<Object> photosList = new ArrayList<>();
    private List<Object> videosList = new ArrayList<>();
    private List<Object> audiosList = new ArrayList<>();
    private int EXTERNAL_STORAGE_TYPE = 0;
    private String sdcardPath;

    {
        dataTypeTagMap.put(EMStringConsts.DATA_TYPE_CONTACTS, EMStringConsts.EM_XML_CONTACT_ENTRY);
        dataTypeTagMap.put(EMStringConsts.DATA_TYPE_CALENDAR, EMStringConsts.EM_XML_CALENDAR_ENTRY);
        dataTypeTagMap.put(EMStringConsts.DATA_TYPE_CALL_LOGS, EMStringConsts.EM_XML_CALL_LOG_ENTRY);
        dataTypeTagMap.put(EMStringConsts.DATA_TYPE_SMS_MESSAGES, EMStringConsts.EM_XML_SMS_MESSAGES);
        dataTypeTagMap.put(EMStringConsts.DATA_TYPE_SETTINGS, EMStringConsts.EM_XML_SETTINGS);
    }

    private RestoreUtility(int storageType) {
        EXTERNAL_STORAGE_TYPE = storageType;
    }

    public static RestoreUtility getInstance(int storageType) {
        if (restoreUtility == null) {
            restoreUtility = new RestoreUtility(storageType);
        }
        return restoreUtility;
    }

    public synchronized Queue<Object> getFileList() {
        return filesQueue;
    }

    public synchronized Object getMetaData() {
        try {
            return filesQueue.remove();
        } catch (Exception e) {
            return null;
        }
    }

    public int getCount(int dataType) {
        int count = 0;
       /* if (EXTERNAL_STORAGE_TYPE == EMStringConsts.EXTERNAL_STORAGE_SDCARD) {
            count = (int) getContentDetails(dataType, true);
        } else {*/
        if (filesQueue == null) {
            prepareList();
        }
        switch (dataType) {
            case EMDataType.EM_DATA_TYPE_PHOTOS:
                count = photosList.size();
                break;
            case EMDataType.EM_DATA_TYPE_VIDEO:
                count =  videosList.size();
                break;
            case EMDataType.EM_DATA_TYPE_MUSIC:
                count = audiosList.size();
                break;
            default:
                break;
        }
        EMMigrateStatus.addContentDetails(dataType, count);
        // }
        return count;
    }

    public long getSize(int dataType) {
        long totalSize = 0L;
        /*if (EXTERNAL_STORAGE_TYPE == EMStringConsts.EXTERNAL_STORAGE_SDCARD) {
            totalSize = getContentDetails(dataType, false);
        } else {*/
        if (filesQueue == null) {
            prepareList();
        }
        switch (dataType) {
            case EMDataType.EM_DATA_TYPE_PHOTOS:
                return photosSize;
            case EMDataType.EM_DATA_TYPE_VIDEO:
                return videosSize;
            case EMDataType.EM_DATA_TYPE_MUSIC:
                return audiosSize;
            default:
                break;
        }
        //}
        return totalSize;
    }

    public int getCount(String datatype) {
        datatype = datatype.toLowerCase();
        String mPath = EMUtility.createTempFile(datatype + ".xml");
        boolean filePresent = false;
        int count = 0;
        if (EXTERNAL_STORAGE_TYPE == EMStringConsts.EXTERNAL_STORAGE_USB) {
            filePresent = copyFileFromUSB(datatype, mPath);
        } else {
            File sourceFile = new File(new SdcardUtils().getSdcardPath() + "/" + Constants.EXTERNAL_STORAGE_BACKUP_FOLDER + "/" + datatype + ".xml");
            if (sourceFile.exists()) {
                File destination = new File(mPath);
                filePresent = EMUtility.copyFile(sourceFile, destination);
            }
        }
        if (filePresent) {
            count = parseXml(mPath, dataTypeTagMap.get(datatype.toUpperCase()));
        }
        EMMigrateStatus.addContentDetails(EMStringConsts.DATATYPE_MAP.get(datatype.toUpperCase()), count);
        return count;
    }

    private boolean copyFileFromUSB(String mDatatype, String mPath) {
        boolean fileCopied = false;
        try {
            UsbFile usbDir = CommonUtil.getInstance().getUsbDevice().getPartitions().get(0).getFileSystem().getRootDirectory().search(Constants.EXTERNAL_STORAGE_BACKUP_FOLDER);
            UsbFile usbFile = usbDir.search(mDatatype + ".xml");
            if (usbFile == null) {
                DLog.log("No file found with name : " + mDatatype);
            } else {
                fileCopied = restoreFromUSB(usbFile, mPath);
                int retryCount = 0;
                while (!fileCopied && retryCount < 3) {
                    fileCopied = restoreFromUSB(usbFile, mPath);
                    ++retryCount;
                }
            }
        } catch (Exception e) {
            DLog.log("Exception while copyfile : " + mDatatype + " " + e.getMessage());
        }
        return fileCopied;
    }

    public void prepareList() {
        if (EXTERNAL_STORAGE_TYPE == EMStringConsts.EXTERNAL_STORAGE_USB) {
            getUSBFilesList();
        } else {
            sdcardPath = new SdcardUtils().getSdcardPath();
            getSDCardFilesList(new File(sdcardPath + "/" + Constants.EXTERNAL_STORAGE_BACKUP_FOLDER));
        }
    }

    public void getSDCardFilesList(File dir) {
        try {
            File listFile[] = dir.listFiles();
            if (listFile != null) {
                for (int i = 0; i < listFile.length; i++) {
                    if (listFile[i].isDirectory()) {
                        getSDCardFilesList(listFile[i]);
                    } else {
                        int pos = listFile[i].getName().lastIndexOf(".");
                        File file = listFile[i];
                        String fileExtension = file.getName().substring(pos + 1).toLowerCase();
                        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
                        DLog.log("Name : " + file.getName() + ",Extension : " + fileExtension + ", MIME Type : " + mimeType);
                        if (mimeType != null) {
                            int type = 0;
                            EMFileMetaData emFileMetaData = new EMFileMetaData();
                            emFileMetaData.mDataType = type;
                            emFileMetaData.mFileName = listFile[i].getName();
                            emFileMetaData.mSize = listFile[i].length();
                            emFileMetaData.mSourceFilePath = listFile[i].getAbsolutePath();
                            String relativePath = listFile[i].getAbsolutePath();
                            if (relativePath.startsWith(sdcardPath)) {
                                relativePath = listFile[i].getAbsolutePath().replace(sdcardPath + "/" + Constants.EXTERNAL_STORAGE_BACKUP_FOLDER, "");
                            } else {
                                DLog.log("Folder structure Mismatch : " + listFile[i].getAbsolutePath());
                                relativePath = null;
                            }
                            emFileMetaData.mRelativePath = relativePath;
                            if (mimeType.startsWith("image")) {
                                emFileMetaData.mDataType = EMDataType.EM_DATA_TYPE_PHOTOS;
                                photosSize += file.length();
                                photosList.add(emFileMetaData);
                            } else if (mimeType.startsWith("video")) {
                                videosSize += file.length();
                                emFileMetaData.mDataType = EMDataType.EM_DATA_TYPE_VIDEO;
                                videosList.add(emFileMetaData);
                            } else if (mimeType.startsWith("audio")) {
                                audiosSize += file.length();
                                emFileMetaData.mDataType = EMDataType.EM_DATA_TYPE_MUSIC;
                                audiosList.add(emFileMetaData);
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getContentDetails(int mDataType, boolean aCountOnly) {
        Uri uri = null;
        int mFilePathColumn = -1;
        long value = -1;
        Cursor mCursor = null;
        String whereQuery = null;

        if (mDataType == EMDataType.EM_DATA_TYPE_PHOTOS) {
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            whereQuery = MediaStore.Images.Media.SIZE + " != 0";
        } else if (mDataType == EMDataType.EM_DATA_TYPE_VIDEO) {
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            whereQuery = MediaStore.Video.Media.SIZE + " != 0";
        } else if (mDataType == EMDataType.EM_DATA_TYPE_MUSIC) {
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            whereQuery = MediaStore.Audio.Media.SIZE + " != 0";
        }

        try {
            mCursor = emGlobals.getmContext().getContentResolver().query(uri,
                    null,       // projection
                    whereQuery, // where - to get Specific rows.
                    null,       // arguments - none
                    null        // ordering - doesn't matter
            );
            if (mCursor != null) {
                if (aCountOnly) {
                    value = mCursor.getCount();
                } else {
                    if (mDataType == EMDataType.EM_DATA_TYPE_PHOTOS)
                        mFilePathColumn = mCursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    else if (mDataType == EMDataType.EM_DATA_TYPE_VIDEO)
                        mFilePathColumn = mCursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA);
                    else if (mDataType == EMDataType.EM_DATA_TYPE_MUSIC)
                        mFilePathColumn = mCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);

                    value = getTotalMediaSize(mCursor, mFilePathColumn);
                }
            }
        } catch (Exception ex) {
            DLog.log(ex);
        } finally {
            if (mCursor != null)
                mCursor.close();
            return value;
        }
    }

    private long getTotalMediaSize(Cursor mCursor, int mFilePathColumn) {
        if (!mCursor.moveToFirst() || mFilePathColumn == -1) {
            // Log.d("TAG", "=== getTotalMediaSize, Could not move to first item");
            return 0;
        }

        long totalSize = 0;

        do {
            String filePath = mCursor.getString(mFilePathColumn);
            File file = new File(filePath);
            if (file.exists() && file.getAbsolutePath().startsWith(sdcardPath)) {
                totalSize += file.length();
            }

        } while (mCursor.moveToNext());

        return totalSize;
    }

    public void prepareFilesQueue(int mDataType) {
        Uri uri = null;
        Cursor mCursor = null;
        int mFilePathColumn = 0;
        sdcardPath = new SdcardUtils().getSdcardPath();
        String whereQuery = null;
        if (mDataType == EMDataType.EM_DATA_TYPE_PHOTOS) {
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            whereQuery = MediaStore.Images.Media.SIZE + " != 0";
        } else if (mDataType == EMDataType.EM_DATA_TYPE_VIDEO) {
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            whereQuery = MediaStore.Video.Media.SIZE + " != 0";
        } else if (mDataType == EMDataType.EM_DATA_TYPE_MUSIC) {
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            whereQuery = MediaStore.Audio.Media.SIZE + " != 0";
        }

        try {
            mCursor = emGlobals.getmContext().getContentResolver().query(uri,
                    null,       // projection
                    whereQuery, // where - to get Specific rows.
                    null,       // arguments - none
                    null        // orderng - doesn't matter
            );

            if (mCursor != null && mDataType == EMDataType.EM_DATA_TYPE_PHOTOS)
                mFilePathColumn = mCursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            else if (mCursor != null && mDataType == EMDataType.EM_DATA_TYPE_VIDEO)
                mFilePathColumn = mCursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA);
            else if (mCursor != null && mDataType == EMDataType.EM_DATA_TYPE_MUSIC)
                mFilePathColumn = mCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);

            if (mCursor != null) {
                while (mCursor.moveToNext()) {
                    String filePath = mCursor.getString(mFilePathColumn);
                    File file = new File(filePath);
                    try {
                        if (file.exists() && file.canRead()) {
                            EMFileMetaData fileMetaData = getFileInfoString(file, mDataType);
                            if (fileMetaData != null) {
                                filesQueue.add(fileMetaData);
                            }
                        }
                    } catch (Exception e) {
                        DLog.log("Exception while adding file to Queue : " + e);
                    }
                }
            }
        } catch (Exception ex) {
            DLog.log("Exception in prepareFilesQueue : " + ex);
        } finally {
            if (mCursor != null)
                mCursor.close();
        }
    }

    private EMFileMetaData getFileInfoString(File file, int type) {
        EMFileMetaData emFileMetaData = null;
        try {
            emFileMetaData = new EMFileMetaData();
            emFileMetaData.mDataType = type;
            emFileMetaData.mFileName = file.getName();
            emFileMetaData.mSize = file.length();
            emFileMetaData.mSourceFilePath = file.getAbsolutePath();
            String relativePath = file.getAbsolutePath();
            if (relativePath.startsWith(sdcardPath)) {
                relativePath = file.getAbsolutePath().replace(sdcardPath, "");
                emFileMetaData.mRelativePath = relativePath;
            } else {
                emFileMetaData = null; // make it null if its not in sdcard.
            }
        } catch (Exception e) {
            DLog.log("Exception while generating file meta data : " + e.getMessage());
        }
        return emFileMetaData;
    }


    private void getUSBFilesList() {
        try {
            filesQueue = new LinkedList<>();
            UsbFile usbDir = CommonUtil.getInstance().getUsbDevice().getPartitions().get(0).getFileSystem().getRootDirectory().search(Constants.EXTERNAL_STORAGE_BACKUP_FOLDER);
            UsbFile[] files = usbDir.listFiles();
            for (UsbFile file : files) {
                try {
                    String fileName = file.getName();
                    String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
                    String fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
                    DLog.log("Name : " + fileName + ",Extension : " + extension + ", MIME Type : " + fileType);
                    if (fileType != null) {
                        if (fileType.startsWith("image")) {
                            photosSize += file.getLength();
                            photosList.add(file);
                        } else if (fileType.startsWith("video")) {
                            videosSize += file.getLength();
                            videosList.add(file);
                        } else if (fileType.startsWith("audio")) {
                            audiosSize += file.getLength();
                            audiosList.add(file);
                        }
                    }
                } catch (Exception e) {
                    DLog.log("Exception while prepareFileslist : " + e.getMessage());
                }
            }
        } catch (Exception e) {
            DLog.log("Exception while prepareFileslist : " + e.getMessage());
        }
    }

    private int parseXml(String mFilePath, String element) {
        int numberOfEntriesRet = 0;
        EMXmlPullParser.EMXmlNodeType nodeType;
        EMXmlPullParser pullParser = new EMXmlPullParser();
        try {
            pullParser.setFilePath(mFilePath);
            nodeType = pullParser.readNode();
        } catch (Exception ex) {
            // Something has gone badly wrong and we can't read the file to bail out
            DLog.log("parseXml failed : " + ex.getMessage());
            return 0;
        }

        while (nodeType != EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ROOT_ELEMENT) {
            try {
                if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_START_ELEMENT) {
                    String elementName = pullParser.name();
                    if ((elementName.equalsIgnoreCase(element))) {
                        nodeType = pullParser.readNode();
                        if (elementName.equalsIgnoreCase(element))
                            numberOfEntriesRet++;
                    }
                }
                nodeType = pullParser.readNode();
            } catch (Exception ex) {
                nodeType = EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ROOT_ELEMENT;
                DLog.log("Exception in parseXML:", ex);
            }
        }
        return numberOfEntriesRet;
    }


    private synchronized boolean restoreFromUSB(UsbFile sourceUSBFile, String destinationPath) {
        boolean result = true;
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new UsbFileInputStream(sourceUSBFile);
            output = new FileOutputStream(new File(destinationPath));
            byte[] buffer = new byte[128 * 1024];
            int bytesRead = 0;
            while ((bytesRead = input.read(buffer)) > 0) {
                output.write(buffer, 0, bytesRead);
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

    public void addToFilesList(int dataType) {
        /*if (EXTERNAL_STORAGE_TYPE == EMStringConsts.EXTERNAL_STORAGE_SDCARD) {
            prepareFilesQueue(dataType);
        } else {*/
        if (dataType == EMDataType.EM_DATA_TYPE_PHOTOS) {
            filesQueue.addAll(photosList);
            photosList.clear();
        } else if (dataType == EMDataType.EM_DATA_TYPE_VIDEO) {
            filesQueue.addAll(videosList);
            videosList.clear();
        } else if (dataType == EMDataType.EM_DATA_TYPE_MUSIC) {
            filesQueue.addAll(audiosList);
            audiosList.clear();
        }
        //}
    }


}
