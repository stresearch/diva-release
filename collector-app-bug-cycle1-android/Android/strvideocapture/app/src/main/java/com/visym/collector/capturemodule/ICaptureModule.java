package com.visym.collector.capturemodule;

import android.content.Context;

import org.json.JSONArray;

public interface ICaptureModule {

    interface ICollectionsView {

        void bindEvents();

        void initializeAdapterWithDatas(JSONArray dataArray);

        void getCollectionProjects(JSONArray dataArray);
    }

    interface ICollectionsPresenter {

        void onViewAttached(ICaptureModule.ICollectionsView view, Context context);

        void onViewDetached();

        void getProjectList();

        void getCollectionsForProjectFromServer(String projectId);
    }

    interface IConsentConfirmationView {

        void bindEvents();

        void initConsentSubjectObjects();

        void initConsentDataObjects(JSONArray data);

        void hideTheConsentView(String endStatmentText);

        void displayRequestFailed();
    }

    interface IConsentConfirmationPresent {

        void onViewAttached(IConsentConfirmationView view, Context context);

        void onViewDetached();

        void getConsentDatas(String version);
    }
}
