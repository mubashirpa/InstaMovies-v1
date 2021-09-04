package instamovies.app.in;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.content.SharedPreferences;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.GridLayoutAnimationController;
import android.webkit.URLUtil;
import android.widget.*;
import java.util.HashMap;
import android.content.Intent;
import android.net.Uri;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.GenericTypeIndicator;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import androidx.appcompat.widget.SearchView;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.preference.PreferenceManager;
import instamovies.app.in.api.movies.MoviesJsonApi;
import instamovies.app.in.api.movies.MoviesJsonResponse;
import instamovies.app.in.fragments.MovieDetailsFragment;
import instamovies.app.in.fragments.RequestDialogFragment;
import instamovies.app.in.player.IntentUtil;
import instamovies.app.in.utils.AppUtils;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MoviesActivity extends AppCompatActivity {

    private ArrayList<HashMap<String, Object>> movieList = new ArrayList<>();
    private final ArrayList<HashMap<String, Object>> subMovieList = new ArrayList<>();
    private DatabaseReference databaseReference;
    private ValueEventListener movieEventListener;
    private GridView gridView;
    private Intent webIntent = new Intent();
    private Intent videoIntent = new Intent();
    private SharedPreferences settingsPreferences;
    private SharedPreferences appSettings;
    private SharedPreferences appData;
    private GridLayoutAnimationController animationController;
    private Context context;
    private AdView adView;
    private String searchText = "";
    private boolean premiumUser = false;
    private boolean systemBrowser = false;
    private boolean chromeTabs = true;
    private boolean dataSaver = false;
    private TextView noResultsText;
    private View progressStatus;
    private ProgressBar progressBar;
    private LinearLayout errorView;
    private TextView errorText;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movies);
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
        gridView = findViewById(R.id.gridView);
        gridView.setSelector(android.R.color.transparent);
        appData = getSharedPreferences("appData", Activity.MODE_PRIVATE);
        appSettings = context.getSharedPreferences("appSettings", Activity.MODE_PRIVATE);
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.move_top);
        animationController = new GridLayoutAnimationController(animation,.2f,.2f);
        String referencePath = getIntent().getStringExtra("reference_path_movie_json");
        String baseURL = getIntent().getStringExtra("base_url_movie_json");
        setTitle(getIntent().getStringExtra("title_movie_act"));
        noResultsText = findViewById(R.id.no_results_text);
        progressStatus = findViewById(R.id.progress_status_view);
        progressBar = progressStatus.findViewById(R.id.progressbar);
        errorView = progressStatus.findViewById(R.id.error_view);
        errorText = progressStatus.findViewById(R.id.cause_text);
        Button retryButton = progressStatus.findViewById(R.id.retry_button);
        adView = findViewById(R.id.adView);
        checkSettings();

        movieEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                movieList = new ArrayList<>();
                GenericTypeIndicator<HashMap<String, Object>> ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    HashMap<String, Object> map = data.getValue(ind);
                    movieList.add(map);
                }
                // This substitute array list is for search option
                subMovieList.addAll(movieList);
                gridView.setAdapter(new GridViewAdapter(movieList));
                ((BaseAdapter)gridView.getAdapter()).notifyDataSetChanged();
                gridView.setLayoutAnimation(animationController);
                connectSuccess();
            }

            @Override
            public void onCancelled(@NotNull DatabaseError error) {
                connectError(getString(R.string.error_fdb_on_cancelled));
            }
        };
        progressStatus.setVisibility(View.VISIBLE);
        if (baseURL.contains("firebaseio.com")) {
            FirebaseDatabase database = FirebaseDatabase.getInstance(baseURL);
            databaseReference = database.getReference(referencePath);
            databaseReference.addValueEventListener(movieEventListener);
        } else {
            if (URLUtil.isNetworkUrl(baseURL) && baseURL.endsWith("/")) {
                loadMoviesFromJson(baseURL, referencePath);
            } else {
                AppUtils.toastError(context, MoviesActivity.this, getString(R.string.error_default));
            }
        }

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (movieList.get(position).containsKey("Premium") && !premiumUser) {
                AppUtils.toast(context,MoviesActivity.this, "You are not a premium user.");
                return;
            }
            if (movieList.get(position).containsKey("Movie")) {
                String movieLink = Objects.requireNonNull(movieList.get(position).get("Movie")).toString();
                if (movieList.get(position).containsKey("movie_id")) {
                    String movieID = Objects.requireNonNull(movieList.get(position).get("movie_id")).toString();
                    webIntent = new Intent();
                    if (appSettings.getBoolean("details_activity", true)) {
                        webIntent.setClass(context, MovieDetailsActivity.class);
                        webIntent.putExtra("movie_details_url", movieLink);
                        webIntent.putExtra("movie_id", movieID);
                    } else {
                        webIntent.setClass(context, HiddenWebActivity.class);
                        webIntent.putExtra("HIDDEN_URL", movieLink);
                    }
                    startActivity(webIntent);
                }
            }
            if (movieList.get(position).containsKey("Link")) {
                String itemLink = Objects.requireNonNull(movieList.get(position).get("Link")).toString();
                if (URLUtil.isNetworkUrl(itemLink)) {
                    webIntent = new Intent();
                    webIntent.setClass(context, HiddenWebActivity.class);
                    webIntent.putExtra("HIDDEN_URL", itemLink);
                    startActivity(webIntent);
                } else {
                    AppUtils.toast(context,MoviesActivity.this, itemLink);
                }
            }
            if (movieList.get(position).containsKey("Link1")) {
                String itemLink = Objects.requireNonNull(movieList.get(position).get("Link1")).toString();
                webIntent = new Intent();
                webIntent.setClass(context, WebActivity.class);
                webIntent.putExtra("WEB_URL", itemLink);
                startActivity(webIntent);
            }
            if (movieList.get(position).containsKey("Link2")) {
                String itemLink = Objects.requireNonNull(movieList.get(position).get("Link2")).toString();
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
                        AppUtils.toastError(context, MoviesActivity.this, getString(R.string.error_activity_not_found));
                    }
                }
            }
            if (movieList.get(position).containsKey("Link3")) {
                String itemLink = Objects.requireNonNull(movieList.get(position).get("Link3")).toString();
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
                            AppUtils.toastError(context, MoviesActivity.this, getString(R.string.error_activity_not_found));
                        }
                    }
                } else {
                    webIntent = new Intent();
                    webIntent.setClass(context, WebActivity.class);
                    webIntent.putExtra("WEB_URL", itemLink);
                    startActivity(webIntent);
                }
            }
            if (movieList.get(position).containsKey("Video")) {
                String itemLink = Objects.requireNonNull(movieList.get(position).get("Video")).toString();
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

        gridView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (movieList.get(position).containsKey("movie_id")) {
                String fileID = Objects.requireNonNull(movieList.get(position).get("movie_id")).toString();
                MovieDetailsFragment movieDetailsFragment = MovieDetailsFragment.newInstance();
                movieDetailsFragment.setFileID(fileID);
                movieDetailsFragment.show(getSupportFragmentManager(), "BottomSheetDialog");
            }
            return true;
        });

        retryButton.setOnClickListener(view -> {
            errorView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            // Using timer to avoid multiple clicking on retry
            handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                if (!isDestroyed()) {
                    if (baseURL.contains("firebaseio.com")) {
                        FirebaseDatabase database = FirebaseDatabase.getInstance(baseURL);
                        databaseReference = database.getReference(referencePath);
                        databaseReference.addValueEventListener(movieEventListener);
                    } else {
                        if (URLUtil.isNetworkUrl(baseURL) && baseURL.endsWith("/")) {
                            loadMoviesFromJson(baseURL, referencePath);
                        } else {
                            AppUtils.toastError(context, MoviesActivity.this, getString(R.string.error_default));
                        }
                    }
                }
            }, getResources().getInteger(R.integer.retry_button_wait_time_default));
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
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_movies_default, menu);

        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQueryHint("Search Movies");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                searchText = s.toUpperCase();
                movieList = new ArrayList<>();
                movieList.addAll(subMovieList);
                if (searchText.length() > 0) {
                    int position = movieList.size() - 1;
                    int size = movieList.size();
                    for (int i = 0; i < size; i++) {
                        if (!Objects.requireNonNull(movieList.get(position).get("title")).toString().toUpperCase().contains(searchText)) {
                            movieList.remove(position);
                        }
                        position--;
                    }
                }
                gridView.setAdapter(new GridViewAdapter(movieList));
                ((BaseAdapter)gridView.getAdapter()).notifyDataSetChanged();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                searchText = s.toUpperCase();
                movieList = new ArrayList<>();
                movieList.addAll(subMovieList);
                if (searchText.length() > 0) {
                    int position = movieList.size() - 1;
                    int size = movieList.size();
                    for (int i = 0; i < size; i++) {
                        if (!Objects.requireNonNull(movieList.get(position).get("title")).toString().toUpperCase().contains(searchText)) {
                            movieList.remove(position);
                        }
                        position--;
                    }
                }
                gridView.setAdapter(new GridViewAdapter(movieList));
                ((BaseAdapter)gridView.getAdapter()).notifyDataSetChanged();
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull final MenuItem item){
        switch (item.getTitle().toString()){
            case "Tutorials":
                Intent webIntent = new Intent();
                webIntent.setClass(context, HiddenWebActivity.class);
                webIntent.putExtra("HIDDEN_URL", getString(R.string.tutorial_url));
                startActivity(webIntent);
                return true;
            case "Request":
                RequestDialogFragment requestDialogFragment = RequestDialogFragment.newInstance();
                requestDialogFragment.setActivity(MoviesActivity.this);
                requestDialogFragment.show(getSupportFragmentManager(), "BottomSheetDialog");
                return true;
            case "Settings":
                Intent settingsIntent = new Intent();
                settingsIntent.setClass(context, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void checkSettings() {
        systemBrowser = settingsPreferences.getBoolean("system_browser_preference", false);
        chromeTabs = settingsPreferences.getBoolean("chrome_tabs_preference", true);
        dataSaver = settingsPreferences.getBoolean("data_saver", false);
        premiumUser = appData.getBoolean("prime_purchased", false);
    }

    private void connectSuccess() {
        progressStatus.setVisibility(View.GONE);
    }

    private void connectError(String error) {
        progressStatus.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        errorText.setText(error);
        errorView.setVisibility(View.VISIBLE);
    }

    private void loadMoviesFromJson(String url, String path) {
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder().addInterceptor(new HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY)).build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MoviesJsonApi moviesJsonApi = retrofit.create(MoviesJsonApi.class);
        Call<ArrayList<MoviesJsonResponse>> call = moviesJsonApi.getMovies(path);
        call.enqueue(new Callback<ArrayList<MoviesJsonResponse>>() {
            @Override
            public void onResponse(@NotNull Call<ArrayList<MoviesJsonResponse>> call, @NotNull Response<ArrayList<MoviesJsonResponse>> response) {
                if (!response.isSuccessful()) {
                    connectError(getString(R.string.error_retrofit_response));
                    return;
                }
                movieList = new ArrayList<>();
                ArrayList<MoviesJsonResponse> moviesJsonResponse;
                moviesJsonResponse = response.body();
                if (moviesJsonResponse != null) {
                    for (int i = 0; i < moviesJsonResponse.size(); i++){
                        HashMap<String,Object> hashMap = new HashMap<>();
                        if (moviesJsonResponse.get(i).getTitle() != null) {
                            hashMap.put("title", moviesJsonResponse.get(i).getTitle());
                        }
                        if (moviesJsonResponse.get(i).getPoster() != null) {
                            hashMap.put("poster", moviesJsonResponse.get(i).getPoster());
                        }
                        if (moviesJsonResponse.get(i).getId() != null) {
                            hashMap.put("movie_id", moviesJsonResponse.get(i).getId());
                        }
                        if (moviesJsonResponse.get(i).getMovie() != null) {
                            hashMap.put("Movie", moviesJsonResponse.get(i).getMovie());
                        }
                        if (moviesJsonResponse.get(i).getLink() != null) {
                            hashMap.put("Link", moviesJsonResponse.get(i).getLink());
                        }
                        if (moviesJsonResponse.get(i).getLink1() != null) {
                            hashMap.put("Link1", moviesJsonResponse.get(i).getLink1());
                        }
                        if (moviesJsonResponse.get(i).getLink2() != null) {
                            hashMap.put("Link2", moviesJsonResponse.get(i).getLink2());
                        }
                        if (moviesJsonResponse.get(i).getLink3() != null) {
                            hashMap.put("Link3", moviesJsonResponse.get(i).getLink3());
                        }
                        if (moviesJsonResponse.get(i).getVideo() != null) {
                            hashMap.put("Video", moviesJsonResponse.get(i).getVideo());
                        }
                        movieList.add(hashMap);
                    }
                    // This substitute array list is for search option
                    subMovieList.addAll(movieList);
                    gridView.setAdapter(new GridViewAdapter(movieList));
                    ((BaseAdapter)gridView.getAdapter()).notifyDataSetChanged();
                    gridView.setLayoutAnimation(animationController);
                    connectSuccess();
                }
            }

            @Override
            public void onFailure(@NotNull Call<ArrayList<MoviesJsonResponse>> call, @NotNull Throwable t) {
                connectError(getString(R.string.error_retrofit_enqueue_failed));
            }
        });
    }

    public class GridViewAdapter extends BaseAdapter {

        final ArrayList<HashMap<String, Object>> data;

        GridViewAdapter(ArrayList<HashMap<String, Object>> arr) {
            data = arr;
            if (data.size() == 0) {
                gridView.setVisibility(View.GONE);
                noResultsText.setVisibility(View.VISIBLE);
            } else {
                noResultsText.setVisibility(View.GONE);
                gridView.setVisibility(View.VISIBLE);
            }
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
                convertView = Objects.requireNonNull(inflater).inflate(R.layout.item_movies, parent, false);
            }

            final ImageView posterFrame = convertView.findViewById(R.id.thumb_image);
            final TextView movieName = convertView.findViewById(R.id.movie_name);
            // Needed for marquee scrolling
            movieName.setSelected(true);

            String movieTitle =  "";
            if (data.get(position).containsKey("title")) {
                movieTitle = Objects.requireNonNull(data.get(position).get("title")).toString().toUpperCase();
            }
            Spannable spannable;
            android.text.style.ForegroundColorSpan foregroundColorSpan = new android.text.style.ForegroundColorSpan(Color.RED);
            android.text.style.BackgroundColorSpan backgroundColorSpan = new android.text.style.BackgroundColorSpan(Color.YELLOW);
            double Index;
            spannable = new SpannableString(movieTitle);
            if (movieTitle.contains(searchText)) {
                Index = movieTitle.indexOf(searchText);
                spannable.setSpan(android.text.style.CharacterStyle.wrap(foregroundColorSpan),(int)Index, (int)Index+searchText.length(),0);
                spannable.setSpan(android.text.style.CharacterStyle.wrap(backgroundColorSpan),(int)Index, (int)Index+searchText.length(),0);
            }
            movieName.setText(spannable);

            if (data.get(position).containsKey("poster") && !dataSaver) {
                String itemLink = Objects.requireNonNull(data.get(position).get("poster")).toString();
                Glide.with(context).load(Uri.parse(itemLink)).into(posterFrame);
            } else {
                posterFrame.setImageResource(R.drawable.img_image_placeholder_v);
            }
            return convertView;
        }
    }
}