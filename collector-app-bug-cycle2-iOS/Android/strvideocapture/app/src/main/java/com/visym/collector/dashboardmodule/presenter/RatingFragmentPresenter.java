package com.visym.collector.dashboardmodule.presenter;

import android.content.Context;
import android.util.Log;

import com.amazonaws.amplify.generated.graphql.StrRatingVideoSortByAssignedDateQuery;
import com.amazonaws.amplify.generated.graphql.StrVideosByQueryAttributeQuery;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.material.snackbar.Snackbar;
import com.visym.collector.R;
import com.visym.collector.dashboardmodule.IDashboardModule.IRatingFragmentPresenter;
import com.visym.collector.dashboardmodule.IDashboardModule.IRatingFragmentView;
import com.visym.collector.network.ConnectivityReceiver;
import com.visym.collector.network.IErrorInterator;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;

import type.ModelSortDirection;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

public class RatingFragmentPresenter implements IRatingFragmentPresenter, IErrorInterator {

    private JSONArray videoData;
    private String nextToken;
    private boolean val;
    private IRatingFragmentView mView;
    private Context mContext;
    private boolean isLoadMore = false;
    private AppSharedPreference preference;
    private GraphQLCall.Callback<StrRatingVideoSortByAssignedDateQuery.Data> ratingVideos = new GraphQLCall.Callback<StrRatingVideoSortByAssignedDateQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<StrRatingVideoSortByAssignedDateQuery.Data> response) {
            if (response != null && response.data() != null && response.data().strRatingVideoSortByAssignedDate() != null) {
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
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
        }
    };

    public static String quote(String s) {
        String[] strings = s.replace("[", "").replace("]", "").split(",");
        String arrayString = "", finalString = "";
        for (int i = 0; i < strings.length; i++) {
            Log.e(Globals.TAG, "quote: strings are before " + strings[i]);
            if (strings[i].contains("{")) {
                String s1 = strings[i].replace("{", "");
                String[] regex = s1.split(":");
                strings[i] = new StringBuilder()
                        .append("{")
                        .append('\"')
                        .append(regex[0].trim())
                        .append('\"')
                        .append(":")
                        .append('\"')
                        .append(regex[1].trim())
                        .append('\"')
                        .toString();
            } else if (strings[i].contains("}")) {
                String s2 = strings[i].replace("}", "");
                String[] regex = s2.split(":");
                if (i != (strings.length - 1)) {
                    strings[i] = new StringBuilder()
                            .append('\"')
                            .append(regex[0].trim())
                            .append('\"')
                            .append(":")
                            .append('\"')
                            .append(regex[1].trim())
                            .append('\"')
                            .append('}')
                            .toString();
                } else {
                    strings[i] = new StringBuilder()
                            .append('\"')
                            .append(regex[0].trim())
                            .append('\"')
                            .append(":")
                            .append('\"')
                            .append(regex[1].trim())
                            .append('\"')
                            .append('}')
                            .toString();
                }
            } else {
                String s3 = strings[i].replace("}", "");
                String[] regex = s3.split(":");
                strings[i] = new StringBuilder()
                        .append('\"')
                        .append(regex[0].trim())
                        .append('\"')
                        .append(":")
                        .append('\"')
                        .append(regex[1].trim())
                        .append('\"')
                        .toString();
            }
            if (i != (strings.length - 1)) {
                arrayString = new StringBuilder()
                        .append(arrayString)
                        .append(strings[i])
                        .append(',')
                        .toString();
            } else {
                arrayString = new StringBuilder()
                        .append(arrayString)
                        .append(strings[i]).toString();
            }

        }

        finalString = new StringBuilder()
                .append("[")
                .append(arrayString)
                .append("]")
                .toString();
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
            if (check == false) {
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
    public void dismissLoading() {

        if (val == false) {
            Globals.dismissLoading();
            Globals.dismissLoading2();
        } else {
            Globals.dismissLoading2();
        }

    }

    @Override
    public void showLoading() {

        if (val == false) {
            Globals.showLoading(mContext);

        } else {
            Globals.showLoading2(mContext);

        }
    }

    @Override
    public void getLoadMoreDatas(boolean check) {
        if (ConnectivityReceiver.isConnected()) {
            if (nextToken != null) {
                val = check;
                if (check == false) {
                    Globals.showLoading(mContext);
                    Globals.dismissLoading2();
                } else {
                    Globals.showLoading2(mContext);

                }
                isLoadMore = true;
                Globals.mAWSAppSyncClient.query(StrRatingVideoSortByAssignedDateQuery.builder()
                        .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY)).
                                sortDirection(ModelSortDirection.DESC)
                        .build()).responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                        .enqueue(ratingVideos);
            }
        } else {
            Globals.showSnackBar(mContext.getResources().getString(R.string.noInternet), mContext, Snackbar.LENGTH_LONG);
        }
    }
}
