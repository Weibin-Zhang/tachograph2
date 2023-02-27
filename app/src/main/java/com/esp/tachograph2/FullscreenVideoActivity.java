package com.esp.tachograph2;

import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class FullscreenVideoActivity extends AppCompatActivity {
    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置全屏模式
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 创建一个 VideoView 控件
        videoView = new VideoView(this);
        setContentView(videoView);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 获取要播放的视频文件路径
        Uri videoUri = getIntent().getData();

        // 将视频文件路径设置到 VideoView 控件中
        videoView.setVideoURI(videoUri);
        videoView.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // 结束当前 Activity
        finish();
    }

}
