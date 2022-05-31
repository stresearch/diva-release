/**
 * @file SessionExpireModel.java
 * @brief This is model class for details screen, it will handle all business logic.
 * @author Shrikant
 * @date 15/04/2018
 */
package com.visym.collector.paypal.tokens;

import android.content.Context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.visym.collector.network.ApiInterface;
import com.visym.collector.paypal.ApiServiceGenerator;
import com.visym.collector.paypal.ConnectionDetector;
import com.visym.collector.paypal.PaypalApiInterface;
import com.visym.collector.utils.Constant;

import java.lang.annotation.Annotation;
import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;


public class AuthorizationCodeModel implements AuthorizationCodeContract.Model {
    @Override
    public void getTokenDetails(OnFinishedListener onFinishedListener, String authCode_refreshToken,String granttype,String key, Context context) {
        final String BASEURL = Constant.BaseUrl;
        final  ApiServiceGenerator retro = new ApiServiceGenerator(context, BASEURL, "token",null);
        PaypalApiInterface create = retro.createService(context, PaypalApiInterface.class);

        final ConnectionDetector connectionDetector= new ConnectionDetector(context);
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "grant_type="+granttype+"&"+key+"="+authCode_refreshToken);
        create.getAuthAndRefreshTokens(body).enqueue(new Callback<AuthorizationCodeDataModel>() {
            @Override
            public void onResponse(Call<AuthorizationCodeDataModel> call, Response<AuthorizationCodeDataModel> response) {

                if(response.code()==200){
                    AuthorizationCodeDataModel authorizationCodeDataModel=response.body();
                    authorizationCodeDataModel.setApiStatus("success");

                    onFinishedListener.onTokenFinished(authorizationCodeDataModel);
                }else{

                    if (response != null && !response.isSuccessful() && response.errorBody() != null) {
                        Converter<ResponseBody, AuthorizationCodeDataModel> converter = retro.getRetrofitObj().responseBodyConverter(AuthorizationCodeDataModel.class, new Annotation[0]);
                        AuthorizationCodeDataModel errors =new AuthorizationCodeDataModel() ;
                        try {
                            errors = converter.convert(response.errorBody());
                        } catch (Exception e) {
                        }

                        errors.setApiStatus("failure");
                        errors.setMessage(errors.getMessage());
                        onFinishedListener.onTokenFailure(null,errors);

                    }else {
                        if(connectionDetector.isConnectingToInternet()){
                            AuthorizationCodeDataModel authorizationCodeDataModel =new AuthorizationCodeDataModel() ;
                            authorizationCodeDataModel.setApiStatus("failure");
                            authorizationCodeDataModel.setMessage("Unknown error occured");
                            onFinishedListener.onTokenFailure(null,authorizationCodeDataModel);

                        }else {

                            AuthorizationCodeDataModel changepassDataModel =new AuthorizationCodeDataModel() ;
                            changepassDataModel.setApiStatus("failure");
                            changepassDataModel.setMessage("Please check your network");
                            onFinishedListener.onTokenFailure(null,changepassDataModel);
                        }

                    }
                }


            }

            @Override
            public void onFailure(Call<AuthorizationCodeDataModel> call, Throwable t) {
                if(connectionDetector.isConnectingToInternet()){
                    AuthorizationCodeDataModel changepassDataModel =new AuthorizationCodeDataModel() ;
                    changepassDataModel.setApiStatus("failure");
                    changepassDataModel.setMessage("Unknown error occured");
                    onFinishedListener.onTokenFailure(t,changepassDataModel);

                }else {

                    AuthorizationCodeDataModel changepassDataModel =new AuthorizationCodeDataModel() ;
                    changepassDataModel.setApiStatus("failure");
                    changepassDataModel.setMessage("Please check your network");
                    onFinishedListener.onTokenFailure(t,changepassDataModel);
                }
            }
        });


    }

}
