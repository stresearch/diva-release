package com.visym.collector.capturemodule.views;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

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
import com.visym.collector.capturemodule.model.FrameJSON;
import com.visym.collector.capturemodule.presenters.VideoPlayerPresenter;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.model.CollectionModel;
import com.visym.collector.model.Frame;
import com.visym.collector.usermodule.view.FrontScreenActivity;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.AwsResponse;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.DateUtil;
import com.visym.collector.utils.FileUtil;
import com.visym.collector.utils.Globals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;
import type.CreateStrRatingInput;
import type.DeleteStrReviewAssignmentInput;
import wseemann.media.FFmpegMediaMetadataRetriever;

public class VideoPlayerActivity extends BaseActivity implements MediaPlayer.OnPreparedListener,
        SeekBar.OnSeekBarChangeListener, MediaPlayer.OnInfoListener, View.OnClickListener,
        VideoPlayerInteractor.VideoPlayerView, MediaPlayer.OnErrorListener {

    public static final String INTENT_CONSENT_TEXT = "CONSENT";
    private static final String TAG = "VideoPlayerActivity";
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

    @BindView(R.id.nextButton)
    TextView nextButton;

    @BindView(R.id.submitButton)
    TextView submitButton;

    @BindView(R.id.pause_play_button)
    ImageButton playPauseButton;

    @BindView(R.id.training_video_hint_text)
    TextView trainingVideoHintText;

    @BindView(R.id.rating_questionnaire_layout)
    LinearLayout ratingQuestionnaireLayout;

    @BindView(R.id.go_back_btn)
    ImageView goBackBtn;

    @BindView(R.id.option_view)
    RelativeLayout optionView;

    @BindView(R.id.parent_view)
    FrameLayout parentView;
    CollectionModel collectionModelObject;
    JSONArray videoArray;
    Random rand;
    int randVideoIndex;
    Context mContext;
    int frameIndex = 0;
    private String jsonUrl, videoFilePath, noResponse, valueAddWhen, yesResponse,
            ratingInstanceId, questionShortName, submittedTime, week, videoUploadedDate, videoId;
    private double frameRate;
    private int totalFrames;
    private Handler handler;
    private boolean isVideoPaused = false;
    private int stoppedPosition;
    private VideoPlayerPresenter presenter;
    private int playerDuration;
    private FrameJSON frameJSON;
    private JSONArray instanceDetails;
    private AppSharedPreference preference;
    private HashMap<Integer, List<Frame>> objectsBoundingBox;
    private HashMap<Integer, JSONObject> ratingQuestions;
    private String module;
    private boolean trainingModule = false, isRatingVideo = false;
    private String orientation;
    private OrientationEventListener orientationEventListener;
    private int currentOrientation;
    private CollectionModel collectionDetail;
    private boolean videoPrepared = false;
    private int currentPosition = 0;
    private double frameDuration;
    private Timer timer;
    private boolean ratingQuestionsAnswered = false;

    private TimerTask startTimer() {
        timer = new Timer();
        return new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onVideoProgress();
                    }
                });
            }
        };
    }

    private void onVideoProgress(){
        if (currentPosition <= playerDuration){
            if (!isVideoPaused) {
                currentPosition += frameDuration;
                //currentPosition = videoPlayerView.getCurrentPosition()

                if (!trainingModule && ratingQuestions != null && ratingQuestions.containsKey(frameIndex)) {
                    getTrackOfFrames();
                }
                if (frameJSON != null) {
                    displayObjectsBoundingBoxes();
                }
                else {
                    // JEBYRNE: this is a problem
                }
                videoSeekBar.setProgress(currentPosition);
                frameIndex++;
                //frameIndex = (int) Math.round(currentPosition / frameDuration);
                Log.d("JEBYRNE", String.format("frameRate=%f, frameIndex=%d", frameRate, frameIndex));  // TESTING
            }
        }else {
            if (timer != null){
                timer.cancel();
                timer = null;
            }
            isVideoPaused = true;
            if (!isRatingVideo) {
                nextButton.setVisibility(View.VISIBLE);
            } else {
                if (ratingQuestionsAnswered) {
                    submitButton.setText(getString(R.string.ok));
                    submitButton.setVisibility(View.VISIBLE);
                }
            }
            playPauseButton.setVisibility(View.VISIBLE);
        }
    }

    private GraphQLCall.Callback<CreateStrRatingMutation.Data> ratingResponse = new GraphQLCall.Callback<CreateStrRatingMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<CreateStrRatingMutation.Data> response) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ratingQuestionsAnswered = true;
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
    private GraphQLCall.Callback<DeleteStrReviewAssignmentMutation.Data> deleteStrReviewAssignmentDelete = new GraphQLCall.Callback<DeleteStrReviewAssignmentMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<DeleteStrReviewAssignmentMutation.Data> response) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Globals.isShowingLoader()) {
                        Globals.dismissLoading();
                    }
                    if (!TextUtils.isEmpty(orientation) && orientation.contains("land")) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    }
                    //redirect to rating screen
                    Intent goBackToRating = new Intent(VideoPlayerActivity.this,
                            DashboardActivity.class);
                    goBackToRating.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("JEBYRNE", "VideoPlayerActivity.onCreate");

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
                    collectionDetail = new Gson().fromJson(intent.getStringExtra("collectionDetail"),
                            CollectionModel.class);
                    cancelButton.setVisibility(View.GONE);
                    nextButton.setVisibility(View.GONE);
                    try {
                        instanceDetails = new JSONArray(intent.getStringExtra("instanceData"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isRatingVideo = true;
                    downloadFiles();
                } else if (module.contentEquals("notraining")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                            .setCancelable(false)
                            .setMessage("Training video for this collection is under progress")
                            .setPositiveButton("OK", (dialog, which) -> {
                                Intent videoCaptureIntent = new Intent(VideoPlayerActivity.this,
                                        VideoCaptureActivity.class);
                                videoCaptureIntent.putExtra("module", "notraining");
                                startActivity(videoCaptureIntent);
                                finish();
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } else {
                    trainingModule = true;
                    collectionModelObject = new Gson().fromJson(preference.readString(Constant.COLLECTION_KEY), CollectionModel.class);
                    if (!TextUtils.isEmpty(collectionModelObject.getTrainingVideoURL())) {
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
                            if (!TextUtils.isEmpty(trainingVideoOverlays)) {
                                String replacedString = trainingVideoOverlays.replaceAll("\\[",
                                        "").replaceAll("]", "");
                                String[] overlays = replacedString.split(",");
                                if (overlays.length > 0) {
                                    trainingVideoHintText.setText(overlays[randVideoIndex]);
                                }
                            }

                            String filePath = videoArray.get(randVideoIndex).toString();

			                // Replace in STR PROD (really should be set in awsconfiguration.json)
                            videoFilePath = filePath.replace("https://diva-str-prod-data-public.s3.amazonaws.com/", "").trim();

			                // Replace in VISYM PROD (really should be set in awsconfiguration.json)
                            videoFilePath = videoFilePath.replace("https://visym-public-data140008-visymcprod.s3.amazonaws.com/", "").trim(); 
                            trainingVideoHintText.setVisibility(View.VISIBLE);
                        } catch (JSONException e) {
                            Log.e(Globals.TAG, "onCreate: " + e.toString());
                        }
                        downloadFiles();
                    }
                }
            }
        }

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
    }

    private void onOrientationChange(int orientation) {
        int x = (int) videoPlayerView.getX();
        int y = (int) videoPlayerView.getY();

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
                        if (frameJSON == null) {
                            throw new Exception("Empty frameJSON");
                        }
                        runOnUiThread(this::init);
                    } catch (Exception e) {  // JEBYRNE: catch all exceptions
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
        DecimalFormat df = new DecimalFormat("#.##");

        if (!trainingModule) {
            if (frameJSON != null) {

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(videoFilePath);

                FFmpegMediaMetadataRetriever metadataRetriever = new FFmpegMediaMetadataRetriever();
                metadataRetriever.setDataSource(videoFilePath);

                //frameRate = (int) Double.parseDouble(metadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE));
                frameRate = (double) Double.parseDouble(metadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE));  // JEBYRNE: fractional framerates
                int ffmpeg_duration_in_ms = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));  // JEBYRNE: integer milliseconds  (FFMPEG is wrong, truncated to seconds)

                if (ffmpeg_duration_in_ms < 1000) {
                    totalFrames = (int) Math.round((ffmpeg_duration_in_ms / frameRate) / 1000);
                }
                else {
                    totalFrames = (int) (TimeUnit.MILLISECONDS.toSeconds(ffmpeg_duration_in_ms) * frameRate);
                }
                Log.d("JEBYRNE", String.format("VideoPlayerActivity.init: ffmpeg_duration_in_ms=%d, frameRate=%f, totalFrames=%d", ffmpeg_duration_in_ms, frameRate, totalFrames));
                //Log.d("JEBYRNE", String.format("VideoPlayerActivity.init (JSON): ffmpeg_duration_in_ms=%d, frameRate=%f", frameJSON.getMetaData().getDuration(), frameJSON.getMetaData().getFrameRate()));

                //frameRate = (int) frameJSON.getMetaData().getFrameRate();
                //frameRate = (double) frameJSON.getMetaData().getFrameRate();  // JEBYRNE: fractional framerates
                //frameDuration = Double.parseDouble(df.format(1000 / frameJSON.getMetaData().getFrameRate()));
                frameDuration = 1000 / frameRate;
                //totalFrames = (int) Math.round((frameJSON.getMetaData().getDuration() * 1000 * frameJSON.getMetaData().getFrameRate()) / 1000);
                if (instanceDetails != null){
                    try {
                        ratingQuestions = new HashMap<>();
                        for (int j = 0; j < instanceDetails.length(); j++) {
                            ratingQuestions.put(instanceDetails.getJSONObject(j).getInt("show_at_frame"),
                                    instanceDetails.getJSONObject(j));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                displayErrorDialog("Failed to play video");
            }
        } else {

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoFilePath);

            FFmpegMediaMetadataRetriever metadataRetriever = new FFmpegMediaMetadataRetriever();
            metadataRetriever.setDataSource(videoFilePath);  // FIXME: this line crashes with "java.lang.IllegalArgumentException: setDataSource failed: status = 0xFFFFFFFF"
            // https://console.firebase.google.com/u/1/project/collector-1581961981383/crashlytics/app/android:com.visym.collector/issues/f02f51cd67347f80c15013a6a6a5b616?time=last-ninety-days&type=all&sessionEventKey=61C3B46701FA000161F1151EA170F8EA_1623118229133719600

            //frameRate = (int) Double.parseDouble(metadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE));
            frameRate = (double) Double.parseDouble(metadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE));  // JEBYRNE: fractional framerates
            int ffmpeg_duration_in_ms = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));  // JEBYRNE: integer milliseconds  (FFMPEG is wrong, truncated to seconds)
            Log.d("JEBYRNE", String.format("init: ffmpeg_duration_in_ms=%d", ffmpeg_duration_in_ms));

            if (ffmpeg_duration_in_ms < 1000) {
                totalFrames = (int) Math.round((ffmpeg_duration_in_ms / frameRate) / 1000);
            }
            else {
                totalFrames = (int) (TimeUnit.MILLISECONDS.toSeconds(ffmpeg_duration_in_ms) * frameRate);
            }

            String format = df.format(1000 / frameRate);
            if (!TextUtils.isEmpty(format) && format.contains(",")){
                format = format.replace(",", ".");
            }
            frameDuration = Double.parseDouble(format);
        }
        videoPlayerView.setVideoURI(Uri.fromFile(new File(videoFilePath)));
        videoPlayerView.setOnInfoListener(this);
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
        totalTimeTextView.setText(getString(R.string.initial_timer_text));
        runningTimeTextView.setText(getString(R.string.initial_timer_text));
        handler = new Handler();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (!this.videoPrepared) {
            playerDuration = mp.getDuration();
            videoSeekBar.setMax(playerDuration);

            int videoWidth = mp.getVideoWidth();
            int videoHeight = mp.getVideoHeight();
            if (videoWidth > videoHeight) {
                orientation = "landscape";
                orientationEventListener.enable();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }

            totalTimeTextView.setText(DateUtil.getTotalVideoTime(playerDuration));
            videoPlayerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onOrientationChange(currentOrientation);
                    if (!trainingModule) {
                        objectsBoundingBox = presenter.generateObjectFrames(frameJSON, videoPlayerView.getWidth(), videoPlayerView.getHeight(), collectionDetail.getDefaultObject());  // JEBYRNE: may be null
                    }
                    progressBar.setVisibility(View.GONE);

                    TimerTask timerTask = startTimer();
                    timer.scheduleAtFixedRate(timerTask, 0, Math.round(frameDuration));
                    videoPlayerView.start();
                    videoPrepared = true;
                }
            }, 1000);
        } else {
            videoPlayerView.seekTo(Math.max(stoppedPosition, 0));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideSystemBar();
        if (!isVideoPaused){
            stoppedPosition = videoSeekBar.getProgress();
            updateVideoProgress();
        }else if (timer != null){
            timer.cancel();
            timer = null;
        }

        if (orientationEventListener != null) {
            orientationEventListener.disable();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            videoPlayerView.seekTo(progress);
            frameIndex = (int) Math.round(((progress * frameRate) / 1000));
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
        if (videoPrepared) {
            if (!isVideoPaused) {
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                videoPlayerView.pause();
            } else {
                if (playPauseButton.getVisibility() == View.VISIBLE) {
                    return;
                }
                if (currentPosition >= 0 && videoPlayerView.getCurrentPosition() != 100) {
                    TimerTask timerTask = startTimer();
                    timer.scheduleAtFixedRate(timerTask, 0, Math.round(frameDuration));
                    videoPlayerView.seekTo(currentPosition);
                    videoPlayerView.start();
                }
            }
            isVideoPaused = !isVideoPaused;

            if (ratingQuestionnaireLayout.getVisibility() == View.VISIBLE) {
                ratingQuestionnaireLayout.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            if (timer != null){
                timer.cancel();
                timer = null;
            }
            progressBar.setVisibility(View.VISIBLE);
            return true;
        }
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            TimerTask timerTask = startTimer();
            timer.scheduleAtFixedRate(timerTask, 0, Math.round(frameDuration));
            isVideoPaused = false;
            progressBar.setVisibility(View.GONE);
            return true;
        }
        progressBar.setVisibility(View.GONE);
        return false;
    }

    @Override
    public void onClick(View v) {
        Log.d("JEBYRNE", "VideoPlayerActivity.onClick");

        switch (v.getId()) {
            case R.id.cancel_button:
                if (videoPlayerView.isPlaying()) {
                    videoPlayerView.pause();
                }
                if (trainingModule) {
                    displayErrorDialog("Exit this collection?");
                } else {
                    onBackPressed();
                }
                break;

            case R.id.nextButton:
                if (!TextUtils.isEmpty(module) && module.contentEquals("training")) {
                    Intent videoCaptureIntent = new Intent(this,
                            VideoCaptureActivity.class);
                    videoCaptureIntent.putExtra("module", "training");
                    startActivity(videoCaptureIntent);
                } else {
                    onBackPressed();
                }
                break;

            case R.id.pause_play_button:
                replayVideo();
                break;

            case R.id.upVoteBtn:
                //hit rating api with data
                hitRatingApiWithData(ratingInstanceId, yesResponse, videoId, submittedTime, week, videoUploadedDate);
                break;

            case R.id.downVoteBtn:
                hitRatingApiWithData(ratingInstanceId, noResponse, videoId, submittedTime, week, videoUploadedDate);
                break;

            case R.id.submitButton:
                DeleteStrReviewAssignmentInput deleteStrReviewAssignmentInput = DeleteStrReviewAssignmentInput.builder()
                        .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY))
                        .video_id(videoId)
                        .build();
                Globals.showLoadingWithFlags(this);
                Globals.mAWSAppSyncClient.mutate(DeleteStrReviewAssignmentMutation.builder().input(deleteStrReviewAssignmentInput).build())
                        .enqueue(deleteStrReviewAssignmentDelete);
                break;
            case R.id.go_back_btn:
                onBackPressed();
                break;

            default:
                Log.d("JEBYRNE", "VideoPlayerActivity.onClick: UNKNOWN");
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("JEBYRNE", "VideoPlayerActivity.onBackPressed");

        if (!TextUtils.isEmpty(orientation) && orientation.contains("land")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        super.onBackPressed();
       // finish();
    }

    private void replayVideo() {
        frameIndex = 0;
        currentPosition = 0;
        videoPlayerView.seekTo(0);
        TimerTask timerTask = startTimer();
        timer.scheduleAtFixedRate(timerTask, 0, Math.round(frameDuration));
        if (submitButton.getVisibility() == View.VISIBLE)
            submitButton.setVisibility(View.GONE);

        if (playPauseButton.getVisibility() == View.VISIBLE)
            playPauseButton.setVisibility(View.GONE);
        videoPlayerView.start();
        isVideoPaused = false;
    }

    public void onFileDownloadFailure(String message) {
        FileUtil.deleteFile(jsonUrl);
        FileUtil.deleteFile(videoFilePath);
        jsonUrl = null;
        videoFilePath = null;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        logout();  // JEBYRNE
        finish();
    }

    public void hitRatingApiWithData(String instanceId, String response, String video_id, String submittedTime, String week, String videoUploadedDate) {
        Log.d("JEBYRNE", "VideoPlayerActivity.hitRatingApiWithData: " + instanceId + " " + response);
        Globals.showLoadingWithFlags(mContext);
        CreateStrRatingInput ratingInput = CreateStrRatingInput.builder()
                .rating_responses(response)  // JEBYRNE: set in displayQuestionAtFrame
                .id(instanceId)
                .video_id(video_id)
                .project_id(preference.readString(Constant.PROJECT_ID_KEY))
                .project_name(preference.readString(Constant.PROJECT_NAME_KEY))
                .program_id(preference.readString(Constant.PROGRAM_ID_KEY))
                .collection_id(preference.readString(Constant.COLLECTION_ID_KEY))
                .collection_name(preference.readString(Constant.COLLECTION_NAME_KEY))
                .reviewer_id(preference.readString(Constant.COLLECTOR_ID_KEY))
                .week(week)
                .video_uploaded_date(videoUploadedDate)
                .submitted_time(submittedTime)
                .build();

        Globals.mAWSAppSyncClient.mutate(CreateStrRatingMutation.builder().input(ratingInput).build()).enqueue(ratingResponse);
    }

    @Override
    public void onFileDownload(AwsResponse response) {
        TransferState state = response.getState();
        if (state == TransferState.COMPLETED) {
            if (response.getFileType() == AwsResponse.FILE_TYPE_JSON) {
                jsonUrl = response.getVideoUrl();
                AsyncTask.execute(() -> {
                    try {
                        frameJSON = FileUtil.readFromFile(VideoPlayerActivity.this, jsonUrl);
                        if (frameJSON == null) {
                            throw new Exception("frameJSON null");
                        }
                    } catch (Exception e) {  // catch all
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Toast.makeText(VideoPlayerActivity.this, "Something went wrong. Please try again later",  Toast.LENGTH_SHORT).show();
                                logout();  // JEBYRNE
                                finish();
                            }
                        });
                    }
                });
                presenter.downloadJSONFile(videoFilePath, trainingModule);
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        videoFilePath = response.getVideoUrl();
                        init();
                    }
                });
            }
        } else if (state == TransferState.FAILED) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideProgress();
                    onFileDownloadFailure(response.getErrorMessage());
                }
            });
        } else if (state == TransferState.IN_PROGRESS) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int progress = response.getProgress();
                    updateProgress(progress);
                }
            });
        }
    }

    @Override
    public void onFailure(String errorMessage) {
        hideProgress();
        //if (!TextUtils.isEmpty(errorMessage)) {
        //    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        //} else {
        //    Toast.makeText(this, "Something went wrong. Please try again later",
        //            Toast.LENGTH_SHORT).show();
        //}
        logout();  // JEBYRNE
        finish();
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
            jsonUrl = null;
            videoFilePath = null;
            Intent goToCollectionList = new Intent(VideoPlayerActivity.this,
                    DashboardActivity.class);
            goToCollectionList.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            goToCollectionList.putExtra("redirectTo", "capture");
            startActivity(goToCollectionList);
            finish();
        });
        dialog.show();
    }

    private void displayObjectsBoundingBoxes() {
        parentView.removeAllViews();
        List<Frame> frames = objectsBoundingBox.get(frameIndex);
        if (frames != null && !frames.isEmpty()) {
            String activityLabels = presenter.getActivityLabels(frameIndex, frameJSON);
            for (int i = 0; i < frames.size(); i++) {
                Frame frame = frames.get(i);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

                ImageView imageView = new ImageView(this);
                imageView.setId(i + 1);
                imageView.setBackgroundResource(R.drawable.bounding_box_border);
                imageView.setLayoutParams(params);

                imageView.getLayoutParams().width = frame.getWidth();
                imageView.getLayoutParams().height = frame.getHeight();

                int x = frame.getX();
                int y = frame.getY();
                imageView.setTranslationX(x + videoPlayerView.getX());
                imageView.setTranslationY(y + videoPlayerView.getY());

                RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

                TextView textView = new TextView(this);
                textView.setLayoutParams(textParams);
                textView.setPadding(4, 0, 4, 0);
                textView.setId(i + 1);
                textView.setBackgroundColor(ContextCompat.getColor(this, R.color.gold_color));
                textView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                textView.measure(0, 0);

                if (frame.isDefault() && !TextUtils.isEmpty(activityLabels)) {
                    textView.setText(activityLabels);
                    textView.setTranslationX(x + videoPlayerView.getX());
                    textView.setTranslationY(y + videoPlayerView.getY() - textView.getMeasuredHeight());
                    parentView.addView(textView);
                }
                parentView.addView(imageView);
                parentView.setVisibility(View.VISIBLE);
            }
        } else {
            parentView.setVisibility(View.GONE);
        }
    }

    private void getTrackOfFrames() {
        updateVideoProgress();
        try {
            JSONObject jsonObject = ratingQuestions.get(frameIndex);
            if (jsonObject != null) {
                displayQuestionAtFrame(
                        jsonObject.getString("no"), jsonObject.getString("instance_id"),
                        jsonObject.getString("question"), jsonObject.getString("yes"),
                        jsonObject.getString("question_short_name"), jsonObject.getString("week"),
                        jsonObject.getString("video_uploaded_date"), jsonObject.getString("value_add_when"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void displayQuestionAtFrame(String noresponse, String instanceId, String questionText,
                                        String yesresponse, String questionShortName, String weekStr, String videoUploadedDateStr, String valueaddwhen) {
        ratingQuestionnaireLayout.setVisibility(View.VISIBLE);
        questionTextView.setText(questionText);
        ratingInstanceId = instanceId;
        noResponse = noresponse + questionShortName;  // JEBYRNE:  'bad' + '_label', Both come from pycollector.admin.review.ReviewAssignment
        yesResponse = yesresponse;  // JEBYRNE: 'good', comes from pycollector.admin.review.ReviewAssignment
        week = weekStr;
        videoUploadedDate = videoUploadedDateStr;
        valueAddWhen = valueaddwhen;   // JEBYRNE: unused (I think)
        // Set submittedTime
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy, hh:mm:ss a");
        String currentDateandTime = sdf.format(new Date());
        submittedTime = currentDateandTime;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgress();
    }

    // JEBYRNE: common function to return to entry screen on error
    private void logout() {
        FileUtil.deleteFile(jsonUrl);
        FileUtil.deleteFile(videoFilePath);
        jsonUrl = null;
        videoFilePath = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(VideoPlayerActivity.this);
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
