package com.mask.screencapture.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.util.DisplayMetrics;

import com.mask.screencapture.interfaces.ScreenCaptureCallback;
import com.mask.screencapture.interfaces.ScreenCaptureNotificationEngine;
import com.mask.screencapture.service.ScreenCaptureService;

/**
 * 屏幕截图 帮助类
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

    private ScreenCaptureNotificationEngine notificationEngine;

    private MediaProjectionManager mediaProjectionManager;
    private DisplayMetrics displayMetrics;

    private ServiceConnection serviceConnection;
    private ScreenCaptureService screenCaptureService;

    /**
     * 设置 屏幕截图通知引擎
     *
     * @param notificationEngine notificationEngine
     */
    public void setNotificationEngine(ScreenCaptureNotificationEngine notificationEngine) {
        this.notificationEngine = notificationEngine;
    }

    /**
     * 开始屏幕截图
     *
     * @param activity activity
     */
    public void startCapture(Activity activity) {
        if (mediaProjectionManager != null) {
            return;
        }

        // 启动系统截屏
        mediaProjectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager != null) {
            activity.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        }

        // 此处宽高需要获取屏幕完整宽高，否则截屏图片会有白/黑边
        displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

        // 绑定服务
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (service instanceof ScreenCaptureService.ScreenCaptureBinder) {
                    screenCaptureService = ((ScreenCaptureService.ScreenCaptureBinder) service).getService();
                    screenCaptureService.setNotificationEngine(notificationEngine);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                screenCaptureService = null;
            }
        };

        ScreenCaptureService.bindService(activity, serviceConnection);
    }

    /**
     * 停止屏幕截图
     *
     * @param context context
     */
    public void stopCapture(Context context) {
        screenCaptureService = null;

        if (serviceConnection != null) {
            ScreenCaptureService.unbindService(context, serviceConnection);
            serviceConnection = null;
        }

        displayMetrics = null;

        mediaProjectionManager = null;
    }

    /**
     * 解析屏幕截图结果(onActivityResult中调用)
     *
     * @param requestCode requestCode
     * @param resultCode  resultCode
     * @param data        data
     */
    public void parseResult(int requestCode, int resultCode, Intent data) {
        if (screenCaptureService == null) {
            return;
        }
        if (requestCode != REQUEST_CODE) {
            return;
        }
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        screenCaptureService.start(resultCode, data, displayMetrics);
    }

    /**
     * 屏幕截图
     *
     * @param callback callback
     */
    public void capture(ScreenCaptureCallback callback) {
        if (screenCaptureService == null) {
            callback.onFail();
            return;
        }
        screenCaptureService.capture(callback);
    }

}
