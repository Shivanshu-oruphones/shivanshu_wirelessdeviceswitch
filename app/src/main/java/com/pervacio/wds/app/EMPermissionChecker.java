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

import android.content.Context;
import android.content.pm.PackageManager;

import java.util.HashSet;
import java.util.Set;

public class EMPermissionChecker {
    public EMPermissionChecker(Context aContext) {
        mContext = aContext;
    }

    private Context mContext;

    public Set<Integer> getDataTypesWithMissingPermissions(boolean aDeviceIsTarget) {
        Set<Integer> dataTypesWithMissingPermissions = new HashSet<Integer>();
        getMissingPermissions(aDeviceIsTarget);

        if (mMissingPermissions.contains("android.permission.READ_CONTACTS"))
            dataTypesWithMissingPermissions.add(EMDataType.EM_DATA_TYPE_CONTACTS);

        if (mMissingPermissions.contains("android.permission.READ_CALENDAR"))
            dataTypesWithMissingPermissions.add(EMDataType.EM_DATA_TYPE_CALENDAR);

        if (mMissingPermissions.contains("android.permission.READ_SMS"))
            dataTypesWithMissingPermissions.add(EMDataType.EM_DATA_TYPE_SMS_MESSAGES);

        if (mMissingPermissions.contains("android.permission.WRITE_SMS"))
            dataTypesWithMissingPermissions.add(EMDataType.EM_DATA_TYPE_SMS_MESSAGES);

        if ((mMissingPermissions.contains("android.permission.WRITE_EXTERNAL_STORAGE"))
                || (mMissingPermissions.contains("android.permission.READ_EXTERNAL_STORAGE"))) {
            dataTypesWithMissingPermissions.add(EMDataType.EM_DATA_TYPE_DOCUMENTS);
            dataTypesWithMissingPermissions.add(EMDataType.EM_DATA_TYPE_MUSIC);
            dataTypesWithMissingPermissions.add(EMDataType.EM_DATA_TYPE_PHOTOS);
            dataTypesWithMissingPermissions.add(EMDataType.EM_DATA_TYPE_VIDEO);
        }

        if (mMissingPermissions.contains("android.permission.WRITE_CONTACTS"))
            dataTypesWithMissingPermissions.add(EMDataType.EM_DATA_TYPE_CONTACTS);

        if (mMissingPermissions.contains("android.permission.WRITE_CALENDAR"))
            dataTypesWithMissingPermissions.add(EMDataType.EM_DATA_TYPE_CALENDAR);

        return dataTypesWithMissingPermissions;
    }

    public void getMissingPermissions(boolean aDeviceIsTarget) {
        // Build a list of all the permissions we need but are missing
        addRequiredPermission("android.permission.READ_CONTACTS");
        addRequiredPermission("android.permission.READ_CALENDAR");
        addRequiredPermission("android.permission.READ_SMS");
        addRequiredPermission("android.permission.WRITE_SMS");
        addRequiredPermission("android.permission.WRITE_EXTERNAL_STORAGE");
        addRequiredPermission("android.permission.WRITE_CONTACTS");
        addRequiredPermission("android.permission.WRITE_CALENDAR");
        addRequiredPermission("android.permission.READ_EXTERNAL_STORAGE");
    }

    public void addRequiredPermission(String aPermission) {
        if (EMUtility.checkSelfPermission(mContext, aPermission) != PackageManager.PERMISSION_GRANTED) {
            mMissingPermissions.add(aPermission);
        }
    }

    public Set<String> missingPermissions() {
        return mMissingPermissions;
    }

    private Set<String> mMissingPermissions = new HashSet<String>();
}