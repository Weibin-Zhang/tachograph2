package com.esp.tachograph2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class RemotePlaybackFragment extends Fragment {

    public RemotePlaybackFragment() {
    }
    private ListView mListView;
    private List<String> mPaths = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;
    private Stack<String> mStack = new Stack<>();
    private Button mBackButton;
    private Button mSearchButton;
    private LayoutInflater mInflater;
    String BASE_URL = "http://192.168.43.33/";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_remote_playback, container, false);
        mListView = view.findViewById(R.id.listView);
        mBackButton = view.findViewById(R.id.backButton);
        mSearchButton = view.findViewById(R.id.searchButton);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //查找esp32cam的ip地址
                ArrayList<String> IP_list = mNetWorkUtils.SearchHotIP();
                ArrayList<String> list = new ArrayList<>();
                for(int i = 0; i < IP_list.size(); i++){
                    String ip = IP_list.get(i);
                    String result = mNetWorkUtils.IsEsp(ip);
                    if (result.contains("this is esp32cam, qianduan")) {
                        String object = "前端：" + ip;
                        list.add(object);
                    } else if (result.contains("this is esp32cam, houduan")) {
                        String object = "后端：" + ip;
                        list.add(object);
                    }
                }
                if(list.size()==0){
                    Toast.makeText(getContext(), "未找到行车记录仪，请检查网络", Toast.LENGTH_SHORT).show();
                }else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("请选择一个行车记录仪");
                    String[] optionsArray = list.toArray(new String[0]);
                    builder.setItems(optionsArray, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // 处理用户的选择
                            String selectedOption = list.get(i);
                            Toast.makeText(getContext(), "你选择了 " + selectedOption, Toast.LENGTH_SHORT).show();
//                            BASE_URL = selectedOption.split(" ")[1];
                            String ip = selectedOption.split("：")[1];
                            BASE_URL = "http://" + ip + "/";
                            new GetAndShowFolderList().execute("");
//                            System.out.println("selectedOption:"+selectedOption);
//                            System.out.println("BASE_URL:"+BASE_URL);
                        }
                    });
                    // 创建并显示对话框
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }


            }
        });
        mAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, mPaths){
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
                        if(position>0){
                            mBackButton.setEnabled(true);
                            //打开该目录
                            new GetAndShowFolderList().execute(finalDir);
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
                        Intent intent = new Intent(getContext(), HistoryPlayBackActivity.class);
                        intent.putExtra("path", finalDir);
                        intent.putExtra("BASE_URL", BASE_URL);

                        startActivity(intent);
                    }
                });
                return convertView;
            }
        };
        mListView.setAdapter(mAdapter);
        mBackButton.setEnabled(false);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStack.size() >= 2) {
                    mStack.pop();
                    String dir = mStack.pop();
                    new GetAndShowFolderList().execute(dir);
                }
            }
        });



        return view;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();

    }


    //获取并更新目录列表
    private class GetAndShowFolderList extends AsyncTask<String, Void, List<String>> {
        @Override
        protected List<String> doInBackground(String... urls) {
            String url = BASE_URL + urls[0];
            mStack.push(urls[0]);
            List<String> paths = new ArrayList<>();
            if (!url.endsWith(".jpg")) {
                try {
                    Document doc = Jsoup.connect(url).get();
                    for (Element link : doc.select("a[href]")) {
                        String path = link.attr("href");
                        paths.add(path);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{

            }
            System.out.println("PATH:"+paths);
            return paths;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            System.out.println("RESULT:"+result);
            if(result.size()!=0){
                mPaths.clear();
                mPaths.addAll(result);
                mAdapter.notifyDataSetChanged();
            } else{
                Toast.makeText(getContext(), "行车过程中请不要访问行车记录仪的内存卡", Toast.LENGTH_SHORT).show();
            }
        }
    }


}