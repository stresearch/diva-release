package com.visym.collector.capturemodule.views;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.Range;
import android.util.SparseArray;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;

import com.visym.collector.BaseActivity;
import com.visym.collector.R;
import com.visym.collector.capturemodule.interfaces.Listeners;
import com.visym.collector.capturemodule.interfaces.Listeners.SubmitVideoToS3BucketListner;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.usermodule.view.FrontScreenActivity;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.AspectRatio;
import com.visym.collector.utils.CameraPreview;
import com.visym.collector.utils.CameraUtil;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.FileUtil;
import com.visym.collector.utils.Globals;
import com.visym.collector.utils.MediaController;
import com.visym.collector.utils.PermissionUtil;
import com.visym.collector.utils.Size;
import com.visym.collector.utils.SizeMap;
import com.visym.collector.utils.VideoView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ConsentVideoCaptureActivity extends BaseActivity implements View.OnClickListener,
        Listeners.CameraOrientationListner, SubmitVideoToS3BucketListner {

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private static final String TAG = ConsentVideoCaptureActivity.class.getSimpleName();
    private Camera mCamera;
    private CameraPreview mPreview;
    private SurfaceView preview;
    private MediaRecorder mMediaRecorder;
    @BindView(R.id.closeFrameLayout)
    FrameLayout closeFrameLayout;
    @BindView(R.id.camera_preview)
    AutoFitTextureView mTextureView;
    @BindView(R.id.recordingFrameLayout)
    FrameLayout recordingFrameLayout;
    @BindView(R.id.actionOuterEllipse)
    ImageView actionOuterEllipse;
    @BindView(R.id.actionInnerEllipse)
    ImageView actionInnerEllipse;
    @BindView(R.id.timerText)
    TextView timerText;
    @BindView(R.id.actionText)
    TextView actionText;
    @BindView(R.id.videoPreview)
    VideoView videoPreview;
    @BindView(R.id.videoRecorder)
    Group videoRecorderGroup;
    @BindView(R.id.videoPreviewFrame)
    FrameLayout videoPreviewFrame;
    @BindView(R.id.permissionDenied)
    TextView permissionDenied;
    @BindView(R.id.retakeVideo)
    ImageView retakeVideo;
    @BindView(R.id.consentText)
    TextView consentHintText;
    private boolean isRecording = false, isVideoPlaying = false;
    private long startTime = 0L;
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;
    private Timer timer;

    private int cameraDisplayOrientation;

    private MediaController mediacontroller;
    private Size mVideoSize;
    private Size mPreviewSize;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private AspectRatio aspectRatio = AspectRatio.of(16, 9);
    private SparseArray<Integer> cameraFaceTypeMap;

    private SizeMap videoSizeMap = new SizeMap();
    private SortedSet<com.visym.collector.utils.Size> supportedVideoSizes = new TreeSet<>();

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
            mCameraOpenCloseLock.release();
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
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private final int CAMERA_PERMISSION_REQUEST = 1000;
    private String[] cameraPermissions = new String[]{
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    private String mNextVideoAbsolutePath;
    private boolean isRetakeVideo = false;

    private CameraCharacteristics characteristics;
    private String cameraId;
    private int counter;
    private Integer sensorOrientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consent_video_capture);
        ButterKnife.bind(this);
        timerText.setVisibility(View.GONE);
        recordingFrameLayout.setOnClickListener(this);
        closeFrameLayout.setOnClickListener(this);
        retakeVideo.setOnClickListener(this);

        cameraFaceTypeMap = new SparseArray<>();
        String consentText = AppSharedPreference.getInstance().readString(Constant.CONSENT_OVERLAY_TEXT);
        if (!TextUtils.isEmpty(consentText)) {
            String replacedString = consentText.replace("\n", "<br>");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                consentHintText.setText(Html.fromHtml(replacedString, Html.FROM_HTML_MODE_LEGACY));
            } else {
                consentHintText.setText(Html.fromHtml(replacedString));
            }
        } else {
            consentHintText.setText(getResources().getString(R.string.consent_hint_text));
        }
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("isRetakeVideo")) {
            isRetakeVideo = intent.getBooleanExtra("isRetakeVideo", false);
        }
    }


    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();

            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mPreviewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.d(Globals.TAG, "onConfigureFailed: ");
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(Globals.TAG, "startPreview: ", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocas) {
        super.onWindowFocusChanged(hasFocas);
        View decorView = getWindow().getDecorView();
        if (hasFocas) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onResume();
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                Toast.makeText(this, "Some permissions are not granted",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();// clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        closeCamera();
        stopBackgroundThread();
    }


    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            releaseMediaRecorder();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (timer != null){
                timer.cancel();
                timer = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        releaseMediaRecorder();
        releaseCamera();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.recordingFrameLayout:
                if (isRecording) {
                    stopRecordingVideo();
                } else {
                    recordingFrameLayout.setVisibility(View.GONE);
                    startRecordingVideo();
                }
                break;
            case R.id.closeFrameLayout:
                if (isVideoPlaying) {
                    closeCamera();
                    if (timer != null) {
                        timer.cancel();
                    }
                    finish();
                }
                else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                            .setCancelable(false)
                            .setMessage("Exit this collection?")
                            .setPositiveButton("YES", (dialog, which) -> {
                                if (timer != null) {
                                    timer.cancel();
                                }
                                closeCamera();
                                if (!isRetakeVideo){
                                    Intent intent = new Intent(this, DashboardActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                    startActivity(intent);
                                }
                                finish();

                            })
                            .setNegativeButton("NO", (dialog, which) -> {
                                dialog.dismiss();
                            });
                    showDialogAndHideSystemBar(builder.create());
                }
                break;
            case R.id.retakeVideo:
                closeCamera();
                refreshActivity();
                break;
        }
    }

    private void refreshActivity() {
        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);
    }


    private void startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
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
            Log.e(Globals.TAG, "startRecordingVideo: surface size : " + surfaces.size());
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isRecording = true;
                            // Start recording
                            mMediaRecorder.start();
                            timer = new Timer();
                            startTimer();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(Globals.TAG, "onConfigureFailed: camera  " + cameraCaptureSession.toString());
                    Toast.makeText(ConsentVideoCaptureActivity.this, "Camera Configuration Failed", Toast.LENGTH_SHORT).show();
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(Globals.TAG, "startRecordingVideo: ", e);
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (JSONException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (counter == 0){
                            recordingFrameLayout.setVisibility(View.VISIBLE);
                            actionOuterEllipse.setImageDrawable(getResources().getDrawable(R.drawable.ellipse_outer_red));
                            actionInnerEllipse.setImageDrawable(getResources().getDrawable(R.drawable.ellipse_red));
                            actionText.setTextColor(getResources().getColor(android.R.color.white));
                            actionText.setText(getResources().getString(R.string.stop));

                            timerText.setVisibility(View.VISIBLE);
                            startTime = SystemClock.uptimeMillis();
                        }
                        counter++;
                        timerText.setText(String.format("%s:%s", "00",
                                counter < 10 ? "0" + counter : "" + counter));
                    }
                });
            }
        };
        if (timer != null){
            timer.scheduleAtFixedRate(task, 1000, 1000);
        }
    }

    private void stopRecordingVideo() {
        if (timer != null){
            timer.cancel();
        }
        counter = 0;
        isRecording = false;
        recordingFrameLayout.setVisibility(View.GONE);
        timeInMilliseconds = 0L;
        timeSwapBuff = 0L;
        closeCamera();
        playCapturedMedia();
    }

    private void setUpMediaRecorder() throws IOException, JSONException {
        CamcorderProfile profile = CameraUtil.getCameraProfile(1);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        String videoFilePath = FileUtil.getVideoFilePath(this, "capture");
        if (videoFilePath != null) {
            mNextVideoAbsolutePath = videoFilePath;
            mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        }
        else {
            Toast.makeText(this, "Recording error - Insufficient space or permissions", Toast.LENGTH_LONG).show();
            logout();  // JEBYRNE
        }

        mMediaRecorder.setOutputFormat(profile.fileFormat);
        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stopRecordingVideo();
                }
            }
        });
        mMediaRecorder.setMaxDuration(Constant.CONSENT_VIDEO_LENGTH);
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(profile.videoCodec);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);

        if (sensorOrientation == null) {
            mMediaRecorder.setOrientationHint(90);
        } else {
            mMediaRecorder.setOrientationHint(sensorOrientation);
        }

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            FileUtil.deleteFile(mNextVideoAbsolutePath);
            Log.d(TAG, "setUpMediaRecorder: ", e.getCause());
        }
    }

    public void playCapturedMedia() {
        Intent intent = new Intent(this, VideoPreviewActivity.class);
        intent.putExtra(Constant.VIDEO_FILE_PATH_KEY, mNextVideoAbsolutePath);
        intent.putExtra(VideoPlayerActivity.INTENT_CONSENT_TEXT, true);
        if (isRetakeVideo) {
            intent.putExtra("isRetakeVideo", isRetakeVideo);
        }
        startActivity(intent);
        finish();
    }

    @Override
    public void getDisplayOrientation(int value) {
        cameraDisplayOrientation = value;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.e(Globals.TAG, "onTouchEvent: " + event.toString());
        if (mediacontroller != null) {
            mediacontroller.doPauseResume();
        }
        return true;
    }

    public void onSubmitButtonClick() {
        Intent captureSubjectVideo = new Intent(ConsentVideoCaptureActivity.this,
                VideoCaptureActivity.class);
        startActivity(captureSubjectVideo);
        //Hit api function to save the consent video
        // and delete the video file from local
    }

    @Override
    public void isPlayerPlaying(Boolean isPlaying) {
        Log.e(Globals.TAG, "isPlayerPlaying: " + isPlaying);
        isVideoPlaying = isPlaying;
    }

    private void collectCameraFaces() throws CameraAccessException {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) return;

        for (final String cameraId : manager.getCameraIdList()) {
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                if (cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_FRONT) != null) {
                    cameraFaceTypeMap.append(CameraCharacteristics.LENS_FACING_FRONT,
                            cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_FRONT) + 1);
                } else {
                    cameraFaceTypeMap.append(CameraCharacteristics.LENS_FACING_FRONT, 1);
                }
            }

            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                if (cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_FRONT) != null) {
                    cameraFaceTypeMap.append(CameraCharacteristics.LENS_FACING_BACK, cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_BACK) + 1);
                } else {
                    cameraFaceTypeMap.append(CameraCharacteristics.LENS_FACING_BACK, 1);
                }
            }
        }
    }

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

    private void openCamera(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) return;

        try {
            if (cameraFaceTypeMap != null) {
                cameraFaceTypeMap.clear();
            }
            collectCameraFaces();

            Integer num_facing_front_camera = cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_FRONT);
            for (final String cameraId : manager.getCameraIdList()) {
                characteristics = manager.getCameraCharacteristics(cameraId);
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                // If facing back camera or facing external camera exist, we won't use facing front camera
                if (num_facing_front_camera != null && num_facing_front_camera > 0) {
                    // We don't use a front facing camera in this sample if there are other camera device facing types
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        // Choose the sizes for camera preview and video recording
                        StreamConfigurationMap map = characteristics
                                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                        if (map == null) {
                            throw new RuntimeException("Cannot get available preview/video sizes");
                        }

                        int displayRotation = 0;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            displayRotation = getDisplay().getRotation();
                        }
                        else {
                            displayRotation = getWindowManager().getDefaultDisplay().getRotation();
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
                        if (videoAspectSizes != null && videoAspectSizes.size() > 0) {
                            mVideoSize = CameraUtil.chooseVideoSize(videoAspectSizes);
                        } else {
                            CamcorderProfile profile = CameraUtil.getCameraProfile(0);
                            mVideoSize = new Size(profile.videoFrameWidth, profile.videoFrameHeight);
                        }

                        mPreviewSize = CameraUtil.chooseOptimalPreviewSize(map.getOutputSizes(SurfaceTexture.class),
                                rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, mVideoSize);

                        int orientation = getResources().getConfiguration().orientation;
                        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                        } else {
                            mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                        }
                        this.cameraId = cameraId;
                        configureTransform(width, height);
                        mMediaRecorder = new MediaRecorder();
                        manager.openCamera(cameraId, mStateCallback, mBackgroundHandler);
                        break;
                    }
                }
            }
            initUI();
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Toast.makeText(this, "Sorry!... This feature is not supported in this device",
                    Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {

        }
    }

    private void initUI() {
        mTextureView.post(new Runnable() {
            @Override
            public void run() {
                int x = (int) mTextureView.getX();
                int y = (int) mTextureView.getY();
                closeFrameLayout.setPadding(x + (int) getResources().getDimension(R.dimen.largeMargin), y + (int) getResources().getDimension(R.dimen.largeMargin), 0, 0);
            }
        });
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreview) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreview.getHeight(), mPreview.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreview.getHeight(),
                    (float) viewWidth / mPreview.getWidth());
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

    private void initVideoSizes(android.util.Size[] outputSizes) {
        supportedVideoSizes.clear();
        for (android.util.Size size : outputSizes) {
            com.visym.collector.utils.Size s = new com.visym.collector.utils.Size(size.getWidth(), size.getHeight());
            supportedVideoSizes.add(s);
            videoSizeMap.add(s);
        }
    }

    private void requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, cameraPermissions,
                CAMERA_PERMISSION_REQUEST);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBar();
        boolean permissionsGranted = PermissionUtil
                .checkRequiredPermissionsGranted(this, cameraPermissions);
        if (!permissionsGranted) {
            requestCameraPermissions();
        } else {
            startBackgroundThread();
            if (mTextureView.isAvailable()) {
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            } else {
                mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            }
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        new Handler().postDelayed(() -> {
            try {
                setUpCaptureRequestBuilder(mPreviewBuilder);
                HandlerThread thread = new HandlerThread("CameraPreview");
                thread.start();
                if (mPreviewSession != null) {
                    mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null,
                            mBackgroundHandler);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }, 500);
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        Range<Integer> range = getRange();
        if (range != null) {
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range);
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
        Intent loginIntent = new Intent(this, FrontScreenActivity.class);
        this.startActivity(loginIntent);
        finish();
    }
}

