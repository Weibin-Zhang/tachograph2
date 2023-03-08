package com.esp.tachograph2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

public class mNetWorkUtils {
    static String TAG = "mNetWorkUtils";
    static String savePath = Environment.getExternalStorageDirectory().toString() + "/tachograph/";
    //查看本地IP
    private static String getMobileIpAddress() {
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = (NetworkInterface) en.nextElement();
                for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static CaptureVideoTask mCaptureVideoTask;
    //查看连接热点设备的ip
    public static ArrayList<String> SearchHotIP(){
        ArrayList<String> IP_list = new ArrayList<>();
        String local_ip = getMobileIpAddress();
        String[] local_ip_split = local_ip.split("\\.");
        System.out.println("local_ip: "+local_ip);

        int version = Build.VERSION.SDK_INT;
        Log.d(TAG, " android version: "+String.valueOf(version));
        if(version <= Build.VERSION_CODES.Q){
            //安卓版本小于等于10
            try{
                Process ipProc = Runtime.getRuntime().exec("ip neigh");
                ipProc.waitFor();
                if (ipProc.exitValue() != 0) {
                    throw new Exception("Unable to access ARP entries");
                }
                BufferedReader br = new BufferedReader(new InputStreamReader(ipProc.getInputStream(), "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] splitted = line.split(" +");
                    if (splitted != null && splitted.length == 6) {
                        String ip = splitted[0];
                        String[] ip_splitted = ip.split("\\.");
                        String flag = splitted[2];
                        String mac = splitted[4];
                        String state = splitted[5];
                        if(ip_splitted[0].equals(local_ip_split[0]) && ip_splitted[1].equals(local_ip_split[1]) && ip_splitted[2].equals(local_ip_split[2])){
                            //如果ip网关和本地IP网关相同，认为是连接了热点的设备的ip
                            IP_list.add(ip);
                            Log.d(TAG, "find a hot ip: "+ip);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            //安卓版本大于10时不允许访问"/proc/net/arp"
            Thread find = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] ip = new byte[4];
                    ip[0] = (byte) Integer.parseInt(local_ip_split[0]);
                    ip[1] = (byte) Integer.parseInt(local_ip_split[1]);
                    ip[2] = (byte) Integer.parseInt(local_ip_split[2]);
                    int self = Integer.parseInt(local_ip_split[3]);
                    ip[3] = (byte)self;
                    try{
                        for(int i = 1; i <= 255; i++)
                        {
                            if(i == self){
                                continue;
                            }
                            ip[3] = (byte) i;
                            InetAddress address = InetAddress.getByAddress(ip);
                            if(address.isReachable(20))
                            {
                                IP_list.add(address.toString().split("/")[1]);
                                Log.d(TAG, address + " machine is turned on and can be pinged");
                                System.out.println(address + " machine is turned on and can be pinged");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            find.start();
            try {
                find.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return IP_list;
    }
    //测试ip地址是否为esp32cam
    public static String IsEsp(String ip){
        final String[] result = {""};
//        String result = null;
        String testUrl = "http://" + ip + "/test";
        Thread testIP = new Thread(new Runnable() {
            @Override
            public void run() {
                int TIMEOUT = 2000;//设置超时时间
                try {
                    URL url = new URL(testUrl);
                    Log.d(TAG, "testUrl: "+testUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(TIMEOUT);
                    connection.setReadTimeout(TIMEOUT);
                    // Get response
                    int responseCode = connection.getResponseCode();
                    if(responseCode == HttpURLConnection.HTTP_OK){
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        result[0] = response.toString();
                        Log.d(TAG,testUrl+" result: "+ result[0]);
                    }else{
                        //连接失败
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        testIP.start();
        try {testIP.join();} catch (InterruptedException e) {e.printStackTrace();}
        return result[0];
    }
    //查找并列出esp32cam的IP
    public static void listESP(TableLayout body, ImageView imageView, Context context, YoloV5Ncnn yoloV5Ncnn){
        body.removeAllViews();
        ArrayList<String> IP_list = SearchHotIP();
        for(int i = 0; i < IP_list.size(); i++){
            String ip = IP_list.get(i);
            String result = IsEsp(ip);
            Log.d(TAG, "result: "+result);
            if (result.contains("this is esp32cam")) {
                Log.d(TAG,"11");
                TableLayout.LayoutParams params = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                String number, IP;
                TableRow tableRow = new TableRow(context);
                TextView textView1 = new TextView(context);
                textView1.setText("前端");
                textView1.setTextSize(20);
                tableRow.addView(textView1);
                TextView textView2 = new TextView(context);
                String url = "http://"+ip;
                textView2.setText(url);
                textView2.setTextSize(20);
                tableRow.addView(textView2);
                Button button = new Button(context);
                button.setText("连接");
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (button.getText().equals("连接")) {
                            mCaptureVideoTask = new CaptureVideoTask(imageView, savePath, yoloV5Ncnn);
                            mCaptureVideoTask.execute(url);
//                            mHttpGetTask.execute(url);
                            button.setText("停止");
                        }
                        else if (button.getText().equals("停止")) {
                            mCaptureVideoTask.cancel(true);
//                            mHttpGetTask.cancel(true);
                            button.setText("连接");
                        }
                    }
                });
                tableRow.addView(button);
                body.addView(tableRow);
            }
        }
    }
    //不断重启的异步任务，用于循环读取esp32cam的画面
    public static class HttpGetTask extends AsyncTask<String, Void, Bitmap> {
        String url = null;
        private ImageView imageView;
        public HttpGetTask(ImageView imageView){
            this.imageView = imageView;
        }
        @Override
        protected Bitmap doInBackground(String... urls) {
            url = urls[0];
            try {
                URLConnection connection = new URL(url).openConnection();
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                imageView.setImageBitmap(result);
            }
            //重启异步任务
            new HttpGetTask(imageView).execute(url);
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
    //异步任务，循环访问读取esp32cam的画面，更新UI并保存成视频进本地SD卡
    public static class CaptureVideoTask extends AsyncTask<String, Bitmap, Void>{
        private ImageView imageView;
        private String savePath;
        private long start, end;
        private Boolean isEncorded = false;
        private int width = 640;
        private int height = 480;
        private double frameRate = 2.5;
        private FFmpegFrameRecorder recorder;
        private String mp4Name = null;
        private String saveMp4FilePath = null;
        private Frame frame = null;
        private AndroidFrameConverter converter = new AndroidFrameConverter();
        private Boolean isRunning = true;
        private String url;
        private String TAG = "CaptureVideoTask";
        private YoloV5Ncnn yoloV5Ncnn;
        public CaptureVideoTask(ImageView imageView, String savePath, YoloV5Ncnn yoloV5Ncnn){
            this.imageView = imageView;
            this.savePath = savePath;
            this.yoloV5Ncnn = yoloV5Ncnn;
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
            try {
                recorder.start();
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
        }
        //在异步任务的后台线程中执行，不能操作UI
        @Override
        protected Void doInBackground(String... urls){
            url = urls[0];
            while(!isCancelled()){
                Bitmap bitmap = getBitmapFromUrl(url);
                if(bitmap == null){
                    Log.w(TAG, "Bitmap is null");
                    continue;
                }
                //bitmap不为空时更新UI
                publishProgress(bitmap);
                end = System.currentTimeMillis();
                frame = converter.convert(bitmap);
                if((end - start) < 2*60000){
                    if(!isEncorded){
                        try {
                            recorder.record(frame);
                            isEncorded = true;
                        } catch (FrameRecorder.Exception e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    //每隔一分钟自动保存一段
                    start = end;
                    end = System.currentTimeMillis();
                    try {
                        recorder.stop();
                    } catch (FrameRecorder.Exception e) {
                        e.printStackTrace();
                    }
                    mp4Name = getMp4SaveName();
                    saveMp4FilePath = savePath + mp4Name;
                    recorder = new FFmpegFrameRecorder(saveMp4FilePath, width, height);
                    recorder.setFormat("mp4");
                    recorder.setFrameRate(frameRate);
                    try {
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
        //在调用'publishProgress()'方法后被调用，可以更新UI
        @Override
        protected void onProgressUpdate(Bitmap... bitmaps){
            imageView.setImageBitmap(bitmaps[0]);
        }
        //在异步任务执行完成后调用
        @Override
        protected void onPostExecute(Void aVoid){

        }
        //异步任务被取消时调用，此时doInBackground()方法被中断
        @Override
        protected void onCancelled(){
//            isRunning = false;
            if(recorder!=null){
                try {
                    recorder.stop();
                } catch (FrameRecorder.Exception e) {
                    e.printStackTrace();
                }
            }
        }
        //http获取esp32cam的画面
        private Bitmap getBitmapFromUrl(String urlString) {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                if(bitmap!=null){
                    isEncorded = false;
                    bitmap = DeteleImg(bitmap, true, yoloV5Ncnn);
//                    imageView.setImageBitmap(bitmap);
                }
                return bitmap;
            } catch (IOException e) {
                Log.e(TAG, "Failed to get bitmap from url " + urlString, e);
                return null;
            }
        }
    }
    //调用yolo模型对目标画框
    private static Bitmap DeteleImg(Bitmap bitmap, Boolean b, YoloV5Ncnn yoloV5Ncnn){
        YoloV5Ncnn.Obj[] objects = yoloV5Ncnn.Detect(bitmap, b);
        if(objects == null){
            return bitmap;
        }
        Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        final int[] colors = new int[] {
                Color.rgb( 54,  67, 244),
                Color.rgb( 99,  30, 233),
                Color.rgb(176,  39, 156),
                Color.rgb(183,  58, 103),
                Color.rgb(181,  81,  63),
                Color.rgb(243, 150,  33),
                Color.rgb(244, 169,   3),
                Color.rgb(212, 188,   0),
                Color.rgb(136, 150,   0),
                Color.rgb( 80, 175,  76),
                Color.rgb( 74, 195, 139),
                Color.rgb( 57, 220, 205),
                Color.rgb( 59, 235, 255),
                Color.rgb(  7, 193, 255),
                Color.rgb(  0, 152, 255),
                Color.rgb( 34,  87, 255),
                Color.rgb( 72,  85, 121),
                Color.rgb(158, 158, 158),
                Color.rgb(139, 125,  96)
        };
        Canvas canvas = new Canvas(rgba);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        Paint encryptpaint = new Paint();
        encryptpaint.setColor(Color.GRAY);
        encryptpaint.setStyle(Paint.Style.FILL);

//        Paint textbgpaint = new Paint();
//        textbgpaint.setColor(Color.WHITE);
//        textbgpaint.setStyle(Paint.Style.FILL);
//
//        Paint textpaint = new Paint();
//        textpaint.setColor(Color.BLACK);
//        textpaint.setTextSize(26);
//        textpaint.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i < objects.length; i++)
        {
            paint.setColor(colors[i % 19]);

            canvas.drawRect(objects[i].x, objects[i].y, objects[i].x + objects[i].w, objects[i].y + objects[i].h, paint);
            canvas.drawRect(objects[i].x, objects[i].y, objects[i].x + objects[i].w, objects[i].y + objects[i].h, encryptpaint);

            // draw filled text inside image
//            {
//                String text = objects[i].label + " = " + String.format("%.1f", objects[i].prob * 100) + "%";
//
//                float text_width = textpaint.measureText(text);
//                float text_height = - textpaint.ascent() + textpaint.descent();
//
//                float x = objects[i].x;
//                float y = objects[i].y - text_height;
//                if (y < 0)
//                    y = 0;
//                if (x + text_width > rgba.getWidth())
//                    x = rgba.getWidth() - text_width;
//
//                canvas.drawRect(x, y, x + text_width, y + text_height, textbgpaint);
//
//                canvas.drawText(text, x, y - textpaint.ascent(), textpaint);
//            }
        }

        return rgba;
    }

}
