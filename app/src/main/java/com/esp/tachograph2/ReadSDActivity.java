package com.esp.tachograph2;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

//import org.apache.commons.io.FileUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ReadSDActivity extends AppCompatActivity{
    private static final String TAG = "ReadSDActivity";
    private ArrayList<String> hotIP = mNetWorkUtils.SearchHotIP();
    private static final String SERVER_IP = "192.168.43.33";  // ESP32CAM的IP地址
    private static final int SERVER_PORT = 80;
    private static final String BASE_URL = "http://" + SERVER_IP + ":" + SERVER_PORT + "/";

    private Button mReturnButton;
    private ListView mFileListView;
    private List<String> mDirList;
    private ArrayAdapter<String> mAdapter;
    private String mCurrentDir;

    private GetDirListTask task;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readsd);
        mReturnButton = findViewById(R.id.return_button);
        mFileListView = findViewById(R.id.file_list);
        mDirList = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mDirList);
        mFileListView.setAdapter(mAdapter);
        mCurrentDir = "";
        task = new GetDirListTask();
//        history = new Stack<List<String>>();
        // 点击目录列表项进入下一级目录
        mFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String dir = mDirList.get(position);

                if(position>0){
                    mCurrentDir = dir;
//                    List<String> temp = new ArrayList<>();
//                    temp = mDirList;
//                    history.push(new ArrayList<>(mDirList));
//                    Log.d(TAG,"history:"+history);
//                    new GetDirListTask().execute();
                    task.execute();
                    List<String> dirlist = task.getMlist();
                    mDirList.clear();
                    mDirList.addAll(dirlist);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });

        // 返回上一级
        mReturnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "return btn clicked");
                List<String> dirlist = task.popMlist();
                mDirList.clear();
                mDirList.addAll(dirlist);
                mAdapter.notifyDataSetChanged();
//                if(!history.empty()){
////                    mDirList.clear();
//                    List<String> temp = new ArrayList<>();
//                    temp = history.pop();
////                    mDirList.clear();
//                    mDirList.addAll(temp);
//                    mAdapter.notifyDataSetChanged();
//                }
//                new DownloadFilesTask().execute();
            }
        });

//        GetDirListTask task = new GetDirListTask();
        task.execute();
        List<String> dirlist = task.getMlist();
        mDirList.clear();
        mDirList.addAll(dirlist);
        mAdapter.notifyDataSetChanged();
    }

    // 获取当前目录下的文件列表
    private class GetDirListTask extends AsyncTask<Void, Void, List<String>> {
        private Stack<List<String>> history;
        List<String> dirList = new ArrayList<>();
        @Override
        protected List<String> doInBackground(Void... voids) {

            try {
                URL url = new URL(BASE_URL + mCurrentDir);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                //如果
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Document doc = (Document) Jsoup.connect(String.valueOf(url)).get();
                    Element body = doc.body();
                    Elements links = body.select("a[href]");
                    for (Element link : links) {
                        String path = link.attr("href");
                        dirList.add(path);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error getting directory list", e);
            }
            history.push(new ArrayList<>(dirList));
            System.out.println("当前目录："+mCurrentDir+"文件列表："+dirList);
            return dirList;
        }
        public List<String> popMlist(){
            if (history.size() > 1) {
                history.pop();
                return history.peek();
            }else{
                return history.peek();
            }
        }
        public List<String> getMlist(){
            return dirList;
        }

        @Override
        protected void onPostExecute(List<String> dirList) {
//            mDirList.clear();
//            mDirList.addAll(dirList);
//            mAdapter.notifyDataSetChanged();
        }
    }
}
