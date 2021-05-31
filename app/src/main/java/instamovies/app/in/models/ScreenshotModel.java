package instamovies.app.in.models;

public class ScreenshotModel {

    private String backdropPath;
    private String videoUrl;

    public void setBackdropPath(String url) {
        backdropPath = url;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    public void setVideoUrl(String url) {
        videoUrl = url;
    }

    public String getVideoUrl() {
        return videoUrl;
    }
}