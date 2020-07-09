package com.example.musicplayer.service;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.example.musicplayer.ActivityController;
import com.example.musicplayer.AppConstant;
import com.example.musicplayer.MusicDTO;
import com.example.musicplayer.enums.MusicType;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//@RequiresApi(api = Build.VERSION_CODES.O)
public class MusicService extends Service {

    private MediaPlayer player;
    private List<MusicDTO> playingMusicList;
    private List<OnStateChangeListener> listenerList;
    private MusicServiceBinder binder;
    private AudioManager audioManager;
    private MusicDTO currentMusic; // 当前就绪的音乐
    private boolean autoPlayAfterFocus;    // 获取焦点之后是否自动播放
    private boolean isNeedReload;     // 播放时是否需要重新加载
    private int playMode;  // 播放模式
    private SharedPreferences spf;

    @Override
    public void onCreate() {
        super.onCreate();
        initPlayList();     //初始化播放列表
        listenerList = new ArrayList<>();    //初始化监听器列表
        player = new MediaPlayer();   //初始化播放器
        player.setOnCompletionListener(onCompletionListener);   //设置播放完成的监听器
        binder = new MusicServiceBinder();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE); //获得音频管理服务
    }

    // 初始化播放列表
    private void initPlayList() {
        playingMusicList = new ArrayList<>();
        List<MusicDTO> list = LitePal.findAll(MusicDTO.class);
        for (MusicDTO i : list) {
            MusicDTO m = (MusicDTO) i.clone();
            m.setMusicType(MusicType.MY_MUSIC);
            playingMusicList.add(m);
        }
        if (playingMusicList.size() > 0) {
            currentMusic = playingMusicList.get(0);
            isNeedReload = true;
        }
    }

    //当前歌曲播放完成的监听器
    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {

            if (playMode == AppConstant.TYPE_SINGLE) {
                //单曲循环
                isNeedReload = true;
                playInner();
            } else {
                playNextInner();
            }
        }
    };

    //对外监听器接口
    public interface OnStateChangeListener {
        void onPlayProgressChange(long played, long duration);  //播放进度变化

        void onPlay(MusicDTO item);    //播放状态变化

        void onPause();   //播放状态变化
    }

    //定义binder与活动通信
    public class MusicServiceBinder extends Binder {

        // 添加一首歌曲
        public void addPlayList(MusicDTO item) {
            addPlayListInner(item);
        }

        // 添加多首歌曲
        public void addPlayList(List<MusicDTO> items) {
            addPlayListInner(items);
        }

        // 移除一首歌曲
        public void removeMusic(int i) {
            removeMusicInner(i);
        }


        public void playOrPause() {
            if (player.isPlaying()) {
                pauseInner();
            } else {
                playInner();
            }
        }

        // 下一首
        public void playNext() {
            playNextInner();
        }

        // 上一首
        public void playPre() {
            playPreInner();
        }

        // 获取当前播放模式
        public int getPlayMode() {
            return getPlayModeInner();
        }

        // 设置播放模式
        public void setPlayMode(int mode) {
            setPlayModeInner(mode);
        }

        // 设置播放器进度
        public void seekTo(int pos) {
            seekToInner(pos);
        }

        // 获取当前就绪的音乐
        public MusicDTO getCurrentMusic() {
            return getCurrentMusicInner();
        }

        // 获取播放器播放状态
        public boolean isPlaying() {
            return isPlayingInner();
        }

        // 获取播放列表
        public List<MusicDTO> getPlayingList() {
            return getPlayingListInner();
        }

        // 注册监听器
        public void registerOnStateChangeListener(OnStateChangeListener l) {
            listenerList.add(l);
        }

        // 注销监听器
        public void unregisterOnStateChangeListener(OnStateChangeListener l) {
            listenerList.remove(l);
        }
    }

    private void addPlayListInner(MusicDTO music) {
        if (!playingMusicList.contains(music)) {
            playingMusicList.add(0, music);
            MusicDTO playingMusic = (MusicDTO) music.clone();
            playingMusic.setMusicType(MusicType.PLAYING_MUSIC);
            playingMusic.save();
        }
        currentMusic = music;
        isNeedReload = true;
        playInner();
    }

    private void addPlayListInner(List<MusicDTO> musicList) {
        playingMusicList.clear();
        LitePal.deleteAll(MusicDTO.class);
        playingMusicList.addAll(musicList);
        for (MusicDTO i : musicList) {

            MusicDTO playingMusic = (MusicDTO) i.clone();
            playingMusic.setMusicType(MusicType.PLAYING_MUSIC);
            playingMusic.save();
        }
        currentMusic = playingMusicList.get(0);
        playInner();
    }

    private void removeMusicInner(int i) {
        LitePal.deleteAll(MusicDTO.class, "title=?", playingMusicList.get(i).getTitle());
        playingMusicList.remove(i);
    }


    private void playInner() {

        //获取音频焦点
        audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
//        AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
//                .setAcceptsDelayedFocusGain(true)
//                .setOnAudioFocusChangeListener(audioFocusListener)
////                .setOnAudioFocusChangeListener(audioFocusListener, handler)
//                .build();
//        audioManager.requestAudioFocus(audioFocusRequest);
        //如果之前没有选定要播放的音乐，就选列表中的第一首音乐开始播放
        if (currentMusic == null && playingMusicList.size() > 0) {
            currentMusic = playingMusicList.get(0);
            isNeedReload = true;
        }

        playMusicItem(currentMusic, isNeedReload);


    }

    private void pauseInner() {
        player.pause();

        for (OnStateChangeListener l : listenerList) {
            l.onPause();
        }
        // 暂停后不需要重新加载
        isNeedReload = false;
    }

    private void playPreInner() {
        //获取当前播放（或者被加载）音乐的上一首音乐
        //如果前面有要播放的音乐，把那首音乐设置成要播放的音乐
        int currentIndex = playingMusicList.indexOf(currentMusic);
        if (currentIndex - 1 >= 0) {
            currentMusic = playingMusicList.get(currentIndex - 1);
            isNeedReload = true;
            playInner();
        }
    }

    private void playNextInner() {

        if (playMode == AppConstant.TYPE_RANDOM) {
            //随机播放
            int i = (int) (0 + Math.random() * (playingMusicList.size() + 1));
            currentMusic = playingMusicList.get(i);
        } else {
            //列表循环
            int currentIndex = playingMusicList.indexOf(currentMusic);
            if (currentIndex < playingMusicList.size() - 1) {
                currentMusic = playingMusicList.get(currentIndex + 1);
            } else {
                currentMusic = playingMusicList.get(0);
            }
        }
        isNeedReload = true;
        playInner();
    }

    private void seekToInner(int pos) {
        //将音乐拖动到指定的时间
        player.seekTo(pos);
    }

    private MusicDTO getCurrentMusicInner() {
        return currentMusic;
    }

    private boolean isPlayingInner() {
        return player.isPlaying();
    }

    public List<MusicDTO> getPlayingListInner() {
        return playingMusicList;
    }

    private int getPlayModeInner() {
        return playMode;
    }

    private void setPlayModeInner(int mode) {
        playMode = mode;
    }

    // 将要播放的音乐载入MediaPlayer，但是并不播放
    private void prepareToPlay(MusicDTO item) {
        try {
            player.reset();
            //设置播放音乐的地址
            player.setDataSource(MusicService.this, Uri.parse(item.getSongUrl()));
            //准备播放音乐
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 播放音乐，根据reload标志位判断是非需要重新加载音乐
    private void playMusicItem(MusicDTO item, boolean reload) {

        if (item == null) {
            return;
        }

        if (reload) {
            //需要重新加载音乐
            prepareToPlay(item);
        }
        player.start();
        for (OnStateChangeListener l : listenerList) {
            l.onPlay(item);
        }
        isNeedReload = true;

        //移除现有的更新消息，重新启动更新
        handler.removeMessages(AppConstant.MESSAGE_FLAG);
        handler.sendEmptyMessage(AppConstant.MESSAGE_FLAG);
    }


    public static class PlayerReceiver extends BroadcastReceiver {

        public static final String PLAY_PRE = "play_pre";
        public static final String PLAY_NEXT = "play_next";
        public static final String PLAY_PAUSE = "play_pause";
        public static final String PLAY_PLAY = "play_play";
        public static final String CLOSE = "close";

        public MusicServiceBinder serviceBinder;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (PLAY_NEXT.equals(intent.getAction())) {//PLAY_NEXT
                Log.e("PlayerReceiver", "通知栏点击了下一首");
                serviceBinder.playNext();
            }
            if (PLAY_PRE.equals(intent.getAction())) {
                Log.e("PlayerReceiver", "通知栏点击了上一首");
                serviceBinder.playPre();
            }
            if (PLAY_PAUSE.equals(intent.getAction())) {
                Log.e("PlayerReceiver", "通知栏点击了暂停");
            }
            if (PLAY_PLAY.equals(intent.getAction())) {
                Log.e("PlayerReceiver", "通知栏点击了开始");
                serviceBinder.playOrPause();
            }
            if (CLOSE.equals(intent.getAction())) {
                Log.e("PlayerReceiver", "通知栏点击了close");
                @SuppressLint("WrongConstant") NotificationManager manager =
                        (NotificationManager) context.getSystemService("notification");
                manager.cancelAll();
                ActivityController.clearAll();
            }
        }
    }


    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == AppConstant.MESSAGE_FLAG) {//通知监听者当前的播放进度
                long played = player.getCurrentPosition();
                long duration = player.getDuration();
                for (OnStateChangeListener l : listenerList) {
                    l.onPlayProgressChange(played, duration);
                }
                //间隔一秒发送一次更新播放进度的消息
                sendEmptyMessageDelayed(AppConstant.MESSAGE_FLAG, 1000);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        //当组件bindService()之后，将这个Binder返回给组件使用
        return binder;
    }

    //焦点控制
    private AudioManager.OnAudioFocusChangeListener audioFocusListener = new AudioManager.OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (player.isPlaying()) {
                        //会长时间失去，所以告知下面的判断，获得焦点后不要自动播放
                        autoPlayAfterFocus = false;
                        pauseInner();//因为会长时间失去，所以直接暂停
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (player.isPlaying()) {
                        //短暂失去焦点，先暂停。同时将标志位置成重新获得焦点后就开始播放
                        autoPlayAfterFocus = true;
                        pauseInner();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (player.isPlaying()) {
                        //短暂失去焦点，先暂停。同时将标志位置成重新获得焦点后就开始播放
                        autoPlayAfterFocus = true;
                        pauseInner();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    //重新获得焦点，且符合播放条件，开始播放
                    if (!player.isPlaying() && autoPlayAfterFocus) {
                        autoPlayAfterFocus = false;
                        playInner();
                    }
                    break;
            }
        }
    };


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player.isPlaying()) {
            player.stop();
        }
        player.release();

        playingMusicList.clear();
        listenerList.clear();
        handler.removeMessages(AppConstant.MESSAGE_FLAG);
        audioManager.abandonAudioFocus(audioFocusListener); //注销音频管理服务
//        AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
//                .setOnAudioFocusChangeListener(audioFocusListener)
//                .build();
//        audioManager.abandonAudioFocusRequest(audioFocusRequest);
    }
}
