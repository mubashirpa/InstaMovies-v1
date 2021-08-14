package instamovies.app.in.api.tmdb;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import instamovies.app.in.api.tmdb.Genres;

public class MovieDetailsResponses {

    @SerializedName("title") private String title;
    @SerializedName("genres") private List<Genres> genres;
    @SerializedName("release_date") private String year;
    @SerializedName("vote_average") private float rating;
    @SerializedName("overview") private String overview;
    @SerializedName("poster_path") private String poster;
    @SerializedName("runtime") private int runtime;
    @SerializedName("adult") private boolean adult;
    @SerializedName("production_countries") private List<ProductionCountries> countries;

    public String getTitle() {
        return title;
    }

    public List<Genres> getGenres() {
        return genres;
    }

    public String getYear() {
        return year;
    }

    public float getRating() {
        return rating;
    }

    public String getOverview() {
        return overview;
    }

    public String getPoster() {
        return poster;
    }

    public int getRuntime() {
        return runtime;
    }

    public List<ProductionCountries> getCountries() {
        return countries;
    }

    public boolean isAdult() {
        return adult;
    }
}