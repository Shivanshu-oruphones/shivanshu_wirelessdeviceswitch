package com.pervacio.wds.custom.imeireader.floatinghead;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
//import android.support.annotation.NonNull;
//import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;


import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.custom.utils.CommonUtil;
import com.pervacio.wds.custom.utils.PreferenceUtil;
import com.pervacio.wds.custom.utils.RomUtils;

public class ScreenshotManager {
    private static final String TAG = "ScreenshotManager";
    private static ScreenshotManager INSTANCE = null;
    public static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    public static final int REQUEST_SCREENSHOT_PERMISSION = 2085;
    private int MAX_TIME_DURATION_IN_MILLISECOND = 500;

    private Intent mIntent;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;
    private ScreenDimensions mScreenDimensions;
    private ImageReader mImageReader;
    private long lastScreenShotTime;
    private int maxScreenShotTimeDuration=500;
    public static boolean romSpecificPermission;


    private ScreenshotManager() {
    }

    public static synchronized ScreenshotManager getInstance() {
        synchronized (ScreenshotManager.class) {
            if (INSTANCE == null) {
                INSTANCE = new ScreenshotManager();
                romSpecificPermission = false;
            }
        }
        ;
        return INSTANCE;
    }

    public static boolean checkIfAllowOverOtherAppPermissionsAllowed(final Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {

                if(!romSpecificPermission && RomUtils.isMIUI(activity))
                {
                    CommonUtil.DialogUtil.showAlert(activity, activity.getString(R.string.imei_capture_title), activity.getString(R.string.permission_show_over_other_apps_xiaomi), activity.getString(R.string.go_to_settings), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            RomUtils.goToMiuiPermissionActivity(activity);
                            romSpecificPermission =true;
                        }
                    });

                } else {
                    CommonUtil.DialogUtil.showAlert(activity, activity.getString(R.string.imei_capture_title), activity.getString(R.string.permission_show_over_other_apps), activity.getString(R.string.str_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
                            activity.startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
                        }
                    });
                }
                return false;
            }
        }
        return true;
    }
    public boolean startService(final Activity activity, boolean hasPermissionForScreenCapture, ServiceConnection boundServiceConnection) {


        if (!checkIfAllowOverOtherAppPermissionsAllowed(activity)) {

        } else if (!hasPermissionForScreenCapture && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DLog.log("Calling REQUEST_SCREENSHOT_PERMISSION permission");
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            activity.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_SCREENSHOT_PERMISSION);
        } else {
            DLog.log("starting FloatingHeadService");
            Intent intent = new Intent(activity, FloatingHeadService.class);
            activity.bindService(intent, boundServiceConnection, Context.BIND_AUTO_CREATE);
            activity.startService(intent);
            return true;
        }
        return false;
    }


    public ScreenDimensions getScreenDimensions() {
        return mScreenDimensions;
    }

    public void onActivityResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null)
            mIntent = data;
        else mIntent = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initProjection(Context context, ImageReader imageReader) {
        mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjection == null)
            mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, mIntent);

        mediaProjection.createVirtualDisplay("screen-mirror", mScreenDimensions.getWidth(), mScreenDimensions.getHeight(), mScreenDimensions.getDensity(), DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, null);
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                mImageReader.setOnImageAvailableListener(null, null);
                if(mediaProjection!=null){
                    mediaProjection.unregisterCallback(this);
                    mediaProjection = null;
                }

            }
        }, null);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean takeScreenshot(@NonNull final Context context, final HeadScreeShotListener screeShotListener) {
        if (mIntent == null)
            return false;

        maxScreenShotTimeDuration = PreferenceUtil.getBoolean(PreferenceUtil.IS_CLOUD_VISION)?1000:500;

        //Prepare dimensions
        lastScreenShotTime = System.currentTimeMillis();
        mScreenDimensions = new ScreenDimensions(context);
        // Instance of Reader to read projected images
        mImageReader = ImageReader.newInstance(mScreenDimensions.getWidth(), mScreenDimensions.getHeight(), PixelFormat.RGBA_8888, 1);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {

                long currentTime = System.currentTimeMillis();
                long difference =   currentTime -  lastScreenShotTime;

                Image image = reader.acquireLatestImage();
                if (difference >=  maxScreenShotTimeDuration ) {

                    final Image.Plane[] planes = image.getPlanes();
                    final ByteBuffer buffer = planes[0].getBuffer();

                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mScreenDimensions.getMetrics().widthPixels;

                    // create bitmap
                    Bitmap bmp = Bitmap.createBitmap(mScreenDimensions.getMetrics().widthPixels + (int) ((float) rowPadding / (float) pixelStride), mScreenDimensions.getMetrics().heightPixels, Bitmap.Config.ARGB_8888);
                    bmp.copyPixelsFromBuffer(buffer);
                    screeShotListener.onScreenCaptured(bmp);
                    lastScreenShotTime = currentTime;
                }
                image.close();
            }
        }, null);


        //Initiate the prjection object
        initProjection(context, mImageReader);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void stopCapturing() {
        if(mediaProjection!=null)
        mediaProjection.stop();
    }


    public interface HeadScreeShotListener {
        void onScreenCaptured(Bitmap imagePath);
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    class ScreenDimensions {
        private DisplayMetrics mMetrics;
        private int width;
        private int height;
        private int density;

        public ScreenDimensions(Context context) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            mMetrics = new DisplayMetrics();
            display.getMetrics(mMetrics);

            Point size = new Point();
            display.getRealSize(size);
            setWidth(size.x);
            setHeight(size.y);
            setDensity(mMetrics.densityDpi);
        }

        public DisplayMetrics getMetrics() {
            return mMetrics;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getDensity() {
            return density;
        }

        public void setDensity(int density) {
            this.density = density;
        }
    }
}