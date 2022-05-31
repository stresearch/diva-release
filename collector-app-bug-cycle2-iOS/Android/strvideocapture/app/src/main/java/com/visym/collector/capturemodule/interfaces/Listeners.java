package com.visym.collector.capturemodule.interfaces;


import com.visym.collector.model.CollectionModel;
import com.visym.collector.model.UserVideoList;

public interface Listeners {

    interface OnSelectListner<T> {
        void onCollectionSelection(T collectionsData, int position);

    }
    interface OnEditOptionSelectListner {
        void onEditCollectionSelection(UserVideoList collectionsData, int position);
    }
    interface PositionListener {
        void getAdapterPositionFromConsentList(int position);
    }
    interface CameraOrientationListner{
        void getDisplayOrientation(int value);
    }

    interface VideoCaptureActivityListener {
        void onActivityClick(int position);

        void updateLabels(int currentSelection);
    }
    interface SubmitVideoToS3BucketListner{
        void onSubmitButtonClick();
        void isPlayerPlaying(Boolean isPlaying);
    }
    interface UserResponseForRAndTVideos{
        void onUpVoteClickEvent(boolean isClicked);
        void onDownVoteClickEvent(boolean isClicked);
        void onReplayVoteClickEvent(boolean isClicked);
        void getSeekBarPositionAtTime(int timeInSec);
    }
}
