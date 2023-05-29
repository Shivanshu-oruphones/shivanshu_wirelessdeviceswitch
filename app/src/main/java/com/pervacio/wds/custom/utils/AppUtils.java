package com.pervacio.wds.custom.utils;

import android.util.Log;

import androidx.annotation.StringDef;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.ServerSocket;

/**
 *
 *This class will required to print All the logs of SyncServer App
 * @author Ravikumar
 * Created by Pervacio on 16-01-2017.
 */

public class AppUtils {
    public static final boolean LOGGING_ENABLED = true;
    public static int numOfAttempts=0;
    public static int FIXED_PORT_NUMBER = 31313;

    private static final String TAG = "SyncServerActivity";

    public static enum LogType {
        INFO, DEBUG, EXCEPTION, WARN, VERBOSE;
    }

    private AppUtils() {
    }

    public static void printLog(String _tag, String message, Throwable exc, LogType logType) {
        message = _tag + ":" + message;
        String exceptionMessage = "";
        if(exc!= null)
        {
            message = message + "exception: " + exc.getMessage();
        }
        if (LOGGING_ENABLED) {
            switch (logType) {
                case DEBUG:
                    // Log.d(TAG, message);
                    break;
                case EXCEPTION:
                    Log.e(TAG, message);
                    break;
                case VERBOSE:
                    Log.v(TAG, message);
                    break;
                case WARN:
                    Log.w(TAG, message);
                    break;
                case INFO:
                default:
                    Log.i(TAG, message);
                    break;
            }
        }
    }

    /**
     * Method used to updating the port number
     *
     * @return Avaialble port number
     */
    public static int createServer() {
        try {
            if (isLocalPortInUse(FIXED_PORT_NUMBER)) {
                if (numOfAttempts == 0)
                    FIXED_PORT_NUMBER = FIXED_PORT_NUMBER + 5;
                else
                    FIXED_PORT_NUMBER = FIXED_PORT_NUMBER + 1;

                numOfAttempts++;
                AppUtils.printLog(TAG, String.format("%s = %d", ": numOfAttempts", numOfAttempts).toString() + " and " + String.format("%s = %d", "Updating port number", FIXED_PORT_NUMBER).toString(), null, LogType.INFO);
                createServer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        numOfAttempts = 0;
        return FIXED_PORT_NUMBER;
    }

    /**
     * @param port Given port number
     * @return true, if Port is using, else false.
     */
    private static boolean isLocalPortInUse(int port) {
        boolean isOpenPort = false;
        try {
            // ServerSocket try to open a LOCAL port
            new ServerSocket(port).close();
            // local port can be opened, it's available
        } catch (IOException e) {
            AppUtils.printLog(TAG, ": Exception while creating the server,Port is using : " + FIXED_PORT_NUMBER + " ", e, LogType.EXCEPTION);
            // local port cannot be opened, it's in use
            isOpenPort = true;
        }
        return isOpenPort;
    }
    public static class PermissionsFlow{

        @Retention(RetentionPolicy.SOURCE)
        @StringDef({Customers.TELEFONICA_O2UK,Customers.TELEFONICA_GERMANY})
        public @interface Customers {
            String  TELEFONICA_O2UK = "TelefonicaO2UK";
            String  TELEFONICA_GERMANY = "TelefonicaGermany";
        }


        @Retention(RetentionPolicy.SOURCE)
        @StringDef({Features.IMEI_READ})
        public @interface Features{
            String  IMEI_READ = "IMEI";
        }

        public static boolean isAllowed( @Customers String customers ,@Features String feature){
            switch (customers){
                case Customers.TELEFONICA_GERMANY:
                    return isAllowedForGermany(feature);
            }
            return true;
        }

        private static boolean isAllowedForGermany(@Features String feature){
            String[] germanyFeature = {};
            return searchFeature(germanyFeature, feature);
        }

        private static boolean searchFeature(String[] features,@Features String feature){
            for(String availableFeature :features){
                if(feature.equalsIgnoreCase(availableFeature)){
                    return true;
                }
            }
            return false;
        }
    }
}
