package com.pervacio.wds.custom.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.TypedValue;



import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;

import com.pervacio.wds.R;

/**
 * Created by Satyanarayana Chidurala on 11/20/2015.
 */
public class ThemeUtil {

    private static String currentCustomer;

    public static void setCustomer(@NonNull String customer) {
        currentCustomer = customer;
    }


    public static void onActivityCreateSetTheme(ContextWrapper activity) {
        if(TextUtils.isEmpty(currentCustomer)){
            activity.setTheme(R.style.AppTheme);
            return;
        }


        switch (currentCustomer) {
            case "TelefonicaMexico":
            case "México":
                activity.setTheme(R.style.Theme_mexico);
                break;
            default:
                activity.setTheme(R.style.AppTheme);
                break;
        }
    }

    public static int getColorsFromAttrs(Context context, @AttrRes int attrFields) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attrFields, typedValue, true);

        return typedValue.data;
    }

    // ThemeUtil.setCustomer("TelefonicaMexico");

}
