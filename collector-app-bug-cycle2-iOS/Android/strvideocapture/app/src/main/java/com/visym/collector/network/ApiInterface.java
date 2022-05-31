package com.visym.collector.network;

import com.google.gson.JsonObject;
import com.visym.collector.capturemodule.model.QuestionnaireResponseWrapper;
import com.visym.collector.model.UserModel;


import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;
import rx.Observable;
import rx.Single;

public interface ApiInterface {


    @POST("user/signin")
    Observable<JsonObject> login(@Body RequestBody requestBody);

    @POST("user/changePassword")
    Observable<JsonObject> changePassword(@Header("userId") String userId, @Body RequestBody body);


    @GET("users/refreshToken")
    Single<ServerResponse<UserModel>>  getRefreshToken(@Header("refreshToken") String refreshToken,@Header("userId") String userId);

    @GET("user/profile")
    Observable<JsonObject> getUserProfile(@Header("userId") String userId);

    @POST("user/profile")
    Observable<JsonObject> updateUserProfile(@Header("userId") String userId, @Body RequestBody body);

    // Logout is pending
    @DELETE("user/logout")
    Observable<JsonObject> signOutUser(@Header("userId") String userId);

    @DELETE("user/revokeConsent")
    Observable<JsonObject> revokeUserConsent(@Header("userId") String userId);

    @POST("user/changeUsername")
    Observable<JsonObject> changeUserEmail(@Header("userId") String userId,@Body RequestBody requestBody);

    @GET("strfunctionality/galleryVideos")
    Observable<JsonObject> getGalleryVideos();

    @GET("user/videos")
    Observable<JsonObject> getUserVideos(@Header("userId") String userId);

    @GET("strfunctionality/projects")
    Observable<JsonObject> getProjects();


    @GET("strfunctionality/project/collections")
    Observable<JsonObject> getProjectCollections(
        @Header("projectId") String projectId);


    @GET("user/consent")
    Observable<JsonObject> getUserConsent(@Header("email") String email,
        @Header("userId") String userId);


    @GET("/strfunctionality/consent")
    Observable<JsonObject> getConsent(@Header("version") String version);

    @Headers({
        "Content-type: application/json"
    })
    @POST("user/updateConsent")
    Observable<JsonObject> updateUserConsent(@Header("userId") String userId,@Body RequestBody body);

    @POST("/strfunctionality/saveVideo")
    Observable<JsonObject> saveVideo(@Body RequestBody body);

    @GET("/strfunctionality/getQuestionnaire")
    Call<QuestionnaireResponseWrapper> getQuestionnaire(@Header("projectId") String projectId, @Header("collectionId") String collectionID);

    @POST("/user/saveQuestionnaire")
    Observable<JsonObject> saveUserQuestionnaire(@Header("projectId") String projectId,@Header("collectionId") String collectionId,
        @Body RequestBody questionnaireBody);

    @GET
    Call<ResponseBody> downloadJsonFrames(@Url String jsonUrl);
}
