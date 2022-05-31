package com.visym.collector.dashboardmodule.presenter;

import android.content.Context;

import com.amazonaws.amplify.generated.graphql.StrVideoByUploaedDateQQuery;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.material.snackbar.Snackbar;
import com.visym.collector.R;
import com.visym.collector.dashboardmodule.IDashboardModule.ICollectionFragmentPresenter;
import com.visym.collector.dashboardmodule.IDashboardModule.ICollectionFragmentView;
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
import type.ModelStringInput;
import type.ModelstrVideosFilterInput;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

public class CollectionFragmentPresenter implements ICollectionFragmentPresenter, IErrorInterator {

    JSONArray videoData;
    String nextToken;
    boolean val;
    private ICollectionFragmentView mview;
    private Context mcontext;
    private boolean isLoadMore = false;
    private AppSharedPreference preference;
    private GraphQLCall.Callback<StrVideoByUploaedDateQQuery.Data> videosCallback =
            new GraphQLCall.Callback<StrVideoByUploaedDateQQuery.Data>() {
                @Override
                public void onResponse(@Nonnull Response<StrVideoByUploaedDateQQuery.Data> response) {
                    if (response.data() != null && response.data().StrVideoByUploaedDateQ() != null) {
                        videoData = new JSONArray();
                        for (int i = 0; i < response.data().StrVideoByUploaedDateQ().items().size(); i++) {
                            JSONObject itemObject = new JSONObject();
                            try {
                                itemObject.put("id", response.data().StrVideoByUploaedDateQ().items().get(i).id());
                                itemObject.put("raw_video_file_path", response.data().StrVideoByUploaedDateQ().items().get(i).raw_video_file_path());
                                itemObject.put("raw_json_file_path", response.data().StrVideoByUploaedDateQ().items().get(i).annotation_file_path());
                                itemObject.put("video_id", response.data().StrVideoByUploaedDateQ().items().get(i).video_id());
                                JSONArray activity_array = new JSONArray(response.data().StrVideoByUploaedDateQ().items().get(i).activities_list().split(","));
                                itemObject.put("activities_list", convertArrayToStringMethod(activity_array));
                                if ((response.data().StrVideoByUploaedDateQ().items().get(i).video_state() != null)) {
                                    itemObject.put("video_state", response.data().StrVideoByUploaedDateQ().items().get(i).video_state());
                                } else {
                                    itemObject.put("video_state", "Collected");

                                }
                                itemObject.put("collection_id", response.data().StrVideoByUploaedDateQ().items().get(i).collection_id());
                                itemObject.put("query_attribute", response.data().StrVideoByUploaedDateQ().items().get(i).query_attribute());
                                itemObject.put("collection_name", response.data().StrVideoByUploaedDateQ().items().get(i).collection_name());
                                itemObject.put("rating", response.data().StrVideoByUploaedDateQ().items().get(i).rating_score());
                                itemObject.put("video_sharing_link", response.data().StrVideoByUploaedDateQ().items().get(i).video_sharing_link());
                                itemObject.put("json_sharing_link", response.data().StrVideoByUploaedDateQ().items().get(i).json_sharing_link());
                                itemObject.put("Thumbnail_small", response.data().StrVideoByUploaedDateQ().items().get(i).thumbnail_small());
                                itemObject.put("duration", response.data().StrVideoByUploaedDateQ().items().get(i).duration());
                                itemObject.put("uploaded_date", response.data().StrVideoByUploaedDateQ().items().get(i).uploaded_date());
                                videoData.put(itemObject);
                            } catch (JSONException e) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mview.handleError();
                                    }
                                });
                            }
                        }
                        if (response.data().StrVideoByUploaedDateQ().nextToken() != null) {
                            nextToken = response.data().StrVideoByUploaedDateQ().nextToken();
                        } else {
                            nextToken = null;
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (videoData.length() > 0) {
                                    if (!isLoadMore) {
                                        mview.initializeVideoListAdapter(videoData);
                                    } else {
                                        isLoadMore = false;
                                        mview.initializeLoadMoreDataToAdapter(videoData);
                                    }
                                } else {
                                    if (Globals.isShowingLoader()) {
                                        Globals.dismissLoading();
                                    }
                                    if (Globals.isShowingLoader2()) {
                                        Globals.dismissLoading2();
                                    }
                                    if (mview != null) {
                                        mview.handleDataNull();
                                    }
                                }
                            }
                        });
                    } else {
                        if (Globals.isShowingLoader()) {
                            Globals.dismissLoading();
                        }
                        if (Globals.isShowingLoader2()) {
                            Globals.dismissLoading2();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mview.handleDataNull();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (Globals.isShowingLoader2()) {
                                Globals.dismissLoading2();
                            }
                            if (Globals.isShowingLoader()) {
                                Globals.dismissLoading();
                            }
                            Globals.showSnackBar("No Data Available!",
                                    mcontext, Snackbar.LENGTH_LONG);
                        }
                    });
                }
            };
    private GraphQLCall.Callback<StrVideoByUploaedDateQQuery.Data> collectorsData = new GraphQLCall.Callback<StrVideoByUploaedDateQQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<StrVideoByUploaedDateQQuery.Data> response) {
            if (response.data() != null && response.data().StrVideoByUploaedDateQ() != null
                    && response.data().StrVideoByUploaedDateQ().items() != null) {
                videoData = new JSONArray();
                for (int i = 0; i < response.data().StrVideoByUploaedDateQ().items().size(); i++) {
                    JSONObject itemObject = new JSONObject();
                    try {
                        itemObject.put("id", response.data().StrVideoByUploaedDateQ().items().get(i).id());
                        itemObject.put("raw_video_file_path", response.data().StrVideoByUploaedDateQ().items().get(i).raw_video_file_path());
                        itemObject.put("raw_json_file_path", response.data().StrVideoByUploaedDateQ().items().get(i).annotation_file_path());
                        itemObject.put("video_id", response.data().StrVideoByUploaedDateQ().items().get(i).video_id());
                        JSONArray activity_array = new JSONArray(response.data().StrVideoByUploaedDateQ().items().get(i).activities_list().split(","));
                        itemObject.put("activities_list", convertArrayToStringMethod(activity_array));
                        if (response.data().StrVideoByUploaedDateQ().items().get(i).video_state() != null) {
                            itemObject.put("video_state", response.data().StrVideoByUploaedDateQ().items().get(i).video_state());
                        } else {
                            itemObject.put("video_state", "Collected");
                        }
                        itemObject.put("query_attribute", response.data().StrVideoByUploaedDateQ().items().get(i).query_attribute());
                        itemObject.put("collection_name", response.data().StrVideoByUploaedDateQ().items().get(i).collection_name());
                        itemObject.put("rating", response.data().StrVideoByUploaedDateQ().items().get(i).rating_score());
                        itemObject.put("collection_id", response.data().StrVideoByUploaedDateQ().items().get(i).collection_id());
                        itemObject.put("video_sharing_link", response.data().StrVideoByUploaedDateQ().items().get(i).video_sharing_link());
                        itemObject.put("json_sharing_link", response.data().StrVideoByUploaedDateQ().items().get(i).json_sharing_link());
                        itemObject.put("Thumbnail_small", response.data().StrVideoByUploaedDateQ().items().get(i).thumbnail_small());
                        itemObject.put("duration", response.data().StrVideoByUploaedDateQ().items().get(i).duration());
                        itemObject.put("uploaded_date", response.data().StrVideoByUploaedDateQ().items().get(i).uploaded_date());
                        videoData.put(itemObject);
                    } catch (JSONException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mview.handleError();
                            }
                        });
                    }
                }
                if (response.data().StrVideoByUploaedDateQ().nextToken() != null) {
                    nextToken = response.data().StrVideoByUploaedDateQ().nextToken();
                } else {
                    nextToken = null;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (videoData.length() > 0) {
                            if (!isLoadMore) {
                                mview.initializeVideoListAdapter(videoData);
                            } else {
                                isLoadMore = false;
                                mview.initializeLoadMoreDataToAdapter(videoData);
                            }
                        }
                    }
                });
            } else {
                if (Globals.isShowingLoader()) {
                    Globals.dismissLoading();
                }
                if (Globals.isShowingLoader2()) {
                    Globals.dismissLoading2();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isLoadMore)
                            mview.handleDataNull();
                    }
                });
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            if (Globals.isShowingLoader2()) {
                Globals.dismissLoading2();
            }
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
            Globals.showSnackBar("No Data Available!", mcontext, Snackbar.LENGTH_LONG);
        }
    };

    @Override
    public String convertArrayToStringMethod(JSONArray strArray) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < strArray.length(); i++) {
            try {
                stringBuilder.append(strArray.get(i).toString().replace("[", "").replace("]", "").replace("\"", ""));
                if (i != strArray.length() - 1) {
                    stringBuilder.append(",");
                } else if (i == strArray.length() - 1) {
                    stringBuilder.append(".");
                }
            } catch (JSONException e) {
                mview.handleError();
            }
        }
        return stringBuilder.toString().replace("\"", "");
    }

    @Override
    public void onViewAttached(ICollectionFragmentView mview, Context context) {
        this.mview = mview;
        this.mcontext = context;
        preference = AppSharedPreference.getInstance();
    }

    @Override
    public void onViewDetached() {
        this.mview = null;
    }

    @Override
    public void getUserCollectionVideos(boolean check) {
        if (ConnectivityReceiver.isConnected()) {
            val = check;
            if (check == false) {
                Globals.showLoading(mcontext);
            } else {
                Globals.showLoading2(mcontext);
            }
            Globals.mAWSAppSyncClient.query(StrVideoByUploaedDateQQuery.builder()
                    .sortDirection(ModelSortDirection.DESC)
                    .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY))
//                    .collector_id("854879ee-ab18-4a53-9333-b3e112f400a2")
                    .filter(ModelstrVideosFilterInput.builder()
                            .query_attribute(ModelStringInput.builder()
                                    .eq("1").build()).build())
                    .build())
                    .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                    .enqueue(videosCallback);
        } else {
            Globals.showSnackBar(mcontext.getResources().getString(R.string.noInternet), mcontext,
                    Snackbar.LENGTH_LONG);
        }
    }

    @Override
    public void dismissLoading() {
        if (!val) {
            Globals.dismissLoading();
            Globals.dismissLoading2();

        } else {
            Globals.dismissLoading2();
        }
    }

    @Override
    public void showLoading() {
        if (!val) {
            Globals.showLoading(mcontext);

        } else {
            Globals.showLoading2(mcontext);

        }
    }


    @Override
    public void getLoadMoreDatas(boolean check) {
        if (ConnectivityReceiver.isConnected()) {
            if (nextToken != null) {
                val = check;
                if (check == false) {
                    Globals.showLoading(mcontext);
                    Globals.dismissLoading2();
                } else {
                    Globals.showLoading2(mcontext);
                }
                isLoadMore = true;
                Globals.mAWSAppSyncClient.query(StrVideoByUploaedDateQQuery.builder()
                        .sortDirection(ModelSortDirection.DESC)
                        .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY))
                        .filter(ModelstrVideosFilterInput.builder().query_attribute(ModelStringInput.builder()
                                .eq("1").build()).build())
                        .nextToken(nextToken).build())
                        .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                        .enqueue(collectorsData);
            }
        } else {
            Globals.showSnackBar(mcontext.getResources().getString(R.string.noInternet), mcontext,
                    Snackbar.LENGTH_LONG);
        }
    }
}
