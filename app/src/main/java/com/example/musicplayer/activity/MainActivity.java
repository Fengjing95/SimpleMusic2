package com.example.musicplayer.activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

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
import com.google.android.material.navigation.NavigationView;
import com.jaeger.library.StatusBarUtil;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "MainActivity";
    private Toolbar toolbar;
    private ListView musicListView;
    private TextView playingTitleView;
    private TextView playingArtistView;
    private ImageView playingImgView;
    private ImageView btnPlayOrPause;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView musicCountView;

    private static List<MusicDTO> musicList;
    private TextView navPhone;
    private static List<Music> musicList;
    private MusicAdapter musicAdapter;
    private SharedPreferences spf;
    private MusicService.MusicServiceBinder serviceBinder;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityController.addActivity(this);

        // 通知栏状态
        stateBar();

        // 初始化活动
        initActivity();

        // 初始化音乐列表
        initMusicList();
        loadCache();

        // 初始化配置
        initSettings();

        // 点击列表项播放音乐
        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MusicDTO music = musicList.get(position);
                serviceBinder.addPlayList(music);
            }
        });

        // 列表项中更多按钮的点击事件
        musicAdapter.setOnMoreButtonListener(new MusicAdapter.onMoreButtonListener() {
            @Override
            public void onClick(final int i) {
                final MusicDTO music = musicList.get(i);

                //弹出操作对话框
                final String[] items = new String[] {"添加到播放列表", "删除"};
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(music.getTitle()+"-"+music.getArtist());

                builder.setItems(items, (dialog, which) -> {
                    switch (which){
                        case 0:
                            serviceBinder.addPlayList(music);
                            break;
                        case 1:
                            //从列表和数据库中删除
                            musicList.remove(i);
                            LitePal.deleteAll(MusicDTO.class, "title = ? and musicType = ?", music.getTitle(), "" + music.getMusicType());
                            musicAdapter.notifyDataSetChanged();
                            break;
                    }
                });
                builder.create().show();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadCache() {
        List<MusicDTO> played = LitePal.findAll(MusicDTO.class)
                .stream()
                .filter(i -> i.getMusicType() != MusicType.PLAYED_MUSIC)
                .collect(Collectors.toList());
        ConcurrentHashMap<MusicDTO, Boolean> concurrentHashMap = new ConcurrentHashMap<>();
        played.forEach(i -> concurrentHashMap.put(i, true));
        Utils.initMusicCache(concurrentHashMap);
    }


    // 初始化活动
    private void initActivity(){
        Log.d(TAG, "initActivity: 1");
        // 申请读写权限
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{WRITE_EXTERNAL_STORAGE}, 1);

        //绑定控件
        musicListView = this.findViewById(R.id.music_list);
        RelativeLayout playerToolView = this.findViewById(R.id.player);
        playingImgView = this.findViewById(R.id.playing_img);
        playingTitleView = this.findViewById(R.id.playing_title);
        playingArtistView = this.findViewById(R.id.playing_artist);
        btnPlayOrPause = this.findViewById(R.id.play_or_pause);
        ImageView btn_playingList = this.findViewById(R.id.playing_list);
        drawerLayout = this.findViewById(R.id.drawer);
        navigationView = this.findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        musicCountView = headerView.findViewById(R.id.nav_num);

        // 设置监听
        playerToolView.setOnClickListener(this);
        btnPlayOrPause.setOnClickListener(this);
        btn_playingList.setOnClickListener(this);
        
        // 使用ToolBar
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        // 状态栏沉浸
        StatusBarUtil.setTransparentForDrawerLayout(this, drawerLayout);

        // 设置侧边栏
        setNavigationView();

        // 启动service并绑定
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initMusicList(){
        Log.d(TAG, "initMusicList: 2");
        //从数据库获取我的音乐
        musicList = new ArrayList<>();

        List<MusicDTO> list = LitePal.findAll(MusicDTO.class)
                    .stream()
                    .filter(i -> i.getMusicType() != MusicType.MY_MUSIC)
                    .collect(Collectors.toList());

        list.forEach(i -> i.setMusicType(MusicType.PLAYING_MUSIC));
        musicList.addAll(list);
        Log.d(TAG, "initMusicList: 2--------"+list.size());


        // 音乐列表绑定适配器
        musicAdapter = new MusicAdapter(this, R.layout.music_item, musicList);
        musicListView.setAdapter(musicAdapter);
    }

    // 读取配置
    private void initSettings(){
        spf = getSharedPreferences("settings", MODE_PRIVATE);
        //累计听歌数量
        musicCountView.setText("累计听歌"+ Utils.MUSIC_CACHE.size() +"首");
    }

    // 保存配置
    private void saveSettings(){
        SharedPreferences.Editor editor = spf.edit();

        //累计听歌数量
        editor.putInt("listen_count", Utils.MUSIC_CACHE.size());
        // 播放模式
        int mode = serviceBinder.getPlayMode();
        editor.putInt("play_mode", mode);

        editor.apply();
    }

    // 设置侧边栏
    private void setNavigationView(){

        // 使用toggle控制侧边栏弹出:
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout,toolbar, R.string.app_name, R.string.app_name);
        toggle.syncState();
        drawerLayout.addDrawerListener(toggle);

        //侧边栏菜单项点击事件
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                switch (menuItem.getItemId()){
                    case R.id.local:
                        // 进入本地音乐
                        Intent intent1 = new Intent(MainActivity.this, LocalMusicActivity.class);
                        startActivity(intent1);
                        break;
                    case R.id.online:
                        // 进入在线音乐
                        Intent intent2 = new Intent(MainActivity.this, OnlineMusicActivity.class);
                        startActivity(intent2);
                        break;
                    case R.id.login:
                        // 进入登录
                        Intent intent3 = new Intent(MainActivity.this, LoginActivity.class);
                        startActivityForResult(intent3,555);
                        break;
//                    case R.id.register:
//                        // 进入注册
//                        Intent intent4 = new Intent(MainActivity.this, RegisterActivity.class);
//                        startActivity(intent4);
//                        break;
                    case R.id.exit:
                        // 退出
                        unbindService(serviceConnection);
                        Intent intent = new Intent(MainActivity.this, MusicService.class);
                        stopService(intent);
                        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                        manager.cancelAll();
                        ActivityController.clearAll();
                        break;

                }
                return true;
            }
        });
    }
    // 监听组件
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.player:
                // 进入播放器
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.bottom_in, R.anim.bottom_silent);
                break;
            case R.id.play_or_pause:
                serviceBinder.playOrPause();
                break;
            case R.id.playing_list:
                // 显示正在播放列表
                showPlayingList();
                break;
            default:
        }
    }

    // 显示当前正在播放的音乐
    private void showPlayingList(){

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //设计对话框的显示标题
        builder.setTitle("播放列表");
        //获取播放列表
        final List<MusicDTO> playingList = serviceBinder.getPlayingList();

        if(playingList.size() > 0) {

            //播放列表有曲目，显示所有音乐
            final PlayingMusicAdapter playingAdapter = new PlayingMusicAdapter(this, R.layout.playinglist_item, playingList);
            builder.setAdapter(playingAdapter, new DialogInterface.OnClickListener() {
                //监听列表项点击事件
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    serviceBinder.addPlayList(playingList.get(which));
                }
            });
            //列表项中删除按钮的点击事件
            playingAdapter.setOnDeleteButtonListener(new PlayingMusicAdapter.onDeleteButtonListener() {
                @Override
                public void onClick(int i) {
                    serviceBinder.removeMusic(i);
                    playingAdapter.notifyDataSetChanged();
                }
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

    // 与后台服务连接的匿名类
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //绑定成功后，取得MusicSercice提供的接口
            serviceBinder = (MusicService.MusicServiceBinder) service;

            //注册监听器
            serviceBinder.registerOnStateChangeListener(listenr);

            MusicDTO item = serviceBinder.getCurrentMusic();

            if(item != null){
                //当前有可播放音乐
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

            int mode = spf.getInt("play_mode", AppConstant.TYPE_ORDER);
            serviceBinder.setPlayMode(mode);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //断开连接之后, 注销监听器
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
        }
    };

    // 对外接口, 插入一首歌曲
    public static  void addMymusic(MusicDTO item){
        if (musicList.contains(item)) {
            return;
        }
        //添加到列表和数据库
        musicList.add(0, item);
        MusicDTO music = (MusicDTO) item.clone();
        music.setMusicType(MusicType.ONLINE_MUSIC);
        music.save();
    }

//    // 对外接口, 插入多首歌曲
//    public static void addMymusic(List<Music> lists){
//
//        for (Music item:lists){
//            addMymusic(item);
//        }
//    }




    public void stateBar() {
        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, 0);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification);

        Intent intentPause = new Intent("play_play");//发送播放音乐的通知(z暂停/播放，动态调整)
        PendingIntent pIntentPause = PendingIntent.getBroadcast(this, 0,
                intentPause, 0);
        remoteViews.setOnClickPendingIntent(R.id.playandpause, pIntentPause);

        Intent intentNext = new Intent("play_next");//发送播放下一曲的通知
        PendingIntent pIntentNext = PendingIntent.getBroadcast(this, 0,
                intentNext, 0);
        remoteViews.setOnClickPendingIntent(R.id.next, pIntentNext);

        Intent intentLast = new Intent("play_pre");//发送播放上一曲的通知
        PendingIntent pIntentLast = PendingIntent.getBroadcast(this, 0,
                intentLast, 0);
        remoteViews.setOnClickPendingIntent(R.id.pre, pIntentLast);

        Intent intentCancel = new Intent("close");//发送关闭通知栏的通知
        PendingIntent pIntentCancel = PendingIntent.getBroadcast(this, 0,
                intentCancel, 0);
        remoteViews.setOnClickPendingIntent(R.id.close, pIntentCancel);

        Notification notification = new Notification.Builder(MainActivity.this)
                .setSmallIcon(R.drawable.app_img)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setContent(remoteViews)
                .build();

        manager.notify(1, notification);
    }

    // 显示菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // 菜单点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.local_music:
                Intent intent1 = new Intent(MainActivity.this, LocalMusicActivity.class);
                startActivity(intent1);
                break;
            case R.id.online_music:
                Intent intent2 = new Intent(MainActivity.this, OnlineMusicActivity.class);
                startActivity(intent2);
                break;
            default:
        }
        return true;
    }
    @Override
    protected void onResume() {
        super.onResume();
        musicCountView.setText("累计听歌"+  Utils.MUSIC_CACHE.size() +"首");
        musicAdapter.notifyDataSetChanged(); //刷新列表
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 555) {
            assert data != null;
            String phoneNum = data.getStringExtra("username");
            if (phoneNum != null) {
                String str = "用户" + phoneNum;
                navPhone = this.findViewById(R.id.nav_phone);
                navPhone.setText(str);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //清空列表
        musicList.clear();
        Glide.with(getApplicationContext()).pauseAllRequests();
        unbindService(serviceConnection);
        saveSettings();
    }
}
