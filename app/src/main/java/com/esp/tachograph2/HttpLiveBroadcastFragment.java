package com.esp.tachograph2;

import static com.esp.tachograph2.mNetWorkUtils.IsEsp;
import static com.esp.tachograph2.mNetWorkUtils.SearchHotIP;
//import static com.esp.tachograph2.mNetWorkUtils.detectAndRecord2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.esp.tachograph2.mNetWorkUtils;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;

public class HttpLiveBroadcastFragment extends Fragment {
    ImageView imageView_qianduan, imageView_houduan;
//    String url = "http://192.168.43.185";
    String TAG = "HttpLiveBroadcastFragment";
    public HttpLiveBroadcastFragment() {}
    private mNetWorkUtils.ImageQueue imageQueue;
    private String savePath1 = Environment.getExternalStorageDirectory().toString() + "/tachograph/qianduan/";
    private GetImg getImg1;
    private YoloV5Ncnn yoloV5Ncnn;
    private DetectAndRecord detectAndRecord1;
    private ExecutorService executorService1 = Executors.newFixedThreadPool(2);
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_http_live_broadcast, container, false);
        imageView_qianduan = (ImageView) view.findViewById(R.id.imageView_esp2_qianduan);
        imageView_houduan = (ImageView) view.findViewById(R.id.imageView_esp2_houduan);
        Button btn_find = (Button) view.findViewById(R.id.button_search_hot_IP2);
        TableLayout body = (TableLayout)view.findViewById(R.id.tablelayout_body2);
        yoloV5Ncnn = new YoloV5Ncnn();
        boolean ret_init = yoloV5Ncnn.Init(getActivity().getAssets());
        if(!ret_init){
            Log.e(TAG, "yolov5ncnn init failed");
        }
        imageQueue = new mNetWorkUtils.ImageQueue();
        btn_find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listESP(body, getContext());
            }
        });
        return view;
    }

    //查找并列出esp32cam的IP
    private void listESP(TableLayout body, Context context){
        body.removeAllViews();
        ArrayList<String> IP_list = SearchHotIP();
//        if()
        for(int i = 0; i < IP_list.size(); i++){
            String ip = IP_list.get(i);
            String result = IsEsp(ip);
            Log.d(TAG, "result: "+result);
            if (result.contains("this is esp32cam, qianduan")) {
                Log.d(TAG,"找到前端esp");
                TableLayout.LayoutParams params = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                String number, IP;
                TableRow tableRow = new TableRow(context);
                tableRow.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                TextView textView1 = new TextView(context);
                textView1.setText("前端");
                textView1.setTextSize(20);
                tableRow.addView(textView1, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
//                tableRow.addView(textView1);
                TextView textView2 = new TextView(context);
                String url = "http://"+ip +"/capture";
                textView2.setText(ip);
                textView2.setTextSize(15);
                textView2.setMaxLines(1);
                tableRow.addView(textView2, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
//                tableRow.addView(textView2);
                Button button = new Button(context);
                button.setText("连接");
//                ExecutorService executorService1 = Executors.newFixedThreadPool(2);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (button.getText().equals("连接")) {
                            getImg1 = new GetImg();
                            detectAndRecord1 = new DetectAndRecord(savePath1, imageView_qianduan);
                            getImg1.executeOnExecutor(executorService1, url);
                            detectAndRecord1.executeOnExecutor(executorService1);
                            button.setText("停止");
                        }
                        else if (button.getText().equals("停止")) {
                            getImg1.cancel(true);
                            detectAndRecord1.cancel(true);
                            button.setText("连接");
                        }
                    }
                });
//                tableRow.addView(button);
                tableRow.addView(button, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                body.addView(tableRow);
            }
        }
    }


    //异步任务，循环获取esp的图片,将图片传递给检测和录制的异步任务
    private class GetImg extends AsyncTask<String, Void, Void>{
        String url = null;
        private volatile boolean isRunning = true;
        @Override
        protected Void doInBackground(String... urls) {
            url = urls[0];
            while(isRunning && !isCancelled()){
//            while(!isCancelled()){
                try{
                    URLConnection connection = new URL(url).openConnection();
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap != null) {
                        System.out.println("http connect success");
                        imageQueue.addImage(bitmap);
                        System.out.println("imagequeue isempty:"+imageQueue.isEmpty());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
        @Override
        protected void onCancelled() {
            super.onCancelled();
            isRunning = false; // 中断正在执行的线程
        }
    }
    //异步任务，接收图片，检测图片并录制视频
    private class DetectAndRecord extends AsyncTask<Void, Bitmap, Void> {
        private WeakReference<ImageView> imageViewWeakReference;
//        private WeakReference<YoloV5Ncnn> yoloV5NcnnWeakReference;
//        YoloV5Ncnn yoloV5Ncnn;
//        ImageView imageView;
        //录屏
        private String savePath;
        private long start, end;
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
        public DetectAndRecord(String savePath, ImageView imageView){
            imageViewWeakReference = new WeakReference<>(imageView);
//            yoloV5NcnnWeakReference = new WeakReference<>(yoloV5Ncnn);
//            this.imageView = imageView;
//            this.yoloV5Ncnn = yoloV5Ncnn;
            this.savePath = savePath;
            this.start = System.currentTimeMillis();
            this.end = System.currentTimeMillis();
        }
        //在异步任务执行之前调用
        @Override
        protected void onPreExecute(){
            this.start = System.currentTimeMillis();
            this.end = System.currentTimeMillis();
            File saveFile = new File(savePath);
            if (!saveFile.exists()) {
                saveFile.mkdir();
            }
            mp4Name = getMp4SaveName();
            saveMp4FilePath = savePath + mp4Name;
            recorder = new FFmpegFrameRecorder(saveMp4FilePath, width, height);
            recorder.setFormat("mp4");
            recorder.setFrameRate(frameRate);
            try{
                recorder.start();
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
        }
        //后台执行
        @Override
        protected Void doInBackground(Void... voids) {
            while (!isCancelled()) {
                //可以适时地添加一些等待时间，例如使用Thread.sleep()，以避免循环占用CPU
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Bitmap bitmap = imageQueue.getImage();
                if(bitmap == null){
                    Log.w(TAG1,"no image now");
                    continue;
                }
                bitmap = mNetWorkUtils.DeteleImg(bitmap, true, yoloV5Ncnn);
                isEncorded = false;
                Log.d(TAG1, "finsh");
                //显示结果
                publishProgress(bitmap);
                end = System.currentTimeMillis();
                frame = converter.convert(bitmap);
                if((end - start) < 2 * 60000){
                    //没超过2分钟，继续记录
                    if (!isEncorded) {
                        try{
                            recorder.record(frame);
                            Log.d(TAG2, "ing");
                            isEncorded = true;
                        } catch (FrameRecorder.Exception e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    //每隔两分钟保存成一段视频
                    start = end;
                    try{
                        recorder.stop();
                        Log.d(TAG2, "stop and restart");
                    } catch (FrameRecorder.Exception e) {
                        e.printStackTrace();
                    }
                    mp4Name = getMp4SaveName();
                    saveMp4FilePath = savePath + mp4Name;
                    recorder = new FFmpegFrameRecorder(saveMp4FilePath, width, height);
                    recorder.setFormat("mp4");
                    recorder.setFrameRate(frameRate);
                    try{
                        recorder.start();
                        if(!isEncorded){
                            recorder.record(frame);
                            isEncorded = true;
                        }
                    } catch (FrameRecorder.Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
        //异步任务被取消时调用，此时doInBackground()方法被中断
        @Override
        protected void onCancelled(){
            if(recorder!=null){
                try {
                    recorder.stop();
                } catch (FrameRecorder.Exception e) {
                    e.printStackTrace();
                }
            }
            recorder = null;
        }
        //在调用'publishProgress()'方法后被调用，可以更新UI
        @Override
        protected void onProgressUpdate(Bitmap... bitmaps){
            ImageView imageView = imageViewWeakReference.get();
            if(imageView!=null){
                imageView.setImageBitmap(bitmaps[0]);
            }
//            imageView.setImageBitmap(bitmaps[0]);
        }
    }
    //根据时间生成视频保存的文件名，如“2010年10月10日10时10分.jpg”
    public static String getMp4SaveName(){
        String name = null;
        long currentTime = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日HH时mm分");
        Date date = new Date(currentTime);
        return format.format(date) + ".mp4";
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        imageView_qianduan = null;
        imageView_houduan = null;
        while(!imageQueue.isEmpty()){
            Bitmap bitmap = imageQueue.getImage();
            bitmap.recycle();
        }
        imageQueue = null;
        if (getImg1 != null) {
            getImg1.cancel(true);
        }
        if (detectAndRecord1 != null) {
            detectAndRecord1.cancel(true);
        }
        getImg1 = null;
        yoloV5Ncnn.Uninit();
        yoloV5Ncnn = null;
        detectAndRecord1 = null;
        executorService1.shutdown();
        executorService1 = null;
    }
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        imageView_qianduan = null;
//        imageView_houduan = null;
//        imageQueue = null;
////        if (getImg1 != null) {
////            getImg1.cancel(true);
////        }
////        if (detectAndRecord1 != null) {
////            detectAndRecord1.cancel(true);
////        }
////        getImg1 = null;
//        yoloV5Ncnn = null;
////        detectAndRecord1 = null;
//        executorService1.shutdown();
//        executorService1 = null;
//    }
}