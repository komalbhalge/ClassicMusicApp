package com.example.classicmusic.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.classicmusic.R;
import com.example.classicmusic.background.MediaPlayerService;
import com.example.classicmusic.module.AudioData;
import com.example.classicmusic.utils.AudioServiceCallback;
import com.example.classicmusic.utils.Constants;
import com.example.classicmusic.utils.MyApplication;
import com.example.classicmusic.utils.StorageUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class AudioPlayActivity extends AppCompatActivity implements View.OnClickListener, AudioServiceCallback {

    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.classicmusic.view.PlayNewAudio";

    //*Activity views*/
    private Button btnNext, btnPrevious, btnPlay;
    private TextView txAudioName, txAudioInfo, seekBarHint;
    private SeekBar audioProgress;
    private ImageView audioCover;
    //*List of available Audio files*//
    private ArrayList<AudioData> audioList = new ArrayList<>();
    private AudioData activeAudio;

    private StorageUtil storage;
    private MediaPlayerService player;

    private int audioIndex = -1;
    boolean serviceBound = false;
    private int resumePosition = 0;

    private Handler mSeekbarUpdateHandler = new Handler();
    private MediaPlayer intentMediaPlayer;
    private boolean ifFromIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_play_activity);

        init();
    }

    private boolean isHandleIncomingIntentRequest() {
        // Get the intent that started this activity
        Intent intent = getIntent();
        Uri data = intent.getData();

        // Figure out what to do based on the intent type
        if (intent != null && intent.getType() != null && intent.getType().indexOf("audio/*") != -1) {
            btnPrevious.setEnabled(false);
            btnNext.setEnabled(false);
            btnNext.setClickable(false);
            Log.e("Apps:", ".mp3" + " " + data);
            // Handle intents with image data ...
            writeAudioDate(data);
            intentMediaPlayer = new MediaPlayer();
            try {
                // mediaPlayer.setDataSource(String.valueOf(myUri));
                intentMediaPlayer.setDataSource(AudioPlayActivity.this, data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                intentMediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            intentMediaPlayer.start();
            updateAudio();
            return true;
        } else {
            return false;
        }

    }

    private void init() {
        audioCover = findViewById(R.id.audio_cover);
        txAudioName = findViewById(R.id.audio_name);
        txAudioInfo = findViewById(R.id.audio_info);
        btnNext = findViewById(R.id.btn_next);
        btnPlay = findViewById(R.id.button_pause);
        btnPrevious = findViewById(R.id.btn_previous);
        seekBarHint = findViewById(R.id.audio_time);
        audioProgress = findViewById(R.id.seekbar_audio);

        btnNext.setOnClickListener(this);
        btnPlay.setOnClickListener(this);
        btnPrevious.setOnClickListener(this);

        //Load data from SharedPreferences
        storage = new StorageUtil(getApplicationContext());
        // audioList = storage.loadAudio();
        //audioIndex = storage.loadAudioIndex();

        audioProgress.setOnSeekBarChangeListener(new SeekBarListener());
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.actionbar_title));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        ifFromIntent = isHandleIncomingIntentRequest();

        if (!ifFromIntent) {
            audioList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();
            //*Start playing the Audio here*//
            playAudio(audioIndex);

            /* Update the view here*/
            updateAudio();
        }
    }

    public void updateAudio() {
        Log.e("Index", audioIndex + " : audioIndex");
        if (audioIndex != -1) {
            activeAudio = audioList.get(audioIndex);
            txAudioName.setText(activeAudio.getTitle());
            txAudioInfo.setText(activeAudio.getAlbum() + " " + activeAudio.getArtist());

            Bitmap coverImage = storage.getAudioCoverImage(activeAudio.getData());
            if (coverImage != null) {
                audioCover.setImageBitmap(coverImage);
            }
        }
    }

    private void playAudio(int audioIndex) {
        //Check is service is active
        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            storage.storeAudioIndex(audioIndex);
            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        } else {
            //Store the new audioIndex to SharedPreferences
            storage.storeAudioIndex(audioIndex);
            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }

    }

    private Runnable mUpdateSeekbar = new Runnable() {
        @Override
        public void run() {
            /*To continuously update the seekbar progress as the audio plays*/
            if (player != null && player.mediaPlayer.isPlaying()) {
                if (player.mediaPlayer.getCurrentPosition() >= player.mediaPlayer.getDuration()) {
                    mSeekbarUpdateHandler.removeCallbacks(this);
                } else {
                    audioProgress.setProgress(player.mediaPlayer.getCurrentPosition());
                    mSeekbarUpdateHandler.postDelayed(this, 50);
                }

            }
        }
    };

    private void writeAudioDate(Uri uri) {
        String path = null;
        try {
            path = new File(uri.getPath()).getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
        metaRetriver.setDataSource(path);
        String title = null, album, artist, duration;
        try {

            title = metaRetriver
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            Log.e("Index", "Title: " + title);
            album = metaRetriver
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            artist = metaRetriver
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            duration = metaRetriver
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        } catch (Exception e) {

            album = "Unknown Album";
            artist = "Unknown Artist";
            duration = "Unknown Genre";
        }

        String song_duration = "";
        if (String.valueOf(duration) != null) {
            try {
                Long time = Long.valueOf(duration);
                long seconds = time / 1000;
                long minutes = seconds / 60;
                seconds = seconds % 60;

                if (seconds < 10) {
                    song_duration = String.valueOf(minutes) + ":0" + String.valueOf(seconds);

                } else {
                    song_duration = String.valueOf(minutes) + ":" + String.valueOf(seconds);
                    //song.put("songDuration", ccsongs_duration);
                }
            } catch (NumberFormatException e) {
                song_duration = duration;
            }
        } else {
            song_duration = "0";

        }
        // Save to audioList
        audioList.add(new AudioData("", title, album, artist, song_duration));
        Log.e("Index", audioList.size() + "");
        audioIndex = 0;

    }

    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to MediaPlayerService, cast the IBinder and get MediaPlayerService instance
            MediaPlayerService.MyBinder binder = (MediaPlayerService.MyBinder) service;
            binder.getService().setCallBack(AudioPlayActivity.this);

            player = binder.getService();
            serviceBound = true;

            audioProgress.setMax(player.mediaPlayer.getDuration());
            mSeekbarUpdateHandler.postDelayed(mUpdateSeekbar, 0);
            //Toast.makeText(AudioPlayActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            Log.d(Constants.TAG, "AudioPlayActivity: onDestroy");
            unbindService(serviceConnection);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.btn_next: /* Play Next Audio clicked*/
                player.skipToNext();
                player.updateMetaData();
                player.buildNotification(MediaPlayerService.PlaybackStatus.PLAYING);
                btnPlay.setBackgroundResource(R.drawable.ic_pause);
                mSeekbarUpdateHandler.postDelayed(mUpdateSeekbar, 0);
                updateAudio();
                break;

            case R.id.button_pause: /* Play/Pause the Audio clicked*/
                if (player != null) {
                    if (player.mediaPlayer != null) {
                        if (player.mediaPlayer.isPlaying()) {
                            player.pauseMedia();
                            player.buildNotification(MediaPlayerService.PlaybackStatus.PAUSED);
                            mSeekbarUpdateHandler.removeCallbacks(mUpdateSeekbar);
                        } else {
                            mSeekbarUpdateHandler.postDelayed(mUpdateSeekbar, 0);
                            player.resumeMedia();
                            player.buildNotification(MediaPlayerService.PlaybackStatus.PLAYING);
                        }
                    }
                } else {

                    if (intentMediaPlayer.isPlaying()) { btnPlay.setBackgroundResource(R.drawable.ic_play);
                        resumePosition = intentMediaPlayer.getCurrentPosition();
                        intentMediaPlayer.pause();
                        mSeekbarUpdateHandler.removeCallbacks(mUpdateSeekbar);
                    } else { btnPlay.setBackgroundResource(R.drawable.ic_pause);
                        mSeekbarUpdateHandler.postDelayed(mUpdateSeekbar, 0);
                        intentMediaPlayer.seekTo(resumePosition);
                        intentMediaPlayer.start();

                    }

                }
                updateAudio();
                break;

            case R.id.btn_previous: /* Play Previous Audio clicked*/

                player.skipToPrevious();
                player.updateMetaData();
                player.buildNotification(MediaPlayerService.PlaybackStatus.PLAYING);
                btnPlay.setBackgroundResource(R.drawable.ic_pause);
                mSeekbarUpdateHandler.postDelayed(mUpdateSeekbar, 0);
                updateAudio();

                break;
        }
    }

    @Override
    public void onAudioChange(AudioData audioData) {
        if (txAudioName == null) {
            txAudioName = findViewById(R.id.audio_name);
            Log.d(Constants.TAG, "onAudioChange()");
        }
        txAudioName.setText(audioData.getTitle());
    }

    @Override
    public void onAudioPausePlay(boolean isPlaying) {

        if (isPlaying) {
            mSeekbarUpdateHandler.postDelayed(mUpdateSeekbar, 0);

            btnPlay.setBackgroundResource(R.drawable.ic_pause);
        } else {
            mSeekbarUpdateHandler.removeCallbacks(mUpdateSeekbar);
            btnPlay.setBackgroundResource(R.drawable.ic_play);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

            int x = (int) Math.ceil(progress / 1000f);

            long minutes = x / 60;
            long seconds = x % 60;

            if (seconds < 10)
                seekBarHint.setText(String.valueOf(minutes) + ":0" + String.valueOf(seconds));
            else
                seekBarHint.setText(String.valueOf(minutes) + ":" + String.valueOf(seconds));

            double percent = progress / (double) seekBar.getMax();
            int offset = seekBar.getThumbOffset();
            int seekWidth = seekBar.getWidth();
            int val = (int) Math.round(percent * (seekWidth - 2 * offset));
            int labelWidth = seekBarHint.getWidth();
            seekBarHint.setX(offset + seekBar.getX() + val
                    - Math.round(percent * offset)
                    - Math.round(percent * labelWidth / 2));

            if (player != null && progress > 0 && player.mediaPlayer != null && !player.mediaPlayer.isPlaying()) {
                //clearMediaPlayer();
                AudioPlayActivity.this.audioProgress.setProgress(0);
            }

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            if (player.mediaPlayer != null && player.mediaPlayer.isPlaying()) {
                player.mediaPlayer.seekTo(seekBar.getProgress());
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            seekBarHint.setVisibility(View.VISIBLE);
            if (player.mediaPlayer != null) {
                player.mediaPlayer.seekTo(seekBar.getProgress());
            }
        }

    }
}
