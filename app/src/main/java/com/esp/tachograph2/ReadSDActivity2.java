package com.esp.tachograph2;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.bytedeco.javacpp.presets.opencv_core;
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReadSDActivity2 extends AppCompatActivity {
    private TextView mTextView;
    private ListView mListView;
    private List<String> mPaths = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;
    private Stack<String> mStack = new Stack<>();
    private Button mBackButton;
    private LayoutInflater mInflater;
    private static final String SERVER_IP = "192.168.43.33";  // ESP32CAM的IP地址
    private static final int SERVER_PORT = 80;
    private static final String BASE_URL = "http://" + SERVER_IP + ":" + SERVER_PORT + "/";
    private ImageQueue imageQueue;
    static String savePath1 = Environment.getExternalStorageDirectory().toString() + "/tachograph/qianduan/";
    private ImageView testImageView;
    private String TAG = "ReadSDActivity2";
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readsd2);
        testImageView = findViewById(R.id.test_imageview);
        imageQueue = new ImageQueue();
//        mTextView = findViewById(R.id.text_view);
        mListView = findViewById(R.id.list_view);
        YoloV5Ncnn yoloV5Ncnn = new YoloV5Ncnn();
        boolean ret_init = yoloV5Ncnn.Init(this.getAssets());
        if(!ret_init){
            Log.e(TAG, "yolov5ncnn init failed");
        }
        DownloadFolder1 downloadFolder1 = new DownloadFolder1();
        DetectAndRecord detectAndRecord = new DetectAndRecord(savePath1, testImageView, yoloV5Ncnn);
//        final DownloadFolder1[] downloadFolder1 = new DownloadFolder1[1];
//        final DetectAndRecord[] detectAndRecord = new DetectAndRecord[1];
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mPaths){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    if (mInflater == null) {
                        mInflater = LayoutInflater.from(getContext());
                    }
                    convertView = mInflater.inflate(R.layout.list_item, parent, false);
                }
                //判断当前路径是一级目录还是二级目录
                String dir = getItem(position);
                dir = dir.replaceFirst("^/+", "");
                String[] parts = dir.split("/");
                int count = parts.length-1;
                if(count == 1){
                    //此时为二级目录，对应于某个时间段，应该提供下载文件夹的服务

                }
                TextView textView = convertView.findViewById(R.id.text_view);
                textView.setText(getItem(position));
                String finalDir = dir;
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
//                        String dir = getItem(position);
//                mStack.push(dir);
                        if(position>0){
                            mBackButton.setEnabled(true);
                            if (finalDir.endsWith(".jpg")) {
                                new DownloadJpgTask().execute(finalDir);
                            }else{
                                new DownloadWebpageTask().execute(finalDir);
                            }

                        }
                    }
                });
                Button downloadButton = convertView.findViewById(R.id.download_button);
                downloadButton.setVisibility(position >= 1 ? View.VISIBLE : View.GONE);
                if(count!=1){
                    //如果不是二级目录，不提供下载服务
                    downloadButton.setVisibility(View.GONE);
                }
                downloadButton.setTag(getItem(position));
                ExecutorService executorService = Executors.newFixedThreadPool(2);
                downloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //下载整个文件夹
//                        if(downloadButton.getText().equals("下载")){
//                            List<String> jpgList = new ArrayList<>();
//                            try {
//                                jpgList = new getJpgList().execute(finalDir).get();
//                            } catch (ExecutionException | InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            int length = jpgList.size() - 1;
//                            downloadButton.setText("停止");
////                            downloadFolder1[0] = new DownloadFolder1();
////                            detectAndRecord[0] = new DetectAndRecord(savePath1, testImageView, yoloV5Ncnn);
//                            downloadFolder1.executeOnExecutor(executorService, jpgList);
//                            detectAndRecord.executeOnExecutor(executorService, length);

//                        } else if (downloadButton.getText().equals("停止")) {
//                            downloadFolder1.cancel(true);
//                            detectAndRecord.cancel(true);
//                            executorService.shutdown();
//                            downloadButton.setText("下载");
//                        }
//                        String path = (String) v.getTag();
//                        path = path.replaceFirst("^/+", "");
//                        new DownloadFolder().execute(finalDir);
//                        String url = BASE_URL + "download/" + finalDir;
//                        Toast.makeText(ReadSDActivity2.this, "开始下载：" + url, Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(getContext(), HistoryPlayBackActivity.class);
                        intent.putExtra("path", finalDir);
                        startActivity(intent);
                    }
                });
                return convertView;
            }
        };
        mListView.setAdapter(mAdapter);
        mBackButton = findViewById(R.id.back_button);
        mBackButton.setEnabled(false);
//        mStack.push("");
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStack.size() >= 2) {
                    mStack.pop();
                    String dir = mStack.pop();
                    new DownloadWebpageTask().execute(dir);
                }
            }
        });
        new DownloadWebpageTask().execute("");
    }
    //获取目录列表
    private class getJpgList extends AsyncTask<String, Void, List<String>>{
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
    }
    //下载整个文件夹
    private class DownloadFolder1 extends AsyncTask<List<String>, Bitmap, Void>{
        @Override
        protected Void doInBackground(List<String>... lists) {
            List<String> jpgList = lists[0];
            for(int i = 1; i < jpgList.size(); i++){
                //从第二行获取图片路径，第一行是文件夹目录
                String jpgPath = jpgList.get(i);
                jpgPath = jpgPath.replaceFirst("^/+", "");
                String urlJpg = BASE_URL + jpgPath;
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
            }
            return null;
        }
    }
    //实时检测和存储
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
            mp4Name = mNetWorkUtils.getMp4SaveName();
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
        protected Void doInBackground(Integer... integers) {
            int length = integers[0];
            int currentNum = 0;
            while (!isCancelled() && currentNum < length) {
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
                frame = converter.convert(bitmap);
                try {
                    recorder.record(frame);
                    isEncorded = true;
                } catch (FrameRecorder.Exception e) {
                    e.printStackTrace();
                }
                currentNum++;
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
            if(recorder!=null){
                try {
                    recorder.stop();
                } catch (FrameRecorder.Exception e) {
                    e.printStackTrace();
                }
            }
        }
        //在调用'publishProgress()'方法后被调用，可以更新UI
        @Override
        protected void onProgressUpdate(Bitmap... bitmaps){
            imageView.setImageBitmap(bitmaps[0]);
        }
    }

    //获取并更新目录列表
    private class DownloadWebpageTask extends AsyncTask<String, Void, List<String>> {
        @Override
        protected List<String> doInBackground(String... urls) {
            String url = BASE_URL + urls[0];
            mStack.push(urls[0]);
            List<String> paths = new ArrayList<>();
            if (url.endsWith(".jpg")) {

            }else{

            }
            try {
                Document doc = Jsoup.connect(url).get();
                for (Element link : doc.select("a[href]")) {
                    String path = link.attr("href");
                    paths.add(path);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return paths;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            mPaths.clear();
            mPaths.addAll(result);
            mAdapter.notifyDataSetChanged();
        }
    }
    //下载单张图片
    private class DownloadJpgTask extends AsyncTask<String, Bitmap, Void>{
        @Override
        protected Void doInBackground(String... urls) {
            String url = urls[0];
            url = BASE_URL + url;
            try {
                URLConnection connection = new URL(url).openConnection();
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                publishProgress(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onProgressUpdate(Bitmap... bitmaps){
            testImageView.setImageBitmap(bitmaps[0]);
        }
    }
    //长度为2的图片队列
    private class ImageQueue{
        private final int MAX_SIZE = 2;
        private Queue<Bitmap> imageQueue = new LinkedList<Bitmap>();
        public void addImage(Bitmap bitmap){
            while (imageQueue.size() >= MAX_SIZE) {
                imageQueue.poll();
            }
            imageQueue.offer(bitmap);
        }
        public Bitmap getImage(){
            if(!imageQueue.isEmpty()){
                return imageQueue.poll();
            }
            return null;
        }
        public Boolean isEmpty(){
            return imageQueue.isEmpty();
        }
    }
    //下载文件夹
    private class DownloadFolder extends AsyncTask<String, Bitmap, Void>{
        @Override
        protected Void doInBackground(String... strings) {
            //先获取文件夹下所有文件（只有图片）的列表
            String urlList = BASE_URL + strings[0];
            List<String> jpgList = new ArrayList<>();
            try {
                Document doc = Jsoup.connect(urlList).get();
                for (Element link : doc.select("a[href]")) {
                    String path = link.attr("href");
                    jpgList.add(path);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("DownloadFolder asynvtask: getJpgList: "+jpgList);
            //根据列表按顺序下载
            for(int i = 1; i < jpgList.size(); i++){
                //从第二行获取图片路径，第一行是文件夹目录
                String jpgPath = jpgList.get(i);
                jpgPath = jpgPath.replaceFirst("^/+", "");
                String urlJpg = BASE_URL + jpgPath;
                try{
                    URLConnection connection = new URL(urlJpg).openConnection();
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if(bitmap!=null){
                        System.out.println("DownloadFolder asynctask: getJpg: " + jpgPath);
                        publishProgress(bitmap);
                        imageQueue.addImage(bitmap);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
        @Override
        protected void onProgressUpdate(Bitmap... bitmaps){
            testImageView.setImageBitmap(bitmaps[0]);
        }

    }

}
