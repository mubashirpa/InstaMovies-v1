package instamovies.app.in.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TMDbApi {

    String JSON_URL = "https://api.themoviedb.org/";

    @GET("3/movie/{movie_id}")
    Call<TMDbResponses> getMovie(@Path ("movie_id") String movieId, @Query("api_key") String apiKey);
}