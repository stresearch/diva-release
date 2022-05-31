package com.visym.collector.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FaceDetectorModule implements Runnable {
    private final FaceDetector detector;
    private final Paint paint;
    private File externalFilesDir;
    private Context context;
    private Handler imageHandler;

    public FaceDetectorModule(Handler imageHandler) {
        this.imageHandler = imageHandler;
        this.context = Globals.getAppContext();
        externalFilesDir = context.getExternalFilesDir(Constant.VIDEO_DIRECTORY_NAME
                + File.separator + "frames");

        detector = new FaceDetector.Builder(context)
                .setProminentFaceOnly(true)
                .setTrackingEnabled(false)
                .build();

        paint = new Paint();
        paint.setStrokeWidth(2);
        paint.setColor(Color.RED);
    }

    @Override
    public void run() {

        File[] files = externalFilesDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = new File(externalFilesDir + "/image" + (i+1) + ".jpg");
            if (file.exists()){
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;

                    Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file),
                            null, options);
                    if (bitmap == null){
                        continue;
                    }
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();

                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawBitmap(bitmap, 0, 0, null);

                    SparseArray<Face> faces = detector.detect(frame);
                    for (int index = 0; index < faces.size(); ++index) {
                        Face face = faces.valueAt(index);
                        canvas.drawRect(
                                face.getPosition().x,
                                face.getPosition().y,
                                face.getPosition().x + face.getWidth(),
                                face.getPosition().y + face.getHeight(), paint);
                    }
                    Log.d("Image", "run: " + i);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                file.delete();
            }
        }
    }
}
