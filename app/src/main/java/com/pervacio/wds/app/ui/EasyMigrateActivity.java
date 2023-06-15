/*************************************************************************
 *
 * Media Mushroom Limited CONFIDENTIAL
 * __________________
 *
 *  Copyright 2017 Media Mushroom Limited
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Media Mushroom Limited.
 *
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Media Mushroom Limited.
 */

package com.pervacio.wds.app.ui;

import static com.pervacio.wds.app.EMUtility.generateRandomString;
import static com.pervacio.wds.custom.appmigration.AppMigrateUtils.prepareMoviStarAppsListMap;
import static com.pervacio.wds.custom.utils.Constants.CLOUD_PAIRING_ENABLED;
import static com.pervacio.wds.custom.utils.Constants.COMPANY_NAME;
import static com.pervacio.wds.custom.utils.Constants.COUNTRY_NAME;
import static com.pervacio.wds.custom.utils.Constants.DST_NW_CHNGD;
import static com.pervacio.wds.custom.utils.Constants.EXCLUDE_SDCARD_MEDIA;
import static com.pervacio.wds.custom.utils.Constants.EXCLUDE_WHATSAPP_MEDIA;
import static com.pervacio.wds.custom.utils.Constants.FEEDBACK;
import static com.pervacio.wds.custom.utils.Constants.GET_PRELOADED_APPS_NAMES_FROM_SERVER;
import static com.pervacio.wds.custom.utils.Constants.IS_MMDS;
import static com.pervacio.wds.custom.utils.Constants.MIGRATION_CANCELLED;
import static com.pervacio.wds.custom.utils.Constants.MIGRATION_STARTED;
import static com.pervacio.wds.custom.utils.Constants.NOT_SELECTED;
import static com.pervacio.wds.custom.utils.Constants.P2P_MODE;
import static com.pervacio.wds.custom.utils.Constants.P2P_MODELS;
import static com.pervacio.wds.custom.utils.Constants.P2P_PROBLEMATIC_MODELS;
import static com.pervacio.wds.custom.utils.Constants.PLATFORM_ANDROID;
import static com.pervacio.wds.custom.utils.Constants.PLATFORM_BLACKBERRY;
import static com.pervacio.wds.custom.utils.Constants.REESTIMATION_REQUIRED;
import static com.pervacio.wds.custom.utils.Constants.SELECTED;
import static com.pervacio.wds.custom.utils.Constants.SRC_NW_CHNGD;
import static com.pervacio.wds.custom.utils.Constants.SUPPORTED_DATATYPE_MAP;
import static com.pervacio.wds.custom.utils.Constants.WDS_MIGRATION_STATUS;
import static com.pervacio.wds.custom.utils.Constants.mTransferMode;
import static com.pervacio.wds.custom.utils.Constants.moviStarAppsMap;
import static com.pervacio.wds.custom.utils.Constants.movistarappsList;
import static com.pervacio.wds.custom.utils.Constants.movistarappsListS3;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.provider.Telephony;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.pervacio.crashreportlib.LogReporting;
import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.CustomButton;
import com.pervacio.wds.CustomTextView;
import com.pervacio.wds.R;
import com.pervacio.wds.StateMachine;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMCommandDelegate;
import com.pervacio.wds.app.EMConfig;
import com.pervacio.wds.app.EMDataType;
import com.pervacio.wds.app.EMDeviceInfo;
import com.pervacio.wds.app.EMDeviceListDelegate;
import com.pervacio.wds.app.EMFileSendingProgressDelegate;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.app.EMMigrateStatus;
import com.pervacio.wds.app.EMPreviouslyTransferredContentRegistry;
import com.pervacio.wds.app.EMProgressHandler;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMRemoteDeviceManager;
import com.pervacio.wds.app.EMRemoteDeviceManagerDelegate;
import com.pervacio.wds.app.EMServer;
import com.pervacio.wds.app.EMStringConsts;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.EMUtilsDefaultSmsApp;
import com.pervacio.wds.app.EMWifiPeerConnector;
import com.pervacio.wds.app.HotspotServer;
import com.pervacio.wds.custom.CustomerRatingDialogActivity;
import com.pervacio.wds.custom.EmailSummaryActivity;
import com.pervacio.wds.custom.ReportAProblem;
import com.pervacio.wds.custom.SplashActivity;
import com.pervacio.wds.custom.ThankYouActivity;
import com.pervacio.wds.custom.appmigration.AppBackupModel;
import com.pervacio.wds.custom.appmigration.AppMigrateUtils;
import com.pervacio.wds.custom.appsinstall.AppSelectionChangedListener;
import com.pervacio.wds.custom.appsinstall.AppsInstallTabActivity;
import com.pervacio.wds.custom.asynctask.URLConnectionTask;
import com.pervacio.wds.custom.models.ContentDetails;
import com.pervacio.wds.custom.models.EDeviceSCSWrapper;
import com.pervacio.wds.custom.models.EDeviceSwitchSourceContentSummary;
import com.pervacio.wds.custom.models.FeatureConfig;
import com.pervacio.wds.custom.models.FeedbackSummary;
import com.pervacio.wds.custom.models.MigrationStats;
import com.pervacio.wds.custom.receivers.BatteryStatusBroadcastReceiver;
import com.pervacio.wds.custom.receivers.UninstallBroadcastReceiver;
import com.pervacio.wds.custom.service.CrashService;
import com.pervacio.wds.custom.service.ToastService;
import com.pervacio.wds.custom.service.TransactionLogService;
import com.pervacio.wds.custom.sms.MessagesXmlParser;
import com.pervacio.wds.custom.utils.AppUtils;
import com.pervacio.wds.custom.utils.CPServerCallBacks;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.ContentDetailsAdapter;
import com.pervacio.wds.custom.utils.ContentProgressDetailsAdapter;
import com.pervacio.wds.custom.utils.ContentSelectionInterface;
import com.pervacio.wds.custom.utils.ContextWrapperHelper;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.DeviceInfo;
import com.pervacio.wds.custom.utils.EstimationTimeUtility;
import com.pervacio.wds.custom.utils.InstallAppsSelectedInterface;
import com.pervacio.wds.custom.utils.NetworkUtil;
import com.pervacio.wds.custom.utils.PreferenceHelper;
import com.pervacio.wds.custom.utils.SelectedAppsDetailsAdapter;
import com.pervacio.wds.custom.utils.SelectedDataTypeDetailsAdapter;
import com.pervacio.wds.custom.utils.startLocationAlert;
import com.pervacio.wds.datawipe.RestrictionCheckActivity;
import com.pervacio.wds.sdk.CMDBackupAndRestoreEngine;
import com.pervacio.wds.sdk.CMDBackupAndRestoreServiceType;
import com.pervacio.wds.sdk.CMDError;
import com.pervacio.wds.sdk.internal.CMDCryptoSettings;
import com.pervacio.wds.sdk.internal.sdcard.CMDSDCardFileAccess;

import org.json.JSONException;
import org.json.JSONObject;
import org.pervacio.onediaglib.diagtests.TestGoogleAccounts;
import org.pervacio.onediaglib.diagtests.TestResult;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import pl.droidsonroids.gif.GifImageView;

public class EasyMigrateActivity extends AppCompatActivity implements EMRemoteDeviceManagerDelegate, EMDeviceListDelegate, EMProgressHandler, ContentSelectionInterface, CPServerCallBacks, WifiP2pManager.ConnectionInfoListener, InstallAppsSelectedInterface {

    Menu aMenu = null;
    static EMGlobals emGlobals = new EMGlobals();
    private boolean connectionInfoAvailable = false;
    private boolean wifiP2PInitDone = false;

    // Enums MUST match the order in which they occur in the main layout
    enum EPTWizardPage {
        Welcome,
        SelectDevice, // LAN device discovery mode only (non-QR code pairing)
        DisplayPin, // LAN device discovery mode only (non-QR code pairing)
        SelectContent,
        Progress,
        Complete,
        EnterPin,
        Connected,
        SelectCloudOrLocal,
        SelectBackupOrRestore,
        ConnectingToCloudService,
        SelectOldOrNewDevice,
        SelectOtherDeviceType,
        ScanQRCode,
        DisplayQRCode,
        SelectWiFiLANOrDirect,
        Connecting,
        About,
        SessionCancel,
        DataTypeInfo,
        endpage,
        CloudPairing,
        Permissions,
        SelectTransferType
    }

    enum EPTOperationMode {
        CloudBackup,
        CloudRestore,
        LocalWiFi
    }

    EPTOperationMode mOperationMode = EPTOperationMode.LocalWiFi; // Default to local WiFi - because we may get incoming requests from WiFi devices
    int mCloudServiceType; // CMDCloudServiceType
    WifiP2pManager mManager = null;
    WifiP2pManager.Channel mChannel = null;


    enum EPTState {
        EPTNone,
        EPTAuthenticatingWithCloud
    }

    EPTState mState;

    private static final int THIS_DEVICE_IS_SOURCE = 1;
    private static final int THIS_DEVICE_IS_TARGET = 2;

    private static final int APP_PERMISSIONS_REQUSET_CODE = 11;
    private static final int ALL_FILES_ACCESS_PERMISSION = 15;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 12;
    private static final int CUSTOMER_SATISFACTION_REQUEST_CODE = 19;
    private static final int P2P_PERMISSION_REQUEST_CODE = 21;
    private static final int SETTINGS_WRITE_PERMISSION_REQUEST_CODE = 2;
    private static final int CAMERA_PERMISSIONS_REQUSET_CODE = 3;
    private static final int GO_TO_SETTINGS_REQUEST_CODE = 4;
    private static final int APK_DOWNLOAD_RETRY_COUNT = 2;
    private int retried_count = 0;

    private int mRole;

    public static final String TAG = "EasyMigrate";

    private EMDeviceInfo mSelectedDevice;

    private boolean mFinishButtonEnabled = false;

    private TextView mPinLabel;

    private boolean mDeviceSelected = false;

    //By default keeping REVIEW_INPROGRESS
    public int migrationStatus = Constants.REVIEW_INPROGRESS;

    private static Field mGetSdkVersionField = null;

    String mSelectedAccountName;

    //size for media type and trnasfer time for data
    private long mAudioSize = 0;
    private long mVideoSize = 0;
    private long mImageSize = 0;
    private long mAppSize = 0;
    private long mDocumentsSize = 0;
    private long mSelectedContentSize = 0;
    private static final long STORAGE_THRESHOLD_VALUE = 200 * 1024; //200 MB of Threshold value.
    public long mBackupStartTime = 0;
    public long mRestoreEndTime = 0;
    public long mTransferTotalTime = 0;

    private int mContactsCount = 0;
    private int mCalendarsCount = 0;
    private int mMessageCount = 0;
    private int mPhotosCount = 0;
    private int mVideosCount = 0;
    private int mAudioCount = 0;
    private int mAppCount = 0;
    private int mCallLogsCount = 0;
    private int mSettingsCount = 0;
    private int mDocumentsCount = 0;

    private Button DataWipeButton;

    private boolean mContactsSelected = false;
    private boolean mCalendarsSelected = false;
    private boolean mMessageSelected = false;
    private boolean mPhotosSelected = false;
    private boolean mVideosSelected = false;
    private boolean mAudioSelected = false;
    private boolean mAppsSelected = false;
    private boolean mCallLogsSelected = false;
    private boolean mSettingsSelected = false;
    private boolean mDocumentsSelected = false;

    private long mLastClickTime = 0;

    private EMDeviceInfo mRemoteDeviceInfo = null;
    static Activity activity;
    static Context context;
    ImageView old_device, new_device, other_android, other_ios;
    private String dataTypeTitle = null;
    private StateMachine stateMachine;
    private EasyMigrateActivity.GpsListner gpsListner;
    private LocationManager locManager;
    private ServiceConnection connection;
    private NotificationManager mNotificationManager;

    private CheckBox content_select_whatsapp_media;
    private CheckBox content_select_sdcard_media;
    private boolean fromExcludeContent = false;
    private boolean mediaContentSelected = false;

    public static final int INSTALL_UNKNOWN_APPS_REQ_CODE = 500;
    //Map with key as time(month) and value as sms count from that point of time
    private final Map<Integer, Long> customSelectMessageMap = new LinkedHashMap<>();

    {
        customSelectMessageMap.put(1, 0L);//Last 1 month
        customSelectMessageMap.put(3, 0L);//Last 3 months
        customSelectMessageMap.put(6, 0L);//Last 6 months
        customSelectMessageMap.put(12, 0L);//Last 1 year
        customSelectMessageMap.put(24, 0L);//Last 2 years
        customSelectMessageMap.put(0, 0L);//All messages
    }

    AppSelectionChangedListener appSelectionChangedListener;

    private AmazonS3Client s3Client;
    private BasicAWSCredentials credentials;
    private Uri fileUri;
    String mFullPath;

    private static void initCompatibility() {
        try {
            mGetSdkVersionField = android.os.Build.VERSION.class.getField("SDK_INT");
            /* success, this is a newer device */
        } catch (Exception ex) {
            /* failure, must be older device */
        }
    }

    /**
     *
     **/
    @Override
    public void onDestroy() {
        DLog.log("onDestroy called");
        if (mNotificationManager != null) {
            mNotificationManager.cancel(0);
        }
        try {
            setContentView(R.layout.welcome); // Work around for strange Android 2.1 bug where ViewFlipper crashes when changing orientation
            if (Constants.IS_MMDS) {
                unregisterReceiver(uninstallBroadcastReceiver);
            } else {
                unregisterReceiver(batteryStatusBroadcastReceiver);
            }
        } catch (Exception ex) {
            // DLog.log(ex);
        }
        if (Constants.mTransferMode.equalsIgnoreCase(P2P_MODE)) {
            disableWifiDirect();
        }
        //When migration not completed successfully or abrupted we need to save migration details for resume
        if ((CommonUtil.getInstance().getMigrationStatus() == Constants.MIGRATION_INPROGRESS) && FeatureConfig.getInstance().getProductConfig().isEnablePauseAndResume()) {
            MigrationStats.getInstance().setElapsedTime(elapsedTimeInsec);
            EMUtility.saveMigrationStats(contentDetailsMap);
        }
        super.onDestroy();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////

	/*
	@Override
    public Object onRetainNonConfigurationInstance()
    {
        return mCurrentPage;
    }
    */

    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////

    Timer mOrientationTimer;
    int mCurrentOrientation;

    ArrayList<EPTWizardPage> mBackStack = new ArrayList<>();

    enum BackBehavior {
        EEnableBack,
        ECloseDataTransferActivityWithWarning,
        ECloseDataTransferActivitySilently,
        EForceCloseAppWithWarning
    }

    void optionToCloseApp() {
        DLog.log("In optionToCloseApp");
        if (cancelDialog != null) {
            cancelDialog.dismiss();
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_NEGATIVE:
                        DLog.log("Pressed negative button");
                        break;

                    case DialogInterface.BUTTON_POSITIVE:
                        DLog.log("Pressed positive button");
                        DLog.log("Sending MIGRATION_CANCELLED command");
                        //Send command only after the devices have been paired
                        if (!IS_MMDS && devicesPaired) {
                            sendCommandToRemote(Constants.MIGRATION_CANCELLED);
                            isMigrationInProgress = false;
                            showProgressDialog("", getString(R.string.cancelling_trasaction));
                            migrationInterrupted.sendEmptyMessageDelayed(MIGRATION_CANCELLED_EVENT, 20 * 1000);
                        } else if (mCurrentPage.name() == EPTWizardPage.Welcome.name()) {
                            initializeTransactionService(DashboardLog.getInstance().isThisDest());
//                            startOverTheApp();
//                            finish();
                            finish();
                            //startActivity(new Intent(EasyMigrateActivity.this, SplashActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        } else {
                            setLayout(EPTWizardPage.Welcome, EPTTransitionReason.UserBack);
                        }
                        cloudPairingInprogress = false;
                        addOverLayMessage(OVERLAY_MIGRATION_INPROGRESS, false);
                        mDisplayMMDSProgress = false;
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(getResources().getString(R.string.cancel_transfer));
        builder.setMessage(R.string.ept_option_to_close_text).setPositiveButton(R.string.str_yes, dialogClickListener)
                .setNegativeButton(R.string.str_no, dialogClickListener);


        cancelDialog = builder.create();
        cancelDialog.show();
    }

    private void startOverTheApp() {
        stopRunningServices();
        stopOrDisconnectFromAnyNetwork();
        if (!IS_MMDS && devicesPaired) {
            startService(new Intent(this, ToastService.class));
            restartApp();
        } else {
            try {
                PreferenceHelper.getInstance(this).putBooleanItem(Constants.CANCELLED_BEFORE_PAIRING, true);
                startActivity(new Intent(getApplicationContext(), EasyMigrateActivity.class));
                overridePendingTransition(0, 0);
            } catch (Exception e) {
                DLog.log(e.getMessage());
            }
        }
        clean();
    }

    private void stopRunningServices() {
        try {
            unbindService(connection);
        } catch (Exception e) {
            DLog.log("unbindService exception : " + e.getMessage());
        }
        try {
            stopService(new Intent(this, EasyMigrateService.class));
        } catch (Exception e) {
            DLog.log("stopService exception : " + e.getMessage());
        }
    }


    private void restartApp() {
        Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.START_OVER, true);
        int mPendingIntentId = new SecureRandom().nextInt();
        PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), mPendingIntentId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mgr.setExact(AlarmManager.RTC, Calendar.getInstance().getTimeInMillis() + 100, mPendingIntent);
        } else {
            mgr.set(AlarmManager.RTC, Calendar.getInstance().getTimeInMillis() + 100, mPendingIntent);
        }
        Process.killProcess(Process.myPid());
    }


    @Override
    public void onBackPressed() {
        if (mBackButton == null || !mBackButton.isEnabled() || cloudPairingInprogress) {
            return;
        }
        DLog.log("onBackPressed mCurrentPage " + mCurrentPage);
        BackBehavior backBehavior = BackBehavior.ECloseDataTransferActivitySilently;

        switch (mCurrentPage) {
            case About:
                backBehavior = BackBehavior.EEnableBack;
                break;
            case Complete:
                backBehavior = BackBehavior.ECloseDataTransferActivitySilently;
                break;

            case SelectDevice:
            case SelectCloudOrLocal:
            case SelectBackupOrRestore:
            case SelectOldOrNewDevice:
                backBehavior = BackBehavior.ECloseDataTransferActivityWithWarning;
                break;
            case CloudPairing:
                if (cloudPairing_scanner != null)
                    cloudPairing_scanner.pause();
                backBehavior = BackBehavior.EEnableBack;
                break;
            case SelectOtherDeviceType:
            case DataTypeInfo:
            case SelectWiFiLANOrDirect:
                backBehavior = BackBehavior.EEnableBack;
                break;
            case DisplayPin:
                backBehavior = BackBehavior.ECloseDataTransferActivityWithWarning;
                break;
            case ScanQRCode:
                backBehavior = BackBehavior.EEnableBack;
                if (barcodeView != null)
                    barcodeView.pause();
                break;
            case DisplayQRCode:
                backBehavior = BackBehavior.EEnableBack;
                EMNetworkManagerHostUIHelper.stopAnyExistingHostNetwork();
                break;
            case Progress:
            case ConnectingToCloudService:
            case Connecting:
            case Connected:
            case SelectTransferType:
            case EnterPin:
                backBehavior = BackBehavior.EForceCloseAppWithWarning;
                break;
            case SelectContent:
                if (FeatureConfig.getInstance().getProductConfig().isTransferTypeSelectionEnabled()) {
                    backBehavior = BackBehavior.EEnableBack;
                } else {
                    backBehavior = BackBehavior.EForceCloseAppWithWarning;
                }
                break;
            default:
                backBehavior = BackBehavior.ECloseDataTransferActivityWithWarning;

        }

        switch (backBehavior) {
            case EEnableBack:
                if (mBackStack.size() == 0)
                    forceCloseApp();
                else {
                    setLayout(mBackStack.get(mBackStack.size() - 1), EPTTransitionReason.UserBack);
                    mBackStack.remove(mBackStack.size() - 1);
                }
                break;
            case ECloseDataTransferActivityWithWarning:
                optionToCloseApp();
                break;
            case ECloseDataTransferActivitySilently:
                forceCloseApp();
                break;
            case EForceCloseAppWithWarning:
                optionToCloseApp();
                break;
        }
    }

    @Override
    public void onStop() {
       /* setWakeLock(false);
        setScreenOn(false);*/

       /* if(mRemoteDeviceManager!=null)
            mRemoteDeviceManager.resetMainSession();*/

		/*
		// TODO: this is a bit grim - instead we should try to close all threads and exit gracefully - or, just go back to the home screen when we restart
		if (mCurrentPage != EPTWizardPage.ConnectingToCloudService)
		{// Don't kill the process if we're on the cloud backup page - we're probably being stopped so the user can choose or create an account
			EMNetworkManagerHostUIHelper.stopAnyExistingHostNetwork();
			EMNetworkManagerClientUIHelper.disconnectFromAnyNetworks();
			android.os.Process.killProcess(android.os.Process.myPid());
		}
		*/
        DLog.log("onstop called : " + mCurrentPage.name());
        if (mCurrentPage == EPTWizardPage.ScanQRCode && null != barcodeView)
            barcodeView.pause();
        else if (mCurrentPage == EPTWizardPage.CloudPairing && null != cloudPairing_scanner) {
            cloudPairing_scanner.pause();
        }
        if (isMigrationInProgress) {
            handler.sendEmptyMessageDelayed(NOTIFICATION_MESSAGE, Constants.NOTIFICATION_TIME);
        }

        super.onStop();
    }

    PowerManager.WakeLock mWakeLock = null;
    PowerManager mPowerManager = null;
    WifiManager.WifiLock mWiFiLock = null;



    private void setWakeLock(boolean aOn) {
        try {
            if (aOn) {
                if (mWakeLock != null) {
                    mWakeLock.release();
                    mWakeLock = null;
                }
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        // | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                getWindow().setStatusBarColor(getResources().getColor(R.color.wds_colorPrimary));

                mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EPT");
                mWakeLock.acquire();

                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                mWiFiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "CMDWiFILockTag");
                mWiFiLock.acquire();
            } else {
                if (mWakeLock != null) {
                    mWakeLock.release();
                }

                if (mWiFiLock != null) {
                    mWiFiLock.release();
                    mWiFiLock = null;
                }
            }
        } catch (Exception ex) {
            // Ignore - probably releasing a wakelock that has already been released
        }
    }

    private void setScreenOn(boolean aOn) {
        try {
            if (aOn) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopOrDisconnectFromAnyNetwork() {
        DLog.log("stopOrDisconnectFromAnyNetwork");
        if (wiFiObserver != null) {
            try {
                unregisterReceiver(wiFiObserver);
            } catch (Exception e) {
                e.printStackTrace();
                forceCloseApp();
            }
        }
        new Thread() {
            @Override
            public void run() {
                super.run();
                if (mTransferMode.equalsIgnoreCase(P2P_MODE)) {
                    disableWifiDirect();
                    NetworkUtil.enableAllNetworks(true, connectedNetworkId);
                } else {
                    EMNetworkManagerHostUIHelper.stopAnyExistingHostNetwork();
                    EMNetworkManagerClientUIHelper.disconnectFromAnyNetworks();
                    connectToNetwork(connectedNetworkId);
                }
            }
        }.start();
    }

    private void forceCloseApp() {
        forceCloseApp(true);
    }

    private void forceCloseApp(boolean exitApp) {
        if (FeatureConfig.getInstance().getProductConfig().isEnablePauseAndResume() && (CommonUtil.getInstance().getMigrationStatus() == Constants.MIGRATION_INPROGRESS)) {
            if (elapsedTime != null)
                MigrationStats.getInstance().setElapsedTime(elapsedTimeInsec);
            EMUtility.saveMigrationStats(contentDetailsMap);
        }
        stopRunningServices();
        if (exitApp) {
//            DLog.log("***** About to system exit");
//            System.exit(0);
//            DLog.log("***** Done to system exit");


            try {
//                finishAffinity();
                DLog.log("***** About to system exit +++ ");
//                android.os.Process.killProcess(android.os.Process.myPid());
//                System.exit(0);
                finish();
                DLog.log("***** Done to system exit +++ ");
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

//		int id= android.os.Process.myPid();
//		android.os.Process.killProcess(id);
    }

    private void disableKeyguard() {
        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
            lock.disableKeyguard();
        } catch (Exception e) {
        }
    }

    //Register the handler to get notified about change in SMS permission
    private static PermissionHandler mSmsPermissionHandler = null;

    //Register the handler to get notified about change in Settings Write permission
    private static PermissionHandler mSettingsPermissionHandler = null;

    //Whether or not the permission was granted
    private static boolean messagePermissionGranted = false;

    /*
    Whether or not user has decided to make up his mind regarding SMS permission.
    If he grants permission in the first attempt, then all good. If not then after 2nd try,
    we'll set this variable to true indicating that the selection is done.
    */
    private static boolean userMadeMessageSelection = false;

    //To store if the first attempt was denied or not.
    private static boolean firstMessagePermissionDenied = false;

    /**
     * Utility method to get message permission if required.
     *
     * @param smsPermissionHandler To be passed only when you want to perform some action. Pass null
     *                             other cases.
     */
    public static void getDefaultSMSAppPermission(PermissionHandler smsPermissionHandler) {
        DLog.log("Messages permission required");
        additionalResponse = ";MessageWritePermission";
        if (isDefaultSMSApp()) {
            if (smsPermissionHandler != null)
                smsPermissionHandler.userAccepted();
            return;
        }
        //If selection has already been made, then just return.
        if (userMadeMessageSelection) {
            DLog.log("User has made selection");
            if (messagePermissionGranted) {
                DLog.log("messagePermissionGranted " + messagePermissionGranted);
                if (smsPermissionHandler != null)
                    smsPermissionHandler.userAccepted();
            } else {
                if (smsPermissionHandler != null)
                    smsPermissionHandler.userDenied();
            }
            return;
        }

        //If handler is not null means the call came after the backup file was received. Assign to handler.
        //Don't show prompt in this case.
        if (smsPermissionHandler != null && IS_MMDS) {
            DLog.log("smsPermissionHandler is not null");
            mSmsPermissionHandler = smsPermissionHandler;
            EMUtilsDefaultSmsApp emUtilsDefaultSmsApp = new EMUtilsDefaultSmsApp(context, activity);
            emUtilsDefaultSmsApp.becomeDefaultSmsApp();
        } else if (smsPermissionHandler != null) {
            DLog.log("smsPermissionHandler is not null");
            mSmsPermissionHandler = smsPermissionHandler;
        }
        //It is null so it came after the migration was just started. So show the prompt.
        else {
            EMUtilsDefaultSmsApp emUtilsDefaultSmsApp = new EMUtilsDefaultSmsApp(context, activity);
            emUtilsDefaultSmsApp.becomeDefaultSmsApp();
        }
    }

    UninstallBroadcastReceiver uninstallBroadcastReceiver;
    BatteryStatusBroadcastReceiver batteryStatusBroadcastReceiver;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        DLog.log("EasyMigrateActivity::onCreate");
        DLog.log("enter EasyMigrateActivity::onCreate AppMigrateUtils.restoreAppList.size " + AppMigrateUtils.restoreAppList.size());

        CMDCryptoSettings.initialize();
        EMMigrateStatus.initialize();

        EMNetworkManagerClientUIHelper.reset();
        EMNetworkManagerHostUIHelper.reset();

        CLOUD_PAIRING_ENABLED = false;

//        AWSMobileClient.getInstance().initialize(this).execute();
//        credentials = new BasicAWSCredentials(Constants.SERVER_KEY, Constants.SERVER_SECRET);
//        s3Client = new AmazonS3Client(credentials);
	/*	 // Moved to service
		EMGlobals.initialize();
	*/
        //EMMigrateStatus.setQrCodeWifiDirectMode(EMConfig.QR_CODE_MODE_BY_DEFAULT);

        // TODO: temp logging
        DLog.log("*** CMDCryptoSettings.enabled(): " + CMDCryptoSettings.enabled());

        super.onCreate(savedInstanceState);

        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        initCompatibility();
        disableKeyguard();
        setWakeLock(true);
        setScreenOn(true);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.back);
        }

//        if (Constants.IS_MMDS) {
//            SUPPORT_P2P_MODE = false;
//            IntentFilter usbStatusHandler = new IntentFilter();
//            usbStatusHandler.addAction(Intent.ACTION_POWER_CONNECTED);
//            usbStatusHandler.addAction(Intent.ACTION_POWER_DISCONNECTED);
//            uninstallBroadcastReceiver = new UninstallBroadcastReceiver(EasyMigrateActivity.this);
//            registerReceiver(uninstallBroadcastReceiver, usbStatusHandler);
//        }
//        mCurrentPage = (EPTWizardPage) getLastNonConfigurationInstance();
//        if (mCurrentPage == null) {
        mCurrentPage = EPTWizardPage.Welcome;
           /* if (EMConfig.SKIP_WELCOME_SCREEN) {
                mCurrentPage = EPTWizardPage.SelectOldOrNewDevice;
            }
            else {
                mCurrentPage = EPTWizardPage.Welcome;
            }*/
//        }
        setDefaultModes();  //Sets the default TransferMode.
        EMUtility.checkforProblematicModel();

        try {
            if (Constants.SUPPORT_P2P_MODE) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
                registerReceiver(wiFiObserver, intentFilter);
            }
            mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
            mChannel = mManager.initialize(this, getMainLooper(), null);

            String apiLevel = String.valueOf(Build.VERSION.SDK_INT);
            DLog.log(TAG + " apiLevel " + apiLevel);
            CommonUtil.getInstance().setDeviceAPILevel(apiLevel);

            // Setting P2P device address start

            if (P2P_PROBLEMATIC_MODELS.contains(DeviceInfo.getInstance().get_model()) && (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)) {
                String randomString = generateRandomString(16);
                Method m = null;
                final String newP2PDeviceName = randomString;
                DLog.log("Geneared random secure string for p2pdevice name " + newP2PDeviceName);
                try {
                    DLog.log("calling setDeviceName");
                    m = mManager.getClass().getMethod("setDeviceName", new Class[]{mChannel.getClass(), String.class,
                            WifiP2pManager.ActionListener.class});
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                try {
                    m.invoke(mManager, mChannel, newP2PDeviceName, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            DLog.log("onSuccess newP2PDeviceName");
                            CommonUtil.getInstance().setDeviceNameP2P(newP2PDeviceName);
                        }

                        @Override
                        public void onFailure(int reason) {
                            DLog.log("onFailure setDeviceName");
                        }
                    });
                } catch (IllegalAccessException e) {
                    DLog.log("Setting P2P  Device Name exception 1: " + e);
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    DLog.log("Setting P2P  Device Name exception 2: " + e);
                    e.printStackTrace();
                }
            } else {
                DLog.log(DeviceInfo.getInstance().get_model() + " not in P2P_PROBLEMATIC_MODELS or OS is grater than android 10");
            }
            // Setting P2P device address end

        } catch (Exception e) {
            DLog.log("Exception in initialize P2P Manager " + e);
        }


        if (Build.VERSION.SDK_INT >= 30) {
            DLog.log("Android 11 are WIFI PROBLAMATIC MODELS : " + Build.MODEL);
            Constants.WIFI_DIRECT_PROBLAMATIC_MODELS.add(Build.MODEL);
        }
        /*
		// Log.d(TAG, "EPT: Main activity - wait for synccomplete");
		Intent intent = new Intent(this, EPTScannerServer.class);
		startService(intent);
		// Log.d(TAG, "EPT: waiting for synccomplete");
		*/

        mOutToLeft = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
        );
        mOutToLeft.setDuration(350);
        mOutToLeft.setInterpolator(new AccelerateInterpolator());

        mInFromRight = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, +1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
        );
        mInFromRight.setDuration(350);
        mInFromRight.setInterpolator(new AccelerateInterpolator());

        mInFromLeft = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
        );
        mInFromLeft.setDuration(350);
        mInFromLeft.setInterpolator(new AccelerateInterpolator());

        mOutToRight = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, +1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
        );
        mOutToRight.setDuration(350);
        mOutToRight.setInterpolator(new AccelerateInterpolator());

        mFadeOut = new AlphaAnimation(1.0f, 0.0f);
        mFadeOut.setDuration(350);

        mFadeIn = new AlphaAnimation(0.0f, 1.0f);
        mFadeIn.setDuration(350);

        setContentView(R.layout.main);
        TextView app_version = (TextView) findViewById(R.id.version);
        TextView welcomeText = (TextView) findViewById(R.id.WelcomeTopText);
        Typeface custom_font = Typeface.createFromAsset(getAssets(), "fonts/roboto_bold.ttf");
        welcomeText.setTypeface(custom_font);
//        app_version.setText(BuildConfig.LIBRARY_PACKAGE_NAME);
        app_version.setText("ORUphones");

        mFlipper = (ViewFlipper) findViewById(R.id.MainViewFlipper);

        mNextButton = (Button) this.findViewById(R.id.NextButton);

        //btn_email_summary = (Button)this.findViewById(R.id.email_summary_button);
        // mNextButton.requestFocus();

        OnClickListener nextButtonOnClickListener = new OnClickListener() {
            // @Override
            public void onClick(View v) {
                DLog.log("callback default smsapp nextButtonOnClickListener onClick");
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    DLog.log("callback default smsapp nextButtonOnClickListener onClick return");
                    return;
                }
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    mRestoreOriginalSMSApp = false;
                }

                mLastClickTime = SystemClock.elapsedRealtime();
                if (!mFinishButtonEnabled) {
                    DLog.log("callback default smsapp nextButtonOnClickListener onClick calling usernext");
                    userNext();
                } else if (!IS_MMDS && mRestoreOriginalSMSApp) {
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        DLog.log("callback default smsapp nextButtonOnClickListener onClick not calling restoreOriginalSmsApp");
                    } else {
                        DLog.log("callback default smsapp nextButtonOnClickListener onClick calling restoreOriginalSmsApp");
                        EMUtilsDefaultSmsApp emUtilsDefaultSmsApp = new EMUtilsDefaultSmsApp(context, activity);
                        emUtilsDefaultSmsApp.restoreOriginalSmsApp();
                    }
                } else {
                    if (mCommandHandlerForME != null)
                        mCommandHandlerForME.cancel(true);

                    if (!hasFeatureAtTheEnd() && FeatureConfig.getInstance().getProductConfig().isUninstallRequired()) {
                        UninstallBroadcastReceiver.deleteUninstallAlarm(context);
                        setLayout(EPTWizardPage.endpage, EPTTransitionReason.UserNext);
//                    }
//                    else if (!Constants.IS_MMDS && !BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT)) {
//                        DLog.log("Setting the preference finished click true");
//                        if (hasFeatureAtTheEnd()) {
//                            proceedWithTheFeature(true);
//                        } else {
//                            PreferenceHelper.getInstance(context).putBooleanItem(Constants.PREF_FINISH_CLICKED, true);
//                            stopOrDisconnectFromAnyNetwork();
//                            UninstallBroadcastReceiver.startUninstallAlarm(context);
//                            forceCloseApp();
//                        }
                    } else {
                        DLog.log("callback default smsapp nextButtonOnClickListener onClick calling hasFeatureAtTheEnd");
                        if (hasFeatureAtTheEnd()) {
                            proceedWithTheFeature(true);
                        } else {
                            PreferenceHelper.getInstance(context).putBooleanItem(Constants.PREF_FINISH_CLICKED, true);
                            stopOrDisconnectFromAnyNetwork();
                            forceCloseApp();
                        }
                    }

                }
            }
        };











        //
        // /*



        DataWipeButton=findViewById(R.id.datawipebutton);
        DataWipeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                showWipeConfirmDialog();

            }
        });



        //
        // */















        OnClickListener finishButtonOnClickListener = new OnClickListener() {
            // @Override
            public void onClick(View v) {
                DLog.log("before calling uninstallation alarm; " + hasFeatureAtTheEnd());
                if (hasFeatureAtTheEnd()) {
                    proceedWithTheFeature(true);
                } else {
                    UninstallBroadcastReceiver.startUninstallAlarm(context);
                    PreferenceHelper.getInstance(context).putBooleanItem(Constants.PREF_FINISH_CLICKED, true);
                    forceCloseApp();
                }
            }
        };

        OnClickListener sendEmailButtonOnClickListener = new OnClickListener() {
            // @Override
            public void onClick(View v) {
                DLog.log("enter sendEmailButtonOnClickListener onClick");
                Intent emailSumamryIntent = new Intent(EasyMigrateActivity.this, EmailSummaryActivity.class);
                startActivity(emailSumamryIntent);
            }
        };


        OnClickListener welcomePageNextClick = new OnClickListener() {
            @Override
            public void onClick(View view) {
                MigrationStats migrationStats = EMUtility.getLastTransactionDetails();
                if (migrationStats != null)
                    DLog.log("migrationStats :" + migrationStats + " " + migrationStats.isMigrationCompleted() + " " + migrationStats.getRemoteDevicePlatform());
                    if (!IS_MMDS && FeatureConfig.getInstance().getProductConfig().isEnablePauseAndResume() && migrationStats != null && !migrationStats.isMigrationCompleted() && PLATFORM_ANDROID.equalsIgnoreCase(migrationStats.getRemoteDevicePlatform())) {
                        MigrationStats.setMigrationStats(migrationStats);
                        EMMigrateStatus.setmInstance(migrationStats.getMigrateStatus());
                        resumeTransactionDialog();
                    } else {
                        EMUtility.clearTransaction();
                    }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkAndRequestPermissions(false)) {
                        if (Settings.System.canWrite(EasyMigrateActivity.this)) {
                            setLayout(EPTWizardPage.SelectOldOrNewDevice, EPTTransitionReason.UserNext);
                        } else {
                            settingsPermissionDialog(context.getResources().getString(R.string.str_write_permission), true);
                        }
                    }
                } else {
                    setLayout(EPTWizardPage.SelectOldOrNewDevice, EPTTransitionReason.UserNext);
                }
            }
        };

        OnClickListener exitFromAppClick = new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FeatureConfig.getInstance().getProductConfig().isCustomerSatisfactionEnabled()) {
                    Intent intent = new Intent(EasyMigrateActivity.this, CustomerRatingDialogActivity.class);
                    intent.putExtra("dontCheckEndFeature", true);
                    startActivityForResult(intent, CUSTOMER_SATISFACTION_REQUEST_CODE);
                } else {
                    forceCloseApp();
                }
            }
        };

        OnClickListener startOverClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                startOverTheApp();
            }
        };

        appSelectionChangedListener = new AppSelectionChangedListener() {

            @Override
            public void onAppSelectionChanged() {
                int toalAppsCount = AppMigrateUtils.backupAppList.size();
                int totalSelectedAppsCount = AppMigrateUtils.totalAppCount;
                DLog.log("enter onAppSelectionChanged toalAppsCount " + toalAppsCount + " totalSelectedAppsCount " + totalSelectedAppsCount);
                if (totalSelectedAppsCount < toalAppsCount) {
                    content_select_all_checkbox.setChecked(false);
                } else {
                    content_select_all_checkbox.setChecked(true);
                }
            }
        };

        mNextButton.setOnClickListener(nextButtonOnClickListener);
        welcomeNext = (Button) findViewById(R.id.done_tv);
        exitFromApp = (Button) findViewById(R.id.exit_tv);
        startOver = (Button) findViewById(R.id.start_over_tv);
//        if (FLAVOUR_SPRINT.equalsIgnoreCase(BuildConfig.FLAVOR)) {
//            startOver.setVisibility(View.VISIBLE);
//        }
        welcomeNext.setOnClickListener(welcomePageNextClick);
        exitFromApp.setOnClickListener(exitFromAppClick);
        startOver.setOnClickListener(startOverClickListener);

        //btn_email_summary.setOnClickListener(sendEmailButtonOnClickListener);


        aboutAccpet = (TextView) findViewById(R.id.accept_tv);
        aboutCancel = (TextView) findViewById(R.id.cancel_tv);
        finishpage = (Button) findViewById(R.id.finish_page);
        aboutAccpet.setOnClickListener(aboutPageOnlickListner);
        aboutCancel.setOnClickListener(aboutPageOnlickListner);
        finishpage.setOnClickListener(finishButtonOnClickListener);
        aboutVersionNumber = (TextView) findViewById(R.id.version_number);
//        aboutVersionNumber.setText(getString(R.string.version_number) + " : " + BuildConfig.LIBRARY_PACKAGE_NAME);
        aboutVersionNumber.setText(getString(R.string.version_number) + " : " + "V1.0.23");
        aboutreleaseDate = (TextView) findViewById(R.id.build_release_date);
        aboutreleaseDate.setText(getString(R.string.build_release_date) + " : " + BuildConfig.BUILD_DATE);
        aboutPrivacyPolicy = (TextView) findViewById(R.id.privacy_policy);
        aboutPrivacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());
        aboutFeedback = (EditText) findViewById(R.id.structured_edittext_answer);

        cloudPairing_scanner_Layout = (LinearLayout) findViewById(R.id.cloudpairing_scanner);
        cloudPairing_QR_Layout = (LinearLayout) findViewById(R.id.cloudpairing_qr);
        cloudPairing_scanner = (DecoratedBarcodeView) findViewById(R.id.cp_barcode_scanner);
        cloudPairing_scanner.setStatusText(getString(R.string.qr_code_scan_message));
        cloudPairing_QR = (ImageView) findViewById(R.id.cp_qr_code_image_view);
        cameraproblem = (TextView) findViewById(R.id.cameraproblem);
        cameraproblem.setMovementMethod(LinkMovementMethod.getInstance());
        session_pin = (TextView) findViewById(R.id.session_pin);
        enterPinSuggestion(cameraproblem);
        rating_bar = (RatingBar) findViewById(R.id.rating_bar);

        excludeSDCardMedia_Layout = (LinearLayout) findViewById(R.id.contentCheckLayoutSDCardMedia);
        excludeWhatsAppMedia_Layout = (LinearLayout) findViewById(R.id.contentCheckLayoutWhatsAppMedia);
        mBackButton = (Button) this.findViewById(R.id.BackButton);

        mBackButton.setOnClickListener(new OnClickListener() {
            // @Override
            public void onClick(View v) {
                userBack();
            }
        });

        mDeviceListView = (ListView) this.findViewById(R.id.deviceListView);

        mDeviceListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                                    long arg3) {
                showProgressDialog(getString(R.string.ept_connecting_to_device_title),
                        getString(R.string.ept_connecting_to_device_text));

                connectToTargetDevice(mRemoteDeviceManager.mRemoteDeviceList.mRemoteDevices.get(position));
            }
        });

        mPinLabel = (TextView) this.findViewById(R.id.pinLabel);

        contentDetailsView = (RecyclerView) findViewById(R.id.contentDetails_list);
        contentDetailsSummaryView = (RecyclerView) findViewById(R.id.contentDetailsSummary_list);
        contentDetailsProgressView = (RecyclerView) findViewById(R.id.contentDetailsProgress_list);
        dataTypeDetailsView = (RecyclerView) findViewById(R.id.datatypeDetails_list);
        deniedPermissionsView = (RecyclerView) findViewById(R.id.denied_perm_list);
        ll_content_select_all = findViewById(R.id.id_ll_content_select_all);


        storageProgressbar = (ProgressBar) findViewById(R.id.storagespaceIndicator);
        storageSuggestion = (TextView) findViewById(R.id.storageSuggestion);
        storageMessage = (TextView) findViewById(R.id.storageSpace);
        elapsedTime = (TextView) findViewById(R.id.elapsedTime);
        estimationTime = (TextView) findViewById(R.id.estimationTime);
        grantPermission = (TextView) findViewById(R.id.grant_perm_tv);
        skip = (Button) findViewById(R.id.skip_tv);

        grantPermission.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndRequestPermissions(false);
            }
        });

        skip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setLayout(EPTWizardPage.SelectOldOrNewDevice, EPTTransitionReason.UserNext);

            }
        });


        mainLayout = (RelativeLayout) findViewById(R.id.MainScrollViewLayout);
        mmds_progressLayout = (RelativeLayout) findViewById(R.id.progressLayout);
        mmds_connetingLayout = (RelativeLayout) findViewById(R.id.pairingInprogress);
        barcodeView = (DecoratedBarcodeView) findViewById(R.id.barcode_scanner);
        barcodeView.setStatusText(getString(R.string.qr_code_scan_message));
        barcodeView.getStatusView().setPadding(5, 0, 0, 2);
        setLayout(mCurrentPage, EPTTransitionReason.StartUp);

        final EasyMigrateActivity thisActivity = this;
        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                WebView hiddenSrpWebview = (WebView) findViewById(R.id.hidden_srp_webview);
                EMGlobals.setJavascriptWebView(hiddenSrpWebview);

                EasyMigrateService.LocalBinder binder = (EasyMigrateService.LocalBinder) service;
                EasyMigrateService easyMigrateService = binder.getService();
                if (mRemoteDeviceManager != null) {

                    mRemoteDeviceManager = easyMigrateService.getRemoteDeviceManager();

                    try {
                        mRemoteDeviceManager.resetDeviceList();
                    }
                    catch (Exception e)
                    {
                        Log.d(TAG, "onServiceConnected: "+e.toString());
                    }
                    try {
                        mRemoteDeviceManager.setDelegate(thisActivity);
                    }
                    catch (Exception e)
                    {
                        Log.d(TAG, "onServiceConnected: "+e.toString());
                    }
                    try {
                        mRemoteDeviceManager.mRemoteDeviceList.setDelegate(thisActivity);
                    }
                    catch (Exception e)
                    {
                        Log.d(TAG, "onServiceConnected: "+e.toString());
                    }
                    try {
                        deviceListChanged();
                    }
                    catch (Exception e)
                    {
                        Log.d(TAG, "onServiceConnected: "+e.toString());
                    }

                } // Force a refresh of the device list
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        if (!CLOUD_PAIRING_ENABLED && !IS_MMDS) {
            Intent intent = new Intent(this, EasyMigrateService.class);
            startService(intent);
            getApplicationContext().bindService(intent, connection, this.BIND_AUTO_CREATE);
        }

        mTotalTransferTime = (TextView) this.findViewById(R.id.timeTakenToTransfer);
        mUniqueTransactionId = (TextView) this.findViewById(R.id.uniqueTransactionId);
        mSmsSuggestionText = (TextView) this.findViewById(R.id.mediaNotTransferText);
        selectContentsuggestion = (TextView) this.findViewById(R.id.selectContentsuggestion);
        selectContentTimeSuggestion = (TextView) this.findViewById(R.id.selectContentTimesuggestion);
        if (!DeviceInfo.getInstance().isTouchPhone()) {
            selectContentTimeSuggestion.setVisibility(View.GONE);
        }
        btn_start_migration = (Button) findViewById(R.id.start_migration);
        btn_cancel = (TextView) findViewById(R.id.btn_cancel);
        btn_summary_finish = (TextView) findViewById(R.id.summary_finish_button);
//        btn_switch_scan = (TextView) findViewById(R.id.switch_scan);
        sessionId = (TextView) findViewById(R.id.sessionId);
        selectContentSessionIdSource = (TextView) findViewById(R.id.sourceSelectContentSessionId);
        sessionIdConnectedScreen = (TextView) findViewById(R.id.connectedSessionId);
        btn_start_migration.setOnClickListener(nextButtonOnClickListener);
        btn_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        btn_summary_finish.setOnClickListener(nextButtonOnClickListener);
//        btn_switch_scan.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                displaySwitchDialog();
//            }
//        });

        if (IS_MMDS) {
            btn_summary_finish.setVisibility(View.GONE);
            btn_cancel.setVisibility(View.GONE);
            app_version.setVisibility(View.VISIBLE);
        }

        Button localCopyButton = (Button) this.findViewById(R.id.localCopyButton);
        localCopyButton.setOnClickListener(new OnClickListener() {
            // @Override
            public void onClick(View v) {
                if (!displayWiFiWarningIfWiFiIssue(true, true)) {
                    EMMigrateStatus.setQrCodeWifiDirectMode(false);
                    mOperationMode = EPTOperationMode.LocalWiFi;
                    setLayout(EPTWizardPage.SelectOldOrNewDevice, EPTTransitionReason.UserNext);
                }
            }
        });

        Button wifiDirectButton = (Button) this.findViewById(R.id.wifiDirectCardButton);
        wifiDirectButton.setOnClickListener(new OnClickListener() {
            // @Override
            public void onClick(View v) {
                if (!displayWiFiWarningIfWiFiIssue(true, false)) {
                    EMMigrateStatus.setQrCodeWifiDirectMode(true);
                    mOperationMode = EPTOperationMode.LocalWiFi;
                    setLayout(EPTWizardPage.SelectOldOrNewDevice, EPTTransitionReason.UserNext);
                }
            }
        });

        Button wifiLocalOrDirectButton = (Button) this.findViewById(R.id.localCopyLANorDirectButton);
        wifiLocalOrDirectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setLayout(EPTWizardPage.SelectWiFiLANOrDirect, EPTTransitionReason.UserNext);
            }
        });

        Button googleDriveButton = (Button) this.findViewById(R.id.googleDriveButton);

        if (!EMConfig.ENABLE_GOOGLE_DRIVE) {
            googleDriveButton.setVisibility(View.GONE);
        }

        googleDriveButton.setOnClickListener(new OnClickListener() {
            // @Override
            public void onClick(View v) {
                if (!displayWiFiWarningIfWiFiIssue(true, true)) {
                    EMMigrateStatus.setTransportMode(EMProgressInfo.EMTransportMode.EM_CLOUD);
                    setLayout(EPTWizardPage.SelectBackupOrRestore, EPTTransitionReason.UserNext);
                }
            }
        });

        Button sdCardButton = (Button) this.findViewById(R.id.sdCardButton);

        if (!EMConfig.ENABLE_SD_CARD) {
            sdCardButton.setVisibility(View.GONE);
        }

        context = this;
        sdCardButton.setOnClickListener(new OnClickListener() {
            // @Override
            public void onClick(View v) {
                EMMigrateStatus.setTransportMode(EMProgressInfo.EMTransportMode.EM_SD_CARD);

                if (!CMDSDCardFileAccess.sdCardExists(context))
                    raiseFatalError(getString(R.string.cmd_no_sd_card_title), getString(R.string.cmd_no_sd_card_body));
                else {
                    // Change text on page to the SD card string
                    // TextView titleText = (TextView) findViewById(R.id.TopText);
                    // titleText.setText(R.string.cmd_sd_card_options_title);

                    TextView optionsText = (TextView) findViewById(R.id.CloudBackupAndRestoreOptionsText1);
                    optionsText.setText(R.string.cmd_sd_card_options_text);

                    Button backupButton = (Button) findViewById(R.id.cloudBackupButton);
                    backupButton.setText(R.string.cmd_sd_card_backup_button_text);

                    Button restoreButton = (Button) findViewById(R.id.cloudRestoreButton);
                    restoreButton.setText(R.string.cmd_sd_card_restore_button_text);

                    setLayout(EPTWizardPage.SelectBackupOrRestore, EPTTransitionReason.UserNext);
                }
            }
        });

        activity = this;

        Button cloudBackupButton = (Button) this.findViewById(R.id.cloudBackupButton);
        cloudBackupButton.setOnClickListener(new OnClickListener() {
            // @Override
            public void onClick(View v) {
                if (EMMigrateStatus.getTransportMode() == EMProgressInfo.EMTransportMode.EM_CLOUD) {
                    mCloudServiceType = CMDBackupAndRestoreServiceType.CMD_GOOGLE_DRIVE;
                    mOperationMode = EPTOperationMode.CloudBackup;
                    mState = EPTState.EPTAuthenticatingWithCloud;
                    TextView infoTextView = (TextView) findViewById(R.id.SelectContentBottomText);
                    infoTextView.setText(getString(R.string.cmd_select_content_to_back_up));
                    authenticateWithCloudService();
                } else if (EMMigrateStatus.getTransportMode() == EMProgressInfo.EMTransportMode.EM_SD_CARD) {
                    CMDSDCardFileAccess testSdCardAccess = new CMDSDCardFileAccess(context);
                    if (testSdCardAccess.itemExistsBlocking("/" + EMStringConsts.EM_BACKUP_FOLDER_NAME, new EMProgressHandler() {
                        @Override
                        public void taskComplete(boolean aSuccess) {
                        }

                        @Override
                        public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
                        }

                        @Override
                        public void progressUpdate(EMProgressInfo aProgressInfo) {
                        }
                    })) {
                        raiseFatalError(getString(R.string.cmd_sd_card_backup_already_exists_title), getString(R.string.cmd_sd_card_backup_already_exists_body));
                        return;
                    }

                    mCloudServiceType = CMDBackupAndRestoreServiceType.CMD_SD_CARD;
                    mOperationMode = EPTOperationMode.CloudBackup;
                    mCloudService = new CMDBackupAndRestoreEngine(CMDBackupAndRestoreServiceType.CMD_SD_CARD, activity, activity);
                    setLayout(EPTWizardPage.SelectContent, EPTTransitionReason.UserNext);
                } else {
                    // TODO: WTF?
                }
            }
        });

        Button cloudRestoreButton = (Button) this.findViewById(R.id.cloudRestoreButton);
        cloudRestoreButton.setOnClickListener(new OnClickListener() {
            // @Override
            public void onClick(View v) {
                if (EMMigrateStatus.getTransportMode() == EMProgressInfo.EMTransportMode.EM_CLOUD) {
                    mCloudServiceType = CMDBackupAndRestoreServiceType.CMD_GOOGLE_DRIVE;
                    mOperationMode = EPTOperationMode.CloudRestore;
                    mState = EPTState.EPTAuthenticatingWithCloud;
                    TextView infoTextView = (TextView) findViewById(R.id.SelectContentBottomText);
                    infoTextView.setText(getString(R.string.cmd_select_content_to_restore));
                    authenticateWithCloudService();
                } else if (EMMigrateStatus.getTransportMode() == EMProgressInfo.EMTransportMode.EM_SD_CARD) {
                    CMDSDCardFileAccess testSdCardAccess = new CMDSDCardFileAccess(context);
                    if (!testSdCardAccess.itemExistsBlocking("/" + EMStringConsts.EM_BACKUP_FOLDER_NAME, new EMProgressHandler() {
                        @Override
                        public void taskComplete(boolean aSuccess) {
                        }

                        @Override
                        public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
                        }

                        @Override
                        public void progressUpdate(EMProgressInfo aProgressInfo) {
                        }
                    })) {
                        raiseFatalError(getString(R.string.cmd_no_sd_card_backup_title), getString(R.string.cmd_no_sd_card_backup_body));
                    } else {
                        mCloudServiceType = CMDBackupAndRestoreServiceType.CMD_SD_CARD;
                        mOperationMode = EPTOperationMode.CloudRestore;
                        mCloudService = new CMDBackupAndRestoreEngine(CMDBackupAndRestoreServiceType.CMD_SD_CARD, activity, activity);
                        setLayout(EPTWizardPage.SelectContent, EPTTransitionReason.UserNext);
                    }
                } else {
                    // TODO: WTF?
                }
            }
        });

        mPinEntryBox = (EditText) this.findViewById(R.id.PinEditText);
        mPinEntryBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    userNext();
                }
                return false;
            }
        });

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(BUNDLE_INFO_CURRENT_SCREEN)
                    && savedInstanceState.containsKey(BUNDLE_INFO_CLOUD_SERVICE_TYPE)
                    && savedInstanceState.containsKey(BUNDLE_INFO_OPERATION_TYPE_KEY)) {
                // Log.d(TAG, "Restoring from saved state");
                mCurrentPage = EPTWizardPage.values()[savedInstanceState.getInt(BUNDLE_INFO_CURRENT_SCREEN)];
                mCloudServiceType = savedInstanceState.getInt(BUNDLE_INFO_CLOUD_SERVICE_TYPE);
                mSelectedAccountName = savedInstanceState.getString(BUNDLE_INFO_SELECTED_ACCOUNT_NAME);
                mOperationMode = EPTOperationMode.values()[savedInstanceState.getInt(BUNDLE_INFO_OPERATION_TYPE_KEY)];
                // Log.d(TAG, "About to set restored layout:");
                setLayout(mCurrentPage, EPTTransitionReason.Automatic);
                // Log.d(TAG, "Layout restored");
            }
        }

        if (EMConfig.ENABLE_ADS)
            showAds();
        else {
           /* AdView adView = (AdView) findViewById(R.id.adView);
            adView.setVisibility(View.GONE);*/
        }

        thisIsSourceDeviceButton = (Button) findViewById(R.id.old_device_button);
        thisIsSourceDeviceButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                DLog.log("Source device selected");
                isDeviceSelectionDone = true;
                AppMigrateUtils.clearBackupAppsList();
                mRole = THIS_DEVICE_IS_SOURCE;
                stateMachine = StateMachine.getInstance(EasyMigrateActivity.this, mRole);
                CommonUtil.getInstance().setSource(true);
                stateMachine.clean();
                if (DashboardLog.getInstance().geteDeviceSwitchSession().getDeviceSwitchSessionId() != null) {
                    initializeTransactionService(DashboardLog.getInstance().isThisDest());
                }
                DashboardLog.getInstance().setThisDest(false);
                DashboardLog.getInstance().geteDeviceSwitchSession().setSourceDeviceInfoId(DeviceInfo.getInstance().getDeviceInfo(true));
                DashboardLog.getInstance().geteDeviceSwitchSession().setDestinationDeviceInfoId(null);
//                loadContentDetails();
                if (!IS_MMDS) {
                    if (CLOUD_PAIRING_ENABLED) {
                        setLayout(EPTWizardPage.CloudPairing, EPTTransitionReason.UserNext);
                        showProgressDialog("", getString(R.string.please_wait)); // check again
                        //satya add download call here
                        Log.i(TAG, " FeatureConfig.getInstance().getProductConfig().isEnableAppTransfer() " + FeatureConfig.getInstance().getProductConfig().isEnableAppTransfer());
                        if (FeatureConfig.getInstance().getProductConfig().isEnableAppTransfer() && (movistarappsList.size() != 0)) {
                            //  showProgressDialog("","Downloading Preloaded Applications from server...."); // check again
                            prepareMoviStarAppsListMap();
                            int appCount = 0;
                            for (Map.Entry mapElement : moviStarAppsMap.entrySet()) {
                                appCount = appCount + 1;
                                String apkS3Name = (String) mapElement.getKey();
                                String apkName = (String) mapElement.getValue();
                                if (AppMigrateUtils.checkAppPresence(apkName)) {
                                    Log.i(TAG, "downloadFile appsPresenceCheck s3APKName app already present : " + apkS3Name + " : apkName : " + apkName);
                                    Log.i(TAG, "downloadFile movistarappsListS3  before remove size : " + movistarappsListS3.size());
                                    appCount = appCount - 1;
                                    if (movistarappsListS3.size() != 0) {
                                        movistarappsListS3.remove(appCount);
                                        Log.i(TAG, "downloadFile appsPresenceCheck s3APKName app already present and removed : " + apkS3Name + " : apkName : " + apkName);

                                        Log.i(TAG, "downloadFile movistarappsListS3  size : " + movistarappsListS3.size());
                                    }
                                }
                            }
                            if (movistarappsListS3.size() != 0) {
                                showProgressDialog("", "Downloading Preloaded Applications from server...."); // check again
                                DLog.log("downloadFile calling case 1");
                                String fileNameS3 = movistarappsListS3.get(0);
//                                downloadFile(movistarappsListS3.get(0));
                                DLog.log("downloadFile calling case 1 fileNameS3 " + fileNameS3);
                                downloadFile(fileNameS3);
                            } else {
                                stateMachine.createSession();
                            }
                        } else {
                            DLog.log("Calling create");
                            showProgressDialog("", getString(R.string.please_wait));
                            stateMachine.createSession();
                        }
                    } else {
                        if (EMMigrateStatus.qrCodeWifiDirectMode()) {
                            if (Constants.PLATFORM.equalsIgnoreCase(Constants.PLATFORM_BLACKBERRY)) {
                                setLayout(EPTWizardPage.ScanQRCode, EPTTransitionReason.UserNext);
                            } else {
                                setLayout(EPTWizardPage.SelectOtherDeviceType, EPTTransitionReason.UserNext);
                            }
                        } else {
                            setLayout(EPTWizardPage.SelectDevice, EPTTransitionReason.UserNext);
                        }
                    }
                }
                loadContentDetails();

            }
        });

        otherDeviceIsIOSButton = (Button) findViewById(R.id.old_device_is_ios_button);
        final TextView iosQRCodeInfo1 = (TextView) findViewById(R.id.ios_qr_code_info_1_textview);
        final TextView iosQRCodeInfo2 = (TextView) findViewById(R.id.ios_qr_code_info_2_textview);
        final TextView displayQRCodeTitle = (TextView) findViewById(R.id.display_qr_code_title);

        old_device = (ImageView) findViewById(R.id.old_device_image);
        new_device = (ImageView) findViewById(R.id.new_device_image);

        thisIsDestinationDeviceButton = (Button) findViewById(R.id.new_device_button);
        if (Constants.PLATFORM.equalsIgnoreCase(PLATFORM_BLACKBERRY)) {
            thisIsDestinationDeviceButton.setEnabled(false);
            thisIsDestinationDeviceButton.setAlpha(0.5f);
            new_device.setEnabled(false);
        }
        thisIsDestinationDeviceButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                DLog.log("Destination device selected");
                if (Constants.mTransferMode.equalsIgnoreCase(P2P_MODE)) {
                    wifiP2PInitDone = false;
                    discoverPeers();
                }
                isDeviceSelectionDone = true;
                mRole = THIS_DEVICE_IS_TARGET;
                CommonUtil.getInstance().setSource(false);
                DashboardLog.getInstance().geteDeviceSwitchSession().setSourceDeviceInfoId(null);
                DashboardLog.getInstance().geteDeviceSwitchSession().setDestinationDeviceInfoId(DeviceInfo.getInstance().getDeviceInfo(false));
                stateMachine = StateMachine.getInstance(EasyMigrateActivity.this, mRole);
                stateMachine.clean();
                loadContentDetails();
                DashboardLog.getInstance().setThisDest(true);
                iosQRCodeInfo1.setVisibility(View.GONE);
                iosQRCodeInfo2.setVisibility(View.GONE);
//                btn_switch_scan.setVisibility(View.INVISIBLE);
                //Once dest device got selected we are Calling Dashboard api to get session ID
                DashboardLog.getInstance().setThisDest(true);
                DashboardLog.getInstance().updateSessionStatus(Constants.SESSION_STATUS.NOT_STARTED);
                DashboardLog.getInstance().geteDeviceSwitchSession().setSessionStage(Constants.SESSION_STAGE.APP_LAUNCHED.value());
                DashboardLog.getInstance().updateToServer(true);
                if (!IS_MMDS) {
                    if (CLOUD_PAIRING_ENABLED) {
                        setLayout(EPTWizardPage.CloudPairing, EPTTransitionReason.UserNext);
                    } else {
                        if (EMMigrateStatus.qrCodeWifiDirectMode()) {
                            if (Constants.PLATFORM.equalsIgnoreCase(Constants.PLATFORM_BLACKBERRY)) {
                                setLayout(EPTWizardPage.ScanQRCode, EPTTransitionReason.UserNext);
                            } else {
                                if (IS_MMDS) {
                                    setLayout(EPTWizardPage.SelectOtherDeviceType, EPTTransitionReason.UserNext);
                                } else {
                                    setLayout(EPTWizardPage.DisplayQRCode, EPTTransitionReason.UserNext);
                                }
                            }
                        } else {
                            setLayout(EPTWizardPage.DisplayPin, EPTTransitionReason.UserNext);
                        }
                    }
                }
            }
        });
        otherDeviceIsIOSButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
//                iosQRCodeInfo1.setVisibility(View.VISIBLE);
//                iosQRCodeInfo2.setVisibility(View.VISIBLE);
////                btn_switch_scan.setVisibility(View.GONE);
//                DashboardLog.getInstance().convertDevicesInfoToDB();
//                if (!CLOUD_PAIRING_ENABLED)
//                    setLayout(EPTWizardPage.DisplayQRCode, EPTTransitionReason.UserNext);
//                if (IS_MMDS || CLOUD_PAIRING_ENABLED) {
//                    //In case of MMDS, there's no need to ask user to Scan QR Code
//                    displayQRCodeTitle.setText(getString(R.string.ept_page_title_default));
//                    iosQRCodeInfo1.setText(getString(R.string.cmd_ios_qr_code_info_3));
//                }
                Toast.makeText(thisActivity, "Coming Soon...", Toast.LENGTH_SHORT).show();
            }
        });

        otherDeviceIsAndroidButton = (Button) findViewById(R.id.old_device_is_android_button);
        otherDeviceIsAndroidButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                iosQRCodeInfo1.setVisibility(View.GONE);
                iosQRCodeInfo2.setVisibility(View.GONE);
//                btn_switch_scan.setVisibility(View.INVISIBLE);
                if (mRole == THIS_DEVICE_IS_TARGET) {
                    setLayout(EPTWizardPage.DisplayQRCode, EPTTransitionReason.UserNext);
                } else
                    setLayout(EPTWizardPage.ScanQRCode, EPTTransitionReason.UserNext);

            }
        });
        other_android = (ImageView) findViewById(R.id.otherdevice_android);
        other_ios = (ImageView) findViewById(R.id.otherdevice_ios);
        old_device.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                thisIsSourceDeviceButton.callOnClick();
            }
        });
        new_device.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                thisIsDestinationDeviceButton.callOnClick();
            }
        });

        other_android.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                otherDeviceIsAndroidButton.callOnClick();
            }
        });
        other_ios.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                otherDeviceIsIOSButton.callOnClick();
            }
        });


        if (IS_MMDS) {
            //initMMDS();
            startMMDSThread();
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            EMUtility.makeActionOverflowMenuShown(this);
        }

//        if (COMPANY_MOBILECOPY.equalsIgnoreCase(COMPANY_NAME) || (BuildConfig.FLAVOR.equalsIgnoreCase(FLAVOUR_SPRINT))) {
//            locManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
//            getLatLong();
//        }
        if (FeatureConfig.getInstance().getProductConfig().isExcludeSDCardMediaEnabled()) {
            excludeSDCardMedia_Layout.setVisibility(View.VISIBLE);
            content_select_sdcard_media = (CheckBox) this.findViewById(R.id.sdCardMediaIncludeCheck);
            content_select_sdcard_media.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fromExcludeContent = true;
                    CheckBox item = (CheckBox) view;
                    DLog.log("sdCardMediaIncludeCheck.............." + item.isChecked());
                    boolean excludeMediaFiles = !item.isChecked();
                    //DeviceInfo.getInstance().setExcludesdCardMedia(excludeMediaFiles);
                    EXCLUDE_SDCARD_MEDIA = excludeMediaFiles;
                    String alertMessage;
                    if (excludeMediaFiles) {
                        alertMessage = getString(R.string.str_sdcard_media_exclude_alert);
                    } else {
                        alertMessage = getString(R.string.str_sdcard_media_include_alert);
                    }
                    showExcludeAlertDialog(alertMessage, getString(R.string.please_wait));
                }
            });
        } else {
            excludeSDCardMedia_Layout.setVisibility(View.GONE);
        }

        if (FeatureConfig.getInstance().getProductConfig().isExcludeWhatsAppMediaEnabled()) {
            excludeWhatsAppMedia_Layout.setVisibility(View.VISIBLE);
            content_select_whatsapp_media = (CheckBox) this.findViewById(R.id.whatsAppMediaIncludeCheck);
            content_select_whatsapp_media.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fromExcludeContent = true;
                    CheckBox item = (CheckBox) view;
                    DLog.log("whatsAppMediaIncludeCheck.............." + item.isChecked());
                    boolean excludeMediaFiles = !item.isChecked();
                    EXCLUDE_WHATSAPP_MEDIA = excludeMediaFiles;
                    String alertMessage;
                    if (excludeMediaFiles) {
                        alertMessage = getString(R.string.str_whatsapp_media_exclude_alert);
                    } else {
                        alertMessage = getString(R.string.str_whatsapp_media_include_alert);
                    }
                    showExcludeAlertDialog(alertMessage, getString(R.string.please_wait));
                }
            });
        } else {
            excludeWhatsAppMedia_Layout.setVisibility(View.GONE);
        }
    }

    private void showExcludeAlertDialog(String alertTitle, final String progressTitle) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        AlertDialog alertDialog = null;
        alertDialogBuilder.setTitle(getString(R.string.str_alert));
        alertDialogBuilder
                .setMessage(alertTitle)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ept_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (dialog != null)
                            dialog.dismiss();

                        loadContentDetails();
                        showProgressDialog("", progressTitle);
                    }
                });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public boolean initMMDS() {
        boolean status = false;
        DLog.log("initMMDS..............");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkAndRequestPermissions(false)) {
                if (Settings.System.canWrite(EasyMigrateActivity.this)) {
                    DLog.log("Write Permission granted..............++");
                    //setLayout(EPTWizardPage.SelectOldOrNewDevice, EPTTransitionReason.UserNext);
                    //startMMDSThread();
                    status = true;
                } else {
                    DLog.log("Write Permission not granted..............Asking now");
                    settingsPermissionDialog(context.getResources().getString(R.string.str_write_permission), true);
                    status = false;
                }
            } else {
                status = false;
            }
        } else {
            DLog.log("Permissions not required for below Marshmallow devices...");
            status = true;
        }
        return status;
    }

    public void startMMDSThread() {
        DLog.log("startMMDSThread..............");
        CLOUD_PAIRING_ENABLED = false;
        Constants.LOGGING_ENABLED = false;
        FeatureConfig.getInstance().getProductConfig().setUninstallRequired(false);
        mCommandHandlerForME = new CommandHandlerForME();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            mCommandHandlerForME.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            mCommandHandlerForME.execute();
    }

    public OnClickListener aboutPageOnlickListner = new OnClickListener() {
        @Override
        public void onClick(View view) {

            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (view.getId() == R.id.accept_tv) {
                sendFeedback(rating_bar.getRating(), aboutFeedback.getText().toString());
            }
            if (mBackStack.size() == 0)
                forceCloseApp();
            else {
                setLayout(mBackStack.get(mBackStack.size() - 1), EPTTransitionReason.Automatic);
                mBackStack.remove(mBackStack.size() - 1);
            }
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        String storedLocale = PreferenceHelper.getInstance(newBase).getStringItem("locale");
        if (TextUtils.isEmpty(storedLocale)) {
            storedLocale = FeatureConfig.getInstance().getProductConfig().getDefaultLanguage();
        }
        DLog.log("Read locale in EasyMigrateActivity: " + storedLocale);
        super.attachBaseContext(ContextWrapperHelper.wrap(newBase, storedLocale));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem aMenuItem) {
        int itemId = aMenuItem.getItemId();// If home icon is clicked return to main Activity
        if (itemId == android.R.id.home) {
            if (mBackButton != null && mBackButton.isEnabled())
                onBackPressed();
        } else if (itemId == R.id.about) {//show about screen
            setLayout(EPTWizardPage.About, EPTTransitionReason.Automatic);
            aMenu.clear();
        }

        return true;
    }

    private void connectToTargetDevice(EMDeviceInfo aDeviceInfo) {
        if (!mDeviceSelected) {
            mDeviceSelected = true;
            mSelectedDevice = aDeviceInfo;
            mRemoteDeviceManager.selectRemoteDevice(aDeviceInfo);
            mRemoteDeviceManager.connectToRemoteDevice();
            mRemoteDeviceManager.remoteToBecomeTarget();
            mRole = THIS_DEVICE_IS_SOURCE;
        }
    }

    void showAds() {
        /*if (EMConfig.ENABLE_ADS) {
            AdView adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice("0144D092CF211FC036D0E174240D5C69") // Test device (Media Mushroom Nexus 5x)
                    .build();
            adView.loadAd(adRequest);
            adView.setVisibility(View.VISIBLE);
        }*/
    }

    void hideAds() {
      /*  AdView adView = (AdView) findViewById(R.id.adView);
        adView.destroy();
        adView.setVisibility(View.INVISIBLE);*/
    }

    boolean displayWiFiWarningIfWiFiIssue(boolean aRequireWiFiOn, boolean aRequireWiFiConnected) {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        boolean wifiProblem = false;

        if (aRequireWiFiOn && !wifiIsOn()) {
            wifiProblem = true;
            raiseFatalError(getString(R.string.ept_wifi_is_off_title), getString(R.string.ept_wifi_is_off_text));
        } else if (aRequireWiFiConnected && !mWifi.isConnected()) {
            wifiProblem = true;
            raiseFatalError(getString(R.string.ept_connect_to_wifi_title), getString(R.string.ept_connect_to_wifi_message));
        }

        return wifiProblem;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////

    boolean fileExists(String aPath, String aFileName) {
        boolean fileAlreadExists = true;

        try {
            File root = Environment.getExternalStorageDirectory();
            File msyncDir = new File(root, aPath);
            File androidFile = new File(msyncDir, aFileName);
            if (!androidFile.exists()) {
                fileAlreadExists = false;
            }

        } catch (Exception e) {
            fileAlreadExists = false;
        }

        if (!fileAlreadExists) {
            try {
                File root = Environment.getExternalStorageDirectory();
                File samsungSd = new File(root, "sd");
                File msyncDir = new File(samsungSd, aPath);
                File androidFile = new File(msyncDir, aFileName);
                if (androidFile.exists()) {
                    fileAlreadExists = true;
                }

            } catch (Exception e) {
                fileAlreadExists = false;
            }
        }

        return fileAlreadExists;
    }

    // Determine if the uniqueif file already exists
    // If it does, then it's a good indicator that this phone has already been set up
    boolean uniqueIdExists() {
        boolean fileAlreadyExists = fileExists("MSYNC", "uniqueid");
        return fileAlreadyExists;
    }

    boolean plusEnabledPCSoftware() {
        boolean fileAlreadyExists = fileExists("MSYNC", "pcversion.txt");
        return fileAlreadyExists;
    }

    boolean alreadyUpgraded() {
        boolean fileAlreadyExists = fileExists("MSYNC", "upgraded.txt");
        return fileAlreadyExists;
    }

    private boolean isWifiOff() {
        return !wifiIsOn();
    }

    private boolean wifiIsOn() {
        if (EMConfig.ALLOW_CONTINUE_WITH_WIFI_OFF) {
            return true;
        } else {
            boolean isWifiEnabled = false;
            try {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null) {
                    isWifiEnabled = wifiManager.isWifiEnabled();
                }
            } catch (Exception e) {
                DLog.log(e.getMessage());
            }
            DLog.log("isWifiOn >> " + isWifiEnabled);
            return isWifiEnabled;
        }
    }

    private void enableWifi(boolean state) {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            boolean currentstate = wifiIsOn();
            if (currentstate != state) {
                wifiManager.setWifiEnabled(state);
            }
        } catch (Exception e) {
            DLog.log("" + e.getMessage());
        }
    }

    private boolean reconnecting = false;

    private boolean connectToNetwork(int networkId) {
        if (reconnecting || networkId == -1) {
            return false;
        }
        boolean reconnected = false;
        try {
            reconnecting = true;
            boolean currentstate = wifiIsOn();
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                if (!currentstate) {
                    wifiManager.setWifiEnabled(true);
                }
                if (wifiManager.getConnectionInfo() != null && wifiManager.getConnectionInfo().getNetworkId() != networkId) {
                    DLog.log("Connecting to network : " + networkId);
                    wifiManager.disconnect();
                }
                wifiManager.enableNetwork(networkId, true);
                while (!reconnected) {
                    reconnected = wifiManager.reconnect();
                }
            }
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
        reconnecting = false;
        return reconnected;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////

    public void userNext() {
        switch (mCurrentPage) {
            case Welcome:
                setLayout(EPTWizardPage.SelectDevice, EPTTransitionReason.UserNext);
//                setLayout(EPTWizardPage.SelectCloudOrLocal, EPTTransitionReason.UserNext);
                break;

            case SelectContent:
                setScreenOn(true);
                setWakeLock(true);
                getSelectedDataTypes();

                if (!Constants.IS_MMDS)
                    registerBatteryStatusBroadcastReceiver();

                if (isAvailableSpaceOnDestination(mSelectedContentSize / 1024)) {
                    showProgressDialog("", getString(R.string.processing));
                    DLog.log("Sending MIGRATION_STARTED to destination");

                    List<EDeviceSwitchSourceContentSummary> srcSummaryList = getSourceContentSummaryDetails();
                    EDeviceSCSWrapper eDeviceSCSWrapper = new EDeviceSCSWrapper();
                    eDeviceSCSWrapper.setEDeviceSwitchSourceContentSummaryCollection(
                            srcSummaryList.toArray(new EDeviceSwitchSourceContentSummary[srcSummaryList.size()]));
                    eDeviceSCSWrapper.setEstimationTime(String.valueOf(estimateTransferTime(false)));
                    if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).isSelected()) {
                        String additionalInfo = "Messages Selected from : " + EMUtility.getDateFromMillis(CommonUtil.getInstance().getMessageSelectionFrom());
                        eDeviceSCSWrapper.setAdditionalInfo(additionalInfo);
                    }

                    //Convert the array to JSON
                    Gson gson = new Gson();
                    String summaryJson = gson.toJson(eDeviceSCSWrapper, EDeviceSCSWrapper.class);
                    DLog.log("Summary JSON: " + summaryJson);

                    //Append with the command with # as delimiter
                    sendCommandToRemote(Constants.MIGRATION_STARTED + "#" + summaryJson);
                    mBackupStartTime = System.currentTimeMillis();
                    if ((metadataThread.getState() == Thread.State.NEW) && !metaDataPrepared)
                        metadataThread.start();

                } else {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    AlertDialog alertDialog = null;
                    alertDialogBuilder.setTitle(getString(R.string.insufficient_space));
                    alertDialogBuilder
                            .setMessage(getString(R.string.ept_could_not_copy_data_message))
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.ept_ok), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    if (dialog != null)
                                        dialog.dismiss();
                                }
                            });
                    alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
                break;

            case EnterPin:
                CMDCryptoSettings.setPassword(mPinEntryBox.getText().toString());
                showProgressDialog(getString(R.string.checking_pin_title),
                        getString(R.string.checking_pin_text));
                mRemoteDeviceManager.setCryptoPassword(mPinEntryBox.getText().toString());
                break;

//    	case Disconnect:
//       		setLayout(EPTWizardPage.PCInfo, EPTTransitionReason.UserNext);
//    		break;

        }
    }

    private void updateSelectedDataTypes(int dataType, boolean isSelected) {
        contentDetailsMap.get(dataType).setSelected(isSelected);
        updateStorageProgress();
        if (FeatureConfig.getInstance().getProductConfig().isExcludeWhatsAppMediaEnabled()) {
            boolean whatsApppresent = AppMigrateUtils.checkAppPresence("WhatsApp");
            DLog.log("enter updateSelectedDataTypes whatsApppresent " + whatsApppresent);
            if (whatsApppresent) {
                if (EXCLUDE_WHATSAPP_MEDIA) {
                    if (!mediaContentSelected) {
                        content_select_whatsapp_media.setEnabled(false);
                    } else {
                        content_select_whatsapp_media.setEnabled(true);
                    }
                    content_select_whatsapp_media.setChecked(false);
                    content_select_whatsapp_media.setTextColor(getResources().getColor(R.color.black));
                } else {
                    if ((!mediaContentSelected)) {
                        content_select_whatsapp_media.setChecked(false);
                        content_select_whatsapp_media.setTextColor(getResources().getColor(R.color.black));
                        content_select_whatsapp_media.setEnabled(false);
                    } else {
                        content_select_whatsapp_media.setChecked(true);
                        content_select_whatsapp_media.setTextColor(getResources().getColor(R.color.black));
                        content_select_whatsapp_media.setEnabled(true);
                    }
                }
            }
        }
    }

    private void updateSelectedApps(int dataType, boolean isSelected) {
        contentDetailsMap.get(dataType).setSelected(isSelected);
        //updateStorageProgress();
    }

    private int getSelectedDataTypes() {
        int dataTypesTemp = 0;
        mSelectedContentSize = 0;

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CONTACTS).isSelected()) {
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_CONTACTS;
            mContactsSelected = true;
            DLog.log("In getSelectedDataTypes.............EM_DATA_TYPE_CONTACTS");
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).isSelected()) {
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_SMS_MESSAGES;
            DLog.log("In getSelectedDataTypes.............EM_DATA_TYPE_SMS_MESSAGES");
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).isSelected()) {
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_CALENDAR;
            DLog.log("In getSelectedDataTypes.............EM_DATA_TYPE_CALENDAR");
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALL_LOGS).isSelected()) {
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_CALL_LOGS;
            DLog.log("In getSelectedDataTypes.............EM_DATA_TYPE_CALL_LOGS");
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SETTINGS).isSelected()) {
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_SETTINGS;
            DLog.log("In getSelectedDataTypes.............EM_DATA_TYPE_SETTINGS");
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).isSelected()) {
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_PHOTOS;
            mSelectedContentSize += mImageSize;
            DLog.log("In getSelectedDataTypes.............EM_DATA_TYPE_PHOTOS");
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).isSelected()) {
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_VIDEO;
            mSelectedContentSize += mVideoSize;
            DLog.log("In getSelectedDataTypes.............EM_DATA_TYPE_VIDEO");
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).isSelected()) {
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_MUSIC;
            mSelectedContentSize += mAudioSize;
            DLog.log("In getSelectedDataTypes.............EM_DATA_TYPE_MUSIC");
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).isSelected()) {
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_APP;
            mSelectedContentSize += mAppSize;
            DLog.log("In getSelectedDataTypes.............EM_DATA_TYPE_APP");
        }
//        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_DOCUMENTS).isSelected()) {
//            dataTypesTemp |= EMDataType.EM_DATA_TYPE_DOCUMENTS;
//            mSelectedContentSize += mDocumentsSize;
//            DLog.log("In getSelectedDataTypes.............EM_DATA_TYPE_DOCUMENTS");
//        }
        return dataTypesTemp;
    }

    void sendData() // TODO: change to sendGetData because we could be restoring from a backup
    {
        final int dataTypes = getSelectedDataTypes();
        MigrationStats.getInstance().setSelectedDataTypes(dataTypes);
        MigrationStats.getInstance().setDataTypesTobeCompleted(dataTypes);
        CommonUtil.getInstance().setMigrationStatus(Constants.MIGRATION_INPROGRESS);
        isMigrationInProgress = true;
        elapsedTime.setVisibility(View.VISIBLE);
        elapsedTimer.start();
        if (Constants.REESTIMATION_REQUIRED) {
            remainingTimeList.clear();
            remainingTimeList.add(CommonUtil.getInstance().getInitialEstimation());
            if (mSelectedContentSize == 0) {
                handler.sendEmptyMessageDelayed(REESTIMATE_TIME, ESTIMATION_INTERVAL);
            } else {
                reEstimationTimer.start();
            }
        }
        if (mOperationMode == EPTOperationMode.LocalWiFi) {
            DLog.log("Previously transferred? " + mRemoteDeviceManager.anyItemsPreviouslyTransferred());
            mRemoteDeviceManager.clearPreviouslyTransferredItems(); // Deleting previous transferred Items.
            if (mRemoteDeviceManager.anyItemsPreviouslyTransferred()) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_NEGATIVE:
                                mRemoteDeviceManager.sendData(dataTypes);
                                break;

                            case DialogInterface.BUTTON_POSITIVE:
                                mRemoteDeviceManager.clearPreviouslyTransferredItems();
                                mRemoteDeviceManager.sendData(dataTypes);
                                break;
                            default:
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(false);
                builder.setMessage(R.string.ept_retransfer_previous_content_dialog_text).setPositiveButton(R.string.ept_transfer_again, dialogClickListener)
                        .setNegativeButton(R.string.ept_skip_it, dialogClickListener);


                final AlertDialog alert = builder.create();
                alert.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button posButton = alert.getButton(DialogInterface.BUTTON_POSITIVE);
                        Button negButton = alert.getButton(DialogInterface.BUTTON_NEGATIVE);

                        LinearLayout.LayoutParams posParams = (LinearLayout.LayoutParams) posButton.getLayoutParams();
                        posParams.weight = 1;
                        posParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;

                        LinearLayout.LayoutParams negParams = (LinearLayout.LayoutParams) negButton.getLayoutParams();
                        negParams.weight = 1;
                        negParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;

                        posButton.setLayoutParams(posParams);
                        negButton.setLayoutParams(negParams);
                    }
                });
                alert.show();
            } else {
                // No data has been previously transferred to this device, so resend it
                DLog.log("No data has been prviously transfered, so resend it");
                mRemoteDeviceManager.sendData(dataTypes);
                handler.sendEmptyMessageDelayed(UPDATE_PROGRESS, 1 * 1000);
            }
        }
//        else {
//            if (mOperationMode == EPTOperationMode.CloudBackup) {
//                // TODO: temp backup name
//                mCloudService.createBackupWithDataFromDeviceAsync(EMStringConsts.EM_BACKUP_FOLDER_NAME, dataTypes, this);
//            } else if (mOperationMode == EPTOperationMode.CloudRestore) {
//                // TODO: temp backup name
//                boolean cloudRestore = (EMMigrateStatus.getTransportMode() == EMProgressInfo.EMTransportMode.EM_CLOUD);
//                mCloudService.restoreDeviceDataFromBackupAsync(EMStringConsts.EM_BACKUP_FOLDER_NAME, dataTypes, this, cloudRestore);
//            }
//        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////


    public void userBack() {
        if (mBackButton.getText().equals(this.getString(R.string.ept_exit_button))) {
            forceCloseApp();
        }

        switch (mCurrentPage) {
            case SelectCloudOrLocal:
                setLayout(EPTWizardPage.Welcome, EPTTransitionReason.UserBack);
                break;
            case SelectDevice:
                setLayout(EPTWizardPage.SelectCloudOrLocal, EPTTransitionReason.UserBack);
                break;
            case SelectBackupOrRestore:
                setLayout(EPTWizardPage.SelectCloudOrLocal, EPTTransitionReason.UserBack);
                break;
            case SelectOldOrNewDevice:
                setLayout(EPTWizardPage.SelectCloudOrLocal, EPTTransitionReason.UserBack);
                break;
            case SelectOtherDeviceType:
                setLayout(EPTWizardPage.SelectOldOrNewDevice, EPTTransitionReason.UserBack);
                break;
//    	case Disconnect:
//    	case SDCardProblem:
//    		setLayout(EPTWizardPage.Welcome, EPTTransitionReason.UserBack);
//    		break;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////

    private static ProgressDialog mProgressDialog;

    private void showProgressDialog(String aTitle, String aText) {
        try {
            if (mProgressDialog != null) {
                mProgressDialog.hide();
                mProgressDialog = null;
            }

            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setTitle(aTitle);
            mProgressDialog.setMessage(aText);
            ;
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelable(false);
            if (!mDisplayMMDSProgress) {
                DLog.log("showProgressDialog with message: " + aText);
                mProgressDialog.show();
            }
        } catch (Exception e) {
            DLog.log("Exception in showProgressDialog: " + e.getMessage());
        }
    }

    public static void hideProgressDialog() {
        try {
            if (mProgressDialog != null) {
                DLog.log("hideProgressDialog");
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        } catch (Exception e) {
            DLog.log("Exception in hideProgressDialog: " + e.getMessage());
        }
    }


    public void usbCableDisconnectionHandler(int type, boolean show) {
        if (IS_MMDS && !show && !devicesPaired) {
            addOverLayMessage(OVERLAY_MIGRATION_INPROGRESS, mDisplayMMDSProgress);
        } else {
            addOverLayMessage(type, show);
        }
        if (show) {
            migrationInterrupted.sendEmptyMessageDelayed(DEVICE_DISCONNECTED_EVENT, 45 * 1000);
        } else {
            migrationInterrupted.removeMessages(DEVICE_DISCONNECTED_EVENT);
        }
    }

    ProgressDialog mSearchingForDevicesProgressDialog;

    public void setLayout(EPTWizardPage aNewLayout, EPTTransitionReason aTransitionReason) {
        DLog.log("Enter setLayout " + aNewLayout.name());
        hideProgressDialog();

        if ((aNewLayout != mCurrentPage) && (aTransitionReason != EPTTransitionReason.UserBack)) {
            mBackStack.add(mCurrentPage);
        }

        if (aMenu != null && aMenu.size() == 0) {
            getMenuInflater().inflate(R.menu.menu, aMenu);
        }
//        ActionBar actionBar = getSupportActionBar();
//        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2C2F44")));
//        actionBar.setTitle(getString(R.string.ept_page_title_default));
        if ((IS_MMDS && (aNewLayout != EPTWizardPage.DataTypeInfo)) || cloudPairingInprogress)
//            actionBar.setDisplayHomeAsUpEnabled(false);
//        else
//            actionBar.setDisplayHomeAsUpEnabled(true);
//        String title = getString(R.string.ept_page_title_default);
            if (aNewLayout == EPTWizardPage.SessionCancel || aNewLayout == EPTWizardPage.Welcome) {
//            if (BuildConfig.FLAVOR.equalsIgnoreCase(FLAVOUR_SPRINT)) {
//                actionBar.setHomeAsUpIndicator(R.drawable.logo_small);
//            } else {
//                actionBar.setDisplayHomeAsUpEnabled(false);
//            }
            } else {
//            actionBar.setHomeAsUpIndicator(R.drawable.back);
            }

        switch (aNewLayout) {

            case SelectContent:
//                title = getString(R.string.ept_page_title_select_content);
                setSessionId(selectContentSessionIdSource, false);
                break;
            case Progress:
                if (mRole == THIS_DEVICE_IS_SOURCE) {
//                    title = getString(R.string.sending_data);
                    setSessionId(sessionId, false);
                } else {
//                    title = getString(R.string.receiving_data);
                    setSessionId(sessionId, true);
                }
                break;
            case Complete:
                DLog.log("SatyaTest" + "enter Complete case AppMigrateUtils.restoreAppList size " + AppMigrateUtils.restoreAppList.size());
                //disable bluetooth if it is enabled
//                if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
//                    BluetoothAdapter.getDefaultAdapter().disable();
//                }
                //disable location if it is enabled
//                if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//                    locManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
//                }
//                title = getString(R.string.ept_page_title_summary);
//                actionBar.setDisplayHomeAsUpEnabled(false);
                break;
            case EnterPin:
            case DisplayPin:
//                title = getString(R.string.ept_page_title_confirm_pin);
                break;
            case SelectWiFiLANOrDirect:
            case SelectOldOrNewDevice:
//                title = getString(R.string.ept_page_title_select_device);
                mRole = 0;
                break;
            case SelectDevice:
//                title = getString(R.string.ept_page_title_select_device);
                break;
            case SelectBackupOrRestore:
//                title = getString(R.string.ept_page_title_backup_or_restore);
                break;
            case Welcome:
                if (aMenu != null) {
                    aMenu.clear();
                }
//                title = getString(R.string.app_name);
                break;
            case Connected:
//                title = getString(R.string.ept_connected_title);
                setSessionId(sessionIdConnectedScreen, false);
                break;
            case SelectCloudOrLocal:
            case ConnectingToCloudService:
            case SelectOtherDeviceType:
//                title = getString(R.string.cmd_local_wifi_or_cloud_title);
                break;
            case ScanQRCode:
//                title = getString(R.string.ept_page_title_scan_barcode);
                break;
            case DisplayQRCode:
//                title = getString(R.string.ept_page_title_scan_barcode);
                break;
            case Connecting:
//                title = getString(R.string.ept_page_title_default);
                break;
            case About:
//                title = getString(R.string.about);
                break;
            case SessionCancel:
//                title = getString(R.string.app_name);
                if (aMenu != null) {
                    aMenu.clear();
                }
                break;
            case endpage:
//                title = getString(R.string.app_name);
//                actionBar.setDisplayHomeAsUpEnabled(false);
                mEndPage_Suggestion = (TextView) findViewById(R.id.step);
                if (mRole == THIS_DEVICE_IS_SOURCE) {
                    mEndPage_Suggestion.setText(R.string.text_suggestionSource);
                } else if (mRole == THIS_DEVICE_IS_TARGET) {
                    mEndPage_Suggestion.setText(R.string.text_suggestionDest);
                }
                break;
            case DataTypeInfo:
//                title = dataTypeTitle;
                if (aMenu != null) {
                    aMenu.clear();
                }
                break;
            case Permissions:
//                title = getString(R.string.ept_page_title_select_content);
                if (aMenu != null) {
                    aMenu.clear();
                }
//                actionBar.setDisplayHomeAsUpEnabled(false);

                break;
            case CloudPairing:
//                title = getString(R.string.ept_page_title_scan_barcode);
                break;
            default:
//                title = getString(R.string.ept_page_title_default);
                break;
        }

        if ((IS_MMDS && !devicesPaired) || cloudPairingInprogress) {
//            title = getString(R.string.ept_page_title_default);
        }

//        if (!(Constants.FLAVOUR_SPRINT.equalsIgnoreCase(BuildConfig.FLAVOR) || IS_MMDS)) {
//            if (cloudPairingInprogress) {
//                title = getString(R.string.pairing);
//            }
//            if (mRole == THIS_DEVICE_IS_SOURCE) {
//                title = getResources().getString(R.string.text_source) + ": " + title;
//            } else if (mRole == THIS_DEVICE_IS_TARGET) {
//                title = getResources().getString(R.string.text_destination) + ": " + title;
//            }
//        }

//        actionBar.setTitle(title);
        boolean enableExit = false;
        boolean enableNext = false;
        boolean enableFinish = false;
        boolean enableBack = true;

        // TODO: this could be a case statement as we are using Java enums
        if (aNewLayout.ordinal() == EPTWizardPage.Welcome.ordinal()) {
            enableFinish = false;
            enableBack = true;
            enableNext = true;
            enableExit = false;
        } else if (aNewLayout.ordinal() == EPTWizardPage.SelectDevice.ordinal()) {
            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;

            if (mDeviceListView.getCount() == 0) {
                // Linkify the message
//    			<p>It may take a while to detect your other device. Check that your other device is running the Device Switch application and is connected to the same unrestricted WiFi network.</p><p>If your other device is not detected within a few minutes touch <a href="http://deviceswitch.com/support/en/device_not_detected.html">here</a> for more information.</p>
//	    	    final SpannableString messageString = new SpannableString(Html.fromHtml(getString(R.string.ept_select_device_help))); // TODO: internationalize this
                String htmlString = getString(R.string.ept_select_device_help);
                final SpannableString messageString = new SpannableString(Html.fromHtml(htmlString));
//	    	    Linkify.addLinks(messageString, Linkify.ALL);

                mSearchingForDevicesProgressDialog = new ProgressDialog(this);
                // TODO: add these strings to the string list so they can be translated
                mSearchingForDevicesProgressDialog.setTitle(getString(R.string.ept_scanning_for_devices_title));
                mSearchingForDevicesProgressDialog.setMessage(messageString);
                mSearchingForDevicesProgressDialog.setCancelable(false);
                mSearchingForDevicesProgressDialog.setCanceledOnTouchOutside(false);
                if (!mDisplayMMDSProgress) {
                    mSearchingForDevicesProgressDialog.show();


                    // Make the textview clickable. Must be called after show()
                    ((TextView) mSearchingForDevicesProgressDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                }
            }
        } else if (aNewLayout.ordinal() == EPTWizardPage.DisplayPin.ordinal()) {
            SecureRandom secureRandom = new SecureRandom();
            float randomFloat = secureRandom.nextFloat();
            float pinFloat = randomFloat * 9999;
            int pinInt = (int) pinFloat;
            mPin = String.format("%04d", pinInt);

            TextView pinTextView = (TextView) findViewById(R.id.pinLabel);
            pinTextView.setText(mPin);

            enableFinish = false;
            enableBack = true; // TODO: should this be true? - will require testing to show that we can reconnect
            enableNext = false;
            enableExit = false;
        } else if (aNewLayout.ordinal() == EPTWizardPage.SelectContent.ordinal()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mPinEntryBox.getWindowToken(), 0);
            enableFinish = false;
            enableBack = true;
            enableNext = true;
            enableExit = false;

            TextView selectContentText = (TextView) findViewById(R.id.SelectContentBottomText);
            switch (mOperationMode) {
                case CloudBackup:
                    selectContentText.setText(R.string.ept_select_content_text_backup);
                    break;
                case CloudRestore:
                    selectContentText.setText(R.string.ept_select_content_text_restore);
                    break;
                case LocalWiFi:
                    selectContentText.setText(R.string.ept_select_content_text);
                    break;
            }
//            loadContentDetails();
            mAppCount = AppMigrateUtils.getSelectedTotalAppsCountList();
            mAppSize = AppMigrateUtils.getSelectedTotalAppsSizeList();
            if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP) != null) {
                contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).setTotalCount((int) mAppCount);
                contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).setTotalSizeOfEntries(mAppSize);
            }

            if ((!isRemoteDeviceIOS()) && FeatureConfig.getInstance().getProductConfig().isEnableAppTransfer()) {
                contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).setTotalCount((int) mAppCount);
                contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).setTotalSizeOfEntries(mAppSize);
                if (mAppCount != 0) {
                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).setSelected(true);
                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).setSupported(true);
                }
            } else {
                contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).setSupported(true);
                contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).setSelected(true);
            }

            contentDetailsAdapter = new ContentDetailsAdapter(EasyMigrateActivity.this, ContentDetailsAdapter.CONTENT_SELECTION, contentDetailsMap);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
            contentDetailsView.setLayoutManager(mLayoutManager);
            contentDetailsView.setItemAnimator(null);
            contentDetailsView.addItemDecoration(new DividerItemDecoration(getApplicationContext(), RecyclerView.VERTICAL));
            contentDetailsView.setAdapter(contentDetailsAdapter);
            contentDetailsAdapter.notifyDataSetChanged();
            updateStorageProgress();
            if (FeatureConfig.getInstance().getProductConfig().isExcludeWhatsAppMediaEnabled()) {
                boolean whatsApppresent = AppMigrateUtils.checkAppPresence("WhatsApp");
                DLog.log("whatsApppresent " + whatsApppresent);
                if (!whatsApppresent) {
                    EXCLUDE_WHATSAPP_MEDIA = true;
                    content_select_whatsapp_media.setChecked(false);
                    content_select_whatsapp_media.setTextColor(getResources().getColor(R.color.black));
                    content_select_whatsapp_media.setEnabled(false);
                    //EXCLUDE_WHATSAPP_MEDIA = true;
                }
            }
            if (FeatureConfig.getInstance().getProductConfig().isExcludeSDCardMediaEnabled()) {
                boolean sdCardExists = CMDSDCardFileAccess.sdCardExists(context);
                DLog.log("sdCardExists_ " + sdCardExists);
                if (!sdCardExists) {
                    content_select_sdcard_media.setChecked(false);
                    content_select_sdcard_media.setTextColor(getResources().getColor(R.color.black));
                    content_select_sdcard_media.setEnabled(false);
                    //EXCLUDE_SDCARD_MEDIA = true;
                }
            }
            if (CommonUtil.getInstance().getTransferSpeed() == 0) {
                sendDataForEstimation();
            }

        } else if (aNewLayout.ordinal() == EPTWizardPage.SelectTransferType.ordinal()) {

            enableFinish = false;
            enableBack = true;
            enableNext = true;
            enableExit = false;

            final RadioGroup radioGroup = findViewById(R.id.radio_group);
            if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).isSupported())
                radioGroup.check(R.id.complete_transfer);
            else
                radioGroup.check(R.id.simple_transfer);
            Button button = findViewById(R.id.next_button_transfertypes);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    boolean complete_transfer = radioGroup.getCheckedRadioButtonId() == R.id.complete_transfer;
                    if (complete_transfer) {
                        DLog.log("Complete transfer selected");
                        for (int key : contentDetailsMap.keySet()) {
                            contentDetailsMap.get(key).setSupported(SUPPORTED_DATATYPE_MAP.get(key));
                            contentDetailsMap.get(key).setSelected(contentDetailsMap.get(key).isSupported() && contentDetailsMap.get(key).getTotalCount() > 0);
                        }
                        disableUnsupportedContentTypes();
                    } else {
                        DLog.log("Simple transfer selected");
                        for (int key : contentDetailsMap.keySet()) {
                            contentDetailsMap.get(key).setSupported(false);
                            contentDetailsMap.get(key).setSelected(false);
                        }
                        contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CONTACTS).setSupported(SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_CONTACTS));
                        contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CONTACTS).setSelected(contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CONTACTS).getTotalCount() > 0);
                        contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).setSupported(SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_CALENDAR));
                        contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).setSelected(contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).getTotalCount() > 0);
                    }
                    if (contentDetailsAdapter != null)
                        contentDetailsAdapter.notifyDataSetChanged();
                    setLayout(EPTWizardPage.SelectContent, EPTTransitionReason.UserNext);
                    updateStorageProgress();
                }
            });
        } else if (aNewLayout.ordinal() == EPTWizardPage.Progress.ordinal()) {
            if (mCurrentPage != EPTWizardPage.Progress) {
                hideAds(); // Don't show ads on the progress page
            }

            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;

            contentProgressDetailsAdapter = new ContentProgressDetailsAdapter(contentDetailsMap);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
            //final LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
            //layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            contentDetailsProgressView.setLayoutManager(layoutManager);
            contentDetailsProgressView.setItemAnimator(null);
            //contentDetailsView.addItemDecoration(new DividerItemDecoration(getApplicationContext(),RecyclerView.VERTICAL));
            contentDetailsProgressView.setAdapter(contentProgressDetailsAdapter);
            contentProgressDetailsAdapter.notifyDataSetChanged();

        } else if (aNewLayout.ordinal() == EPTWizardPage.Complete.ordinal()) {
            if (mCurrentPage != EPTWizardPage.Complete) {
                showAds(); // Don't show ads on the progress page
            }
            if (FeatureConfig.getInstance().getProductConfig().isUninstallRequired()) {
                btn_summary_finish.setText(R.string.ept_next_button);
            }

            enableFinish = true;
            enableBack = true;
            enableNext = false;
            enableExit = false;

            contentDetailsAdapter = new ContentDetailsAdapter(EasyMigrateActivity.this, ContentDetailsAdapter.SUMMARY, contentDetailsMap);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
            contentDetailsSummaryView.setLayoutManager(mLayoutManager);
            contentDetailsSummaryView.setItemAnimator(null);
            //contentDetailsView.addItemDecoration(new DividerItemDecoration(getApplicationContext(),RecyclerView.VERTICAL));
            contentDetailsSummaryView.setAdapter(contentDetailsAdapter);
            contentDetailsAdapter.notifyDataSetChanged();
            // Commented based on https://mobilicis.atlassian.net/browse/WDS-244
/*            if (mRole == THIS_DEVICE_IS_TARGET) {
                btn_email_summary.setVisibility(View.VISIBLE);
            }*/
        } else if (aNewLayout.ordinal() == EPTWizardPage.EnterPin.ordinal()) {
            mPinEntryBox.requestFocus();

            enableFinish = false;
            enableBack = true;
            enableNext = true;
            enableExit = false;
        } else if (aNewLayout.ordinal() == EPTWizardPage.CloudPairing.ordinal()) {
            if (mRole == THIS_DEVICE_IS_SOURCE) {
                cloudPairing_scanner_Layout.setVisibility(View.GONE);
                cloudPairing_QR_Layout.setVisibility(View.VISIBLE);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    requestCameraPermission();

                cloudPairing_scanner_Layout.setVisibility(View.VISIBLE);
                cloudPairing_QR_Layout.setVisibility(View.GONE);
                scanDataFromQR(cloudPairing_scanner);
            }

            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;
        } else if (aNewLayout.ordinal() == EPTWizardPage.Connected.ordinal()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mPinEntryBox.getWindowToken(), 0);

            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;
        } else if (aNewLayout.ordinal() == EPTWizardPage.About.ordinal()) {
            aboutFeedback.setText("");
            rating_bar.setRating(0);

            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;
        } else if (aNewLayout.ordinal() == EPTWizardPage.ConnectingToCloudService.ordinal()) {
            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;
        } else if (aNewLayout.ordinal() == EPTWizardPage.SelectBackupOrRestore.ordinal()) {
            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;
        } else if (aNewLayout.ordinal() == EPTWizardPage.SelectCloudOrLocal.ordinal()) {
            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;
        } else if (aNewLayout.ordinal() == EPTWizardPage.SelectOldOrNewDevice.ordinal()) {
//            if ("Pervacio".equalsIgnoreCase(Constants.COMPANY_NAME) || Constants.NEW_PLAYSTORE_FLOW) {
            LinearLayout deviceDetailsLayout = findViewById(R.id.device_details_layout);
            deviceDetailsLayout.setVisibility(View.VISIBLE);
            GifImageView arrowGifview = findViewById(R.id.arrow_gifview);
            arrowGifview.setVisibility(View.VISIBLE);
            TextView _deviceMake = findViewById(R.id.old_new_device_make);
            TextView _deviceModel = findViewById(R.id.old_new_device_model);
            _deviceMake.setText(DeviceInfo.getInstance().get_make());
            _deviceModel.setText(DeviceInfo.getInstance().get_model());

//            }
            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;
            if (Constants.WIFI_PROBLAMATIC_MODELS.contains(Build.MODEL))
                settingsPermissionDialog(context.getResources().getString(R.string.hotspot_permission_message), false);
        } else if (aNewLayout.ordinal() == EPTWizardPage.SelectOtherDeviceType.ordinal()) {
            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;
        } else if (aNewLayout.ordinal() == EPTWizardPage.Connecting.ordinal()) {
            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;
        } else if (aNewLayout.ordinal() == EPTWizardPage.DataTypeInfo.ordinal()) {
            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;
            ArrayList<String> dataList = null;
            Log.d(TAG, "Migration Succeeded1" + EMMigrateStatus.getItemsNotTransferred(selectedDatatypeforDataTypeInfo));
            try {
                if (CommonUtil.getInstance().getMigrationStatus() == Constants.MIGRATION_SUCCEEDED) {
                    Log.d(TAG, "Migration Succeeded2" + EMMigrateStatus.getItemsNotTransferred(selectedDatatypeforDataTypeInfo));
                    int itemsCount = EMMigrateStatus.getItemsNotTransferred(selectedDatatypeforDataTypeInfo);
                    if (itemsCount == 0) {
                        itemsCount = contentDetailsMap.get(selectedDatatypeforDataTypeInfo).getTotalCount() - EMMigrateStatus.getItemsTransferred(selectedDatatypeforDataTypeInfo);
                    }
                    String itemsNotTransferredCountMsg = MessageFormat.format(getString(R.string.str_unable_to_transfer), String.format(itemsCount + " " + dataTypeTitle));
                    if(EMDataType.EM_DATA_TYPE_SETTINGS == selectedDatatypeforDataTypeInfo) {
                        dataList = new ArrayList<>(Arrays.asList(itemsNotTransferredCountMsg));
                    }
                    Log.d(TAG, "itemsNotTransferredCountMsg: " + itemsNotTransferredCountMsg);
                    Log.d(TAG, "dataList: " + dataList);
                } else {
                    String[] settingsInfo = getResources().getStringArray(R.array.settingsInfo);
                    if (selectedDatatypeforDataTypeInfo == EMDataType.EM_DATA_TYPE_CONTACTS) {
                        dataList = EMMigrateStatus.getPreviouslyTransferredContacts();
                    } else if (selectedDatatypeforDataTypeInfo == EMDataType.EM_DATA_TYPE_SMS_MESSAGES) {
                        dataList = EMMigrateStatus.getPreviouslyTransferredMessages();
                    } else
                        dataList = new ArrayList<>(Arrays.asList(settingsInfo));
                }
                if (selectedDatatypeforDataTypeInfo == EMDataType.EM_DATA_TYPE_APP) {
                    AppMigrateUtils.procureAppsDetails();
                    ll_content_select_all.setVisibility(View.VISIBLE);
                    content_select_all_checkbox = (CheckBox) this.findViewById(R.id.content_select_all_checkbox);

                    content_select_all_checkbox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            CheckBox item = (CheckBox) view;
                            DLog.log("select all apps .............." + item.isChecked());
                            boolean checked = false;
                            if (item.isChecked()) {
                                checked = true;
                            } else {
                                checked = false;
                            }
                            AppMigrateUtils.selectOrUnselectAllApps(checked);
                            if (appDetailsAdapter != null) {
                                appDetailsAdapter.notifyDataSetChanged();
                            } else {
                                DLog.log("appDetailsAdapter is null");
                            }
                            DLog.log("select all apps .............. before mAppCount" + mAppCount + " mAppSize " + mAppSize);
                            mAppCount = AppMigrateUtils.totalAppCount;
                            mAppSize = AppMigrateUtils.totalAppSize;
                            DLog.log("select all apps .............. before mAppCount" + mAppCount + " mAppSize " + mAppSize);

                        }
                    });

                    appDetailsAdapter = new SelectedAppsDetailsAdapter(EasyMigrateActivity.this, appSelectionChangedListener);
                    RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
                    dataTypeDetailsView.setLayoutManager(mLayoutManager);
                    dataTypeDetailsView.setItemAnimator(null);
                    dataTypeDetailsView.setAdapter(appDetailsAdapter);

                } else {
                    ll_content_select_all.setVisibility(View.GONE);
                    dataTypeDetailsAdapter = new SelectedDataTypeDetailsAdapter(EasyMigrateActivity.this, dataList);
                    RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
                    dataTypeDetailsView.setLayoutManager(mLayoutManager);
                    dataTypeDetailsView.setItemAnimator(null);
                    dataTypeDetailsView.setAdapter(dataTypeDetailsAdapter);
                }
            } catch (Exception e) {
                DLog.log(e.getMessage());
                e.printStackTrace();
            }
        } else if (aNewLayout.ordinal() == EPTWizardPage.Permissions.ordinal()) {
            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;

            clickHereToGrantPermission(grantPermission);
            contentDetailsAdapter = new ContentDetailsAdapter(EasyMigrateActivity.this, ContentDetailsAdapter.PERMISSIONS, contentDetailsMap);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
            deniedPermissionsView.setLayoutManager(mLayoutManager);
            deniedPermissionsView.setItemAnimator(null);
            deniedPermissionsView.setAdapter(contentDetailsAdapter);
        } else if (aNewLayout.ordinal() == EPTWizardPage.ScanQRCode.ordinal()) {
            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;

            if (!Constants.CLOUD_PAIRING_ENABLED)
                requestCameraPermission();

            EMNetworkManagerClientUIHelper.connectToClientNetwork(barcodeView, new EMNetworkManagerClientUIHelper.ProgressObserver() {
                @Override
                void remoteDeviceFound(EMDeviceInfo aDeviceInfo, EMConfig.EMWiFiConnectionMode aWiFiConnectionMode) {
                    notConnectingToNetwork();
                    mRemoteDeviceManager.selectRemoteDevice(aDeviceInfo);
                    mRemoteDeviceManager.connectToRemoteDevice();
                    if (mRole == THIS_DEVICE_IS_TARGET) {
                        mRemoteDeviceManager.remoteToBecomeSource();
                    } else {
                        mRemoteDeviceManager.remoteToBecomeTarget();
                    }
                    if (IS_MMDS)
                        sendResponse(outputStreamForME, Constants.WDS_CONNECTION_OK);
                }

                @Override
                void connectionError(int aError) {
                    if (aError == 3) {
                        if (CLOUD_PAIRING_ENABLED) {
                            EMNetworkManagerHostUIHelper.reset();
                            sendNetworkInfo();
                        } else {
                            if (IS_MMDS) {
                                sendResponse(outputStreamForME, Constants.WDS_CONNECTION_FAILED);
                            } else {
//                                displaySwitchDialog();
//                                connectToP2PDevice();
                                reconnectP2P();
                            }
                        }
                    } else if (aError == 4) {
                        raiseFatalError("", getString(R.string.str_wrong_selection), 1);
                    } else if (aError == 11) {
                        showWifiConnectDialog(false);
                    } else {
                        notConnectingToNetwork();
                        raiseFatalError(getString(R.string.ept_cant_connect_to_network_title), getString(R.string.ept_cant_connect_to_network_text));
                    }
                }

                @Override
                void progressUpdate(EMNetworkManagerClientUIHelper.State aState) {
                    switch (aState) {
                        case CONNECTING_TO_WIFI_DIRECT_NETWORK:
                            setLayout(EPTWizardPage.Connecting, EPTTransitionReason.UserNext);
                            CustomButton btnConnect = (CustomButton) findViewById(R.id.btnConnect);
                            btnConnect.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    final Intent wifisettingsIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                    wifisettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(wifisettingsIntent);
                                }
                            });

                            CustomButton btnTryAgain = (CustomButton) findViewById(R.id.btnTryAgain);
                            btnTryAgain.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    setLayout(EPTWizardPage.ScanQRCode, EPTTransitionReason.UserNext);
                                }
                            });
                            //connectingToNetwork();
                            break;
                        case TRYING_CONNECTION_TO_LAN_PEER:
                            // No need to update progress here
                            break;
                        case TRYING_CONNECTION_TO_WIFI_DIRECT_PEER:
                            // No need to update progress here
                            break;
                    }
                }
            }, emGlobals.getmContext());
        } else if (aNewLayout.ordinal() == EPTWizardPage.DisplayQRCode.ordinal()) {
            enableFinish = false;
            enableBack = true;
            enableNext = false;
            enableExit = false;

            TextView network_Name = (TextView) findViewById(R.id.network_Name);
            TextView ssidTextView = (TextView) findViewById(R.id.network_name_text_view);
            TextView passPhraseTextView = (TextView) findViewById(R.id.network_passphrase_text_view);
            TextView network_Passphrase = (TextView) findViewById(R.id.network_Passphrase);
            final ImageView ecodedQRCodeImageView = (ImageView) findViewById(R.id.generated_qr_code_image_view);
            final TextView scanSuggestion = (TextView) findViewById(R.id.scan_suggesion);
            Typeface typefaceRegular = Typeface.createFromAsset(getAssets(), "fonts/roboto_regular.ttf");
            Typeface typefaceBold = Typeface.createFromAsset(getAssets(), "fonts/roboto_bold.ttf");
            ssidTextView.setTypeface(typefaceRegular);
            passPhraseTextView.setTypeface(typefaceRegular);
            scanSuggestion.setTypeface(typefaceRegular);
            network_Passphrase.setTypeface(typefaceBold);
            network_Name.setTypeface(typefaceBold);

            EMNetworkManagerHostUIHelper.createHostNetwork(mRemoteDeviceManager, ecodedQRCodeImageView, ssidTextView, passPhraseTextView, emGlobals.getmContext(), new EMNetworkManagerHostUIHelper.ProgressObserver() {

                @Override
                void progressUpdate(EMNetworkManagerHostUIHelper.State aState) {
                    switch (aState) {
                        case CREATING_WIFI_DIRECT_NETWORK:
                            creatingNetwork();
                            break;
                        case WAITING_FOR_CONNECTION:
                            notCreatingNetwork();
                            if (CLOUD_PAIRING_ENABLED) {
                                scanSuggestion.setVisibility(View.INVISIBLE);
                                ecodedQRCodeImageView.setVisibility(View.GONE);
                                displayWifiSuggestionScreen();
                                if (!hotSpotInfoposted)
                                    stateMachine.postData(StateMachine.WIFI_INFO);
                            } else {
                                if (IS_MMDS) {
                                    scanSuggestion.setVisibility(View.INVISIBLE);
                                    ecodedQRCodeImageView.setVisibility(View.GONE);
                                    CommonUtil commonUtil = CommonUtil.getInstance();
                                    sendResponse(outputStreamForME, Constants.COMMAND_NETWORK_DETAILS + ":" + commonUtil.getmWiFiPeerAddress() + ":" + commonUtil.getmWiFiPeerSSID() + ":" + commonUtil.getmWiFiPeerPassphrase() + ":" + commonUtil.getmCryptoEncryptPass() + ":" + commonUtil.getmFrequency() + ":" + commonUtil.getNetworkType());
                                } else {
                                    int frequency = CommonUtil.getInstance().getmFrequency();
                                    String ssid = CommonUtil.getInstance().getmWiFiPeerSSID();
//                                    if ((ssid == null || ssid.isEmpty() && btn_switch_scan.getVisibility() != View.GONE)) {
//                                        btn_switch_scan.setVisibility(View.VISIBLE);
//                                    } else if ((frequency > 3000 && btn_switch_scan.getVisibility() != View.GONE)) {
//                                        btn_switch_scan.setVisibility(View.VISIBLE);
//                                    }
                                }
                            }
                            break;
                    }
                }
            });
        }

        if ((IS_MMDS && (aNewLayout != EPTWizardPage.DataTypeInfo))) {
            enableBack = true;
        }

        // Apply button status
        if (enableNext || enableFinish)
            mNextButton.setEnabled(true);
        else
            mNextButton.setEnabled(false);

        if (enableBack)
            mBackButton.setEnabled(true);
        else
            mBackButton.setEnabled(true);

        if (enableExit) {
            mBackButton.setEnabled(true);
            mBackButton.setText(this.getString(R.string.ept_exit_button));
        } else {
            mBackButton.setText(this.getString(R.string.ept_back_button));
        }

        if (aTransitionReason != EPTTransitionReason.StartUp)
        // Dont do any animation if we're just setting up - go straight to the page
        {
            // Determine which transition should be used
            Animation inAnimation = mFadeIn;
            Animation outAnimation = mFadeOut;

            if (aTransitionReason == EPTTransitionReason.UserNext) {
                inAnimation = mInFromRight;
                outAnimation = mOutToLeft;
            }

            if (aTransitionReason == EPTTransitionReason.UserBack) {
                outAnimation = mOutToRight;
                inAnimation = mInFromLeft;
            }

            mFlipper.setInAnimation(inAnimation);
            mFlipper.setOutAnimation(outAnimation);
        }

        String nextButtonText;
        if (enableFinish) {
            mFinishButtonEnabled = true;
            nextButtonText = this.getString(R.string.ept_finish_button);
        } else {
            mFinishButtonEnabled = false;
            nextButtonText = this.getString(R.string.ept_next_button);
        }

        mNextButton.setText(nextButtonText);

        mCurrentPage = aNewLayout;
        mFlipper.setDisplayedChild(aNewLayout.ordinal());

       /* RelativeLayout mainScrollView = (RelativeLayout) findViewById(R.id.MainScrollViewLayout);
        mainScrollView.fullScroll(ScrollView.FOCUS_UP);*/

        if (mFailure) {
            // TODO: display failure message box
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

            // set title
            alertDialogBuilder.setTitle(getString(R.string.ept_could_not_copy_data_title)); // TODO: internationalize this

            // set dialog message
            alertDialogBuilder
                    .setMessage(getString(R.string.ept_could_not_copy_data_message))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ept_ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Ignore
                        }
                    });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            /*// show it
            if(!mDisplayMMDSProgress)
                alertDialog.show();*/
        }
    }

    private void setSessionId(TextView textView, boolean status) {
//        try {
//            if (BuildConfig.FLAVOR.equalsIgnoreCase(FLAVOUR_SPRINT)) {
//                String transactionId = DashboardLog.getInstance().geteDeviceSwitchSession().getDeviceSwitchSessionId();
//                textView.setVisibility(View.VISIBLE);
//                if (transactionId != null && !transactionId.isEmpty()) {
//                    DLog.log("Transaction id " + transactionId);
//                    String mTrasnactionId = getResources().getString(R.string.uniqueTransactionId) + "<b>" + " " + transactionId + "</b>";
//                    textView.setText(Html.fromHtml(mTrasnactionId));
//                } else {
//                    if (status)
//                        textView.setText(getResources().getString(R.string.str_transactionid_empty));
//                }
//            } else {
        textView.setVisibility(View.GONE);
//            }
//        } catch (Exception e) {
//            DLog.log("Exception in setSessionId" + e.getMessage());
//        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////

    private Animation mInFromLeft;
    private Animation mInFromRight;
    private Animation mOutToLeft;
    private Animation mOutToRight;
    private Animation mFadeOut;
    private Animation mFadeIn;
    private ViewFlipper mFlipper;

    private EPTWizardPage mCurrentPage;

    private Button mNextButton;
    private Button mBackButton;
    private ListView mDeviceListView;

    private Button mEmailButton;
    private Button mEmailButton2;
    private Button mFeedbackButton;

    private TextView aboutAccpet, aboutCancel, aboutVersionNumber, aboutPrivacyPolicy, aboutreleaseDate, mEndPage_Suggestion;

    private RatingBar rating_bar;

    private EditText aboutFeedback;
    private LinearLayout cloudPairing_scanner_Layout, cloudPairing_QR_Layout;
    private ImageView cloudPairing_QR;
    private TextView cameraproblem, session_pin;

    private RecyclerView contentDetailsView;
    private RecyclerView contentDetailsSummaryView;
    private RecyclerView contentDetailsProgressView;
    private RecyclerView dataTypeDetailsView;
    private RecyclerView deniedPermissionsView;
    private LinearLayout ll_content_select_all;
    private CheckBox content_select_all_checkbox;

    private ProgressBar storageProgressbar;

    private TextView storageMessage, elapsedTime, estimationTime;

    private TextView mTotalTransferTime;

    private TextView mUniqueTransactionId;

    private TextView mSmsSuggestionText;

    private Button welcomeNext;

    private Button exitFromApp;

    private Button startOver;

    private Button finishpage;

    private TextView storageSuggestion;

    private TextView sessionId;

    private TextView sessionIdConnectedScreen;

    private TextView selectContentSessionIdSource;

    private boolean mDisconnectPageDisplayed = false;

    private boolean mFailure = false;

    private TextView btn_cancel, btn_summary_finish, btn_email_summary;
    private TextView selectContentsuggestion, selectContentTimeSuggestion;
    private TextView grantPermission;
    private Button btn_start_migration, skip;
    private LinearLayout excludeSDCardMedia_Layout, excludeWhatsAppMedia_Layout;

    enum EPTTransitionReason {
        UserBack, // User touched back
        UserNext, // User touched next
        Automatic, // Automatic state change, e.g. USB connection detected
        StartUp    // Setting up the first page
    }

    private EMRemoteDeviceManager mRemoteDeviceManager = new EMRemoteDeviceManager(emGlobals.getmContext());
    EMPreviouslyTransferredContentRegistry emPreviouslyTransferredContentRegistry = null;

    void authenticateWithCloudService() {
        mCloudService = new CMDBackupAndRestoreEngine(CMDBackupAndRestoreServiceType.CMD_GOOGLE_DRIVE, this, this);

        // Start the log-in, we will be called back in either cmdCommandComplete, or cmdError
        mCloudService.startUserLoginAsync(this);
    }

    @Override
    public void progressUpdate(EMProgressInfo aProgressInfo) {

        // START - Pervacio
        if (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_TEXT_COMMAND_RECEIVED) {
            // Will Enter into this loop When text command is received from source.
            String receivedCommand = aProgressInfo.mTextCommand;
            DLog.log("EM_TEXT_COMMAND_RECEIVED >>" + aProgressInfo.mTextCommand);

            if (receivedCommand != null && receivedCommand.contains(Constants.MIGRATION_STARTED)) {
                if (!Constants.IS_MMDS)
                    registerBatteryStatusBroadcastReceiver();

                mBackupStartTime = System.currentTimeMillis();
                CommonUtil.getInstance().setBackupStartedTime(mBackupStartTime);
                // starting elapsed timer
                elapsedTimer.start();
                CommonUtil.getInstance().setMigrationStatus(Constants.MIGRATION_INPROGRESS);
                isMigrationInProgress = true;
                DashboardLog.getInstance().geteDeviceSwitchSession().setStartDateTime(String.valueOf(mBackupStartTime));
                int selectedDataTypeCount = 0;

                //Sample Command: MIGRATION_STARTED#{"this":"json", "another":"field"}
                try {
                    DLog.log("Received command. Trying to parse JSON: " + receivedCommand);

                    //Separate the JSON from command and parse it. Take everything after MIGRATION_STARTED#
                    String srcJson = receivedCommand.substring(MIGRATION_STARTED.length() + 1);

                    EDeviceSCSWrapper eDeviceSCSWrapper = new Gson().fromJson(srcJson, EDeviceSCSWrapper.class);
                    EDeviceSwitchSourceContentSummary[] scs = eDeviceSCSWrapper.getEDeviceSwitchSourceContentSummaryCollection();
                    DashboardLog.getInstance().geteDeviceSwitchSession().setEstimatedTimeInMS(eDeviceSCSWrapper.getEstimationTime());
                    DashboardLog.getInstance().addAdditionalInfo(eDeviceSCSWrapper.getAdditionalInfo());
                    //Better safe than sorry, check one more time
                    if (scs.length > 0) {
                        //Iterate over the array and pull out info about whether or not, a particular type was selected
                        for (EDeviceSwitchSourceContentSummary iscs : scs) {
                            boolean mIsSelected = false;
                            int totalCount = iscs.getNumberOfEntries();

                            if (iscs.getSelected().equals(SELECTED)) {
                                mIsSelected = true;
                                if (iscs.getTotalSizeOfEntries() != -1) {
                                    mSelectedContentSize += iscs.getTotalSizeOfEntries();
                                }
                            }

                            DLog.log("DATA TYPE FROM SOURCE: " + iscs.getContentType() + " isSelected: " + mIsSelected);
                            if (mIsSelected)
                                selectedDataTypeCount++;

                            switch (Constants.DATATYPE.valueFor(iscs.getContentType())) {
                                case CONTACT:
                                    mContactsSelected = mIsSelected;
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CONTACTS).setSelected(mIsSelected);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CONTACTS).setTotalCount(totalCount);
                                    if (mIsSelected)
                                        CommonUtil.getInstance().addToDataTobeTransferred(EMDataType.EM_DATA_TYPE_CONTACTS);
                                    break;

                                case MESSAGE:
                                    mMessageSelected = mIsSelected;
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).setSelected(mIsSelected);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).setTotalCount(totalCount);
                                    if (mIsSelected)
                                        CommonUtil.getInstance().addToDataTobeTransferred(EMDataType.EM_DATA_TYPE_SMS_MESSAGES);
                                    if (IS_MMDS && mIsSelected && !"Android".equalsIgnoreCase(remotePlatform)) {
                                        iosSMSRestored = false;
                                        contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).setSupported(true);
                                        contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).setProgressCount(0);
                                    }
                                    break;

                                case CALENDAR:
                                    mCalendarsSelected = mIsSelected;
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).setSelected(mIsSelected);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).setTotalCount(totalCount);
                                    if (mIsSelected)
                                        CommonUtil.getInstance().addToDataTobeTransferred(EMDataType.EM_DATA_TYPE_CALENDAR);
                                    break;

                                case AUDIO:
                                    mAudioSelected = mIsSelected;
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).setSelected(mIsSelected);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).setTotalCount(totalCount);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).setTotalSizeOfEntries(iscs.getTotalSizeOfEntries() * 1024);
                                    if (mIsSelected) {
                                        CommonUtil.getInstance().addToDataTobeTransferred(EMDataType.EM_DATA_TYPE_MUSIC);
                                        CommonUtil.getInstance().setSelectedMediaCount(totalCount);
                                    }
                                    break;

                                case IMAGE:
                                    mPhotosSelected = mIsSelected;
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).setSelected(mIsSelected);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).setTotalCount(totalCount);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).setTotalSizeOfEntries(iscs.getTotalSizeOfEntries() * 1024);
                                    if (mIsSelected) {
                                        CommonUtil.getInstance().setSelectedMediaCount(totalCount);
                                        CommonUtil.getInstance().addToDataTobeTransferred(EMDataType.EM_DATA_TYPE_PHOTOS);
                                    }
                                    break;

                                case VIDEO:
                                    mVideosSelected = mIsSelected;
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).setSelected(mIsSelected);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).setTotalCount(totalCount);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).setTotalSizeOfEntries(iscs.getTotalSizeOfEntries() * 1024);
                                    if (mIsSelected) {
                                        CommonUtil.getInstance().addToDataTobeTransferred(EMDataType.EM_DATA_TYPE_VIDEO);
                                        CommonUtil.getInstance().setSelectedMediaCount(totalCount);
                                    }
                                    break;

                                case APP:
                                    mAppsSelected = mIsSelected;
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).setSelected(mIsSelected);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).setTotalCount(totalCount);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).setTotalSizeOfEntries(iscs.getTotalSizeOfEntries() * 1024);
                                    if (mIsSelected) {
                                        CommonUtil.getInstance().addToDataTobeTransferred(EMDataType.EM_DATA_TYPE_APP);
                                        CommonUtil.getInstance().setSelectedMediaCount(totalCount);
                                    }
                                    break;

                                case CALLLOG:
                                    mCallLogsSelected = mIsSelected;
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALL_LOGS).setSelected(mIsSelected);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALL_LOGS).setTotalCount(totalCount);
                                    if (mIsSelected)
                                        CommonUtil.getInstance().addToDataTobeTransferred(EMDataType.EM_DATA_TYPE_CALL_LOGS);
                                    break;

                                case SETTINGS:
                                    mSettingsSelected = mIsSelected;
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SETTINGS).setSelected(mIsSelected);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SETTINGS).setTotalCount(totalCount);
                                    if (mIsSelected)
                                        CommonUtil.getInstance().addToDataTobeTransferred(EMDataType.EM_DATA_TYPE_SETTINGS);
                                    break;
                                case DOCUMENTS:
                                    mDocumentsSelected = mIsSelected;
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_DOCUMENTS).setSelected(mIsSelected);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_DOCUMENTS).setTotalCount(totalCount);
                                    contentDetailsMap.get(EMDataType.EM_DATA_TYPE_DOCUMENTS).setTotalSizeOfEntries(iscs.getTotalSizeOfEntries() * 1024);
                                    if (mIsSelected) {
                                        CommonUtil.getInstance().addToDataTobeTransferred(EMDataType.EM_DATA_TYPE_DOCUMENTS);
                                        CommonUtil.getInstance().setSelectedMediaCount(totalCount);
                                    }
                                    break;
                            }

                        }

                        //Set the info in the session object
                        DashboardLog.getInstance().geteDeviceSwitchSession().
                                setEDeviceSwitchSourceContentSummaryCollection(scs);
                        DashboardLog.getInstance().geteDeviceSwitchSession().setSessionStage(Constants.SESSION_STAGE.TRANSFER_IN_PROGRESS.value());
                    }
                } catch (Exception ex) {
                    DLog.log("Exception parsing JSON");
                    DLog.log(ex);
                }

                //If message was selected in source device, show prompt to change default app in destination
                if (mMessageSelected && !IS_MMDS) {
                    EasyMigrateActivity.getDefaultSMSAppPermission(null);
                }
                if (IS_MMDS && mMessageSelected && (selectedDataTypeCount == 1) && !"Android".equalsIgnoreCase(remotePlatform)) {
                    DLog.log("iosOnlySMSSelected true");
                    iosOnlySMSSelected = true;
                    setLayout(EPTWizardPage.Progress, EPTTransitionReason.UserNext);
                }
            }
            //Operation was cancelled on other device
            else if (receivedCommand != null && (receivedCommand.contains(Constants.MIGRATION_CANCELLED) || receivedCommand.contains(Constants.COMMAND_APP_KILLED) || receivedCommand.contains(Constants.COMMAND_APP_CRASHED))) {
                isInterrupted = true;
                isMigrationInProgress = false;
                stopOrDisconnectFromAnyNetwork();//need to stop the wifi observer
                stopAllProcesses();
                CommonUtil.getInstance().setMigrationInterrupted(true);
                CommonUtil.getInstance().pauseMigration();
                showInterruptedDialog(receivedCommand);
            } else if (receivedCommand != null && receivedCommand.contains(Constants.LINK_SPEED)) {
                CommonUtil.getInstance().setLinkSpeed(Integer.parseInt(receivedCommand.split(":")[1]));
            }
        } else if (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_TEXT_COMMAND_SENT) {
            String receivedCommand = aProgressInfo.mTextCommand;
            if (receivedCommand != null && receivedCommand.equalsIgnoreCase(EMStringConsts.EM_TEXT_RESPONSE_OK)) {
                if (sentCommandToRemote != null && sentCommandToRemote.contains(MIGRATION_STARTED)) {
                    handler.sendEmptyMessage(START_MIGRATION);
                } else if (MIGRATION_CANCELLED.equalsIgnoreCase(sentCommandToRemote)) {
                    stopOrDisconnectFromAnyNetwork();
                    migrationInterrupted.removeMessages(MIGRATION_CANCELLED_EVENT);
                    stopAllProcesses();
                    if (CommonUtil.getInstance().getMigrationStatus() == Constants.MIGRATION_INPROGRESS)
                        EMUtility.saveMigrationStats(contentDetailsMap);
                    if (FeatureConfig.getInstance().getProductConfig().isReportProblemEnabled()) {
                        File externalStorage = context.getFilesDir();
                        EMUtility.copyFile(new File(externalStorage.toString() + "/log.txt"), new File(externalStorage.toString() + "/reportlog.txt"));
                        startActivity(new Intent(EasyMigrateActivity.this, ReportAProblem.class));
                    }
                    setLayout(EPTWizardPage.SessionCancel, EPTTransitionReason.Automatic);
                    initializeTransactionService(DashboardLog.getInstance().isThisDest());
                }
            }
            if (aProgressInfo.mTextCommand != null && EMStringConsts.EM_COMMAND_TEXT_QUIT.equalsIgnoreCase(aProgressInfo.mTextCommand)) {
                DLog.log("sending QUIT to destination");
                mRemoteDeviceManager.sendQuit();
                handler.sendEmptyMessageDelayed(DISPLAY_FINAL_SUMMERY_SCREEN, 5 * 1000);

                DLog.log("showProgressDialog--1");
                showProgressDialog(getString(R.string.generating_summary_title), getString(R.string.generating_summary_description));

            }
        }
        // END - Pervacio
        if (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_USER_LOGGING_IN) {
            if (mCurrentPage != EPTWizardPage.ConnectingToCloudService) {
                setLayout(EPTWizardPage.ConnectingToCloudService, EPTTransitionReason.Automatic);
            }
        } else {
            //MODIFIED - PERVACIO
            if (isMigrationInProgress && mCurrentPage != EPTWizardPage.About && mCurrentPage != EPTWizardPage.Progress && (aProgressInfo.mOperationType != EMProgressInfo.EMOperationType.EM_TEXT_COMMAND_RECEIVED && aProgressInfo.mOperationType != EMProgressInfo.EMOperationType.EM_TEXT_COMMAND_SENT)) {
                setLayout(EPTWizardPage.Progress, EPTTransitionReason.Automatic);
            }

            boolean sendingFiles = false;

            if (aProgressInfo.mFailed) {
                mFailure = true;
                showFailDialog();
            }

            if ((aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_QUIT_COMMAND_SENT)
                    || (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_QUIT_COMMAND_RECEIVED)) {
                //Need to release wakelock and wifi lock resources after migration
                setScreenOn(false);
                setWakeLock(false);
                elapsedTimer.cancel();
                reEstimationTimer.cancel();
                handler.removeMessages(REESTIMATE_TIME);
                handler.removeMessages(CONNECTION_LOST);
                handler.removeMessages(REESTIMATE_TIME);
                handler.removeMessages(UPDATE_PROGRESS);
                handler.removeMessages(DISPLAY_FINAL_SUMMERY_SCREEN);
                handler.removeMessages(NOTIFICATION_MESSAGE);
                MigrationStats.getInstance().setMigrationCompleted(true);
                EMUtility.saveMigrationStats(contentDetailsMap);
                isMigrationInProgress = false;
                if (failedDialog != null) {
                    failedDialog.dismiss();
                }
                if (cancelDialog != null) {
                    cancelDialog.dismiss();
                }
                if (interruptedDialog != null) {
                    interruptedDialog.dismiss();
                }
                hideProgressDialog();
                DLog.log("EasyMigrateActivity Transaction is complete.");
                if (Constants.mTransferMode.equalsIgnoreCase(P2P_MODE)) {
                    wifiP2PInitDone = false;
                    p2pReconnect = false;
                    handler.postDelayed(stopP2PThread, 5000);
                }
                DashboardLog.getInstance().geteDeviceSwitchSession().setCancellationReason(Constants.CANCEL_REASON.NO_ERROR.value());
                /*** Once the transaction is complete, let's log it to Dashboard ***/
                if (mRole == THIS_DEVICE_IS_TARGET) {
                    mRestoreEndTime = System.currentTimeMillis();
                    mTransferTotalTime = elapsedTimeInsec * 1000;//((mRestoreEndTime - Long.parseLong(DashboardLog.getInstance().geteDeviceSwitchSession().getStartDateTime())));
                    updateTransactionStatus(true);
                    String mTransfertime = EMUtility.getReadableTime(EasyMigrateActivity.this, mTransferTotalTime, false);
                    if (mTransfertime.isEmpty()) {
                        mTransfertime = String.format("1 %s", getString(R.string.sec));
                    }
                    mTotalTransferTime.setVisibility(View.VISIBLE);
                    DLog.log("Transfer time : " + mTransfertime);
                    String sourceString = getResources().getString(R.string.transfer_completed_in) + "<b>" + " " + mTransfertime + "</b>";
                    mTotalTransferTime.setText(Html.fromHtml(sourceString));
                    //session id need to be displayed in destination
                    String sessionId = DashboardLog.getInstance().geteDeviceSwitchSession().getDeviceSwitchSessionId();
                    if (sessionId != null && !sessionId.isEmpty()) {
                        DLog.log("Transaction id " + sessionId);
                        String mTrasnactionId = getResources().getString(R.string.uniqueTransactionId) + "<b>" + " " + sessionId + "</b>";
                        mUniqueTransactionId.setText(Html.fromHtml(mTrasnactionId));
                    }
                } else {
                    mTotalTransferTime.setVisibility(View.GONE);
                    mUniqueTransactionId.setVisibility(View.GONE);
//                    if (BuildConfig.FLAVOR.equalsIgnoreCase(FLAVOUR_SPRINT) && contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).isSelected()) {
//                        mSmsSuggestionText.setText(getString(R.string.large_text_msg_maynot_transfer));
//                        mSmsSuggestionText.setVisibility(View.VISIBLE);
//                    }
                    setSessionId(mUniqueTransactionId, false);
                }

                setLayout(EPTWizardPage.Complete, EPTTransitionReason.UserNext);
                String sessionID = DashboardLog.getInstance().geteDeviceSwitchSession().getDeviceSwitchSessionId();
                if (!Constants.WIFI_PROBLAMATIC_MODELS.contains(Build.MODEL) && Constants.LOGGING_ENABLED && mRole == THIS_DEVICE_IS_TARGET && (sessionID == null || sessionID.isEmpty())) {
                    getSessionID();
                }
                if (!Constants.IS_MMDS) {
                    if (batteryStatusBroadcastReceiver != null)
                        try {
                            DLog.log("unregistering the batteryStatusBroadcastReceiver");
                            unregisterReceiver(batteryStatusBroadcastReceiver);
                        } catch (Exception e) {
                            DLog.log("exception while unregistering the batteryStatusBroadcastReceiver : " + e.getMessage());
                        }
                    DLog.log("WDS product. Starting TransactionLogService");
//                  DashboardLog.getInstance().geteDeviceSwitchSession().setAdditionalInfo(getGoogleAccountRemovalStatus());
                    DashboardLog.getInstance().addAdditionalInfo(getGoogleAccountRemovalStatus());
                    DashboardLog.getInstance().updateToServer(true);
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    DLog.log("showProgressDialog--2");
//                        showProgressDialog(getString(R.string.generating_summary_title), getString(R.string.generating_summary_description));
//                            handler.sendEmptyMessageDelayed(HIDE_PROGRESS_DIALOG, 5 * 1000);
//                    }
                    DLog.log(("Calling sendTransactionInfoTOServer 1"));
                    sendTransactionInfoTOServer();

//                    if (BuildConfig.FLAVOR.equalsIgnoreCase(FLAVOUR_SPRINT))
//                        displayRatingDialog();
                } else {
                    mUniqueTransactionId.setVisibility(View.GONE);
                }
                CommonUtil.getInstance().setMigrationStatus(Constants.MIGRATION_SUCCEEDED);
                handler.sendEmptyMessageDelayed(DISCONNECT_NETWORK, 3 * 1000);
            } else if (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_UPDATE_DB_DETAILS) {
                updateTransactionStatus(true);
            } else if ((aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_OUTGOING_DATA)
                    || (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_INCOMING_DATA)) {
                /*if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_CALENDAR) {
                    mProgressText.setText(getString(R.string.ept_processing_calendar));
                } else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_CONTACTS) {
                    mProgressText.setText(getString(R.string.ept_processing_contacts));
                }else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_SMS_MESSAGES) {
                    mProgressText.setText(getString(R.string.ept_processing_messages));
                }else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_CALL_LOGS) {
                    mProgressText.setText(getString(R.string.ept_processing_call_logs));
                }*/
                updateProgress(aProgressInfo.mDataType, aProgressInfo.mCurrentItemNumber, aProgressInfo.mTotalItems);
            } else if (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_OPERATION_SENDING_DATA) {
                /*if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_CALENDAR) {
                    mProgressText.setText(getString(R.string.ept_sending_calendar));
                } else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_CONTACTS) {
                    mProgressText.setText(getString(R.string.ept_sending_contacts));
                } else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_SMS_MESSAGES) {
                    mProgressText.setText(getString(R.string.ept_sending_messages));
                } else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_PHOTOS) {
                    mProgressText.setText(getString(R.string.ept_sending_photos));
                    sendingFiles = true;
                } else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_VIDEO) {
                    mProgressText.setText(getString(R.string.ept_sending_videos));
                    sendingFiles = true;
                } else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_CALL_LOGS) {
                    mProgressText.setText(getString(R.string.ept_sending_call_logs));
                }*/
                updateProgress(aProgressInfo.mDataType, aProgressInfo.mCurrentItemNumber, aProgressInfo.mTotalItems);
            } else if (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_OPERATION_RECEIVING_DATA) {
                if (!progressUpdating) {
                    progressUpdating = true;
                    handler.sendEmptyMessage(UPDATE_PROGRESS);
                }

                if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_PHOTOS) {

                    //This point of code will be called when we start receiving photos
                    //Log the start time only once
                    if (DashboardLog.getInstance().getImageStartTime() == Constants.UNINITIALIZED) {
                        DLog.log("Logging photo start time");
                        DashboardLog.getInstance().setImageStartTime(System.currentTimeMillis());
                        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.IMAGE, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).getTotalCount(), contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).getTotalSizeOfEntries(), Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, false);
                        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.IMAGE, 0, 0, Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, true, true);
                    }
                    // DLog.log("Logging photo end time");
                    //End time will be time of last update
                    DashboardLog.getInstance().setImageEndTime(System.currentTimeMillis());
                } else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_VIDEO) {

                    //This point of code will be called when we start receiving photos
                    //Log the start time only once
                    if (DashboardLog.getInstance().getVideoStartTime() == Constants.UNINITIALIZED) {
                        DLog.log("Logging video start time");
                        DashboardLog.getInstance().setVideoStartTime(System.currentTimeMillis());
                        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.VIDEO, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).getTotalCount(), contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).getTotalSizeOfEntries(), Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, false);
                        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.VIDEO, 0, 0, Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, true, true);
                    }
                    //End time will be time of last update
                    //DLog.log("Logging video end time");
                    DashboardLog.getInstance().setVideoEndTime(System.currentTimeMillis());
                } else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_MUSIC) {

                    //This point of code will be called when we start receiving audio
                    //Log the start time only once
                    if (DashboardLog.getInstance().getAudioStartTime() == Constants.UNINITIALIZED) {
                        DLog.log("Logging Audio start time");
                        DashboardLog.getInstance().setAudioStartTime(System.currentTimeMillis());
                        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.AUDIO, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).getTotalCount(), contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).getTotalSizeOfEntries(), Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, false);
                        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.AUDIO, 0, 0, Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, true, true);
                    }
                    //End time will be time of last update
                    // DLog.log( "Logging Audio end time");
                    DashboardLog.getInstance().setAudioEndTime(System.currentTimeMillis());
                } else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_APP) {
                    if (DashboardLog.getInstance().getAppsStartTime() == Constants.UNINITIALIZED) {
                        DLog.log("Logging App start time");
                        DashboardLog.getInstance().setAppsStartTime(System.currentTimeMillis());
                        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.APP, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).getTotalCount(), contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).getTotalSizeOfEntries(), Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, false);
                        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.APP, 0, 0, Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, true, true);
                    }
                    DashboardLog.getInstance().setAppsEndTime(System.currentTimeMillis());
                } else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_DOCUMENTS) {
                    if (DashboardLog.getInstance().getDocumentsStartTime() == Constants.UNINITIALIZED) {
                        DLog.log("Logging Documents start time");
                        DashboardLog.getInstance().setDocumentsStartTime(System.currentTimeMillis());
                        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.DOCUMENTS, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_DOCUMENTS).getTotalCount(), contentDetailsMap.get(EMDataType.EM_DATA_TYPE_DOCUMENTS).getTotalSizeOfEntries(), Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, false);
                        DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.DOCUMENTS, 0, 0, Constants.TRANSFER_STATUS.FAILED, Constants.TRANSFER_STATE.IN_PROGRESS, true, true);
                    }
                    DashboardLog.getInstance().setDocumentsEndTime(System.currentTimeMillis());
                }
                updateProgress(aProgressInfo.mDataType, aProgressInfo.mCurrentItemNumber, aProgressInfo.mTotalItems);
            } else if (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_OPERATION_SENT_DATA) {
                if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_CALENDAR) {
                    // TODO: localize this text!!!
                    //mProgressText.setText(R.string.ept_sent_calendar);
                } else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_CONTACTS) {
                    //mProgressText.setText(R.string.ept_sent_contacts);
                } else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_SMS_MESSAGES) {
                    //mProgressText.setText(R.string.ept_sent_messages);
                } else if (aProgressInfo.mDataType == EMDataType.EM_DATA_TYPE_CALL_LOGS) {
                    //mProgressText.setText(R.string.ept_sent_call_logs);
                }
            } else if (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_TEXT_RESTORE_COMPLETED) {
                int dataType = aProgressInfo.mDataType;
                DLog.log("isRestoreCompleted for Data type - (" + dataType + ") - is: " + isRestoreCompleted(dataType));
                if (isRestoreCompleted(dataType) && !transactionResumed) {
                    DLog.log("started handler for displaying summery screen");
                    //Need to trigger timer for summery page(restore completed)
                    handler.sendEmptyMessageDelayed(DISPLAY_FINAL_SUMMERY_SCREEN, 10 * 1000);
//                    if (EMMigrateStatus.getTotalSizeTobeTransfered() == EMMigrateStatus.getTransferredFilesSize()) {
                    DLog.log("showProgressDialog--3");
                    showProgressDialog(getString(R.string.generating_summary_title), getString(R.string.generating_summary_description));
//                    }
                }
            } else if (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_FINDING_FILES) {
                // mProgressText.setText(R.string.ept_finding_files);
                //mProgressDetailText.setText("");
            } else if (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_CHECKING_REMOTE_STORAGE_SPACE) {
                //mProgressText.setText(R.string.ept_checking_remote_storage_space);
                //mProgressDetailText.setText("");
            } else {
                //  mProgressText.setText("");
            }
        }
    }

    @Override
    public void haveBecomeSource() {
        DashboardLog.getInstance().setThisDest(false);
        mRole = THIS_DEVICE_IS_SOURCE;
    }

    @Override
    public void haveBecomeTarget() {
        DashboardLog.getInstance().setThisDest(true);
        mRole = THIS_DEVICE_IS_TARGET;
    }

    private void changeConnectionCheckTime() {
        try {
            if (Constants.PLATFORM.equalsIgnoreCase(PLATFORM_BLACKBERRY) || PLATFORM_BLACKBERRY.equalsIgnoreCase(mRemoteDeviceInfo.dbDevicePlatform)) {
                CONNECTION_CHECK_TIMEOUT = 5 * CONNECTION_CHECK_TIMEOUT;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*** This method is called back after the handshake is done and devices are connected. ***/
    @Override
    public void pinOk() {
        DLog.log("In pinOk()");
        if (EMMigrateStatus.qrCodeWifiDirectMode()) {
			/*
			// Set the encryption password, depending on whether we are displaying or reading the QR code
			if (EMNetworkManagerClientUIHelper.getEncryptionPasscode() != null) {
				CMDCryptoSettings.setPassword(EMNetworkManagerClientUIHelper.getEncryptionPasscode());
			}
			else if (EMNetworkManagerHostUIHelper.getEncryptionPasscode() != null) {
				CMDCryptoSettings.setPassword(EMNetworkManagerHostUIHelper.getEncryptionPasscode());
			}
			*/
        } else {
            //Stop publishing service and Listening for service
            mRemoteDeviceManager.stop();

        }
        cloudPairingInprogress = false;
        mDisplayMMDSProgress = false;
        mRemoteDeviceInfo = DashboardLog.getInstance().destinationEMDeviceInfo;
        if (transactionResumed && MigrationStats.getInstance().getRemoteDeviceIMEI().equalsIgnoreCase(mRemoteDeviceInfo.dbDeviceIMEI)) {
            migrationStatus = Constants.MIGRATION_INPROGRESS;
            EMMigrateStatus.setmInstance(MigrationStats.getInstance().getMigrateStatus());
            resumeMigration();
            setLayout(EPTWizardPage.Progress, EPTTransitionReason.UserNext);
        } else {
            EMUtility.clearTransaction();
            // clear previously transfered data from map
            mRemoteDeviceManager.clearPreviouslyTransferredItems();
            if (mRole == THIS_DEVICE_IS_SOURCE) {
                mRemoteDeviceInfo = DashboardLog.getInstance().destinationEMDeviceInfo;
                if (mRemoteDeviceInfo.deniedPermissionsDataTypes != null) {
                    try {
                        String denied = mRemoteDeviceInfo.deniedPermissionsDataTypes;
                        if (denied != null) {
                            DLog.log("*** denied datatypes : " + mRemoteDeviceInfo.deniedPermissionsDataTypes);
                            int deniedDatatype = Integer.parseInt(mRemoteDeviceInfo.deniedPermissionsDataTypes);
                            disableDataTypes(deniedDatatype);
                        }
                    } catch (Exception e) {
                        DLog.log("Exception in pinOk > Handling Denied Permissions : " + e);
                    }
                }
                disableUnsupportedContentTypes();
                if (FeatureConfig.getInstance().getProductConfig().isTransferTypeSelectionEnabled()) {
                    setLayout(EPTWizardPage.SelectTransferType, EPTTransitionReason.UserNext);
                } else {
                    setLayout(EPTWizardPage.SelectContent, EPTTransitionReason.UserNext);
                }

            } else if (mRole == THIS_DEVICE_IS_TARGET) {
                mRemoteDeviceInfo = DashboardLog.getInstance().sourceEMDeviceInfo;
                setLayout(EPTWizardPage.Connected, EPTTransitionReason.UserNext);
                DashboardLog.getInstance().setAppVersion();
                DashboardLog.getInstance().convertDevicesInfoToDB();
                DashboardLog.getInstance().geteDeviceSwitchSession().setTransferMode(mTransferMode);
                DashboardLog.getInstance().geteDeviceSwitchSession().setAdditionalInfo("Frequency=" + String.valueOf(CommonUtil.getInstance().getmFrequency()));
                DashboardLog.getInstance().updateSessionStatus(Constants.SESSION_STATUS.IN_PROGRESS);
                DashboardLog.getInstance().geteDeviceSwitchSession().setSessionStage(Constants.SESSION_STAGE.DEVICES_CONNECTED.value());
                DashboardLog.getInstance().updateToServer(true);
            }
        }
        if (CommonUtil.getInstance().isGroupOwner()) {
            registerReceiver(wiFiObserver, new IntentFilter(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION));
        } else {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Constants.ACTION_RECONNECT_WIFI);
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            registerReceiver(wiFiObserver, intentFilter);
        }
        if (mTransferMode.equalsIgnoreCase("WLAN")) {
            unregisterReceiver(wiFiObserver);
            registerReceiver(wiFiObserver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
        devicesPaired = true;
        addOverLayMessage(OVERLAY_MIGRATION_INPROGRESS, false);
        if (CLOUD_PAIRING_ENABLED)
            stateMachine.endSession();
        //Fix for App crashing issue,while chrome updates(Making webview null).
        EMGlobals.setJavascriptWebView(null);
        changeConnectionCheckTime();
        NetworkUtil.logNetworkDetails();
        addOverLayMessage(OVERLAY_MIGRATION_INPROGRESS, false);
        MigrationStats.getInstance().setmRole(mRole);
        MigrationStats.getInstance().setRemoteDeviceIMEI(mRemoteDeviceInfo.dbDeviceIMEI);
        MigrationStats.getInstance().setRemoteDevicePlatform(mRemoteDeviceInfo.dbDevicePlatform);

        //Releasing the camera
        if (cloudPairing_scanner != null && cloudPairing_scanner.getBarcodeView() != null) {
            cloudPairing_scanner.getBarcodeView().pause();
        }
        if (barcodeView != null && barcodeView.getBarcodeView() != null) {
            barcodeView.getBarcodeView().pause();
        }
        LogReporting.setApplicationService(CrashService.class); // Setting service class to Crashlytics library to initiate when app crashed.
    }

    public WifiManager wifiManager;
//    public void connectToWifi(String ssid, String key) {
//
//        Log.e(TAG, "connection wifi pre Q");
//        WifiConfiguration wifiConfig = new WifiConfiguration();
//        wifiConfig.SSID = "\"" + ssid + "\"";
//        wifiConfig.preSharedKey = "\"" + key + "\"";
//        int netId = wifiManager.addNetwork(wifiConfig);
//        if (netId == -1) netId = getExistingNetworkId(wifiConfig.SSID);
//
//        wifiManager.disconnect();
//        wifiManager.enableNetwork(netId, true);
//        wifiManager.reconnect();
//    }

    private void resumeMigration() {
        mRestoreOriginalSMSApp = isDefaultSMSApp();
        elapsedTimeInsec = MigrationStats.getInstance().getElapsedTime();
        elapsedTimer.start();
        if (mRole == THIS_DEVICE_IS_SOURCE) {
            CommonUtil.getInstance().setLinkSpeed(5 * 1024 * 1024);
            mRemoteDeviceManager.sendData(MigrationStats.getInstance().getDataTypesTobeCompleted());
            elapsedTime.setVisibility(View.VISIBLE);
            estimationTime.setVisibility(View.VISIBLE);
            estimationTime.setText(Html.fromHtml(MigrationStats.getInstance().getEstimationTime()));
            handler.sendEmptyMessageDelayed(UPDATE_PROGRESS, 1 * 1000);
        }
    }

    @Override
    public void cryptoPasswordRequested() {
        if (EMMigrateStatus.qrCodeWifiDirectMode()) {
            String passcode = EMNetworkManagerClientUIHelper.getEncryptionPasscode();
            if (passcode == null) {
                passcode = EMNetworkManagerHostUIHelper.getEncryptionPasscode();
            }
            mRemoteDeviceManager.setCryptoPassword(passcode);
        } else {
            if (mRole == THIS_DEVICE_IS_TARGET) {
                mRemoteDeviceManager.setCryptoPassword(mPin);
            } else {
                if (mCurrentPage == EPTWizardPage.EnterPin) {
                    TextView incorrectPinTextView = (TextView) findViewById(R.id.incorrectPinTextView);
                    incorrectPinTextView.setVisibility(View.VISIBLE);
                    hideProgressDialog();
                } else {
                    setLayout(EPTWizardPage.EnterPin, EPTTransitionReason.UserNext);
                }
            }
        }
    }

	/*
	@Override
	public void wifiCryptoComplete() {
		sendData();
	}
	*/

    @Override
    public void remoteDeviceManagerError(int aError) {

    }

    @Override
    public void deviceListChanged() {
        // TODO Auto-generated method stub
        ArrayList<EMDeviceInfo> devices = mRemoteDeviceManager.mRemoteDeviceList.mRemoteDevices;
        ArrayAdapter<EMDeviceInfo> adapter = new ArrayAdapter<EMDeviceInfo>(this,
                android.R.layout.simple_list_item_1, mRemoteDeviceManager.mRemoteDeviceList.mRemoteDevices);

        mDeviceListView.setAdapter(adapter);
        if ((devices.size() > 0) && (mSearchingForDevicesProgressDialog != null) && (mSearchingForDevicesProgressDialog.isShowing()))
            mSearchingForDevicesProgressDialog.dismiss();
    }

    private EditText mPinEntryBox;

    // This is static because we find that it can be null in onActivityResult sometimes when we create a new account from the app
    // TODO: investigate this further - is it that we end up in a different instance of the activity?
    static private CMDBackupAndRestoreEngine mCloudService;

    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////

    private void raiseFatalError(String title, String body) {
        raiseFatalError(title, body, 0);
    }

    private void raiseFatalError(String title, String body, final int dialogType) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(title); // TODO: internationalize this

        // set dialog message
        alertDialogBuilder
                .setMessage(body)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ept_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (dialogType == 0) {
                            forceCloseApp();
                        } else if (dialogType == 2) {
                            EMUtility.clearTransaction();
                            dialog.dismiss();
//                            System.exit(0);
                        } else {
                            dialog.dismiss();
                        }
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    @Override
    public void taskError(int errorCode, boolean alreadyDisplayedDialog) {
        if (!alreadyDisplayedDialog) {
            switch (errorCode) {
                case CMDError.CMD_ERROR_DOWNLOADING_FILE:
                    raiseFatalError(getString(R.string.cmd_unable_to_download_backup_title),
                            getString(R.string.cmd_unable_to_download_backup_body));

                    break;
                case CMDError.CMD_ERROR_FINDING_REMOTE_FILE:
                    raiseFatalError(getString(R.string.cmd_unable_to_find_backup_title),
                            getString(R.string.cmd_unable_to_find_backup_body));

                    break;
                case CMDError.CMD_ERROR_CREATING_REMOTE_PATH:
                case CMDError.CMD_ERROR_UPLOADING_FILE:
                    raiseFatalError(getString(R.string.cmd_unable_to_create_backup_title),
                            getString(R.string.cmd_unable_to_create_backup_body));

                    break;
                case CMDError.CMD_ERROR_NOT_ENOUGH_SPACE_ON_TARGET_FILE_SYSTEM:
                    if (EMMigrateStatus.getTransportMode() == EMProgressInfo.EMTransportMode.EM_SD_CARD) {
                        raiseFatalError(getString(R.string.cmd_insufficient_sd_card_space_title),
                                getString(R.string.cmd_insufficient_sd_card_space_body));
                    } else {
                        raiseFatalError(getString(R.string.cmd_insufficient_google_drive_space_title),
                                getString(R.string.cmd_insufficient_google_drive_space_body));
                    }
                    break;

                case CMDError.CMD_GOOGLE_DRIVE_ACCESS_RECOVERABLE_AUTHENTICATION_ERROR:
                    // Do nothing - the user probably just needs to log in
                    break;
                default:
                    raiseFatalError(getString(R.string.cmd_insufficient_google_drive_space_title),
                            getString(R.string.cmd_insufficient_google_drive_space_body));
                    break;
            }
        }
    }

    @Override
    public void taskComplete(boolean aSuccess) {
        if (mCurrentPage == EPTWizardPage.ConnectingToCloudService) {
            if (mOperationMode == EPTOperationMode.CloudRestore) {
                sendData();
            } else {
                setLayout(EPTWizardPage.SelectContent, EPTTransitionReason.UserNext);
            }
        } else if (mCurrentPage == EPTWizardPage.Progress) {
           /* mContactsSummary.setText(String.format(getString(R.string.ept_select_content_contacts) + " : %d", mTotalContactsTransferred));
            mCalendarSummary.setText(String.format(getString(R.string.ept_select_content_calendar) + " : %d", mTotalCalendarEntriesTransferred));
            mPhotosSummary.setText(String.format(getString(R.string.ept_select_content_photos) + " : %d", mTotalPhotosTransferred));
            mVideosSummary.setText(String.format(getString(R.string.ept_select_content_videos) + " : %d", mTotalVideosTransferred));
           *//* if(mAudioCheckBox.isChecked()) {
                mAudioSummary.setVisibility(View.VISIBLE);
                mAudioSummary.setText(String.format(getString(R.string.ept_select_content_audio) + " : %d", mTotalAudioTransferred));
            }
            if(mMessagesCheckBox.isChecked()) {
                mMessagesSummary.setVisibility(View.VISIBLE);
                mMessagesSummary.setText(String.format(getString(R.string.ept_select_content_messages) + " : %d", mTotalMessagesTransferred));
            }*/
//    		mWakeLock.release();
//    		setWakeLock(false);
            setLayout(EPTWizardPage.Complete, EPTTransitionReason.UserNext);
        }
    }

    private final static String BUNDLE_INFO_CLOUD_SERVICE_TYPE = "CloudServiceType";
    private final static String BUNDLE_INFO_OPERATION_TYPE_KEY = "OperationMode";
    private final static String BUNDLE_INFO_CURRENT_SCREEN = "CurrentScreen";
    private final static String BUNDLE_INFO_SELECTED_ACCOUNT_NAME = "SelectedAccountName";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        DLog.log("onSaveInstanceState mCurrentPage " + mCurrentPage);

        if (mCurrentPage != null)
            outState.putInt(BUNDLE_INFO_CURRENT_SCREEN, mCurrentPage.ordinal());

        outState.putInt(BUNDLE_INFO_CLOUD_SERVICE_TYPE, mCloudServiceType);

        if (mOperationMode != null)
            outState.putInt(BUNDLE_INFO_OPERATION_TYPE_KEY, mOperationMode.ordinal());

        if (mSelectedAccountName != null)
            outState.putString(BUNDLE_INFO_SELECTED_ACCOUNT_NAME, mSelectedAccountName);
    }

    private static boolean isDefaultSMSApp() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(context);
            String thisPackage = context.getPackageName();
            return defaultSmsApp.equals(thisPackage);
        }
        return true;
    }


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        DLog.log("onActivityResult: requestCode:" + requestCode + " resultCode: " + resultCode + " , Page : " + mCurrentPage.name());

        //101 is message write request
        if (requestCode == EMUtilsDefaultSmsApp.REQUEST_CODE_SMSAPP_DEFAULT) {
            boolean isDefaultapp = isDefaultSMSApp();
            DLog.log("Default SMS App " + isDefaultapp);
            //If our app was granted permission, all is good
            additionalResponse = "";
            if (isDefaultapp) {
                if (IS_MMDS && !"Android".equalsIgnoreCase(remotePlatform)) {
                    (new Handler()).postDelayed(smsRunnable, 100);
                }
                mRestoreOriginalSMSApp = true;
                messagePermissionGranted = true;
                userMadeMessageSelection = true;

                //If handler is registered, notify it as well
                if (mSmsPermissionHandler != null) {
                    mSmsPermissionHandler.userAccepted();
                }
            } else if (IS_MMDS) {
                if (mSmsPermissionHandler != null) {
                    mSmsPermissionHandler.userDenied();
                }
            } else {
                //If this is first call, firstMessagePermissionDenied will be false
                if (!firstMessagePermissionDenied) {
                    firstMessagePermissionDenied = true;
                    //Show prompt again
                    if (messageNotGrantedDialog != null) {
                        messageNotGrantedDialog.dismiss();
                    }

                    //Show prompt warning user that message permission is required. Give him chance to grant it again.
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                    alertDialogBuilder.setTitle(getString(R.string.msg_permission_title));
                    alertDialogBuilder
                            .setMessage(getString(R.string.msg_permission_description))
                            .setCancelable(false)

                            .setNegativeButton(getString(R.string.msg_permission_yes), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    DLog.log("Grant permission selected");
                                    EasyMigrateActivity.getDefaultSMSAppPermission(null);
                                }
                            })

                            .setPositiveButton(getString(R.string.msg_permission_no), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    DLog.log("Didn't grant permission");
                                    DLog.log("Grant permission not selected, igoned");

                                    //User has decided not to grant the permission
                                    userMadeMessageSelection = true;

                                    //If handler is registered, notify it as well
                                    if (mSmsPermissionHandler != null) {
                                        mSmsPermissionHandler.userDenied();
                                    }
                                }
                            });

                    messageNotGrantedDialog = alertDialogBuilder.create();
                    messageNotGrantedDialog.show();
                }
                //This is second failed attempt. User has made selection and not decided to grant permission.
                else {
                    userMadeMessageSelection = true;

                    //If handler is registered, notify it as well
                    if (mSmsPermissionHandler != null) {
                        mSmsPermissionHandler.userDenied();
                    }
                }
            }
        } else if (requestCode == SETTINGS_WRITE_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (mCurrentPage == EPTWizardPage.SelectOldOrNewDevice) {
                    if (Settings.System.canWrite(this)) {
                        DLog.log("onACtivityResult callback : WRITE_SETTINGS_PERMISSION accessed");
                    } else {
                        DLog.log("onACtivityResult callback : WRITE_SETTINGS_PERMISSION denied");
                        settingsPermissionDialog(context.getResources().getString(R.string.hotspot_permission_message), true);
                    }
                } else if (mCurrentPage == EPTWizardPage.Welcome) {
                    if (anyPermissionRequired()) {
                        if (IS_MMDS) {
                            DLog.log("should start MMDS after permission here..............");
                            //initMMDS();
                            sendResponse(outputStreamForME, Constants.WDS_OK);
                        } else {
                            setLayout(EPTWizardPage.Permissions, EPTTransitionReason.UserNext);
                        }
                    } else {
                        if (IS_MMDS) {
                            DLog.log("should start MMDS after permission here..............");
                            //initMMDS();
                            sendResponse(outputStreamForME, Constants.WDS_OK);
                        } else {
                            //call all files access here
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                if (Environment.isExternalStorageManager()) {
                                    setLayout(EPTWizardPage.SelectOldOrNewDevice, EPTTransitionReason.UserNext);
                                } else {
                                    displayAllFilesAccessPermissionsDialog(getString(R.string.app_install_permissions_str), getString(R.string.str_all_files_access_permission));
                                }

                            } else {
                                setLayout(EPTWizardPage.SelectOldOrNewDevice, EPTTransitionReason.UserNext);
                            }
                        }
                    }
                } else {
                    if (Settings.System.canWrite(this)) {
                        DLog.log("onACtivityResult callback : WRITE_SETTINGS_PERMISSION accessed");
                        mSettingsPermissionHandler.userAccepted();
                    } else {
                        DLog.log("onACtivityResult callback : WRITE_SETTINGS_PERMISSION denied");
                        mSettingsPermissionHandler.userDenied();
                    }
                }
            }
        } else if (requestCode == APP_PERMISSIONS_REQUSET_CODE) {
            DLog.log("onACtivityResult callback called : permissions from settings page");
            checkAndRequestPermissions(true);
        } else if (requestCode == ALL_FILES_ACCESS_PERMISSION) {
            DLog.log("onACtivityResult callback called : permissions from settings page");
            setLayout(EPTWizardPage.SelectOldOrNewDevice, EPTTransitionReason.UserNext);
        } else if (requestCode == startLocationAlert.REQUEST_CHECK_SETTINGS) {
            if (locationRequestFromP2P) {
                locationRequestFromP2P = false;
                handler.postDelayed(startP2PThread, 100);
            } else if (resultCode == -1) {
                getLatLong();
            }
        } else if (requestCode == EMUtilsDefaultSmsApp.REQUEST_CODE_CHANGEBACK_SMSAPP) {
            DLog.log("callback default smsapp");
            if (IS_MMDS && isFromRestoredSmsHangler) {
                DLog.log("callback default smsapp case 1");
                restoredSmsHangler.sendEmptyMessage(2);
            } else {
                mRestoreOriginalSMSApp = false;
                DLog.log("callback default smsapp case 2");
                mNextButton.callOnClick();
            }
        } else if (requestCode == CUSTOMER_SATISFACTION_REQUEST_CODE) {
            finish();
//            System.exit(0);
/*            String customerRating = data.getStringExtra("customer_rating");
            DashboardLog.getInstance().addAdditionalInfo("CustomerRating="+customerRating);
            DashboardLog.getInstance().updateToServer(true);
            boolean dontCheckEndFeature = data.getBooleanExtra("dontCheckEndFeature", false);
            if(CommonUtil.getInstance().isSource() && !dontCheckEndFeature) {
                if (FeatureConfig.getInstance().getProductConfig().isDataWipeEnabled()) {
                    showWipeConfirmDialog();
                } else if (FeatureConfig.getInstance().getProductConfig().isAccAndPinRemovalEnabled()) {
                    showAccLockConfirmDialog();
                }
            } else {
                if (!dontCheckEndFeature && FeatureConfig.getInstance().getProductConfig().isUninstallRequired() &&
                        (!Constants.IS_MMDS && !BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT))) {
                    UninstallBroadcastReceiver.startUninstallAlarm(EasyMigrateActivity.this);
                }
                finish();
                System.exit(0);
            }*/
        } else if (requestCode == INSTALL_UNKNOWN_APPS_REQ_CODE) {

            DLog.log("enter case requestCode == INSTALL_UNKNOWN_APPS_REQ_CODE");
            DLog.log("enter case requestCode == INSTALL_UNKNOWN_APPS_REQ_CODE AppMigrateUtils.restoreAppList size " + AppMigrateUtils.restoreAppList.size());
            launchAppsInstallScreen();
        }

        if ((mOperationMode == EPTOperationMode.CloudBackup)
                || (mOperationMode == EPTOperationMode.CloudRestore)) {

            if (mCloudService == null) {
                // Log.d(TAG, "mCloudService is null - the activity was probably previously closed, so recreate the cloud service object");
                mCloudService = new CMDBackupAndRestoreEngine(CMDBackupAndRestoreServiceType.CMD_GOOGLE_DRIVE, this, this); // TODO: assume it's google for now
            }

            switch (requestCode) {
                case CMDError.RESOLVE_CONNECTION_REQUEST_CODE:
                    if (mSelectedAccountName == null) {
                        // Log.d(TAG, "Not already got an account name, so start start loging without it...");
                        mCloudService.startUserLoginAsync(this);
                    } else
                        // Log.d(TAG, "Already got an account name, so start start loging with it: " + mSelectedAccountName);
                        mCloudService.startLogInWithAccountNameAsync(mSelectedAccountName, this, (mOperationMode == EPTOperationMode.CloudRestore));
                    break;
                case CMDError.SELECTING_ACCOUNT:
                    if (resultCode == RESULT_OK) {
                        // Log.d(TAG, "SELECTING_ACCOUNT complete...");
                        // Log.d(TAG, "data: " + data.toString());
                        // Log.d(TAG, "mCloudService: " + mCloudService.toString());
                        mSelectedAccountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                        // Log.d(TAG, "accountName: " + mSelectedAccountName);
                        mCloudService.startLogInWithAccountNameAsync(mSelectedAccountName, this, (mOperationMode == EPTOperationMode.CloudRestore));
                        // Log.d(TAG, "started login with account name");
                    }
                    // TODO: handle bad result - currently we just ignore
                    break;
            }
        }
    }

    private void creatingNetwork() {
		/*
		if (mCreatingNetworkProgressDialog == null) {
			mCreatingNetworkProgressDialog = new ProgressDialog(this);
			mCreatingNetworkProgressDialog.setTitle(this.getString(R.string.ept_creating_network_title));
			mCreatingNetworkProgressDialog.setMessage(this.getString(R.string.ept_creating_network_text));
		}

		mCreatingNetworkProgressDialog.show();
		*/

        showProgressDialog(getString(R.string.ept_creating_network_title), getString(R.string.ept_creating_network_text));
    }

    private void notCreatingNetwork() {
        hideProgressDialog();
/*		if (mCreatingNetworkProgressDialog != null) {
			mCreatingNetworkProgressDialog.hide();
		} */
    }

    private void connectingToNetwork() {
		/*
		if (mConnectingToNetworkProgressDialog == null) {
			mConnectingToNetworkProgressDialog = new ProgressDialog(this);
			mConnectingToNetworkProgressDialog.setTitle(this.getString(R.string.ept_connecting_to_network_title));
			mConnectingToNetworkProgressDialog.setMessage(this.getString(R.string.ept_connecting_to_network_text));
		}

		mConnectingToNetworkProgressDialog.show();
		*/
        showProgressDialog(getString(R.string.ept_connecting_to_network_title), getString(R.string.ept_connecting_to_network_text));
    }

    private void notConnectingToNetwork() {
        // hideProgressDialog(); // Don't hide the progress dialog - wait until we advance to the next screen (we want the dialog to be displayed while SRP negotiation completes)
		/*
		if (mConnectingToNetworkProgressDialog != null) {
			mConnectingToNetworkProgressDialog.hide();
		} */
    }

    // private ProgressDialog mCreatingNetworkProgressDialog;
    // private ProgressDialog mConnectingToNetworkProgressDialog;
    private String mPin;


    //START - PERVACIO


    private void changeButtonStatus(boolean setEnable) {
        if (setEnable) {
            btn_start_migration.setEnabled(true);
            btn_start_migration.setAlpha(1.0f);
        } else {
            btn_start_migration.setEnabled(false);
            btn_start_migration.setAlpha(0.5f);
        }
    }

    private CountDownTimer elapsedTimer = new CountDownTimer(ELAPSED_TIME, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            ++elapsedTimeInsec;
            if (mRole == THIS_DEVICE_IS_SOURCE) {
                String sourceString = getResources().getString(R.string.elapsed_time) + "<b>" + " " + EMUtility.getReadableTime(EasyMigrateActivity.this, elapsedTimeInsec * 1000, false) + "</b>";
                elapsedTime.setText(Html.fromHtml(sourceString));
            }
        }

        @Override
        public void onFinish() {
            this.start();
        }
    };

    private long elapsedTimeInsec = 0;
    private static final long ELAPSED_TIME = 5 * 60 * 60 * 1000L;
    public CommandHandlerForME mCommandHandlerForME = null;
    OutputStream outputStreamForME = null;
    private RelativeLayout mainLayout;
    private RelativeLayout mmds_progressLayout, mmds_connetingLayout;
    private boolean mDisplayMMDSProgress = false;
    private static String remoteDevice = "";
    private boolean isRemoteDeviceDualBand = true;
    private AlertDialog failedDialog, wipeConfirmDialog;
    private AlertDialog cancelDialog;
    private AlertDialog interruptedDialog;
    private AlertDialog messageNotGrantedDialog;
    private boolean isMigrationInProgress = false;
    private boolean isInterrupted = false;
    private boolean isDeviceSelectionDone = false;
    private String sentCommandToRemote = "";
    private boolean mRestoreOriginalSMSApp = false;

    private static String additionalResponse = "";

    //Buttons

    Button thisIsSourceDeviceButton;
    Button thisIsDestinationDeviceButton;
    Button otherDeviceIsIOSButton;
    Button otherDeviceIsAndroidButton;
    DecoratedBarcodeView barcodeView, cloudPairing_scanner;
    private static final int OVERLAY_MIGRATION_INPROGRESS = 0;
    public static final int DEVICE_DISCONNECTED = 101;
    private static final int REMOTE_DEVICE_DISCONNECTED = 102;
    private static final int DEVICE_DISCONNECTED_EVENT = 1111;
    private static final int MIGRATION_CANCELLED_EVENT = 2222;
    public static boolean devicesPaired = false;

    private void sendCommandToRemote(String command) {
        sentCommandToRemote = command;
        if (Constants.stopMigration) {
            handler.sendEmptyMessageDelayed(SEND_QUEUED_MESSAGE, 5 * 1000L);
        } else {
            if (mRemoteDeviceManager != null) {
                mRemoteDeviceManager.sendTextCommand(command);
            }
        }
    }


    private void setDefaultModes() {
//        if (Constants.mTransferMode.equalsIgnoreCase("Cloud")) {
//            EMMigrateStatus.setTransportMode(EMProgressInfo.EMTransportMode.EM_CLOUD);
//            mCurrentPage = EPTWizardPage.SelectBackupOrRestore;
//        } else if (Constants.mTransferMode.equalsIgnoreCase("WLAN")) {
        mOperationMode = EPTOperationMode.LocalWiFi;
        EMMigrateStatus.setQrCodeWifiDirectMode(true);
//        } else {
//            mOperationMode = EPTOperationMode.LocalWiFi;
//            EMMigrateStatus.setQrCodeWifiDirectMode(true);
//            enableWifi(true);
//        }
        disableWifiDirect();
        if (!REESTIMATION_REQUIRED) {
            displayRemainingTime = false;
            Constants.ESTIMATION_LOWERLIMIT = 1.0f;
            Constants.ESTIMATION_UPPERLIMIT = 2.0f;
            Constants.ESTIMATION_CALCULATION_TIME = 10 * 1000L;
        }
        connectedNetworkId = NetworkUtil.getConnectedNetworkId();
    }

    private void addOverLayMessage(int type, boolean status) {
        try {
            DLog.log("addOverLayMessage");
            TextView textView = (TextView) findViewById(R.id.overLayMessage);
            if (status) {
                mainLayout.setVisibility(View.GONE);
                mmds_progressLayout.setVisibility(View.VISIBLE);
                switch (type) {
                    case DEVICE_DISCONNECTED:
                        mmds_connetingLayout.setVisibility(View.GONE);
                        textView.setVisibility(View.VISIBLE);
                        textView.setText(R.string.device_disconnection_message);
                        break;
                    case REMOTE_DEVICE_DISCONNECTED:
                        mmds_connetingLayout.setVisibility(View.GONE);
                        textView.setVisibility(View.VISIBLE);
                        if (mRole == THIS_DEVICE_IS_SOURCE) {
                            textView.setText(R.string.destination_device_disconnection_message);
                        } else {
                            textView.setText(R.string.source_device_disconnection_message);
                        }
                        break;
                    case OVERLAY_MIGRATION_INPROGRESS:
                        mDisplayMMDSProgress = true;
                        mmds_connetingLayout.setVisibility(View.VISIBLE);
                        textView.setVisibility(View.GONE);
                        break;
                    default:
                        break;
                }
            } else {
                mainLayout.setVisibility(View.VISIBLE);
                mmds_progressLayout.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            DLog.log(e);
        }
    }

    private Handler migrationInterrupted = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == DEVICE_DISCONNECTED_EVENT) {
                forceCloseApp();
            } else if (msg.what == MIGRATION_CANCELLED_EVENT) {
                if (FeatureConfig.getInstance().getProductConfig().isReportProblemEnabled()) {
                    File externalStorage = context.getFilesDir();
                    EMUtility.copyFile(new File(externalStorage.toString() + "/log.txt"), new File(externalStorage.toString() + "/reportlog.txt"));
                    startActivity(new Intent(EasyMigrateActivity.this, ReportAProblem.class));
                }
                initializeTransactionService(DashboardLog.getInstance().isThisDest());
                setLayout(EPTWizardPage.SessionCancel, EPTTransitionReason.Automatic);
            }
        }
    };

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault());

    private String currentDate() {
        return dateFormat.format(new Date());
    }

    String messagesXmlPath = "/data/local/tmp/pva/pimrestore/"; //"/sdcard/wds/";
    Runnable smsRunnable = new Runnable() {
        public void run() {
            try {
                DLog.log("In Runnable totalSMSCount : " + totalSMSCount);
                contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).setProgressCount(0);
                contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).setTotalCount(totalSMSCount);
                DLog.log("Start Message restore form  : " + messagesXmlPath + "messages.xml");
                FileInputStream inputStreamMessages = new FileInputStream(messagesXmlPath + "messages.xml");
                addMessagesFromXml(inputStreamMessages, outputStreamForME);
                DLog.log("End Message restore form  : " + messagesXmlPath + "messages.xml");
            } catch (FileNotFoundException e) {
                iosSMSRestored = true;
                iosSMSFailed = true;
                DLog.log("Command : " + Log.getStackTraceString(e));
            }
        }
    };

    boolean isFromRestoredSmsHangler = false;
    private Handler restoredSmsHangler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                EMMigrateStatus.addItemTransferred(EMDataType.EM_DATA_TYPE_SMS_MESSAGES);
                updateProgress(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_SMS_MESSAGES), totalSMSCount);
                DLog.log("Message Added...." + EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_SMS_MESSAGES));
            } else if (msg.what == 1) {
                DLog.log("restoreOriginalSmsApp from handler....");
                isFromRestoredSmsHangler = true;
                try {
//                    EMUtilsDefaultSmsApp emUtilsDefaultSmsApp = new EMUtilsDefaultSmsApp(context, activity);
//                    emUtilsDefaultSmsApp.restoreOriginalSmsApp();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (msg.what == 2) {
                DLog.log("Unblocking SMS restored....");
                iosSMSRestored = true;
                iosSMSFailed = false;
                if (iosOnlySMSSelected) {
                    setLayout(EPTWizardPage.Complete, EPTTransitionReason.UserNext);
                    //sendCommandToRemote(EMStringConsts.EM_COMMAND_RESTORE_COMPLETED + ":" + EMDataType.EM_DATA_TYPE_SMS_MESSAGES);
                }
            }
        }
    };

    public static boolean iosSMSRestored = true;
    public static boolean iosSMSFailed = false;
    public static boolean iosOnlySMSSelected = false;

    int totalSMSCount = 0;
    public static String remotePlatform = "";

    private void addMessagesFromXml(InputStream aInputStream, OutputStream aOutputStream) {

        SAXParserFactory saxFactory = SAXParserFactory.newInstance();

        int counter = 0;
        try {
            SAXParser parser = saxFactory.newSAXParser();

            // Parse XML file
            MessagesXmlParser messagesParser = new MessagesXmlParser(restoredSmsHangler, EasyMigrateActivity.this, aOutputStream, counter);
            //publishProgress(STATE_ADDING_MESSAGES);

            parser.parse(aInputStream, messagesParser);

            //contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).setProgressCount(2);
            AppUtils.printLog(TAG, currentDate() + "  Method : Adding Messages ", null, AppUtils.LogType.INFO);

        } catch (ParserConfigurationException e) {
            // Log and Fail
            e.printStackTrace();
            AppUtils.printLog(TAG, currentDate() + "  Exception : addMessagesFromXml ", e, AppUtils.LogType.EXCEPTION);
            DLog.log(e);
        } catch (SAXException e) {
            // Log and Fail
            e.printStackTrace();
            AppUtils.printLog(TAG, currentDate() + "  Exception : addMessagesFromXml ", e, AppUtils.LogType.EXCEPTION);
            DLog.log(e);
        } catch (IOException e) {
            // Log and Fail
            e.printStackTrace();
            AppUtils.printLog(TAG, currentDate() + "  Exception : addMessagesFromXml ", e, AppUtils.LogType.EXCEPTION);
            DLog.log(e);
        } catch (Exception eofException) {
            // Do nothing, we've just got to the end of the messages xml
            AppUtils.printLog(TAG, currentDate() + "  Exception : addMessagesFromXml ", eofException, AppUtils.LogType.EXCEPTION);
            AppUtils.printLog(TAG, currentDate() + "  End of messages ", eofException, AppUtils.LogType.INFO);
        } finally {

        }
    }

    private class CommandHandlerForME extends AsyncTask<Void, String, String> {
        static final int MAX_COMMAND_LENGTH = 1024 * 1024; //1 MB


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DLog.log("onPreExecute");
            addOverLayMessage(OVERLAY_MIGRATION_INPROGRESS, true);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            String command = values[0];

            if (!command.contains(Constants.WDS_SEND_SESSION_JSON)) {
                // Log.d(TAG, "Received command From ME : " + command);
                // Toast.makeText(getApplicationContext(), command, Toast.LENGTH_SHORT).show();
            }

            if (command.equalsIgnoreCase(Constants.WDS_OSTYPE_REQUEST)) {
                sendResponse(outputStreamForME, Constants.WDS_ANDROID_RESPONSE);
            } else if (command.contains(Constants.WDS_SOURCE_HEADSUP)) {
                sendResponse(outputStreamForME, Constants.WDS_OK);
                thisIsSourceDeviceButton.callOnClick();
            } else if (command.equalsIgnoreCase(Constants.WDS_ARE_YOU_ALIVE)) {
                sendResponse(outputStreamForME, Constants.WDS_OK);
            } else if (command.contains(Constants.WDS_DEST_HEADSUP)) {
                DLog.log("CMD :" + command);
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(command.substring(Constants.WDS_DEST_HEADSUP.length() + 1));
                    if (jsonObject != null && !"".equals(jsonObject)) {
                        if (jsonObject.has("remotePlatform"))
                            remotePlatform = jsonObject.getString("remotePlatform");
                    }
                } catch (Exception e) {
                    remotePlatform = "Android";
                    DLog.log("Exception in getting remote device platform " + e.getMessage());
                }
                DLog.log("remotePlatform :" + remotePlatform);
                if ("Android".equalsIgnoreCase(remotePlatform)) {
                    if (initMMDS()) {
                        sendResponse(outputStreamForME, Constants.WDS_OK);
                    }
                } else {
                    sendResponse(outputStreamForME, Constants.WDS_OK);
                }
                thisIsDestinationDeviceButton.callOnClick();
            } else if (command.contains(Constants.WDS_REMOTE_DEVICE_ANDROID) || command.contains(Constants.WDS_REMOTE_DEVICE_IOS)) {
                remoteDevice = command;
                sendResponse(outputStreamForME, Constants.WDS_OK);
            } else if (command.equalsIgnoreCase(Constants.WDS_GET_NETWORK_DETAILS_REQUEST)) {
                DLog.log("Calling display QR code layout from CommandHandler ");
                if (remoteDevice.equalsIgnoreCase(Constants.WDS_REMOTE_DEVICE_IOS) || remoteDevice.contains("ios")) {
                    int oSversion = 0;
                    try {
                        oSversion = Integer.parseInt(remoteDevice.substring(Constants.WDS_REMOTE_DEVICE_IOS.length() + 1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (oSversion < 11) {
                        mDisplayMMDSProgress = false;
                        addOverLayMessage(OVERLAY_MIGRATION_INPROGRESS, false);
                    }
                    otherDeviceIsIOSButton.callOnClick();
                } else {
                    if ((CommonUtil.getInstance().getMigrationStatus() == Constants.REVIEW_INPROGRESS)) {
                        if (mRole == THIS_DEVICE_IS_SOURCE) {
                            setLayout(EPTWizardPage.DisplayQRCode, EPTTransitionReason.UserNext);
                        } else {
                            otherDeviceIsAndroidButton.callOnClick();
                        }
                    } else {
                        //Due to connection lost,generating wdirect once again
                        createHostNetwork();
                    }
                }
            } else if (command.contains(Constants.COMMAND_NETWORK_DETAILS)) {
                //network_details:/192.168.49.1:DIRECT-3b-Galaxy S5:qa8B5wNi    TODO logic has to be changed while receiving n/w details
                String[] details = command.split(":");
                String ipAddress = details[1].replace("/", "");
                CommonUtil commonUtil = CommonUtil.getInstance();
                commonUtil.setmWiFiPeerSSID(details[2]);
                commonUtil.setmWiFiPeerPassphrase(details[3]);
                commonUtil.setmCryptoEncryptPass(details[4]);
                commonUtil.setmFrequency(Integer.parseInt(details[5]));
                commonUtil.setNetworkType(details[6]);
                InetAddress inetAddress = null;
                try {
                    inetAddress = InetAddress.getByName(ipAddress);
                    CommonUtil.getInstance().setmWiFiPeerAddress(inetAddress);
                } catch (Exception Ex) {

                }
                barcodeView = null;
                if ((CommonUtil.getInstance().getMigrationStatus() == Constants.REVIEW_INPROGRESS)) {
                    if (mRole == THIS_DEVICE_IS_TARGET) {
                        disableWifiDirect();
                        setLayout(EPTWizardPage.ScanQRCode, EPTTransitionReason.UserNext);
                    } else {
                        otherDeviceIsAndroidButton.callOnClick();
                    }
                } else
                    connectToClientNetwork();// Due to connection lost,connecting to client n/w one more time
            } else if (command.contains(Constants.COMMAND_GET_TRANSACTION_DETAILS)) {
                //Set session end time
                DashboardLog.getInstance().geteDeviceSwitchSession().setEndDateTime(String.valueOf(System.currentTimeMillis()));

                //Convert to Object to JSON String
                Gson gson = new Gson();
                sendResponse(outputStreamForME,
                        gson.toJson(DashboardLog.getInstance().geteDeviceSwitchSession()));
                CommonUtil.getInstance().setMigrationDone(true);
            } else if (command.equalsIgnoreCase(Constants.WDS_WAIT_FOR_MIGRATION_DONE)) {
                sendResponse(outputStreamForME, getMigrationSummary());
            } else if (command.contains(Constants.WDS_SEND_SESSION_JSON)) {
                //Process EDeviceSwitchSession object JSON
                DashboardLog.getInstance().processSessionJsonFromME(command);

                //Send OK to ME
                sendResponse(outputStreamForME, Constants.WDS_OK);
            } else if (command.equalsIgnoreCase(Constants.WDS_SRC_DISCONNECTED) || command.equalsIgnoreCase(Constants.WDS_DEST_DISCONNECTED)) {
                sendResponse(outputStreamForME, Constants.WDS_OK);
//                usbCableDisconnectionHandler(REMOTE_DEVICE_DISCONNECTED, true);
            } else if (command.equalsIgnoreCase(Constants.WDS_SRC_CONNECTED) || command.equalsIgnoreCase(Constants.WDS_DEST_CONNECTED)) {
                sendResponse(outputStreamForME, Constants.WDS_OK);
                migrationInterrupted.removeMessages(DEVICE_DISCONNECTED_EVENT);
                addOverLayMessage(OVERLAY_MIGRATION_INPROGRESS, !devicesPaired);
            } else if (command.equalsIgnoreCase(Constants.WDS_DEVICE_INFO)) {
                sendResponse(outputStreamForME, StateMachine.getInstance(EasyMigrateActivity.this, mRole).getDetails(StateMachine.DEVICE_INFO));
            } else if (command.contains(Constants.WDS_REMOTE_DEVICE_INFO)) {
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(command.substring(Constants.WDS_REMOTE_DEVICE_INFO.length() + 1));
                    if (jsonObject.has("platform"))
                        remoteDevice = jsonObject.getString("platform");

                    if (jsonObject.has("dualBandSupported")) {
                        isRemoteDeviceDualBand = Boolean.parseBoolean(jsonObject.getString("dualBandSupported"));
                    }
                    CommonUtil.getInstance().setRemoteDeviceDualBand(isRemoteDeviceDualBand);
                } catch (Exception e) {
                    DLog.log("Exception in getting remote device platform " + e.getMessage());
                }
                try {
                    if (jsonObject != null) {
                        if (jsonObject.has("fixedPort") && jsonObject.getInt("fixedPort") != EMConfig.FIXED_PORT_NUMBER)
                            EMConfig.FIXED_PORT_NUMBER = jsonObject.getInt("fixedPort");
                        if (jsonObject.has("dataTransferPort") && jsonObject.getInt("dataTransferPort") != EMServer.CONTENT_TRANSFER_PORT)
                            EMServer.CONTENT_TRANSFER_PORT = jsonObject.getInt("dataTransferPort");
                        DLog.log("Cloud Pairing :: updated ports : " + EMConfig.FIXED_PORT_NUMBER + " " + EMServer.CONTENT_TRANSFER_PORT);
                        PreferenceHelper.getInstance(getApplicationContext()).putIntegerItem("fixed_port", EMConfig.FIXED_PORT_NUMBER);
                    }
                } catch (Exception e) {
                    DLog.log("Exception in parsing remote device info " + e.getMessage());
                }
                Intent intent = new Intent(EasyMigrateActivity.this, EasyMigrateService.class);
                startService(intent);
                getApplicationContext().bindService(intent, connection, EasyMigrateActivity.BIND_AUTO_CREATE);
                sendResponse(outputStreamForME, Constants.WDS_OK);
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            // Log.d(TAG, "CommandHandlerForME  doInBackground().....");
            boolean localPortInUse = EMUtility.isLocalPortInUse(Constants.ME_PORT_NUMBER);
            if (localPortInUse) {
                Constants.ME_PORT_NUMBER = EMUtility.getOpenport();
            }

            String filePath = "/data/local/tmp/pva/";
            FileOutputStream fileOutputStream = null;
            OutputStreamWriter outputStreamWriter = null;
            try {
                File portNumberFile = new File(filePath + Constants.PORT_NUMBER_FILE_NAME);
                ;
                fileOutputStream = new FileOutputStream(portNumberFile);
                outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                File filePathDirectory = new File(portNumberFile.getParent());
                if (!filePathDirectory.exists()) {
                    DLog.log("Trying to create file Directory : " + filePathDirectory);
                    filePathDirectory.mkdirs();
                }
                portNumberFile.createNewFile();
                outputStreamWriter.append("TransferPortNumber=" + Constants.ME_PORT_NUMBER);
                outputStreamWriter.flush();
            } catch (Exception e) {
                DLog.log("Exception in writing PortNumber.txt file : " + e.getMessage());
            } finally {
                if (outputStreamWriter != null) {
                    try {
                        outputStreamWriter.close();
                    } catch (Exception e) {
                        DLog.log("Exception in closing outputStream writer " + e.getMessage());
                    }
                }
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (Exception e) {
                        DLog.log("Exception in closing file outputStream " + e.getMessage());
                    }
                }
            }

            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(Constants.ME_PORT_NUMBER); // TODO: get rid of the magic number
                do {
                    DLog.log("Waiting for ME connection");
                    Socket socket = serverSocket.accept();
                    DLog.log("Me Client connected");
                    InputStream inputStream = socket.getInputStream();
                    outputStreamForME = socket.getOutputStream();
                    do {
                        final String command = waitForCommand(inputStream);

                        if (command != null) {
                            if (!command.contains(Constants.WDS_SEND_SESSION_JSON)) {
                                DLog.log("Command From ME : " + command);
                            }
                            if (command.equalsIgnoreCase(WDS_MIGRATION_STATUS)) {
                                migrationStatus = CommonUtil.getInstance().getMigrationStatus();
                                sendResponse(outputStreamForME, Constants.WDS_STATUS + ":" + migrationStatus + additionalResponse);
                            } else if (command.equalsIgnoreCase("wds_add_messages_failed")) {
                                iosSMSRestored = true;
                                iosSMSFailed = true;
                            } else if (command.contains("wds_add_messages")) {
                                try {
                                    String countStr = command.substring(command.indexOf(":") + 1);
                                    totalSMSCount = Integer.parseInt(countStr);
                                    DLog.log(currentDate() + "Command : " + command);
                                    DLog.log(currentDate() + "In ME totalSMSCount : " + totalSMSCount);
                                    EasyMigrateActivity.getDefaultSMSAppPermission(null);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                publishProgress(command);
                            }
                        } else {
                            break;
                        }
                    } while (!isCancelled());
                } while (!isCancelled());
            } catch (Exception ex) {
                DLog.log("got execption in doInBackground(): " + ex.getMessage());
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        String waitForCommand(InputStream inputStream) {

            String command = null;

            byte[] commandBytes = new byte[MAX_COMMAND_LENGTH];

            try {
                boolean haveCompleteCommand = false;
                int totalBytesRead = 0;
                while (!haveCompleteCommand) {
                    byte byteRead = (byte) inputStream.read();

                    boolean ignoreByte = false;

                    if (totalBytesRead == 0) {
                        switch (byteRead) {
                            case 0x0a: // lf
                            case 0x0d: // cr
                            case 0x20: // space
                            case 0x09: // tab
                                ignoreByte = true;
                                break;
                            default:
                                break;
                        }
                    }

                    if (byteRead == -1) {
                        // We have an error reading the socket, so just exit
                        break;
                    }

                    if (!ignoreByte) {
                        commandBytes[totalBytesRead] = byteRead;
                        totalBytesRead += 1;

                        if (totalBytesRead > 2) {
                            if ((commandBytes[totalBytesRead - 2] == '\r')
                                    && (commandBytes[totalBytesRead - 1] == '\n')) {
                                // command = commandBytes.toString();
                                command = new String(commandBytes, 0, totalBytesRead - 2, "US-ASCII");
                                haveCompleteCommand = true;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                // Fail and log
                DLog.log(ex);
                ex.printStackTrace();
            }
            return command;
        }
    }

    public void sendResponse(final OutputStream aOutputStream, final String aString) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DLog.log("Sending response to ME - status - " + migrationStatus + " response " + aString + " " + mRole);
                byte[] bytesWithCrLf = new byte[aString.length() + 2];
                try {
                    byte[] bytesWithoutCrLf = aString.getBytes("US-ASCII");

                    System.arraycopy(bytesWithoutCrLf,
                            0,
                            bytesWithCrLf,
                            0,
                            bytesWithoutCrLf.length);

                    bytesWithCrLf[bytesWithoutCrLf.length] = '\r';
                    bytesWithCrLf[bytesWithoutCrLf.length + 1] = '\n';

                    try {
                        aOutputStream.write(bytesWithCrLf);
                    } catch (IOException e) {
                        // Fail
                        e.printStackTrace();
                        DLog.log(e);
                    }

                } catch (UnsupportedEncodingException e) {
                    // Fail and log
                    e.printStackTrace();
                    DLog.log(e);
                }
            }
        }).start();

    }

    private void disableWifiDirect() {
        try {
            DLog.log("<--disableWifiDirect");
            WifiP2pManager mManager = (WifiP2pManager) getApplicationContext().getSystemService(Context.WIFI_P2P_SERVICE);
            WifiP2pManager.Channel mChannel = mManager.initialize(this, this.getMainLooper(), null);

            if (mChannel != null && mManager != null) {
                mManager.removeGroup(mChannel, null);
            }
            DLog.log("disableWifiDirect-->");
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }


    private void disableDataType(CheckBox aCheckBox) {
        if (aCheckBox != null) {
            aCheckBox.setChecked(false);
            aCheckBox.setTextColor(getResources().getColor(R.color.black));
            aCheckBox.setEnabled(false);
        }
    }

    private void loadContentDetails() {
        new Thread() {
            @Override
            public void run() {
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CONTACTS) != null)
                    latestSelectionMap.put(EMDataType.EM_DATA_TYPE_CONTACTS, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CONTACTS).isSelected());
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR) != null)
                    latestSelectionMap.put(EMDataType.EM_DATA_TYPE_CALENDAR, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).isSelected());
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALL_LOGS) != null)
                    latestSelectionMap.put(EMDataType.EM_DATA_TYPE_CALL_LOGS, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALL_LOGS).isSelected());
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES) != null)
                    latestSelectionMap.put(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).isSelected());
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS) != null)
                    latestSelectionMap.put(EMDataType.EM_DATA_TYPE_PHOTOS, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).isSelected());
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO) != null)
                    latestSelectionMap.put(EMDataType.EM_DATA_TYPE_VIDEO, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).isSelected());
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC) != null)
                    latestSelectionMap.put(EMDataType.EM_DATA_TYPE_MUSIC, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).isSelected());
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_DOCUMENTS) != null)
                    latestSelectionMap.put(EMDataType.EM_DATA_TYPE_DOCUMENTS, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_DOCUMENTS).isSelected());
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SETTINGS) != null)
                    latestSelectionMap.put(EMDataType.EM_DATA_TYPE_SETTINGS, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SETTINGS).isSelected());
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP) != null)
                    latestSelectionMap.put(EMDataType.EM_DATA_TYPE_APP, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).isSelected());
                contentDetailsMap.clear();
                mCalendarsCount = mAppCount = mContactsCount = mPhotosCount = mVideosCount = mSettingsCount = mMessageCount = mAudioCount = mCallLogsCount = mDocumentsCount = 0;
                EMMigrateStatus.addContentDetails(EMDataType.EM_DATA_TYPE_SETTINGS, EMStringConsts.SETTINGS_LIST.size());
//                if (BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT))
//                    Constants.SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_APP, false);
//                if (FeatureConfig.getInstance().getProductConfig().isEnableAppTransfer()/*&& (movistarappsList.size()!=0)*/) {
//                else {
                Constants.SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_APP, true);
//                    Constants.SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_APP, false);
//                }
                Log.i(TAG, "isRemoteDeviceIOS() " + isRemoteDeviceIOS());

                if (isRemoteDeviceIOS()) {
                    Constants.SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_APP, false);
                    Constants.SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_SETTINGS, false);
                    Constants.SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, false);
                    Constants.SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_DOCUMENTS, false);
                    Constants.SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_MUSIC, false);
                    Constants.SUPPORTED_DATATYPE_MAP.put(EMDataType.EM_DATA_TYPE_CALL_LOGS, false);
                }
                if (mRole == THIS_DEVICE_IS_SOURCE) {
                    DeviceInfo deviceInfo = DeviceInfo.getInstance();
                    mContactsCount = (int) deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_CONTACTS, true);
                    DLog.log("Contacts Count : " + mContactsCount);

                    mCalendarsCount = (int) deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_CALENDAR, true);
                    DLog.log("Calendars Count : " + mCalendarsCount);

                    mPhotosCount = (int) deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_PHOTOS, true);
                    DLog.log("Photos Count : " + mPhotosCount);

                    mVideosCount = (int) deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_VIDEO, true);
                    DLog.log("Videos Count : " + mVideosCount);

                    mAudioCount = (int) deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_MUSIC, true);
                    DLog.log("Audio Count : " + mAudioCount);

                    mMessageCount = (int) deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, true);
                    DLog.log("Messages Count : " + mMessageCount);
                    mCallLogsCount = (int) deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_CALL_LOGS, true);
                    DLog.log("Call Logs Count : " + mCallLogsCount);

                    mSettingsCount = EMStringConsts.SETTINGS_LIST.size();

                    mImageSize = deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_PHOTOS, false);
                    DLog.log("Photos Size : " + EMUtility.readableFileSize(mImageSize));

                    mVideoSize = deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_VIDEO, false);
                    DLog.log("Videos Size : " + EMUtility.readableFileSize(mVideoSize));

                    mAudioSize = deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_MUSIC, false);
                    DLog.log("Audio Size : " + EMUtility.readableFileSize(mAudioSize));


                    if (Constants.SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_DOCUMENTS)) {
                        mDocumentsCount = (int) deviceInfo.getDocumentDetails(true);
                        DLog.log("Documents Count : " + mDocumentsCount);

                        mDocumentsSize = deviceInfo.getDocumentDetails(false);
                        DLog.log("Documents Size : " + EMUtility.readableFileSize(mDocumentsSize));
                    }

                    AppMigrateUtils.procureAppsDetails();
                    if (Constants.SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_APP)) {
                        mAppCount = AppMigrateUtils.totalAppCount;
                        /*if(mAppCount==0){
//                            downLoadFiles();
                            if(movistarappsListS3.size()!=0) {
                                downloadFile(movistarappsListS3.get(0));
                            }
                        }*/
                        mAppSize = AppMigrateUtils.totalAppSize;
                    }
                    DLog.log("Apps Count : " + mAppCount);
                    DLog.log("Apps Size : " + mAppSize);

//                    loadContentDetailsMap();

                    //Changed as few sony models, getting blocked while querying sms content provider intermittent #51703
                    mMessageCount = (int) deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, true);
                    DLog.log("Messages Count : " + mMessageCount);
                    Date currentDate = new Date();
                    Calendar calendar = Calendar.getInstance();
                    for (Integer noOfMonths : customSelectMessageMap.keySet()) {
                        calendar.setTime(currentDate);
                        calendar.add(Calendar.MONTH, -noOfMonths);
                        calendar.set(Calendar.HOUR, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        long time = calendar.getTimeInMillis();
                        if (noOfMonths == 0) {
                            time = 0;
                        }
                        int count = DeviceInfo.getInstance().getSMSCountFromTime(time);
                        customSelectMessageMap.put(noOfMonths, (long) count);
                    }
                }
                loadContentDetailsMap();
                if (fromExcludeContent) {
                    loadContentDetailsMapLatest();
                    handler.sendEmptyMessage(EXCLUDE_MEDIA_CONTENT);
                }
            }
        }.start();
        return;
    }

    private List<EDeviceSwitchSourceContentSummary> getSourceContentSummaryDetails() {
        ArrayList<EDeviceSwitchSourceContentSummary> srcSummaryList = new ArrayList<>();
        for (ContentDetails contentDetails : contentDetailsMap.values()) {
            if (contentDetails.isSupported()) {
                srcSummaryList.add(new EDeviceSwitchSourceContentSummary(Constants.DATATYPE_VALUES.get(contentDetails.getContentType()),
                        null, contentDetails.getTotalCount(), contentDetails.isSelected() ?
                        SELECTED : NOT_SELECTED, null, contentDetails.getTotalSizeOfEntries() / 1024));
            }
        }
        return srcSummaryList;
    }


    private void loadContentDetailsMap() {
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_CONTACTS, (getContentDetails(EMDataType.EM_DATA_TYPE_CONTACTS, getString(R.string.ept_select_content_contacts), -1, mContactsCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_CONTACTS), mContactsCount > 0, new int[]{R.drawable.ic_contact, R.drawable.ic_contact_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_CALENDAR, (getContentDetails(EMDataType.EM_DATA_TYPE_CALENDAR, getString(R.string.ept_select_content_calendar), -1, mCalendarsCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_CALENDAR), mCalendarsCount > 0, new int[]{R.drawable.ic_calendar, R.drawable.ic_calendar_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_CALL_LOGS, (getContentDetails(EMDataType.EM_DATA_TYPE_CALL_LOGS, getString(R.string.ept_select_content_call_logs), -1, mCallLogsCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_CALL_LOGS), mCallLogsCount > 0, new int[]{R.drawable.calllog, R.drawable.callog_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, (getContentDetails(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, getString(R.string.ept_select_content_messages), -1, mMessageCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES), mMessageCount > 0, new int[]{R.drawable.ic_message, R.drawable.ic_message_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_PHOTOS, (getContentDetails(EMDataType.EM_DATA_TYPE_PHOTOS, getString(R.string.ept_select_content_photos), -1, mPhotosCount, mImageSize, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_PHOTOS), mPhotosCount > 0, new int[]{R.drawable.ic_photo, R.drawable.ic_photo_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_VIDEO, (getContentDetails(EMDataType.EM_DATA_TYPE_VIDEO, getString(R.string.ept_select_content_videos), -1, mVideosCount, mVideoSize, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_VIDEO), mVideosCount > 0, new int[]{R.drawable.ic_video, R.drawable.ic_video_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_MUSIC, (getContentDetails(EMDataType.EM_DATA_TYPE_MUSIC, getString(R.string.ept_select_content_audio), -1, mAudioCount, mAudioSize, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_MUSIC), mAudioCount > 0, new int[]{R.drawable.ic_music, R.drawable.ic_music_disabled})));
//        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_DOCUMENTS, (getContentDetails(EMDataType.EM_DATA_TYPE_DOCUMENTS, getString(R.string.ept_select_content_documents), -1, mDocumentsCount, mDocumentsSize, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_DOCUMENTS), mDocumentsCount > 0, new int[]{R.drawable.ic_document, R.drawable.ic_document_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_SETTINGS, (getContentDetails(EMDataType.EM_DATA_TYPE_SETTINGS, getString(R.string.ept_select_content_settings), -1, mSettingsCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_SETTINGS), mSettingsCount > 0, new int[]{R.drawable.setting, R.drawable.setting_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_APP, (getContentDetails(EMDataType.EM_DATA_TYPE_APP, getString(R.string.ept_select_content_app), -1, mAppCount, mAppSize, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_APP), mAppCount >= 0, new int[]{R.drawable.app, R.drawable.app_disabled})));
    }

    private void loadContentDetailsMapLatest() {
        DLog.log("loadContentDetailsMapLatest photos " + latestSelectionMap.get(EMDataType.EM_DATA_TYPE_PHOTOS));
        DLog.log("loadContentDetailsMapLatest contacts " + latestSelectionMap.get(EMDataType.EM_DATA_TYPE_CONTACTS));
        DLog.log("loadContentDetailsMapLatest video " + latestSelectionMap.get(EMDataType.EM_DATA_TYPE_VIDEO));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_CONTACTS, (getContentDetails(EMDataType.EM_DATA_TYPE_CONTACTS, getString(R.string.ept_select_content_contacts), -1, mContactsCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_CONTACTS), latestSelectionMap.get(EMDataType.EM_DATA_TYPE_CONTACTS), new int[]{R.drawable.ic_contact, R.drawable.ic_contact_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_CALENDAR, (getContentDetails(EMDataType.EM_DATA_TYPE_CALENDAR, getString(R.string.ept_select_content_calendar), -1, mCalendarsCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_CALENDAR), latestSelectionMap.get(EMDataType.EM_DATA_TYPE_CALENDAR), new int[]{R.drawable.ic_calendar, R.drawable.ic_calendar_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_CALL_LOGS, (getContentDetails(EMDataType.EM_DATA_TYPE_CALL_LOGS, getString(R.string.ept_select_content_call_logs), -1, mCallLogsCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_CALL_LOGS), latestSelectionMap.get(EMDataType.EM_DATA_TYPE_CALL_LOGS), new int[]{R.drawable.calllog, R.drawable.callog_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, (getContentDetails(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, getString(R.string.ept_select_content_messages), -1, mMessageCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES), latestSelectionMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES), new int[]{R.drawable.ic_message, R.drawable.ic_message_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_PHOTOS, (getContentDetails(EMDataType.EM_DATA_TYPE_PHOTOS, getString(R.string.ept_select_content_photos), -1, mPhotosCount, mImageSize, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_PHOTOS), latestSelectionMap.get(EMDataType.EM_DATA_TYPE_PHOTOS), new int[]{R.drawable.ic_photo, R.drawable.ic_photo_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_VIDEO, (getContentDetails(EMDataType.EM_DATA_TYPE_VIDEO, getString(R.string.ept_select_content_videos), -1, mVideosCount, mVideoSize, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_VIDEO), latestSelectionMap.get(EMDataType.EM_DATA_TYPE_VIDEO), new int[]{R.drawable.ic_video, R.drawable.ic_video_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_MUSIC, (getContentDetails(EMDataType.EM_DATA_TYPE_MUSIC, getString(R.string.ept_select_content_audio), -1, mAudioCount, mAudioSize, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_MUSIC), latestSelectionMap.get(EMDataType.EM_DATA_TYPE_MUSIC), new int[]{R.drawable.ic_music, R.drawable.ic_music_disabled})));
//        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_DOCUMENTS, (getContentDetails(EMDataType.EM_DATA_TYPE_DOCUMENTS, getString(R.string.ept_select_content_documents), -1, mDocumentsCount, mDocumentsSize, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_DOCUMENTS), latestSelectionMap.get(EMDataType.EM_DATA_TYPE_DOCUMENTS), new int[]{R.drawable.ic_document, R.drawable.ic_document_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_SETTINGS, (getContentDetails(EMDataType.EM_DATA_TYPE_SETTINGS, getString(R.string.ept_select_content_settings), -1, mSettingsCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_SETTINGS), latestSelectionMap.get(EMDataType.EM_DATA_TYPE_SETTINGS), new int[]{R.drawable.setting, R.drawable.setting_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_APP, (getContentDetails(EMDataType.EM_DATA_TYPE_APP, getString(R.string.ept_select_content_app), -1, mAppCount, mAppSize, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_APP), latestSelectionMap.get(EMDataType.EM_DATA_TYPE_APP), new int[]{R.drawable.app, R.drawable.app_disabled})));
    }

    private boolean isAvailableSpaceOnDestination(long mSelectedContentSize) {
        long availbaleSpaceInDest = DashboardLog.getInstance().destinationEMDeviceInfo.dbDeviceFreeStorage;
        DLog.log(String.format("Selected Files size %2d KB", mSelectedContentSize));
        DLog.log(String.format("Available Space in Destination %2d KB", availbaleSpaceInDest));
        if (availbaleSpaceInDest > 0 && (mSelectedContentSize > (availbaleSpaceInDest + STORAGE_THRESHOLD_VALUE))) {
            return false;
        }
        return true;
    }

    private boolean isRemoteDeviceIOS() {
        if (mRemoteDeviceInfo != null && mRemoteDeviceInfo.dbDevicePlatform.equalsIgnoreCase(Constants.PLATFORM_IOS)) {
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        DLog.log("onresume called " + mCurrentPage.name());
        if (handler != null) {
            handler.removeMessages(NOTIFICATION_MESSAGE);
        }
        if (mNotificationManager != null) {
            mNotificationManager.cancel(0);
        }
        try {
            if (Constants.PLATFORM.equalsIgnoreCase(Constants.PLATFORM_BLACKBERRY) && mTransferMode.equalsIgnoreCase("WDIRECT") && (mCurrentPage == EPTWizardPage.Connecting || mCurrentPage == EPTWizardPage.ScanQRCode)) {
                final String mWiFiDirectPeerSSID = CommonUtil.getInstance().getmWiFiPeerSSID();
                final String connectedNetworkName = NetworkUtil.getConnectedNetworkName();
                DLog.log(mWiFiDirectPeerSSID + ", Connected name " + connectedNetworkName);
                if (connectedNetworkName != null && EMUtility.isStringMatches(mWiFiDirectPeerSSID, connectedNetworkName)) {
                    EMWifiPeerConnector.mPeerNetworkId = NetworkUtil.getConnectedNetworkId();
                    setLayout(EPTWizardPage.Connecting, EPTTransitionReason.UserNext);
                    EMNetworkManagerClientUIHelper.startPairing();
                } else {
                    showWifiConnectDialog(false);
                }
            } else if (mCurrentPage == EPTWizardPage.ScanQRCode && null != barcodeView)
                barcodeView.resume();
            else if (mCurrentPage == EPTWizardPage.CloudPairing && cloudPairing_scanner != null)
                cloudPairing_scanner.resume();
            else if (PreferenceHelper.getInstance(this).getBooleanItem(Constants.CANCELLED_BEFORE_PAIRING)) { // called, if you cancel the transaction before pairing
//                setLayout(EPTWizardPage.Welcome, EPTTransitionReason.Automatic);
                PreferenceHelper.getInstance(this).putBooleanItem(Constants.CANCELLED_BEFORE_PAIRING, false);
            } else if (mCurrentPage == EPTWizardPage.Complete) {
                DLog.log("onResume complete page");
            }
        } catch (Exception e) {
            DLog.log("got exception in onResume : " + e.getMessage());
        }
    }
//
//    private void showAccLockConfirmDialog() {
//        if (wipeConfirmDialog != null) {
//            wipeConfirmDialog.dismiss();
//        }
//        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
//        alertDialogBuilder.setTitle(getString(R.string.str_alert));
//        alertDialogBuilder
//                .setMessage(getString(R.string.want_to_return_device))
//                .setCancelable(false)
//                .setPositiveButton(getString(R.string.str_yes), new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        dialog.dismiss();
//                        Intent intent = new Intent(EasyMigrateActivity.this, ThankYouActivity.class);
//                        startActivity(intent);
//                        finish();
//                    }
//                })
//                .setNegativeButton(getString(R.string.str_no), new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        dialog.dismiss();
//                        PreferenceHelper.getInstance(context).putBooleanItem(Constants.PREF_FINISH_CLICKED, true);
//                        Intent accLockRemoval = new Intent(EasyMigrateActivity.this, LockRemoveinstructionActivity.class);
//                        startActivity(accLockRemoval);
//                        finish();
//                    }
//                });
//        wipeConfirmDialog = alertDialogBuilder.create();
//        wipeConfirmDialog.show();
//    }
//
    private void showWipeConfirmDialog() {

        if (wipeConfirmDialog != null) {
            wipeConfirmDialog.dismiss();
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.proceed_to_wipe_title));
        alertDialogBuilder
                .setMessage(getString(R.string.proceed_to_wipe_msg))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ept_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        PreferenceHelper.getInstance(context).putBooleanItem(Constants.PREF_FINISH_CLICKED, true);
                        Intent dataWipe = new Intent(EasyMigrateActivity.this, RestrictionCheckActivity.class);
                        startActivity(dataWipe);
                        finish();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        if (FeatureConfig.getInstance().getProductConfig().isUninstallRequired() ||
                                (!Constants.IS_MMDS && !BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT))) {
                            UninstallBroadcastReceiver.startUninstallAlarm(context);
                        }
                        PreferenceHelper.getInstance(context).putBooleanItem(Constants.PREF_FINISH_CLICKED, true);
                        forceCloseApp();
                    }
                });
        wipeConfirmDialog = alertDialogBuilder.create();
        wipeConfirmDialog.show();
    }

    private void showFailDialog() {
        DLog.log("Displaying connection lost dialog");
        if (failedDialog != null) {
            failedDialog.dismiss();
        }
        //If the migration has already been interrupted, don't show it
        if (isInterrupted || !isMigrationInProgress) {
            return;
        }
        //Cancelled the Timer when Connection Lost pop up displayed.
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.alert_oops));
        alertDialogBuilder
                .setMessage(getString(R.string.connection_lost_msg))
                .setCancelable(false)
                .setPositiveButton("CONTINUE", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
//                        stopOrDisconnectFromAnyNetwork();
//                        DashboardLog.getInstance().geteDeviceSwitchSession().setCancellationReason(Constants.CANCEL_REASON.MIGRATION_FAILED.value());
//                        sendTransactionInfoTOServer();
//                        reconnectP2P();
//                        if (hasFeatureAtTheEnd())
//                            proceedWithTheFeature(false);
//                        else
//                            forceCloseApp();
                    }
                })
                .setNegativeButton("CLOSE", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        elapsedTimer.cancel();
                        dialog.dismiss();
                        stopOrDisconnectFromAnyNetwork();
                        DashboardLog.getInstance().geteDeviceSwitchSession().setCancellationReason(Constants.CANCEL_REASON.MIGRATION_FAILED.value());
                        sendTransactionInfoTOServer();
                        if (hasFeatureAtTheEnd())
                            proceedWithTheFeature(false);
                        else
                            forceCloseApp();
                    }
                });
        failedDialog = alertDialogBuilder.create();
        failedDialog.show();
    }

    public void resumeTransactionDialog() {
        DLog.log("--resumeTransactionDialog--");
        final Dialog dialog = new Dialog(EasyMigrateActivity.this);
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.resume_transaction_dialog);
        TextView btnStartAgain = (TextView) dialog.findViewById(R.id.btn_start_again);
        TextView btnResume = (TextView) dialog.findViewById(R.id.btn_resume);
        btnStartAgain.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                DLog.log("--Button StartAgain--");
                EMUtility.clearTransaction();
                welcomeNext.callOnClick();
            }
        });
        btnResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                DLog.log("--Transaction Resumed--" + MigrationStats.getInstance().getmRole());
                transactionResumed = true;
                Constants.IS_MIGRATION_RESUMED = transactionResumed;
                DashboardLog.getInstance().seteDeviceSwitchSession(MigrationStats.getInstance().geteDeviceSwitchSession());
                contentDetailsMap = MigrationStats.getInstance().getContentDetailsMap();
                EMMigrateStatus.setmInstance(MigrationStats.getInstance().getMigrateStatus());
                if (MigrationStats.getInstance().getmRole() == THIS_DEVICE_IS_SOURCE) {
                    mRole = THIS_DEVICE_IS_SOURCE;
                    metadataThread.start();
                    DLog.log("Source device metadataThread started --- ");
                    stateMachine = StateMachine.getInstance(EasyMigrateActivity.this, mRole);
                    setLayout(EPTWizardPage.CloudPairing, EPTTransitionReason.UserNext);
                    showProgressDialog("", getString(R.string.please_wait));
                    stateMachine.createSession();
                } else {
                    mRole = THIS_DEVICE_IS_TARGET;
                    DashboardLog.getInstance().addAdditionalInfo("Transaction got resumed");
                    setLayout(EPTWizardPage.CloudPairing, EPTTransitionReason.UserNext);
                    stateMachine = StateMachine.getInstance(EasyMigrateActivity.this, mRole);
                }
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    private boolean transactionResumed = false;

    private void showInterruptedDialog(String interruptedEvent) {
        if (CommonUtil.getInstance().getMigrationStatus() == Constants.MIGRATION_SUCCEEDED) {
            return;
        }
        DLog.log("Displaying interrupted dialog");

        if (interruptedDialog != null) {
            interruptedDialog.dismiss();
        }
        String title = getString(R.string.alert_oops);
        String message = getString(R.string.killed_crashed_msg);
        if (!TextUtils.isEmpty(interruptedEvent) && interruptedEvent.contains(MIGRATION_CANCELLED)) {
            title = getString(R.string.transfer_cancelled);
            message = getString(R.string.transfer_cancelled_msg);
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ept_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        initializeTransactionService(!DashboardLog.getInstance().isThisDest());
                        if (hasFeatureAtTheEnd())
                            proceedWithTheFeature(false);
                        else
                            forceCloseApp();
                    }
                });
        interruptedDialog = alertDialogBuilder.create();
        interruptedDialog.show();
    }


    private int retryCount = 0;

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info1) {
        DLog.log("Before In onConnectionInfoAvailable() " + connectionInfoAvailable);
        if (connectionInfoAvailable) return;
        connectionInfoAvailable = true;
        DLog.log("After In onConnectionInfoAvailable()***");
        if (wifiP2PInitDone) {
            if (isMigrationInProgress && Constants.stopMigration) {
                CommonUtil.getInstance().resumeMigration();
                if (elapsedTimer != null)
                    elapsedTimer.start();
            }
            return;
        }
        wifiP2PInitDone = true;
        DLog.log("onConnectionInfoAvailable() - " + connectionInfoAvailable);
        final WifiP2pInfo info = info1;
        Log.d(TAG, "onConnectionInfoAvailable() - " + info);
        try {
            if (info.groupFormed && info.isGroupOwner) {
                DLog.log("group owner Ip address=+" + info.groupOwnerAddress);
            } else if (info.groupFormed) {
                // The other device acts as the client. In this case, we enable the
                // get file button.
                DLog.log("Not Group Owner");
                if (EMNetworkManagerClientUIHelper.getInstance() == null) {
                    DLog.log("EMNetworkManagerClientUIHelper object is null");
                } else {
                    DLog.log("do Attempt to Connection To Peer......." + info.groupOwnerAddress);
                    CommonUtil.getInstance().setRemoteDeviceIpAddress(info.groupOwnerAddress);
                    setLayout(EPTWizardPage.ScanQRCode, EPTTransitionReason.UserNext);
                    handler.postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    EMNetworkManagerClientUIHelper.getInstance().attemptConnectionToPeer(info.groupOwnerAddress);
                                }
                            }, 1000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean selectedDontAskAgain = false;

    public void displayP2PPermissionsDialog(String title, String message) {
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(EasyMigrateActivity.this);
            alert.setTitle(title);
            alert.setMessage(message);
            String positiveString = getString(R.string.ept_ok);
            if (!selectedDontAskAgain) {
                alert.setPositiveButton(positiveString, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog != null) {
                            dialog.cancel();
                            handler.postDelayed(startP2PThread, 100);
                        }
                    }
                });
            }

            alert.setNegativeButton(getString(R.string.ept_exit_button), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    DashboardLog.getInstance().geteDeviceSwitchSession().setSessionStatus(Constants.SESSION_STATUS.CANCELLED.value());
                    if (CommonUtil.getInstance().isSource()) {
                        DashboardLog.getInstance().geteDeviceSwitchSession().setCancellationReason(Constants.CANCEL_REASON.BEFORE_PAIRING_SOURCE_DENIED_PERMISSION.value());
                    } else {
                        DashboardLog.getInstance().geteDeviceSwitchSession().setCancellationReason(Constants.CANCEL_REASON.BEFORE_PAIRING_DESTINATION_DENIED_PERMISSION.value());
                    }
                    DashboardLog.getInstance().addAdditionalInfo("User Denied Permissions");
                    DashboardLog.getInstance().updateToServer(true);
//                    System.exit(0);
                    finish();
                }
            });
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        } catch (Exception e) {
            DLog.log(e.getMessage());
            DLog.log("P2P Permissions Dialog Exception: " + Log.getStackTraceString(e));
        }
    }

    public void locationAlertResultCallback() {
        handler.postDelayed(startP2PThread, 1000);
    }

    boolean locationRequestFromP2P = false;

    private boolean locationPermissionRequired() {
        boolean required = false;
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(EasyMigrateActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                required = true;
                DLog.log("@ selectedDontAskAgain: " + selectedDontAskAgain);
                if (selectedDontAskAgain)
                    displayP2PPermissionsDialog(getString(R.string.str_alert), getString(R.string.location_permission_denied_msg));
                else
                    ActivityCompat.requestPermissions(EasyMigrateActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            P2P_PERMISSION_REQUEST_CODE);
            } else {
                LocationManager locManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                if (locManager.isProviderEnabled("gps")) {
                    required = false;
                } else {
                    required = true;
                    if ("huawei".equalsIgnoreCase(DeviceInfo.getInstance().get_make()) && !isGoogleServicesAvailable()) {
                        showLocationPopup();
                    } else {
                        locationRequestFromP2P = true;
                        new startLocationAlert(EasyMigrateActivity.this);
                    }
                }
            }
        }
        return required;
    }

    GpsChangeReceiver gpsChangeReceiver;

    public void showLocationPopup() {
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(EasyMigrateActivity.this);
            alert.setTitle("");
            alert.setMessage(getString(R.string.location_turn_on_msg));
            String positiveString = getString(R.string.ept_ok);
            alert.setPositiveButton(positiveString, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (dialog != null) {
                        dialog.cancel();
                    }
                    gpsChangeReceiver = new GpsChangeReceiver();
                    getApplicationContext().registerReceiver(gpsChangeReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    public class GpsChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DLog.log("In GpsChangeReceiver ........++ ");
            if (gpsChangeReceiver != null)
                getApplicationContext().unregisterReceiver(gpsChangeReceiver);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(getIntent());
                }
            }, 1000);
            handler.postDelayed(startP2PThread, 1200);
            DLog.log("Out GpsChangeReceiver ........++ ");
        }
    }

    private boolean isGoogleServicesAvailable() {
        int checkGooglePlayServices = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(EasyMigrateActivity.this);
        if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
            /*
             * Google Play Services is missing or update is required
             *  return code could be
             * SUCCESS,
             * SERVICE_MISSING, SERVICE_VERSION_UPDATE_REQUIRED,
             * SERVICE_DISABLED, SERVICE_INVALID.
             */
/*            GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices,
                    mContext, REQUEST_CODE_RECOVER_PLAY_SERVICES).show();*/

            return false;
        }

        return true;
    }

    Runnable startP2PThread = new Runnable() {
        @Override
        public void run() {
            DLog.log("enter startP2PThread run ");
            if (!locationPermissionRequired()) {
                DLog.log("In startP2PThread ........");
                // Commented below code as wifi on and off is not happening and due to this there is a delay of 15 sec in P2P transfer mode
//                enableWifi(false);
//                handler.postDelayed(checkWifiOffThread, 3000);
                // Commented below code as wifi on and off is not happening and due to this there is a delay of 15 sec in P2P transfer mode
                NetworkUtil.enableAllNetworks(false, -1);
                initiateDiscoverPeers();
            }
        }
    };
    Runnable stopP2PThread = new Runnable() {
        @Override
        public void run() {
            if (mTransferMode.equalsIgnoreCase(P2P_MODE)) {
                try {
                    if (mManager != null && mRole == THIS_DEVICE_IS_SOURCE) {
                        mManager.cancelConnect(mChannel, null);
                    }
                    mManager.removeGroup(mChannel, null);
                } catch (Exception e) {
                    DLog.log("Exception in stopP2PThread : " + e.getMessage());
                }
                connectToNetwork(connectedNetworkId);
            }
        }
    };

    Runnable checkWifiOffThread = new Runnable() {
        @Override
        public void run() {
            DLog.log("In checkWifiOffThread..............retryCount " + retryCount);
            if (connectionInfoAvailable) return;
            if (isWifiOff() || retryCount >= 5) {
                retryCount = 0;
                enableWifi(true);
                handler.postDelayed(checkWifiOnThread, 3000);
            } else {
                retryCount++;
                handler.postDelayed(checkWifiOffThread, 3000);
            }
        }

    };
    Runnable checkWifiOnThread = new Runnable() {
        @Override
        public void run() {
            DLog.log("In checkWifiOnThread..............");
            if (wifiIsOn() || retryCount >= 5) {
                retryCount = 0;
                NetworkUtil.enableAllNetworks(false, -1);
                initiateDiscoverPeers();
            } else {
                retryCount++;
                handler.postDelayed(checkWifiOnThread, 3000);
            }
        }
    };

    Runnable checkAndDisableNetwork = new Runnable() {
        @Override
        public void run() {
            DLog.log("In checkAndDisableNetwork..............isWifiConnected" + NetworkUtil.isWifiConnected() + "-----retryCount" + retryCount);
            if (NetworkUtil.isWifiConnected() || retryCount >= 25) {
                DLog.log("try to disconnecting Wifi.............." + retryCount);
                retryCount = 0;
                disconnectWifi();
            } else {
                retryCount++;
                handler.postDelayed(checkAndDisableNetwork, 3000);
            }
        }
    };

    private void initiateDiscoverPeers() {
        DLog.log("In initiateDiscoverPeers..............");
        deviceConnected = false;
        connectionInfoAvailable = false;
        discoverPeers();
    }

    Runnable discoveryThread = new Runnable() {
        @Override
        public void run() {
            discoverPeers();
        }
    };

    private synchronized void discoverPeers() {
        DLog.log("in discoverPeers");
        DLog.log("deviceConnected = " + deviceConnected + " connectionInfoAvailable = " + connectionInfoAvailable);
        if ((deviceConnected || connectionInfoAvailable)) return;
        DLog.log("in discoverPeers+++++++++++++++++");
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                DLog.log("discoverPeers success.......");
                if ((CommonUtil.getInstance().isSource() && !deviceConnected && !connectionInfoAvailable)) {
                    handler.postDelayed(discoveryThread, 3000);
                }
            }

            @Override
            public void onFailure(int reasonCode) {
                DLog.log("discoverPeers fail......." + reasonCode);
                if ((CommonUtil.getInstance().isSource() && !deviceConnected && !connectionInfoAvailable)) {
                    handler.postDelayed(discoveryThread, 3000);
                }
            }
        });
    }

    private boolean deviceConnected = false;

    synchronized void connectToP2PDevice() {
        DLog.log("connectToP2PDevice 1........... " + deviceConnected + " " + connectionInfoAvailable);
        if (discoveryThread != null)
            handler.removeCallbacks(discoveryThread);
        if ((deviceConnected || connectionInfoAvailable)) return;
        DLog.log("connectToP2PDevice 2...........");
        deviceConnected = true;
        WifiP2pConfig config = new WifiP2pConfig();
        DLog.log("remote deviceAddress = " + CommonUtil.getInstance().getRemoteDeviceAddress());
        config.deviceAddress = CommonUtil.getInstance().getRemoteDeviceAddress();
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 0;
        DLog.log("connectToP2PDevice 3........... " + config);

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(EasyMigrateActivity.this, "Connected Successfully....",
                        Toast.LENGTH_SHORT).show();
                DLog.log("Connection Success.......");
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(EasyMigrateActivity.this, " Connect failed. Retry.............." + reason,
                        Toast.LENGTH_SHORT).show();
                DLog.log("Connection Failed.......");
                deviceConnected = false;
                discoverPeers();
            }
        });
    }

    private void displayP2PConnectionLost() {
        DLog.log("Displaying P2P connection lost dialog");

        //If the migration has already been interrupted, don't show it
        if (isInterrupted) {
            return;
        }

        try {
            p2pReconnect = false;
            AlertDialog.Builder alert = new AlertDialog.Builder(EasyMigrateActivity.this);
            alert.setTitle(getString(R.string.alert_oops));
            alert.setMessage(getString(R.string.p2p_connection_lost_msg));
            alert.setPositiveButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DLog.log("P2P connection lost dialog : Retry Clicked");
                    if (dialog != null)
                        dialog.dismiss();
                    reconnectP2P();
                }
            });
            alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DLog.log("P2P connection lost dialog : Cancel Clicked");
                    if (dialog != null)
                        dialog.dismiss();
                    stopOrDisconnectFromAnyNetwork();
                    DashboardLog.getInstance().geteDeviceSwitchSession().setCancellationReason(Constants.CANCEL_REASON.MIGRATION_FAILED.value());
                    sendTransactionInfoTOServer();
                    forceCloseApp();
                }
            });
            alert.setCancelable(false);
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    boolean p2pReconnect = false;

    public void reconnectP2P() {
        DLog.log("In p2pReconnect+++++++++++");
        p2pReconnect = false;
        retryCount = 0;
        deviceConnected = false;
        connectionInfoAvailable = false;
        handler.postDelayed(startP2PThread, 10000);
    }

    private BroadcastReceiver wiFiObserver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                DLog.log("wiFiObserver - " + action);
                if (action != null && action.equalsIgnoreCase(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    DLog.log("Action: WIFI_P2P_STATE_CHANGED_ACTION : State = " + state);
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        DLog.log("Wifi P2P is enabled.......");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            mManager.requestDeviceInfo(mChannel, new WifiP2pManager.DeviceInfoListener() {
                                @Override
                                public void onDeviceInfoAvailable(@Nullable WifiP2pDevice wifiP2pDevice) {
                                    //TODO
                                    try{CommonUtil.getInstance().setDeviceAddress(wifiP2pDevice.deviceAddress);
                                    CommonUtil.getInstance().setDeviceNameP2P(wifiP2pDevice.deviceName);}
                                    catch (Exception e){
                                        Log.d(TAG, "onDeviceInfoAvailable: "+e.toString());
                                    }
                                }
                            });
                        }
                    } else {
                        DLog.log( "Wifi P2P is disabled.......");
                    }
                }
                if (action != null && action.equalsIgnoreCase(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {
                    DLog.log("Peers changed : WIFI_P2P_PEERS_CHANGED_ACTION");
                    if (mManager != null && Constants.mTransferMode.equalsIgnoreCase(P2P_MODE)) {
                        mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                            @Override
                            public void onPeersAvailable(WifiP2pDeviceList peers) {
                                DLog.log(peers.getDeviceList() + "");
                                DLog.log("onPeersAvailable..........");
                                DLog.log("-- Searching for: " + CommonUtil.getInstance().getRemoteDeviceAddress());
                                if (CommonUtil.getInstance().isSource() && CommonUtil.getInstance().getRemoteDeviceAddress() != null) {
                                    Collection<WifiP2pDevice> list = peers.getDeviceList();
                                    Iterator<WifiP2pDevice> iterator = list.iterator();
                                    String targetDevice = CommonUtil.getInstance().getRemoteDeviceAddress();
                                    String targetP2PDeviceName = CommonUtil.getInstance().getRemoteDeviceNameP2P();
                                    while (iterator.hasNext()) {
                                        WifiP2pDevice wifiP2pDevice = iterator.next();
                                        if (wifiP2pDevice != null) {
                                            String foundDeviceAddress = wifiP2pDevice.deviceAddress;
                                            String foundDeviceNameP2P = wifiP2pDevice.deviceName;
                                            DLog.log("Target device Address : " + targetDevice + ", Found device address : " + foundDeviceAddress);
                                            DLog.log("Target device name p2p : " + targetP2PDeviceName + ", Found device name p2p : " + foundDeviceNameP2P);
                                            String defaultMACAdress = "02:00:00:00:00:00";
                                            if (!TextUtils.isEmpty(targetDevice)) {
                                                if (targetDevice.equalsIgnoreCase(defaultMACAdress)) {
                                                    if (!TextUtils.isEmpty(foundDeviceNameP2P) && foundDeviceNameP2P.equalsIgnoreCase(targetP2PDeviceName) && (!connectionInfoAvailable)) {
                                                        CommonUtil.getInstance().setRemoteDeviceAddress(foundDeviceAddress);
                                                        connectToP2PDevice();
                                                    }
                                                } else {
                                                    if (!TextUtils.isEmpty(foundDeviceAddress) && foundDeviceAddress.equalsIgnoreCase(targetDevice) && (!connectionInfoAvailable)) {
                                                        CommonUtil.getInstance().setRemoteDeviceAddress(foundDeviceAddress);
                                                        connectToP2PDevice();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    DLog.log("Ignoring the peers, as this is destination");
                                }
                            }

                        });
                    } else {
                        if (mManager != null) {
                            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                                @Override
                                public void onGroupInfoAvailable(WifiP2pGroup group) {
                                    if (group != null) {
                                        Collection<WifiP2pDevice> list = group.getClientList();
                                        DLog.log("onGroupInfoAvailable > groupInfo : " + group.toString());
                                        DLog.log("onGroupInfoAvailable > list size : " + list.size());
                                        if (list.isEmpty()) {
                                            DLog.log("********** Other device disconnected from WifiDirect **********");
                                            CommonUtil.getInstance().setMigrationInterrupted(true);
                                            CommonUtil.getInstance().pauseMigration();
                                            handler.sendEmptyMessageDelayed(CONNECTION_LOST, CONNECTION_CHECK_TIMEOUT);
                                        } else { //TODO:Need to validate connected network
                                            DLog.log("********** Other device connected to WifiDirect **********");
                                            handler.removeMessages(CONNECTION_LOST);
                                            handler.removeCallbacks(autoPairHandler);
                                            CommonUtil.getInstance().setAutoPairingStatus(false);
                                            if (CommonUtil.getInstance().isMigrationInterrupted()) {
                                                if (mRole == THIS_DEVICE_IS_TARGET) {
                                                    mRemoteDeviceManager.reconnectToRemoteDevice();
                                                }
                                                CommonUtil.getInstance().setMigrationStatus(Constants.MIGRATION_INPROGRESS);
                                                CommonUtil.getInstance().setMigrationInterrupted(false);
                                                hideProgressDialog();
                                            }
                                            CommonUtil.getInstance().resumeMigration();
                                            if (failedDialog != null) {
                                                failedDialog.dismiss();
                                                //Starting the Elasped Timer after connection established.
                                                elapsedTimer.start();
                                            }
                                        }
                                    } else {
                                        DLog.log("onGroupInfoAvailable : groupInfo null");
                                        DLog.log("Wifi State : " + wifiIsOn());
                                    }
                                }
                            });
                        }
                    }
                } else if (action != null && action.equalsIgnoreCase(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                    if (Constants.mTransferMode.equalsIgnoreCase(P2P_MODE))
                        return;
                    int extraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                    DLog.log("Wifi state : "+extraWifiState);
                    switch (extraWifiState) {
                        case WifiManager.WIFI_STATE_DISABLED:
                            DLog.log("Wifi disabled");
                            enableWifi(true);
                            break;
                        case WifiManager.WIFI_STATE_ENABLED:
                            DLog.log("Wifi Enabled");
                            handler.removeMessages(ENABLE_WIFI);
                            connectToNetwork(EMWifiPeerConnector.mPeerNetworkId);
                            break;
                        default:
                            break;
                    }
                } else if (action != null && (action.equalsIgnoreCase(ConnectivityManager.CONNECTIVITY_ACTION) || action.equalsIgnoreCase(BuildConfig.APPLICATION_ID + ".autopair.connected")) && (isMigrationInProgress || devicesPaired)) {
                    DLog.log("=============== CONNECTIVITY CHANGED ===============");
                    if (Constants.mTransferMode.equalsIgnoreCase(P2P_MODE) || CommonUtil.getInstance().isAutoPairingStarted()) {
                        return;
                    }
                    String connectedNetwork = NetworkUtil.getConnectedNetworkName();
                    String remoteNetwork = CommonUtil.getInstance().getmWiFiPeerSSID();
                    int currentNetworkID = NetworkUtil.getConnectedNetworkId();
                    DLog.log("is WifiEnabled : "+wifiIsOn());
                    DLog.log("Connected Network : " + connectedNetwork + " Remote Network : " + remoteNetwork);
                    DLog.log("Original network id : "+EMWifiPeerConnector.mPeerNetworkId + " Connected Network id : "+currentNetworkID);
                    if (devicesPaired && mTransferMode.equalsIgnoreCase("WLAN")) {
                        if (connectedNetwork == null || !remoteNetwork.contains(connectedNetwork)) {
                            showFailDialog();
                        }
                    } else if (((connectedNetwork != null && connectedNetwork.equalsIgnoreCase(remoteNetwork)) || currentNetworkID == EMWifiPeerConnector.mPeerNetworkId) ||
                            (Constants.PLATFORM.equalsIgnoreCase(PLATFORM_BLACKBERRY) && EMUtility.isStringMatches(connectedNetwork, remoteNetwork))) {
                        handler.removeMessages(CONNECTION_LOST);
                        if (failedDialog != null) {
                            failedDialog.dismiss();
                            //Starting the Elasped Timer after connection established.
                            elapsedTimer.start();
                        }
                        if (CommonUtil.getInstance().isMigrationInterrupted()) {
                            CommonUtil.getInstance().setMigrationInterrupted(false);
                            hideProgressDialog();
                            handler.removeCallbacks(autoPairHandler);
                            CommonUtil.getInstance().setAutoPairingStatus(false);
                        }
                        CommonUtil.getInstance().resumeMigration();
                        if (mRole == THIS_DEVICE_IS_TARGET && (isMigrationInProgress)) {
                            mRemoteDeviceManager.reconnectToRemoteDevice();// need to check this line req or not
                        }
                        if (blackberryWifiDialog != null) {
                            blackberryWifiDialog.dismiss();
                        }
                    } else {
                        //Stop Migration and DIsconnect and reconnect wifi
                        DLog.log("pausing migration");
                        CommonUtil.getInstance().pauseMigration();
                        if (Constants.PLATFORM.equalsIgnoreCase(PLATFORM_ANDROID)) {
                            if (!CommonUtil.getInstance().isAutoPairingStarted()) { // Once autopairing started no need to re-connect
                                connectToNetwork(EMWifiPeerConnector.mPeerNetworkId);
                            }
                        } else // Block berry devices
                            showWifiConnectDialog(true);
                        if (!CommonUtil.getInstance().isMigrationInterrupted()) {
                            handler.sendEmptyMessageDelayed(CONNECTION_LOST, CONNECTION_CHECK_TIMEOUT);
                            CommonUtil.getInstance().setMigrationInterrupted(true);
                        }
                    }
                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

                    if (mManager == null || !Constants.mTransferMode.equalsIgnoreCase(P2P_MODE)) {
                        return;
                    }
                    DLog.log( "WIFI_P2P_CONNECTION_CHANGED_ACTION");
                    NetworkInfo networkInfo = (NetworkInfo) intent
                            .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    if (networkInfo.isConnected()) {
                        DLog.log("P2P network connected.......");
                        p2pReconnect = true;
                        mManager.requestConnectionInfo(mChannel, EasyMigrateActivity.this);
                    } else {
                        DLog.log("P2P network not connected.......");
                        //if (mRole == THIS_DEVICE_IS_SOURCE && isMigrationInProgress) {
                        if (wifiP2PInitDone) {
                            DLog.log("isMigrationInProgress and P2P network not connected.......");
                            //reconnectP2P();
                            if (p2pReconnect) {
                                //if (CommonUtil.getInstance().isSource())
                                if (isMigrationInProgress) {
                                    if (elapsedTimer != null)
                                        elapsedTimer.cancel();
                                    CommonUtil.getInstance().pauseMigration();
                                }
                                try {
                                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                                    r.play();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                displayP2PConnectionLost();
                            }
                        }
                        //}
                    }
                } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                    WifiP2pDevice p2pDevice = (WifiP2pDevice) intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                    CommonUtil.getInstance().setDeviceAddress(p2pDevice.deviceAddress);
                    CommonUtil.getInstance().setDeviceNameP2P(p2pDevice.deviceName);
                    DLog.log("WIFI_P2P_THIS_DEVICE_CHANGED_ACTION case,This device wifiDirect address: "+p2pDevice.deviceAddress+" , p2pDevice.deviceName "+p2pDevice.deviceName);
                }
            } catch (Exception e) {
                DLog.log("Exception in WIfiObserver : " + e.getMessage());
            }
        }
    };

    private void updateStorageProgress() {
        int selectedDataTypes = getSelectedDataTypes();
        long estimationTime = estimateTransferTime(false);
        List<EDeviceSwitchSourceContentSummary> srcList = getSourceContentSummaryDetails();

        DashboardLog.getInstance().geteDeviceSwitchSession().setEDeviceSwitchSourceContentSummaryCollection(srcList.toArray(
                new EDeviceSwitchSourceContentSummary[srcList.size()]));
        DashboardLog.getInstance().geteDeviceSwitchSession().setEstimatedTimeInMS(String.valueOf(estimationTime));
        CommonUtil.getInstance().setInitialEstimation(estimationTime);
        MigrationStats.getInstance().setEstimationTime(String.valueOf(estimationTime));
        long destFreeStorage = (DashboardLog.getInstance().destinationEMDeviceInfo.dbDeviceFreeStorage) * 1024;
        int progressData = (int) ((mSelectedContentSize * 100) / destFreeStorage);

        if (progressData > 90) {
            storageProgressbar.setProgressDrawable(getResources().getDrawable(R.drawable.storage_progressbar_red));
        } else if (progressData > 80) {
            storageProgressbar.setProgressDrawable(getResources().getDrawable(R.drawable.storage_progressbar_orange));
        } else {
            storageProgressbar.setProgressDrawable(getResources().getDrawable(R.drawable.storage_progressbar_green));
        }
        if (progressData >= 99 || selectedDataTypes == 0) {
            changeButtonStatus(false);
        } else {
            changeButtonStatus(true);
        }
        if (progressData >= 99) {
            DLog.log("Insufficient space on destination");
            selectContentsuggestion.setText(R.string.insufficient_space_on_dest);
            selectContentsuggestion.setTextColor(getResources().getColor(R.color.midRed));
            selectContentTimeSuggestion.setText("");
        } else if (estimationTime == 0) {
            selectContentsuggestion.setText("");
            selectContentTimeSuggestion.setText("");
        }
        String selectedStorage = EMUtility.readableFileSize(mSelectedContentSize);
        String destinationFreeStorage = EMUtility.readableFileSize(destFreeStorage);

        DLog.log("Selected Data size : " + selectedStorage + ", Available space on Destination : " + destinationFreeStorage);

        if (selectedStorage.isEmpty()) {
            selectedStorage = "0 KB";
        }
        storageProgressbar.setProgress(progressData);
        if (COMPANY_NAME.equalsIgnoreCase(Constants.COMPANY_TMOBILE)) {
            storageMessage.setVisibility(View.GONE);
            String availSpaceMsg = MessageFormat.format(getString(R.string.space_needed_on_destination) + " " + destinationFreeStorage, " " + selectedStorage);
            storageSuggestion.setText(availSpaceMsg);
        } else
            storageMessage.setText(String.format("%s / %s", selectedStorage, destinationFreeStorage));
        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).isSelected() || contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).isSelected() || contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).isSelected()) {
            mediaContentSelected = true;
        } else {
            mediaContentSelected = false;
        }
    }

    private void initializeTransactionService(boolean destCancelled) {

        DLog.log("In initializeTransactionService");

        if (Constants.IS_MMDS) {
            DLog.log("In case of MMDS, no need to start service. Returning.");
            return;
        }

        DashboardLog.getInstance().geteDeviceSwitchSession().setSessionStatus(Constants.SESSION_STATUS.CANCELLED.value());

        //After the devices have been paired or the migration is in progress
        if (devicesPaired || isMigrationInProgress) {

            //Log only from destination device
            if (DashboardLog.getInstance().isThisDest()) {
                DLog.log("Logging from destination");

                updateTransactionStatus(false);
                if (destCancelled) {
                    if (isMigrationInProgress) {
                        DashboardLog.getInstance().geteDeviceSwitchSession()
                                .setCancellationReason(Constants.CANCEL_REASON.IN_MIGRATION_DESTINATION_CANCELLED.value());
                    } else {
                        DashboardLog.getInstance().geteDeviceSwitchSession()
                                .setCancellationReason(Constants.CANCEL_REASON.AFTER_PAIRING_DESTINATION_CANCELLED.value());
                    }
                } else {
                    if (isMigrationInProgress) {
                        DashboardLog.getInstance().geteDeviceSwitchSession()
                                .setCancellationReason(Constants.CANCEL_REASON.IN_MIGRATION_SOURCE_CANCELLED.value());
                    } else {
                        DashboardLog.getInstance().geteDeviceSwitchSession()
                                .setCancellationReason(Constants.CANCEL_REASON.AFTER_PAIRING_SOURCE_CANCELLED.value());
                    }
                }
            } else {
                DLog.log("This device is source.");
            }
        }
        //If the device selection was done
        else if (isDeviceSelectionDone) {
            if (DashboardLog.getInstance().isThisDest()) {
                DashboardLog.getInstance().geteDeviceSwitchSession().
                        setCancellationReason(Constants.CANCEL_REASON.BEFORE_PAIRING_DESTINATION_CANCELLED.value());
            } else {
                DashboardLog.getInstance().geteDeviceSwitchSession().
                        setCancellationReason(Constants.CANCEL_REASON.BEFORE_PAIRING_SOURCE_CANCELLED.value());
            }
        }
        //The default case
        else {
            DashboardLog.getInstance().geteDeviceSwitchSession().
                    setCancellationReason(Constants.CANCEL_REASON.CANCELLED_UNKNOWN.value());
        }
        DashboardLog.getInstance().geteDeviceSwitchSession().setEndDateTime(String.valueOf(System.currentTimeMillis()));
        sendTransactionInfoTOServer();
    }

    private void disableUnsupportedContentTypes() {
        String remotePlatform = null;
        if (mRemoteDeviceInfo != null) {
            remotePlatform = mRemoteDeviceInfo.dbDevicePlatform;
        }
        boolean isRemoteDeviceIOS = isRemoteDeviceIOS();
        boolean isRemoteDeviceBlackBerry = remotePlatform != null && remotePlatform.equalsIgnoreCase(Constants.PLATFORM_BLACKBERRY);
        DLog.log("Destination Platform " + remotePlatform);
        if (isRemoteDeviceIOS || isRemoteDeviceBlackBerry) {
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).setSupported(false);
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).setSelected(false);
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALL_LOGS).setSupported(false);
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALL_LOGS).setSelected(false);
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SETTINGS).setSupported(false);
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SETTINGS).setSelected(false);
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).setSupported(false);
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).setSelected(false);
        }
        if (isRemoteDeviceIOS) {
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).setSupported(false);
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).setSelected(false);
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_DOCUMENTS).setSupported(false);
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_DOCUMENTS).setSelected(false);
        } else if (isRemoteDeviceBlackBerry) {
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).setSupported(false);
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).setSelected(false);
        }
    }

    private void updateTransactionStatus(boolean isSuccess) {
        DLog.log("In updateTransactionStatus. isSuccess is: " + isSuccess);

        Constants.TRANSFER_STATE transferState = Constants.TRANSFER_STATE.IN_PROGRESS;
        Constants.TRANSFER_STATUS transferStatus = Constants.TRANSFER_STATUS.FAILED;
        if (isSuccess) {
            DashboardLog.getInstance().geteDeviceSwitchSession().setCancellationReason(Constants.CANCEL_REASON.NO_ERROR.value());
            transferState = Constants.TRANSFER_STATE.COMPLETED;
            transferStatus = Constants.TRANSFER_STATUS.SUCCESS;
        }
        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CONTACTS).isSelected()) {
            DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.CONTACT, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CONTACTS).getTotalCount(), -1, transferStatus, transferState, false);

            if (!isSuccess)
                DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.CONTACT, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_CONTACTS), -1, transferStatus, transferState, false, true);
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).isSelected()) {
            DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.CALENDAR, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).getTotalCount(), -1, transferStatus, transferState, false);

            if (!isSuccess)
                DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.CALENDAR, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_CALENDAR), -1, transferStatus, transferState, false, true);
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).isSelected()) {
            DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.MESSAGE, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).getTotalCount(), -1, transferStatus, transferState, false);

            if (!isSuccess)
                DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.MESSAGE, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_SMS_MESSAGES), -1, transferStatus, transferState, false, true);
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALL_LOGS).isSelected()) {
            DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.CALLLOG, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALL_LOGS).getTotalCount(), -1, transferStatus, transferState, false);

            if (!isSuccess)
                DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.CALLLOG, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_CALL_LOGS), -1, transferStatus, transferState, false, true);
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SETTINGS).isSelected()) {
            DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.SETTINGS, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SETTINGS).getTotalCount(), -1, transferStatus, transferState, false);
            DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.SETTINGS, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_SETTINGS), -1, transferStatus, transferState, false, true);
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).isSelected()) {
            DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.IMAGE, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).getTotalCount(), contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).getTotalSizeOfEntries(), transferStatus, transferState, false);
            if (!isSuccess)
                DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.IMAGE, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_PHOTOS), EMMigrateStatus.getTransferedFilesSize(EMDataType.EM_DATA_TYPE_PHOTOS), transferStatus, transferState, false, true);
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).isSelected()) {
            DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.VIDEO, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).getTotalCount(), contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).getTotalSizeOfEntries(), transferStatus, transferState, false);
            if (!isSuccess)
                DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.VIDEO, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_VIDEO), EMMigrateStatus.getTransferedFilesSize(EMDataType.EM_DATA_TYPE_VIDEO), transferStatus, transferState, false, true);
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).isSelected()) {
            DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.AUDIO, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).getTotalCount(), contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).getTotalSizeOfEntries(), transferStatus, transferState, false);
            if (!isSuccess)
                DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.AUDIO, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_MUSIC), EMMigrateStatus.getTransferedFilesSize(EMDataType.EM_DATA_TYPE_MUSIC), transferStatus, transferState, false, true);
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).isSelected()) {
            DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.APP, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).getTotalCount(), contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).getTotalSizeOfEntries(), transferStatus, transferState, false);
            if (!isSuccess)
                DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.APP, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_APP), EMMigrateStatus.getTransferedFilesSize(EMDataType.EM_DATA_TYPE_APP), transferStatus, transferState, false, true);
        }

        //TODO
        if(contentDetailsMap.containsValue(EMDataType.EM_DATA_TYPE_DOCUMENTS)){
            if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_DOCUMENTS).isSelected()) {
                DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.DOCUMENTS, contentDetailsMap.get(EMDataType.EM_DATA_TYPE_DOCUMENTS).getTotalCount(), contentDetailsMap.get(EMDataType.EM_DATA_TYPE_DOCUMENTS).getTotalSizeOfEntries(), transferStatus, transferState, false);
                if (!isSuccess)
                    DashboardLog.getInstance().addOrUpdateContentTransferDetail(Constants.DATATYPE.DOCUMENTS, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_DOCUMENTS), EMMigrateStatus.getTransferedFilesSize(EMDataType.EM_DATA_TYPE_DOCUMENTS), transferStatus, transferState, false, true);
            }
        }

        DashboardLog.getInstance().geteDeviceSwitchSession().setEndDateTime(String.valueOf(System.currentTimeMillis()));

        if (isSuccess) {
            if (mTransferTotalTime == 0) {
                mRestoreEndTime = System.currentTimeMillis();
                mTransferTotalTime = ((mRestoreEndTime - mBackupStartTime));
            }
            DashboardLog.getInstance().geteDeviceSwitchSession().setActualTimeInMS(String.valueOf(mTransferTotalTime));
            DashboardLog.getInstance().updateSessionStatus(Constants.SESSION_STATUS.SUCCESS);
        } else {
            if (mBackupStartTime != 0) {
                DashboardLog.getInstance().geteDeviceSwitchSession().setActualTimeInMS(
                        String.valueOf(System.currentTimeMillis() - mBackupStartTime));
            }
        }
        CommonUtil.getInstance().setMigrationDone(true);
    }


    private ContentDetails getContentDetails(int dataType, String contentName, int progressCount, int totalCount, long totalSize, boolean supported, boolean selected, int[] drawableId) {
        ContentDetails contentDetails = new ContentDetails();
        contentDetails.setContentType(dataType);
        contentDetails.setProgressCount(progressCount);
        contentDetails.setTotalCount(totalCount);
        contentDetails.setTotalSizeOfEntries(totalSize);
        contentDetails.setPermissionGranted(isPermissionGranted(dataType));
        contentDetails.setSupported(supported);
        contentDetails.setSelected(supported && selected);
        contentDetails.setContentName(contentName);
        contentDetails.setImageDrawableId(drawableId);
        DLog.log("permission check *** " + contentName + " : " + isPermissionGranted(dataType));
        return contentDetails;
    }

    /**
     * @param dataType passing the current datatype
     * @return true if the permission is granted , else return false
     */
    private boolean isPermissionGranted(int dataType) {
        boolean isPermissionGranted = true;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (String str : Constants.PERMISSIONMAP.get(dataType)) {
                    if (checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED)
                        isPermissionGranted = false;
                }
            }
        } catch (Exception e) {
            DLog.log(e);
            isPermissionGranted = false;
        }
        return isPermissionGranted;
    }

    private ContentDetailsAdapter contentDetailsAdapter;
    private ContentProgressDetailsAdapter contentProgressDetailsAdapter;
    private SelectedDataTypeDetailsAdapter dataTypeDetailsAdapter;
    private SelectedAppsDetailsAdapter appDetailsAdapter;


    @Override
    public void updateContentSelection(int datatype, boolean isSelected) {
        DLog.log("updateContentSelection " + datatype + " " + isSelected);
        updateSelectedDataTypes(datatype, isSelected);

    }

    private int selectedDatatypeforDataTypeInfo = -1;

    @Override
    public void selectedDataTypeInfo(int dataType) {
        DLog.log("Selected datatype  for Data type info : " + dataType);
        if (mCurrentPage == EPTWizardPage.SelectContent && dataType == EMDataType.EM_DATA_TYPE_SMS_MESSAGES) {
            messageCustomSelectionDialog();
        } else {
            selectedDatatypeforDataTypeInfo = dataType;
            dataTypeTitle = contentDetailsMap.get(dataType).getContentName();
            setLayout(EPTWizardPage.DataTypeInfo, EPTTransitionReason.UserNext);
        }
    }

    public synchronized boolean isRestoreCompleted(int dataType) {
        long mDataTypesTobeTransferred = CommonUtil.getInstance().getDatatypesTobeTransferred();
        if (dataType == EMDataType.EM_DATA_TYPE_MEDIA) {
            if ((mDataTypesTobeTransferred & EMDataType.EM_DATA_TYPE_PHOTOS) != 0)
                mDataTypesTobeTransferred = mDataTypesTobeTransferred ^ EMDataType.EM_DATA_TYPE_PHOTOS;
            if ((mDataTypesTobeTransferred & EMDataType.EM_DATA_TYPE_VIDEO) != 0)
                mDataTypesTobeTransferred = mDataTypesTobeTransferred ^ EMDataType.EM_DATA_TYPE_VIDEO;
            if ((mDataTypesTobeTransferred & EMDataType.EM_DATA_TYPE_MUSIC) != 0)
                mDataTypesTobeTransferred = mDataTypesTobeTransferred ^ EMDataType.EM_DATA_TYPE_MUSIC;
            if ((mDataTypesTobeTransferred & EMDataType.EM_DATA_TYPE_DOCUMENTS) != 0)
                mDataTypesTobeTransferred = mDataTypesTobeTransferred ^ EMDataType.EM_DATA_TYPE_DOCUMENTS;
        } else {
            if ((mDataTypesTobeTransferred & dataType) != 0) {
                mDataTypesTobeTransferred = mDataTypesTobeTransferred ^ dataType;
            }
        }
        CommonUtil.getInstance().setDatatypesTobeTransferred(mDataTypesTobeTransferred);
        DLog.log("isRestoreCompleted : mDataTypesTobeTransferred = " + mDataTypesTobeTransferred);
        return mDataTypesTobeTransferred == 0;
    }

    private LinkedHashMap<Integer, ContentDetails> contentDetailsMap = new LinkedHashMap<>();

    private LinkedHashMap<Integer, Boolean> latestSelectionMap = new LinkedHashMap<>();

    private void updateProgress(int datatype, int progressCount, int totalCount) {
        try {
            contentDetailsMap.get(datatype).setProgressCount(progressCount);
            if (progressCount != 0 && contentDetailsMap.get(datatype).isSelected()) {
                contentProgressDetailsAdapter.notifyItemChanged(getContentTypePosition(datatype));
            }
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    private int getContentTypePosition(int dataType) {
        List<Integer> nameList = new ArrayList<Integer>(contentDetailsMap.keySet());
        return nameList.indexOf(dataType);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        if (BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT)) {
//            this.aMenu = menu;
//        }
        return true;
    }

    private int connectedNetworkId = -1;

    private void getSessionID() {
        DLog.log("showProgressDialog--4");
        showProgressDialog(getString(R.string.generating_summary_title), getString(R.string.generating_summary_description));
        boolean connected = connectToNetwork(connectedNetworkId);
        if (connected) {
            try {
                DashboardLog.getInstance().updateToServer(true);
            } catch (Exception e) {
                //Toast
                Toast.makeText(this, "Session ID not generated", Toast.LENGTH_SHORT).show();
                DLog.log(" FATAL EXCEPTION : Session ID not generated");
                Log.d(TAG, "getSessionID: "+" FATAL EXCEPTION : Session ID not generated " );

                e.printStackTrace();
            }
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                hideProgressDialog();
                String sessionId = DashboardLog.getInstance().geteDeviceSwitchSession().getDeviceSwitchSessionId();
                if (sessionId != null && !sessionId.isEmpty()) {
                    DLog.log("Transaction id " + sessionId);
                    String mTrasnactionId = getResources().getString(R.string.uniqueTransactionId) + "<b>" + " " + sessionId + "</b>";
                    mUniqueTransactionId.setText(Html.fromHtml(mTrasnactionId));
                }
            }
        }, Constants.TRANSACTION_COMPLETE_DIALOG_TIMEOUT);
    }

    private void displaySwitchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        String message = getString(R.string.switch_scan_source_msg);
        String positiveButton = getResources().getString(R.string.str_continue);
        if (mRole == THIS_DEVICE_IS_TARGET) {
            message = getString(R.string.switch_scan_destination_msg);
            positiveButton = getResources().getString(R.string.str_yes);
            builder.setNegativeButton(R.string.str_no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        }
        builder.setMessage(message);
        builder.setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mRole == THIS_DEVICE_IS_TARGET) {
                    disableWifiDirect();
                    DashboardLog.getInstance().updateSessionStatus(Constants.SESSION_STATUS.IN_PROGRESS);
                    DashboardLog.getInstance().geteDeviceSwitchSession().setSessionStage(Constants.SESSION_STAGE.QR_GENERATED.value());
                    DashboardLog.getInstance().updateToServer(true);
                    EMNetworkManagerClientUIHelper.reset();
                    setLayout(EPTWizardPage.ScanQRCode, EPTTransitionReason.Automatic);
                } else {
                    EMNetworkManagerHostUIHelper.reset();
                    setLayout(EPTWizardPage.DisplayQRCode, EPTTransitionReason.Automatic);
                }
                mBackStack.remove(mBackStack.size() - 1);
            }
        });
        final AlertDialog alert = builder.create();

        alert.show();

    }

    private String getMigrationSummary() {
        StringBuffer summary = new StringBuffer();
        summary.append(Constants.WDS_MIGRATION_DONE);
        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CONTACTS).isSupported() && contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CONTACTS).isSelected()) {
            summary.append(",");
            summary.append("contact:" + EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_CONTACTS) + "/" + contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CONTACTS).getTotalCount());
        }
        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).isSupported() && contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).isSelected()) {
            summary.append(",");
            summary.append("calendar:" + EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_CALENDAR) + "/" + contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).getTotalCount());
        }
        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALL_LOGS).isSupported() && contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALL_LOGS).isSelected()) {
            summary.append(",");
            summary.append("calllog:" + EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_CALL_LOGS) + "/" + contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALL_LOGS).getTotalCount());
        }
        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).isSupported() && contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).isSelected()) {
            summary.append(",");
            summary.append("message:" + EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_SMS_MESSAGES) + "/" + contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).getTotalCount());
        }
        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SETTINGS).isSupported() && contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SETTINGS).isSelected()) {
            summary.append(",");
            summary.append("setting:" + EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_SETTINGS) + "/" + contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SETTINGS).getTotalCount());
        }
        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).isSupported() && contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).isSelected()) {
            summary.append(",");
            summary.append("image:" + EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_PHOTOS) + "/" + contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).getTotalCount());
        }
        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).isSupported() && contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).isSelected()) {
            summary.append(",");
            summary.append("audio:" + EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_MUSIC) + "/" + contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).getTotalCount());
        }
        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).isSupported() && contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).isSelected()) {
            summary.append(",");
            summary.append("video:" + EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_VIDEO) + "/" + contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).getTotalCount());
        }
        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).isSupported() && contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).isSelected()) {
            summary.append(",");
            summary.append("app:" + EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_APP) + "/" + contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).getTotalCount());
        }
        return summary.toString();
    }

    public void loadMediaDetails() {
        DLog.log("Starting metadata Queue");
        DeviceInfo.getInstance().getFilesQueue().clear();
        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).isSelected()) {
            DeviceInfo.getInstance().prepareFilesQueue(EMDataType.EM_DATA_TYPE_VIDEO);
            DLog.log("Starting metadata Queue EM_DATA_TYPE_VIDEO ");
        }
        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).isSelected()) {
            DeviceInfo.getInstance().prepareFilesQueue(EMDataType.EM_DATA_TYPE_PHOTOS);
            DLog.log("Starting metadata Queue EM_DATA_TYPE_PHOTOS ");
        }
        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).isSelected()) {
            DeviceInfo.getInstance().prepareFilesQueue(EMDataType.EM_DATA_TYPE_MUSIC);
            DLog.log("Starting metadata Queue EM_DATA_TYPE_MUSIC ");
        }

        try {
            Collections.shuffle((List<?>) DeviceInfo.getInstance().getFilesQueue());
        } catch (Exception e) {
            e.printStackTrace();
        }
        metaDataPrepared = true;
        DLog.log("Ending metadata Queue " + DeviceInfo.getInstance().getFilesQueue().size());
    }

    private boolean metaDataPrepared = false;
    private Thread metadataThread = new Thread() {
        @Override
        public void run() {
            super.run();
            DLog.log("Calling loadMediaDetails");
            loadMediaDetails();
        }
    };

    private boolean progressUpdating = false;

    private void disconnectWifi() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && wifiManager.getConnectionInfo() != null) {
                int connectedId = wifiManager.getConnectionInfo().getNetworkId();
                DLog.log("Link Speed : " + wifiManager.getConnectionInfo().getLinkSpeed());
                wifiManager.disableNetwork(connectedId);
                wifiManager.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long estimateTransferTime(boolean reEstimatation) {
        if (FeatureConfig.getInstance().getProductConfig().isEstimationtimeRequired()) {

            long estimatedTimeForMedia = 0;
            long estimationTimeForPim = EstimationTimeUtility.getInstance().getEstimationForPIM(getSelectedDataTypes());
            long totalEstimation = 0;
            boolean updateUIwithEstimation = true;
            boolean displayAvgRemainingTime = true;
            try {
                long transferSpeed = CommonUtil.getInstance().getTransferSpeed();
                long totalSizetoBeTransferred = mSelectedContentSize;
                if (totalSizetoBeTransferred != 0 && transferSpeed == 0) {
                    return 0;
                }
                if (reEstimatation) {
                    transferSpeed = avgTransferSpeed;
                    totalSizetoBeTransferred = mSelectedContentSize - EMMigrateStatus.getTransferredBytesSofar();
                    if (totalSizetoBeTransferred < 0) {
                        totalSizetoBeTransferred = mSelectedContentSize - (EMMigrateStatus.getTransferredFilesSize() + EMMigrateStatus.getTransferredFilesSize());
                    }
                    if ((totalSizetoBeTransferred <= mSelectedContentSize * 0.9) || totalSizetoBeTransferred == 0 || transferSpeed == 0) {
                        displayAvgRemainingTime = false;
                    }
                }
                DLog.log("Estimation time details >>  Size :" + totalSizetoBeTransferred + " , Speed : " + transferSpeed);
                try {
                    estimatedTimeForMedia = totalSizetoBeTransferred / transferSpeed;
                    if (totalSizetoBeTransferred > 0 && estimatedTimeForMedia == 0) {
                        estimatedTimeForMedia = 1;
                    }
                    if (estimatedTimeForMedia != 0 && !reEstimatation)
                        estimatedTimeForMedia = EstimationTimeUtility.getInstance().addThresholdValue(estimatedTimeForMedia * 1000);
                    else if (reEstimatation) {
                        estimatedTimeForMedia = estimatedTimeForMedia + elapsedTimeInsec;
                        estimatedTimeForMedia = estimatedTimeForMedia * 1000;     //Converting to milliseconds.
                    }
                } catch (Exception e) {
                    DLog.log(e.getMessage());
                }
                totalEstimation = EstimationTimeUtility.getInstance().getHigherValue(estimationTimeForPim, estimatedTimeForMedia);
                totalEstimation = EMUtility.roundUp(totalEstimation, 60 * 1000L);      //Rounding upto minute.
                DLog.log("PIM Estimation time : " + estimationTimeForPim + ", Media Estimation Time : " + estimatedTimeForMedia + " , Overall Estimation time " + totalEstimation);
                long lowerEstimation = (long) (totalEstimation * Constants.ESTIMATION_LOWERLIMIT);
                long upperEstimation = (long) (totalEstimation * Constants.ESTIMATION_UPPERLIMIT);
                if (lowerEstimation >= Constants.NINTY_MINUTES) {
                    lowerEstimation = Constants.NINTY_MINUTES;
                    upperEstimation = 2 * Constants.NINTY_MINUTES;
                }
                /*if (displayRemainingTime && !reEstimatation) {
                    if (upperEstimation > Constants.NINTY_MINUTES) {
                        upperEstimation = Constants.NINTY_MINUTES;
                    }
                    if (80 * 60 * 1000L < lowerEstimation && lowerEstimation < Constants.NINTY_MINUTES) {
                        lowerEstimation = 80 * 60 * 1000L;
                    } else if (lowerEstimation >= Constants.NINTY_MINUTES) {
                        lowerEstimation = Constants.NINTY_MINUTES;
                        upperEstimation = 0;
                    }
                }*/

                String minEstimationTime = EMUtility.getReadableTime(EasyMigrateActivity.this, lowerEstimation, true);
                String maxEstimationTime = EMUtility.getReadableTime(EasyMigrateActivity.this, upperEstimation, true);
                long remainingEstimationTime = totalEstimation - (elapsedTimeInsec * 1000);
                DLog.log("Estimation time " + totalEstimation);
                DLog.log("Remaining time : " + remainingEstimationTime);
                if (remainingEstimationTime <= 0) {
                    displayAvgRemainingTime = false;
                    remainingEstimationTime = Constants.THRESHOLD_ESTIMATION;
                }
                if (remainingTimeList.size() == (ESTIMATION_INTERVAL / SPEEDCHECK_INTERVAL)) {
                    remainingTimeList.removeFirst();
                }
                if (!remainingTimeList.isEmpty())
                    remainingTimeList.addLast(remainingEstimationTime);

                long avgRemainingTime = remainingEstimationTime;
                if (!remainingTimeList.isEmpty() && displayAvgRemainingTime) {
                    avgRemainingTime = EMUtility.getMean(remainingTimeList);
                }
                if (reEstimatation) {
                    updateUIwithEstimation = EMUtility.checkVariation(CommonUtil.getInstance().getInitialEstimation(), totalEstimation, 10);
                }

                String estimatedTime = getResources().getString(R.string.estiamated_transfer_time) + "<b>" + " " + minEstimationTime + "</b>";
                String estimation = getResources().getString(R.string.estimation) + "<b>" + " " + minEstimationTime + "</b>";
                if (!maxEstimationTime.isEmpty()) {
                    estimatedTime = getResources().getString(R.string.estiamated_transfer_time) + "<b>" + " " + minEstimationTime + "</b>" + " " + getString(R.string.str_to) + " " + "<b>" + " " + maxEstimationTime + "</b>";
                    estimation = getResources().getString(R.string.estimation) + "<b>" + " " + minEstimationTime + "</b>" + " " + getString(R.string.str_to) + " " + "<b> " + " " + maxEstimationTime + "</b>";
                }
                String remainingTime = "Remaining time : " + "<b>" + " " + EMUtility.getReadableTime(EasyMigrateActivity.this, avgRemainingTime, true) + "</b>";
                if (minEstimationTime.isEmpty()) {
                    selectContentsuggestion.setText("");
                    selectContentTimeSuggestion.setText("");
                    estimationTime.setText("");
                } else if (updateUIwithEstimation || displayRemainingTime) {
                    estimationTime.setVisibility(View.VISIBLE);
                    selectContentsuggestion.setText(Html.fromHtml(estimatedTime));
                    selectContentsuggestion.setTextColor(getResources().getColor(R.color.black));
                    selectContentTimeSuggestion.setText(getResources().getString(R.string.estimated_transfer_time_suggestion));
                    if (displayRemainingTime) {
                        estimationTime.setText(Html.fromHtml(remainingTime));
                    } else {
                        estimationTime.setText(Html.fromHtml(estimation));
                        MigrationStats.getInstance().setEstimationTime(estimation);
                    }
                }
            } catch (Exception e) {
                DLog.log(e.getMessage());
            }
            return totalEstimation;
        }
        return 0;
    }

    private Runnable autoPairHandler = new Runnable() {
        @Override
        public void run() {
            if (CommonUtil.getInstance().isAutoPairingStarted()) {
                CommonUtil.getInstance().setAutoPairingStatus(false);
                showFailDialog();
            }
        }
    };

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int message = msg.what;
            Log.d(TAG, "handleMessage: " + message);
            handler.removeMessages(message);
            switch (message) {
                case START_MIGRATION:
                    if (metaDataPrepared) {
                        sendData();
                        setLayout(EPTWizardPage.Progress, EPTTransitionReason.UserNext);
                        hideProgressDialog();
                    } else {
                        handler.sendEmptyMessageDelayed(START_MIGRATION, 1000);
                    }
                    break;
                case DISCONNECT_WIFI:
                    disconnectWifi();
                    break;
                case CONNECT_WIFI:
                    connectToNetwork(connectedNetworkId);
                    break;
                case UPDATE_PROGRESS:
                    updateProgress(EMDataType.EM_DATA_TYPE_PHOTOS, EMMigrateStatus.getItemTransferStarted(EMDataType.EM_DATA_TYPE_PHOTOS), 0);
                    updateProgress(EMDataType.EM_DATA_TYPE_VIDEO, EMMigrateStatus.getItemTransferStarted(EMDataType.EM_DATA_TYPE_VIDEO), 0);
                    updateProgress(EMDataType.EM_DATA_TYPE_MUSIC, EMMigrateStatus.getItemTransferStarted(EMDataType.EM_DATA_TYPE_MUSIC), 0);
                    updateProgress(EMDataType.EM_DATA_TYPE_APP, EMMigrateStatus.getItemTransferStarted(EMDataType.EM_DATA_TYPE_APP), 0);
                    updateProgress(EMDataType.EM_DATA_TYPE_DOCUMENTS, EMMigrateStatus.getItemTransferStarted(EMDataType.EM_DATA_TYPE_DOCUMENTS), 0);
                    handler.sendEmptyMessageDelayed(UPDATE_PROGRESS, PROGRESS_INTERVAL);
                    break;
                case REESTIMATE_TIME:
                    long estimatedTime = estimateTransferTime(true);
                    handler.sendEmptyMessageDelayed(REESTIMATE_TIME, ESTIMATION_INTERVAL);
                    break;
                case DISCONNECT_NETWORK:
                    stopOrDisconnectFromAnyNetwork();
                    break;
                case CONNECTION_LOST:
                    if (isInterrupted || (IS_MMDS && iosOnlySMSSelected)) {
                        return;
                    }
                    DLog.log("Autopairing started");
                    if (IS_MMDS && !isRemoteDeviceIOS()) {
                        if (CommonUtil.getInstance().isGroupOwner() && mRole == THIS_DEVICE_IS_TARGET) {
                            //need to display dialog and and generate new wifi details send to ME
                            CommonUtil.getInstance().setMigrationStatus(SRC_NW_CHNGD);
                        } else if (mRole == THIS_DEVICE_IS_TARGET) {
                            CommonUtil.getInstance().setMigrationStatus(DST_NW_CHNGD);
                        }
                        showProgressDialog("", getString(R.string.reconnecting_message));
                    } else if (!isRemoteDeviceIOS() && FeatureConfig.getInstance().getProductConfig().isAutoPairingEnabled()) {
                        // For auto pairing we need to get pin
                        // Reconnect to previous data n/w and Check for Internet
                        if (!CommonUtil.getInstance().isGroupOwner()) {
                            EMNetworkManagerClientUIHelper.disconnectFromAnyNetworks();
                        }
                        CommonUtil.getInstance().setAutoPairingStatus(true);
                        handler.postDelayed(autoPairHandler, 5000);
                        if (mRole == THIS_DEVICE_IS_SOURCE) {
                            stateMachine.createSessionWithSessionID();
                        } else {
                            DashboardLog.getInstance().addAdditionalInfo("AutoPairing Started");
                            stateMachine.setSessionID(PreferenceHelper.getInstance(emGlobals.getmContext()).getStringItem(Constants.CLD_PIRNG_SESION_ID));
                            if (CommonUtil.getInstance().isGroupOwner()) {
                                createHostNetwork();
                            } else
                                stateMachine.getData();
                        }
                    } else {
                        showFailDialog();
                    }
                    break;
                case ESTIMATION_TIMEOUT:
                    long linkSpeed = CommonUtil.getInstance().getLinkSpeed();
                    DLog.log("Linkspeed : " + linkSpeed);
                    if (linkSpeed == 0 || isRemoteDeviceIOS()) {
                        linkSpeed = 3 * 1024 * 1024L; //Default Estimation speed : 1GB/5 Min
                    } else {
                        linkSpeed = (linkSpeed * (1024 * 1024)) / (8); //Converting into Megabits/sec to bytes/sec.
                        linkSpeed = (linkSpeed / 4);    //Using 25% of link speed to measure estimation time.
                    }
                    DLog.log("Transfer speed (25% of link speed) : " + linkSpeed);
                    CommonUtil.getInstance().setTransferSpeed(linkSpeed); //setting 25% of linkspeed as transfer speed.
                    CommonUtil.getInstance().setMigrationStatus(Constants.REVIEW_SUCCEEDED);
                    updateStorageProgress();
                    hideProgressDialog();
                    break;
                case SEND_QUEUED_MESSAGE:
                    sendCommandToRemote(sentCommandToRemote);
                    break;
                case ENABLE_WIFI:
                    enableWifi(true);
                    break;
                case DISPLAY_FINAL_SUMMERY_SCREEN:
                    DLog.log("Trying to display summery screen,with the help of timer");
                    handler.removeMessages(DISPLAY_FINAL_SUMMERY_SCREEN);
                    EMProgressInfo progressInfo = new EMProgressInfo();
                    progressInfo.mOperationType = EMProgressInfo.EMOperationType.EM_QUIT_COMMAND_SENT;
                    progressUpdate(progressInfo);
                    hideProgressDialog();
                    break;
                case NOTIFICATION_MESSAGE:
                    handler.removeMessages(NOTIFICATION_MESSAGE);
                    showNotification(getResources().getString(R.string.app_name), getResources().getString(R.string.app_running_in_background));
                    break;
                case HIDE_PROGRESS_DIALOG:
                    handler.removeMessages(HIDE_PROGRESS_DIALOG);
                    hideProgressDialog();
                    break;
                case INVALID_SESSIONID_FOR_AUTOPAIRING:
                    handler.removeMessages(INVALID_SESSIONID_FOR_AUTOPAIRING);
                    raiseFatalError("", getString(R.string.unable_to_resume), 2);
                    break;
                case EXCLUDE_MEDIA_CONTENT:
                    fromExcludeContent = false;
                    updateStorageProgress();
                    hideProgressDialog();
                    contentNotifyDatasetChanged();
                    break;
                default:
                    break;
            }
        }
    };

    private final int ESTIMATION_INTERVALS = 4;
    private int reEstimationCount = 0;
    private final int DISCONNECT_WIFI = 0;
    private final int CONNECT_WIFI = 1;
    private final int REESTIMATE_TIME = 2;
    private final int UPDATE_PROGRESS = 3;
    private final int ESTIMATION_TIMEOUT = 4;
    private final int CONNECTION_LOST = 5;
    private final int START_MIGRATION = 6;
    private final int SEND_QUEUED_MESSAGE = 7;
    private final int ENABLE_WIFI = 8;
    private final int INVALID_SESSIONID_FOR_AUTOPAIRING = 18;
    private final int NOTIFICATION_MESSAGE = 9;
    private final int DISCONNECT_NETWORK = 10;
    private final int HIDE_PROGRESS_DIALOG = 11;
    private final int DISPLAY_FINAL_SUMMERY_SCREEN = 15;
    private final int EXCLUDE_MEDIA_CONTENT = 50;
    private final int PROGRESS_INTERVAL = 3 * 1000;
    private long CONNECTION_CHECK_TIMEOUT = 40 * 1000L;

    private void sendDataForEstimation() {
        int startedChannelCount = 3;
        int completedChannelCount = 0;
        int successesChannelCount = 0;
        final int[] channelDetails = {startedChannelCount, completedChannelCount, successesChannelCount};
        final long[] totalByteswritten = {0};
        if (FeatureConfig.getInstance().getProductConfig().isEstimationtimeRequired() && mSelectedContentSize > 0) {
            showProgressDialog("", getString(R.string.estimating_pleasewait));
            if (Constants.PLATFORM_BLACKBERRY.equalsIgnoreCase(Build.BRAND) || DashboardLog.getInstance().destinationEMDeviceInfo.dbDevicePlatform.equalsIgnoreCase(Constants.PLATFORM_BLACKBERRY)) {
                channelDetails[0] = 1;
            }
            DLog.log("Sending Data for Estimation. Channels using : " + channelDetails[0]);
            handler.sendEmptyMessageDelayed(ESTIMATION_TIMEOUT, (5 * 1000) + Constants.ESTIMATION_CALCULATION_TIME);
            for (int i = 0; i < channelDetails[0]; i++) {
                mRemoteDeviceManager.sendDataForEstimation(new EMCommandDelegate() {
                    @Override
                    public void sendText(String text) {
                        //Ignore
                    }

                    @Override
                    public void sendFile(String aFilePath, boolean aDeleteFileWhenDone, EMFileSendingProgressDelegate aFileSendingProgressDelegate) {
                        //Ignore
                    }

                    @Override
                    public void getText() {
                        //Ignore
                    }

                    @Override
                    public void getXmlAsFile() {
                        //Ignore
                    }

                    @Override
                    public void getRawDataAsFile(long aLength, String aTargetFilePath) {
                        //Ignore
                    }

                    @Override
                    synchronized public void commandComplete(boolean aSuccess) {
                        ++channelDetails[1];
                        if (aSuccess) {
                            ++channelDetails[2];
                        }
                        DLog.log("command completed : " + aSuccess + " " + channelDetails[1] + "/" + channelDetails[0] + ", Success channels : " + channelDetails[2]);
                        if (channelDetails[0] == channelDetails[1]) {
                            handler.removeMessages(ESTIMATION_TIMEOUT);
                            CommonUtil.getInstance().setMigrationStatus(Constants.REVIEW_SUCCEEDED);
                            long totalBytesWritten = totalByteswritten[0];
                            long transferSpeed = (totalBytesWritten * 1000) / Constants.ESTIMATION_CALCULATION_TIME;
                            long linkSpeed = CommonUtil.getInstance().getLinkSpeed();
                            DLog.log("Wifi Linkspeed : " + linkSpeed);
                            if (linkSpeed == 0 || isRemoteDeviceIOS()) {
                                linkSpeed = 3 * 1024 * 1024L; //Default Estimation speed : 1GB/5 Min
                            } else {
                                linkSpeed = (linkSpeed * (1024 * 1024)) / (8); //Converting into Megabits/sec to bytes/sec.
                                linkSpeed = (linkSpeed / 4);    //Using 25% of link speed to measure estimation time.
                            }
                            DLog.log("Data Sent for Estimation in bytes " + totalBytesWritten + " , sent in " + Constants.ESTIMATION_CALCULATION_TIME + " Ms, speed in bytes/sec : " + transferSpeed);
                            if (transferSpeed < linkSpeed) {
                                DLog.log("Setting Transfer speed (25% of link speed) : " + linkSpeed);
                                transferSpeed = linkSpeed;
                            }
                            if (CommonUtil.getInstance().getTransferSpeed() == 0) {
                                CommonUtil.getInstance().setTransferSpeed(transferSpeed);
                                updateStorageProgress();
                                hideProgressDialog();
                            }
                        }
                    }

                    @Override
                    public void setSharedObject(String aKey, Object aObject) {
                        //Ignore
                    }

                    @Override
                    public Object getSharedObject(String aKey) {
                        return null;
                    }

                    @Override
                    public void startNoopTimer() {
                        //Ignore
                    }

                    @Override
                    public void stopNoopTimer() {
                        //Ignore
                    }

                    @Override
                    public void disableTimeoutTimer() {
                        //Ignore
                    }

                    @Override
                    public void enableTimeoutTimer() {
                        //Ignore
                    }

                    @Override
                    synchronized public void addToPreviouslyTransferredItems(String aItem) {
                        long bytesTransferred = Long.parseLong(aItem);
                        DLog.log("Bytes trasnferred : " + bytesTransferred);
                        totalByteswritten[0] = totalByteswritten[0] + bytesTransferred;
                    }

                    @Override
                    public boolean itemHasBeenPreviouslyTransferred(String aItem) {
                        return false;
                    }
                });
            }
        } else {
            CommonUtil.getInstance().setMigrationStatus(Constants.REVIEW_SUCCEEDED);
        }
    }


    private void sendFeedback(float rating, String message) {
        String url = Constants.FEEDBACK_URL;
        DeviceInfo deviceInfo = DeviceInfo.getInstance();
        FeedbackSummary feedbackSummary = new FeedbackSummary(message, String.valueOf(rating), "wds", COMPANY_NAME, deviceInfo.getBuildNumber(), deviceInfo.getDeviceLanguage());
        feedbackSummary.setMake(deviceInfo.get_make());
        feedbackSummary.setModel(deviceInfo.get_model());
        feedbackSummary.setOsVersion(deviceInfo.getOSversion());
        feedbackSummary.setDeviceId(deviceInfo.get_imei());

        Gson gson = new Gson();
        String loggingInfo = gson.toJson(feedbackSummary);
        URLConnectionTask serviceCallTask = new URLConnectionTask(EasyMigrateActivity.this, FEEDBACK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            serviceCallTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url, null, loggingInfo);
        else
            serviceCallTask.execute(url, null, loggingInfo);

    }


    private void displayNetworkDialog() {
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(EasyMigrateActivity.this);
            alert.setTitle(getString(R.string.no_network));
            alert.setMessage(getString(R.string.no_internet_message));
            alert.setPositiveButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (dialog != null)
                        dialog.cancel();
                    if (CLOUD_PAIRING_ENABLED) {
                        if (stateMachine.getSessionID() == null)
                            showProgressDialog("", getString(R.string.please_wait));
                        DLog.log("Cloud Pairing :: No Internet Popup Clicked on retry button");
                        stateMachine.startServiceCall();
                    }
                }
            });
            alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    cloudPairingInprogress = false;
                    setLayout(EPTWizardPage.Welcome, EPTTransitionReason.Automatic);
                    addOverLayMessage(OVERLAY_MIGRATION_INPROGRESS, false);
                }
            });
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    private int serverCallRetryCount = 0;

    @Override
    public void onresponseReceived(String response) {
        DLog.log("Cloud Pairing2 :: onresponseReceived : " + response);
        JSONObject jsonObj = null;
        if (devicesPaired && !CommonUtil.getInstance().isMigrationInterrupted()) {
            DLog.log("devices paired, ignoring Cloud Pairing response : " + response);
        } else if (response != null && (response.isEmpty())) {
            stateMachine.startServiceCall();
        } else if (response != null && (response.equalsIgnoreCase("NO_INTERNET"))) {
            // Handling NO Internet Scenario, showing dialog for the same
            DLog.log("Cloud Pairing :: Response :" + response);
//            if (CLOUD_PAIRING_ENABLED && (!devicesPaired || CommonUtil.getInstance().isAutoPairingStarted()) && (serverCallRetryCount == 3) && !hotSpotInfoposted) {
//                serverCallRetryCount = 0;
//                hideProgressDialog();
////                displayNetworkDialog();
//            }
//            //In low bandwidth environment we are trying three server calls
//            else if (CLOUD_PAIRING_ENABLED && (!devicesPaired || CommonUtil.getInstance().isAutoPairingStarted()) && (serverCallRetryCount < 3) && !hotSpotInfoposted) {
//                ++serverCallRetryCount;
//                stateMachine.startServiceCall();
//            }
        } else if (response != null && (response.equalsIgnoreCase("ERROR") || response.equalsIgnoreCase("TIMEOUT") || response.contains("Proxy Error"))) {
            DLog.log("Cloud Pairing :: Handling Error/Time out, retrying the server call");
            //Not doing the server calls once hotspot info posted.
            if (!hotSpotInfoposted)
                stateMachine.startServiceCall();
        }
        // response is not empty, parsing the response
        else if (response != null && !response.isEmpty()) {
            try {
                jsonObj = new JSONObject(response);
                // response contains "dataStatus"
                if (jsonObj.has("dataStatus")) {
                    try {
                        String status = jsonObj.getString("dataStatus");
                        if (status.equalsIgnoreCase("SUCCESS")) {
                            if (jsonObj.has("type") && jsonObj.has("value")) {
                                String sessionId = jsonObj.getString("value");
                                stateMachine.setSessionID(sessionId);
                                PreferenceHelper.getInstance(emGlobals.getmContext()).putStringItem(Constants.CLD_PIRNG_SESION_ID, sessionId);
                            } else if (jsonObj.has("sessionId") && jsonObj.has("pin")) {
                                String sessionId = jsonObj.getString("sessionId");
                                stateMachine.setSessionID(sessionId);
                                PreferenceHelper.getInstance(emGlobals.getmContext()).putStringItem(Constants.CLD_PIRNG_SESION_ID, sessionId);
                                stateMachine.setmPin(jsonObj.getString("pin"));
                                String pin_string = "<b>" + getResources().getString(R.string.pin) + " : " + "</b>" + jsonObj.getString("pin");
                                session_pin.setText(Html.fromHtml(pin_string));// Showing PIN on the screen
                                setDataToQR(stateMachine.getmPin(), cloudPairing_QR);// generating QR for the PIN
                                if (CommonUtil.getInstance().isGroupOwner() && CommonUtil.getInstance().isAutoPairingStarted()) {
                                    createHostNetwork();
                                } else
                                    stateMachine.getData();
                            }
                        } else { // Authentication is failed due to wrong pin, showing toast message for the same
                            if (jsonObj.has("type") && jsonObj.has("value")) {
                                if ("INVALID_AUTH".equalsIgnoreCase(jsonObj.getString("value")) && !CommonUtil.getInstance().isAutoPairingStarted()) {
                                    Toast.makeText(this, getString(R.string.ept_incorrect_pin), Toast.LENGTH_SHORT).show();
                                    scanDataFromQR(cloudPairing_scanner);
                                }
                            }
                        }
                        hideProgressDialog();
                    } catch (JSONException e) {
                        DLog.log("Cloud Pairing :: " + e);
                    }
                } else if (jsonObj.has("status") && jsonObj.getString("status").equalsIgnoreCase("INVALID_SESSION")) {
                    if (CommonUtil.getInstance().isGroupOwner()) {
                        stateMachine.postData(stateMachine.WIFI_INFO);
                    } else {
                        stateMachine.getData();
                    }
                    return;
                } else if (CommonUtil.getInstance().isAutoPairingStarted() && jsonObj.has("data") && jsonObj.getString("data").equalsIgnoreCase("INVALID_SESSION")) {
                    stateMachine.getData();
                }
                // response from server is success for previous call
                else if (jsonObj.has("status") && (!devicesPaired || CommonUtil.getInstance().isAutoPairingStarted())) {
                    // post data callback.
                    int frequency = CommonUtil.getInstance().getmFrequency();
                    if (Constants.PLATFORM.equalsIgnoreCase(Constants.PLATFORM_ANDROID) && mRole == THIS_DEVICE_IS_TARGET && Constants.PLATFORM_ANDROID.equalsIgnoreCase(remoteDevice)) {
                        // current device is Android & its at destination & supports frequency > 3000 & Source device do not supports dual band
                        stateMachine.getData();
                    } else if (Constants.SWITCH_TO_SORUCE_5GHZ && Constants.PLATFORM.equalsIgnoreCase(Constants.PLATFORM_ANDROID) && Constants.PLATFORM_ANDROID.equalsIgnoreCase(remoteDevice) && mRole == THIS_DEVICE_IS_TARGET && frequency < 3000 && isRemoteDeviceDualBand && !hotSpotInfoposted) {
                        stateMachine.getData();
                    } else if (Constants.PLATFORM_ANDROID.equalsIgnoreCase(remoteDevice) && TextUtils.isEmpty(CommonUtil.getInstance().getmWiFiPeerSSID())) {
                        stateMachine.getData();
                    }
                    if (jsonObj.getString("status").equalsIgnoreCase("SUCCESS") && hotSpotInfoposted) {
                        // HotSpot info posted successfully & creating HotSpot. Other device can connect to this HotSpot now
                        setLayout(EPTWizardPage.DisplayQRCode, EPTTransitionReason.UserNext);
                    } else if (jsonObj.getString("status").equalsIgnoreCase("SUCCESS") && CommonUtil.getInstance().getNetworkType().equalsIgnoreCase("hotspot") && !hotSpotInfoposted) {
                        // HotSpot info posted successfully & creating HotSpot. Other device can connect to this HotSpot now
                        hotSpotInfoposted = true;
                        HotspotServer hotspotServer = new HotspotServer();
                        hotspotServer.toggleHotspot(true);
                    }
                }

                try {
                    JSONObject responseData = null;
                    if (jsonObj.has("data") && "TIMEOUT".equalsIgnoreCase(jsonObj.getString("data"))) {
                        DLog.log("Cloud Pairing :: Server call timeout - retrying");
                        stateMachine.startServiceCall();
                        return;
                    }
                    if (jsonObj.has("seq")) {
                        stateMachine.setDataRecordSeq(jsonObj.getInt("seq"));
                        responseData = jsonObj;
                    } else if (jsonObj.has("dataRecord")) {
                        JSONObject dataRecord = jsonObj.getJSONObject("dataRecord");
                        if (dataRecord != null && dataRecord.has("seq")) {
                            stateMachine.setDataRecordSeq(dataRecord.getInt("seq"));
                        }
                        responseData = dataRecord;
                    }
                    if (responseData != null && responseData.has("dataType")) {
                        handleResponse(responseData.getString("dataType"), responseData.get("data").toString());
                    }
                } catch (Exception e) {
                    DLog.log("Cloud Pairing :: " + e);
                }
            } catch (Exception e) {
                DLog.log("Cloud Pairing :: " + e);
            }
        }
    }

    private boolean hotSpotInfoposted = false;
    private boolean showWifiInstructions = false;

    private void sendNetworkInfo() {
        if (Constants.WIFI_PROBLAMATIC_MODELS.contains(Build.MODEL)) {
            CommonUtil.getInstance().setmCryptoEncryptPass("");
            hotSpotInfoposted = true;
            stateMachine.postData(StateMachine.WIFI_INFO);
        } else {
            if (CommonUtil.getInstance().isAutoPairingStarted()) {
                createHostNetwork();
            } else
                setLayout(EPTWizardPage.DisplayQRCode, EPTTransitionReason.UserNext);
        }
        DLog.log("Remote Device : " + remoteDevice);
    }

    private void displayWifiSuggestionScreen() {
        if (!Constants.PLATFORM_ANDROID.equalsIgnoreCase(remoteDevice)) {
            boolean showWifiScreen = true;
            try {
                if (remoteDevice.contains("Apple")) {
                    String s = remoteDevice.substring("Apple".length() + 1);
                    int oSversion = Integer.parseInt(s.split(Pattern.quote("."))[0]);
                    DLog.log("OS version " + oSversion);
                    if (oSversion > 10) {
                        showWifiScreen = false;
                    }
                }
            } catch (Exception e) {
                DLog.log("Not IOS or not below IOS 11" + e.getMessage());
            }
            showWifiInstructions = showWifiScreen;
            if (showWifiScreen) {
                mDisplayMMDSProgress = false;
                otherDeviceIsIOSButton.callOnClick();
                addOverLayMessage(OVERLAY_MIGRATION_INPROGRESS, false);
            }
        }
    }

    private void handleResponse(String responseType, String response) {
        DLog.log("Cloud Pairing :: handleResponse : " + responseType + " - " + response);
        try {
            JSONObject jsonObject = new JSONObject(response);
            String remoteDevicePlatform;
            if (responseType.equalsIgnoreCase(StateMachine.DEVICE_INFO) && jsonObject.has("platform")) {
                DLog.log("Cloud Pairing :: Retrieved Device info");
                ActionBar supportActionBar = getSupportActionBar();
                if (supportActionBar != null && !CommonUtil.getInstance().isMigrationInterrupted()) {
                    supportActionBar.setDisplayHomeAsUpEnabled(false);
//                    if (!(IS_MMDS || Constants.FLAVOUR_SPRINT.equalsIgnoreCase(BuildConfig.FLAVOR))) {
//                        String title = getString(R.string.pairing);
//                        if (mRole == THIS_DEVICE_IS_SOURCE) {
//                            title = getResources().getString(R.string.text_source) + ": " + title;
//                        } else if (mRole == THIS_DEVICE_IS_TARGET) {
//                            title = getResources().getString(R.string.text_destination) + ": " + title;
//                        }
//                        supportActionBar.setTitle(title);
//                    }
                }
                if (!CommonUtil.getInstance().isMigrationInterrupted()) {
                    addOverLayMessage(OVERLAY_MIGRATION_INPROGRESS, true);
                    cloudPairingInprogress = true;
                    mDisplayMMDSProgress = true;
                }

                remoteDevicePlatform = jsonObject.getString("platform");
                remoteDevice = remoteDevicePlatform;

                if (jsonObject.has("deviceAddress")) {
                    CommonUtil.getInstance().setRemoteDeviceAddress(jsonObject.getString("deviceAddress"));
                }
                String remoteDeviceModel = "";
                if (jsonObject.has("model")) {
                    remoteDeviceModel = jsonObject.getString("model");
                    CommonUtil.getInstance().setRemoteDeviceModel(remoteDeviceModel);
                }
                String remoteDeviceNameP2P = "";
                if (jsonObject.has("deviceNameP2P")) {
                    remoteDeviceNameP2P = jsonObject.getString("deviceNameP2P");
                    CommonUtil.getInstance().setRemoteDeviceNameP2P(remoteDeviceNameP2P);
                }
                String remoteDeviceAPILevel = "";
                if (jsonObject.has("deviceAPILevel")) {
                    remoteDeviceAPILevel = jsonObject.getString("deviceAPILevel");
                    CommonUtil.getInstance().setRemoteDeviceAPILevel(remoteDeviceAPILevel);
                    DLog.log(TAG + " setRemoteDeviceAPILevel " + remoteDeviceAPILevel);
                }
                if (jsonObject.has("dualBandSupported")) {
                    isRemoteDeviceDualBand = Boolean.parseBoolean(jsonObject.getString("dualBandSupported"));
                }
                CommonUtil.getInstance().setRemoteDeviceDualBand(isRemoteDeviceDualBand);

                boolean isRemotP2PSupported = false;
                if (jsonObject.has("isP2PSupported")) {
                    isRemotP2PSupported = jsonObject.getBoolean("isP2PSupported");
                    // Log.d("prem", "isRemotP2PSupported = " + isRemotP2PSupported);
                }

                boolean isRemoteDeviceP2PModel = false;
                if (jsonObject.has("isP2PModel")) {
                    isRemoteDeviceP2PModel = jsonObject.getBoolean("isP2PModel");
                }
                DLog.log("Cloud Pairing :: handleResponse : remoteDevicePlatform : " + remoteDevicePlatform);
                if ((Build.VERSION.SDK_INT >= 30) && (remoteDevicePlatform.contains("Apple"))) {
                    DLog.log("Cloud Pairing :: handleResponse : remoteDevicePlatform is iOS");
                    DLog.log("Android 11 are WIFI PROBLAMATIC MODELS : " + Build.MODEL);
                    Constants.WIFI_DIRECT_PROBLAMATIC_MODELS.remove(Build.MODEL);
                    DLog.log("Android 11 are WIFI PROBLAMATIC MODELS : but due to remote Device iOS, it is removed ");
                }
                if (Constants.SUPPORT_P2P_MODE && remoteDevicePlatform.equalsIgnoreCase(PLATFORM_ANDROID) && isRemotP2PSupported && DeviceInfo.getInstance().isP2PSupported()/*isRemoteDeviceDualBand && DeviceInfo.getInstance().is5GhzSupported()*/) {
                    // Log.d("prem", "P2P Condition1...");
                    // Log.d("prem", "P2P Device Address: " + CommonUtil.getInstance().getDeviceAddress());
                    // Log.d("prem", "P2P Remote Device Address: " + CommonUtil.getInstance().getRemoteDeviceAddress());
                    String rdAPILevel = CommonUtil.getInstance().getRemoteDeviceAPILevel();
                    int rdAPILevelInt = Integer.parseInt(rdAPILevel);

                    if (!TextUtils.isEmpty(CommonUtil.getInstance().getDeviceAddress()) && !TextUtils.isEmpty(CommonUtil.getInstance().getRemoteDeviceAddress()) &&
                            (P2P_MODELS.contains(DeviceInfo.getInstance().get_model()) || isRemoteDeviceP2PModel || ((Build.VERSION.SDK_INT >= 30) && (rdAPILevelInt >= 30)))) {
                        // Log.d("prem", "P2P Condition2...");
                        DLog.log(TAG + " mTransferMode set to P2P_MODE  because both src and dst are android 11 and above or they are added in p2p model list in server side ");
                        Constants.mTransferMode = P2P_MODE;
                    }
                }

                DLog.log("CommonUtil.getInstance().getDeviceNameP2P() : " + CommonUtil.getInstance().getDeviceNameP2P() + " , CommonUtil.getInstance().getRemoteDeviceNameP2P() " + CommonUtil.getInstance().getRemoteDeviceNameP2P());
                DLog.log("DeviceInfo.getInstance().get_model() : " + DeviceInfo.getInstance().get_model() + " , CommonUtil.getInstance().getRemoteDeviceModel() " + CommonUtil.getInstance().getRemoteDeviceModel());
                /*if (!TextUtils.isEmpty(CommonUtil.getInstance().getDeviceNameP2P()) && !TextUtils.isEmpty(CommonUtil.getInstance().getRemoteDeviceNameP2P())
                        && (P2P_PROBLEMATIC_MODELS.contains(CommonUtil.getInstance().getRemoteDeviceModel() ) && P2P_PROBLEMATIC_MODELS.contains(CommonUtil.getInstance().getRemoteDeviceModel()))) {
                    DLog.log("Satya P2P Condition2...");
                    Constants.mTransferMode = P2P_MODE;
                }*/
                /*if (!TextUtils.isEmpty(CommonUtil.getInstance().getDeviceNameP2P()) && !TextUtils.isEmpty(CommonUtil.getInstance().getRemoteDeviceNameP2P())) {
                    DLog.log("Satya P2P Condition2...");
                    Constants.mTransferMode = P2P_MODE;
                }*/


                DLog.log("Final Constants.mTransferMode ... " + Constants.mTransferMode);
                if (mTransferMode.equalsIgnoreCase(P2P_MODE)) {
                    deviceConnected = false;
                    connectionInfoAvailable = false;
                    if (Build.VERSION.SDK_INT >= 23) {
                        selectedDontAskAgain = false;
                    }
                    //Location permissions required from Marshmallow(23)
                    if (!locationPermissionRequired()) {
                        handler.postDelayed(startP2PThread, 3000);
                    }
                } else if ((remoteDevicePlatform.equalsIgnoreCase(PLATFORM_ANDROID) && mRole == THIS_DEVICE_IS_SOURCE) || (Constants.PLATFORM.equalsIgnoreCase(PLATFORM_BLACKBERRY) && PLATFORM_ANDROID.equalsIgnoreCase(remoteDevicePlatform))) {
                    // A-A in this case source A is waits for dest A's wifi info || (BB-A || A-BB)//  in this case BB waits for A's wifi info
                    DLog.log("Cloud Pairing :: Polling for WIFI info");
                    stateMachine.getData(); // Polling for WIFI info
                } else if ((Constants.PLATFORM.equalsIgnoreCase(PLATFORM_BLACKBERRY) && mRole == THIS_DEVICE_IS_TARGET) && remoteDevicePlatform.contains("Apple")) {
                    // iOS-BB (currently it is not supported, showing same info in dialog)
                    addOverLayMessage(OVERLAY_MIGRATION_INPROGRESS, false);
                    cloudPairingInprogress = false;
                    onBackPressed();
                    raiseFatalError("", getString(R.string.wrong_combination), 1);
                } else if (Constants.PLATFORM.equalsIgnoreCase(PLATFORM_BLACKBERRY) && (remoteDevicePlatform.contains("Apple") || remoteDevicePlatform.contains(PLATFORM_BLACKBERRY))) {
                    // BB-BB || BB-iOS (Changing to WLAN, as BB & iOS platforms do not support  WIFI Direct)
                    DLog.log("Cloud Pairing :: Changing transfer mode to WLAN");
                    //mTransferMode= "WLAN";
                    if (mRole == THIS_DEVICE_IS_SOURCE) {
                        stateMachine.getData();
                    } else {
                        String networkDetails = setWLANDetails();
                        if (networkDetails == null || networkDetails.isEmpty()) {
                            displayWlanDialog(null, 1);
                        } else {
                            stateMachine.postData(StateMachine.WIFI_INFO);
                        }
                    }
                } else {
                    // Any other combination (A-A(Dest A send wifi info to Source A), A-iOS, iOS-A, W-A, A-BB, BB-A (in this case A sends wifi Direct info to other platform))
                    DLog.log("Cloud Pairing :: Sending network info");
                    sendNetworkInfo(); //sending WIFI Direct / WIFI LAN details
                }
                if (!mTransferMode.equalsIgnoreCase(P2P_MODE) && !CommonUtil.getInstance().isAutoPairingStarted()) {
                    try {
                        unregisterReceiver(wiFiObserver);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (jsonObject.has("fixedPort"))
                    EMConfig.FIXED_PORT_NUMBER = jsonObject.getInt("fixedPort");
                if (jsonObject.has("dataTransferPort"))
                    EMServer.CONTENT_TRANSFER_PORT = jsonObject.getInt("dataTransferPort");

                DLog.log("Cloud Pairing :: updated ports : " + EMConfig.FIXED_PORT_NUMBER + " " + EMServer.CONTENT_TRANSFER_PORT);
                PreferenceHelper.getInstance(getApplicationContext()).putIntegerItem("fixed_port", EMConfig.FIXED_PORT_NUMBER);

                Intent intent = new Intent(this, EasyMigrateService.class);
                startService(intent);
                getApplicationContext().bindService(intent, connection, this.BIND_AUTO_CREATE);
            } else if (responseType.equalsIgnoreCase(StateMachine.WIFI_INFO)) {
                //response contains WIFI info
                DLog.log("Cloud Pairing :: Retrieved WIFI info ");
                // after receiving WIFI info, destination device will disable the wifi direct and
                // before connecting to source device's wifi, it will update the server to retrieve the session id.
                // Because once its connects to source, it will lost internet connection.
                if (mRole == THIS_DEVICE_IS_TARGET && !CommonUtil.getInstance().isAutoPairingStarted()) {
                    DLog.log("Cloud Pairing :: Going to disable WiFi Direct & updating to server");
                    disableWifiDirect();
                    DashboardLog.getInstance().updateSessionStatus(Constants.SESSION_STATUS.IN_PROGRESS);
                    DashboardLog.getInstance().geteDeviceSwitchSession().setSessionStage(Constants.SESSION_STAGE.QR_GENERATED.value());
                    DashboardLog.getInstance().updateToServer(true);
                    EMNetworkManagerClientUIHelper.reset();
                }
                // parsing the response
                CommonUtil commonUtil = CommonUtil.getInstance();
                if (jsonObject.has("SSID"))
                    commonUtil.setmWiFiPeerSSID(jsonObject.getString("SSID"));
                if (jsonObject.has("PASSWORD"))
                    commonUtil.setmWiFiPeerPassphrase(jsonObject.getString("PASSWORD"));
                if (jsonObject.has("FREQUENCY"))
                    commonUtil.setmFrequency(Integer.parseInt(jsonObject.getString("FREQUENCY")));
                if (jsonObject.has("PIN"))
                    commonUtil.setmCryptoEncryptPass(jsonObject.getString("PIN"));
                if (jsonObject.has("TYPE"))
                    commonUtil.setNetworkType(jsonObject.getString("TYPE"));
                InetAddress inetAddress = null;
                try {
                    inetAddress = InetAddress.getByName(jsonObject.getString("IP").replace("/", ""));//"192.168.49.1"
                    CommonUtil.getInstance().setmWiFiPeerAddress(inetAddress);
                    CommonUtil.getInstance().setRemoteDeviceIpAddress(inetAddress);
                } catch (Exception Ex) {
                    DLog.log("Cloud Pairing :: " + Ex);
                }
                if (barcodeView != null) {
                    barcodeView.getBarcodeView().pause();
                    barcodeView = null;
                }
                if (mTransferMode.equalsIgnoreCase("WLAN")) {
                    //connects to source device's wifi LAN
                    String connectedNetwork = NetworkUtil.getConnectedNetworkName();
                    String destNetworkName = commonUtil.getmWiFiPeerSSID();
                    if (connectedNetwork == null || !connectedNetwork.equals(destNetworkName)) {
                        displayWlanDialog(destNetworkName, 0);
                    } else {
                        setLayout(EPTWizardPage.ScanQRCode, EPTTransitionReason.UserNext);
                    }
                } else {
                    // Connects to source device's wifi Direct
                    DLog.log("Cloud Pairing :: Connect to Other device's wifi Direct");
                    connectToClientNetwork();
                }
            }
        } catch (JSONException e) {
            DLog.log("Cloud Pairing :: " + e);
        }
    }

    private void displayWlanDialog(final String networkName, final int type) {
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(EasyMigrateActivity.this);
            if (type == 1) {
                alert.setTitle(getString(R.string.ept_connect_to_wifi_title));
                alert.setMessage(getString(R.string.ept_connect_to_wifi_message));
            } else {
                String message = MessageFormat.format(getString(R.string.wlan_suggestion), networkName);
                alert.setMessage(message);
            }
            alert.setPositiveButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (type == 0) {
                        String connectedNetwork = NetworkUtil.getConnectedNetworkName();
                        if (connectedNetwork != null && !connectedNetwork.equals(networkName)) {
                            displayWlanDialog(networkName, type);
                        } else {
                            setLayout(EPTWizardPage.ScanQRCode, EPTTransitionReason.UserNext);
                        }
                    } else {
                        String networkDetails = setWLANDetails();
                        if (networkDetails == null || networkDetails.isEmpty()) {
                            displayWlanDialog(null, type);
                        } else {
                            stateMachine.postData(StateMachine.WIFI_INFO);
                        }
                    }
                }
            });
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean cloudPairingInprogress = false;

    public void displayPINDialog() {
        try {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            String message = getString(R.string.str_enterpin);
            LayoutInflater layoutInflaterAndroid = LayoutInflater.from(EasyMigrateActivity.this);
            View mView = layoutInflaterAndroid.inflate(R.layout.input_dialog, null);
            builder.setView(mView);
            builder.setMessage(message);
            final EditText userInputDialogEditText = (EditText) mView.findViewById(R.id.userInputDialog);
            final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        dialog.dismiss();
                        String enteredPin = userInputDialogEditText.getText().toString().trim();
                        if (enteredPin.isEmpty() || enteredPin.length() != 6) {
                            displayPINDialog();
                            Toast.makeText(EasyMigrateActivity.this, getString(R.string.ept_incorrect_pin), Toast.LENGTH_SHORT).show();
                        } else {
                            showProgressDialog("", getString(R.string.please_wait));
                            stateMachine.validateSession(enteredPin);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            String positiveButton = getResources().getString(R.string.submit);
            builder.setPositiveButton(positiveButton, onClickListener);
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Ignore
                }
            });
            final AlertDialog alert = builder.create();
            alert.setCancelable(false);
            alert.setCanceledOnTouchOutside(false);
            alert.show();
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

            userInputDialogEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (TextUtils.isEmpty(s) || s.length() < 6) {
                        alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    } else {
                        alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            userInputDialogEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        alert.getButton(AlertDialog.BUTTON_POSITIVE).callOnClick();
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void setDataToQR(String messageString, ImageView aImageView) {
        DLog.log("Setting Pin to QR Code " + messageString);
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap qrCodeBitmap = barcodeEncoder.encodeBitmap(messageString, BarcodeFormat.QR_CODE, 1024, 1024);
            aImageView.setImageBitmap(qrCodeBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanDataFromQR(final DecoratedBarcodeView aBarcodeView) {
        if (aBarcodeView != null) {
            aBarcodeView.resume();
            aBarcodeView.decodeSingle(new BarcodeCallback() {
                @Override
                public void barcodeResult(BarcodeResult barcodeResult) {
                    String messageString = barcodeResult.getText();
                    if (aBarcodeView.getBarcodeView() != null) {
                        aBarcodeView.getBarcodeView().pause();
                    }
                    DLog.log("Got QR code message string : " + messageString);
                    boolean validQrcode = true;
                    try {
                        Integer.parseInt(messageString);
                    } catch (Exception e) {
                        e.printStackTrace();
                        validQrcode = false;
                    }
                    if (validQrcode) {
                        showProgressDialog("", getString(R.string.please_wait));
                        StateMachine.getInstance(context, 2).validateSession(messageString.trim());
                    } else {
                        Toast.makeText(EasyMigrateActivity.this, getString(R.string.ept_incorrect_pin), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void possibleResultPoints(List<ResultPoint> list) {
                    // Ignore
                }
            });
        }
    }


    AlertDialog blackberryWifiDialog = null;

    private void showWifiConnectDialog(final boolean reconnect) {
        final String mWiFiDirectPeerSSID = CommonUtil.getInstance().getmWiFiPeerSSID();
        final String password = CommonUtil.getInstance().getmWiFiPeerPassphrase();

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setCancelable(false);
        String message = getString(R.string.copy_connect_network);
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(EasyMigrateActivity.this);
        View mView = layoutInflaterAndroid.inflate(R.layout.input_dialog_wifi, null);
        alertDialogBuilder.setView(mView);

        final TextView wifiName = (TextView) mView.findViewById(R.id.name);
        final TextView wifiPassword = (TextView) mView.findViewById(R.id.password);
        final TextView copyButton = (TextView) mView.findViewById(R.id.copy);
        wifiName.setText(getResources().getString(R.string.str_name) + " " + mWiFiDirectPeerSSID);
        wifiPassword.setText(getResources().getString(R.string.str_password) + " " + password);
        if (reconnect) {
            message = MessageFormat.format(getString(R.string.wlan_suggestion), mWiFiDirectPeerSSID);
            wifiName.setVisibility(View.GONE);
            wifiPassword.setVisibility(View.GONE);
            copyButton.setVisibility(View.GONE);
        }
        alertDialogBuilder.setMessage(message);
        copyButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EMUtility.copyTextToClipboard("password", password);
                copyButton.setText(getString(R.string.copied));
                copyButton.setAlpha(0.5f);
            }
        });
        alertDialogBuilder.setPositiveButton(getString(R.string.str_go_to_settings), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });
                /*.setPositiveButton(getString(R.string.str_continue), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (mWiFiDirectPeerSSID != null && mWiFiDirectPeerSSID.equalsIgnoreCase(NetworkUtil.getConnectedNetworkName())) {
                            dialog.dismiss();
                            if (reconnect) {
                                handler.removeMessages(CONNECTION_LOST);
                                CommonUtil.getInstance().resumeMigration();
                            } else {
                                setLayout(EPTWizardPage.Connecting, EPTTransitionReason.UserNext);
                                EMNetworkManagerClientUIHelper.startPairing();
                            }
                        } else {
                            showWifiConnectDialog(reconnect);
                        }
                    }
                })*/
        blackberryWifiDialog = alertDialogBuilder.create();
        blackberryWifiDialog.setCanceledOnTouchOutside(false);
        blackberryWifiDialog.setCancelable(false);
        blackberryWifiDialog.show();
    }

    private void enterPinSuggestion(final TextView textView) {
        SpannableString ss = new SpannableString(getString(R.string.faulty_camera_suggestion));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                displayPINDialog();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
            }
        };
        int startLength = 0;
        int endLength = 0;
        try {
            startLength = getString(R.string.faulty_camera_suggestion).indexOf(".") + 1;
            endLength = getString(R.string.faulty_camera_suggestion).length();
            ss.setSpan(clickableSpan, startLength, endLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), startLength, endLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(ss);
        } catch (Exception e) {
            e.printStackTrace();
            textView.setText(getString(R.string.faulty_camera_suggestion));
        }
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    //To send WLAN Details (BB-BB, BB-IOS)
    private String setWLANDetails() {
        try {
            CommonUtil commonUtil = CommonUtil.getInstance();
            String netWorkName = NetworkUtil.getConnectedNetworkName();
            int pin = EMUtility.getRandomSecurePIN();
            commonUtil.setmFrequency(2500);
            commonUtil.setmWiFiPeerAddress(InetAddress.getByName(NetworkUtil.getIpAddress()));
            commonUtil.setmWiFiPeerSSID(netWorkName);
            commonUtil.setmWiFiPeerPassphrase("null");
            commonUtil.setmCryptoEncryptPass(String.valueOf(pin));
            EMNetworkManagerClientUIHelper.setEncryptionPasscode(String.valueOf(pin));
            return netWorkName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void clean() {
        isInterrupted = false;
        isMigrationInProgress = false;
        Constants.stopMigration = false;
        devicesPaired = false;
        EMMigrateStatus.initialize();
        transactionResumed = false;
        CommonUtil.getInstance().setTransferSpeed(0);
    }

    private void stopAllProcesses() {
        try {
            stopRunningServices();
            elapsedTimer.cancel();
            reEstimationTimer.cancel();
            handler.removeMessages(REESTIMATE_TIME);
            mRemoteDeviceManager.cancelSessions();
            CommonUtil.getInstance().cleanRunningAsyncTask();
            CommonUtil.getInstance().cleanRunningThreads();
            CommonUtil.getInstance().setTransferSpeed(0);
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

    /**
     * Handled all the scenorio's in this method related to the permissions
     *
     * @param isFromSettings returns true if Permissions enable or disable from settings else return false.
     * @return true if all the permissions are granted, else return false
     */

    private boolean checkAndRequestPermissions(boolean isFromSettings) {
        updatePermissionsList();
        boolean isPermissionRequired = true;
        Set<String> deniedPerm = new HashSet<>();
        for (ContentDetails contentDetails : contentDetailsMap.values()) {
            if (contentDetails.isSupported() && !contentDetails.isPermissionGranted()) {
                deniedPerm.addAll(Arrays.asList(Constants.PERMISSIONMAP.get(contentDetails.getContentType())));
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            deniedPerm.add(Manifest.permission.CAMERA);
        }
        if (!deniedPerm.isEmpty() && !isFromSettings) {
            ActivityCompat.requestPermissions(this,
                    deniedPerm.toArray(new String[deniedPerm.size()]), APP_PERMISSIONS_REQUSET_CODE);
            isPermissionRequired = false;
        } else if (deniedPerm.isEmpty() && isFromSettings) {
            setLayout(EPTWizardPage.SelectOldOrNewDevice, EPTTransitionReason.UserNext);
        } else if (isFromSettings) {
            permissionsNotifyDatasetChanged();
        }
        return isPermissionRequired;
    }


    private boolean anyPermissionRequired() {
        updatePermissionsList();
        Set<String> deniedPerm = new HashSet<>();
        for (ContentDetails contentDetails : contentDetailsMap.values()) {
            if (SUPPORTED_DATATYPE_MAP.get(contentDetails.getContentType()) && contentDetails.isSupported() && !contentDetails.isPermissionGranted()) {
                deniedPerm.addAll(Arrays.asList(Constants.PERMISSIONMAP.get(contentDetails.getContentType())));
            }
        }
        return !deniedPerm.isEmpty();
    }

    /**
     * Camera permission is required for the API level 23 and above.
     *
     * @return true, if the camera permission is granted else return false.
     */
    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSIONS_REQUSET_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        DLog.log("Permission callback called------- on page : " + mCurrentPage.name());
        switch (requestCode) {
            case APP_PERMISSIONS_REQUSET_CODE: {
                ArrayList<Integer> deniedPermissionList = new ArrayList<>();
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            deniedPermissionList.add(grantResults[i]);
                        }
                    }
                }
                updatePermissionsList();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this) && mCurrentPage == EPTWizardPage.Welcome) {
                    DLog.log("onACtivityResult callback : WRITE_SETTINGS_PERMISSION denied");
                    settingsPermissionDialog(context.getResources().getString(R.string.str_write_permission), true);
                } else if (deniedPermissionList.isEmpty()) {
                    setLayout(EPTWizardPage.SelectOldOrNewDevice, EPTTransitionReason.UserNext);
                } else {
                    if (mCurrentPage == EPTWizardPage.Welcome)
                        setLayout(EPTWizardPage.Permissions, EPTTransitionReason.UserNext);
                    else {
                        permissionsNotifyDatasetChanged();
                        //shouldShowRequestPermissionRationale will return false
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALENDAR)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALL_LOG)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS)) {
                        }
                        //permission is denied (and Don't/Never ask again is  checked)
                        else {
                            showPermissionDialog(getResources().getString(R.string.str_permission_go_to_settings), getResources().getString(R.string.ept_select_content_settings), GO_TO_SETTINGS_REQUEST_CODE);
                        }
                    }
                }

                break;
            }
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLatLong();
                }
                break;
            case P2P_PERMISSION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    handler.postDelayed(startP2PThread, 100);
                } else {
                    if (Build.VERSION.SDK_INT >= 23) {
                        selectedDontAskAgain = !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                    if (selectedDontAskAgain)
                        displayP2PPermissionsDialog(getString(R.string.str_alert), getString(R.string.location_permission_denied_msg));
                    else
                        displayP2PPermissionsDialog(getString(R.string.str_alert), getString(R.string.location_permission_required_msg));
                }
                break;
        }
    }

    /**
     * Displaying the dialog to enable the permissions if user checked the don't ask again
     *
     * @param message is displayed according to the user interaction.
     */
    public void showPermissionDialog(String message, String positiveBtn, final int requestCode) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(positiveBtn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (requestCode) {
                            case GO_TO_SETTINGS_REQUEST_CODE:
                                startActivityForResult(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID)), APP_PERMISSIONS_REQUSET_CODE);
                                break;
                        }

                    }
                })
                .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Ignore
                    }
                })
                .create()
                .show();
    }


    /**
     * Permission is required for modify the settings in the device, if the API level is 23 and above.
     * if permission is enabled, then sipmly parsing the xml file.
     */
    public static void enableWritePermissionForSettings(PermissionHandler getSettingsPermissionHandler) {
        mSettingsPermissionHandler = getSettingsPermissionHandler;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(activity)) {
                DLog.log("settings ------> write permission enabled");
                mSettingsPermissionHandler.userAccepted();
            } else {
                DLog.log("settings -----> write permission disabled");
                mSettingsPermissionHandler.userDenied();
                //settingsPermissionDialog(context.getResources().getString(R.string.str_write_permission),true);
            }
        }
    }

    /**
     * Displaying dialog if the write permission is disabled.
     *
     * @param message
     */
    public void settingsPermissionDialog(String message, boolean optionTodeny) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(this)) {
            return;
        }
        DLog.log("Displaying write settingsPermissionDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog);
        CustomTextView title = new CustomTextView(EasyMigrateActivity.this);
        title.setText(Html.fromHtml(message));
        title.setBackgroundResource(R.color.white);
        title.setPadding(30, 30, 20, 0);
        title.setTextColor(getResources().getColor(R.color.black));
        title.setTextAppearance(R.style.textStyle_title);
        title.setLineSpacing(3.0f, 1.0f);
        try {
            Typeface font = Typeface.createFromAsset(getAssets(), "fonts/" + getString(R.string.font_regular));
            title.setTypeface(font);
        } catch (Exception e) {
            e.printStackTrace();
        }
        builder.setCustomTitle(title);
        builder.setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.str_allow), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        startWatchingPermission(AppOpsManager.OPSTR_WRITE_SETTINGS);
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, SETTINGS_WRITE_PERMISSION_REQUEST_CODE);
                    }
                });
        if (optionTodeny) {
            builder.setNegativeButton(getResources().getString(R.string.str_deny), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    DLog.log("User denied the settings ----->");
                    if (mCurrentPage == EPTWizardPage.Welcome) {
                        onActivityResult(SETTINGS_WRITE_PERMISSION_REQUEST_CODE, 1, null);
                    } else if (mSettingsPermissionHandler != null) {
                        mSettingsPermissionHandler.userDenied();
                    }
                }
            });
        }

        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    /**
     * If permissions are not granted, here again we asking click here to grant the permissions.
     *
     * @param textView
     */
    private void clickHereToGrantPermission(final TextView textView) {
        try {
            SpannableString ss = new SpannableString(textView.getText().toString());
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View view) {
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            };
            String clickHere = getString(R.string.click_here).toLowerCase();
            int startIndex = textView.getText().toString().toLowerCase().indexOf(clickHere);
            int endIndex = startIndex + clickHere.length();
            ss.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(ss);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * disabled or denied datatypes from the destination device If API level 23 and above.
     *
     * @param deniedDatatype
     */
    private void disableDataTypes(int deniedDatatype) {
        for (ContentDetails contentDetails : contentDetailsMap.values()) {
            if ((deniedDatatype & contentDetails.getContentType()) != 0) {
                contentDetails.setPermissionGranted(false);
            }
        }
    }

    /**
     * updating the permissions in the contentDetailsMap
     */
    private void updatePermissionsList() {
        try {
            if (contentDetailsMap == null || contentDetailsMap.isEmpty()) {
                loadContentDetailsMap();
            } else {
                for (ContentDetails contentDetails : contentDetailsMap.values()) {
                    if (contentDetails.isSupported()) {
                        boolean permission = isPermissionGranted(contentDetails.getContentType());
                        contentDetails.setPermissionGranted(permission);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void permissionsNotifyDatasetChanged() {
        contentDetailsAdapter = new ContentDetailsAdapter(EasyMigrateActivity.this, ContentDetailsAdapter.PERMISSIONS, contentDetailsMap);
        deniedPermissionsView.setAdapter(contentDetailsAdapter);
    }

    private void contentNotifyDatasetChanged() {
        contentDetailsAdapter = new ContentDetailsAdapter(EasyMigrateActivity.this, ContentDetailsAdapter.CONTENT_SELECTION, contentDetailsMap);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        contentDetailsView.setLayoutManager(mLayoutManager);
        contentDetailsView.setItemAnimator(null);
        contentDetailsView.setAdapter(contentDetailsAdapter);
        contentDetailsAdapter.notifyDataSetChanged();
    }

    private void registerBatteryStatusBroadcastReceiver() {
        //register intent only for wds
        IntentFilter intentFilterBatteryChange = new IntentFilter();
        intentFilterBatteryChange.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilterBatteryChange.addAction(Intent.ACTION_POWER_CONNECTED);
        batteryStatusBroadcastReceiver = new BatteryStatusBroadcastReceiver(this);
        registerReceiver(batteryStatusBroadcastReceiver, intentFilterBatteryChange);
    }

    public void displayRatingDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.rating_dialog);
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        TextView btnRating = (TextView) dialog.findViewById(R.id.btn_ratenow);
        TextView btnNotNow = (TextView) dialog.findViewById(R.id.btn_cancel);
        RatingBar ratingBar = (RatingBar) dialog.findViewById(R.id.rating_bar);
        // modifying the rating bar color
        LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
        stars.getDrawable(2).setColorFilter(getResources().getColor(R.color.yellow), PorterDuff.Mode.SRC_ATOP);
        if (BuildConfig.DEBUG) {
            btnRating.setAlpha(0.5f);
            btnRating.setEnabled(false);
        }
        btnRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                Intent sprintPlayStoreLink = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(sprintPlayStoreLink);
                } catch (Exception e) {
                    DLog.log("got exception while rating the app : " + e.getMessage());
                }
            }
        });
        btnNotNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    private class GpsListner implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                DLog.log("onLocationChanged " + longitude + " " + latitude);
                locManager.removeUpdates(gpsListner);
                DashboardLog.getInstance().geteDeviceSwitchSession().setLatitude(BigDecimal.valueOf(latitude));
                DashboardLog.getInstance().geteDeviceSwitchSession().setLongitude(BigDecimal.valueOf(longitude));
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    private void getLatLong() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        if (this.locManager != null && locManager.isProviderEnabled("gps")) {
            gpsListner = new GpsListner();
            this.locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2 * 1000L, 10, gpsListner);
            this.locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2 * 1000L, 10, gpsListner);
        } else {
            new startLocationAlert(EasyMigrateActivity.this);
        }
    }

    private long lastcheckedBytes = 0;
    private long avgTransferSpeed = 0;
    private final List<Long> intervalSpeeds = new ArrayList<>();
    private static final long ESTIMATION_INTERVAL = 60 * 1000L;
    private static final long SPEEDCHECK_INTERVAL = 15 * 1000L;
    private boolean displayRemainingTime = true;
    private LinkedList<Long> remainingTimeList = new LinkedList<>();

    private long getTransferSpeed(long interval) {
        long transferredBytes = EMMigrateStatus.getTransferredBytesSofar();
        long recentTransferSpeed = (transferredBytes - lastcheckedBytes) / (interval / 1000);
        long initialTransferSpeed = CommonUtil.getInstance().getTransferSpeed();
        lastcheckedBytes = transferredBytes;

        if (recentTransferSpeed <= 0) {
            recentTransferSpeed = CommonUtil.getInstance().getTransferSpeed();
        } else if (recentTransferSpeed < (initialTransferSpeed * 0.5)) {
            recentTransferSpeed = (long) (initialTransferSpeed * 0.5);
        }
        return recentTransferSpeed;
    }

    private CountDownTimer reEstimationTimer = new CountDownTimer(ESTIMATION_INTERVAL, SPEEDCHECK_INTERVAL) {
        @Override
        public void onTick(long millisUntilFinished) {
            long speed = getTransferSpeed(SPEEDCHECK_INTERVAL);
            if (speed != 0)
                intervalSpeeds.add(speed);
        }

        @Override
        public void onFinish() {
            avgTransferSpeed = EMUtility.getMean(intervalSpeeds);
            intervalSpeeds.clear();
            estimateTransferTime(true);
            this.start();
        }
    };

    private void showNotification(String title, String content) {
        if (mNotificationManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("default", "General Notifications", NotificationManager.IMPORTANCE_HIGH);
                channel.setLightColor(Color.BLUE);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                mNotificationManager.createNotificationChannel(channel);
            }
            long[] pattern = {0, 0};
            Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "default")
                    .setSmallIcon(R.drawable.notification_small)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setVibrate(pattern)
                    .setLargeIcon(largeIcon)
                    .setContentTitle(title)
                    .setAutoCancel(true)
                    .setContentText(content);
            Intent intent = new Intent(getApplicationContext(), EasyMigrateActivity.class);
            PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setDefaults(Notification.DEFAULT_ALL);
            mBuilder.setContentIntent(pi);
            mNotificationManager.notify(0, mBuilder.build());
        }
    }

    private void displayDateDialog(final EditText editText) {
        DLog.log("showing date picker");
        final Calendar c = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        editText.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        TextView after = new TextView(context);
        after.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        after.setText(getResources().getString(R.string.after));
        after.setTextSize(16);
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/" + getString(R.string.font_regular));
        after.setTypeface(font);
        after.setTextColor(getResources().getColor(R.color.white));
        after.setPadding(50, 30, 0, 0);
        datePickerDialog.setCustomTitle(after);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private int customSMSSelectionButtonId = 0;

    private void messageCustomSelectionDialog() {
        final Dialog dialog = new Dialog(EasyMigrateActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.message_setting);
        final RadioGroup radioGroup = dialog.findViewById(R.id.radio_group);
        CustomTextView cancelButton = (CustomTextView) dialog.findViewById(R.id.message_setting_cancel);
        CustomTextView transfer_button = (CustomTextView) dialog.findViewById(R.id.message_setting_transfer);
        final EditText addDatePicker = (EditText) dialog.findViewById(R.id.datePicker);
        final int customRadioButtonId = 111;
        addDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DLog.log("editext clicked");
                RadioButton radioButton = (RadioButton) radioGroup.findViewById(customRadioButtonId);
                if (radioButton.isChecked()) {
                    displayDateDialog(addDatePicker);
                } else {
                    radioButton.setChecked(true);
                }
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });
        transfer_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long count = 0;
                int id = radioGroup.getCheckedRadioButtonId();
                if (id == -1) {
                    return;
                }
                customSMSSelectionButtonId = id;
                if (id == customRadioButtonId) {
                    if (addDatePicker.getText().toString().isEmpty()) {
                        Toast.makeText(EasyMigrateActivity.this, getResources().getString(R.string.select_proper_date), Toast.LENGTH_SHORT).show();
                    } else {
                        String dateString = addDatePicker.getText().toString();
                        try {
                            long millisFromDate = EMUtility.getMillisFromDate(dateString);
                            CommonUtil.getInstance().setMessageSelectionFrom(millisFromDate);
                            count = DeviceInfo.getInstance().getSMSCountFromTime(millisFromDate);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    count = customSelectMessageMap.get(radioGroup.getCheckedRadioButtonId());
                    Date currentDate = new Date();
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(currentDate);
                    calendar.add(Calendar.MONTH, -radioGroup.getCheckedRadioButtonId());
                    calendar.set(Calendar.HOUR, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    long time = calendar.getTimeInMillis();
                    CommonUtil.getInstance().setMessageSelectionFrom(time);
                    if (radioGroup.getCheckedRadioButtonId() == 0) {
                        CommonUtil.getInstance().setMessageSelectionFrom(0);
                    }
                }

                mMessageCount = (int) count;
                contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).setTotalCount((int) count);
                contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).setSelected(count > 0);
                contentDetailsAdapter.notifyItemChanged(getContentTypePosition(EMDataType.EM_DATA_TYPE_SMS_MESSAGES));
                EMMigrateStatus.addContentDetails(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, mMessageCount);
                updateStorageProgress();
                DLog.log("Selected Messages Count : " + count);
                dialog.cancel();

            }
        });
        try {
            float scale = getResources().getDisplayMetrics().density;
            int dpAsPixels = (int) (10 * scale + 0.5f);
            int i = 0;
            String[] stringArray = getResources().getStringArray(R.array.message_setting_Array);
            for (Integer month : customSelectMessageMap.keySet()) {
                RadioButton radioButton = new RadioButton(EasyMigrateActivity.this);
                radioButton.setId(month);
                radioButton.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels);
                String currentString = stringArray[i++];
                String value = String.valueOf(customSelectMessageMap.get(month));
                if (customSelectMessageMap.get(month) == 0) {
                    value = getResources().getString(R.string.no);
                }
                String message = String.format(currentString, value);
                if (customSelectMessageMap.get(month) != 0) {
                    String estimationTime = EMUtility.getRange(EstimationTimeUtility.getInstance().getIndividualEstimationTime(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, customSelectMessageMap.get(month)), EasyMigrateActivity.this);
                    message = message + "<br/>" + estimationTime;
                }
                radioButton.setClickable(true);
                radioButton.setText(Html.fromHtml(message));
                radioGroup.addView(radioButton);
            }
            RadioButton customRadioButton = new RadioButton(EasyMigrateActivity.this);
            customRadioButton.setText(Html.fromHtml(getResources().getString(R.string.customize)));
            customRadioButton.setPadding(dpAsPixels, (int) (1 * scale + 0.5f), dpAsPixels, (int) (1 * scale + 0.5f));
            customRadioButton.setId(customRadioButtonId);
            customRadioButton.setClickable(true);
            //radioGroup.addView(customRadioButton); // Commented as Customized date option is removed as of now.
            RadioButton selectedRadioButton = (RadioButton) radioGroup.findViewById(customSMSSelectionButtonId);
            selectedRadioButton.setChecked(true);
            if (customRadioButtonId == customSMSSelectionButtonId) {
                long milliSeconds = CommonUtil.getInstance().getMessageSelectionFrom();
                String dateFromMillis = EMUtility.getDateFromMillis(milliSeconds);
                addDatePicker.setText(dateFromMillis);
            }
            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                public void onCheckedChanged(RadioGroup group, int checkedId) {

                    RadioButton checkedRadioButton = (RadioButton) group.findViewById(checkedId);
                    boolean isChecked = checkedRadioButton.isChecked();
                    if (isChecked) {
                        if (checkedRadioButton.getId() == customRadioButtonId) {
                            displayDateDialog(addDatePicker);
                        } else {
                            addDatePicker.getText().clear();
                        }
                    }
                }
            });


            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
            dialog.show();
        } catch (Exception e) {
            DLog.log("Exception in showing custom message setting dialog: " + e.getMessage());
        }
    }

    @SuppressLint("NewApi")
    public void startWatchingPermission(final String permission) {
        DLog.log("startWatchingPermission: " + permission);
        final AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (manager == null) {
            return;
        }
        manager.startWatchingMode(permission, context.getPackageName(), new AppOpsManager.OnOpChangedListener() {
            @Override
            public void onOpChanged(String op, String packageName) {
                DLog.log("onOpChanged : " + op + " " + packageName);
                PackageManager packageManager = context.getPackageManager();
                ApplicationInfo applicationInfo = null;
                try {
                    applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
                    int mode = -1;
                    if (applicationInfo != null) {
                        mode = manager.checkOpNoThrow(permission, applicationInfo.uid, applicationInfo.packageName);
                    }
                    DLog.log("onOpChanged : mode > " + mode);
                    if (mode == AppOpsManager.MODE_ALLOWED) {
                        manager.stopWatchingMode(this);
                        Intent intent = new Intent(context, EasyMigrateActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    DLog.log("onOpChanged exception : " + e);
                }
            }
        });
    }

    public void sendTransactionInfoTOServer() {
        if (FeatureConfig.getInstance().getProductConfig().isTransactionLoggingEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                EMUtility.createWorkerForDBUpdate();
            } else {
                startService(TransactionLogService.getServiceIntent());
            }
        }
    }


    private boolean hasFeatureAtTheEnd() {
        DLog.log("In hasFeatureAtTheEnd()");

        DLog.log("CustomerSatisfactionEnabled; " + FeatureConfig.getInstance().getProductConfig().isCustomerSatisfactionEnabled());
        if (CommonUtil.getInstance().isSource() &&
                (FeatureConfig.getInstance().getProductConfig().isDataWipeEnabled() ||
                        FeatureConfig.getInstance().getProductConfig().isAccAndPinRemovalEnabled()
                        /*||FeatureConfig.getInstance().getProductConfig().isCustomerSatisfactionEnabled()*/)) {
            DLog.log("hasFeatureAtTheEnd 1");
            return true;
        }
        if (!CommonUtil.getInstance().isSource() && FeatureConfig.getInstance().getProductConfig().isCustomerSatisfactionEnabled()) {
            DLog.log("hasFeatureAtTheEnd 2");
            return true;
        }
/*        if(!CommonUtil.getInstance().isSource() &&
            FeatureConfig.getInstance().getProductConfig().isCustomerSatisfactionEnabled()){
            DLog.log("hasFeatureAtTheEnd 2");
            return true;
        }*/
        if (!CommonUtil.getInstance().isSource() && isDefaultSMSApp()) {
            return true;
        }
        DLog.log("hasFeatureAtTheEnd 2, no feature");
        return false;
    }

    private void proceedWithTheFeature(boolean isTransferSuccess) {
        DLog.log("In proceedWithTheFeature() isTransferSuccess " + isTransferSuccess);
        PreferenceHelper.getInstance(context).putBooleanItem(Constants.PREF_FINISH_CLICKED, true);
        stopOrDisconnectFromAnyNetwork();
        forceCloseApp(false);
        UninstallBroadcastReceiver.deleteUninstallAlarm(context);
        if (!CommonUtil.getInstance().isSource() && FeatureConfig.getInstance().getProductConfig().isCustomerSatisfactionEnabled()) {
            Intent intent = new Intent(EasyMigrateActivity.this, CustomerRatingDialogActivity.class);
            intent.putExtra("dontCheckEndFeature", !isTransferSuccess);
            intent.putExtra("isDataWipeEnabled", !FeatureConfig.getInstance().getProductConfig().isDataWipeEnabled());
            //intent.putExtra("isAccAndPinRemovalEnabled", !FeatureConfig.getInstance().getProductConfig().isAccAndPinRemovalEnabled());
            startActivityForResult(intent, CUSTOMER_SATISFACTION_REQUEST_CODE);
            //startActivity(intent);
            //finish();
        } else if (isTransferSuccess && CommonUtil.getInstance().isSource() && FeatureConfig.getInstance().getProductConfig().isDataWipeEnabled()) {
//            showWipeConfirmDialog();
        } else if ((CommonUtil.getInstance().isSource()) && isTransferSuccess && FeatureConfig.getInstance().getProductConfig().isAccAndPinRemovalEnabled()) {
//            showAccLockConfirmDialog();
        } else if (isDefaultSMSApp()) {

//            launchEndScreen();

        } else {
//            if (FeatureConfig.getInstance().getProductConfig().isUninstallRequired() &&
//                    (!Constants.IS_MMDS && !BuildConfig.FLAVOR.equalsIgnoreCase(Constants.FLAVOUR_SPRINT))) {
//                UninstallBroadcastReceiver.startUninstallAlarm(EasyMigrateActivity.this);
//            }
            finish();
//            System.exit(0);
        }
    }

    public void connectToClientNetwork() {
        DLog.log("connectToClientNetwork");
        EMNetworkManagerClientUIHelper.connectToClientNetwork(new EMNetworkManagerClientUIHelper.ProgressObserver() {
            @Override
            void remoteDeviceFound(EMDeviceInfo aDeviceInfo, EMConfig.EMWiFiConnectionMode aWiFiConnectionMode) {
                //Remote Device Found
                notConnectingToNetwork();
                mRemoteDeviceManager.selectRemoteDevice(aDeviceInfo);
                mRemoteDeviceManager.connectToRemoteDevice();
                if (mRole == THIS_DEVICE_IS_TARGET) {
                    mRemoteDeviceManager.remoteToBecomeSource();
                } else {
                    mRemoteDeviceManager.remoteToBecomeTarget();
                }
                if (IS_MMDS)
                    sendResponse(outputStreamForME, Constants.WDS_CONNECTION_OK);

            }

            @Override
            void connectionError(int aError) {
                if (aError == 3) {
                    if (CLOUD_PAIRING_ENABLED) {
                        EMNetworkManagerHostUIHelper.reset();
                        sendNetworkInfo();
                    } else {
                        if (IS_MMDS) {
                            sendResponse(outputStreamForME, Constants.WDS_CONNECTION_FAILED);
                        } else {
                            displaySwitchDialog();
                        }
                    }
                } else if (aError == 4) {
                    raiseFatalError("", getString(R.string.str_wrong_selection), 1);
                } else if (aError == 11) {
                    showWifiConnectDialog(false);
                } else {
                    notConnectingToNetwork();
                    raiseFatalError(getString(R.string.ept_cant_connect_to_network_title), getString(R.string.ept_cant_connect_to_network_text));
                }
            }

            @Override
            void progressUpdate(EMNetworkManagerClientUIHelper.State aState) {
                switch (aState) {
                    case CONNECTING_TO_WIFI_DIRECT_NETWORK:
                        break;
                    case TRYING_CONNECTION_TO_LAN_PEER:
                        // No need to update progress here
                        break;
                    case TRYING_CONNECTION_TO_WIFI_DIRECT_PEER:
                        // No need to update progress here
                        break;
                    case CONNECTED_TO_WIFI_DIRECT_NETWORK:
                        DLog.log("CONNECTED_TO_WIFI_DIRECT_NETWORK :");
                        if (IS_MMDS) {
                            sendResponse(outputStreamForME, Constants.WDS_CONNECTION_OK);
                            CommonUtil.getInstance().setMigrationStatus(Constants.MIGRATION_INPROGRESS);
                        }
                        CommonUtil.getInstance().setAutoPairingStatus(false);
                        sendBroadcast(new Intent(BuildConfig.APPLICATION_ID + ".autopair.connected"));
                        break;
                }
            }
        }, this);

    }

    private void createHostNetwork() {
        DLog.log("<--createHostNetwork");
        EMNetworkManagerHostUIHelper.createHostNetwork(mRemoteDeviceManager, null, null, null, this, new EMNetworkManagerHostUIHelper.ProgressObserver() {

            @Override
            void progressUpdate(EMNetworkManagerHostUIHelper.State aState) {
                switch (aState) {
                    case CREATING_WIFI_DIRECT_NETWORK:
                        //creatingNetwork();
                        break;
                    case WAITING_FOR_CONNECTION:
                        notCreatingNetwork();
                        if (CLOUD_PAIRING_ENABLED) {
                            displayWifiSuggestionScreen();
                            if (!hotSpotInfoposted)
                                stateMachine.postData(StateMachine.WIFI_INFO);
                        } else {
                            if (IS_MMDS) {
                                CommonUtil commonUtil = CommonUtil.getInstance();
                                sendResponse(outputStreamForME, Constants.COMMAND_NETWORK_DETAILS + ":" + commonUtil.getmWiFiPeerAddress() + ":" + commonUtil.getmWiFiPeerSSID() + ":" + commonUtil.getmWiFiPeerPassphrase() + ":" + commonUtil.getmCryptoEncryptPass() + ":" + commonUtil.getmFrequency() + ":" + commonUtil.getNetworkType());
                            } else {
                                int frequency = CommonUtil.getInstance().getmFrequency();
                                String ssid = CommonUtil.getInstance().getmWiFiPeerSSID();
//                                if ((ssid == null || ssid.isEmpty() && btn_switch_scan.getVisibility() != View.GONE)) {
//                                    btn_switch_scan.setVisibility(View.VISIBLE);
//                                } else if ((frequency > 3000 && btn_switch_scan.getVisibility() != View.GONE)) {
//                                    btn_switch_scan.setVisibility(View.VISIBLE);
//                                }
                            }
                        }

                        break;
                }
            }
        });
        DLog.log("createHostNetwork-->");
    }

    private void installApps(String appPathName) {
        DLog.log("Enter installApps appPathName : " + appPathName);
        Uri apkUri = FileProvider.getUriForFile(EasyMigrateActivity.this, BuildConfig.APPLICATION_ID + ".fileprovider", new File(appPathName));

        Intent intent = new Intent(Intent.ACTION_VIEW, apkUri);
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //dont forget add this line
        startActivity(intent);
    }

    private int downloadedFileCount = 0;


    private void downloadFile(final String s3APKName_) {
        DLog.log("Enter downloadFile s3APKName_ : " + s3APKName_);
        String countryName = "";

        if (GET_PRELOADED_APPS_NAMES_FROM_SERVER) {

            countryName = COUNTRY_NAME + "/";
        }

        final String s3APKName = countryName + s3APKName_;


        try {
            File storageDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            File localFile = File.createTempFile(s3APKName_,  // prefix
                    ".apk",         // suffix
                    storageDir      // directory
            );
            // Save a file: path for use with ACTION_VIEW intents
            mFullPath = localFile.getAbsolutePath();

            File file = new File(mFullPath);
            if (file.exists()) {
//                        Log.i(TAG, "downloadFile file.exists() : " + file.exists());
                DLog.log("downloadFile file.exists() : " + file.exists());
            }
            TransferUtility transferUtility =
                    TransferUtility.builder()
                            .context(getApplicationContext())
                            .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                            .s3Client(s3Client)
                            .build();
//                    TransferObserver downloadObserver =transferUtility.download(s3APKName, localFile);
            DLog.log("Enter downloadFile s3APKName " + s3APKName_);

            TransferObserver downloadObserver = transferUtility.download(s3APKName, localFile);


            downloadObserver.setTransferListener(new TransferListener() {

                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (TransferState.COMPLETED == state) {
//                                Log.i(TAG, "downloadFile Download Completed mFullPath : " + mFullPath);
                        DLog.log("downloadFile Download Completed mFullPath : " + mFullPath);
//                                Toast.makeText(getApplicationContext(), "Download Completed! " + downloadedFileCount, Toast.LENGTH_SHORT).show();
                        File file = new File(mFullPath);
                        int file_size = Integer.parseInt(String.valueOf(file.length() / 1024));
//                                Log.i(TAG, "downloadFile file_size of " + mFullPath + " " + file_size +" MB");
                        DLog.log("downloadFile file_size of " + mFullPath + " " + file_size + " KB");

                        try {
                            final PackageManager pm = getPackageManager();
                            PackageInfo info = pm.getPackageArchiveInfo(mFullPath, 0);
//                                    Log.i(TAG, "info.versionName " + info.versionName);
                            DLog.log("app versionName " + info.versionName);
                            info.applicationInfo.publicSourceDir = mFullPath;
                            File appFile = new File(info.applicationInfo.publicSourceDir);
                            //info.applicationInfo.publicSourceDir = mFullPath;
                            long appMemory = appFile.length();
//                                    Log.i(TAG, "appMemory : " + appMemory);
                            DLog.log("appMemory : " + appMemory);
                            Drawable appIcon = info.applicationInfo.loadIcon(pm);
                            String appName = info.applicationInfo.loadLabel(pm).toString();
                            //Log.i(TAG, "appName : " + appName);
                            DLog.log("appName : " + appName);
                            String packageName = info.packageName;
                            DLog.log("packageName : " + packageName);
                            String versionName = info.versionName;
                            DLog.log("versionName : " + versionName);
                            int versionCode = info.versionCode;
                            DLog.log("versionCode : " + versionCode);

                            AppBackupModel app = new AppBackupModel();
                            app.setAppName(appName);
                            app.setPackageName(packageName);
                            app.setVersionName(versionName);
                            app.setVersionCode(versionCode);
                            app.setAppIcon(appIcon);
                            app.setFile(appFile);
                            app.setChecked(true);
                            AppMigrateUtils.addAppBackupList(app);
                            downloadedFileCount = downloadedFileCount + 1;
                            Toast.makeText(getApplicationContext(), "Download Completed! " + downloadedFileCount, Toast.LENGTH_SHORT).show();
                            retried_count = 0;
                            DLog.log("downloadFile movistarappsListS3.size() " + movistarappsListS3.size());
                            DLog.log("downloadFile downloadedFileCount " + downloadedFileCount);
                            if (downloadedFileCount < movistarappsListS3.size()) {
                                DLog.log("downloadFile downloadedFileCount " + downloadedFileCount);
                                DLog.log("downloadFile Calling case 2");
//                                        downloadFile(movistarappsListS3.get(downloadedFileCount));
                                String fnameS3 = movistarappsListS3.get(downloadedFileCount);
                                DLog.log("downloadFile Calling case 2 fnameS3 : " + fnameS3);
                                downloadFile(fnameS3);
                            } else {
                                stateMachine.createSession();
                                DLog.log("downloadFile download all files done , create session called");
                            }
                        } catch (Exception e) {
                            DLog.log("downloadFile download  exception : " + e);
//                            final PackageManager pm = getPackageManager();
//                            PackageInfo info = pm.getPackageArchiveInfo(mFullPath, 0);
//                            Log.i(TAG, "info.versionName " + info.versionName);
                        }
                        ;
                    }

                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                    float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                    int percentDone = (int) percentDonef;
                    DLog.log("downloadFile download onProgressChanged case percentDone " + percentDone + " : s3APKName " + s3APKName);
                }

                @Override
                public void onError(int id, Exception ex) {
                    ex.printStackTrace();
                    Log.i(TAG, "downloadFile onError Case : " + id);
                    retried_count = retried_count + 1;
                    Log.i(TAG, "downloadFile onError case retried_count : " + retried_count);
                    if (retried_count <= APK_DOWNLOAD_RETRY_COUNT) {
                        Log.i(TAG, "downloadFile onError case : " + id);
                        String fnameArr[] = s3APKName.split("/");
                        if (fnameArr.length > 1) {
                            downloadFile(fnameArr[1]);
                        } else {
                            downloadFile(s3APKName);
                        }
                    } else {
                        Toast.makeText(EasyMigrateActivity.this, "Download Files TimeOut", Toast.LENGTH_SHORT).show();
                        stateMachine.createSession();
                        Log.i(TAG, "download all files done , create session called Timeout case");
                    }
                }

            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void launchInstallUnknownAppsSettings() {
        DLog.log("enter launchInstallUnknownAppsSettings getPackageName() " + getPackageName() + " , AppMigrateUtils.restoreAppList.size() " + AppMigrateUtils.restoreAppList.size());
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, INSTALL_UNKNOWN_APPS_REQ_CODE);
    }

    private String getGoogleAccountRemovalStatus() {
        String result = null;
        boolean googleAccountRemoved = false;
        TestGoogleAccounts testGoogleAccounts;
        testGoogleAccounts = new TestGoogleAccounts();
        TestResult testResult = testGoogleAccounts.checkGoogleAccountStatus();
        DLog.log("checkGoogleAccountStatus testResult " + testResult.getResultCode());
        if (TestResult.RESULT_PASS == testResult.getResultCode()) {
            DLog.log("checkGoogleAccountStatus testResult : PASS ");
            googleAccountRemoved = true;
        } else {
            DLog.log("checkGoogleAccountStatus testResult :Not PASS ");
            googleAccountRemoved = false;
        }
        if (googleAccountRemoved)
            result = googleAccountRemoved ? "GoogleAccountRemoved=Yes" : "GoogleAccountRemoved=No";
        DLog.log("accountRemovalStatus data " + result);
        return result;
    }

    private void launchEndScreen() {
        Intent intent = new Intent(EasyMigrateActivity.this, ThankYouActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void launchAppsInstallScreen() {
        Intent intent = new Intent(this, AppsInstallTabActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onInstallAppsSelectedSummary() {
        DLog.log(TAG + " enter onInstallAppsSelectedSummary Build.VERSION.SDK_INT " + Build.VERSION.SDK_INT + " Build.VERSION_CODES.Q " + Build.VERSION_CODES.Q);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DLog.log(TAG + " enter onInstallAppsSelectedSummary");
                if (getPackageManager().canRequestPackageInstalls()) {
                    // higher or equal to android O
                    launchAppsInstallScreen();
                } else {
                    launchInstallUnknownAppsSettings();
                }
            } else {
                // lower than android O
//                launchInstallUnknownAppsSettings();
                // in os 8, launchInstallUnknownAppsSettings working
                // below os 0, launchInstallUnknownAppsSettings not working
                launchAppsInstallScreen();
            }
        } else {
            // android11 case
            launchAppsInstallScreen();
        }
    }

    private void requestAllFilesAccessPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
        startActivityForResult(intent, ALL_FILES_ACCESS_PERMISSION);
    }

    public void displayAllFilesAccessPermissionsDialog(String title, String message) {
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(EasyMigrateActivity.this);
            alert.setTitle(title);
            alert.setMessage(message);
            String positiveString = getString(R.string.ept_ok);
            alert.setPositiveButton(positiveString, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    requestAllFilesAccessPermission();
                }
            });
            alert.setNegativeButton(getString(R.string.ept_exit_button), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
//                    System.exit(0);
                    finish();
                }
            });
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        } catch (Exception e) {
            DLog.log(e.getMessage());
        }
    }

}
