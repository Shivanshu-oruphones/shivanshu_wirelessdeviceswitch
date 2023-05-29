package com.pervacio.wds.custom.imeireader.manual;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.pervacio.wds.custom.imeireader.IMEIReadService;
import com.pervacio.wds.custom.imeireader.IMEIReaderListener;


import java.io.File;

public class IMEIReader {
    private static final String TAG = "IMEIReader";
    private Context mContext;
    private IMEIReaderListener mReaderListener;
    private Uri imgUri;
    private ScreenshotDetector screenshotDetectionDelegate;
    private static IMEIReader mIMEIReader;

    private IMEIReader() {

    }

    public static IMEIReader getInstance() {
        if (mIMEIReader == null) {
            mIMEIReader = new IMEIReader();
        }
        return mIMEIReader;
    }

    public void readIMEI(Activity activity) {
        if(screenshotDetectionDelegate == null) {
            screenshotDetectionDelegate = new ScreenshotDetector(activity, mScreenshotDetectionListener);
            mContext = activity;
            mReaderListener = (IMEIReaderListener) activity;
        }
        screenshotDetectionDelegate.startScreenshotDetection();
    }

    public void stopImeiReader() {
        imgUri = null;
        mReaderListener = null;
        screenshotDetectionDelegate = null;
        mContext = null;
        mIMEIReader = null;
    }



    ScreenshotDetector.ScreenshotDetectionListener mScreenshotDetectionListener = new ScreenshotDetector.ScreenshotDetectionListener() {
        @Override
        public void onScreenCaptured(String path, Uri uri) {
            if(imgUri != null) {
                return;
            }
            imgUri = uri;
            screenshotDetectionDelegate.stopScreenshotDetection();
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            File file = new File(path);
            if(file.exists()){
                IMEIReadService.startReadIMEIWithImageURI(uri,mReaderListener);
            } else {
                mReaderListener.onError("Calling ImeiReadservice  failed file not exist ::");
                Log.e(TAG,"Calling ImeiReadservice  failed file not exist ::" );
            }
        }
    };
}
