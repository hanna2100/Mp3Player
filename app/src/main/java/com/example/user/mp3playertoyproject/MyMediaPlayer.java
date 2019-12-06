package com.example.user.mp3playertoyproject;

import android.media.MediaPlayer;

public class MyMediaPlayer {

    private static MediaPlayer mediaPlayer = null;

    private static class MyMediaPlayerHolder {
        public static final MediaPlayer INSTANCE = new MediaPlayer();
    }

    public static MediaPlayer getMediaPlayer() {
        return MyMediaPlayerHolder.INSTANCE;
    }

}
