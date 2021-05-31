package instamovies.app.in.api;

import com.google.gson.annotations.SerializedName;

public class Genres {

    @SerializedName("name") private String genreName;

    public String getGenreName() {
        return genreName;
    }
}