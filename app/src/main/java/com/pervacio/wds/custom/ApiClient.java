package com.pervacio.wds.custom;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pervacio.wds.app.DLog;
import com.pervacio.wds.custom.utils.Constants;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class ApiClient {

   // public static final String BASE_URL = Constants.SERVER_ADDRESS;
    private static Retrofit retrofit = null;
    private static OkHttpClient okHttpClient;


    public static Retrofit getClient() {
        //if (retrofit==null) {
            if(okHttpClient == null){
                initOkHttp();;
            }
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(60,TimeUnit.SECONDS);
            builder.readTimeout(6,TimeUnit.SECONDS);
            //final OkHttpClient okHttpClient = new OkHttpClient().newBuilder().connectTimeout(60,TimeUnit.SECONDS).readTimeout(60,TimeUnit.SECONDS).writeTimeout(60,TimeUnit.SECONDS);
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        DLog.log("Server URL "+Constants.SERVER_ADDRESS);
            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.SERVER_ADDRESS)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        //}
        return retrofit;
    }

    private static void initOkHttp() {
        OkHttpClient.Builder httpClient = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS);

        httpClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder()
                        .addHeader("Accept", "application/json")
                        .addHeader("Request-Type", "Android")
                        .addHeader("Content-Type", "application/json");

                Request request = requestBuilder.build();
                try {
                    return chain.proceed(request);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

        okHttpClient = httpClient.build();
    }



}
