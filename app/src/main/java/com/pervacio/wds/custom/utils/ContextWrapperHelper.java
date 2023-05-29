package com.pervacio.wds.custom.utils;

/**
 * Context wrapper to support consistent localization in Android Nougat onwards devices.
 *
 * Reference: https://stackoverflow.com/questions/39705739/android-n-change-language-programmatically/40849142#40849142
 *
 * Created by Darpan Dodiya on 26-Sep-17.
 */

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;

import com.pervacio.wds.BuildConfig;
import com.pervacio.wds.app.DLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import static com.pervacio.wds.custom.utils.Constants.IS_MMDS;

public class ContextWrapperHelper extends ContextWrapper {

    public ContextWrapperHelper(Context base) {
        super(base);
    }

    @SuppressWarnings("deprecation")
    public static ContextWrapper wrap(Context context, String language) {
        Resources res = context.getResources();
        Configuration configuration = res.getConfiguration();
        ArrayList<String> supportedLanguages = new ArrayList<String>(Arrays.asList("en", "pl", "fr", "sk","es","pt","pt-rBR", "pt-rPT"));
        Locale sysLocale;
        Locale newLocale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sysLocale = getSystemLocale(configuration);
        } else {
            sysLocale = getSystemLocaleLegacy(configuration);
        }
        DLog.log("System Locale: " + sysLocale.getLanguage()+" Change request to "+language);
        if (IS_MMDS || !Constants.NEW_PLAYSTORE_FLOW) {
            try {
                if (TextUtils.isEmpty(language) && supportedLanguages.contains(sysLocale.getLanguage())) {
                    newLocale = sysLocale;
                } else if (supportedLanguages.contains(language)) {
                    newLocale = new Locale(language);
                } else {
                    newLocale = new Locale("en");
                }
            } catch (Exception e) {
                e.printStackTrace();
                newLocale = new Locale("en");
            }
        } else {
            try {
                if (supportedLanguages.contains(sysLocale.getLanguage())) {
                    newLocale = sysLocale;
                }  else {
                    newLocale = new Locale("en");
                }
            } catch (Exception e) {
                e.printStackTrace();
                newLocale = new Locale("en");
            }
        }

        DLog.log("Changing locale to: " + newLocale.getLanguage());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(newLocale);

            LocaleList localeList = new LocaleList(newLocale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);

            context = context.createConfigurationContext(configuration);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(newLocale);
            context = context.createConfigurationContext(configuration);

        } else {
            configuration.locale = newLocale;
            res.updateConfiguration(configuration, res.getDisplayMetrics());
        }

        return new ContextWrapperHelper(context);
    }

    @SuppressWarnings("deprecation")
    public static Locale getSystemLocaleLegacy(Configuration config){
        return config.locale;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static Locale getSystemLocale(Configuration config){
        return config.getLocales().get(0);
    }

    @SuppressWarnings("deprecation")
    public static void setSystemLocaleLegacy(Configuration config, Locale locale){
        config.locale = locale;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static void setSystemLocale(Configuration config, Locale locale){
        config.setLocale(locale);
    }
}
