package com.mask.screencapturelibrary;

import android.app.Notification;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mask.photo.interfaces.SaveBitmapCallback;
import com.mask.photo.utils.BitmapUtils;
import com.mask.screencapture.interfaces.ScreenCaptureCallback;
import com.mask.screencapture.interfaces.ScreenCaptureNotificationEngine;
import com.mask.screencapture.utils.ScreenCaptureHelper;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private View layout_root;
    private View layout_group_1;
    private View layout_group_2;
    private View layout_group_3;
    private View layout_space;
    private Button btn_screen_capture_start;
    private Button btn_screen_capture_stop;
    private Button btn_screen_capture;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ScreenCaptureHelper.getInstance().parseResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initListener();
    }

    @Override
    protected void onDestroy() {
        ScreenCaptureHelper.getInstance().stopCapture(this);
        super.onDestroy();
    }

    private void initView() {
        layout_root = findViewById(R.id.layout_root);
        layout_group_1 = findViewById(R.id.layout_group_1);
        layout_group_2 = findViewById(R.id.layout_group_2);
        layout_group_3 = findViewById(R.id.layout_group_3);
        layout_space = findViewById(R.id.layout_space);
        btn_screen_capture_start = findViewById(R.id.btn_screen_capture_start);
        btn_screen_capture_stop = findViewById(R.id.btn_screen_capture_stop);
        btn_screen_capture = findViewById(R.id.btn_screen_capture);
    }

    private void initData() {
        ScreenCaptureHelper.getInstance().setNotificationEngine(new ScreenCaptureNotificationEngine() {
            @Override
            public Notification getNotification() {
                return NotificationHelper.getInstance().createSystem()
                        .setOngoing(true)// 常驻通知栏
                        .setTicker(getString(R.string.screen_capture_start))
                        .setContentText(getString(R.string.screen_capture_start))
                        .setDefaults(Notification.DEFAULT_ALL)
                        .build();
            }
        });
    }

    private void initListener() {
        btn_screen_capture_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doScreenCaptureStart();
            }
        });
        btn_screen_capture_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doScreenCaptureStop();
            }
        });
        btn_screen_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doScreenCapture();
            }
        });
    }

    /**
     * 开始系统截图
     */
    private void doScreenCaptureStart() {
        ScreenCaptureHelper.getInstance().startCapture(this);
    }

    /**
     * 停止系统截图
     */
    private void doScreenCaptureStop() {
        ScreenCaptureHelper.getInstance().stopCapture(this);
    }

    /**
     * 系统截图
     */
    private void doScreenCapture() {
        ScreenCaptureHelper.getInstance().capture(new ScreenCaptureCallback() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                super.onSuccess(bitmap);

                LogUtil.i("ScreenCapture onSuccess");
//
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
