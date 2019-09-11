package com.example.classicmusic.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.classicmusic.OnItemClickListener;
import com.example.classicmusic.R;
import com.example.classicmusic.module.AudioData;
import com.example.classicmusic.utils.Constants;

import java.util.List;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.MyViewHolder> {
    public static final int VIEW_TYPE_EMPTY = 0;
    public static final int VIEW_TYPE_NORMAL = 1;

    List<AudioData> audioList;
    OnItemClickListener listener;

    public AudioAdapter(List<AudioData> songlist, OnItemClickListener listener){
        this.audioList =songlist;
        this.listener=listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_audio_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        AudioData data= audioList.get(position);
        Log.e(Constants.TAG,"AudioData: "+"Album: "+data.getAlbum());
        holder.title.setText(data.getTitle());
        holder.description.setText(String.valueOf(data.getAlbum()));
        holder.duration.setText(data.getDuration());
    }

    @Override
    public int getItemCount() {
        return audioList.size();
    }
    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title,description, duration;
        public MyViewHolder(View itemView) {
            super(itemView);
            title=itemView.findViewById(R.id.cardview_list_title);
            description=itemView.findViewById(R.id.short_description);
            duration=itemView.findViewById(R.id.tv_duration);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClick(audioList.get(getAdapterPosition()),getAdapterPosition());
                }
            });
        }

    }
}
