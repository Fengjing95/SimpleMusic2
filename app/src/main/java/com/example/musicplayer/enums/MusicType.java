package com.example.musicplayer.enums;

public interface MusicType {
    /**
     * 正在播放的
     */
    int PLAYING_MUSIC = 1;
    /**
     * 我的音乐
     */
    int MY_MUSIC = 2;
    /**
     * 本地音乐
     */
    int LOCAL_MUSIC = 3;
    /**
     * 在线音乐
     */
    int ONLINE_MUSIC = 4;
    /**
     * 已播放（去重计数用）
     */
    int PLAYED_MUSIC = 5;
    /**
     * 播放列表
     */
    int PLAY_LIST = 6;
}
