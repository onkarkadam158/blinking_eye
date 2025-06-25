package com.example.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class GifAdapter extends RecyclerView.Adapter<GifAdapter.GifViewHolder> {

    private final Context context;
    private final List<File> gifFiles;
    private final GifUploader gifUploader;
    private final Runnable onGifDeleted;

    public GifAdapter(Context context, List<File> gifFiles, GifUploader gifUploader, Runnable onGifDeleted) {
        this.context = context;
        this.gifFiles = gifFiles;
        this.gifUploader = gifUploader;
        this.onGifDeleted = onGifDeleted;
    }

    @NonNull
    @Override
    public GifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.gif_item, parent, false);
        return new GifViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GifViewHolder holder, int position) {
        File gifFile = gifFiles.get(position);

        Glide.with(context)
                .asGif()
                .load(gifFile)
                .into(holder.gifImageView);

        holder.deleteButton.setOnClickListener(v -> {
            boolean deleted = gifUploader.deleteGif(gifFile);
            if (deleted) {
                gifFiles.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, gifFiles.size());
                Toast.makeText(context, "GIF deleted", Toast.LENGTH_SHORT).show();
                onGifDeleted.run();
            } else {
                Toast.makeText(context, "Failed to delete GIF", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return gifFiles.size();
    }

    static class GifViewHolder extends RecyclerView.ViewHolder {
        ImageView gifImageView, deleteButton;

        public GifViewHolder(@NonNull View itemView) {
            super(itemView);
            gifImageView = itemView.findViewById(R.id.gifImageView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
