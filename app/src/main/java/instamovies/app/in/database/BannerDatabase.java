package instamovies.app.in.database;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import instamovies.app.in.adapters.BannerAdapter;

public class BannerDatabase {

    private ArrayList<HashMap<String, Object>> ViewSliderList = new ArrayList<>();
    private static final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private static ViewPager2 viewPager2;
    private final String LOG_TAG = "BannerDatabase";

    public BannerDatabase(@NotNull Context context, Activity activity, ViewPager2 viewPager2){
        BannerDatabase.viewPager2 = viewPager2;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = database.getReference("Thumbnail");

        databaseReference.child("Banner").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ViewSliderList = new ArrayList<>();
                try {
                    GenericTypeIndicator<HashMap<String, Object>> ind = new GenericTypeIndicator<HashMap<String, Object>>() {
                    };
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        HashMap<String, Object> map = data.getValue(ind);
                        ViewSliderList.add(map);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
                BannerAdapter bannerAdapter = new BannerAdapter(ViewSliderList, viewPager2, context, activity);
                viewPager2.setAdapter(bannerAdapter);
                viewPager2.setClipToPadding(false);
                viewPager2.setClipChildren(false);
                viewPager2.setOffscreenPageLimit(3);
                viewPager2.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

                CompositePageTransformer pageTransformer = new CompositePageTransformer();
                pageTransformer.addTransformer(new MarginPageTransformer(40));
                pageTransformer.addTransformer((page, position) -> {
                    float r = 1 - Math.abs(position);
                    page.setScaleY(0.85f + r * 0.15f);
                });
                viewPager2.setPageTransformer(pageTransformer);

                viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        sliderHandler.removeCallbacks(sliderRunnable);
                        sliderHandler.postDelayed(sliderRunnable, 3000);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(LOG_TAG, databaseError.getMessage());
            }
        });
    }

    private static final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewPager2 != null) {
                viewPager2.setCurrentItem(viewPager2.getCurrentItem() + 1);
            }
        }
    };

    public static void onPause() {
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    public static void onResume() {
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }
}