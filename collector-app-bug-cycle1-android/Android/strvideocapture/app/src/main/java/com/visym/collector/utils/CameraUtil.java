package com.visym.collector.utils;

import android.graphics.Point;
import android.media.CamcorderProfile;
import android.view.Surface;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraUtil {
    private static final int VIDEO_MAX_SIZE = 1920 * 1080;

    public static Size chooseVideoSize(List<Size> choices) {
        for (Size size : choices) {
            Log.d("JEBYRNE", String.format("CameraUtil.chooseVideoSize: (%d,%d)", size.getHeight(), size.getWidth()));
        }

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

    public static Size chooseOptimalPreviewSize(android.util.Size[] choices, int textureViewWidth,
                                                int textureViewHeight, int maxWidth, int maxHeight,
                                                Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        for (android.util.Size size : choices) {
            if (size.getWidth() <= maxWidth && size.getHeight() <= maxHeight &&
            size.getHeight() == size.getWidth() * h / w){
                if (size.getWidth() >= textureViewWidth && size.getHeight() >= textureViewHeight){
                    bigEnough.add(new Size(size.getWidth(), size.getHeight()));
                }else {
                    notBigEnough.add(new Size(size.getWidth(), size.getHeight()));
                }
            }
        }
        if (bigEnough.size() > 0){
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        }else {
            return new Size(choices[0].getWidth(), choices[0].getHeight());
        }
    }

    public static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
