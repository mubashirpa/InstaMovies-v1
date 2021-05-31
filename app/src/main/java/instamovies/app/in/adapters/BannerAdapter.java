package instamovies.app.in.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import instamovies.app.in.HiddenWebActivity;
import instamovies.app.in.MovieDetailsActivity;
import instamovies.app.in.player.IntentUtil;
import instamovies.app.in.PlayerActivity;
import instamovies.app.in.R;
import instamovies.app.in.utils.AppUtils;
import instamovies.app.in.WebActivity;
import instamovies.app.in.YouTubePlayerActivity;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private final ArrayList<HashMap<String, Object>> sliderList;
    private final ViewPager2 viewPager;
    private final Context context;
    private final Activity activity;
    private boolean premiumUser = false;
    private boolean chromeTabs = true;
    private boolean systemBrowser = false;
    private Intent webIntent = new Intent();
    private Intent videoIntent = new Intent();
    private final SharedPreferences settingsPreferences;
    private final SharedPreferences appData;

    public BannerAdapter(ArrayList<HashMap<String, Object>> arrayList, ViewPager2 viewPager2, @NotNull Context context, Activity activity) {
        sliderList = arrayList;
        viewPager = viewPager2;
        this.context = context;
        this.activity = activity;
        appData = context.getSharedPreferences("appData", Activity.MODE_PRIVATE);
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        checkSettings();
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner_main, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        holder.setSliderImage(sliderList, context, position);
        if (sliderList.get(position).containsKey("Video")) {
            holder.playButton.setVisibility(View.VISIBLE);
        } else {
            holder.playButton.setVisibility(View.GONE);
        }

        holder.sliderImage.setOnClickListener(v -> {
            if (sliderList.get(position).containsKey("Premium") && !premiumUser) {
                AppUtils.toastShortDefault(context, activity, "You are not a premium user.");
                return;
            }
            if (sliderList.get(position).containsKey("Movie")) {
                String movieLink = Objects.requireNonNull(sliderList.get(position).get("Movie")).toString();
                webIntent = new Intent();
                webIntent.setClass(context, MovieDetailsActivity.class);
                webIntent.putExtra("Movie Link", movieLink);
                context.startActivity(webIntent);
            }
            if (sliderList.get(position).containsKey("Link")) {
                String itemLink = Objects.requireNonNull(sliderList.get(position).get("Link")).toString();
                if (URLUtil.isNetworkUrl(itemLink)) {
                    webIntent = new Intent();
                    webIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    webIntent.setClass(context, HiddenWebActivity.class);
                    webIntent.putExtra("HIDDEN_URL", itemLink);
                    context.startActivity(webIntent);
                } else {
                    AppUtils.toastShortDefault(context, activity, itemLink);
                }
            }
            if (sliderList.get(position).containsKey("Link1")) {
                String itemLink = Objects.requireNonNull(sliderList.get(position).get("Link1")).toString();
                webIntent = new Intent();
                webIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                webIntent.setClass(context, WebActivity.class);
                webIntent.putExtra("WEB_URL", itemLink);
                context.startActivity(webIntent);
            }
            if (sliderList.get(position).containsKey("Link2")) {
                String itemLink = Objects.requireNonNull(sliderList.get(position).get("Link2")).toString();
                if (chromeTabs) {
                    CustomTabsIntent.Builder customTabsBuilder = new CustomTabsIntent.Builder();
                    CustomTabsIntent customTabsIntent = customTabsBuilder.build();
                    customTabsIntent.launchUrl(context, Uri.parse(itemLink));
                } else {
                    try {
                        webIntent = new Intent();
                        webIntent.setAction(Intent.ACTION_VIEW);
                        webIntent.setData(Uri.parse(itemLink));
                        context.startActivity(webIntent);
                    } catch (android.content.ActivityNotFoundException notFoundException){
                        AppUtils.toastShortError(context, activity, "Failed to load url");
                    }
                }
            }
            if (sliderList.get(position).containsKey("Link3")) {
                String itemLink = Objects.requireNonNull(sliderList.get(position).get("Link3")).toString();
                if (systemBrowser) {
                    if (chromeTabs) {
                        CustomTabsIntent.Builder customTabsBuilder = new CustomTabsIntent.Builder();
                        CustomTabsIntent customTabsIntent = customTabsBuilder.build();
                        customTabsIntent.launchUrl(context, Uri.parse(itemLink));
                    } else {
                        try {
                            webIntent = new Intent();
                            webIntent.setAction(Intent.ACTION_VIEW);
                            webIntent.setData(Uri.parse(itemLink));
                            context.startActivity(webIntent);
                        } catch (android.content.ActivityNotFoundException notFoundException){
                            AppUtils.toastShortError(context, activity, "Failed to load url");
                        }
                    }
                } else {
                    webIntent = new Intent();
                    webIntent.setClass(context, WebActivity.class);
                    webIntent.putExtra("WEB_URL", itemLink);
                    context.startActivity(webIntent);
                }
            }
            if (sliderList.get(position).containsKey("Video")) {
                String itemLink = Objects.requireNonNull(sliderList.get(position).get("Video")).toString();
                if (itemLink.startsWith("https://youtu.be/")) {
                    videoIntent = new Intent();
                    videoIntent.setClass(context, YouTubePlayerActivity.class);
                    videoIntent.putExtra("VIDEO_ID", itemLink);
                } else {
                    videoIntent = new Intent();
                    videoIntent.setClass(context, PlayerActivity.class);
                    videoIntent.putExtra("VIDEO_URI", itemLink);
                    videoIntent.putExtra(IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA, true);
                }
                context.startActivity(videoIntent);
            }
        });

        if (position == sliderList.size() - 2) {
            viewPager.post(runnable);
        }
    }

    @Override
    public int getItemCount() {
        return sliderList.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {

        private final ImageView sliderImage;
        private final ImageView playButton;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            sliderImage = itemView.findViewById(R.id.backdrop);
            playButton = itemView.findViewById(R.id.play_button);
        }

        void setSliderImage(@NotNull ArrayList<HashMap<String, Object>> sliderModels, Context context, int position) {
            Glide.with(context)
                    .load(sliderModels.get(position).get("Image"))
                    .error(R.drawable.img_loading_placeholder_horizontal)
                    .placeholder(R.drawable.img_loading_placeholder_horizontal)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(sliderImage);
        }
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //noinspection CollectionAddedToSelf
            sliderList.addAll(sliderList);
            notifyDataSetChanged();
        }
    };

    private void checkSettings() {
        premiumUser = appData.getBoolean("prime_purchased", false);
        systemBrowser = settingsPreferences.getBoolean("system_browser_preference", false);
        chromeTabs = settingsPreferences.getBoolean("chrome_tabs_preference", true);
    }
}