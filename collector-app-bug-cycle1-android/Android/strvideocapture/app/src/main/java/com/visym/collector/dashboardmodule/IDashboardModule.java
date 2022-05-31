package com.visym.collector.dashboardmodule;

import android.content.Context;

import com.visym.collector.model.CollectionModel;

import org.json.JSONArray;

public interface IDashboardModule {

    interface ICollectionFragmentView {

        void initializeVideoListAdapter(JSONArray videoObject);

        void bindEvents();


        void initializeLoadMoreDataToAdapter(JSONArray videoObject);

        void handleDataNull();

        void handleError();

        void onCollectionDetailDownload(CollectionModel collection);

        void finish();
    }

    interface ICollectionFragmentPresenter {

        void onViewAttached(IDashboardModule.ICollectionFragmentView mview, Context context);

        void onViewDetached();

        void getUserCollectionVideos(boolean check);

        void getLoadMoreDatas(boolean check);

        String convertArrayToStringMethod(JSONArray strArray);

        void getCollectionDetails(String collectionId);
    }

    interface ICaptureFragmentView {

        void initializeProjectListViews(JSONArray object);

        void bindEvents();

        void handleError();
    }

    interface ICaptureFragmentPresenter {

        void onViewAttached(IDashboardModule.ICaptureFragmentView view, Context context);

        void onViewDetached();

        void getProjectList();
    }

    interface IRatingFragmentView {

        void handleDataAndInitializeAdapter(JSONArray videoData);

        void handleLoadMoreDataAndInitializeAdapter(JSONArray videoData);

        void bindEvents();

        void handleError();

        void handleDataNull();

        void onCollectionDetailDownload(CollectionModel collection);

        void finish();
    }

    interface IRatingFragmentPresenter {

        void onViewAttached(IDashboardModule.IRatingFragmentView view, Context context);

        void onViewDetached();

        void getUserRatingsVideo(boolean check);

        void getLoadMoreDatas(boolean check);

        void getCollectionDetails(String collection_id);
    }


}
