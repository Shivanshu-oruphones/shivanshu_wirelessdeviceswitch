/**
 * This is class is used to generate Settings backup file.
 *
 * Created by Surya Polasanapalli on 30-Nov-17.
 */


package com.pervacio.wds.app;

import android.provider.Settings;
import android.util.Base64;

import com.pervacio.wds.custom.utils.EMSettingsMigrateUtility;

import java.io.IOException;
import java.util.HashMap;

import static com.pervacio.wds.app.EMStringConsts.EM_XML_NAME;
import static com.pervacio.wds.app.EMStringConsts.EM_XML_SETTINGS;
import static com.pervacio.wds.app.EMStringConsts.EM_XML_VALUE;
import static com.pervacio.wds.app.EMStringConsts.SETTINGS_BRIGHTNESS;
import static com.pervacio.wds.app.EMStringConsts.SETTINGS_RINGMODE;
import static com.pervacio.wds.app.EMStringConsts.SETTINGS_SCREEN_TIMEOUT;
import static com.pervacio.wds.app.EMStringConsts.SETTINGS_WALLPAPER;

public class EMGenerateSettingsBackupTask extends EMGenerateDataInThread {

    public String filePath = null;
    EMXmlGenerator xmlGenerator = null;
    private EMSettingsMigrateUtility emSettingsMigrateUtility = new EMSettingsMigrateUtility();

    private int mNumOfEntries=0;
    private int mTotalEntries=EMStringConsts.SETTINGS_LIST.size();

    @Override
    protected void runTask() {
        DLog.log(">> EMGenerateSettingsBackupTask::run()");
        HashMap<String,String> settings= new HashMap<>();
        try {
            /*try {
                settings = emSettingsMigrateUtility.collectSystemSettings();
            } catch (Exception e) {
                e.printStackTrace();
            }*/
            //mTotalEntries+=settings.size();
            xmlGenerator = new EMXmlGenerator();
            xmlGenerator.startDocument();
            writeElement(SETTINGS_SCREEN_TIMEOUT,String.valueOf(emSettingsMigrateUtility.getScreenTimeout()));
            writeElement(SETTINGS_BRIGHTNESS, emSettingsMigrateUtility.getScreenBrightnessAutoMode()+","+emSettingsMigrateUtility.getScreenBrightnessValue());
            writeElement(SETTINGS_RINGMODE,emSettingsMigrateUtility.getRingerMode()+","+emSettingsMigrateUtility.getRingVolume()+","+emSettingsMigrateUtility.getSettingValue(Settings.System.VIBRATE_WHEN_RINGING));
            writeElement(SETTINGS_WALLPAPER, Base64.encodeToString(emSettingsMigrateUtility.getWallpaper(), Base64.DEFAULT));
            /*if (settings != null) {
                for (String key : settings.keySet()) {
                    writeElement(key.toString(), settings.get(key));
                }
            }*/
        } catch (Exception ex) {
            DLog.log("Exception while backing up settings : " + ex);
        } finally {
            try {
                xmlGenerator.endElement(EMStringConsts.EM_XML_ROOT);
            } catch (Exception ex) {
                // nothing we can do here
            }
            try {
                setFilePath(xmlGenerator.endDocument());
            } catch (IOException e) {
                // Nothing we can do here
            }
        }
        sendProgress();
        DLog.log("Processing Settings Done >> ");

    }

    private void writeElement(String name, String value) {
        try {
            //DLog.log(name + " " + value);
            ++mNumOfEntries;
            xmlGenerator.startElement(EM_XML_SETTINGS);
            xmlGenerator.startElement(EM_XML_NAME);
            xmlGenerator.writeText(name);
            xmlGenerator.endElement(EM_XML_NAME);
            xmlGenerator.startElement(EM_XML_VALUE);
            xmlGenerator.writeText(value);
            xmlGenerator.endElement(EM_XML_VALUE);
            xmlGenerator.endElement(EM_XML_SETTINGS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        EMMigrateStatus.addItemTransferred(EMDataType.EM_DATA_TYPE_SETTINGS);
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
