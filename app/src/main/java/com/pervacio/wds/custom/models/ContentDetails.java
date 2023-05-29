package com.pervacio.wds.custom.models;

import static com.pervacio.wds.custom.utils.Constants.MIGRATION_NOT_STARTED;

/**
 * Created by Surya Polasanapalli on 12/18/2017.
 */

public class ContentDetails {
    private int contentType;
    private int totalCount;
    private boolean selected;
    private long totalSizeOfEntries = -1;
    private boolean isSupported;
    private int progressCount = -1;
    private boolean isPermissionGranted;
    private int migrationStatus = MIGRATION_NOT_STARTED;

    public ContentDetails(){

    }
    public ContentDetails(int contentType){
        this.contentType=contentType;
    }

    public int[] getImageDrawableId() {
        return imageDrawableId;
    }

    public void setImageDrawableId(int[] imageDrawableId) {
        this.imageDrawableId = imageDrawableId;
    }

    private int [] imageDrawableId;

    public String getContentName() {
        return ContentName;
    }

    public void setContentName(String contentName) {
        ContentName = contentName;
    }

    private String ContentName;

    public int getMigrationStatus() {
        return migrationStatus;
    }

    public void setMigrationStatus(int migrationStatus) {
        this.migrationStatus = migrationStatus;
    }

    public boolean isSupported() {
        return isSupported;
    }

    public void setSupported(boolean supported) {
        isSupported = supported;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public long getTotalSizeOfEntries() {
        return totalSizeOfEntries;
    }

    public void setTotalSizeOfEntries(long totalSizeOfEntries) {
        this.totalSizeOfEntries = totalSizeOfEntries;
    }

    public int getProgressCount() {
        return progressCount;
    }

    public void setProgressCount(int progressCount) {
        this.progressCount = progressCount;
    }

    @Override
    public String toString() {
        return String.valueOf(contentType);
    }

    @Override
    public int hashCode() {
        return contentType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ContentDetails) {
            if (contentType == ((ContentDetails) obj).contentType)
                return true;
            return false;
        }
        return false;
    }

    public boolean isPermissionGranted() {
        return isPermissionGranted;
    }

    public void setPermissionGranted(boolean permissionGranted) {
        isPermissionGranted = permissionGranted;
    }
}
