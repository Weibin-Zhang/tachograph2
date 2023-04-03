package com.esp.tachograph2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
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
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;

public class LocalPlaybackFragment extends Fragment {

    TableLayout tableLayoutVideoList, tableLayoutVideoList2;

    public LocalPlaybackFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_playback, container, false);
        tableLayoutVideoList = view.findViewById(R.id.tablelayoutVideoList);
        tableLayoutVideoList2 = view.findViewById(R.id.tablelayoutVideoList2);
        File sdDir = Environment.getExternalStorageDirectory();
        String savePath1 = sdDir.toString()+"/tachograph/qianduan/";
        String savePath2 = sdDir.toString()+"/tachograph/houduan/";
        initVideoList(tableLayoutVideoList, savePath1);
        initVideoList(tableLayoutVideoList2, savePath2);
        return view;
    }

    private void initVideoList(TableLayout tableLayout, String path){
        File folder = new File(path);
        if(!folder.exists()){
            return;
        }
        tableLayout.removeAllViews();
        File[] videos = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return isMp4Playable(file.getPath());
            }
        });
//        File[] videos = folder.listFiles();
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
                imageView.setLayoutParams(layoutParams);
                new LoadThumbnailTask(imageView).execute(video);//使用异步任务加载缩略图，防止线程阻塞
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
                tableLayout.addView(tableRow);
            }
        }
    }

    private boolean isMp4Playable(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            String hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
            return "yes".equals(hasVideo);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }
        return false;
    }

    private static class LoadThumbnailTask extends AsyncTask<File, Void, Bitmap> {

        private WeakReference<ImageView> imageViewRef;

        public LoadThumbnailTask(ImageView imageView) {
            this.imageViewRef = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(File... files) {

            File videoFile = files[0];
            if (videoFile.exists() && videoFile.canRead()) {
                String videoPath = videoFile.getPath();
                return ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND);
            } else {
                return null;
            }
//            String videoPath = files[0].getPath();
//            return ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView imageView = imageViewRef.get();
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }


}