package com.visym.collector.capturemodule.presenters;

import android.content.Intent;
import android.widget.Toast;

import com.visym.collector.capturemodule.interactor.VideoPlayerInteractor;
import com.visym.collector.capturemodule.model.ActivityLabel;
import com.visym.collector.capturemodule.model.FrameJSON;
import com.visym.collector.capturemodule.model.FrameMetaData;
import com.visym.collector.capturemodule.model.FrameObject;
import com.visym.collector.model.BoundingBox;
import com.visym.collector.model.Frame;
import com.visym.collector.model.NetworkCallback;
import com.visym.collector.network.project.AwsS3Repository;
import com.visym.collector.usermodule.view.FrontScreenActivity;
import com.visym.collector.utils.AwsResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class VideoPlayerPresenter implements VideoPlayerInteractor.VideoPlayerPresenter {

    private AwsS3Repository awsS3Repository;
    private VideoPlayerInteractor.VideoPlayerView view;

    public VideoPlayerPresenter(VideoPlayerInteractor.VideoPlayerView view) {
        this.view = view;
        this.awsS3Repository = new AwsS3Repository();
    }

    public void downloadJSONFile(String videoFilePath, boolean trainingModule) {
        awsS3Repository.downloadFile(videoFilePath, trainingModule, new NetworkCallback<AwsResponse>() {
            @Override
            public void onSuccess(AwsResponse response) {
                view.onFileDownload(response);
            }

            @Override
            public void onFailure(String error) {
                view.onFailure(error);
            }
        });
    }

    public String getActivityLabels(int frameIndex, FrameJSON copyFrame) {
        StringBuilder activityLabel = null;
        List<ActivityLabel> activities = copyFrame.getActivity();
        if (activities != null && !activities.isEmpty()) {
            for (ActivityLabel activity : activities) {
                if (frameIndex >= activity.getStartFrame() && frameIndex <= activity.getEndFrame()) {
                    if (activityLabel == null) {
                        activityLabel = new StringBuilder(activity.getLabel());
                    } else {
                        if (!activityLabel.toString().contains(activity.getLabel())) {
                            activityLabel.append("\n").append(activity.getLabel());
                        }
                    }
                }
            }
        }
        if (activityLabel != null) {
            return activityLabel.toString();
        }
        return null;
    }

    public HashMap<Integer, List<Frame>>
    generateObjectFrames(FrameJSON copyFrame, int previewWidth,
                         int previewHeight, String defaultObjectName) {
        HashMap<Integer, List<Frame>> objectFrames = new LinkedHashMap<>();
        if (copyFrame != null) {
            FrameMetaData metaData = copyFrame.getMetaData();
            if (metaData == null) {
                return null;
            }
            int frameWidth = metaData.getFrameWidth();
            int frameHeight = metaData.getFrameHeight();

            List<FrameObject> objects = copyFrame.getObject();
            if (objects != null && !objects.isEmpty()) {
                for (int i = 0; i < objects.size(); i++) {
                    FrameObject object = objects.get(i);
                    List<BoundingBox> boundingBox = object.getBoundingBox();
                    if (boundingBox != null && !boundingBox.isEmpty()) {
                        for (BoundingBox box : boundingBox) {
                            List<Frame> frames = objectFrames.get(box.getFrameIndex());
                            if (frames == null) {
                                frames = new ArrayList<>();
                            }
                            Frame frame = box.getFrame();
                            Frame newFrame = getConvertedFrame(frameWidth, frameHeight,
                                    previewWidth, previewHeight, frame);
                            if (defaultObjectName != null && defaultObjectName.contentEquals(object.getLabel())) {
                                newFrame.setDefault(true);
                            }
                            frames.add(newFrame);
                            objectFrames.put(box.getFrameIndex(), frames);
                        }
                    }
                }
            }
        }
        else {
            return null;  // JEBYRNE: should never get here
        }
        return objectFrames;
    }

    public Frame getConvertedFrame(int frameWidth, int frameHeight, int videoWidth,
                                   int videoHeight, Frame frame) {
        int width = (videoWidth * frame.getWidth()) / frameWidth;
        int height = (videoHeight * frame.getHeight()) / frameHeight;

        int newX = (videoWidth * frame.getX()) / frameWidth;
        int newY = (videoHeight * frame.getY()) / frameHeight;

        Frame newFrame = new Frame(newX, newY, width, height);
        newFrame.setDefault(frame.isDefault());
        return newFrame;
    }
}