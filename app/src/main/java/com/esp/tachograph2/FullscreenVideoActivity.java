package com.esp.tachograph2;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
public class FullscreenVideoActivity extends AppCompatActivity {
    private VideoView videoView;
    private SeekBar seekBar;
    private volatile Thread updateSeekBarThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置全屏模式
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_full_screen_video);
        videoView = findViewById(R.id.videoView);
        seekBar = findViewById(R.id.seekBar);

    }
    @Override
    protected void onStart() {
        super.onStart();
        // 获取要播放的视频文件路径
        Uri videoUri = getIntent().getData();
        // 将视频文件路径设置到 VideoView 控件中
        videoView.setVideoURI(videoUri);
        videoView.start();
        seekBar.setMax(videoView.getDuration());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) { // 如果是用户手动拖动进度条
                    videoView.seekTo(progress); // 跳转到指定位置
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 空实现
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 空实现
            }
        });
// 在视频播放的过程中，更新SeekBar组件的进度
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                seekBar.setProgress(0); // 初始化进度条
                seekBar.setMax(mp.getDuration()); // 设置进度条的最大值为视频的总时长
                updateSeekBarThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            int currentPosition = videoView.getCurrentPosition();
                            seekBar.setProgress(currentPosition); // 更新进度条
                            try {
                                Thread.sleep(500); // 每隔500ms更新一次进度条
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                updateSeekBarThread.start();
            }
        });
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        // 停止线程
        if (updateSeekBarThread != null && updateSeekBarThread.isAlive()) {
            updateSeekBarThread.interrupt();
        }
        //释放内存
        videoView.stopPlayback();
        seekBar.setOnSeekBarChangeListener(null);
        videoView = null;
        seekBar = null;
        updateSeekBarThread = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // 结束当前 Activity
        finish();
    }

}
