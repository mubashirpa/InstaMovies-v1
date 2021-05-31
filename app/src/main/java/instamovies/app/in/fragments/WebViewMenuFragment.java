package instamovies.app.in.fragments;

import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import instamovies.app.in.R;
import instamovies.app.in.adapters.ShareIntentAdapter;
import instamovies.app.in.models.ShareIntentModel;
import instamovies.app.in.utils.AppUtils;
import instamovies.app.in.utils.RecyclerDecorationHorizontal;
import instamovies.app.in.utils.RecyclerTouchListener;
import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.Context.DOWNLOAD_SERVICE;

public class WebViewMenuFragment extends BottomSheetDialogFragment {

    private String title = "";
    private String url = "";
    private String iconUrl;
    private Context context;
    private WebView webView;

    @Contract(" -> new")
    public static @NotNull WebViewMenuFragment newInstance() {
        return new WebViewMenuFragment();
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
        View contentView = View.inflate(context, R.layout.layout_menu_webview, null);
        dialog.setContentView(contentView);
        BottomSheetBehavior<View> sheetBehavior = BottomSheetBehavior.from((View) contentView.getParent());
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        ImageView linkIcon = contentView.findViewById(R.id.icon);
        TextView linkTitle = contentView.findViewById(R.id.title);
        TextView linkUrl = contentView.findViewById(R.id.url);
        RecyclerView recyclerView = contentView.findViewById(R.id.recycler_view);
        RecyclerView shareRecycler = contentView.findViewById(R.id.share_recycler);

        linkTitle.setText(title);
        linkUrl.setText(url);
        if (iconUrl != null) {
            if (context != null) {
                Glide.with(context).load(iconUrl).into(linkIcon);
            }
        }

        initializeRecycler(recyclerView);
        initializeRecycler(shareRecycler);

        ArrayList<ShareIntentModel> menuList = new ArrayList<>();
        ShareIntentModel menuModel = new ShareIntentModel();
        menuModel.setName("Open link");
        menuModel.setIconId(R.drawable.ic_baseline_open_in_new_24);
        menuList.add(menuModel);
        menuModel = new ShareIntentModel();
        menuModel.setName("Copy link");
        menuModel.setIconId(R.drawable.ic_baseline_content_copy_24);
        menuList.add(menuModel);
        if (iconUrl != null) {
            menuModel = new ShareIntentModel();
            menuModel.setName("Download image");
            menuModel.setIconId(R.drawable.ic_baseline_download_24);
            menuList.add(menuModel);
        }
        menuModel = new ShareIntentModel();
        menuModel.setName("Share link");
        menuModel.setIconId(R.drawable.ic_baseline_share_24);
        menuList.add(menuModel);
        ShareIntentAdapter intentAdapter = new ShareIntentAdapter(menuList);
        recyclerView.setAdapter(intentAdapter);
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(context, recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                if (menuList.get(position).getName().equals("Open link")) {
                    dialog.dismiss();
                    webView.loadUrl(url);
                }
                if (menuList.get(position).getName().equals("Copy link")) {
                    dialog.dismiss();
                    ((ClipboardManager) Objects.requireNonNull(context.getSystemService(CLIPBOARD_SERVICE))).setPrimaryClip(ClipData.newPlainText("clipboard", url));
                    AppUtils.toastShortDefault(context, requireActivity(), "Link copied");
                }
                if (menuList.get(position).getName().equals("Download image")) {
                    dialog.dismiss();
                    downloadFromUrl(url);
                }
                if (menuList.get(position).getName().equals("Share link")) {
                    showShareList(shareRecycler, dialog);
                }
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setIconUrl(String url) {
        this.iconUrl = url;
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    private void initializeRecycler(RecyclerView recyclerView) {
        RecyclerDecorationHorizontal recyclerDecoration = new RecyclerDecorationHorizontal(15, 15, 10);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(recyclerDecoration);
    }

    private void showShareList(RecyclerView recyclerView, Dialog dialog) {
        recyclerView.setVisibility(View.VISIBLE);
        ArrayList<ShareIntentModel> intentList = new ArrayList<>();
        if (getActivity() != null) {
            PackageManager packageManager = getActivity().getPackageManager();
            Intent intent = new Intent(Intent.ACTION_SEND, null);
            intent.setType("text/plain");
            List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);
            for (ResolveInfo info : resolveInfoList) {
                ApplicationInfo applicationInfo = info.activityInfo.applicationInfo;
                ShareIntentModel intentModel = new ShareIntentModel();
                intentModel.setName(applicationInfo.loadLabel(packageManager).toString());
                intentModel.setPackageName(applicationInfo.packageName);
                intentModel.setIcon(applicationInfo.loadIcon(packageManager));
                intentList.add(intentModel);
            }
            ShareIntentAdapter intentAdapter = new ShareIntentAdapter(intentList);
            recyclerView.setAdapter(intentAdapter);
            recyclerView.addOnItemTouchListener(new RecyclerTouchListener(context, recyclerView, new RecyclerTouchListener.ClickListener() {
                @Override
                public void onClick(View view, int position) {
                    dialog.dismiss();
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.setPackage(intentList.get(position).getPackageName());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, url);
                    startActivity(shareIntent);
                }

                @Override
                public void onLongClick(View view, int position) {

                }
            }));
        }
    }

    private void downloadFromUrl(String downloadUrl) {
        if (URLUtil.isValidUrl((downloadUrl))) {
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
            Uri Download_Uri = Uri.parse(downloadUrl);
            DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setAllowedOverRoaming(false);
            String cookies = CookieManager.getInstance().getCookie(downloadUrl);
            request.addRequestHeader("cookie", cookies);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            AppUtils.toastShortDefault(context, requireActivity(), "Downloading file");
            downloadManager.enqueue(request);
        } else {
            AppUtils.toastShortError(context, requireActivity(),"Download failed");
        }
    }
}