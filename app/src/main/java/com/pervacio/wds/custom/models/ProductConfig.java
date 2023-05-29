package com.pervacio.wds.custom.models;

public class ProductConfig {
    private boolean geoFencingEnabled = false;
    private boolean storeIdValidationEnabled = false;
    private boolean repIdValidationEnabled = false;
    private boolean repIdLogin = false;
    private boolean estimationtimeRequired = true;
    private boolean cloudpairingRequired = false;
    private String supportedLanguages = "en";
    private String supportedDatatypes;
    private String batteryWarningLevel;
    private boolean certificationCheck = false;
    private boolean transactionLoggingEnabled;
    private boolean pauseAndResumeEnabled = false;
    private boolean autoPairingEnabled = true;
    private boolean reportProblemEnabled = false;
    private String defaultLanguage = "en";
    private boolean userIdValidationEnabled = false;
    private String companyName;
    private int companyId;
    private String rapEmail = "support@pervacio.com";
    private String rapSubject = "Issue Reported by";
    private boolean uninstallRequired = false;
    private boolean transferTypeSelectionEnabled = false;
    private boolean showPermissionsInstructions = false;
    private boolean supportSDCardTransfer;
    private boolean supportUSBTransfer;
    private boolean excludeSDCardMediaEnabled = false;
    private boolean excludeWhatsAppMediaEnabled = false;
    private boolean dataWipeEnabled = false;
    private boolean accAndPinRemovalEnabled = false;
    private boolean customerSatisfactionEnabled = false;
    private boolean enableAppTransfer = false;
    private String preloadedApps;
    private String preloadedAppNamesS3;

    public boolean isCustomerSatisfactionEnabled() {
        return customerSatisfactionEnabled;
    }

    public void setCustomerSatisfactionEnabled(boolean customerSatisfactionEnabled) {
        this.customerSatisfactionEnabled = customerSatisfactionEnabled;
    }

    public boolean isDataWipeEnabled() {
        return dataWipeEnabled;
    }

    public void setDataWipeEnabled(boolean dataWipeEnabled) {
        this.dataWipeEnabled = dataWipeEnabled;
    }

    public boolean isAccAndPinRemovalEnabled() {
        return accAndPinRemovalEnabled;
    }

    public void setAccAndPinRemovalEnabled(boolean accAndPinRemovalEnabled) {
        this.accAndPinRemovalEnabled = accAndPinRemovalEnabled;
    }

    public boolean isExcludeSDCardMediaEnabled() {
        return excludeSDCardMediaEnabled;
    }

    public void setExcludeSDCardMediaEnabled(boolean excludeSDCardMediaEnabled) {
        this.excludeSDCardMediaEnabled = excludeSDCardMediaEnabled;
    }

    public boolean isExcludeWhatsAppMediaEnabled() {
        return excludeWhatsAppMediaEnabled;
    }

    public void setExcludeWhatsAppMediaEnabled(boolean excludeWhatsAppMediaEnabled) {
        this.excludeWhatsAppMediaEnabled = excludeWhatsAppMediaEnabled;
    }

    public boolean isShowPermissionsInstructions() {
        return showPermissionsInstructions;
    }

    public void setShowPermissionsInstructions(boolean showPermissionsInstructions) {
        this.showPermissionsInstructions = showPermissionsInstructions;
    }

    public boolean isRepIdLogin() {
        return repIdLogin;
    }

    public void setRepIdLogin(boolean repIdLogin) {
        this.repIdLogin = repIdLogin;
    }

    public boolean isTransferTypeSelectionEnabled() {
        return transferTypeSelectionEnabled;
    }

    public void setTransferTypeSelectionEnabled(boolean transferTypeSelectionEnabled) {
        this.transferTypeSelectionEnabled = transferTypeSelectionEnabled;
    }

    public boolean isUninstallRequired() {
        return uninstallRequired;
    }

    public void setUninstallRequired(boolean uninstallRequired) {
        this.uninstallRequired = uninstallRequired;
    }

    public boolean isAutoPairingEnabled() {
        return autoPairingEnabled;
    }

    public void setAutoPairingEnabled(boolean autoPairingEnabled) {
        this.autoPairingEnabled = autoPairingEnabled;
    }

    public boolean isSupportSDCardTransfer() {
        return supportSDCardTransfer;
    }

    public void setSupportSDCardTransfer(boolean supportSDCardTransfer) {
        this.supportSDCardTransfer = supportSDCardTransfer;
    }

    public boolean isSupportUSBTransfer() {
        return supportUSBTransfer;
    }

    public void setSupportUSBTransfer(boolean supportUSBTransfer) {
        this.supportUSBTransfer = supportUSBTransfer;
    }

    public String getRapEmail() {
        return rapEmail;
    }

    public void setRapEmail(String rapEmail) {
        this.rapEmail = rapEmail;
    }

    public String getRapSubject() {
        return rapSubject;
    }

    public void setRapSubject(String rapSubject) {
        this.rapSubject = rapSubject;
    }

    public boolean isUserIdValidationEnabled() {
        return userIdValidationEnabled;
    }

    public void setUserIdValidationEnabled(boolean userIdValidationEnabled) {
        this.userIdValidationEnabled = userIdValidationEnabled;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public boolean isReportProblemEnabled() {
        return reportProblemEnabled;
    }

    public void setReportProblemEnabled(boolean reportProblemEnabled) {
        this.reportProblemEnabled = reportProblemEnabled;
    }

    public boolean isEnablePauseAndResume() {
        return pauseAndResumeEnabled;
    }

    public void setEnablePauseAndResume(boolean enablePauseAndResume) {
        this.pauseAndResumeEnabled = enablePauseAndResume;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public boolean isTransactionLoggingEnabled() {
        return transactionLoggingEnabled;
    }

    public void setTransactionLoggingEnabled(boolean transactionLoggingEnabled) {
        this.transactionLoggingEnabled = transactionLoggingEnabled;
    }

    public boolean isGeoFencingEnabled() {
        return geoFencingEnabled;
    }

    public void setGeoFencingEnabled(boolean geoFencingEnabled) {
        this.geoFencingEnabled = geoFencingEnabled;
    }

    public boolean isStoreIdValidationEnabled() {
        return storeIdValidationEnabled;
    }

    public void setStoreIdValidationEnabled(boolean storeIdValidationEnabled) {
        this.storeIdValidationEnabled = storeIdValidationEnabled;
    }

    public boolean isRepIdValidationEnabled() {
        return repIdValidationEnabled;
    }

    public void setRepIdValidationEnabled(boolean repIdValidationEnabled) {
        this.repIdValidationEnabled = repIdValidationEnabled;
    }

    public boolean isEstimationtimeRequired() {
        return estimationtimeRequired;
    }

    public void setEstimationtimeRequired(boolean estimationtimeRequired) {
        this.estimationtimeRequired = estimationtimeRequired;
    }

    public boolean isCloudpairingRequired() {
        return cloudpairingRequired;
    }

    public void setCloudpairingRequired(boolean cloudpairingRequired) {
        this.cloudpairingRequired = cloudpairingRequired;
    }

    public String getSupportedLanguages() {
        return supportedLanguages;
    }

    public void setSupportedLanguages(String supportedLanguages) {
        this.supportedLanguages = supportedLanguages;
    }

    public String getBatteryWarningLevel() {
        return batteryWarningLevel;
    }

    public void setBatteryWarningLevel(String batteryWarningLevel) {
        this.batteryWarningLevel = batteryWarningLevel;
    }

    public boolean isCertificationCheck() {
        return certificationCheck;
    }

    public void setCertificationCheck(boolean certificationCheck) {
        this.certificationCheck = certificationCheck;
    }

    public String getSupportedDatatypes() {
        return supportedDatatypes;
    }

    public void setSupportedDatatypes(String supportedDatatypes) {
        this.supportedDatatypes = supportedDatatypes;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public boolean isEnableAppTransfer() {
        return enableAppTransfer;
    }

    public void setEnableAppTransfer(boolean enableAppTransfer) {
        this.enableAppTransfer = enableAppTransfer;
    }
    public String getPreloadedApps() {
        return preloadedApps;
    }

    public void setPreloadedApps(String preloadedApps) {
        this.preloadedApps = preloadedApps;
    }

    public String getPreloadedAppNamesS3() {
        return preloadedAppNamesS3;
    }

    public void setPreloadedAppNamesS3(String preloadedAppNamesS3) {
        this.preloadedAppNamesS3 = preloadedAppNamesS3;
    }

}
