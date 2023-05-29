package com.pervacio.wds.custom.utils;

import android.Manifest;
import android.os.Build;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Central class to store all constants.
 *
 * Created by: Surya on 5/26/2017.
 * Contributors: Darpan Dodiya
 *
 * Last updated on: 06/09/2017
 */

public class Constants {

    public static final String COMPANY_PERVACIO = "Pervacio";
    public static final String COMPANY_ORANGE = "Orange";
    public static final String COMPANY_ORANGE_SENEGAL = "OrangeSenegal";
    public static final String COMPANY_ORANGE_POLAND = "OrangePoland";
    public static final String COMPANY_BELL = "Bell";
    public static final String COMPANY_TMOBILE = "TMobile";
    public static final String COMPANY_ROGERS = "Rogers";
    public static final String COMPANY_STAPLES = "Staples";
    public static final String COMPANY_SPRINT = "Sprint";
    public static final String COMPANY_MOBILECOPY= "MobileCopy";
    public static final String COMPANY_TMS = "TMS";
    public static final String COMPANY_TELEFONICA = "Telefonica";
    public final static String productConfig = "config.xml";
    public static final String LOCATION_VALIDATION = "locationvalidation";
    public static final String STORE_AND_REPVALIDATION = "storeandrepvalidation";
    public static final String REP_VALIDATION = "repvalidation";
    public static final String FEEDBACK = "feeback";
    public static final String STOREID_VALIDATION = "storeidvalidation";
    public static final String INSTALLATION_LOGGING = "installationlogging";
    public static final String TRANSACTION_LOGGING = "transactionlogging";
    public static final String SUPPORTED_CHECK= "supported_check";
    public static final String STORE_ID = "store_id";
    public static final String MIGRATION_STARTED = "MIGRATION_STARTED";
    public static final String MIGRATION_CANCELLED = "MIGRATION_CANCELLED";
    public static final String COMMAND_APP_KILLED = "APP_KILLED";
    public static final String COMMAND_APP_CRASHED = "APP_CRASHED";
    public static final String LINK_SPEED = "LINK_SPEED";
    /** URLs & Endpoints **/
    //Do NOT add / at the end of URL
    public static String SERVER_ADDRESS= "http://192.168.0.45:8080";
    public static String R2USESSIONLOGGING = SERVER_ADDRESS+"/r2uservice";
    public static String FEEDBACK_URL= SERVER_ADDRESS+"/care/sprintservice/feedbackEmail";
    public static String STOREID_VALIDATION_URL = SERVER_ADDRESS+"/DeviceSwitchLogging/storeLogin";
    public static String GPS_VALIDATION_URL = SERVER_ADDRESS+"/DeviceSwitchLogging/gpsLogin";
    public static String INSTALLATION_LOGGING_URL = SERVER_ADDRESS+"/DeviceSwitchLogging/logAppUsageHistory";
    public static String LOGGING_API_ENDPOINT = SERVER_ADDRESS+"/DeviceSwitchLogging/logDeviceSwitchSession";
    public static String SUPPORTED_CHECK_URL = SERVER_ADDRESS+"/DeviceCertification/checkDeviceCertificationStatus";
    public static String LOG_UPLOAD_URL = SERVER_ADDRESS+"/CommonServices/fileupload";
    public static String CLOUDPAIRING_URL = SERVER_ADDRESS+"/pairingservice";
    public static String STORELOGINBYSTOREIDANDREPID_URL = SERVER_ADDRESS+"/DeviceSwitchLogging/storeLoginByStoreIdandRepid";
    public static String REP_LOGIN_URL = SERVER_ADDRESS+"/DeviceSwitchLogging/repLogin";
    public static String PROPERTY_FILE_PATH = "/data/local/tmp/pva/property.txt";
    public static final String PORT_NUMBER_FILE_NAME = "PortNumber.txt";
    public static boolean IS_MIGRATION_RESUMED = false;
    public static boolean ENABLE_LOGS_UPLOAD = false;
    public static String SERVER_KEY = "";
    public static String SERVER_SECRET = "";

    public static void setServerAddress (final String SERVER_URL) {
        SERVER_ADDRESS = SERVER_URL;
        R2USESSIONLOGGING = SERVER_ADDRESS+"/r2uservice";
        LOG_UPLOAD_URL = SERVER_ADDRESS+"/CommonServices/fileupload";
        STOREID_VALIDATION_URL = SERVER_ADDRESS+"/DeviceSwitchLogging/storeLogin";
        GPS_VALIDATION_URL = SERVER_ADDRESS+"/DeviceSwitchLogging/gpsLogin";
        INSTALLATION_LOGGING_URL = SERVER_ADDRESS+"/DeviceSwitchLogging/logAppUsageHistory";
        LOGGING_API_ENDPOINT = SERVER_ADDRESS+"/DeviceSwitchLogging/logDeviceSwitchSession";
        SUPPORTED_CHECK_URL = SERVER_ADDRESS+"/DeviceCertification/checkDeviceCertificationStatus";
        FEEDBACK_URL= SERVER_ADDRESS+"/care/sprintservice/feedbackEmail";
        CLOUDPAIRING_URL= SERVER_ADDRESS+"/pairingservice";
        STORELOGINBYSTOREIDANDREPID_URL = SERVER_ADDRESS+"/DeviceSwitchLogging/storeLoginByStoreIdandRepid";
        REP_LOGIN_URL = SERVER_ADDRESS+"/DeviceSwitchLogging/repLogin";
/*        if(!COMPANY_NAME.equalsIgnoreCase(COMPANY_MOBILECOPY)){
            LOG_UPLOAD_URL = SERVER_ADDRESS+"/PervacioCommonServices/fileupload";
        }*/
        if(!COMPANY_NAME.equalsIgnoreCase(COMPANY_MOBILECOPY)){
            LOG_UPLOAD_URL = SERVER_ADDRESS+"/api/device/fileupload";
        }
        setPlatform();
    }


    private static void setPlatform(){
       if(Build.BRAND.equalsIgnoreCase(PLATFORM_BLACKBERRY)){
           String os = System.getProperty("os.name");
           if ("qnx".equalsIgnoreCase(os)) {
               Constants.PLATFORM = PLATFORM_BLACKBERRY;
               SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_CALENDAR, false);
               SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, false);
               SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_SETTINGS, false);
               SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_CALL_LOGS, false);
           }
           DLog.log("OS name "+os);
        }
    }

    public static final String RESPONSE_KEY_LOGIN_STATUS = "loginStatus";
    public static final String RESPONSE_KEY_STORE_ID = "storeCode";
    public static final String RESPONSE_KEY_STORE_ID_LIST = "storeIdList";
    public static final String RESPONSE_KEY_COMPANY_ID = "companyId";
    public static final String HTTP_AUTHENTICATION_USERNAME = "appstoreuser";
    public static final String HTTP_AUTHENTICATION_PASSWORD = "$ecr3T";

    /*** Constants For Dashboard Logging ***/
    public static final String PLATFORM_ANDROID = "Android";
    public static final String PLATFORM_IOS = "iOS";
    public static final String PLATFORM_WINDOWS = "windows";
    public static final String PLATFORM_BLACKBERRY = "Blackberry";
    public static String PLATFORM = PLATFORM_ANDROID;
    public static final String UNKNOWN = "Unknown";
    public static final long NO_STORAGE = -1;
    public static final long NO_SIZE = -1;
    public static final long NO_ENTRIES = -1;
    public static final long NO_TIME = 0;
    public static final long UNINITIALIZED = 0;

    public static final String DASHBOARD_TAG = "DLU";
    public static final String SELECTED = "Y";
    public static final String NOT_SELECTED = "N";

    /*** SharedPrefs Constants ***/
    public static final String PREF_UPLOAD_FINISHED = "UPLOAD_FINISHED";
    public static final String PREF_FINISH_CLICKED = "FINISH_CLICKED";

    public static final String APP_MIGRATION_DIRECTORY = "Apps";
    public static final String DOCUMENTS_MIGRATION_DIRECTORY = "Documents";
    public static final String TERMS_AGREED = "termsagreed";
    public static final String FLAVOUR_SPRINT = "sprint";
    public static final String FLAVOUR_PLAYSTORE = "playstore";
    public static final String FLAVOUR_TMS = "tms";
    public static final int IMEI_CHECK_MIN_APILEVEL = 29;
    public static String[] manualDevice = new String[]{"Nokia 1.3","SM-A013M","Nokia C1 Plus"};

    /** Operation type **/
    public enum OPERATION_TYPE {
        BACKUP("backup"),
        RESTORE("restore");

        private String value;

        OPERATION_TYPE(String value) {
            this.value = value;
        }
        public String value() {
            return value;
        }
    }

    /*** XML tag names ***/
    public enum DB_XML {
        MAKE("db_device_make"),
        MODEL("db_device_model"),
        PLATFORM("db_device_platform"),
        OS_VERSION("db_device_os_version"),
        OPERATION_TYPE("db_device_operation_type"),
        IMEI("db_device_imei"),
        BUILD_NO("db_device_build_no"),
        FIRMWARE("db_device_firmware"),
        FREE_STORAGE("db_device_free_storage"),
        TOTAL_STORAGE("db_device_total_storage"),
        APP_VERSION("db_device_app_version");

        private String value;
        DB_XML(String value) {
            this.value = value;
        }
        public String value() {
            return value;
        }
    }

    /*** Datatypes ***/
    public enum DATATYPE {

        CONTACT("contact"),
        CALENDAR("calendar"),
        MESSAGE("message"),
        IMAGE("image"),
        VIDEO("video"),
        AUDIO("audio"),
        APP("app"),
        CALLLOG("call logs"),
        SETTINGS("settings"),
        DOCUMENTS("documents");

        private String value;
        DATATYPE(String value) {
            this.value = value;
        }
        public String value() {
            return value;
        }

        /*
        Below map file is for reverse relation.

        e.g. "contact" >> CONTACT
             "calendar" >> CALENDAR

        Normally, we don't need such reverse relation. Useful in switch() to compare String to enum.
        */

        private static final Map<String, DATATYPE> map = new HashMap<>();
        static {
            for (DATATYPE en : values()) {
                map.put(en.value, en);
            }
        }

        public static DATATYPE valueFor(String name) {
            return map.get(name);
        }
    }

    /*** Transfer state ***/
    public enum TRANSFER_STATE {
        NOT_STARTED("notstarted"),
        IN_PROGRESS("inprogress"),
        COMPLETED("completed");

        private String value;
        TRANSFER_STATE(String value) {
            this.value = value;
        }
        public String value() {
            return value;
        }
    }

    /*** Transfer status ***/
    public enum TRANSFER_STATUS {
        SUCCESS("success"),
        FAILED("fail");

        private String value;
        TRANSFER_STATUS(String value) {
            this.value = value;
        }
        public String value() {
            return value;
        }
    }

    /*** Session stages ***/
    public enum SESSION_STAGE {
        APP_LAUNCHED("wds_app_launched"),
        STORE_AUTHENTICATED("wds_store_authenticated"),
        QR_GENERATED("wds_qr_generated"),
        DEVICES_CONNECTED("wds_devices_paired"),
        TRANSFER_STARTED("wds_transfer_started"),
        TRANSFER_IN_PROGRESS("wds_transfer_in_progress"),
        TRANSFER_COMPLETED("wds_transfer_completed"),
        TRANSFER_CLOSED("wds_transfer_closed");

        private String value;
        SESSION_STAGE(String value) {
            this.value = value;
        }
        public String value() {
            return value;
        }
    }

    /*** Session status ***/
    public enum SESSION_STATUS {
        NOT_STARTED("wds_not_started"),
        IN_PROGRESS("wds_in_progress"),
        SUCCESS("wds_success"),
        CANCELLED("wds_cancelled");

        private String value;
        SESSION_STATUS(String value) {
            this.value = value;
        }
        public String value() {
            return value;
        }
    }

    /*** Cancellation reasons ***/
    public enum CANCEL_REASON {
        AFTER_PAIRING_SOURCE_CANCELLED("Cancelled on source device after pairing"),
        AFTER_PAIRING_DESTINATION_CANCELLED("Cancelled on destination device after pairing"),
        BEFORE_PAIRING_SOURCE_CANCELLED("Cancelled on source device after selecting device but before pairing"),
        BEFORE_PAIRING_DESTINATION_CANCELLED("Cancelled on destination device after selecting device but before pairing"),
        IN_MIGRATION_SOURCE_CANCELLED("Cancelled on source device during migration"),
        IN_MIGRATION_DESTINATION_CANCELLED("Cancelled on destination device during migration"),
        BEFORE_PAIRING_SOURCE_DENIED_PERMISSION("User denied permissions in source device before pairing"),
        BEFORE_PAIRING_DESTINATION_DENIED_PERMISSION("User denied permissions in destination device before pairing"),
        CANCELLED_UNKNOWN("Transaction cancelled with unknown reason"),
        NO_ERROR("No error"),
        MIGRATION_FAILED("Migration Failed"),
        PAIRING_FAILED("Pairing failed"),
        ESTIMATION_FAILED("Estimation failed"),
        CONNECTION_FAILED("Connection failed"),
        FORCE_CLOSED("App force closed");

        private String value;
        CANCEL_REASON(String value) {
            this.value = value;
        }
        public String value() {
            return value;
        }
    }

    /** Transaction type **/
    public enum TRANSACTION_TYPE {
        WDS("WDS"),
        MMDS("MMDS");
        private String value;
        TRANSACTION_TYPE(String value) {
            this.value = value;
        }
        public String value() {
            return value;
        }
    }

    /** Transfer mode **/
    public enum TRANSFER_MODE {
        WLAN("WLAN"),
        WIFI_DIRECT("WDIRECT"),
        CLOUD("CLOUD");

        private String value;
        TRANSFER_MODE(String value) {
            this.value = value;
        }
        public String value() {
            return value;
        }
    }

    public static String EM_TEXT_COMMAND = "command";

    public static boolean stopMigration = false;

    //if logging (GEO Fencing) is not required , make this variable false, vice versa.
    public static  boolean LOGGING_ENABLED = false;
    public static boolean IS_MMDS= false;
    public static String mTransferMode= "WDirect";
    public static boolean AUTHENTICATION_REQUIRED= false;
    public static boolean CERTIFICATION_REQUIRED= false;
    public static boolean ESTIMATION_TIME_REQUIRED= true;
    public static boolean CLOUD_PAIRING_ENABLED= true;

    public static final boolean MMS_SUPPORT = false;
    public static boolean NEW_PLAYSTORE_FLOW = false;
    public static String COMPANY_NAME = "Home";
    public static String COUNTRY_NAME = "";
    public static final ArrayList<String> UNINSTALLATION_SUGGESTIONS = new ArrayList<>(Arrays.asList(COMPANY_SPRINT,COMPANY_TMS,COMPANY_BELL,COMPANY_ORANGE,COMPANY_ORANGE_SENEGAL,COMPANY_ORANGE_POLAND,COMPANY_TELEFONICA));

    /*** Timeouts ***/
    public static final long TRANSACTION_COMPLETE_DIALOG_TIMEOUT = 10 * 1000; //10 secs

    public static final String WDS_SOURCE_HEADSUP = "wds_device_source";
    public static final String WDS_DEST_HEADSUP = "wds_device_destination";
    public static final String WDS_MIGRATION_DONE = "wds_migration_done";
    public static final String WDS_CONNECTION_OK = "wds_connection_ok";
    public static final String WDS_CONNECTION_FAILED = "wds_connection_failed";
    public static final String WDS_SRC_DISCONNECTED="wds_src_disconnected";
    public static final String WDS_DEST_DISCONNECTED="wds_dest_disconnected";
    public static final String WDS_SRC_CONNECTED="wds_src_connected";
    public static final String WDS_DEST_CONNECTED="wds_dest_connected";

    //Request Commands
    public static final String WDS_ARE_YOU_ALIVE = "wds_are_you_alive";
    public static final String WDS_OSTYPE_REQUEST = "wds_device_ostype";
    public static final String WDS_REMOTE_DEVICE_ANDROID = "wds_remote_device_ostype_android";
    public static final String WDS_REMOTE_DEVICE_IOS = "wds_remote_device_ostype_ios";
    public static final String WDS_GET_NETWORK_DETAILS_REQUEST = "wds_get_network_details";
    public static final String WDS_WAIT_FOR_MIGRATION_DONE = "wds_wait_for_wds_migration_done";
    public static final String COMMAND_GET_TRANSACTION_DETAILS = "wds_get_transaction_details";
    public static final String WDS_SEND_SESSION_JSON = "wds_send_session_json";
    public static final String WDS_DEVICE_INFO = "wds_device_info";
    public static final String WDS_REMOTE_DEVICE_INFO = "wds_remote_device_info";

    //Response Commands
    public static final String WDS_ANDROID_RESPONSE = "wds_device_android";
    public static final String WDS_OK = "wds_ok";
    public static final String COMMAND_NETWORK_DETAILS = "wds_network_details"; //wds_network_details:/192.168.49.1:DIRECT-3b-Galaxy S5:qa8B5wNi
    public static final String WDS_STATUS="wds_status";
    public static final String WDS_MIGRATION_STATUS="wds_migration_status";
    public static final int SRC_NW_CHNGD = 511;
    public static final int DST_NW_CHNGD = 512;

    //PAIRING
    public static final int PAIRING_NOT_STARTED = 100;
    public static final int PAIRING_INPROGRESS = 101;
    public static final int PAIRING_FAILED_UNKNOWN = 102;
    public static final int PAIRING_FAILED_TIMEOUT = 103;
    public static final int PAIRING_FAILED_USER_DENIED = 104;
    public static final int PAIRING_SUCCEEDED = 105;

    //REVIEW
    public static final int REVIEW_NOT_STARTED = 200;
    public static final int REVIEW_INPROGRESS = 201;
    public static final int REVIEW_SUCCEEDED = 202;

    //BACKUP
    public static final int BACKUP_NOT_STARTED = 300;
    public static final int BACKUP_INPROGRESS = 301;
    public static final int BACKUP_SUCCEEDED = 302;

    //RESTORE
    public static final int RESTORE_NOT_STARTED = 400;
    public static final int RESTORE_INPROGRESS = 401;
    public static final int RESTORE_SUCCEEDED = 402;

    //MIGRATION
    public static final int MIGRATION_NOT_STARTED = 500;
    public static final int MIGRATION_INPROGRESS = 501;
    public static final int MIGRATION_SUCCEEDED = 502;
    public static final int MIGRATION_FAILED = 503;

    //LOGGING
    public static final int LOG_UPLOAD_NOT_STARTED = 600;
    public static final int LOG_UPLOAD_INPROGRESS = 601;
    public static final int LOG_UPLOAD_SUCCEEDED = 602;

    public static final long ESTIMATION_TIME_FILESIZE = 10 * 1024 * 1024L;
    public static final long THRESHOLD_ESTIMATION = 5 * 60 * 1000L;

    public static final int NOTIFICATION_TIME = 15*1000;

    public static int ME_PORT_NUMBER = 31323;

    public static boolean CUSTOM_SELECTION_ENABLED = true;

    public static boolean EXCLUDE_WHATSAPP_MEDIA = false;
    public static boolean EXCLUDE_SDCARD_MEDIA = false;
    public static boolean SELECT_ALL_APPS_CHECKBOX= false;
    public static boolean ENABLE_INSTALL_MOVISTAR_APPS = false;
    public static boolean ENABLE_INSTALL_NON_MOVISTAR_APPS = true;
    public static boolean ENABLE_DOWNLOAD_MOVISTAR_APPS = true;
//    public static List<String> movistarappsList=new ArrayList<String>(Arrays.asList(" My Movistar","Movistar Music","Movistar Play","Movistar Club"));
//    public static List<String> movistarappsList=new ArrayList<String>(Arrays.asList("Mi Movistar","Club Movistar","Movistar Play"));
/*    public static List<String> movistarappsList=new ArrayList<String>(Arrays.asList("Mi Movistar","Club Movistar"));
    public static List<String> movistarappsListS3 = new ArrayList<String>(Arrays.asList("app-movistarUY-enterprise-11.8.26.apk","movistarRelease-1-1-14-33-1e8afhfsf.apk"));*/
public static List<String> movistarappsList=new ArrayList<String>();
    public static List<String> movistarappsListS3 = new ArrayList<String>();
    public static Map<String,String> moviStarAppsMap = new LinkedHashMap<>();
    public static List<String> preLoadedAppsListServer =new ArrayList<String>(); // App Names List
    public static List<String> preLoadedAppNamesS3ListServer =new ArrayList<String>(); // App Names S3 List
    public static final HashMap<Integer, Boolean> SUPPORTED_DATATYPE_MAP = new HashMap<>();
//    public static boolean GET_PRELOADED_APPS_NAMES_FROM_SERVER = true;
    public static boolean GET_PRELOADED_APPS_NAMES_FROM_SERVER = false;
    public static boolean launchingAppsInstallFirstTime = true;

    static {
        SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_CONTACTS, true);
        SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_CALENDAR, true);
        SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, true);
        SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_CALL_LOGS, true);
        SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_SETTINGS, true);
        SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_PHOTOS, true);
        SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_VIDEO, true);
        SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_MUSIC, true);
        SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_APP, true);
        SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_DOCUMENTS, true);
    }

    public static final ArrayList<String> WIFI_PROBLAMATIC_MODELS = new ArrayList<>(Arrays.asList("RNE-L21","SM-A510F","SM-A320FL","XT1045","MotoG3","MotoG3-TE","ALE-L21","XT1032","XT1079","SM-A500W","SM-A500G","SM-A520F","SM-G386W","Moto G Play","XT1540","XT1670","XT1064","XT1563","LG-H812","LG-D852","C6506")); //Models those disconnects from wifi when we host wifiDirect.
    public static final ArrayList<String> WIFI_DIRECT_PROBLAMATIC_MODELS = new ArrayList<>(Arrays.asList("RNE-L21","SM-A510F","SM-A320FL","SM-A500W","SM-A500G","SM-A520F","SM-G386W","LG-H812","LG-D852"));// Models those host the wifi-Direct and can not connect(Not in Range).
    public static final ArrayList<String> SMS_BULK_INSERTION_PROBLAMATIC_MODELS = new ArrayList<>(Arrays.asList("D5803","D6503","STV100-3","STV100-1","6045I","E6833","6055A","E6830","E6853","F8131","F5321", "ZTE B2017G", "ASUS_Z010D","E2306")); //ZTE B2017G Models for which bulk insertion is not working /*#54767*/
    public static final Set<String> P2P_MODELS = new HashSet<>(Arrays.asList("SM-A500FU","SM-A510F","SM-A320FL","SM-A500G","SM-A520F","SM-A500W", "SM-A520W", "SM-A530W", "SM-A730DS", "SM-A730F"));
    public static final Set<String> P2P_PROBLEMATIC_MODELS = new HashSet<>(Arrays.asList("SM-A013M","SM-A750G","SC-02K")); // Satya added , in this models p2p device address is coming as "02:00:00:00:00:00"

    public static boolean SUPPORT_P2P_MODE = true;
    public static final String P2P_MODE = "P2P";

    public static final int DEFAULT_PIN = 4589;
    public static final String LOCALE = "locale";
    public static final String ACTION_RECONNECT_WIFI = "ACTION_RECONNECT_WIFI";
	 public static final String MIGRATION_STATS = "MIGRATION_STATS";
    public static final String CLD_PIRNG_SESION_ID = "CLD_SESSION_ID";
    public static final String START_OVER = "START_OVER";
    public static final String CANCELLED_BEFORE_PAIRING = "CANCELLED_BEFORE_PAIRING";
    public static final String RECOVERED = "RECOVERED";
    public static final boolean SWITCH_TO_SORUCE_5GHZ = true;
    public static float ESTIMATION_LOWERLIMIT = 0.75F;
    public static float ESTIMATION_UPPERLIMIT = 1.25F;
    public static boolean REESTIMATION_REQUIRED = true;
    public static long ESTIMATION_CALCULATION_TIME = 15*1000L;
    public static final long NINTY_MINUTES = 90*60*1000L;
    public static final long ONE_MINUTE = 60*1000L;


    public static final HashMap<Integer, String[]> PERMISSIONMAP = new HashMap<>();
    static {
        PERMISSIONMAP.put(EMDataType.EM_DATA_TYPE_CONTACTS, new String[]{Manifest.permission.WRITE_CONTACTS,Manifest.permission.READ_CONTACTS,Manifest.permission.GET_ACCOUNTS});
        PERMISSIONMAP.put(EMDataType.EM_DATA_TYPE_CALENDAR, new String[]{Manifest.permission.WRITE_CALENDAR,Manifest.permission.READ_CALENDAR});
        PERMISSIONMAP.put(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, new String[]{Manifest.permission.READ_SMS});
        PERMISSIONMAP.put(EMDataType.EM_DATA_TYPE_CALL_LOGS, new String[]{Manifest.permission.READ_CALL_LOG,Manifest.permission.WRITE_CALL_LOG,Manifest.permission.READ_PHONE_STATE});//Adding read_phone_state permission into group for IMEI
        PERMISSIONMAP.put(EMDataType.EM_DATA_TYPE_SETTINGS, new String[]{Manifest.permission.INTERNET}); //adding the dummy permission for settings always true.
        PERMISSIONMAP.put(EMDataType.EM_DATA_TYPE_PHOTOS, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE});
        PERMISSIONMAP.put(EMDataType.EM_DATA_TYPE_VIDEO, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE});
        PERMISSIONMAP.put(EMDataType.EM_DATA_TYPE_MUSIC, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE});
        PERMISSIONMAP.put(EMDataType.EM_DATA_TYPE_APP, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE});
        PERMISSIONMAP.put(EMDataType.EM_DATA_TYPE_DOCUMENTS, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE});
    }

    public static final HashMap<Integer, String> DATATYPE_VALUES = new HashMap<>();
    static {
        DATATYPE_VALUES.put(EMDataType.EM_DATA_TYPE_CONTACTS, DATATYPE.CONTACT.value());
        DATATYPE_VALUES.put(EMDataType.EM_DATA_TYPE_CALENDAR, DATATYPE.CALENDAR.value());
        DATATYPE_VALUES.put(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, DATATYPE.MESSAGE.value());
        DATATYPE_VALUES.put(EMDataType.EM_DATA_TYPE_CALL_LOGS, DATATYPE.CALLLOG.value());
        DATATYPE_VALUES.put(EMDataType.EM_DATA_TYPE_SETTINGS, DATATYPE.SETTINGS.value());
        DATATYPE_VALUES.put(EMDataType.EM_DATA_TYPE_PHOTOS, DATATYPE.IMAGE.value());
        DATATYPE_VALUES.put(EMDataType.EM_DATA_TYPE_VIDEO, DATATYPE.VIDEO.value());
        DATATYPE_VALUES.put(EMDataType.EM_DATA_TYPE_MUSIC, DATATYPE.AUDIO.value());
        DATATYPE_VALUES.put(EMDataType.EM_DATA_TYPE_APP, DATATYPE.APP.value());
        DATATYPE_VALUES.put(EMDataType.EM_DATA_TYPE_DOCUMENTS, DATATYPE.DOCUMENTS.value());
    }

    public static final String EXTERNAL_STORAGE_BACKUP_FOLDER = "BACKUP";
}
