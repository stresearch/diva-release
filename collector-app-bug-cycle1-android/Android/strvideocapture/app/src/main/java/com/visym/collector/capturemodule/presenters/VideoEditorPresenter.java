package com.visym.collector.capturemodule.presenters;

import com.visym.collector.capturemodule.IVideoEditModule;
import com.visym.collector.model.NetworkCallback;
import com.visym.collector.network.project.AwsS3Repository;
import com.visym.collector.utils.AwsResponse;

public class VideoEditorPresenter implements IVideoEditModule.IVideoEditor {

    private IVideoEditModule.IVideoEditView mView;
    private AwsS3Repository awsS3Repository;

    @Override
    public void onViewAttached(IVideoEditModule.IVideoEditView view) {
        this.mView = view;
        awsS3Repository = new AwsS3Repository();
    }

    @Override
    public void downloadFile(String videoFilePath) {
        awsS3Repository.downloadFile(videoFilePath, false, new NetworkCallback<AwsResponse>() {
            @Override
            public void onSuccess(AwsResponse response) {
                if (mView != null) {
                    mView.onFileDownload(response);
                }
            }

            @Override
            public void onFailure(String error) {
                if (mView != null) {
                    mView.onFailure(error);
                }
            }
        });
    }

    public void uploadFile(String videoFilePath, int fileType) {
        awsS3Repository.uploadFile(videoFilePath, fileType, new NetworkCallback<AwsResponse>() {
            @Override
            public void onSuccess(AwsResponse response) {
                if (mView != null){
                    mView.onFileUploadSuccess(response);
                }
            }

            @Override
            public void onFailure(String error) {
                if (mView != null){
                    mView.onFileUploadFailure(error);
                }
            }
        });
    }

    @Override
    public void onViewDetached() {
        this.mView = null;
    }
}
