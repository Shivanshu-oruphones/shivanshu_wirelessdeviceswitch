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

import java.util.Date;

public class CMDBackupDetails {
    public enum CMDBackupType {
        CMD_GOOGLE_DRIVE,
        CMD_SD_CARD
    }

    public CMDBackupType mBackupType;
    public Date mBackupTimeStamp;
    public String mBackupDescription; // Device name
    public boolean mIsEncrypted = false;
    public byte[] mSalt; // The password to key generating salt
    public byte[] mReferenceData; // The data used to verify that the entered password + salt can be used to decrypt the data
}