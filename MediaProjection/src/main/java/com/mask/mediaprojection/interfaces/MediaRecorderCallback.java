package com.mask.mediaprojection.interfaces;

import java.io.File;

/**
 * 媒体录制回调
 * Created by lishilin on 2020/03/20
 */
public abstract class MediaRecorderCallback {

    /**
     * 成功
     *
     * @param file 录制后的File
     */
    public void onSuccess(File file) {

    }

    /**
     * 失败
     */
    public void onFail() {

    }

}
