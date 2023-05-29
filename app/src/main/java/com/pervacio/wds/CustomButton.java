package com.pervacio.wds;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import androidx.appcompat.widget.AppCompatButton;

import com.pervacio.wds.custom.utils.DeviceInfo;

/**
 * Created by Pervacio
 */
public class CustomButton extends AppCompatButton {
    public CustomButton(Context context) {
        super(context);
        init(context);
    }

    public CustomButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context) {
        try {
            Typeface tf = Typeface.createFromAsset(context.getAssets(),
                    "fonts/"+context.getString(R.string.font_regular));
            setTypeface(tf);
            if (!DeviceInfo.getInstance().isTouchPhone()) {
                setOnFocusChangeListener(new OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            setTextColor(getResources().getColor(R.color.button_text_color));
                            setBackgroundColor(getResources().getColor(R.color.button_background));
                        } else {
                            setBackgroundColor(getResources().getColor(R.color.zxing_transparent));
                            setTextColor(getResources().getColor(R.color.wds_button_color));
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init(Context context, AttributeSet attrs) {
        try {
            TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.customfont);
            String fontFamily = context.getString(R.string.font_regular);
            final int n = a.getIndexCount();
            for (int i = 0; i < n; ++i) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.customfont_android_fontFamily) {
                    fontFamily = a.getString(attr);
                }
                if (Build.VERSION.SDK_INT < 21)
                    a.recycle();
            }

            if (!isInEditMode()) {
                try {
                    Typeface tf = Typeface.createFromAsset(
                            getContext().getAssets(), "fonts/" + fontFamily);
                    setTypeface(tf);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!DeviceInfo.getInstance().isTouchPhone()) {
                setOnFocusChangeListener(new OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            setTextColor(getResources().getColor(R.color.button_text_color));
                            setBackgroundColor(getResources().getColor(R.color.button_background));
                        } else {
                            setBackgroundColor(getResources().getColor(R.color.zxing_transparent));
                            setTextColor(getResources().getColor(R.color.wds_button_color));
                        }
                    }
                });
            }
            a.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
