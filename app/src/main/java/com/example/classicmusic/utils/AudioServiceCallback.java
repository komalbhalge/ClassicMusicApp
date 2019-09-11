package com.example.classicmusic.utils;

import com.example.classicmusic.module.AudioData;

public interface AudioServiceCallback {
    void onAudioChange(AudioData audioData);

    void onAudioPausePlay(boolean isPlaying);
}
