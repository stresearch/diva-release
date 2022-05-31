package com.visym.collector.paypal;





import com.visym.collector.paypal.identitys.IdentityDataModel;
import com.visym.collector.paypal.tokens.AuthorizationCodeDataModel;

import java.util.HashMap;


import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

public interface PaypalApiInterface {

    @GET("userinfo?schema=paypalv1.1")
    Call<IdentityDataModel> getIdentityDataModel();

    @POST("token")
    Call<AuthorizationCodeDataModel> getAuthAndRefreshTokens(@Body RequestBody body);

    /*@POST("forgotPassword")
    Call<ForgotPassDataModel> getDataForgotPass(@Body HashMap<String, String> body);

    @POST("register")
    Call<SignupDataModel> getDataSignup(@Body HashMap<String, String> body);

    @POST("changePassword")
    Call<ChangepassDataModel> getDataChangePass(@Body HashMap<String, String> body, @HeaderMap HashMap<String, String> val);

    @GET("resendVerificationToken")
    Call<VerifyTokenDataModel> getDataVerifyToken(@HeaderMap HashMap<String, String> body);

    @GET("patient")
    //@GET("users/earlmegget21")
    Call<PatientListDataModel> getDataPatientList(@HeaderMap HashMap<String, String> body);
    //Call<PatientListDataModel> getDataPatientList();


    @GET("lookUp")
    Call<LookUpDataModel> getDataLookUp(@HeaderMap HashMap<String, String> body);
  *//*  @GET("find/mark")
    Call<LookUpDataModel> getDataLookUp();*//*

    @GET("image/getImages")
    Call<GetImageDataModel> getDataGetImaget(@HeaderMap HashMap<String, Object> body);


    @GET("technician")
    Call<GetTechDataModel> getDataGetTech(@Body HashMap<String, String> body);

    @POST("image/filter")
    Call<UpdateFilterDataModel> getDataUpdateFilter(@Body HashMap<String, String> body, @HeaderMap JsonObject jsonObject);


    @GET("patient/id")
    Call<GenPatientIdDataModel> getDataGenPatientId(@Body HashMap<String, String> body);

    @GET("image/id")
    Call<GenImageIdDataModel> getDataGenImageId(@Body HashMap<String, String> body);


    @POST("patient/update")
    Call<UpdatePatientDataModel> getDataUpdatePatient(@Body HashMap<String, String> body, @HeaderMap HashMap<String, String> body2);


    @POST("image/updateImage")
    Call<UpdateImageDataModel> getDataUpdateImage(@Body HashMap<String, String> body, @HeaderMap HashMap<String, String> body2);

    @POST("saveSettings")
    Call<SaveSettingDataModel> getDataSaveSetting(@Body HashMap<String, String> body, @HeaderMap JsonObject jsonObject);

    @GET("getSettings")
    Call<GetSettingDataModel> getDataGetSetting(@Body HashMap<String, String> body);

    @POST("emailVerification")
    Call<EmailVerifDataModel> getDataEmailVerif(@Body HashMap<String, String> body, @HeaderMap HashMap<String, String> body2);*/

}
