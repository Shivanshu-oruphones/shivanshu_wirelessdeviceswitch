package com.pervacio.wds.custom.utils;

import com.pervacio.wds.custom.appmigration.AppInfoModel;

import java.util.List;

public class AppsDetails {


    String title;
//    List<String> childList;
    public List<AppInfoModel> childList;

    boolean showExpandButton;
    String type;

    public AppsDetails(String title, List<AppInfoModel> childList, boolean showExpandButton, String type) {
        this.title = title;
        this.childList = childList;
        this.showExpandButton = showExpandButton;
        this.type = type;
    }

}
