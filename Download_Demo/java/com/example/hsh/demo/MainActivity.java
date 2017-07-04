package com.example.hsh.demo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    static final int INITIAL = 0;
    static final int UPDATE = 1;
    static final int FINISH = 2;
    static final int FAILED = 3;
    static final int SPEED = 4;
    static final int ERROR = 5;

    private Button new_task;
    private Button start_task;
    private Button cancel_task;
    private ProgressBar progressBar;
    private TextView show_fileName;
    private TextView show_url;
    private TextView show_fileLength;
    private TextView show_threadNum;
    private TextView show_state;
    private TextView show_progressNum;
    private TextView show_speed;

    private String url_str = "";    // 记录下载地址
    private String fileName;        // 记录文件名
    private int threadNum;          // 记录线程数
    private long fileLength;        // 记录文件大小
    private boolean isFinish;       // 判断是否下载完成
    private SharedPreferences startCheck;   // 记录未完成的下载任务
    private SharedPreferences downloading;  // 记录下载任务的信息
    private SharedPreferences.Editor dEditor;
    int progress;                   // 记录下载进度
    double speed;                   // 记录下载速度
    static boolean isPause = false; // 判断是否下载暂停

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View view = findViewById(R.id.container);
        view.setOnClickListener(this);
        new_task = (Button) findViewById(R.id.new_task);
        new_task.setOnClickListener(this);
        start_task = (Button) findViewById(R.id.start_task);
        start_task.setEnabled(false);
        start_task.setOnClickListener(this);
        cancel_task = (Button) findViewById(R.id.cancel_task);
        cancel_task.setEnabled(false);
        cancel_task.setOnClickListener(this);

        progressBar = (ProgressBar) findViewById(R.id.show_progress);
        progressBar.setMax(100);
        show_fileName = (TextView) findViewById(R.id.show_file_name);
        show_fileName.setOnClickListener(this);
        show_url = (TextView) findViewById(R.id.show_url);
        show_url.setOnClickListener(this);
        show_fileLength = (TextView) findViewById(R.id.show_file_length);
        show_threadNum = (TextView) findViewById(R.id.show_thread_num);
        show_state = (TextView) findViewById(R.id.show_state);
        show_progressNum = (TextView) findViewById(R.id.show_progress_num);
        show_speed = (TextView) findViewById(R.id.show_speed);
        isFinish = false;

        // 判断是否被浏览器调用执行下载任务
        Intent intent = getIntent();
        boolean judge = intent.getBooleanExtra("judge", false);
        if (judge) {
            String getUrl = intent.getStringExtra("url");
            Log.d("MainActivity", "myDemo 被调用 url: " + getUrl);
            if (getUrl != null)
                newTask(getUrl);
        }

        // 申请运行时权限
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        // 检查是否有上次关闭时未完成下载的文件
        if (check()) {
            downloading = getSharedPreferences(fileName, MODE_PRIVATE);
            dEditor = downloading.edit();
            url_str = downloading.getString("url", "");
            threadNum = downloading.getInt("thread num", 0);
            fileLength = downloading.getLong("file length", 0);
            progress = downloading.getInt("progress", 0);

            show_url.setText("文件地址：" + url_str);
            show_fileName.setText("文  件  名：" + fileName);
            adjustLength(fileLength);
            show_threadNum.setText("线程数量：" + threadNum);
            show_state.setText("当前状态：暂停ing  (～﹃～)~zZ");
            show_progressNum.setText("下载进度：" + progress + " %");
            progressBar.setProgress(progress);
            view.setVisibility(View.VISIBLE);

            start_task.setEnabled(true);
            start_task.setText("恢复下载");
            cancel_task.setEnabled(true);
            Toast.makeText(this, "欢迎回来，您还有未完成的下载任务", Toast.LENGTH_SHORT).show();
            Log.d("MainActivity", "myDemo onCreate 未完成下载name: " + fileName);
            Log.d("MainActivity", "myDemo onCreate 未完成下载url: " + url_str);
            Log.d("MainActivity", "myDemo onCreate 未完成下载size: " + fileLength);
            Log.d("MainActivity", "myDemo onCreate 未完成下载num: " + threadNum);
        }
    }

    // 显示应用退出信息
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "应用已退出", Toast.LENGTH_SHORT).show();
        finish();
    }

    // 权限申请结果反馈
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(MainActivity.this, "写入权限 get √", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(MainActivity.this, "写入权限denied ×\n程序无法正常使用", Toast.LENGTH_SHORT).show();
                if (grantResults[1] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(MainActivity.this, "读取权限 get √", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(MainActivity.this, "读取权限denied ×\n程序无法正常使用", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 组件点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.new_task:
                newTask("");
                break;
            case R.id.start_task:
                if ("开始下载".equals(start_task.getText().toString())
                        || "重新下载".equals(start_task.getText().toString())) {
                    isPause = false;
                    new_task.setEnabled(false);
                    cancel_task.setEnabled(true);
                    show_state.setText("当前状态：下载ing  =￣ω￣=");
                    start_task.setText("暂停下载");
                    SharedPreferences.Editor editor = startCheck.edit();
                    editor.putString("file name", fileName);
                    editor.commit();
                    dEditor = downloading.edit();
                    dEditor.putString("url", url_str);
                    dEditor.putInt("thread num", threadNum);
                    dEditor.putLong("file length", fileLength);
                    dEditor.commit();
                    new Start(url_str, fileName, fileLength, threadNum, this);
                    Toast.makeText(MainActivity.this, "下载开始", Toast.LENGTH_SHORT).show();
                } else if ("暂停下载".equals(start_task.getText().toString())) {
                    isPause = true;
                    show_state.setText("当前状态：暂停ing  (～﹃～)~zZ");
                    show_speed.setText("下载速度：0.00 KB/s");
                    start_task.setText("恢复下载");
                    Toast.makeText(MainActivity.this, "下载暂停", Toast.LENGTH_SHORT).show();
                } else if ("恢复下载".equals(start_task.getText().toString())) {
                    isPause = false;
                    show_state.setText("当前状态：下载ing  =￣ω￣=");
                    start_task.setText("暂停下载");
                    new Start(url_str, fileName, fileLength, threadNum, this);
                    Toast.makeText(MainActivity.this, "下载继续", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.cancel_task:
                new_task.setEnabled(true);
                cancel_task.setEnabled(false);
                show_state.setText("当前状态：被取消了  T_T");
                show_speed.setText("下载速度：0 KB/s");
                start_task.setText("重新下载");
                isPause = true;
                dEditor.clear();
                dEditor.apply();
                SharedPreferences.Editor editor = startCheck.edit();
                editor.clear();
                editor.apply();
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                File file = new File(path + File.separator + fileName);
                if (file.exists()) {
                    boolean isDelete = file.delete();
                    Log.d("MainActivity", "myDemo cancel: " + isDelete);
                }
                isFinish = false;
                break;
            case R.id.container:
                if (isFinish) {
                    path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    file = new File(path + File.separator + fileName);
                    // 配置Intent
                    Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction(Intent.ACTION_VIEW);
                    String fileType = OpenFile.getMIME(file);
                    Log.d("MainActivity", "myDemo onClick type: " + fileType);
                    // 判断安卓版本是否在7.0以上，7.0以上需要使用fileprovider打开文件
                    if (Build.VERSION.SDK_INT > 24) {
                        Uri uri = FileProvider.getUriForFile(MainActivity.this, "com.example.hsh.demo.fileprovider", file);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setDataAndType(uri, fileType);
                    } else
                        intent.setDataAndType(Uri.fromFile(file), fileType);
                    // 打开文件
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "找不到打开此文件的应用!", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.show_url:
                Toast.makeText(MainActivity.this, "文件地址: " + url_str, Toast.LENGTH_SHORT).show();
                break;
            case R.id.show_file_name:
                Toast.makeText(MainActivity.this, "文件名: " + fileName, Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    // 异步处理
    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INITIAL:
                    View view = findViewById(R.id.container);
                    view.setVisibility(View.VISIBLE);
                    show_url.setText("文件地址：" + url_str);
                    show_fileName.setText("文  件  名：" + fileName);
                    adjustLength(fileLength);
                    show_threadNum.setText("线程数量：" + threadNum);
                    show_state.setText("当前状态：暂停ing");
                    show_speed.setText("下载速度：0 KB/s");
                    show_progressNum.setText("下载进度：0 %");
                    break;
                case UPDATE:
                    progressBar.setProgress(progress);
                    show_progressNum.setText("下载进度：" + progress + " %");
                    dEditor.putInt("progress", progress);
                    dEditor.apply();
                    break;
                case FINISH:
                    show_state.setText("当前状态：下载完成(点击打开) =w=");
                    start_task.setText("完成下载");
                    start_task.setEnabled(false);
                    new_task.setEnabled(true);
                    dEditor.clear();
                    dEditor.apply();
                    SharedPreferences.Editor editor = startCheck.edit();
                    editor.clear();
                    editor.apply();
                    isFinish = true;
                    Toast.makeText(MainActivity.this, fileName + " 下载完成啦", Toast.LENGTH_SHORT).show();
                    break;
                case FAILED:
                    isPause = true;
                    new_task.setEnabled(true);
                    cancel_task.setEnabled(true);
                    start_task.setText("恢复下载");
                    show_state.setText("当前状态：下载出错了 (°ー°〃)");
                    Toast.makeText(MainActivity.this, "与服务器通信出现错误!", Toast.LENGTH_SHORT).show();
                    break;
                case SPEED:
                    adjustSpeed(speed);
                    break;
                case ERROR:
                    Toast.makeText(MainActivity.this, "获取文件信息错误!", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    // 新建下载任务方法
    private void newTask(final String urlStr) {
        final View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.new_task, null);
        final EditText url = (EditText) view.findViewById(R.id.get_url);
        final Spinner count = (Spinner) view.findViewById(R.id.choose_thread_num);
        // 显示对话框
        AlertDialog.Builder myDialog = new AlertDialog.Builder(MainActivity.this);
        myDialog.setTitle("新建下载");
        myDialog.setCancelable(true);
        myDialog.setView(view);
        myDialog.setPositiveButton("Start", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String url_str1 = url.getText().toString().trim();
                String getNum = count.getSelectedItem().toString();
                int num = Integer.parseInt(getNum);
                if (url_str1.length() != 0) {
                    initialize(url_str1, num);
                    start_task.setEnabled(true);
                    downloading = getSharedPreferences(fileName, MODE_PRIVATE);
                } else
                    Toast.makeText(MainActivity.this, "请输入资源地址", Toast.LENGTH_SHORT).show();
            }
        });
        myDialog.show();
        // 在输入框中设置下载地址
        if (urlStr.length() != 0)
            url.setText(urlStr);
        start_task.setText("开始下载");
        isFinish = false;
        if (url_str.length() != 0) {
            start_task.setEnabled(true);
        }
    }

    // 初始化方法，获取文件大小、文件名等
    private void initialize(final String url, final int num) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    url_str = url;
                    fileName = url.substring(url.lastIndexOf("/") + 1);
                    threadNum = num;
                    // 获取文件大小
                    URL url1 = new URL(url);
                    connection = (HttpURLConnection) url1.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.connect();
                    int code = connection.getResponseCode();
                    if (code == 200) {
                        fileLength = connection.getContentLength();
                        connection.disconnect();
                    } else {
                        fileLength = -1;
                        Log.d("Initialize", "myDemo run: -!-!-!- Request Error -!-!-!-");
                        handler.sendEmptyMessage(ERROR);
                    }
                    Log.d("Initialize", "myDemo url: " + url_str);
                    Log.d("Initialize", "myDemo File Name: " + fileName);
                    Log.d("Initialize", "myDemo File Length: " + fileLength);
                    Log.d("Initialize", "myDemo Thread threadNum: " + threadNum);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null)
                        connection.disconnect();
                    // 发送消息显示详细信息面板
                    handler.sendEmptyMessage(INITIAL);
                }
            }
        }).start();
    }

    // 启动检查
    private boolean check() {
        startCheck = getSharedPreferences("start check", MODE_PRIVATE);
        String name = startCheck.getString("file name", "");
        if (name.length() != 0) {
            fileName = name;
            return true;
        } else
            return false;
    }

    // 优化文件大小显示，以合适的单位表示文件大小
    private void adjustLength(long length) {
        if (length < 1024) {
            show_fileLength.setText("文件大小：" + fileLength + " B");
        } else if (length >= 1024 && length < 1024 * 1024) {
            double temp = length * 1.0 / 1024;
            String show = String.format("%.2f", temp);
            show_fileLength.setText("文件大小：" + show + " KB");
        } else if (length >= 1024 * 1024 && length < 1024 * 1024 * 1024) {
            double temp = length * 1.0 / 1024 / 1024;
            String show = String.format("%.2f", temp);
            show_fileLength.setText("文件大小：" + show + " MB");
        } else {
            double temp = length * 1.0 / 1024 / 1024 / 1024;
            String show = String.format("%.2f", temp);
            show_fileLength.setText("文件大小：" + show + " GB");
        }
    }

    // 优化速度显示，以合适的单位表示速度大小
    private void adjustSpeed(double sp) {
        if (sp < 1024) {
            String show = String.format("%.2f", sp);
            show_speed.setText("下载速度：" + show + " KB/s");
        } else if (sp >= 1024 && sp < 1024 * 1024) {
            double temp = sp / 1024;
            String show = String.format("%.2f", temp);
            show_speed.setText("下载速度：" + show + " MB/s");
        }
    }

    // 被调用方法，记录线程下载总字节
    void thread_record(String name, long size) {
        dEditor.putLong(name, size);
        dEditor.apply();
    }

    // 被调用方法，获取线程下载总字节
    long get_thread_record(int i) {
        return downloading.getLong("thread" + i, 0);
    }

    // 显示菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // 菜单点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.about_item) {
            AlertDialog.Builder myDialog = new AlertDialog.Builder(MainActivity.this);
            myDialog.setTitle("关于");
            myDialog.setMessage("Downloader  v1.0\n\n" +
                    "* 多线程下载\n" + "* 断点续传\n\n" +
                    "Created by H.S.H\non 2017.5.3");
            myDialog.setCancelable(true);
            myDialog.show();
        }
        return true;
    }
}
