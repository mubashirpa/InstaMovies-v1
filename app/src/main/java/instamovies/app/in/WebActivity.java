package instamovies.app.in;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import android.webkit.*;
import java.io.File;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import android.content.Intent;
import android.net.Uri;
import android.app.AlertDialog;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import instamovies.app.in.fragments.BottomSheetFragment;
import instamovies.app.in.fragments.DownloadDialogFragment;
import instamovies.app.in.fragments.WebViewMenuFragment;
import instamovies.app.in.player.IntentUtil;
import instamovies.app.in.utils.AdBlocker;
import instamovies.app.in.utils.AppUtils;
import instamovies.app.in.utils.FileUtil;
import instamovies.app.in.utils.NetworkUtil;
import instamovies.app.in.utils.SnackBarHelper;
import okhttp3.HttpUrl;

public class WebActivity extends AppCompatActivity {

    private AlertDialog.Builder reportDialog;
    private String reportMessage;
    private WebView webView;
    private ProgressBar progressBar;
    private SharedPreferences appData;
    private final FirebaseFirestore reportDB = FirebaseFirestore.getInstance();
    private AlertDialog.Builder sslErrorPop;
    private SharedPreferences settingsPreferences;
    private String errorUrl = null;
    private TextView titleText;
    private final String errorPageUrl = "about:blank";
    private String pageUrl;
    private ValueCallback<Uri[]> uploadMessage;
    private View errorLinear;
    private BottomSheetFragment bottomSheetDialog;
    private Context context;
    private final String LOG_TAG = "WebActivity";
    private boolean hasShownCustomView = false;
    private String errorDetails = "";
    private AdView adView;
    private DownloadManager downloadManager;
    private long downloadReference;
    private boolean allowCookies = false;
    private boolean blockAds = false;
    private boolean blockPopups = true;
    private boolean premiumUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        context = this;
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(context, R.color.colorLayoutPrimary));
        switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
            case Configuration.UI_MODE_NIGHT_YES:

                break;
            case Configuration.UI_MODE_NIGHT_NO:
                if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    WindowInsetsController controller = getWindow().getInsetsController();
                    if(controller != null) {
                        controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                    //Deprecated in Api level 30
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

                } else {

                    //Deprecated in Api level 23
                    window.setStatusBarColor(Color.BLACK);

                }
                break;
        }

        initializeActivity();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initializeActivity() {
        MobileAds.initialize(context);
        AdBlocker.init(context);
        webView = findViewById(R.id.webView);
        webView.getSettings().setSupportZoom(true);
        progressBar = findViewById(R.id.progress_bar);
        sslErrorPop = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
        reportDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
        appData = getSharedPreferences("appData", Activity.MODE_PRIVATE);
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        ImageView closeButton = findViewById(R.id.close_button);
        ImageView moreButton = findViewById(R.id.more_button);
        titleText = findViewById(R.id.title_text);
        pageUrl = getIntent().getStringExtra("WEB_URL");
        errorLinear = findViewById(R.id.error_linear);
        Button reloadButton = errorLinear.findViewById(R.id.reload_button);
        Button detailsButton = errorLinear.findViewById(R.id.details_button);
        ImageView reloadButtonToolbar = findViewById(R.id.reload_button_toolbar);
        bottomSheetDialog = BottomSheetFragment.newInstance();
        Intent videoIntent = new Intent();
        PopupMenu popupMenu = new PopupMenu(context, moreButton, R.style.PopupMenuTheme);
        popupMenu.getMenuInflater().inflate(R.menu.menu_web_activity, popupMenu.getMenu());
        adView = findViewById(R.id.adView);
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        if (!appData.getBoolean("WebUsed", false)) {
            BottomSheetFragment bottomSheetDialog = BottomSheetFragment.newInstance();
            bottomSheetDialog.setTitle("Information");
            bottomSheetDialog.setMessage(getString(R.string.warning_web));
            bottomSheetDialog.setPositiveButton("Understood", v -> {
                bottomSheetDialog.dismiss();
                appData.edit().putBoolean("WebUsed", true).apply();
            });
            bottomSheetDialog.show(getSupportFragmentManager(), "BottomSheetDialog");
        }

        checkSettings();

        webView.setWebViewClient(new WebViewClient() {

            private final Map<String, Boolean> loadedUrls = new HashMap<>();

            @Nullable
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request){
                if (!blockAds){
                    return super.shouldInterceptRequest(view,request);
                }
                String resourceUrl = request.getUrl().toString();
                boolean ad;
                if (!loadedUrls.containsKey(resourceUrl)) {
                    ad = AdBlocker.isAd(resourceUrl);
                    loadedUrls.put(resourceUrl, ad);
                } else {
                    //noinspection ConstantConditions
                    ad = loadedUrls.get(resourceUrl);
                }
                return ad ? AdBlocker.createEmptyResource() : super.shouldInterceptRequest(view, request);
            }

            @Override
            @TargetApi(Build.VERSION_CODES.M)
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String loadingUrl = request.getUrl().toString();
                if(URLUtil.isNetworkUrl(loadingUrl)) {
                    HttpUrl httpUrl = HttpUrl.parse(loadingUrl);
                    String urlHost = null;
                    if (httpUrl != null) {
                        urlHost = httpUrl.host();
                    }

                    //Blocking advertisement pages from ad page list
                    File adFilePath = new File(FileUtil.getPackageDataDir(context) + "/downloads", "ad-pages-list.txt");
                    if (urlHost != null && FileUtil.isExistFile(adFilePath) &&
                            FileUtil.readFile(adFilePath).contains(urlHost) && blockPopups) {
                        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),"Popup blocked", Snackbar.LENGTH_SHORT);
                        snackbar.setAction("Details", snackView -> {
                            snackbar.dismiss();
                            BottomSheetFragment bottomSheetDialog2 = BottomSheetFragment.newInstance();
                            bottomSheetDialog2.setMessage(loadingUrl);
                            bottomSheetDialog2.setPositiveButton("Done", v -> bottomSheetDialog2.dismiss());
                            bottomSheetDialog2.show(getSupportFragmentManager(), "BottomSheetDialog");
                        });
                        SnackBarHelper.configSnackBar(context, snackbar);
                        snackbar.show();
                        return true;
                    }
                    return false;
                } else {
                    try {
                        Intent handleIntent = new Intent(Intent.ACTION_VIEW);
                        handleIntent.setData(Uri.parse(loadingUrl));
                        startActivity(handleIntent);
                    } catch (ActivityNotFoundException e) {
                        if (loadingUrl.startsWith("intent://")) {
                            try {
                                Context context = view.getContext();
                                Intent handlerIntent = Intent.parseUri(loadingUrl, Intent.URI_INTENT_SCHEME);
                                if (handlerIntent != null){
                                    PackageManager packageManager = context.getPackageManager();
                                    ResolveInfo info = packageManager.resolveActivity(handlerIntent, PackageManager.MATCH_DEFAULT_ONLY);
                                    if (info != null){
                                        context.startActivity(handlerIntent);
                                    } else {
                                        Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                                        marketIntent.setData(Uri.parse("market://details?id="+handlerIntent.getPackage()));
                                        try {
                                            startActivity(marketIntent);
                                        } catch (ActivityNotFoundException notFoundException) {
                                            Log.e(LOG_TAG, notFoundException.getMessage());
                                        }
                                    }
                                }
                            } catch (URISyntaxException uriSyntaxException){
                                Log.e(LOG_TAG, uriSyntaxException.getMessage());
                            }
                        } else if (loadingUrl.startsWith("magnet:")){
                            bottomSheetDialog.setTitle("Error");
                            bottomSheetDialog.setMessage("Torrent clients are not installed. Please click the download button to continue.");
                            bottomSheetDialog.setPositiveButton("Download", v -> {
                                bottomSheetDialog.dismiss();
                                Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                                marketIntent.setData(Uri.parse("market://details?id=com.utorrent.client"));
                                try {
                                    startActivity(marketIntent);
                                } catch (ActivityNotFoundException notFoundException1){
                                    AppUtils.toastShortError(context,WebActivity.this, "Failed to download Torrent client");
                                }
                            });
                            bottomSheetDialog.show(getSupportFragmentManager(), "BottomSheetDialog");
                        } else {
                            AppUtils.toastShortError(context,WebActivity.this, "Unsupported URL");
                        }
                    }
                }
                return true;
            }

            @Deprecated
            public boolean shouldOverrideUrlLoading(WebView view, String loadingUrl){
                if(URLUtil.isNetworkUrl(loadingUrl)) {
                    HttpUrl httpUrl = HttpUrl.parse(loadingUrl);
                    String urlHost = null;
                    if (httpUrl != null) {
                        urlHost = httpUrl.host();
                    }

                    //Blocking advertisement pages from ad page list
                    File adFilePath = new File(FileUtil.getPackageDataDir(context) + "/downloads", "ad-pages-list.txt");
                    if (urlHost != null && FileUtil.isExistFile(adFilePath) &&
                            FileUtil.readFile(adFilePath).contains(urlHost) && blockPopups) {
                        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),"Popup blocked", Snackbar.LENGTH_SHORT);
                        snackbar.setAction("Details", snackView -> {
                            snackbar.dismiss();
                            BottomSheetFragment bottomSheetDialog2 = BottomSheetFragment.newInstance();
                            bottomSheetDialog2.setMessage(loadingUrl);
                            bottomSheetDialog2.setPositiveButton("Done", v -> bottomSheetDialog2.dismiss());
                            bottomSheetDialog2.show(getSupportFragmentManager(), "BottomSheetDialog");
                        });
                        SnackBarHelper.configSnackBar(context, snackbar);
                        snackbar.show();
                        return true;
                    }
                    return false;
                } else {
                    try {
                        Intent handleIntent = new Intent(Intent.ACTION_VIEW);
                        handleIntent.setData(Uri.parse(loadingUrl));
                        startActivity(handleIntent);
                    } catch (ActivityNotFoundException e) {
                        if (loadingUrl.startsWith("intent://")){
                            try {
                                Context context = view.getContext();
                                Intent handlerIntent = Intent.parseUri(loadingUrl, Intent.URI_INTENT_SCHEME);
                                if (handlerIntent != null){
                                    PackageManager packageManager = context.getPackageManager();
                                    ResolveInfo info = packageManager.resolveActivity(handlerIntent, PackageManager.MATCH_DEFAULT_ONLY);
                                    if (info != null){
                                        context.startActivity(handlerIntent);
                                    } else {
                                        Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                                        marketIntent.setData(Uri.parse("market://details?id="+handlerIntent.getPackage()));
                                        try {
                                            startActivity(marketIntent);
                                        } catch (ActivityNotFoundException notFoundException) {
                                            Log.e(LOG_TAG, notFoundException.getMessage());
                                        }
                                    }
                                }
                            } catch (URISyntaxException uriSyntaxException){
                                Log.e(LOG_TAG, uriSyntaxException.getMessage());
                            }
                        } else if (loadingUrl.startsWith("magnet:")){
                            bottomSheetDialog.setTitle("Error");
                            bottomSheetDialog.setMessage("Torrent clients are not installed. Please click the download button to continue.");
                            bottomSheetDialog.setPositiveButton("Download", v -> {
                                bottomSheetDialog.dismiss();
                                Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                                marketIntent.setData(Uri.parse("market://details?id=com.utorrent.client"));
                                try {
                                    startActivity(marketIntent);
                                } catch (ActivityNotFoundException notFoundException1){
                                    AppUtils.toastShortError(context,WebActivity.this, "Failed to download Torrent client");
                                }
                            });
                            bottomSheetDialog.show(getSupportFragmentManager(), "BottomSheetDialog");
                        } else {
                            AppUtils.toastShortError(context,WebActivity.this, "Unsupported URL");
                        }
                    }
                }
                return true;
            }

            @Override
            @TargetApi(Build.VERSION_CODES.M)
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                String failingUrl = request.getUrl().toString();
                handleWebError(view, failingUrl);
                errorDetails = failingUrl + "\n\nError: " + error.getDescription().toString() + "\n\nError code: " + error.getErrorCode();
            }

            @Deprecated
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                handleWebError(view, failingUrl);
                errorDetails = failingUrl + "\n\nError: " + description + "\n\nError code: " + errorCode;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                String sslMessage = "Certificate error.";
                switch (error.getPrimaryError()){
                    case SslError.SSL_UNTRUSTED:
                        sslMessage = "The certificate authority is not trusted.";
                        break;
                    case SslError.SSL_EXPIRED:
                        sslMessage = "The certificate has been expired.";
                        break;
                    case SslError.SSL_IDMISMATCH:
                        sslMessage = "The certificate hostname mismatch.";
                        break;
                    case SslError.SSL_NOTYETVALID:
                        sslMessage = "The certificate is not yet valid.";
                        break;
                    case SslError.SSL_DATE_INVALID:
                        sslMessage = "The certificate date is invalid";
                        break;
                }
                sslMessage += "\nDo you want to continue anyway?";
                sslErrorPop.setTitle("SSL Certificate Error");
                sslErrorPop.setMessage(sslMessage);
                sslErrorPop.setPositiveButton("PROCEED", (dialogInterface, i) -> handler.proceed());
                sslErrorPop.setNegativeButton("CANCEL", (dialogInterface, i) -> handler.cancel());
                sslErrorPop.setCancelable(false);
                sslErrorPop.create().show();
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                if (!url.equals(errorPageUrl)) {
                    errorUrl = null;
                    if (!view.isShown()) {
                        view.setVisibility(View.VISIBLE);
                    }
                    if (errorLinear.isShown()) {
                        errorLinear.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url.startsWith("https://dood.so/d/")) {
                    view.evaluateJavascript("javascript:var link = document.getElementsByClassName('btn')[2].href; location.replace(link);", null);
                }
                if (url.startsWith("https://dood.so/download/")) {
                    view.evaluateJavascript("javascript:document.getElementsByClassName('btn-primary')[0].click();", null);
                }
            }
        });

        if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
            } else {
                WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_OFF);
            }
        }
        if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            WebSettingsCompat.setForceDarkStrategy(webView.getSettings(), WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY);
        }
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAllowFileAccess(true);

        //Deprecated
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);

        webView.getSettings().setBlockNetworkImage(false);
        webView.getSettings().setBlockNetworkLoads(false);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setGeolocationEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        webView.getSettings().setNeedInitialFocus(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.getSettings().setSafeBrowsingEnabled(true);
        }
        webView.getSettings().setSupportMultipleWindows(false);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setUseWideViewPort(true);
        String versionRelease = Build.VERSION.RELEASE;
        String deviceModel = Build.MODEL;
        String chromeVersion = getString(R.string.google_chrome_version);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android " + versionRelease + "; " + deviceModel +") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + chromeVersion + " Mobile Safari/537.36");
        webView.setSoundEffectsEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setWebChromeClient(new CustomWebClient());
        webView.setLongClickable(true);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE,null);
        webView.requestFocusFromTouch();
        webView.loadUrl(pageUrl);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.addJavascriptInterface(new MyJavaScriptInterface(this), "Android");

        webView.setOnLongClickListener(view -> {
            WebView.HitTestResult hitTestResult = webView.getHitTestResult();
            WebViewMenuFragment menuFragment = WebViewMenuFragment.newInstance();
            if (hitTestResult.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                menuFragment.setTitle(webView.getTitle());
                menuFragment.setUrl(hitTestResult.getExtra());
                menuFragment.setWebView(webView);
                menuFragment.show(getSupportFragmentManager(), "BottomSheetDialog");
                return true;
            }
            if (hitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                    hitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                menuFragment.setTitle(webView.getTitle());
                menuFragment.setUrl(hitTestResult.getExtra());
                menuFragment.setIconUrl(hitTestResult.getExtra());
                menuFragment.setWebView(webView);
                menuFragment.show(getSupportFragmentManager(), "BottomSheetDialog");
                return true;
            }
            return false;
        });

        webView.setDownloadListener((downloadUrl, userAgent, contentDisposition, mimeType, contentLength) -> {
            final String fileName = URLUtil.guessFileName(downloadUrl, contentDisposition, mimeType);
            int fileType = R.drawable.ic_unknown_file;
            if (mimeType.contains("video")) {
                fileType = R.drawable.ic_video_file;
            } else if (mimeType.contains("audio")) {
                fileType = R.drawable.ic_music_file;
            } else if (mimeType.contains("image")) {
                fileType = R.drawable.ic_image_file;
            } else if (mimeType.contains("text")) {
                fileType = R.drawable.ic_text_file;
            } else if (mimeType.contains("application/vnd.android.package-archive")) {
                fileType = R.drawable.ic_android_logo;
            }
            double fileSize = 0;
            if (contentLength > 0) {
                fileSize = contentLength >> 20;
            }
            DownloadDialogFragment downloadDialogFragment = DownloadDialogFragment.newInstance();
            downloadDialogFragment.setFileTitle(fileName);
            downloadDialogFragment.setFileSize("(".concat(String.valueOf(fileSize)).concat("MB)"));
            if (!NetworkUtil.isWifiConnected(context)) {
                downloadDialogFragment.showCellularInfo(true);
            }
            downloadDialogFragment.setFileType(fileType);
            if (mimeType.contains("video")) {
                downloadDialogFragment.showPlayOnline(true);
            }
            downloadDialogFragment.setPlayOnlineClickListener(v -> {
                downloadDialogFragment.dismiss();
                videoIntent.setClass(getApplicationContext(), PlayerActivity.class);
                videoIntent.putExtra("VIDEO_URI", downloadUrl);
                videoIntent.putExtra(IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA, true);
                startActivity(videoIntent);
            });
            downloadDialogFragment.setConfirmListener(v -> {
                downloadDialogFragment.dismiss();
                if (settingsPreferences.getString("download_preference", "Via external apps").equals("Via external apps")) {
                    Intent uriIntent = new Intent();
                    uriIntent.setAction(Intent.ACTION_VIEW);
                    uriIntent.setData(Uri.parse(downloadUrl));
                    startActivity(uriIntent);
                } else {
                    if (downloadDialogFragment.getFileName().equals("")) {
                        downloadReference = downloadMedia(downloadUrl, mimeType, userAgent, downloadDialogFragment.getFileName());
                    } else {
                        downloadReference = downloadMedia(downloadUrl, mimeType, userAgent, fileName);
                    }
                    IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                    registerReceiver(downloadReceiver, filter);
                }
            });
            downloadDialogFragment.setCancelListener(v -> downloadDialogFragment.dismiss());
            downloadDialogFragment.show(getSupportFragmentManager(), "BottomSheetDialog");
        });

        closeButton.setOnClickListener(v -> finish());

        reloadButtonToolbar.setOnClickListener(v -> {
            if (webView.getUrl().equals(errorPageUrl)) {
                goBackWebView();
            } else {
                webView.reload();
            }
        });

        reloadButton.setOnClickListener(v -> goBackWebView());

        detailsButton.setOnClickListener(v -> {
            AlertDialog.Builder errorDetailsDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
            errorDetailsDialog.setMessage(errorDetails);
            errorDetailsDialog.setPositiveButton("CLOSE", (dialogInterface, i) -> {
            });
            errorDetailsDialog.create().show();
        });

        moreButton.setOnClickListener(v -> popupMenu.show());

        popupMenu.setOnMenuItemClickListener(item -> {
            Intent popupIntent;
            switch (item.getTitle().toString()){
                case "Reload":
                    if (webView.getUrl().equals(errorPageUrl)) {
                        goBackWebView();
                    } else {
                        webView.reload();
                    }
                    return true;
                case "Clear cache":
                    webView.clearCache(true);
                    AppUtils.toastShortDefault(context,WebActivity.this,"Cache Cleared");
                    return true;
                case "Report page":
                    if (webView.getUrl() != null){
                        ReportPage(webView.getUrl());
                    } else {
                        AppUtils.toastShortError(context,WebActivity.this,"An error occurred");
                    }
                    return true;
                case "Settings":
                    popupIntent = new Intent();
                    popupIntent.setClass(context, SettingsActivity.class);
                    startActivity(popupIntent);
                    return true;
                case "Send feedback":
                    popupIntent = new Intent();
                    popupIntent.setClass(context, FeedbackActivity.class);
                    startActivity(popupIntent);
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        webView.destroy();
        try {
            unregisterReceiver(downloadReceiver);
        } catch (Exception exception) {
            Log.e(LOG_TAG, exception.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
        if (errorUrl != null){
            if (pageUrl.contains(errorUrl)){
                finish();
            } else {
                goBackWebView();
            }
        } else {
            goBackWebView();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        checkSettings();
        if (allowCookies) {
            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        } else {
            CookieManager.getInstance().setAcceptCookie(false);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        }
        if (!premiumUser) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } else {
            adView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && hasShownCustomView) {
            hideSystemUi();
        }
    }

    final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        int resultCode = result.getResultCode();
        Intent data = result.getData();
        if (resultCode == Activity.RESULT_OK) {
            if (uploadMessage == null) {
                return;
            }
            uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
        } else {
            uploadMessage.onReceiveValue(null);
        }
        uploadMessage = null;
    });

    public long downloadMedia(String url, String mimeType, String userAgent, @NotNull String fileName) {
        Uri Download_Uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setAllowedOverRoaming(false);
        request.setTitle(fileName);
        request.setDescription(getString(R.string.app_name));
        request.setMimeType(mimeType);
        String cookies = CookieManager.getInstance().getCookie(url);
        request.addRequestHeader("cookie", cookies);
        request.addRequestHeader("User-Agent", userAgent);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        AppUtils.toastShortDefault(context, WebActivity.this, "Downloading file");
        return downloadManager.enqueue(request);
    }

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NotNull Intent intent) {
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadReference == referenceId) {
                AppUtils.toastShortDefault(context, WebActivity.this, "Download complete");
            }
        }
    };

    public class CustomWebClient extends WebChromeClient {

        private View customView;
        private WebChromeClient.CustomViewCallback customViewCallback;
        private int defaultOrientation;
        private int defaultSystemUiVisibility;
        private AlertDialog.Builder javaScriptDialog;

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            progressBar.setProgress(newProgress);
            if (newProgress == 100) {
                progressBar.setVisibility(View.GONE);
            }
        }

        public void onReceivedTitle(WebView view, @NotNull String title) {
            if (title.equals("Webpage not available") || title.equals(errorPageUrl)){
                setTitle("Insta Movies");
            } else {
                titleText.setText(title);
            }
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, @NotNull JsResult result) {
            BottomSheetFragment bottomSheetDialog1 = BottomSheetFragment.newInstance();
            bottomSheetDialog1.setTitle("Hint from page");
            bottomSheetDialog1.setMessage(message);
            bottomSheetDialog1.setPositiveButton("Acknowledge", v -> {
                bottomSheetDialog1.dismiss();
                result.confirm();
            });
            bottomSheetDialog1.setCancelable(false);
            bottomSheetDialog1.show(getSupportFragmentManager(), "BottomSheetDialog");
            return true;
        }

        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            javaScriptDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
            javaScriptDialog.setTitle("Hint from page");
            javaScriptDialog.setMessage(message);
            javaScriptDialog.setPositiveButton("OK", (dialogInterface, i) -> result.confirm());
            javaScriptDialog.setNegativeButton("CANCEL", ((dialog, which) -> result.cancel()));
            javaScriptDialog.setCancelable(false);
            javaScriptDialog.create().show();
            return true;
        }

        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
            View editTextView = getLayoutInflater().inflate(R.layout.layout_edittext, null);
            EditText inputText = editTextView.findViewById(R.id.input_text);
            inputText.setHint("");
            inputText.setText(defaultValue);
            javaScriptDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
            javaScriptDialog.setMessage(message);
            javaScriptDialog.setPositiveButton("OK", (dialogInterface, i) -> result.confirm());
            javaScriptDialog.setNegativeButton("CANCEL", ((dialog, which) -> result.cancel()));
            javaScriptDialog.setCancelable(false);
            javaScriptDialog.setView(editTextView);
            javaScriptDialog.create().show();
            return true;
        }

        public void onShowCustomView(View view, CustomViewCallback callback) {
            hasShownCustomView = true;
            if (customView != null) {
                onHideCustomView();
                return;
            }
            view.setBackgroundColor(0xFF000000);
            customView = view;
            defaultSystemUiVisibility = getDefaultSystemUiVisibility();
            defaultOrientation = getRequestedOrientation();
            customViewCallback = callback;
            ((FrameLayout)getWindow().getDecorView()).addView(customView, new FrameLayout.LayoutParams(-1, -1));
            hideSystemUi();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                customView.setOnApplyWindowInsetsListener((v, insets) -> {
                    WindowInsets suppliedInsets = v.onApplyWindowInsets(insets);
                    if (suppliedInsets.isVisible(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars())) {
                        updateControls(0);
                    } else {
                        updateControls(getNavigationBarHeight());
                    }
                    return suppliedInsets;
                });
            } else {

                //Deprecated in Api level 30
                customView.setOnSystemUiVisibilityChangeListener(visibility -> {
                    if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0){
                        updateControls(getNavigationBarHeight());
                    } else {
                        updateControls(0);
                    }
                });

            }
        }

        public void onHideCustomView() {
            hasShownCustomView = false;
            ((FrameLayout)getWindow().getDecorView()).removeView(customView);
            showSystemUi(defaultSystemUiVisibility);
            setRequestedOrientation(defaultOrientation);
            customViewCallback.onCustomViewHidden();
            customView = null;
            customViewCallback = null;
        }

        void updateControls(int bottomMargin){
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) customView.getLayoutParams();
            params.bottomMargin = bottomMargin;
            customView.setLayoutParams(params);
        }

        int getNavigationBarHeight(){
            Resources resources = getResources();
            int resourceId = resources.getIdentifier("navigation_bar_height","dimen","android");
            if (resourceId > 0){
                return resources.getDimensionPixelSize(resourceId);
            }
            return 0;
        }

        @Nullable
        public Bitmap getDefaultVideoPoster() {
            if (super.getDefaultVideoPoster() == null){
                return BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.img_image_placeholder_h);
            } else {
                return super.getDefaultVideoPoster();
            }
        }

        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, @NotNull FileChooserParams fileChooserParams) {
            final boolean allowMultiple  = fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE;
            if (uploadMessage != null) {
                uploadMessage.onReceiveValue(null);
                uploadMessage = null;
            }
            uploadMessage = filePathCallback;

            Intent chooserIntent = fileChooserParams.createIntent();
            chooserIntent.addCategory(Intent.CATEGORY_OPENABLE);
            if (allowMultiple) {
                chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
            activityResultLauncher.launch(chooserIntent);
            return true;
        }

        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            AlertDialog.Builder geoLocationDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
            geoLocationDialog.setTitle("Fetch Geolocation Info");
            geoLocationDialog.setMessage(origin + " need to fetch your geolocation info");
            geoLocationDialog.setPositiveButton("ALLOW", (dialogInterface, i) -> callback.invoke(origin, true, false));
            geoLocationDialog.setNegativeButton("DENY", ((dialog, which) -> callback.invoke(origin, false, false)));
            geoLocationDialog.setCancelable(false);
            geoLocationDialog.create().show();
        }

        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, @NotNull Message resultMsg) {
            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
        }
    }

    private void ReportPage(String url) {
        View view = getLayoutInflater().inflate(R.layout.layout_edittext, null);
        EditText inputText = view.findViewById(R.id.input_text);
        inputText.setHint("Enter here why or leave it blank");
        reportDialog.setTitle("Report page");
        reportDialog.setPositiveButton("Report", (_dialog, _which) -> {
            reportMessage = inputText.getText().toString();
            Map<String, Object> reportMap = new HashMap<>();
            String timeStamp = new SimpleDateFormat("dd/MM hh:mm a", Locale.getDefault()).format(new Date());
            if (!reportMessage.equals("")) {
                reportMap.put("Reason", reportMessage);
            }
            reportMap.put("Url", url);
            reportMap.put("Time", timeStamp);
            reportDB.collection("WebReport")
                    .add(reportMap)
                    .addOnSuccessListener(documentReference -> AppUtils.toastShortDefault(context,WebActivity.this,"Reported successfully"))
                    .addOnFailureListener(e -> AppUtils.toastShortError(context,WebActivity.this,"Failed to send report"));
        });
        reportDialog.setView(view);
        reportDialog.create().show();
    }

    private void handleWebError(@NotNull WebView view, String failingUrl) {
        view.setVisibility(View.GONE);
        view.loadUrl(errorPageUrl);
        errorUrl = failingUrl;
        errorLinear.setVisibility(View.VISIBLE);
    }

    public void goBackWebView() {
        WebBackForwardList history = webView.copyBackForwardList();
        int index = -1;
        String url = null;

        while (webView.canGoBackOrForward(index)) {
            if (!history.getItemAtIndex(history.getCurrentIndex() + index).getUrl().equals(errorPageUrl)){
                webView.goBackOrForward(index);
                url = history.getItemAtIndex(-index).getUrl();
                break;
            }
            index--;
        }
        if (url == null) {
            finish();
        }
    }

    private void hideSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {

            //Deprecated in Api level 30
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        }
    }

    private void showSystemUi(int defaultSystemUiVisibility) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(defaultSystemUiVisibility);
            }
        } else {

            //Deprecated in Api level 30
            getWindow().getDecorView().setSystemUiVisibility(defaultSystemUiVisibility);

        }
    }

    private int getDefaultSystemUiVisibility() {
        int defaultSystemUiVisibility;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            defaultSystemUiVisibility = insetsController.getSystemBarsBehavior();
        } else {

            //Deprecated in Api level 30
            defaultSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();

        }
        return defaultSystemUiVisibility;
    }

    private void checkSettings() {
        allowCookies = settingsPreferences.getBoolean("cookie_preference", false);
        blockAds = settingsPreferences.getBoolean("advertisement_preference", false);
        blockPopups = !settingsPreferences.getBoolean("popup_preference", false);
        premiumUser = appData.getBoolean("prime_purchased", false);
    }

    public class MyJavaScriptInterface {

        final Activity activity;

        public MyJavaScriptInterface(Activity a) {
            activity = a;
        }

        @JavascriptInterface
        public void showToast(String toast) {
            AppUtils.toastShortDefault(context , activity, toast);
        }
    }
}