package instamovies.app.in.api.tmdb;

import com.google.gson.annotations.SerializedName;

public class ProductionCountries {

    @SerializedName("iso_3166_1") private String country;

    public String getCountry() {
        return country;
    }
}