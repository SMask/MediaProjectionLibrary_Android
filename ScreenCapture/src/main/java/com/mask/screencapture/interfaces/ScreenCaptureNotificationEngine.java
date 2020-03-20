package com.mask.screencapture.interfaces;

import android.app.Notification;

/**
 * 屏幕截图通知引擎
 * Created by lishilin on 2020/03/19
 */
public interface ScreenCaptureNotificationEngine {

    /**
     * 获取 Notification
     *
     * @return Notification
     */
    Notification getNotification();

}
