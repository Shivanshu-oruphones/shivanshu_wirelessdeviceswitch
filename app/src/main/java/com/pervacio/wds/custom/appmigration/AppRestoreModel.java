package com.pervacio.wds.custom.appmigration;

import android.graphics.drawable.Drawable;

import java.io.File;

/**
 * Model class to store metadata information of applications while restoring in destination.
 *
 * Note than right now, only one property (path) of the below model is being used. Other properties are
 * present for future extension.
 *
 * @author : Darpan Dodiya <darpan.dodiya@pervacio.com>
 */

public class AppRestoreModel {

    private String name;
    private String path;
    private File file;
    private Drawable icon;
    private long app_memory;
    private boolean checked;
    private String packageName;

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        setApp_memory(this.file.length());
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public long getApp_memory() {
        return app_memory;
    }

    private void setApp_memory(long app_memory) {
        this.app_memory = app_memory;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
