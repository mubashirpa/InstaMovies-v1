package instamovies.app.in.api.tmdb;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MovieDetailsApi {

    String JSON_URL = "https://api.themoviedb.org/";

    @GET("3/{type}/{movie_id}")
    Call<MovieDetailsResponses> getMovie(@Path ("type") String type,
                                         @Path ("movie_id") String movieId,
                                         @Query("api_key") String apiKey,
                                         @Query("language") String language);
}