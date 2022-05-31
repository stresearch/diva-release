package com.visym.collector.network;

import com.visym.collector.utils.Globals;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ConnectivityInterceptor implements Interceptor {



    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request authorizedRequest;
        String accessToken = Globals.getAccessToken();
        /*String authorizationToken = "asdf";

        if (Globals.getCount() > 2)
            authorizationToken = new SharedPref(context).getToken();*/

        if (accessToken != null) {
            authorizedRequest = originalRequest.newBuilder()
                .header("accessToken", accessToken)
                .build();
        } else {
            authorizedRequest = originalRequest.newBuilder()
                .build();
        }
        return chain.proceed(authorizedRequest);
    }
}
