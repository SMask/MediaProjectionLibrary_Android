package com.mask.screencapturelibrary;

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
import com.mask.screencapture.utils.ScreenCaptureHelper;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private Button btn_screen_capture;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ScreenCaptureHelper.getInstance().parseResult(requestCode, resultCode, data, new ScreenCaptureCallback() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                super.onSuccess(bitmap);

                LogUtil.i("ScreenCapture onSuccess");

                saveBitmapToFile(bitmap, "ScreenCapture");
            }

            @Override
            public void onFail() {
                super.onFail();

                LogUtil.e("ScreenCapture onFail");
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initListener();
    }

    private void initView() {
        btn_screen_capture = findViewById(R.id.btn_screen_capture);
    }

    private void initData() {

    }

    private void initListener() {
        btn_screen_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doScreenCapture();
            }
        });
    }

    /**
     * 系统截图
     */
    private void doScreenCapture() {
        ScreenCaptureHelper.getInstance().startCapture(this);
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
