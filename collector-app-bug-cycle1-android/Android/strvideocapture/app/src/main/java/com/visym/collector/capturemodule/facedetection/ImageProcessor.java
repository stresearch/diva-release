/*  JEBYRNE: COMMENT EVERYTHING


package com.visym.collector.capturemodule.facedetection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.visym.collector.R;
import com.visym.collector.capturemodule.model.FrameObject;
import com.visym.collector.model.BoundingBox;
import com.visym.collector.rs.ScriptC_rotators;
import com.visym.collector.utils.FrameUtil;
import com.visym.collector.utils.Globals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class ImageProcessor implements Runnable {
    private static final String VIDEO = "video/";
    private static final String TAG = "VideoDecoder";
    private static final long DEFAULT_TIMEOUT_US = 0;
    private final String inputFile;
    private final String outputFile;
    private MediaCodec mDecoder;

    private MediaExtractor mExtractor;
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private int width;
    private int height;

    private MediaCodec mEncoder;
    private MediaMuxer mediaMuxer;
    private int mTrackIndex;
    private ScriptC_rotators rotateScript;
    private int newWidth = 0, newHeight = 0;

    private int preRotateHeight;
    private int preRotateWidth;
    private Allocation fromRotateAllocation;
    private Allocation toRotateAllocation;
    private int frameIndex;
    private boolean faceDetectionEnabled = true;
    private FaceDetector faceDetector;
    private int deviceOrientation;
    private int sensorOrientation;
    private final Handler handler;

    boolean sawOutputEOS = false;
    boolean sawInputEOS = false;

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private FrameObject defaultObject;
    private int faceBlurCount;
    private CodecInputSurface inputSurface;

    public ImageProcessor(Handler handler, String inputFile, String outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.handler = handler;
    }

    public void setDeviceOrientation(int deviceOrientation) {
        this.deviceOrientation = deviceOrientation;
    }

    public void setSensorOrientation(int sensorOrientation) {
        this.sensorOrientation = sensorOrientation;
    }

    public void setDefaultObject(FrameObject frameObject) {
        this.defaultObject = frameObject;
    }

    private void init() {
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(inputFile);

            faceDetector = new FaceDetector.Builder(Globals.getAppContext())
                    .setTrackingEnabled(false)
                    .setMode(FaceDetector.FAST_MODE)
                    .build();

            if (!faceDetector.isOperational()) {
                faceDetectionEnabled = false;
            }
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(inputFile);

            FFmpegMediaMetadataRetriever metadataRetriever = new FFmpegMediaMetadataRetriever();
            metadataRetriever.setDataSource(inputFile);

            rs = RenderScript.create(Globals.getAppContext());
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
            rotateScript = new ScriptC_rotators(rs);

            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                MediaFormat format = mExtractor.getTrackFormat(i);

                String mimeType = format.getString(MediaFormat.KEY_MIME);

                width = format.getInteger(MediaFormat.KEY_WIDTH);
                height = format.getInteger(MediaFormat.KEY_HEIGHT);

                float frameRate = Float.parseFloat(metadataRetriever.extractMetadata(
                        FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE));

                int bitRate = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
                if (mimeType != null && mimeType.startsWith(VIDEO)) {
                    mExtractor.selectTrack(i);

                    MediaCodecInfo mediaCodecInfo = selectCodec(mimeType);
                    if (mediaCodecInfo == null) {
                        throw new RuntimeException("Failed to initialise codec");
                    }
                    switch (deviceOrientation) {
                        case Surface.ROTATION_0:
                        case Surface.ROTATION_180:
                            newWidth = height;
                            newHeight = width;
                            break;

                        case Surface.ROTATION_90:
                        case Surface.ROTATION_270:
                            newWidth = width;
                            newHeight = height;
                            break;
                    }
                    MediaFormat mediaFormat = MediaFormat.createVideoFormat(mimeType, newWidth, newHeight);
                    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
                    mediaFormat.setFloat(MediaFormat.KEY_FRAME_RATE, frameRate);
                    mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

                    mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    inputSurface = new CodecInputSurface(mEncoder.createInputSurface());
                    mediaMuxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    mEncoder.start();

                    mDecoder = MediaCodec.createDecoderByType(mimeType);
                    mDecoder.configure(format, null, null, 0 */
/* Decoder *//*
);
                    mDecoder.start();

                    inputSurface.makeCurrent();
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialise codec");
        }
    }

    */
/**
     * Returns the first codec capable of encoding the specified MIME type, or null if no
     * match was found.
     *//*

    private MediaCodecInfo selectCodec(String mimeType) throws IOException {
        MediaCodecList list = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos = list.getCodecInfos();
        for (MediaCodecInfo info : codecInfos) {
            if (info.isEncoder()) {
                mEncoder = MediaCodec.createByCodecName(info.getName());
                String[] types = info.getSupportedTypes();
                for (String type : types) {
                    if (type.equalsIgnoreCase(mimeType)) {
                        return info;
                    }
                }
            }
        }
        return null;
    }

    public void startProcessing() {
        init();
        MediaCodec.BufferInfo decoderBuff = new MediaCodec.BufferInfo();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        TextureRenderer textureRenderer = new TextureRenderer();
        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                int inputBufIndex = mDecoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = mDecoder.getInputBuffer(inputBufIndex);
                    int size = mExtractor.readSampleData(dstBuf, 0);
                    if (size >= 0) {
                        mDecoder.queueInputBuffer(inputBufIndex, 0, size,
                                mExtractor.getSampleTime(), mExtractor.getSampleFlags());
                        mExtractor.advance();
                    } else {
                        mDecoder.queueInputBuffer(inputBufIndex, 0, 0,
                                0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        sawInputEOS = true;
                    }
                }
            }

            final int outputBufIndex = mDecoder.dequeueOutputBuffer(decoderBuff, DEFAULT_TIMEOUT_US);
            if (outputBufIndex >= 0) {
                boolean endOfStream = (decoderBuff.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                if (!endOfStream){
                    boolean doRender = decoderBuff.size != 0;
                    if (doRender){
                        Image image = mDecoder.getOutputImage(outputBufIndex);
                        if (image != null) {
                            try {
                                byte[] frameData = quarterNV21(convertYUV420888ToNV21(image), image.getWidth(), image.getHeight());
                                byte[] data = getDataFromImage(image);

                                SparseArray<Face> faces = null;
                                if (faceDetectionEnabled) {
                                    Frame outputFrame = new Frame.Builder()
                                            .setImageData(ByteBuffer.wrap(frameData),
                                                    image.getWidth(), image.getHeight(), ImageFormat.NV21)
                                            .setId(frameIndex)
                                            .setTimestampMillis(System.currentTimeMillis())
                                            .setRotation(getDetectorOrientation())
                                            .build();
                                    faces = faceDetector.detect(outputFrame);
                                }

                                Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(data.length);
                                Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

                                Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
                                Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

                                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                                in.copyFromUnchecked(data);

                                yuvToRgbIntrinsic.setInput(in);
                                yuvToRgbIntrinsic.forEach(out);
                                out.copyTo(bitmap);
                                image.close();

                                Bitmap rotatedBitmap = null;
                                switch (deviceOrientation) {
                                    case Surface.ROTATION_0:
                                        if (sensorOrientation == SENSOR_ORIENTATION_DEFAULT_DEGREES) {
                                            rotatedBitmap = rotateBitmap(bitmap, 270);
                                        } else {
                                            rotatedBitmap = rotateBitmap(bitmap, 90);
                                        }
                                        bitmap.recycle();
                                        break;

                                    case Surface.ROTATION_90:
                                        Bitmap newBitmap = rotateBitmap(bitmap, 90);
                                        bitmap.recycle();
                                        rotatedBitmap = rotateBitmap(newBitmap, 90);
                                        newBitmap.recycle();
                                        break;

                                    default:
                                        rotatedBitmap = bitmap;
                                }

                                addCanvas(rotatedBitmap, faces);

                                drainEncoder(info, false);
                                textureRenderer.renderBitmap(rotatedBitmap.getWidth(),
                                        rotatedBitmap.getHeight(), rotatedBitmap, getMVP());
                                inputSurface.setPresentationTime(decoderBuff.presentationTimeUs * 1000);
                                inputSurface.swapBuffers();
                                frameIndex++;
                                rotatedBitmap.recycle();
                            } catch (Exception e) {
                                Log.d(TAG, "startProcessing: " + e.getMessage());
                            }
                        }
                        mDecoder.releaseOutputBuffer(outputBufIndex, false);
                    }
                }else {
                    drainEncoder(info, true);
                    sawOutputEOS = true;
                }
            }
        }
    }

    private float[] getMVP(){
        float[] mvp = new float[16];
        Matrix.setIdentityM(mvp, 0);
        Matrix.scaleM(mvp, 0, 1f, -1f, 1f);
        return mvp;
    }

    private byte[] convertYUV420888ToNV21(Image image) {
        byte[] data;
        ByteBuffer buffer0 = image.getPlanes()[0].getBuffer();
        ByteBuffer buffer2 = image.getPlanes()[2].getBuffer();
        int buffer0_size = buffer0.remaining();
        int buffer2_size = buffer2.remaining();
        data = new byte[buffer0_size + buffer2_size];
        buffer0.get(data, 0, buffer0_size);
        buffer2.get(data, buffer0_size, buffer2_size);
        return data;
    }

    private byte[] quarterNV21(byte[] data, int iWidth, int iHeight) {
        byte[] yuv = new byte[iWidth * iHeight * 3 / 2];
        // halve yuma
        int i = 0;
        for (int y = 0; y < iHeight; y++) {
            for (int x = 0; x < iWidth; x++) {
                yuv[i] = data[y * iWidth + x];
                i++;
            }
        }
        return yuv;
    }

    private void release() {
        try {
            if (mExtractor != null) {
                mExtractor.release();
                mExtractor = null;
            }
            if (mDecoder != null) {
                mDecoder.stop();
                mDecoder.release();
                mDecoder = null;
            }

            if (mEncoder != null) {
                mEncoder.stop();
                mEncoder.release();
                mEncoder = null;
            }

            if (inputSurface != null) {
                inputSurface.release();
                inputSurface = null;
            }

            if (mediaMuxer != null) {
                mediaMuxer.stop();
                mediaMuxer.release();
                mediaMuxer = null;
            }

            if (faceDetector != null) {
                faceDetector.release();
            }

        } catch (Exception e) {
            Log.d(TAG, "imageprocessor release: " + e.fillInStackTrace());
        }
        Message message = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(FrameUtil.COMPUTATION_SUCCESS_KEY, this.outputFile);
        bundle.putInt(FrameUtil.FACE_BLUR_COUNT, faceBlurCount);
        message.setData(bundle);
        handler.sendMessage(message);
    }

    // encode the bitmap to a new video file
    private void drainEncoder(MediaCodec.BufferInfo encoderBufferInfo, boolean endOfStream) {
        if (endOfStream){
            mEncoder.signalEndOfInputStream();
        }

        while (true){
            int encoderStatus = mEncoder.dequeueOutputBuffer(encoderBufferInfo, DEFAULT_TIMEOUT_US);
            if (encoderStatus >= 0){
                ByteBuffer outputBuffer = mEncoder.getOutputBuffer(encoderStatus);
                mediaMuxer.writeSampleData(mTrackIndex, outputBuffer, encoderBufferInfo);
                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((encoderBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }else if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) {
                    break;
                }
            }else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mEncoder.getOutputFormat();
                mTrackIndex = mediaMuxer.addTrack(newFormat);
                mediaMuxer.start();
            }
        }
    }

    private void addCanvas(Bitmap rotatedBitmap, SparseArray<Face> faces) {
        Canvas canvas = new Canvas(rotatedBitmap);
        canvas.drawBitmap(rotatedBitmap, 0, 0, null);

        double viewWidth = canvas.getWidth();
        double viewHeight = canvas.getHeight();
        double imageWidth = rotatedBitmap.getWidth();
        double imageHeight = rotatedBitmap.getHeight();
        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);

        List<BoundingBox> boxes = defaultObject.getBoundingBox();
        if (boxes != null && boxes.size() > frameIndex) {
            BoundingBox boundingBox = boxes.get(frameIndex);
            com.visym.collector.model.Frame frame = boundingBox.getFrame();
            int x1 = frame.getX();
            int y1 = frame.getY();

            int x2 = x1 + frame.getWidth();
            int y3 = y1 + frame.getHeight();

            if (faces != null && faces.size() > 0) {
                if (faceBlurCount < faces.size()) {
                    faceBlurCount = faces.size();
                }
                for (int i = 0; i < faces.size(); i++) {
                    com.google.android.gms.vision.face.Face face = faces.valueAt(i);
                    float left = (float) (face.getPosition().x * scale);
                    float top = (float) (face.getPosition().y * scale);
                    float right = (float) scale * (face.getPosition().x + face.getWidth());
                    float bottom = (float) scale * (face.getPosition().y + face.getHeight());

                    if ((left < x1 && right < x1) || (top < y1 && bottom < y1) || (right > x2 && left > x2)
                            || (top > y3 && bottom > y3)) {
                        Rect rect = new Rect((int) left, (int) top, (int) right, (int) bottom);
                        canvas.drawBitmap(BitmapFactory.decodeResource(Globals.getAppContext().getResources(),
                                R.drawable.circle_cropped), null, rect, null);
                    }
                }
            }
        }
    }

    private Allocation getFromRotateAllocation(Bitmap bitmap) {
        int targetHeight = bitmap.getWidth();
        int targetWidth = bitmap.getHeight();
        if (targetHeight != preRotateHeight || targetWidth != preRotateWidth) {
            preRotateHeight = targetHeight;
            preRotateWidth = targetWidth;
            fromRotateAllocation = Allocation.createFromBitmap(rs, bitmap,
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SCRIPT);
        }
        return fromRotateAllocation;
    }

    private Allocation getToRotateAllocation(Bitmap bitmap) {
        int targetHeight = bitmap.getWidth();
        int targetWidth = bitmap.getHeight();
        if (targetHeight != preRotateHeight || targetWidth != preRotateWidth) {
            toRotateAllocation = Allocation.createFromBitmap(rs, bitmap,
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SCRIPT);
        }
        return toRotateAllocation;
    }


    private Bitmap rotateBitmap(Bitmap bitmap, int angle) {
        Bitmap.Config config = bitmap.getConfig();
        int targetHeight = bitmap.getWidth();
        int targetWidth = bitmap.getHeight();

        rotateScript.set_inWidth(bitmap.getWidth());
        rotateScript.set_inHeight(bitmap.getHeight());

        Allocation sourceAllocation = getFromRotateAllocation(bitmap);
        sourceAllocation.copyFrom(bitmap);
        rotateScript.set_inImage(sourceAllocation);

        Bitmap target = Bitmap.createBitmap(targetWidth, targetHeight, config);
        final Allocation targetAllocation = getToRotateAllocation(target);
        if (angle == 90) {
            rotateScript.forEach_rotate_90_clockwise(targetAllocation, targetAllocation);
        } else {
            rotateScript.forEach_rotate_270_clockwise(targetAllocation, targetAllocation);
        }

        targetAllocation.copyTo(target);

        return target;
    }

    private static byte[] getDataFromImage(Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];

        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;
                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;
                    break;
            }

            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();

            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }

    private int getDetectorOrientation() {
        if (sensorOrientation == SENSOR_ORIENTATION_DEFAULT_DEGREES) {
            switch (deviceOrientation) {
                case Surface.ROTATION_90:
                    return Surface.ROTATION_180;

                case Surface.ROTATION_270:
                    return Surface.ROTATION_0;

                default:
                    return 1;
            }
        } else {
            switch (deviceOrientation) {
                case Surface.ROTATION_0:
                    return Surface.ROTATION_270;

                case Surface.ROTATION_90:
                    return Surface.ROTATION_180;

                case Surface.ROTATION_270:
                    return Surface.ROTATION_0;

                default:
                    return 1;
            }
        }
    }

    @Override
    public void run() {
        try {
            startProcessing();
        } catch (Exception ex) {
            Log.d(TAG, "run: " + ex.getCause());
        }finally {
            release();
        }
    }

    public void stopProcessing() {
        sawOutputEOS = true;
    }
}
*/
