package com.visym.collector.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

public class PermissionUtil {

    public static boolean checkRequiredPermissionsGranted(Context context, String[] permissions) {
        if (permissions == null || permissions.length == 0){
            throw new RuntimeException("permissions must not be null or empty");
        }

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
