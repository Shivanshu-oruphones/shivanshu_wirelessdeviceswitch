package com.pervacio.wds.custom;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMMigrateStatus;
import com.pervacio.wds.app.EMStringConsts;
import com.pervacio.wds.app.EMUtility;
import com.pervacio.wds.app.ui.EasyMigrateActivity;
import com.pervacio.wds.custom.externalstorage.BackupRestoreActivity;
import com.pervacio.wds.custom.externalstorage.SdcardUtils;
import com.pervacio.wds.custom.models.EDeviceInfo;
import com.pervacio.wds.custom.models.FeatureConfig;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.Constants;
import com.pervacio.wds.custom.utils.DashboardLog;
import com.pervacio.wds.custom.utils.DeviceInfo;

import java.util.HashMap;
import java.util.Iterator;


public class TransferModeSelectionActivity extends AppCompatActivity {


    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private Button usbTransferBtn, sdcardTransferBtn, wirelessTransferBtn;
    private UsbManager usbManager;
    private int operationType = EMStringConsts.OPERATION_TYPE_BACKUP;
    private View.OnClickListener sdCardOnlickListner = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SdcardUtils sdcardUtils = new SdcardUtils();
            if (sdcardUtils.isSdCardInserted()) {
                Intent intent = new Intent(TransferModeSelectionActivity.this, BackupRestoreActivity.class);
                if (true) {
                    operationType = EMStringConsts.OPERATION_TYPE_BACKUP;
                } else {
                    operationType = EMStringConsts.OPERATION_TYPE_RESTORE;
                }
                prepareDeviceInfo(EMStringConsts.EXTERNAL_STORAGE_SDCARD);
                intent.putExtra(EMStringConsts.OPERATION_TYPE, operationType);
                intent.putExtra(EMStringConsts.TRANSFER_TYPE, EMStringConsts.EXTERNAL_STORAGE_SDCARD);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(TransferModeSelectionActivity.this, "Please insert SDCARD", Toast.LENGTH_SHORT).show();
            }
        }
    };
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            DLog.log("Name: " + device.getDeviceName());
                            CommonUtil.getInstance().setUsbDevice(getUsbDevice());
                            CommonUtil.getInstance().setUsbBackupPath(CommonUtil.getInstance().getUsbDevice().getPartitions().get(0).getFileSystem().getRootDirectory() + "/" + Constants.EXTERNAL_STORAGE_BACKUP_FOLDER);
                            prepareDeviceInfo(EMStringConsts.EXTERNAL_STORAGE_USB);
                            DLog.log("path : " + CommonUtil.getInstance().getUsbBackupPath());
                            /*Intent intent1 = new Intent(TransferModeSelectionActivity.this, BackupRestoreActivity.class);
                            intent1.putExtra(EMStringConsts.OPERATION_TYPE, operationType);
                            intent1.putExtra(EMStringConsts.TRANSFER_TYPE, EMStringConsts.EXTERNAL_STORAGE_USB);
                            startActivity(intent1);*/

                            Intent intent1 = new Intent(TransferModeSelectionActivity.this, EasyMigrateActivity.class);
                            intent1.putExtra(EMStringConsts.TRANSFER_TYPE, EMStringConsts.EXTERNAL_STORAGE_USB);
                            startActivity(intent1);

                            finish();
                        }
                    } else {
                        DLog.log("permission denied for device " + device);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_mode_selection);
        EMMigrateStatus.initialize();
        wirelessTransferBtn = findViewById(R.id.wireless);
        usbTransferBtn = findViewById(R.id.usb_transfer);
        sdcardTransferBtn = findViewById(R.id.sdcarad_transfer);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        if (FeatureConfig.getInstance().getProductConfig().isSupportSDCardTransfer()) {
            sdcardTransferBtn.setVisibility(View.VISIBLE);
        }
        if (FeatureConfig.getInstance().getProductConfig().isSupportUSBTransfer()) {
            usbTransferBtn.setVisibility(View.VISIBLE);
        }
        wirelessTransferBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TransferModeSelectionActivity.this, EasyMigrateActivity.class);
                intent.putExtra(EMStringConsts.TRANSFER_TYPE, EMStringConsts.WIRELESS_TRANSFER);
                startActivity(intent);
                finish();
            }
        });

        usbTransferBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PendingIntent permissionIntent = PendingIntent.getBroadcast(TransferModeSelectionActivity.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                UsbDevice device = getDevice();
                if (device == null) {
                    Toast.makeText(TransferModeSelectionActivity.this, "Please Connect USB Device", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (usbManager.hasPermission(device)) {
                    DLog.log("Name: " + device.getDeviceName());
                    String  value = EMUtility.createTempFile2(device.getDeviceName());
                    Toast.makeText(TransferModeSelectionActivity.this, ""+value, Toast.LENGTH_SHORT).show();
                    CommonUtil.getInstance().setUsbDevice(getUsbDevice());
                    CommonUtil.getInstance().setUsbBackupPath(CommonUtil.getInstance().getUsbDevice().getPartitions().get(0).getFileSystem().getRootDirectory() + "/" + Constants.EXTERNAL_STORAGE_BACKUP_FOLDER);
                    prepareDeviceInfo(EMStringConsts.EXTERNAL_STORAGE_USB);
                    finish();
                    Intent intent1 = new Intent(TransferModeSelectionActivity.this, BackupRestoreActivity.class);
                    intent1.putExtra(EMStringConsts.TRANSFER_TYPE, EMStringConsts.EXTERNAL_STORAGE_USB);
                    startActivity(intent1);
                } else {
                    usbManager.requestPermission(getDevice(), permissionIntent);
                }
            }
        });

        sdcardTransferBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SdcardUtils sdcardUtils = new SdcardUtils();
                if (sdcardUtils.isSdCardInserted()) {
                    Intent intent = new Intent(TransferModeSelectionActivity.this, BackupRestoreActivity.class);
                    prepareDeviceInfo(EMStringConsts.EXTERNAL_STORAGE_SDCARD);
                    intent.putExtra(EMStringConsts.TRANSFER_TYPE, EMStringConsts.EXTERNAL_STORAGE_SDCARD);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(TransferModeSelectionActivity.this, "Please insert SDCARD", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private UsbDevice getDevice() {
        try {
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            UsbDevice usbDevice = null;
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
            while (deviceIterator.hasNext()) {
                UsbDevice device = deviceIterator.next();
                usbDevice = device;
                return usbDevice;
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                // Only uses the first partition on the device
                if (device.getPartitions().isEmpty()) {

                } else {
                    FileSystem currentFs = device.getPartitions().get(0).getFileSystem();
                    DLog.log("Capacity: " + currentFs.getCapacity());
                    DLog.log("Occupied Space: " + currentFs.getOccupiedSpace());
                    DLog.log("Free Space: " + currentFs.getFreeSpace());
                    DLog.log("Chunk size: " + currentFs.getChunkSize());
                }
                return device;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        DLog.log("onPause: ");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        DLog.log("onWindowFocusChanged: " + hasFocus);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mUsbReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareDeviceInfo(int deviceType) {
        EDeviceInfo deviceInfo = DeviceInfo.getInstance().getDeviceInfo(operationType == EMStringConsts.OPERATION_TYPE_BACKUP);
        EDeviceInfo externalDisk = new EDeviceInfo();
        if (deviceType == EMStringConsts.EXTERNAL_STORAGE_SDCARD) {
            SdcardUtils sdcardUtils = new SdcardUtils();
            externalDisk.setTotalStorage(sdcardUtils.getSdCardToatalSize());
            externalDisk.setFreeStorage(sdcardUtils.getSdCardAvailableSize());
        } else {
            long totalSpace = CommonUtil.getInstance().getUsbDevice().getPartitions().get(0).getFileSystem().getCapacity();
            long freeSpace = CommonUtil.getInstance().getUsbDevice().getPartitions().get(0).getFileSystem().getFreeSpace();
            externalDisk.setFreeStorage(freeSpace / 1024);
            externalDisk.setTotalStorage(totalSpace / 1024);
        }
        if (operationType == EMStringConsts.OPERATION_TYPE_BACKUP) {
            deviceInfo.setOperationType(Constants.OPERATION_TYPE.BACKUP.value());
            externalDisk.setOperationType(Constants.OPERATION_TYPE.RESTORE.value());
            DashboardLog.getInstance().geteDeviceSwitchSession().setSourceDeviceInfoId(deviceInfo);
            DashboardLog.getInstance().geteDeviceSwitchSession().setDestinationDeviceInfoId(externalDisk);
        } else {
            deviceInfo.setOperationType(Constants.OPERATION_TYPE.RESTORE.value());
            externalDisk.setOperationType(Constants.OPERATION_TYPE.BACKUP.value());
            DashboardLog.getInstance().geteDeviceSwitchSession().setSourceDeviceInfoId(externalDisk);
            DashboardLog.getInstance().geteDeviceSwitchSession().setDestinationDeviceInfoId(deviceInfo);
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }
}
