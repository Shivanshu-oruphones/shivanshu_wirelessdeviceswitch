package com.pervacio.wds.custom.models;

/**
 * In the transaction logging, to store info of content selection on source device, this class is used.
 *
 * We'll get the content details when next button is clicked on source device. It will be sent in
 * JSON format along with MIGRATION_STARTED command. e.g. MIGRATION_STARTED#{"json":"here"}
 *
 * @author Darpan Dodiya <darpan.dodiya@pervacio.com>
 */

public class EDeviceSwitchSourceContentSummary implements java.io.Serializable {
    /*** Required Fields ***/
    private String contentType;
    private EDeviceSwitchSession deviceSwitchSessionId;
    private Integer numberOfEntries;
    private String selected;
    private Long sourceContentSummaryId;
    private Long totalSizeOfEntries;

    public EDeviceSwitchSourceContentSummary() {
    }

    public EDeviceSwitchSourceContentSummary(
            String contentType,
            EDeviceSwitchSession deviceSwitchSessionId,
            Integer numberOfEntries,
            String selected,
            Long sourceContentSummaryId,
            Long totalSizeOfEntries) {
        this.contentType = contentType;
        this.deviceSwitchSessionId = deviceSwitchSessionId;
        this.numberOfEntries = numberOfEntries;
        this.selected = selected;
        this.sourceContentSummaryId = sourceContentSummaryId;
        this.totalSizeOfEntries = totalSizeOfEntries;
    }


    /**
     * Gets the contentType value for this EDeviceSwitchSourceContentSummary.
     *
     * @return contentType
     */
    public String getContentType() {
        return contentType;
    }


    /**
     * Sets the contentType value for this EDeviceSwitchSourceContentSummary.
     *
     * @param contentType
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }


    /**
     * Gets the deviceSwitchSessionId value for this EDeviceSwitchSourceContentSummary.
     *
     * @return deviceSwitchSessionId
     */
    public EDeviceSwitchSession getDeviceSwitchSessionId() {
        return deviceSwitchSessionId;
    }


    /**
     * Sets the deviceSwitchSessionId value for this EDeviceSwitchSourceContentSummary.
     *
     * @param deviceSwitchSessionId
     */
    public void setDeviceSwitchSessionId(EDeviceSwitchSession deviceSwitchSessionId) {
        this.deviceSwitchSessionId = deviceSwitchSessionId;
    }


    /**
     * Gets the numberOfEntries value for this EDeviceSwitchSourceContentSummary.
     *
     * @return numberOfEntries
     */
    public Integer getNumberOfEntries() {
        return numberOfEntries;
    }


    /**
     * Sets the numberOfEntries value for this EDeviceSwitchSourceContentSummary.
     *
     * @param numberOfEntries
     */
    public void setNumberOfEntries(Integer numberOfEntries) {
        this.numberOfEntries = numberOfEntries;
    }


    /**
     * Gets the selected value for this EDeviceSwitchSourceContentSummary.
     *
     * @return selected
     */
    public String getSelected() {
        return selected;
    }


    /**
     * Sets the selected value for this EDeviceSwitchSourceContentSummary.
     *
     * @param selected
     */
    public void setSelected(String selected) {
        this.selected = selected;
    }


    /**
     * Gets the sourceContentSummaryId value for this EDeviceSwitchSourceContentSummary.
     *
     * @return sourceContentSummaryId
     */
    public Long getSourceContentSummaryId() {
        return sourceContentSummaryId;
    }


    /**
     * Sets the sourceContentSummaryId value for this EDeviceSwitchSourceContentSummary.
     *
     * @param sourceContentSummaryId
     */
    public void setSourceContentSummaryId(Long sourceContentSummaryId) {
        this.sourceContentSummaryId = sourceContentSummaryId;
    }


    /**
     * Gets the totalSizeOfEntries value for this EDeviceSwitchSourceContentSummary.
     *
     * @return totalSizeOfEntries
     */
    public Long getTotalSizeOfEntries() {
        return totalSizeOfEntries;
    }


    /**
     * Sets the totalSizeOfEntries value for this EDeviceSwitchSourceContentSummary.
     *
     * @param totalSizeOfEntries
     */
    public void setTotalSizeOfEntries(Long totalSizeOfEntries) {
        this.totalSizeOfEntries = totalSizeOfEntries;
    }

}

