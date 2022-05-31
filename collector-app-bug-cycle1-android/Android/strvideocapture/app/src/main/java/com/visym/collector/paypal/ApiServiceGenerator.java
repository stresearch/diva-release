package com.visym.collector.paypal;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiServiceGenerator {

    private android.content.Context Context;
    private String url = "";
    private String flag = "";
    private Retrofit retrofit;
    private String contentType = "";
    private String authorization = "";


    public ApiServiceGenerator(android.content.Context mContext, String urlToPass, String flag,String datafetcher) {
        this.Context = mContext;
        url = urlToPass;
        flag = flag;
        if(flag.equalsIgnoreCase("identity")){
            contentType="application/json";
             authorization="Bearer "+datafetcher;
        }else {
            contentType="application/x-www-form-urlencoded";
            authorization="Basic QVVwa0RGcHNNVUZob1JBcmZWY0g3RWcxOUNjUnNfVy0yZ2pKMGdwQTRLbm1pMi13MmxTamgwWVdPdm1qZ0V6aWptX1p0aHlqSlB2SG93eGE6RU1TMm1vYm9fQmdWclpVaHBvZmQ0N0ZqTWMwTE43TmxjaURfS0o5UmdXdFRZM2hRTTdqOEVmU1Y2c2FLa0lVdXpKcThkSkFCUV91dC1sTHE=";
        }
    }


    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Interceptor.Chain chain) throws IOException {

                    Request authorisedRequest = chain.request().newBuilder()

                            .addHeader("Content-Type", contentType)
                            .addHeader("Authorization",authorization)
                            .build();
                    return chain.proceed(authorisedRequest);
                }
            })
            .connectTimeout(2, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build();

  /*  OkHttpClient client = new OkHttpClient().newBuilder()
            .build();
    MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
    RequestBody body = RequestBody.create(mediaType, "grant_type=authorization_code&code=C21AAH1tnijhe5LvphApDuWOlIg2nq_Ej_H-UGSWI3luLKEz4X9xM71V1iLAILKsFrWAQNdZSzQKNnSE1kIEa8qYBWFVkwiXw");
    Request request = new Request.Builder()
            .url("https://api.sandbox.paypal.com/v1/oauth2/token")
            .method("POST", body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Authorization", "Basic QVZXVXl4OVlKd3Z0bW1lZ0VTSG1LemdtT2xjZjhjOHdWWjMtSHRCSllyTjVvV3JYcC1CT0lYZkpqSWJMdl8yY2Q3SmZfY3lfWFFSNDkwejQ6RUNad2ZOMDEtQzdZR0RWX0ZFenlnNU4tZ3dTTXJDZ3dHd2p1VkJ3TzNrSkVnYVlPSHI1bDU4QzJBLVI4YmV6SlRUcERKX2lfc2l4a0xWdjQ=")
            .build();
    Response response = client.newCall(request).execute();*/

    public <S> S createService(android.content.Context context, Class<S> serviceClass) {
        Context = context;
        Retrofit.Builder builder =
                new Retrofit.Builder()
                        .baseUrl(url)
                        .client(httpClient)
                        .addConverterFactory(GsonConverterFactory.create());

        retrofit = builder.build();
        return retrofit.create(serviceClass);
    }

    public Retrofit getRetrofitObj()
    {
        return retrofit;
    }
}
