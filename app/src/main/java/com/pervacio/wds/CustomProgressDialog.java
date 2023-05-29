package com.pervacio.wds;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;




public class CustomProgressDialog {
    private ProgressDialog progressDialog;
    private String title, message;
    private Context mContext;

    public CustomProgressDialog(Activity activity) {
        mContext = activity;
        progressDialog = new ProgressDialog(activity, AlertDialog.THEME_HOLO_LIGHT);
        setInfo(R.string.connecting_to_server, R.string.wds_please_wait);
        progressDialog.setCancelable(false);
    }



    public CustomProgressDialog setInfo(String title, String message) {
        this.title = title;
        this.message = message;
        return this;
    }

//    public CustomProgressDialog setInfo(@StringRes int title, @StringRes int message) {
    public CustomProgressDialog setInfo( int title,  int message) {
        this.title = mContext.getString(title);
        this.message = mContext.getString(message);
        return this;
    }

    public boolean isShowing(){
        return progressDialog != null && progressDialog.isShowing();
    }

    public void show() {
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    public void hide() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
