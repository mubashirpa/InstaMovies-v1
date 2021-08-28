package instamovies.app.in.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import instamovies.app.in.HiddenWebActivity;
import instamovies.app.in.PlayerActivity;
import instamovies.app.in.R;
import instamovies.app.in.WebActivity;
import instamovies.app.in.YouTubePlayerActivity;
import instamovies.app.in.adapters.DownloadLinksAdapter;
import instamovies.app.in.models.DownloadLinksModel;
import instamovies.app.in.player.IntentUtil;
import instamovies.app.in.utils.AppUtils;

public class DownloadOptionsFragment extends BottomSheetDialogFragment {

    private JSONArray mJsonArray;
    private final ArrayList<DownloadLinksModel> downloadsList = new ArrayList<>();
    private Context context;
    private Activity activity;
    private boolean systemBrowser = false;
    private boolean chromeTabs = true;

    @Contract(" -> new")
    public static @NotNull DownloadOptionsFragment newInstance() {
        return new DownloadOptionsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogDefaultTheme);
        } catch (IllegalStateException stateException) {
            stateException.printStackTrace();
        }
    }

    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {
        context = getContext();
        activity = getActivity();
        View contentView = View.inflate(context, R.layout.layout_download_options, null);
        dialog.setContentView(contentView);
        BottomSheetBehavior<View> sheetBehavior = BottomSheetBehavior.from((View) contentView.getParent());
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        checkSettings();

        ListView listView = contentView.findViewById(R.id.listView);

        try {
            ListAdapter listAdapter = parseJSONArray(mJsonArray);
            listView.setAdapter(listAdapter);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (downloadsList.get(position).getLink() != null) {
                dialog.dismiss();
                loadUrl(downloadsList.get(position).getLink());
            }
        });
    }

    @NonNull
    @Contract("_ -> new")
    private DownloadLinksAdapter parseJSONArray(@NonNull JSONArray jsonArray) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            DownloadLinksModel model = new DownloadLinksModel();
            model.setTitle(jsonArray.getJSONObject(i).getString("title"));
            if (jsonArray.getJSONObject(i).has("sub_title")) {
                model.setSubTitle(jsonArray.getJSONObject(i).getString("sub_title"));
            }
            if (jsonArray.getJSONObject(i).has("link")) {
                model.setLink(jsonArray.getJSONObject(i).getString("link"));
            }
            downloadsList.add(model);
        }
        return new DownloadLinksAdapter(downloadsList);
    }

    public void setJsonArray(JSONArray jsonArray) {
        mJsonArray = jsonArray;
    }

    private void checkSettings() {
        SharedPreferences settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        systemBrowser = settingsPreferences.getBoolean("system_browser_preference", false);
        chromeTabs = settingsPreferences.getBoolean("chrome_tabs_preference", true);
    }

    private void loadUrl(@NonNull String url) {
        Intent webIntent;
        if (url.startsWith("link://")) {
            String replacedUrl = url.replace("link://","");
            webIntent = new Intent();
            webIntent.setClass(context, HiddenWebActivity.class);
            webIntent.putExtra("HIDDEN_URL", replacedUrl);
            startActivity(webIntent);
        }
        if (url.startsWith("link1://")) {
            String replacedUrl = url.replace("link1://","");
            webIntent = new Intent();
            webIntent.setClass(context, WebActivity.class);
            webIntent.putExtra("WEB_URL", replacedUrl);
            startActivity(webIntent);
        }
        if (url.startsWith("link2://")) {
            String replacedUrl = url.replace("link2://","");
            if (chromeTabs) {
                CustomTabsIntent.Builder customTabsBuilder = new CustomTabsIntent.Builder();
                CustomTabsIntent customTabsIntent = customTabsBuilder.build();
                customTabsIntent.launchUrl(context, Uri.parse(replacedUrl));
            } else {
                try {
                    Intent handleIntent = new Intent();
                    handleIntent.setAction(Intent.ACTION_VIEW);
                    handleIntent.setData(Uri.parse(replacedUrl));
                    startActivity(handleIntent);
                } catch (android.content.ActivityNotFoundException notFoundException){
                    AppUtils.toastError(context, activity, getString(R.string.error_activity_not_found));
                }
            }
        }
        if (url.startsWith("link3://")) {
            String replacedUrl = url.replace("link3://","");
            if (systemBrowser) {
                if (chromeTabs) {
                    CustomTabsIntent.Builder customTabsBuilder = new CustomTabsIntent.Builder();
                    CustomTabsIntent customTabsIntent = customTabsBuilder.build();
                    customTabsIntent.launchUrl(context, Uri.parse(replacedUrl));
                } else {
                    try {
                        Intent handleIntent = new Intent();
                        handleIntent.setAction(Intent.ACTION_VIEW);
                        handleIntent.setData(Uri.parse(replacedUrl));
                        startActivity(handleIntent);
                    } catch (android.content.ActivityNotFoundException notFoundException){
                        AppUtils.toastError(context, activity, getString(R.string.error_activity_not_found));
                    }
                }
            } else {
                webIntent = new Intent();
                webIntent.setClass(context, WebActivity.class);
                webIntent.putExtra("WEB_URL", replacedUrl);
                startActivity(webIntent);
            }
        }
        if (url.startsWith("video://")) {
            String replacedUrl = url.replace("video://","");
            Intent videoIntent;
            if (replacedUrl.startsWith("https://youtu.be/")) {
                videoIntent = new Intent();
                videoIntent.setClass(context, YouTubePlayerActivity.class);
                videoIntent.putExtra("VIDEO_ID", replacedUrl);
            } else {
                videoIntent = new Intent();
                videoIntent.setClass(context, PlayerActivity.class);
                videoIntent.putExtra("VIDEO_URI", replacedUrl);
                videoIntent.putExtra(IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA, true);
            }
            startActivity(videoIntent);
        }
        if (url.startsWith("magnet:")) {
            Intent torrentIntent = new Intent(Intent.ACTION_VIEW);
            torrentIntent.setData(Uri.parse(url));
            try {
                startActivity(torrentIntent);
            } catch (ActivityNotFoundException notFoundException) {
                BottomSheetFragment bottomSheetDialog = BottomSheetFragment.newInstance();
                bottomSheetDialog.setTitle("Error");
                bottomSheetDialog.setMessage("Torrent clients are not installed. Please click the download button to continue.");
                bottomSheetDialog.setPositiveButton("Download", v -> {
                    bottomSheetDialog.dismiss();
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                    marketIntent.setData(Uri.parse("market://details?id=com.utorrent.client"));
                    try {
                        context.startActivity(marketIntent);
                    } catch (ActivityNotFoundException notFoundException1){
                        AppUtils.toastError(context, activity, getString(R.string.error_activity_not_found));
                    }
                });
                bottomSheetDialog.show(requireActivity().getSupportFragmentManager(), "BottomSheetDialog");
            }
        }
    }
}