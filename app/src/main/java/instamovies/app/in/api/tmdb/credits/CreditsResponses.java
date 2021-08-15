package instamovies.app.in.api.tmdb.credits;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CreditsResponses {

    @SerializedName("cast") private List<Cast> cast;

    public List<Cast> getCast() {
        return cast;
    }
}