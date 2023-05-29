package com.pervacio.wds.custom;


import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.pervacio.wds.custom.models.EDeviceLoggingInfo;
import com.pervacio.wds.custom.models.EmailServiceDto;
import com.pervacio.wds.custom.models.FeatureConfig;

import org.json.JSONObject;

import okio.Timeout;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;


public interface ApiInterface {

//
    @POST("/pvco-devicedb-service/api/devicedb/getAppConfig")
    Call<FeatureConfig> getConfigDetails(@Header("Authorization") String authkey, @Body JsonObject jsonObject);

    @POST("/DeviceSwitchLogging/gpsLogin")
    Call<JsonObject> validateGpsLogin(@Body EDeviceLoggingInfo requestBody);

    @POST("/DeviceSwitchLogging/rap")
    Call<JsonObject> reportAProblemService(@Header("Authorization") String authkey,@Body EmailServiceDto emailServiceDto);


    @POST("/DeviceSwitchLogging/authenticateUser")
    Call<JsonObject> validateUserLogin(@Body JsonObject requestBody);

    @GET("/pervacioappservices/api/data/getStoreInfo/{storeId}/{product}")
    Call<JsonObject> getStoreConfig(@Header("Authorization") String authkey, @Path("storeId") String storeId, @Path("product") String product);
    @POST("/api/device/emailSummaryPDF")
    Call<JsonPrimitive> sendSummaryEmail(@Header("Authorization") String authkey, @Body JsonObject requestBody);
}

