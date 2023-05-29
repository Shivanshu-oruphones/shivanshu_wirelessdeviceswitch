package com.pervacio.wds.custom.appsinstall;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

/**
 * Created by Satyanarayana Chidurala on 22/07/2021.
 */

public class CustomViewPager extends ViewPager {
    private boolean enabled;

    public CustomViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.enabled) {
            return super.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.enabled) {
            return super.onInterceptTouchEvent(event);
        }

        return false;
    }
// to enable swipe set setswipeenabed to true.
    public void setSwipeEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

