package com.pervacio.wds.custom.models;


public class FeatureConfig {
    private DeviceConfig deviceConfig = new DeviceConfig();
    private ProductConfig productConfig = new ProductConfig();
    private static FeatureConfig featureConfig = null;

    public static FeatureConfig getInstance() {
        if (featureConfig == null) {
            featureConfig = new FeatureConfig();
        }
        return featureConfig;
    }

    public static void setInstance(FeatureConfig config) {
        featureConfig = config;
    }

    public DeviceConfig getDeviceConfig() {
        return deviceConfig;
    }

    public void setDeviceConfig(DeviceConfig deviceConfig) {
        this.deviceConfig = deviceConfig;
    }

    public ProductConfig getProductConfig() {
        return productConfig;
    }

    public void setProductConfig(ProductConfig productConfig) {
        this.productConfig = productConfig;
    }
}
