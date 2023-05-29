package com.pervacio.wds.custom.utils;

/**
 * Created by Pervacio on 09-11-2016.
 */
import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.pervacio.wds.R;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.ui.EasyMigrateActivity;
import com.pervacio.wds.custom.SplashActivity;
import com.pervacio.wds.custom.StoreValidationActivity;

/**
 * Created by Pervacio on 09-11-2016.
 */


public class startLocationAlert implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final String TAG = "LOCATION_ALERT";
    Activity context;
    public static final int REQUEST_CHECK_SETTINGS = 0x1;
    GoogleApiClient googleApiClient;

    public startLocationAlert(Activity context) {
        this.context = context;
        googleApiClient = getInstance();
        if (googleApiClient != null) {
            //googleApiClient.connect();
            settingsrequest();
            googleApiClient.connect();
        }
    }

    public GoogleApiClient getInstance() {
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(context).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        return mGoogleApiClient;
    }

    public void settingsrequest() {

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true); //this is the key ingredient

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                DLog.log("onResult "+status.getStatusCode());
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        DLog.log("Button Clicked");
                        try {
                            if(context instanceof StoreValidationActivity) {
                                StoreValidationActivity callBack = (StoreValidationActivity) context;
                                callBack.gpsLocationRequested(true);
                            } else if(context instanceof SplashActivity) {
                                SplashActivity sa = (SplashActivity) context;
                                sa.locationAlertResultCallback();
                            } else if(context instanceof EasyMigrateActivity) {
                                EasyMigrateActivity ema = (EasyMigrateActivity) context;
                                ema.locationAlertResultCallback();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        // Log.e("Application","Button Clicked1");
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(context, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                            DLog.log(e);
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        //Log.e("Application","Button Clicked2");
                        Toast.makeText(context, "Location is Enabled", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Log.d(TAG, "onConnected : ");
        try {
            if(context instanceof StoreValidationActivity) {
                StoreValidationActivity callBack = (StoreValidationActivity) context;
                callBack.gpsLocationRequested(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Log.d(TAG, "onConnectionSuspended : " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        String failReason;
        DLog.log("GPS : onConnectionFailed: " + connectionResult.getErrorCode());
        if (connectionResult.getErrorCode() == 2) {
            failReason = context.getResources().getString(R.string.str_google_services_update);
        } else {
            failReason = context.getResources().getString(R.string.str_google_services_problem);
        }
        try {
            if(context instanceof StoreValidationActivity) {
                StoreValidationActivity callBack = (StoreValidationActivity) context;
                callBack.locationValidation(false, failReason);
            } else if(context instanceof EasyMigrateActivity) {
                EasyMigrateActivity ema = (EasyMigrateActivity) context;
                ema.locationAlertResultCallback();
            } else if(context instanceof SplashActivity) {
                SplashActivity ema = (SplashActivity) context;
                ema.locationAlertResultCallback();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}