package instamovies.app.in;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.webkit.URLUtil;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import instamovies.app.in.fragments.RequestDialogFragment;
import instamovies.app.in.player.IntentUtil;
import instamovies.app.in.utils.AppUtils;

public class MovieLanguagesActivity extends AppCompatActivity {

    private Intent webIntent = new Intent();
    private AlertDialog.Builder notifyDialog;
    private SharedPreferences appData;
    private ArrayList<HashMap<String,Object>> languageList = new ArrayList<>();
    private GridView gridView;
    private Context context;
    private final int REQUEST_CODE_STORAGE = 1001;
    private Intent videoIntent = new Intent();
    private SharedPreferences settingsPreferences;
    private boolean premiumUser = false;
    private AdView adView;
    private boolean systemBrowser = false;
    private boolean chromeTabs = true;
    private boolean dataSaver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_languages);

        context = this;
        MobileAds.initialize(context);
        appData = getSharedPreferences("appData", Activity.MODE_PRIVATE);
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        adView = findViewById(R.id.adView);

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

        SharedPreferences settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
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
            AlertDialog.Builder permissionDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
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
        AlertDialog.Builder tutorialDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
        gridView = findViewById(R.id.gridView);
        FloatingActionButton fab = findViewById(R.id.fab);
        TextView scrollBottom = findViewById(R.id.scroll_bottom);
        scrollBottom.setSelected(true);
        checkSettings();

        if (!appData.getString("LanguageData","").equals("")) {
            languageList = new Gson().fromJson(appData.getString("LanguageData",""), new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
        } else {
            HashMap<String,Object> mal = new HashMap<>();
            mal.put("Title","Malayalam");
            languageList.add(mal);
            HashMap<String,Object> tam = new HashMap<>();
            tam.put("Title","Tamil");
            languageList.add(tam);
            HashMap<String,Object> eng = new HashMap<>();
            eng.put("Title","English");
            languageList.add(eng);
            HashMap<String,Object> onl = new HashMap<>();
            onl.put("Title","Hindi");
            languageList.add(onl);
            HashMap<String,Object> hin = new HashMap<>();
            hin.put("Title","Loading");
            languageList.add(hin);
            HashMap<String,Object> ser = new HashMap<>();
            ser.put("Title","Loading");
            languageList.add(ser);
        }
        gridView.setAdapter(new GridViewAdapter(languageList));
        getLanguages();

        fab.setOnClickListener(_view -> {
            RequestDialogFragment requestDialogFragment = RequestDialogFragment.newInstance();
            requestDialogFragment.show(getSupportFragmentManager(), "BottomSheetDialog");
        });

        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        DocumentReference langReference = firebaseFirestore.collection("Languages").document("Notify");
        langReference.addSnapshotListener((documentSnapshot, e) -> {
            if (documentSnapshot != null && documentSnapshot.exists()) {
                String Header = Objects.requireNonNull(documentSnapshot.get("Header")).toString();
                String Message = Objects.requireNonNull(documentSnapshot.get("Message")).toString();

                notifyDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
                View dialogView = getLayoutInflater().inflate(R.layout.layout_checkbox, null);
                CheckBox checkBox = dialogView.findViewById(R.id.checkBox);
                notifyDialog.setTitle(Header);
                notifyDialog.setMessage("\n" + Message);
                notifyDialog.setView(dialogView);
                notifyDialog.setPositiveButton("OK", (_dialog, _which) -> _dialog.dismiss());
                notifyDialog.setCancelable(false);
                if (!Message.equals(appData.getString("message_language_activity", "")) && !isDestroyed()) {
                    notifyDialog.create().show();
                }
                checkBox.setOnCheckedChangeListener((compoundButton, b) -> {
                    if(compoundButton.isChecked()){
                        appData.edit().putString("message_language_activity", Message).apply();
                    }else{
                        appData.edit().putString("message_language_activity", "").apply();
                    }
                });
            }
        });

        View dialogView = getLayoutInflater().inflate(R.layout.layout_checkbox, null);
        CheckBox checkBox = dialogView.findViewById(R.id.checkBox);
        tutorialDialog.setMessage("Do you want to watch the tutorial videos for how to download movies and more");
        tutorialDialog.setView(dialogView);
        tutorialDialog.setPositiveButton("WATCH", (_dialog, _which) -> {
            webIntent = new Intent();
            webIntent.setClass(context, HiddenWebActivity.class);
            webIntent.putExtra("HIDDEN_URL", getString(R.string.tutorial_url));
            startActivity(webIntent);
        });
        tutorialDialog.setNegativeButton("LATER", (_dialog, _which) -> _dialog.dismiss());
        tutorialDialog.setCancelable(false);
        if (!appData.getBoolean("show_tutorial_language_activity", false)){
            tutorialDialog.create().show();
        }
        checkBox.setOnCheckedChangeListener((compoundButton, b) -> {
            if(compoundButton.isChecked()){
                appData.edit().putBoolean("show_tutorial_language_activity", true).apply();
            }else{
                appData.edit().putBoolean("show_tutorial_language_activity", false).apply();
            }
        });

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (languageList.get(position).containsKey("Premium") && !premiumUser) {
                AppUtils.toastShortDefault(context,MovieLanguagesActivity.this, "You are not a premium user.");
                return;
            }
            if (languageList.get(position).containsKey("Instance Link")) {
                String instanceLink = Objects.requireNonNull(languageList.get(position).get("Instance Link")).toString();
                String title = Objects.requireNonNull(languageList.get(position).get("Title")).toString();
                webIntent = new Intent();
                webIntent.setClass(context, MoviesActivity.class);
                webIntent.putExtra("Instance Link", instanceLink);
                webIntent.putExtra("Title", title);
                startActivity(webIntent);
            }
            if (languageList.get(position).containsKey("Movie")) {
                String movieLink = Objects.requireNonNull(languageList.get(position).get("Movie")).toString();
                webIntent = new Intent();
                webIntent.setClass(context, MovieDetailsActivity.class);
                webIntent.putExtra("Movie Link", movieLink);
                startActivity(webIntent);
            }
            if (languageList.get(position).containsKey("Link")) {
                String itemLink = Objects.requireNonNull(languageList.get(position).get("Link")).toString();
                if (URLUtil.isNetworkUrl(itemLink)) {
                    webIntent = new Intent();
                    webIntent.setClass(context, HiddenWebActivity.class);
                    webIntent.putExtra("HIDDEN_URL", itemLink);
                    startActivity(webIntent);
                } else {
                    AppUtils.toastShortDefault(context,MovieLanguagesActivity.this, itemLink);
                }
            }
            if (languageList.get(position).containsKey("Link1")) {
                String itemLink = Objects.requireNonNull(languageList.get(position).get("Link1")).toString();
                webIntent = new Intent();
                webIntent.setClass(context, WebActivity.class);
                webIntent.putExtra("WEB_URL", itemLink);
                startActivity(webIntent);
            }
            if (languageList.get(position).containsKey("Link2")) {
                String itemLink = Objects.requireNonNull(languageList.get(position).get("Link2")).toString();
                if (chromeTabs) {
                    CustomTabsIntent.Builder customTabsBuilder = new CustomTabsIntent.Builder();
                    CustomTabsIntent customTabsIntent = customTabsBuilder.build();
                    customTabsIntent.launchUrl(context, Uri.parse(itemLink));
                } else {
                    try {
                        webIntent = new Intent();
                        webIntent.setAction(Intent.ACTION_VIEW);
                        webIntent.setData(Uri.parse(itemLink));
                        startActivity(webIntent);
                    } catch (android.content.ActivityNotFoundException notFoundException){
                        AppUtils.toastShortError(context,MovieLanguagesActivity.this, "Failed to load url");
                    }
                }
            }
            if (languageList.get(position).containsKey("Link3")) {
                String itemLink = Objects.requireNonNull(languageList.get(position).get("Link3")).toString();
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
                            startActivity(webIntent);
                        } catch (android.content.ActivityNotFoundException notFoundException){
                            AppUtils.toastShortError(context,MovieLanguagesActivity.this, "Failed to load url");
                        }
                    }
                } else {
                    webIntent = new Intent();
                    webIntent.setClass(context, WebActivity.class);
                    webIntent.putExtra("WEB_URL", itemLink);
                    startActivity(webIntent);
                }
            }
            if (languageList.get(position).containsKey("Video")) {
                String itemLink = Objects.requireNonNull(languageList.get(position).get("Video")).toString();
                if (itemLink.contains("https://youtu.be/")) {
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
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkSettings();
        if (!premiumUser) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } else {
            adView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String @NotNull [] permissions, int @NotNull [] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if (requestCode == REQUEST_CODE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                AppUtils.toastShortError(context,MovieLanguagesActivity.this,"Failed to get permission. Give us permission manually");
                finish();
            } else {
                initializeActivity();
            }
        }
    }

    private void getLanguages(){
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = firebaseDatabase.getReference("Languages");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                languageList = new ArrayList<>();
                try {
                    GenericTypeIndicator<HashMap<String, Object>> ind = new GenericTypeIndicator<HashMap<String, Object>>() {
                    };
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        HashMap<String, Object> map = data.getValue(ind);
                        languageList.add(map);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                appData.edit().putString("LanguageData",new Gson().toJson(languageList)).apply();
                gridView.setAdapter(new GridViewAdapter(languageList));
                ((BaseAdapter)gridView.getAdapter()).notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NotNull DatabaseError error) {
                AppUtils.toastShortError(context,MovieLanguagesActivity.this,error.getMessage());
            }
        });
    }

    private void checkSettings() {
        systemBrowser = settingsPreferences.getBoolean("system_browser_preference", false);
        chromeTabs = settingsPreferences.getBoolean("chrome_tabs_preference", true);
        dataSaver = settingsPreferences.getBoolean("data_saver", false);
        premiumUser = appData.getBoolean("prime_purchased", false);
    }

    public class GridViewAdapter extends BaseAdapter {

        final ArrayList<HashMap<String, Object>> data;

        GridViewAdapter(ArrayList<HashMap<String, Object>> arr) {
            data = arr;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public HashMap<String, Object> getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                convertView = Objects.requireNonNull(inflater).inflate(R.layout.item_languages, parent, false);
            }

            final TextView title = convertView.findViewById(R.id.title);
            final ImageView backdrop = convertView.findViewById(R.id.backdrop);

            if (languageList.get(position).containsKey("Title")) {
                title.setText(Objects.requireNonNull(languageList.get(position).get("Title")).toString().toUpperCase());
            }
            if (languageList.get(position).containsKey("Backdrop") && !dataSaver) {
                String backdropPath = Objects.requireNonNull(languageList.get(position).get("Backdrop")).toString();
                Glide.with(context).load(backdropPath).error(R.drawable.img_backdrop_languages).into(backdrop);
            } else {
                backdrop.setImageResource(R.drawable.img_backdrop_languages);
            }
            return convertView;
        }
    }
}