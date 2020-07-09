package com.example.musicplayer;

import org.litepal.crud.LitePalSupport;

import java.util.Objects;

public class MusicDTO extends LitePalSupport implements Cloneable {

    private String artist;   //歌手
    private String title;     //歌曲名
    private String songUrl;     //歌曲地址
    private String imgUrl;
    private boolean isOnlineMusic;
    private int musicType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MusicDTO music = (MusicDTO) o;
        return isOnlineMusic == music.isOnlineMusic &&
                musicType == music.musicType &&
                Objects.equals(artist, music.artist) &&
                Objects.equals(title, music.title) &&
                Objects.equals(songUrl, music.songUrl) &&
                Objects.equals(imgUrl, music.imgUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artist, title, songUrl, imgUrl, isOnlineMusic, musicType);
    }

    public MusicDTO(String songUrl, String title, String artist, String imgUrl, boolean isOnlineMusic, int musicType) {
        this.title = title;
        this.artist = artist;
        this.songUrl = songUrl;
        this.imgUrl = imgUrl;
        this.isOnlineMusic = isOnlineMusic;
        this.musicType = musicType;
    }
    @Override
    public Object clone() {
        return new MusicDTO(songUrl, title, artist, imgUrl, isOnlineMusic, musicType);
    }
    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSongUrl() {
        return songUrl;
    }

    public void setSongUrl(String songUrl) {
        this.songUrl = songUrl;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public boolean isOnlineMusic() {
        return isOnlineMusic;
    }

    public void setOnlineMusic(boolean onlineMusic) {
        isOnlineMusic = onlineMusic;
    }

    public int getMusicType() {
        return musicType;
    }

    public void setMusicType(int musicType) {
        this.musicType = musicType;
    }
}