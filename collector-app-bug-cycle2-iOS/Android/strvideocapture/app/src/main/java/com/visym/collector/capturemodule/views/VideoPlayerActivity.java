package com.visym.collector.capturemodule.views;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;

import com.amazonaws.amplify.generated.graphql.CreateStrRatingMutation;
import com.amazonaws.amplify.generated.graphql.DeleteStrReviewAssignmentMutation;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.gson.Gson;
import com.visym.collector.BaseActivity;
import com.visym.collector.R;
import com.visym.collector.capturemodule.interactor.VideoPlayerInteractor;
import com.visym.collector.capturemodule.model.ActivityLabel;
import com.visym.collector.capturemodule.model.FrameJSON;
import com.visym.collector.capturemodule.model.FrameMetaData;
import com.visym.collector.capturemodule.model.FrameObject;
import com.visym.collector.capturemodule.presenters.VideoPlayerPresenter;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.model.BoundingBox;
import com.visym.collector.model.CollectionModel;
import com.visym.collector.model.CollectionQuestionnaire;
import com.visym.collector.model.Frame;
import com.visym.collector.model.Instance;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.AwsResponse;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.DateUtil;
import com.visym.collector.utils.FileUtil;
import com.visym.collector.utils.Globals;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;
import type.CreateStrRatingInput;
import type.DeleteStrReviewAssignmentInput;
import wseemann.media.FFmpegMediaMetadataRetriever;

public class VideoPlayerActivity extends BaseActivity implements MediaPlayer.OnPreparedListener,
        SeekBar.OnSeekBarChangeListener, MediaPlayer.OnInfoListener, View.OnClickListener,
        VideoPlayerInteractor.VideoPlayerView, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static final String TAG = "VideoPlayerActivity";
    public static final String INTENT_CONSENT_TEXT = "CONSENT";

    @BindView(R.id.video_player_view)
    VideoView videoPlayerView;

    @BindView(R.id.mediacontroller_progress)
    SeekBar videoSeekBar;

    @BindView(R.id.loading_progressbar)
    ProgressBar progressBar;

    @BindView(R.id.cancel_button)
    ImageButton cancelButton;

    @BindView(R.id.running_time_textview)
    TextView runningTimeTextView;

    @BindView(R.id.total_time_textview)
    TextView totalTimeTextView;

    @BindView(R.id.upVoteBtn)
    ImageView upVoteBtn;

    @BindView(R.id.downVoteBtn)
    ImageView downVoteBtn;

    @BindView(R.id.questionnaire)
    TextView questionTextView;

    @BindView(R.id.activity_textview)
    TextView activityNameTextView;

    @BindView(R.id.nextButton)
    TextView nextButton;

    @BindView(R.id.submitButton)
    TextView submitButton;

    @BindView(R.id.bounding_box_view)
    ImageView boundingBoxView;

    @BindView(R.id.pause_play_button)
    ImageButton playPauseButton;

    @BindView(R.id.training_video_hint_text)
    TextView trainingVideoHintText;

    @BindView(R.id.rating_questionnaire_layout)
    LinearLayout ratingQuestionnaireLayout;
    @BindView(R.id.go_back_btn)
    ImageView goBackBtn;


    private String jsonUrl, videoFilePath, noResponse, valueAddWhen, yesResponse, ratingInstanceId, videoId;
    private double frameRate;
    private int totalFrames;
    private Handler handler;
    private boolean isVideoPaused = false;
    private VideoPlayerTimer playerTimer;
    private MediaPlayer mediaPlayer;
    private int stoppedPosition;
    private VideoPlayerPresenter presenter;
    private int playerDuration;
    private FrameJSON frameJSON;
    private int displayWidth;
    private int displayHeight;
    private List<String> questionAtFrame;
    private JSONArray instanceDetails;
    private AppSharedPreference preference;
    CollectionModel collectionModelObject;
    JSONArray videoArray;
    Random rand;
    int randVideoIndex;
    Context mContext;

    private String module;
    private boolean trainingModule = false, isRatingVideo = false;
    private String orientation;
    int frameIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        ButterKnife.bind(this);
        presenter = new VideoPlayerPresenter(this);
        preference = AppSharedPreference.getInstance();
        mContext = this;

        getApplicationContext().startService(new Intent(getApplicationContext(), TransferService.class));

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("module")) {
            module = intent.getStringExtra("module");

            if (!TextUtils.isEmpty(module)) {
                if (module.contentEquals("collections")) {
                    jsonUrl = intent.getStringExtra("jsonURL");
                    videoFilePath = intent.getStringExtra("videoURL");
                    downloadFiles();
                } else if (module.contentEquals("ratings")) {
                    jsonUrl = intent.getStringExtra("jsonURL");
                    videoFilePath = intent.getStringExtra("videoURL");
                    videoId = intent.getStringExtra("videoId");
                    cancelButton.setVisibility(View.GONE);
                    nextButton.setVisibility(View.GONE);
                    try {
                        instanceDetails = new JSONArray(intent.getStringExtra("instanceData"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (instanceDetails != null) {

                        Log.e(TAG, "onCreate: instanceDetails string " + instanceDetails.toString());
                    }
                    isRatingVideo = true;
                    downloadFiles();
//                    String videoId = intent.getStringExtra("videoId");
//                    presenter.getCollectionQuestionnaire();
//                    presenter.getActivityInstances(videoId);
                }
                else if(module.contentEquals("notraining")){
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                            .setCancelable(false)
                            .setMessage("Training video for this collection is under progress")
                            .setPositiveButton("OK", (dialog, which) -> {
                    /*Intent goNextIntent = new Intent(this,
                            ConsentConfirmationActivity.class);
                    startActivity(goNextIntent);*/

                                Intent videoCaptureIntent = new Intent(VideoPlayerActivity.this,
                                        VideoCaptureActivity.class);
                                videoCaptureIntent.putExtra("module","notraining");
                                startActivity(videoCaptureIntent);
                                finish();
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
                else {
                    trainingModule = true;
                    collectionModelObject = new Gson().fromJson(preference.readString(Constant.COLLECTION_KEY), CollectionModel.class);
                    if (!TextUtils.isEmpty(collectionModelObject.getTrainingVideoURL())){
                        String s1 = collectionModelObject.getTrainingVideoURL().replace("[", "");
                        String s2 = s1.replace("]", "");

                        try {
                            videoArray = new JSONArray(s2.split(","));
                            if (videoArray.length() > 0) {
                                rand = new Random();
                                randVideoIndex = rand.nextInt(videoArray.length());
                            } else {
                                randVideoIndex = 0;
                            }

                            String trainingVideoOverlays = collectionModelObject.getTrainingVideosOverlay();
                            if (!TextUtils.isEmpty(trainingVideoOverlays)){
                                String replacedString = trainingVideoOverlays.replaceAll("\\[",
                                        "").replaceAll("]", "");
                                String[] overlays = replacedString.split(",");
                                if (overlays.length > 0) {
                                    trainingVideoHintText.setText(overlays[randVideoIndex]);
                                }
                            }

                            String filePath = videoArray.get(randVideoIndex).toString();
                            videoFilePath = filePath.replace("https://diva-str-prod-data-public.s3.amazonaws.com/",
                                    "").trim();
                            trainingVideoHintText.setVisibility(View.VISIBLE);
                        } catch (JSONException e) {
                            Log.e(Globals.TAG, "onCreate: " + e.toString());
                        }
                        downloadFiles();
                    }
                }
            }
        }
    }

    private void downloadFiles() {
        File file = new File(Globals.getInstance().getFilesDir(), videoFilePath);
        if (file.exists()) {
            videoFilePath = file.getAbsolutePath();

            if (trainingModule) {
                init();
            } else {
                AsyncTask.execute(() -> {
                    try {
                        File file1 = new File(Globals.getInstance().getFilesDir(), jsonUrl);
                        frameJSON = FileUtil.readFromFile(VideoPlayerActivity.this, file1.getAbsolutePath());
                        runOnUiThread(this::init);
                    } catch (IOException e) {
                        e.printStackTrace();
                        onFileDownloadFailure(e.getMessage());
                    }
                });
            }
        } else {
            showProgress("Downloading video please wait...", true);
            presenter.downloadJSONFile(trainingModule ? videoFilePath : jsonUrl, trainingModule);
        }
    }

    private void init() {
        if (!trainingModule) {
            if (frameJSON != null) {
                orientation = frameJSON.getMetaData().getOrientation();
                if (!TextUtils.isEmpty(orientation) && orientation.contains("land")) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
                frameRate = (int) frameJSON.getMetaData().getFrameRate();
                totalFrames = (int)Math.round((frameJSON.getMetaData().getDuration()
                        * 1000 * frameJSON.getMetaData().getFrameRate()) / 1000);
            } else {
                displayErrorDialog("Failed to play video");

            }
        }
        videoPlayerView.setVideoPath(videoFilePath);

        playPauseButton.setVisibility(View.GONE);
        videoSeekBar.setOnSeekBarChangeListener(this);
        videoPlayerView.setOnPreparedListener(this);
        videoPlayerView.setOnErrorListener(this);
        cancelButton.setOnClickListener(this);
        goBackBtn.setOnClickListener(this);
        upVoteBtn.setOnClickListener(this);
        downVoteBtn.setOnClickListener(this);
        nextButton.setOnClickListener(this);
        submitButton.setOnClickListener(this);
        playPauseButton.setOnClickListener(this);

        handler = new Handler();
        playerTimer = new VideoPlayerTimer();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        progressBar.setVisibility(View.GONE);
        if (this.mediaPlayer == null) {
            this.mediaPlayer = mp;
            mediaPlayer.setOnInfoListener(this);
            mediaPlayer.setOnCompletionListener(this);

            playerDuration = mediaPlayer.getDuration();
            videoSeekBar.setMax(playerDuration);

            if (trainingModule) {
                int videoWidth = mediaPlayer.getVideoWidth();
                int videoHeight = mediaPlayer.getVideoHeight();
                if (videoWidth > videoHeight) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }

            if (totalFrames == 0) {
                FFmpegMediaMetadataRetriever metadataRetriever = new FFmpegMediaMetadataRetriever();
                metadataRetriever.setDataSource(videoFilePath);

                frameRate = (int) Double.parseDouble(metadataRetriever
                        .extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE));

                if (playerDuration < 1000) {
                    totalFrames = (int)Math.round((playerDuration / frameRate) / 1000);
                } else {
                    totalFrames = (int) (TimeUnit.MILLISECONDS.toSeconds(playerDuration) * frameRate);
                }
            }

            mp.start();
            playerTimer.run();

            totalTimeTextView.setText(getString(R.string.initial_timer_text));
            runningTimeTextView.setText(getString(R.string.initial_timer_text));
            totalTimeTextView.setText(DateUtil.getTotalVideoTime(playerDuration));
        } else {
            isVideoPaused = !isVideoPaused;
            videoPlayerView.seekTo(stoppedPosition);
            videoPlayerView.start();
            playerTimer.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            progressBar.setVisibility(View.VISIBLE);
            stoppedPosition = videoPlayerView.getCurrentPosition();
            updateVideoProgress();
        }
    }

    protected void release() {
        if (videoPlayerView != null) {
            videoPlayerView.seekTo(0);
            mediaPlayer = null;
            handler.removeCallbacks(playerTimer);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            videoPlayerView.seekTo(progress);
            playerTimer.setFrameIndex(progress);
        }
        Log.d(TAG, "onProgressChanged: "+ progress);
        videoSeekBar.setProgress(progress);
        if (!trainingModule) {
            getTrackOfFrames();
        }
        runningTimeTextView.setText(DateUtil.getTotalVideoTime(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && event.getPointerCount() == 1) {
            updateVideoProgress();
        }
        return true;
    }

    private void updateVideoProgress() {
        if (isVideoPaused) {
            playerTimer.onResume();
            videoPlayerView.start();
        } else {
            playerTimer.onPause();
            videoPlayerView.pause();
        }

        if (ratingQuestionnaireLayout.getVisibility() == View.VISIBLE) {
            ratingQuestionnaireLayout.setVisibility(View.GONE);
        }
        isVideoPaused = !isVideoPaused;
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel_button:
                if (videoPlayerView.isPlaying()) {
                    videoPlayerView.stopPlayback();
                }
                if (trainingModule) {
                    displayErrorDialog("Exit this collection?");
                } else {
                    onBackPressed();

                }
                break;

            case R.id.nextButton:
                if (!TextUtils.isEmpty(module) && module.contentEquals("training")) {
                    playPauseButton.setVisibility(View.GONE);
                    Intent videoCaptureIntent = new Intent(this,
                            VideoCaptureActivity.class);
                    videoCaptureIntent.putExtra("module","training");
                    startActivity(videoCaptureIntent);
                } else {
                    onBackPressed();
                }
                break;

            case R.id.pause_play_button:
                replayVideo();
                break;

            case R.id.upVoteBtn:
                Log.e(TAG, "onClick: upVoteBtn  yes response " + yesResponse + " valueAddwhen " + valueAddWhen);

                //hit rating api with data
                hitRatingApiWithData(ratingInstanceId, yesResponse, videoId);
                break;
            case R.id.downVoteBtn:
                Log.e(TAG, "onClick: downVoteBtn  no response " + noResponse + " valueAddwhen " + valueAddWhen);
                //updateVideoProgress();
                hitRatingApiWithData(ratingInstanceId, noResponse, videoId);
                break;

            case R.id.submitButton:
                // delete the video
                DeleteStrReviewAssignmentInput deleteStrReviewAssignmentInput = DeleteStrReviewAssignmentInput.builder()
                        .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY))
                        .video_id(videoId)
                        .build();
                Globals.showLoading(this);
                Globals.mAWSAppSyncClient.mutate(DeleteStrReviewAssignmentMutation.builder().input(deleteStrReviewAssignmentInput).build())
                        .enqueue(deleteStrReviewAssignmentDelete);
                break;
            case R.id.go_back_btn:
                    super.onBackPressed();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!TextUtils.isEmpty(orientation) && orientation.contains("land")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void replayVideo() {
        frameIndex = 0;
        init();
        if (submitButton.getVisibility() == View.VISIBLE)
            submitButton.setVisibility(View.GONE);
    }

    public void onFileDownloadFailure(String message) {
        FileUtil.deleteFile(jsonUrl);
        FileUtil.deleteFile(videoFilePath);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    public void hitRatingApiWithData(String instanceId, String response, String video_id) {
       /* runOnUiThread(new Runnable() {
            @Override
            public void run() {*/
        Globals.showLoading(mContext);
        CreateStrRatingInput ratingInput = CreateStrRatingInput.builder()
                .rating_response(response)
                .id(instanceId)
                .video_id(video_id)
                .project_id(preference.readString(Constant.PROJECT_ID_KEY))
                .project_name(preference.readString(Constant.PROJECT_NAME_KEY))
                .program_id(preference.readString(Constant.PROGRAM_ID_KEY))
                .collection_id(preference.readString(Constant.COLLECTION_ID_KEY))
                .collection_name(preference.readString(Constant.COLLECTION_NAME_KEY))
                .reviewer_id(preference.readString(Constant.COLLECTOR_ID_KEY))
                .build();

        Globals.mAWSAppSyncClient.mutate(CreateStrRatingMutation.builder().input(ratingInput).build()).enqueue(ratingResponse);
          /*  }
        });*/

    }

    private GraphQLCall.Callback<CreateStrRatingMutation.Data> ratingResponse = new GraphQLCall.Callback<CreateStrRatingMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<CreateStrRatingMutation.Data> response) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Globals.isShowingLoader())
                        Globals.dismissLoading();
                    updateVideoProgress();
                }
            });
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Globals.isShowingLoader())
                        Globals.dismissLoading();
                }
            });
        }
    };

    @Override
    public void onFileDownload(AwsResponse response) {
        TransferState state = response.getState();
        if (state == TransferState.COMPLETED) {
            if (response.getFileType() == AwsResponse.FILE_TYPE_JSON) {
                jsonUrl = response.getVideoUrl();
                AsyncTask.execute(() -> {
                    try {
                        frameJSON = FileUtil.readFromFile(VideoPlayerActivity.this, jsonUrl);
                    } catch (IOException e) {
                        Log.d(TAG, "run: Failed to read JSON file", e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(VideoPlayerActivity.this,
                                        "Something went wrong. Please try again later",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    }
                });
                presenter.downloadJSONFile(videoFilePath, trainingModule);
            } else {
                hideProgress();
                videoFilePath = response.getVideoUrl();
                init();
            }
        } else if (state == TransferState.FAILED) {
            hideProgress();
            onFileDownloadFailure(response.getErrorMessage());
        } else if (state == TransferState.IN_PROGRESS) {
            int progress = response.getProgress();
            updateProgress(progress);
        }
    }

    @Override
    public void onFailure(String errorMessage) {
        hideProgress();
        if (!TextUtils.isEmpty(errorMessage)) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Something went wrong. Please try again later",
                    Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public void onQuestionnaireResponse(List<CollectionQuestionnaire> questionnaireList) {
//        this.questionnaires = questionnaireList;
    }

    @Override
    public void onInstanceCallback(List<Instance> instances) {
//        this.instances = instances;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        displayErrorDialog("Failed to play video");
        return true;
    }

    private void displayErrorDialog(String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setPositiveButton("OK", (dialog1, which) -> {
            FileUtil.deleteFile(jsonUrl);
            FileUtil.deleteFile(videoFilePath);
            Intent goToCollectionList = new Intent(VideoPlayerActivity.this,DashboardActivity.class);
            goToCollectionList.putExtra("redirectTo","capture");
            startActivity(goToCollectionList);
            finish();
        });
        dialog.show();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        release();
        if (!isRatingVideo) {
            nextButton.setVisibility(View.VISIBLE);
        } else {
            submitButton.setText(getString(R.string.ok));
            submitButton.setVisibility(View.VISIBLE);
        }
        playPauseButton.setVisibility(View.VISIBLE);
    }

    private class VideoPlayerTimer implements Runnable {

        int increment = 0;

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
//                increment += seekBarIncrementCounter;
//                videoSeekBar.setProgress(increment);
                videoSeekBar.setProgress(videoPlayerView.getCurrentPosition());
                frameIndex++;
                handler.postDelayed(playerTimer, Math.round(1000 / frameRate));
            } else {
                handler.postDelayed(playerTimer, 0);
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
         * @param progress resets the frame index and increment and totalSec value
         */
        public void setFrameIndex(int progress) {
            frameIndex = (int)Math.round(((progress * frameRate) / 1000));
            increment = progress;
        }

    }

    private void getTrackOfFrames() {
        try {
            for (int j = 0; j < instanceDetails.length(); j++) {
                if (frameIndex == instanceDetails.getJSONObject(j).getInt("show_at_frame")) {

                    updateVideoProgress();
                    displayQuestionAtFrame(
                            instanceDetails.getJSONObject(j).getString("no"),
                            instanceDetails.getJSONObject(j).getString("instance_id"),
                            instanceDetails.getJSONObject(j).getString("question"),
                            instanceDetails.getJSONObject(j).getString("yes"),
                            instanceDetails.getJSONObject(j).getString("value_add_when")
                    );

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private int questionNumber = 0;

    //api hit for rating
    // Videoplay


    private void updateBorder(int x, int y, int width, int height) {
        displayHeight = videoPlayerView.getHeight();
        displayWidth = videoPlayerView.getWidth();
        Log.d(TAG, "updateBorder: " + displayWidth + " " + displayHeight);
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

            boundingBoxView.setTranslationX(nx + (int) videoPlayerView.getX());
            boundingBoxView.setTranslationY(ny + (int) videoPlayerView.getY());

            activityNameTextView.setTranslationY(ny + (int) videoPlayerView.getY() - activityNameTextView.getMeasuredHeight());
            activityNameTextView.setTranslationX(nx + (int) videoPlayerView.getX());
        }

        boundingBoxView.requestLayout();
        activityNameTextView.requestLayout();
    }

    private void displayQuestionAtFrame(String noresponse, String instanceId, String questionText, String yesresponse, String valueaddwhen) {
        Log.e(TAG, "displayQuestionAtFrame: noResponse " + noResponse + " instanceID " + instanceId
                + " questionText " + questionText + " yesResponse " + yesResponse + " valueAddWhen " + valueAddWhen);


        ratingQuestionnaireLayout.setVisibility(View.VISIBLE);
        questionTextView.setText(questionText);
        ratingInstanceId = instanceId;
        noResponse = noresponse;
        yesResponse = yesresponse;
        valueAddWhen = valueaddwhen;

    }

    private GraphQLCall.Callback<DeleteStrReviewAssignmentMutation.Data> deleteStrReviewAssignmentDelete = new GraphQLCall.Callback<DeleteStrReviewAssignmentMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<DeleteStrReviewAssignmentMutation.Data> response) {
            Log.e(TAG, "onResponse:  deleted" + response.data().toString());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Globals.isShowingLoader()) {
                        Globals.dismissLoading();
                    }
                    //redirect to rating screen
                    Intent goBackToRating = new Intent(VideoPlayerActivity.this, DashboardActivity.class);
                    goBackToRating.putExtra("redirectTo", "gallery");
                    startActivity(goBackToRating);
                    finish();
                }
            });
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(TAG, "onFailure: delete " + e.toString());
        }
    };
}
