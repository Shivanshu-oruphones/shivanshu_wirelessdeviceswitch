
package com.pervacio.wds.custom.imeireader.manual;

import android.app.Activity;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

import java.lang.ref.WeakReference;

public class ScreenshotDetector {
    private static final String TAG = "ScreenshotDetector";
    private WeakReference<Activity> activityWeakReference;
    private ScreenshotDetectionListener listener;


    public ScreenshotDetector(Activity activityWeakReference, ScreenshotDetectionListener listener) {
        this.activityWeakReference = new WeakReference<>(activityWeakReference);
        this.listener = listener;
    }

    public void startScreenshotDetection() {
        activityWeakReference.get().getContentResolver()
                .registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, contentObserver);
    }

    public void stopScreenshotDetection() {
        activityWeakReference.get().getContentResolver().unregisterContentObserver(contentObserver);
        listener = null;
    }

    private ContentObserver contentObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            Log.e(TAG, "ScreenshotDetector  onChange ::");
            String path = getFilePathFromContentResolver(activityWeakReference.get(), uri);
            if (isScreenshotPath(path)) {
                onScreenCaptured(path, uri);
            }
        }
    };

    private void onScreenCaptured(String path, Uri uri) {
        Log.e(TAG, "ScreenshotDetector  onScreenCaptured  ::");
        if (listener != null) {
            listener.onScreenCaptured(path, uri);
        }
    }

    private boolean isScreenshotPath(String path) {
        return path != null && path.toLowerCase().contains("screenshots");
    }

    private String getFilePathFromContentResolver(Context context, Uri uri) {
        try {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATA
            }, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                cursor.close();
                return path;
            }
        } catch (IllegalStateException ignored) {
        }
        return null;
    }

    public interface ScreenshotDetectionListener {
        void onScreenCaptured(String path, Uri uri);
    }
}
