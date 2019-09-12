package com.example.classicmusic.module;

import java.io.Serializable;

public class AudioData implements Serializable {

    private String data;
    private String title;
    private String album;
    private String artist;
    private String duration;

    public AudioData(String data, String title, String album, String artist, String duration) {
        this.data = data;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.duration = duration;
    }

    public String getDuration() {
        return duration;
    }

    public String getData() {
        return data;
    }

    public String getTitle() {
        return title;
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artist;
    }

}

