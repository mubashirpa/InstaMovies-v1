package instamovies.app.in.api.tmdb;

import com.google.gson.annotations.SerializedName;

public class Genres {

    @SerializedName("name") private String genre;

    public String getGenre() {
        return genre;
    }
}