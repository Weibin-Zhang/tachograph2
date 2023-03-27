package com.esp.tachograph2;

import android.content.Context;
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
    private LayoutInflater mInflater;
    String BASE_URL = "http://192.168.43.33/";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_remote_playback, container, false);
        mListView = view.findViewById(R.id.listView);
        mBackButton = view.findViewById(R.id.backButton);
        String wifiIP = getWifiIP();
//        BASE_URL = "http://" + wifiIP + "/";
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
        new GetAndShowFolderList().execute("");


        return view;
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
            return paths;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            if(result!=null){
                mPaths.clear();
                mPaths.addAll(result);
                mAdapter.notifyDataSetChanged();
            } else{

            }
        }
    }

    private String getWifiIP(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(getContext().CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // 设备当前连接到WiFi网络
                WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int ipInt = wifiInfo.getIpAddress();
                String wifiIP = String.format("%d.%d.%d.%d", (ipInt & 0xff), (ipInt >> 8 & 0xff), (ipInt >> 16 & 0xff), (ipInt >> 24 & 0xff));
                Log.d("TAG", "WiFi IP: " + wifiIP);
                return wifiIP;
//                try {
//                    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
//                    while (interfaces.hasMoreElements()) {
//                        NetworkInterface networkInterface = interfaces.nextElement();
//                        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
//                        while (addresses.hasMoreElements()) {
//                            InetAddress address = addresses.nextElement();
//                            if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
//                                // 找到设备的IPv4地址
//                                String localIP = address.getHostAddress();
//                                Log.d("TAG", "Local IP: " + localIP);
//                                break;
//                            }
//                        }
//                    }
//                } catch (SocketException e) {
//                    e.printStackTrace();
//                }
            }
//            else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
//                // 设备当前连接到移动网络
//                try {
//                    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
//                    while (interfaces.hasMoreElements()) {
//                        NetworkInterface networkInterface = interfaces.nextElement();
//                        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
//                        while (addresses.hasMoreElements()) {
//                            InetAddress address = addresses.nextElement();
//                            if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
//                                // 找到设备的IPv4地址
//                                String localIP = address.getHostAddress();
//                                Log.d("TAG", "Local IP: " + localIP);
//                                break;
//                            }
//                        }
//                    }
//                } catch (SocketException e) {
//                    e.printStackTrace();
//                }
//            }
        }
//        else {
//            // 设备未连接到网络
//
//        }
        return null;
    }

}