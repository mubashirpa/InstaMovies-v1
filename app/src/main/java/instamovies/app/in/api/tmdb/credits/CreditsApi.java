package instamovies.app.in.api.tmdb.credits;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CreditsApi {

    String JSON_URL = "https://api.themoviedb.org/";

    @GET("3/movie/{movie_id}/credits")
    Call<CreditsResponses> getCredits(@Path("movie_id") String movieId,
                                           @Query("api_key") String apiKey,
                                           @Query("language") String language);
}