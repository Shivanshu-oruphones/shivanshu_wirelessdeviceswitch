package com.pervacio.wds.custom.imeireader;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
//import android.support.annotation.NonNull;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AppCompatActivity;
import android.os.Looper;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/*import org.pervacio.wirelessapp.R;
import org.pervacio.wirelessapp.controller.BaseActivity;
import org.pervacio.wirelessapp.controllers.IMEIReadChildActivity;
import org.pervacio.wirelessapp.imeireader.floatinghead.FloatingHeadService;
import org.pervacio.wirelessapp.imeireader.floatinghead.ScreenshotManager;
import org.pervacio.wirelessapp.logging.LogUtil;

import org.pervacio.wirelessapp.util.ThemeUtil;*/

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;



import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.custom.IMEIReadChildActivity;
import com.pervacio.wds.custom.imeireader.floatinghead.FloatingHeadService;
import com.pervacio.wds.custom.imeireader.floatinghead.ScreenshotManager;
import com.pervacio.wds.custom.imeireader.manual.IMEIReader;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.CustomProgressDialog;

public abstract class IMEIReadActivity extends AppCompatActivity implements IMEIReaderListener {

    private static final String TAG = "ImeiReadeActivity";
    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE_PERMISSION = 183;
    public static final int RC_IMEI_READ = 1483;
    public static final String EX_SHOW = "ex_show";

    private Button startImeiReaderButton;
    //Manual
    private LinearLayout llManualContainer;
    private TextView instructionText;
    //auto
    private LinearLayout llContainerFHead,llAutomaticContainer;


    private ScreenshotManager mScreenshotManager;
    private FloatingHeadService mHeadService;
    private boolean isBound;
    private static Class<IMEIReadChildActivity> sClassName;
    private CustomProgressDialog mProgressDialog;
    private boolean isManual = CommonUtil.isForManualIMEI();


    protected static void startActivity(Activity activity, Class<IMEIReadChildActivity> className) {
        sClassName = className;
        Intent intent = new Intent(activity, className);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivityForResult(intent, RC_IMEI_READ);
    }

    private static void startActivity(Activity activity, boolean showPopup, Class<IMEIReadChildActivity> className) {
        Intent intent = new Intent(activity, className);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EX_SHOW, showPopup);
        activity.startActivityForResult(intent, RC_IMEI_READ);
    }

    private void initManual() {
        llManualContainer = findViewById(R.id.imei_android_10_manual);
        instructionText = findViewById(R.id.imei_instruction_text);
        llManualContainer.setVisibility(isManual?View.VISIBLE:View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            instructionText.setText(Html.fromHtml(getString(R.string.imei_read_manual_instructions_10), Html.FROM_HTML_MODE_COMPACT));
        }else {
            instructionText.setText(Html.fromHtml(getString(R.string.imei_read_manual_instructions_10)));
        }
    }

    private void initAutomatic(){

        llAutomaticContainer = findViewById(R.id.imei_android_10_automatically);
        llAutomaticContainer.setVisibility(isManual?View.GONE:View.VISIBLE);

        llContainerFHead = findViewById(R.id.llContainerFHead);
        llContainerFHead.removeAllViews();
        llContainerFHead.addView(CommonUtil.getNewGIFMovieView(getApplicationContext(), "dial_call.gif"));

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: 20/08/20 this is only for temporary purpose it need to be handled by base activity only
//        ThemeUtil.onActivityCreateSetTheme(this);

        setContentView(R.layout.activity_imeiread_activity);

        mProgressDialog = new CustomProgressDialog(this);
        mScreenshotManager = ScreenshotManager.getInstance();

        startImeiReaderButton = findViewById(R.id.imei_start_btn);
        startImeiReaderButton.setEnabled(true);
        startImeiReaderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkReadExternalStoragePermission();
            }
        });
        DLog.log("IMEI Capture IsManual "+isManual);
        if(isManual)
            initManual();
        else
            initAutomatic();

//        LogUtil.printLog(TAG, " Show Pop menu " + (getIntent() != null ? getIntent().getBooleanExtra(EX_SHOW, false) : "Intent is null"));
        DLog.log( " Show Pop menu " + (getIntent() != null ? getIntent().getBooleanExtra(EX_SHOW, false) : "Intent is null"));
        // Show message
        if (getIntent() != null && getIntent().getBooleanExtra(EX_SHOW, false)) {
            // getIntent().putExtra(EX_SHOW, false);
            CommonUtil.DialogUtil.showAlert(IMEIReadActivity.this, getString(R.string.imei_capture_title), getString(R.string.imei_search_time_out), getString(R.string.str_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        }

    }

   /* @Override
    protected String getToolBarName() {
        return getResources().getString(R.string.imei_capture_title);
    }

    @Override
    protected boolean setBackButton() {
        return false;
    }

    @Override
    protected boolean isFullscreenActivity() {
        return false;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_imeiread_activity;
    }*/


    private void getImei(boolean hasPermissionForScreenCapture) {
        DLog.log("enter getImei  hasPermissionForScreenCapture "+hasPermissionForScreenCapture);
        if(isManual){
            IMEIReader.getInstance().readIMEI(this);
            showDialor();
        }else if (mScreenshotManager.startService(this, hasPermissionForScreenCapture, boundServiceConnection)) {
            showDialor();
        }
    }

    private void showDialor() {
        DLog.log("enter showDialor " );
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("copied text", "*#06#");
        clipboard.setPrimaryClip(clip);
        Intent intent = new Intent(Intent.ACTION_DIAL);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_READ_EXTERNAL_STORAGE_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    requestReadExternalStoragePermission();
                } else {
                    DLog.log("calling getImei false from onRequestPermissionsResult REQUEST_CODE_READ_EXTERNAL_STORAGE_PERMISSION");
                    getImei(false);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void checkReadExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestReadExternalStoragePermission();
        } else {
            DLog.log("calling getImei false from checkReadExternalStoragePermission");
            getImei(false);
        }
    }

    private void requestReadExternalStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_READ_EXTERNAL_STORAGE_PERMISSION);
    }

    /*NEW FLOW */
    protected abstract void startDiagnostics(String imei);

    /*{
     *//* Util.saveImeiInPrefs(imei);
        if ("d2d".equalsIgnoreCase(BuildConfig.FLAVOR_flav)) {
            //startApp();
        } else {
            PinValidationActivity.startActivity(this);
        }*//*
        finish();
    }*/

    /*NEW FLOW */

    @Override
    protected void onResume() {
        super.onResume();
        if (mHeadService != null) {
            mHeadService.stopSelf();
            removeAndUnboundTheService();
        }

    }

    @Override
    protected void onDestroy() {
        removeAndUnboundTheService();
        super.onDestroy();
    }

    private void removeAndUnboundTheService() {
        if (isBound) {
            unbindService(boundServiceConnection);
            isBound = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ScreenshotManager.CODE_DRAW_OVER_OTHER_APP_PERMISSION:
                DLog.log("got CODE_DRAW_OVER_OTHER_APP_PERMISSION");
                if(mScreenshotManager.romSpecificPermission){
                    showHide(true);
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showHide(false);
                            getImei(false);
                        }
                    }, 500);
                }else {
                    getImei(false);
                }
                break;
            case ScreenshotManager.REQUEST_SCREENSHOT_PERMISSION:
                DLog.log("got REQUEST_SCREENSHOT_PERMISSION");
                if (resultCode == RESULT_OK) {
                    mScreenshotManager.onActivityResult(resultCode, data);
                    DLog.log("calling getImei true from REQUEST_SCREENSHOT_PERMISSION case");
                    getImei(true);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private String arraysToStringSeparatedBy(List<String> list, String sep) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            values.append(list.get(i));
            if (i != list.size() - 1) {
                values.append(sep);
            }
        }
        return values.toString();
    }

    private ServiceConnection boundServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;
            FloatingHeadService.IMEIBinder binderBridge = (FloatingHeadService.IMEIBinder) service;
            mHeadService = binderBridge.getService();
            mHeadService.setReaderListener(IMEIReadActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mHeadService = null;
        }
    };


    private void showHide(boolean isShow){
        if(mProgressDialog==null || instructionText ==null)
            return;
        if(isShow){
            mProgressDialog.setInfo(R.string.capturing_imei, R.string.wds_please_wait);
            mProgressDialog.show();
        }else {
            mProgressDialog.hide();
        }
    }

    @Override
    public void onIMEI(IMEIReaderListener.ImeiStatus status, List<String> list) {
        DLog.log(TAG+ " IMEI IMEIReaderListener.ImeiStatus :" + status);
        showHide(false);
        switch (status) {
            case FOUND:

                startDiagnostics(arraysToStringSeparatedBy(list, ","));

                break;
            case TIME_OUT:
                DLog.log(TAG+  " onIMEI: " + status);
                if (mHeadService != null) {
                    mHeadService.stopSelf();
                    removeAndUnboundTheService();
                }
                finish();
                if (sClassName != null) {
                    startActivity(IMEIReadActivity.this, true, sClassName);
                }
                break;
        }
    }

    @Override
    public void onError(String error) {
        showHide(false);
        startDiagnostics(CommonUtil.getMACAddressInIMEIFormat(IMEIReadActivity.this));
    }


}

