package instamovies.app.in;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.preference.PreferenceManager;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.jetbrains.annotations.NotNull;
import java.net.URISyntaxException;
import javax.annotation.Nullable;
import instamovies.app.in.fragments.BottomSheetFragment;
import instamovies.app.in.fragments.MovieDetailsFragment;
import instamovies.app.in.player.IntentUtil;
import instamovies.app.in.utils.AppUtils;

public class HiddenWebActivity extends AppCompatActivity {

    private WebView webView;
    private String pageUrl;
    private Intent webIntent = new Intent();
    private Intent handleIntent = new Intent();
    private String errorUrl = null;
    private Intent videoIntent = new Intent();
    private SharedPreferences settingsPreferences;
    private SharedPreferences appSettings;
    private BottomSheetFragment bottomSheetDialog;
    private ValueCallback<Uri[]> uploadMessage;
    private ProgressBar progressBar;
    private Handler handler;
    private LinearLayout errorLinear;
    private TextView causeText;
    private final String errorPageUrl = "about:blank";
    private Context context;
    private boolean hasShownCustomView = false;
    private boolean systemBrowser = false;
    private boolean chromeTabs = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidden_web);
        setTitle(getString(R.string.app_name));
        context = this;
        initializeActivity();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initializeActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        toolbar.setNavigationOnClickListener(_v -> finish());
        webView = findViewById(R.id.webView);
        pageUrl = getIntent().getStringExtra("HIDDEN_URL");
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        appSettings = context.getSharedPreferences("appSettings", Activity.MODE_PRIVATE);
        bottomSheetDialog = BottomSheetFragment.newInstance();
        progressBar = findViewById(R.id.progressbar);
        errorLinear = findViewById(R.id.error_view);
        causeText = findViewById(R.id.cause_text);
        Button retryButton = findViewById(R.id.retry_button);
        checkSettings();

        retryButton.setOnClickListener(v -> {
            errorLinear.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            // Using timer to avoid multiple clicking on retry
            handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                if (!isDestroyed()) {
                    goBackWebView();
                }
            }, getResources().getInteger(R.integer.retry_button_wait_time_default));
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            @TargetApi(Build.VERSION_CODES.M)
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String loadingUrl = request.getUrl().toString();
                if (URLUtil.isNetworkUrl(loadingUrl)) {
                    if (loadingUrl.contains("mediafire.com") || loadingUrl.contains("https://dood.so")) {
                        if (!systemBrowser) {
                            webIntent = new Intent();
                            webIntent.setClass(context, WebActivity.class);
                            webIntent.putExtra("WEB_URL", loadingUrl);
                            startActivity(webIntent);
                        } else {
                            if (chromeTabs) {
                                CustomTabsIntent.Builder customTabsBuilder = new CustomTabsIntent.Builder();
                                CustomTabsIntent customTabsIntent = customTabsBuilder.build();
                                customTabsIntent.launchUrl(context, Uri.parse(loadingUrl));
                            } else {
                                try {
                                    handleIntent = new Intent();
                                    handleIntent.setAction(Intent.ACTION_VIEW);
                                    handleIntent.setData(Uri.parse(loadingUrl));
                                    startActivity(handleIntent);
                                } catch (android.content.ActivityNotFoundException notFoundException){
                                    AppUtils.toastError(context, HiddenWebActivity.this, getString(R.string.error_activity_not_found));
                                }
                            }
                        }
                    } else if (loadingUrl.contains("https://youtu.be/")) {
                        String itemLink = loadingUrl.replace("https://youtu.be/","");
                        videoIntent = new Intent();
                        videoIntent.setClass(context, YouTubePlayerActivity.class);
                        videoIntent.putExtra("VIDEO_ID", itemLink);
                        startActivity(videoIntent);
                    } else {
                        return false;
                    }
                } else {
                    handleNonNetworkUrls(loadingUrl);
                }
                return true;
            }

            @Deprecated
            public boolean shouldOverrideUrlLoading(WebView view, String loadingUrl) {
                if (URLUtil.isNetworkUrl(loadingUrl)) {
                    if (loadingUrl.contains("mediafire.com") || loadingUrl.contains("https://dood.so")) {
                        if (!systemBrowser) {
                            webIntent = new Intent();
                            webIntent.setClass(context, WebActivity.class);
                            webIntent.putExtra("WEB_URL", loadingUrl);
                            startActivity(webIntent);
                        } else {
                            if (chromeTabs) {
                                CustomTabsIntent.Builder customTabsBuilder = new CustomTabsIntent.Builder();
                                CustomTabsIntent customTabsIntent = customTabsBuilder.build();
                                customTabsIntent.launchUrl(context, Uri.parse(loadingUrl));
                            } else {
                                try {
                                    handleIntent = new Intent();
                                    handleIntent.setAction(Intent.ACTION_VIEW);
                                    handleIntent.setData(Uri.parse(loadingUrl));
                                    startActivity(handleIntent);
                                } catch (android.content.ActivityNotFoundException notFoundException){
                                    AppUtils.toastError(context, HiddenWebActivity.this, getString(R.string.error_activity_not_found));
                                }
                            }
                        }
                    } else if (loadingUrl.contains("https://youtu.be/")) {
                        String itemLink = loadingUrl.replace("https://youtu.be/","");
                        videoIntent = new Intent();
                        videoIntent.setClass(context, YouTubePlayerActivity.class);
                        videoIntent.putExtra("VIDEO_ID", itemLink);
                        startActivity(videoIntent);
                    } else {
                        return false;
                    }
                } else {
                    handleNonNetworkUrls(loadingUrl);
                }
                return true;
            }

            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                if (handler != null){
                    handler.proceed();
                }
            }

            @Override
            @TargetApi(Build.VERSION_CODES.M)
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                String failingUrl = request.getUrl().toString();
                onPageError(getString(R.string.error_no_connection), failingUrl);
            }

            @Deprecated
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                onPageError(getString(R.string.error_no_connection), failingUrl);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (!url.equals(errorPageUrl)) {
                    errorLinear.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.VISIBLE);
                    errorUrl = null;
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
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
        webView.getSettings().setBlockNetworkImage(false);
        webView.getSettings().setBlockNetworkLoads(false);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.getSettings().setDatabaseEnabled(false);
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
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true);
        webView.setSoundEffectsEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setLongClickable(true);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE,null);
        webView.setOnLongClickListener(view -> true);
        webView.requestFocusFromTouch();
        webView.loadUrl(pageUrl);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.setWebChromeClient(new CustomWebClient());
        webView.addJavascriptInterface(new MyJavaScriptInterface(this), "Android");
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkSettings();
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
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        webView.destroy();
    }

    @Override
    public void onBackPressed() {
        if (errorUrl != null) {
            // If error occurred in first page, then on back pressed it will finish the activity else webview go back
            if (errorUrl.contains(pageUrl)){
                finish();
            } else {
                goBackWebView();
            }
        } else {
            goBackWebView();
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

    private void goBackWebView() {
        WebBackForwardList webHistory = webView.copyBackForwardList();
        int index = -1;
        String url = null;

        while (webView.canGoBackOrForward(index)) {
            if (!webHistory.getItemAtIndex(webHistory.getCurrentIndex() + index).getUrl().equals(errorPageUrl)) {
                webView.goBackOrForward(index);
                url = webHistory.getItemAtIndex(-index).getUrl();
                break;
            }
            index--;
        }
        if (url == null) {
            finish();
        }
    }

    public class CustomWebClient extends WebChromeClient {

        private View customView;
        private WebChromeClient.CustomViewCallback customViewCallback;
        private int defaultOrientation;
        private int defaultSystemUiVisibility;
        private AlertDialog.Builder javaScriptDialog;

        public void onReceivedTitle(WebView view, @NotNull String title) {
            if (title.equals(errorPageUrl) || title.length() >= 14){
                setTitle(getString(R.string.app_name));
            } else {
                setTitle(title);
            }
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, @NotNull JsResult result) {
            BottomSheetFragment bottomSheetDialog1 = BottomSheetFragment.newInstance();
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
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.img_image_placeholder_h);
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
        systemBrowser = settingsPreferences.getBoolean("system_browser_preference", false);
        chromeTabs = settingsPreferences.getBoolean("chrome_tabs_preference", true);
    }

    private void onPageError(String error, String url) {
        progressBar.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        webView.loadUrl(errorPageUrl);
        errorUrl = url;
        causeText.setText(error);
        errorLinear.setVisibility(View.VISIBLE);
    }

    private void handleNonNetworkUrls(@NonNull String url) {
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
                    handleIntent = new Intent();
                    handleIntent.setAction(Intent.ACTION_VIEW);
                    handleIntent.setData(Uri.parse(replacedUrl));
                    startActivity(handleIntent);
                } catch (android.content.ActivityNotFoundException notFoundException){
                    AppUtils.toastError(context, HiddenWebActivity.this, getString(R.string.error_activity_not_found));
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
                        handleIntent = new Intent();
                        handleIntent.setAction(Intent.ACTION_VIEW);
                        handleIntent.setData(Uri.parse(replacedUrl));
                        startActivity(handleIntent);
                    } catch (android.content.ActivityNotFoundException notFoundException){
                        AppUtils.toastError(context, HiddenWebActivity.this, getString(R.string.error_activity_not_found));
                    }
                }
            } else {
                webIntent = new Intent();
                webIntent.setClass(context, WebActivity.class);
                webIntent.putExtra("WEB_URL", replacedUrl);
                startActivity(webIntent);
            }
        }
        if (url.startsWith("movie://")) {
            String replacedUrl = url.replace("movie://","");
            Uri uri = Uri.parse(replacedUrl);
            String movieID = uri.getQueryParameter("movie_id");
            webIntent = new Intent();
            if (appSettings.getBoolean("details_activity", true)) {
                webIntent.setClass(context, MovieDetailsActivity.class);
                webIntent.putExtra("movie_details_url", replacedUrl);
                webIntent.putExtra("movie_id", movieID);
            } else {
                webIntent.setClass(context, HiddenWebActivity.class);
                webIntent.putExtra("HIDDEN_URL", replacedUrl);
            }
            startActivity(webIntent);
        }
        if (url.startsWith("video://")) {
            String replacedUrl = url.replace("video://","");
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
                bottomSheetDialog.setTitle("Error");
                bottomSheetDialog.setMessage("Torrent clients are not installed. Please click the download button to continue.");
                bottomSheetDialog.setPositiveButton("Download", v -> {
                    bottomSheetDialog.dismiss();
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                    marketIntent.setData(Uri.parse("market://details?id=com.utorrent.client"));
                    try {
                        startActivity(marketIntent);
                    } catch (ActivityNotFoundException notFoundException1){
                        AppUtils.toastError(context, HiddenWebActivity.this, getString(R.string.error_activity_not_found));
                    }
                });
                bottomSheetDialog.show(getSupportFragmentManager(), "BottomSheetDialog");
            }
        }
        if (url.startsWith("intent://")){
            try {
                Intent handlerIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                if (handlerIntent != null){
                    PackageManager packageManager = context.getPackageManager();
                    ResolveInfo info = packageManager.resolveActivity(handlerIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    if (info != null){
                        startActivity(handlerIntent);
                    } else {
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                        marketIntent.setData(Uri.parse("market://details?id="+handlerIntent.getPackage()));
                        try {
                            startActivity(marketIntent);
                        } catch (ActivityNotFoundException notFoundException) {
                            AppUtils.toastError(context, HiddenWebActivity.this, getString(R.string.error_activity_not_found));
                        }
                    }
                }
            } catch (URISyntaxException uriSyntaxException){
                AppUtils.toastError(context, HiddenWebActivity.this, getString(R.string.error_uri_syntax_exception));
            }
        }
    }

    public class MyJavaScriptInterface {

        final Activity activity;

        public MyJavaScriptInterface(Activity a) {
            activity = a;
        }

        @JavascriptInterface
        public void showToast(String toast) {
            AppUtils.toast(context , activity, toast);
        }

        @JavascriptInterface
        public void showMovieDetails(String movieID) {
            MovieDetailsFragment movieDetailsFragment = MovieDetailsFragment.newInstance();
            movieDetailsFragment.setFileID(movieID);
            movieDetailsFragment.show(getSupportFragmentManager(), "BottomSheetDialog");
        }

        @JavascriptInterface
        public void loadMovies(String baseURL, String referencePath, String title) {
            webIntent = new Intent();
            webIntent.setClass(context, MoviesActivity.class);
            webIntent.putExtra("base_url_movie_json", baseURL);
            webIntent.putExtra("reference_path_movie_json", referencePath);
            webIntent.putExtra("title_movie_act", title);
            startActivity(webIntent);
        }
    }
}