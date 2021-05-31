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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import instamovies.app.in.fragments.DownloadOptionsFragment;
import instamovies.app.in.utils.AppUtils;
import instamovies.app.in.utils.RecyclerDecorationHorizontal;
import instamovies.app.in.adapters.ScreenshotAdapter;
import instamovies.app.in.models.ScreenshotModel;

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

        fetchData(dataURL);

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
                runOnUiThread(() -> {
                    progressbarLayout.setVisibility(View.GONE);
                    errorLayout.setVisibility(View.VISIBLE);
                });
                return;
            }

            Element omdbJSON = document.getElementById("omdb_json");
            Element moreJSON = document.getElementById("more_json");
            String omdbString = omdbJSON.text();
            String moreString = moreJSON.text();
            runOnUiThread(() -> {
                parseOMDB(omdbString);
                parseMore(moreString);
                progressbarLayout.setVisibility(View.GONE);
            });
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
            Glide.with(context)
                    .load(Uri.parse(jsonObject.getString("Poster")))
                    .error(R.drawable.img_image_placeholder_h).into(moviePoster);
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

    private void fetchScreenshots(JSONArray jsonArray) throws JSONException {
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
}