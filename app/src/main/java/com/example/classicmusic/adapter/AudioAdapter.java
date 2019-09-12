package com.example.classicmusic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.classicmusic.OnItemClickListener;
import com.example.classicmusic.R;
import com.example.classicmusic.module.AudioData;

import java.util.List;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.MyViewHolder> {
    Context context;
    List<AudioData> audioList;
    OnItemClickListener listener;

    public AudioAdapter(Context context, List<AudioData> songlist, OnItemClickListener listener) {
        this.context = context;
        this.audioList = songlist;
        this.listener = listener;
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
        AudioData data = audioList.get(position);

        holder.title.setText(data.getTitle());
        holder.description.setText(String.valueOf(data.getAlbum()));
        holder.duration.setText(data.getDuration());
    }

    @Override
    public int getItemCount() {
        return audioList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, duration;
        CardView cardView;

        public MyViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            cardView.setBackgroundResource(R.drawable.bg_border_blue_round);

            //*Set respective Audio data to the rows*//
            title = itemView.findViewById(R.id.cardview_list_title);
            description = itemView.findViewById(R.id.short_description);
            duration = itemView.findViewById(R.id.tv_duration);

            //*Perform Row click and pass itemClick ot the listener*//
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClick(audioList.get(getAdapterPosition()), getAdapterPosition());
                }
            });
        }

    }
}
