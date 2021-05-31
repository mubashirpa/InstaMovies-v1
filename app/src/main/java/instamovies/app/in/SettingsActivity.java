package instamovies.app.in;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.util.Objects;
import instamovies.app.in.utils.AppUtils;
import instamovies.app.in.utils.FileUtil;

public class SettingsActivity extends AppCompatActivity {

    private static SharedPreferences appSettings;
    private Context context;
    private SharedPreferences appData;
    private AdView adView;
    private boolean premiumUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        context = this;
        initializeActivity();
    }

    private void initializeActivity() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_layout, new SettingsFragment())
                .commit();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        toolbar.setNavigationOnClickListener(_v -> onBackPressed());
        MobileAds.initialize(context);
        appData = getSharedPreferences("appData", Activity.MODE_PRIVATE);
        appSettings = getSharedPreferences("appSettings", Activity.MODE_PRIVATE);
        adView = findViewById(R.id.adView);
        checkSettings();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        checkSettings();
        if (!premiumUser) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } else {
            adView.setVisibility(View.GONE);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private Preference cachePref;
        private ListPreference themePreference;
        private Intent webIntent = new Intent();
        private String WHATS_NEW_URL;
        private Context context;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey);

            context = getContext();
            Preference notificationPref = findPreference("notification");
            SwitchPreferenceCompat dataSaverSwitch = findPreference("data_saver");
            SwitchPreferenceCompat browserSwitch = findPreference("system_browser_preference");
            SwitchPreferenceCompat adBlockerSwitch = findPreference("advertisement_preference");
            cachePref = findPreference("cache");
            Preference clearUpdate = findPreference("clear_update");
            Preference versionPref = findPreference("version");
            Preference repairPref = findPreference("repair");
            Preference whatsNewPref = findPreference("whats_new");
            themePreference = findPreference("theme_preference");
            ListPreference downloadPreference = findPreference("download_preference");
            WHATS_NEW_URL = getString(R.string.whats_new_url);
            initializeCache();

            if (versionPref != null) {
                try {
                    versionPref.setSummary("Version: " + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
                } catch (android.content.pm.PackageManager.NameNotFoundException nameNotFoundException) {
                    String LOG_TAG = "SettingsFragment";
                    Log.e(LOG_TAG, nameNotFoundException.getMessage());
                }
            }

            if (downloadPreference != null) {
                if (downloadPreference.getValue() != null){
                    downloadPreference.setSummary(downloadPreference.getValue());
                } else {
                    downloadPreference.setSummary("Choose download option");
                }
            }

            if (browserSwitch != null && appSettings.getBoolean("web_maintenance", false)) {
                browserSwitch.setSummary("Currently In-App Browser experiencing problems, so keep this on");
            }

            if (notificationPref != null) {
                notificationPref.setOnPreferenceClickListener(preference -> {
                    Intent settingsIntent = new Intent();
                    settingsIntent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        settingsIntent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
                    } else {
                        settingsIntent.putExtra("app_package", context.getPackageName());
                        settingsIntent.putExtra("app_uid", context.getApplicationInfo().uid);
                    }
                    startActivity(settingsIntent);
                    return true;
                });
            }
            if (cachePref != null) {
                cachePref.setOnPreferenceClickListener(preference -> {
                    FileUtil.deleteCache(getContext());
                    AppUtils.toastShortDefault(getContext(), requireActivity(),"Cache cleared");
                    initializeCache();
                    return true;
                });
            }
            if (clearUpdate != null) {
                clearUpdate.setOnPreferenceClickListener(preference -> {
                    File filePath = new File(FileUtil.getPackageDataDir(context) + "/downloads/update");
                    if (FileUtil.deleteDirectory(filePath)) {
                        AppUtils.toastShortDefault(getContext(), requireActivity(),"Successfully cleared files");
                    }
                    return true;
                });
            }
            if (repairPref != null) {
                repairPref.setOnPreferenceClickListener(preference -> {
                    ((ActivityManager) Objects.requireNonNull(requireActivity().getSystemService(Context.ACTIVITY_SERVICE))).clearApplicationUserData();
                    return true;
                });
            }
            if (whatsNewPref != null){
                whatsNewPref.setOnPreferenceClickListener(preference -> {
                    webIntent = new Intent();
                    webIntent.setClass(requireContext(), HiddenWebActivity.class);
                    webIntent.putExtra("HIDDEN_URL", WHATS_NEW_URL);
                    startActivity(webIntent);
                    return true;
                });
            }
            if (dataSaverSwitch != null) {
                dataSaverSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                    if (newValue.equals(true)) {
                        AppUtils.toastShortDefault(getContext(), requireActivity(),"Data saver enabled");
                    }
                    return true;
                });
            }
            if (adBlockerSwitch != null) {
                adBlockerSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                    if (newValue.equals(true)) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
                        alertDialog.setMessage("AdBlocker may not work with some websites and may show lags while loading, If you have any such problems please disable the AdBlocker and send us a Feedback.");
                        alertDialog.setPositiveButton("OK", (dialogInterface, i) -> { });
                        alertDialog.setCancelable(false);
                        alertDialog.create().show();
                    }
                    return true;
                });
            }
            if (themePreference != null){
                if (themePreference.getValue() != null){
                    switch (themePreference.getValue()) {
                        case "System default":
                            themePreference.setSummary("Turn on dark theme by default");
                            break;
                        case "Set by Battery Saver":
                            themePreference.setSummary("Turn on dark theme when your device's Battery Saver is on");
                            break;
                        default:
                            themePreference.setSummary(themePreference.getValue());
                            break;
                    }
                } else {
                    themePreference.setSummary("Choose your theme");
                }

                themePreference.setOnPreferenceChangeListener(((preference, newValue) -> {
                    if (newValue.equals("System default")){
                        themePreference.setSummary("Turn on dark theme by default");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    }
                    if (newValue.equals("Set by Battery Saver")){
                        themePreference.setSummary("Turn on dark theme when your device's Battery Saver is on");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                    }
                    if (newValue.equals("Light")){
                        themePreference.setSummary("Light");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                    if (newValue.equals("Dark")){
                        themePreference.setSummary("Dark");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    }
                    return true;
                }));
            }
        }

        private void initializeCache() {
            String cacheSize;
            long size = 0;
            double fileSize;
            size += getDirSize(requireActivity().getCacheDir());
            size += getDirSize(Objects.requireNonNull(requireActivity().getExternalCacheDir()));
            if (size > 0) {
                fileSize = size >> 20;
            } else {
                fileSize = 0;
            }
            cacheSize = String.valueOf(fileSize);
            if (cachePref != null){
                cachePref.setSummary(cacheSize + " MB");
            }
        }

        long getDirSize(@NotNull File dir) {
            long size = 0;
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file != null && file.isDirectory()) {
                    size += getDirSize(file);
                } else if (file != null && file.isFile()) {
                    size += file.length();
                }
            }
            return size;
        }
    }

    private void checkSettings() {
        premiumUser = appData.getBoolean("prime_purchased", false);
    }
}