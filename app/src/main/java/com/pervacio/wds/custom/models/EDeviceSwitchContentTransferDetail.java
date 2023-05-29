package com.pervacio.wds.custom.models;

/**
 * Created by: Darpan Dodiya on 5/24/2017.
 * Contributors:
 * Last updated on: 5/26/2017
 */

public class EDeviceSwitchContentTransferDetail  implements java.io.Serializable {

    /*** Required Fields ***/
    private String contentType;
    private Integer numberOfEntries;
    private long totalSizeOfEntries;
    private long startDateTime;
    private long endDateTime;
    private String transferState;
    private String transferStatus;

    private Long contentTransferDetailId;
    private EDeviceInfo deviceInfoId;

    //Not needed fields for now. Adding transient keyword will exclude them from JSON serialization.
    private Long estimatedTimeInMS;

    public EDeviceSwitchContentTransferDetail() {
    }

    public EDeviceSwitchContentTransferDetail(String contentType) {
        this.contentType = contentType;
    }

    public EDeviceSwitchContentTransferDetail(
            Long contentTransferDetailId,
            String contentType,
            EDeviceInfo deviceInfoId,
            long endDateTime,
            Long estimatedTimeInMS,
            Integer numberOfEntries,
            long startDateTime,
            long totalSizeOfEntries,
            String transferState,
            String transferStatus) {
        this.contentTransferDetailId = contentTransferDetailId;
        this.contentType = contentType;
        this.deviceInfoId = deviceInfoId;
        this.endDateTime = endDateTime;
        this.estimatedTimeInMS = estimatedTimeInMS;
        this.numberOfEntries = numberOfEntries;
        this.startDateTime = startDateTime;
        this.totalSizeOfEntries = totalSizeOfEntries;
        this.transferState = transferState;
        this.transferStatus = transferStatus;
    }


    /**
     * Gets the contentTransferDetailId value for this EDeviceSwitchContentTransferDetail.
     *
     * @return contentTransferDetailId
     */
    public Long getContentTransferDetailId() {
        return contentTransferDetailId;
    }


    /**
     * Sets the contentTransferDetailId value for this EDeviceSwitchContentTransferDetail.
     *
     * @param contentTransferDetailId
     */
    public void setContentTransferDetailId(Long contentTransferDetailId) {
        this.contentTransferDetailId = contentTransferDetailId;
    }


    /**
     * Gets the contentType value for this EDeviceSwitchContentTransferDetail.
     *
     * @return contentType
     */
    public String getContentType() {
        return contentType;
    }


    /**
     * Sets the contentType value for this EDeviceSwitchContentTransferDetail.
     *
     * @param contentType
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }


    /**
     * Gets the deviceInfoId value for this EDeviceSwitchContentTransferDetail.
     *
     * @return deviceInfoId
     */
    public EDeviceInfo getDeviceInfoId() {
        return deviceInfoId;
    }


    /**
     * Sets the deviceInfoId value for this EDeviceSwitchContentTransferDetail.
     *
     * @param deviceInfoId
     */
    public void setDeviceInfoId(EDeviceInfo deviceInfoId) {
        this.deviceInfoId = deviceInfoId;
    }


    /**
     * Gets the endDateTime value for this EDeviceSwitchContentTransferDetail.
     *
     * @return endDateTime
     */
    public long getEndDateTime() {
        return endDateTime;
    }


    /**
     * Sets the endDateTime value for this EDeviceSwitchContentTransferDetail.
     *
     * @param endDateTime
     */
    public void setEndDateTime(long endDateTime) {
        this.endDateTime = endDateTime;
    }


    /**
     * Gets the estimatedTimeInMS value for this EDeviceSwitchContentTransferDetail.
     *
     * @return estimatedTimeInMS
     */
    public Long getEstimatedTimeInMS() {
        return estimatedTimeInMS;
    }


    /**
     * Sets the estimatedTimeInMS value for this EDeviceSwitchContentTransferDetail.
     *
     * @param estimatedTimeInMS
     */
    public void setEstimatedTimeInMS(Long estimatedTimeInMS) {
        this.estimatedTimeInMS = estimatedTimeInMS;
    }


    /**
     * Gets the numberOfEntries value for this EDeviceSwitchContentTransferDetail.
     *
     * @return numberOfEntries
     */
    public Integer getNumberOfEntries() {
        return numberOfEntries;
    }


    /**
     * Sets the numberOfEntries value for this EDeviceSwitchContentTransferDetail.
     *
     * @param numberOfEntries
     */
    public void setNumberOfEntries(Integer numberOfEntries) {
        this.numberOfEntries = numberOfEntries;
    }


    /**
     * Gets the startDateTime value for this EDeviceSwitchContentTransferDetail.
     *
     * @return startDateTime
     */
    public long getStartDateTime() {
        return startDateTime;
    }


    /**
     * Sets the startDateTime value for this EDeviceSwitchContentTransferDetail.
     *
     * @param startDateTime
     */
    public void setStartDateTime(long startDateTime) {
        this.startDateTime = startDateTime;
    }


    /**
     * Gets the totalSizeOfEntries value for this EDeviceSwitchContentTransferDetail.
     *
     * @return totalSizeOfEntries
     */
    public long getTotalSizeOfEntries() {
        return totalSizeOfEntries;
    }


    /**
     * Sets the totalSizeOfEntries value for this EDeviceSwitchContentTransferDetail.
     *
     * @param totalSizeOfEntries
     */
    public void setTotalSizeOfEntries(long totalSizeOfEntries) {
        this.totalSizeOfEntries = totalSizeOfEntries;
    }


    /**
     * Gets the transferState value for this EDeviceSwitchContentTransferDetail.
     *
     * @return transferState
     */
    public String getTransferState() {
        return transferState;
    }


    /**
     * Sets the transferState value for this EDeviceSwitchContentTransferDetail.
     *
     * @param transferState
     */
    public void setTransferState(String transferState) {
        this.transferState = transferState;
    }


    /**
     * Gets the transferStatus value for this EDeviceSwitchContentTransferDetail.
     *
     * @return transferStatus
     */
    public String getTransferStatus() {
        return transferStatus;
    }


    /**
     * Sets the transferStatus value for this EDeviceSwitchContentTransferDetail.
     *
     * @param transferStatus
     */
    public void setTransferStatus(String transferStatus) {
        this.transferStatus = transferStatus;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof EDeviceSwitchContentTransferDetail){
            return ((EDeviceSwitchContentTransferDetail) obj).contentType.equalsIgnoreCase(this.contentType);
        }
        return false;
    }

}
