package com.mask.mediaprojection.interfaces;

import android.graphics.Bitmap;

/**
 * 屏幕截图回调
 * Created by lishilin on 2020/03/18
 */
public abstract class ScreenCaptureCallback {

    /**
     * 成功
     *
     * @param bitmap 截图后的Bitmap
     */
    public void onSuccess(Bitmap bitmap) {

    }

    /**
     * 失败
     */
    public void onFail() {

    }

}
