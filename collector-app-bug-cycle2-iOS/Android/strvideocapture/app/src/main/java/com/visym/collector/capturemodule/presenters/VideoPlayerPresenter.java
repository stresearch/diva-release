package com.visym.collector.capturemodule.presenters;

import android.util.Log;

import com.amazonaws.amplify.generated.graphql.InstanceByStrVideoIdQuery;
import com.amazonaws.amplify.generated.graphql.ListStrConsentQuestionnairesQuery;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.visym.collector.capturemodule.interactor.VideoPlayerInteractor;
import com.visym.collector.model.CollectionQuestionnaireResponse;
import com.visym.collector.model.Instance;
import com.visym.collector.model.CollectionQuestionnaire;
import com.visym.collector.model.NetworkCallback;
import com.visym.collector.model.RatingQuestionResponse;
import com.visym.collector.network.NetworkClientError;
import com.visym.collector.network.project.AwsS3Repository;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.AwsResponse;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.ErrorHandler;
import com.visym.collector.utils.Globals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class VideoPlayerPresenter implements VideoPlayerInteractor.VideoPlayerPresenter {

    private AwsS3Repository awsS3Repository;
    private VideoPlayerInteractor.VideoPlayerView view;

    public VideoPlayerPresenter(VideoPlayerInteractor.VideoPlayerView view) {
        this.view = view;
        this.awsS3Repository = new AwsS3Repository();
    }

    public void downloadJSONFile(String videoFilePath, boolean trainingModule) {
        awsS3Repository.downloadFile(videoFilePath, trainingModule, new NetworkCallback<AwsResponse>() {
            @Override
            public void onSuccess(AwsResponse response) {
                view.onFileDownload(response);
            }

            @Override
            public void onFailure(NetworkClientError error) {
                view.onFailure(ErrorHandler.getErrorMessage(error));
            }
        });
    }

    @Override
    public void getCollectionQuestionnaire() {
        Globals.getAppSyncClient().query(ListStrConsentQuestionnairesQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(questionnairesCallback);
    }

    @Override
    public void getActivityInstances(String videoId) {
        Globals.getAppSyncClient().query(InstanceByStrVideoIdQuery.builder()
                .video_id(videoId).build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(instancesCallback);
    }

    @Override
    public void updateQuestionnaireResponse(
            List<CollectionQuestionnaireResponse> questionnaireResponses, String currentInstanceId) {
        RatingQuestionResponse response = new RatingQuestionResponse();
        response.setID(currentInstanceId);
        response.setReviewer_ID(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_EMAIL));

        for (CollectionQuestionnaireResponse questionnaireResponse : questionnaireResponses) {
            String questionId = questionnaireResponse.getQuestionId();
            int questionResponse = questionnaireResponse.getQuestionResponse();
            switch (questionId){
                case "badalignment":
                    response.setBAD_Alignment(questionResponse);
                    break;
                case "badbox":
                    response.setBad_Label(questionResponse);
                    break;
                case "badbox_big":
                    response.setBad_Box_big(questionResponse);
                    break;
                case "badbox_small":
                    response.setBad_Box_Small(questionResponse);
                    break;
                case "badtiming":
                    response.setBad_Timing(questionResponse);
                    break;
                case "badviewpoint":
                    response.setBad_Viewpoint(questionResponse);
                    break;
                case "good":
                    response.setBad_Video(questionResponse);
                    break;
            }
        }
    }

    private GraphQLCall.Callback<ListStrConsentQuestionnairesQuery.Data> questionnairesCallback
            = new GraphQLCall.Callback<ListStrConsentQuestionnairesQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<ListStrConsentQuestionnairesQuery.Data> response) {
            if (!response.hasErrors()) {
                if (response.data() != null) {
                    ListStrConsentQuestionnairesQuery.ListStrConsentQuestionnaires questionnaires
                            = response.data().listStrConsentQuestionnaires();
                    List<CollectionQuestionnaire> questionnaireList = new ArrayList<>();
                    if (questionnaires != null) {
                        List<ListStrConsentQuestionnairesQuery.Item> items = questionnaires.items();
                        if (items != null && !items.isEmpty()) {
                            for (ListStrConsentQuestionnairesQuery.Item item : items) {
                                /*CollectionQuestionnaire questionnaire = new CollectionQuestionnaire();
                                questionnaire.setId(item.id());
                              *//*  questionnaire.setCollectionId(item.);
                                questionnaire.setStartFrame(item.Start_Frame());
                                questionnaire.setEndFrame(item.End_Frame());
                                questionnaire.setQuestion(item.Question());*//*
                                questionnaire.setUpdatedDate(item.updated_date());

                                questionnaireList.add(questionnaire);*/
                            }
                        }
                    }
                    view.onQuestionnaireResponse(questionnaireList);
                } else {
                    view.onQuestionnaireResponse(new ArrayList<>());
                }
            } else {
                // TODO parse errors and display message
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.d(" ", "onResponse: ");
        }
    };

    private GraphQLCall.Callback<InstanceByStrVideoIdQuery.Data> instancesCallback =
            new GraphQLCall.Callback<InstanceByStrVideoIdQuery.Data>() {
                @Override
                public void onResponse(@Nonnull Response<InstanceByStrVideoIdQuery.Data> response) {
                    if (!response.hasErrors()) {
                        if (response.data() != null) {
                            List<Instance> instances = new ArrayList<>();
//                            response.data().;
//                            if (instanceQueries != null){
//                                List<ListInstancessQuery.Item> items = instanceQueries.items();
//                                if (items != null && !items.isEmpty()) {
//                                    for (ListInstancessQuery.Item item : items) {
//                                        Instance instance = new Instance();
//                                        instance.setInstanceId(item.Instance_ID());
//                                        instance.setStartFrame(item.Start_Frame());
//
//                                        instances.add(instance);
//                                    }
//                                }
//                            }
                            view.onInstanceCallback(instances);
                        }else {
                            view.onInstanceCallback(new ArrayList<>());
                        }
                    }else {
                        // TODO parse errors and display message
                    }
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {

                }
            };
}