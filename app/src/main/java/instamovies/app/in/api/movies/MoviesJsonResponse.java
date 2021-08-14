package instamovies.app.in.api.movies;

import com.google.gson.annotations.SerializedName;

public class MoviesJsonResponse {

    @SerializedName("Name") private String name;
    @SerializedName("Thumbnail") private String thumbnail;
    @SerializedName("Movie") private String movie;
    @SerializedName("Link") private String link;
    @SerializedName("Link1") private String link1;
    @SerializedName("Link2") private String link2;
    @SerializedName("Link3") private String link3;
    @SerializedName("Video") private String video;

    public String getName() {
        return name;
    }

    public  String getThumbnail() {
        return thumbnail;
    }

    public String getMovie() {
        return movie;
    }

    public String getLink() {
        return link;
    }

    public String getLink1() {
        return link1;
    }

    public String getLink2() {
        return link2;
    }

    public String getLink3() {
        return link3;
    }

    public String getVideo() {
        return video;
    }
}