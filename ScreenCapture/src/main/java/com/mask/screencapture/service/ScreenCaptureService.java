package com.mask.screencapture.service;

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
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.DisplayMetrics;

import com.mask.screencapture.interfaces.ScreenCaptureCallback;
import com.mask.screencapture.interfaces.ScreenCaptureNotificationEngine;
import com.mask.screencapture.utils.ScreenCaptureHelper;

import java.nio.ByteBuffer;

/**
 * 屏幕截图 Service
 * Created by lishilin on 2020/03/19
 */
public class ScreenCaptureService extends Service {

    private static final int ID_SCREEN_CAPTURE = ScreenCaptureHelper.REQUEST_CODE;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private boolean isImageAvailable;

    private ScreenCaptureNotificationEngine notificationEngine;

    public class ScreenCaptureBinder extends Binder {

        public ScreenCaptureService getService() {
            return ScreenCaptureService.this;
        }

    }

    /**
     * 绑定Service
     *
     * @param context           context
     * @param serviceConnection serviceConnection
     */
    public static void bindService(Context context, ServiceConnection serviceConnection) {
        Intent intent = new Intent(context, ScreenCaptureService.class);
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
        return new ScreenCaptureBinder();
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    /**
     * 结束
     */
    private void stop() {
        isImageAvailable = false;
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
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
     * 显示通知栏
     */
    private void showNotification() {
        if (notificationEngine == null) {
            return;
        }

        Notification notification = notificationEngine.getNotification();

        startForeground(ID_SCREEN_CAPTURE, notification);
    }

    /**
     * 设置 屏幕截图通知引擎
     *
     * @param notificationEngine notificationEngine
     */
    public void setNotificationEngine(ScreenCaptureNotificationEngine notificationEngine) {
        this.notificationEngine = notificationEngine;
    }

    /**
     * 开始
     *
     * @param resultCode     resultCode
     * @param data           data
     * @param displayMetrics displayMetrics
     */
    public void start(int resultCode, Intent data, DisplayMetrics displayMetrics) {
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        int densityDpi = displayMetrics.densityDpi;
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

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);

        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                width, height, densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                isImageAvailable = true;
            }
        }, null);
    }

    /**
     * 屏幕截图
     *
     * @param callback callback
     */
    public void capture(ScreenCaptureCallback callback) {
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

}
