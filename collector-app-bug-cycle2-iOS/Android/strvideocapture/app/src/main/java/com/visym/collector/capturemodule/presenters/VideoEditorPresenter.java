package com.visym.collector.capturemodule.presenters;

import android.text.TextUtils;
import android.util.Log;

import com.amazonaws.amplify.generated.graphql.StrCollectionsByCollectionIdQuery;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.visym.collector.capturemodule.IVideoEditModule;
import com.visym.collector.capturemodule.model.ActivityLabel;
import com.visym.collector.capturemodule.model.FrameJSON;
import com.visym.collector.model.NetworkCallback;
import com.visym.collector.network.NetworkClientError;
import com.visym.collector.network.project.AwsS3Repository;
import com.visym.collector.utils.AwsResponse;
import com.visym.collector.utils.ErrorHandler;
import com.visym.collector.utils.Globals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import javax.annotation.Nonnull;

import static com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers.NETWORK_ONLY;

public class VideoEditorPresenter implements IVideoEditModule.IVideoEditor {

    private IVideoEditModule.IVideoEditView mView;
    private AwsS3Repository awsS3Repository;

    @Override
    public void onViewAttached(IVideoEditModule.IVideoEditView view) {
        this.mView = view;
        awsS3Repository = new AwsS3Repository();
    }

    @Override
    public void getActivitiesForCollections(String collectionId) {
        Globals.mAWSAppSyncClient.query(StrCollectionsByCollectionIdQuery.builder()
                .collection_id(collectionId).build())
                .responseFetcher(NETWORK_ONLY)
                .enqueue(activityList);
    }

    @Override
    public void downloadJSONFile(String videoFilePath) {
        awsS3Repository.downloadFile(videoFilePath, false, new NetworkCallback<AwsResponse>() {
            @Override
            public void onSuccess(AwsResponse response) {
                if (mView != null) {
                    mView.onFileDownload(response);
                }
            }

            @Override
            public void onFailure(NetworkClientError error) {
                if (mView != null) {
                    mView.onFailure(ErrorHandler.getErrorMessage(error));
                }
            }
        });
    }

    @Override
    public void uploadSONFile(String fileName, String inputFilePath) {
        awsS3Repository.uploadFile(fileName + "," + inputFilePath, AwsResponse.FILE_TYPE_UPDATED_JSON,
                new NetworkCallback<AwsResponse>() {
                    @Override
                    public void onSuccess(AwsResponse response) {
                        if (mView != null){
                            mView.onFileDownload(response);
                        }
                    }

                    @Override
                    public void onFailure(NetworkClientError error) {
                        if (mView != null){
                            mView.onFailure(ErrorHandler.getErrorMessage(error));
                        }
                    }
                });
    }

    @Override
    public JSONArray getComputedActivityLabels(JSONArray sortedArray) {
        try {
            JSONArray jsonArray = new JSONArray();
            if (sortedArray.length() == 1){
                JSONObject currentJsonObject = sortedArray.getJSONObject(0);
                jsonArray.put(currentJsonObject);
            }else {
                for (int i = 0; i < sortedArray.length(); i++) {
                    JSONObject currentJsonObject = sortedArray.getJSONObject(i);
                    for (int j = i + 1; j < sortedArray.length(); j++) {
                        JSONObject nextJsonObject = sortedArray.getJSONObject(j);

                        if (currentJsonObject.getInt("endFrame") > nextJsonObject.getInt("startFrame")) {
                            if (currentJsonObject.getInt("endFrame") < nextJsonObject.getInt("endFrame")) {
                                nextJsonObject.put("startFrame", currentJsonObject.getInt("endFrame") + 1);
                                if (jsonArray.length() > 0) {
                                    JSONObject previousObject = jsonArray.getJSONObject(jsonArray.length() - 1);
                                    if (currentJsonObject.getInt("startFrame") > previousObject.getInt("startFrame") &&
                                            currentJsonObject.getInt("endFrame") > previousObject.getInt("endFrame")) {
                                        jsonArray.put(currentJsonObject);
                                    }
                                } else {
                                    jsonArray.put(currentJsonObject);
//                                    jsonArray.put(nextJsonObject);
                                }
                                i++;
                            } else if (currentJsonObject.getInt("endFrame") > nextJsonObject.getInt("endFrame")) {
//                                jsonArray.put(currentJsonObject);
//                                jsonArray.put(nextJsonObject);
                            }else {
                                if (jsonArray.length() > 0) {
                                    JSONObject previousObject = jsonArray.getJSONObject(jsonArray.length() - 1);
                                    if (currentJsonObject.getInt("startFrame") > previousObject.getInt("startFrame") &&
                                            currentJsonObject.getInt("endFrame") > previousObject.getInt("endFrame")) {
                                        jsonArray.put(currentJsonObject);
                                    }
                                } else {
                                    jsonArray.put(currentJsonObject);
                                }
                            }
                        } else {
                            if (jsonArray.length() > 0) {
                                JSONObject previousObject = jsonArray.getJSONObject(jsonArray.length() - 1);
                                if (currentJsonObject.getInt("startFrame") > previousObject.getInt("startFrame") &&
                                        currentJsonObject.getInt("endFrame") > previousObject.getInt("endFrame")) {
                                    jsonArray.put(currentJsonObject);
                                }
                            } else {
                                jsonArray.put(currentJsonObject);
                            }
                            break;
                        }
                    }

                    if (i == sortedArray.length() - 1) {
                        JSONObject previousJsonObject = jsonArray.getJSONObject(jsonArray.length() - 1);
                        if (!(previousJsonObject.getInt("endFrame") > currentJsonObject.getInt("startFrame"))) {
                            jsonArray.put(currentJsonObject);
                        }
                    }
                }
            }
            return jsonArray;
        }catch (Exception e){
            return null;
        }
    }

    @Override
    public int getActivityCount(String selectedItem, JSONArray sortedArray) throws JSONException {
        int activityCount = 0;
        for (int i = 0; i < sortedArray.length(); i++) {
            JSONObject jsonObject = sortedArray.getJSONObject(i);
            if (jsonObject.getString("activityLabel").equalsIgnoreCase(selectedItem)){
                activityCount++;
            }
        }
        return activityCount;
    }

    private final GraphQLCall.Callback<StrCollectionsByCollectionIdQuery.Data>
            activityList = new GraphQLCall.Callback<StrCollectionsByCollectionIdQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<StrCollectionsByCollectionIdQuery.Data> response) {
            String activities_name = null, activities_short_name = null, description = null;
            JSONArray activitiesArray = new JSONArray(); String[] objects = null;

            if (response.data() != null && response.data().strCollectionsByCollectionId() != null) {
                if (response.data().strCollectionsByCollectionId().items().size() > 0) {
                    Log.e(Globals.TAG, "onResponse: size " + response.data().strCollectionsByCollectionId().items().size());
                    if (response.data().strCollectionsByCollectionId().items().get(0).activities() != null) {
                        activities_name = response.data().strCollectionsByCollectionId().items().get(0).activities();
                    }
                    if (response.data().strCollectionsByCollectionId().items().get(0).activity_short_names() != null) {
                        activities_short_name = response.data().strCollectionsByCollectionId().items().get(0).activity_short_names();
                    }
                    description = response.data().strCollectionsByCollectionId().items().get(0).collection_description();
                    if (activities_short_name.contains(",")) {
                        String[] asn = activities_short_name.split(",");
                        for (int i = 0; i < asn.length; i++) {
                            activitiesArray.put(asn[i]);
                        }
                    } else {
                        activitiesArray.put(activities_short_name);
                    }

                    String objectsString = response.data().strCollectionsByCollectionId().items().get(0).objects_list();
                    if (!TextUtils.isEmpty(objectsString)){
                        objects = objectsString.split(",");
                    }
                }
            }
            if (mView != null) {
                mView.populateActivities(activitiesArray, objects, description);
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(Globals.TAG, "onFailure: "+e.toString());
            if (mView != null) {
                mView.populateActivities(new JSONArray(), new String[]{}, null);
            }
        }
    };

    @Override
    public void onViewDetached() {
        this.mView = null;
    }
}
