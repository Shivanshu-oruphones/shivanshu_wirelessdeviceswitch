package com.pervacio.wds.custom.models;
import com.google.gson.Gson;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMMigrateStatus;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
public class MigrationStats {
    private static MigrationStats migrationStats = null;
    private int mRole;
    private String remoteDeviceIMEI;
    private String pinForAutoconnect;

    public String getRemoteDevicePlatform() {
        return remoteDevicePlatform;
    }

    public void setRemoteDevicePlatform(String remoteDevicePlatform) {
        this.remoteDevicePlatform = remoteDevicePlatform;
    }

    private String remoteDevicePlatform;
    private int selectedDataTypes;
    private boolean migrationCompleted;
    private Map<String, String> pimBackupFilesList = new HashMap<>();
    private LinkedHashMap<Integer, ContentDetails> contentDetailsMap = new LinkedHashMap<>();
    private String estimationTime;
    private long elapsedTime;
    private int dataTypesTobeCompleted = 0;
    private String eDeviceSwitchSession = null;
    private String cloudPairingSessionID = null;

    public void seteDeviceSwitchSession(String eDeviceSwitchSession){
        this.eDeviceSwitchSession = eDeviceSwitchSession;
    }

    public EDeviceSwitchSession geteDeviceSwitchSession() {
        return (new Gson().fromJson(eDeviceSwitchSession, EDeviceSwitchSession.class));
    }

    public EMMigrateStatus getMigrateStatus() {
        return migrateStatus;
    }

    public void setMigrateStatus(EMMigrateStatus migrateStatus) {
        this.migrateStatus = migrateStatus;
    }

    private EMMigrateStatus migrateStatus;

    public static MigrationStats getInstance() {
        if (migrationStats == null) {
            migrationStats = new MigrationStats();
        }
        return migrationStats;
    }
    public static void setMigrationStats(MigrationStats migrationStats) {
        MigrationStats.migrationStats = migrationStats;
    }
    public String getEstimationTime() {
        return estimationTime;
    }

    public void setEstimationTime(String estimationTime) {
        this.estimationTime = estimationTime;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public int getDataTypesTobeCompleted() {
        return dataTypesTobeCompleted;
    }

    public void setDataTypesTobeCompleted(int dataTypesTobeCompleted) {
        this.dataTypesTobeCompleted = dataTypesTobeCompleted;
    }

    public String getRemoteDeviceIMEI() {
        return remoteDeviceIMEI;
    }

    public void setRemoteDeviceIMEI(String remoteDeviceIMEI) {
        this.remoteDeviceIMEI = remoteDeviceIMEI;
    }

    public LinkedHashMap<Integer, ContentDetails> getContentDetailsMap() {
        return contentDetailsMap;
    }

    public void setContentDetailsMap(LinkedHashMap<Integer, ContentDetails> contentDetailsMap) {
        this.contentDetailsMap = contentDetailsMap;
    }
    public int getmRole() {
        return mRole;
    }

    public void setmRole(int mRole) {
        this.mRole = mRole;
    }

    public int getSelectedDataTypes() {
        return selectedDataTypes;
    }

    public void setSelectedDataTypes(int selectedDataTypes) {
        this.selectedDataTypes = selectedDataTypes;
    }

    public boolean isMigrationCompleted() {
        return migrationCompleted;
    }

    public void setMigrationCompleted(boolean migrationCompleted) {
        this.migrationCompleted = migrationCompleted;
    }

    public String getPimBackupFile(String dataType) {
        return pimBackupFilesList.get(dataType);
    }

    public void addToBackupList(String dataType, String fileName) {
        pimBackupFilesList.put(dataType, fileName);
    }

    public void removeBackupFile(String dataType) {
        pimBackupFilesList.remove(dataType);
    }

    public void setCloudPairingSessionID(String value) {
        DLog.log("setCloudPairingSessionID value :"+value);
        this.cloudPairingSessionID = value;
    }

    public String getCloudPairingSessionID() {
        return cloudPairingSessionID;
    }
}
