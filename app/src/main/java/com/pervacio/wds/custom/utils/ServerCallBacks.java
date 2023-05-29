package com.pervacio.wds.custom.utils;

/**
 * Created by Surya Polasanapalli on 25-05-2017.
 */

public interface ServerCallBacks {

    void gpsLocationGranted(boolean result);

    void gpsLocationRequested(boolean requested);

    void locationValidation(boolean result, String response);

    void storeidValidation(boolean result,String response);

    void onserverCallCompleted(String response);

    void storeidAndRepidValidation(boolean result,String response);

    void repLoginValidation(boolean result,String response);

}
