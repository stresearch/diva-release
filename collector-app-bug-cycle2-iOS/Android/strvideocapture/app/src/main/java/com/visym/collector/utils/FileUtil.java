package com.visym.collector.utils;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.visym.collector.capturemodule.model.FrameJSON;
import com.visym.collector.exception.InsufficientStorageException;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.regex.Pattern;

public class FileUtil {

    private static final String COLLECTOR_FOLDER_NAME = "Visym Collector";

    public static File writeToJSONFile(String fileName, String content) throws IOException {
        Context context = Globals.getAppContext().getApplicationContext();
        File file = new File(context.getCacheDir(), fileName);
        file.createNewFile();
        OutputStreamWriter streamWriter = new OutputStreamWriter(new FileOutputStream(file));
        streamWriter.flush();
        streamWriter.write(content);
        streamWriter.close();
        return file;
    }

    public static String getVideoFilePath(Context context, String subDir) throws IOException, JSONException {
        try {
            File externalFilesDir = context.getExternalFilesDir(Constant.VIDEO_DIRECTORY_NAME
                    + File.separator + subDir);;
            if (externalFilesDir == null) {
                return null;
            }
            if (!externalFilesDir.exists()) {
                externalFilesDir.mkdirs();
            }

            long availableStorage = externalFilesDir.getFreeSpace();
            if (availableStorage < 100000000) {
                throw new InsufficientStorageException("Storage is low. Please clear some storage and try again later");
            }

            File tempFile = File.createTempFile(FileUtil.getUUID(), ".mp4", externalFilesDir);
            return tempFile.getAbsolutePath();
        } catch (IOException ex) {
            throw new IOException("Failed to create file. Check storage or permission is granted");
        }
    }

    private static String getUUID() {
        return UUID.randomUUID().toString();
    }

    public static FrameJSON readFromFile(Context context, String filePath) throws IOException {
        File file;
        if (filePath.contains(Constant.FRAMES_JSON_FILE_NAME)) {
            file = new File(context.getCacheDir(), filePath);
        } else {
            file = new File(filePath);
        }
        BufferedReader br = new BufferedReader(new FileReader(file));

        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            builder.append(line);
        }
        br.close();

        Type type = new TypeToken<FrameJSON>() {
        }.getType();
        return new Gson().fromJson(builder.toString(), type);
    }


    public static String getFilePath(String fileName) throws IOException {
        Context context = Globals.getAppContext().getApplicationContext();
        File file = new File(context.getCacheDir(), fileName);
        if (file.exists()) {
            return file.getAbsolutePath();
        } else {
            file.createNewFile();
            return file.getAbsolutePath();
        }
    }

    public static void deleteFile(String videoFilePath) {
        if (videoFilePath != null) {
            File file = new File(videoFilePath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public static String appendTimestamp(String fileUrl) {
        if (!TextUtils.isEmpty(fileUrl)) {
            String[] splitArray = fileUrl.split("/");
            if (splitArray.length > 0) {
                String fileName = splitArray[splitArray.length - 1];
                String[] split = fileName.split(Pattern.quote("."));
                if (splitArray.length > 1) {
                    splitArray[splitArray.length - 1] = split[0] + "_"
                            + System.currentTimeMillis() + "." + split[1];
                }

                StringBuilder updatedFileName = null;
                for (String string : splitArray) {
                    if (updatedFileName == null) {
                        updatedFileName = new StringBuilder();
                        updatedFileName.append(string);
                        continue;
                    }
                    updatedFileName.append("/").append(string);
                }
                return updatedFileName.toString();
            }
        }
        return null;
    }
}
