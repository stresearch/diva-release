package com.visym.collector.capturemodule.views;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.amazonaws.amplify.generated.graphql.CreateStrSubjectMutation;
import com.amazonaws.amplify.generated.graphql.StrCollectorByEmailQuery;
import com.amazonaws.amplify.generated.graphql.UpdateStrCollectorMutation;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.visym.collector.BaseActivity;
import com.visym.collector.R;
//import com.visym.collector.capturemodule.facedetection.ImageProcessor;
import com.visym.collector.capturemodule.interactor.VideoUploadInteractor;
import com.visym.collector.capturemodule.model.ActivityLabel;
import com.visym.collector.capturemodule.model.FrameJSON;
import com.visym.collector.capturemodule.model.FrameMetaData;
import com.visym.collector.capturemodule.model.FrameObject;
import com.visym.collector.capturemodule.presenters.VideoUploadPresenter;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.model.BoundingBox;
import com.visym.collector.model.CollectionModel;
import com.visym.collector.model.Frame;
import com.visym.collector.network.ConnectivityReceiver;
import com.visym.collector.usermodule.view.FrontScreenActivity;
import com.visym.collector.usermodule.view.LoginActivity;
import com.visym.collector.usermodule.view.UserSetUpActivity;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.AwsResponse;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.DateUtil;
import com.visym.collector.utils.FileUtil;
import com.visym.collector.utils.FrameUtil;
import com.visym.collector.utils.Globals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;
import type.CreateStrSubjectInput;
import type.UpdateStrCollectorInput;
import wseemann.media.FFmpegMediaMetadataRetriever;

import static com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers.NETWORK_ONLY;

public class VideoPreviewActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener,
        MediaPlayer.OnPreparedListener, View.OnClickListener, MediaPlayer.OnInfoListener, View.OnTouchListener,
        VideoUploadInteractor.VideoUploadView, MediaPlayer.OnErrorListener {

    private static final String TAG = "VideoPreviewActivity";

    @BindView(R.id.preview_video_view)
    VideoView previewVideoView;

    @BindView(R.id.cancel_image_view)
    ImageView cancelButton;

    @BindView(R.id.retake_image_view)
    ImageView retakeImageView;

    @BindView(R.id.progress_seekbar)
    SeekBar progressSeekBar;

    @BindView(R.id.submit_textview)
    TextView submitButton;

    @BindView(R.id.running_time_textview)
    TextView runningTextView;

    @BindView(R.id.total_time_textview)
    TextView totalTimeTextView;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @BindView(R.id.pause_play_button)
    ImageButton pausePlayButton;

    @BindView(R.id.activity_textview)
    TextView activityNameTextView;

    @BindView(R.id.bounding_box_view)
    ImageView boundingBoxView;

    @BindView(R.id.preview_rootview)
    RelativeLayout rootView;

    @BindView(R.id.option_view)
    RelativeLayout optionView;

    @BindView(R.id.edit_video_icon)
    ImageView editVideoIcon;
    JSONArray responseArray;
    private String videoFilePath;
    private double frameRate;  // JEBYRNE: int -> double
    private int totalFrames;
    private int seekBarIncrementCounter;
    private MediaPlayer mediaPlayer;
    private boolean isRetakeVideo = false;
    private VideoPlayerTimer playerTimer;
    private int videoTotalDuration;
    private double frameDuration;
    private FrameJSON frameJSON;
    private boolean fromConsent;
    private VideoUploadPresenter presenter;
    private int currentOrientation;
    private Context mcontext;
    private String orientation, videoUrl, jsonUrl;
    private OrientationEventListener orientationEventListener;
    private int displayWidth;
    private int displayHeight;
    private AppSharedPreference preference;
    private boolean seekBarDisabled = false;
//    private String newVideoFilePath;
    private boolean videoProcessed = false;
    private boolean replayVideo = false;
    private boolean videoEditEnabled = false;
    private int stoppedPosition;

    private String jsonFilePath;

//    private ImageProcessor imageProcessor;
//    private Handler handler = new Handler(Looper.getMainLooper()) {
//        @Override
//        public void handleMessage(@NonNull Message msg) {
//            super.handleMessage(msg);
//            Bundle data = msg.getData();
//            if (data.containsKey(FrameUtil.COMPUTATION_SUCCESS_KEY)) {
//                videoProcessed = true;
//                newVideoFilePath = data.getString(FrameUtil.COMPUTATION_SUCCESS_KEY);
//            }
//            faceBlurCount = data.getInt(FrameUtil.FACE_BLUR_COUNT);
//            if (videoEditEnabled) {
//                if (progressDialog != null) {
//                    progressDialog.dismiss();
//                    progressDialog = null;
//                }
//                navigateToVideoEditor();
//            } else if (progressDialog != null) {
//                progressDialog.dismiss();
//                progressDialog = null;
//
//                String collectionString = preference.readString(Constant.COLLECTION_KEY);
//                if (!TextUtils.isEmpty(collectionString)) {
//                    CollectionModel collection = new Gson().fromJson(collectionString, CollectionModel.class);
//                    String activityShortNames = collection.getActivityShortNames();
//                    if (TextUtils.isEmpty(activityShortNames)) {
//                        continueSubmittingVideo();
//                    } else if (frameJSON != null) {
//                        List<ActivityLabel> activities = frameJSON.getActivity();
//                        if (activities != null && activities.isEmpty()) {
//                            displayNoActivityDialog();
//                        } else {
//                            continueSubmittingVideo();
//                        }
//                    }
//                }
//            }
//        }
//    };

    private Handler timerHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };

    private GraphQLCall.Callback<UpdateStrCollectorMutation.Data> updateResponse = new GraphQLCall.Callback<UpdateStrCollectorMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<UpdateStrCollectorMutation.Data> response) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideProgress();
                    preference.storeValue(Constant.IS_USER_CONSENTED, "true");
                    if (!Globals.isSetupFlow) {
                        boolean isPlayTrainingVideo = preference.readBoolean(Constant.IS_PLAY_TRAINING);
                        if (isPlayTrainingVideo) {
                            String trainingVideoURL = preference.readString(Constant.TRAINING_VIDEO_URL);
                            if (TextUtils.isEmpty(trainingVideoURL) || trainingVideoURL.contentEquals("[]")) {
                                Intent goNextIntent = new Intent(VideoPreviewActivity.this,
                                        VideoPlayerActivity.class);
                                goNextIntent.putExtra("module", "notraining");
                                goNextIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(goNextIntent);
                                finish();
                            } else {
                                Intent goNextIntent = new Intent(VideoPreviewActivity.this,
                                        VideoPlayerActivity.class);
                                goNextIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                goNextIntent.putExtra("module", "training");
                                startActivity(goNextIntent);
                                finish();
                            }
                        } else {
                            Intent setupScreenIntent = new Intent(VideoPreviewActivity.this, VideoCaptureActivity.class);
                            startActivity(setupScreenIntent);
                        }

                    } else {
                        Globals.isSetupFlow = false;
                        Globals.showSnackBar(getResources().getString(R.string.consentSuccess), mcontext, Snackbar.LENGTH_LONG);

                        Intent setupScreenIntent = new Intent(VideoPreviewActivity.this, UserSetUpActivity.class);
                        startActivity(setupScreenIntent);
                    }
                    finish();
                }
            });
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideProgress();
                    //Globals.showSnackBar(getResources().getString(R.string.unknown_error_message), mcontext, Snackbar.LENGTH_LONG);
                    logout();
                }
            });
        }
    };
    private GraphQLCall.Callback<CreateStrSubjectMutation.Data> consentMutation =
            new GraphQLCall.Callback<CreateStrSubjectMutation.Data>() {
                @Override
                public void onResponse(@Nonnull Response<CreateStrSubjectMutation.Data> response) {
                    if (response.data() != null && response.data().createStrSubject() != null) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONArray recentSubjects = new JSONArray(preference.readString(Constant.SUBJECT_EMAIL_TEXT));
                                    String collectorId = response.data().createStrSubject().collector_id();
                                    //preference.storeValue(Constant.SUBJECT_ID_TEXT, collectorId);  // JEBYRNE: do not do this here,
                                    String collectorEmail = preference.readString(Constant.COLLECTOR_EMAIL);
                                    if (!Globals.isSetupFlow) {
                                        if (collectorEmail.equals(recentSubjects.getString(recentSubjects.length() - 1))) {
                                            UpdateStrCollectorInput userData = UpdateStrCollectorInput.builder()
                                                    .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY))
                                                    .collector_email(collectorEmail)
                                                    .is_consented(true).build();
                                            Globals.mAWSAppSyncClient.mutate(UpdateStrCollectorMutation.builder()
                                                    .input(userData).build()).enqueue(updateResponse);
                                        } else {
                                            hideProgress();
                                            Toast.makeText(VideoPreviewActivity.this,
                                                    "Consent video uploaded successfully", Toast.LENGTH_SHORT).show();
                                            boolean isPlayTrainingVideo = preference.readBoolean(Constant.IS_PLAY_TRAINING);
                                            if (isPlayTrainingVideo) {
                                                String trainingVideoURL = preference.readString(Constant.TRAINING_VIDEO_URL);
                                                if (TextUtils.isEmpty(trainingVideoURL) || trainingVideoURL.contentEquals("[]")) {
                                                    /*displayNoTrainingVideoDialog();*/
                                                    Intent goNextIntent = new Intent(VideoPreviewActivity.this,
                                                            VideoPlayerActivity.class);
                                                    goNextIntent.putExtra("module", "notraining");
                                                    goNextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    startActivity(goNextIntent);
                                                    finish();
                                                } else {
                                                    Intent goNextIntent = new Intent(VideoPreviewActivity.this,
                                                            VideoPlayerActivity.class);
                                                    goNextIntent.putExtra("module", "training");
                                                    goNextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    startActivity(goNextIntent);
                                                    finish();
                                                }

                                            } else {
                                                Intent setupScreenIntent = new Intent(VideoPreviewActivity.this, VideoCaptureActivity.class);
                                                startActivity(setupScreenIntent);
                                                finish();
                                            }
                                        }
                                    } else {
                                        UpdateStrCollectorInput userData = UpdateStrCollectorInput.builder()
                                                .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY))
                                                .collector_email(collectorEmail)
                                                .is_consented(true).build();
                                        Globals.mAWSAppSyncClient.mutate(UpdateStrCollectorMutation.builder()
                                                .input(userData).build()).enqueue(updateResponse);
                                    }
                                } catch (Exception e) {
                                    hideProgress();
                                    Toast.makeText(VideoPreviewActivity.this,
                                            "Consent video uploaded successfully", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(VideoPreviewActivity.this,
                                            VideoCaptureActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgress();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideProgress();
                            //Toast.makeText(VideoPreviewActivity.this,
                            //        "Failed to upload video. Please try again", Toast.LENGTH_SHORT).show();
                            //Intent captureVideoIntent = new Intent(VideoPreviewActivity.this,
                            //        ConsentVideoCaptureActivity.class);
                            //startActivity(captureVideoIntent);
                            logout();
                        }
                    });
                }
            };
    private GraphQLCall.Callback<StrCollectorByEmailQuery.Data> collectorCallback
            = new GraphQLCall.Callback<StrCollectorByEmailQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<StrCollectorByEmailQuery.Data> response) {
            if (!response.hasErrors()) {
                if (response.data().StrCollectorByEmail() != null) {
                    String collectorId = null;
                    if (response.data().StrCollectorByEmail().items().size() > 0) {
                        collectorId = response.data().StrCollectorByEmail().items().get(0).collector_id();
                    }
                    if (TextUtils.isEmpty(collectorId)) {
                        collectorId = UUID.randomUUID().toString();  // JEBYRNE: WHAT??
                    }
                    preference.storeValue(Constant.SUBJECT_ID_TEXT, collectorId);  // JEBYRNE: FOR JSON

                    try {
                        JSONArray recentSubjects = new JSONArray(preference.readString(Constant.SUBJECT_EMAIL_TEXT));
                        String programName = preference.readString(Constant.PROGRAM_ID_KEY);  // JEBYRNE

                        String s = AppSharedPreference.getInstance().readString(Constant.CONSENT_QUESTION_RESPONSE);
                        responseArray = new JSONArray(s);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("response", responseArray);

                        CreateStrSubjectInput consentSubject = CreateStrSubjectInput.builder()
                                .consent_response(String.valueOf(jsonObject))
                                .subject_email(programName + '_' + recentSubjects.getString(recentSubjects.length() - 1))  // JEBYRNE
                                .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY))
                                .collector_email(preference.readString(Constant.COLLECTOR_EMAIL))
                                .consent_video_id(videoUrl)
                                .uuid(collectorId)
                                .status("active")
                                .build();

                        preference.storeValue(Constant.SUBJECT_ID_TEXT, collectorId);  // JEBYRNE: this is for frameJson, do this here instead of inside consentMutation

                        Globals.getAppSyncClient()
                                .mutate(CreateStrSubjectMutation.builder().input(consentSubject).build())
                                .enqueue(consentMutation);
                    } catch (Exception e) {
                        hideProgress();
                        Toast.makeText(VideoPreviewActivity.this,
                                "Failed to upload video", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            } else {
                hideProgress();
                Toast.makeText(VideoPreviewActivity.this,
                        "Failed to upload video", Toast.LENGTH_LONG).show();
                finish();
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            hideProgress();
            //Toast.makeText(VideoPreviewActivity.this,
            //        "Failed to upload video", Toast.LENGTH_LONG).show();
            logout();
        }
    };
    private int faceBlurCount;
//    private ProgressDialog progressDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d("JEBYRNE", "VideoPreviewActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview_layout);
        ButterKnife.bind(this);
        presenter = new VideoUploadPresenter(this);
        preference = AppSharedPreference.getInstance();
        mcontext = this;
        getApplicationContext().startService(new Intent(getApplicationContext(), TransferService.class));

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Constant.VIDEO_FILE_PATH_KEY)) {
            videoFilePath = intent.getStringExtra(Constant.VIDEO_FILE_PATH_KEY);
        }

        if (intent != null && intent.hasExtra(VideoPlayerActivity.INTENT_CONSENT_TEXT)) {
            fromConsent = intent.getBooleanExtra(VideoPlayerActivity.INTENT_CONSENT_TEXT, false);
        }
        if (intent != null && intent.hasExtra("isRetakeVideo")) {
            isRetakeVideo = intent.getBooleanExtra("isRetakeVideo", false);
        }

        progressSeekBar.setOnSeekBarChangeListener(this);
        progressSeekBar.setOnTouchListener(this);
        previewVideoView.setOnPreparedListener(this);
        cancelButton.setOnClickListener(this);
        retakeImageView.setOnClickListener(this);
        submitButton.setOnClickListener(this);
        pausePlayButton.setOnClickListener(this);
        editVideoIcon.setOnClickListener(this);
        previewVideoView.setOnTouchListener(this);

        if (fromConsent) {
            editVideoIcon.setVisibility(View.GONE);
            init();
        } else {
            orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
                @Override
                public void onOrientationChanged(int orientation) {
                    if ((orientation >= 0 && orientation < 45) || (orientation >= 315 && orientation < 360)) {
                        int rotation = Surface.ROTATION_0;
                        if (rotation != currentOrientation) {
                            currentOrientation = rotation;
                        }
                        onOrientationChange(currentOrientation);
                    } else if (orientation >= 45 && orientation < 135) {
                        int rotation = Surface.ROTATION_90;
                        onOrientationChange(currentOrientation);
                        if (rotation != currentOrientation) {
                            currentOrientation = rotation;
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                        }
                    } else if (orientation >= 135 && orientation < 225) {
                        int rotation = Surface.ROTATION_180;
                        if (rotation != currentOrientation) {
                            currentOrientation = rotation;
                        }
                    } else if (orientation >= 225 && orientation < 315) {
                        int rotation = Surface.ROTATION_270;
                        onOrientationChange(currentOrientation);
                        if (rotation != currentOrientation) {
                            currentOrientation = rotation;
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        }
                    }
                }
            };

            AppSharedPreference instance = AppSharedPreference.getInstance();
            orientation = instance.readString(AppSharedPreference.ORIENTATION_KEY);
            if (orientation != null && orientation.contains("landscape")) {
                orientationEventListener.enable();
                if (orientation.contains("landscapeLeft")) {
                    currentOrientation = Surface.ROTATION_270;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else if (orientation.contains("landscapeRight")) {
                    currentOrientation = Surface.ROTATION_90;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                }
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    AsyncTask.execute(() -> {
                        try {
                            frameJSON = FileUtil.readFromFile(VideoPreviewActivity.this, Constant.FRAMES_JSON_FILE_NAME);
                            if (frameJSON == null) {
                                throw new Exception("Empty frameJSON");
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    init();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            logout();  // JEBYRNE
                        }
                    });
                }
            }, 1000);
        }

        //if (!fromConsent) {
        if (true) {  // Always get coarse location 	
            getPublicIpAddress();
        } else {
            preference.storeValue(Constant.IP_ADDRESS_KEY, "");
        }
    }

    private void onOrientationChange(int orientation) {
        int x = (int) previewVideoView.getX();
        int y = (int) previewVideoView.getY();

        switch (orientation) {
            case Surface.ROTATION_0:
                optionView.setPadding(x, y + (int) getResources().getDimension(R.dimen.semiSmallMargin), x, 0);
                break;

            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                optionView.setPadding(x, (int) getResources().getDimension(R.dimen.semiSmallMargin), x, 0);
                break;
        }
    }

    public void getPublicIpAddress() {
        AsyncTask.execute(() -> {
            try (Scanner s = new Scanner(new java.net.URL("https://api.ipify.org")
                    .openStream(), "UTF-8").useDelimiter("\\A")) {
                preference.storeValue(Constant.IP_ADDRESS_KEY, s.next());
            } catch (java.io.IOException e) {
                e.printStackTrace();
                preference.storeValue(Constant.IP_ADDRESS_KEY, "");
            }
        });
    }

    private void init() {
        Log.d("JEBYRNE", "VideoPreviewActivity.init");
        previewVideoView.setVideoURI(Uri.fromFile(new File(videoFilePath)));
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(videoFilePath);
        } catch (IOException e) {
            Toast.makeText(this, "Recorded video is corrupted", Toast.LENGTH_LONG)
                    .show();
            finish();
        }
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            String mimeType = format.getString(MediaFormat.KEY_MIME);
            if (mimeType != null && mimeType.startsWith("video/")) {
                mediaExtractor.selectTrack(i);

                FFmpegMediaMetadataRetriever metadataRetriever = new FFmpegMediaMetadataRetriever();
                metadataRetriever.setDataSource(videoFilePath);

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(videoFilePath);

                frameRate = Double.parseDouble(metadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE));
                //long millis = Integer.parseInt(metadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION));  // JEBYRNE: integer milliseconds
                long millis = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));  // JEBYRNE: integer milliseconds  (FFMPEG is wrong, truncated to seconds)
                videoTotalDuration = (int) millis;  // JEBYRNE: set here instead of onPrepared

                frameDuration = (1000 / frameRate);  // JEBYRNE: this is now fractional milliseconds
                Log.d("JEBYRNE", String.format("VideoPreviewActivity.init: videoTotalDuration=%d, frameRate=%f, frameDuration=%f", videoTotalDuration, frameRate, frameDuration));

                //frameRate = format.getFloat(MediaFormat.KEY_FRAME_RATE);
                //long millis = TimeUnit.MICROSECONDS.toMillis(format.getLong(MediaFormat.KEY_DURATION));
                if (millis < 1000) {
                    totalFrames = (int) (frameRate * (int) (1));  // JEBYRNE: this is not precisely correct
                } else {
                    totalFrames = (int) (frameRate * (int) (millis / 1000));  // JEBYRNE: floor to int
                }

//                try {
//                    if (!replayVideo && !fromConsent) {
//                        FrameMetaData metaData = frameJSON.getMetaData();
//                        String outputFile = FileUtil.getVideoFilePath(Globals.getAppContext(), "capture");
//                        imageProcessor = new ImageProcessor(handler, videoFilePath, outputFile);
//
//                        String frameObjectLabel = preference.readString(AppSharedPreference.FRAME_OBJECT_LABEL);
//                        if (frameJSON != null) {
//                            List<FrameObject> objects = frameJSON.getObject();
//                            if (objects != null && !objects.isEmpty()) {
//                                for (FrameObject object : objects) {
//                                    if (object.getLabel().equalsIgnoreCase(frameObjectLabel)) {
//                                        imageProcessor.setDefaultObject(object);
//                                        break;
//                                    }
//                                }
//                            }
//                        }
//                        imageProcessor.setDeviceOrientation(metaData.getDeviceOrientation());
//                        imageProcessor.setSensorOrientation(metaData.getSensorOrientation());
//                        Thread thread = new Thread(imageProcessor);
//                        thread.start();
//                    }
//                } catch (Exception e) {
//                    Log.d(TAG, "init: " + "Failed encoding and decoding");
//                    FirebaseCrashlytics.getInstance().recordException(Objects.requireNonNull(e.getCause()));
//                }
                playerTimer = new VideoPlayerTimer();
                mediaExtractor.release();
                break;
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            previewVideoView.seekTo(progress);
            if (playerTimer != null) {
                playerTimer.setFrameIndex(progress);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d("JEBYRNE", "VideoPreviewActivity.onPrepared");
        if (totalFrames == 0) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Corrupted video file", Toast.LENGTH_LONG).show();
            finish();
        } else if (this.mediaPlayer == null) {
            this.mediaPlayer = mp;
            mediaPlayer.setOnInfoListener(this);
            mediaPlayer.setOnErrorListener(this);
            //videoTotalDuration = mediaPlayer.getDuration();  // JEBYRNE: set this in onCreate from video file metadata, (retrieving from media is off by a few seconds for some reason)


            seekBarDisabled = false;
            progressSeekBar.setMax(videoTotalDuration);
            seekBarIncrementCounter = videoTotalDuration / totalFrames;

            pausePlayButton.setVisibility(View.GONE);
            totalTimeTextView.setText(getString(R.string.initial_timer_text));
            runningTextView.setText(getString(R.string.initial_timer_text));
            totalTimeTextView.setText(DateUtil.getTotalVideoTime(videoTotalDuration));

            previewVideoView.post(new Runnable() {
                @Override
                public void run() {
                    onOrientationChange(currentOrientation);
                    mp.start();
                    playerTimer.run();
                    progressBar.setVisibility(View.GONE);
                }
            });
        } else {
            previewVideoView.seekTo(Math.max(stoppedPosition, 0));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideSystemBar();
        if (mediaPlayer != null) {
            timerHandler.removeCallbacks(playerTimer);
            previewVideoView.pause();
            stoppedPosition = progressSeekBar.getProgress();
        }
    }

    protected void stopPreview() {
        playerTimer.reset();
        previewVideoView.seekTo(0);
        previewVideoView.pause();
        progressSeekBar.setProgress(0);
        runningTextView.setText(getString(R.string.initial_timer_text));
        timerHandler.removeCallbacks(playerTimer);
        pausePlayButton.setVisibility(View.VISIBLE);
        boundingBoxView.setVisibility(View.GONE);
        activityNameTextView.setVisibility(View.GONE);
        seekBarDisabled = true;
    }

    private void updateVideoProgress() {
        pausePlayButton.setVisibility(View.GONE);
        previewVideoView.start();
        timerHandler.post(playerTimer);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel_image_view:
                displayDialog("Exit this collection?", 0);
                break;

            case R.id.edit_video_icon:
                if (previewVideoView.isPlaying()) {
                    timerHandler.removeCallbacks(playerTimer);
                    previewVideoView.pause();
                }
                //videoEditEnabled = true;
                //navigateToVideoEditor();

                // JEBYRNE: do not allow editing after recording.
                // - There is a problem with slower network connections failing to upload the video, but then successfully uploading the edits.
                // - This manifests as "Missing MP4" errors in the editor lambda
                // - To avoid this, force collectors to submit the video (which has better error handling), then edit after
                Toast.makeText(this, "Live video editor temporarily disabled.  To edit, submit this video, then edit and resubmit after this video is available in your Video feed.", Toast.LENGTH_LONG).show();
                break;

            case R.id.retake_image_view:
                if (fromConsent) {
                    displayDialog("Retake consent?", 1);
                } else {
                    displayDialog("Retake collection?", 1);
                }
                break;

            case R.id.submit_textview:
                if (fromConsent) {
                    if (ConnectivityReceiver.isConnected()) {
                        if (isRetakeVideo) {
                            Globals.isRetakeConsentVideo = true;
                        }
                        showProgress("Uploading video...", true);
                        presenter.uploadVideo(videoFilePath, AwsResponse.FILE_TYPE_CONSENT);
                    } else {
                        Toast.makeText(this, "Check internet connection and try again", Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (frameJSON == null) {
                        logout();
                    }
                    else {
                        String collectionString = preference.readString(Constant.COLLECTION_KEY);
                        if (!TextUtils.isEmpty(collectionString)) {
                            CollectionModel collection = new Gson().fromJson(collectionString, CollectionModel.class);
                            String activityShortNames = collection.getActivityShortNames();
                            if (TextUtils.isEmpty(activityShortNames)) {
                                continueSubmittingVideo();
                            } else if (frameJSON != null) {
                                List<ActivityLabel> activities = frameJSON.getActivity();
                                if (activities != null && activities.isEmpty()) {
                                    displayNoActivityDialog();
                                    //continueSubmittingVideo();  // JEBYRNE: disable me
                                } else {
                                    continueSubmittingVideo();
                                }
                            }
                        }
                    }
                }
                break;

            case R.id.pause_play_button:
                replayVideo = true;
                updateVideoProgress();
                break;
        }
    }

//    private void showVideoProcessingDialog() {
//        progressDialog = new ProgressDialog(this);
//        progressDialog.setMessage("Processing video please wait...");
//        progressDialog.setCancelable(false);
//        progressDialog.show();
//        Window window = progressDialog.getWindow();
//        if (window != null) {
//            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
//                    | View.SYSTEM_UI_FLAG_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
//        }
//    }

    private void navigateToVideoEditor() {
        try {
            if (frameJSON != null && frameJSON.getMetaData() != null) {
                frameJSON.getMetaData().setBlurredFaces(faceBlurCount);
                Type type = new TypeToken<FrameJSON>() {
                }.getType();
                String jsonString = new Gson().toJson(frameJSON, type);
                FileUtil.writeToJSONFile(Constant.FRAMES_JSON_FILE_NAME, jsonString);
            }
            else {
                logout();  // JEBYRNE
            }
        } catch (Exception e) {
            logout();    // JEBYRNE
        }

        Intent intent = new Intent(VideoPreviewActivity.this, VideoEditorActivity.class);
        intent.putExtra(Constant.VIDEO_CONTENT_KEY, true);
        intent.putExtra("jsonURL", new File(getCacheDir(), Constant.FRAMES_JSON_FILE_NAME).getAbsolutePath());
        intent.putExtra("videoURL", videoFilePath);
        intent.putExtra("collectionId", preference.readString(Constant.COLLECTION_ID_KEY));
        intent.putExtra(Constant.IS_VIDEO_EDITABLE, true);
        intent.putExtra("originalVideoPath", videoFilePath);
        if (Globals.isShowingLoader()) {
            Globals.dismissLoading();
        }
        startActivity(intent);
    }

    private void displayNoActivityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setCancelable(false)
                .setMessage("Your video has no activities annotated. Submit?")
                .setPositiveButton("OK", (dialog, which) -> {
                    continueSubmittingVideo();
                })
                .setNegativeButton("CANCEL", (dialog, which) -> {
                    FileUtil.deleteFile(videoFilePath);
                    Intent intent = new Intent(VideoPreviewActivity.this, VideoCaptureActivity.class);
                    startActivity(intent);
                    finish();
                });
        if(!this.isFinishing()) {
            showDialogAndHideSystemBar(builder.create());
        }
    }

    private void continueSubmittingVideo() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ConnectivityReceiver.isConnected()) {
                    try {
                        showProgress("Uploading video...", true);

                        String filePath = FileUtil.getFilePath(Constant.FRAMES_JSON_FILE_NAME);
                        presenter.uploadVideo(filePath + "," + videoFilePath, AwsResponse.FILE_TYPE_JSON);

                    } catch (Exception e) {
                        hideProgress();
                        Toast.makeText(VideoPreviewActivity.this, "Failed to read file. Please try again", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(VideoPreviewActivity.this, "Check your internet connection and try again", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void displayDialog(String message, int what) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton("YES", (dialog, which) -> {
                    if (fromConsent) {
                        FileUtil.deleteFile(videoFilePath);
                        if (isRetakeVideo) {
                            if (what == 1) {
                                Intent intent = new Intent(this, ConsentVideoCaptureActivity.class);
                                intent.putExtra(VideoPlayerActivity.INTENT_CONSENT_TEXT, fromConsent);
                                intent.putExtra("isRetakeVideo", isRetakeVideo);
                                startActivity(intent);
                            }
                        } else {
                            if (what == 0) {
                                try {
                                    JSONArray recentSubjects = new JSONArray(preference.readString(Constant.SUBJECT_EMAIL_TEXT));
                                    if (recentSubjects.length() > 0) {
                                        recentSubjects.remove(recentSubjects.length() - 1);
                                        preference.storeValue(Constant.SUBJECT_EMAIL_TEXT, recentSubjects.toString());
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                Intent intent = new Intent(this, DashboardActivity.class);
                                intent.putExtra("redirectTo", "capture");
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                startActivity(intent);
                            } else {
                                Intent intent = new Intent(this, ConsentVideoCaptureActivity.class);
                                intent.putExtra(VideoPlayerActivity.INTENT_CONSENT_TEXT, fromConsent);
                                intent.putExtra("isRetakeVideo", isRetakeVideo);
                                startActivity(intent);
                            }
                        }
                    } else {
//                        if (imageProcessor != null) {
//                            imageProcessor.stopProcessing();
//                            imageProcessor = null;
//                        }
                        Intent intent;
                        if (what == 0) {
                            intent = new Intent(this, DashboardActivity.class);
                            intent.putExtra("redirectTo", "capture");
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        } else {
                            intent = new Intent(this, VideoCaptureActivity.class);
                        }
                        startActivity(intent);
                    }
                    finish();
                })
                .setNegativeButton("NO", (dialog, which) -> {
                });
        if(!((VideoPreviewActivity)this).isFinishing()) {
            showDialogAndHideSystemBar(builder.create());
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("JEBYRNE", "VideoPreviewActivity.onBackPressed");

        if (fromConsent) {
            try {
                JSONArray recentSubjects = new JSONArray(preference.readString(Constant.SUBJECT_EMAIL_TEXT));
                if (recentSubjects.length() > 0) {
                    recentSubjects.remove(recentSubjects.length() - 1);
                    preference.storeValue(Constant.SUBJECT_EMAIL_TEXT, recentSubjects.toString());
                }
                Intent intent = new Intent(this, ConsentVideoCaptureActivity.class);
                startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (isRetakeVideo) {
            Intent videoCaptureIntent = new Intent(this,
                    ConsentVideoCaptureActivity.class);
            videoCaptureIntent.putExtra("isRetakeVideo", true);
            startActivity(videoCaptureIntent);
        }
        if (!TextUtils.isEmpty(orientation) && orientation.contains("land")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        finish();
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            timerHandler.removeCallbacks(playerTimer);
            progressBar.setVisibility(View.VISIBLE);
            return true;
        }
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            timerHandler.post(playerTimer);
            progressBar.setVisibility(View.GONE);
            return true;
        }
        progressBar.setVisibility(View.GONE);
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideProgress();
        hideSystemBar();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.preview_video_view:
                if (event.getAction() == MotionEvent.ACTION_DOWN && event.getPointerCount() == 1) {
                    if (previewVideoView.isPlaying()) {
                        timerHandler.removeCallbacks(playerTimer);
                        previewVideoView.pause();
                    } else {
                        if (pausePlayButton.getVisibility() == View.VISIBLE) {
                            return false;
                        }
                        timerHandler.post(playerTimer);
                        previewVideoView.start();
                    }
                }
                return true;

            case R.id.progress_seekbar:
                if (previewVideoView.isPlaying()) {
                    seekBarDisabled = false;
                }
                return seekBarDisabled;
        }
        return false;
    }

    @Override
    public void onFileUploadSuccess(AwsResponse response) {
        TransferState state = response.getState();
        if (state == TransferState.COMPLETED) {
            int fileType = response.getFileType();
            switch (fileType) {
                case AwsResponse.FILE_TYPE_VIDEO:
                    videoUrl = response.getVideoUrl();
                    hideProgress();
                    AlertDialog.Builder builder = new AlertDialog.Builder(VideoPreviewActivity.this);
                    builder.setMessage("Video uploaded successfully");
                    builder.setCancelable(false);
                    builder.setPositiveButton("OK", (dialog1, which) -> {
                        if (!TextUtils.isEmpty(orientation) && orientation.contains("land")) {
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        }
                        FileUtil.deleteFile(jsonUrl);
                        FileUtil.deleteFile(videoUrl);
                        Intent intent = new Intent(VideoPreviewActivity.this, DashboardActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("redirectTo", "capture");
                        startActivity(intent);
                        finish();
                    });
                    if(!((VideoPreviewActivity)this).isFinishing()) {
                        showDialogAndHideSystemBar(builder.create());
                    }
                    break;

                case AwsResponse.FILE_TYPE_JSON:
                    // JEBYRNE: upload order
                    // - AFter the JSON is successfully uploaded, then upload the video, then and only then show success
                    // - This terrible code is going to give me a brain tumor
                    jsonUrl = response.getVideoUrl();
                    presenter.uploadVideo(videoFilePath, AwsResponse.FILE_TYPE_VIDEO);
                    break;

                case AwsResponse.FILE_TYPE_CONSENT:
                    videoUrl = response.getVideoUrl();
                    storeConsentDetails();
                    break;
            }
        } else if (state == TransferState.IN_PROGRESS) {
            int progress = response.getProgress();
            updateProgress(progress);
        } else if (state == TransferState.WAITING_FOR_NETWORK) {
            updateMessage("Waiting for network...");
        } else if (state == TransferState.CANCELED) {
            hideProgress();
        } else if (state == TransferState.FAILED) {

            hideProgress();
            String errorMessage = response.getErrorMessage();
            if (!TextUtils.isEmpty(errorMessage)) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Upload failed", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void storeConsentDetails() {
        try {
            if (isRetakeVideo) {
                Toast.makeText(this, "Consent video uploaded successfully", Toast.LENGTH_SHORT)
                        .show();
                Globals.isRetakeConsentVideo = false;
                finish();
            } else {
                String recentSubjectEmailList = preference.readString(Constant.SUBJECT_EMAIL_TEXT);
                String collectorEmail = preference.readString(Constant.COLLECTOR_EMAIL);
                String programName = preference.readString(Constant.PROGRAM_ID_KEY);  // JEBYRNE
                JSONArray recentSubjects;
                if (recentSubjectEmailList == null) {
                    recentSubjects = new JSONArray(collectorEmail);
                } else {
                    recentSubjects = new JSONArray(recentSubjectEmailList);
                }

                String subjectEmail = recentSubjects.getString(recentSubjects.length() - 1);
                if (subjectEmail.contentEquals(collectorEmail)) {
                    String s = AppSharedPreference.getInstance().readString(Constant.CONSENT_QUESTION_RESPONSE);
                    responseArray = new JSONArray(s);

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("response", responseArray);
                    CreateStrSubjectInput consentSubject = CreateStrSubjectInput.builder()
                            .consent_response(String.valueOf(jsonObject))
                            .subject_email(programName + '_' + subjectEmail)   // JEBYRNE
                            .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY))
                            .collector_email(preference.readString(Constant.COLLECTOR_EMAIL))
                            .consent_video_id(videoUrl)
                            .uuid(preference.readString(Constant.COLLECTOR_ID_KEY))
                            .status("active")
                            .build();
                    preference.storeValue(Constant.SUBJECT_ID_TEXT, preference.readString(Constant.COLLECTOR_ID_KEY));  // JEBYRNE: this is for frameJson, do this here instead of inside consentMutation

                    Globals.getAppSyncClient()
                            .mutate(CreateStrSubjectMutation.builder().input(consentSubject).build())
                            .enqueue(consentMutation);
                } else {
                    Globals.mAWSAppSyncClient.query(StrCollectorByEmailQuery.builder()
                            .collector_email(subjectEmail).build())
                            .responseFetcher(NETWORK_ONLY)
                            .enqueue(collectorCallback);
                }
            }
        } catch (Exception e) {
            hideProgress();
            Toast.makeText(VideoPreviewActivity.this, "Video upload failed!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onFileUploadFailure(String errorMessage) {
        hideProgress();
        //Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        logout();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        displayErrorDialog();
        return true;
    }

    private void displayErrorDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage("Failed to play video");
        dialog.setCancelable(false);
        dialog.setPositiveButton("OK", (dialog1, which) -> {
            FileUtil.deleteFile(videoFilePath);
            finish();
        });
        dialog.show();
    }

    private void updateBorder(int x, int y, int width, int height) {
        displayWidth = previewVideoView.getWidth();
        displayHeight = previewVideoView.getHeight();

        if ((frameJSON == null) || (frameJSON.getMetaData() == null)) {
            logout();  // JEBYRNE
        }
        else {
            FrameMetaData metaData = frameJSON.getMetaData();
            if (metaData != null) {
                int frameWidth = metaData.getFrameWidth();
                int frameHeight = metaData.getFrameHeight();

                int w = (displayWidth * width) / frameWidth;
                int h = (displayHeight * height) / frameHeight;

                boundingBoxView.getLayoutParams().width = w;
                boundingBoxView.getLayoutParams().height = h;

                int nx = (displayWidth * x) / frameWidth;
                int ny = (displayHeight * y) / frameHeight;

                boundingBoxView.setTranslationX(nx + (int) previewVideoView.getX());
                boundingBoxView.setTranslationY(ny + (int) previewVideoView.getY());

                activityNameTextView.setTranslationX(nx + (int) previewVideoView.getX());
                activityNameTextView.setTranslationY(ny + (int) previewVideoView.getY() - activityNameTextView.getMeasuredHeight());
            }

            boundingBoxView.requestLayout();
            activityNameTextView.requestLayout();
        }
    }

    private class VideoPlayerTimer implements Runnable {

        int increment = 0;
        int frameIndex = 0;

        @Override
        public void run() {
            if (frameJSON != null) {
                String activityLabel = getActivityLabel(frameIndex);
                if (!TextUtils.isEmpty(activityLabel)) {
                    activityNameTextView.setVisibility(View.VISIBLE);
                    activityNameTextView.setText(activityLabel);
                } else {
                    activityNameTextView.setVisibility(View.GONE);
                }
                List<FrameObject> frameObjects = frameJSON.getObject();
                if (frameObjects != null && !frameObjects.isEmpty()) {
                    List<BoundingBox> boundingBoxs = frameObjects.get(0).getBoundingBox();
                    if (boundingBoxs != null && !boundingBoxs.isEmpty() && frameIndex < boundingBoxs.size()) {
                        BoundingBox boundingBox = boundingBoxs.get(frameIndex);
                        if (boundingBox.getFrameIndex() == frameIndex) {
                            boundingBoxView.setVisibility(View.VISIBLE);
                            Frame frame = boundingBox.getFrame();
                            updateBorder(frame.getX(), frame.getY(), frame.getWidth(), frame.getHeight());
                        } else {
                            boundingBoxView.setVisibility(View.GONE);
                            activityNameTextView.setVisibility(View.GONE);
                        }
                    } else {
                        boundingBoxView.setVisibility(View.GONE);
                        activityNameTextView.setVisibility(View.GONE);
                    }
                }
            }
            else if (!fromConsent) {
                logout();
            }

            //increment += seekBarIncrementCounter;
            int currentPosition = previewVideoView.getCurrentPosition();
            int getDuration = previewVideoView.getDuration();
            increment = currentPosition;

            Log.d("JEBYRNE", String.format("VideoPreviewActivity.getDuration: %d", getDuration));
            if (increment > (getDuration - 750)) {  // JEBYRNE: get duration is off by about 500 ms, and never gets to the end, yuck
                stopPreview();
            } else {
                progressSeekBar.setProgress(increment);
                runningTextView.setText(DateUtil.getTotalVideoTime(increment));
                //frameIndex++;
                frameIndex = (int) Math.round(currentPosition / frameDuration);
                timerHandler.postDelayed(playerTimer, (int) Math.round(frameDuration/2));  // JEBYRNE: double to int
            }
        }

        private String getActivityLabel(int frameIndex) {
            if (frameJSON == null) {
                logout();
            }
            else {
                List<ActivityLabel> activities = frameJSON.getActivity();
                if (activities != null && !activities.isEmpty()) {
                    for (ActivityLabel activity : activities) {
                        int startFrame = activity.getStartFrame();
                        int endFrame = activity.getEndFrame();
                        if (frameIndex >= startFrame && frameIndex <= endFrame) {
                            return activity.getLabel();
                        }
                    }
                }
            }
            return null;
        }

        /**
         * @param progress resets the frame index and increment value
         */
        public void setFrameIndex(int progress) {
            //this.frameIndex = progress / seekBarIncrementCounter;
            this.frameIndex = (int) Math.round(progressSeekBar.getProgress() / frameDuration);  // JEBYRNE
            increment = progress;
        }

        public void reset() {
            increment = 0;
            frameIndex = 0;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgress();
    }

    // JEBYRNE: common function to return to entry screen on error
    private void logout() {
        frameJSON = null;
        FileUtil.deleteFile(videoFilePath);
        FileUtil.deleteFile(Constant.FRAMES_JSON_FILE_NAME);

        AlertDialog.Builder builder = new AlertDialog.Builder(VideoPreviewActivity.this);
        builder.setMessage("Network Error!  Restarting due to poor network connection...");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", (dialog1, which) -> {
            Intent loginIntent = new Intent(this, FrontScreenActivity.class);
            this.startActivity(loginIntent);
            finish();
        });
        showDialogAndHideSystemBar(builder.create());
    }
}
