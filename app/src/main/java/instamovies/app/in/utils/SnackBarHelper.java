package instamovies.app.in.utils;

import android.content.Context;
import android.view.ViewGroup;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import com.google.android.material.snackbar.Snackbar;
import org.jetbrains.annotations.NotNull;
import instamovies.app.in.R;

public class SnackBarHelper {

    public static void configSnackBar(Context context, Snackbar snackbar){
        addMargins(snackbar);
        setRoundBoarder(context,snackbar);
        ViewCompat.setElevation(snackbar.getView(),6f);
    }

    private static void addMargins(@NotNull Snackbar snackbar) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) snackbar.getView().getLayoutParams();
        layoutParams.setMargins(12, 12, 12, 12);
        snackbar.getView().setLayoutParams(layoutParams);
    }

    private static void setRoundBoarder(@NotNull Context context, @NotNull Snackbar snackbar) {
        snackbar.getView().setBackground(ContextCompat.getDrawable(context, R.drawable.bg_snackbar));
    }
}