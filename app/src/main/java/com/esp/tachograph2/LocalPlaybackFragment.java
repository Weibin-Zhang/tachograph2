package com.esp.tachograph2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class LocalPlaybackFragment extends Fragment {

    TableLayout tableLayout_videoList, tableLayout_videoList2;

    public LocalPlaybackFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_playback, container, false);
        tableLayout_videoList = view.findViewById(R.id.tablelayout_videoList);
        tableLayout_videoList2 = view.findViewById(R.id.tablelayout_videoList2);
        initVideoList();
        initVideoList2();
        return view;
    }

    private void initVideoList(){
        File sdDir = Environment.getExternalStorageDirectory();
        String savePath = sdDir.toString()+"/tachograph/qianduan/";
        File folder = new File(savePath);
        if(!folder.exists()){
            return;
        }
        tableLayout_videoList.removeAllViews();
        File[] videos = folder.listFiles();
        //时间倒序排列
        Arrays.sort(videos, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });
        for(File video : videos){
            String videoPath = video.getPath();
            String videoName = video.getName();
            if(videoName.endsWith(".mp4")){
                TableRow tableRow = new TableRow(getContext());
                ImageView imageView = new ImageView(getContext());
                int width = getResources().getDisplayMetrics().widthPixels * 1 / 3; // 宽度为屏幕宽度的2/3
                int height = width * 3 / 4; // 高度为宽度的3/4
                TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(width, height);
//                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
                imageView.setLayoutParams(layoutParams);
//                imageView.setLayoutParams(params);
                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND);
//                thumbnail = scaleMatrix(thumbnail, 200, 300);
                imageView.setImageBitmap(thumbnail);
                TextView textView = new TextView(getContext());
                textView.setText(videoName);
                textView.setTextSize(10);
                Button button = new Button(getContext());
                button.setText("播放");
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getContext(), FullscreenVideoActivity.class);
                        Uri videoUri = Uri.parse(videoPath);
                        intent.setData(videoUri);
                        startActivity(intent);
                    }
                });
                tableRow.addView(imageView);
                tableRow.addView(textView);
                tableRow.addView(button);
                tableLayout_videoList.addView(tableRow);
            }
        }


    }

    private void initVideoList2(){
        File sdDir = Environment.getExternalStorageDirectory();
        String savePath = sdDir.toString()+"/tachograph/houduan/";
        File folder = new File(savePath);
        if(!folder.exists()){
            return;
        }
        tableLayout_videoList2.removeAllViews();
        File[] videos = folder.listFiles();
        //时间倒序排列
        Arrays.sort(videos, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });
        for(File video : videos){
            String videoPath = video.getPath();
            String videoName = video.getName();
            if(videoName.endsWith(".mp4")){
                TableRow tableRow = new TableRow(getContext());
                ImageView imageView = new ImageView(getContext());
                int width = getResources().getDisplayMetrics().widthPixels * 1 / 3; // 宽度为屏幕宽度的2/3
                int height = width * 3 / 4; // 高度为宽度的3/4
                TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(width, height);
//                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
                imageView.setLayoutParams(layoutParams);
//                imageView.setLayoutParams(params);
                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND);
//                thumbnail = scaleMatrix(thumbnail, 200, 300);
                imageView.setImageBitmap(thumbnail);
                TextView textView = new TextView(getContext());
                textView.setText(videoName);
                textView.setTextSize(10);
                Button button = new Button(getContext());
                button.setText("播放");
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getContext(), FullscreenVideoActivity.class);
                        Uri videoUri = Uri.parse(videoPath);
                        intent.setData(videoUri);
                        startActivity(intent);
                    }
                });
                tableRow.addView(imageView);
                tableRow.addView(textView);
                tableRow.addView(button);
                tableLayout_videoList2.addView(tableRow);
            }
        }


    }

    private  Bitmap scaleMatrix(Bitmap bitmap, int width, int height){
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float scaleW = width/w;
        float scaleH = height/h;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleW, scaleH); // 长和宽放大缩小的比例
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
    }
}