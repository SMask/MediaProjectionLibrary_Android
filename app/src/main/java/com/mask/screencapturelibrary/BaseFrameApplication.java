package com.mask.screencapturelibrary;

import android.app.Application;
import android.os.Handler;

public abstract class BaseFrameApplication extends Application {

    public static BaseFrameApplication instance;

    private final Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        LogUtil.i("BaseFrameApplication onCreate");
        onInitData();
        handler.post(new Runnable() {
            @Override
            public void run() {
                onInitDataThread();
            }
        });
    }

    /**
     * 获取崩溃后启动的Activity Class
     *
     * @return 崩溃后启动的Activity Class
     */
    protected abstract Class getCrashLauncherActivity();

    /**
     * CrashHandler uncaughtException
     *
     * @param thread thread
     * @param ex     ex
     */
    protected void uncaughtException(Thread thread, Throwable ex) {

    }

    public static BaseFrameApplication getInstance() {
        return instance;
    }

    /**
     * 初始化数据
     */
    protected void onInitData() {
        LogUtil.i("BaseFrameApplication onInitData");
    }

    /**
     * 线程初始化数据
     */
    protected void onInitDataThread() {
        LogUtil.i("BaseFrameApplication onInitDataThread");
    }

}
