package com.mask.mediaprojection.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.DisplayMetrics;

import com.mask.mediaprojection.interfaces.MediaProjectionNotificationEngine;
import com.mask.mediaprojection.interfaces.MediaRecorderCallback;
import com.mask.mediaprojection.interfaces.ScreenCaptureCallback;
import com.mask.mediaprojection.utils.FileUtils;
import com.mask.mediaprojection.utils.MediaProjectionHelper;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * 媒体投影 Service
 * Created by lishilin on 2020/03/19
 */
public class MediaProjectionService extends Service {

    private static final int ID_MEDIA_PROJECTION = MediaProjectionHelper.REQUEST_CODE;

    private DisplayMetrics displayMetrics;
    private boolean isScreenCaptureEnable;// 是否可以屏幕截图
    private boolean isMediaRecorderEnable;// 是否可以媒体录制

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;

    private VirtualDisplay virtualDisplayImageReader;
    private ImageReader imageReader;
    private boolean isImageAvailable;

    private VirtualDisplay virtualDisplayMediaRecorder;
    private MediaRecorder mediaRecorder;
    private File mediaFile;
    private boolean isMediaRecording;

    private MediaProjectionNotificationEngine notificationEngine;

    public class MediaProjectionBinder extends Binder {

        public MediaProjectionService getService() {
            return MediaProjectionService.this;
        }

    }

    /**
     * 绑定Service
     *
     * @param context           context
     * @param serviceConnection serviceConnection
     */
    public static void bindService(Context context, ServiceConnection serviceConnection) {
        Intent intent = new Intent(context, MediaProjectionService.class);
        context.bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE);
    }

    /**
     * 解绑Service
     *
     * @param context           context
     * @param serviceConnection serviceConnection
     */
    public static void unbindService(Context context, ServiceConnection serviceConnection) {
        context.unbindService(serviceConnection);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MediaProjectionBinder();
    }

    @Override
    public void onDestroy() {
        destroy();
        super.onDestroy();
    }

    /**
     * 销毁
     */
    private void destroy() {
        stopImageReader();

        stopMediaRecorder();

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        if (mediaProjectionManager != null) {
            mediaProjectionManager = null;
        }

        stopForeground(true);
    }

    /**
     * 结束 屏幕截图
     */
    private void stopImageReader() {
        isImageAvailable = false;

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        if (virtualDisplayImageReader != null) {
            virtualDisplayImageReader.release();
            virtualDisplayImageReader = null;
        }
    }

    /**
     * 结束 媒体录制
     */
    private void stopMediaRecorder() {
        stopRecording(null);

        if (virtualDisplayMediaRecorder != null) {
            virtualDisplayMediaRecorder.release();
            virtualDisplayMediaRecorder = null;
        }
    }

    /**
     * 显示通知栏
     */
    private void showNotification() {
        if (notificationEngine == null) {
            return;
        }

        Notification notification = notificationEngine.getNotification();

        startForeground(ID_MEDIA_PROJECTION, notification);
    }

    /**
     * 创建 屏幕截图
     */
    private void createImageReader() {
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        int densityDpi = displayMetrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                isImageAvailable = true;
            }
        }, null);

        virtualDisplayImageReader = mediaProjection.createVirtualDisplay("ScreenCapture",
                width, height, densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);
    }

    /**
     * 创建 媒体录制
     */
    private void createMediaRecorder() {
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        int densityDpi = displayMetrics.densityDpi;

        // 创建保存路径
        final File dirFile = FileUtils.getCacheMovieDir(this);
        boolean mkdirs = dirFile.mkdirs();
        // 创建保存文件
        mediaFile = new File(dirFile, FileUtils.getDateName("MediaRecorder") + ".mp4");

        // 调用顺序不能乱
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(mediaFile.getAbsolutePath());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoSize(width, height);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoEncodingBitRate(5 * width * height);

        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (virtualDisplayMediaRecorder == null) {
            virtualDisplayMediaRecorder = mediaProjection.createVirtualDisplay("MediaRecorder",
                    width, height, densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecorder.getSurface(), null, null);
        } else {
            virtualDisplayMediaRecorder.setSurface(mediaRecorder.getSurface());
        }
    }

    /**
     * 设置 通知引擎
     *
     * @param notificationEngine notificationEngine
     */
    public void setNotificationEngine(MediaProjectionNotificationEngine notificationEngine) {
        this.notificationEngine = notificationEngine;
    }

    /**
     * 创建VirtualDisplay
     *
     * @param resultCode            resultCode
     * @param data                  data
     * @param displayMetrics        displayMetrics
     * @param isScreenCaptureEnable 是否可以屏幕截图
     * @param isMediaRecorderEnable 是否可以媒体录制
     */
    public void createVirtualDisplay(int resultCode, Intent data, DisplayMetrics displayMetrics, boolean isScreenCaptureEnable, boolean isMediaRecorderEnable) {
        this.displayMetrics = displayMetrics;
        this.isScreenCaptureEnable = isScreenCaptureEnable;
        this.isMediaRecorderEnable = isMediaRecorderEnable;

        if (data == null) {
            stopSelf();
            return;
        }

        showNotification();

        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager == null) {
            stopSelf();
            return;
        }

        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            stopSelf();
            return;
        }

        if (isScreenCaptureEnable) {
            createImageReader();
        }
    }

    /**
     * 屏幕截图
     *
     * @param callback callback
     */
    public void capture(ScreenCaptureCallback callback) {
        if (!isScreenCaptureEnable) {
            callback.onFail();
            return;
        }
        if (imageReader == null) {
            callback.onFail();
            return;
        }
        if (!isImageAvailable) {
            callback.onFail();
            return;
        }
        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            callback.onFail();
            return;
        }

        // 获取数据
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane plane = image.getPlanes()[0];
        final ByteBuffer buffer = plane.getBuffer();

        // 重新计算Bitmap宽度，防止Bitmap显示错位
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        int bitmapWidth = width + rowPadding / pixelStride;

        // 创建Bitmap
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        // 释放资源
        image.close();

        // 裁剪Bitmap，因为重新计算宽度原因，会导致Bitmap宽度偏大
        Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        bitmap.recycle();

        isImageAvailable = false;

        callback.onSuccess(result);
    }

    /**
     * 开始 媒体录制
     */
    public void startRecording() {
        if (!isMediaRecorderEnable) {
            return;
        }
        if (isMediaRecording) {
            return;
        }

        createMediaRecorder();

        mediaRecorder.start();

        isMediaRecording = true;
    }

    /**
     * 停止 媒体录制
     *
     * @param callback callback
     */
    public void stopRecording(MediaRecorderCallback callback) {
        if (!isMediaRecorderEnable) {
            if (callback != null) {
                callback.onFail();
            }
        }

        if (mediaRecorder == null) {
            if (callback != null) {
                callback.onFail();
            }
            return;
        }
        if (!isMediaRecording) {
            if (callback != null) {
                callback.onFail();
            }
            return;
        }

        mediaRecorder.stop();
        mediaRecorder.reset();
        mediaRecorder.release();

        mediaRecorder = null;

        if (callback != null) {
            callback.onSuccess(mediaFile);
        }
        mediaFile = null;

        isMediaRecording = false;
    }

}
