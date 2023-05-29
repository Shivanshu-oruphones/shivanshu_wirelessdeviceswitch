package com.pervacio.wds.custom.models;

import java.math.BigDecimal;

/**
 * This class is parent class (entry point) to Dashboard logging. All other classes will be referenced
 * by this class.
 *
 * Created by: Darpan Dodiya on 5/24/2017.
 * Contributors:
 * Last updated on: 5/26/2017
 */

public class EDeviceSwitchSession implements java.io.Serializable {

    /*** Required Fields ***/
    //Session related
    private String companyId;
    private String sessionStage;
    private String sessionStatus;
    private String storeId;
    private String userId;
    private String startDateTime;
    private String endDateTime;
    private String stationId;
    private String transactionType;
    private BigDecimal latitude = BigDecimal.valueOf(-1);
    private BigDecimal longitude = BigDecimal.valueOf(-1);
    private String transferMode;
    private String reasonCode;
    private String devicesSwapped;
    private String guiversion;
    private String actualTimeInMS = "-1";
    private String cancellationReason;
    private String estimatedTimeInMS;
    private String transferType;

    private String deviceSwitchSessionId = null;

    //Content related
    private EDeviceSwitchSourceContentSummary[] EDeviceSwitchSourceContentSummaryCollection;

    //Device related
    private EDeviceInfo sourceDeviceInfoId;
    private EDeviceInfo destinationDeviceInfoId;

    //Not needed fields for now. Adding transient keyword will exclude them from JSON serialization.
    private String additionalInfo = "";
    private transient String componentFailed;
    private transient String creationDttm;
    private transient String lastUpdatedDttm;
    private transient String systemException;
    private transient byte[] systemInfo;
    private transient String timeToGetContentInfoForReview;
    private transient EDeviceSwitchActivitLlog[] EDeviceSwitchActivitLlogCollection;
    private transient EDeviceSwitchLogFiles[] EDeviceSwitchLogFilesCollection;


    public EDeviceSwitchSession() {
    }

    public EDeviceSwitchSession(
            String actualTimeInMS,
            String additionalInfo,
            String cancellationReason,
            String companyId,
            String componentFailed,
            String creationDttm,
            EDeviceInfo destinationDeviceInfoId,
            String devicesSwapped,
            EDeviceSwitchActivitLlog[] EDeviceSwitchActivitLlogCollection,
            EDeviceSwitchLogFiles[] EDeviceSwitchLogFilesCollection,
            EDeviceSwitchSourceContentSummary[] EDeviceSwitchSourceContentSummaryCollection,
            String endDateTime,
            String estimatedTimeInMS,
            String guiversion,
            String lastUpdatedDttm,
            String reasonCode,
            String sessionStage,
            String sessionStatus,
            EDeviceInfo sourceDeviceInfoId,
            String startDateTime,
            String stationId,
            String storeId,
            String systemException,
            byte[] systemInfo,
            String timeToGetContentInfoForReview,
            String userId) {
        this.actualTimeInMS = actualTimeInMS;
        this.additionalInfo = additionalInfo;
        this.cancellationReason = cancellationReason;
        this.companyId = companyId;
        this.componentFailed = componentFailed;
        this.creationDttm = creationDttm;
        this.destinationDeviceInfoId = destinationDeviceInfoId;
        this.devicesSwapped = devicesSwapped;
        this.EDeviceSwitchActivitLlogCollection = EDeviceSwitchActivitLlogCollection;
        this.EDeviceSwitchLogFilesCollection = EDeviceSwitchLogFilesCollection;
        this.EDeviceSwitchSourceContentSummaryCollection = EDeviceSwitchSourceContentSummaryCollection;
        this.endDateTime = endDateTime;
        this.estimatedTimeInMS = estimatedTimeInMS;
        this.guiversion = guiversion;
        this.lastUpdatedDttm = lastUpdatedDttm;
        this.reasonCode = reasonCode;
        this.sessionStage = sessionStage;
        this.sessionStatus = sessionStatus;
        this.sourceDeviceInfoId = sourceDeviceInfoId;
        this.startDateTime = startDateTime;
        this.stationId = stationId;
        this.storeId = storeId;
        this.systemException = systemException;
        this.systemInfo = systemInfo;
        this.timeToGetContentInfoForReview = timeToGetContentInfoForReview;
        this.userId = userId;
    }


    /**
     * Gets the actualTimeInMS value for this EDeviceSwitchSession.
     *
     * @return actualTimeInMS
     */
    public String getActualTimeInMS() {
        return actualTimeInMS;
    }


    /**
     * Sets the actualTimeInMS value for this EDeviceSwitchSession.
     *
     * @param actualTimeInMS
     */
    public void setActualTimeInMS(String actualTimeInMS) {
        this.actualTimeInMS = actualTimeInMS;
    }


    /**
     * Gets the additionalInfo value for this EDeviceSwitchSession.
     *
     * @return additionalInfo
     */
    public String getAdditionalInfo() {
        return additionalInfo;
    }


    /**
     * Sets the additionalInfo value for this EDeviceSwitchSession.
     *
     * @param additionalInfo
     */
    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }


    /**
     * Gets the cancellationReason value for this EDeviceSwitchSession.
     *
     * @return cancellationReason
     */
    public String getCancellationReason() {
        return cancellationReason;
    }


    /**
     * Sets the cancellationReason value for this EDeviceSwitchSession.
     *
     * @param cancellationReason
     */
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }


    /**
     * Gets the companyId value for this EDeviceSwitchSession.
     *
     * @return companyId
     */
    public String getCompanyId() {
        return companyId;
    }


    /**
     * Sets the companyId value for this EDeviceSwitchSession.
     *
     * @param companyId
     */
    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }


    /**
     * Gets the componentFailed value for this EDeviceSwitchSession.
     *
     * @return componentFailed
     */
    public String getComponentFailed() {
        return componentFailed;
    }


    /**
     * Sets the componentFailed value for this EDeviceSwitchSession.
     *
     * @param componentFailed
     */
    public void setComponentFailed(String componentFailed) {
        this.componentFailed = componentFailed;
    }


    /**
     * Gets the creationDttm value for this EDeviceSwitchSession.
     *
     * @return creationDttm
     */
    public String getCreationDttm() {
        return creationDttm;
    }


    /**
     * Sets the creationDttm value for this EDeviceSwitchSession.
     *
     * @param creationDttm
     */
    public void setCreationDttm(String creationDttm) {
        this.creationDttm = creationDttm;
    }


    /**
     * Gets the destinationDeviceInfoId value for this EDeviceSwitchSession.
     *
     * @return destinationDeviceInfoId
     */
    public EDeviceInfo getDestinationDeviceInfoId() {
        return destinationDeviceInfoId;
    }


    /**
     * Sets the destinationDeviceInfoId value for this EDeviceSwitchSession.
     *
     * @param destinationDeviceInfoId
     */
    public void setDestinationDeviceInfoId(EDeviceInfo destinationDeviceInfoId) {
        this.destinationDeviceInfoId = destinationDeviceInfoId;
    }


    /**
     * Gets the devicesSwapped value for this EDeviceSwitchSession.
     *
     * @return devicesSwapped
     */
    public String getDevicesSwapped() {
        return devicesSwapped;
    }


    /**
     * Sets the devicesSwapped value for this EDeviceSwitchSession.
     *
     * @param devicesSwapped
     */
    public void setDevicesSwapped(String devicesSwapped) {
        this.devicesSwapped = devicesSwapped;
    }


    /**
     * Gets the EDeviceSwitchActivitLlogCollection value for this EDeviceSwitchSession.
     *
     * @return EDeviceSwitchActivitLlogCollection
     */
    public EDeviceSwitchActivitLlog[] getEDeviceSwitchActivitLlogCollection() {
        return EDeviceSwitchActivitLlogCollection;
    }


    /**
     * Sets the EDeviceSwitchActivitLlogCollection value for this EDeviceSwitchSession.
     *
     * @param EDeviceSwitchActivitLlogCollection
     */
    public void setEDeviceSwitchActivitLlogCollection(EDeviceSwitchActivitLlog[] EDeviceSwitchActivitLlogCollection) {
        this.EDeviceSwitchActivitLlogCollection = EDeviceSwitchActivitLlogCollection;
    }

    public EDeviceSwitchActivitLlog getEDeviceSwitchActivitLlogCollection(int i) {
        return this.EDeviceSwitchActivitLlogCollection[i];
    }

    public void setEDeviceSwitchActivitLlogCollection(int i, EDeviceSwitchActivitLlog _value) {
        this.EDeviceSwitchActivitLlogCollection[i] = _value;
    }


    /**
     * Gets the EDeviceSwitchLogFilesCollection value for this EDeviceSwitchSession.
     *
     * @return EDeviceSwitchLogFilesCollection
     */
    public EDeviceSwitchLogFiles[] getEDeviceSwitchLogFilesCollection() {
        return EDeviceSwitchLogFilesCollection;
    }


    /**
     * Sets the EDeviceSwitchLogFilesCollection value for this EDeviceSwitchSession.
     *
     * @param EDeviceSwitchLogFilesCollection
     */
    public void setEDeviceSwitchLogFilesCollection(EDeviceSwitchLogFiles[] EDeviceSwitchLogFilesCollection) {
        this.EDeviceSwitchLogFilesCollection = EDeviceSwitchLogFilesCollection;
    }

    public EDeviceSwitchLogFiles getEDeviceSwitchLogFilesCollection(int i) {
        return this.EDeviceSwitchLogFilesCollection[i];
    }

    public void setEDeviceSwitchLogFilesCollection(int i, EDeviceSwitchLogFiles _value) {
        this.EDeviceSwitchLogFilesCollection[i] = _value;
    }


    /**
     * Gets the EDeviceSwitchSourceContentSummaryCollection value for this EDeviceSwitchSession.
     *
     * @return EDeviceSwitchSourceContentSummaryCollection
     */
    public EDeviceSwitchSourceContentSummary[] getEDeviceSwitchSourceContentSummaryCollection() {
        return EDeviceSwitchSourceContentSummaryCollection;
    }


    /**
     * Sets the EDeviceSwitchSourceContentSummaryCollection value for this EDeviceSwitchSession.
     *
     * @param EDeviceSwitchSourceContentSummaryCollection
     */
    public void setEDeviceSwitchSourceContentSummaryCollection(EDeviceSwitchSourceContentSummary[] EDeviceSwitchSourceContentSummaryCollection) {
        this.EDeviceSwitchSourceContentSummaryCollection = EDeviceSwitchSourceContentSummaryCollection;
    }

    public EDeviceSwitchSourceContentSummary getEDeviceSwitchSourceContentSummaryCollection(int i) {
        return this.EDeviceSwitchSourceContentSummaryCollection[i];
    }

    public void setEDeviceSwitchSourceContentSummaryCollection(int i, EDeviceSwitchSourceContentSummary _value) {
        this.EDeviceSwitchSourceContentSummaryCollection[i] = _value;
    }


    /**
     * Gets the endDateTime value for this EDeviceSwitchSession.
     *
     * @return endDateTime
     */
    public String getEndDateTime() {
        return endDateTime;
    }


    /**
     * Sets the endDateTime value for this EDeviceSwitchSession.
     *
     * @param endDateTime
     */
    public void setEndDateTime(String endDateTime) {
        this.endDateTime = endDateTime;
    }


    /**
     * Gets the estimatedTimeInMS value for this EDeviceSwitchSession.
     *
     * @return estimatedTimeInMS
     */
    public String getEstimatedTimeInMS() {
        return estimatedTimeInMS;
    }


    /**
     * Sets the estimatedTimeInMS value for this EDeviceSwitchSession.
     *
     * @param estimatedTimeInMS
     */
    public void setEstimatedTimeInMS(String estimatedTimeInMS) {
        this.estimatedTimeInMS = estimatedTimeInMS;
    }


    /**
     * Gets the GUIVersion value for this EDeviceSwitchSession.
     *
     * @return GUIVersion
     */
    public String getGUIVersion() {
        return guiversion;
    }


    /**
     * Sets the GUIVersion value for this EDeviceSwitchSession.
     *
     * @param GUIVersion
     */
    public void setGUIVersion(String GUIVersion) {
        this.guiversion = GUIVersion;
    }


    /**
     * Gets the lastUpdatedDttm value for this EDeviceSwitchSession.
     *
     * @return lastUpdatedDttm
     */
    public String getLastUpdatedDttm() {
        return lastUpdatedDttm;
    }


    /**
     * Sets the lastUpdatedDttm value for this EDeviceSwitchSession.
     *
     * @param lastUpdatedDttm
     */
    public void setLastUpdatedDttm(String lastUpdatedDttm) {
        this.lastUpdatedDttm = lastUpdatedDttm;
    }


    /**
     * Gets the reasonCode value for this EDeviceSwitchSession.
     *
     * @return reasonCode
     */
    public String getReasonCode() {
        return reasonCode;
    }


    /**
     * Sets the reasonCode value for this EDeviceSwitchSession.
     *
     * @param reasonCode
     */
    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }


    /**
     * Gets the sessionStage value for this EDeviceSwitchSession.
     *
     * @return sessionStage
     */
    public String getSessionStage() {
        return sessionStage;
    }


    /**
     * Sets the sessionStage value for this EDeviceSwitchSession.
     *
     * @param sessionStage
     */
    public void setSessionStage(String sessionStage) {
        this.sessionStage = sessionStage;
    }


    /**
     * Gets the sessionStatus value for this EDeviceSwitchSession.
     *
     * @return sessionStatus
     */
    public String getSessionStatus() {
        return sessionStatus;
    }


    /**
     * Sets the sessionStatus value for this EDeviceSwitchSession.
     *
     * @param sessionStatus
     */
    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }


    /**
     * Gets the sourceDeviceInfoId value for this EDeviceSwitchSession.
     *
     * @return sourceDeviceInfoId
     */
    public EDeviceInfo getSourceDeviceInfoId() {
        return sourceDeviceInfoId;
    }


    /**
     * Sets the sourceDeviceInfoId value for this EDeviceSwitchSession.
     *
     * @param sourceDeviceInfoId
     */
    public void setSourceDeviceInfoId(EDeviceInfo sourceDeviceInfoId) {
        this.sourceDeviceInfoId = sourceDeviceInfoId;
    }


    /**
     * Gets the startDateTime value for this EDeviceSwitchSession.
     *
     * @return startDateTime
     */
    public String getStartDateTime() {
        return startDateTime;
    }


    /**
     * Sets the startDateTime value for this EDeviceSwitchSession.
     *
     * @param startDateTime
     */
    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }


    /**
     * Gets the stationId value for this EDeviceSwitchSession.
     *
     * @return stationId
     */
    public String getStationId() {
        return stationId;
    }


    /**
     * Sets the stationId value for this EDeviceSwitchSession.
     *
     * @param stationId
     */
    public void setStationId(String stationId) {
        this.stationId = stationId;
    }


    /**
     * Gets the storeId value for this EDeviceSwitchSession.
     *
     * @return storeId
     */
    public String getStoreId() {
        return storeId;
    }


    /**
     * Sets the storeId value for this EDeviceSwitchSession.
     *
     * @param storeId
     */
    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }


    /**
     * Gets the systemException value for this EDeviceSwitchSession.
     *
     * @return systemException
     */
    public String getSystemException() {
        return systemException;
    }


    /**
     * Sets the systemException value for this EDeviceSwitchSession.
     *
     * @param systemException
     */
    public void setSystemException(String systemException) {
        this.systemException = systemException;
    }


    /**
     * Gets the systemInfo value for this EDeviceSwitchSession.
     *
     * @return systemInfo
     */
    public byte[] getSystemInfo() {
        return systemInfo;
    }


    /**
     * Sets the systemInfo value for this EDeviceSwitchSession.
     *
     * @param systemInfo
     */
    public void setSystemInfo(byte[] systemInfo) {
        this.systemInfo = systemInfo;
    }


    /**
     * Gets the timeToGetContentInfoForReview value for this EDeviceSwitchSession.
     *
     * @return timeToGetContentInfoForReview
     */
    public String getTimeToGetContentInfoForReview() {
        return timeToGetContentInfoForReview;
    }


    /**
     * Sets the timeToGetContentInfoForReview value for this EDeviceSwitchSession.
     *
     * @param timeToGetContentInfoForReview
     */
    public void setTimeToGetContentInfoForReview(String timeToGetContentInfoForReview) {
        this.timeToGetContentInfoForReview = timeToGetContentInfoForReview;
    }


    /**
     * Gets the userId value for this EDeviceSwitchSession.
     *
     * @return userId
     */
    public String getUserId() {
        return userId;
    }


    /**
     * Sets the userId value for this EDeviceSwitchSession.
     *
     * @param userId
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getTransferMode() {
        return transferMode;
    }

    public void setTransferMode(String transferMode) {
        this.transferMode = transferMode;
    }

    public String getDeviceSwitchSessionId() {
        return deviceSwitchSessionId;
    }

    public void setDeviceSwitchSessionId(String deviceSwitchSessionId) {
        this.deviceSwitchSessionId = deviceSwitchSessionId;
    }

    public String getTransferType() {
        return transferType;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }
}
