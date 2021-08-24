package instamovies.app.in;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.webkit.URLUtil;
import android.widget.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Objects;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import android.content.Intent;

import instamovies.app.in.utils.AppUtils;

public class NotificationsActivity extends AppCompatActivity {

    private ArrayList<HashMap<String, Object>> notificationList = new ArrayList<>();
    private SharedPreferences appData;
    private Intent webIntent = new Intent();
    private Context context;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference databaseReference = database.getReference("Notifications");
    private final String LOG_TAG = "NotificationActivity";
    private boolean premiumUser = false;
    private boolean chromeTabs = true;
    private SharedPreferences settingsPreferences;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        context = this;
        initializeActivity();
    }

    private void initializeActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        toolbar.setNavigationOnClickListener(_v -> onBackPressed());
        MobileAds.initialize(context);
        ListView listView = findViewById(R.id.listView);
        appData = getSharedPreferences("appData", Activity.MODE_PRIVATE);
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        progressBar = findViewById(R.id.progressbar);
        checkSettings();

        if (!appData.getString("list_notification","").equals("")) {
            notificationList = new Gson().fromJson(appData.getString("list_notification",""), new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
            listView.setAdapter(new ListViewAdapter(notificationList));
        }

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                notificationList = new ArrayList<>();
                try {
                    GenericTypeIndicator<HashMap<String, Object>> ind = new GenericTypeIndicator<HashMap<String, Object>>() {
                    };
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        HashMap<String, Object> map = data.getValue(ind);
                        notificationList.add(map);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                appData.edit().putString("list_notification", new Gson().toJson(notificationList)).apply();
                listView.setAdapter(new ListViewAdapter(notificationList));
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(LOG_TAG, databaseError.getMessage());
                progressBar.setVisibility(View.GONE);
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (notificationList.get(position).containsKey("Premium") && !premiumUser) {
                AppUtils.toastShortDefault(context,NotificationsActivity.this, "You are not a premium user.");
                return;
            }
            if (notificationList.get(position).containsKey("base_url") && notificationList.get(position).containsKey("reference_path")) {
                String baseURL = Objects.requireNonNull(notificationList.get(position).get("base_url")).toString();
                String referencePath = Objects.requireNonNull(notificationList.get(position).get("reference_path")).toString();
                String title = getString(R.string.app_name);
                if (notificationList.get(position).containsKey("title")) {
                    title = Objects.requireNonNull(notificationList.get(position).get("title")).toString();
                }
                webIntent = new Intent();
                webIntent.setClass(context, MoviesActivity.class);
                webIntent.putExtra("base_url_movie_json", baseURL);
                webIntent.putExtra("reference_path_movie_json", referencePath);
                webIntent.putExtra("title_movie_act", title);
                startActivity(webIntent);
            }
            if (notificationList.get(position).containsKey("Movie")) {
                String movieLink = Objects.requireNonNull(notificationList.get(position).get("Movie")).toString();
                webIntent = new Intent();
                webIntent.setClass(context, MovieDetailsActivity.class);
                webIntent.putExtra("Movie Link", movieLink);
                startActivity(webIntent);
                return;
            }
            if (notificationList.get(position).containsKey("Link")) {
                String itemLink = Objects.requireNonNull(notificationList.get(position).get("Link")).toString();
                if (URLUtil.isNetworkUrl(itemLink)) {
                    webIntent = new Intent();
                    webIntent.setClass(context, HiddenWebActivity.class);
                    webIntent.putExtra("HIDDEN_URL", itemLink);
                    startActivity(webIntent);
                } else {
                    AppUtils.toastShortError(context, NotificationsActivity.this, itemLink);
                }
                return;
            }
            if (notificationList.get(position).containsKey("Link1")) {
                String itemLink = Objects.requireNonNull(notificationList.get(position).get("Link1")).toString();
                webIntent = new Intent();
                webIntent.setClass(context, WebActivity.class);
                webIntent.putExtra("WEB_URL", itemLink);
                startActivity(webIntent);
                return;
            }
            if (notificationList.get(position).containsKey("Link2")) {
                String itemLink = Objects.requireNonNull(notificationList.get(position).get("Link2")).toString();
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
                        AppUtils.toastShortError(context,NotificationsActivity.this, "Failed to load url");
                    }
                }
                return;
            }

            //if none of the above available closes the activity
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        checkSettings();
    }

    private void checkSettings() {
        premiumUser = appData.getBoolean("prime_purchased", false);
        chromeTabs = settingsPreferences.getBoolean("chrome_tabs_preference", true);
    }

    public class ListViewAdapter extends BaseAdapter {

        final ArrayList<HashMap<String, Object>> data;

        ListViewAdapter(ArrayList<HashMap<String, Object>> arr) {
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
            LayoutInflater inflater = (LayoutInflater)getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_notifications, parent, false);
            }
            final TextView text_subject = convertView.findViewById(R.id.text_subject);
            final TextView text_time = convertView.findViewById(R.id.text_time);
            final AdView adView = convertView.findViewById(R.id.adView);

            if (!premiumUser && position % 2 == 1) {
                adView.setVisibility(View.VISIBLE);
                AdRequest adRequest = new AdRequest.Builder().build();
                adView.loadAd(adRequest);
            } else {
                adView.setVisibility(View.GONE);
            }

            if (notificationList.get(position).containsKey("Subject")) {
                text_subject.setText(Objects.requireNonNull(notificationList.get(position).get("Subject")).toString());
            }
            if (notificationList.get(position).containsKey("Time")) {
                text_time.setText(Objects.requireNonNull(notificationList.get(position).get("Time")).toString());
            }
            return convertView;
        }
    }
}