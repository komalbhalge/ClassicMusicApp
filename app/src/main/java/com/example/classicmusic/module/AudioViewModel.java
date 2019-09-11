package com.example.classicmusic.module;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;

public class AudioViewModel extends AndroidViewModel {

    private MutableLiveData<ArrayList<AudioData> >audioListData = new MutableLiveData<>();


    private MutableLiveData<AudioData> audioData = new MutableLiveData<>();

    public AudioViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<ArrayList<AudioData>> getAudioListData() {
        return audioListData;
    }

    public void setAudioListData(ArrayList<AudioData> userLiveData) {
        this.audioListData.setValue(userLiveData);
    }
    public void setAudioData(AudioData audioLiveData) {
        this.audioData.setValue( audioLiveData);
    }
    public LiveData<AudioData> getAudioData() {
        return audioData;
    }


}