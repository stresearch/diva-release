package com.visym.collector.capturemodule.views;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.visym.collector.BaseActivity;
import com.visym.collector.R;
import com.visym.collector.capturemodule.facedetection.ImageProcessor;
import com.visym.collector.capturemodule.interactor.VideoUploadInteractor;
import com.visym.collector.capturemodule.model.ActivityLabel;
import com.visym.collector.capturemodule.model.FrameJSON;
import com.visym.collector.capturemodule.model.FrameMetaData;
import com.visym.collector.capturemodule.model.FrameObject;
import com.visym.collector.capturemodule.presenters.VideoUploadPresenter;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.model.BoundingBox;
import com.visym.collector.model.Frame;
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

    private String videoFilePath;
    private int frameRate;
    private int totalFrames;
    private int seekBarIncrementCounter;
    private MediaPlayer mediaPlayer;
    private boolean isVideoPaused, isRetakeVideo = false;
    private VideoPlayerTimer playerTimer;
    private int videoTotalDuration;
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
    JSONArray responseArray;
    private String newVideoFilePath;
    private boolean videoProcessed = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
        previewVideoView.setOnTouchListener(this);

        if (fromConsent) {
            init();
        } else {
            orientationEventListener = new OrientationEventListener(this) {
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
            }
            AsyncTask.execute(() -> {
                try {
                    frameJSON = FileUtil.readFromFile(VideoPreviewActivity.this, Constant.FRAMES_JSON_FILE_NAME);
                    runOnUiThread(this::init);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "onCreate: ", e.getCause());
                }
            });
        }

        if (!fromConsent) {
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
        previewVideoView.setVideoPath(videoFilePath);
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(videoFilePath);
        } catch (IOException e) {
            Log.e(TAG, "init: ", e.getCause());
            return;
        }
        MediaFormat trackFormat = mediaExtractor.getTrackFormat(0);
        mediaExtractor.release();

        frameRate = trackFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
        long millis = TimeUnit.MICROSECONDS.toMillis(trackFormat.getLong(MediaFormat.KEY_DURATION));
        if (millis < 1000) {
            totalFrames = frameRate;
        } else {
            totalFrames = frameRate * (int) (millis / 1000);
        }
        try {
            FrameMetaData metaData = frameJSON.getMetaData();
            String outputFile = FileUtil.getVideoFilePath(Globals.getAppContext(), "capture");
            ImageProcessor imageProcessor = new ImageProcessor(handler, videoFilePath, outputFile);
            imageProcessor.setDeviceRotation(metaData.getDeviceOrientation());
            imageProcessor.setSensorRotation(metaData.getSensorOrientation());
            new Thread(imageProcessor).start();
        }catch (Exception e){
            Log.d(TAG, "init: "+ "Failed encoding and decoding");
        }
        playerTimer = new VideoPlayerTimer();
    }

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            if (data.containsKey(FrameUtil.COMPUTATION_SUCCESS_KEY)) {
                videoProcessed = true;
                newVideoFilePath = data.getString(FrameUtil.COMPUTATION_SUCCESS_KEY);
            }if (Globals.isShowingLoader()){
                Globals.dismissLoading();
                continueSubmittingVideo();
            }
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (previewVideoView.isPlaying()) {
                previewVideoView.seekTo(progress);
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
        progressBar.setVisibility(View.GONE);
        if (totalFrames == 0) {
            Toast.makeText(this, "Corrupted video file", Toast.LENGTH_SHORT).show();
            finish();
        } else if (this.mediaPlayer == null) {
            this.mediaPlayer = mp;
            mediaPlayer.setOnInfoListener(this);
            mediaPlayer.setOnErrorListener(this);
            videoTotalDuration = mediaPlayer.getDuration();
            seekBarDisabled = false;
            progressSeekBar.setMax(videoTotalDuration);
            seekBarIncrementCounter = videoTotalDuration / totalFrames;

            pausePlayButton.setVisibility(View.GONE);
            totalTimeTextView.setText(getString(R.string.initial_timer_text));
            runningTextView.setText(getString(R.string.initial_timer_text));
            totalTimeTextView.setText(DateUtil.getTotalVideoTime(videoTotalDuration));

            mp.start();
            playerTimer.run();
        }
        previewVideoView.post(new Runnable() {
            @Override
            public void run() {
                onOrientationChange(currentOrientation);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideSystemBar();
        if (mediaPlayer != null) {
            progressBar.setVisibility(View.VISIBLE);

            playerTimer.onPause();
            previewVideoView.pause();
            isVideoPaused = true;
        }

        if (!fromConsent && orientationEventListener != null) {
            orientationEventListener.disable();
        }
    }

    protected void stopPreview() {
        playerTimer.reset();
        timerHandler.removeCallbacks(playerTimer);
        pausePlayButton.setVisibility(View.VISIBLE);
        boundingBoxView.setVisibility(View.GONE);
        activityNameTextView.setVisibility(View.GONE);
        seekBarDisabled = true;
        mediaPlayer = null;
        previewVideoView.stopPlayback();
    }

    private void updateVideoProgress() {
        pausePlayButton.setVisibility(View.GONE);
        init();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel_image_view:
                if (fromConsent) {
                    displayDialog("Exit consent?", 0);
                } else {
                    displayDialog("Exit collection?", 0);
                }
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
                    if (isRetakeVideo) {
                        Globals.isRetakeConsentVideo = true;
                    }
                    showProgress("Uploading video...", true);
                    presenter.uploadVideo(videoFilePath, AwsResponse.FILE_TYPE_CONSENT);
                } else {
                    if (frameJSON == null) {
                        return;
                    }
                    List<ActivityLabel> activities = frameJSON.getActivity();
                    if (activities == null || activities.isEmpty()) {
                        displayNoActivityDialog();
                    } else {
                        if (videoProcessed) {
                            continueSubmittingVideo();
                        }else {
                            Globals.showLoading(this);
                        }
                    }
                }
                break;

            case R.id.pause_play_button:
                updateVideoProgress();
                break;
        }
    }

    private void displayNoActivityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setCancelable(false)
                .setMessage("Your collection has no activities. Submit?")
                .setPositiveButton("OK", (dialog, which) -> {
                    if (videoProcessed) {
                        continueSubmittingVideo();
                    }else {
                        Globals.showLoading(this);
                    }
                })
                .setNegativeButton("CANCEL", (dialog, which) -> {
                    FileUtil.deleteFile(videoFilePath);
                    FileUtil.deleteFile(newVideoFilePath);
                    Intent intent = new Intent(VideoPreviewActivity.this, VideoCaptureActivity.class);
                    startActivity(intent);
                    finish();
                });
        showDialogAndHideSystemBar(builder.create());
    }

    private void continueSubmittingVideo() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FFmpegMediaMetadataRetriever metadataRetriever = new FFmpegMediaMetadataRetriever();
                showProgress("Uploading video...", true);
                try {
                    if (!TextUtils.isEmpty(newVideoFilePath)) {
                        metadataRetriever.setDataSource(newVideoFilePath);
                    }else {
                        metadataRetriever.setDataSource(videoFilePath);
                        newVideoFilePath = videoFilePath;
                    }
                    double frameRate = Double.parseDouble(metadataRetriever
                            .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE));
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(newVideoFilePath);

                    int duration = Integer.parseInt(retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    if (frameJSON != null) {
                        frameJSON.getMetaData().setFrameRate(frameRate);
                        frameJSON.getMetaData().setDuration((double) (duration) / 1000);
                        Type type = new TypeToken<FrameJSON>() {}.getType();
                        String jsonString = new Gson().toJson(frameJSON, type);
                        FileUtil.writeToJSONFile(Constant.FRAMES_JSON_FILE_NAME, jsonString);
                    }

                    String filePath = FileUtil.getFilePath(Constant.FRAMES_JSON_FILE_NAME);
                    presenter.uploadVideo(filePath + "," + newVideoFilePath,
                            AwsResponse.FILE_TYPE_JSON);
                } catch (IOException e) {
                    hideProgress();
                    Log.d(TAG, "Uploading: ", e.getCause());
                    Toast.makeText(VideoPreviewActivity.this, "Failed to read file. Please try again",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void displayDialog(String message, int what) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton("YES", (dialog, which) -> {
                    FileUtil.deleteFile(videoFilePath);
                    if (fromConsent) {
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
                        FileUtil.deleteFile(videoFilePath);
                        FileUtil.deleteFile(newVideoFilePath);
                        if (what == 0) {
                            Intent intent = new Intent(this, DashboardActivity.class);
                            intent.putExtra("redirectTo", "capture");
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(this, VideoCaptureActivity.class);
                            startActivity(intent);
                        }
                    }
                    finish();
                })
                .setNegativeButton("NO", (dialog, which) -> {
                });
        showDialogAndHideSystemBar(builder.create());
    }

    @Override
    public void onBackPressed() {
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
            playerTimer.onPause();
            progressBar.setVisibility(View.VISIBLE);
            return true;
        }
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            playerTimer.onResume();
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
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.preview_video_view:
                if (event.getAction() == MotionEvent.ACTION_DOWN && event.getPointerCount() == 1) {
                    if (previewVideoView.isPlaying()) {
                        playerTimer.onPause();
                        previewVideoView.pause();
                    } else {
                        playerTimer.onResume();
                        previewVideoView.start();
                    }
                }
                return true;

            case R.id.progress_seekbar:
                return seekBarDisabled;
        }
        return false;
    }

    private Handler timerHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };

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
                    showDialogAndHideSystemBar(builder.create());
                    break;

                case AwsResponse.FILE_TYPE_JSON:
                    jsonUrl = response.getVideoUrl();
                    presenter.uploadVideo(newVideoFilePath, AwsResponse.FILE_TYPE_VIDEO);
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
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void storeConsentDetails() {
        try {
            if (isRetakeVideo) {
                Toast.makeText(this, "Consent video uploaded sucessfully", Toast.LENGTH_SHORT)
                        .show();
                Globals.isRetakeConsentVideo = false;
                finish();
            } else {
                String recentSubjectEmailList = preference.readString(Constant.SUBJECT_EMAIL_TEXT);
                String collectorEmail = preference.readString(Constant.COLLECTOR_EMAIL);
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
                    s = jsonObject.toString().replaceAll("\\\\", "");

                    CreateStrSubjectInput consentSubject = CreateStrSubjectInput.builder()
                            .consent_response(String.valueOf(jsonObject))
                            .subject_email(subjectEmail)
                            .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY))
                            .collector_email(preference.readString(Constant.COLLECTOR_EMAIL))
                            .consent_video_id(videoUrl)
                            .uuid(preference.readString(Constant.COLLECTOR_ID_KEY))
                            .status("active")
                            .build();

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
            Toast.makeText(VideoPreviewActivity.this,
                    "Failed to uploaded video", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

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
                        collectorId = UUID.randomUUID().toString();
                    }

                    try {
                        JSONArray recentSubjects = new JSONArray(preference.readString(Constant.SUBJECT_EMAIL_TEXT));

                        String s = AppSharedPreference.getInstance().readString(Constant.CONSENT_QUESTION_RESPONSE);
                        responseArray = new JSONArray(s);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("response", responseArray);
                        s = jsonObject.toString().replaceAll("\\\\", "");

                        CreateStrSubjectInput consentSubject = CreateStrSubjectInput.builder()
                                .consent_response(String.valueOf(jsonObject))
                                .subject_email(recentSubjects.getString(recentSubjects.length() - 1))
                                .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY))
                                .collector_email(preference.readString(Constant.COLLECTOR_EMAIL))
                                .consent_video_id(videoUrl)
                                .uuid(collectorId)
                                .status("active")
                                .build();

                        Globals.getAppSyncClient()
                                .mutate(CreateStrSubjectMutation.builder().input(consentSubject).build())
                                .enqueue(consentMutation);
                    } catch (Exception e) {
                        hideProgress();
                        Toast.makeText(VideoPreviewActivity.this,
                                "Failed to uploaded video", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            } else {
                hideProgress();
                Toast.makeText(VideoPreviewActivity.this,
                        "Failed to uploaded video", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            hideProgress();
            Toast.makeText(VideoPreviewActivity.this,
                    "Failed to uploaded video", Toast.LENGTH_SHORT).show();
            finish();
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
                                    preference.storeValue(Constant.SUBJECT_ID_TEXT, collectorId);
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

                                           /* Intent intent = new Intent(VideoPreviewActivity.this,
                                                    VideoCaptureActivity.class);
                                            startActivity(intent);
                                            finish();*/
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
                            Toast.makeText(VideoPreviewActivity.this,
                                    "Failed to upload video. Please try again", Toast.LENGTH_SHORT).show();
                            Intent captureVideoIntent = new Intent(VideoPreviewActivity.this,
                                    ConsentVideoCaptureActivity.class);
                            startActivity(captureVideoIntent);
                            finish();
                        }
                    });
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
                                //  displayNoTrainingVideoDialog();
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
                    Globals.showSnackBar(getResources().getString(R.string.unknown_error_message), mcontext, Snackbar.LENGTH_LONG);
                }
            });
        }
    };

    @Override
    public void onFileUploadFailure(String errorMessage) {
        hideProgress();
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onError: " + what + " " + extra);
        return false;
    }

    private class VideoPlayerTimer implements Runnable {

        int increment = 0;
        int frameIndex = 0;
        private boolean isPaused = false;

        @Override
        public void run() {
            if (!isPaused) {
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

                increment += seekBarIncrementCounter;
                if (increment > videoTotalDuration) {
                    stopPreview();
                } else {
                    progressSeekBar.setProgress(increment);
                    runningTextView.setText(DateUtil.getTotalVideoTime(increment));
                    frameIndex++;
                    timerHandler.postDelayed(playerTimer, (1000 / frameRate));
                }
            } else {
                timerHandler.postDelayed(playerTimer, 0);
            }
        }

        private String getActivityLabel(int frameIndex) {
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
            return null;
        }

        /**
         * Call this to pause.
         */
        public void onPause() {
            isPaused = true;
        }

        /**
         * Call this to resume.
         */
        public void onResume() {
            isPaused = false;
        }

        /**
         * @param progress resets the frame index and increment value
         */
        public void setFrameIndex(int progress) {
            this.frameIndex = progress / frameRate;
            increment = progress;
        }

        public void reset() {
            increment = 0;
            frameIndex = 0;
        }
    }

    private void updateBorder(int x, int y, int width, int height) {
        displayWidth = previewVideoView.getWidth();
        displayHeight = previewVideoView.getHeight();
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
