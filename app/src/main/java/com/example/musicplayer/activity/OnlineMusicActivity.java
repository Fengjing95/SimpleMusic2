package com.example.musicplayer.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.musicplayer.ActivityController;
import com.example.musicplayer.AppConstant;
import com.example.musicplayer.MusicDTO;
import com.example.musicplayer.MusicAdapter;
import com.example.musicplayer.PlayingMusicAdapter;
import com.example.musicplayer.R;
import com.example.musicplayer.Utils;
import com.example.musicplayer.enums.MusicType;
import com.example.musicplayer.service.MusicService;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OnlineMusicActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "OnlineMusicActivity";
    private TextView musicCountView;
    private ListView musicListView;
    private TextView playingTitleView;
    private TextView playingArtistView;
    private ImageView playingImgView;
    private ImageView btnPlayOrPause;

    private List<MusicDTO> onlineMusicList;
    private MusicService.MusicServiceBinder serviceBinder;
    private MusicAdapter adapter;

    private OkHttpClient client;
    private Handler mainHanlder;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onlinemusic);

        ActivityController.addActivity(this);

        //初始化
        initActivity();

        mainHanlder = new Handler(){
            @Override
            public void handleMessage(@NotNull Message msg) {
                super.handleMessage(msg);
                if (AppConstant.UPDATE_MUSIC == msg.what) {
                    //更新一首歌曲
                    MusicDTO music = (MusicDTO) msg.obj;
                    onlineMusicList.add(music);
                    adapter.notifyDataSetChanged();
                    musicCountView.setText("播放全部(共" + onlineMusicList.size() + "首)");
                }
            }
        };

        // 列表项点击事件
        musicListView.setOnItemClickListener((parent, view, position, id) -> {
            MusicDTO music = onlineMusicList.get(position);
            serviceBinder.addPlayList(music);
        });

        //列表项中更多按钮的点击事件
        adapter.setOnMoreButtonListener(i -> {
            final MusicDTO music = onlineMusicList.get(i);
            final String[] items = new String[] {"收藏到我的音乐", "添加到播放列表", "删除"};
            AlertDialog.Builder builder = new AlertDialog.Builder(OnlineMusicActivity.this);
            builder.setTitle(music.getTitle()+"-"+music.getArtist());

            builder.setItems(items, (dialog, which) -> {
                switch (which){
                    // 收藏
                    case 0:
                        MainActivity.addMymusic(music);
                        break;
                    // 添加到播放列表
                    case 1:
                        serviceBinder.addPlayList(music);
                        break;
                    // 刪除
                    case 2:
                        // 每次加载都会重新加载，所以此项功能无太大用处.单纯是为了适配
                        onlineMusicList.remove(i);
                        adapter.notifyDataSetChanged();
                        musicCountView.setText("播放全部(共"+ onlineMusicList.size()+"首)");
                        break;
                }
            });
            builder.create().show();
        });

    }

    // 监听组件
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.play_all:
                serviceBinder.addPlayList(onlineMusicList);
                break;
            case R.id.player:
                Intent intent = new Intent(OnlineMusicActivity.this, PlayerActivity.class);
                startActivity(intent);
                //弹出动画
                overridePendingTransition(R.anim.bottom_in, R.anim.bottom_silent);
                break;
            case R.id.play_or_pause:
                serviceBinder.playOrPause();
                break;
            case R.id.playing_list:
                showPlayList();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        onlineMusicList.clear();
        unbindService(mServiceConnection);
        client.dispatcher().cancelAll();
    }

    // 初始化活动
    private void initActivity(){
        //初始化控件
        ImageView btnPlayAll = this.findViewById(R.id.play_all);
        musicCountView = this.findViewById(R.id.play_all_title);
        musicListView = this.findViewById(R.id.music_list);
        RelativeLayout playerToolView = this.findViewById(R.id.player);
        playingImgView = this.findViewById(R.id.playing_img);
        playingTitleView = this.findViewById(R.id.playing_title);
        playingArtistView = this.findViewById(R.id.playing_artist);
        btnPlayOrPause = this.findViewById(R.id.play_or_pause);
        ImageView btn_playingList = this.findViewById(R.id.playing_list);

        // 设置监听
        btnPlayAll.setOnClickListener(this);
        playerToolView.setOnClickListener(this);
        btnPlayOrPause.setOnClickListener(this);
        btn_playingList.setOnClickListener(this);

        //绑定播放服务
        Intent i = new Intent(this, MusicService.class);
        bindService(i, mServiceConnection, BIND_AUTO_CREATE);

        // 使用ToolBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("网易云热歌榜");
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setSupportActionBar(toolbar);

        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)//设置连接超时时间
                .readTimeout(10, TimeUnit.SECONDS)//设置读取超时时间
                .build();

        // 获取在线音乐
        onlineMusicList = new ArrayList<>();
        adapter = new MusicAdapter(this, R.layout.music_item, onlineMusicList);
        musicListView.setAdapter(adapter);
        musicCountView.setText("播放全部(共"+ onlineMusicList.size()+"首)");
        getOlineMusic();
    }


    // 显示当前正在播放的音乐
    private void showPlayList(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //设计对话框的显示标题
        builder.setTitle("播放列表");

        //获取播放列表
        final List<MusicDTO> playingList = serviceBinder.getPlayingList();

        if(playingList.size() > 0) {
            //播放列表有曲目，显示所有音乐
            final PlayingMusicAdapter playingAdapter = new PlayingMusicAdapter(this, R.layout.playinglist_item, playingList);
            //监听列表项点击事件
            builder.setAdapter(playingAdapter, (dialog, which) -> serviceBinder.addPlayList(playingList.get(which)));

            //列表项中删除按钮的点击事件
            playingAdapter.setOnDeleteButtonListener(i -> {
                serviceBinder.removeMusic(i);
                playingAdapter.notifyDataSetChanged();
            });
        }
        else {
            //播放列表没有曲目，显示没有音乐
            builder.setMessage("没有正在播放的音乐");
        }

        //设置该对话框是可以自动取消的，例如当用户在空白处随便点击一下，对话框就会关闭消失
        builder.setCancelable(true);

        //创建并显示对话框
        builder.create().show();
    }

    // 定义与服务的连接的匿名类
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        //绑定成功时调用
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            //绑定成功后，取得MusicService提供的接口
            serviceBinder = (MusicService.MusicServiceBinder) service;

            //注册监听器
            serviceBinder.registerOnStateChangeListener(listenr);

            MusicDTO item = serviceBinder.getCurrentMusic();

            if (serviceBinder.isPlaying()){
                //如果正在播放音乐, 更新控制栏信息
                btnPlayOrPause.setImageResource(R.drawable.zanting);
                playingTitleView.setText(item.getTitle());
                playingArtistView.setText(item.getArtist());
                if (item.isOnlineMusic()){
                    Glide.with(getApplicationContext())
                            .load(item.getImgUrl())
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(playingImgView);
                }
                else {
                    ContentResolver resolver = getContentResolver();
                    Bitmap img = Utils.getLocalMusicBmp(resolver, item.getImgUrl());
                    Glide.with(getApplicationContext())
                            .load(img)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(playingImgView);
                }
            }
            else if (item != null){
                //当前有可播放音乐但没有播放
                btnPlayOrPause.setImageResource(R.drawable.bofang);
                playingTitleView.setText(item.getTitle());
                playingArtistView.setText(item.getArtist());
                if (item.isOnlineMusic()){
                    Glide.with(getApplicationContext())
                            .load(item.getImgUrl())
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(playingImgView);
                }
                else {
                    ContentResolver resolver = getContentResolver();
                    Bitmap img = Utils.getLocalMusicBmp(resolver, item.getImgUrl());
                    Glide.with(getApplicationContext())
                            .load(img)
                            .placeholder(R.drawable.defult_music_img)
                            .error(R.drawable.defult_music_img)
                            .into(playingImgView);
                }
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //断开连接时注销监听器
            serviceBinder.unregisterOnStateChangeListener(listenr);
        }
    };

    // 实现监听器监听MusicService的变化，
    private MusicService.OnStateChangeListener listenr = new MusicService.OnStateChangeListener() {

        @Override
        public void onPlayProgressChange(long played, long duration) {}

        @Override
        public void onPlay(MusicDTO item) {
            //播放状态变为播放时
            btnPlayOrPause.setImageResource(R.drawable.zanting);
            playingTitleView.setText(item.getTitle());
            playingArtistView.setText(item.getArtist());
            btnPlayOrPause.setEnabled(true);
            if (item.isOnlineMusic()){
                Glide.with(getApplicationContext())
                        .load(item.getImgUrl())
                        .placeholder(R.drawable.defult_music_img)
                        .error(R.drawable.defult_music_img)
                        .into(playingImgView);
            }
            else {
                ContentResolver resolver = getContentResolver();
                Bitmap img = Utils.getLocalMusicBmp(resolver, item.getImgUrl());
                Glide.with(getApplicationContext())
                        .load(img)
                        .placeholder(R.drawable.defult_music_img)
                        .error(R.drawable.defult_music_img)
                        .into(playingImgView);
            }
        }

        @Override
        public void onPause() {
            //播放状态变为暂停时
            btnPlayOrPause.setImageResource(R.drawable.bofang);
            btnPlayOrPause.setEnabled(true);
        }
    };

    // 获取在线音乐
    private void getOlineMusic() {

        Request request = new Request.Builder()
                .url("https://api.itooi.cn/netease/songList?id=3778678&format=1")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> Toast.makeText(OnlineMusicActivity.this, "网络错误", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String result = Objects.requireNonNull(response.body()).string();
                Log.d(TAG, "onResponse: "+result);
                try{
                    JSONObject obj = new JSONObject(result);
                    JSONArray songs = new JSONArray(obj.getString("data"));
                    Log.d(TAG, "onResponse: songs.length"+songs.length());
                    for(int i=0; i<songs.length(); i++){
                        JSONObject song = songs.getJSONObject(i);

                        String id = song.getString("id");
                        String songurl = "https://api.itooi.cn/netease/url?id=" + id + "&quality=128";
                        String name = song.getString("name");
                        String singer = song.getString("singer");
                        String pic = "https://api.itooi.cn/netease/pic?id=" + id;

                        //实例化一首音乐并发送到主线程更新
                        MusicDTO music = new MusicDTO(songurl, name, singer, pic, true, MusicType.ONLINE_MUSIC);
                        Message message = mainHanlder.obtainMessage();
                        message.what = AppConstant.UPDATE_MUSIC;
                        message.obj = music;
                        mainHanlder.sendMessage(message);
                    }
                }
                catch (Exception ignored){}
            }
        });
    }

    // 返回按钮
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
