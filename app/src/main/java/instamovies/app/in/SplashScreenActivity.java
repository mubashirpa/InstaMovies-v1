package instamovies.app.in;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import org.jetbrains.annotations.NotNull;
import instamovies.app.in.fragments.BottomSheetFragment;
import instamovies.app.in.utils.AppUtils;

public class SplashScreenActivity extends AppCompatActivity {

    private SharedPreferences appDetails;
    private Context context;
    private final int REQUEST_CODE_STORAGE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        context = this;
        Window window = getWindow();
        switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
            case Configuration.UI_MODE_NIGHT_YES:

                break;
            case Configuration.UI_MODE_NIGHT_NO:
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    WindowInsetsController controller = getWindow().getInsetsController();
                    if(controller != null) {
                        controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                    //Deprecated in Api level 30
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

                } else {
                    window.setStatusBarColor(Color.BLACK);
                }
                break;
        }

        SharedPreferences settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String themePreference = settingsPreferences.getString("theme_preference","");

        switch (themePreference) {
            case "System default":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "Set by Battery Saver":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                break;
            case "Light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "Dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            AlertDialog.Builder permissionDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
            permissionDialog.setTitle("Need permission");
            permissionDialog.setMessage("To download movies and files, give us permission to use your storage");
            permissionDialog.setPositiveButton("GRANT PERMISSION", (dialogInterface, i) -> ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE));
            permissionDialog.setNegativeButton("EXIT", (dialogInterface, i) -> finishAffinity());
            permissionDialog.setCancelable(false);
            permissionDialog.create().show();
        } else {
            initializeActivity();
        }
    }

    private void initializeActivity() {
        appDetails = getSharedPreferences("appDetails", MODE_PRIVATE);
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.anim_fade_in_splash_text);
        ImageView appIcon = findViewById(R.id.app_icon);
        appIcon.startAnimation(animation);

        if (appDetails != null) {
            if (!appDetails.getBoolean("First Open", false)) {
                BottomSheetFragment bottomSheetDialog = BottomSheetFragment.newInstance();
                bottomSheetDialog.setTitle("Important");
                bottomSheetDialog.setMessage(getString(R.string.copyright_info));
                bottomSheetDialog.setPositiveButton("Continue", v -> {
                    bottomSheetDialog.dismiss();
                    appDetails.edit().putBoolean("First Open", true).apply();
                    startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
                    overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
                    finish();
                });
                bottomSheetDialog.setCancelable(false);
                bottomSheetDialog.show(getSupportFragmentManager(), "BottomSheetDialog");
            } else {
                startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
                overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String @NotNull [] permissions, int @NotNull [] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if (requestCode == REQUEST_CODE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                AppUtils.toastError(context, SplashScreenActivity.this, getString(R.string.error_permission_denied, "storage"));
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                Uri uri = Uri.fromParts("package",getPackageName(),null);
                intent.setData(uri);
                try {
                    startActivity(intent);
                } catch (Exception exception) {
                    Log.e("SplashScreenActivity", exception.getMessage());
                }
                finish();
            } else {
                initializeActivity();
            }
        }
    }
}