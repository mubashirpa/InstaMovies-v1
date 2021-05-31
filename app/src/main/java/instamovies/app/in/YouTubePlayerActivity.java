package instamovies.app.in;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragmentX;
import instamovies.app.in.utils.AppUtils;

public class YouTubePlayerActivity extends AppCompatActivity {

    private String videoID;
    private YouTubePlayerSupportFragmentX playerSupportFragment;
    private YouTubePlayer tubePlayer;
    private String API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_player);
        initializeActivity();
    }

    private void initializeActivity() {
        API_KEY = getString(R.string.youtube_player_api_key);
        videoID = getIntent().getStringExtra("VIDEO_ID");
        if (videoID.contains("https://youtu.be/")) {
            videoID = videoID.replace("https://youtu.be/","");
        }
        playerSupportFragment = (YouTubePlayerSupportFragmentX) getSupportFragmentManager().findFragmentById(R.id.youtube_player_view);
        initializePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tubePlayer != null) {
            tubePlayer.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tubePlayer != null) {
            tubePlayer.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tubePlayer != null) {
            tubePlayer.pause();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
        }
    }

    private void initializePlayer() {
        playerSupportFragment.initialize(API_KEY, new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
                if (youTubePlayer != null) {
                    try {
                        tubePlayer = youTubePlayer;
                        youTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);
                        youTubePlayer.setShowFullscreenButton(false);
                        if (!wasRestored) {
                            youTubePlayer.loadVideo(videoID);
                        }
                    } catch (Exception e) {
                        initializePlayer();
                    }
                }
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                AppUtils.toastShortError(getApplicationContext(), YouTubePlayerActivity.this, "Failed to initialize player");
            }
        });
    }

    private void hideSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {

            //Deprecated in Api level 30
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        }
    }
}