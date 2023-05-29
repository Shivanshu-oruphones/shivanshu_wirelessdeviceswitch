package com.pervacio.wds.custom.utils;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.custom.APPI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Created by Pervacio on 11/29/2017.
 */

public class EMSettingsMigrateUtility {

    static EMGlobals emGlobals = new EMGlobals();

    public boolean setWallPaper(byte[] value) {
        InputStream is = new ByteArrayInputStream(value);
        WallpaperManager wallpaperManager =
                WallpaperManager.getInstance(emGlobals.getmContext());
        try {
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setStream(is, null, true, WallpaperManager.FLAG_SYSTEM);
            }else{
                wallpaperManager.setStream(is);
            }
        } catch (Exception e) {
            DLog.log("Exception while setting wallpaper :"+e.getMessage());
            return false;
        }
        return true;
    }

    public boolean setScreenTimeOut(int i) {
        try {
            if (Build.MODEL.equalsIgnoreCase("T-02D")) {
                Settings.System.putInt(emGlobals.getmContext().getContentResolver(), "lock_screen_off_timeout", i);
            } else {
                Settings.System.putInt(emGlobals.getmContext().getContentResolver(), "screen_off_timeout", i);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean setScreenBrightnessAutoMode(int mode) {
        boolean isSettingSaved = false;
        try {
            isSettingSaved = Settings.System.putInt(emGlobals.getmContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
        } catch (Exception var3) {
            var3.printStackTrace();
        }
        return isSettingSaved;
    }

    public boolean setScreenBrightnessValue(int value) {
        boolean isSettingSaved = false;
        try {
            isSettingSaved = Settings.System.putInt(emGlobals.getmContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
        } catch (Exception var3) {
            var3.printStackTrace();
        }
        return isSettingSaved;
    }

    public boolean setScreenBrightness(float percentage) {
        try {
            String model = Build.MODEL;
            int minValue = this.getMinimumScreenBrightnessSetting();
            int maxValue = this.getMaximumScreenBrightnessSetting();
            if (model.equalsIgnoreCase("SC-05D")) {
                minValue = 30;
                maxValue = 205;
            }

            if (model.startsWith("SH") && model.contains("E")) {
                minValue = 30;
            }
            int brightnessValue = (int) (((percentage * (maxValue - minValue)) / 100) + minValue);
            return setScreenBrightnessValue(brightnessValue);
        } catch (Exception var6) {
            return false;
        }
    }

    public byte[] getWallpaper() {
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(emGlobals.getmContext());
        final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        Bitmap bitmap = ((BitmapDrawable) wallpaperDrawable).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    public int getScreenTimeout() {
        int defTimeOut = 0;
        try {
            String model = Build.MODEL;
            if (model.equalsIgnoreCase("T-02D")) {
                defTimeOut = Settings.System.getInt(emGlobals.getmContext().getContentResolver(), "lock_screen_off_timeout", 15000);
            } else {
                defTimeOut = Settings.System.getInt(emGlobals.getmContext().getContentResolver(), "screen_off_timeout");
            }
        } catch (Settings.SettingNotFoundException e) {
            DLog.log(e.getMessage());
        }
        return defTimeOut;
    }


    public int getScreenBrightnessAutoMode() {
        int brightnessMode = 0;
        try {
            brightnessMode = Settings.System.getInt(emGlobals.getmContext().getContentResolver(), "screen_brightness_mode");
        } catch (Exception var3) {
            DLog.log(var3.getMessage());
        }
        return brightnessMode;
    }

    public int getMinimumScreenBrightnessSetting() {
        try {
            Resources res = Resources.getSystem();
            int id = res.getIdentifier("config_screenBrightnessSettingMinimum", "integer", "android");
            if (id == 0) {
                id = res.getIdentifier("config_screenBrightnessDim", "integer", "android");
            }

            if (id != 0) {
                try {
                    return res.getInteger(id);
                } catch (Resources.NotFoundException var4) {
                }
            }

            return 0;
        } catch (Exception var5) {
            return 0;
        }
    }

    public int getMaximumScreenBrightnessSetting() {
        try {
            Resources res = Resources.getSystem();
            int id = res.getIdentifier("config_screenBrightnessSettingMaximum", "integer", "android");
            if (id != 0) {
                try {
                    return res.getInteger(id);
                } catch (Resources.NotFoundException var4) {
                }
            }

            return 255;
        } catch (Exception var5) {
            return 255;
        }
    }

    public float getScreenBrightnessValue() {
        float brightnessPercentage = 0.0F;

        try {
            int brightnessValue = Settings.System.getInt(emGlobals.getmContext().getContentResolver(), "screen_brightness", -1);
            String model = Build.MODEL;
            int minValue = this.getMinimumScreenBrightnessSetting();
            int maxValue = this.getMaximumScreenBrightnessSetting();
            if (model.equalsIgnoreCase("SC-05D")) {
                minValue = 30;
                maxValue = 205;
            }

            if (model.startsWith("SH") && model.contains("E")) {
                minValue = 30;
            }

            brightnessPercentage = (float) (brightnessValue - minValue) * 100.0F / (float) (maxValue - minValue);
        } catch (Exception var6) {
            DLog.log(var6.getMessage());
        }
        return brightnessPercentage;
    }


    public int getSettingValue(String settingKey) {
        int defValue = -1;
        try {
            defValue = Settings.System.getInt(emGlobals.getmContext().getContentResolver(), settingKey);
        } catch (Exception ex) {
            DLog.log(ex.getMessage());
        }
        DLog.log("setting Value : "+defValue);
        return defValue;
    }

    public boolean putSettingValue(String settingKey, int settingValue) {
        boolean isSettingSaved = false;
        if (settingValue != -1) {
            try {
                isSettingSaved = Settings.System.putInt(emGlobals.getmContext().getContentResolver(), settingKey, settingValue);
            } catch (Exception ex) {
                DLog.log("Exception " + ex.getMessage() + " Setting key and value : " + settingKey + " , " + settingValue);
            }
        }
        return isSettingSaved;
    }

    public static String collectSystemSettings(Context ctx) {
        StringBuilder result = new StringBuilder();
        for (Field key : Settings.System.class.getFields()) {
            if (!key.isAnnotationPresent(Deprecated.class) && key.getType() == String.class) {
                try {
                    String value = Settings.System.getString(ctx.getContentResolver(), (String) key.get(null));
                    if (value != null) {
                        result.append(key.getName()).append("=").append(value).append("\n");
                    }
                } catch (IllegalArgumentException e) {

                } catch (IllegalAccessException e2) {

                }
            }
        }
        return result.toString();
    }


    public static HashMap<String, String> collectSystemSettings() {
        HashMap<String, String> settings = new HashMap<>();
        for (Field key : Settings.System.class.getFields()) {
            if (!key.isAnnotationPresent(Deprecated.class) && key.getType() == String.class) {
                try {
                    String value = Settings.System.getString(emGlobals.getmContext().getContentResolver(), (String) key.get(null));
                    if (value != null) {
                        settings.put((String) key.get(null), value);
                    }
                } catch (Exception e) {
                    DLog.log(e);
                }
            }
        }
        return settings;
    }

    public static String collectSecureSettings(Context ctx) {
        StringBuilder result = new StringBuilder();
        for (Field key : Settings.Secure.class.getFields()) {
            if (!key.isAnnotationPresent(Deprecated.class) && key.getType() == String.class && isAuthorized(key)) {
                try {
                    String value = Settings.Secure.getString(ctx.getContentResolver(), (String) key.get(null));
                    if (value != null) {
                        result.append(key.getName()).append("=").append(value).append("\n");
                    }
                } catch (IllegalArgumentException e) {

                } catch (IllegalAccessException e2) {

                }
            }
        }
        return result.toString();
    }

    private static boolean isAuthorized(Field key) {
        if (key == null || key.getName().startsWith("WIFI_AP")) {
            return false;
        }
        return true;
    }

    public boolean setRingerMode(int ringerMode, int ringVolume) {
        try {
            AudioManager mobileMode = (AudioManager) emGlobals.getmContext().getSystemService(Context.AUDIO_SERVICE);
            mobileMode.setRingerMode(ringerMode);
            int maxVolume = mobileMode.getStreamMaxVolume(AudioManager.STREAM_RING);
            DLog.log("ringVolume=" + ringVolume);
            float value = ((float) maxVolume * ((float) ringVolume / 100));
            if (value > 0 && value < 1)
                mobileMode.setStreamVolume(AudioManager.STREAM_RING, 1, 0);
            else
                mobileMode.setStreamVolume(AudioManager.STREAM_RING, (int) ((float) maxVolume * ((float) ringVolume / 100)), 0);

        } catch (Exception e) {
            DLog.log("Exception in setRingerMode : "+e.getMessage());
            return false;
        }
        return true;
    }

    public int getRingerMode() {
        int ringMode = 0;
        try {
            AudioManager mobileMode = (AudioManager) emGlobals.getmContext().getSystemService(Context.AUDIO_SERVICE);
            ringMode = mobileMode.getRingerMode();
        } catch (Exception e) {
            DLog.log("Exception in getRingerMode : "+e.getMessage());
        }
        return ringMode;
    }

    public int getRingVolume() {
        int ringVolume = 0;
        try {
            AudioManager mobileMode = (AudioManager) emGlobals.getmContext().getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = mobileMode.getStreamMaxVolume(AudioManager.STREAM_RING);
            int currentVolume = mobileMode.getStreamVolume(AudioManager.STREAM_RING);
            DLog.log("currentVolume : " + currentVolume + " maxVolume : " + maxVolume);
            ringVolume = (int) (((float) currentVolume / (float) maxVolume) * 100);
            if (currentVolume != 0 && ringVolume == 0) {
                ringVolume = 1;
            }
            DLog.log("ringVolume : " + ringVolume);
        } catch (Exception e) {
            DLog.log("Exception in getRingVolume : "+e.getMessage());
        }
        return ringVolume;
    }

}
