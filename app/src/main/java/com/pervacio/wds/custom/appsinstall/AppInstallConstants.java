package com.pervacio.wds.custom.appsinstall;

import java.util.HashMap;
import java.util.Map;

public class AppInstallConstants {
    public static AppInstallationsCategory.AppsStatus[] categoryList = {AppInstallationsCategory.AppsStatus.PENDING,AppInstallationsCategory.AppsStatus.FAILED,AppInstallationsCategory.AppsStatus.INSTALLED};
    public static Map<AppInstallationsCategory.AppsStatus, String> appsStatusStringMap = new HashMap<>();
    public static Map<AppInstallationsCategory.AppsStatus, String> appsSuggestionStringMap = new HashMap<>();
    public static Map<AppInstallationsCategory.AppsStatus, String> appsSuggestionStringMapEmpty = new HashMap<>();
}
