package com.mask.screencapture.interfaces;

import android.graphics.Bitmap;

/**
 * 截屏回调
 * Created by lishilin on 2020/03/18
 */
public abstract class ScreenCaptureCallback {

    /**
     * 成功
     *
     * @param bitmap 截屏后的Bitmap
     */
    public void onSuccess(Bitmap bitmap) {

    }

    /**
     * 失败
     */
    public void onFail() {

    }

}
