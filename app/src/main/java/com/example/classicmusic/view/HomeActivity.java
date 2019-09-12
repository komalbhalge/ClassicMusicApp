package com.example.classicmusic.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.classicmusic.OnItemClickListener;
import com.example.classicmusic.R;
import com.example.classicmusic.adapter.AudioAdapter;
import com.example.classicmusic.background.MediaPlayerService;
import com.example.classicmusic.module.AudioData;
import com.example.classicmusic.utils.Constants;
import com.example.classicmusic.utils.MyApplication;
import com.example.classicmusic.utils.StorageUtil;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.classicmusic.view.PlayNewAudio";
    private boolean serviceBound = false;
    private RecyclerView recyclerView;
    private AudioAdapter adapter;
    private ArrayList<AudioData> audioList;
    private AsyncQueryHandler mAsyncQueryHandler;
    private TextView txNoAudioFound;
    private StorageUtil storage;
    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);
        storage = new StorageUtil(getApplicationContext());

        recyclerView = (RecyclerView) findViewById(R.id.song_list);
        txNoAudioFound = findViewById(R.id.no_mp3);

        initAsyncLoadMusicFiles();
        // Requesting run time permission for Read External Storage.
        AndroidRuntimePermission();

    }

    @SuppressLint("HandlerLeak")
    private void initAsyncLoadMusicFiles() {
        mAsyncQueryHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (cursor != null && cursor.getCount() > 0) {
                    audioList = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                        String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                        String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                        String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                        String length = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                        String song_duration = "";
                        if (String.valueOf(length) != null) {
                            try {
                                Long time = Long.valueOf(length);
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
                                song_duration = length;
                            }
                        } else {
                            song_duration = "0";

                        }
                        // Save to audioList
                        audioList.add(new AudioData(data, title, album, artist, song_duration));
                    }

                    setAdapter();
                } else {

                    recyclerView.setVisibility(View.GONE);
                    txNoAudioFound.setVisibility(View.VISIBLE);
                    Log.d(Constants.TAG, "curson empty");
                }
                if (cursor != null) {
                    cursor.close();
                }

            }

            ;
        };

    }

    private void setAdapter() {
        storage.storeAudio(audioList);
        recyclerView.setVisibility(View.VISIBLE);
        txNoAudioFound.setVisibility(View.GONE);
        adapter = new AudioAdapter(this,audioList, new OnItemClickListener() {
            @Override
            public void onItemClick(AudioData item, int position) {
                playAudioService(position);

            }
        });

        RecyclerView.LayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(adapter);


    }

    private void loadAudio() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {

            // try to get title and artist from the media content provider
            mAsyncQueryHandler.startQuery(0, null, uri,
                    null,
                    selection, null, sortOrder);

        } else if (uri.getScheme().equals("file")) {
            // check if this file is in the media database (clicking on a download
            // in the download manager might follow this path
            String path = uri.getPath();
            mAsyncQueryHandler.startQuery(0, null, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.ARTIST},
                    MediaStore.Audio.Media.DATA + "=?", new String[]{path}, null);
        }
    }

    private void playAudioService(int audioIndex) {
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
        startActivity(new Intent(HomeActivity.this, AudioPlayActivity.class));
    }

    // Creating Runtime permission function.
    public void AndroidRuntimePermission() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        loadAudio();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
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
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }
}