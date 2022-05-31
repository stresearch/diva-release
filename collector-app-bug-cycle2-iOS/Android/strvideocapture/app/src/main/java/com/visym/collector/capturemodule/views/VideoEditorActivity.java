package com.visym.collector.capturemodule.views;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

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
import com.visym.collector.model.Frame;
import com.visym.collector.utils.AwsResponse;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.CoordinateUtil;
import com.visym.collector.utils.DateUtil;
import com.visym.collector.utils.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

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

    @BindView(R.id.zoom_view)
    LinearLayout zoomView;

    @BindView(R.id.zoom_in_view)
    ImageView zoomInView;

    @BindView(R.id.zoom_out_view)
    ImageView zoomOutView;

    @BindView(R.id.root_view)
    FrameLayout rootView;

    @BindView(R.id.option_view)
    RelativeLayout optionView;

    private OrientationEventListener orientationListener;
    private Handler videoHandler;
    private boolean isEditingEnabled = false;
    private boolean isVideoPlaying = false;
    private boolean isVideoEditable = false;
    private String jsonUrl, videoUrl;
    private FrameJSON copyFrame;
    private int moduleSelected = MODULE_VIDEO_PREVIEW;
    private boolean dialogOutsideTouch = true;

    private double frameDuration;
    private int frameIndex, videoDuration, editActivityStartFrame, editActivityEndFrame;

    private String selectedItem, editActivityName;
    private String collectionId;
    private VideoEditorPresenter editPresenter;
    private ActivityEditPresenter activityEditPresenter;
    private ObjectEditPresenter objectEditPresenter;
    private JSONArray activities_array;
    List<ActivityLabel> activities;
    private boolean isTwoFingerGesture = false;
    private HashMap<Integer, Frame> boundingBoxes;

    private int videoPreviewWidth, editActivityNumberOfInstance = 0;
    private int videoPreviewHeight;
    private int currentPosition = 0;

    private HashMap<Integer, List<Frame>> objectsBoundingBox;

    private int initX, initY;

    private String[] objects;
    private String description;
    private boolean singleGestureMove = false;
    private Boolean editingValuesAvailable = null;
    private boolean leftOrRightOriented = false;

    private int zoomValue = 1;
    private int previousZoomWidth, previousZoomHeight;
    private boolean activityTracking = false;
    private boolean activityTrackingForCaption = false;
    private File videoFile;

    // temporary activity label
    private List<ActivityLabel> tempActivities;
    private HashMap<Integer, Frame> tempBoundingBoxes;
    private boolean isDefaultObject;
    private boolean objectTracking = false;
    private boolean isVideoPrepared = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_editor);

        ButterKnife.bind(this);
        attachPresenter();

        Intent intent = getIntent();
        if (intent.hasExtra(Constant.IS_VIDEO_EDITABLE)) {
            isVideoEditable = intent.getBooleanExtra(Constant.IS_VIDEO_EDITABLE, false);
        }

        if (intent.hasExtra(Constant.VIDEO_CONTENT_KEY)) {
            jsonUrl = intent.getStringExtra("jsonURL");
            videoUrl = intent.getStringExtra("videoURL");
            collectionId = intent.getStringExtra("collectionId");
            downloadFiles();
        }

        editPresenter.getActivitiesForCollections(collectionId);
        orientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation >= 45 && orientation < 135) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                } else if (orientation >= 225 && orientation < 315) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        };
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
            activities = copyFrame.getActivity();
            JSONArray finalSegments = activityEditPresenter.getActivitySegments(activities,
                    frameDuration, videoDuration);
            if (finalSegments != null && finalSegments.length() > 0) {
                for (int z = 0; z < finalSegments.length(); z++) {
                    ProgressSegment progressSegment = new ProgressSegment(Parcel.obtain());
                    progressSegment.progress = finalSegments.getJSONObject(z).getInt("endFrame")
                            - finalSegments.getJSONObject(z).getInt("startFrame");
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
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void downloadFiles() {
        videoFile = new File(getFilesDir(), videoUrl);
        if (videoFile.exists()) {
            AsyncTask.execute(() -> {
                try {
                    File jsonFile = new File(getFilesDir(), jsonUrl);
                    copyFrame = FileUtil.readFromFile(VideoEditorActivity.this,
                            jsonFile.getAbsolutePath());
                    runOnUiThread(() -> initVideoViewer(videoFile.getAbsolutePath()));
                } catch (IOException e) {
                    onFileDownloadFailure(e.getMessage());
                }
            });
        } else {
            showProgress("Downloading video please wait...", true);
            editPresenter.downloadJSONFile(jsonUrl);
        }
    }

    private void onFileDownloadFailure(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    FileUtil.deleteFile(jsonUrl);
                    FileUtil.deleteFile(videoUrl);
                    dialog.dismiss();
                    finish();
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void initVideoViewer(String filePath) {
        videoView.setVideoPath(filePath);
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
        videoHandler = new Handler();

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
            saveButton.setEnabled(false);
            saveButton.setClickable(false);
        } else {
            editButton.setVisibility(View.VISIBLE);
            activitySelectorImageView.setVisibility(View.GONE);
            objectSelectorImageView.setVisibility(View.GONE);
            retakeButton.setVisibility(View.GONE);
            zoomView.setVisibility(View.GONE);
        }

        DecimalFormat df = new DecimalFormat("#.##");
        frameDuration = Double.parseDouble(df.format(1000 / copyFrame.getMetaData().getFrameRate()));
        videoView.start();
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            videoHandler.post(videoProgressUpdater);
            return true;
        }
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            videoHandler.removeCallbacks(videoProgressUpdater);
            progressBar.setVisibility(View.VISIBLE);
            return true;
        }
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            videoHandler.post(videoProgressUpdater);
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
            isVideoPlaying = true;
            progressBar.setVisibility(View.GONE);
            videoView.post(new Runnable() {
                @Override
                public void run() {
                    videoPreviewWidth = videoView.getWidth();
                    videoPreviewHeight = videoView.getHeight();

                    int x = (int) videoView.getX();
                    int y = (int) videoView.getY();

                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) optionView.getLayoutParams();
                    layoutParams.topMargin = y;
                    layoutParams.leftMargin = x;
                    layoutParams.rightMargin = x;
                    optionView.requestLayout();

                    previousZoomWidth = videoPreviewWidth;
                    previousZoomHeight = videoPreviewHeight;

                    if (videoPreviewWidth > videoPreviewHeight) {
                        leftOrRightOriented = true;
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        if (orientationListener != null) {
                            orientationListener.enable();
                        }
                    }
                    objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame,
                            videoView.getWidth(), videoView.getHeight());
                }
            });

            videoDuration = mp.getDuration();
            seekBar.setMax(videoDuration);
            totalTimeTextView.setText(DateUtil.getTotalVideoTime((int) videoDuration));
            showCustomSeekBar();
        }
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
        if (isVideoPrepared) {
            if (videoView.getCurrentPosition() == 100 || currentPosition == 100) {
                currentPosition = 0;
            }
            videoView.seekTo(currentPosition);
        }
    }

    private Runnable videoProgressUpdater = new Runnable() {
        @Override
        public void run() {
            if (currentPosition <= videoDuration) {
                isVideoPlaying = true;
                currentPosition += frameDuration;
                seekBar.setProgress(currentPosition);
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
                frameIndex++;
                videoHandler.postDelayed(this, (long) frameDuration);
            } else {
                videoHandler.removeCallbacks(videoProgressUpdater);
                frameIndex = 0;
                isVideoPlaying = false;
                currentPosition = 0;
                seekBar.setProgress(currentPosition);
                videoView.seekTo(currentPosition);
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
                        objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame,
                                videoView.getWidth(), videoView.getHeight());
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
            }
        }
    };

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
        parentView.removeAllViews();
        List<Frame> frames = objectsBoundingBox.get(frameIndex);
        if (frames != null && !frames.isEmpty()) {
            String activityLabels = activityEditPresenter.getActivityLabels(frameIndex, copyFrame);
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
                imageView.setTranslationX(x + videoView.getX());
                imageView.setTranslationY(y + videoView.getY());

                RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

                TextView textView = new TextView(this);
                textView.setLayoutParams(textParams);
                textView.setPadding(4, 0, 4, 0);
                textView.setId(i + 1);
                textView.setBackgroundColor(ContextCompat.getColor(this, R.color.gold_color));
                textView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                textView.measure(0, 0);

                if (moduleSelected == MODULE_ACTIVITY_EDITOR && !TextUtils.isEmpty(selectedItem)) {
                    if (frame.isDefault()){
                        if (activityTrackingForCaption) {
                            textView.setText(selectedItem);
                            textView.setTranslationX(x + videoView.getX());
                            textView.setTranslationY(y + videoView.getY() - textView.getMeasuredHeight());
                            parentView.addView(textView);
                        }else if (!TextUtils.isEmpty(activityLabels) && activityLabels.contains(selectedItem)) {
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
                if (moduleSelected != MODULE_ACTIVITY_EDITOR){
                    parentView.addView(imageView);
                }else if (frame.isDefault()){
                    parentView.addView(imageView);
                }
                parentView.setVisibility(View.VISIBLE);
            }
        } else {
            parentView.setVisibility(View.GONE);
        }
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
            frameIndex = seekBar.getProgress() / (int) frameDuration;
            currentPosition = seekBar.getProgress();
            videoView.seekTo(currentPosition);
        }
    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        frameIndex = seekBar.getProgress() / (int) frameDuration;
        videoView.seekTo(seekBar.getProgress());
        currentPosition = videoView.getCurrentPosition();
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
            JSONArray boundingBoxesGap = activityEditPresenter.getBoundingBoxesGap(copyFrame,
                    frameDuration, videoDuration);
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
        } catch (Exception e) {
            Log.d(TAG, "displayCustomSeekBarColors: ", e.getCause());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_button:
                isEditingEnabled = true;
                saveButton.setText(getString(R.string.save_text));
                editButton.setVisibility(View.INVISIBLE);
                objectSelectorImageView.setVisibility(View.VISIBLE);
                activitySelectorImageView.setVisibility(View.VISIBLE);
                retakeButton.setVisibility(View.VISIBLE);
                zoomView.setVisibility(View.VISIBLE);

                retakeButton.setEnabled(false);
                retakeButton.setClickable(false);
                break;

            case R.id.zoom_in_view:
                if (!videoView.isPlaying()) {
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
                if (!videoView.isPlaying()) {
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
                if (editingValuesAvailable == null){
                    onFailure("Please wait activities are downloading...");
                    return;
                }
                if (editingValuesAvailable) {
                    if (activities_array != null && activities_array.length() > 0) {
                        boundingBoxView.setVisibility(View.GONE);
                        activityNameTextView.setVisibility(View.GONE);

                        activitySelectorImageView.setBackgroundResource(R.drawable.ellipse_white);
                        activitySelectorImageView.setImageResource(R.drawable.ic_edit_activity_black);

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
                if (editingValuesAvailable == null){
                    onFailure("Please wait objects are downloading...");
                    return;
                }
                if (editingValuesAvailable) {
                    if (objects != null && objects.length > 0) {
                        objectSelectorImageView.setBackgroundResource(R.drawable.ellipse_white);
                        objectSelectorImageView.setImageResource(R.drawable.ic_edit_object_black);

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
                    List<ActivityLabel> activities = copyFrame.getActivity();
                    if (!activities.isEmpty()) {
                        Iterator<ActivityLabel> iterator = activities.iterator();
                        while (iterator.hasNext()) {
                            ActivityLabel activityLabel = iterator.next();
                            if (activityLabel.getLabel().equalsIgnoreCase(selectedItem)) {
                                iterator.remove();
                            }
                        }
                    }
                    activities.addAll(tempActivities);
                    try {
                        JSONArray activitySegments = activityEditPresenter.getActivitySegments(copyFrame.getActivity(),
                                frameDuration, videoDuration);
                        displayCustomSeekBarColors(activitySegments);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    retakeButton.setEnabled(false);
                    retakeButton.setClickable(false);

                    retakeButton.setBackgroundResource(R.drawable.ellips);
                    retakeButton.setImageResource(R.drawable.ic_retake);
                } else if (moduleSelected == MODULE_OBJECT_EDITOR) {
                    frameIndex = 0;
                    isVideoPlaying = false;
                    currentPosition = 0;
                    seekBar.setProgress(currentPosition);
                    videoView.seekTo(currentPosition);
                    videoHandler.removeCallbacks(videoProgressUpdater);
                    boundingBoxView.setVisibility(View.GONE);

                    if (tempBoundingBoxes != null) {
                        boundingBoxes.clear();
                        boundingBoxes.putAll(tempBoundingBoxes);
                        tempBoundingBoxes = new LinkedHashMap<>();
                    }
                    objectEditPresenter.updateEditedObject(selectedItem, copyFrame, boundingBoxes,
                            videoPreviewWidth, videoPreviewHeight, videoView.getWidth(),
                            videoView.getHeight(), (int) videoView.getX(), (int) videoView.getY());
                    objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame,
                            videoView.getWidth(), videoView.getHeight());
                    intervalCustomSeekBar.setVisibility(View.GONE);

                    retakeButton.setBackgroundResource(R.drawable.ellips);
                    retakeButton.setImageResource(R.drawable.ic_retake);
                    displayBoundingBox(0);

                    retakeButton.setEnabled(false);
                    retakeButton.setClickable(false);

                    saveButton.setEnabled(false);
                    saveButton.setClickable(false);
                }
                Toast.makeText(this, "Previous edits are cleared", Toast.LENGTH_SHORT).show();
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
                    descriptionTextView.setText(Html.fromHtml(description));
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
                    }
                    Intent intent = new Intent(this, DashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else if (buttonText.contentEquals("SAVE")) {
                    selectedItem = null;
                    editActivityNumberOfInstance = 0;
                    if (moduleSelected == MODULE_OBJECT_EDITOR) {
                        if (videoView.isPlaying()) {
                            changeVideoState();
                        }
                        boundingBoxView.setVisibility(View.GONE);
                        activityNameTextView.setVisibility(View.GONE);
                        objectEditPresenter.updateEditedObject(selectedItem, copyFrame, boundingBoxes,
                                videoPreviewWidth, videoPreviewHeight, videoView.getWidth(),
                                videoView.getHeight(), (int) videoView.getX(), (int) videoView.getY());
                        objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame,
                                videoView.getWidth(), videoView.getHeight());
                    } else if (moduleSelected == MODULE_ACTIVITY_EDITOR) {
                        tempActivities = null;
                        List<ActivityLabel> activities = copyFrame.getActivity();
                        if (activities != null && !activities.isEmpty()) {
                            Collections.sort(activities);
                        }
                        showCustomSeekBar();
                    }
                    displayObjectsBoundingBoxes();

                    frameIndex = 0;
                    isVideoPlaying = false;
                    currentPosition = 0;
                    seekBar.setProgress(currentPosition);
                    videoView.seekTo(currentPosition);
                    videoHandler.removeCallbacks(videoProgressUpdater);
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

                } else {
                    String fileName = FileUtil.appendTimestamp(jsonUrl);
                    if (TextUtils.isEmpty(fileName)) {
                        return;
                    }
                    showProgress("Uploading file please wait...", true);
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Type type = new TypeToken<FrameJSON>() {
                                }.getType();
                                String jsonString = new Gson().toJson(copyFrame, type);
                                File file = FileUtil.writeToJSONFile(Constant.FRAMES_JSON_FILE_NAME, jsonString);
                                editPresenter.uploadSONFile(fileName, file.getAbsolutePath());
                            } catch (IOException e) {
                                Log.d(TAG, "uploading file failure: ", e.getCause());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        hideProgress();
                                        Toast.makeText(VideoEditorActivity.this, "Failed to upload changes",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });
                }
                break;
        }
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
                if (moduleSelected == MODULE_OBJECT_EDITOR) {
                    boundingBoxes = objectEditPresenter.getObjectBoxes(selectedItem, copyFrame,
                            videoView.getWidth(), videoView.getHeight());
                } else {
                    objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame,
                            videoView.getWidth(), videoView.getHeight());
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("You want to discard the changes?")
                .setPositiveButton("YES", (dialog, which) -> {
                    dialog.dismiss();
                    if (leftOrRightOriented) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    }
                    Intent intent = new Intent(this, DashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("NO", (dialog, which) -> {
                    dialog.dismiss();
                });
        AlertDialog alertDialog = builder.create();
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
        alertDialog.show();
        alertDialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getPointerCount() == 1) {
            if (isEditingEnabled) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (moduleSelected == MODULE_ACTIVITY_EDITOR) {
                        int childCount = parentView.getChildCount();
                        if (childCount > 0) {
                            for (int i = 0; i < childCount; i++) {
                                if (parentView.getChildAt(i) instanceof ImageView) {
                                    ImageView view = (ImageView) parentView.getChildAt(i);
                                    boolean insideBox = CoordinateUtil.checkTouchPointsInsideBox(event,
                                            (int) view.getX() - (int) videoView.getX(),
                                            (int) view.getY() - (int) videoView.getY(), view.getMeasuredWidth(),
                                            view.getMeasuredHeight());
                                    if (videoView.getCurrentPosition() == 0 && editActivityNumberOfInstance == 0) {
                                        editActivityNumberOfInstance++;
                                    }
                                    if (insideBox) {
                                        activityTracking = true;
                                        activityTrackingForCaption = true;
                                        editActivityStartFrame = videoView.getCurrentPosition();
                                        retakeButton.setEnabled(true);
                                        retakeButton.setClickable(true);
                                        if (!videoView.isPlaying()) {
                                            changeVideoState();
                                        }
                                    } else {
                                        activityTracking = false;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
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
                                                        boolean insideBox = CoordinateUtil.checkTouchPointsInsideBox(event,
                                                                (int) view.getX() - (int) videoView.getX(),
                                                                (int) view.getY() - (int) videoView.getY(),
                                                                frame.getWidth(), frame.getHeight());
                                                        if (!insideBox) {
                                                            changeVideoState();
                                                        } else {
                                                            if (isVideoPlaying || activityTracking) {
                                                                if (isVideoPlaying) {
                                                                    int currentPosition = videoView.getCurrentPosition();
                                                                    if (currentPosition == 100 && editActivityStartFrame > currentPosition) {
                                                                        editActivityEndFrame = (int) Math.round(frameIndex * frameDuration);
                                                                    } else {
                                                                        editActivityEndFrame = currentPosition;
                                                                    }
                                                                } else {
                                                                    editActivityEndFrame = videoDuration;
                                                                }

                                                                if (editActivityEndFrame - editActivityStartFrame < 150) {
                                                                    return false;
                                                                }

                                                                try {
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
                                                                    copyFrame.getActivity().add(label);

                                                                    JSONArray activitySegments = activityEditPresenter.getActivitySegments(copyFrame.getActivity(),
                                                                            frameDuration, videoDuration);
                                                                    Log.d(TAG, "onTouch: "+ activitySegments.toString());
                                                                    displayCustomSeekBarColors(activitySegments);
                                                                    if (activityTracking) {
                                                                        retakeButton.setBackgroundResource(R.drawable.ellipse_white);
                                                                        retakeButton.setImageResource(R.drawable.ic_retake_black);
                                                                    }
                                                                } catch (JSONException e) {
                                                                    Log.d(TAG, "onTouch: activity editing " + e.getMessage());
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                changeVideoState();
                                            }
                                        }
                                    }
                                }
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
                                objectEditPresenter.updateEditedObject(selectedItem, copyFrame, boundingBoxes,
                                        videoPreviewWidth, videoPreviewHeight, videoView.getWidth(), videoView.getHeight(),
                                        (int) videoView.getX(), (int) videoView.getY());
                                boundingBoxes = objectEditPresenter.getObjectBoxes(selectedItem, copyFrame,
                                        videoView.getWidth(), videoView.getHeight());
                                displayCustomSeekBarColors(activityEditPresenter.getActivitySegments(copyFrame.getActivity(),
                                        frameDuration, videoDuration));
                            } catch (JSONException ex) {
                                Log.d(TAG, "onTouch: " + ex.getMessage());
                            }
                        }
                        initX = 0;
                        initY = 0;
                        isTwoFingerGesture = false;
                    }
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
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
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (moduleSelected != MODULE_ACTIVITY_EDITOR && moduleSelected != MODULE_OBJECT_EDITOR) {
                    changeVideoState();
                }
            }
        } else if (event.getPointerCount() == 2 && moduleSelected == MODULE_OBJECT_EDITOR) {
            if (!videoView.isPlaying() && !objectTracking) {
                objectTracking = true;
                changeVideoState();
            }
            boundingBoxView.setVisibility(View.VISIBLE);
            activityNameTextView.setVisibility(View.VISIBLE);
            isTwoFingerGesture = true;
            updateBoundingBox(event);
        }
        return true;
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
            videoView.pause();
            isVideoPlaying = false;
            videoHandler.removeCallbacks(videoProgressUpdater);
        } else {
            if (currentPosition >= 0 && videoView.getCurrentPosition() != 100) {
                videoView.start();
                isVideoPlaying = true;
                videoHandler.post(videoProgressUpdater);
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        dialogOutsideTouch = false;
        selectedItem = item.getTitle().toString();

        frameIndex = 0;
        videoView.seekTo(0);
        seekBar.setProgress(0);
        currentPosition = 0;
        isVideoPlaying = false;
        boundingBoxView.setVisibility(View.GONE);

        retakeButton.setEnabled(false);
        retakeButton.setClickable(false);

        saveButton.setEnabled(false);
        saveButton.setClickable(false);

        if (moduleSelected == MODULE_OBJECT_EDITOR) {
            isDefaultObject = objectEditPresenter.isDefaultObject(selectedItem, copyFrame);
            activitySelectorImageView.setBackgroundResource(R.drawable.ellips);
            activitySelectorImageView.setImageResource(R.drawable.ic_activity_white);

            parentView.removeAllViews();
            boundingBoxes = new LinkedHashMap<>();

            tempBoundingBoxes = new LinkedHashMap<>();
            tempBoundingBoxes.putAll(objectEditPresenter.getObjectBoxes(selectedItem, copyFrame,
                    videoView.getWidth(), videoView.getHeight()));
        } else if (moduleSelected == MODULE_ACTIVITY_EDITOR) {
            objectSelectorImageView.setBackgroundResource(R.drawable.ellips);
            objectSelectorImageView.setImageResource(R.drawable.ic_object_white);

            try {
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
                        if (activity.getLabel().equalsIgnoreCase(selectedItem)) {
                            tempActivities.add(activity);
                            labelIterator.remove();
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

                JSONArray activitySegments = activityEditPresenter.getActivitySegments(activities,
                        frameDuration, videoDuration);
                displayCustomSeekBarColors(activitySegments);
                displayObjectsBoundingBoxes();
            } catch (JSONException e) {
                Log.d(TAG, "onMenuItemClick: activity selection " + e.getCause());
            }
            objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame,
                    videoView.getWidth(), videoView.getHeight());
        }
        return true;
    }

    @Override
    public void onFileDownload(AwsResponse response) {
        TransferState state = response.getState();
        if (state == TransferState.COMPLETED) {
            if (response.getFileType() == AwsResponse.FILE_TYPE_JSON) {
                editPresenter.downloadJSONFile(videoUrl);
            } else if (response.getFileType() == AwsResponse.FILE_TYPE_VIDEO) {
                videoUrl = response.getVideoUrl();
                AsyncTask.execute(() -> {
                    try {
                        File jsonFile = new File(getFilesDir(), jsonUrl);
                        copyFrame = FileUtil.readFromFile(VideoEditorActivity.this, jsonFile.getAbsolutePath());
                        objectsBoundingBox = objectEditPresenter.generateObjectFrames(copyFrame,
                                videoView.getWidth(), videoView.getHeight());
                        runOnUiThread(() -> {
                            hideProgress();
                            initVideoViewer(videoUrl);
                        });
                    } catch (IOException e) {
                        Log.e(TAG, "onFileDownload: ", e.getCause());
                        runOnUiThread(() -> {
                            hideProgress();
                            onFileDownloadFailure(e.getMessage());
                        });
                    }
                });
            } else if (response.getFileType() == AwsResponse.FILE_TYPE_UPDATED_JSON) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        Toast.makeText(VideoEditorActivity.this, "File uploaded successfully",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideProgress();
                Toast.makeText(VideoEditorActivity.this, errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void populateActivities(JSONArray activitiesList, String[] objects, String description) {
        activities_array = activitiesList;
        this.objects = objects;
        this.description = description;
        if (activities_array != null && activities_array.length() > 0) {
            editingValuesAvailable = true;
        }

        if (objects != null && objects.length > 0) {
            editingValuesAvailable = true;
        }
        if (editingValuesAvailable == null){
            editingValuesAvailable = false;
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
    }
}
