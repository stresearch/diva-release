package com.visym.collector.capturemodule.views;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.util.Range;
import android.util.SparseArray;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;

import com.visym.collector.R;
import com.visym.collector.capturemodule.interfaces.Listeners;
import com.visym.collector.capturemodule.interfaces.Listeners.SubmitVideoToS3BucketListner;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.AspectRatio;
import com.visym.collector.utils.CameraPreview;
import com.visym.collector.utils.CameraUtil;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.FileUtil;
import com.visym.collector.utils.Globals;
import com.visym.collector.utils.MediaController;
import com.visym.collector.utils.PermissionUtil;
import com.visym.collector.utils.SharedPref;
import com.visym.collector.utils.Size;
import com.visym.collector.utils.SizeMap;
import com.visym.collector.utils.VideoView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ConsentVideoCaptureActivity extends AppCompatActivity implements View.OnClickListener,
        Listeners.CameraOrientationListner, SubmitVideoToS3BucketListner {
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
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
    private long startHTime = 0L;
    private Handler customHandler = new Handler();
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;
    private SharedPref sharedPref;
    private int cameraDisplayOrientation;
    private static Camera c = null;
    int PERMISSION_ALL = 1;
    private MediaController mediacontroller;
    private Size mVideoSize;
    private Size mPreviewSize;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private AspectRatio aspectRatio = AspectRatio.of(16, 9);
    private SparseArray<Integer> cameraFaceTypeMap;

    private SizeMap previewSizeMap = new SizeMap();
    private SortedSet<com.visym.collector.utils.Size> supportedPreviewSizes = new TreeSet<>();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consent_video_capture);
        ButterKnife.bind(this);
        timerText.setVisibility(View.GONE);
        recordingFrameLayout.setOnClickListener(this);
        closeFrameLayout.setOnClickListener(this);
        retakeVideo.setOnClickListener(this);
        sharedPref = new SharedPref(this);
        cameraFaceTypeMap = new SparseArray<>();
        if (AppSharedPreference.getInstance().readString(Constant.CONSENT_OVERLAY_TEXT) != null) {
            consentHintText.setText(AppSharedPreference.getInstance().readString(Constant.CONSENT_OVERLAY_TEXT));
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
            mMediaRecorder.reset();   // clear recorder configuration
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
        super.onDestroy();
        releaseMediaRecorder();
        releaseCamera();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.recordingFrameLayout:
                if (isRecording) {
                    stopRecordingVideo();
                } else {
                    startRecordingVideo();
                }
                break;
            case R.id.closeFrameLayout:
                if (isVideoPlaying) {
                    closeCamera();
                    if (customHandler != null) {
                        customHandler.removeCallbacks(updateTimerThread);
                    }
                    finish();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                            .setCancelable(false)
                            .setMessage("Exit consent?")
                            .setPositiveButton("YES", (dialog, which) -> {
                                if (customHandler != null) {
                                    customHandler.removeCallbacks(updateTimerThread);
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
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
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
                            // UI
                            actionOuterEllipse.setImageDrawable(getResources().getDrawable(R.drawable.ellipse_outer_red));
                            actionInnerEllipse.setImageDrawable(getResources().getDrawable(R.drawable.ellipse_red));
                            actionText.setTextColor(getResources().getColor(android.R.color.white));
                            actionText.setText(getResources().getString(R.string.stop));
                            // Camera is available and unlocked, MediaRecorder is prepared,
                            // now you can start recording


                            timerText.setVisibility(View.VISIBLE);
                            startHTime = SystemClock.uptimeMillis();
                            customHandler.postDelayed(updateTimerThread, 1000);

                            // inform the user that recording has started
                            // setCaptureButtonText("Stop");
                            isRecording = true;
                            // Start recording
                            mMediaRecorder.start();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(Globals.TAG, "onConfigureFailed: camera  " + cameraCaptureSession.toString());
                    Toast.makeText(ConsentVideoCaptureActivity.this, "Failed" + cameraCaptureSession.toString(), Toast.LENGTH_SHORT).show();
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

    private void stopRecordingVideo() {
        // UI
        customHandler.removeCallbacks(updateTimerThread);
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

        mMediaRecorder.setOutputFormat(profile.fileFormat);
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(profile.videoCodec);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);

        Integer orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (orientation == null) {
            mMediaRecorder.setOrientationHint(90);
        } else {
            mMediaRecorder.setOrientationHint(orientation);
        }

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            FileUtil.deleteFile(mNextVideoAbsolutePath);
            Log.d(TAG, "setUpMediaRecorder: ", e.getCause());
        }
    }


    private Runnable updateTimerThread = new Runnable() {

        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startHTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            if (timerText != null)
                timerText.setText("" + String.format("%02d", mins) + ":"
                        + String.format("%02d", secs));

            if (secs == Constant.CONSENT_VIDEO_LENGTH) {
                stopRecordingVideo();
            } else {
                customHandler.postDelayed(this, 1000);
            }
        }

    };

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

                        initPreviewSizes(map.getOutputSizes(SurfaceTexture.class));
                        initVideoSizes(map.getOutputSizes(MediaRecorder.class));

                        List<Size> videoAspectSizes = videoSizeMap.sizes(aspectRatio);
                        if (videoAspectSizes != null && videoAspectSizes.size() > 0) {
                            mVideoSize = CameraUtil.chooseVideoSize(videoAspectSizes);
                        } else {
                            CamcorderProfile profile = CameraUtil.getCameraProfile(0);
                            mVideoSize = new Size(profile.videoFrameWidth, profile.videoFrameHeight);
                        }

                        List<Size> sizesWithAspectRatio = previewSizeMap.sizes(aspectRatio);
                        if (sizesWithAspectRatio != null && sizesWithAspectRatio.size() > 0) {
                            mPreviewSize = CameraUtil.getPreferredPreviewSize(sizesWithAspectRatio,
                                    width, height);
                        } else {
                            CamcorderProfile profile = CameraUtil.getCameraProfile(0);
                            mPreviewSize = new Size(profile.videoFrameWidth, profile.videoFrameHeight);
                        }

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
                closeFrameLayout.setPadding(x + (int) getResources().getDimension(R.dimen.titleTextSize),
                        y + (int) getResources().getDimension(R.dimen.titleTextSize), 0, 0);
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

    private void initPreviewSizes(android.util.Size[] outputSizes) {
        supportedPreviewSizes.clear();
        for (android.util.Size size : outputSizes) {
            com.visym.collector.utils.Size s = new com.visym.collector.utils.Size(size.getWidth(), size.getHeight());
            supportedPreviewSizes.add(s);
            previewSizeMap.add(s);
        }
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
}

