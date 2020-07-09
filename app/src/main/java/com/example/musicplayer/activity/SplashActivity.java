package com.example.musicplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.ActivityController;
import com.example.musicplayer.R;

import org.litepal.tablemanager.Connector;

/**
 * 打开软件暂停1秒，展示图片
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityController.addActivity(this);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);//去掉标题栏注意这句一定要写在setContentView()方法的前面，不然会报错的
        setContentView(R.layout.activity_splash);
        Connector.getDatabase();//创建数据库
//        LocalMusic localMusic = new LocalMusic("/storage/emulated/0/mymusic/世间美好与你环环相扣 - 柏松.mp3", "世间美好与你环环相扣 - 柏松.mp3", "柏松", "/storage/emulated/0/mymusic/c1bb3415dd22eebf89fb79b72d0e4060.png", false);
//        localMusic.save();
        Handler handler = new Handler();
        // 延迟SPLASH_DISPLAY_LENGHT时间然后跳转到MainActivity
        int SPLASH_DISPLAY_LENGHT = 1000;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DISPLAY_LENGHT);//两秒后调用此Runnable对象
    }
}
