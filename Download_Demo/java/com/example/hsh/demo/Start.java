package com.example.hsh.demo;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import static android.content.ContentValues.TAG;

/**
 * To start download task
 * Created by H.S.H on 2017/5/3.
 */

class Start extends Thread {

    private String fileUrl;                 // 资源地址
    private String fileName;                // 文件名
    private String path;                    // 储存路径
    private int threadNum;                  // 线程数量
    private long fileLength;                // 文件总大小
    private long finishSize;                // 已完成大小
    private DownloadThread threads[];       // 储存线程对象
    private MainActivity context;           // 用于统计进度和速度

    double[] thread_speed;                 // 用于统计每个线程的下载速度

    // 构造方法，初始化，并启动线程
    Start(String url, String file_name, long file_length, int threadNum, MainActivity context) {
        // url to test:
        // http://dldir1.qq.com/weixin/android/weixin657android1040.apk
        // https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk
        // http://s1.music.126.net/download/android/CloudMusic_official_4.0.2_182112.apk
        // http://dl.hdslb.com/mobile/latest/iBiliPlayer-bili.apk
        // http://wap.dl.pinyin.sogou.com/wapdl/android/apk/SogouInput_android_v8.10_sweb.apk
        // http://cailing.595818.com/upload/ring/000/982/1fd9588ff2ede659eedbc567d5a3c58a.mp3
        // https://ss0.bdstatic.com/5aV1bjqh_Q23odCf/static/superman/img/logo/bd_logo1_31bdc765.png

        this.fileUrl = url;
        this.fileName = file_name;
        this.fileLength = file_length;
        this.threadNum = threadNum;
        this.context = context;
        threads = new DownloadThread[threadNum];
        thread_speed = new double[threadNum];
        Log.d(TAG, "myDemo StartDownload: sd card: " + Environment.getExternalStorageState());
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        finishSize = 0;

        allocate();
        start();
    }

    // 计算总速度
    @Override
    public void run() {
        while (!MainActivity.isPause) {
            double speed = 0;
            for (int i = 0; i < threadNum; i++) {
                speed += thread_speed[i];
            }
            context.speed = speed;
            context.handler.sendEmptyMessage(MainActivity.SPEED);
            try {
                sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 分割任务，分配给各个线程
    private void allocate() {
        try {
            File file = new File(path + File.separator + fileName);
            String file_path = file.getAbsolutePath();
            Log.d(TAG, "myDemo file_path: " + file_path);
            Log.d(TAG, "myDemo allocate file length: " + fileLength);

            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.setLength(fileLength);
            raf.close();
            // 分配任务给每个线程
            int part = (int) (fileLength / threadNum);    // 分块大小
            Log.d(TAG, "myDemo allocate part: " + part);
            for (int i = 0; i < threadNum; i++) {
                long start = i * part;
                long end = (i + 1) * part - 1;
                if (i == threadNum - 1)
                    end = fileLength - 1;
                // 加上该线程已完成的部分
                long add = context.get_thread_record(i);
                start += add;
                Log.d(TAG, "myDemo allocate start size: " + start);
                Log.d(TAG, "myDemo allocate end size: " + end);
                threads[i] = new DownloadThread(this, i, fileUrl, file_path, start, end, add);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 同步方法，统计进度
    synchronized void count(long size, long thread_size, int id, boolean flag) {
        if (flag)
            finishSize += thread_size;
        else
            finishSize += size;
        context.thread_record("thread" + id, thread_size);
        context.progress = (int) (finishSize * 100 / fileLength);
        context.handler.sendEmptyMessage(MainActivity.UPDATE);
        if (finishSize >= fileLength) {
            context.handler.sendEmptyMessage(MainActivity.FINISH);
            Log.d(TAG, "myDemo count: 下载完成");
        }
    }

    // 线程下载出错处理方法
    synchronized void error() {
        context.handler.sendEmptyMessage(MainActivity.FAILED);
    }
}
