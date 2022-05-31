package com.visym.collector.utils;

import android.media.CamcorderProfile;
import android.util.SparseIntArray;
import android.view.Surface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraUtil {
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    private static final int VIDEO_MAX_SIZE = 1920 * 1080;

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

    public static int getCameraAngle(int orientation) {
        return DEFAULT_ORIENTATIONS.get(orientation);
    }

    public static Size chooseVideoSize(List<Size> choices) {
        int i = 0;
        for (Size size : choices) {
            if (size.getAreaSize() == VIDEO_MAX_SIZE) {
                return size;
            } else if (size.getAreaSize() < VIDEO_MAX_SIZE) {
                return i == 0 ? choices.get(i) : choices.get(i - 1);
            }
            i++;
        }
        return choices.get(0);
    }

    public static Size getPreferredPreviewSize(List<Size> mapSizes, int width, int height) {
        List<Size> collectorSizes = new ArrayList<>();
        for (Size option : mapSizes) {
            if (width > height) {
                if (option.getWidth() > width &&
                        option.getHeight() > height) {
                    collectorSizes.add(option);
                }
            } else {
                if (option.getWidth() > height &&
                        option.getHeight() > width) {
                    collectorSizes.add(option);
                }
            }
        }
        if (collectorSizes.size() > 0) {
            return Collections.min(collectorSizes, new CompareSizesByArea());
        }
        return mapSizes.get(0);
    }

    public static String getOrientationKey(int orientation) {
        switch (orientation) {
            case Surface.ROTATION_90:
                return Constant.LANDSCAPE_RIGHT_KEY;
            case Surface.ROTATION_270:
                return Constant.LANDSCAPE_LEFT_KEY;
            default:
                return Constant.PORTRAIT_KEY;
        }
    }

    public static CamcorderProfile getCameraProfile(int cameraId) {
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
        } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_LOW)) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
        }
        return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
    }

    public static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

}
