package com.visym.collector.capturemodule.presenters;

import android.os.AsyncTask;
import android.util.Log;

import com.visym.collector.capturemodule.interactor.VideoUploadInteractor;
import com.visym.collector.model.NetworkCallback;
import com.visym.collector.network.NetworkClientError;
import com.visym.collector.network.project.AwsS3Repository;
import com.visym.collector.utils.AwsResponse;
import com.visym.collector.utils.ErrorHandler;
import com.visym.collector.utils.Globals;

import java.util.Scanner;

public class VideoUploadPresenter implements VideoUploadInteractor.VideoUploadPresenter {

    private VideoUploadInteractor.VideoUploadView mView;
    private AwsS3Repository s3Repository;

    public VideoUploadPresenter(VideoUploadInteractor.VideoUploadView mView) {
        this.mView = mView;
        s3Repository = new AwsS3Repository();
    }

    public void uploadVideo(String videoFilePath, int fileType) {
        s3Repository.uploadFile(videoFilePath, fileType, new NetworkCallback<AwsResponse>() {
            @Override
            public void onSuccess(AwsResponse response) {
                mView.onFileUploadSuccess(response);
            }

            @Override
            public void onFailure(NetworkClientError error) {
                mView.onFileUploadFailure(ErrorHandler.getErrorMessage(error));
            }
        });
    }
}
