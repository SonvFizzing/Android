package com.example.administrator.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Choose_Course extends AppCompatActivity implements View.OnClickListener {

    private static final int Main_Free = 0;
    private static final int Free = 1;
    private static final int Limit = 2;

    private ImageView code_image;
    private EditText course_id_input;
    private EditText code_input;
    private WebView web;
    private File file;

    private String url;
    private String cookie;
    String queryStr;
    String code_str;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("开始选课");
        setContentView(R.layout.choose);
        // 获取传递信息
        Intent intent = getIntent();
        cookie = intent.getStringExtra("cookie");
        url = intent.getStringExtra("url");
        // 获取验证码
        getImg();
        // 获取组件
        Button ensure = (Button) findViewById(R.id.ensure);
        Button cancel = (Button) findViewById(R.id.cancel);
        code_image = (ImageView) findViewById(R.id.image_code);
        course_id_input = (EditText) findViewById(R.id.course_code_input);
        code_input = (EditText) findViewById(R.id.choose_code_input);
        web = (WebView) findViewById(R.id.show_result);
        // 组件设置
        web.getSettings().setDefaultTextEncodingName("GB2312"); // 设置为GB2312
        web.setWebViewClient(new WebViewClient());
        ensure.setOnClickListener(this);
        cancel.setOnClickListener(this);
        code_image.setOnClickListener(this);

    }

    // 异步处理
    public Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Main_Free:
                    Toast.makeText(Choose_Course.this, "主选可以选啦!", Toast.LENGTH_SHORT).show();
                    break;
                case Free:
                    Toast.makeText(Choose_Course.this, "非主选可以选啦!", Toast.LENGTH_SHORT).show();
                    break;
                case Limit:
                    Toast.makeText(Choose_Course.this, "还没有空位...", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    // 获取验证码
    void getImg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // double a = Math.random();
                    String getImg = url + "/code.asp";
                    Log.d("1", "getImg: " + getImg);
                    URL u = new URL(getImg);
                    HttpURLConnection connection = (HttpURLConnection) u.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "image/webp,image/*,*/*;q=0.8");
                    connection.setRequestProperty("Cookie", cookie);

                    DataInputStream input = new DataInputStream(connection.getInputStream());
                    file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "codeimg1.jpg");
                    FileOutputStream output = new FileOutputStream(file);
                    byte[] b = new byte[1024];
                    int len;
                    while ((len = input.read(b)) != -1) {
                        output.write(b, 0, len);
                    }
                    output.flush();
                    output.close();
                    input.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            code_image.setImageURI(null);
                            code_image.setImageURI(Uri.fromFile(file));
                            Log.d("1", "img1 run code");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ensure) {
            queryStr = course_id_input.getText().toString();
            code_str = code_input.getText().toString();
            is_select(queryStr);    // 查询是否可选该课程
        } else if (v.getId() == R.id.cancel) {
            finish();
        } else if (v.getId() == R.id.image_code) {
            getImg();
        }
    }


    void is_select(final String str) {
        // 抓取人数信息
        new Thread(new Runnable() {
            @Override
            public void run() {
                String num_status1;
                num_status1 = getRequest("/sele_count1.asp?course_no=" + str);
                Log.d("1", "run getrequest: " + num_status1);
                String num_regex = "<br>主选学生限制人数：([\\d]+)&nbsp;&nbsp;主选学生已选人数：([\\d]+)<br>非主选学生限制人数：([\\d]+)&nbsp;&nbsp;非主选学生已选人数：([\\d]+)</body>";
                Pattern pattern1 = Pattern.compile(num_regex);
                Matcher matcher1 = pattern1.matcher(num_status1);
                while (matcher1.find()) {
                    int main_limit_num = Integer.parseInt(matcher1.group(1));
                    int main_chosen_num = Integer.parseInt(matcher1.group(2));
                    int limit_num = Integer.parseInt(matcher1.group(3));
                    int chosen_num = Integer.parseInt(matcher1.group(4));
                    Log.d("1", "main_limit num: " + main_limit_num);
                    Log.d("1", "main_chosen num: " + main_chosen_num);
                    Log.d("1", "limit num: " + limit_num);
                    Log.d("1", "chosen num: " + chosen_num);
                    if (main_limit_num > main_chosen_num) {     // 主选有空位
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    web.loadUrl(url + "/choose.asp?GetCode=" + code_str + "&no_type=" + queryStr
                                            + URLEncoder.encode("必修", "utf-8") + "&submit=%E7%A1%AE++++%E5%AE%9A");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } else if (limit_num > chosen_num) {        // 非主选有空位
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    web.loadUrl(url + "/choose.asp?GetCode=" + code_str + "&no_type=" + queryStr
                                            + URLEncoder.encode("必修", "utf-8") + "&submit=%E7%A1%AE++++%E5%AE%9A");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } else {                                    // 没有空位
                        Looper.prepare();
                        Toast.makeText(Choose_Course.this, "还没有空位...", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }
                }
            }
        }).start();
    }

    // 获得相应课程的人数信息
    private String getRequest(String keyword) {
        try {
            String get_result = url + keyword;
            Log.d("1", "getRequest: " + get_result);
            URL u = new URL(get_result);
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Cookie", cookie);

            Reader r = new InputStreamReader(connection.getInputStream(), "GB2312");
            int c;
            String html = "";
            while ((c = r.read()) != -1) {
                html += (char) c;
            }
            r.close();
            return html;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
