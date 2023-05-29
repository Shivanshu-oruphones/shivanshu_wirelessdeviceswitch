package com.pervacio.wds.custom.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class EmailServiceDto implements Serializable {
    private String emailBody;
    private Map<String, byte[]> attachment;
    private Map<String, String> dispostion;
    private String[] emailTo;
    private String[] emailCC;
    private String[] emailBCC;
    private String subject;
    private String userName;
    private String password;
    private String storeCode;

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmailBody() {
        return emailBody;
    }


    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }


    public Map<String, byte[]> getAttachment() {
        return attachment;
    }


    public void setAttachment(Map<String, byte[]> attachment) {
        this.attachment = attachment;
    }


    public String[] getEmailTo() {
        return emailTo;
    }


    public void setEmailTo(String[] emailTo) {
        this.emailTo = emailTo;
    }


    public String[] getEmailCC() {
        return emailCC;
    }


    public void setEmailCC(String[] emailCC) {
        this.emailCC = emailCC;
    }


    public String[] getEmailBCC() {
        return emailBCC;
    }


    public void setEmailBCC(String[] emailBCC) {
        this.emailBCC = emailBCC;
    }


    public String getSubject() {
        return subject;
    }


    public void setSubject(String subject) {
        this.subject = subject;
    }


    public Map<String, String> getDispostion() {
        return dispostion;
    }


    public void setDispostion(Map<String, String> dispostion) {
        this.dispostion = dispostion;
    }

    public void addDispostion(String key, String value) {
        if (dispostion == null) {
            dispostion = new HashMap<>();
        }
        dispostion.put(key, value);
    }

    public void addAttachement(String key, byte[] value) {
        if (attachment == null) {
            attachment = new HashMap<>();
        }
        attachment.put(key, value);
    }
}
