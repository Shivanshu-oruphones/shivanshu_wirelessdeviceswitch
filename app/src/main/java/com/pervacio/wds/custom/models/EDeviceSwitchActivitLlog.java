package com.pervacio.wds.custom.models;

/**
 * Created by: Darpan Dodiya on 5/24/2017.
 * Contributors:
 * Last updated on: 5/26/2017
 */

public class EDeviceSwitchActivitLlog  implements java.io.Serializable {
    private Long activityLogId;
    private String activityType;
    private String additionalInfo;
    private String destinationDesktopTools;
    private String destinationDeviceAgents;
    private EDeviceSwitchSession deviceSwitchSessionId;
    private long endDateTime;
    private String sourceDesktopTools;
    private String sourceDeviceAgents;
    private long startDateTime;

    public EDeviceSwitchActivitLlog() {
    }

    public EDeviceSwitchActivitLlog(
            Long activityLogId,
            String activityType,
            String additionalInfo,
            String destinationDesktopTools,
            String destinationDeviceAgents,
            EDeviceSwitchSession deviceSwitchSessionId,
            long endDateTime,
            String sourceDesktopTools,
            String sourceDeviceAgents,
            long startDateTime) {
        this.activityLogId = activityLogId;
        this.activityType = activityType;
        this.additionalInfo = additionalInfo;
        this.destinationDesktopTools = destinationDesktopTools;
        this.destinationDeviceAgents = destinationDeviceAgents;
        this.deviceSwitchSessionId = deviceSwitchSessionId;
        this.endDateTime = endDateTime;
        this.sourceDesktopTools = sourceDesktopTools;
        this.sourceDeviceAgents = sourceDeviceAgents;
        this.startDateTime = startDateTime;
    }


    /**
     * Gets the activityLogId value for this EDeviceSwitchActivitLlog.
     *
     * @return activityLogId
     */
    public Long getActivityLogId() {
        return activityLogId;
    }


    /**
     * Sets the activityLogId value for this EDeviceSwitchActivitLlog.
     *
     * @param activityLogId
     */
    public void setActivityLogId(Long activityLogId) {
        this.activityLogId = activityLogId;
    }


    /**
     * Gets the activityType value for this EDeviceSwitchActivitLlog.
     *
     * @return activityType
     */
    public String getActivityType() {
        return activityType;
    }


    /**
     * Sets the activityType value for this EDeviceSwitchActivitLlog.
     *
     * @param activityType
     */
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }


    /**
     * Gets the additionalInfo value for this EDeviceSwitchActivitLlog.
     *
     * @return additionalInfo
     */
    public String getAdditionalInfo() {
        return additionalInfo;
    }


    /**
     * Sets the additionalInfo value for this EDeviceSwitchActivitLlog.
     *
     * @param additionalInfo
     */
    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }


    /**
     * Gets the destinationDesktopTools value for this EDeviceSwitchActivitLlog.
     *
     * @return destinationDesktopTools
     */
    public String getDestinationDesktopTools() {
        return destinationDesktopTools;
    }


    /**
     * Sets the destinationDesktopTools value for this EDeviceSwitchActivitLlog.
     *
     * @param destinationDesktopTools
     */
    public void setDestinationDesktopTools(String destinationDesktopTools) {
        this.destinationDesktopTools = destinationDesktopTools;
    }


    /**
     * Gets the destinationDeviceAgents value for this EDeviceSwitchActivitLlog.
     *
     * @return destinationDeviceAgents
     */
    public String getDestinationDeviceAgents() {
        return destinationDeviceAgents;
    }


    /**
     * Sets the destinationDeviceAgents value for this EDeviceSwitchActivitLlog.
     *
     * @param destinationDeviceAgents
     */
    public void setDestinationDeviceAgents(String destinationDeviceAgents) {
        this.destinationDeviceAgents = destinationDeviceAgents;
    }


    /**
     * Gets the deviceSwitchSessionId value for this EDeviceSwitchActivitLlog.
     *
     * @return deviceSwitchSessionId
     */
    public EDeviceSwitchSession getDeviceSwitchSessionId() {
        return deviceSwitchSessionId;
    }


    /**
     * Sets the deviceSwitchSessionId value for this EDeviceSwitchActivitLlog.
     *
     * @param deviceSwitchSessionId
     */
    public void setDeviceSwitchSessionId(EDeviceSwitchSession deviceSwitchSessionId) {
        this.deviceSwitchSessionId = deviceSwitchSessionId;
    }


    /**
     * Gets the endDateTime value for this EDeviceSwitchActivitLlog.
     *
     * @return endDateTime
     */
    public long getEndDateTime() {
        return endDateTime;
    }


    /**
     * Sets the endDateTime value for this EDeviceSwitchActivitLlog.
     *
     * @param endDateTime
     */
    public void setEndDateTime(long endDateTime) {
        this.endDateTime = endDateTime;
    }


    /**
     * Gets the sourceDesktopTools value for this EDeviceSwitchActivitLlog.
     *
     * @return sourceDesktopTools
     */
    public String getSourceDesktopTools() {
        return sourceDesktopTools;
    }


    /**
     * Sets the sourceDesktopTools value for this EDeviceSwitchActivitLlog.
     *
     * @param sourceDesktopTools
     */
    public void setSourceDesktopTools(String sourceDesktopTools) {
        this.sourceDesktopTools = sourceDesktopTools;
    }


    /**
     * Gets the sourceDeviceAgents value for this EDeviceSwitchActivitLlog.
     *
     * @return sourceDeviceAgents
     */
    public String getSourceDeviceAgents() {
        return sourceDeviceAgents;
    }


    /**
     * Sets the sourceDeviceAgents value for this EDeviceSwitchActivitLlog.
     *
     * @param sourceDeviceAgents
     */
    public void setSourceDeviceAgents(String sourceDeviceAgents) {
        this.sourceDeviceAgents = sourceDeviceAgents;
    }


    /**
     * Gets the startDateTime value for this EDeviceSwitchActivitLlog.
     *
     * @return startDateTime
     */
    public long getStartDateTime() {
        return startDateTime;
    }


    /**
     * Sets the startDateTime value for this EDeviceSwitchActivitLlog.
     *
     * @param startDateTime
     */
    public void setStartDateTime(long startDateTime) {
        this.startDateTime = startDateTime;
    }

}

