package com.pervacio.wds.custom.models;

/**
 * Created by: Darpan Dodiya on 5/24/2017.
 * Contributors:
 * Last updated on: 5/26/2017
 */

public class EDeviceSwitchLogFiles  implements java.io.Serializable {
    private EDeviceSwitchSession deviceSwitchSessionId;
    private long endDateTime;
    private byte[] fileContent;
    private String fileFormat;
    private String fileLocation;
    private String fileName;
    private Integer fileSize;
    private Long logFileId;
    private String logFileType;
    private long startDateTime;
    private String storageServerType;

    public EDeviceSwitchLogFiles() {
    }

    public EDeviceSwitchLogFiles(
            EDeviceSwitchSession deviceSwitchSessionId,
            long endDateTime,
            byte[] fileContent,
            String fileFormat,
            String fileLocation,
            String fileName,
            Integer fileSize,
            Long logFileId,
            String logFileType,
            long startDateTime,
            String storageServerType) {
        this.deviceSwitchSessionId = deviceSwitchSessionId;
        this.endDateTime = endDateTime;
        this.fileContent = fileContent;
        this.fileFormat = fileFormat;
        this.fileLocation = fileLocation;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.logFileId = logFileId;
        this.logFileType = logFileType;
        this.startDateTime = startDateTime;
        this.storageServerType = storageServerType;
    }


    /**
     * Gets the deviceSwitchSessionId value for this EDeviceSwitchLogFiles.
     *
     * @return deviceSwitchSessionId
     */
    public EDeviceSwitchSession getDeviceSwitchSessionId() {
        return deviceSwitchSessionId;
    }


    /**
     * Sets the deviceSwitchSessionId value for this EDeviceSwitchLogFiles.
     *
     * @param deviceSwitchSessionId
     */
    public void setDeviceSwitchSessionId(EDeviceSwitchSession deviceSwitchSessionId) {
        this.deviceSwitchSessionId = deviceSwitchSessionId;
    }


    /**
     * Gets the endDateTime value for this EDeviceSwitchLogFiles.
     *
     * @return endDateTime
     */
    public long getEndDateTime() {
        return endDateTime;
    }


    /**
     * Sets the endDateTime value for this EDeviceSwitchLogFiles.
     *
     * @param endDateTime
     */
    public void setEndDateTime(long endDateTime) {
        this.endDateTime = endDateTime;
    }


    /**
     * Gets the fileContent value for this EDeviceSwitchLogFiles.
     *
     * @return fileContent
     */
    public byte[] getFileContent() {
        return fileContent;
    }


    /**
     * Sets the fileContent value for this EDeviceSwitchLogFiles.
     *
     * @param fileContent
     */
    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }


    /**
     * Gets the fileFormat value for this EDeviceSwitchLogFiles.
     *
     * @return fileFormat
     */
    public String getFileFormat() {
        return fileFormat;
    }


    /**
     * Sets the fileFormat value for this EDeviceSwitchLogFiles.
     *
     * @param fileFormat
     */
    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }


    /**
     * Gets the fileLocation value for this EDeviceSwitchLogFiles.
     *
     * @return fileLocation
     */
    public String getFileLocation() {
        return fileLocation;
    }


    /**
     * Sets the fileLocation value for this EDeviceSwitchLogFiles.
     *
     * @param fileLocation
     */
    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }


    /**
     * Gets the fileName value for this EDeviceSwitchLogFiles.
     *
     * @return fileName
     */
    public String getFileName() {
        return fileName;
    }


    /**
     * Sets the fileName value for this EDeviceSwitchLogFiles.
     *
     * @param fileName
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


    /**
     * Gets the fileSize value for this EDeviceSwitchLogFiles.
     *
     * @return fileSize
     */
    public Integer getFileSize() {
        return fileSize;
    }


    /**
     * Sets the fileSize value for this EDeviceSwitchLogFiles.
     *
     * @param fileSize
     */
    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }


    /**
     * Gets the logFileId value for this EDeviceSwitchLogFiles.
     *
     * @return logFileId
     */
    public Long getLogFileId() {
        return logFileId;
    }


    /**
     * Sets the logFileId value for this EDeviceSwitchLogFiles.
     *
     * @param logFileId
     */
    public void setLogFileId(Long logFileId) {
        this.logFileId = logFileId;
    }


    /**
     * Gets the logFileType value for this EDeviceSwitchLogFiles.
     *
     * @return logFileType
     */
    public String getLogFileType() {
        return logFileType;
    }


    /**
     * Sets the logFileType value for this EDeviceSwitchLogFiles.
     *
     * @param logFileType
     */
    public void setLogFileType(String logFileType) {
        this.logFileType = logFileType;
    }


    /**
     * Gets the startDateTime value for this EDeviceSwitchLogFiles.
     *
     * @return startDateTime
     */
    public long getStartDateTime() {
        return startDateTime;
    }


    /**
     * Sets the startDateTime value for this EDeviceSwitchLogFiles.
     *
     * @param startDateTime
     */
    public void setStartDateTime(long startDateTime) {
        this.startDateTime = startDateTime;
    }


    /**
     * Gets the storageServerType value for this EDeviceSwitchLogFiles.
     *
     * @return storageServerType
     */
    public String getStorageServerType() {
        return storageServerType;
    }


    /**
     * Sets the storageServerType value for this EDeviceSwitchLogFiles.
     *
     * @param storageServerType
     */
    public void setStorageServerType(String storageServerType) {
        this.storageServerType = storageServerType;
    }
}

