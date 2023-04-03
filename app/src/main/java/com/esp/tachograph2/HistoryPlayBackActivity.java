package com.esp.tachograph2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryPlayBackActivity extends AppCompatActivity {
    private ImageView imageView1;
    private String TAG = "HistoryPlayBackActivity";
    static String savePath1 = Environment.getExternalStorageDirectory().toString() + "/tachograph/qianduan/";
    String BASE_URL = "http://192.168.43.2/";
    mNetWorkUtils.ImageQueue imageQueue = new mNetWorkUtils.ImageQueue();
    String path = "";
    DownloadFolder1 downloadFolder1;
    DetectAndRecord detectAndRecord;
    YoloV5Ncnn yoloV5Ncnn;
    private boolean downloadFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        yoloV5Ncnn = new YoloV5Ncnn();
        boolean ret_init = yoloV5Ncnn.Init(this.getAssets());
        if(!ret_init){
            Log.e(TAG, "yolov5ncnn init failed");
        }
        // 设置全屏模式
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 创建一个 VideoView 控件
        imageView1 = new ImageView(this);
        setContentView(imageView1);
        downloadFolder1 = new DownloadFolder1();
        detectAndRecord = new DetectAndRecord(savePath1, imageView1, yoloV5Ncnn);
    }
    @Override
    protected void onStart() {
        super.onStart();
        path = getIntent().getStringExtra("path");
        BASE_URL = getIntent().getStringExtra("BASE_URL");
        List<String> jpgList = new ArrayList<>();
        try {
            jpgList = new getJpgList().execute(path).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        int length = jpgList.size() - 1;
        ExecutorService executorService = Executors.newFixedThreadPool(2);
//        downloadFolder1.execute(jpgList);
//        detectAndRecord.execute(length);
        downloadFolder1.executeOnExecutor(executorService, jpgList);
        detectAndRecord.executeOnExecutor(executorService, length);

    }
    //获取目录列表
    private class getJpgList extends AsyncTask<String, Void, List<String>> {
        @Override
        protected List<String> doInBackground(String... strings) {
            String urlFolder = BASE_URL + strings[0];
            List<String> jpgList = new ArrayList<>();
            try {
                Document doc = Jsoup.connect(urlFolder).get();
                for (Element link : doc.select("a[href]")) {
                    String path = link.attr("href");
                    jpgList.add(path);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return jpgList;
        }
        //被取消时释放资源
        @Override
        protected void onCancelled(){

        }
        //完成时释放资源
        @Override
        protected void onPostExecute(List<String> list){

        }
    }

    //下载整个文件夹
    private class DownloadFolder1 extends AsyncTask<List<String>, Bitmap, Void>{
        @Override
        protected void onPreExecute(){
            downloadFlag = true;
        }
        @Override
        protected Void doInBackground(List<String>... lists) {
            downloadFlag = true;
            List<String> jpgList = lists[0];
            int i = 1;
            while(i<jpgList.size() && !isCancelled()){
//            for(int i = 1; i < jpgList.size(); i++){
                //从第二行获取图片路径，第一行是文件夹目录
                String jpgPath = jpgList.get(i);
                jpgPath = jpgPath.replaceFirst("^/+", "");
                String urlJpg = BASE_URL + jpgPath;
                long time1 = System.currentTimeMillis();
                try{
                    URLConnection connection = new URL(urlJpg).openConnection();
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if(bitmap!=null){
                        System.out.println("DownloadFolder asynctask: getJpg: " + jpgPath);
//                        publishProgress(bitmap);
                        imageQueue.addImage(bitmap);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("本次下载图片用时：",String.valueOf((System.currentTimeMillis() - time1))+"ms");
                i++;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid){
            super.onPostExecute(aVoid);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    downloadFlag = false;
                }
            }, 1000);
        }
    }

    //异步任务，接收图片，检测图片并录制视频
    private class DetectAndRecord extends AsyncTask<Integer, Bitmap, Void> {
        YoloV5Ncnn yoloV5Ncnn;
        ImageView imageView;
        //录屏
        private String savePath;
        private Boolean isEncorded = false;
        private int width = 640;
        private int height = 480;
        private double frameRate = 5;
        private FFmpegFrameRecorder recorder;
        private String mp4Name = null;
        private String saveMp4FilePath = null;
        private Frame frame = null;
        private AndroidFrameConverter converter = new AndroidFrameConverter();
        private String TAG1 = "Detect:";
        private String TAG2 = "Record:";
        public DetectAndRecord(String savePath, ImageView imageView, YoloV5Ncnn yoloV5Ncnn){
            this.imageView = imageView;
            this.yoloV5Ncnn = yoloV5Ncnn;
            this.savePath = savePath;
        }
        //在异步任务执行之前调用
        @Override
        protected void onPreExecute(){
            File saveFile = new File(savePath);
            if (!saveFile.exists()) {
                saveFile.mkdir();
            }
            path = getIntent().getStringExtra("path");
            Log.d(TAG1, path);

            mp4Name = getSaveMp4FilePath(path);
            Log.d(TAG2,"saveMp4FilePath:"+mp4Name);
//            mp4Name = "test.mp4";
            saveMp4FilePath = savePath + mp4Name;
            recorder = new FFmpegFrameRecorder(saveMp4FilePath, width, height);
            recorder.setFormat("mp4");
            recorder.setFrameRate(frameRate);
            try{
                recorder.start();
            } catch (FrameRecorder.Exception e) {
                Log.e(TAG1, "222");
                e.printStackTrace();
            }
            Log.d(TAG1, "333");
        }
        //后台执行
        @Override
        protected Void doInBackground(Integer... integers) {
            Log.d(TAG1, "111");
            int length = integers[0];
            int currentNum = 0;
            long time1 = System.currentTimeMillis();
            while (!isCancelled() && currentNum < length && downloadFlag) {
                Log.d(TAG1, "inblckground");
                Bitmap bitmap = imageQueue.getImage();
                if(bitmap == null){
                    Log.w(TAG1,"no image now");
                    continue;
                }
                bitmap = mNetWorkUtils.DeteleImg(bitmap, true, yoloV5Ncnn);
                isEncorded = false;
                Log.d(TAG1, "finsh");
                //显示结果
                Log.d("show:","ing");
                publishProgress(bitmap);
                frame = converter.convert(bitmap);
                try {
                    recorder.record(frame);
                    isEncorded = true;
                } catch (FrameRecorder.Exception e) {
                    e.printStackTrace();
                }
                currentNum++;
                Log.d("更新一次图片用时：", String.valueOf(System.currentTimeMillis() - time1) + "ms");
                time1 = System.currentTimeMillis();
            }
            if(recorder!=null){
                try {
                    recorder.stop();
                } catch (FrameRecorder.Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
        //异步任务被取消时调用，此时doInBackground()方法被中断
        @Override
        protected void onCancelled(){
            System.out.println("DetectAndRecord is cancelled");
            if(recorder!=null){
                try {
                    recorder.stop();
                    Log.d(TAG2, "over");
                } catch (FrameRecorder.Exception e) {
                    e.printStackTrace();
                }
            }
        }
        //在调用'publishProgress()'方法后被调用，可以更新UI
        @Override
        protected void onProgressUpdate(Bitmap... bitmaps){
            imageView.setImageBitmap(bitmaps[0]);
            delay(150);
        }
        private void delay(int ms){
            try {
                Thread.currentThread();
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //将形如"2023_04_02/19_55"的表达式转为"2023年04月02日19时55分"的形式
        private String getSaveMp4FilePath(String dateTime){
            String[] parts = dateTime.split("[/_]");
            String result = parts[0] + "年" + parts[1] + "月" + parts[2] + "日" + parts[3] + "时" + parts[4] + "分.mp4";
            return result;
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // 结束当前 Activity
        if (downloadFolder1 != null) {
            downloadFolder1.cancel(true);
        }
        if (detectAndRecord != null) {
            detectAndRecord.cancel(true);
        }
        finish();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG,"destory");
        if (downloadFolder1 != null) {
            downloadFolder1.cancel(true);
        }
        if (detectAndRecord != null) {
            detectAndRecord.cancel(true);
        }
        downloadFolder1 = null;
        detectAndRecord = null;
        while(!imageQueue.isEmpty()){
            Bitmap bitmap = imageQueue.getImage();
            bitmap.recycle();
        }
        imageView1.setImageDrawable(null);
        yoloV5Ncnn.Uninit();
        finish();
    }
}
