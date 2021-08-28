package instamovies.app.in.database;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import org.jetbrains.annotations.NotNull;
import instamovies.app.in.models.ThumbnailModel;
import instamovies.app.in.CategoriesActivity;
import instamovies.app.in.R;
import instamovies.app.in.utils.RecyclerTouchListener;

public class ThumbnailDatabase {

    public static void ThumbnailDatabaseMain(String childID, @NotNull RecyclerView recyclerView, Context context, LinearLayout linearLayout) {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = database.getReference("Thumbnail");
        Query query = databaseReference.child(childID);

        FirebaseRecyclerOptions<ThumbnailModel> recyclerOptions = new FirebaseRecyclerOptions.Builder<ThumbnailModel>()
                .setQuery(query, ThumbnailModel.class)
                .build();

        FirebaseRecyclerAdapter<ThumbnailModel, ThumbnailViewHolder> recyclerAdapter = new FirebaseRecyclerAdapter<ThumbnailModel, ThumbnailViewHolder>(recyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull ThumbnailViewHolder holder, int position, @NonNull ThumbnailModel model) {
                if (linearLayout.isShown()) {
                    linearLayout.setVisibility(View.GONE);
                }
                Glide.with(context)
                        .load(model.getThumbLink())
                        .error(R.drawable.img_loading_placeholder_vertical)
                        .placeholder(R.drawable.img_loading_placeholder_vertical)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(holder.thumbImage);
            }

            @NonNull
            @Override
            public ThumbnailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(context).inflate(R.layout.item_thumbnail_main, parent, false);
                return new ThumbnailViewHolder(itemView);
            }
        };
        recyclerAdapter.startListening();
        recyclerAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(context, recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Intent moviesIntent = new Intent();
                moviesIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                moviesIntent.setClass(context, CategoriesActivity.class);
                context.startActivity(moviesIntent);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }

    public static void ThumbnailDatabaseMore(String childID, @NotNull RecyclerView recyclerView, Context context) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = database.getReference("Thumbnail");
        Query query = databaseReference.child(childID);

        FirebaseRecyclerOptions<ThumbnailModel> recyclerOptions = new FirebaseRecyclerOptions.Builder<ThumbnailModel>()
                .setQuery(query, ThumbnailModel.class)
                .build();

        FirebaseRecyclerAdapter<ThumbnailModel, ThumbnailViewHolder> recyclerAdapter = new FirebaseRecyclerAdapter<ThumbnailModel, ThumbnailViewHolder>(recyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull ThumbnailViewHolder holder, int position, @NonNull ThumbnailModel model) {
                Glide.with(context)
                        .load(model.getThumbLink())
                        .error(R.drawable.img_loading_placeholder_horizontal)
                        .placeholder(R.drawable.img_loading_placeholder_horizontal)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(holder.thumbImage);
            }

            @NonNull
            @Override
            public ThumbnailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(context).inflate(R.layout.item_thumbnail_main_more, parent, false);
                return new ThumbnailViewHolder(itemView);
            }
        };
        recyclerAdapter.startListening();
        recyclerAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(context, recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Intent moviesIntent = new Intent();
                moviesIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                moviesIntent.setClass(context, CategoriesActivity.class);
                context.startActivity(moviesIntent);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }

    public static class ThumbnailViewHolder extends RecyclerView.ViewHolder {

        private final ImageView thumbImage;

        public ThumbnailViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbImage = itemView.findViewById(R.id.thumb_image);
        }
    }
}