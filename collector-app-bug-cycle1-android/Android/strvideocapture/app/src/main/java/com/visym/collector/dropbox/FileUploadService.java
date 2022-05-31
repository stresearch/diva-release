package com.visym.collector.dropbox;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.dropbox.core.v2.DbxClientV2;
import com.visym.collector.R;
import com.visym.collector.dashboardmodule.view.DashboardActivity;

import java.io.File;

public class FileUploadService extends Service {

    public static final String START_UPLOAD = "START_UPLOAD";
    public static final String CANCEL_UPLOAD = "CANCEL_UPLOAD";
    private static final String CHANNEL_ID = "FILE_UPLOAD";
    File file;
    String ACCESS_TOKEN;
    private DbxClientV2 dbxClient;
    private Context context;
    private NotificationManager mManager;

    public static void cancelUpload(Context context) {
        Intent intent = new Intent(context, FileUploadService.class);
        intent.setAction(CANCEL_UPLOAD);
        context.startService(intent);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        File file5 = null;
        if (intent.getStringExtra("file") != null) {
            file5 = new File(intent.getStringExtra("file"));
        }
        String accesstoken = intent.getStringExtra("ACCESS_TOKEN");
        if (!TextUtils.isEmpty(action)) {
            switch (action) {
                case START_UPLOAD:
                    if (file5 != null) {

                        startFileUpload(file5, accesstoken);
                    }
                    break;
                case CANCEL_UPLOAD:
                    cancelFileUpload();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    void cancelFileUpload() {

        hideNotification();
    }

    void startFileUpload(File fileList, String Accesstoken) {
        file = fileList;
        ACCESS_TOKEN = Accesstoken;
        startUploadingFile();
    }

    void startUploadingFile() {

        new UploadTask(DropboxClient.getClient(ACCESS_TOKEN), file, getApplicationContext()).execute();
        showUploadNotification(file.getName());
    }


    void hideNotification() {
        getManager().cancel(1);
        stopForeground(true);
    }

    public NotificationManager getManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(String channelId, String channelName, String msg) {
        Intent resultIntent = new Intent(this, DashboardActivity.class);
// Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Uploading to dropbox")
                .setContentText(msg)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(resultPendingIntent) //intent
                .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, notificationBuilder.build());
        startForeground(1, notification);
    }

    void showUploadNotification(String fileName) {
        String messageText = "";

        messageText = fileName;
        if (Build.VERSION.SDK_INT >= 26) {
            createNotificationChannel(CHANNEL_ID, "ho", messageText);
        } else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
            builder
                    .setContentTitle("Uploading to dropbox")
                    .setContentText(messageText)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setAutoCancel(true);

            startForeground(1, builder.build());
        }
    }


}