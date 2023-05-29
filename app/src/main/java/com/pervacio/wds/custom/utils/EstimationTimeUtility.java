package com.pervacio.wds.custom.utils;

import com.pervacio.wds.app.EMDataType;
import com.pervacio.wds.app.EMMigrateStatus;

import java.util.HashMap;

import static com.pervacio.wds.custom.utils.Constants.THRESHOLD_ESTIMATION;

/**
 * Created by Surya Polasanapalli on 2/9/2018.
 */

public class EstimationTimeUtility {

    private static EstimationTimeUtility mInstance = null;

    private final int PIM_BASE_COUNT = 100;
    private final HashMap<Integer, Long> pimDataEstimationMap = new HashMap<>();

    {
        pimDataEstimationMap.put(EMDataType.EM_DATA_TYPE_CONTACTS, 18 * 1000L); // 1000 per 3 Min (100/18 sec)
        pimDataEstimationMap.put(EMDataType.EM_DATA_TYPE_CALENDAR, 12 * 1000L); //1000 per 2 Min  (100/12 sec)
        pimDataEstimationMap.put(EMDataType.EM_DATA_TYPE_CALL_LOGS, 6 * 1000L);    //1000 per 1 Min (100/6 sec)
        pimDataEstimationMap.put(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, 12 * 1000L); //1000 per 2 Min (100 / 2 sec)
        pimDataEstimationMap.put(EMDataType.EM_DATA_TYPE_SETTINGS, 6 * 1000L);         //100 per 1 Min
    }

    private EstimationTimeUtility() {

    }

    public static EstimationTimeUtility getInstance() {
        if (mInstance == null) {
            mInstance = new EstimationTimeUtility();
        }
        return mInstance;
    }

    public long getEstimationForPIM(int selectedDataType) {
        long estimationTime = 0;
        if ((selectedDataType & EMDataType.EM_DATA_TYPE_CONTACTS) != 0) {
            long individualEstimationTime = getIndividualEstimationTime(EMDataType.EM_DATA_TYPE_CONTACTS, roundUpNumber(EMMigrateStatus.getContentDetails(EMDataType.EM_DATA_TYPE_CONTACTS), PIM_BASE_COUNT));
            estimationTime = getHigherValue(estimationTime, individualEstimationTime);
        }
        if ((selectedDataType & EMDataType.EM_DATA_TYPE_CALENDAR) != 0) {
            long individualEstimationTime = getIndividualEstimationTime(EMDataType.EM_DATA_TYPE_CALENDAR, roundUpNumber(EMMigrateStatus.getContentDetails(EMDataType.EM_DATA_TYPE_CALENDAR), PIM_BASE_COUNT));
            estimationTime = getHigherValue(estimationTime, individualEstimationTime);
        }
        if ((selectedDataType & EMDataType.EM_DATA_TYPE_SMS_MESSAGES) != 0) {
            long individualEstimationTime = getIndividualEstimationTime(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, roundUpNumber(EMMigrateStatus.getContentDetails(EMDataType.EM_DATA_TYPE_SMS_MESSAGES), PIM_BASE_COUNT));
            estimationTime = getHigherValue(estimationTime, individualEstimationTime);
        }
        if ((selectedDataType & EMDataType.EM_DATA_TYPE_CALL_LOGS) != 0) {
            long individualEstimationTime = getIndividualEstimationTime(EMDataType.EM_DATA_TYPE_CALL_LOGS, roundUpNumber(EMMigrateStatus.getContentDetails(EMDataType.EM_DATA_TYPE_CALL_LOGS), PIM_BASE_COUNT));
            estimationTime = getHigherValue(estimationTime, individualEstimationTime);
        }
        if ((selectedDataType & EMDataType.EM_DATA_TYPE_SETTINGS) != 0) {
            long individualEstimationTime = getIndividualEstimationTime(EMDataType.EM_DATA_TYPE_SETTINGS, roundUpNumber(EMMigrateStatus.getContentDetails(EMDataType.EM_DATA_TYPE_SETTINGS), PIM_BASE_COUNT));
            estimationTime = getHigherValue(estimationTime, individualEstimationTime);
        }
        return estimationTime;
    }


    private long roundUpNumber(long number, int roundTo) {
        if (number <= 0)
            return 0;
        return ((number + (roundTo - 1)) / roundTo) * roundTo;
    }

    public long getIndividualEstimationTime(int dataType, long dataCount) {
        long baseEstimation;
        long estimation = 0;
        try {
            baseEstimation = pimDataEstimationMap.get(dataType);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
        if (dataCount <= 0)
            return 0;
        estimation = (dataCount * baseEstimation) / PIM_BASE_COUNT;
        return estimation;
    }

    public long getHigherValue(long value1, long value2) {
        if (value1 > value2) {
            return value1;
        }
        return value2;
    }


    //Returns if Threshold value, if time is less than threshold value.
    //else adds 10% of time and returns

    public long addThresholdValue(long time) {
       /* if (time < THRESHOLD_ESTIMATION) {
            return THRESHOLD_ESTIMATION;
        }
        return (long) (time+(time*0.1));*/
       return (time+Constants.ONE_MINUTE);
    }


}
