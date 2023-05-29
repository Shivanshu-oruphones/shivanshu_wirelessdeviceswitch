package com.pervacio.wds.custom.externalstorage;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.custom.utils.Constants;

import java.io.File;

public class SdcardUtils {
    /**
     * This method checks sdcard slot available in the device or not.
     *
     * @return boolean
     */
    public boolean isSdCardFeatureAvailable() {
        //To do planning to give generic solution
        return false;
    }

    /**
     * This method checks the sdcard is inserted in the device or not
     *
     * @return boolean
     */
    public boolean isSdCardInserted() {
        return AppUtils.isSDCardPresent();

    }

    /**
     * This method will returns the inserted sdcard path
     *
     * @return String
     */
    public String getSdcardPath() {
        return new AppUtils().getSDCardPath();
    }

    /**
     * This method will returns the sdcard total size
     *
     * @return long
     */
    public long getSdCardToatalSize() {
        return AppUtils.getMemorySize(getSdcardPath(), true);
    }

    /**
     * This method will returns the sdcard available size
     *
     * @return long
     */
    public long getSdCardAvailableSize() {
        return AppUtils.getMemorySize(getSdcardPath(), false);
    }

    /**
     * This method will
     *
     * @return String Path - Returns path if sdcard is adopted as internal storage or else retuns
     * empty
     */
    public String getExpandedStoragePath() {
        return AppUtils.getExtendedMemoryPath();
    }

    /**
     * This method will returns sdcard adopted as internal total size
     *
     * @return long
     */
    public long getExpandedStorageTotalSize() {
        return AppUtils.getMemorySize(getExpandedStoragePath(), true);
    }

    /**
     * This method will returns the sdcard  adopted as internal available size
     *
     * @return long
     */
    public long getExpandedStorageAvailableSize() {
        return AppUtils.getMemorySize(getExpandedStoragePath(), false);
    }


    @TargetApi(Build.VERSION_CODES.N)
    public void triggerStorageAccessFramework(Activity mActivity, Context context) {
        try {
            String sdCardPath = AppUtils.getSDCardPath();
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            StorageVolume volume = storageManager.getStorageVolume(new File(sdCardPath));
            Intent intent = volume.createAccessIntent(null);
            mActivity.startActivityForResult(intent, 42);
        } catch (Exception e) {
            DLog.log("Exception: triggerStorageAccessFramework");
        }
    }

}
