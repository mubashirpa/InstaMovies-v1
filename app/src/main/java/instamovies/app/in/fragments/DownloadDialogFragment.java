package instamovies.app.in.fragments;

import android.app.Dialog;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import instamovies.app.in.R;

public class DownloadDialogFragment extends BottomSheetDialogFragment {

    private String titleDownloadFile = "";
    private String sizeDownloadFile = "";
    private int fileType;
    private boolean showCellularInfoText = false;
    private boolean showPlayOnlineText = false;
    private View.OnClickListener confirmListener = null;
    private View.OnClickListener cancelListener = null;
    private View.OnClickListener playOnlineListener = null;
    private EditText titleText;

    @Contract(" -> new")
    public static @NotNull DownloadDialogFragment newInstance() {
        return new DownloadDialogFragment();
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
        View contentView = View.inflate(getContext(), R.layout.layout_download, null);
        dialog.setContentView(contentView);
        BottomSheetBehavior<View> sheetBehavior = BottomSheetBehavior.from((View)contentView.getParent());
        sheetBehavior.setDraggable(false);
        sheetBehavior.setFitToContents(true);
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.setCanceledOnTouchOutside(false);

        TextView cellularDataInfo = contentView.findViewById(R.id.cellular_data_info);
        ImageView iconType = contentView.findViewById(R.id.icon_type);
        titleText = contentView.findViewById(R.id.title_text);
        TextView sizeText = contentView.findViewById(R.id.size_text);
        Button confirmButton = contentView.findViewById(R.id.confirm_button);
        Button cancelButton = contentView.findViewById(R.id.cancel_button);
        TextView playOnline = contentView.findViewById(R.id.play_online);

        if (isShowCellularInfo()) {
            cellularDataInfo.setVisibility(View.VISIBLE);
        }
        if (isShowPlayOnlineText()) {
            playOnline.setVisibility(View.VISIBLE);
        }
        titleText.setText(getFileTitle());
        sizeText.setText(getFileSize());
        confirmButton.setOnClickListener(confirmListener);
        cancelButton.setOnClickListener(cancelListener);
        playOnline.setOnClickListener(playOnlineListener);
        iconType.setImageResource(getFileType());
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

    public void setFileTitle(String title) {
        titleDownloadFile = title;
    }

    private String getFileTitle() {
        return titleDownloadFile;
    }

    public void setFileSize(String size) {
        sizeDownloadFile = size;
    }

    private String getFileSize() {
        return sizeDownloadFile;
    }

    public void setConfirmListener(View.OnClickListener onClickListener) {
        confirmListener = onClickListener;
    }

    public void setCancelListener(View.OnClickListener onClickListener) {
        cancelListener = onClickListener;
    }

    public void setPlayOnlineClickListener(View.OnClickListener onClickListener) {
        playOnlineListener = onClickListener;
    }

    public void showCellularInfo(boolean show) {
        showCellularInfoText = show;
    }

    private boolean isShowCellularInfo() {
        return showCellularInfoText;
    }

    public void showPlayOnline(boolean show) {
        showPlayOnlineText = show;
    }

    private boolean isShowPlayOnlineText() {
        return showPlayOnlineText;
    }

    public void setFileType(int file) {
        fileType = file;
    }

    private int getFileType() {
        return fileType;
    }

    public String getFileName() {
        String title;
        if (titleText != null) {
            title = titleText.getText().toString();
        } else {
            title = "";
        }
        return title;
    }
}