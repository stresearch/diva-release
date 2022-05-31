/**
 * @file SessionExpireModel.java
 * @brief This is model class for details screen, it will handle all business logic.
 * @author Shrikant
 * @date 15/04/2018
 */
package com.visym.collector.paypal.identitys;

import android.content.Context;

import com.visym.collector.paypal.ApiServiceGenerator;
import com.visym.collector.paypal.ConnectionDetector;
import com.visym.collector.paypal.PaypalApiInterface;
import com.visym.collector.utils.Constant;

import java.lang.annotation.Annotation;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;


public class IdentityModel implements IdentityContract.Model {
    @Override
    public void getIdentityDetails(OnFinishedListener onFinishedListener, String accesstoken, Context context) {
        final String BASEURL = Constant.BaseUrl2;
        final  ApiServiceGenerator retro = new ApiServiceGenerator(context, BASEURL, "identity",accesstoken);
        PaypalApiInterface create = retro.createService(context, PaypalApiInterface.class);

        final ConnectionDetector connectionDetector= new ConnectionDetector(context);

        create.getIdentityDataModel().enqueue(new Callback<IdentityDataModel>() {
            @Override
            public void onResponse(Call<IdentityDataModel> call, Response<IdentityDataModel> response) {

                if(response.code()==200){
                    IdentityDataModel authorizationCodeDataModel=response.body();
                    authorizationCodeDataModel.setApiStatus("success");

                    onFinishedListener.onIdentityFinished(authorizationCodeDataModel);
                }else{

                    if (response != null && !response.isSuccessful() && response.errorBody() != null) {
                        Converter<ResponseBody, IdentityDataModel> converter = retro.getRetrofitObj().responseBodyConverter(IdentityDataModel.class, new Annotation[0]);
                        IdentityDataModel errors =new IdentityDataModel() ;
                        try {
                            errors = converter.convert(response.errorBody());
                        } catch (Exception e) {
                        }

                        errors.setApiStatus("failure");
                        errors.setMessage(errors.getMessage());
                        onFinishedListener.onIdentityFailure(null,errors);

                    }else {
                        if(connectionDetector.isConnectingToInternet()){
                            IdentityDataModel authorizationCodeDataModel =new IdentityDataModel() ;
                            authorizationCodeDataModel.setApiStatus("failure");
                            authorizationCodeDataModel.setMessage("Unknown error occured");
                            onFinishedListener.onIdentityFailure(null,authorizationCodeDataModel);

                        }else {

                            IdentityDataModel changepassDataModel =new IdentityDataModel() ;
                            changepassDataModel.setApiStatus("failure");
                            changepassDataModel.setMessage("Please check your network");
                            onFinishedListener.onIdentityFailure(null,changepassDataModel);
                        }

                    }
                }


            }

            @Override
            public void onFailure(Call<IdentityDataModel> call, Throwable t) {
                if(connectionDetector.isConnectingToInternet()){
                    IdentityDataModel changepassDataModel =new IdentityDataModel() ;
                    changepassDataModel.setApiStatus("failure");
                    changepassDataModel.setMessage("Unknown error occured");
                    onFinishedListener.onIdentityFailure(t,changepassDataModel);

                }else {

                    IdentityDataModel changepassDataModel =new IdentityDataModel() ;
                    changepassDataModel.setApiStatus("failure");
                    changepassDataModel.setMessage("Please check your network");
                    onFinishedListener.onIdentityFailure(t,changepassDataModel);
                }
            }
        });


    }

}
