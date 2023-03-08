package com.esp.tachograph2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.BoringLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class LiveBroadcastFragment extends Fragment {

    TableLayout header, body;
    Socket testSocket;
    InputStream testInputStream;
    OutputStream testOutputStream;
    byte[] RevBuff = new byte[1024];//定义接收数据流的包的大小
    Boolean testingIp = false, isQianduanIP = false, isHouduanIP = false;
    String[] ips = new String[2];
    Socket socket;
    InputStream inputStream;
    OutputStream outputStream;
    ImageView imageView_esp;
    Bitmap esp_img = null;
    Bitmap record_img = null;
    Frame record_frame = null;
    byte[] temp = new byte[0]; //存放一帧图像的数据
    MyHandler myHandler;
    int headFlag = 0;// 0 数据流不是图像数据   1 数据流是图像数据
    Boolean isrecorded = true;
    FFmpegFrameRecorder recorder;
    FFmpegFrameRecorder recorder2;
    long t1, t2;
    String savePath;
    double frameRate = 5;
    YoloV5Ncnn yoloV5Ncnn = new YoloV5Ncnn();
    Boolean flag = true;

    Boolean isRecording = false;
    Thread recordThread;
    long index1 = 0; long index2 = 0;

    public LiveBroadcastFragment() {
        // Required empty public constructor

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_live_broadcast, container, false);
        View view = inflater.inflate(R.layout.fragment_live_broadcast, container, false);
//        header = view.findViewById(R.id.tablelayout_header);
        boolean ret_init = yoloV5Ncnn.Init(getActivity().getAssets());
        recordThread = new Thread(record);
        if (!ret_init)
        {
            Log.e("liveBroadcastFragment: ", "yolov5ncnn Init failed");
        }
        body = view.findViewById(R.id.tablelayout_body);
        imageView_esp = view.findViewById(R.id.imageView_esp);
        Button btn_find = view.findViewById(R.id.button_search_hot_IP);
        myHandler = new MyHandler();
        File sdDir = Environment.getExternalStorageDirectory();
        savePath = sdDir.toString()+"/tachograph/";
        File saveFile = new File(savePath);
        if (!saveFile.exists()) {
            saveFile.mkdir();
        }
        checkNeedPermissions();

        t1 = getTime1(); t2 = getTime1();
        btn_find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SearchCamera();
            }
        });
        return view;

    }

    private void SearchCamera(){
        body.removeAllViews();
        ArrayList<String> IP_list = null;
        try {
            IP_list = SearchHotIP();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(int i = 0; i< IP_list.size(); i++){
            String ip = IP_list.get(i);
            System.out.println("IP: "+ip);
            String result = getCameraByIp(ip);
            System.out.println("result:"+result+ip);
        }
        System.out.println("ips:"+ips);
        if(ips[0]!=null){
            TableLayout.LayoutParams params = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            String number, IP;
            TableRow tableRow = new TableRow(getContext());
            TextView textView1 = new TextView(getContext());
            textView1.setText("前端");
            textView1.setTextSize(20);
            tableRow.addView(textView1);
            TextView textView2 = new TextView(getContext());
            textView2.setText(ips[0]);
            textView2.setTextSize(20);
            tableRow.addView(textView2);
            Button button = new Button(getContext());
            button.setText("连接");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(button.getText() == "连接"){
                        button.setText("停止");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Message msg = myHandler.obtainMessage();
                                try {
                                    //如果 host_editText  port_editText为空的话 点击连接 会退出程序
//                                socket = new Socket((host_editText.getText()).toString(),Integer.valueOf(port_editText.getText().toString()));
                                    socket = new Socket(ips[0],8080);
                                    if(socket.isConnected()){
                                        t1 = getTime1(); t2 = getTime1();
                                        String mp4FileName = savePath + getTime2() + ".mp4";
                                        recorder = new FFmpegFrameRecorder(mp4FileName, 640, 480);
                                        recorder.setFormat("mp4");
                                        recorder.setFrameRate(frameRate);
                                        recorder.start();
//                                        recorder2 = new FFmpegFrameRecorder(mp4FileName, 640, 480);
//                                        recorder2.setFormat("mp4");
//                                        recorder2.setFrameRate(frameRate);
//                                        recorder2.start();
                                        msg.what = 0;//显示连接服务器成功信息
                                        inputStream = socket.getInputStream();
                                        outputStream = socket.getOutputStream();
//                                        recordThread.start();
                                        Recv();//接收数据


//                                        msg.what = 0;//显示连接服务器成功信息
//                                        inputStream = socket.getInputStream();
//                                        outputStream = socket.getOutputStream();
////                                        recordThread.start();
//                                        Recv();//接收数据

                                    }else{
                                        msg.what = 1;//显示连接服务器失败信息
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    msg.what = 1;//显示连接服务器失败信息
                                }
                                myHandler.sendMessage(msg);
                            }
                        }).start();
                    }else if(button.getText() == "停止"){
                        //关闭socket连接
                        if (socket != null) {try { socket.close(); } catch (IOException e) { e.printStackTrace(); }}
                        if(inputStream!=null){try { inputStream.close(); }catch (IOException e) { e.printStackTrace(); }}
                        if(outputStream!=null){try { outputStream.close(); }catch (IOException e) { e.printStackTrace(); }}
                        //把视频保存下来
                        flag = false;
                        try {recorder.stop();} catch (FrameRecorder.Exception e) {e.printStackTrace();}
//                        try {recorder2.stop();} catch (FrameRecorder.Exception e) {e.printStackTrace();}
                        button.setText("连接");
                    }
                }
            });
            tableRow.addView(button);
            body.addView(tableRow);
        }
        if(ips[1]!=null){
            TableLayout.LayoutParams params = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            String number, IP;
            TableRow tableRow = new TableRow(getContext());
            TextView textView1 = new TextView(getContext());
            textView1.setText("后端");
            textView1.setTextSize(20);
            tableRow.addView(textView1);
            TextView textView2 = new TextView(getContext());
            textView2.setText(ips[1]);
            textView2.setTextSize(20);
            tableRow.addView(textView2);
            Button button = new Button(getContext());
            button.setText("连接");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(button.getText() == "连接"){
                        button.setText("停止");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Message msg = myHandler.obtainMessage();
                                try {
                                    //如果 host_editText  port_editText为空的话 点击连接 会退出程序
//                                socket = new Socket((host_editText.getText()).toString(),Integer.valueOf(port_editText.getText().toString()));
                                    socket = new Socket(ips[1],8080);
                                    if(socket.isConnected()){
                                        t1 = getTime1(); t2 = getTime1();
                                        String mp4FileName = savePath + getTime2() + ".mp4";
                                        recorder = new FFmpegFrameRecorder(mp4FileName, 640, 480);
                                        recorder.setFormat("mp4");
                                        recorder.setFrameRate(frameRate);
                                        recorder.start();
//                                        recorder2 = new FFmpegFrameRecorder(mp4FileName, 640, 480);
//                                        recorder2.setFormat("mp4");
//                                        recorder2.setFrameRate(frameRate);
//                                        recorder2.start();
                                        msg.what = 0;//显示连接服务器成功信息
                                        inputStream = socket.getInputStream();
                                        outputStream = socket.getOutputStream();
//                                        recordThread.start();
                                        Recv();//接收数据

                                    }else{
                                        msg.what = 1;//显示连接服务器失败信息
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    msg.what = 1;//显示连接服务器失败信息
                                }
                                myHandler.sendMessage(msg);
                            }
                        }).start();
                    }else if(button.getText() == "停止"){
                        //关闭socket连接
                        if (socket != null) {try { socket.close(); } catch (IOException e) { e.printStackTrace(); }}
                        if(inputStream!=null){try { inputStream.close(); }catch (IOException e) { e.printStackTrace(); }}
                        if(outputStream!=null){try { outputStream.close(); }catch (IOException e) { e.printStackTrace(); }}
                        //把视频保存下来
                        flag = false;
                        try {recorder.stop();} catch (FrameRecorder.Exception e) {e.printStackTrace();}
//                        try {recorder2.stop();} catch (FrameRecorder.Exception e) {e.printStackTrace();}
                        button.setText("连接");
                    }
                }
            });
            tableRow.addView(button);
            body.addView(tableRow);
        }

    }

    //    接收数据方法
    public void Recv(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(socket != null && socket.isConnected()){
                    try {
                        int Len = inputStream.read(RevBuff);
                        if(Len != -1){
//                          图像数据包的头  FrameBegin
                            boolean begin_cam_flag = RevBuff[0] == 70 && RevBuff[1] == 114 && RevBuff[2] == 97 && RevBuff[3] == 109 && RevBuff[4] == 101
                                    && RevBuff[5] == 66 && RevBuff[6] == 101 && RevBuff[7] == 103 && RevBuff[8] == 105 && RevBuff[9] == 110 ;
//                            图像数据包的尾  FrameOverr
                            boolean end_cam_flag = RevBuff[0] == 70 && RevBuff[1] == 114 && RevBuff[2] == 97 && RevBuff[3] == 109 && RevBuff[4] == 101
                                    && RevBuff[5] == 79 && RevBuff[6] == 118 && RevBuff[7] == 101 && RevBuff[8] == 114 && RevBuff[9] == 114;
//                            判断接收的包是不是图片的开头数据 是的话s说明下面的数据属于图片数据 将headFlag置1
                            if(headFlag == 0 && begin_cam_flag){
                                headFlag = 1;
                            }else if(end_cam_flag){  //判断包是不是图像的结束包 是的话 将数据传给 myHandler  3 同时将headFlag置0
                                Message msg = myHandler.obtainMessage();
                                msg.what = 3;
                                myHandler.sendMessage(msg);
                                headFlag = 0;
                            }else if(headFlag == 1){ //如果 headFlag == 1 说明包是图像数据  将数据发给byteMerger方法 合并一帧图像
                                temp = byteMerger(temp,RevBuff);
                            }
//                            定义包头 Esp32Msg  判断包头 在向myHandler  2 发送数据    eadFlag == 0 && !end_cam_flag没用 会展示图像的数据
                            boolean begin_msg_begin = RevBuff[0] == 69 && RevBuff[1] == 115 && RevBuff[2] == 112 && RevBuff[3] == 51 && RevBuff[4] == 50
                                    && RevBuff[5] == 77 && RevBuff[6] == 115 && RevBuff[7] == 103 ;
                            if(begin_msg_begin){
                                Message msg = myHandler.obtainMessage();
                                msg.what = 2;
                                msg.arg1 = Len;
                                msg.obj = RevBuff;
                                myHandler.sendMessage(msg);
                            }
                        }else{
//                            如果Len = -1 说明接受异常  显示连接服务器失败信息  跳出循环
                            Message msg = myHandler.obtainMessage();
                            msg.what = 1;
                            myHandler.sendMessage(msg);
                            break;
                        }
                    } catch (IOException e) {
//                        如果接受数据inputStream.read(RevBuff)语句执行失败 显示连接服务器失败信息  跳出循环
                        e.printStackTrace();
                        Message msg = myHandler.obtainMessage();
                        msg.what = 1;
                        myHandler.sendMessage(msg);
                        break;
                    }
                    if(index1 != index2){
//                    if(!isrecorded){
                        t2 = getTime1();
                        record_frame = converter.convert(record_img);
                        if((t2-t1)<100){
//                            Frame frame = new Frame.Builder().setBitmap(esp_img).build();
                            try {
                                recorder.record(record_frame);
                            } catch (FrameRecorder.Exception e) {
                                e.printStackTrace();
                            }
                        }else
                        {
                            t1 = t2;
                            t2 = getTime1();
                            try {
                                recorder.stop();
                            } catch (FrameRecorder.Exception e) {
                                e.printStackTrace();
                            }
                            String mp4FileName = savePath + getTime2() + ".mp4";
                            recorder = new FFmpegFrameRecorder(mp4FileName, 640, 480);
                            recorder.setFormat("mp4");
                            recorder.setFrameRate(frameRate);
                            try {
                                recorder.start();
                                recorder.record(record_frame);
                            } catch (FrameRecorder.Exception e) {
                                e.printStackTrace();
                            }
                        }
//                        isrecorded = true;
                        index2 = index1;
                    }
                }
            }
        }).start();
    }

    AndroidFrameConverter converter = new AndroidFrameConverter();
//    static OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

    //    合并一帧图像数据  a 全局变量 temp   b  接受的一个数据包 RevBuff
    public byte[] byteMerger(byte[] a,byte[] b){
        int i = a.length + b.length;
        byte[] t = new byte[i]; //定义一个长度为 全局变量temp  和 数据包RevBuff 一起大小的字节数组 t
        System.arraycopy(a,0,t,0,a.length);  //先将 temp（先传过来的数据包）放进  t
        System.arraycopy(b,0,t,a.length,b.length);//然后将后进来的这各数据包放进t
        return t; //返回t给全局变量 temp
    }

    private ArrayList<String> SearchHotIP() throws InterruptedException {
        ArrayList<String> IP_list = new ArrayList<>();
        String local_ip = getMobileIpAddress();
        String[] local_ip_split = local_ip.split("\\.");
        System.out.println("local_ip: "+local_ip);
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
                            System.out.println(address + " machine is turned on and can be pinged");
                        }
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        find.start();
        find.join();
        return IP_list;
    }

    private void checkNeedPermissions(){
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //多个权限一起申请
            ActivityCompat.requestPermissions(getActivity(), new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
        }
    }

    //处理一些不能在线程里面执行的信息
    class MyHandler extends Handler {
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
//                    连接服务器成功信息
                    Toast.makeText(getContext(),"连接服务器成功！",Toast.LENGTH_SHORT).show();
                    break;
                case 1:
//                    连接服务器失败信息
                    Toast.makeText(getContext(),"连接服务器失败！",Toast.LENGTH_SHORT).show();
                    break;
                case 2:
//                    处理接收到的非图像数据
//                    byte[] Buffer = new byte[msg.arg1];
//                    System.arraycopy((byte[])msg.obj,0,Buffer,0,msg.arg1);
//                    SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
//                    Date date = new Date(System.currentTimeMillis());
////                    String content = (new String(Buffer)) + "----"  + formatter.format(date) + "\n";
//                    String content = (new String(Buffer)) + "\n";
//                    System.out.println("content: " + content);
//                    rec_data.append(content);
//                    break;
                case 3:
//                    处理接受到的图像数据 并展示
                    esp_img = BitmapFactory.decodeByteArray(temp, 0,temp.length);
                    if(esp_img!=null){

//                        CommonUtil.saveBitmap2file(bitmap, getApplicationContext());//存图
                        esp_img = DeteleImg(esp_img, true);
//                        record_frame = converter.convert(esp_img);
                        record_img = esp_img;
                        imageView_esp.setImageBitmap(esp_img);//这句就能显示图片(bitmap数据没问题的情况下) 存在图像闪烁情况 待解决
                        index1 = System.currentTimeMillis();
                        System.out.println("现在的时间是"+index1);
                        System.out.println("刷新一次耗时："+(index1-index2));
                        isrecorded = false;
                    }
//                    if(flag){
//                        Thread timerThread = new Thread(timer);
//                        timerThread.start();
//                        flag = false;
//                    }
                    //CommonUtil.saveBitmap2file(bitmap, getApplicationContext());//存图
                    temp = new byte[0];  //一帧图像显示结束  将 temp清零
                    break;
                default: break;
            }
        }
    }

    private Thread record = new Thread(new Runnable() {
        String mp4FileName = savePath + getTime2() + ".mp4";
//        FFmpegFrameRecorder recorder2 = null;
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis();
        Frame frame = null;
        @Override
        public void run() {
//            recorder2 = new FFmpegFrameRecorder(mp4FileName, 640, 480);
//            recorder2.setFormat("mp4");
//            recorder2.setFrameRate(frameRate);
//            try {
//                recorder2.start();
//                } catch (FrameRecorder.Exception ex) {
//                ex.printStackTrace();
//            }
            Boolean b = flag;
            while(flag) {
//                while(true){
                end = System.currentTimeMillis();
                frame = converter.convert(esp_img);
                if (end - start < 60000) {
                    try {
                        if (index1 != index2) {
                            recorder2.record(frame);
                            index2 = index1;
                        }
//                        if(!isrecorded){
//                            recorder2.record(frame);
//                            isrecorded = true;
//                        }

                    } catch (FrameRecorder.Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        recorder2.stop();
                    } catch (FrameRecorder.Exception e) {
                        e.printStackTrace();
                    }
                    start = end;
                    end = System.currentTimeMillis();
                    mp4FileName = savePath + getTime2() + ".mp4";
                    recorder2 = new FFmpegFrameRecorder(mp4FileName, 640, 480);
                    recorder2.setFormat("mp4");
                    recorder2.setFrameRate(frameRate);
                    try {
                        recorder2.start();
                        if(!isrecorded){
                            recorder2.record(frame);
                            isrecorded = true;
                        }

                    } catch (FrameRecorder.Exception e) {
                        e.printStackTrace();
                    }

                }
            }
            try {
                recorder2.stop();
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
        }
    });

    private Thread timer = new Thread(new Runnable() {

        private final Callable<Boolean> recordCall = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                String mp4FileName = savePath + getTime2() + ".mp4";
                FFmpegFrameRecorder recorder1 = new FFmpegFrameRecorder(mp4FileName, 640, 480);
                try {
                    recorder1.setFormat("mp4");
                    recorder1.setFrameRate(frameRate);
                    recorder1.start();
                    while (isRecording) {
                        if(esp_img!=null){
                            Frame myframe = converter.convert(esp_img);
                            recorder1.record(myframe);
                        }
                    }
                    recorder1.stop();
                } catch (FrameRecorder.Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        };

        long start;
        long end;
        @Override
        public void run() {
            start = System.currentTimeMillis(); end = System.currentTimeMillis();
            FutureTask<Boolean> ft;
            while(socket != null && socket.isConnected()){
                ft = new FutureTask<Boolean>(recordCall);
                isRecording = true;
                new Thread(ft).start();
                if(end-start<60000){
                    end = System.currentTimeMillis();
                }else{
                    isRecording = false;
                    try {
                        if(ft.get()) {
                            System.out.println("一分钟视频存储好了");
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    start = end;
                    end = System.currentTimeMillis();
                }
            }

        }
    });


    private Bitmap DeteleImg(Bitmap bitmap, Boolean b){
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

        Paint textbgpaint = new Paint();
        textbgpaint.setColor(Color.WHITE);
        textbgpaint.setStyle(Paint.Style.FILL);

        Paint textpaint = new Paint();
        textpaint.setColor(Color.BLACK);
        textpaint.setTextSize(26);
        textpaint.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i < objects.length; i++)
        {
            paint.setColor(colors[i % 19]);

            canvas.drawRect(objects[i].x, objects[i].y, objects[i].x + objects[i].w, objects[i].y + objects[i].h, paint);

            // draw filled text inside image
            {
                String text = objects[i].label + " = " + String.format("%.1f", objects[i].prob * 100) + "%";

                float text_width = textpaint.measureText(text);
                float text_height = - textpaint.ascent() + textpaint.descent();

                float x = objects[i].x;
                float y = objects[i].y - text_height;
                if (y < 0)
                    y = 0;
                if (x + text_width > rgba.getWidth())
                    x = rgba.getWidth() - text_width;

                canvas.drawRect(x, y, x + text_width, y + text_height, textbgpaint);

                canvas.drawText(text, x, y - textpaint.ascent(), textpaint);
            }
        }

        return rgba;
    }

    private String getCameraByIp(String ip){
        testingIp = true;
        isHouduanIP = false;
        isQianduanIP = false;
        new Thread(() -> {
            try {
                testSocket = new Socket(ip, 8080);
                if (testSocket.isConnected()) {
                    testInputStream = testSocket.getInputStream();
                    testOutputStream = testSocket.getOutputStream();
                    RecvTestMsg(ip);//接收数据
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        Date start = new Date();
        long waitDuration = 0;
        while(testingIp && waitDuration<5000){waitDuration = new Date().getTime() - start.getTime();}
        if (testSocket != null) {
            releaseTestCamera();
        }
        if(isQianduanIP){
            return "qianduan:";
        }else if(isHouduanIP){
            return "houduan:";
        }else{
            return "";
        }
    }

    private void RecvTestMsg(String save_ip){
        while(testSocket != null && testSocket.isConnected()){
            try {
                int Len = testInputStream.read(RevBuff);
                if(Len != -1){
//                            定义包头 Esp32Msg  判断包头 在向myHandler  2 发送数据    eadFlag == 0 && !end_cam_flag没用 会展示图像的数据
                    boolean begin_msg_begin = RevBuff[0] == 69 && RevBuff[1] == 115 && RevBuff[2] == 112 && RevBuff[3] == 51 && RevBuff[4] == 50
                            && RevBuff[5] == 77 && RevBuff[6] == 115 && RevBuff[7] == 103 ;
                    if(begin_msg_begin){//处理接收到的非图像数据
                        byte[] Buffer = new byte[Len];
                        System.arraycopy((byte[]) RevBuff, 0, Buffer, 0, Len);
                        String receive = new String(Buffer);
//                        String receive = byteToString(Buffer);
                        if(receive.contains("Esp32MsgClient is Connect!")){
                            if(receive.contains("id1")){
                                ips[0] = save_ip;
                                isQianduanIP = true;
                            }else if(receive.contains("id2")){
                                ips[1] = save_ip;
                                isHouduanIP = true;
                            }
                            testingIp = false;
                            break;
                        }
                    }
                }else{
//                            如果Len = -1 说明接受异常  显示连接服务器失败信息  跳出循环
                    break;
                }
            } catch (IOException e) {
//                        如果接受数据inputStream.read(RevBuff)语句执行失败 显示连接服务器失败信息  跳出循环
                e.printStackTrace();
                break;
            }
        }
    }

    private long getTime1(){
        long time;
        long currentTime = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date(currentTime);
        String time_str = format.format(date) + "";
        time = Long.parseLong(time_str);
        return time;
    }

    private String getTime2(){
        long currentTime = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日HH时mm分");
        Date date = new Date(currentTime);
        return format.format(date) + "";
    }

    private void releaseTestCamera(){
        try { testSocket.close(); } catch (IOException e) { e.printStackTrace(); }
        try { testInputStream.close(); }catch (IOException e) { e.printStackTrace(); }
        try { testOutputStream.close(); }catch (IOException e) { e.printStackTrace(); }
    }

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

}