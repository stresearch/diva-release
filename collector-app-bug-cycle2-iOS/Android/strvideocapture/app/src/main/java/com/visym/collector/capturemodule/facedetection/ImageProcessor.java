package com.visym.collector.capturemodule.facedetection;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.visym.collector.rs.ScriptC_rotators;
import com.visym.collector.utils.CameraUtil;
import com.visym.collector.utils.FrameUtil;
import com.visym.collector.utils.Globals;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;

public class ImageProcessor implements Runnable{
    private static final String VIDEO = "video/";
    private static final String TAG = "VideoDecoder";
    private static final long DEFAULT_TIMEOUT_US = 1000;
    private final String inputFile;
    private final String outputFile;
    private MediaCodec mDecoder;

    private MediaExtractor mExtractor;
    private RenderScript renderScript;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private int width;
    private int height;

    private static final int COLOR_FormatI420 = 1;
    private static final int COLOR_FormatNV21 = 2;

    private MediaCodec mEncoder;
    private MediaMuxer mediaMuxer;
    private int mTrackIndex;
    private ScriptC_rotators rotateScript;


    private int preRotateHeight;
    private int preRotateWidth;
    private Allocation fromRotateAllocation;
    private Allocation toRotateAllocation;
    private int frameIndex;
    private Paint myRectPaint;
    private boolean faceDetectionEnabled = true;
    private FaceDetector faceDetector;
    private int deviceOrientation;
    private int sensorOrientation;
    private ScriptIntrinsicBlur scriptIntrinsicBlur;
    private Handler handler;

    public ImageProcessor(Handler handler, String inputFile, String outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.handler = handler;
    }

    public void setDeviceRotation(int rotation) {
        this.deviceOrientation = rotation;
    }

    public void setSensorRotation(int orientation) {
        this.sensorOrientation = orientation;
    }

    private void init() {
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(inputFile);

            myRectPaint = new Paint();
            myRectPaint.setColor(Color.parseColor("#F5F5F5"));
            myRectPaint.setStyle(Paint.Style.FILL);

            faceDetector = new FaceDetector.Builder(Globals.getAppContext())
                    .setTrackingEnabled(false)
                    .setMode(FaceDetector.FAST_MODE)
                    .build();

            if (!faceDetector.isOperational()) {
                faceDetectionEnabled = false;
            }


            renderScript = RenderScript.create(Globals.getAppContext());
            scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(renderScript, Element.U8_4(renderScript));
            rotateScript = new ScriptC_rotators(renderScript);
            CamcorderProfile profile = CameraUtil.getCameraProfile(0);

            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                MediaFormat format = mExtractor.getTrackFormat(i);

                String mimeType = format.getString(MediaFormat.KEY_MIME);

                width = format.getInteger(MediaFormat.KEY_WIDTH);
                height = format.getInteger(MediaFormat.KEY_HEIGHT);

                int frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);

                if (mimeType.startsWith(VIDEO)) {
                    mExtractor.selectTrack(i);
                    mDecoder = MediaCodec.createDecoderByType(mimeType);
                    mDecoder.configure(format, null, null, 0 /* Decoder */);
                    mDecoder.start();

                    MediaCodecInfo mediaCodecInfo = selectCodec(mimeType);
                    if (mediaCodecInfo == null) {
                        throw new RuntimeException("Failed to initialise codec");
                    }
                    int colorFormat = selectColorFormat(mediaCodecInfo, mimeType);
                    int newWidth = 0, newHeight = 0;
                    switch (sensorOrientation) {
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
                    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, profile.videoBitRate);
                    mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
                    mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatYUV420SemiPlanar);
                    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
                    mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    mEncoder.start();

                    mediaMuxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialise codec");
        }
    }


    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no
     * match was found.
     */
    private MediaCodecInfo selectCodec(String mimeType) throws IOException {
        mEncoder = MediaCodec.createEncoderByType(mimeType);
        MediaCodecInfo codecInfo = mEncoder.getCodecInfo();
        String[] types = codecInfo.getSupportedTypes();
        for (String type : types) {
            if (type.equalsIgnoreCase(mimeType)) {
                return codecInfo;
            }
        }
        return null;
    }

    /**
     * Returns a color format that is supported by the codec and by this test code.  If no
     * match is found, this throws a test failure -- the set of formats known to the test
     * should be expanded for new platforms.
     */
    private int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        return 0;   // not reached
    }

    /**
     * Returns true if this is a color format that this test code understands (i.e. we know how
     * to read and generate frames in this format).
     */
    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    public void startProcessing() {
        init();

        MediaCodec.BufferInfo decoderBufferInfo = new MediaCodec.BufferInfo();
        MediaCodec.BufferInfo encoderBufferInfo = new MediaCodec.BufferInfo();

        boolean sawOutputEOS = false;
        boolean sawInputEOS = false;

        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                int inputBufferId = mDecoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                if (inputBufferId >= 0) {
                    ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputBufferId);
                    int sampleSize = mExtractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        mDecoder.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        sawInputEOS = true;
                    } else {
                        long presentationTimeUs = mExtractor.getSampleTime();
                        mDecoder.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0);
                        mExtractor.advance();
                    }
                }
            }

            int outputBufferId = mDecoder.dequeueOutputBuffer(decoderBufferInfo, DEFAULT_TIMEOUT_US);
            if (outputBufferId >= 0) {
                if ((decoderBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;
                }

                boolean doRender = (decoderBufferInfo.size != 0);
                if (doRender) {
                    Image image = mDecoder.getOutputImage(outputBufferId);
                    if (image != null) {
                        try {
                            frameIndex++;
                            byte[] frameData = quarterNV21(convertYUV420888ToNV21(image), image.getWidth(), image.getHeight());
                            byte[] data = getDataFromImage(image, COLOR_FormatNV21);
                            image.close();

                            SparseArray<Face> faces = null;
                            if (faceDetectionEnabled) {
                                Frame outputFrame = new Frame.Builder()
                                        .setImageData(ByteBuffer.wrap(frameData),
                                                width, height, ImageFormat.NV21)
                                        .setId(frameIndex)
                                        .setTimestampMillis(System.currentTimeMillis())
                                        .setRotation(getDetectorOrientation())
                                        .build();

                                faces = faceDetector.detect(outputFrame);
                            }

                            Type.Builder yuvType = new Type.Builder(renderScript, Element.U8(renderScript)).setX(data.length);
                            Allocation in = Allocation.createTyped(renderScript, yuvType.create(), Allocation.USAGE_SCRIPT);

                            Type.Builder rgbaType = new Type.Builder(renderScript, Element.RGBA_8888(renderScript)).setX(width).setY(height);
                            Allocation out = Allocation.createTyped(renderScript, rgbaType.create(), Allocation.USAGE_SCRIPT);

                            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                            in.copyFromUnchecked(data);

                            yuvToRgbIntrinsic.setInput(in);
                            yuvToRgbIntrinsic.forEach(out);
                            out.copyTo(bitmap);

                            encodeBitmaps(bitmap, encoderBufferInfo, decoderBufferInfo.presentationTimeUs, faces);
                        } catch (Exception e) {
                            Log.d(TAG, "startProcessing: " + e.getCause());
                        }
                    }
                    mDecoder.releaseOutputBuffer(outputBufferId, false);
                }
            }
        }
        Log.d(TAG, "run: length " + frameIndex);
        release();
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
        // Converting YUV_420_888 data to NV21.
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

        if (mExtractor != null) {
            mExtractor.release();
            mExtractor = null;
        }

        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }

        if (faceDetector != null) {
            faceDetector.release();
        }

        Message message = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(FrameUtil.COMPUTATION_SUCCESS_KEY, this.outputFile);
        message.setData(bundle);
        handler.sendMessage(message);
    }

    // encode the bitmap to a new video file
    private void encodeBitmaps(Bitmap bitmap, MediaCodec.BufferInfo encoderBufferInfo,
                               long presentationTimeUs, SparseArray<Face> faces) {
        Bitmap rotatedBitmap = null;
        switch (sensorOrientation) {
            case Surface.ROTATION_0:
                rotatedBitmap = rotateBitmap(bitmap, 270);
                break;

            case Surface.ROTATION_90:
                Bitmap newBitmap = rotateBitmap(bitmap, 90);
                bitmap.recycle();
                rotatedBitmap = rotateBitmap(newBitmap, 90);
                break;

            default:
                rotatedBitmap = bitmap;
        }

        addCanvas(rotatedBitmap, faces);

        byte[] bytes = getNV21(rotatedBitmap.getWidth(), rotatedBitmap.getHeight(), rotatedBitmap);
        int inputBufIndex = mEncoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
        if (inputBufIndex >= 0) {
            ByteBuffer inputBuffer = mEncoder.getInputBuffer(inputBufIndex);
            if (inputBuffer != null) {
                inputBuffer.rewind();
                inputBuffer.put(bytes);
                mEncoder.queueInputBuffer(inputBufIndex, 0, bytes.length,
                        presentationTimeUs, 0);
            }
        }
        int encoderStatus = mEncoder.dequeueOutputBuffer(encoderBufferInfo, DEFAULT_TIMEOUT_US);
        if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat newFormat = mEncoder.getOutputFormat();
            mTrackIndex = mediaMuxer.addTrack(newFormat);
            mediaMuxer.start();
        } else if (encoderBufferInfo.size != 0) {
            ByteBuffer outputBuffer = mEncoder.getOutputBuffer(encoderStatus);
            if (outputBuffer != null) {
                outputBuffer.position(encoderBufferInfo.offset);
                outputBuffer.limit(encoderBufferInfo.offset + encoderBufferInfo.size);
                mediaMuxer.writeSampleData(mTrackIndex, outputBuffer, encoderBufferInfo);
                mEncoder.releaseOutputBuffer(encoderStatus, false);
            }
            if ((encoderBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                mEncoder.signalEndOfInputStream();
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

        if (faces != null && faces.size() > 0) {
            for (int i = 0; i < faces.size(); i++) {
                com.google.android.gms.vision.face.Face face = faces.valueAt(i);

                float left = (float) (face.getPosition().x * scale);
                float top = (float) (face.getPosition().y * scale);
                float right = (float) scale * (face.getPosition().x + face.getWidth());
                float bottom = (float) scale * (face.getPosition().y + face.getHeight());

//                Bitmap bitmap = createBlurredBitmap(left, top, right, bottom);
//                canvas.drawBitmap(bitmap, left, top, null);
                canvas.drawRect(left, top, right, bottom, myRectPaint);
            }
        }
    }

    private Bitmap createBlurredBitmap(float left, float top, float right, float bottom) {
        Bitmap srcBitmap = Bitmap.createBitmap((int) (right - left), (int) (bottom - top),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(srcBitmap);
//        myRectPaint.setColor(Color.parseColor("#F5F5F5"));
        myRectPaint.setColor(Color.RED);
        canvas.drawRect(0F, 0F, right - left, bottom - top, myRectPaint);

        Bitmap outputBitmap = Bitmap.createBitmap(srcBitmap);
        Allocation allocationIn = Allocation.createFromBitmap(renderScript, srcBitmap);
        Allocation allocationOut = Allocation.createFromBitmap(renderScript, outputBitmap);

        scriptIntrinsicBlur.setRadius(25.0f);
        scriptIntrinsicBlur.setInput(allocationIn);
        scriptIntrinsicBlur.forEach(allocationOut);

        allocationOut.copyTo(outputBitmap);

        return outputBitmap;
    }

    private Bitmap createBlur(Bitmap srcBitmap, float blurOpacity) {
        Bitmap copyBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap outputBitmap = Bitmap.createBitmap(copyBitmap);

        Allocation allocationIn = Allocation.createFromBitmap(renderScript, srcBitmap);
        Allocation allocationOut = Allocation.createFromBitmap(renderScript, outputBitmap);

        scriptIntrinsicBlur.setRadius(blurOpacity);
        scriptIntrinsicBlur.setInput(allocationIn);
        scriptIntrinsicBlur.forEach(allocationOut);

        allocationOut.copyTo(outputBitmap);

        return outputBitmap;
    }

    private Allocation getFromRotateAllocation(Bitmap bitmap) {
        int targetHeight = bitmap.getWidth();
        int targetWidth = bitmap.getHeight();
        if (targetHeight != preRotateHeight || targetWidth != preRotateWidth) {
            preRotateHeight = targetHeight;
            preRotateWidth = targetWidth;
            fromRotateAllocation = Allocation.createFromBitmap(renderScript, bitmap,
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SCRIPT);
        }
        return fromRotateAllocation;
    }

    private Allocation getToRotateAllocation(Bitmap bitmap) {
        int targetHeight = bitmap.getWidth();
        int targetWidth = bitmap.getHeight();
        if (targetHeight != preRotateHeight || targetWidth != preRotateWidth) {
            toRotateAllocation = Allocation.createFromBitmap(renderScript, bitmap,
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

    private byte[] getNV21(int inputWidth, int inputHeight, Bitmap bitmap) {
        int[] argb = new int[inputWidth * inputHeight];
        bitmap.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);
        byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight);
        bitmap.recycle();
        return yuv;
    }

    private void encodeYUV420SP(byte[] yuv420sp, int[] rgb, int width, int height) {

        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                //a = (aRGB[index] & 0xff000000) >> 24; //not using it right now
                R = (rgb[index] & 0xff0000) >> 16;
                G = (rgb[index] & 0xff00) >> 8;
                B = (rgb[index] & 0xff);


                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));

                }

                index++;
            }
        }
    }

    private boolean isColorFormatSupported(int colorFormat, MediaCodecInfo.CodecCapabilities caps) {
        for (int c : caps.colorFormats) {
            if (c == colorFormat) {
                return true;
            }
        }
        return false;
    }

    private static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
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
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
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

    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }

    private int getDetectorOrientation() {
        switch (sensorOrientation) {
            case Surface.ROTATION_90:
                return Surface.ROTATION_180;

            case Surface.ROTATION_270:
                return 0;

            default:
                return 1;
        }
    }

    @Override
    public void run() {
        startProcessing();
    }
}
