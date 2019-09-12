package com.example.classicmusic.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.classicmusic.OnItemClickListener;
import com.example.classicmusic.R;
import com.example.classicmusic.module.AudioData;
import com.example.classicmusic.utils.StorageUtil;

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

        Bitmap coverImage = new StorageUtil(context).getAudioCoverImage(data.getData());
        if (coverImage != null) {
            coverImage=getCroppedBitmap(coverImage);
            holder.mediaCover.setImageBitmap(coverImage);
        }

    }

    @Override
    public int getItemCount() {
        return audioList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, duration;
        CardView cardView;

        ImageView mediaCover;

        public MyViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            cardView.setBackgroundResource(R.drawable.bg_border_blue_round);

            //*Set respective Audio data to the rows*//
            mediaCover = itemView.findViewById(R.id.audio_cover);
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

    public Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output;
    }
}
