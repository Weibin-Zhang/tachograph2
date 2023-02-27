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

public class PlaybackFragment extends Fragment {

    TableLayout tableLayout_videoList;

    public PlaybackFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_playback, container, false);
        tableLayout_videoList = view.findViewById(R.id.tablelayout_videoList);

        initVideoList();

        return view;
    }

    private void initVideoList(){
        File sdDir = Environment.getExternalStorageDirectory();
        String savePath = sdDir.toString()+"/tachograph/";
        File folder = new File(savePath);
        if(!folder.exists()){
            return;
        }
        tableLayout_videoList.removeAllViews();
        int width = 200; int height = 300;
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(width, height);
        File[] videos = folder.listFiles();
        for(File video : videos){
            String videoPath = video.getPath();
            String videoName = video.getName();
            if(videoName.endsWith(".mp4")){
                TableRow tableRow = new TableRow(getContext());
                ImageView imageView = new ImageView(getContext());
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