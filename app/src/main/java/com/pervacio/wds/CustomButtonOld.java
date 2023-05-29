package com.pervacio.wds;

 import android.content.Context;
 import android.graphics.Typeface;
 import android.util.AttributeSet;


 import androidx.appcompat.widget.AppCompatButton;

 /**
 * Created by Satya on 14-Mar-18.
 */

public class CustomButtonOld extends AppCompatButton {
    public CustomButtonOld(Context context) {
        super(context);
        try {
            init(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CustomButtonOld(Context context, AttributeSet attrs) {
        super(context, attrs);
        try {
            init(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CustomButtonOld(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        try {
            Typeface tf = Typeface.createFromAsset(context.getAssets(),
                    "fonts/aileron_regular.ttf");
            setTypeface(tf);


            /*if(BaseActivity.isIsAssistedApp())
                setEnabled(false);*/

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
