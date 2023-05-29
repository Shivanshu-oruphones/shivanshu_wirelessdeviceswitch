package com.pervacio.wds;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * Created by Surya Polasapalli on 18-12-2017.
 */
public class CustomTextView extends AppCompatTextView {
    public CustomTextView(Context context) {
        super(context);
        try {
            init(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        try {
            init(context, attrs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CustomTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
   /* @TargetApi(21)
    public CustomTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }*/
    private void init(Context context) {
        try {
            Typeface tf = Typeface.createFromAsset(context.getAssets(),
                    "fonts/"+getContext().getString(R.string.font_regular));
            setTypeface(tf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init(Context context, AttributeSet attrs) {
        try {
            TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.customfont);
            String fontFamily = getContext().getString(R.string.font_regular);
            final int n = a.getIndexCount();
          /*  int cf = a.getInteger(R.styleable.customfont_android_fontFamily, 0);
            fontFamily= a.getString(cf);*/

            for (int i = 0; i < n; ++i) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.customfont_android_fontFamily) {
                    fontFamily = a.getString(attr);
                }
                if(Build.VERSION.SDK_INT < 21)
                    a.recycle();
            }

            if (!isInEditMode()) {
                try {
                    Typeface tf = Typeface.createFromAsset(
                            getContext().getAssets(), "fonts/"+fontFamily);
                    setTypeface(tf);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            a.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
