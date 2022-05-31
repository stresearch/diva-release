package com.visym.collector.capturemodule.interactor;

import com.visym.collector.utils.AwsResponse;

public interface VideoUploadInteractor {

    interface VideoUploadPresenter {
    }

    interface VideoUploadView {
        void onFileUploadSuccess(AwsResponse response);

        void onFileUploadFailure(String errorMessage);
    }
}
