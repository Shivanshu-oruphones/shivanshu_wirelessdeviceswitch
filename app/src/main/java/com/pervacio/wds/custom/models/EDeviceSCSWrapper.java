package com.pervacio.wds.custom.models;

/**
 * Wrapper class to facilitate smooth conversion to & from JSON of
 * EDeviceSwitchSourceContentSummary class. Simple POJO class, no logic here.
 *
 * @author Darpan Dodiya <darpan.dodiya@pervacio.com>
 */

public class EDeviceSCSWrapper {
    private EDeviceSwitchSourceContentSummary[] EDeviceSwitchSourceContentSummaryCollection;

    public EDeviceSwitchSourceContentSummary[] getEDeviceSwitchSourceContentSummaryCollection() {
        return EDeviceSwitchSourceContentSummaryCollection;
    }

    public void setEDeviceSwitchSourceContentSummaryCollection(EDeviceSwitchSourceContentSummary[] EDeviceSwitchSourceContentSummaryCollection) {
        this.EDeviceSwitchSourceContentSummaryCollection = EDeviceSwitchSourceContentSummaryCollection;
    }

    public String getEstimationTime() {
        return estimationTime;
    }

    public void setEstimationTime(String estimationTime) {
        this.estimationTime = estimationTime;
    }

    private String estimationTime;

    private String additionalInfo;

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}
