package com.pervacio.wds.custom.tests;

import com.google.gson.Gson;
import com.pervacio.wds.custom.models.EDeviceInfo;
import com.pervacio.wds.custom.models.EDeviceSwitchContentTransferDetail;
import com.pervacio.wds.custom.models.EDeviceSwitchSession;
import com.pervacio.wds.custom.models.EDeviceSwitchSourceContentSummary;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;


/**
 * Class to test Dashboard logging REST APIs.
 *
 * Created by: Darpan Dodiya on 5/24/2017.
 * Contributors:
 * Last updated on: 5/26/2017
 */

public class LoggingAPITest {

    public static void main(String[] args) {


        //SET THIS APP WIDE
        EDeviceSwitchSession eDeviceSwitchSession = new EDeviceSwitchSession();
        eDeviceSwitchSession.setCompanyId("2222");
        eDeviceSwitchSession.setStationId("-1");
        eDeviceSwitchSession.setStoreId("0000");
        eDeviceSwitchSession.setSessionStage("Started");
        eDeviceSwitchSession.setUserId("12456");
        eDeviceSwitchSession.setGUIVersion("1.1");
        eDeviceSwitchSession.setStartDateTime(String.valueOf(System.currentTimeMillis()));

        EDeviceInfo eDeviceInfo = new EDeviceInfo();
        eDeviceInfo.setMake("Samsung");
        eDeviceInfo.setModel("S8 Plus");
        eDeviceInfo.setImei("1204597451021456");

        EDeviceInfo eDeviceInfo1 = new EDeviceInfo();
        eDeviceInfo1.setMake("Apple");
        eDeviceInfo1.setModel("iPhone 7");
        eDeviceInfo1.setImei("56412365478954123");

        EDeviceSwitchContentTransferDetail detail = new EDeviceSwitchContentTransferDetail();
        detail.setContentType("image");
        detail.setNumberOfEntries(1000);
        detail.setStartDateTime(System.currentTimeMillis());
        detail.setEndDateTime(System.currentTimeMillis());
        detail.setEstimatedTimeInMS(10L);
        detail.setTransferStatus("success");
        detail.setTransferState("completed");

        EDeviceSwitchContentTransferDetail detail1 = new EDeviceSwitchContentTransferDetail();
        detail1.setContentType("video");
        detail1.setNumberOfEntries(1000);
        detail1.setStartDateTime(System.currentTimeMillis());
        detail1.setEndDateTime(System.currentTimeMillis());
        detail1.setEstimatedTimeInMS(10L);
        detail1.setTransferStatus("success");
        detail1.setTransferState("completed");

        EDeviceSwitchContentTransferDetail[] details = new EDeviceSwitchContentTransferDetail[2];
        details[0] = detail;
        details[1] = detail1;

        //eDeviceInfo.setEDeviceSwitchContentTransferDetailCollection(details);
        //eDeviceInfo1.setEDeviceSwitchContentTransferDetailCollection(details);

        eDeviceSwitchSession.setSourceDeviceInfoId(eDeviceInfo);
        eDeviceSwitchSession.setDestinationDeviceInfoId(eDeviceInfo1);

        EDeviceSwitchSourceContentSummary contentSummary = new EDeviceSwitchSourceContentSummary();
        contentSummary.setContentType("audio");
        contentSummary.setSelected("true");
        contentSummary.setNumberOfEntries(1234);
        contentSummary.setTotalSizeOfEntries(908908L);
        EDeviceSwitchSourceContentSummary contentSummary1 = new EDeviceSwitchSourceContentSummary();
        contentSummary1.setContentType("video");
        contentSummary1.setSelected("true");
        contentSummary1.setNumberOfEntries(1234);
        contentSummary1.setTotalSizeOfEntries(908908L);

        EDeviceSwitchSourceContentSummary[] summaries = new EDeviceSwitchSourceContentSummary[2];
        summaries[0] = contentSummary;
        summaries[1] = contentSummary1;

        eDeviceSwitchSession.setEDeviceSwitchSourceContentSummaryCollection(summaries);

        eDeviceSwitchSession = eDeviceSwitchSession;

//        DashboardLog.addOrUpdateContentTransferDetail(DashboardLog.DATATYPE_CONTACT, 1000);
//        DashboardLog.addOrUpdateContentTransferDetail(DashboardLog.DATATYPE_CALENDAR, 5000);
//        DashboardLog.addOrUpdateContentTransferDetail(DashboardLog.DATATYPE_PHOTO, 100);
//        DashboardLog.addOrUpdateContentTransferDetail(DashboardLog.DATATYPE_VIDEO, 10);
        //DashboardLog.updateToServer();


        //JSON Handling
        Gson gson = new Gson();
        String json = gson.toJson(eDeviceSwitchSession);
        System.out.println(json);
        json = "{   \"companyId\": \"1234\",   \"destinationDeviceInfoId\": {     \"OSVersion\": \"5.0.1\",     \"buildNumber\": \"LRX22C.I9505XXUPOJ2\",     \"edeviceSwitchContentTransferDetailCollection\": [       {         \"contentType\": \"contact\",         \"estimatedTimeInMS\": -1,         \"numberOfEntries\": 1,         \"transferState\": \"Completed\",         \"transferStatus\": \"Success\",         \"endDateTime\": 1496750615101,         \"startDateTime\": 1496750614682,         \"totalSizeOfEntries\": -1       }     ],     \"firmware\": \"LRX22C.I9505XXUPOJ2\",     \"imei\": \"357377052665867\",     \"make\": \"samsung\",     \"model\": \"GT-I9505\",     \"operationType\": \"restore\",     \"platform\": \"Android\",     \"endDateTime\": 0,     \"freeStorage\": -1,     \"startDateTime\": 0,     \"totalStorage\": -1   },   \"endDateTime\": \"1496750615423\",   \"sessionStage\": \"wds_transfer_completed\",   \"sessionStatus\": \"wds_success\",   \"sourceDeviceInfoId\": {     \"OSVersion\": \"7.0\",     \"buildNumber\": \"NBD90Z\",     \"edeviceSwitchContentTransferDetailCollection\": [       {         \"contentType\": \"contact\",         \"estimatedTimeInMS\": -1,         \"numberOfEntries\": 1,         \"transferState\": \"Completed\",         \"transferStatus\": \"Success\",         \"endDateTime\": 1496750615101,         \"startDateTime\": 1496750614682,         \"totalSizeOfEntries\": -1       }     ],     \"firmware\": \"NBD90Z\",     \"imei\": \"355470060643760\",     \"make\": \"motorola\",     \"model\": \"Nexus 6\",     \"operationType\": \"backup\",     \"platform\": \"Android\",     \"endDateTime\": 0,     \"freeStorage\": -1,     \"startDateTime\": 0,     \"totalStorage\": -1   },   \"startDateTime\": \"1496750589346\",   \"stationId\": \"-1\",   \"storeId\": \"1234\",   \"transactionType\": \"WDS\",   \"transferMode\": \"WLAN\",   \"userId\": \"user1\",   \"deviceSwitchSessionId\": null }";

        //Call REST API
        SimpleURLConnectionTest.doInBackground(new String[]{Constants.LOGGING_API_ENDPOINT, json});

    }

    public static void callA(String json) {
        Gson gson = new Gson();
        DashboardLog.getInstance().seteDeviceSwitchSession(gson.fromJson(json, EDeviceSwitchSession.class));
        String gsonString = gson.toJson(DashboardLog.getInstance().geteDeviceSwitchSession());
        System.out.println("Converted JSON: "  + gsonString);
    }
}
