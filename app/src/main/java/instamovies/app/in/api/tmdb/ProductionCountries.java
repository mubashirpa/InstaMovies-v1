package instamovies.app.in.api.tmdb;

import com.google.gson.annotations.SerializedName;

public class ProductionCountries {

    @SerializedName("name") private String country;

    public String getCountry() {
        return country;
    }
}