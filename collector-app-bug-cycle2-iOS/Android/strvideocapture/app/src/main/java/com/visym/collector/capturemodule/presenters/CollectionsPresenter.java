package com.visym.collector.capturemodule.presenters;

import android.content.Context;
import android.util.Log;

import com.amazonaws.amplify.generated.graphql.ListStrCollectionsAssignmentsQuery;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.material.snackbar.Snackbar;
import com.visym.collector.R;
import com.visym.collector.capturemodule.ICaptureModule;
import com.visym.collector.capturemodule.ICaptureModule.ICollectionsPresenter;
import com.visym.collector.capturemodule.ICaptureModule.ICollectionsView;
import com.visym.collector.model.CollectionModel;
import com.visym.collector.network.ApiClient;
import com.visym.collector.network.ApiInterface;
import com.visym.collector.network.ConnectivityReceiver;
import com.visym.collector.network.ErrorHandler;
import com.visym.collector.network.IErrorInterator;
import com.visym.collector.network.NetworkRequest;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;
import com.visym.collector.utils.SharedPref;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import javax.annotation.Nonnull;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

public class CollectionsPresenter implements ICollectionsPresenter, IErrorInterator {
    private ICaptureModule.ICollectionsView mview;
    private Context mcontext;
    private List<CollectionModel> collectionsList;
    private ApiInterface api;
    private NetworkRequest networkRequest;
    private ErrorHandler errorHandler;
    private Globals globals = new Globals();
    private SharedPref sharedPref;
    private String projectId;
    private JSONArray collectionListArray;
    private JSONArray projectListArray;
    private GraphQLCall.Callback<ListStrCollectionsAssignmentsQuery.Data> allAssignedCollections = new GraphQLCall.Callback<ListStrCollectionsAssignmentsQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<ListStrCollectionsAssignmentsQuery.Data> response) {
            if (response != null && response.data() != null && response.data().listStrCollectionsAssignments() != null && response.data().listStrCollectionsAssignments().items() != null) {
                for (int i = 0; i < response.data().listStrCollectionsAssignments().items().size(); i++) {
                    if (response.data().listStrCollectionsAssignments().items().get(i).active() && response.data().listStrCollectionsAssignments().items().get(i).project_id().equalsIgnoreCase(projectId)) {
                        JSONObject itemsObject = new JSONObject();
                        try {
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
                            if (response.data().listStrCollectionsAssignments().items().get(i).training_videos() != null) {
                                // list of training_Videos
                                itemsObject.put("training_videos", response.data().listStrCollectionsAssignments().items().get(i).training_videos());

                            }
                            if (response.data().listStrCollectionsAssignments().items().get(i).training_videos_low() != null) {
                                // list of training_Videos
                                itemsObject.put("training_videos_low", response.data().listStrCollectionsAssignments().items().get(i).training_videos_low());
                            }

                            collectionListArray.put(itemsObject);
                        } catch (Exception e) {
                            Log.e(Globals.TAG, "onResponse: " + e.toString());
                        }
                    }
                }
                if (response.data().listStrCollectionsAssignments().nextToken() != null) {
                    getAssignedCollections(response.data().listStrCollectionsAssignments().nextToken());
                } else {
                    Globals.dismissLoading();
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            // Stuff that updates the UI
                            mview.initializeAdapterWithDatas(collectionListArray);
                        }
                    });
                }
            } else {
                if (Globals.isShowingLoader())
                    Globals.dismissLoading();
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {

        }
    };
    private GraphQLCall.Callback<ListStrCollectionsAssignmentsQuery.Data> assignedCollections = new GraphQLCall.Callback<ListStrCollectionsAssignmentsQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<ListStrCollectionsAssignmentsQuery.Data> response) {
            if (response != null && response.data() != null && response.data().listStrCollectionsAssignments() != null && response.data().listStrCollectionsAssignments().items() != null) {
                collectionListArray = new JSONArray();

                for (int i = 0; i < response.data().listStrCollectionsAssignments().items().size(); i++) {
                    if (response.data().listStrCollectionsAssignments().items().get(i).active() && response.data().listStrCollectionsAssignments().items().get(i).project_id().equalsIgnoreCase(projectId)) {
                        JSONObject itemsObject = new JSONObject();
                        try {
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
                            itemsObject.put("consent_overlay_text", response.data().listStrCollectionsAssignments().items().get(i).consent_overlay_text());
                            itemsObject.put("isConsentRequired", response.data().listStrCollectionsAssignments().items().get(i).isConsentRequired());
                            itemsObject.put("training_videos_overlay", response.data().listStrCollectionsAssignments().items().get(i).training_videos_overlay());
                            if (response.data().listStrCollectionsAssignments().items().get(i).training_videos() != null) {
                                // list of training_Videos
                                itemsObject.put("training_videos", response.data().listStrCollectionsAssignments().items().get(i).training_videos());

                            }
                            if (response.data().listStrCollectionsAssignments().items().get(i).training_videos_low() != null) {
                                // list of training_Videos
                                itemsObject.put("training_videos_low", response.data().listStrCollectionsAssignments().items().get(i).training_videos_low());
                            }
                            collectionListArray.put(itemsObject);
                        } catch (Exception e) {
                            Log.e(Globals.TAG, "onResponse: " + e.toString());
                        }
                    }
                }
                if (response.data().listStrCollectionsAssignments().nextToken() != null) {
                    getAssignedCollections(response.data().listStrCollectionsAssignments().nextToken());
                } else {
                    Globals.dismissLoading();
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {


                            // Stuff that updates the UI
                            mview.initializeAdapterWithDatas(collectionListArray);
                        }
                    });
                }
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {

        }
    };
    private GraphQLCall.Callback<ListStrCollectionsAssignmentsQuery.Data> projectsList
            = new GraphQLCall.Callback<ListStrCollectionsAssignmentsQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<ListStrCollectionsAssignmentsQuery.Data> response) {
            if (response != null && response.data() != null && response.data().listStrCollectionsAssignments() != null && response.data().listStrCollectionsAssignments().items() != null) {
                collectionListArray = new JSONArray();

                for (int i = 0; i < response.data().listStrCollectionsAssignments().items().size(); i++) {
                    if (response.data().listStrCollectionsAssignments().items().get(i).active() != null && response.data().listStrCollectionsAssignments().items().get(i).active()) {
                        JSONObject itemsObject = new JSONObject();
                        try {
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
                            if (response.data().listStrCollectionsAssignments().items().get(i).training_videos() != null) {
                                // list of training_Videos
                                itemsObject.put("training_videos", response.data().listStrCollectionsAssignments().items().get(i).training_videos());

                            }
                            if (response.data().listStrCollectionsAssignments().items().get(i).training_videos_low() != null) {
                                // list of training_Videos
                                itemsObject.put("training_videos_low", response.data().listStrCollectionsAssignments().items().get(i).training_videos_low());
                            }
                            collectionListArray.put(itemsObject);
                        } catch (Exception e) {
                            Log.e(Globals.TAG, "onResponse: " + e.toString());
                        }
                    }
                }

                Globals.dismissLoading();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {


                        // Stuff that updates the UI
                        mview.getCollectionProjects(collectionListArray);
                    }
                });
            } else {
                Log.e(Globals.TAG, "onFileUploadFailure: ");
                Globals.dismissLoading();
            }


        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Globals.dismissLoading();
        }
    };

    @Override
    public void onViewAttached(ICollectionsView view, Context context) {
        this.mview = view;
        this.mcontext = context;
        this.api = ApiClient.buildService();
        this.networkRequest = new NetworkRequest();
        this.sharedPref = new SharedPref(context);
        this.errorHandler = new ErrorHandler(context, this);
        collectionListArray = new JSONArray();
    }

    @Override
    public void onViewDetached() {
        this.mview = null;
    }

    @Override
    public void getCollectionsForProjectFromServer(String projectId) {
        if (ConnectivityReceiver.isConnected()) {
            this.projectId = projectId;
            Globals.showLoading(mcontext);
            Globals.mAWSAppSyncClient.query(ListStrCollectionsAssignmentsQuery.builder()
                    .collector_id(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_ID_KEY))
                    .limit(50).build())
                    .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                    .enqueue(assignedCollections);
        } else {
            Globals.showSnackBar(mcontext.getResources().getString(R.string.noInternet), mcontext,
                    Snackbar.LENGTH_SHORT);
        }
    }

    private void getAssignedCollections(String nextToken) {
        Globals.mAWSAppSyncClient.query(ListStrCollectionsAssignmentsQuery.builder()
                .collector_id(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_ID_KEY))
                .limit(50).nextToken(nextToken).build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(allAssignedCollections);
    }

    @Override
    public void dismissLoading() {

    }

    @Override
    public void showLoading() {


    }

    @Override
    public void getProjectList() {

        if (ConnectivityReceiver.isConnected()) {
            Globals.mAWSAppSyncClient.query(ListStrCollectionsAssignmentsQuery.builder()
                    .collector_id(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_ID_KEY))
                    .limit(50).build())
                    .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                    .enqueue(projectsList);
        } else {
            Globals.showSnackBar(mcontext.getResources().getString(R.string.noInternet), mcontext,
                    Snackbar.LENGTH_SHORT);
        }
    }

}
