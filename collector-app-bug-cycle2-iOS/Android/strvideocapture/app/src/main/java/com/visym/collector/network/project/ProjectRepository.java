package com.visym.collector.network.project;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.visym.collector.capturemodule.model.FrameJSON;
import com.visym.collector.capturemodule.model.Questionnaire;
import com.visym.collector.capturemodule.model.QuestionnaireResponseWrapper;
import com.visym.collector.model.NetworkCallback;
import com.visym.collector.network.ApiClient;
import com.visym.collector.network.ApiInterface;
import com.visym.collector.network.NetworkClientError;
import com.visym.collector.network.NetworkRequest;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;


public class ProjectRepository {

    private ApiInterface apiInterface;
    private NetworkRequest networkRequest;

    public ProjectRepository() {
        apiInterface = ApiClient.buildService();
        networkRequest = new NetworkRequest();
    }

    public void getCollectionQuestionnaire(String projectId, String collectionId, NetworkCallback<List<Questionnaire>> callback) {
        Call<QuestionnaireResponseWrapper> questionnaire = apiInterface.getQuestionnaire(projectId, collectionId);
        networkRequest.requestCall(questionnaire, new NetworkCallback<QuestionnaireResponseWrapper>() {

            @Override
            public void onSuccess(QuestionnaireResponseWrapper response) {
                callback.onSuccess(response.getContent());
            }

            @Override
            public void onFailure(NetworkClientError response) {
                callback.onFailure(response);
            }
        });
    }

    public void getCollectionDetails(String projectId, String collectionId) {

    }
}
