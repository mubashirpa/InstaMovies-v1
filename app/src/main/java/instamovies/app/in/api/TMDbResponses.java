package instamovies.app.in.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TMDbResponses {

    @SerializedName("title") private String movieTitle;
    @SerializedName("genres") private List<Genres> genres;
    @SerializedName("release_date") private String releaseDate;
    @SerializedName("vote_average") private String rating;
    @SerializedName("overview") private String overview;

    public String getMovieTitle() {
        return movieTitle;
    }

    public List<Genres> getGenres() {
        return genres;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getRating() {
        return rating;
    }

    public String getOverview() {
        return overview;
    }
}