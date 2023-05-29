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

import java.util.ArrayList;

public class EMContentSizeCalculator {

    public interface EMContentSizeCalculatorDelegate {
        void contentSizesCalculatorResult(ContentSizeInfo aContentSizeInfo);
    }

    public class ContentSizeInfo {
        public int mPictureCount;
        public long mPictureFileSize;
        public int mVideoCount;
        public long mVideoFileSize;
        public int mMusicCount;
        public long mMusicFileSize;
        public int mDocumentsCount;
        public long mDocumentsFileSize;
    }

    private EMFileFinder mFileFinderTask;
    private ArrayList<EMFileMetaData> mFileMetaData = new ArrayList<EMFileMetaData>();

    public void start(EMContentSizeCalculatorDelegate aDelegate) {
        mDelegate = aDelegate;
        mFileFinderTask = new EMFileFinder(EMDataType.EM_DATA_TYPE_PHOTOS
                                            | EMDataType.EM_DATA_TYPE_VIDEO
                                            | EMDataType.EM_DATA_TYPE_MUSIC
                                            | EMDataType.EM_DATA_TYPE_DOCUMENTS, mFileMetaData);

        EMProgressHandler dummyProgressHandler = new EMProgressHandler() {
            @Override
            public void taskComplete(boolean aSuccess) {
                ContentSizeInfo info = new ContentSizeInfo();

                info.mPictureCount = getFileContentCount(EMDataType.EM_DATA_TYPE_PHOTOS);
                info.mPictureFileSize = getFileContentSize(EMDataType.EM_DATA_TYPE_PHOTOS);

                info.mVideoCount = getFileContentCount(EMDataType.EM_DATA_TYPE_VIDEO);
                info.mVideoFileSize = getFileContentSize(EMDataType.EM_DATA_TYPE_VIDEO);

                info.mMusicCount = getFileContentCount(EMDataType.EM_DATA_TYPE_MUSIC);
                info.mMusicFileSize = getFileContentSize(EMDataType.EM_DATA_TYPE_MUSIC);

                info.mDocumentsCount = getFileContentCount(EMDataType.EM_DATA_TYPE_DOCUMENTS);
                info.mDocumentsFileSize = getFileContentSize(EMDataType.EM_DATA_TYPE_DOCUMENTS);

                mDelegate.contentSizesCalculatorResult(info);
            }

            @Override
            public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
                taskComplete(true); // Ignore any errors
            }

            @Override
            public void progressUpdate(EMProgressInfo aProgressInfo) {
                // Ignore
            }
        };

        mFileFinderTask.startTask(dummyProgressHandler);
    }

    private int getFileContentCount(int dataType) {
        int count = 0;
        for(EMFileMetaData mData : mFileMetaData){
            if (mData.mDataType == dataType) {
                count++;
            }
        }
        return count;
    }

    private long getFileContentSize(int dataType) {
        long size = 0;
        for(EMFileMetaData mData : mFileMetaData){
            if (mData.mDataType == dataType) {
                size+=mData.mSize;
            }
        }
        return size;
    }

    EMContentSizeCalculatorDelegate mDelegate;
}
