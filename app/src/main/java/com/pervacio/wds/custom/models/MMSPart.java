package com.pervacio.wds.custom.models;


import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class MMSPart implements Serializable {
    @SerializedName("ct")
    private String ct;
    @SerializedName("cid")
    private String cid;
    @SerializedName("cl")
    private String cl;
    @SerializedName("name")
    private String name;
    @SerializedName("text")
    private String text;
    @SerializedName("data")
    private String data;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getCl() {
        return cl;
    }

    public void setCl(String cl) {
        this.cl = cl;
    }

    public String getCt() {
        return ct;
    }

    public void setCt(String ct) {
        this.ct = ct;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
