package com.visym.collector.network;

import com.visym.collector.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    public static String URL = "https://us-central1-fakestrapi.cloudfunctions.net/";
    private static Retrofit retrofit;


    public static ApiInterface buildService() {
        OkHttpClient.Builder okHttpClient;

        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(1);
        dispatcher.setMaxRequestsPerHost(1);
        okHttpClient = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .addInterceptor(new ConnectivityInterceptor())
                //.authenticator(new TokenAuthenticator())
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS);
        if (BuildConfig.DEBUG) {
            //Set logging of server response and request
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            okHttpClient.addInterceptor(logging);
        }
        retrofit = new Retrofit.Builder()
                .baseUrl(URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(okHttpClient.build())
                .build();

        return retrofit.create(ApiInterface.class);
    }

    public static Retrofit getRetrofit() {
        return retrofit;
    }
}
