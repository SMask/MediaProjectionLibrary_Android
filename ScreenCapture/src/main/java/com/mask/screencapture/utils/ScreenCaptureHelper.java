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

    public void startCapture(Activity activity) {
        DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;
        densityDpi = displayMetrics.densityDpi;

        mediaProjectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager != null) {
            activity.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        }
    }

    public void parseResult(int requestCode, int resultCode, Intent data, final ScreenCaptureCallback callback) {
        if (mediaProjectionManager == null) {
            callback.onFail();
            return;
        }
        if (requestCode != REQUEST_CODE) {
            callback.onFail();
            return;
        }
        if (resultCode != Activity.RESULT_OK) {
            callback.onFail();
            return;
        }

        final MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            callback.onFail();
            return;
        }

        ImageReader imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3);

        final VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                width, height, densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image == null) {
                    callback.onFail();
                    return;
                }

                int width = image.getWidth();
                int height = image.getHeight();
                final Image.Plane plane = image.getPlanes()[0];
                final ByteBuffer buffer = plane.getBuffer();
                int pixelStride = plane.getPixelStride();
                int rowStride = plane.getRowStride();
                int rowPadding = rowStride - pixelStride * width;

                Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);

                image.close();
                virtualDisplay.release();
                mediaProjection.stop();

                callback.onSuccess(bitmap);
            }
        }, null);
    }

}
