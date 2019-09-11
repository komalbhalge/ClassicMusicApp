package com.example.classicmusic.adapter;

import com.example.classicmusic.module.AudioData;

public interface PlayerAdapter {
        void loadMedia(AudioData audioData);

        void release();

        boolean isPlaying();

        void play();

        void reset();

        void next();

        void previous();

        void pause();

        void initializeProgressCallback();

        void seekTo(int position);
}
