package instamovies.app.in;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.webkit.URLUtil;
import android.widget.*;
import android.content.*;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import android.net.Uri;
import instamovies.app.in.utils.AppUtils;

public class MoreActivity extends AppCompatActivity {

    private ArrayList<HashMap<String, Object>> moreList = new ArrayList<>();
    private ListView listView;
    private Intent webIntent = new Intent();
    private final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private final DatabaseReference databaseReference = firebaseDatabase.getReference("More");
    private ProgressBar progressBar;
    private SharedPreferences appData;
    private Context context;
    private AdView adView;
    private SharedPreferences settingsPreferences;
    private boolean premiumUser = false;
    private boolean chromeTabs = true;
    private final String LOG_TAG = "MoreActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);
        context = this;
        MobileAds.initialize(context);
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
        listView = findViewById(R.id.listView);
        progressBar = findViewById(R.id.progressbar);
        appData = getSharedPreferences("appData", Activity.MODE_PRIVATE);
        adView = findViewById(R.id.adView);
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        checkSettings();
        listView.setSelector(android.R.color.transparent);

        if (!appData.getString("list_more_activity","").equals("")) {
            moreList = new Gson().fromJson(appData.getString("list_more_activity",""), new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
            listView.setAdapter(new ListViewAdapter(moreList));
        }

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                moreList = new ArrayList<>();
                try {
                    GenericTypeIndicator<HashMap<String, Object>> ind = new GenericTypeIndicator<HashMap<String, Object>>() {
                    };
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        HashMap<String, Object> map = data.getValue(ind);
                        moreList.add(map);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                appData.edit().putString("list_more_activity",new Gson().toJson(moreList)).apply();
                listView.setAdapter(new ListViewAdapter(moreList));
                ((BaseAdapter)listView.getAdapter()).notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(LOG_TAG, databaseError.getMessage());
                progressBar.setVisibility(View.GONE);
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (moreList.get(position).containsKey("Link")) {
                String itemLink = Objects.requireNonNull(moreList.get(position).get("Link")).toString();
                if (URLUtil.isNetworkUrl(itemLink)) {
                    webIntent = new Intent();
                    webIntent.setClass(context, HiddenWebActivity.class);
                    webIntent.putExtra("HIDDEN_URL", itemLink);
                    startActivity(webIntent);
                } else {
                    AppUtils.toast(context, MoreActivity.this, itemLink);
                }
            }
            if (moreList.get(position).containsKey("Link1")) {
                String itemLink = Objects.requireNonNull(moreList.get(position).get("Link1")).toString();
                webIntent = new Intent();
                webIntent.setClass(context, WebActivity.class);
                webIntent.putExtra("WEB_URL", itemLink);
                startActivity(webIntent);
            }
            if (moreList.get(position).containsKey("Link2")) {
                String itemLink = Objects.requireNonNull(moreList.get(position).get("Link2")).toString();
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
                        AppUtils.toastError(context,MoreActivity.this, getString(R.string.error_activity_not_found));
                    }
                }
            }
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
        if (!premiumUser) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } else {
            adView.setVisibility(View.GONE);
        }
    }

    private void checkSettings() {
        chromeTabs = settingsPreferences.getBoolean("chrome_tabs_preference", true);
        premiumUser = appData.getBoolean("prime_purchased", false);
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
                convertView = Objects.requireNonNull(inflater).inflate(R.layout.item_more, parent, false);
            }

            final TextView headerText = convertView.findViewById(R.id.header_text);

            if (moreList.get(position).containsKey("Title")) {
                headerText.setText(Objects.requireNonNull(moreList.get(position).get("Title")).toString());
            }
            return convertView;
        }
    }
}