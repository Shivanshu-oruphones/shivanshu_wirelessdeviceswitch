package com.pervacio.wds.custom.externalstorage;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMDataCommandDelegate;
import com.pervacio.wds.app.EMDataType;
import com.pervacio.wds.app.EMMigrateStatus;
import com.pervacio.wds.app.EMProgressInfo;
import com.pervacio.wds.app.EMStringConsts;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.EMUtilsDefaultSmsApp;
import com.pervacio.wds.custom.appmigration.AppMigrateUtils;
import com.pervacio.wds.custom.models.ContentDetails;
import com.pervacio.wds.custom.service.TransactionLogService;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.ContentDetailsAdapter;
import com.pervacio.wds.custom.utils.ContentProgressDetailsAdapter;
import com.pervacio.wds.custom.utils.ContentSelectionInterface;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.DeviceInfo;
import com.pervacio.wds.custom.utils.EstimationTimeUtility;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import static com.pervacio.wds.app.EMStringConsts.OPERATION_TYPE_BACKUP;
import static com.pervacio.wds.app.EMStringConsts.OPERATION_TYPE_RESTORE;
import static com.pervacio.wds.custom.utils.Constants.COMPANY_NAME;
import static com.pervacio.wds.custom.utils.Constants.ESTIMATION_TIME_REQUIRED;
import static com.pervacio.wds.custom.utils.Constants.SUPPORTED_DATATYPE_MAP;

/*Created by Surya Polasanapalli*/

public class BackupRestoreActivity extends AppCompatActivity implements EMDataCommandDelegate, View.OnClickListener, ContentSelectionInterface {

    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final long ELAPSED_TIME = 5 * 60 * 60 * 1000L;
    private static final long STORAGE_THRESHOLD_VALUE = 200 * 1024; //200 MB of Threshold value.
    private static ProgressDialog mProgressDialog;
    private final int UPDATE_PROGRESS = 3;
    private final int PROGRESS_INTERVAL = 2 * 1000;
    private final String USB_STATE_CHANGED_ACTION = "android.hardware.usb.action.USB_STATE";
    public long mBackupStartTime = 0;
    public long mRestoreEndTime = 0;
    public long mTransferTotalTime = 0;
    ArrayList<EPTWizardPage> mBackStack = new ArrayList<>();
    PowerManager.WakeLock mWakeLock = null;
    PowerManager mPowerManager = null;
    WifiManager.WifiLock mWiFiLock = null;
    private int mCalendarsCount, mAppCount, mContactsCount, mPhotosCount, mVideosCount, mSettingsCount, mMessageCount, mAudioCount, mCallLogsCount;
    private int mRole = OPERATION_TYPE_BACKUP;
    private int STORAGE_DEVICE_TYPE = 0;
    private long elapsedTimeInsec = 0;
    private long mAudioSize = 0;
    private long mVideoSize = 0;
    private long mImageSize = 0;
    private long mAppSize = 0;
    private long mSelectedContentSize = 0;
    private TextView selectContentsuggestion, selectContentTimeSuggestion;
    private ProgressBar storageProgressbar;
    private TextView storageMessage, elapsedTime, estiamtionTime;
    private TextView mTotalTransferTime;
    private TextView mUniqueTransactionId;
    private TextView storageSuggestion;
    private Button btn_start_migration;
    private TextView btn_summary_finish, btn_cancel;
    private RecyclerView contentDetailsView;
    private RecyclerView contentDetailsSummaryView;
    private RecyclerView contentDetailsProgressView;
    private Animation mInFromLeft;
    private Animation mInFromRight;
    private Animation mOutToLeft;
    private Animation mOutToRight;
    private Animation mFadeOut;
    private Animation mFadeIn;
    private ViewFlipper mFlipper;
    private EPTWizardPage mCurrentPage;
    private ContentDetailsAdapter contentDetailsAdapter;
    private ContentProgressDetailsAdapter contentProgressDetailsAdapter;
    private LinkedHashMap<Integer, ContentDetails> contentDetailsMap = new LinkedHashMap<>();
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_PROGRESS:
                    updateProgress(EMDataType.EM_DATA_TYPE_PHOTOS, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_PHOTOS), 0);
                    updateProgress(EMDataType.EM_DATA_TYPE_VIDEO, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_VIDEO), 0);
                    updateProgress(EMDataType.EM_DATA_TYPE_MUSIC, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_MUSIC), 0);
                    updateProgress(EMDataType.EM_DATA_TYPE_APP, EMMigrateStatus.getItemsTransferred(EMDataType.EM_DATA_TYPE_APP), 0);
                    handler.sendEmptyMessageDelayed(UPDATE_PROGRESS, PROGRESS_INTERVAL);
                    break;
            }
        }
    };
    private CountDownTimer elapsedTimer = new CountDownTimer(ELAPSED_TIME, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            ++elapsedTimeInsec;
            String sourceString = getResources().getString(R.string.elapsed_time) + "<b>" + " " + EMUtility.getReadableTime(BackupRestoreActivity.this,elapsedTimeInsec * 1000, false) + "</b>";
            elapsedTime.setText(Html.fromHtml(sourceString));
        }

        @Override
        public void onFinish() {
            this.start();
        }
    };
    private Dialog cancelDialog;
    private UsbManager usbManager;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            try {
                DLog.log("Received Broadcast: " + action);
                if (UsbManager.ACTION_USB_DEVICE_DETACHED.equalsIgnoreCase(action)) {
                    CommonUtil.getInstance().pauseMigration();
                    usbDisconnectedDialog();
                } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equalsIgnoreCase(action)) {
                    UsbDevice usbDevice = getDevice();
                    if (usbDevice == null) {
                        usbDisconnectedDialog();
                    } else if (usbManager.hasPermission(getDevice())) {
                        CommonUtil.getInstance().resumeMigration();
                    } else {
                        if (cancelDialog != null) {
                            cancelDialog.dismiss();
                        }
                        PendingIntent permissionIntent = PendingIntent.getBroadcast(BackupRestoreActivity.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                        usbManager.requestPermission(getDevice(), permissionIntent);
                    }
                } else if (USB_STATE_CHANGED_ACTION.equalsIgnoreCase(action)) {
                    if (intent.getExtras().getBoolean("connected")) {
                        CommonUtil.getInstance().resumeMigration();
                    } else {
                        CommonUtil.getInstance().pauseMigration();
                    }
                } else if (ACTION_USB_PERMISSION.equalsIgnoreCase(action)) {
                    synchronized (this) {
                        UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                DLog.log("Name: " + device.getDeviceName());
                                CommonUtil.getInstance().setUsbDevice(getUsbDevice());
                                CommonUtil.getInstance().setUsbBackupPath(CommonUtil.getInstance().getUsbDevice().getPartitions().get(0).getFileSystem().getRootDirectory() + "/" + Constants.EXTERNAL_STORAGE_BACKUP_FOLDER);
                                CommonUtil.getInstance().resumeMigration();
                            }
                        } else {
                            DLog.log("permission denied for device " + device);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private Thread metadataThread = new Thread() {
        @Override
        public void run() {
            super.run();
            if (mRole == OPERATION_TYPE_BACKUP) {
                DLog.log("Starting metadata Queue");
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).isSelected())
                    DeviceInfo.getInstance().prepareFilesQueue(EMDataType.EM_DATA_TYPE_VIDEO);
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).isSelected())
                    DeviceInfo.getInstance().prepareFilesQueue(EMDataType.EM_DATA_TYPE_PHOTOS);
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).isSelected())
                    DeviceInfo.getInstance().prepareFilesQueue(EMDataType.EM_DATA_TYPE_MUSIC);

                try {
                    Collections.shuffle((List<?>) DeviceInfo.getInstance().getFilesQueue());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                DLog.log("Ending metadata Queue " + DeviceInfo.getInstance().getFilesQueue().size());
            } else {
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).isSelected())
                    RestoreUtility.getInstance(STORAGE_DEVICE_TYPE).addToFilesList(EMDataType.EM_DATA_TYPE_VIDEO);
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).isSelected())
                    RestoreUtility.getInstance(STORAGE_DEVICE_TYPE).addToFilesList(EMDataType.EM_DATA_TYPE_PHOTOS);
                if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).isSelected())
                    RestoreUtility.getInstance(STORAGE_DEVICE_TYPE).addToFilesList(EMDataType.EM_DATA_TYPE_MUSIC);

                try {
                    Collections.shuffle((List<?>) RestoreUtility.getInstance(STORAGE_DEVICE_TYPE).getFileList());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                DLog.log("Ending metadata Queue " + RestoreUtility.getInstance(STORAGE_DEVICE_TYPE).getFileList().size());
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if ((!DeviceInfo.getInstance().getFilesQueue().isEmpty() || !RestoreUtility.getInstance(STORAGE_DEVICE_TYPE).getFileList().isEmpty())) {
                        handler.sendEmptyMessageDelayed(UPDATE_PROGRESS, 1000L);
                    }
                    elapsedTimer.start();
                    hideProgressDialog();
                    mBackupStartTime = System.currentTimeMillis();
//                    BackupRestoreSession backupRestoreSession = new BackupRestoreSession(BackupRestoreActivity.this);
//                    backupRestoreSession.backupOrRestoreData(getSelectedDataTypes(), mRole, STORAGE_DEVICE_TYPE);
                    setLayout(EPTWizardPage.Progress, EPTTransitionReason.UserNext);
                }
            });
        }
    };
    private boolean isMigrationInprogress = false;

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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.start_migration) {
            startBackupOrRestore();
        } else if (id == R.id.summary_finish_button) {
            forceCloseApp();
        } else if (id == R.id.btn_cancel) {
            optionToCloseApp();
        }
    }

    void usbDisconnectedDialog() {
        DLog.log("In usbDisconnectedDialog");
        if (cancelDialog != null) {
            cancelDialog.dismiss();
        }
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_NEGATIVE:
                        forceCloseApp();
                        break;

                    case DialogInterface.BUTTON_POSITIVE:
                        DLog.log("Pressed positive button");
                        UsbDevice usbDevice = getDevice();
                        if (usbDevice == null) {
                            usbDisconnectedDialog();
                        } else if (usbManager.hasPermission(getDevice())) {
                            CommonUtil.getInstance().resumeMigration();
                        } else {
                            PendingIntent permissionIntent = PendingIntent.getBroadcast(BackupRestoreActivity.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                            usbManager.requestPermission(getDevice(), permissionIntent);
                        }
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(getResources().getString(R.string.wds_alert));
        builder.setMessage(R.string.usb_reconnect_msg).setPositiveButton(R.string.str_yes, dialogClickListener)
                .setNegativeButton(R.string.str_no, dialogClickListener);

        cancelDialog = builder.create();
        cancelDialog.show();
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
                        forceCloseApp();
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

    @Override
    public void updateContentSelection(int datatype, boolean isSelected) {
        DLog.log("updateContentSelection " + datatype + " " + isSelected);
        updateSelectedDataTypes(datatype, isSelected);
    }

    @Override
    public void selectedDataTypeInfo(int dataType) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_restore);
        TextView appVersion = (TextView) findViewById(R.id.version);
        appVersion.setText(BuildConfig.VERSION_NAME);
        disableKeyguard();
        disableKeyguard();
        setWakeLock(true);
        setScreenOn(true);

        mRole = getIntent().getIntExtra(EMStringConsts.OPERATION_TYPE, OPERATION_TYPE_BACKUP);
        STORAGE_DEVICE_TYPE = getIntent().getIntExtra(EMStringConsts.TRANSFER_TYPE, EMStringConsts.EXTERNAL_STORAGE_SDCARD);
        DLog.log("Operationtype : " + mRole + " , storage type : " + STORAGE_DEVICE_TYPE);
        CommonUtil.getInstance().setExternalStorageType(STORAGE_DEVICE_TYPE);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mFlipper = (ViewFlipper) findViewById(R.id.MainViewFlipper);
        mTotalTransferTime = (TextView) this.findViewById(R.id.timeTakenToTransfer);
        mUniqueTransactionId = (TextView) this.findViewById(R.id.uniqueTransactionId);
        selectContentsuggestion = (TextView) this.findViewById(R.id.selectContentsuggestion);
        selectContentTimeSuggestion = (TextView) this.findViewById(R.id.selectContentTimesuggestion);
        btn_start_migration = (Button) findViewById(R.id.start_migration);
        btn_cancel = (TextView) findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(this);
        btn_summary_finish = (TextView) findViewById(R.id.summary_finish_button);
        btn_summary_finish.setOnClickListener(this);
        storageProgressbar = (ProgressBar) findViewById(R.id.storagespaceIndicator);
        storageSuggestion = (TextView) findViewById(R.id.storageSuggestion);
        storageMessage = (TextView) findViewById(R.id.storageSpace);
        elapsedTime = (TextView) findViewById(R.id.elapsedTime);
        elapsedTime.setVisibility(View.VISIBLE);
        estiamtionTime = (TextView) findViewById(R.id.estimationTime);
        setLayout(EPTWizardPage.selectoldornewdevice,EPTTransitionReason.UserNext);
        if(STORAGE_DEVICE_TYPE == EMStringConsts.EXTERNAL_STORAGE_USB) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            filter.addAction(ACTION_USB_PERMISSION);
            //filter.addAction(USB_STATE_CHANGED_ACTION);
            registerReceiver(mUsbReceiver, filter);
        }
    }

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
    public void progressUpdate(EMProgressInfo aProgressInfo) {
        if (mCurrentPage == EPTWizardPage.SelectContent) {
            setLayout(EPTWizardPage.Progress, EPTTransitionReason.Automatic);
        }

        if ((aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_QUIT_COMMAND_SENT)
                || (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_QUIT_COMMAND_RECEIVED)) {
            //Need to release wakelock and wifi lock resources after migration
            setScreenOn(false);
            setWakeLock(false);
            elapsedTimer.cancel();
            hideProgressDialog();
            DLog.log("Transaction is complete.");
            DashboardLog.getInstance().geteDeviceSwitchSession().setCancellationReason(Constants.CANCEL_REASON.NO_ERROR.value());
            /*** Once the transaction is complete, let's log it to Dashboard ***/
            mRestoreEndTime = System.currentTimeMillis();
            mTransferTotalTime = ((mRestoreEndTime - mBackupStartTime));
            String mTransfertime = EMUtility.getReadableTime(this,mTransferTotalTime, false);
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
            setLayout(EPTWizardPage.Complete, EPTTransitionReason.UserNext);
            String sessionID = DashboardLog.getInstance().geteDeviceSwitchSession().getDeviceSwitchSessionId();
            DLog.log("WDS product. Starting TransactionLogService");
            DashboardLog.getInstance().updateToServer(true);
            unregisterReceiver();
            startService(TransactionLogService.getServiceIntent());
            CommonUtil.getInstance().setMigrationStatus(Constants.MIGRATION_SUCCEEDED);
        } else if ((aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_OUTGOING_DATA)
                || (aProgressInfo.mOperationType == EMProgressInfo.EMOperationType.EM_OPERATION_PROCESSING_INCOMING_DATA)) {
            updateProgress(aProgressInfo.mDataType, aProgressInfo.mCurrentItemNumber, aProgressInfo.mTotalItems);
        }
    }

    private void unregisterReceiver(){
        try {
            unregisterReceiver(mUsbReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSelectedDataTypes(int dataType, boolean isSelected) {
        contentDetailsMap.get(dataType).setSelected(isSelected);
        updateStorageProgress();
        if (mRole == OPERATION_TYPE_RESTORE && dataType == EMDataType.EM_DATA_TYPE_SMS_MESSAGES && isSelected) {
            EMUtilsDefaultSmsApp emUtilsDefaultSmsApp = new EMUtilsDefaultSmsApp(this, BackupRestoreActivity.this);
            emUtilsDefaultSmsApp.becomeDefaultSmsApp();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EMUtilsDefaultSmsApp.REQUEST_CODE_SMSAPP_DEFAULT) {
            contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).setSelected(EMUtility.isDefaultSMSApp());
            contentDetailsAdapter.notifyDataSetChanged();
        }
    }

    private long estimateTransferTime(boolean reEstimatation) {
        if (ESTIMATION_TIME_REQUIRED) {
            Constants.ESTIMATION_LOWERLIMIT = 1.0f;
            Constants.ESTIMATION_UPPERLIMIT = 2.0f;
            long estimatedTimeForMedia = 0;
            long estimationTimeForPim = EstimationTimeUtility.getInstance().getEstimationForPIM(getSelectedDataTypes());
            estimationTimeForPim = estimationTimeForPim / 2;    //bcz either backup or restore only one per session.
            long totalEstimation = 0;
            boolean updateUIwithEstimation = true;
            try {
                long transferSpeed = 10 * 1024 * 1024L;// CommonUtil.getInstance().getTransferSpeed();
                long totalSizetoBeTransferred = mSelectedContentSize;
                if (totalSizetoBeTransferred != 0 && transferSpeed == 0) {
                    return 0;
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

                String minEstimationTime = EMUtility.getReadableTime(this,lowerEstimation, true);
                String maxEstimationTime = EMUtility.getReadableTime(this,upperEstimation, true);
                DLog.log("Estimation time " + totalEstimation);
                if (reEstimatation) {
                    updateUIwithEstimation = EMUtility.checkVariation(CommonUtil.getInstance().getInitialEstimation(), totalEstimation, 10);
                }

                String estimatedTime = getResources().getString(R.string.estiamated_transfer_time) + "<b>" + " " + minEstimationTime + "</b>";
                String estimation = getResources().getString(R.string.estimation) + "<b>" + " " + minEstimationTime + "</b>";
                if (!maxEstimationTime.isEmpty()) {
                    estimatedTime = getResources().getString(R.string.estiamated_transfer_time) + "<b>" + " " + minEstimationTime + "</b>" + " to " + "<b>" + " " + maxEstimationTime + "</b>";
                    estimation = getResources().getString(R.string.estimation) + "<b>" + " " + minEstimationTime + "</b>" + " to " + "<b>" + " " + maxEstimationTime + "</b>";
                }
                if (minEstimationTime.isEmpty()) {
                    selectContentsuggestion.setText("");
                    selectContentTimeSuggestion.setText("");
                    estiamtionTime.setText("");
                } else if (updateUIwithEstimation) {
                    estiamtionTime.setVisibility(View.VISIBLE);
                    selectContentsuggestion.setText(Html.fromHtml(estimatedTime));
                    selectContentsuggestion.setTextColor(getResources().getColor(R.color.black));
                    selectContentTimeSuggestion.setText(getResources().getString(R.string.estimated_transfer_time_suggestion));
                    estiamtionTime.setText(Html.fromHtml(estimation));
                }
            } catch (Exception e) {
                DLog.log(e.getMessage());
            }
            return totalEstimation;
        }
        return 0;
    }

    private void updateStorageProgress() {
        int selectedDataTypes = getSelectedDataTypes();
        long estimationTime = estimateTransferTime(false);
        CommonUtil.getInstance().setInitialEstimation(estimationTime);
        long destFreeStorage = (DashboardLog.getInstance().geteDeviceSwitchSession().getDestinationDeviceInfoId().getFreeStorage()) * 1024;
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
    }

    private int getSelectedDataTypes() {
        int dataTypesTemp = 0;
        mSelectedContentSize = 0;

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CONTACTS).isSelected()) {
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_CONTACTS;
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).isSelected())
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_SMS_MESSAGES;

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALENDAR).isSelected())
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_CALENDAR;

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_CALL_LOGS).isSelected())
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_CALL_LOGS;

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SETTINGS).isSelected())
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_SETTINGS;

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_PHOTOS).isSelected()) {
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_PHOTOS;
            mSelectedContentSize += mImageSize;
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_VIDEO).isSelected()) {
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_VIDEO;
            mSelectedContentSize += mVideoSize;
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_MUSIC).isSelected()) {
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_MUSIC;
            mSelectedContentSize += mAudioSize;
        }

        if (contentDetailsMap.get(EMDataType.EM_DATA_TYPE_APP).isSelected()) {
            dataTypesTemp |= EMDataType.EM_DATA_TYPE_APP;
            mSelectedContentSize += mAppSize;
        }
        return dataTypesTemp;
    }

    private void startBackupOrRestore() {
        showProgressDialog("", getString(R.string.processing));
        metadataThread.start();
    }

    private void loadContentDetailsMap() {
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_CONTACTS, (getContentDetails(EMDataType.EM_DATA_TYPE_CONTACTS, getString(R.string.ept_select_content_contacts), -1, mContactsCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_CONTACTS), mContactsCount > 0, new int[]{R.drawable.ic_contact, R.drawable.ic_contact_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_CALENDAR, (getContentDetails(EMDataType.EM_DATA_TYPE_CALENDAR, getString(R.string.ept_select_content_calendar), -1, mCalendarsCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_CALENDAR), mCalendarsCount > 0, new int[]{R.drawable.ic_calendar, R.drawable.ic_calendar_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_CALL_LOGS, (getContentDetails(EMDataType.EM_DATA_TYPE_CALL_LOGS, getString(R.string.ept_select_content_call_logs), -1, mCallLogsCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_CALL_LOGS), mCallLogsCount > 0, new int[]{R.drawable.calllog, R.drawable.callog_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, (getContentDetails(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, getString(R.string.ept_select_content_messages), -1, mMessageCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES), mMessageCount > 0, new int[]{R.drawable.ic_message, R.drawable.ic_message_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_PHOTOS, (getContentDetails(EMDataType.EM_DATA_TYPE_PHOTOS, getString(R.string.ept_select_content_photos), -1, mPhotosCount, mImageSize, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_PHOTOS), mPhotosCount > 0, new int[]{R.drawable.ic_photo, R.drawable.ic_photo_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_VIDEO, (getContentDetails(EMDataType.EM_DATA_TYPE_VIDEO, getString(R.string.ept_select_content_videos), -1, mVideosCount, mVideoSize, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_VIDEO), mVideosCount > 0, new int[]{R.drawable.ic_video, R.drawable.ic_video_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_MUSIC, (getContentDetails(EMDataType.EM_DATA_TYPE_MUSIC, getString(R.string.ept_select_content_audio), -1, mAudioCount, mAudioSize, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_MUSIC), mAudioCount > 0, new int[]{R.drawable.ic_music, R.drawable.ic_music_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_SETTINGS, (getContentDetails(EMDataType.EM_DATA_TYPE_SETTINGS, getString(R.string.ept_select_content_settings), -1, mSettingsCount, -1, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_SETTINGS), mSettingsCount > 0, new int[]{R.drawable.setting, R.drawable.setting_disabled})));
        contentDetailsMap.put(EMDataType.EM_DATA_TYPE_APP, (getContentDetails(EMDataType.EM_DATA_TYPE_APP, getString(R.string.ept_select_content_app), -1, mAppCount, mAppSize, SUPPORTED_DATATYPE_MAP.get(EMDataType.EM_DATA_TYPE_APP), mAppCount > 0, new int[]{R.drawable.app, R.drawable.app_disabled})));
    }

    private ContentDetails getContentDetails(int dataType, String contentName, int progressCount, int totalCount, long totalSize, boolean supported, boolean selected, int[] drawableId) {
        ContentDetails contentDetails = new ContentDetails();
        contentDetails.setContentType(dataType);
        contentDetails.setProgressCount(progressCount);
        contentDetails.setTotalCount(totalCount);
        contentDetails.setTotalSizeOfEntries(totalSize);
        contentDetails.setPermissionGranted(true);
        contentDetails.setSupported(supported);
        contentDetails.setSelected(supported && selected);
        contentDetails.setContentName(contentName);
        contentDetails.setImageDrawableId(drawableId);
        return contentDetails;
    }

    private void loadContentDetails() {
        showProgressDialog(getString(R.string.wds_please_wait), getString(R.string.estimating_pleasewait));
        new Thread() {
            @Override
            public void run() {
                contentDetailsMap.clear();
                mCalendarsCount = mAppCount = mContactsCount = mPhotosCount = mVideosCount = mSettingsCount = mMessageCount = mAudioCount = mCallLogsCount = 0;
                EMMigrateStatus.addContentDetails(EMDataType.EM_DATA_TYPE_SETTINGS, EMStringConsts.SETTINGS_LIST.size());
                if (mRole == OPERATION_TYPE_BACKUP) {
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

                    mCallLogsCount = (int) deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_CALL_LOGS, true);
                    DLog.log("Call Logs Count : " + mCallLogsCount);

                    mSettingsCount = EMStringConsts.SETTINGS_LIST.size();

                    mImageSize = deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_PHOTOS, false);
                    DLog.log("Photos Size : " + EMUtility.readableFileSize(mImageSize));

                    mVideoSize = deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_VIDEO, false);
                    DLog.log("Videos Size : " + EMUtility.readableFileSize(mVideoSize));

                    mAudioSize = deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_MUSIC, false);
                    DLog.log("Audio Size : " + EMUtility.readableFileSize(mAudioSize));

                    AppMigrateUtils.procureAppsDetails();
                    mAppCount = AppMigrateUtils.totalAppCount;
                    mAppSize = AppMigrateUtils.totalAppSize;
                    DLog.log("Apps Count : " + mAppCount);
                    DLog.log("Apps Size : " + mAppSize);
                    loadContentDetailsMap();

                    //Changed as few sony models, getting blocked while querying sms content provider intermittent #51703
                    mMessageCount = (int) deviceInfo.getContentDetails(EMDataType.EM_DATA_TYPE_SMS_MESSAGES, true);
                    DLog.log("Messages Count : " + mMessageCount);
                } else {
                    RestoreUtility restoreUtility = RestoreUtility.getInstance(STORAGE_DEVICE_TYPE);
                    mContactsCount = restoreUtility.getCount(EMStringConsts.DATA_TYPE_CONTACTS);
                    DLog.log("Contacts Count : " + mContactsCount);

                    mCalendarsCount = restoreUtility.getCount(EMStringConsts.DATA_TYPE_CALENDAR);
                    DLog.log("Calendars Count : " + mCalendarsCount);

                    mCallLogsCount = restoreUtility.getCount(EMStringConsts.DATA_TYPE_CALL_LOGS);
                    DLog.log("Call Logs Count : " + mCallLogsCount);

                    mSettingsCount = restoreUtility.getCount(EMStringConsts.EM_XML_SETTINGS);
                    DLog.log("Settings Count : " + mSettingsCount);

                    mMessageCount = restoreUtility.getCount(EMStringConsts.DATA_TYPE_SMS_MESSAGES);
                    DLog.log("Messages Count : " + mMessageCount);

                    restoreUtility.prepareList();

                    mPhotosCount = restoreUtility.getCount(EMDataType.EM_DATA_TYPE_PHOTOS);
                    DLog.log("Photos Count : " + mPhotosCount);

                    mVideosCount = restoreUtility.getCount(EMDataType.EM_DATA_TYPE_VIDEO);
                    DLog.log("Videos Count : " + mVideosCount);

                    mAudioCount = restoreUtility.getCount(EMDataType.EM_DATA_TYPE_MUSIC);
                    DLog.log("Audio Count : " + mAudioCount);

                    mImageSize = restoreUtility.getSize(EMDataType.EM_DATA_TYPE_PHOTOS);
                    DLog.log("Photos Size : " + EMUtility.readableFileSize(mImageSize));

                    mVideoSize = restoreUtility.getSize(EMDataType.EM_DATA_TYPE_VIDEO);
                    DLog.log("Videos Size : " + EMUtility.readableFileSize(mVideoSize));

                    mAudioSize = restoreUtility.getSize(EMDataType.EM_DATA_TYPE_MUSIC);
                    DLog.log("Audio Size : " + EMUtility.readableFileSize(mAudioSize));

                }
                loadContentDetailsMap();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLayout(EPTWizardPage.SelectContent, EPTTransitionReason.UserNext);
                        hideProgressDialog();
                    }
                });

            }
        }.start();
        return;
    }

    private void changeButtonStatus(boolean setEnable) {
        if (setEnable) {
            btn_start_migration.setEnabled(true);
            btn_start_migration.setAlpha(1.0f);
        } else {
            btn_start_migration.setEnabled(false);
            btn_start_migration.setAlpha(0.5f);
        }
    }

    private void showProgressDialog(String aTitle, String aText) {
        try {
            if (mProgressDialog != null) {
                mProgressDialog.hide();
                mProgressDialog = null;
            }

            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setTitle(aTitle);
            mProgressDialog.setMessage(aText);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelable(false);
            DLog.log("showProgressDialog with message: " + aText);
            mProgressDialog.show();
        } catch (Exception e) {
            DLog.log("Exception in showProgressDialog: " + e.getMessage());
        }
    }

    public void setLayout(EPTWizardPage aNewLayout, EPTTransitionReason aTransitionReason) {

        DLog.log("setLayout : " + aNewLayout.name());
        hideProgressDialog();

        if ((aNewLayout != mCurrentPage) && (aTransitionReason != EPTTransitionReason.UserBack)) {
            mBackStack.add(mCurrentPage);
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.ept_page_title_default));
        actionBar.setDisplayHomeAsUpEnabled(true);
        String title = getString(R.string.ept_page_title_default);
        actionBar.setHomeAsUpIndicator(R.drawable.back);

        switch (aNewLayout) {

            case SelectContent:
                if (mRole == OPERATION_TYPE_BACKUP) {
                    title = getString(R.string.ept_select_content_text_backup);
                } else {
                    title = getString(R.string.ept_select_content_text_restore);
                }
                break;
            case Progress:
                if (mRole == OPERATION_TYPE_BACKUP) {
                    title = getString(R.string.backup_inprogress);
                } else {
                    title = getString(R.string.receiving_data);
                }
                break;
            case Complete:
                title = getString(R.string.ept_page_title_summary);
                actionBar.setDisplayHomeAsUpEnabled(false);
                break;
            default:
                title = getString(R.string.ept_page_title_default);
                break;
        }
        actionBar.setTitle(title);

        if (aNewLayout.ordinal() == EPTWizardPage.SelectContent.ordinal()) {

            contentDetailsView = (RecyclerView) findViewById(R.id.contentDetails_list);
            btn_start_migration = findViewById(R.id.start_migration);
            btn_start_migration.setOnClickListener(this);

            TextView selectContentText = (TextView) findViewById(R.id.SelectContentBottomText);
            selectContentText.setText(R.string.ept_select_content_text);
            contentDetailsAdapter = new ContentDetailsAdapter(BackupRestoreActivity.this, ContentDetailsAdapter.CONTENT_SELECTION, contentDetailsMap);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
            contentDetailsView.setLayoutManager(mLayoutManager);
            contentDetailsView.setItemAnimator(null);
            //contentDetailsView.addItemDecoration(new DividerItemDecoration(getApplicationContext(),RecyclerView.VERTICAL));
            contentDetailsView.setAdapter(contentDetailsAdapter);
            contentDetailsAdapter.notifyDataSetChanged();
            updateStorageProgress();
            if (mRole == OPERATION_TYPE_RESTORE && contentDetailsMap.get(EMDataType.EM_DATA_TYPE_SMS_MESSAGES).isSelected()) {
                EMUtilsDefaultSmsApp emUtilsDefaultSmsApp = new EMUtilsDefaultSmsApp(this, BackupRestoreActivity.this);
                emUtilsDefaultSmsApp.becomeDefaultSmsApp();
            }

        } else if (aNewLayout.ordinal() == EPTWizardPage.selectoldornewdevice.ordinal()) {

            ImageView backup = findViewById(R.id.old_device_image);
            backup.setImageResource(R.drawable.device_backup);
            ImageView restore = findViewById(R.id.new_device_image);
            restore.setImageResource(R.drawable.device_restore);
            Button thisIsSourceDeviceButton = (Button) findViewById(R.id.old_device_button);
            thisIsSourceDeviceButton.setText(getString(R.string.backup));
            Button thisIsDestinationDeviceButton = (Button) findViewById(R.id.new_device_button);
            thisIsDestinationDeviceButton.setText(getString(R.string.restore));
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.getId() == R.id.old_device_button) {
                        mRole = OPERATION_TYPE_BACKUP;

                    } else {
                        mRole = OPERATION_TYPE_RESTORE;
                    }
                    loadContentDetails();
                }
            };
            thisIsSourceDeviceButton.setOnClickListener(listener);
            thisIsDestinationDeviceButton.setOnClickListener(listener);
        }else if (aNewLayout.ordinal() == EPTWizardPage.Progress.ordinal()) {

            contentDetailsProgressView = (RecyclerView) findViewById(R.id.contentDetailsProgress_list);
            contentProgressDetailsAdapter = new ContentProgressDetailsAdapter(contentDetailsMap);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
            contentDetailsProgressView.setLayoutManager(layoutManager);
            contentDetailsProgressView.setItemAnimator(null);
            contentDetailsProgressView.setAdapter(contentProgressDetailsAdapter);
            contentProgressDetailsAdapter.notifyDataSetChanged();

        } else if (aNewLayout.ordinal() == EPTWizardPage.Complete.ordinal()) {
            if (Constants.UNINSTALLATION_SUGGESTIONS.contains(Constants.COMPANY_NAME)) {
                //btn_summary_finish.setText(R.string.ept_next_button);
            }

            contentDetailsSummaryView = (RecyclerView) findViewById(R.id.contentDetailsSummary_list);

            contentDetailsAdapter = new ContentDetailsAdapter(BackupRestoreActivity.this, ContentDetailsAdapter.SUMMARY, contentDetailsMap);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
            contentDetailsSummaryView.setLayoutManager(mLayoutManager);
            contentDetailsSummaryView.setItemAnimator(null);
            //contentDetailsView.addItemDecoration(new DividerItemDecoration(getApplicationContext(),RecyclerView.VERTICAL));
            contentDetailsSummaryView.setAdapter(contentDetailsAdapter);
            contentDetailsAdapter.notifyDataSetChanged();
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
        mCurrentPage = aNewLayout;
        mFlipper.setDisplayedChild(aNewLayout.ordinal());
    }

    @Override
    public void onBackPressed() {
        switch (mCurrentPage) {
            case SelectContent:
                optionToCloseApp();
            case Progress:
                optionToCloseApp();
                break;
            case Complete:
                break;
            default:
                break;

        }
    }

    private void forceCloseApp() {
        DLog.log("***** About to system exit");
//        System.exit(0);
        finish();
        DLog.log("***** Done to system exit");
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

    @Override
    public boolean onOptionsItemSelected(MenuItem aMenuItem) {
        switch (aMenuItem.getItemId()) {
            // If home icon is clicked return to main Activity
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                break;
        }
        return true;
    }

    private UsbDevice getDevice() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        UsbDevice usbDevice = null;
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            usbDevice = device;
            return usbDevice;
        }
        return null;
    }

    private UsbMassStorageDevice getUsbDevice() {
        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(this);
        if (devices.length == 0) {
            return null;
        }
        for (UsbMassStorageDevice device : devices) {
            // before interacting with a device you need to call init()!
            try {
                device.init();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Only uses the first partition on the device
            FileSystem currentFs = device.getPartitions().get(0).getFileSystem();
            DLog.log("Capacity: " + currentFs.getCapacity());
            DLog.log("Occupied Space: " + currentFs.getOccupiedSpace());
            DLog.log("Free Space: " + currentFs.getFreeSpace());
            DLog.log("Chunk size: " + currentFs.getChunkSize());
            return device;
        }
        return null;
    }

    enum EPTWizardPage {
        selectoldornewdevice,
        SelectContent,
        Progress,
        Complete,
    }


    enum EPTTransitionReason {
        UserBack, // User touched back
        UserNext, // User touched next
        Automatic, // Automatic state change, e.g. USB connection detected
        StartUp    // Setting up the first page
    }


}