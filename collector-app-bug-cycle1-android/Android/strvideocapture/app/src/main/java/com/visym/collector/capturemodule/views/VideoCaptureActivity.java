package com.visym.collector.capturemodule.views;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.Range;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.gson.Gson;
import com.visym.collector.BaseActivity;
import com.visym.collector.R;
import com.visym.collector.capturemodule.adapters.ActivitiesDisplayAdapter;
import com.visym.collector.capturemodule.interfaces.Listeners;
import com.visym.collector.capturemodule.model.ActivityLabel;
import com.visym.collector.capturemodule.model.FrameActivity;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.model.CollectionModel;
import com.visym.collector.model.Coordinate;
import com.visym.collector.usermodule.view.FrontScreenActivity;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.AspectRatio;
import com.visym.collector.utils.CameraUtil;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.CoordinateUtil;
import com.visym.collector.utils.DateUtil;
import com.visym.collector.utils.FileUtil;
import com.visym.collector.utils.FrameUtil;
import com.visym.collector.utils.Globals;
import com.visym.collector.utils.PermissionUtil;
import com.visym.collector.utils.Size;
import com.visym.collector.utils.SizeMap;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VideoCaptureActivity extends BaseActivity implements View.OnClickListener,
        Listeners.VideoCaptureActivityListener, View.OnTouchListener {

    private static final String TAG = "VideoCaptureActivity";

    private int initX = 0, initY = 0, boxWidth = 0;
    private final int CAMERA_PERMISSION_REQUEST = 1000;
    private String[] cameraPermissions = new String[]{Manifest.permission.CAMERA};

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    @BindView(R.id.texture)
    AutoFitTextureView mTextureView;

    @BindView(R.id.recordingFrameLayout)
    FrameLayout captureButtonView;

    @BindView(R.id.center_box)
    ImageView centerBoxImageView;

    private MediaRecorder mMediaRecorder;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;
    private boolean mIsRecordingVideo = false;
    private boolean buttonClicked = false;

    private String mNextVideoAbsolutePath;

    private AspectRatio aspectRatio = AspectRatio.of(16, 9);

    private Size previewSize;

    private SizeMap videoSizeMap = new SizeMap();
    private Size videoSize;

    @BindView(R.id.activities_recyclerview)
    RecyclerView activitiesRecyclerView;

    @BindView(R.id.cancel_button)
    ImageView cancelButton;

    @BindView(R.id.go_back_btn)
    ImageView goBackBtn;

    @BindView(R.id.flash_button)
    ImageView flashButton;

    @BindView(R.id.timer_textview)
    TextView timerTextView;

    @BindView(R.id.q_button)
    ImageView qButton;

    @BindView(R.id.option_view)
    RelativeLayout optionView;

    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;
    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;
    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private boolean layerInitialised = false;
    private List<Integer> x1 = new ArrayList<>();
    private List<Integer> x2 = new ArrayList<>();
    private List<Integer> y1 = new ArrayList<>();
    private List<Integer> y2 = new ArrayList<>();
    private List<Integer> x3 = new ArrayList<>();
    private List<Integer> y3 = new ArrayList<>();

    private List<Long> timestamp = new ArrayList<>();

    List<Coordinate> coordinates = new ArrayList<>();

    private GestureDetectorCompat mDetector;
    private OrientationEventListener orientationEventListener;
    private int displayWidth;
    private int displayHeight;

    private List<FrameActivity> activities;
    private ActivitiesDisplayAdapter activitiesDisplayAdapter;

    @BindView(R.id.actionText)
    TextView actionText;

    @BindView(R.id.actionOuterEllipse)
    ImageView actionOuterEllipse;

    @BindView(R.id.actionInnerEllipse)
    ImageView actionInnerEllipse;

    @BindView(R.id.camera_switch_icon)
    ImageView cameraSwitchIcon;

    private long startTime_in_milliseconds = 0;
    private Timer timer;
    private String projectId;
    private String collectionId;
    private String cameraId;
    private CollectionModel collection;
    private int deviceOrientation;
    private LinearLayoutManager layoutManager;
    private String orientation, module;
    private int mTextureViewWidth;
    private int mTextureViewHeight;
    private ImageReader imageReader;
    private AppSharedPreference preference;
    private int currentLabelPosition;
    int frameIndex = -1;
    private boolean isConsentScreen = false, isFlashOn = false;
    private AlertDialog infoDialog;
    private CameraCharacteristics characteristics = null;

    private final int CAMERA_FRONT = 0;
    private final int CAMERA_BACK = 1;
    private int SELECTED_CAMERA = CAMERA_BACK;
    private Integer sensorOrientation;
    private int counter;
    private int initialPreviewX, initialPreviewY;

    public VideoCaptureActivity() {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_video_capture_layout);

        ButterKnife.bind(this);
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("module")) {
            module = intent.getStringExtra("module");
            if (!TextUtils.isEmpty(module)) {
                if (module.contentEquals("notraining")) {
                    isConsentScreen = true;
                }
            }
        }
        mTextureView.setOnTouchListener(this);
        captureButtonView.setOnClickListener(this);
        cameraSwitchIcon.setOnClickListener(this);
        flashButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        goBackBtn.setOnClickListener(this);
        qButton.setOnClickListener(this);
        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        preference = AppSharedPreference.getInstance();

        String collectionString = preference.readString(Constant.COLLECTION_KEY);
        if (!TextUtils.isEmpty(collectionString)) {
            collection = new Gson().fromJson(collectionString, CollectionModel.class);
        }

        projectId = preference.readString(Constant.PROJECT_ID_KEY);
        collectionId = preference.readString(Constant.COLLECTION_ID_KEY);

        initLayout();

        orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if ((orientation >= 0 && orientation < 45) || (orientation >= 315 && orientation < 360)) {
                    int rotation = Surface.ROTATION_0;
                    if (rotation != deviceOrientation) {
                        deviceOrientation = rotation;
                        onOrientationChange();
                    }
                } else if (orientation >= 45 && orientation < 180) {
                    int rotation = Surface.ROTATION_90;
                    if (rotation != deviceOrientation) {
                        deviceOrientation = rotation;
                        onOrientationChange();
                    }
                } else if (orientation >= 180 && orientation < 315) {
                    int rotation = Surface.ROTATION_270;
                    if (rotation != deviceOrientation) {
                        deviceOrientation = rotation;
                        onOrientationChange();
                    }
                }
            }
        };
    }

    private void onOrientationChange() {
        FrameLayout.LayoutParams recordingParams =
                (FrameLayout.LayoutParams) captureButtonView.getLayoutParams();

//        int x = (int) mTextureView.getX();
//        int y = (int) mTextureView.getY();
        switch (deviceOrientation) {
            case Surface.ROTATION_0:
                float rotation = optionView.getRotation();
                if (rotation != 0) {
                    optionView.setRotation(0);
                    captureButtonView.setRotation(0);
                    cameraSwitchIcon.setRotation(0);
                    optionView.getLayoutParams().width = getDisplaySize().x;
                    optionView.getLayoutParams().height = getDisplaySize().y;
                    recordingParams.gravity = Gravity.BOTTOM | Gravity.CENTER;
                    recordingParams.bottomMargin = activitiesRecyclerView.getMeasuredHeight();
                }
                optionView.setPadding(initialPreviewX, initialPreviewY + (int) getResources().getDimension(R.dimen.semiSmallMargin), initialPreviewX, 0);
                break;

            case Surface.ROTATION_90:
                optionView.setRotation(-90);
                captureButtonView.setRotation(-90);
                optionView.getLayoutParams().width = getDisplaySize().y;
                optionView.getLayoutParams().height = getDisplaySize().x;
                recordingParams.gravity = Gravity.TOP | Gravity.CENTER;
                recordingParams.topMargin = initialPreviewY;
                optionView.setPadding(initialPreviewY, (int) getResources().getDimension(R.dimen.semiSmallMargin), initialPreviewY, 0);
                break;

            case Surface.ROTATION_270:
                optionView.setRotation(90);
                captureButtonView.setRotation(90);
                optionView.getLayoutParams().width = getDisplaySize().y;
                optionView.getLayoutParams().height = getDisplaySize().x;
                recordingParams.bottomMargin = initialPreviewY;

                optionView.setPadding(initialPreviewY, (int) getResources().getDimension(R.dimen.semiSmallMargin), initialPreviewY, 0);
                break;
        }
        updateDialogOrientation();
        optionView.requestLayout();
    }

    private void initLayout() {
        if (collection == null) {
            return;
        }
        String activityString = collection.getActivityShortNames();
        this.activities = new ArrayList<>();
        if (!TextUtils.isEmpty(activityString)) {

            String[] strings = activityString.split(",");
            for (String string : strings) {
                this.activities.add(new FrameActivity(string));
            }
        }

        if (activitiesDisplayAdapter == null && !this.activities.isEmpty()) {
            activitiesRecyclerView.setVisibility(View.VISIBLE);
            activitiesDisplayAdapter = new ActivitiesDisplayAdapter(this.activities, this);
            layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            activitiesRecyclerView.setLayoutManager(layoutManager);
            final SnapHelper snapHelper = new LinearSnapHelper();
            snapHelper.attachToRecyclerView(activitiesRecyclerView);
            activitiesRecyclerView.setAdapter(activitiesDisplayAdapter);

            activitiesRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    FrameLayout.LayoutParams recordingParams =
                            (FrameLayout.LayoutParams) captureButtonView.getLayoutParams();
                    recordingParams.bottomMargin = activitiesRecyclerView.getMeasuredHeight();
                    activitiesRecyclerView.requestLayout();
                    cameraSwitchIcon.requestLayout();
                }
            });
        }

        boolean isFlashAvailable = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!isFlashAvailable) {
            flashButton.setEnabled(false);
        } else {
            flashButton.setEnabled(true);
        }
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
            mCameraOpenCloseLock.release();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraOpenCloseLock.release();
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            camera.close();
            mCameraDevice = null;
        }
    };
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        boolean permissionsGranted = PermissionUtil
                .checkRequiredPermissionsGranted(this, cameraPermissions);
        if (!permissionsGranted) {
            requestCameraPermissions();
        }
    }

    private void requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, cameraPermissions,
                CAMERA_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(this, "Some permissions are not granted",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {
        Log.d("JEBYRNE", "VideoCaptureActivity.onClick");
        switch (v.getId()) {
            case R.id.camera_switch_icon:
                if (mIsRecordingVideo) {
                    return;
                }
                switchCamera();
                break;

            case R.id.recordingFrameLayout:
                if (mIsRecordingVideo) {
                    stopRecordingVideo();
                } else {
                    if (!buttonClicked) {
                        startRecordingVideo();
                    }
                }
                break;

            case R.id.cancel_button:
                displayDialog("Exit this collection?", true);
                break;

            case R.id.flash_button:
                boolean isFlashAvailable = getApplicationContext().getPackageManager()
                        .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
                if (isFlashAvailable) {
                    flashButton.setEnabled(true);
                    switchFlashLight();
                } else {
                    flashButton.setEnabled(false);
                    Toast.makeText(this, "Flash is not available", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.q_button:
                if (mIsRecordingVideo) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Collection Description");
                View view = getLayoutInflater().inflate(R.layout.layout_video_help_icon, null);
                builder.setView(view);

                TextView descriptionTextView = view.findViewById(R.id.descriptionTextview);
                if (preference.readString(Constant.COLLECTION_DESCRIPTION) != null) {
                    String replacedString = preference.readString(Constant.COLLECTION_DESCRIPTION).replace("\n", "<br>");
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
                        infoDialog = null;
                    }
                });
                infoDialog = builder.create();
                showDialogAndHideSystemBar(infoDialog);
                updateDialogOrientation();
                break;
            case R.id.go_back_btn:
                // go back to training video or consent start screen
                if (isConsentScreen) {
                    Intent goToConsentScreen = new Intent(this, ConsentConfirmationActivity.class);
                    startActivity(goToConsentScreen);
                } else if (preference.readBoolean(Constant.IS_PLAY_TRAINING)) {
                    Intent goToTrainingVideoScreen = new Intent(this, VideoPlayerActivity.class);
                    goToTrainingVideoScreen.putExtra("module", "training");
                    startActivity(goToTrainingVideoScreen);
                }
                finish();
                break;
        }
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();// clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
        }
    }

    private void switchCamera() {
        SELECTED_CAMERA = SELECTED_CAMERA == CAMERA_FRONT ? CAMERA_BACK : CAMERA_FRONT;
        closeCamera();
        reopenCamera();
    }

    public void reopenCamera() {
        mTextureView.getLayoutParams().width = displayWidth;
        mTextureView.getLayoutParams().height = displayHeight;
        mTextureView.requestLayout();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    private void updateDialogOrientation() {
        if (infoDialog != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            Window window = infoDialog.getWindow();
            if (window != null) {
                layoutParams.copyFrom(window.getAttributes());
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

                switch (deviceOrientation) {
                    case Surface.ROTATION_0:
                        layoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                        break;

                    case Surface.ROTATION_90:
                        layoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                        break;

                    case Surface.ROTATION_270:
                        layoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                        break;
                }
                window.setAttributes(layoutParams);
            }
        }
    }

    private void startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == previewSize) {
            closeCamera();
            Toast.makeText(this, "Failed to record a video. Please try again.",
                    Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, VideoCaptureActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        try {
            Log.d("JEBYRNE", "VideoCaptureActivity.startRecordingVideo: entered");

            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (manager == null) {
                return;
            }
            buttonClicked = true;
            File filesDir = getExternalFilesDir(Constant.VIDEO_DIRECTORY_NAME
                    + File.separator + "capture");
            if (filesDir.isDirectory() && filesDir.exists()) {
                FileUtils.deleteDirectory(filesDir);
            }

            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // Set UP Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            // Set UP Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            Log.d("JEBYRNE", "VideoCaptureActivity.startRecordingVideo: before createCaptureSession");
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.d("JEBYRNE", "VideoCaptureActivity.startRecordingVideo: createCaptureSession");

                    orientation = CameraUtil.getOrientationKey(deviceOrientation);
                    mPreviewSession = cameraCaptureSession;
                    orientation = CameraUtil.getOrientationKey(deviceOrientation);

                    updatePreview();
                    Log.d("JEBYRNE", "VideoCaptureActivity.startRecordingVideo: createCaptureSession - done updatePreview");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Start recording
                            if (orientationEventListener != null) {
                                orientationEventListener.disable();
                            }
                            AppSharedPreference instance = AppSharedPreference.getInstance();
                            instance.storeValue(AppSharedPreference.ORIENTATION_KEY, orientation);
                            instance.storeValue(Constant.COLLECTED_DATE_KEY, DateUtil.getDateInUTC(new Date()));
                            mIsRecordingVideo = true;
                            timer = new Timer();
                            startTimer();
                            Log.d("JEBYRNE", "VideoCaptureActivity.startRecordingVideo: before mMediaRecorder.start()");

                            mMediaRecorder.start();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(VideoCaptureActivity.this, "Camera Configuration Failed", Toast.LENGTH_LONG).show();

                    // JEBYRNE (03OCT21): Debugging the front camera crash on some older devices
                    // - If we default to SELECTED_CAMERA=1, so that the front camera is initialized by constructor this works
                    // - The problem is that we default to SELECTED_CAMERA=0, then calling switchCamera results in some configuration that was set for the high-res back camera, which cannot be used for recording in the lower res front cam
                    // - Front camera does work for consent recording which defaults to the front camera
                    // - There should be a nuclear option here for switching the intent and recreating everything using the new camera, rather than dynamically reinitializing
                    // - removing recorderSurface from surfaces results in passing onConfigured, but obviously the video does not get recorded
                    // - Can get this to at least not crash if we handle front camera special in openCamera(), but the preview is squashed until we call startRecordingVideo()
                    // - It seems that the culprit is configureTransform, and the difference between previewSize, videoSize and displaySize.
                    // - If we force MediaRecorder to use a recommended resolution in the profile, then we can get everything to record, and the preview video is right, but the boxes are off relative to the display resolution
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "startRecordingVideo: ", e);
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startTimer() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (counter == 0) {
                            actionOuterEllipse.setImageDrawable(getResources().getDrawable(R.drawable.ellipse_outer_red));
                            actionInnerEllipse.setImageDrawable(getResources().getDrawable(R.drawable.ellipse_red));
                            actionText.setText(getResources().getString(R.string.stop));
                            actionText.setTextColor(ContextCompat.getColor(VideoCaptureActivity.this, R.color.textLightColor));

                        }
                        counter++;
                        timerTextView.setText(String.format("%s:%s", "00", counter < 10 ? "0" + counter : "" + counter));  // JEBYRNE: this will not work for counters > 60 seconds (yuck..)
                    }
                });
            }
        };
        if (timer != null) {
            timer.scheduleAtFixedRate(task, 1000, 1000);
        }
    }

    private void addPoints() {
        int width = centerBoxImageView.getLayoutParams().width;
        int height = centerBoxImageView.getLayoutParams().height;

        int previewWidth = mTextureView.getWidth();
        int previewHeight = mTextureView.getHeight();

        int x1 = (previewWidth - width) / 2;
        int y12 = (previewHeight - height) / 2;
        int x23 = previewWidth - (x1);
        int y3 = previewHeight - y12;

        addCoordinatesPoints(x1, y12, x23, y12, x23, y3);
    }

    private void stopRecordingVideo() {
        try {
            if (timer != null) {
                timer.cancel();
            }
            captureButtonView.setVisibility(View.GONE);

            mIsRecordingVideo = false;
            closePreviewSession();

            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

            // Stop recording
            if (mMediaRecorder != null) {
                // JEBYRNE: here is where the video object is flushed to finish the recording
                Log.d("JEBYRNE", "VideoCaptureActivity.stopRecordingVideo");
                mMediaRecorder.stop();
                mMediaRecorder.reset();  // JEBYRNE
                mMediaRecorder.release();
            }

            updateLabel();
            processAndCreateFrameData();
        } catch (Exception e) {
            Log.d(TAG, "stopRecordingVideo: " + e.getCause());
            Toast.makeText(this, "Recording failed. Please try again", Toast.LENGTH_LONG).show();
            recreate();
        }
    }

    private void updateLabel() {
        if (activitiesDisplayAdapter != null) {
            int position = activitiesDisplayAdapter.getSelectedPosition();
            if (position != -1 && activityLabels != null && !activityLabels.isEmpty()) {
                ActivityLabel activityLabel = activityLabels.get(activityLabels.size() - 1);
                int endFrame = activityLabel.getEndFrame();
                if (endFrame == 0) {
                    if (frameIndex > activityLabel.getStartFrame()) {
                        activityLabel.setEndFrame(frameIndex);
                    } else if (frameIndex == activityLabel.getStartFrame()) {
                        frameIndex++;
                        Log.d("JEBYRNE", String.format("VideoCaptureActivity.updateLabel: frameIndex=%d", frameIndex));
                        activityLabel.setEndFrame(frameIndex);
                    }
                }
            }
        }
    }

    private Coordinate updateVideoFrameTime() {
        int videoWidth = videoSize.getHeight();
        int videoHeight = videoSize.getWidth();

        int previewWidth = mTextureView.getWidth();
        int previewHeight = mTextureView.getHeight();

        int x = (videoWidth * x1.get(x1.size() - 1)) / previewWidth;
        int y = (videoHeight * y1.get(y1.size() - 1)) / previewHeight;

        int w = x2.get(x2.size() - 1) - x1.get(x1.size() - 1);
        int h = y3.get(y3.size() - 1) - y2.get(y2.size() - 1);

        int boxWidth = (videoWidth * w) / previewWidth;
        int boxHeight = (videoHeight * h) / previewHeight;

        Coordinate coordinate = new Coordinate();
        if (orientation.contentEquals(Constant.LANDSCAPE_RIGHT_KEY)
                || orientation.contentEquals(Constant.LANDSCAPE_LEFT_KEY)) {
            coordinate.setX(y);
            coordinate.setY(x);
            coordinate.setWidth(boxHeight);
            coordinate.setHeight(boxWidth);
        } else {
            coordinate.setX(x);
            coordinate.setY(y);
            coordinate.setWidth(boxWidth);
            coordinate.setHeight(boxHeight);
        }
        return coordinate;
    }

    private void processAndCreateFrameData() {
        // JEBYRNE: this will generate the annotation JSON
        // - boxes in member attribute "coordinates" which is a list of com/visym/collector/model/Coordinate.java
        // - activities in member attribute activityLabels which is a list of activityLabel com/visym/collector/capturemodule/model/ActivityLabel.java
        // - This assumes that each index is a frame
        // - Preview happens after this is created by creating an intent, which read Constant.FRAMES_JSON_FILE_NAME from
        Log.d("JEBYRNE", String.format("processAndCreateFrameData: frameIndex=%d", frameIndex));  // TESTING
        Globals.showLoadingWithFlags(this);

        Double annotatedFrameDuration = timestamp.stream().mapToDouble(val -> val).average().orElse(0.0);
        Log.d("JEBYRNE", String.format("processAndCreateFrameData: average collected frameDuration=%f", annotatedFrameDuration));

        FrameUtil computer = new FrameUtil(handler, mNextVideoAbsolutePath, coordinates, activityLabels, annotatedFrameDuration);
        computer.setDeviceOrientation(deviceOrientation);
        computer.setSensorOrientation(sensorOrientation);
        new Thread(computer).start();
    }

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
            buttonClicked = false;
            if (data.containsKey(FrameUtil.COMPUTATION_ERROR_KEY)) {
                FileUtil.deleteFile(mNextVideoAbsolutePath);
                String string = data.getString(FrameUtil.COMPUTATION_ERROR_MESSAGE_KEY);
                Toast.makeText(VideoCaptureActivity.this, string, Toast.LENGTH_LONG).show();
                onPause();
                onResume();
            } else if (data.containsKey(FrameUtil.COMPUTATION_SUCCESS_KEY)) {
                Intent intent = new Intent(VideoCaptureActivity.this, VideoPreviewActivity.class);
                intent.putExtra(Constant.VIDEO_FILE_PATH_KEY, data.getString(FrameUtil.COMPUTATION_FILE_PATH_KEY));
                intent.putExtra(Constant.PROJECT_ID_KEY, projectId);
                intent.putExtra(Constant.COLLECTION_ID_KEY, collectionId);
                startActivity(intent);  // JEBYRNE: this is what happens after the video recording is complete, before upload
                finish();
            }
        }
    };

    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray REAR_ORIENTATIONS = new SparseIntArray();

    static {
        REAR_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        REAR_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        REAR_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        REAR_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private void setUpMediaRecorder() throws IOException, JSONException {
        mMediaRecorder = new MediaRecorder();
        CamcorderProfile profile = CameraUtil.getCameraProfile(Integer.parseInt(cameraId));

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        String videoFilePath = FileUtil.getVideoFilePath(this, "capture");  // JEBYRNE: generates video ID as filename
        if (videoFilePath != null) {
            mNextVideoAbsolutePath = videoFilePath;
            mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);

            mMediaRecorder.setOutputFormat(profile.fileFormat);
            mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        stopRecordingVideo();
                    }
                    else {
                        Log.d("JEBYRNE", String.format("VideoCaptureActivity.onInfo: %d", what));
                    }
                }
            });
            mMediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());  // JEBYRNE: this line is suspicious for lower-res front camera, set in openCamera() but the recorder may not support it
            //mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);  // JEBYRNE: if we use this line instead, we can get recording to happen, but the boxes are now off for both front and back
            mMediaRecorder.setVideoEncoder(profile.videoCodec);
            mMediaRecorder.setMaxDuration(Constant.COLLECTION_VIDEO_LENGTH);
            mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
            mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
            Log.d("JEBYRNE", String.format("setUpMediaRecorder: profile.videoFrameRate=%d", profile.videoFrameRate));  // 30Hz, but does not match ffmpeg result
            if (sensorOrientation != null) {
                switch (sensorOrientation) {
                    case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                        if (SELECTED_CAMERA == CAMERA_FRONT) {
                            mMediaRecorder.setOrientationHint(REAR_ORIENTATIONS.get(this.deviceOrientation));
                        } else {
                            mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(this.deviceOrientation));
                        }
                        break;
                    case SENSOR_ORIENTATION_INVERSE_DEGREES:
                        mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(this.deviceOrientation));
                        break;
                }
            }

            try {
                mMediaRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                FileUtil.deleteFile(mNextVideoAbsolutePath);
            }
        }
        else {
            displayDialog("Recording error - Insufficient space or permissions", false);  // JEBYRNE: something went wrong trying to create a new file for recording
            logout();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == previewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextureView.setTransform(matrix);
            }
        });
    }


    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == previewSize) {
            return;
        }
        try {
            closePreviewSession();

            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
            mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mPreviewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.d(TAG, "onConfigureFailed: ");
                        }
                    }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }

        setUpCaptureRequestBuilder(mPreviewBuilder);
        //HandlerThread thread = new HandlerThread("CameraPreview");
        //thread.start();

        try {
            if (mPreviewSession != null) {
                mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), mCaptureCallback,
                        mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }
        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                       @NonNull android.hardware.camera2.CaptureFailure result) {
            Log.d("JEBYRNE", "VideoCaptureActivity.onCaptureFailed!");
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            if (mIsRecordingVideo) {
                // JEBYRNE: this is where the frameIndex is updated for the boxes and times
                // - This will be incremented when each frame is captured
                // - However, the video recorder is not synchronized here

                frameIndex++;
                if (startTime_in_milliseconds != 0) {
                    timestamp.add(System.currentTimeMillis() - startTime_in_milliseconds);
                }
                Log.d("JEBYRNE", String.format("VideoCaptureActivity.onCaptureCompleted: frameIndex=%d, msecs=%d, getFrameNumber=%d", frameIndex, System.currentTimeMillis() - startTime_in_milliseconds, result.getFrameNumber()));
                startTime_in_milliseconds = (long) System.currentTimeMillis();
                addPoints();
                addLabel();
            }
        }
    };

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        Range<Integer> range = getRange();
        if (range != null) {
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range);
        }
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBar();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        if (orientationEventListener != null) {
            orientationEventListener.enable();
            onOrientationChange();
        }
    }

    private void openCamera(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) return;

        try {
            Integer facing;
            for (final String cameraId : manager.getCameraIdList()) {
                characteristics = manager.getCameraCharacteristics(cameraId);
                facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (SELECTED_CAMERA == CAMERA_BACK && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    this.cameraId = cameraId;
                    break;
                } else if (SELECTED_CAMERA == CAMERA_FRONT && facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    this.cameraId = cameraId;
                    break;
                }
            }
            if (characteristics == null) {
                return;
            }
            StreamConfigurationMap map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map == null) {
                return;
            }

            int displayRotation = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                displayRotation = getDisplay().getRotation();  // requires API version 30
            }
            else {
                displayRotation = getWindowManager().getDefaultDisplay().getRotation();  // JEBYRNE
            }
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            boolean swappedDimensions = false;
            switch (displayRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        swappedDimensions = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    if (sensorOrientation == 0 || sensorOrientation == 180) {
                        swappedDimensions = true;
                    }
                    break;
                default:
                    Log.e(TAG, "Display rotation is invalid: " + displayRotation);
            }

            Point displaySize = getDisplaySize();
            int rotatedPreviewWidth = width;
            int rotatedPreviewHeight = height;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            if (swappedDimensions) {
                rotatedPreviewWidth = height;
                rotatedPreviewHeight = width;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH;
            }

            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT;
            }

            initVideoSizes(map.getOutputSizes(MediaRecorder.class));

            List<Size> videoAspectSizes = videoSizeMap.sizes(aspectRatio);
            if (videoAspectSizes != null && videoAspectSizes.size() > 0) {  // JEBYRNE: only valid for default camera from constructor
                videoSize = CameraUtil.chooseVideoSize(videoAspectSizes);
            } else {
                CamcorderProfile profile = CameraUtil.getCameraProfile(0);  // JEBYRNE: FRONT camera
                videoSize = new Size(profile.videoFrameWidth, profile.videoFrameHeight);
            }

            previewSize = CameraUtil.chooseOptimalPreviewSize(map.getOutputSizes(SurfaceTexture.class),
                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, videoSize);

            // JEBYRNE: this does not change from rotation=0 when changing the orientation of the device.  What is going on here?
            int rotation = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                rotation = getDisplay().getRotation();  // JEBYRNE: API version 29:  https://developer.android.com/reference/android/os/Build.VERSION_CODES#R
            }
            else {
                rotation = getWindowManager().getDefaultDisplay().getRotation();
            }
            Log.d("JEBYRNE", String.format("VideoCaptureActivity.openCamera: cameraId=%s, rotation=%d, portrait=%d", cameraId, rotation, Configuration.ORIENTATION_PORTRAIT));
            if (rotation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(this.previewSize.getWidth(), this.previewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(this.previewSize.getHeight(), this.previewSize.getWidth());
            }

            initLayers();
            configureTransform(width, height);
            if (cameraId != null) {
                manager.openCamera(cameraId, mStateCallback, mBackgroundHandler);
            } else {
                displayDialog("Camera error", false);
            }
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Toast.makeText(this, "Sorry!... This feature is not supported in this device",
                    Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Log.d(TAG, "openCamera: " + e.getCause());
        }
    }

    private void displayDialog(String message, boolean isCancellable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setCancelable(isCancellable)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (timer != null) {
                        timer.cancel();
                    }
                    if (!TextUtils.isEmpty(mNextVideoAbsolutePath)) {
                        FileUtil.deleteFile(mNextVideoAbsolutePath);
                    }
                    if (message.contains("Exit")) {
                        Intent goToCollectionList = new Intent(VideoCaptureActivity.this, DashboardActivity.class);
                        goToCollectionList.putExtra("redirectTo", "capture");

                        // JEBYRNE: without these lines, the following happens:
                        // - Open a collection and get to the Video Capture UI
                        // - Hit the "X" button to "Exit this Collection?"
                        // - Return to the capture screen
                        // - Select a new collection
                        // - The new collection details are the last collection and not the one selected.
                        // - This is because CollectionDetailsActivity.onCreate never fires
                        // - Go back, then select the same collection again, and everything is correct.
                        goToCollectionList.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(goToCollectionList);
                        finish();  // JEBYRNE: what does this do?  Repeating this from ConsentConfirmationActivity displayCloseButtonDialog
                    } else {
                        closeCamera();
                    }
                    finish();
                });
        showDialogAndHideSystemBar(builder.create());
    }

    private List<ActivityLabel> activityLabels = new ArrayList<>();

    private void addLabel() {
        // JEBYRNE: this function does not trigger from onClick or onTouch.
        // - It runs on every iteration from onCaptureCompleted, which also updates frameIndex
        // - Check here to see if any of the buttons are active, and if so, record the current frameIndex into the activityLabels array

        if (activitiesDisplayAdapter != null) {
            int position = activitiesDisplayAdapter.getSelectedPosition();
            if (position != -1) {
                String selectedLabel = activitiesDisplayAdapter.getSelectedActivityLabel(position);
                if (activityLabels.isEmpty()) {
                    ActivityLabel activityLabel = new ActivityLabel();
                    activityLabel.setLabel(selectedLabel);
                    activityLabel.setStartFrame(frameIndex);
                    activityLabels.add(activityLabel);
                } else {
                    ActivityLabel previousLabel = activityLabels.get(activityLabels.size() - 1);
                    if (currentLabelPosition != position) {
                        int endFrame = previousLabel.getEndFrame();
                        if (endFrame == 0) {
                            previousLabel.setEndFrame(frameIndex - 1);
                            Log.d("JEBYRNE", String.format("VideoCaptureActivity.addLabel: %s, start=%d, end=%d", previousLabel.getLabel(), previousLabel.getStartFrame(), previousLabel.getEndFrame()));
                        }

                        ActivityLabel activityLabel = new ActivityLabel();
                        activityLabel.setLabel(selectedLabel);
                        activityLabel.setStartFrame(frameIndex);
                        activityLabels.add(activityLabel);
                    }
                }
            } else {
                if (!activityLabels.isEmpty()) {
                    ActivityLabel previousLabel = activityLabels.get(activityLabels.size() - 1);
                    int endFrame = previousLabel.getEndFrame();
                    if (endFrame == 0) {
                        previousLabel.setEndFrame(frameIndex - 1);
                        Log.d("JEBYRNE", String.format("VideoCaptureActivity.addLabel: %s, start=%d, end=%d", previousLabel.getLabel(), previousLabel.getStartFrame(), previousLabel.getEndFrame()));
                    }
                }
            }
            currentLabelPosition = position;
        }
    }

    private void initVideoSizes(android.util.Size[] outputSizes) {
        for (android.util.Size size : outputSizes) {
            Size s = new Size(size.getWidth(), size.getHeight());
            videoSizeMap.add(s);
        }
    }

    private void initLayers() {
        mTextureView.post(new Runnable() {
            @Override
            public void run() {
                int width = mTextureView.getMeasuredWidth();
                int height = mTextureView.getMeasuredHeight();

                int maxHeight = height / 3;

                initialPreviewX = (int) mTextureView.getX();
                initialPreviewY = (int) mTextureView.getY();

                optionView.setPadding(initialPreviewX, initialPreviewY + (int) getResources().getDimension(R.dimen.semiSmallMargin), initialPreviewX, 0);
                onOrientationChange();

                centerBoxImageView.getLayoutParams().width = width;
                centerBoxImageView.getLayoutParams().height = height - (maxHeight * 2);
                centerBoxImageView.requestLayout();

                displayWidth = width;
                displayHeight = height;
            }
        });
    }

    public void switchFlashLight() {

        try {
            if (!isFlashOn) {
                flashButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_flash_on_icon));
                mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, null);
                isFlashOn = true;
            } else {
                flashButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_flash_off_icon));
                isFlashOn = false;
                mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void addCoordinatesPoints(int x1, int y1, int x2, int y2, int x3, int y3) {
        int[] coordinates = transformCoordinates(x1, y1, x2, y2, x3, y3);
        this.x1.add(coordinates[0] < 0 ? 0 : coordinates[0]);
        this.y1.add(coordinates[1] < 0 ? 0 : coordinates[1]);
        this.x2.add(coordinates[2] < 0 ? 0 : coordinates[2]);
        this.y2.add(coordinates[3] < 0 ? 0 : coordinates[3]);
        this.x3.add(coordinates[4] < 0 ? 0 : coordinates[4]);
        this.y3.add(coordinates[5] < 0 ? 0 : coordinates[5]);
        this.coordinates.add(updateVideoFrameTime());
    }

    private int[] transformCoordinates(int x1, int y1, int x2, int y2, int x3, int y3) {
        int[] values = new int[6];

        values[0] = x1;
        values[1] = y1;
        values[2] = x2;
        values[3] = y2;
        values[4] = x3;
        values[5] = y3;

        return values;
    }

    @Override
    public void onPause() {
        hideSystemBar();
        closeCamera();
        stopBackgroundThread();
        if (orientationEventListener != null) {
            orientationEventListener.disable();
        }
        super.onPause();
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
            if (timer != null) {
                timer.cancel();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            //throw new RuntimeException("Interrupted while trying to lock camera closing.");
            // JEBYRNE: is this ok to ignore?  maybe we just tried to close this twice?
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean doublePinch = false;

    private Point getDisplaySize() {
        Display display = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            display = getDisplay();
        }
        else {
            display = getWindowManager().getDefaultDisplay();
        }
        Point point = new Point();
        display.getRealSize(point);
        return point;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d("JEBYRNE", String.format("VideoCaptureActivity.onTouch"));

        switch (v.getId()) {
            case R.id.texture:
                try {
                    int action = event.getActionMasked();
                    this.mDetector.onTouchEvent(event);
                    if (action == MotionEvent.ACTION_DOWN) {
                        initX = (int) Math.abs(event.getX());
                        initY = (int) Math.abs(event.getY());
                        return true;
                    }

                    if (event.getPointerCount() == 1 && action == MotionEvent.ACTION_MOVE) {
                        if (doublePinch) return true;
                        int x = (int) Math.abs(event.getX());
                        int y = (int) Math.abs(event.getY());
                        int corner = CoordinateUtil.getCorner(initX, initY, mTextureView.getMeasuredWidth(),
                                mTextureView.getMeasuredHeight());
                        if (initX != x) {
                            updateBoxWidth(corner, initX, x);
                        }
                        if (initY != y) {
                            updateBoxHeight(corner, initY, y);
                        }

                        initX = x;
                        initY = y;
                    } else if (event.getPointerCount() == 2 && action == MotionEvent.ACTION_MOVE) {
                        int pointer1Index = event.findPointerIndex(0);
                        int pointer2Index = event.findPointerIndex(1);

                        double angle = CoordinateUtil.getAngle(event.getX(pointer1Index), event.getY(pointer1Index),
                                event.getX(pointer2Index), event.getY(pointer2Index));

                        //Diagonal zooming
                        if ((angle > 15 && angle < 70) || (angle > 110 && angle < 160) || (angle > 200 && angle < 250)
                                || (angle > 290 && angle < 350)) {
                            int x = (int) Math.abs(event.getX());

                            doublePinch = true;
                            updateBoxSize(event, true);
                            initX = x;
                        } else if (Math.abs(event.getX(pointer1Index) - event.getX(pointer2Index)) > Math.abs(event.getY(pointer1Index)
                                - event.getY(pointer2Index))) {
                            //Horizontal zooming
                            // Ignore Y diagonalCoordinates and consider X diagonalCoordinates
                            int x = (int) Math.abs(event.getX());
                            if (initX == x) {
                                return true;
                            }

                            doublePinch = true;
                            updateBoxSize(event, true);
                            initX = x;

                        } else if (Math.abs(event.getY(pointer1Index) - event.getY(pointer2Index))
                                > Math.abs(event.getX(pointer1Index) - event.getX(pointer2Index))) {
                            //Vertical zooming
                            float pointerValue = Math.abs(event.getY(pointer1Index) - event.getY(pointer2Index));
                            int x = (int) Math.abs(event.getY());
                            if (initX == x) {
                                return true;
                            }

                            doublePinch = true;
                            updateBoxSize(event, false);
                            initX = x;
                        }
                    }

                    if (action == MotionEvent.ACTION_UP) {
                        initX = 0;
                        initY = 0;
                        boxWidth = 0;
                        doublePinch = false;
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
        }
        return false;
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d("JEBYRNE", "VideoCaptureActivity.onSingleTapConfirmed");
            mTextureViewWidth = mTextureView.getWidth();
            mTextureViewHeight = mTextureView.getHeight();

            int x = (int) Math.abs(e.getX());
            int y = (int) Math.abs(e.getY());
            int corner = CoordinateUtil.getCorner(x, y, mTextureViewWidth, mTextureViewHeight);
            int width = centerBoxImageView.getLayoutParams().width;
            int height = centerBoxImageView.getLayoutParams().height;
            boolean touchesBorder = CoordinateUtil.checkPointsTouchesBorder(x, y, corner, width,
                    height, mTextureViewWidth, mTextureViewHeight);
            if (!touchesBorder) {
                if (activitiesDisplayAdapter != null) {
                    activitiesDisplayAdapter.getUpdatedGesturePosition("single");
                    int position = activitiesDisplayAdapter.getSelectedPosition();
                    if (position != -1) {
                        onActivityClick(position);
                    }
                }
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d("JEBYRNE", "VideoCaptureActivity.onDoubleTap");

            if (activitiesDisplayAdapter != null) {
                activitiesDisplayAdapter.getUpdatedGesturePosition("double");
                int position = activitiesDisplayAdapter.getSelectedPosition();
                if (position != -1) {
                    onActivityClick(position);
                }
                int previousPosition = activitiesDisplayAdapter.getPreviousPosition();
                if (previousPosition == -1 && position > previousPosition) {
                    String selectedLabel = activitiesDisplayAdapter.getSelectedActivityLabel(position);
                    ActivityLabel activityLabel = new ActivityLabel();
                    activityLabel.setLabel(selectedLabel);
                    activityLabel.setStartFrame(frameIndex);
                    activityLabels.add(activityLabel);
                }
            }
            return true;
        }

    }

    private int previousWidth;
    private int previousHeight;

    private void updateBoxSize(MotionEvent event, boolean diagonalPinch) {
        float a1 = event.getX(0);
        float b1 = event.getY(0);

        float a2 = event.getX(1);
        float b2 = event.getY(1);

        int width = a1 > a2 ? (int) (a1 - a2) : (int) (a2 - a1);
        int height = b1 > b2 ? (int) (b1 - b2) : (int) (b2 - b1);

        if (boxWidth != 0) {
            int xDiff = Math.abs(previousWidth - width);
            int yDiff = Math.abs(previousHeight - height);
            if (diagonalPinch) {
                changeTextureViewSize(!(previousWidth > width), xDiff, yDiff);
            } else {
                changeTextureViewSize(!(previousHeight > height), xDiff, yDiff);
            }
        } else {
            centerBoxImageView.getLayoutParams().width = width;
            centerBoxImageView.getLayoutParams().height = height;
            centerBoxImageView.requestLayout();
        }
        previousWidth = width;
        previousHeight = height;
        boxWidth++;
        centerBoxImageView.requestLayout();

    }

    private void updateBoxHeight(int corner, int initY, int y) {
        int height = centerBoxImageView.getLayoutParams().height;
        int j;
        switch (corner) {
            case 1:
            case 2:
                if (y > initY) {
                    j = y - initY;
                    height -= j * 2;
                    if (height < 100) {
                        return;
                    }
                } else {
                    j = initY - y;
                    height += j * 2;
                    if (height > displayHeight) {
                        return;
                    }
                }
                break;

            case 3:
            case 4:
                if (y > initY) {
                    j = y - initY;
                    height += j * 2;
                    if (height > displayHeight) {
                        return;
                    }
                } else {
                    j = initY - y;
                    height -= j * 2;
                    if (height < 100) {
                        return;
                    }
                }
                break;
        }
        centerBoxImageView.getLayoutParams().height = height;
        centerBoxImageView.requestLayout();

    }

    private void updateBoxWidth(int corner, int initX, int x) {
        int i;
        int width = centerBoxImageView.getLayoutParams().width;
        switch (corner) {
            case 1:
            case 4:
                if (x > initX) {
                    i = x - initX;
                    width -= i * 2;
                    if (width < 100) {
                        return;
                    }
                } else {
                    i = initX - x;
                    width += i * 2;
                    if (width > displayWidth) {
                        return;
                    }
                }
                break;

            case 2:
            case 3:
                if (x > initX) {
                    i = x - initX;
                    width += i * 2;
                    if (width > displayWidth) {
                        return;
                    }
                } else {
                    i = initX - x;
                    width -= i * 2;
                    if (width < 100) {
                        return;
                    }
                }
                break;
        }
        centerBoxImageView.getLayoutParams().width = width;
        centerBoxImageView.requestLayout();
    }

    private void changeTextureViewSize(boolean increased, int xDiff, int yDiff) {
        Log.d(TAG, "changeTextureViewSize: " + xDiff + " " + yDiff);
        mTextureViewWidth = mTextureView.getWidth();
        mTextureViewHeight = mTextureView.getHeight();

        double heightRatio = (double) displayWidth / (double) displayHeight;
        if (increased) {
            centerBoxImageView.getLayoutParams().width += xDiff;
            centerBoxImageView.getLayoutParams().height += yDiff;

            mTextureViewWidth += xDiff * 3;
            mTextureViewHeight += (int) Math.round(xDiff * 3 / heightRatio);
        } else {
            int width = centerBoxImageView.getLayoutParams().width;
            int height = centerBoxImageView.getLayoutParams().height;
            if (width - xDiff < 50 || height - yDiff < 50) {
                return;
            }
            centerBoxImageView.getLayoutParams().width = width - xDiff;
            centerBoxImageView.getLayoutParams().height = height - yDiff;

            if (mTextureViewWidth - xDiff * 3 > displayWidth) {
                mTextureViewWidth -= xDiff * 3;
                mTextureViewHeight -= (int) Math.round(xDiff * 3 / heightRatio);
            }
        }

        mTextureView.getLayoutParams().width = mTextureViewWidth;
        mTextureView.getLayoutParams().height = mTextureViewHeight;

        centerBoxImageView.requestLayout();
        mTextureView.requestLayout();
    }

    @Override
    public void onActivityClick(int position) {
        if (position > 1) {
            View view = layoutManager.findViewByPosition(position - 1);
            if (view != null) {
                int width = view.getWidth() + 50;
                layoutManager.scrollToPositionWithOffset(position, width);
            }
            activitiesRecyclerView.smoothScrollToPosition(position);
        }
    }

    @Override
    public void updateLabels(int currentSelection) {
        Log.d("JEBYRNE", "VideoCaptureActivity.updateLabels");
        if (mIsRecordingVideo) {
            addLabel();
        }
    }

    private Range<Integer> getRange() {
        CameraManager mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Range<Integer> result = null;
        try {
            CameraCharacteristics chars = mCameraManager.getCameraCharacteristics(cameraId);
            Range<Integer>[] ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            if (ranges != null && ranges.length > 0) {
                for (Range<Integer> range : ranges) {
                    int upper = range.getUpper();

                    // 10 - min range upper for my needs
                    if (upper >= 10) {
                        if (result == null || upper < result.getUpper().intValue()) {
                            result = range;
                        }
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return result;
    }

    // JEBYRNE: common function to return to entry screen on error
    private void logout() {
        FileUtil.deleteFile(mNextVideoAbsolutePath);
        Intent loginIntent = new Intent(this, FrontScreenActivity.class);
        this.startActivity(loginIntent);
        finish();
    }
}
