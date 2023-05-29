package com.pervacio.wds.custom.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.os.SystemClock;
import android.view.View;

import java.io.InputStream;

/**
 * Created by Pervacio on 10/4/2017.
 */

public class GIFMovieView extends View{
    private Movie mMovie;

    private long mMoviestart;

    public GIFMovieView(Context context, InputStream stream) {
        super(context);
        mMovie = Movie.decodeStream(stream);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        super.onDraw(canvas);
        final long now = SystemClock.uptimeMillis();
        int relTime=0;
        if (mMoviestart == 0) {
            mMoviestart = now;
        }
         if(mMovie.duration()>0)
        relTime = (int)((now - mMoviestart) % mMovie.duration());
        mMovie.setTime(relTime);
        canvas.scale((float)this.getWidth() / (float)mMovie.width(),(float)this.getHeight() / (float)mMovie.height());
        mMovie.draw(canvas, 0, 0);
        this.invalidate();
    }
}
