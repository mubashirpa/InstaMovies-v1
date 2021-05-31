package instamovies.app.in.fragments;

import android.app.Dialog;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import instamovies.app.in.R;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    private String title, message = "";
    private String positiveButtonText = "";
    private View.OnClickListener positiveButtonListener;

    @Contract(" -> new")
    public static @NotNull BottomSheetFragment newInstance() {
        return new BottomSheetFragment();
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
        View contentView = View.inflate(getContext(), R.layout.layout_bottomsheet, null);
        dialog.setContentView(contentView);
        BottomSheetBehavior<View> sheetBehavior = BottomSheetBehavior.from((View)contentView.getParent());
        sheetBehavior.setDraggable(false);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.setCanceledOnTouchOutside(false);

        TextView titleBody = contentView.findViewById(R.id.title);
        TextView messageBody = contentView.findViewById(R.id.message);
        Button buttonPositive = contentView.findViewById(R.id.positive_button);

        if (!title.equals("")) {
            titleBody.setVisibility(View.VISIBLE);
            titleBody.setText(title);
        }
        messageBody.setText(message);
        buttonPositive.setText(positiveButtonText);
        if (positiveButtonListener != null) {
            buttonPositive.setOnClickListener(positiveButtonListener);
        }
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
        return (int) ((450 * Resources.getSystem().getDisplayMetrics().density) + 0.5f);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setPositiveButton(String text, View.OnClickListener listener) {
        this.positiveButtonText = text;
        this.positiveButtonListener = listener;
    }
}