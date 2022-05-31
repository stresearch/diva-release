package com.visym.collector.capturemodule.views;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.visym.collector.BaseActivity;
import com.visym.collector.R;
import com.visym.collector.capturemodule.IVideoEditModule;
import com.visym.collector.capturemodule.model.ActivityLabel;
import com.visym.collector.capturemodule.model.FrameJSON;
import com.visym.collector.capturemodule.presenters.ActivityEditPresenter;
import com.visym.collector.capturemodule.presenters.ObjectEditPresenter;
import com.visym.collector.capturemodule.presenters.VideoEditorPresenter;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.helper.CustomSeekBar;
import com.visym.collector.helper.ProgressSegment;
import com.visym.collector.model.CollectionModel;
import com.visym.collector.model.Frame;
import com.visym.collector.usermodule.view.FrontScreenActivity;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.AwsResponse;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.CoordinateUtil;
import com.visym.collector.utils.DateUtil;
import com.visym.collector.utils.FileUtil;
import com.visym.collector.utils.Globals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import wseemann.media.FFmpegMediaMetadataRetriever;

import static com.visym.collector.utils.Constant.ZOOM_CONSTANT_VALUE;

public class VideoEditorActivity extends BaseActivity implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnPreparedListener,
        SeekBar.OnSeekBarChangeListener, View.OnClickListener, View.OnTouchListener,
        PopupMenu.OnMenuItemClickListener, IVideoEditModule.IVideoEditView {

    private static final int MODULE_VIDEO_PREVIEW = 0;
    private static final int MODULE_ACTIVITY_EDITOR = 1;
    private static final int MODULE_OBJECT_EDITOR = 2;
    private static final String TAG = "VideoEditorActivity";

    @BindView(R.id.video_player_view)
    VideoView videoView;

    @BindView(R.id.total_time_textview)
    TextView totalTimeTextView;

    @BindView(R.id.running_time_textview)
    TextView runningTimeTextView;

    @BindView(R.id.edit_button)
    ImageView editButton;

    @BindView(R.id.info_icon)
    ImageView helpIconImageView;

    @BindView(R.id.retake_button)
    ImageView retakeButton;

    @BindView(R.id.activity_selector)
    ImageView activitySelectorImageView;

    @BindView(R.id.object_selector)
    ImageView objectSelectorImageView;

    @BindView(R.id.close_button)
    ImageView closeButton;

    @BindView(R.id.save_button)
    Button saveButton;

    @BindView(R.id.seekBar)
    SeekBar seekBar;

    @BindView(R.id.activity_custom_seekbar)
    CustomSeekBar activityCustomSeekBar;

    @BindView(R.id.interval_custom_seekbar)
    CustomSeekBar intervalCustomSeekBar;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @BindView(R.id.activity_textview)
    TextView activityNameTextView;

    @BindView(R.id.bounding_box_view)
    ImageView boundingBoxView;

    @BindView(R.id.parent_view)
    FrameLayout parentView;

    @BindView(R.id.root_view)
    FrameLayout rootView;

    @BindView(R.id.zoom_view)
    LinearLayout zoomView;

    @BindView(R.id.zoom_in_view)
    ImageView zoomInView;

    @BindView(R.id.zoom_out_view)
    ImageView zoomOutView;

    @BindView(R.id.option_view)
    RelativeLayout optionView;
    List<ActivityLabel> activities;
    private OrientationEventListener orientationListener;
    private boolean isEditingEnabled = false;
    private boolean isVideoEditable = false;
    private String jsonUrl, videoUrl;
    private FrameJSON copyFrame = null;
    private int moduleSelected = MODULE_VIDEO_PREVIEW;
    private boolean dialogOutsideTouch = true;
    private double frameDuration;
    private int frameIndex, videoDuration, editActivityStartFrame, editActivityEndFrame;
    private String selectedItem, editActivityName;
    private VideoEditorPresenter editPresenter;
    private ActivityEditPresenter activityEditPresenter;
    private ObjectEditPresenter objectEditPresenter;
    private JSONArray activities_array;
    private boolean isTwoFingerGesture = false;
    private HashMap<Integer, Frame> boundingBoxes;

    private int videoPreviewWidth, editActivityNumberOfInstance = 0;
    private int videoPreviewHeight;
    private int currentPosition = 0;

    private HashMap<Integer, List<Frame>> objectsBoundingBox;

    private int initX, initY;
    private boolean isVideoPlaying;
    private String[] objects;
    private String description;
    private String defaultObjectName;
    private boolean singleGestureMove = false;
    private Boolean editingValuesAvailable = null;
    private boolean leftOrRightOriented = false;

    private int zoomValue = 1;
    private int previousZoomWidth, previousZoomHeight;
    private boolean activityTracking = false;
    private boolean activityTrackingForCaption = false;
    private boolean isVideoEdited = false;
    private File videoFile;

    private String jsonFilePath;

    // temporary activity label
    private List<ActivityLabel> tempActivities;
    private HashMap<Integer, Frame> tempBoundingBoxes;
    private boolean isDefaultObject = false;
    private boolean objectTracking = false;
    private boolean isVideoPrepared = false;
    private int orientationValue;
    private String originalVideoPath;
    private CollectionModel collection;
    private AppSharedPreference preference;
    private VideoTimer videoTimer;

    private long startTime_in_nanoseconds;

    private class VideoTimer implements Runnable {
        @Override
        public void run() {
            onVideoProgress();
        }
    }

    private Handler timerHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };

    private void onVideoProgress() {
        //currentPositionAsFloat += frameDuration;  // JEBYRNE: frameDuration is double, keep high-res timing here
        //currentPosition = (int) Math.round(currentPositionAsFloat);  // integer milliseconds
        currentPosition = videoView.getCurrentPosition();
        seekBar.setProgress(currentPosition);  // JEBYRNE

        //Log.d("JEBYRNE", String.format("VideoEditorActivity.onVideoProgress: currentPositionAsFloat=%f, currentPosition=%d", currentPositionAsFloat, currentPosition));

        if (currentPosition >= (videoDuration - 750)) {  // JEBYRNE: still off by about 500 ms for some reason, just don't preview last second, yuck
            timerHandler.removeCallbacks(videoTimer);
            isVideoPlaying = false;
            if (moduleSelected == MODULE_VIDEO_PREVIEW) {
                frameIndex = 0;
                currentPosition = 0;
                seekBar.setProgress(currentPosition);
                videoView.seekTo(currentPosition);
            }
            videoView.pause();
            boundingBoxView.setVisibility(View.GONE);
            activityNameTextView.setVisibility(View.GONE);
            parentView.removeAllViews();

            try {
                if (moduleSelected == MODULE_ACTIVITY_EDITOR && !activityTracking) {
                    retakeButton.setEnabled(true);
                    retakeButton.setClickable(true);

                    saveButton.setEnabled(true);
                    saveButton.setClickable(true);

                    retakeButton.setBackgroundResource(R.drawable.ellipse_white);
                    retakeButton.setImageResource(R.drawable.ic_retake_black);
                    if (editActivityNumberOfInstance > 1) {
                        activityEditPresenter.deleteActivity(selectedItem, copyFrame);
                    }
                    objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame, videoView.getWidth(), videoView.getHeight(), defaultObjectName);  // JEBYRNE: may be null
                    JSONArray activityValues = activityEditPresenter.getActivitySegments(copyFrame.getActivity(), frameDuration, videoDuration);
                    displayCustomSeekBarColors(activityValues);
                } else if (moduleSelected == MODULE_OBJECT_EDITOR) {
                    if (boundingBoxes != null && boundingBoxes.isEmpty()) {
                        retakeButton.setEnabled(true);
                        retakeButton.setClickable(true);

                        saveButton.setEnabled(true);
                        saveButton.setClickable(true);

                        tempBoundingBoxes = new LinkedHashMap<>();
                        tempBoundingBoxes.putAll(boundingBoxes);

                        retakeButton.setBackgroundResource(R.drawable.ellipse_white);
                        retakeButton.setImageResource(R.drawable.ic_retake_black);

                        objectEditPresenter.deleteObjectFrames(selectedItem, copyFrame.getObject());
                        boundingBoxes = objectEditPresenter.getObjectBoxes(selectedItem, copyFrame,
                                videoView.getWidth(), videoView.getHeight());
                        displayCustomSeekBarColors(activityEditPresenter.getActivitySegments(copyFrame.getActivity(), frameDuration, videoDuration));
                    }
                }
                if (moduleSelected == MODULE_VIDEO_PREVIEW || moduleSelected == MODULE_ACTIVITY_EDITOR) {
                    displayObjectsBoundingBoxes();
                }
            } catch (JSONException e) {
                Log.d(TAG, "No activity tracking exception " + e.getMessage());
            }
        } else if (isVideoPlaying){
            //Log.d(TAG, "onVideoProgress: "+ videoView.getCurrentPosition());
            if (moduleSelected == MODULE_OBJECT_EDITOR) {
                if (currentPosition == 0) {
                    boundingBoxView.setVisibility(View.GONE);
                    activityNameTextView.setVisibility(View.GONE);
                }
                if (isTwoFingerGesture || singleGestureMove) {
                    trackBoundingBox();
                } else {
                    displayBoundingBox(frameIndex);
                }
            } else {
                displayObjectsBoundingBoxes();
            }
            seekBar.setProgress(currentPosition);
            //frameIndex++;
            frameIndex = (int) Math.round(currentPosition / frameDuration);  // JEBYRNE: This keeps frameIndex in lockstep with the current video, but there may be missing frameIndexes

            //timerHandler.postDelayed(videoTimer, (int) Math.round(frameDuration));  // JEBYRNE: rounding error?
            timerHandler.postDelayed(videoTimer, (int) Math.round(frameDuration/2));  // JEBYRNE: double update frequency to avoid missing frameIndex
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_editor);

        ButterKnife.bind(this);
        attachPresenter();
        preference = AppSharedPreference.getInstance();
        Intent intent = getIntent();
        if (intent.hasExtra(Constant.IS_VIDEO_EDITABLE)) {
            isVideoEditable = intent.getBooleanExtra(Constant.IS_VIDEO_EDITABLE, false);
        }

        if (intent.hasExtra(Constant.VIDEO_CONTENT_KEY)) {
            jsonUrl = intent.getStringExtra("jsonURL");
            videoUrl = intent.getStringExtra("videoURL");
            collection = new Gson().fromJson(intent.getStringExtra("collectionDetail"),
                    CollectionModel.class);
            originalVideoPath = intent.getStringExtra("originalVideoPath");
        }

        orientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation >= 45 && orientation < 135) {
                    if (orientationValue == Surface.ROTATION_90) {
                        return;
                    }
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    orientationValue = Surface.ROTATION_90;
                } else if (orientation >= 225 && orientation < 315) {
                    if (orientationValue == Surface.ROTATION_270) {
                        return;
                    }
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    orientationValue = Surface.ROTATION_270;
                }
            }
        };
        downloadFiles();
    }

    private void updateOrientation() {
        if (copyFrame == null) {
            logout();
        }
        else {
            String orientation = copyFrame.getMetaData().getOrientation();
            if (orientation != null && orientation.contains("landscape")) {
                if (orientation.contains("landscapeLeft")) {
                    orientationListener.enable();
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    orientationValue = Surface.ROTATION_270;
                } else if (orientation.contains("landscapeRight")) {
                    orientationListener.enable();
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    orientationValue = Surface.ROTATION_90;
                }
                objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame, videoView.getWidth(), videoView.getHeight(), defaultObjectName);  // JEBYRNE: may be null
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
    }

    public void attachPresenter() {
        if (editPresenter == null) {
            editPresenter = new VideoEditorPresenter();
        }
        if (activityEditPresenter == null) {
            activityEditPresenter = new ActivityEditPresenter();
        }
        if (objectEditPresenter == null) {
            objectEditPresenter = new ObjectEditPresenter();
        }

        editPresenter.onViewAttached(this);
        activityEditPresenter.onViewAttached(this, this);
        objectEditPresenter.onViewAttached(this, this);

    }

    public void showCustomSeekBar() {
        try {
            ArrayList<ProgressSegment> progressSegments = new ArrayList<>();
            if (copyFrame == null) {
                logout();
            }
            else {
                activities = copyFrame.getActivity();
                JSONArray finalSegments = activityEditPresenter.getActivitySegments(activities, frameDuration, videoDuration);
                if (finalSegments != null && finalSegments.length() > 0) {
                    for (int z = 0; z < finalSegments.length(); z++) {
                        ProgressSegment progressSegment = new ProgressSegment(Parcel.obtain());
                        progressSegment.progress = finalSegments.getJSONObject(z).getInt("endFrame") - finalSegments.getJSONObject(z).getInt("startFrame");
                        progressSegment.color = finalSegments.getJSONObject(z).getInt("color");
                        progressSegments.add(progressSegment);
                    }
                } else {
                    ProgressSegment progressSegment = new ProgressSegment(Parcel.obtain());
                    progressSegment.progress = videoDuration;
                    progressSegment.color = getResources().getColor(R.color.noActivityColor);
                    progressSegments.add(progressSegment);
                }
                activityCustomSeekBar.setProgressSegments(progressSegments);
                activityCustomSeekBar.setVisibility(View.VISIBLE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            logout();
        }
    }

    private void downloadFiles() {
        File jsonFile;
        if (jsonUrl.contains(Constant.FRAMES_JSON_FILE_NAME)) {
            jsonFile = new File(jsonUrl);
            videoFile = new File(videoUrl);
        } else {
            jsonFile = new File(getFilesDir(), jsonUrl);
            videoFile = new File(getFilesDir(), videoUrl);
        }
        if (!jsonFile.exists() || !videoFile.exists()) {
            showProgress("Downloading video please wait...", true);
            editPresenter.downloadFile(jsonUrl);
            return;
        }

        AsyncTask.execute(() -> {
            try {
                if (jsonUrl.contains(Constant.FRAMES_JSON_FILE_NAME)) {
                    copyFrame = FileUtil.readFromFile(VideoEditorActivity.this, Constant.FRAMES_JSON_FILE_NAME);
                } else {
                    copyFrame = FileUtil.readFromFile(VideoEditorActivity.this, jsonFile.getAbsolutePath());
                }
                if (copyFrame == null) {
                    throw new Exception("Empty copyFrame");
                }
                runOnUiThread(() -> initVideoViewer(videoFile.getAbsolutePath()));

            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onFileDownloadFailure(e.getMessage());
                    }
                });
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void onFileDownloadFailure(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    FileUtil.deleteFile(new File(getFilesDir(), jsonUrl).getAbsolutePath());
                    FileUtil.deleteFile(new File(getFilesDir(), videoUrl).getAbsolutePath());
                    dialog.dismiss();
                    logout();  // JEBYRNE
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void initVideoViewer(String filePath) {
        Log.d("JEBYRNE", String.format("VideoEditorActivity.initVideoViewer: %s", filePath));

        videoView.setVideoURI(Uri.fromFile(new File(filePath)));
        videoView.setOnErrorListener(this);
        videoView.setOnInfoListener(this);
        videoView.setOnPreparedListener(this);

        videoView.setOnTouchListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        editButton.setOnClickListener(this);
        activitySelectorImageView.setOnClickListener(this);
        objectSelectorImageView.setOnClickListener(this);
        retakeButton.setOnClickListener(this);
        zoomInView.setOnClickListener(this);
        zoomOutView.setOnClickListener(this);

        closeButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        helpIconImageView.setOnClickListener(this);
        initCollectionDetails();

        if (isVideoEditable) {
            isEditingEnabled = true;
            editButton.setVisibility(View.INVISIBLE);
            activitySelectorImageView.setVisibility(View.VISIBLE);
            objectSelectorImageView.setVisibility(View.VISIBLE);
            retakeButton.setVisibility(View.VISIBLE);
            zoomView.setVisibility(View.VISIBLE);
            retakeButton.setEnabled(false);
            retakeButton.setClickable(false);

            saveButton.setText(getString(R.string.save_text));
            saveButton.setEnabled(true);
            saveButton.setClickable(true);
        } else {
            editButton.setVisibility(View.VISIBLE);
            activitySelectorImageView.setVisibility(View.GONE);
            objectSelectorImageView.setVisibility(View.GONE);
            retakeButton.setVisibility(View.GONE);
            zoomView.setVisibility(View.GONE);
        }
        videoTimer = new VideoTimer();
        DecimalFormat df = new DecimalFormat("#.##");

        // JEBYRNE: crashalytics
        // https://console.firebase.google.com/u/1/project/collector-1581961981383/crashlytics/app/android:com.visym.collector/issues/509860d8eea5969da548dcb93e27b346?time=last-twenty-four-hours&sessionEventKey=617F8DC40350000172BEF328210AAB3C_1603935463032407366
        if (copyFrame == null) {
            logout();
        }
        else {
            frameDuration = 1000 / copyFrame.getMetaData().getFrameRate();  // JEBYRNE: this is now fractional milliseconds
            startTime_in_nanoseconds = currentPosition * 1000 * 1000;
            Log.d("JEBYRNE", String.format("VideoEditorActivity.initVideoViewer: frameRate=%f, frameDuration=%f", copyFrame.getMetaData().getFrameRate(), frameDuration));  // JEBYRNE
        }
    }

    private void initCollectionDetails() {
        if (!TextUtils.isEmpty(originalVideoPath)) {
            collection = new Gson().fromJson(preference.readString(Constant.COLLECTION_KEY),
                    CollectionModel.class);
        }
        if (collection != null) {
            this.description = collection.getCollectionDescription();
            this.defaultObjectName = collection.getDefaultObject();
            String activityNames = collection.getActivityShortNames();
            if (!TextUtils.isEmpty(activityNames)) {
                activities_array = new JSONArray();
                if (activityNames.contains(",")) {
                    String[] asn = activityNames.split(",");
                    for (String s : asn) {
                        activities_array.put(s);
                    }
                } else {
                    activities_array.put(activityNames);
                }
            }

            String objectList = collection.getObjectList();
            if (!TextUtils.isEmpty(objectList)) {
                objects = objectList.split(",");
            }
        }

        if (activities_array != null && activities_array.length() > 0) {
            editingValuesAvailable = true;
        }

        if (objects != null && objects.length > 0) {
            editingValuesAvailable = true;
        }
        if (editingValuesAvailable == null) {
            editingValuesAvailable = false;
        }
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
            FileUtil.deleteFile(new File(getFilesDir(), jsonUrl).getAbsolutePath());
            FileUtil.deleteFile(new File(getFilesDir(), videoUrl).getAbsolutePath());
            finish();
        });
        dialog.show();
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            timerHandler.removeCallbacks(videoTimer);
            isVideoPlaying = false;
            progressBar.setVisibility(View.VISIBLE);
            return true;
        }
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            timerHandler.post(videoTimer);
            isVideoPlaying = true;
            progressBar.setVisibility(View.GONE);
            return true;
        }
        progressBar.setVisibility(View.GONE);
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (!isVideoPrepared) {
            isVideoPrepared = true;
            //videoDuration = mp.getDuration();  // JEBYRNE: this is wrong

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoFile.getAbsolutePath());
            videoDuration = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));  // JEBYRNE: integer milliseconds
            Log.d("JEBYRNE", String.format("VideoEditorActivity.onPrepared: videoDuration (ms)=%d, mp.videoDuration (ms)=%d", videoDuration, mp.getDuration()));

            seekBar.setMax(videoDuration);
            totalTimeTextView.setText(DateUtil.getTotalVideoTime((int) videoDuration));
            showCustomSeekBar();
            updateOrientation();
            videoView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateUI();
                    progressBar.setVisibility(View.GONE);
                    videoPreviewWidth = videoView.getWidth();
                    videoPreviewHeight = videoView.getHeight();

                    previousZoomWidth = videoPreviewWidth;
                    previousZoomHeight = videoPreviewHeight;

                    if (videoPreviewWidth > videoPreviewHeight) {
                        leftOrRightOriented = true;
                    }
                    if (copyFrame == null) {
                        logout();
                    }
                    else {
                        objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame, videoView.getWidth(), videoView.getHeight(), defaultObjectName);  // JEBYRNE: may be null
                        videoView.start();
                        isVideoPlaying = true;
                        videoTimer.run();
                    }
                }
            }, 1000);
        }else {
            videoView.seekTo(Math.max(currentPosition, 0));
        }
    }

    private void updateUI() {
        int x = (int) videoView.getX();
        int y = (int) videoView.getY();

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) optionView.getLayoutParams();
        layoutParams.topMargin = y;
        layoutParams.leftMargin = x;
        layoutParams.rightMargin = x;
        optionView.requestLayout();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isVideoPlaying) {
            changeVideoState();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBar();
        currentPosition = seekBar.getProgress();
        if (isVideoPrepared) {
            if (videoView.getCurrentPosition() == 100 || currentPosition == 100) {
                currentPosition = 0;
            }
        }
    }


    private void trackBoundingBox() {
        int width = boundingBoxView.getLayoutParams().width;
        int height = boundingBoxView.getLayoutParams().height;

        float translationX = boundingBoxView.getX() - videoView.getX();
        float translationY = boundingBoxView.getY() - videoView.getY();

        Frame frame = new Frame((int) translationX, (int) translationY, width, height);
        frame.setDefault(isDefaultObject);
        boundingBoxes.put(frameIndex, frame);
    }

    private void displayBoundingBox(int frameIndex) {
        if (boundingBoxes != null) {
            Frame frame = boundingBoxes.get(frameIndex);
            if (frame != null) {
                activityNameTextView.setVisibility(frame.isDefault() ? View.VISIBLE : View.GONE);
                boundingBoxView.setVisibility(View.VISIBLE);
                if (!singleGestureMove) {
                    updateBorder(frame.getX(), frame.getY(), frame.getWidth(), frame.getHeight());
                }
            } else {
                activityNameTextView.setVisibility(View.GONE);
                boundingBoxView.setVisibility(View.GONE);
            }
        }
    }

    private void displayObjectsBoundingBoxes() {
        new Thread(){
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Log.d("JEBYRNE", String.format("VideoEditorActivity.displayObjectsBoundingBoxes: enter"));
                        parentView.removeAllViews();

                        // JEBYRNE: crashalytics
                        // https://console.firebase.google.com/u/1/project/collector-1581961981383/crashlytics/app/android:com.visym.collector/issues/4d2f300eb17c990be887bd7087938297?time=last-twenty-four-hours&sessionEventKey=617F4B3D015900017CD4B4A631D8AC08_1603865850511860756
                        if (objectsBoundingBox == null) {
                            logout();
                        }
                        else {
                            List<Frame> frames = objectsBoundingBox.get(frameIndex);
                            if (frames != null && !frames.isEmpty()) {
                                for (int i = 0; i < frames.size(); i++) {
                                    Frame frame = frames.get(i);
                                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                                            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

                                    ImageView imageView = new ImageView(VideoEditorActivity.this);
                                    imageView.setId(i + 1);
                                    imageView.setBackgroundResource(R.drawable.bounding_box_border);
                                    imageView.setLayoutParams(params);

                                    imageView.getLayoutParams().width = frame.getWidth();
                                    imageView.getLayoutParams().height = frame.getHeight();

                                    int x = frame.getX();
                                    int y = frame.getY();
                                    imageView.setTranslationX(x + videoView.getX());
                                    imageView.setTranslationY(y + videoView.getY());

                                    RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
                                            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

                                    TextView textView = new TextView(VideoEditorActivity.this);
                                    textView.setLayoutParams(textParams);
                                    textView.setPadding(10, 0, 10, 0);
                                    textView.setId(i + 1);
                                    textView.setBackgroundColor(ContextCompat.getColor(VideoEditorActivity.this, R.color.gold_color));
                                    textView.setTextColor(ContextCompat.getColor(VideoEditorActivity.this, android.R.color.black));
                                    textView.measure(0, 0);

                                    if (copyFrame == null) {
                                        logout();
                                    }
                                    else {
                                        String activityLabels = activityEditPresenter.getActivityLabels(frameIndex, copyFrame);
                                        if (moduleSelected == MODULE_ACTIVITY_EDITOR && !TextUtils.isEmpty(selectedItem)) {
                                            if (frame.isDefault()) {
                                                if (activityTrackingForCaption) {
                                                    textView.setText(selectedItem);
                                                    textView.setTranslationX(x + videoView.getX());
                                                    textView.setTranslationY(y + videoView.getY() - textView.getMeasuredHeight());
                                                    parentView.addView(textView);
                                                } else if (!TextUtils.isEmpty(activityLabels) && activityLabels.contains(selectedItem)) {
                                                    textView.setText(selectedItem);
                                                    textView.setTranslationX(x + videoView.getX());
                                                    textView.setTranslationY(y + videoView.getY() - textView.getMeasuredHeight());
                                                    parentView.addView(textView);
                                                }
                                            }
                                        } else {
                                            if (frame.isDefault() && !TextUtils.isEmpty(activityLabels)) {
                                                textView.setText(activityLabels);
                                                textView.setTranslationX(x + videoView.getX());
                                                textView.setTranslationY(y + videoView.getY() - textView.getMeasuredHeight());
                                                parentView.addView(textView);
                                            }
                                        }
                                        if (moduleSelected != MODULE_ACTIVITY_EDITOR) {
                                            parentView.addView(imageView);
                                        } else if (frame.isDefault()) {
                                            parentView.addView(imageView);
                                        }
                                        imageView.requestLayout();
                                        textView.requestLayout();
                                        parentView.setVisibility(View.VISIBLE);
                                    }

                                }
                            } else {
                                parentView.setVisibility(View.INVISIBLE);
                            }
                            //Log.d("JEBYRNE", String.format("VideoEditorActivity.displayObjectsBoundingBoxes: exit"));
                        }
                    }
                });
            }
        }.start();
    }

    private void updateBorder(int x, int y, int width, int height) {

        boundingBoxView.getLayoutParams().width = width;
        boundingBoxView.getLayoutParams().height = height;

        boundingBoxView.setTranslationX(x + videoView.getX());
        boundingBoxView.setTranslationY(y + videoView.getY());

        if (moduleSelected == MODULE_OBJECT_EDITOR) {
            activityNameTextView.setText(selectedItem);
            activityNameTextView.setVisibility(View.VISIBLE);
        }

        activityNameTextView.setTranslationX(x + videoView.getX());
        activityNameTextView.setTranslationY(y + videoView.getY() - activityNameTextView.getMeasuredHeight());

        boundingBoxView.requestLayout();
        activityNameTextView.requestLayout();
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        runningTimeTextView.setText(DateUtil.getTotalVideoTime(progress));
        if (fromUser) {
            frameIndex = (int) Math.round(seekBar.getProgress() / frameDuration);
            currentPosition = seekBar.getProgress();
            videoView.seekTo(currentPosition);
        }
    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        frameIndex = (int) Math.round(seekBar.getProgress() / frameDuration);
        videoView.seekTo(seekBar.getProgress());
        currentPosition = seekBar.getProgress();
        if (moduleSelected == MODULE_ACTIVITY_EDITOR) {
            displayObjectsBoundingBoxes();
        }
    }

    private void displayCustomSeekBarColors(JSONArray activityArray) {
        try {
            if (activityArray != null && activityArray.length() > 0) {
                ArrayList<ProgressSegment> nonEditedProgressSegments = new ArrayList<>();

                for (int i = 0; i < activityArray.length(); i++) {
                    JSONObject jsonObject = activityArray.getJSONObject(i);
                    ProgressSegment progressSegment2 = new ProgressSegment(Parcel.obtain());
                    progressSegment2.progress = jsonObject.getInt("endFrame") - jsonObject.getInt("startFrame");
                    progressSegment2.color = jsonObject.getInt("color");
                    nonEditedProgressSegments.add(progressSegment2);
                }
                activityCustomSeekBar.setVisibility(View.VISIBLE);
                activityCustomSeekBar.setProgressSegments(nonEditedProgressSegments, videoDuration);
            } else {
                activityCustomSeekBar.setVisibility(View.GONE);
            }
            activityCustomSeekBar.invalidate();

            ArrayList<ProgressSegment> intervalSegments = new ArrayList<>();
            if (copyFrame == null) {
                logout();
            }
            else {
                JSONArray boundingBoxesGap = activityEditPresenter.getBoundingBoxesGap(defaultObjectName, copyFrame, frameDuration, videoDuration);
                if (boundingBoxesGap != null && boundingBoxesGap.length() > 0) {
                    intervalCustomSeekBar.setVisibility(View.VISIBLE);
                    for (int i = 0; i < boundingBoxesGap.length(); i++) {
                        JSONObject jsonObject = boundingBoxesGap.getJSONObject(i);
                        ProgressSegment progressSegment = new ProgressSegment(Parcel.obtain());
                        progressSegment.progress = jsonObject.getInt("endFrame") - jsonObject.getInt("startFrame");
                        progressSegment.color = jsonObject.getInt("color");
                        intervalSegments.add(progressSegment);
                    }
                } else {
                    ProgressSegment progressSegment = new ProgressSegment(Parcel.obtain());
                    progressSegment.progress = videoDuration;
                    progressSegment.color = Color.TRANSPARENT;
                    intervalSegments.add(progressSegment);
                }
                intervalCustomSeekBar.setProgressSegments(intervalSegments, videoDuration);
                intervalCustomSeekBar.setVisibility(View.VISIBLE);
                intervalCustomSeekBar.invalidate();
            }
        } catch (Exception e) {
            Log.d(TAG, "displayCustomSeekBarColors: ", e.getCause());
            logout();
        }
    }

    @Override
    public void onClick(View v) {
        Log.d("JEBYRNE", String.format("VideoEditorActivity.onClick: enter"));

        switch (v.getId()) {
            case R.id.edit_button:
                isEditingEnabled = true;
                editButton.setVisibility(View.INVISIBLE);
                objectSelectorImageView.setVisibility(View.VISIBLE);
                activitySelectorImageView.setVisibility(View.VISIBLE);
                retakeButton.setVisibility(View.VISIBLE);
                zoomView.setVisibility(View.VISIBLE);

                retakeButton.setEnabled(false);
                retakeButton.setClickable(false);


                break;

            case R.id.zoom_in_view:
                if (!isVideoPlaying) {
                    if (zoomValue < 4) {
                        previousZoomWidth = videoView.getWidth();
                        previousZoomHeight = videoView.getHeight();
                        zoomValue++;
                        updatePreviewSize(true);
                    }
                } else {
                    Toast.makeText(this, "Video must be paused to use zoom feature",
                            Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.zoom_out_view:
                if (!isVideoPlaying) {
                    if (zoomValue > 1) {
                        previousZoomWidth = videoView.getWidth();
                        previousZoomHeight = videoView.getHeight();
                        updatePreviewSize(false);
                        zoomValue--;
                    }
                } else {
                    Toast.makeText(this, "Video must be paused to use zoom feature",
                            Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.activity_selector:
                if (editingValuesAvailable == null) {
                    onFailure("Please wait activities are downloading...");
                    return;
                }
                if (editingValuesAvailable) {
                    if (activities_array != null && activities_array.length() > 0) {
                        boundingBoxView.setVisibility(View.GONE);
                        activityNameTextView.setVisibility(View.GONE);

                        activitySelectorImageView.setBackgroundResource(R.drawable.ellipse_white);
                        activitySelectorImageView.setImageResource(R.drawable.ic_edit_activity_black);
                        objectSelectorImageView.setBackgroundResource(R.drawable.ellips);
                        objectSelectorImageView.setImageResource(R.drawable.ic_object_white);
                        retakeButton.setBackgroundResource(R.drawable.ellips);
                        retakeButton.setImageResource(R.drawable.ic_retake);

                        if (videoView.isPlaying()) {
                            changeVideoState();
                        }
                        moduleSelected = MODULE_ACTIVITY_EDITOR;
                        displayActivityPopupMenu(v, activities_array);
                    } else {
                        onFailure("Activities are not available for editing");
                    }
                } else {
                    onFailure("Activities are not available for editing");
                }
                break;

            case R.id.object_selector:

                if (editingValuesAvailable == null) {
                    onFailure("Please wait objects are downloading...");
                    return;
                }
                if (editingValuesAvailable) {
                    if (objects != null && objects.length > 0) {
                        objectSelectorImageView.setBackgroundResource(R.drawable.ellipse_white);
                        objectSelectorImageView.setImageResource(R.drawable.ic_edit_object_black);
                        activitySelectorImageView.setBackgroundResource(R.drawable.ellips);
                        activitySelectorImageView.setImageResource(R.drawable.ic_activity_white);
                        retakeButton.setBackgroundResource(R.drawable.ellips);
                        retakeButton.setImageResource(R.drawable.ic_retake);

                        if (videoView.isPlaying()) {
                            changeVideoState();
                        }
                        moduleSelected = MODULE_OBJECT_EDITOR;
                        displayPopupMenu(v, objects);
                    } else {
                        onFailure("Objects are not available for editing");
                    }
                } else {
                    onFailure("Objects are not available for editing");
                }
                break;

            case R.id.retake_button:
                if (moduleSelected == MODULE_ACTIVITY_EDITOR) {
                    if (copyFrame == null) {
                        logout();
                    }
                    else {
                        List<ActivityLabel> activities = copyFrame.getActivity();
                        if (!activities.isEmpty()) {
                            Iterator<ActivityLabel> iterator = activities.iterator();
                            while (iterator.hasNext()) {
                                ActivityLabel activityLabel = iterator.next();

                                // JEBYRNE: crashalytics
                                // https://console.firebase.google.com/u/1/project/collector-1581961981383/crashlytics/app/android:com.visym.collector/issues/56c8429a48570525b00624171e5dce21?time=last-twenty-four-hours&sessionEventKey=617F8724005300012125B473418FE4DB_1603930933305100268
                                String label = activityLabel.getLabel();
                                if (label == null) {
                                    logout();
                                } else if (activityLabel.getLabel().equalsIgnoreCase(selectedItem)) {
                                    iterator.remove();
                                }
                            }
                        }
                        // JEBYRNE: crashalytics
                        // https://console.firebase.google.com/u/1/project/collector-1581961981383/crashlytics/app/android:com.visym.collector/issues/d57449e28df451a57889d4f331b163e8?time=last-twenty-four-hours&sessionEventKey=617FECEC01130001278165AE7E8C5263_1604043884927757252
                        if (tempActivities == null) {
                            logout();
                        }
                        else {
                            activities.addAll(tempActivities);
                            try {
                                JSONArray activitySegments = activityEditPresenter.getActivitySegments(copyFrame.getActivity(), frameDuration, videoDuration);
                                displayCustomSeekBarColors(activitySegments);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            retakeButton.setEnabled(false);
                            retakeButton.setClickable(false);

                            retakeButton.setBackgroundResource(R.drawable.ellips);
                            retakeButton.setImageResource(R.drawable.ic_retake);
                        }
                    }
                } else if (moduleSelected == MODULE_OBJECT_EDITOR) {
                    frameIndex = 0;
                    currentPosition = 0;
                    seekBar.setProgress(currentPosition);
                    videoView.seekTo(currentPosition);
                    timerHandler.removeCallbacks(videoTimer);
                    isVideoPlaying = false;
                    boundingBoxView.setVisibility(View.GONE);

                    if (tempBoundingBoxes != null) {
                        boundingBoxes.clear();
                        boundingBoxes.putAll(tempBoundingBoxes);
                        tempBoundingBoxes = new LinkedHashMap<>();
                    }
                    if (copyFrame == null) {
                        logout();
                    }
                    else {
                        objectEditPresenter.updateEditedObject(selectedItem, copyFrame, boundingBoxes,
                                videoPreviewWidth, videoPreviewHeight, videoView.getWidth(),
                                videoView.getHeight(), (int) videoView.getX(), (int) videoView.getY());
                        objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame, videoView.getWidth(), videoView.getHeight(), defaultObjectName);  // JEBYRNE: may be null
                        intervalCustomSeekBar.setVisibility(View.GONE);

                        retakeButton.setBackgroundResource(R.drawable.ellips);
                        retakeButton.setImageResource(R.drawable.ic_retake);
                        displayBoundingBox(0);

                        retakeButton.setEnabled(false);
                        retakeButton.setClickable(false);

                        saveButton.setEnabled(true);
                        saveButton.setClickable(true);
                    }
                }
                Toast.makeText(this, "Previous edits are cleared", Toast.LENGTH_SHORT).show();
                frameIndex = 0;
                currentPosition = 0;
                seekBar.setProgress(currentPosition);
                videoView.seekTo(currentPosition);
                break;

            case R.id.close_button:
                onBackPressed();
                break;

            case R.id.info_icon:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Collection Description");
                View view = getLayoutInflater().inflate(R.layout.layout_video_help_icon, null);
                builder.setView(view);

                TextView descriptionTextView = view.findViewById(R.id.descriptionTextview);
                if (description != null) {
                    String replacedString = description.replace("\n", "<br>");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        descriptionTextView.setText(Html.fromHtml(replacedString, Html.FROM_HTML_MODE_LEGACY));
                    } else {
                        descriptionTextView.setText(Html.fromHtml(replacedString));
                    }
                } else {
                    descriptionTextView.setText(getResources().getString(R.string.noCollectionDescription));
                }

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                showDialogAndHideSystemBar(builder.create());
                break;

            case R.id.save_button:
                String buttonText = saveButton.getText().toString();
                if (buttonText.equalsIgnoreCase(getString(R.string.ok))) {
                    if (leftOrRightOriented) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        if (orientationListener != null) {
                            orientationListener.disable();
                        }
                    }
                    Intent intent = new Intent(this, DashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else if (buttonText.contentEquals("SAVE")) {
                    isVideoEdited = true;
                    selectedItem = null;
                    editActivityNumberOfInstance = 0;
                    if (copyFrame == null) {
                        logout();
                    }
                    else {
                        if (moduleSelected == MODULE_OBJECT_EDITOR) {
                            boundingBoxView.setVisibility(View.GONE);
                            activityNameTextView.setVisibility(View.GONE);
                            objectEditPresenter.updateEditedObject(selectedItem, copyFrame, boundingBoxes,
                                    videoPreviewWidth, videoPreviewHeight, videoView.getWidth(),
                                    videoView.getHeight(), (int) videoView.getX(), (int) videoView.getY());
                            objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame, videoView.getWidth(), videoView.getHeight(), defaultObjectName);  // JEBYRNE: may be null
                        } else if (moduleSelected == MODULE_ACTIVITY_EDITOR) {
                            tempActivities = null;
                            List<ActivityLabel> activities = copyFrame.getActivity();
                            if (activities != null && !activities.isEmpty()) {
                                Collections.sort(activities);
                            }
                            showCustomSeekBar();
                        }
                        if (videoView.isPlaying()) {
                            changeVideoState();
                        }
                        displayObjectsBoundingBoxes();

                        frameIndex = 0;
                        currentPosition = 0;
                        seekBar.setProgress(currentPosition);
                        videoView.seekTo(currentPosition);
                        timerHandler.removeCallbacks(videoTimer);
                        isVideoPlaying = false;
                        boundingBoxView.setVisibility(View.GONE);
                        moduleSelected = MODULE_VIDEO_PREVIEW;
                        isEditingEnabled = false;
                        saveButton.setText(getString(R.string.submit));
                        editButton.setVisibility(View.VISIBLE);
                        activitySelectorImageView.setVisibility(View.GONE);
                        objectSelectorImageView.setVisibility(View.GONE);
                        retakeButton.setVisibility(View.GONE);
                        zoomView.setVisibility(View.GONE);

                        activitySelectorImageView.setBackgroundResource(R.drawable.ellips);
                        activitySelectorImageView.setImageResource(R.drawable.ic_activity_white);

                        objectSelectorImageView.setBackgroundResource(R.drawable.ellips);
                        objectSelectorImageView.setImageResource(R.drawable.ic_object_white);

                        retakeButton.setBackgroundResource(R.drawable.ellips);
                        retakeButton.setImageResource(R.drawable.ic_retake);
                    }
                } else {
                    if (collection != null){
                        String activityShortNames = collection.getActivityShortNames();
                        if (TextUtils.isEmpty(activityShortNames)){
                            displayNoActivityDialog();
                            //uploadFiles();  // JEBYRNE: disable me
                        }else {
                            uploadFiles();
                        }
                    }else {
                        uploadFiles();
                    }
                }
                break;
        }
        Log.d("JEBYRNE", String.format("VideoEditorActivity.onClick: exit"));
    }

    private void displayNoActivityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setCancelable(false)
                .setMessage("Your video has no activities annotated. Submit?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    uploadFiles();
                })
                .setNegativeButton("No", (dialog, which) -> {
                });
        showDialogAndHideSystemBar(builder.create());
    }

    private void uploadFiles() {
        showProgress("Uploading video...", true);
        if (jsonUrl.contains(Constant.FRAMES_JSON_FILE_NAME)) {
            uploadAsNewVideo();
        } else {
            uploadEditedJSONFile();
        }
    }

    private void uploadAsNewVideo() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FFmpegMediaMetadataRetriever metadataRetriever = new FFmpegMediaMetadataRetriever();
                showProgress("Uploading video...", true);
                try {
                    metadataRetriever.setDataSource(videoUrl);

                    double frameRate = Double.parseDouble(metadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE));
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(videoUrl);

                    int duration = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));  // JEBYRNE: integer milliseconds
                    //int duration = Integer.parseInt(metadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION));  // JEBYRNE: integer milliseconds, this is wrong, floored to seconds

                    if (copyFrame == null) {
                        logout();  // JEBYRNE
                    }
                    else {
                        String videoId = new File(videoUrl).getName().replace(".mp4", "");
                        copyFrame.getMetaData().setVideoId(videoId);
                        copyFrame.getMetaData().setFrameRate(frameRate);
                        copyFrame.getMetaData().setDuration((float) (duration / 1000));
                        Type type = new TypeToken<FrameJSON>() {}.getType();
                        String jsonString = new Gson().toJson(copyFrame, type);
                        FileUtil.writeToJSONFile(Constant.FRAMES_JSON_FILE_NAME, jsonString);

                        String filePath = FileUtil.getFilePath(Constant.FRAMES_JSON_FILE_NAME);
                        editPresenter.uploadFile(filePath + "," + videoUrl, AwsResponse.FILE_TYPE_JSON);
                    }

                } catch (IOException e) {
                    hideProgress();
                    //Toast.makeText(VideoEditorActivity.this, "Video upload failed!", Toast.LENGTH_LONG).show();
                    logout();
                }
            }
        });
    }


    private void uploadEditedJSONFile() {
        String fileName = FileUtil.appendTimestamp(jsonUrl);
        if (TextUtils.isEmpty(fileName)) {
            return;
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (copyFrame == null) {
                        logout();
                    }
                    else {
                        Type type = new TypeToken<FrameJSON>() {
                        }.getType();
                        String jsonString = new Gson().toJson(copyFrame, type);
                        File file = FileUtil.writeToJSONFile(Constant.FRAMES_JSON_FILE_NAME, jsonString);
                        editPresenter.uploadFile(fileName + "," + file.getAbsolutePath(), AwsResponse.FILE_TYPE_UPDATED_JSON);
                    }
                } catch (IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logout();
                        }
                    //            hideProgress();
                    //        Toast.makeText(VideoEditorActivity.this, "Edited video upload failed!", Toast.LENGTH_LONG).show();
                    //    }
                    });
                }
            }
        });
    }

    private void updatePreviewSize(boolean zoomIn) {
        int viewWidth = videoView.getLayoutParams().width;
        int viewHeight = videoView.getLayoutParams().height;

        if (viewWidth == -1 || viewHeight == -1) {
            viewWidth = videoView.getWidth();
            viewHeight = videoView.getHeight();
        }

        if (zoomIn) {
            viewWidth += ZOOM_CONSTANT_VALUE;
            viewHeight += ZOOM_CONSTANT_VALUE;
        } else {
            viewWidth -= ZOOM_CONSTANT_VALUE;
            viewHeight -= ZOOM_CONSTANT_VALUE;
        }

        videoView.getLayoutParams().width = viewWidth;
        videoView.getLayoutParams().height = viewHeight;
        videoView.requestLayout();

        videoView.post(new Runnable() {
            @Override
            public void run() {
                if (copyFrame == null) {
                    logout();
                }
                else {
                    if (moduleSelected == MODULE_OBJECT_EDITOR) {
                        boundingBoxes = objectEditPresenter.getObjectBoxes(selectedItem, copyFrame,
                                videoView.getWidth(), videoView.getHeight());
                    } else {
                        objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame, videoView.getWidth(), videoView.getHeight(), defaultObjectName);  // JEBYRNE: may be null
                    }
                    if (moduleSelected != MODULE_OBJECT_EDITOR) {
                        displayObjectsBoundingBoxes();
                    }
                }
            }
        });
    }

    private void displayPopupMenu(View view, String[] items) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        Menu menu = popupMenu.getMenu();
        for (int i = 0; i < items.length; i++) {
            menu.add(0, i, 0, items[i]);
        }
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.setOnDismissListener(menu1 -> {
            if (dialogOutsideTouch) {
                currentPosition = videoView.getCurrentPosition();
                if (currentPosition > 0) {
                    changeVideoState();
                }
                objectSelectorImageView.setBackgroundResource(R.drawable.ellips);
                objectSelectorImageView.setImageResource(R.drawable.ic_object_white);
                moduleSelected = MODULE_VIDEO_PREVIEW;
            }
        });
        popupMenu.show();
    }

    private void displayActivityPopupMenu(View view, JSONArray activities) {
        try {
            PopupMenu popupMenu = new PopupMenu(this, view);
            Menu menu = popupMenu.getMenu();
            for (int i = 0; i < activities.length(); i++) {
                menu.add(0, i, 0, activities.getString(i));
            }
            popupMenu.show();
            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.setOnDismissListener(menu1 -> {
                if (dialogOutsideTouch) {
                    int currentPosition = videoView.getCurrentPosition();
                    if (currentPosition > 0) {
                        changeVideoState();
                    }
                    activitySelectorImageView.setBackgroundResource(R.drawable.ellips);
                    activitySelectorImageView.setImageResource(R.drawable.ic_activity_white);
                    moduleSelected = MODULE_VIDEO_PREVIEW;
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("JEBYRNE", "VideoEditorActivity.onBackPressed");

        if (isVideoEdited) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage("You want to discard the changes?")
                    .setPositiveButton("YES", (dialog, which) -> {
                        dialog.dismiss();
                        if (leftOrRightOriented) {
                            if (TextUtils.isEmpty(originalVideoPath)) {
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                            }
                            if (orientationListener != null) {
                                orientationListener.disable();
                            }
                        }
                        if (!jsonUrl.contains(Constant.FRAMES_JSON_FILE_NAME)) {
                            Intent intent = new Intent(this, DashboardActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                        finish();
                    })
                    .setNegativeButton("NO", (dialog, which) -> {
                        dialog.dismiss();
                    });
            showDialogAndHideSystemBar(builder.create());
        } else {
            if (leftOrRightOriented) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            super.onBackPressed();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d("JEBYRNE", String.format("VideoEditorActivity.onTouch: enter"));
        if (event.getPointerCount() == 1) {
            if (isEditingEnabled) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (moduleSelected == MODULE_ACTIVITY_EDITOR) {
                        int childCount = parentView.getChildCount();
                        if (childCount > 0) {
                            for (int i = 0; i < childCount; i++) {
                                if (parentView.getChildAt(i) instanceof ImageView) {
                                    ImageView view = (ImageView) parentView.getChildAt(i);
                                    List<Frame> frames = objectsBoundingBox.get(frameIndex);
                                    if (frames != null && !frames.isEmpty()) {
                                        for (Frame frame : frames) {
                                            if (frame.isDefault()) {
                                                // JEBYRNE: cannot use view.getMeasuredWidth() here since view may not be created yet, and this will be zero, use this hack instead
                                                boolean insideBox = CoordinateUtil.checkTouchPointsInsideBox(event,
                                                        (int) view.getX() - (int) videoView.getX(),
                                                        (int) view.getY() - (int) videoView.getY(),
                                                        frame.getWidth(), frame.getHeight());

                                                if (videoView.getCurrentPosition() == 0 && editActivityNumberOfInstance == 0) {
                                                    editActivityNumberOfInstance++;
                                                }
                                                if (insideBox) {
                                                    Log.d("JEBYRNE", String.format("VideoEditorActivity.onTouch: ACTIVITY_EDITOR, ACTION_DOWN (one finger, inside box)"));
                                                    activityTracking = true;
                                                    activityTrackingForCaption = true;
                                                    editActivityStartFrame = seekBar.getProgress();
                                                    retakeButton.setEnabled(true);
                                                    retakeButton.setClickable(true);
                                                    if (!videoView.isPlaying()) {
                                                        changeVideoState();
                                                    }
                                                } else {
                                                    Log.d("JEBYRNE", String.format("VideoEditorActivity.onTouch: ACTIVITY_EDITOR, ACTION_DOWN (one finger, outside box)"));
                                                    activityTracking = false;
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else if (event.getAction() == MotionEvent.ACTION_UP) {
                    singleGestureMove = false;
                    if (!isTwoFingerGesture) {
                        if (moduleSelected == MODULE_OBJECT_EDITOR) {
                            changeVideoState();
                        } else {
                            int childCount = parentView.getChildCount();
                            if (childCount > 0) {
                                for (int i = 0; i < childCount; i++) {
                                    if (parentView.getChildAt(i) instanceof ImageView) {
                                        ImageView view = (ImageView) parentView.getChildAt(i);
                                        if (moduleSelected == MODULE_VIDEO_PREVIEW) {
                                            changeVideoState();
                                        } else {
                                            activityTrackingForCaption = false;
                                            List<Frame> frames = objectsBoundingBox.get(frameIndex);
                                            if (frames != null && !frames.isEmpty()) {
                                                for (Frame frame : frames) {
                                                    if (frame.isDefault()) {
                                                        if (videoView.isPlaying() || activityTracking) {
                                                            if (videoView.isPlaying()) {
                                                                editActivityEndFrame = seekBar.getProgress();
                                                            } else {
                                                                editActivityEndFrame = videoDuration;
                                                            }

                                                            boolean insideBox = CoordinateUtil.checkTouchPointsInsideBox(event,
                                                                    (int) view.getX() - (int) videoView.getX(),
                                                                    (int) view.getY() - (int) videoView.getY(),
                                                                    frame.getWidth(), frame.getHeight());
                                                            if (!insideBox && !activityTracking) {
                                                                changeVideoState();
                                                                return false;
                                                            }
                                                            if (editActivityEndFrame - editActivityStartFrame < 300) {
                                                                return false;
                                                            }
                                                            trackActivity();
                                                        } else {
                                                            changeVideoState();
                                                        }
                                                    }
                                                }
                                            } else {
                                                changeVideoState();
                                            }
                                        }
                                        break;
                                    }
                                }
                            } else if (activityTracking) {
                                activityTrackingForCaption = false;
                                editActivityEndFrame = seekBar.getProgress();
                                if (editActivityEndFrame - editActivityStartFrame < 300) {
                                    return false;
                                }
                                trackActivity();
                            } else {
                                changeVideoState();
                            }
                        }
                    } else {
                        objectTracking = false;
                        if (boundingBoxView.getVisibility() == View.VISIBLE) {
                            parentView.removeAllViews();
                            boundingBoxView.setVisibility(View.GONE);
                            activityNameTextView.setVisibility(View.GONE);
                        }

                        if (moduleSelected == MODULE_OBJECT_EDITOR &&
                                boundingBoxes != null && boundingBoxes.size() > 0) {
                            retakeButton.setEnabled(true);
                            retakeButton.setClickable(true);

                            saveButton.setEnabled(true);
                            saveButton.setClickable(true);

                            retakeButton.setBackgroundResource(R.drawable.ellipse_white);
                            retakeButton.setImageResource(R.drawable.ic_retake_black);

                            try {
                                retakeButton.setBackgroundResource(R.drawable.ellipse_white);
                                retakeButton.setImageResource(R.drawable.ic_retake_black);

                                previousZoomWidth = videoView.getWidth();
                                previousZoomHeight = videoView.getHeight();

                                if (copyFrame == null) {
                                    logout();
                                }
                                else {
                                    objectEditPresenter.updateEditedObject(selectedItem, copyFrame, boundingBoxes,
                                            videoPreviewWidth, videoPreviewHeight, videoView.getWidth(), videoView.getHeight(),
                                            (int) videoView.getX(), (int) videoView.getY());
                                    boundingBoxes = objectEditPresenter.getObjectBoxes(selectedItem, copyFrame,
                                            videoView.getWidth(), videoView.getHeight());
                                    displayCustomSeekBarColors(activityEditPresenter.getActivitySegments(copyFrame.getActivity(), frameDuration, videoDuration));
                                }
                            } catch (JSONException ex) {
                                Log.d(TAG, "onTouch: " + ex.getMessage());
                            }
                        }
                        initX = 0;
                        initY = 0;
                        isTwoFingerGesture = false;
                    }
                }
                else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (moduleSelected == MODULE_OBJECT_EDITOR) {
                        int viewX = (int) Math.abs(event.getX());
                        int viewY = (int) Math.abs(event.getY());
                        if (initX == 0 || initY == 0) {
                            initX = viewX;
                            initY = viewY;
                            return true;
                        }

                        boolean insideBox = CoordinateUtil.checkTouchPointsInsideBox(event,
                                (int) boundingBoxView.getX(), (int) boundingBoxView.getY(),
                                boundingBoxView.getWidth(), boundingBoxView.getHeight());
                        if (insideBox) {
                            singleGestureMove = true;
                            if (viewX > initX) {
                                int diff = viewX - initX;
                                boundingBoxView.setX(boundingBoxView.getX() + diff);
                            } else {
                                int diff = initX - viewX;
                                boundingBoxView.setX(boundingBoxView.getX() - diff);
                            }

                            if (viewY > initY) {
                                int diff = viewY - initY;
                                boundingBoxView.setY(boundingBoxView.getY() + diff);
                            } else {
                                int diff = initY - viewY;
                                boundingBoxView.setY(boundingBoxView.getY() - diff);
                            }
                            boundingBoxView.requestLayout();
                            initX = viewX;
                            initY = viewY;
                        }
                    }
                }
            }
            else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (moduleSelected != MODULE_ACTIVITY_EDITOR && moduleSelected != MODULE_OBJECT_EDITOR) {
                    changeVideoState();
                }
            }
        }
        else if (event.getPointerCount() == 2 && moduleSelected == MODULE_OBJECT_EDITOR) {
            if (!videoView.isPlaying() && !objectTracking) {
                objectTracking = true;
                changeVideoState();
            }
            boundingBoxView.setVisibility(View.VISIBLE);
            activityNameTextView.setVisibility(View.VISIBLE);
            isTwoFingerGesture = true;
            updateBoundingBox(event);
        }
        Log.d("JEBYRNE", String.format("VideoEditorActivity.onTouch: exit"));
        return true;
    }

    private void trackActivity() {
        try {
            Log.d("JEBYRNE", String.format("VideoEditorActivity.trackActivity: enter"));

            retakeButton.setEnabled(true);
            retakeButton.setClickable(true);

            saveButton.setEnabled(true);
            saveButton.setClickable(true);

            int startFrame = (int) Math.round(editActivityStartFrame / frameDuration);
            int endFrame = (int) Math.round(editActivityEndFrame / frameDuration);

            ActivityLabel label = new ActivityLabel();
            label.setStartFrame(startFrame);
            label.setEndFrame(endFrame);
            label.setLabel(selectedItem);

            if (copyFrame == null) {
                logout();
            }
            else {
                copyFrame.getActivity().add(label);

                JSONArray activitySegments = activityEditPresenter.getActivitySegments(copyFrame.getActivity(), frameDuration, videoDuration);
                displayCustomSeekBarColors(activitySegments);

                if (activityTracking) {
                    retakeButton.setBackgroundResource(R.drawable.ellipse_white);
                    retakeButton.setImageResource(R.drawable.ic_retake_black);
                }
            }
        } catch (JSONException e) {
            Log.d(TAG, "onTouch: activity editing " + e.getMessage());
        }
    }


    private void updateBoundingBox(MotionEvent event) {
        float a1 = event.getX(0);
        float b1 = event.getY(0);

        float a2 = event.getX(1);
        float b2 = event.getY(1);

        int width = a1 > a2 ? (int) (a1 - a2) : (int) (a2 - a1);
        int height = b1 > b2 ? (int) (b1 - b2) : (int) (b2 - b1);

        float x = Math.min(a1, a2);
        float y = Math.min(b1, b2);

        if (moduleSelected == MODULE_OBJECT_EDITOR) {
            activityNameTextView.setText(selectedItem);
        }

        boundingBoxView.getLayoutParams().width = width;
        boundingBoxView.getLayoutParams().height = height;

        boundingBoxView.setTranslationX(x + videoView.getX());
        boundingBoxView.setTranslationY(y + videoView.getY());

        activityNameTextView.setTranslationX(x + videoView.getX());
        activityNameTextView.setTranslationY(y + videoView.getY() - activityNameTextView.getMeasuredHeight());

        boundingBoxView.requestLayout();
        activityNameTextView.requestLayout();
    }

    private void changeVideoState() {
        if (isVideoPlaying) {
            timerHandler.removeCallbacks(videoTimer);
            videoView.pause();
        } else {
            if (currentPosition >= 0 && videoView.getCurrentPosition() != 100) {
                timerHandler.post(videoTimer);
                videoView.start();
            }
        }
        isVideoPlaying = !isVideoPlaying;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        dialogOutsideTouch = false;
        selectedItem = item.getTitle().toString();
        saveButton.setText(getString(R.string.save_text));
        frameIndex = 0;
        videoView.seekTo(0);
        videoView.pause();
        seekBar.setProgress(0);
        currentPosition = 0;
        isVideoPlaying = false;
        timerHandler.removeCallbacks(videoTimer);
        boundingBoxView.setVisibility(View.GONE);

        retakeButton.setEnabled(false);
        retakeButton.setClickable(false);

        saveButton.setEnabled(true);
        saveButton.setClickable(true);

        if (moduleSelected == MODULE_OBJECT_EDITOR) {
            isDefaultObject = defaultObjectName != null
                    && defaultObjectName.contentEquals(selectedItem);
            activitySelectorImageView.setBackgroundResource(R.drawable.ellips);
            activitySelectorImageView.setImageResource(R.drawable.ic_activity_white);

            parentView.removeAllViews();
            boundingBoxes = new LinkedHashMap<>();

            tempBoundingBoxes = new LinkedHashMap<>();
            if (copyFrame == null) {
                logout();
            }
            else {
                tempBoundingBoxes.putAll(objectEditPresenter.getObjectBoxes(selectedItem, copyFrame,
                        videoView.getWidth(), videoView.getHeight()));
            }
        } else if (moduleSelected == MODULE_ACTIVITY_EDITOR) {
            objectSelectorImageView.setBackgroundResource(R.drawable.ellips);
            objectSelectorImageView.setImageResource(R.drawable.ic_object_white);

            try {
                if (copyFrame == null) {
                    logout();
                }
                else {
                    if (editActivityNumberOfInstance == 0 && tempActivities != null && !tempActivities.isEmpty()) {
                        copyFrame.getActivity().addAll(tempActivities);
                    }
                    if (tempActivities == null) {
                        tempActivities = new ArrayList<>();
                    } else {
                        tempActivities.clear();
                    }
                    editActivityNumberOfInstance = 0;
                    List<ActivityLabel> activities = copyFrame.getActivity();
                    if (activities != null && !activities.isEmpty()) {
                        Iterator<ActivityLabel> labelIterator = activities.iterator();
                        while (labelIterator.hasNext()) {
                            ActivityLabel activity = labelIterator.next();

                            // JEBYRNE: crashalytics
                            // https://console.firebase.google.com/u/1/project/collector-1581961981383/crashlytics/app/android:com.visym.collector/issues/da852ccaa766372cb6cf32f9f4354215?time=last-twenty-four-hours&sessionEventKey=61802FA701BF0001597AC60EEE4045BF_1604115248142518994
                            if (objectsBoundingBox == null) {
                                logout();
                            } else {
                                if (activity.getLabel().equalsIgnoreCase(selectedItem)) {
                                    tempActivities.add(activity);
                                    labelIterator.remove();
                                }
                            }
                        }
                    }

                    if (tempActivities.size() > 0) {
                        retakeButton.setBackgroundResource(R.drawable.ellipse_white);
                        retakeButton.setImageResource(R.drawable.ic_retake_black);
                    } else {
                        retakeButton.setBackgroundResource(R.drawable.ellips);
                        retakeButton.setImageResource(R.drawable.ic_retake);
                    }
                    JSONArray activitySegments = activityEditPresenter.getActivitySegments(activities, frameDuration, videoDuration);
                    displayCustomSeekBarColors(activitySegments);
                    displayObjectsBoundingBoxes();
                }
            } catch (JSONException e) {
                Log.d(TAG, "onMenuItemClick: activity selection " + e.getCause());
            }
            if (copyFrame == null) {
                logout();
            }
            objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame, videoView.getWidth(), videoView.getHeight(), defaultObjectName);  // JEBYRNE: may be null
        }
        return true;
    }

    @Override
    public void onFileDownload(AwsResponse response) {
        TransferState state = response.getState();
        if (state == TransferState.COMPLETED) {
            if (response.getFileType() == AwsResponse.FILE_TYPE_JSON) {
                editPresenter.downloadFile(videoUrl);
            } else if (response.getFileType() == AwsResponse.FILE_TYPE_VIDEO) {
                videoUrl = response.getVideoUrl();
                AsyncTask.execute(() -> {
                    try {
                        File jsonFile = new File(getFilesDir(), jsonUrl);
                        copyFrame = FileUtil.readFromFile(VideoEditorActivity.this, jsonFile.getAbsolutePath());
                        if (copyFrame == null) {
                            throw new Exception("Empty copyFrame");
                        }
                        objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame, videoView.getWidth(), videoView.getHeight(), defaultObjectName);  // JEBYRNE: may be null
                        if (objectsBoundingBox == null) {
                            throw new Exception("Empty objectsBoundingBox");
                        }
                        runOnUiThread(() -> {
                            hideProgress();
                            initVideoViewer(videoUrl);
                        });
                    } catch (Exception e) {  // JEBYRNE: catch all exceptions
                        runOnUiThread(() -> {
                            hideProgress();
                            onFileDownloadFailure(e.getMessage());
                        });
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FileUtil.deleteFile(new File(getFilesDir(), jsonUrl).getAbsolutePath());
                FileUtil.deleteFile(new File(getFilesDir(), videoUrl).getAbsolutePath());
                hideProgress();
                Toast.makeText(VideoEditorActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                if (copyFrame == null) {
                    logout();  // JEBYRNE
                }
                finish();
            }
        });
    }

    @Override
    public void onFileUploadFailure(String errorMessage) {
        //onFailure(errorMessage);
        logout();  // JEBYRNE
    }

    @Override
    public void onFileUploadSuccess(AwsResponse response) {
        TransferState state = response.getState();
        if (state == TransferState.COMPLETED) {
            int fileType = response.getFileType();
            switch (fileType) {
                case AwsResponse.FILE_TYPE_VIDEO:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            videoUrl = response.getVideoUrl();
                            hideProgress();
                            AlertDialog.Builder builder = new AlertDialog.Builder(VideoEditorActivity.this);
                            builder.setMessage("Video uploaded successfully");
                            builder.setCancelable(false);
                            builder.setPositiveButton("OK", (dialog1, which) -> {
                                if (copyFrame == null) {
                                    logout();  // JEBYRNE
                                }
                                else {
                                    String orientation = copyFrame.getMetaData().getOrientation();
                                    if (!TextUtils.isEmpty(orientation) && orientation.contains("land")) {
                                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                                    }
                                    FileUtil.deleteFile(new File(getFilesDir(), jsonUrl).getAbsolutePath());
                                    FileUtil.deleteFile(new File(getFilesDir(), videoUrl).getAbsolutePath());
                                    FileUtil.deleteFile(originalVideoPath);

                                    Intent intent = new Intent(VideoEditorActivity.this, DashboardActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.putExtra("redirectTo", "capture");
                                    startActivity(intent);

                                    finish();
                                }
                            });
                            showDialogAndHideSystemBar(builder.create());
                        }
                    });
                    break;

                case AwsResponse.FILE_TYPE_JSON:
                    // JEBYRNE: upload order
                    // - AFter the JSON is successfully uploaded, then upload the video, then and only then show success
                    // - This terrible code is going to give me a brain tumor
                    jsonUrl = response.getVideoUrl();
                    editPresenter.uploadFile(videoUrl, AwsResponse.FILE_TYPE_VIDEO);
                    break;

                case AwsResponse.FILE_TYPE_UPDATED_JSON:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            FileUtil.deleteFile(jsonUrl);
                            hideProgress();

                            AlertDialog.Builder builder = new AlertDialog.Builder(VideoEditorActivity.this);
                            builder.setMessage("Video edited successfully");
                            builder.setCancelable(false);
                            builder.setPositiveButton("OK", (dialog1, which) -> {
                                Intent intent = new Intent(VideoEditorActivity.this, DashboardActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                intent.putExtra("redirectTo", "home");
                                startActivity(intent);
                                finish();
                            });
                            showDialogAndHideSystemBar(builder.create());
                        }
                    });

            }
        } else if (state == TransferState.FAILED){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideProgress();
                    Toast.makeText(VideoEditorActivity.this, "Upload failed!", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (editPresenter != null) {
            editPresenter.onViewDetached();
        }
        if (activityEditPresenter != null) {
            activityEditPresenter.onViewDetached();
        }
        if (objectEditPresenter != null) {
            objectEditPresenter.onViewDetached();
        }
        hideProgress();
    }

    // JEBYRNE: common function to return to entry screen on error
    private void logout() {
        copyFrame = null;
        FileUtil.deleteFile(new File(getFilesDir(), jsonUrl).getAbsolutePath());
        FileUtil.deleteFile(new File(getFilesDir(), videoUrl).getAbsolutePath());
        FileUtil.deleteFile(originalVideoPath);
        FileUtil.deleteFile(Constant.FRAMES_JSON_FILE_NAME);

        AlertDialog.Builder builder = new AlertDialog.Builder(VideoEditorActivity.this);
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
