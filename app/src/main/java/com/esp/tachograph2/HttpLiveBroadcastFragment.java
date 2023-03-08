package com.esp.tachograph2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class HttpLiveBroadcastFragment extends Fragment {
    ImageView imageView_now;
    String url = "http://192.168.43.185";
    String TAG = "HttpLiveBroadcastFragment";
//    TableLayout body;
//    YoloV5Ncnn yoloV5Ncnn = new YoloV5Ncnn();

    public HttpLiveBroadcastFragment() {}
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_http_live_broadcast, container, false);
        imageView_now = (ImageView) view.findViewById(R.id.imageView_esp2);
        Button btn_find = (Button) view.findViewById(R.id.button_search_hot_IP2);
        TableLayout body = (TableLayout)view.findViewById(R.id.tablelayout_body2);
        YoloV5Ncnn yoloV5Ncnn = new YoloV5Ncnn();
        boolean ret_init = yoloV5Ncnn.Init(getActivity().getAssets());
        if(!ret_init){
            Log.e(TAG, "yolov5ncnn init failed");
        }
        btn_find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNetWorkUtils.listESP(body, imageView_now, getContext(), yoloV5Ncnn);
            }
        });
        return view;
    }



}