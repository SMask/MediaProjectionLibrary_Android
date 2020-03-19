package com.mask.screencapture.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;

import com.mask.screencapture.interfaces.ScreenCaptureCallback;

import java.nio.ByteBuffer;

/**
 * 截屏 帮助类
 * Created by lishilin on 2020/03/18
 */
public class ScreenCaptureHelper {

    public static final int REQUEST_CODE = 10086;

    private static class InstanceHolder {
        private static final ScreenCaptureHelper instance = new ScreenCaptureHelper();
    }

    public static ScreenCaptureHelper getInstance() {
        return InstanceHolder.instance;
    }

    private ScreenCaptureHelper() {
        super();
    }

    private int width;
    private int height;
    private int densityDpi;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    /**
     * 开始屏幕截图
     *
     * @param activity activity
     */
    public void startCapture(Activity activity) {
        // 此处宽高需要获取屏幕完整宽高，否则截屏图片会有白/黑边
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;
        densityDpi = displayMetrics.densityDpi;

        mediaProjectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager != null) {
            activity.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        }
    }

    /**
     * 停止屏幕截图
     */
    public void stopCapture() {
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
    }

    /**
     * 解析屏幕截图结果
     *
     * @param requestCode requestCode
     * @param resultCode  resultCode
     * @param data        data
     */
    public void parseResult(int requestCode, int resultCode, Intent data) {
        if (mediaProjectionManager == null) {
            return;
        }
        if (requestCode != REQUEST_CODE) {
            return;
        }
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            return;
        }

        ImageReader imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);

        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                width, height, densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                ScreenCaptureHelper.this.imageReader = reader;
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

        callback.onSuccess(result);
    }

}
