package com.pervacio.wds.custom.imeireader.floatinghead;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;



import java.util.List;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.custom.imeireader.IMEIReadActivity;
import com.pervacio.wds.custom.imeireader.IMEIReadService;
import com.pervacio.wds.custom.imeireader.IMEIReaderListener;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.PreferenceUtil;

public class FloatingHeadService extends Service implements View.OnTouchListener {

    public static final String CHANNEL_ID_IMEI = "pervacio_imei_channel";

    private static final String TAG = "ChatHeadService";
    private WindowManager mWindowManager;
    private View mFloatingHeadView;

    private WindowManager.LayoutParams params = null;
    private ScreenshotManager mScreenshotManager;


    private LinearLayout mGIFMovieViewContainer;
    private TextView tvAssistedMessage;
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private IMEIReaderListener mReaderListener;
    private boolean foundImei;
    private long startTime;
    private long MAX_EXECUTION_TIME = 15*1000;


    //Instance of inner class created to provide access  to public methods in this class
    private final IBinder localBinder = new IMEIBinder();

    @Override
    public IBinder onBind(Intent intent) {

        return localBinder;
    }

    /**
     * This method is  Called when activity have disconnected from a particular interface published by the service.
     * Note: Default implementation of the  method just  return false
     */
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    /**
     * Called when an activity is connected to the service, after it had
     * previously been notified that all had disconnected in its
     * onUnbind method.  This will only be called by system if the implementation of onUnbind method was overridden to return true.
     */
    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }


    //This function do the placement of FloatingHead on screen
    private WindowManager.LayoutParams params() {
        WindowManager.LayoutParams params = null;
        int flag =WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        int _type = WindowManager.LayoutParams.TYPE_PHONE;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            _type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                _type,
                flag,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.y = 60;
        return params;
    }


    //Prepare the floating head view
    @SuppressLint("ClickableViewAccessibility")
    private void prepareView() {

        mFloatingHeadView = LayoutInflater.from(this).inflate(R.layout.layout_floating_head, null);


        mGIFMovieViewContainer = mFloatingHeadView.findViewById(R.id.llContainerFHead);
        tvAssistedMessage = mFloatingHeadView.findViewById(R.id.tvAssistedMessage);

        mGIFMovieViewContainer.addView(CommonUtil.getNewGIFMovieView(getApplicationContext(), "dial_call.gif"));
        //Drag and move chat head using user's touch action.
        //mGIFMovieViewContainer.setOnTouchListener(this);
    }


    /* Foreground service */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID_IMEI,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if(manager!=null)
             manager.createNotificationChannel(serviceChannel);
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, IMEIReadActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_IMEI)
                .setContentTitle("Foreground Service")
                .setContentText("input")
//                .setSmallIcon(R.drawable.launcher_icon)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        //stopSelf();*/
        startCapturing();

        return START_NOT_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate() {
        super.onCreate();
        startTime =  System.currentTimeMillis();
//        ThemeUtil.onActivityCreateSetTheme(this);
        mScreenshotManager = ScreenshotManager.getInstance();
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        prepareView();
        params = params();
        mWindowManager.addView(mFloatingHeadView, params);

    }

    private boolean isTimeOver(){
        long latestTime= System.currentTimeMillis();
        long spentTime = latestTime-startTime;

        return spentTime>MAX_EXECUTION_TIME;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingHeadView != null) mWindowManager.removeView(mFloatingHeadView);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //remember the initial position.
                initialX = params.x;
                initialY = params.y;

                //get the touch location
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                return true;
            case MotionEvent.ACTION_UP:
                /*if ((Math.abs(initialTouchX - event.getRawX()) < 5) && (Math.abs(initialTouchY - event.getRawY()) < 5)) {
                    startCapturing();
                }*/
                return true;
            case MotionEvent.ACTION_MOVE:
                //Calculate the X and Y coordinates of the view.
                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                params.y = initialY + (int) (event.getRawY() - initialTouchY);

                //Update the layout with new X & Y coordinate
                mWindowManager.updateViewLayout(mFloatingHeadView, params);
                return true;
        }
        return false;
    }

    private void startCapturing() {
        foundImei = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mScreenshotManager.takeScreenshot(FloatingHeadService.this, new ScreenshotManager.HeadScreeShotListener() {
                @Override
                public void onScreenCaptured(Bitmap imagePath) {
                    if (!foundImei)
                        IMEIReadService.readIMEI(imagePath, new IMEIReaderListener() {
                            @Override
                            public void onIMEI(ImeiStatus status,List<String> list) {

                                if(isTimeOver()){
                                    // Show message
                                    finishWithStatus(IMEIReaderListener.ImeiStatus.TIME_OUT,null);
                                } else if (!foundImei && !list.isEmpty() && mReaderListener != null) {
//                                    LogUtil.printLog(TAG, "Final : IMEI :: : ----" + list.toString());
                                    DLog.log ("Final : IMEI :: : ----" + list.toString());
                                    finishWithStatus(ImeiStatus.FOUND,list);

                                }
                            }

                            @Override
                            public void onError(String error) {
                                boolean isVisionCloudApi = PreferenceUtil.getBoolean(PreferenceUtil.IS_CLOUD_VISION);
                                if(!isVisionCloudApi){
                                    PreferenceUtil.putBoolean(PreferenceUtil.IS_CLOUD_VISION,true);
                                }else {
//                                     LogUtil.printLog(TAG, " IMEI error:: ----" + error);
                                    DLog.log( " IMEI error:: ----" + error);
                                    if (!foundImei){
                                        finishWithStatus(ImeiStatus.ERROR,null);
                                    }
                                }

                            }
                        });
                }
            });
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void finishWithStatus(final IMEIReaderListener.ImeiStatus status , final List<String> list){
        mScreenshotManager.stopCapturing();
        foundImei = true;
        finishing(status,list);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void finishing(final IMEIReaderListener.ImeiStatus status , final List<String> list) {
        if(mFloatingHeadView==null)
            return;
        //Update the layout with new X & Y coordinate
       // params.x = mScreenshotManager.getScreenDimensions().getMetrics().widthPixels/2-100;
        tvAssistedMessage.setVisibility(View.GONE);
        params.y = mScreenshotManager.getScreenDimensions().getMetrics().heightPixels/2-100;
        params.gravity = Gravity.TOP | Gravity.CENTER;
        mWindowManager.updateViewLayout(mFloatingHeadView, params);
        mGIFMovieViewContainer.removeAllViews();
        mGIFMovieViewContainer.addView(CommonUtil.getNewGIFMovieView(getApplicationContext(), "count_down.gif"));
        PreferenceUtil.putBoolean(PreferenceUtil.IS_CLOUD_VISION, false);
        IMEIReadService.destroy();
       (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                 stopSelf();
                if (mReaderListener != null){
                    if(status == IMEIReaderListener.ImeiStatus.ERROR){
                        mReaderListener.onError("Error ");
                    }else{
                        mReaderListener.onIMEI(status,list);
                    }
                }
            }
        }, 3000);
    }


    //BINDER
    public void setReaderListener(IMEIReaderListener readerListener) {
        mReaderListener = readerListener;
    }

    public class IMEIBinder extends Binder {
        public FloatingHeadService getService() {
            return FloatingHeadService.this;

        }
    }

}

