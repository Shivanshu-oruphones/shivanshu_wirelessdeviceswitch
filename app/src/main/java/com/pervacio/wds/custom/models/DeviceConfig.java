package com.pervacio.wds.custom.models;


public class DeviceConfig {
    private boolean hasBulkSMSInsertionProblem;
    private boolean enableWifiP2P;
    private boolean hasWifiDirectProblem;

    public boolean isEnableWifiP2P() {
        return enableWifiP2P;
    }

    public void setEnableWifiP2P(boolean enableWifiP2P) {
        this.enableWifiP2P = enableWifiP2P;
    }

    public boolean isHasWifiDirectProblem() {
        return hasWifiDirectProblem;
    }

    public void setHasWifiDirectProblem(boolean hasWifiDirectProblem) {
        this.hasWifiDirectProblem = hasWifiDirectProblem;
    }

    public boolean isHasBulkSMSInsertionProblem() {
        return hasBulkSMSInsertionProblem;
    }

    public void setHasBulkSMSInsertionProblem(boolean hasBulkSMSInsertionProblem) {
        this.hasBulkSMSInsertionProblem = hasBulkSMSInsertionProblem;
    }
}
