package instamovies.app.in;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import instamovies.app.in.api.tmdb.Genres;
import instamovies.app.in.api.tmdb.MovieDetailsApi;
import instamovies.app.in.api.tmdb.ProductionCountries;
import instamovies.app.in.api.tmdb.MovieDetailsResponses;
import instamovies.app.in.api.tmdb.credits.Cast;
import instamovies.app.in.api.tmdb.credits.CreditsApi;
import instamovies.app.in.api.tmdb.credits.CreditsResponses;
import instamovies.app.in.fragments.DownloadOptionsFragment;
import instamovies.app.in.player.IntentUtil;
import instamovies.app.in.utils.AppUtils;
import instamovies.app.in.utils.RecyclerDecorationHorizontal;
import instamovies.app.in.adapters.ScreenshotAdapter;
import instamovies.app.in.models.ScreenshotModel;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MovieDetailsActivity extends AppCompatActivity {

    private ImageView moviePoster;
    private TextView movieTitle, movieRating, movieGenre, movieDuration, movieYear, movieDurationMinutes, movieCertificate, adultWarning, movieSummary, movieCasts;
    private RatingBar ratingBar;
    private LinearLayout screenshotsLayout;
    private RecyclerView screenshotRecycler;
    private Context context;
    private String trailerURL;
    private Intent videoIntent = new Intent();
    private Intent webIntent = new Intent();
    private JSONArray downloadsArray;
    private DownloadOptionsFragment downloadOptionsFragment;
    private String downloadURL;
    private boolean systemBrowser = false;
    private boolean chromeTabs = true;
    private LinearLayout progressStatus;
    private ProgressBar progressBar;
    private LinearLayout errorView;
    private TextView errorCauseText;
    private Handler handler;
    private boolean tvShow = false;
    private String mediaType = "movie";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);

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

    private void initializeActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        toolbar.setNavigationOnClickListener(_v -> onBackPressed());
        View toolbarDivider = findViewById(R.id.toolbar_divider);
        ScrollView scrollView = findViewById(R.id.scrollView);
        moviePoster = findViewById(R.id.poster);
        movieTitle = findViewById(R.id.title);
        ratingBar = findViewById(R.id.rating_bar);
        movieRating = findViewById(R.id.rating);
        movieGenre = findViewById(R.id.genre);
        movieDuration = findViewById(R.id.duration);
        movieYear = findViewById(R.id.year);
        movieDurationMinutes = findViewById(R.id.duration_minutes);
        movieCertificate = findViewById(R.id.certificate);
        adultWarning = findViewById(R.id.warning_adult);
        Button trailerButton = findViewById(R.id.trailer_button);
        Button downloadButton = findViewById(R.id.download_button);
        movieSummary = findViewById(R.id.summary);
        screenshotsLayout = findViewById(R.id.screenshots_layout);
        screenshotRecycler = findViewById(R.id.screenshots_recycler);
        movieCasts = findViewById(R.id.casts);
        progressStatus = findViewById(R.id.progress_status_view);
        progressBar = progressStatus.findViewById(R.id.progressbar);
        errorView = progressStatus.findViewById(R.id.error_view);
        errorCauseText = progressStatus.findViewById(R.id.cause_text);
        Button retryButton = progressStatus.findViewById(R.id.retry_button);
        String movieID = getIntent().getStringExtra("movie_id");
        String detailsURL = getIntent().getStringExtra("movie_details_url");
        checkSettings();
        // Initializing recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        RecyclerDecorationHorizontal recyclerDecoration = new RecyclerDecorationHorizontal(30, 30, 10);
        screenshotRecycler.setLayoutManager(layoutManager);
        screenshotRecycler.setItemAnimator(new DefaultItemAnimator());
        screenshotRecycler.addItemDecoration(recyclerDecoration);

        if (movieID.endsWith("-tv")) {
            String tempID = movieID;
            movieID = tempID.replace("-tv", "");
            tvShow = true;
            mediaType = "tv";
        }
        fetchInstaData(detailsURL);
        fetchMovieDetails(movieID);
        fetchCredits(movieID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY > 0) {
                    if (toolbarDivider.getVisibility() == View.GONE) {
                        toolbarDivider.setVisibility(View.VISIBLE);
                    }
                } else {
                    toolbarDivider.setVisibility(View.GONE);
                }
            });
        }

        trailerButton.setOnClickListener(v -> {
            if (trailerURL != null) {
                videoIntent = new Intent();
                videoIntent.setClass(context, YouTubePlayerActivity.class);
                videoIntent.putExtra("VIDEO_ID", trailerURL);
                startActivity(videoIntent);
            }
        });

        downloadButton.setOnClickListener(v -> {
            if (downloadURL != null) {
                loadUrl(downloadURL);
            } else if (downloadsArray != null) {
                downloadOptionsFragment = DownloadOptionsFragment.newInstance();
                downloadOptionsFragment.setJsonArray(downloadsArray);
                downloadOptionsFragment.show(getSupportFragmentManager(), "BottomSheetDialog");
            } else {
                webIntent = new Intent();
                webIntent.setClass(context, HiddenWebActivity.class);
                webIntent.putExtra("HIDDEN_URL", detailsURL);
                startActivity(webIntent);
            }
        });

        String finalMovieID = movieID;  // Variable used in lambda expression should be final or effectively final
        retryButton.setOnClickListener(v -> {
            errorView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            // Using timer to avoid multiple clicking on retry
            handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                if (!isDestroyed()) {
                    fetchInstaData(detailsURL);
                    fetchMovieDetails(finalMovieID);
                    fetchCredits(finalMovieID);
                }
            }, getResources().getInteger(R.integer.retry_button_wait_time_default));
        });
    }

    @Override
    public void onBackPressed() {
        finish();
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
        inflater.inflate(R.menu.menu_movie_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getTitle().equals(getString(R.string.more))) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkSettings() {
        SharedPreferences settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        systemBrowser = settingsPreferences.getBoolean("system_browser_preference", false);
        chromeTabs = settingsPreferences.getBoolean("chrome_tabs_preference", true);
    }

    private void fetchInstaData(String url) {
        new Thread(() -> {
            Document document;
            try {
                document = Jsoup.connect(url).get();
            } catch (IOException e) {
                return;
            }

            Element instaJSON = document.getElementById("insta-json");
            if (instaJSON != null) {
                String jsonString = instaJSON.text();
                runOnUiThread(() -> parseInstaJson(jsonString));
            }
        }).start();
    }

    private void parseInstaJson(String json) {
        if (isDestroyed()) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("certificate")) {
                movieCertificate.setText(jsonObject.getString("certificate"));
            }
            if (jsonObject.has("adult") && jsonObject.getBoolean("adult")) {
                adultWarning.setVisibility(View.VISIBLE);
            }
            if (jsonObject.has("trailer")) {
                trailerURL = jsonObject.getString("trailer");
            }
            if (jsonObject.has("link")) {
                downloadURL = jsonObject.getString("link");
            }
            if (jsonObject.has("downloads")) {
                downloadsArray = jsonObject.getJSONArray("downloads");
            }
            if (jsonObject.has("screenshots")) {
                JSONArray screenshotsArray = jsonObject.getJSONArray("screenshots");
                screenshotsLayout.setVisibility(View.VISIBLE);
                parseScreenshotsArray(screenshotsArray);
            }
        } catch (JSONException jsonException) {
            Log.e("MovieDetailsActivity", jsonException.getMessage());
        }
    }

    private void parseScreenshotsArray(@NonNull JSONArray jsonArray) throws JSONException {
        ArrayList<ScreenshotModel> screenshotsArrayList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            ScreenshotModel model = new ScreenshotModel();
            model.setScreenshotPath(jsonArray.getJSONObject(i).getString("screenshot_path"));
            screenshotsArrayList.add(model);
        }
        ScreenshotAdapter adapter = new ScreenshotAdapter(screenshotsArrayList);
        screenshotRecycler.setAdapter(adapter);
    }

    private void fetchMovieDetails(@NonNull String fileId) {
        String apiKey = getString(R.string.tmdb_api_key);
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder().addInterceptor(new HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY)).build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(MovieDetailsApi.JSON_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MovieDetailsApi theMovieDbApi = retrofit.create(MovieDetailsApi.class);
        Call<MovieDetailsResponses> call = theMovieDbApi.getMovie(mediaType, fileId, apiKey, "en-US");
        call.enqueue(new Callback<MovieDetailsResponses>() {
            @Override
            public void onResponse(@NotNull Call<MovieDetailsResponses> call, @NotNull Response<MovieDetailsResponses> response) {
                if (!response.isSuccessful()) {
                    onFetchError(getString(R.string.error_retrofit_response));
                    return;
                }
                MovieDetailsResponses dbResponses = response.body();
                if (dbResponses != null) {
                    if (tvShow) {
                        setTvDetails(dbResponses);
                    } else {
                        Glide.with(context)
                                .load(Uri.parse("https://www.themoviedb.org/t/p/w220_and_h330_face/" + dbResponses.getPoster()))
                                .into(moviePoster);
                        String title = "N/A";
                        if (dbResponses.getTitle() != null) {
                            title = dbResponses.getTitle();
                            setTitle(title);
                        }
                        movieTitle.setText(title);
                        float starRating = dbResponses.getRating() / 2;
                        ratingBar.setRating(starRating);
                        movieRating.setText(String.valueOf(dbResponses.getRating()));
                        StringBuilder genre = new StringBuilder();
                        List<Genres> genresList = dbResponses.getGenres();
                        for (int i = 0; i < genresList.size(); i++) {
                            if (i != genresList.size() - 1) {
                                genre.append(genresList.get(i).getGenre()).append(", ");
                            } else {
                                genre.append(genresList.get(i).getGenre());
                            }
                        }
                        movieGenre.setText(genre);
                        if (dbResponses.getRuntime() != 0) {
                            int totalMinutes = dbResponses.getRuntime();
                            String durationHour = String.valueOf(totalMinutes / 60);
                            String durationMinutes = String.valueOf(totalMinutes % 60);
                            movieDuration.setText(String.format(Locale.getDefault(),"%shr %smin", durationHour, durationMinutes));
                            movieDurationMinutes.setText(String.format(Locale.getDefault(), "%d min", totalMinutes));
                        } else {
                            movieDuration.setText("N/A");
                            movieDurationMinutes.setText("N/A");
                        }
                        StringBuilder country = new StringBuilder();
                        List<ProductionCountries> productionCountries = dbResponses.getCountries();
                        for (int i = 0; i < productionCountries.size(); i++) {
                            if (i != productionCountries.size() - 1) {
                                country.append(productionCountries.get(i).getCountry()).append(", ");
                            } else {
                                country.append(productionCountries.get(i).getCountry());
                            }
                        }
                        String releaseYear = "N/A";
                        if (dbResponses.getYear() != null) {
                            releaseYear = String.format("%s  |  %s", dbResponses.getYear().substring(0, 4), country);
                        }
                        movieYear.setText(releaseYear);

                        movieSummary.setText(dbResponses.getOverview());
                    }
                    progressStatus.setVisibility(View.GONE);    // On connected with database hide the progressbar
                }
            }

            @Override
            public void onFailure(@NotNull Call<MovieDetailsResponses> call, @NotNull Throwable t) {
                onFetchError(getString(R.string.error_retrofit_enqueue_failed));
            }
        });
    }

    private void fetchCredits(@NonNull String fileId) {
        String apiKey = getString(R.string.tmdb_api_key);
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder().addInterceptor(new HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY)).build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(CreditsApi.JSON_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        CreditsApi creditsApi = retrofit.create(CreditsApi.class);
        Call<CreditsResponses> call = creditsApi.getCredits(mediaType, fileId, apiKey, "en-US");
        call.enqueue(new Callback<CreditsResponses>() {
            @Override
            public void onResponse(@NonNull Call<CreditsResponses> call, @NonNull Response<CreditsResponses> response) {
                if (!response.isSuccessful()) {
                    movieCasts.setText(getString(R.string.failed_to_load_data));
                    return;
                }
                CreditsResponses creditsResponses = response.body();
                if (creditsResponses != null) {
                    StringBuilder name = new StringBuilder();
                    List<Cast> castList = creditsResponses.getCast();
                    for (int i = 0; i < castList.size(); i++) {
                        if (i != castList.size() - 1) {
                            name.append(castList.get(i).getName()).append(", ");
                        } else {
                            name.append(castList.get(i).getName());
                        }
                    }
                    movieCasts.setText(name);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CreditsResponses> call, @NonNull Throwable t) {
                movieCasts.setText(getString(R.string.failed_to_load_data));
            }
        });
    }

    private void setTvDetails(@NonNull MovieDetailsResponses dbResponses) {
        Glide.with(context)
                .load(Uri.parse("https://www.themoviedb.org/t/p/w220_and_h330_face/" + dbResponses.getPoster()))
                .into(moviePoster);
        String title = "N/A";
        if (dbResponses.getName() != null) {
            title = dbResponses.getName();
            setTitle(title);
        }
        movieTitle.setText(title);
        float starRating = dbResponses.getRating() / 2;
        ratingBar.setRating(starRating);
        movieRating.setText(String.valueOf(dbResponses.getRating()));
        StringBuilder genre = new StringBuilder();
        List<Genres> genresList = dbResponses.getGenres();
        for (int i = 0; i < genresList.size(); i++) {
            if (i != genresList.size() - 1) {
                genre.append(genresList.get(i).getGenre()).append(", ");
            } else {
                genre.append(genresList.get(i).getGenre());
            }
        }
        movieGenre.setText(genre);
        if (dbResponses.getRuntime() != 0) {
            int totalMinutes = dbResponses.getRuntime();
            String durationHour = String.valueOf(totalMinutes / 60);
            String durationMinutes = String.valueOf(totalMinutes % 60);
            movieDuration.setText(String.format(Locale.getDefault(),"%shr %smin", durationHour, durationMinutes));
            movieDurationMinutes.setText(String.format(Locale.getDefault(), "%d min", totalMinutes));
        } else {
            movieDuration.setText("N/A");
            movieDurationMinutes.setText("N/A");
        }
        StringBuilder country = new StringBuilder();
        List<ProductionCountries> productionCountries = dbResponses.getCountries();
        for (int i = 0; i < productionCountries.size(); i++) {
            if (i != productionCountries.size() - 1) {
                country.append(productionCountries.get(i).getCountry()).append(", ");
            } else {
                country.append(productionCountries.get(i).getCountry());
            }
        }
        String releaseYear = "N/A";
        if (dbResponses.getTvYear() != null) {
            releaseYear = String.format("%s  |  %s", dbResponses.getTvYear().substring(0, 4), country);
        }
        movieYear.setText(releaseYear);
        movieSummary.setText(dbResponses.getOverview());
    }

    private void loadUrl(@NonNull String url) {
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
                    Intent handleIntent = new Intent();
                    handleIntent.setAction(Intent.ACTION_VIEW);
                    handleIntent.setData(Uri.parse(replacedUrl));
                    startActivity(handleIntent);
                } catch (android.content.ActivityNotFoundException notFoundException){
                    AppUtils.toastError(context, MovieDetailsActivity.this, getString(R.string.error_activity_not_found));
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
                        Intent handleIntent = new Intent();
                        handleIntent.setAction(Intent.ACTION_VIEW);
                        handleIntent.setData(Uri.parse(replacedUrl));
                        startActivity(handleIntent);
                    } catch (android.content.ActivityNotFoundException notFoundException){
                        AppUtils.toastError(context, MovieDetailsActivity.this, getString(R.string.error_activity_not_found));
                    }
                }
            } else {
                webIntent = new Intent();
                webIntent.setClass(context, WebActivity.class);
                webIntent.putExtra("WEB_URL", replacedUrl);
                startActivity(webIntent);
            }
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
    }

    private void onFetchError(String cause) {
        progressBar.setVisibility(View.GONE);
        errorCauseText.setText(cause);
        errorView.setVisibility(View.VISIBLE);
    }
}