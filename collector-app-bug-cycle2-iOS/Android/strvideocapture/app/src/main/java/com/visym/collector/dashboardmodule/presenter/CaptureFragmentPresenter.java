package com.visym.collector.dashboardmodule.presenter;

import android.content.Context;
import android.widget.Toast;

import com.amazonaws.amplify.generated.graphql.ListStrCollectionsAssignmentsQuery;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.visym.collector.dashboardmodule.IDashboardModule.ICaptureFragmentPresenter;
import com.visym.collector.dashboardmodule.IDashboardModule.ICaptureFragmentView;
import com.visym.collector.network.IErrorInterator;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

public class CaptureFragmentPresenter implements ICaptureFragmentPresenter, IErrorInterator {

    private JSONArray projectListArray, collectionListArray;
    private Context mcontext;
    private Set<Object> sets;
    private JSONArray collectionCountListArray;
    private ICaptureFragmentView mview;
    private int projectCount = 0;
    private GraphQLCall.Callback<ListStrCollectionsAssignmentsQuery.Data> allAssignedCollections = new GraphQLCall.Callback<ListStrCollectionsAssignmentsQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<ListStrCollectionsAssignmentsQuery.Data> response) {
            if (response != null && response.data() != null && response.data().listStrCollectionsAssignments() != null) {
                for (int i = 0; i < response.data().listStrCollectionsAssignments().items().size(); i++) {
                    projectCount += 1;
                    JSONObject itemsObject = new JSONObject();
                    JSONObject projectObject = new JSONObject();
                    try {
                        if (response.data().listStrCollectionsAssignments().items().get(i).active() != null && response.data().listStrCollectionsAssignments().items().get(i).active()) {
                            projectObject.put("id", response.data().listStrCollectionsAssignments().items().get(i).program_name());
                            projectObject.put("project_id", response.data().listStrCollectionsAssignments().items().get(i).project_id());
                            projectObject.put("name", response.data().listStrCollectionsAssignments().items().get(i).project_name());
                            Object item = projectObject.getString("project_id");
                            if (sets.add(item)) {
                                projectListArray.put(projectObject);
                            }

                            itemsObject.put("id", response.data().listStrCollectionsAssignments().items().get(i).program_name()
                                    + "_" + response.data().listStrCollectionsAssignments().items().get(i).project_name());
                            itemsObject.put("collection_id", response.data().listStrCollectionsAssignments().items().get(i).collection_id());
                            itemsObject.put("collection_name", response.data().listStrCollectionsAssignments().items().get(i).collection_name());
                            itemsObject.put("project_id", response.data().listStrCollectionsAssignments().items().get(i).project_id());
                            itemsObject.put("project_name", response.data().listStrCollectionsAssignments().items().get(i).project_name());
                            itemsObject.put("program_id", response.data().listStrCollectionsAssignments().items().get(i).program_id());
                            itemsObject.put("program_name", response.data().listStrCollectionsAssignments().items().get(i).program_name());
                            itemsObject.put("collection_description", response.data().listStrCollectionsAssignments().items().get(i).collection_description());
                            itemsObject.put("activities", response.data().listStrCollectionsAssignments().items().get(i).activities());
                            itemsObject.put("default_object", response.data().listStrCollectionsAssignments().items().get(i).default_object());
                            itemsObject.put("collector_email", response.data().listStrCollectionsAssignments().items().get(i).collector_email());
                            itemsObject.put("collector_id", response.data().listStrCollectionsAssignments().items().get(i).collector_id());
                            itemsObject.put("training_videos", response.data().listStrCollectionsAssignments().items().get(i).training_videos());
                            itemsObject.put("object_list", response.data().listStrCollectionsAssignments().items().get(i).objects_list());
                            itemsObject.put("active", response.data().listStrCollectionsAssignments().items().get(i).active());
                            itemsObject.put("activity_short_names", response.data().listStrCollectionsAssignments().items().get(i).activity_short_names());
                            itemsObject.put("play_training_video", response.data().listStrCollectionsAssignments().items().get(i).isTrainingVideoEnabled());
                            collectionListArray.put(itemsObject);
                        }
                    } catch (JSONException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mview.handleError();
                            }
                        });
                    }
                }
                if (response.data().listStrCollectionsAssignments().nextToken() != null) {
                    getAllAssignedCollections(response.data().listStrCollectionsAssignments().nextToken());
                } else {
                    getCollectionCount(projectListArray, collectionListArray);
                }
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Stuff that updates the UI
                        Globals.dismissLoading();
                    }
                });
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Toast.makeText(mcontext, e.toString(), Toast.LENGTH_SHORT).show();
        }
    };
    private GraphQLCall.Callback<ListStrCollectionsAssignmentsQuery.Data> projectsList
            = new GraphQLCall.Callback<ListStrCollectionsAssignmentsQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<ListStrCollectionsAssignmentsQuery.Data> response) {
            projectListArray = new JSONArray();
            collectionListArray = new JSONArray();
            sets = new HashSet<>();
            if (response != null && response.data() != null && response.data().listStrCollectionsAssignments() != null) {
                for (int i = 0; i < response.data().listStrCollectionsAssignments().items().size(); i++) {
                    projectCount += 1;
                    JSONObject itemsObject = new JSONObject();
                    JSONObject projectObject = new JSONObject();
                    try {
                        if (response.data().listStrCollectionsAssignments().items().get(i).active() != null && response.data().listStrCollectionsAssignments().items().get(i).active()) {
                            projectObject.put("id", response.data().listStrCollectionsAssignments().items().get(i).program_name());
                            projectObject.put("project_id", response.data().listStrCollectionsAssignments().items().get(i).project_id());
                            projectObject.put("name", response.data().listStrCollectionsAssignments().items().get(i).project_name());
                            Object item = projectObject.getString("project_id");
                            if (sets.add(item)) {
                                projectListArray.put(projectObject);
                            }
                            itemsObject.put("id", response.data().listStrCollectionsAssignments().items().get(i).program_name()
                                    + "_" + response.data().listStrCollectionsAssignments().items().get(i).project_name());
                            itemsObject.put("collection_id", response.data().listStrCollectionsAssignments().items().get(i).collection_id());
                            itemsObject.put("collection_name", response.data().listStrCollectionsAssignments().items().get(i).collection_name());
                            itemsObject.put("project_id", response.data().listStrCollectionsAssignments().items().get(i).project_id());
                            itemsObject.put("project_name", response.data().listStrCollectionsAssignments().items().get(i).project_name());
                            itemsObject.put("program_id", response.data().listStrCollectionsAssignments().items().get(i).program_id());
                            itemsObject.put("program_name", response.data().listStrCollectionsAssignments().items().get(i).program_name());
                            itemsObject.put("collection_description", response.data().listStrCollectionsAssignments().items().get(i).collection_description());
                            itemsObject.put("activities", response.data().listStrCollectionsAssignments().items().get(i).activities());
                            itemsObject.put("default_object", response.data().listStrCollectionsAssignments().items().get(i).default_object());
                            itemsObject.put("collector_email", response.data().listStrCollectionsAssignments().items().get(i).collector_email());
                            itemsObject.put("collector_id", response.data().listStrCollectionsAssignments().items().get(i).collector_id());
                            itemsObject.put("training_videos", response.data().listStrCollectionsAssignments().items().get(i).training_videos());
                            itemsObject.put("object_list", response.data().listStrCollectionsAssignments().items().get(i).objects_list());
                            itemsObject.put("active", response.data().listStrCollectionsAssignments().items().get(i).active());
                            itemsObject.put("activity_short_names", response.data().listStrCollectionsAssignments().items().get(i).activity_short_names());
                            itemsObject.put("play_training_video", response.data().listStrCollectionsAssignments().items().get(i).isTrainingVideoEnabled());
                            collectionListArray.put(itemsObject);
                        }
                    } catch (JSONException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mview.handleError();
                            }
                        });
                    }
                }
                if (response.data().listStrCollectionsAssignments().nextToken() != null) {
                    getAllAssignedCollections(response.data().listStrCollectionsAssignments().nextToken());
                } else {
                    getCollectionCount(projectListArray, collectionListArray);
                }
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.dismissLoading();
                    }
                });
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Globals.dismissLoading();
        }
    };

    @Override
    public void onViewAttached(ICaptureFragmentView mview, Context context) {
        this.mview = mview;
        this.mcontext = context;
        collectionCountListArray = new JSONArray();
    }

    @Override
    public void onViewDetached() {
        this.mview = null;
    }

    @Override
    public void getProjectList() {
        Globals.mAWSAppSyncClient.query(ListStrCollectionsAssignmentsQuery.builder()
                .collector_id(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_ID_KEY))
                .limit(50).build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(projectsList);
    }

    private void getAllAssignedCollections(String nextToken) {
        Globals.mAWSAppSyncClient.query(ListStrCollectionsAssignmentsQuery.builder()
                .collector_id(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_ID_KEY))
                .limit(50).nextToken(nextToken)
                .build()).responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY).enqueue(allAssignedCollections);
    }

    private void getCollectionCount(JSONArray projectArray, JSONArray collectionsArray) {
        try {
            for (int p = 0; p < projectArray.length(); p++) {
                int collectionCount = 0;
                for (int c = 0; c < collectionsArray.length(); c++) {
                    if (projectArray.getJSONObject(p).getString("project_id").equalsIgnoreCase(collectionsArray.getJSONObject(c).getString("project_id"))) {
                        collectionCount++;
                    }
                }
                if (collectionCount > 0) {
                    projectArray.getJSONObject(p).put("collectionsCount", collectionCount);
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mview.initializeProjectListViews(projectListArray);
                }
            });
        } catch (Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mview.handleError();
                }
            });
        }
    }

    @Override
    public void dismissLoading() {

    }

    @Override
    public void showLoading() {

    }
}
