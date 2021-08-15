package instamovies.app.in.api.movies;

import java.util.ArrayList;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface MoviesJsonApi {

    @GET()
    Call<ArrayList<MoviesJsonResponse>> getMovies(@Url String referencePath);
}