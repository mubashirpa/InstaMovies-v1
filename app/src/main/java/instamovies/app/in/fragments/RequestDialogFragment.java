package instamovies.app.in.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import instamovies.app.in.R;
import instamovies.app.in.utils.AppUtils;

public class RequestDialogFragment extends BottomSheetDialogFragment {

    private final FirebaseFirestore requestDatabase = FirebaseFirestore.getInstance();
    private AlertDialog progressDialog;
    private Context context;

    @Contract(" -> new")
    public static @NotNull RequestDialogFragment newInstance() {
        return new RequestDialogFragment();
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
        View contentView = View.inflate(getContext(), R.layout.layout_request, null);
        dialog.setContentView(contentView);
        BottomSheetBehavior<View> sheetBehavior = BottomSheetBehavior.from((View)contentView.getParent());
        sheetBehavior.setDraggable(true);
        sheetBehavior.setFitToContents(true);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        context = dialog.getContext();

        EditText movieName = contentView.findViewById(R.id.movie_name);
        EditText movieLanguage = contentView.findViewById(R.id.movie_language);
        EditText movieSize = contentView.findViewById(R.id.movie_size);
        EditText movieYear = contentView.findViewById(R.id.movie_year);
        Button requestButton = contentView.findViewById(R.id.request_button);
        Button closeButton = contentView.findViewById(R.id.close_button);

        requestButton.setOnClickListener(v -> {
            if (movieName.getText().toString().equals("")) {
                AppUtils.toastShortDefault(requireContext(), requireActivity(),"Enter required fields");
            } else {
                dialog.dismiss();
                String reqDetails = movieName.getText().toString()+" | " + movieLanguage.getText().toString()+" | " + movieYear.getText().toString()+" | " + movieSize.getText().toString();
                showProgressDialog();
                requestMovie(reqDetails);
            }
        });

        closeButton.setOnClickListener(v -> dismiss());
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

    private void requestMovie(String requestDetails) {
        String timeStamp = new SimpleDateFormat("dd/MM hh:mm a", Locale.getDefault()).format(new Date());
        Map<String, Object> Details = new HashMap<>();
        Details.put("Details", requestDetails);
        Details.put("Time", timeStamp);
        requestDatabase.collection("Request")
                .add(Details)
                .addOnSuccessListener(documentReference -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    dismiss();
                    AppUtils.toastShortDefault(getContext(), requireActivity(), "Successfully requested");
                })
                .addOnFailureListener(e -> {
                    AppUtils.toastShortError(getContext(), requireActivity(), "Failed to Request movie");
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                });
    }

    private void showProgressDialog() {
        progressDialog = new AlertDialog.Builder(context).create();
        Window window = progressDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        LayoutInflater inflater = getLayoutInflater();
        View convertView = inflater.inflate(R.layout.layout_progress_dialog, null);
        progressDialog.setView(convertView);
        progressDialog.setTitle(null);
        TextView titleText = convertView.findViewById(R.id.title_text);
        ImageView dialogCloseButton = convertView.findViewById(R.id.dialog_close_button);
        titleText.setText(getString(R.string.sending));
        dialogCloseButton.setOnClickListener(v -> progressDialog.dismiss());
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }
}