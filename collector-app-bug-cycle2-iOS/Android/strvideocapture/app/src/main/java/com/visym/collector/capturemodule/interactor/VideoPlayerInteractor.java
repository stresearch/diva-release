package com.visym.collector.capturemodule.interactor;

import com.visym.collector.model.CollectionQuestionnaire;
import com.visym.collector.model.CollectionQuestionnaireResponse;
import com.visym.collector.model.Instance;
import com.visym.collector.utils.AwsResponse;

import java.util.List;

public interface VideoPlayerInteractor {

    interface VideoPlayerPresenter {
        void downloadJSONFile(String videoFilePath, boolean trainingModule);

        void getCollectionQuestionnaire();

        void getActivityInstances(String videoId);

        void updateQuestionnaireResponse(List<CollectionQuestionnaireResponse> questionnaireResponses, String currentInstanceId);
    }

    interface VideoPlayerView {
        void onFileDownload(AwsResponse response);

        void onFailure(String errorMessage);

        void onQuestionnaireResponse(List<CollectionQuestionnaire> questionnaireList);

        void onInstanceCallback(List<Instance> instances);
    }
}
