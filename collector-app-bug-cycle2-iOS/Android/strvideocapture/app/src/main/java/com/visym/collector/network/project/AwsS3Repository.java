package com.visym.collector.network.project;

import android.net.Uri;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.visym.collector.model.NetworkCallback;
import com.visym.collector.network.NetworkClientError;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.AwsResponse;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;

import org.json.JSONArray;

import java.io.File;
import java.util.regex.Pattern;

public class AwsS3Repository {

    private AppSharedPreference preference;
    private String subjectId;
    private JSONArray recentSubjects;
    public AwsS3Repository() {
        preference = AppSharedPreference.getInstance();
    }

    private static final String TAG = "AwsS3Repository";

    private static TransferUtility getTransferUtility() {
        return TransferUtility.builder()
                .context(Globals.getAppContext())
                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                .s3Client(new AmazonS3Client(AWSMobileClient.getInstance(),
                        Region.getRegion(Regions.US_EAST_1)))
                .build();
    }

    public void uploadFile(String videoFilePath, int fileType, NetworkCallback<AwsResponse> callback) {
        TransferUtility transferUtility = getTransferUtility();

        AwsResponse awsResponse = new AwsResponse();
        awsResponse.setState(TransferState.IN_PROGRESS);

        File file = null;
        String videoUrl = null;

        TransferObserver transferObserver = null;
        try {
            if (fileType == AwsResponse.FILE_TYPE_CONSENT) {
                awsResponse.setFileType(fileType);
                file = new File(videoFilePath);
                String collectorId = preference.readString(Constant.COLLECTOR_EMAIL);
                if(AppSharedPreference.getInstance().readString(Constant.SUBJECT_EMAIL_TEXT) != null) {
                    recentSubjects = new JSONArray(AppSharedPreference.getInstance().readString(Constant.SUBJECT_EMAIL_TEXT));
                }
                if(Globals.isRetakeConsentVideo){
                    subjectId = collectorId;
                }else if(recentSubjects != null) {
                    subjectId = recentSubjects.getString(recentSubjects.length() - 1);
                }
                String BUCKET_CONSENT_SUBFOLDER = "uploads/Consent_Documentations";
                videoUrl = BUCKET_CONSENT_SUBFOLDER + "/" + collectorId + "/" + subjectId + "/consent_video.mp4";
                transferObserver = transferUtility.upload(videoUrl, file);

            } else if (fileType == AwsResponse.FILE_TYPE_VIDEO) {
                awsResponse.setFileType(fileType);
                file = new File(videoFilePath);
                String programName = preference.readString(Constant.PROGRAM_ID_KEY);
                String projectName = preference.readString(Constant.PROJECT_NAME_KEY);
                videoUrl = "uploads/Programs/" + programName + "/" + projectName + "/" + file.getName();
                transferObserver = transferUtility.upload(videoUrl, file);
            } else if (fileType == AwsResponse.FILE_TYPE_JSON) {
                String programName = preference.readString(Constant.PROGRAM_ID_KEY);
                String projectName = preference.readString(Constant.PROJECT_NAME_KEY);
                String path = "uploads/Programs/" + programName + "/" + projectName + "/";
                awsResponse.setFileType(fileType);
                String[] split = videoFilePath.split(",");
                String filePath = split[0];
                String fileNamePath = split[1];
                file = new File(filePath);
                if (file.exists() && file.length() == 0) {
                    awsResponse.setState(TransferState.FAILED);
                    awsResponse.setErrorMessage("Upload failed");
                    callback.onSuccess(awsResponse);
                    return;
                }

                String fileName = Uri.parse(fileNamePath).getLastPathSegment().split(".mp4")[0];
                videoUrl = path + fileName + ".json";

                transferObserver = transferUtility.upload(path + fileName + ".json", file);
            }else if (fileType == AwsResponse.FILE_TYPE_UPDATED_JSON){
                awsResponse.setFileType(fileType);
                String[] splitArray = videoFilePath.split(",");
                awsResponse.setVideoUrl(splitArray[1]);
                transferObserver = transferUtility.upload(splitArray[0], new File(splitArray[1]));
            }
            callback.onSuccess(awsResponse);

            String finalVideoUrl = videoUrl;
            File finalFile = file;

            if (transferObserver == null) {
                awsResponse.setState(TransferState.FAILED);
                callback.onSuccess(awsResponse);
                return;
            }

            transferObserver.setTransferListener(new TransferListener() {
                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (TransferState.COMPLETED == state) {
                        awsResponse.setVideoUrl(finalVideoUrl);
                    }
                    awsResponse.setState(state);
                    callback.onSuccess(awsResponse);
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                    float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                    int percentDone = (int) percentDonef;
                    awsResponse.setProgress(percentDone);
                    callback.onSuccess(awsResponse);
                }

                @Override
                public void onError(int id, Exception ex) {
                    FirebaseCrashlytics.getInstance().recordException(ex.getCause());
                    callback.onFailure(new NetworkClientError(ex.getCause()));
                }
            });
        }catch (Exception e){
            Log.e(TAG, "uploadFile: "+e.toString());
        }

    }

    public void downloadFile(String url, boolean trainingModule, NetworkCallback<AwsResponse> callback) {
        TransferUtility transferUtility = getTransferUtility();
        String[] split = url.split(Pattern.quote("."));
        String lastPathSegment = split[split.length - 1];
        File file = new File(Globals.getInstance().getFilesDir(), url);

        AwsResponse awsResponse = new AwsResponse();
        if (lastPathSegment == null){
            awsResponse.setState(TransferState.FAILED);
        }else {
            awsResponse.setState(TransferState.IN_PROGRESS);
            if (lastPathSegment.contains("json")){
                awsResponse.setFileType(AwsResponse.FILE_TYPE_JSON);
            }else {
                awsResponse.setFileType(AwsResponse.FILE_TYPE_VIDEO);
            }
        }
        if (file.exists()){
            awsResponse.setState(TransferState.COMPLETED);
            awsResponse.setVideoUrl(file.getAbsolutePath());
            callback.onSuccess(awsResponse);
            return;
        }
        callback.onSuccess(awsResponse);

        TransferObserver transferObserver;
        if (trainingModule){
            transferObserver = transferUtility.download("diva-str-prod-data-public", url, file);
        }else {
            transferObserver = transferUtility.download(url, file);
        }

        transferObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, "onStateChanged: ");
                if (TransferState.COMPLETED == state) {
                    awsResponse.setState(state);
                    awsResponse.setVideoUrl(file.getAbsolutePath());
                }else if (TransferState.FAILED == state) {

                }else {
                    awsResponse.setState(state);
                }
                callback.onSuccess(awsResponse);
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int) percentDonef;
                awsResponse.setProgress(percentDone);
                callback.onSuccess(awsResponse);
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d(TAG, "onError: "+ ex.getCause());
                callback.onFailure(new NetworkClientError(ex.getCause()));
            }
        });
    }
}
