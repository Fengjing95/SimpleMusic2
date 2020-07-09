package com.example.musicplayer.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.musicplayer.ActivityController;
import com.example.musicplayer.LoadingDialog;
import com.example.musicplayer.MusicDTO;
import com.example.musicplayer.MusicAdapter;
import com.example.musicplayer.PlayingMusicAdapter;
import com.example.musicplayer.R;
import com.example.musicplayer.Utils;
import com.example.musicplayer.enums.MusicType;
import com.example.musicplayer.service.MusicService;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class LocalMusicActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "LocalMusicActivity";
    private TextView musicCountView;
    private ListView musicListView;
    private TextView playingTitleView;
    private TextView playingArtistView;
    private ImageView playingImgView;
    private ImageView btnPlayOrPause;

    private List<MusicDTO> localMusicList;
    private MusicAdapter adapter;
    private MusicService.MusicServiceBinder serviceBinder;
    private MusicUpdateTask updateTask;
    private LoadingDialog loadingDialog;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localmusic);
        ActivityController.addActivity(this);

        //初始化
        initActivity();

        // 列表项点击事件
        // 加入到播放列表
        musicListView.setOnItemClickListener((parent, view, position, id) -> {
            MusicDTO music = localMusicList.get(position);
            serviceBinder.addPlayList(music);
        });

        //列表项中更多按钮的点击事件
        adapter.setOnMoreButtonListener(i -> {
            final MusicDTO music = localMusicList.get(i);
            final String[] items = new String[] {"收藏到我的音乐", "添加到播放列表", "删除"};
            AlertDialog.Builder builder = new AlertDialog.Builder(LocalMusicActivity.this);
            builder.setTitle(music.getTitle()+"-"+music.getArtist());
            builder.setItems(items, (dialog, which) -> {
                switch (which){
                    case 0:
                        MainActivity.addMymusic(music);
                        break;
                    case 1:
                        serviceBinder.addPlayList(music);
                        break;
                    case 2:
                        //从列表和数据库中删除
                        localMusicList.remove(i);
                        LitePal.deleteAll(MusicDTO.class, "title = ? and musicType = ?", music.getTitle(), music.getMusicType() + "");
                        adapter.notifyDataSetChanged();
                        musicCountView.setText("播放全部(共"+ localMusicList.size()+"首)");
                        break;
                }
            });
            builder.create().show();
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            // 添加到播放列表
            case R.id.play_all:
                serviceBinder.addPlayList(localMusicList);
                break;
            // 刷新
            case R.id.refresh:
                localMusicList.clear();
                LitePal.deleteAll(MusicDTO.class);
                updateTask = new MusicUpdateTask();
                updateTask.execute();
                break;
            // 播放器
            case R.id.player:
                Intent intent = new Intent(LocalMusicActivity.this, PlayerActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.bottom_in, R.anim.bottom_silent);
                break;
            // 暂停播放
            case R.id.play_or_pause:
                serviceBinder.playOrPause();
                break;
            // 播放列表
            case R.id.playing_list:
                showPlayList();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(updateTask != null && updateTask.getStatus() == AsyncTask.Status.RUNNING) {
            updateTask.cancel(true);
        }
        updateTask = null;
        localMusicList.clear();
        unbindService(mServiceConnection);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initActivity(){
        //初始化控件
        ImageView btn_playAll = this.findViewById(R.id.play_all);
        musicCountView = this.findViewById(R.id.play_all_title);
        ImageView btn_refresh = this.findViewById(R.id.refresh);
        musicListView = this.findViewById(R.id.music_list);
        RelativeLayout playerToolView = this.findViewById(R.id.player);
        playingImgView = this.findViewById(R.id.playing_img);
        playingTitleView = this.findViewById(R.id.playing_title);
        playingArtistView = this.findViewById(R.id.playing_artist);
        btnPlayOrPause = this.findViewById(R.id.play_or_pause);
        ImageView btn_playingList = this.findViewById(R.id.playing_list);

        btn_playAll.setOnClickListener(this);
        btn_refresh.setOnClickListener(this);
        playerToolView.setOnClickListener(this);
        btnPlayOrPause.setOnClickListener(this);
        btn_playingList.setOnClickListener(this);

        localMusicList = new ArrayList<>();

        //绑定播放服务
        Intent i = new Intent(this, MusicService.class);
        bindService(i, mServiceConnection, BIND_AUTO_CREATE);

        // 使用ToolBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("本地音乐");
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setSupportActionBar(toolbar);

        //从数据库获取保存的本地音乐列表
        localMusicList.addAll(LitePal.findAll(MusicDTO.class)
                .stream()
                .filter(o -> o.getMusicType() != MusicType.LOCAL_MUSIC)
                .collect(Collectors.toList()));

        // 本地音乐列表绑定适配器
        adapter = new MusicAdapter(this, R.layout.music_item, localMusicList);
        musicListView.setAdapter(adapter);

        musicCountView.setText("播放全部(共"+ localMusicList.size()+"首)");
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
        // 绑定成功时调用
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            // 绑定成功后，取得MusicSercice提供的接口
            serviceBinder = (MusicService.MusicServiceBinder) service;

            // 注册监听器
            serviceBinder.registerOnStateChangeListener(listener);

            MusicDTO item = serviceBinder.getCurrentMusic();

            if (serviceBinder.isPlaying()){
                // 如果正在播放音乐, 更新控制栏信息
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
                // 当前有可播放音乐但没有播放
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
            // 断开连接时注销监听器
            serviceBinder.unregisterOnStateChangeListener(listener);
        }
    };

    // 实现监听器监听MusicService的变化，
    private MusicService.OnStateChangeListener listener = new MusicService.OnStateChangeListener() {

        @Override
        public void onPlayProgressChange(long played, long duration) {}

        @Override
        public void onPlay(MusicDTO item) {
            // 播放状态变为播放时
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
            // 播放状态变为暂停时
            btnPlayOrPause.setImageResource(R.drawable.bofang);
            btnPlayOrPause.setEnabled(true);
        }
    };

    // 异步获取本地所有音乐
    @SuppressLint("StaticFieldLeak")
    private class MusicUpdateTask extends AsyncTask<Object, MusicDTO, Void> {

        // 开始获取, 显示一个进度条
        @Override
        protected void onPreExecute(){
            loadingDialog = new LoadingDialog(LocalMusicActivity.this);
            loadingDialog.setMessage("获取本地音乐中...");
            loadingDialog.setCancelable(false);
            loadingDialog.show();
        }

        // 子线程中获取音乐
        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        protected Void doInBackground(Object... params) {

            String[] searchKey = new String[]{
                    MediaStore.Audio.Media._ID,     //对应文件在数据库中的检索ID
                    MediaStore.Audio.Media.TITLE,   //标题
                    MediaStore.Audio.Media.ARTIST,  //歌手
                    MediaStore.Audio.Albums.ALBUM_ID,   //专辑ID
                    MediaStore.Audio.Media.DURATION,     //播放时长
                    MediaStore.Audio.Media.IS_MUSIC     //是否为音乐文件
            };

            ContentResolver resolver = getContentResolver();
            Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, searchKey, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext() && !isCancelled()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    //通过URI和ID，组合出改音乐特有的Uri地址
                    Uri musicUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    int albumId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ID));
                    int isMusic = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC));
                    // 本地获取, 播放时长时间大于60s
                    if (isMusic != 0 && duration/(500*60) >= 2) {
                        //再通过专辑Id组合出音乐封面的Uri地址
                        Uri musicPic = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
                        MusicDTO data = new MusicDTO(musicUri.toString(), title, artist, musicPic.toString(), false, MusicType.LOCAL_MUSIC);
                        //切换到主线程进行更新
                        publishProgress(data);
                    }
                }
                cursor.close();
            }
            return null;
        }

        //主线程
        @Override
        protected void onProgressUpdate(MusicDTO... values) {
            MusicDTO data = values[0];
            //判断列表中是否已存在当前音乐
            if (!localMusicList.contains(data)){
                //添加到列表和数据库
                localMusicList.add(data);
                MusicDTO music = (MusicDTO) data.clone();
                music.save();
            }
            //刷新UI界面
            MusicAdapter adapter = (MusicAdapter) musicListView.getAdapter();
            adapter.notifyDataSetChanged();
            musicCountView.setText("播放全部(共"+ localMusicList.size()+"首)");
        }

        //任务结束, 关闭进度条
        @Override
        protected void onPostExecute(Void aVoid) {
            loadingDialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
