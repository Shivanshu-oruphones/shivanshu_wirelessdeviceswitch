package com.pervacio.wds.custom.models;

import java.io.Serializable;

/**
 * Created by Pervacio on 2/26/2018.
 */

public class FeedbackSummary implements Serializable{

        private static final long serialVersionUID = 1L;
        private String description;
        private String rating;
        private String productName;
        private String companyName;
        private String buildNumber;
        private String locale;
        private String deviceId;
        private String make;
        private String model;
        private String osVersion;

        public FeedbackSummary() {
        }

        public FeedbackSummary(String description, String rating, String productName, String companyName, String buildNumber, String locale) {
            this.description = description;
            this.rating = rating;
            this.productName = productName;
            this.companyName = companyName;
            this.buildNumber = buildNumber;
            this.locale = locale;
        }

        public String getDescription() {
            return this.description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getRating() {
            return this.rating;
        }

        public void setRating(String rating) {
            this.rating = rating;
        }

        public String getProductName() {
            return this.productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getCompanyName() {
            return this.companyName;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public String getBuildNumber() {
            return this.buildNumber;
        }

        public void setBuildNumber(String buildNumber) {
            this.buildNumber = buildNumber;
        }

        public String getLocale() {
            return this.locale;
        }

        public void setLocale(String locale) {
            this.locale = locale;
        }

        public String getDeviceId() {
            return this.deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getMake() {
            return this.make;
        }

        public void setMake(String make) {
            this.make = make;
        }

        public String getModel() {
            return this.model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getOsVersion() {
            return this.osVersion;
        }

        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }

}
