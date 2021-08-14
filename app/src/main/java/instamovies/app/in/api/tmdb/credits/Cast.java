package instamovies.app.in.api.tmdb.credits;

import com.google.gson.annotations.SerializedName;

public class Cast {

    @SerializedName("name") private String name;
    @SerializedName("profile_path") private String profile_path;

    public String getName() {
        return name;
    }

    public String getProfile() {
        return profile_path;
    }
}