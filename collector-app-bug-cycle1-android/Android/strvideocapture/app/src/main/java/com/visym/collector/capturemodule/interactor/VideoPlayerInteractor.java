package com.visym.collector.capturemodule.interactor;

import com.visym.collector.capturemodule.model.FrameJSON;
import com.visym.collector.model.Frame;
import com.visym.collector.utils.AwsResponse;

import java.util.HashMap;
import java.util.List;

public interface VideoPlayerInteractor {

    interface VideoPlayerPresenter {
        void downloadJSONFile(String videoFilePath, boolean trainingModule);

        HashMap<Integer, List<Frame>> generateObjectFrames(FrameJSON copyFrame,
                                                           int width, int height, String defaultObjectName);

        String getActivityLabels(int frameIndex, FrameJSON copyFrame);
    }

    interface VideoPlayerView {
        void onFileDownload(AwsResponse response);

        void onFailure(String errorMessage);
    }
}
