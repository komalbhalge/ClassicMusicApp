package com.example.classicmusic.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import com.example.classicmusic.R;
import com.example.classicmusic.background.MediaPlayerService;
import com.example.classicmusic.module.AudioData;
import com.example.classicmusic.module.AudioViewModel;
import com.example.classicmusic.utils.AudioServiceCallback;
import com.example.classicmusic.utils.Constants;
import com.example.classicmusic.utils.MyApplication;
import com.example.classicmusic.utils.StorageUtil;

import java.util.ArrayList;

public class AudioPlayActivity extends AppCompatActivity implements View.OnClickListener, AudioServiceCallback {
    private Button btnNext, btnPrevious, btnPlay;
    private TextView txAudioName, txAudioInfo, seekBarHint;
    private SeekBar audioProgress;
    private AudioViewModel audioViewModel;
    //List of available Audio files
    private ArrayList<AudioData> audioList;
    private AudioData activeAudio;
    private MediaPlayerService player;
    boolean serviceBound = false;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.classicmusic.view.PlayNewAudio";
    private int audioIndex = -1;
    private StorageUtil storage;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_play_activity);

        audioViewModel = ViewModelProviders.of(this).get(AudioViewModel.class);

        init();
    }


    private void init() {
        txAudioName=findViewById(R.id.audio_name);
        txAudioInfo=findViewById(R.id.audio_info);
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
        audioList = storage.loadAudio();
        audioIndex = storage.loadAudioIndex();

        audioProgress.setOnSeekBarChangeListener(new SeekBarListener());
        ActionBar actionBar=getSupportActionBar();
actionBar.setTitle(getResources().getString(R.string.actionbar_title));
actionBar.setDisplayHomeAsUpEnabled(true);
actionBar.setDisplayShowHomeEnabled(true);

        playAudio(audioIndex);
        updateAudio();
        //performAudioActions(PlaybackStatus.PLAYING);

    }
private class SeekBarListener implements SeekBar.OnSeekBarChangeListener{
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

        if (player!=null&&progress > 0 && player.mediaPlayer != null && !player.mediaPlayer.isPlaying()) {
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
        if (player.mediaPlayer != null ) {
            player.mediaPlayer.seekTo(seekBar.getProgress());
        }
    }

}
    public void updateAudio() {

        Log.e(Constants.TAG, "updateAudio()");
        audioList = storage.loadAudio();
        audioIndex = storage.loadAudioIndex();
        activeAudio=audioList.get(audioIndex);
        txAudioName.setText(activeAudio.getTitle());
        txAudioInfo.setText(activeAudio.getAlbum()+ " "+activeAudio.getArtist());

    }

    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.MyBinder binder = (MediaPlayerService.MyBinder) service;
            binder.getService().setCallBack(AudioPlayActivity.this);

            player = binder.getService();
            serviceBound = true;

            audioProgress.setMax(player.mediaPlayer.getDuration());
            mSeekbarUpdateHandler.postDelayed(mUpdateSeekbar, 0);
            Toast.makeText(AudioPlayActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };
    private Handler mSeekbarUpdateHandler = new Handler();
    private Runnable mUpdateSeekbar = new Runnable() {
        @Override
        public void run() {
            //if (player.mediaPlayer.isPlaying()) {
                audioProgress.setProgress(player.mediaPlayer.getCurrentPosition());
                mSeekbarUpdateHandler.postDelayed(this, 50);

            /*}else {
                audioProgress.setProgress(0);
            }*/
        }
    };

    private void playAudio(int audioIndex) {
        //Check is service is active
        if (!serviceBound) {
            audioViewModel.setAudioListData(audioList);
            audioViewModel.setAudioData(audioList.get(audioIndex));
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(audioList);
            Log.e(Constants.TAG, "Storing audio list: " + audioList.size());
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }

    }

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
            Log.e(Constants.TAG,"AudioPlayActivity: onDestroy");
            unbindService(serviceConnection);
            //service is active
            //player.stopSelf();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_next:
                player.skipToNext();
                player.updateMetaData();
                player.buildNotification(MediaPlayerService.PlaybackStatus.PLAYING);
                updateAudio();
                btnPlay.setBackgroundResource(R.drawable.ic_pause);
                mSeekbarUpdateHandler.postDelayed(mUpdateSeekbar, 0);
                //playbackAction(2);
                break;
            case R.id.button_pause:
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

                updateAudio();
                break;
            case R.id.btn_previous:
                //audioProgress.setProgress(0);
                mSeekbarUpdateHandler.postDelayed(mUpdateSeekbar, 0);
                player.skipToPrevious();
                player.updateMetaData();
                player.buildNotification(MediaPlayerService.PlaybackStatus.PLAYING);
                btnPlay.setBackgroundResource(R.drawable.ic_pause);
                updateAudio();
                //playbackAction(3);
                break;
        }
    }

    /*private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }*/

    @Override
    public void onAudioChange(AudioData audioData) {
        if (txAudioName == null) {
            txAudioName = findViewById(R.id.audio_name);
            Log.e(Constants.TAG, "onAudioChange()");
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
        if (item.getItemId()==android.R.id.home){
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
