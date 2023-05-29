/**
 * This is class is used to restore the settings.
 *
 * Created by Surya Polasanapalli on 30-Nov-17.
 */


package com.pervacio.wds.app;

import android.provider.Settings;
import android.util.Base64;

import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.EMSettingsMigrateUtility;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import static com.pervacio.wds.app.EMStringConsts.EM_XML_NAME;
import static com.pervacio.wds.app.EMStringConsts.EM_XML_SETTINGS;
import static com.pervacio.wds.app.EMStringConsts.EM_XML_VALUE;
import static com.pervacio.wds.app.EMStringConsts.SETTINGS_BRIGHTNESS;
import static com.pervacio.wds.app.EMStringConsts.SETTINGS_RINGMODE;
import static com.pervacio.wds.app.EMStringConsts.SETTINGS_SCREEN_TIMEOUT;
import static com.pervacio.wds.app.EMStringConsts.SETTINGS_WALLPAPER;

public class EMParseSettingsXmlInThread extends EMParseDataInThread {
    private int mTotalEntries = 0;

    private EMSettingsMigrateUtility emSettingsMigrateUtility = new EMSettingsMigrateUtility();
    private int mNumOfEntries = 0;

    @Override
    protected void runTask() {
        DashboardLog.getInstance().setSettingsStartTime(CommonUtil.getInstance().getBackupStartedTime());
        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.SETTINGS, -1, -1, Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, false);
        try {
            mTotalEntries = parseXml(true);
            parseXml(false);
        } catch (Exception ex) {
            DLog.log("exception in runtask of EMParseSettingXmlInThread " + ex);
        }
        DashboardLog.getInstance().setSettingsEndTime(System.currentTimeMillis());
        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.SETTINGS, mTotalEntries, -1, Constants.TRANSFER_STATUS.SUCCESS, Constants.TRANSFER_STATE.COMPLETED, true);
    }

    private int parseXml(boolean aCountOnly) throws IOException, XmlPullParserException {
        int entryNumber = 0;
        long lastUpdateTime = 0;
        long uiUpdateIntervalInMs = 3 * 1000;
        EMXmlPullParser pullParser = new EMXmlPullParser();
        pullParser.setFilePath(mFilePath);

        EMXmlPullParser.EMXmlNodeType nodeType = pullParser.readNode();

        while (nodeType != EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ROOT_ELEMENT) // While there is no error and we haven't reached the last node
        {

            if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_START_ELEMENT) {
                String elementName = pullParser.name();

                if (elementName.equals(EM_XML_SETTINGS)) {
                    ++entryNumber;
                    if ((System.currentTimeMillis() - lastUpdateTime) > uiUpdateIntervalInMs && !aCountOnly) {
                        lastUpdateTime = System.currentTimeMillis();
                        EMProgressInfo progress = new EMProgressInfo();
                        progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_OUTGOING_DATA;
                        progress.mDataType = EMDataType.EM_DATA_TYPE_SETTINGS;
                        progress.mTotalItems = mTotalEntries;
                        progress.mCurrentItemNumber = entryNumber;
                        updateProgressFromWorkerThread(progress);
                    }
                    boolean endOfCallLogEntry = false;
                    String dataTypeName = "";
                    int settingsXmlLevel = 0;
                    String settingName = "";
                    String settingValue = "";


                    while (!endOfCallLogEntry) {
                        nodeType = pullParser.readNode();
                        if (settingsXmlLevel == 0) {
                            if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ELEMENT) {
                                endOfCallLogEntry = true;
                                if (!aCountOnly) {
                                    saveSetting(settingName, settingValue);
                                }
                            } else if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_START_ELEMENT) {
                                // We have found a data element, so save the type
                                dataTypeName = pullParser.name();
                                settingsXmlLevel += 1;
                            }
                        }
                        if (settingsXmlLevel == 1) {
                            if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_END_ELEMENT) {
                                dataTypeName = "";
                                settingsXmlLevel -= 1;
                            } else if (nodeType == EMXmlPullParser.EMXmlNodeType.EM_NODE_TYPE_TEXT) {
                                String value = pullParser.value();
                                if (dataTypeName.equals(EM_XML_NAME)) {
                                    settingName = value;
                                } else if (dataTypeName.equals(EM_XML_VALUE)) {
                                    settingValue = value;
                                }
                            }
                        }
                    }
                }
            }

            nodeType = pullParser.readNode();
        }
        if (!aCountOnly) {
            sendProgress();
            DLog.log("Processing Settings Done >> ");
        }
        return entryNumber;
    }


    private void saveSetting(String name, String value) {
        boolean settingSaved = false;
        try {
            if (SETTINGS_WALLPAPER.equalsIgnoreCase(name)) {
                settingSaved = emSettingsMigrateUtility.setWallPaper(Base64.decode(value, Base64.DEFAULT));
            } else if (SETTINGS_SCREEN_TIMEOUT.equalsIgnoreCase(name)) {
                settingSaved = emSettingsMigrateUtility.setScreenTimeOut(Integer.parseInt(value));
            } else if (SETTINGS_BRIGHTNESS.equalsIgnoreCase(name)) {
                String values[] = value.split(",");
                int brightnessMode = Integer.parseInt(values[0]);
                float brightnessValue = Float.parseFloat(values[1]);
                emSettingsMigrateUtility.setScreenBrightnessAutoMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);             // make auto brightness disable and change brightness value and apply setting.
                emSettingsMigrateUtility.setScreenBrightness(brightnessValue);
                emSettingsMigrateUtility.setScreenBrightnessAutoMode(brightnessMode);
                settingSaved = true;
            } else if (SETTINGS_RINGMODE.equalsIgnoreCase(name)) {
                String values[] = value.split(",");
                int ringMode = Integer.parseInt(values[0]);
                int ringLevel = Integer.parseInt(values[1]);
                int vibrateWhenRinging= Integer.parseInt(values[2]);
                settingSaved = emSettingsMigrateUtility.setRingerMode(ringMode, ringLevel);
                emSettingsMigrateUtility.putSettingValue(Settings.System.VIBRATE_WHEN_RINGING,vibrateWhenRinging);
            } else {
                try {
                    DLog.log(name + " - " + value);
                    settingSaved = emSettingsMigrateUtility.putSettingValue(name, Integer.parseInt(value));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ++mNumOfEntries;
        if (settingSaved) {
            EMMigrateStatus.addItemTransferred(EMDataType.EM_DATA_TYPE_SETTINGS);
        } else {
            DLog.log("setting failed : " + name + " - " + value);
            EMMigrateStatus.addItemNotTransferred(EMDataType.EM_DATA_TYPE_SETTINGS);
        }
        sendProgress();
    }

    private void sendProgress(){
        EMProgressInfo progress = new EMProgressInfo();
        progress.mOperationType = EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_INCOMING_DATA;
        progress.mDataType = EMDataType.EM_DATA_TYPE_SETTINGS;
        progress.mTotalItems = mTotalEntries;
        progress.mCurrentItemNumber = mNumOfEntries;
        updateProgressFromWorkerThread(progress);
    }


}
