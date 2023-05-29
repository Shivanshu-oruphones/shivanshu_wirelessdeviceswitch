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

public abstract class CMDBackupMonitorDelegate {
    public abstract void backupFound(CMDBackupDetails aDetails);
    public abstract void noBackupFound(); // Only ever called when in EMSingleCheck mode
    public abstract void backupMonitorError(int aError); // Only ever called when in EMSingleCheck mode
    public abstract void backupMonitorPaused(); // Called when the backup monitor is paused (for example when no wifi network is available)
    public abstract void backupMonitorResumed();  // Called when the backup monitor is resumed (for example when the wifi network becomes available)
}