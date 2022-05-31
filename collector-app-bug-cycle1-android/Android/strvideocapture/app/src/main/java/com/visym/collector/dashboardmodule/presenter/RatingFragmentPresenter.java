package com.visym.collector.dashboardmodule.presenter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.amazonaws.amplify.generated.graphql.StrCollectionsByCollectionIdQuery;
import com.amazonaws.amplify.generated.graphql.StrRatingVideoSortByAssignedDateQuery;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.google.android.material.snackbar.Snackbar;
import com.visym.collector.R;
import com.visym.collector.dashboardmodule.IDashboardModule.IRatingFragmentPresenter;
import com.visym.collector.dashboardmodule.IDashboardModule.IRatingFragmentView;
import com.visym.collector.model.CollectionModel;
import com.visym.collector.network.ConnectivityReceiver;

import com.visym.collector.usermodule.view.LoginActivity;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;

import type.ModelSortDirection;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;
import static com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers.NETWORK_ONLY;

public class RatingFragmentPresenter implements IRatingFragmentPresenter{

    private JSONArray videoData;
    private String nextToken;
    private boolean val;
    private IRatingFragmentView mView;
    private final GraphQLCall.Callback<StrCollectionsByCollectionIdQuery.Data>
            collectionDetailQuery = new GraphQLCall.Callback<StrCollectionsByCollectionIdQuery.Data>() {

        @Override
        public void onResponse(@Nonnull Response<StrCollectionsByCollectionIdQuery.Data> response) {
            if (mView != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        CollectionModel collection = null;
                        if (response.data() != null && response.data().strCollectionsByCollectionId() != null) {
                            if (response.data().strCollectionsByCollectionId().items().size() > 0) {
                                StrCollectionsByCollectionIdQuery.Item item = response.data().strCollectionsByCollectionId().items().get(0);
                                collection = new CollectionModel();
                                collection.setActivityShortNames(item.activity_short_names());
                                collection.setCollectionDescription(item.collection_description());
                                collection.setObjectList(item.objects_list());
                                collection.setDefaultObject(item.default_object());
                            }
                        }
                        mView.onCollectionDetailDownload(collection);
                    }
                });
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            if (mView != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mView.onCollectionDetailDownload(null);
                    }
                });
            }
        }
    };
    private Context mContext;
    private boolean isLoadMore = false;
    private AppSharedPreference preference;
    private GraphQLCall.Callback<StrRatingVideoSortByAssignedDateQuery.Data> ratingVideos = new GraphQLCall.Callback<StrRatingVideoSortByAssignedDateQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<StrRatingVideoSortByAssignedDateQuery.Data> response) {
            if (response != null && response.data() != null && response.data().strRatingVideoSortByAssignedDate().items().size() > 0) {
                videoData = new JSONArray();
                try {
                    for (int i = 0; i < response.data().strRatingVideoSortByAssignedDate().items().size(); i++) {
                        JSONObject itemObject = new JSONObject();
                        itemObject.put("annotation_file_path", response.data().strRatingVideoSortByAssignedDate().items().get(i).annotation_file_path());
                        itemObject.put("collection_id", response.data().strRatingVideoSortByAssignedDate().items().get(i).collection_id());
                        itemObject.put("collection_name", response.data().strRatingVideoSortByAssignedDate().items().get(i).collection_name());
                        itemObject.put("collector_id", response.data().strRatingVideoSortByAssignedDate().items().get(i).collector_id());
                        itemObject.put("duration", response.data().strRatingVideoSortByAssignedDate().items().get(i).duration());
                        itemObject.put("program_id", response.data().strRatingVideoSortByAssignedDate().items().get(i).program_id());
                        itemObject.put("program_name", response.data().strRatingVideoSortByAssignedDate().items().get(i).program_name());
                        itemObject.put("project_id", response.data().strRatingVideoSortByAssignedDate().items().get(i).project_id());
                        itemObject.put("project_name", response.data().strRatingVideoSortByAssignedDate().items().get(i).project_name());
                        itemObject.put("review_status", response.data().strRatingVideoSortByAssignedDate().items().get(i).review_status());
                        itemObject.put("thumbnail", response.data().strRatingVideoSortByAssignedDate().items().get(i).thumbnail());
                        itemObject.put("thumbnail_small", response.data().strRatingVideoSortByAssignedDate().items().get(i).thumbnail_small());
                        itemObject.put("uploaded_date", response.data().strRatingVideoSortByAssignedDate().items().get(i).uploaded_date());
                        itemObject.put("raw_video_file_path", response.data().strRatingVideoSortByAssignedDate().items().get(i).video_file_path());
                        itemObject.put("video_id", response.data().strRatingVideoSortByAssignedDate().items().get(i).video_id());
                        itemObject.put("week", response.data().strRatingVideoSortByAssignedDate().items().get(i).week());
                        String jsonString = response.data().strRatingVideoSortByAssignedDate().items().get(i).instance_ids().replace("=", ":");
                        JSONArray instanceObject = new JSONArray(quote(jsonString));
                        itemObject.put("instance_ids", instanceObject);
                        videoData.put(itemObject);
                    }
                } catch (JSONException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mView.handleError();
                        }
                    });

                }
                if (response.data().strRatingVideoSortByAssignedDate().nextToken() != null) {
                    nextToken = response.data().strRatingVideoSortByAssignedDate().nextToken();
                } else {
                    nextToken = null;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isLoadMore) {
                            mView.handleDataAndInitializeAdapter(videoData);
                        } else {
                            isLoadMore = false;
                            mView.handleLoadMoreDataAndInitializeAdapter(videoData);
                        }
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mView.handleDataNull();
                    }
                });

            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (e instanceof ApolloHttpException && ((ApolloHttpException) e).code() == 401){
                        Intent intent = new Intent(mContext, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        mContext.startActivity(intent);
                        mView.finish();
                    }else {
                        if (Globals.isShowingLoader2()) {
                            Globals.dismissLoading2();
                        }
                        if (Globals.isShowingLoader()) {
                            Globals.dismissLoading();
                        }
                        Globals.showSnackBar("No rating videos available!",
                                mContext, Snackbar.LENGTH_LONG);
                    }
                }
            });
        }
    };

    public static String quote(String s) {
        String[] strings = s.replace("[", "").replace("]", "").split(",");
        StringBuilder arrayString = new StringBuilder();
        String finalString = "";
        for (int i = 0; i < strings.length; i++) {
            if (strings[i].contains("{")) {
                String s1 = strings[i].replace("{", "");
                String[] regex = s1.split(":");
                strings[i] = "{" +
                        '\"' +
                        regex[0].trim() +
                        '\"' +
                        ":" +
                        '\"' +
                        regex[1].trim() +
                        '\"';
            } else if (strings[i].contains("}")) {
                String s2 = strings[i].replace("}", "");
                String[] regex = s2.split(":");
                if (i != (strings.length - 1)) {
                    strings[i] = '\"' +
                            regex[0].trim() +
                            '\"' +
                            ":" +
                            '\"' +
                            regex[1].trim() +
                            '\"' +
                            '}';
                } else {
                    strings[i] = '\"' +
                            regex[0].trim() +
                            '\"' +
                            ":" +
                            '\"' +
                            regex[1].trim() +
                            '\"' +
                            '}';
                }
            } else {
                String s3 = strings[i].replace("}", "");
                String[] regex = s3.split(":");

                if (regex[0].contains("\"")) {
                    String nomalC0 = regex[1].replaceAll("\"", "\\\\\"").trim();
                    strings[i] = '\"' +
                            nomalC0 +
                            '\"' +
                            ":" +
                            '\"' +
                            regex[1].trim() +
                            '\"';

                } else if (regex.length > 1 && (regex[1].contains("\""))) {
                    String nomalC1 = regex[1].replaceAll("\"", "\\\\\"").trim();
                    strings[i] = '\"' +
                            regex[0].trim() +
                            '\"' +
                            ":" +
                            '\"' +
                            nomalC1 +
                            '\"';
                } else if (s3.contains("video_uploaded_date:")){
                    strings[i] = '\"' +
                            regex[0].trim() +
                            '\"' +
                            ":" +
                            '\"' +
                            s3.replace("video_uploaded_date:", "").trim() +
                            '\"';
                }else   {
                    strings[i] = '\"' +
                            regex[0].trim() +
                            '\"' +
                            ":" +
                            '\"';
                    if (regex.length > 1) {
                        strings[i] +=regex[1].trim() + '\"';   // JEBYRNE
                    }
                }

            }
            if (i != (strings.length - 1)) {
                arrayString.append(strings[i]).append(',');
            } else {
                arrayString.append(strings[i]);
            }

        }

        finalString = "[" + arrayString + "]";
        return finalString;
    }

    @Override
    public void onViewAttached(IRatingFragmentView view, Context context) {
        this.mView = view;
        this.mContext = context;
        preference = AppSharedPreference.getInstance();
    }

    @Override
    public void onViewDetached() {
        this.mView = null;
    }

    @Override
    public void getUserRatingsVideo(boolean check) {
        if (ConnectivityReceiver.isConnected()) {
            val = check;
            if (!check) {
                Globals.showLoading(mContext);
            } else {
                Globals.showLoading2(mContext);
            }
            Globals.mAWSAppSyncClient.query(StrRatingVideoSortByAssignedDateQuery.builder()
                    .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY)).
                            sortDirection(ModelSortDirection.DESC)
                    .build()).responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                    .enqueue(ratingVideos);
        } else {
            Globals.showSnackBar(mContext.getResources().getString(R.string.noInternet), mContext, Snackbar.LENGTH_SHORT);
        }
    }


    @Override
    public void getLoadMoreDatas(boolean check) {
        if (ConnectivityReceiver.isConnected()) {
            if (nextToken != null) {
                val = check;
                if (!check) {
                    Globals.showLoading(mContext);
                    Globals.dismissLoading2();
                } else {
                    Globals.showLoading2(mContext);
                }
                isLoadMore = true;
                Globals.mAWSAppSyncClient.query(StrRatingVideoSortByAssignedDateQuery.builder()
                        .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY)).
                                sortDirection(ModelSortDirection.DESC)
                        .nextToken(nextToken)
                        .build()).responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                        .enqueue(ratingVideos);
            }
        } else {
            Globals.showSnackBar(mContext.getResources().getString(R.string.noInternet), mContext, Snackbar.LENGTH_LONG);
        }
    }

    @Override
    public void getCollectionDetails(String collectionId) {
        Globals.mAWSAppSyncClient.query(StrCollectionsByCollectionIdQuery.builder()
                .collection_id(collectionId).build())
                .responseFetcher(NETWORK_ONLY)
                .enqueue(collectionDetailQuery);
    }

}
