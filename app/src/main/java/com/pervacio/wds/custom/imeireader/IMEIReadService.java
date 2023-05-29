package com.pervacio.wds.custom.imeireader;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import com.google.android.gms.common.util.IOUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.custom.imeireader.IMEIReaderListener;
import com.pervacio.wds.custom.utils.PreferenceUtil;

import org.pervacio.onediaglib.APPI;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class IMEIReadService  {
    static EMGlobals emGlobals = new EMGlobals();
    private static final String TAG = IMEIReadService.class.getName();

    private static boolean isProcessing;

    public static void startReadIMEIWithImageURI(Uri imgUri, IMEIReaderListener imeiReaderListener) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = null;
        File nfile = getFileFromStorage(imgUri);
        try {
             DLog.log("enter startReadIMEIWithImageURI startReadImei ::" + nfile.getAbsolutePath());
             bitmap = BitmapFactory.decodeStream(new FileInputStream(nfile), null, options);
        } catch (FileNotFoundException e) {
             e.printStackTrace();
             DLog.log(TAG+" startReadImei ::" + nfile.getAbsolutePath());
        }
        readIMEI(bitmap,imeiReaderListener);
    }

    @TargetApi(29)
    private static File getFileFromStorage(Uri uri) {
        Context context = emGlobals.getmContext();
        String fileName = null;
        try {
            ParcelFileDescriptor fileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r", null);
            InputStream is = new FileInputStream(fileDescriptor.getFileDescriptor());

            fileName = getFileName(context,uri);
            File cfile = new File(context.getCacheDir(), fileName);
            OutputStream os = new FileOutputStream(cfile);
            IOUtils.copyStream(is, os);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new File(context.getCacheDir(), fileName);

    }

    private static String getFileName(Context context, Uri uri) {
        String result = null;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        } finally {
            cursor.close();
        }

        return result;
    }


    /*IMEI READ*/
    public static void readIMEI(final Bitmap bm, final IMEIReaderListener listener) {
        if(isProcessing){
            listener.onIMEI(IMEIReaderListener.ImeiStatus.NOT_FOUND,Collections.<String>emptyList());
            return;
        }
         DLog.log(TAG+" IMEI Start:");
        isProcessing = true;
        final boolean isVisionCloudApi = PreferenceUtil.getBoolean(PreferenceUtil.IS_CLOUD_VISION);
        FirebaseVisionTextRecognizer detector;
        if(isVisionCloudApi){
             DLog.log(TAG+ " Google Cloud API");
            detector = FirebaseVision.getInstance().getCloudTextRecognizer();
        }else{
            DLog.log(TAG+ " Offline Google  API");
            detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        }
        detector.processImage(FirebaseVisionImage.fromBitmap(bm)).addOnSuccessListener(
                new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText texts) {
                        DLog.log(TAG+" IMEI finish: Text found");
                        isProcessing = false;
                        listener.onIMEI(IMEIReaderListener.ImeiStatus.FOUND,processTextResult(texts));
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                isProcessing=false;
                                DLog.log( TAG+" IMEI finish:" + e.getMessage());//,e, LogUtil.LogType.EXCEPTION);
                                listener.onError(e.getMessage());
                            }
                        });
    }

    public static void destroy(){
        isProcessing = false;
    }

    private static List<String> processTextResult(FirebaseVisionText texts) {
        List<String> imeiList = new ArrayList<String>();
        List<FirebaseVisionText.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.size() == 0) {
            return imeiList;
        }
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {

                FirebaseVisionText.Line line = lines.get(j);
                String imeiFromLine = getImei(line.getText().replace(" ", ""));
                if (!TextUtils.isEmpty(imeiFromLine)) {
                    imeiList.add(imeiFromLine);
                    continue;
                }

                List<FirebaseVisionText.Element> elements = line.getElements();
                for (int k = 0; k < elements.size(); k++) {
                    String etext = elements.get(k).getText();
                    String imei = getImei(etext);
                    if (imei != null) {
                        imeiList.add(imei);
                    }
                }
            }
        }
        return imeiList;
    }

    private static String getImei(String etext) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < etext.length(); i++) {
            char c = etext.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            } else if (sb.length() == 15) {
                break;
            } else if (etext.length() - (i + 1) >= 15) {
                if (sb.length() > 0 && sb.length() < 15) {
                    sb.setLength(0);
                }
            } else {
                break;
            }
        }
        if (sb.length() == 15) {
            return sb.toString();
        } else {
            return null;
        }
    }
}
