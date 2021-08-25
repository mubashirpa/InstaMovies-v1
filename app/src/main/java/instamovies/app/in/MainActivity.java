package instamovies.app.in;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.FirebaseDatabase;
import android.provider.Settings;
import android.util.Log;
import android.view.*;
import android.webkit.URLUtil;
import android.widget.*;
import android.graphics.*;
import java.io.File;
import java.util.HashMap;
import java.util.Objects;
import android.content.Intent;
import android.net.Uri;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.storage.FirebaseStorage;
import android.app.Activity;
import org.jetbrains.annotations.NotNull;
import instamovies.app.in.database.ThumbnailDatabase;
import instamovies.app.in.database.BannerDatabase;
import instamovies.app.in.fragments.BottomSheetFragment;
import instamovies.app.in.fragments.RequestDialogFragment;
import instamovies.app.in.fragments.UpdateProgressFragment;
import instamovies.app.in.player.IntentUtil;
import instamovies.app.in.utils.AppUtils;
import instamovies.app.in.utils.RecyclerDecorationHorizontal;
import instamovies.app.in.utils.FileUtil;
import instamovies.app.in.utils.NetworkUtil;
import instamovies.app.in.utils.NotificationUtils;

public class MainActivity extends AppCompatActivity {

    private TextView scrollTop;
    private AlertDialog.Builder notifyDialog;
    private AlertDialog.Builder maintenanceDialog;
    private DrawerLayout drawerLayout;
    private RewardedAd mRewardedAd;
    private SharedPreferences appSettings, appData, settingsPreferences;
    private Intent webIntent = new Intent();
    private Intent languageIntent = new Intent();
    private final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private final DatabaseReference SystemDB = firebaseDatabase.getReference("System");
    private final DatabaseReference VersionDB = firebaseDatabase.getReference("Version");
    private ChildEventListener systemChildEventListener;
    private ChildEventListener versionChildEventListener;
    private AdView adView1, adView2;
    private final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private String HOT_LINKS_URL, CONTACT_URL, ABOUT_URL, MUSIC_URL, GAMES_URL, TUTORIAL_URL;
    private LinearLayout progressbarLayout;
    private Context context;
    private final String LOG_TAG = "MainActivity";
    private final int REQUEST_CODE_STORAGE = 1001;
    private boolean premiumUser = false;
    private View errorLinear;
    private boolean isDatabaseConnected = false;
    private String OFFICIAL_WEBSITE_URL = "";
    private UpdateProgressFragment updateProgressFragment;
    private DownloadManager downloadManager;
    private long downloadReference;
    private String latestVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        MobileAds.initialize(context);
        adView1 = findViewById(R.id.adView1);
        adView2 = findViewById(R.id.adView2);
        appData = getSharedPreferences("appData", Activity.MODE_PRIVATE);
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String themePreference = settingsPreferences.getString("theme_preference","");

        switch (themePreference) {
            case "System default":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "Set by Battery Saver":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                break;
            case "Light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "Dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            AlertDialog.Builder permissionDialog = new AlertDialog.Builder(context);
            permissionDialog.setTitle("Need permission");
            permissionDialog.setMessage("To download movies and files, give us permission to use your storage");
            permissionDialog.setPositiveButton("GRANT PERMISSION", (dialogInterface, i) -> ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE));
            permissionDialog.setNegativeButton("EXIT", (dialogInterface, i) -> finishAffinity());
            permissionDialog.setCancelable(false);
            permissionDialog.create().show();
        } else {
            initializeActivity();
        }
    }

    private void initializeActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        drawerLayout = findViewById(R.id.drawer);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(navigationItemSelectedListener);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        scrollTop = findViewById(R.id.scroll_top);
        scrollTop.setSelected(true);
        TextView scrollBottom = findViewById(R.id.scroll_bottom);
        scrollBottom.setSelected(true);
        appSettings = getSharedPreferences("appSettings", Activity.MODE_PRIVATE);
        notifyDialog = new AlertDialog.Builder(context);
        maintenanceDialog = new AlertDialog.Builder(context);
        AlertDialog.Builder uninstallDialog = new AlertDialog.Builder(context);
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        errorLinear = findViewById(R.id.error_linear);
        Button retryButton = findViewById(R.id.retry_button);
        RecyclerView recyclerMalayalam = findViewById(R.id.recycler_malayalam);
        RecyclerView recyclerTamil = findViewById(R.id.recycler_tamil);
        RecyclerView recyclerEnglish = findViewById(R.id.recycler_english);
        RecyclerView recyclerHindi = findViewById(R.id.recycler_hindi);
        RecyclerView recyclerMore = findViewById(R.id.recycler_more);
        ViewPager2 viewSlider = findViewById(R.id.view_slider);
        LinearLayout quickLinkMusic = findViewById(R.id.quick_link_music);
        LinearLayout quickLinkGames = findViewById(R.id.quick_link_games);
        LinearLayout quickLinkHot = findViewById(R.id.quick_link_hot);
        LinearLayout quickLinkMore = findViewById(R.id.quick_link_more);
        LinearLayout layoutMalayalam = findViewById(R.id.layout_malayalam);
        LinearLayout layoutTamil = findViewById(R.id.layout_tamil);
        LinearLayout layoutEnglish = findViewById(R.id.layout_english);
        LinearLayout layoutHindi = findViewById(R.id.layout_hindi);
        LinearLayout layoutMore = findViewById(R.id.layout_more);
        progressbarLayout = findViewById(R.id.progressbar_layout);
        HOT_LINKS_URL = getString(R.string.hot_links_url);
        CONTACT_URL = getString(R.string.contact_url);
        ABOUT_URL = getString(R.string.about_url);
        MUSIC_URL = getString(R.string.music_url);
        GAMES_URL = getString(R.string.games_url);
        TUTORIAL_URL = getString(R.string.tutorial_url);
        OFFICIAL_WEBSITE_URL = getString(R.string.official_download_website);
        checkSettings();

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
            }
        };
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        Bundle bundle = new Bundle();
        bundle.putString("status","opened");
        firebaseAnalytics.logEvent("main_open", bundle);

        new BannerDatabase(context, MainActivity.this, viewSlider);

        NotificationUtils notificationService = new NotificationUtils(context);
        notificationService.CreateUpdateChannel();
        notificationService.CreateMoviesChannel();
        if (NetworkUtil.isOnline(context)) {
            AppUtils.toastShortError(context,MainActivity.this,"No connection");
            progressbarLayout.setVisibility(View.GONE);
            errorLinear.setVisibility(View.VISIBLE);
        }
        boolean autoUpdate = settingsPreferences.getBoolean("auto_update", false);
        if (!autoUpdate) {
            checkUpdate();
        }

        try {
            getPackageManager().getPackageInfo("instamovies.haxd", android.content.pm.PackageManager.GET_ACTIVITIES);
            uninstallDialog.setMessage("An older version of Insta Movies found. Do you want to uninstall it?\n\n(Note-If the uninstall button doesn't work properly please uninstall the older version manually.)");
            uninstallDialog.setPositiveButton("UNINSTALL", (dialogInterface, i) -> {
                Uri packageURI = Uri.parse("package:instamovies.haxd");
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                startActivity(uninstallIntent);
            });
            uninstallDialog.setNegativeButton("LATER", (dialogInterface, i) -> {
            });
            uninstallDialog.create().show();
        } catch (android.content.pm.PackageManager.NameNotFoundException nameNotFoundException) {
            Log.e(LOG_TAG, nameNotFoundException.getMessage());
        }

        retryButton.setOnClickListener(v -> {
            if (isDatabaseConnected) {
                if (progressbarLayout.isShown()) {
                    progressbarLayout.setVisibility(View.GONE);
                }
                if (errorLinear.isShown()) {
                    errorLinear.setVisibility(View.GONE);
                }
            } else {
                errorLinear.setVisibility(View.GONE);
                progressbarLayout.setVisibility(View.VISIBLE);
                if (NetworkUtil.isOnline(context)) {
                    progressbarLayout.setVisibility(View.GONE);
                    errorLinear.setVisibility(View.VISIBLE);
                }
            }
        });

        quickLinkMusic.setOnClickListener(v -> {
            webIntent = new Intent();
            webIntent.setClass(context, WebActivity.class);
            webIntent.putExtra("WEB_URL", MUSIC_URL);
            startActivity(webIntent);
        });

        quickLinkGames.setOnClickListener(v -> {
            webIntent = new Intent();
            webIntent.setClass(context, HiddenWebActivity.class);
            webIntent.putExtra("HIDDEN_URL", GAMES_URL);
            startActivity(webIntent);
        });

        quickLinkHot.setOnClickListener(v -> {
            webIntent = new Intent();
            webIntent.setClass(context, HiddenWebActivity.class);
            webIntent.putExtra("HIDDEN_URL", HOT_LINKS_URL);
            startActivity(webIntent);
        });

        quickLinkMore.setOnClickListener(v -> {
            Intent moreIntent = new Intent();
            moreIntent.setClass(context, MoreActivity.class);
            startActivity(moreIntent);
        });

        View.OnClickListener languageClickListener = v -> {
            languageIntent = new Intent();
            languageIntent.setClass(context, CategoriesActivity.class);
            startActivity(languageIntent);
        };
        layoutMalayalam.setOnClickListener(languageClickListener);
        layoutTamil.setOnClickListener(languageClickListener);
        layoutEnglish.setOnClickListener(languageClickListener);
        layoutHindi.setOnClickListener(languageClickListener);
        layoutMore.setOnClickListener(languageClickListener);

        if (!appData.getString("scroll_main", "").equals("")) {
            scrollTop.setText(appData.getString("scroll_main", ""));
        }

        systemChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                GenericTypeIndicator<HashMap<String, Object>> ind = new GenericTypeIndicator<HashMap<String, Object>>() {
                };
                final String childKey = dataSnapshot.getKey();
                final HashMap<String, Object> childValue = dataSnapshot.getValue(ind);

                isDatabaseConnected = true;
                if (progressbarLayout.isShown()) {
                    progressbarLayout.setVisibility(View.GONE);
                }
                if (errorLinear.isShown()) {
                    errorLinear.setVisibility(View.GONE);
                }

                if (childKey != null && childKey.equals("Scroll")) {
                    if (childValue != null && childValue.containsKey("Text")) {
                        String scrollTopMessage = Objects.requireNonNull(childValue.get("Text")).toString();
                        if (!scrollTopMessage.equals(appData.getString("scroll_main", ""))) {
                            appData.edit().putString("scroll_main", scrollTopMessage).apply();
                            scrollTop.setText(scrollTopMessage);
                        }
                    } else {
                        scrollTop.setVisibility(View.GONE);
                    }
                }

                if (childKey != null && childKey.equals("Form")) {
                    if (childValue != null) {
                        String Message = "";
                        View dialogView = getLayoutInflater().inflate(R.layout.layout_checkbox, null);
                        CheckBox checkBox = dialogView.findViewById(R.id.checkBox);
                        if (childValue.containsKey("Title")) {
                            String Title = Objects.requireNonNull(childValue.get("Title")).toString();
                            notifyDialog.setTitle(Title);
                        }
                        if (childValue.containsKey("Message")){
                            Message = Objects.requireNonNull(childValue.get("Message")).toString();
                            notifyDialog.setMessage("\n" + Message);
                            notifyDialog.setView(dialogView);
                            notifyDialog.setPositiveButton("CLOSE", (_dialog, _which) -> _dialog.dismiss());
                            if (childValue.containsKey("Link")) {
                                notifyDialog.setNegativeButton("OK", ((dialog, which) -> {
                                    webIntent = new Intent();
                                    webIntent.setClass(context, WebActivity.class);
                                    webIntent.putExtra("WEB_URL", Objects.requireNonNull(childValue.get("Link")).toString());
                                    startActivity(webIntent);
                                }));
                            }
                            if (!Message.equals(appData.getString("form_message_main", ""))) {
                                notifyDialog.create().show();
                            }
                        }

                        String finalMessage = Message;
                        checkBox.setOnCheckedChangeListener((compoundButton, b) -> {
                            if(compoundButton.isChecked()){
                                appData.edit().putString("form_message_main", finalMessage).apply();
                            }else{
                                appData.edit().putString("form_message_main", "").apply();
                            }
                        });
                    }
                }

                if (childKey != null && childKey.equals("Maintenance")) {
                    if (childValue != null && childValue.containsKey("Notice")) {
                        String Notice = Objects.requireNonNull(childValue.get("Notice")).toString();
                        maintenanceDialog.setMessage(Notice);
                        maintenanceDialog.setNeutralButton("Close", (dialog, which) -> finish());
                        maintenanceDialog.setCancelable(false);
                        maintenanceDialog.create().show();
                        NotificationUtils notificationService = new NotificationUtils(context);
                        notificationService.DefaultNotificationService("Maintenance", Notice);
                    }
                }

                if (childKey != null && childKey.equals("WebView")) {
                    if (childValue != null) {
                        if (childValue.containsKey("Maintenance")){
                            if (Objects.equals(childValue.get("Maintenance"), "true")){
                                settingsPreferences.edit().putBoolean("system_browser_preference", true).apply();
                                appSettings.edit().putBoolean("web_maintenance", true).apply();
                            } else {
                                settingsPreferences.edit().putBoolean("system_browser_preference", false).apply();
                                appSettings.edit().putBoolean("web_maintenance", false).apply();
                            }
                        }
                        if (childValue.containsKey("AdServer")) {
                            String fileUrl = Objects.requireNonNull(childValue.get("AdServer")).toString();
                            File directory = new File(FileUtil.getPackageDataDir(context) + "/downloads");
                            boolean success;
                            if (!directory.exists()) {
                                success = directory.mkdirs();
                                if (!success) {
                                    return;
                                }
                            }

                            File filePath = new File(FileUtil.getPackageDataDir(context) + "/downloads", "ad-pages-list.txt");
                            if (!appData.getString("web_ad_hosts", "").equals(fileUrl)) {
                                if (FileUtil.isExistFile(filePath)) {
                                    FileUtil.deleteFile(filePath);
                                }
                                firebaseStorage.getReferenceFromUrl(fileUrl).getFile(filePath)
                                        .addOnSuccessListener(taskSnapshot -> appData.edit().putString("ad_pages_list", fileUrl).apply());
                            }
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                AppUtils.toastShortDefault(context, MainActivity.this, databaseError.getMessage());
            }
        };
        SystemDB.addChildEventListener(systemChildEventListener);
        initializeRecycler(recyclerMalayalam, "Malayalam");
        initializeRecycler(recyclerTamil, "Tamil");
        initializeRecycler(recyclerEnglish, "English");
        initializeRecycler(recyclerHindi, "Hindi");

        RecyclerDecorationHorizontal recyclerDecoration = new RecyclerDecorationHorizontal(20, 20, 10);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerMore.setLayoutManager(layoutManager);
        recyclerMore.setItemAnimator(new DefaultItemAnimator());
        recyclerMore.addItemDecoration(recyclerDecoration);
        ThumbnailDatabase.ThumbnailDatabaseMore("More", recyclerMore, context);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            AlertDialog.Builder exitDialog = new AlertDialog.Builder(context);
            exitDialog.setTitle("Confirm");
            exitDialog.setMessage("Are you sure want to exit?");
            exitDialog.setPositiveButton("Yes", (_dialog, _which) -> finish());
            exitDialog.setNegativeButton("Cancel", (_dialog, _which) -> {
            });
            exitDialog.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BannerDatabase.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        BannerDatabase.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        checkSettings();
        if (!premiumUser) {
            AdRequest adRequest1 = new AdRequest.Builder().build();
            adView1.loadAd(adRequest1);
            AdRequest adRequest2 = new AdRequest.Builder().build();
            adView2.loadAd(adRequest2);
        } else {
            adView1.setVisibility(View.GONE);
            adView2.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (versionChildEventListener != null) {
            VersionDB.removeEventListener(versionChildEventListener);
        }
        if (systemChildEventListener != null) {
            SystemDB.removeEventListener(systemChildEventListener);
        }
        try {
            unregisterReceiver(downloadReceiver);
        } catch (Exception exception) {
            Log.e(LOG_TAG, exception.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String @NotNull [] permissions, int @NotNull [] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if (requestCode == REQUEST_CODE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                Uri uri = Uri.fromParts("package",getPackageName(),null);
                intent.setData(uri);
                try {
                    startActivity(intent);
                } catch (Exception exception) {
                    Log.e(LOG_TAG, exception.getMessage());
                }
                AppUtils.toastShortError(context,MainActivity.this,"Failed to get permission. Give us permission manually");
                finish();
            } else {
                initializeActivity();
            }
        }
    }

    final NavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
            int itemId = item.getItemId();
            Intent drawerIntent;
            if (itemId == R.id.drawerNotify){
                drawerIntent = new Intent();
                drawerIntent.setClass(context, NotificationsActivity.class);
                startActivity(drawerIntent);
            }
            if (itemId == R.id.drawerFeedback){
                drawerIntent = new Intent();
                drawerIntent.setClass(context, FeedbackActivity.class);
                startActivity(drawerIntent);
            }
            if (itemId == R.id.drawerMore){
                drawerIntent = new Intent();
                drawerIntent.setClass(context, MoreActivity.class);
                startActivity(drawerIntent);
            }
            if (itemId == R.id.drawerRequest){
                RequestDialogFragment requestDialogFragment = RequestDialogFragment.newInstance();
                requestDialogFragment.setActivity(MainActivity.this);
                requestDialogFragment.show(getSupportFragmentManager(), "BottomSheetDialog");
            }
            if (itemId == R.id.drawerStream){
                String networkURL = pasteText();
                AlertDialog.Builder streamDialog = new AlertDialog.Builder(context);
                View view = getLayoutInflater().inflate(R.layout.layout_edittext, null);
                EditText inputText = view.findViewById(R.id.input_text);
                inputText.setHint("Enter a network URL");
                if (URLUtil.isNetworkUrl(networkURL)) {
                    inputText.setText(networkURL);
                }
                streamDialog.setTitle("Network stream");
                streamDialog.setMessage("\n" + "Please enter a network URL:");
                streamDialog.setPositiveButton("OK", (_dialog, _which) -> {
                    String streamLink = inputText.getText().toString();
                    if (!streamLink.equals("")) {
                        if (!URLUtil.isValidUrl(streamLink)) {
                            AppUtils.toastShortDefault(context, MainActivity.this, "Please enter a valid URL");
                            return;
                        }
                        Intent videoIntent = new Intent();
                        videoIntent.setClass(context, PlayerActivity.class);
                        videoIntent.putExtra("VIDEO_URI", streamLink);
                        videoIntent.putExtra(IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA, true);
                        startActivity(videoIntent);
                    }
                });
                streamDialog.setNegativeButton("CANCEL", (_dialog, _which) -> {

                });
                streamDialog.setView(view);
                streamDialog.create().show();
            }
            if (itemId == R.id.drawerSettings){
                drawerIntent = new Intent();
                drawerIntent.setClass(context, SettingsActivity.class);
                startActivity(drawerIntent);
            }
            if (itemId == R.id.drawerUpdate){
                checkUpdate();
            }
            if (itemId == R.id.drawerTutorial){
                webIntent = new Intent();
                webIntent.setClass(context, HiddenWebActivity.class);
                webIntent.putExtra("HIDDEN_URL", TUTORIAL_URL);
                startActivity(webIntent);
            }
            if (itemId == R.id.drawerDonate){
                if (!premiumUser) {
                    AppUtils.toastShortDefault(context, MainActivity.this, "Coming soon");
                } else {
                    drawerIntent = new Intent();
                    drawerIntent.setClass(context, BillingActivity.class);
                    startActivity(drawerIntent);
                }
            }
            if (itemId == R.id.drawerShare){
                shareApp();
            }
            if (itemId == R.id.drawerContact){
                drawerIntent = new Intent();
                drawerIntent.setAction(Intent.ACTION_VIEW);
                drawerIntent.setData(Uri.parse(CONTACT_URL));
                try {
                    startActivity(drawerIntent);
                } catch (android.content.ActivityNotFoundException activityNotFoundException){
                    Log.e(LOG_TAG, activityNotFoundException.getMessage());
                }
            }
            if (itemId == R.id.drawerSupport){
                loadRewardedAd();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull final MenuItem item){
        Intent menuIntent = new Intent();
        switch (item.getTitle().toString()) {
            case "Notification":
                menuIntent.setClass(context, NotificationsActivity.class);
                startActivity(menuIntent);
                return true;
            case "Settings":
                menuIntent = new Intent();
                menuIntent.setClass(context, SettingsActivity.class);
                startActivity(menuIntent);
                return true;
            case "Share":
                shareApp();
                return true;
            case "About":
                webIntent = new Intent();
                webIntent.setClass(context, HiddenWebActivity.class);
                webIntent.putExtra("HIDDEN_URL", ABOUT_URL);
                startActivity(webIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initializeRecycler(@NotNull RecyclerView recyclerView, String ChildID) {
        RecyclerDecorationHorizontal recyclerDecoration = new RecyclerDecorationHorizontal(20, 20, 10);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(recyclerDecoration);
        ThumbnailDatabase.ThumbnailDatabaseMain(ChildID, recyclerView, context, progressbarLayout);
    }

    private void checkUpdate() {
        String currentVersion;
        try {
            currentVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException nameNotFoundException) {
            AppUtils.toastShortDefault(context, MainActivity.this, "Failed to check update");
            return;
        }
        versionChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                GenericTypeIndicator<HashMap<String, Object>> ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
                final String childKey = snapshot.getKey();
                final HashMap<String, Object> childValue = snapshot.getValue(ind);
                if (childKey != null && childKey.equals("Latest")) {
                    if (childValue != null && childValue.containsKey("Latest Version")) {
                        latestVersion = Objects.requireNonNull(childValue.get("Latest Version")).toString();
                        if (latestVersion.equals(currentVersion)) {
                            AppUtils.toastShortDefault(context, MainActivity.this, "App is up to date");
                        } else {
                            AppUtils.toastShortDefault(context, MainActivity.this, "Update Available");
                            if (!childValue.containsKey("Update Link")) {
                                return;
                            }
                            String updateLink = Objects.requireNonNull(childValue.get("Update Link")).toString();
                            //Showing update dialog
                            final AlertDialog updateDialog = new AlertDialog.Builder(context).create();
                            View convertView = getLayoutInflater().inflate(R.layout.layout_update_dialog, null);
                            updateDialog.setView(convertView);
                            updateDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                            Button updateButton = convertView.findViewById(R.id.update_button);
                            Button laterButton = convertView.findViewById(R.id.later_button);
                            TextView versionText = convertView.findViewById(R.id.version);
                            TextView changeLog = convertView.findViewById(R.id.change_log);
                            TextView manualUpdate = convertView.findViewById(R.id.manual_update);
                            versionText.setText(latestVersion);
                            if (childValue.containsKey("Change Log")) {
                                changeLog.setText(Objects.requireNonNull(childValue.get("Change Log")).toString());
                            } else {
                                changeLog.setText(getString(R.string.update_message));
                            }

                            updateButton.setOnClickListener(v -> {
                                updateDialog.dismiss();
                                File filePath = new File(FileUtil.getPackageDataDir(context) + "/downloads/update", "app_update_" + latestVersion + ".apk");
                                if (filePath.exists()) {
                                    AlertDialog.Builder installDialog = new AlertDialog.Builder(context);
                                    installDialog.setTitle("File detected");
                                    installDialog.setMessage("Detected necessary file for app update. Do you want to install it?\n\nNote: if you are having any problem with installing the current file please re-download it.");
                                    installDialog.setPositiveButton("Install", (_dialog, _which) -> installUpdate());
                                    installDialog.setNegativeButton("Re download", (_dialog, _which) -> {
                                        FileUtil.deleteFile(filePath);
                                        if (childValue.containsKey("Instance Link")) {
                                            String instanceLink = Objects.requireNonNull(childValue.get("Instance Link")).toString();
                                            firebaseUpdate(updateLink, instanceLink);
                                        } else {
                                            downloadManagerUpdate(updateLink);
                                        }
                                    });
                                    installDialog.show();
                                } else {
                                    if (childValue.containsKey("Instance Link")) {
                                        String instanceLink = Objects.requireNonNull(childValue.get("Instance Link")).toString();
                                        firebaseUpdate(updateLink, instanceLink);
                                    } else {
                                        downloadManagerUpdate(updateLink);
                                    }
                                }
                            });

                            laterButton.setOnClickListener(v -> {
                                updateDialog.dismiss();
                                BottomSheetFragment bottomSheetDialog = BottomSheetFragment.newInstance();
                                bottomSheetDialog.setTitle("Important");
                                bottomSheetDialog.setMessage(getString(R.string.warning_bottomsheet_main));
                                bottomSheetDialog.setPositiveButton("Close", v1 -> bottomSheetDialog.dismiss());
                                bottomSheetDialog.setCancelable(true);
                                bottomSheetDialog.show(getSupportFragmentManager(), "BottomSheetDialog");
                            });

                            manualUpdate.setOnClickListener(v ->{
                                updateDialog.dismiss();
                                webIntent = new Intent();
                                webIntent.setAction(Intent.ACTION_VIEW);
                                webIntent.setData(Uri.parse(updateLink));
                                try {
                                    startActivity(webIntent);
                                } catch (android.content.ActivityNotFoundException activityNotFoundException){
                                    //failed to load url
                                    BottomSheetFragment bottomSheetDialog = BottomSheetFragment.newInstance();
                                    bottomSheetDialog.setTitle("Error");
                                    bottomSheetDialog.setMessage("We can't find any application to open the URL. Please browse our official website " + OFFICIAL_WEBSITE_URL + " to download the latest version.");
                                    bottomSheetDialog.setPositiveButton("Close", v1 -> bottomSheetDialog.dismiss());
                                    bottomSheetDialog.setCancelable(true);
                                    bottomSheetDialog.show(getSupportFragmentManager(), "BottomSheetDialog");
                                }
                            });

                            updateDialog.setCancelable(false);
                            updateDialog.show();
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                AppUtils.toastShortDefault(context, MainActivity.this, "Failed to check update");
            }
        };
        VersionDB.addChildEventListener(versionChildEventListener);
    }

    private void firebaseUpdate(String updateLink, String instanceLink) {
        File directory = new File(FileUtil.getPackageDataDir(context) + "/downloads/update");
        boolean success;
        if (!directory.exists()) {
            success = directory.mkdirs();
            if (!success) {
                downloadManagerUpdate(updateLink);
                return;
            }
        }
        File filePath = new File(FileUtil.getPackageDataDir(context) + "/downloads/update", "app_update_" + latestVersion + ".apk");

        updateProgressFragment = UpdateProgressFragment.newInstance();
        updateProgressFragment.setCancelable(false);
        updateProgressFragment.show(getSupportFragmentManager(), "BottomSheetDialog");

        FirebaseStorage updateStorage = FirebaseStorage.getInstance(instanceLink);
        updateStorage.getReferenceFromUrl(updateLink).getFile(filePath)
                .addOnSuccessListener(taskSnapshot -> {
                    if (updateProgressFragment.getDialog() != null && updateProgressFragment.getDialog().isShowing()) {
                        updateProgressFragment.dismiss();
                    }
                    installUpdate();
                })
                .addOnFailureListener(e -> {
                    if (updateProgressFragment.getDialog() != null && updateProgressFragment.getDialog().isShowing()) {
                        updateProgressFragment.dismiss();
                    }
                    AppUtils.toastShortError(context,MainActivity.this, "Update failed");
                })
                .addOnProgressListener(taskSnapshot -> {
                    long progressValue = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    String sizeDownloaded = taskSnapshot.getBytesTransferred() / 1048576 + " MB/" + taskSnapshot.getTotalByteCount() / 1048576 + " MB";
                    if (updateProgressFragment.getDialog() != null) {
                        updateProgressFragment.progress.setText(String.format("%s%s", progressValue, "%"));
                        updateProgressFragment.sizeDownloaded.setText(sizeDownloaded);
                        updateProgressFragment.progressBar.setProgress((int) progressValue);
                    }
                });
    }

    private void downloadManagerUpdate(String downloadUrl) {
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadReference = downloadUpdate(downloadUrl);
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);

        new Thread(() -> {
            boolean downloading = true;
            while (downloading) {
                DownloadManager.Query downloadQuery = new DownloadManager.Query();
                downloadQuery.setFilterById(downloadReference);
                Cursor cursor = downloadManager.query(downloadQuery);
                cursor.moveToFirst();

                int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                if (checkStatus(downloadReference) == DownloadManager.STATUS_SUCCESSFUL) {
                    downloading = false;
                }

                long progressValue = (100L * bytesDownloaded) / bytesTotal;
                String sizeDownloaded = bytesDownloaded / 1048576 + " MB/" + bytesTotal / 1048576 + " MB";

                runOnUiThread(() -> {
                    if (updateProgressFragment.getDialog() != null) {
                        updateProgressFragment.progress.setText(String.format("%s%s", progressValue, "%"));
                        updateProgressFragment.sizeDownloaded.setText(sizeDownloaded);
                        updateProgressFragment.progressBar.setProgress((int) progressValue);
                    }
                });
                cursor.close();
            }
        }).start();
    }

    private long downloadUpdate(String downloadUrl) {
        updateProgressFragment = UpdateProgressFragment.newInstance();
        updateProgressFragment.setCancelable(false);
        updateProgressFragment.show(getSupportFragmentManager(), "BottomSheetDialog");

        Uri downloadUri = Uri.parse(downloadUrl);
        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setAllowedOverRoaming(false);
        request.setTitle(getString(R.string.app_name));
        request.setDescription(OFFICIAL_WEBSITE_URL);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);

        //Deprecated in Api level 29
        request.setVisibleInDownloadsUi(false);

        File filePath = new File(FileUtil.getPackageDataDir(context) + "/downloads/update", "app_update_" + latestVersion + ".apk");
        request.setDestinationUri(Uri.fromFile(filePath));
        return downloadManager.enqueue(request);
    }

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NotNull Intent intent) {
            //check if the broadcast message is for our Enqueued download
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadReference == referenceId) {
                if (updateProgressFragment.getDialog() != null && updateProgressFragment.getDialog().isShowing()) {
                    updateProgressFragment.dismiss();
                }
                installUpdate();
            }
        }
    };

    private int checkStatus(long downloadReferenceId) {
        DownloadManager.Query downloadQuery = new DownloadManager.Query();
        downloadQuery.setFilterById(downloadReferenceId);
        Cursor cursor = downloadManager.query(downloadQuery);
        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            return cursor.getInt(columnIndex);
        }
        return 0;
    }

    private void installUpdate() {
        File filePath = new File(FileUtil.getPackageDataDir(context) + "/downloads/update", "app_update_" + latestVersion + ".apk");
        Uri fileUri = Uri.fromFile(filePath);
        Uri contentUri = FileProvider.getUriForFile(context, "instamovies.app.in.provider", filePath);
        Uri pathUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pathUri = contentUri;
        } else {
            pathUri = fileUri;
        }
        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        openIntent.setDataAndType(pathUri, "application/vnd.android.package-archive");
        try {
            startActivity(openIntent);
        } catch (Exception exception) {
            AppUtils.toastShortError(context,MainActivity.this, "Update failed");
        }
    }

    final FullScreenContentCallback contentCallback = new FullScreenContentCallback() {
        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
            super.onAdFailedToShowFullScreenContent(adError);
            AppUtils.toastShortError(context, MainActivity.this, "Ad failed to display");
        }

        @Override
        public void onAdShowedFullScreenContent() {
            super.onAdShowedFullScreenContent();
            mRewardedAd = null;
        }
    };

    private void loadRewardedAd() {
        AdRequest rewardedAdRequest = new AdRequest.Builder().build();
        RewardedAd.load(context, getString(R.string.rewarded_ad_unit_id), rewardedAdRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                super.onAdLoaded(rewardedAd);
                mRewardedAd = rewardedAd;
                mRewardedAd.setFullScreenContentCallback(contentCallback);
                showRewardedAd();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                mRewardedAd = null;
                AppUtils.toastShortError(context, MainActivity.this, "Ad failed to load");
            }
        });
    }

    private void showRewardedAd() {
        if (mRewardedAd != null) {
            Activity activityContext = MainActivity.this;
            mRewardedAd.show(activityContext, rewardItem -> {
                int rewardAmount = rewardItem.getAmount();
                String rewardType = rewardItem.getType();
                Log.e(LOG_TAG, String.valueOf(rewardAmount));
                Log.e(LOG_TAG, rewardType);
            });
        }
    }

    private void shareApp() {
        String shareText = getString(R.string.share_message, OFFICIAL_WEBSITE_URL);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Insta Movies");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share app using"));
    }

    private void checkSettings() {
        premiumUser = appData.getBoolean("prime_purchased", false);
    }

    private String pasteText() {
        ClipboardManager clipboardManager = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if (clipboardManager.getPrimaryClip() != null) {
            return clipboardManager.getPrimaryClip().getItemAt(0).coerceToText(context).toString();
        }
        return null;
    }
}