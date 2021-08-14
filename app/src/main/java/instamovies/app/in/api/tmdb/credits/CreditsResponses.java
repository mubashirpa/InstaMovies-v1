package instamovies.app.in.api.tmdb.credits;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import instamovies.app.in.api.tmdb.Genres;
import instamovies.app.in.api.tmdb.ProductionCountries;

public class CreditsResponses {

    @SerializedName("cast") private List<Cast> cast;

    public List<Cast> getCast() {
        return cast;
    }
}