package com.visym.collector.utils;

import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.visym.collector.capturemodule.facedetection.ImageProcessor;
import com.visym.collector.capturemodule.model.ActivityLabel;
import com.visym.collector.capturemodule.model.FrameJSON;
import com.visym.collector.capturemodule.model.FrameMetaData;
import com.visym.collector.capturemodule.model.FrameObject;
import com.visym.collector.model.CollectionModel;
import com.visym.collector.model.Coordinate;
import com.visym.collector.model.Frame;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class FrameUtil implements Runnable{

    private static final String TAG = "FrameUtil";

    private Handler handler;
    private String inputFile;
    private List<Coordinate> coordinates;
    private List<ActivityLabel> activities;

    private final int COMPUTATION_ERROR_CODE = 1;
    private final int COMPUTATION_SUCCESS_CODE = 2;

    public static final String COMPUTATION_ERROR_KEY = "error";
    public static final String COMPUTATION_ERROR_MESSAGE_KEY = "message";

    public static final String COMPUTATION_SUCCESS_KEY = "success";
    public static final String COMPUTATION_FILE_PATH_KEY = "FILE_PATH_KEY";
    private int displayOrientation;
    private int sensorOrientation;

    public FrameUtil(Handler handler, String inputFile, List<Coordinate> coordinates,
                     List<ActivityLabel> activities) {
        this.handler = handler;
        this.inputFile = inputFile;
        this.coordinates = coordinates;
        this.activities = activities;
    }

    @Override
    public void run() {
        Message message = handler.obtainMessage();
        Bundle bundle = new Bundle();

        try {
            FFmpegMediaMetadataRetriever metadataRetriever = new FFmpegMediaMetadataRetriever();
            metadataRetriever.setDataSource(inputFile);

            double frameRate = Double.parseDouble(metadataRetriever
                    .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE));

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(inputFile);

            int duration = Integer.parseInt(retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

            int width, height;
            if (sensorOrientation == Surface.ROTATION_0){
                width = Integer.parseInt(metadataRetriever
                        .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                height = Integer.parseInt(metadataRetriever
                        .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            }else {
                height = Integer.parseInt(metadataRetriever
                        .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                width = Integer.parseInt(metadataRetriever
                        .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            }

            double v = (double) (duration) / 1000;

            List<com.visym.collector.model.BoundingBox> boundingBoxes = new ArrayList<>();
            List<FrameObject> frameObjects = new ArrayList<>();
            FrameMetaData frameMetaData = new FrameMetaData();
            AppSharedPreference instance = AppSharedPreference.getInstance();

            for (int i = 0; i < coordinates.size(); i++) {
                Coordinate coordinate = coordinates.get(i);
                Frame frame = new Frame(coordinate.getX(), coordinate.getY(),
                        coordinate.getWidth(), coordinate.getHeight());
                boundingBoxes.add(new com.visym.collector.model.BoundingBox(frame, i));
            }

            String frameObjectLabel = instance.readString(AppSharedPreference.FRAME_OBJECT_LABEL);
            String collection = instance.readString(Constant.COLLECTION_KEY);

            if (!TextUtils.isEmpty(collection)){
                CollectionModel collectionModel = new Gson().fromJson(collection, CollectionModel.class);
                frameMetaData.setCategory(collectionModel.getActivities());
                frameMetaData.setShortName(collectionModel.getActivityShortNames());
            }

            FrameObject frameObject = new FrameObject(!TextUtils.isEmpty(frameObjectLabel) ? frameObjectLabel
                    : "", boundingBoxes);
            frameObjects.add(frameObject);

            frameMetaData.setProjectId(instance.readString(Constant.PROJECT_ID_KEY));
            frameMetaData.setCollectionName(instance.readString(Constant.COLLECTION_NAME_KEY));
            frameMetaData.setProjectName(instance.readString(Constant.PROJECT_NAME_KEY));
            frameMetaData.setProgramName(instance.readString(Constant.PROGRAM_ID_KEY));
            frameMetaData.setCollectionId(instance.readString(Constant.COLLECTION_ID_KEY));
            frameMetaData.setCollectedDate(instance.readString(Constant.COLLECTED_DATE_KEY));
            frameMetaData.setOrientation(instance.readString(AppSharedPreference.ORIENTATION_KEY));
            frameMetaData.setFrameRate(frameRate);
            frameMetaData.setFrameWidth(width);
            frameMetaData.setFrameHeight(height);
            frameMetaData.setDuration(v);
            String ipAddress = instance.readString(Constant.IP_ADDRESS_KEY);
            if (!TextUtils.isEmpty(ipAddress)) {
                frameMetaData.setIpAddress(ipAddress);
            }
            frameMetaData.setBlurredFaces(0);
            String replacedString = new File(inputFile).getName().replace(".mp4", "");
            frameMetaData.setVideoId(replacedString);
            frameMetaData.setCollectorId(instance.readString(Constant.COLLECTOR_ID_KEY));
            String[] subjectIds = {instance.readString(Constant.SUBJECT_ID_TEXT)};
            frameMetaData.setSubjectIds(subjectIds);
            frameMetaData.setDeviceOrientation(displayOrientation);
            frameMetaData.setSensorOrientation(sensorOrientation);

            FrameJSON frameJSON = new FrameJSON(frameMetaData, activities, frameObjects);

            Type type = new TypeToken<FrameJSON>(){}.getType();
            String jsonString = new Gson().toJson(frameJSON, type);

            File file = FileUtil.writeToJSONFile(Constant.FRAMES_JSON_FILE_NAME, jsonString);
            if (file.exists() && file.length() <= 0){
                file.delete();
                bundle.putInt(COMPUTATION_ERROR_KEY, COMPUTATION_ERROR_CODE);
                bundle.putString(COMPUTATION_ERROR_MESSAGE_KEY, "Unable to process the video. Please try again");
                message.setData(bundle);
                handler.sendMessage(message);
                return;
            }
        } catch (Exception e) {
            Log.d(TAG, "run: image processing"+ e.getCause());
            bundle.putInt(COMPUTATION_ERROR_KEY, COMPUTATION_ERROR_CODE);
            bundle.putString(COMPUTATION_ERROR_MESSAGE_KEY, "Unable to process the video. Please try again");
            message.setData(bundle);
            handler.sendMessage(message);
            return;
        }

        bundle.putInt(COMPUTATION_SUCCESS_KEY, COMPUTATION_SUCCESS_CODE);
        bundle.putString(COMPUTATION_FILE_PATH_KEY, inputFile);
        message.setData(bundle);
        handler.sendMessage(message);
    }

    public void setDisplayOrientation(int displayOrientation) {
        this.displayOrientation = displayOrientation;
    }

    public void setSensorOrientation(int sensorOrientation) {
        this.sensorOrientation = sensorOrientation;
    }
}
