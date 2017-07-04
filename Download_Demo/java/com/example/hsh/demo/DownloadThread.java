package com.example.hsh.demo;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.content.ContentValues.TAG;

/**
 * To download a part of the whole task
 * Created by H.S.H on 2017/5/4.
 */
class DownloadThread extends Thread{

    private int id;             // 线程号
    private long startSize;      // 开始位置
    private long endSize;        // 结束位置
    private long finishSize;     // 已完成大小
    private String fileUrl;
    private String filePath;
    private Start sd;

    DownloadThread(Start sd, int id, String fileUrl, String filePath, long startSize, long endSize, long finishSize) {
        this.sd = sd;
        this.id = id;
        this.fileUrl = fileUrl;
        this.filePath = filePath;
        this.startSize = startSize;
        this.endSize = endSize;
        this.finishSize = finishSize;
        Log.d(TAG, "myDemo DownloadThread " + id + " start:" + startSize);
        Log.d(TAG, "myDemo DownloadThread " + id + " end:" + endSize);
        Log.d(TAG, "myDemo DownloadThread " + id + " finish:" + finishSize);
        start();
    }

    @Override
    public void run() {
        HttpURLConnection connection = null;
        InputStream is = null;
        RandomAccessFile raf = null;
        boolean flag = true;

        try {
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestProperty("Range", "bytes=" + startSize + "-" + endSize);
            int code = connection.getResponseCode();
            if (code == 206) {
                Log.d(TAG, "myDemo run id " + id + ": download start");
                // 开始下载
                File file = new File(filePath);
                raf = new RandomAccessFile(file, "rw");
                // 设置数据写入位置
                raf.seek(startSize);
                int length;
                byte[] data = new byte[1024];
                is = connection.getInputStream();
                // 记录时间
                long begin = System.currentTimeMillis();
                long sum = 0;
                // 写入数据
                while ((length = is.read(data)) != -1) {
                    if (!MainActivity.isPause) {
                        raf.write(data, 0, length);
                        sum += length;
                        finishSize += length;
                        // 计算这个线程的下载速度
                        long end = System.currentTimeMillis();
                        if (end - begin > 1000) {
                            double speed = sum * 1000.0 / (end - begin) / 1024; // KB/s
                            begin = System.currentTimeMillis();
                            sum = 0;
                            sd.thread_speed[id] = speed;
                        }
                        // 统计总进度和线程进度
                        sd.count(length, finishSize, id, flag);
                        if (flag)
                            flag = false;
                    } else {
                        Log.d(TAG, "myDemo run id " + id + ": 暂停了");
                        break;
                    }
                }
                if (!MainActivity.isPause) {
                    Log.d(TAG, "myDemo run id " + id + ": -------下载完成-------");
                    Log.d(TAG, "myDemo run id " + id + ": " + finishSize);
                    Log.d(TAG, "myDemo run id " + id + ": " + length);
                }
            } else {
                Log.d(TAG, "myDemo run id " + id + ": request error");
                Log.d(TAG, "myDemo run id " + id + ": " + code);
                sd.error();
            }
        } catch (IOException e) {
            Log.d(TAG, "myDemo run id " + id + ": ");
            e.printStackTrace();
        } finally {
            sd.thread_speed[id] = 0;
            if (connection != null)
                connection.disconnect();
            try {
                if (is != null)
                    is.close();
                if (raf != null)
                    raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

