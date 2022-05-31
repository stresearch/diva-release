package com.visym.collector.capturemodule;

import android.content.Context;

import com.visym.collector.capturemodule.model.ActivityLabel;
import com.visym.collector.capturemodule.model.FrameJSON;
import com.visym.collector.capturemodule.model.FrameObject;
import com.visym.collector.model.BoundingBox;
import com.visym.collector.model.Frame;
import com.visym.collector.utils.AwsResponse;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.List;

public interface IVideoEditModule {

    interface IActivityEdit {
        void onViewAttached(IVideoEditView view, Context context);

        void onViewDetached();

        JSONArray getBoundingBoxesGap(String defaultObject, FrameJSON copyFrame, double frameDuration, int videoDuration)
                throws JSONException;

        void deleteActivity(String selectedItem, FrameJSON copyFrame);

        JSONArray getActivityValues(FrameJSON copyFrame, double frameDuration, int videoDuration) throws JSONException;

        JSONArray updateTempArray(String selectedItem, JSONArray tempArray,
                                  int startFrame, int endFrame, double duration, double frameDuration) throws JSONException;

        JSONArray getActivitySegments(List<ActivityLabel> activities, double duration, double frameDuration) throws JSONException;

    }

    interface IObjectEditing {
        void onViewAttached(IVideoEditView view, Context context);

        void onViewDetached();

        void updateEditedObject(String selectedItem, FrameJSON copyFrame,
                                HashMap<Integer, Frame> boundingBoxes, int originalWidth, int originalHeight,
                                int currentWidth, int currentHeight, int x, int y);

        List<BoundingBox> getObject(String selectedItem, FrameJSON copyFrame);

        Frame getConvertedFrame(int frameWidth, int frameHeight, int videoWidth, int videoHeight, Frame frame);

        HashMap<Integer, Frame> getObjectBoxes(String selectedItem, FrameJSON copyFrame, int currentWidth, int currentHeight);

        void deleteObjectFrames(String selectedItem, List<FrameObject> object);
    }

    interface IVideoEditor {
        void onViewAttached(IVideoEditView view);

        void onViewDetached();

        void downloadFile(String videoFilePath);
    }

    interface IVideoEditView {
        void onFileDownload(AwsResponse response);

        void onFailure(String errorMessage);

        void onFileUploadFailure(String errorMessage);

        void onFileUploadSuccess(AwsResponse response);
    }
}
