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

public class FrameUtil implements Runnable {

    private static final String TAG = "FrameUtil";

    private Handler handler;
    private String inputFile;
    private List<Coordinate> coordinates;
    private List<ActivityLabel> activities;
    private List<Coordinate> calibrated_coordinates = new ArrayList<>();
    private List<ActivityLabel> calibrated_activities = new ArrayList<>();
    private double annotationFrameDuration;

    private final int COMPUTATION_ERROR_CODE = 1;
    private final int COMPUTATION_SUCCESS_CODE = 2;

    public static final String COMPUTATION_ERROR_KEY = "error";
    public static final String COMPUTATION_ERROR_MESSAGE_KEY = "message";

    public static final String COMPUTATION_SUCCESS_KEY = "success";
    public static final String FACE_BLUR_COUNT = "face_blur_count";
    public static final String COMPUTATION_FILE_PATH_KEY = "FILE_PATH_KEY";
    private int deviceOrientation;
    private int sensorOrientation;

    public FrameUtil(
            Handler handler,
            String inputFile,
            List<Coordinate> coordinates,
            List<ActivityLabel> activities,
            double annotationFrameDuration) {
        this.handler = handler;
        this.inputFile = inputFile;
        this.coordinates = coordinates;
        this.activities = activities;
        this.annotationFrameDuration = annotationFrameDuration;
    }

    private void calibrate_frame_duration(double recordedframeDuration, double annotationFrameDuration) {
        // JEBYRNE:  collected vs. recorded frame duration
        // - First of all, this sucks.  Second, I want to note that this sucks.  Third, yuck.
        // - When recording a video from the camera, we cannot guarantee a framerate for some reason on older devices
        // - This means that we capture as fast as we can in VideoCaptureActivity and we get frames in the onCaptureCompleted callback
        // - When each frame is captured, we increment a frame counter and record the boxes and activity start/end frames indexed by this frameIndex
        // - We assume that each frame is recorded into the video in "inputFile", so that when we decode this video and read frame by frame, that each frame corresponds to each frameIndex.
        // - However, this is not true.  The videoRecorder attempts to encode a video, and generates a frameDuration at a given frameRate reported by FFMPEG.
        // - When we measure the actual frameDuration from subsequent calls to onCaptureCompleted, we see that the annotation frameDuration is shorter than the recorded frameDuration reported by FFMPEG.
        // - This manifests as the boxes start lagging the video.  A recorded video at frame i with frame duration p will be at time t=i*p, however the annotations at frame i with duration q will be at time i*q.
        //   If p > q, then for any frame i, the recorded video will be ahead of the annotations by an additive bias of (p-q)*i seconds.
        // - This means we need to align the annotations back to the recorded video.  So, frame j in the recorded video is at time j*p, then we need to index the annotations at round(j*p/q)

        this.calibrated_activities.clear();
        this.calibrated_coordinates.clear();

        for (int i = 0; i < coordinates.size(); i++) {
            int j = (int) Math.round((annotationFrameDuration*i) / recordedframeDuration);
            if (j < coordinates.size()) {
                Coordinate c = coordinates.get(j);
                int n = this.calibrated_coordinates.size();
                if (j <= (n - 1)) {
                    calibrated_coordinates.set(j, c);
                } else if (j == n) {
                    calibrated_coordinates.add(c);
                } else if (j > n) {
                    Coordinate e = coordinates.get(coordinates.size() - 1);
                    for (int k = 0; k < (j - n); k++) {
                        this.calibrated_coordinates.add(e);
                    }
                    this.calibrated_coordinates.add(c);
                } else {
                    // cannot get here
                }
            }
        }
        for (int i=0; i < activities.size(); i++) {
            ActivityLabel a = activities.get(i);
            a.setEndFrame((int) Math.round((annotationFrameDuration*a.getEndFrame()) / recordedframeDuration));
            a.setStartFrame((int) Math.round((annotationFrameDuration*a.getStartFrame()) / recordedframeDuration));
            calibrated_activities.add(a);
        }
        this.coordinates = this.calibrated_coordinates;  // in-place update
        this.activities = this.calibrated_activities;    // in-place update
    }

    @Override
    public void run() {
        Message message = handler.obtainMessage();
        Bundle bundle = new Bundle();

        try {
            Log.d("JEBYRNE", String.format("FrameUtil.run"));

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(inputFile);

            FFmpegMediaMetadataRetriever metadataRetriever = new FFmpegMediaMetadataRetriever();
            metadataRetriever.setDataSource(inputFile);

            double frameRate =
                    Double.parseDouble(
                            metadataRetriever.extractMetadata(
                                    FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE));

            int duration = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));  // JEBYRNE: integer milliseconds
            //int duration = Integer.parseInt(metadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION));  // JEBYRNE: integer milliseconds, this is wrong, floored to seconds

            double recordedFrameDuration = 1000 / frameRate;
            Log.d("JEBYRNE", String.format("FrameUtil.run: ffmpeg recorded frameDuration=%f, annotation frameDuration=%f", recordedFrameDuration, annotationFrameDuration));
            calibrate_frame_duration(recordedFrameDuration, annotationFrameDuration);  // mutate activities and coordinates in-place

            int width, height;
            if (deviceOrientation == Surface.ROTATION_0) {
                width =
                        Integer.parseInt(
                                metadataRetriever.extractMetadata(
                                        FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                height =
                        Integer.parseInt(
                                metadataRetriever.extractMetadata(
                                        FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            } else {
                height =
                        Integer.parseInt(
                                metadataRetriever.extractMetadata(
                                        FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                width =
                        Integer.parseInt(
                                metadataRetriever.extractMetadata(
                                        FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            }

            float v = (float) (duration / 1000);

            List<com.visym.collector.model.BoundingBox> boundingBoxes = new ArrayList<>();
            List<FrameObject> frameObjects = new ArrayList<>();
            FrameMetaData frameMetaData = new FrameMetaData();

            // JEBYRNE: this is where the annotation JSON file is created
            //
            // Global variable tracking (yuck):
            // - AppSharedPreference is defined in com.visym.collector.utils.AppSharedPreference
            // - This is global state accessible anywhere in the Application
            // - Global state is accessed as a dictionary with string keys defined in com.visym.collector.utils.Constant.Constant
            // - The state is written by
            //     - com/visym/collector/dashboardmodule/view/CaptureFragment::onCollectionSelection
            //     - com/visym/collector/dashboardmodule/view/CaptureFragment::initializeProjectListViews
            //     - com/visym/collector/dashboardmodule/view/ProjectCollections::onCollectionSelection
            //     - com/visym/collector/dashboardmodule/view/ProjectCollections::getCollectionProjects
            //     - com/visym/collector/capturemodule/views/CollectionsActivity::onCollectionSelection
            //
            // - These two methods also use a "redirectIntent.putExtra" with key "databundle" for shared state
            //     - com/visym/collector/dashboardmodule/view/ProjectCollections::onCollectionSelection
            //     - com/visym/collector/capturemodule/views/CollectionsActivity::onCollectionSelection
            //     - This data is written when a collection is selected to store the collection dictionary read from the backend to pass to the recorder.
            //     - An "intent" is an abstract definition of an action to perform and the data used to perform.  The action here is collecting a video, and the data is the collection details.
            //     - com/visym/collector/capturemodule/views/CollectionDetailsActivity intent is executed, then reads the databundle to collectionModelObject
            //     - The collectionModelObject is then stored as serialized JSON in AppSharedPreference under the key COLLECTION_KEY when the "Next" button is clicked.
            //     -
            AppSharedPreference instance = AppSharedPreference.getInstance();

            for (int i = 0; i < coordinates.size(); i++) {
                Coordinate coordinate = coordinates.get(i);
                Frame frame =
                        new Frame(
                                coordinate.getX(),
                                coordinate.getY(),
                                coordinate.getWidth(),
                                coordinate.getHeight());
                boundingBoxes.add(new com.visym.collector.model.BoundingBox(frame, i));
            }

            int totalFrames = (int) Math.round((duration * frameRate) / 1000);
            Log.d(TAG, "run: frameRate " + frameRate + " duration " + duration + " total frames " + totalFrames);
            if (totalFrames != coordinates.size()) {
                Log.d("JEBYRNE", String.format("FRAME LENGTH MISMATCH: %d", totalFrames - coordinates.size()));
            }
            if (totalFrames > coordinates.size()) {
                int diff = totalFrames - coordinates.size();
                Coordinate coordinate = coordinates.get(coordinates.size() - 1);
                int j = coordinates.size() - 1;
                for (int i = 0; i < diff; i++) {
                    j++;
                    Frame frame =
                            new Frame(
                                    coordinate.getX(),
                                    coordinate.getY(),
                                    coordinate.getWidth(),
                                    coordinate.getHeight());
                    boundingBoxes.add(new com.visym.collector.model.BoundingBox(frame, j));
                }
            }

            String frameObjectLabel = instance.readString(AppSharedPreference.FRAME_OBJECT_LABEL);
            String collection = instance.readString(Constant.COLLECTION_KEY);

            if (!TextUtils.isEmpty(collection)) {
                CollectionModel collectionModel = new Gson().fromJson(collection, CollectionModel.class);
                frameMetaData.setCategory(collectionModel.getActivities());
                frameMetaData.setShortName(collectionModel.getActivityShortNames());
            }

            FrameObject frameObject =
                    new FrameObject(
                            !TextUtils.isEmpty(frameObjectLabel) ? frameObjectLabel : "", boundingBoxes);
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
            frameMetaData.setDuration(v);  // JEBYRNE: fractional seconds
            String ipAddress = instance.readString(Constant.IP_ADDRESS_KEY);
            if (!TextUtils.isEmpty(ipAddress)) {
                frameMetaData.setIpAddress(ipAddress);
            }
            frameMetaData.setBlurredFaces(0);
            String replacedString = new File(inputFile).getName().replace(".mp4", "");
            frameMetaData.setVideoId(replacedString);
            frameMetaData.setCollectorId(instance.readString(Constant.COLLECTOR_ID_KEY));
            String subjectId = instance.readString(Constant.SUBJECT_ID_TEXT);
            if (!TextUtils.isEmpty(subjectId)) {
                String[] subjectIds = {subjectId};
                frameMetaData.setSubjectIds(subjectIds);
            } else {
                frameMetaData.setSubjectIds(new String[]{});
            }
            frameMetaData.setDeviceOrientation(deviceOrientation);
            frameMetaData.setSensorOrientation(sensorOrientation);

            FrameJSON frameJSON = new FrameJSON(frameMetaData, activities, frameObjects);

            Type type = new TypeToken<FrameJSON>() {
            }.getType();
            String jsonString = new Gson().toJson(frameJSON, type);

            File file = FileUtil.writeToJSONFile(Constant.FRAMES_JSON_FILE_NAME, jsonString);
            if (file.exists() && file.length() <= 0) {
                file.delete();
                bundle.putInt(COMPUTATION_ERROR_KEY, COMPUTATION_ERROR_CODE);
                bundle.putString(
                        COMPUTATION_ERROR_MESSAGE_KEY, "Unable to process the video. Please try again");
                message.setData(bundle);
                handler.sendMessage(message);
                return;
            }
        } catch (Exception e) {
            //e.printStackTrace();
            Log.d(TAG, "FrameUtil.run: JSON creation error" + e.getCause());
            bundle.putInt(COMPUTATION_ERROR_KEY, COMPUTATION_ERROR_CODE);
            bundle.putString(
                    COMPUTATION_ERROR_MESSAGE_KEY, "Unable to process the video. Please try again");
            message.setData(bundle);
            handler.sendMessage(message);
            return;
        }

        bundle.putInt(COMPUTATION_SUCCESS_KEY, COMPUTATION_SUCCESS_CODE);
        bundle.putString(COMPUTATION_FILE_PATH_KEY, inputFile);
        message.setData(bundle);
        handler.sendMessage(message);
    }

    public void setDeviceOrientation(int deviceOrientation) {
        this.deviceOrientation = deviceOrientation;
    }

    public void setSensorOrientation(int sensorOrientation) {
        this.sensorOrientation = sensorOrientation;
    }
}
