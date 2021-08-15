package instamovies.app.in;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.content.SharedPreferences;
import com.github.florent37.fiftyshadesof.FiftyShadesOf;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
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
import java.util.List;
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
import instamovies.app.in.api.tmdb.Genres;
import instamovies.app.in.api.tmdb.MovieDetailsApi;
import instamovies.app.in.api.tmdb.MovieDetailsResponses;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MoviesActivity extends AppCompatActivity {

    private ArrayList<HashMap<String, Object>> movieList = new ArrayList<>();
    private DatabaseReference databaseReference;
    private ValueEventListener movieEventListener;
    private GridView gridView;
    private Intent webIntent = new Intent();
    private Intent videoIntent = new Intent();
    private SharedPreferences settingsPreferences;
    private SharedPreferences appData;
    private GridLayoutAnimationController animationController;
    private Context context;
    private AdView adView;
    private final String LOG_TAG = "MoviesActivity";
    private String searchText = "";
    private boolean premiumUser = false;
    private boolean systemBrowser = false;
    private boolean chromeTabs = true;
    private boolean dataSaver = false;
    private String apiKey;

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
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.move_top);
        animationController = new GridLayoutAnimationController(animation,.2f,.2f);
        String referencePath = getIntent().getStringExtra("reference_path_movie_json");
        String baseURL = getIntent().getStringExtra("base_url_movie_json");

        progressStatus = findViewById(R.id.progress_status_view);
        progressBar = progressStatus.findViewById(R.id.progressbar);
        errorView = progressStatus.findViewById(R.id.error_view);
        errorText = progressStatus.findViewById(R.id.cause_text);
        Button retryButton = progressStatus.findViewById(R.id.retry_button);

        adView = findViewById(R.id.adView);
        apiKey = getString(R.string.tmdb_api_key);
        checkSettings();

        movieEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                movieList = new ArrayList<>();
                try {
                    GenericTypeIndicator<HashMap<String, Object>> ind = new GenericTypeIndicator<HashMap<String, Object>>() {
                    };
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        HashMap<String, Object> map = data.getValue(ind);
                        movieList.add(map);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
                connectSuccess();
                gridView.setAdapter(new GridViewAdapter(movieList));
                ((BaseAdapter)gridView.getAdapter()).notifyDataSetChanged();
                gridView.setLayoutAnimation(animationController);
            }

            @Override
            public void onCancelled(@NotNull DatabaseError error) {
                Log.e(LOG_TAG, error.getMessage());
                connectError(error.getMessage());
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
                AppUtils.toastShortError(context, MoviesActivity.this, "Something wrong");
                finish();
            }
        }

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (movieList.get(position).containsKey("Premium") && !premiumUser) {
                AppUtils.toastShortDefault(context,MoviesActivity.this, "You are not a premium user.");
                return;
            }
            if (movieList.get(position).containsKey("Movie")) {
                String movieLink = Objects.requireNonNull(movieList.get(position).get("Movie")).toString();
                webIntent = new Intent();
                webIntent.setClass(context, MovieDetailsActivity.class);
                webIntent.putExtra("Movie Link", movieLink);
                startActivity(webIntent);
            }
            if (movieList.get(position).containsKey("Link")) {
                String itemLink = Objects.requireNonNull(movieList.get(position).get("Link")).toString();
                if (URLUtil.isNetworkUrl(itemLink)) {
                    webIntent = new Intent();
                    webIntent.setClass(context, HiddenWebActivity.class);
                    webIntent.putExtra("HIDDEN_URL", itemLink);
                    startActivity(webIntent);
                } else {
                    AppUtils.toastShortDefault(context,MoviesActivity.this, itemLink);
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
                        AppUtils.toastShortError(context,MoviesActivity.this, "Failed to load url");
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
                            AppUtils.toastShortError(context,MoviesActivity.this, "Failed to load url");
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
            if (movieList.get(position).containsKey("IMDb")) {
                String itemID = Objects.requireNonNull(movieList.get(position).get("IMDb")).toString();
                fetchMovieDetails(itemID);
            }
            return true;
        });

        retryButton.setOnClickListener(view -> {
            onRetry();
            // using timer to avoid multiple clicking on retry
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
                            AppUtils.toastShortError(context, MoviesActivity.this, "Something wrong");
                            finish();
                        }
                    }
                }
            }, 500);
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

                databaseReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot _dataSnapshot) {
                        movieList = new ArrayList<>();
                        try {
                            GenericTypeIndicator<HashMap<String, Object>>_ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
                            for (DataSnapshot _data:_dataSnapshot.getChildren()) {
                                HashMap<String, Object>_map = _data.getValue(_ind);
                                movieList.add(_map);
                            }
                        }
                        catch (Exception _e) {
                            _e.printStackTrace();
                        }
                        if (searchText.length()>0){
                            double mapLength = movieList.size()-1;
                            double currentLength = movieList.size();
                            for (int _repeat46 = 0; _repeat46<(int)(currentLength);_repeat46++){
                                if (!Objects.requireNonNull(movieList.get((int) mapLength).get("Name")).toString().toUpperCase().contains(searchText)) {
                                    movieList.remove((int)(mapLength));
                                }
                                mapLength--;
                            }
                        }
                        gridView.setAdapter(new GridViewAdapter(movieList));
                        ((BaseAdapter)gridView.getAdapter()).notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(LOG_TAG, databaseError.getMessage());
                    }
                });
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                searchText = s.toUpperCase();

                databaseReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot _dataSnapshot) {
                        movieList = new ArrayList<>();
                        try {
                            GenericTypeIndicator<HashMap<String, Object>>_ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
                            for (DataSnapshot _data:_dataSnapshot.getChildren()) {
                                HashMap<String, Object>_map = _data.getValue(_ind);
                                movieList.add(_map);
                            }
                        }
                        catch (Exception _e) {
                            _e.printStackTrace();
                        }
                        if (searchText.length()>0){
                            double mapLength = movieList.size()-1;
                            double currentLength = movieList.size();
                            for (int _repeat46 = 0; _repeat46<(int)(currentLength);_repeat46++){
                                if (!Objects.requireNonNull(movieList.get((int) mapLength).get("Name")).toString().toUpperCase().contains(searchText)) {
                                    movieList.remove((int)(mapLength));
                                }
                                mapLength--;
                            }
                        }
                        gridView.setAdapter(new GridViewAdapter(movieList));
                        ((BaseAdapter)gridView.getAdapter()).notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(LOG_TAG, databaseError.getMessage());
                    }
                });
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

    private void onRetry() {
        errorView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void fetchMovieDetails(String fileId) {
        MovieDetailsFragment movieDetailsFragment = MovieDetailsFragment.newInstance();
        movieDetailsFragment.setOnClickListener(v -> movieDetailsFragment.dismiss());
        movieDetailsFragment.show(getSupportFragmentManager(), "BottomSheetDialog");

        OkHttpClient okHttpClient = new OkHttpClient
                .Builder().addInterceptor(new HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY)).build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(MovieDetailsApi.JSON_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MovieDetailsApi theMovieDbApi = retrofit.create(MovieDetailsApi.class);
        Call<MovieDetailsResponses> call = theMovieDbApi.getMovie(fileId, apiKey, "en-US");
        call.enqueue(new Callback<MovieDetailsResponses>() {
            @Override
            public void onResponse(@NotNull Call<MovieDetailsResponses> call, @NotNull Response<MovieDetailsResponses> response) {
                if (!response.isSuccessful()) {
                    if (movieDetailsFragment.getDialog() != null) {
                        movieDetailsFragment.progressBar.setVisibility(View.GONE);
                        movieDetailsFragment.errorMessage.setVisibility(View.VISIBLE);
                    }
                    return;
                }
                MovieDetailsResponses dbResponses = response.body();
                if (dbResponses != null) {
                    StringBuilder genre = new StringBuilder();
                    List<Genres> genresList = dbResponses.getGenres();
                    for (int i = 0; i < genresList.size(); i++) {
                        if (i != genresList.size() - 1) {
                            genre.append(genresList.get(i).getGenre()).append(", ");
                        } else {
                            genre.append(genresList.get(i).getGenre());
                        }
                    }

                    if (movieDetailsFragment.getDialog() != null) {
                        movieDetailsFragment.progressLayout.setVisibility(View.GONE);
                        movieDetailsFragment.contentLayout.setVisibility(View.VISIBLE);
                        movieDetailsFragment.movieTitle.setText(dbResponses.getTitle());
                        movieDetailsFragment.movieGenre.setText(String.valueOf(genre));
                        movieDetailsFragment.movieYear.setText(dbResponses.getYear());
                        movieDetailsFragment.movieRating.setText(String.valueOf(dbResponses.getRating()));
                        movieDetailsFragment.movieSummary.setText(dbResponses.getOverview());
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call<MovieDetailsResponses> call, @NotNull Throwable t) {
                if (movieDetailsFragment.getDialog() != null) {
                    movieDetailsFragment.progressBar.setVisibility(View.GONE);
                    movieDetailsFragment.errorMessage.setVisibility(View.VISIBLE);
                }
            }
        });
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
                    connectError("Response unsuccessful");
                    return;
                }
                movieList = new ArrayList<>();
                ArrayList<MoviesJsonResponse> moviesJsonResponse;
                moviesJsonResponse = response.body();
                if (moviesJsonResponse != null) {
                    for (int i = 0; i < moviesJsonResponse.size(); i++){
                        HashMap<String,Object> hashMap = new HashMap<>();
                        if (moviesJsonResponse.get(i).getName() != null) {
                            hashMap.put("Name", moviesJsonResponse.get(i).getName());
                        }
                        if (moviesJsonResponse.get(i).getThumbnail() != null) {
                            hashMap.put("Thumbnail", moviesJsonResponse.get(i).getThumbnail());
                        }
                        if (moviesJsonResponse.get(i).getImdb() != null) {
                            hashMap.put("IMDb", moviesJsonResponse.get(i).getImdb());
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
                    connectSuccess();
                    gridView.setAdapter(new GridViewAdapter(movieList));
                    ((BaseAdapter)gridView.getAdapter()).notifyDataSetChanged();
                    gridView.setLayoutAnimation(animationController);
                }
            }

            @Override
            public void onFailure(@NotNull Call<ArrayList<MoviesJsonResponse>> call, @NotNull Throwable t) {
                connectError(t.getMessage());
            }
        });
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
                convertView = Objects.requireNonNull(inflater).inflate(R.layout.item_movies, parent, false);
            }

            final ImageView thumbnailImage = convertView.findViewById(R.id.thumb_image);
            final TextView movieName = convertView.findViewById(R.id.movie_name);
            // Needed for marquee scrolling
            movieName.setSelected(true);

            FiftyShadesOf.with(context).on(thumbnailImage).fadein(true).start();

            String movieTitle =  "";
            if (movieList.get(position).containsKey("Name")) {
                movieTitle = Objects.requireNonNull(movieList.get(position).get("Name")).toString().toUpperCase();
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

            if (movieList.get(position).containsKey("Thumbnail") && !dataSaver) {
                String itemLink = Objects.requireNonNull(movieList.get(position).get("Thumbnail")).toString();
                Glide.with(context).load(Uri.parse(itemLink)).into(thumbnailImage);
            } else {
                thumbnailImage.setImageResource(R.drawable.img_image_placeholder_v);
            }
            return convertView;
        }
    }
}