package com.pervacio.wds.custom.appmigration;

import android.graphics.drawable.Drawable;

import java.io.File;

/**
 * Model class to store metadata information of applications while backing up on source.
 *
 * @author : Darpan Dodiya <darpan.dodiya@pervacio.com>
 */
public class AppBackupModel {

    private String appName;
    private String packageName;
    private String versionName;
    private int versionCode;
    private Drawable appIcon;
    private long appMemory;
    private boolean checked = false;
    private boolean exist = false;
    private File file;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        setAppMemory(this.file.length());
    }

    public boolean isExist() {
        return exist;
    }

    public void setExist(boolean exist) {
        this.exist = exist;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public long getAppMemory() {
        return appMemory;
    }

    private void setAppMemory(long appMemory) {
        this.appMemory = appMemory;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
