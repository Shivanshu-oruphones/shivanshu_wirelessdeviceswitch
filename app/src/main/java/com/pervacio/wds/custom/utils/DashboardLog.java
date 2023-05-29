package com.pervacio.wds.custom.utils;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMDeviceInfo;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.custom.asynctask.TransactionLogging;
import com.pervacio.wds.custom.models.EDeviceInfo;
import com.pervacio.wds.custom.models.EDeviceSwitchContentTransferDetail;
import com.pervacio.wds.custom.models.EDeviceSwitchSession;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;

import static com.pervacio.wds.custom.utils.Constants.HTTP_AUTHENTICATION_PASSWORD;
import static com.pervacio.wds.custom.utils.Constants.HTTP_AUTHENTICATION_USERNAME;
import static com.pervacio.wds.custom.utils.Constants.LOGGING_API_ENDPOINT;

/**
 * Class to support Dashboard logging functionality.
 *
 * This class follows singleton pattern as there has to be only one instance present throughout the
 * application.
 *
 * Created by: Darpan Dodiya on 5/24/2017.
 * Contributors:
 * Last updated on: 06/08/2017
 */

public class DashboardLog {

    /*** Start and end time of data types ***/
    private long contactStartTime = Constants.UNINITIALIZED;
    private long contactEndTime = Constants.UNINITIALIZED;

    private long calendarStartTime = Constants.UNINITIALIZED;
    private long calendarEndTime = Constants.UNINITIALIZED;

    private long messagesStartTime = Constants.UNINITIALIZED;
    private long messagesEndTime = Constants.UNINITIALIZED;

    private long callLogsStartTime = Constants.UNINITIALIZED;
    private long callLogsEndTime = Constants.UNINITIALIZED;

    private long appsStartTime = Constants.UNINITIALIZED;
    private long appsEndTime = Constants.UNINITIALIZED;

    private long imageStartTime = Constants.UNINITIALIZED;
    private long imageEndTime = Constants.UNINITIALIZED;

    private long videoStartTime = Constants.UNINITIALIZED;
    private long videoEndTime = Constants.UNINITIALIZED;

    private long audioStartTime = Constants.UNINITIALIZED;
    private long audioEndTime = Constants.UNINITIALIZED;

    public long getDocumentsStartTime() {
        return documentsStartTime;
    }

    public void setDocumentsStartTime(long documentsStartTime) {
        this.documentsStartTime = documentsStartTime;
    }

    public long getDocumentsEndTime() {
        return documentsEndTime;
    }

    public void setDocumentsEndTime(long documentsEndTime) {
        this.documentsEndTime = documentsEndTime;
    }

    private long documentsStartTime = Constants.UNINITIALIZED;
    private long documentsEndTime = Constants.UNINITIALIZED;

    public long getSettingsStartTime() {
        return settingsStartTime;
    }

    public void setSettingsStartTime(long settingsStartTime) {
        this.settingsStartTime = settingsStartTime;
    }

    public long getSettingsEndTime() {
        return settingsEndTime;
    }

    public void setSettingsEndTime(long settingsEndTime) {
        this.settingsEndTime = settingsEndTime;
    }

    private long settingsStartTime = Constants.UNINITIALIZED;
    private long settingsEndTime = Constants.UNINITIALIZED;

    /*** THE session object ***/
    private EDeviceSwitchSession eDeviceSwitchSession;

    /*** Device info objects ***/
    public EMDeviceInfo sourceEMDeviceInfo;
    public EMDeviceInfo destinationEMDeviceInfo;

    private boolean isThisDest = false;

    public boolean isUpdateDBdetails() {
        return updateDBdetails;
    }

    public void setUpdateDBdetails(boolean updateDBdetails) {
        this.updateDBdetails = updateDBdetails;
    }

    private boolean updateDBdetails = false;
    private boolean lastServerCallReturned = true;
    private boolean meSentProperJson = false;
    private ArrayList<HttpURLConnection> existingConnections = new ArrayList<>();

    private boolean isTransferFinished = false;

    private static DashboardLog dashboardLog;

    //Private constructor for singleton pattern.
    private DashboardLog() {
        //Block instance creation
    }

    //Call this method to get an instance
    public static DashboardLog getInstance() {
        if(dashboardLog == null) {
            dashboardLog = new DashboardLog();

            //Initialize session object
            EDeviceSwitchSession eDeviceSwitchSession = new EDeviceSwitchSession();
            eDeviceSwitchSession.setStoreId("-1");
            eDeviceSwitchSession.setCompanyId(String.valueOf(EMUtility.getCompanyID()));
            eDeviceSwitchSession.setUserId("-1");
            eDeviceSwitchSession.setStationId("-1");
            eDeviceSwitchSession.setStartDateTime(String.valueOf(System.currentTimeMillis()));
            eDeviceSwitchSession.setEndDateTime(String.valueOf(System.currentTimeMillis()));
            eDeviceSwitchSession.setSessionStage(Constants.SESSION_STAGE.APP_LAUNCHED.value());
            eDeviceSwitchSession.setSessionStatus(Constants.SESSION_STATUS.NOT_STARTED.value());
            eDeviceSwitchSession.setGUIVersion(BuildConfig.VERSION_NAME);

            if(Constants.IS_MMDS) {
                eDeviceSwitchSession.setTransactionType(Constants.TRANSACTION_TYPE.MMDS.value());
                eDeviceSwitchSession.setTransferType("CustomFlow");
            }
            else {
                eDeviceSwitchSession.setTransactionType(Constants.TRANSACTION_TYPE.WDS.value());
            }
            eDeviceSwitchSession.setTransferMode(Constants.mTransferMode);

            dashboardLog.eDeviceSwitchSession = eDeviceSwitchSession;
        }
        return dashboardLog;
    }

    /**
     * This will be called after handshaking. When both devices have info of each other.
     *
     * @param eDeviceInfo
     * @param mDeviceInfo
     */
    public void setDevicesInfo(EMDeviceInfo eDeviceInfo, EMDeviceInfo mDeviceInfo){
        //If either of them are null, don't set it
        if(eDeviceInfo == null || mDeviceInfo == null) {
            return;
        }
        if (eDeviceInfo.dbOperationType == Constants.OPERATION_TYPE.BACKUP.value()) {
            sourceEMDeviceInfo = eDeviceInfo;
            destinationEMDeviceInfo = mDeviceInfo;
        } else {
            sourceEMDeviceInfo = mDeviceInfo;
            destinationEMDeviceInfo = eDeviceInfo;
        }
    }


    /**
     * Call this method to update session data to server.
     *
     * Can be called anytime from anywhere in the code. It will convert current session object to
     * JSON and update it to server.
     *
     * @param forceUpdate true if you want to update to server regardless of status of previous calls.
     *                    false otherwise. Recommended to use true only when needed.
     */
    public synchronized void updateToServer(boolean forceUpdate) {
        DLog.log("In updateToServer");
        //Whenever we update to server, we set the session end time as current time
        eDeviceSwitchSession.setEndDateTime(String.valueOf(System.currentTimeMillis()));

        if((!isThisDest() && !isUpdateDBdetails() ) || !Constants.LOGGING_ENABLED) {
            DLog.log("This device is source or logging is not enabled or server issue. Not calling server for DB update");
            return;
        }

        //Convert to Object to JSON String
        Gson gson = new Gson();
        String gsonString = gson.toJson(eDeviceSwitchSession);
        DLog.log("JSON: " + gsonString);

        //If its force update, kill all existing connections
        if(forceUpdate) {
            killExistingConnections();
            hasPendingTransactions = false;
            lastServerCallReturned = true;
        }

        if (isLastServerCallReturned()) {
            //Set this server call returned to false until we get response
            lastServerCallReturned = false;
            hasPendingTransactions = false;
            TransactionLogging transactionLogging = new TransactionLogging(LOGGING_API_ENDPOINT,
                    HTTP_AUTHENTICATION_USERNAME + ":" + HTTP_AUTHENTICATION_PASSWORD, gsonString, null);
            transactionLogging.startServiceCall();

            existingConnections.add(transactionLogging.getUrlConnection());
        } else {
            DLog.log("Last Server call not completed");
            hasPendingTransactions = true;
        }

        DLog.log("Exited updateToServer");
    }

    public void killExistingConnections() {
        DLog.log("Killing existing connections");

        if(existingConnections !=null && existingConnections.size() > 0) {
            for (HttpURLConnection currentConnection : existingConnections) {
                try {
                    if (currentConnection != null) {
                        currentConnection.disconnect();
                    }
                } catch (Exception ex) {
                    DLog.log(ex);
                }
            }
        }
    }

    private boolean hasPendingTransactions = false;

    public void performPendingTransactions() {
        DLog.log("performPendingTransactions : " + hasPendingTransactions);
        if (hasPendingTransactions) {
            hasPendingTransactions = false;
            updateToServer(false);
        }
    }


    /**
     * Convert EMDeviceInfo to EDeviceInfo
     *
     * EMDeviceInfo :  Class to hold Handshake device info
     * EDeviceInfo : Dashboard logging class to hold device info
     */
    public void convertDevicesInfoToDB() {
        DLog.log("In convertDevicesInfoToDB");

        //If its MMDS, then we'll use device info set by GUI. No need to replace device info in
        //that case.
        if(Constants.IS_MMDS && meSentProperJson) {
            DLog.log("Product is MMDS, returning.");
            return;
        }

        EDeviceInfo sourceDeviceInfoDB = fillDeviceInfoToDB(sourceEMDeviceInfo);
        EDeviceInfo destinationDeviceInfoDB = fillDeviceInfoToDB(destinationEMDeviceInfo);

        eDeviceSwitchSession.setSourceDeviceInfoId(sourceDeviceInfoDB);
        eDeviceSwitchSession.setDestinationDeviceInfoId(destinationDeviceInfoDB);

        DLog.log("Exited convertDevicesInfoToDB");
    }

    /**
     * Worker method that will convert EMDeviceInfo to EDeviceInfo
     * @param emDeviceInfo
     * @return
     */
    private static EDeviceInfo fillDeviceInfoToDB(EMDeviceInfo emDeviceInfo) {
        EDeviceInfo eDeviceInfo = new  EDeviceInfo();

        //If null then do nothing
        if(emDeviceInfo == null) {
            return  eDeviceInfo;
        }
        eDeviceInfo.setMake(emDeviceInfo.dbDeviceMake);
        eDeviceInfo.setModel(emDeviceInfo.dbDeviceModel);
        eDeviceInfo.setOSVersion(emDeviceInfo.dbDeviceOSVersion);
        eDeviceInfo.setPlatform(emDeviceInfo.dbDevicePlatform);
        eDeviceInfo.setOperationType(emDeviceInfo.dbOperationType);
        eDeviceInfo.setImei(emDeviceInfo.dbDeviceIMEI);
        eDeviceInfo.setFirmware(emDeviceInfo.dbDeviceFirmware);
        eDeviceInfo.setBuildNumber(emDeviceInfo.dbDeviceBuildNumber);
        eDeviceInfo.setTotalStorage(emDeviceInfo.dbDeviceTotalStorage);
        eDeviceInfo.setFreeStorage(emDeviceInfo.dbDeviceFreeStorage);
        eDeviceInfo.setStartDateTime(Constants.UNINITIALIZED);
        eDeviceInfo.setEndDateTime(Constants.UNINITIALIZED);
        eDeviceInfo.setSerialNumber(emDeviceInfo.mDeviceUniqueId);

        return eDeviceInfo;
    }

    public void addOrUpdateContentTransferDetail(Constants.DATATYPE datatype,
                                                 int contentCount, long contentSize,
                                                 Constants.TRANSFER_STATUS transferStatus,
                                                 Constants.TRANSFER_STATE transferState,
                                                 boolean updateToServer) {
        addOrUpdateContentTransferDetail(datatype,contentCount,contentSize,transferStatus,
                transferState,updateToServer, false);
    }

    /**
     * As and when the datatype is transferred, call this method to update its info in session object.
     *
     * @param datatype ENUM of content type
     * @param contentCount Count of transferred content
     * @param contentSize Size in bytes. If not available/applicable, set as -1
     * @param transferStatus ENUM of transfer status
     * @param transferState ENUM of transfer state
     * @param updateToServer Whether to call server or not
     */
    public synchronized void addOrUpdateContentTransferDetail(Constants.DATATYPE datatype,
                                                 int contentCount, long contentSize,
                                                 Constants.TRANSFER_STATUS transferStatus,
                                                 Constants.TRANSFER_STATE transferState,
                                                 boolean updateToServer, boolean updateFailedTxn) {

        ArrayList<EDeviceSwitchContentTransferDetail> currentContentListSrc;
        ArrayList<EDeviceSwitchContentTransferDetail> currentContentListDest;


        //If there are some datatypes already added
        try {
        if(eDeviceSwitchSession.getDestinationDeviceInfoId().getEDeviceSwitchContentTransferDetailCollection() != null) {
            currentContentListDest = new ArrayList<>(Arrays.asList(eDeviceSwitchSession.getDestinationDeviceInfoId().
                    getEDeviceSwitchContentTransferDetailCollection()));

            currentContentListSrc = new ArrayList<>(Arrays.asList(eDeviceSwitchSession.getSourceDeviceInfoId().
                    getEDeviceSwitchContentTransferDetailCollection()));

        }
        else {
            currentContentListDest = new ArrayList<>();
            currentContentListSrc = new ArrayList<>();
        }

        //Since database has the size field defined as int, we need to convert the bytes to kb.
        if(contentSize != -1) {
            contentSize = contentSize/1024;
        }

        int index;
        for(index = 0; index < currentContentListDest.size(); index++) {
            //If the type is already present in the array, that means this is an update call
            if(currentContentListDest.get(index).getContentType().equalsIgnoreCase(datatype.value())) {


                currentContentListSrc.get(index).setTransferStatus(transferStatus.value());
                currentContentListDest.get(index).setTransferStatus(transferStatus.value());

                currentContentListSrc.get(index).setTransferState(transferState.value());
                currentContentListDest.get(index).setTransferState(transferState.value());

                //In case of failed transaction, update only destination
                if(updateFailedTxn) {
                    currentContentListDest.get(index).setNumberOfEntries(contentCount);
                    currentContentListDest.get(index).setTotalSizeOfEntries(contentSize);
                }
                //Else update both
                else {
                    currentContentListSrc.get(index).setNumberOfEntries(contentCount);
                    currentContentListDest.get(index).setNumberOfEntries(contentCount);

                    currentContentListSrc.get(index).setTotalSizeOfEntries(contentSize);
                    currentContentListDest.get(index).setTotalSizeOfEntries(contentSize);
                }

                break;
            }
        }

        //Means the type is not present in the array. So create a new type.
        if(index == currentContentListDest.size()) {
            EDeviceSwitchContentTransferDetail contentTransferDetailSrc = new EDeviceSwitchContentTransferDetail();
            contentTransferDetailSrc.setContentType(datatype.value());
            contentTransferDetailSrc.setNumberOfEntries(contentCount);

            contentTransferDetailSrc.setTransferStatus(transferStatus.value());
            contentTransferDetailSrc.setTransferState(transferState.value());

            contentTransferDetailSrc.setTotalSizeOfEntries(contentSize);
            contentTransferDetailSrc.setEstimatedTimeInMS(Constants.NO_TIME);

            EDeviceSwitchContentTransferDetail contentTransferDetailDest = new EDeviceSwitchContentTransferDetail();
            contentTransferDetailDest.setContentType(datatype.value());
            contentTransferDetailDest.setNumberOfEntries(contentCount);

            contentTransferDetailDest.setTransferStatus(transferStatus.value());
            contentTransferDetailDest.setTransferState(transferState.value());

            contentTransferDetailDest.setTotalSizeOfEntries(contentSize);
            contentTransferDetailDest.setEstimatedTimeInMS(Constants.NO_TIME);

            //Add to current list
            currentContentListSrc.add(contentTransferDetailSrc);
            currentContentListDest.add(contentTransferDetailDest);
        }

        //Set start and end time
        if(datatype == Constants.DATATYPE.CONTACT) {
            currentContentListSrc.get(index).setStartDateTime(contactStartTime);
            currentContentListSrc.get(index).setEndDateTime(contactEndTime);

            currentContentListDest.get(index).setStartDateTime(contactStartTime);
            currentContentListDest.get(index).setEndDateTime(contactEndTime);
        }
        else if(datatype == Constants.DATATYPE.CALENDAR) {
            currentContentListSrc.get(index).setStartDateTime(calendarStartTime);
            currentContentListSrc.get(index).setEndDateTime(calendarEndTime);

            currentContentListDest.get(index).setStartDateTime(calendarStartTime);
            currentContentListDest.get(index).setEndDateTime(calendarEndTime);
        } else if(datatype == Constants.DATATYPE.MESSAGE) {
            currentContentListSrc.get(index).setStartDateTime(messagesStartTime);
            currentContentListSrc.get(index).setEndDateTime(messagesEndTime);

            currentContentListDest.get(index).setStartDateTime(messagesStartTime);
            currentContentListDest.get(index).setEndDateTime(messagesEndTime);
        } else if (datatype == Constants.DATATYPE.CALLLOG) {
            currentContentListSrc.get(index).setStartDateTime(callLogsStartTime);
            currentContentListSrc.get(index).setEndDateTime(callLogsEndTime);

            currentContentListDest.get(index).setStartDateTime(callLogsStartTime);
            currentContentListDest.get(index).setEndDateTime(callLogsEndTime);
        } else if (datatype == Constants.DATATYPE.APP) {
            currentContentListSrc.get(index).setStartDateTime(appsStartTime);
            currentContentListSrc.get(index).setEndDateTime(appsEndTime);

            currentContentListDest.get(index).setStartDateTime(appsStartTime);
            currentContentListDest.get(index).setEndDateTime(appsEndTime);
        }
        else if(datatype == Constants.DATATYPE.IMAGE) {
            currentContentListSrc.get(index).setStartDateTime(imageStartTime);
            currentContentListSrc.get(index).setEndDateTime(imageEndTime);

            currentContentListDest.get(index).setStartDateTime(imageStartTime);
            currentContentListDest.get(index).setEndDateTime(imageEndTime);
        }
        else if(datatype == Constants.DATATYPE.VIDEO) {
            currentContentListSrc.get(index).setStartDateTime(videoStartTime);
            currentContentListSrc.get(index).setEndDateTime(videoEndTime);

            currentContentListDest.get(index).setStartDateTime(videoStartTime);
            currentContentListDest.get(index).setEndDateTime(videoEndTime);
        }
        else if(datatype == Constants.DATATYPE.AUDIO) {
            currentContentListSrc.get(index).setStartDateTime(audioStartTime);
            currentContentListSrc.get(index).setEndDateTime(audioEndTime);

            currentContentListDest.get(index).setStartDateTime(audioStartTime);
            currentContentListDest.get(index).setEndDateTime(audioEndTime);
        }
        else if(datatype == Constants.DATATYPE.SETTINGS) {
            currentContentListSrc.get(index).setStartDateTime(settingsStartTime);
            currentContentListSrc.get(index).setEndDateTime(settingsEndTime);

            currentContentListDest.get(index).setStartDateTime(settingsStartTime);
            currentContentListDest.get(index).setEndDateTime(settingsEndTime);
        }
        else if(datatype == Constants.DATATYPE.DOCUMENTS) {
            currentContentListSrc.get(index).setStartDateTime(documentsStartTime);
            currentContentListSrc.get(index).setEndDateTime(documentsEndTime);

            currentContentListDest.get(index).setStartDateTime(documentsStartTime);
            currentContentListDest.get(index).setEndDateTime(documentsEndTime);
        }
        else {
            currentContentListSrc.get(index).setStartDateTime(Constants.NO_TIME);
            currentContentListSrc.get(index).setEndDateTime(Constants.NO_TIME);

            currentContentListDest.get(index).setStartDateTime(Constants.NO_TIME);
            currentContentListDest.get(index).setEndDateTime(Constants.NO_TIME);
        }

        //Set info in both source and destination
        eDeviceSwitchSession.getSourceDeviceInfoId().
                setEDeviceSwitchContentTransferDetailCollection(currentContentListSrc.toArray(
                        new EDeviceSwitchContentTransferDetail[currentContentListSrc.size()]));

        eDeviceSwitchSession.getDestinationDeviceInfoId().
                setEDeviceSwitchContentTransferDetailCollection(currentContentListDest.toArray(
                        new EDeviceSwitchContentTransferDetail[currentContentListDest.size()]));


        if(updateToServer) {
            updateToServer(false);
        }
        } catch (Exception e) {
            DLog.log("Exception in addOrUpdateContentTransferDetail : " + e.getMessage());
        }
    }

    /*
    * Merging only contentdetailsId and deviceinfo id from remote object to local Object.
    * */
    public static synchronized void mergeObjects(EDeviceSwitchSession localSession, EDeviceSwitchSession remoteSession) {
        try {
            if (remoteSession != null) {
                if (!TextUtils.isEmpty(remoteSession.getDeviceSwitchSessionId())) {
                    localSession.setDeviceSwitchSessionId(remoteSession.getDeviceSwitchSessionId());
                }
                if (remoteSession.getEDeviceSwitchSourceContentSummaryCollection() != null) {
                    localSession.setEDeviceSwitchSourceContentSummaryCollection(remoteSession.getEDeviceSwitchSourceContentSummaryCollection());
                }
                if (remoteSession.getSourceDeviceInfoId() != null) {
                    localSession.getSourceDeviceInfoId().setDeviceInfoId(remoteSession.getSourceDeviceInfoId().getDeviceInfoId());
                    if (remoteSession.getSourceDeviceInfoId().getEDeviceSwitchContentTransferDetailCollection() != null) {
                        mergeContentDetails(localSession.getSourceDeviceInfoId().getEDeviceSwitchContentTransferDetailCollection(), remoteSession.getSourceDeviceInfoId().getEDeviceSwitchContentTransferDetailCollection());
                    }
                }
                if (remoteSession.getDestinationDeviceInfoId() != null) {
                    localSession.getDestinationDeviceInfoId().setDeviceInfoId(remoteSession.getDestinationDeviceInfoId().getDeviceInfoId());
                    if (remoteSession.getDestinationDeviceInfoId().getEDeviceSwitchContentTransferDetailCollection() != null) {
                        mergeContentDetails(localSession.getDestinationDeviceInfoId().getEDeviceSwitchContentTransferDetailCollection(), remoteSession.getDestinationDeviceInfoId().getEDeviceSwitchContentTransferDetailCollection());
                    }
                }
            }
        } catch (Exception e) {
            DLog.log("Exception in mergeObjects : "+e);
        }
    }

    private static void mergeContentDetails(EDeviceSwitchContentTransferDetail[] localDetails, EDeviceSwitchContentTransferDetail[] remoteDetails) {
        try {
            for (EDeviceSwitchContentTransferDetail remoteContentTransferDetail : remoteDetails) {
                for (EDeviceSwitchContentTransferDetail localContentTransferDetail : localDetails) {
                    if (localContentTransferDetail.equals(remoteContentTransferDetail)) {
                        localContentTransferDetail.setContentTransferDetailId(remoteContentTransferDetail.getContentTransferDetailId());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            DLog.log("Exception in mergeContentDetails : "+e);
        }
    }



    /**
     * Once the transfer is completed, mark the session as complete.
     *
     * @param session_status
     */
    public void updateSessionStatus(Constants.SESSION_STATUS session_status) {
        eDeviceSwitchSession.setSessionStatus(session_status.value());

        if(session_status == Constants.SESSION_STATUS.SUCCESS) {
            eDeviceSwitchSession.setSessionStage(Constants.SESSION_STAGE.TRANSFER_COMPLETED.value());
        }
    }

    /**
     * In case of MSDS, logging will not be done from apk itself, not from GUI.
     * GUI will send the pre-filled EDeviceSwitchSession object in serialized JSON form,
     * we need to merge it with current session. (Or just replace it)
     *
     * @param json
     */
    public void processSessionJsonFromME(String json) {
        DLog.log("In processSessionJsonFromME. JSON received: " + json);

        try {
            //Sample JSON: "wds_send_session_json:{this is actual json}";
            String actualJson = json.substring(Constants.WDS_SEND_SESSION_JSON.length()+1);
            Gson gson = new Gson();
            EDeviceSwitchSession sessionObjFromME = gson.fromJson(actualJson, EDeviceSwitchSession.class);

            if(sessionObjFromME != null && sessionObjFromME.getSourceDeviceInfoId() != null
                    && sessionObjFromME.getDestinationDeviceInfoId() != null) {
                DLog.log("JSON deserialization successful");
                //Everything is proper, replace current session obj with remote obj
                eDeviceSwitchSession = sessionObjFromME;
                meSentProperJson = true;
            }
            DashboardLog.getInstance().updateToServer(true);
        }
        catch (Exception ex) {
            DLog.log("Exception while parsing JSON sent by ME");
        }
    }

    public void setAppVersion() {
        StringBuilder guiVersion = new StringBuilder();
        guiVersion.append("src:"+ sourceEMDeviceInfo.appVersion);
        guiVersion.append(",");
        guiVersion.append("dest:"+ destinationEMDeviceInfo.appVersion);
        eDeviceSwitchSession.setGUIVersion(guiVersion.toString());
    }

    public void addAdditionalInfo(String info) {
        StringBuilder stringBuilder = new StringBuilder(eDeviceSwitchSession.getAdditionalInfo());
//        stringBuilder.append("\n").append(info);  // old one satya modified
        stringBuilder.append("|").append(info);
        eDeviceSwitchSession.setAdditionalInfo(stringBuilder.toString());
    }

    /** Getters and setters start **/
    public long getContactStartTime() {
        return contactStartTime;
    }

    public void setContactStartTime(long contactStartTime) {
        this.contactStartTime = contactStartTime;
    }
    public void setCallLogsStartTime(long callLogsStartTime) {
        this.callLogsStartTime = callLogsStartTime;
    }
    public void setCallLogsEndTime(long callLogsEndTime) {
        this.callLogsEndTime = callLogsEndTime;
    }

    public void setAppsStartTime(long appsStartTime) {
        this.appsStartTime = appsStartTime;
    }
    public void setAppsEndTime(long appsEndTime) {
        this.appsEndTime = appsEndTime;
    }
    public long getAppsEndTime() {
        return appsEndTime;
    }
    public long getAppsStartTime() {
        return appsStartTime;
    }
    public long getContactEndTime() {
        return contactEndTime;
    }

    public void setContactEndTime(long contactEndTime) {
        this.contactEndTime = contactEndTime;
    }

    public long getCalendarStartTime() {
        return calendarStartTime;
    }

    public void setCalendarStartTime(long calendarStartTime) {
        this.calendarStartTime = calendarStartTime;
    }

    public long getCalendarEndTime() {
        return calendarEndTime;
    }

    public void setCalendarEndTime(long calendarEndTime) {
        this.calendarEndTime = calendarEndTime;
    }

    public void setMessagesEndTime(long messagesEndTime) {
        this.messagesEndTime = messagesEndTime;
    }

    public void setMessagesStartTime(long messagesStartTime) {
        this.messagesStartTime = messagesStartTime;
    }

    public long getMessagesStartTime() {
        return messagesStartTime;
    }

    public long getMessagesEndTime() {
        return messagesEndTime;
    }

    public long getImageStartTime() {
        return imageStartTime;
    }

    public void setImageStartTime(long imageStartTime) {
        this.imageStartTime = imageStartTime;
    }

    public long getImageEndTime() {
        return imageEndTime;
    }

    public void setImageEndTime(long imageEndTime) {
        this.imageEndTime = imageEndTime;
    }

    public long getVideoStartTime() {
        return videoStartTime;
    }

    public void setVideoStartTime(long videoStartTime) {
        this.videoStartTime = videoStartTime;
    }

    public long getVideoEndTime() {
        return videoEndTime;
    }

    public void setVideoEndTime(long videoEndTime) {
        this.videoEndTime = videoEndTime;
    }

    public long getAudioStartTime() {
        return audioStartTime;
    }

    public void setAudioStartTime(long audioStartTime) {
        this.audioStartTime = audioStartTime;
    }

    public long getAudioEndTime() {
        return audioEndTime;
    }

    public void setAudioEndTime(long audioEndTime) {
        this.audioEndTime = audioEndTime;
    }

    public EDeviceSwitchSession geteDeviceSwitchSession() {
        return eDeviceSwitchSession;
    }

    public void seteDeviceSwitchSession(EDeviceSwitchSession eDeviceSwitchSession) {
        this.eDeviceSwitchSession = eDeviceSwitchSession;
    }

    public boolean isThisDest() {
        return isThisDest;
    }

    public void setThisDest(boolean thisDest) {
        isThisDest = thisDest;
    }

    public boolean isLastServerCallReturned() {
        return lastServerCallReturned;
    }

    public void setLastServerCallReturned(boolean lastServerCallReturned) {
        this.lastServerCallReturned = lastServerCallReturned;
    }


    public boolean isTransferFinished() {
        return isTransferFinished;
    }

    public void setTransferFinished(boolean transferStarted) {
        isTransferFinished = transferStarted;
    }
    /** Getters and setters end **/
}
