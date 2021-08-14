package instamovies.app.in;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    private TextView movieSummary, movieTitle, releaseYear, movieDuration, durationMins, movieRating, movieGenre, IMDbRating;
    private TextView castDetails;
    private ImageView moviePoster;
    private Context context;
    private RecyclerView thumbnailRecycler;
    private final ArrayList<ScreenshotModel> screenshotsArrayList = new ArrayList<>();
    private RatingBar ratingBar;
    private LinearLayout progressbarLayout;
    private View errorLayout;
    private String dataURL;
    private Button buttonDownload;
    private TextView adultWarning;
    private LinearLayout screenshotsLayout;

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
        movieSummary = findViewById(R.id.movie_summary);
        movieTitle = findViewById(R.id.movie_title);
        releaseYear = findViewById(R.id.release_year);
        movieDuration = findViewById(R.id.duration);
        durationMins = findViewById(R.id.duration_mins);
        movieRating = findViewById(R.id.rating);
        moviePoster = findViewById(R.id.poster);
        thumbnailRecycler = findViewById(R.id.thumbnail_recycler);
        movieGenre = findViewById(R.id.genre);
        IMDbRating = findViewById(R.id.imdb_rating);
        ratingBar = findViewById(R.id.rating_bar);
        castDetails = findViewById(R.id.cast_details);
        progressbarLayout = findViewById(R.id.progressbar_layout);
        errorLayout = findViewById(R.id.error_linear);
        buttonDownload = findViewById(R.id.button_download);
        Button retryButton = errorLayout.findViewById(R.id.retry_button);
        ScrollView scrollView = findViewById(R.id.scrollView);
        View toolbarDivider = findViewById(R.id.toolbar_divider);
        adultWarning = findViewById(R.id.adult_warning);
        screenshotsLayout = findViewById(R.id.screenshots_layout);
        dataURL = getIntent().getStringExtra("Movie Link");

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        RecyclerDecorationHorizontal recyclerDecoration = new RecyclerDecorationHorizontal(30, 30, 10);
        thumbnailRecycler.setLayoutManager(layoutManager);
        thumbnailRecycler.setItemAnimator(new DefaultItemAnimator());
        thumbnailRecycler.addItemDecoration(recyclerDecoration);

        //fetchData(dataURL);
        fetchMovieDetails("tt10661848");
        fetchCredits("tt10661848");

        retryButton.setOnClickListener(v -> {
            progressbarLayout.setVisibility(View.VISIBLE);
            errorLayout.setVisibility(View.GONE);
            fetchData(dataURL);
        });

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
    }

    @Override
    public void onBackPressed() {
        finish();
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

    private void fetchData(String url) {
        new Thread(() -> {
            Document document;
            try {
                document = Jsoup.connect(url).get();
            } catch (IOException e) {
                runOnUiThread(this::fetchError);
                return;
            }

            Element omdbJSON = document.getElementById("omdb_json");
            Element moreJSON = document.getElementById("more_json");
            runOnUiThread(this::fetchSuccess);
            if (omdbJSON != null) {
                String omdbString = omdbJSON.text();
                runOnUiThread(() -> parseOMDB(omdbString));
            }
            if (moreJSON != null) {
                String moreString = moreJSON.text();
                runOnUiThread(() -> parseMore(moreString));
            }
        }).start();
    }

    private void parseOMDB(String json) {
        if (isDestroyed()) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            setTitle(jsonObject.getString("Title"));
            movieTitle.setText(jsonObject.getString("Title"));
            releaseYear.setText(String.format("%s  |  %s", jsonObject.getString("Year"), jsonObject.getString("Country")));
            int totalMinutes = Integer.parseInt(jsonObject.getString("Runtime").replace(" min", ""));
            String durationHour = String.valueOf((long)(totalMinutes / 60));
            String durationMinutes = String.valueOf((long)(totalMinutes % 60));
            movieDuration.setText(String.format(Locale.getDefault(),"%shr %smin", durationHour, durationMinutes));
            durationMins.setText(jsonObject.getString("Runtime"));
            movieRating.setText(jsonObject.getString("Rated"));
            IMDbRating.setText(jsonObject.getString("imdbRating"));
            int starRating = jsonObject.getInt("imdbRating") / 2;
            ratingBar.setRating(starRating);
            movieSummary.setText(jsonObject.getString("Plot"));
            movieGenre.setText(jsonObject.getString("Genre"));
            castDetails.setText(jsonObject.getString("Actors"));
            Glide.with(context).load(Uri.parse(jsonObject.getString("Poster"))).into(moviePoster);
        } catch (JSONException e) {
            AppUtils.toastShortError(context, MovieDetailsActivity.this, e.getMessage());
        }
    }

    private void parseMore(String json) {
        if (isDestroyed()) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("adult") && jsonObject.getBoolean("adult")) {
                adultWarning.setVisibility(View.VISIBLE);
            }

            //fetching screenshots
            if (jsonObject.has("screenshots")) {
                JSONArray screenshotsArray = jsonObject.getJSONArray("screenshots");
                fetchScreenshots(screenshotsArray);
            } else {
                screenshotsLayout.setVisibility(View.GONE);
            }

            //fetching download links
            if (jsonObject.has("downloads")) {
                JSONArray downloadsArray = jsonObject.getJSONArray("downloads");
                buttonDownload.setOnClickListener(v -> fetchDownloads(downloadsArray));
            }
        } catch (JSONException e) {
            AppUtils.toastShortError(context, MovieDetailsActivity.this, e.getMessage());
        }
    }

    private void fetchScreenshots(@NonNull JSONArray jsonArray) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            ScreenshotModel model = new ScreenshotModel();
            model.setBackdropPath(jsonArray.getJSONObject(i).getString("backdrop_path"));
            if (jsonArray.getJSONObject(i).has("video_url")) {
                model.setVideoUrl(jsonArray.getJSONObject(i).getString("video_url"));
            }
            screenshotsArrayList.add(model);
        }
        ScreenshotAdapter adapter = new ScreenshotAdapter(screenshotsArrayList);
        thumbnailRecycler.setAdapter(adapter);
    }

    private void fetchDownloads(JSONArray jsonArray) {
        DownloadOptionsFragment downloadOptionsFragment = DownloadOptionsFragment.newInstance();
        downloadOptionsFragment.setJsonArray(jsonArray);
        downloadOptionsFragment.show(getSupportFragmentManager(), "BottomSheetDialog");
    }

    private void fetchMovieDetails(String fileId) {
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
        Call<MovieDetailsResponses> call = theMovieDbApi.getMovie(fileId, apiKey, "en-US");
        call.enqueue(new Callback<MovieDetailsResponses>() {
            @Override
            public void onResponse(@NotNull Call<MovieDetailsResponses> call, @NotNull Response<MovieDetailsResponses> response) {
                if (!response.isSuccessful()) {
                    AppUtils.toastShortError(context, MovieDetailsActivity.this, "Error");
                    return;
                }
                progressbarLayout.setVisibility(View.GONE);
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

                    StringBuilder country = new StringBuilder();
                    List<ProductionCountries> productionCountries = dbResponses.getCountries();
                    for (int i = 0; i < productionCountries.size(); i++) {
                        if (i != productionCountries.size() - 1) {
                            country.append(productionCountries.get(i).getCountry()).append(", ");
                        } else {
                            country.append(productionCountries.get(i).getCountry());
                        }
                    }

                    setTitle(dbResponses.getTitle());
                    movieTitle.setText(dbResponses.getTitle());
                    Glide.with(context)
                            .load(Uri.parse("https://www.themoviedb.org/t/p/w220_and_h330_face/" + dbResponses.getPoster()))
                            .into(moviePoster);
                    IMDbRating.setText(String.valueOf(dbResponses.getRating()));
                    float starRating = dbResponses.getRating() / 2;
                    ratingBar.setRating(starRating);
                    movieGenre.setText(genre);
                    int totalMinutes = dbResponses.getRuntime();
                    String durationHour = String.valueOf(totalMinutes / 60);
                    String durationMinutes = String.valueOf(totalMinutes % 60);
                    movieDuration.setText(String.format(Locale.getDefault(),"%shr %smin", durationHour, durationMinutes));
                    releaseYear.setText(String.format("%s  |  %s", dbResponses.getYear().substring(0, 4), country));
                    durationMins.setText(String.format(Locale.getDefault(), "%d min", totalMinutes));
                    movieSummary.setText(dbResponses.getOverview());
                    if (dbResponses.isAdult()) {
                        adultWarning.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call<MovieDetailsResponses> call, @NotNull Throwable t) {
                AppUtils.toastShortError(context, MovieDetailsActivity.this, "Error: " + t.getMessage());
            }
        });
    }

    private void fetchCredits(String fileId) {
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
        Call<CreditsResponses> call = creditsApi.getCredits(fileId, apiKey, "en-US");
        call.enqueue(new Callback<CreditsResponses>() {
            @Override
            public void onResponse(@NonNull Call<CreditsResponses> call, @NonNull Response<CreditsResponses> response) {
                if (!response.isSuccessful()) {
                    AppUtils.toastShortError(context, MovieDetailsActivity.this, "Error");
                    return;
                }
                CreditsResponses creditsResponses = response.body();
                if (creditsResponses != null) {
                    StringBuilder name = new StringBuilder();
                    List<Cast> castList = creditsResponses.getCast();
                    for (int i = 0; i < castList.size(); i++) {
                        name.append(castList.get(i).getName());
                    }
                    castDetails.setText(name);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CreditsResponses> call, @NonNull Throwable t) {
                AppUtils.toastShortError(context, MovieDetailsActivity.this, "Error: " + t.getMessage());
            }
        });
    }

    private void fetchSuccess() {
        progressbarLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.GONE);
    }

    private void fetchError() {
        progressbarLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
    }
}