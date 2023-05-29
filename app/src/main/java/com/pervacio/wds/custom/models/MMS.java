package com.pervacio.wds.custom.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class MMS implements Serializable {

    @SerializedName("date")
    private long date;
    @SerializedName("address")
    private String address;
    @SerializedName("contentType")
    private String contentType;
    @SerializedName("read")
    private int read;
    @SerializedName("msgBox")
    private int msgBox;
    @SerializedName("parts")
    private List<MMSPart> parts;
    @SerializedName("subject")
    private String subject;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getRead() {
        return read;
    }

    public void setRead(int read) {
        this.read = read;
    }

    public int getMsgBox() {
        return msgBox;
    }

    public void setMsgBox(int msgBox) {
        this.msgBox = msgBox;
    }

    public List<MMSPart> getParts() {
        return parts;
    }

    public void setParts(List<MMSPart> parts) {
        this.parts = parts;
    }
}

