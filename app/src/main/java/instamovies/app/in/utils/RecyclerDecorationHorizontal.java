package instamovies.app.in.utils;

import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerDecorationHorizontal extends RecyclerView.ItemDecoration {

    private final int marginStart;
    private final int marginEnd;
    private final int spaceBetween;

    public RecyclerDecorationHorizontal(int marginStart, int marginEnd, int spaceBetween) {
        this.marginStart = marginStart;
        this.marginEnd = marginEnd;
        this.spaceBetween = spaceBetween;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int total = state.getItemCount();
        int position = parent.getChildAdapterPosition(view);
        outRect.right = dp(spaceBetween);
        if (position == 0) {
            outRect.left = dp(marginStart);
        }
        if (position == total - 1) {
            outRect.right = dp(marginEnd);
        }
    }

    private static int dp(int dp) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        return (int) ((dp * density) + 0.5f);
    }
}