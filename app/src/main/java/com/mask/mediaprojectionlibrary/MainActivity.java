package com.mask.mediaprojectionlibrary;

import android.Manifest;
import android.app.Notification;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.mask.mediaprojection.interfaces.MediaProjectionNotificationEngine;
import com.mask.mediaprojection.interfaces.MediaRecorderCallback;
import com.mask.mediaprojection.interfaces.ScreenCaptureCallback;
import com.mask.mediaprojection.utils.MediaProjectionHelper;
import com.mask.photo.interfaces.SaveBitmapCallback;
import com.mask.photo.utils.BitmapUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private View layout_root;
    private View layout_group_1;
    private View layout_group_2;
    private View layout_group_3;
    private View layout_space;
    private Button btn_service_start;
    private Button btn_service_stop;
    private Button btn_screen_capture;
    private Button btn_media_recorder_start;
    private Button btn_media_recorder_stop;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MediaProjectionHelper.getInstance().createVirtualDisplay(requestCode, resultCode, data, true, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
        initData();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
        }
    }

    @Override
    protected void onDestroy() {
        MediaProjectionHelper.getInstance().stopService(this);
        super.onDestroy();
    }

    private void initView() {
        layout_root = findViewById(R.id.layout_root);
        layout_group_1 = findViewById(R.id.layout_group_1);
        layout_group_2 = findViewById(R.id.layout_group_2);
        layout_group_3 = findViewById(R.id.layout_group_3);
        layout_space = findViewById(R.id.layout_space);
        btn_service_start = findViewById(R.id.btn_service_start);
        btn_service_stop = findViewById(R.id.btn_service_stop);
        btn_screen_capture = findViewById(R.id.btn_screen_capture);
        btn_media_recorder_start = findViewById(R.id.btn_media_recorder_start);
        btn_media_recorder_stop = findViewById(R.id.btn_media_recorder_stop);
    }

    private void initListener() {
        btn_service_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doServiceStart();
            }
        });
        btn_service_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doServiceStop();
            }
        });
        btn_screen_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doScreenCapture();
            }
        });
        btn_media_recorder_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doMediaRecorderStart();
            }
        });
        btn_media_recorder_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doMediaRecorderStop();
            }
        });
    }

    private void initData() {
        MediaProjectionHelper.getInstance().setNotificationEngine(new MediaProjectionNotificationEngine() {
            @Override
            public Notification getNotification() {
                String title = getString(R.string.service_start);
                return NotificationHelper.getInstance().createSystem()
                        .setOngoing(true)// 常驻通知栏
                        .setTicker(title)
                        .setContentText(title)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .build();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            LogUtil.i("Environment.isExternalStorageLegacy: " + Environment.isExternalStorageLegacy());
        }
    }

    /**
     * 启动媒体投影服务
     */
    private void doServiceStart() {
        MediaProjectionHelper.getInstance().startService(this);
    }

    /**
     * 停止媒体投影服务
     */
    private void doServiceStop() {
        MediaProjectionHelper.getInstance().stopService(this);
    }

    /**
     * 屏幕截图
     */
    private void doScreenCapture() {
        MediaProjectionHelper.getInstance().capture(new ScreenCaptureCallback() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                super.onSuccess(bitmap);

                LogUtil.i("ScreenCapture onSuccess");

//                int[] position = new int[2];
//                layout_space.getLocationOnScreen(position);
//                int width = layout_space.getWidth();
//                int height = layout_space.getHeight();
//                bitmap = Bitmap.createBitmap(bitmap, position[0], position[1], width, height);

                saveBitmapToFile(bitmap, "ScreenCapture");
            }

            @Override
            public void onFail() {
                super.onFail();

                LogUtil.e("ScreenCapture onFail");
            }
        });
    }

    /**
     * 开始屏幕录制
     */
    private void doMediaRecorderStart() {
        MediaProjectionHelper.getInstance().startMediaRecorder(new MediaRecorderCallback() {
            @Override
            public void onSuccess(File file) {
                super.onSuccess(file);

                LogUtil.i("MediaRecorder onSuccess: " + file.getAbsolutePath());

                Toast.makeText(getApplication(), getString(R.string.content_media_recorder_result, file.getAbsolutePath()), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFail() {
                super.onFail();

                LogUtil.e("MediaRecorder onFail");
            }
        });
    }

    /**
     * 停止屏幕录制
     */
    private void doMediaRecorderStop() {
        MediaProjectionHelper.getInstance().stopMediaRecorder();
    }

    /**
     * 保存Bitmap到文件
     *
     * @param bitmap     bitmap
     * @param filePrefix 文件前缀名
     */
    private void saveBitmapToFile(Bitmap bitmap, String filePrefix) {
        BitmapUtils.saveBitmapToFile(this, bitmap, filePrefix, new SaveBitmapCallback() {
            @Override
            public void onSuccess(File file) {
                super.onSuccess(file);

                LogUtil.i("Save onSuccess: " + file.getAbsolutePath());

                Toast.makeText(getApplication(), getString(R.string.content_save_bitmap_result, file.getAbsolutePath()), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFail(Exception e) {
                super.onFail(e);

                LogUtil.e("Save onError");

                e.printStackTrace();
            }
        });
    }
}
