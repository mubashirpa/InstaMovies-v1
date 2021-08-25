package instamovies.app.in.fragments;

import android.app.Dialog;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import instamovies.app.in.R;
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

public class MovieDetailsFragment extends BottomSheetDialogFragment {

    private TextView movieTitle, movieGenre, movieYear, movieRating, movieSummary, errorMessage;
    private ProgressBar progressBar;
    private LinearLayout contentLayout, progressLayout;
    private String fileID;

    @Contract(" -> new")
    public static @NotNull MovieDetailsFragment newInstance() {
        return new MovieDetailsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogTheme);
        } catch (IllegalStateException stateException) {
            stateException.printStackTrace();
        }
    }

    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {
        View contentView = View.inflate(getContext(), R.layout.layout_movie_details, null);
        dialog.setContentView(contentView);
        BottomSheetBehavior<View> sheetBehavior = BottomSheetBehavior.from((View) contentView.getParent());
        sheetBehavior.setDraggable(false);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        movieTitle = contentView.findViewById(R.id.title);
        movieGenre = contentView.findViewById(R.id.genre);
        movieYear = contentView.findViewById(R.id.year);
        movieRating = contentView.findViewById(R.id.rating);
        movieSummary = contentView.findViewById(R.id.summary);
        progressBar = contentView.findViewById(R.id.progress_circular);
        errorMessage = contentView.findViewById(R.id.error_message);
        contentLayout = contentView.findViewById(R.id.layout_content);
        progressLayout = contentView.findViewById(R.id.layout_progress);
        Button downloadButton = contentView.findViewById(R.id.button_download);
        downloadButton.setOnClickListener(v -> dismiss());
        fetchData(fileID);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            Configuration configuration = getActivity().getResources().getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                    configuration.screenWidthDp > 470) {
                if (getDialog() != null) {
                    getDialog().getWindow().setLayout(dp(), -1);
                }
            } else {
                if (getDialog() != null) {
                    getDialog().getWindow().setLayout(-1, -1);
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                newConfig.screenWidthDp > 470) {
            if (getDialog() != null) {
                getDialog().getWindow().setLayout(dp(), -1);
            }
        } else {
            if (getDialog() != null) {
                getDialog().getWindow().setLayout(-1, -1);
            }
        }
    }

    private static int dp() {
        float density = Resources.getSystem().getDisplayMetrics().density;
        return (int) ((450 * density) + 0.5f);
    }

    private void fetchData(String id) {
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
        Call<MovieDetailsResponses> call = theMovieDbApi.getMovie(id, apiKey, "en-US");
        call.enqueue(new Callback<MovieDetailsResponses>() {
            @Override
            public void onResponse(@NotNull Call<MovieDetailsResponses> call, @NotNull Response<MovieDetailsResponses> response) {
                if (!response.isSuccessful()) {
                    progressBar.setVisibility(View.GONE);
                    errorMessage.setText(getString(R.string.error_retrofit_response));
                    errorMessage.setVisibility(View.VISIBLE);
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
                    progressLayout.setVisibility(View.GONE);
                    contentLayout.setVisibility(View.VISIBLE);
                    movieTitle.setText(dbResponses.getTitle());
                    movieGenre.setText(String.valueOf(genre));
                    movieYear.setText(dbResponses.getYear());
                    movieRating.setText(String.valueOf(dbResponses.getRating()));
                    movieSummary.setText(dbResponses.getOverview());
                }
            }

            @Override
            public void onFailure(@NotNull Call<MovieDetailsResponses> call, @NotNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                errorMessage.setText(getString(R.string.error_retrofit_enqueue_failed));
                errorMessage.setVisibility(View.VISIBLE);
            }
        });
    }

    public void setFileID(String fileID) {
        this.fileID = fileID;
    }
}