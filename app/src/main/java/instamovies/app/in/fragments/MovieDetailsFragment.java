package instamovies.app.in.fragments;

import android.app.Dialog;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import instamovies.app.in.R;

public class MovieDetailsFragment extends BottomSheetDialogFragment {

    private View.OnClickListener onClickListener = null;
    public TextView movieTitle, movieGenre, movieYear, movieRating, movieSummary, errorMessage;
    public ProgressBar progressBar;
    public LinearLayout contentLayout, progressLayout;

    @Contract(" -> new")
    public static @NotNull MovieDetailsFragment newInstance() {
        return new MovieDetailsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogTheme);
        } catch (IllegalStateException stateException) {
            stateException.printStackTrace();
        }
    }

    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {
        View contentView = View.inflate(getContext(), R.layout.layout_movie_details, null);
        dialog.setContentView(contentView);
        BottomSheetBehavior<View> sheetBehavior = BottomSheetBehavior.from((View) contentView.getParent());
        sheetBehavior.setDraggable(false);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        movieTitle = contentView.findViewById(R.id.title);
        movieGenre = contentView.findViewById(R.id.genre);
        movieYear = contentView.findViewById(R.id.year);
        movieRating = contentView.findViewById(R.id.rating);
        movieSummary = contentView.findViewById(R.id.summary);
        progressBar = contentView.findViewById(R.id.progress_circular);
        errorMessage = contentView.findViewById(R.id.error_message);
        contentLayout = contentView.findViewById(R.id.layout_content);
        progressLayout = contentView.findViewById(R.id.layout_progress);

        Button downloadButton = contentView.findViewById(R.id.button_download);
        downloadButton.setOnClickListener(onClickListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            Configuration configuration = getActivity().getResources().getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                    configuration.screenWidthDp > 470) {
                if (getDialog() != null) {
                    getDialog().getWindow().setLayout(dp(), -1);
                }
            } else {
                if (getDialog() != null) {
                    getDialog().getWindow().setLayout(-1, -1);
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                newConfig.screenWidthDp > 470) {
            if (getDialog() != null) {
                getDialog().getWindow().setLayout(dp(), -1);
            }
        } else {
            if (getDialog() != null) {
                getDialog().getWindow().setLayout(-1, -1);
            }
        }
    }

    private static int dp() {
        float density = Resources.getSystem().getDisplayMetrics().density;
        return (int) ((450 * density) + 0.5f);
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }
}