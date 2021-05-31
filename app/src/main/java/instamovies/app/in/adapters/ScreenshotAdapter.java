package instamovies.app.in.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import instamovies.app.in.R;
import instamovies.app.in.YouTubePlayerActivity;
import instamovies.app.in.models.ScreenshotModel;

public class ScreenshotAdapter extends RecyclerView.Adapter<ScreenshotAdapter.MyViewHolder> {

    private final ArrayList<ScreenshotModel> screenshotsArray;
    private Context context;

    public ScreenshotAdapter(ArrayList<ScreenshotModel> modelArrayList) {
        screenshotsArray = modelArrayList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_movie_screenshots, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context)
                .load(Uri.parse(screenshotsArray.get(position).getBackdropPath()))
                .into(holder.screenshots);

        if (screenshotsArray.get(position).getVideoUrl() != null) {
            holder.playButton.setVisibility(View.VISIBLE);
        } else {
            holder.playButton.setVisibility(View.GONE);
        }

        holder.screenshots.setOnClickListener(v -> {
            if (screenshotsArray.get(position).getVideoUrl() != null) {
                Intent videoIntent = new Intent();
                videoIntent.setClass(context, YouTubePlayerActivity.class);
                videoIntent.putExtra("VIDEO_ID", screenshotsArray.get(position).getVideoUrl());
                context.startActivity(videoIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return screenshotsArray.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        private final ImageView screenshots;
        private final ImageView playButton;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            screenshots = itemView.findViewById(R.id.screenshots);
            playButton = itemView.findViewById(R.id.play_button);
        }
    }
}