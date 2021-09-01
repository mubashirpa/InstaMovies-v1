package instamovies.app.in.api.tmdb;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MovieDetailsResponses {

    @SerializedName("title") private String title;
    @SerializedName("name") private String name;
    @SerializedName("genres") private List<Genres> genres;
    @SerializedName("release_date") private String year;
    @SerializedName("first_air_date") private String tv_year;
    @SerializedName("vote_average") private float rating;
    @SerializedName("overview") private String overview;
    @SerializedName("poster_path") private String poster;
    @SerializedName("runtime") private int runtime;
    @SerializedName("production_countries") private List<ProductionCountries> countries;

    public String getTitle() {
        return title;
    }

    public String getName() {
        return name;
    }

    public List<Genres> getGenres() {
        return genres;
    }

    public String getYear() {
        return year;
    }

    public String getTvYear() {
        return tv_year;
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
}