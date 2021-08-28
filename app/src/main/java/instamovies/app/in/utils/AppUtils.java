package instamovies.app.in.utils;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import org.jetbrains.annotations.NotNull;
import instamovies.app.in.R;

public class AppUtils {

    public static void toast(Context context, @NotNull Activity activity, String message){
        LayoutInflater inflater = activity.getLayoutInflater();
        View viewLayout = inflater.inflate(R.layout.layout_toast, activity.findViewById(R.id.parent_view));
        TextView toast_text = viewLayout.findViewById(R.id.toast_text);
        toast_text.setText(message);
        Toast toast = Toast.makeText(context,"",Toast.LENGTH_SHORT);

        //Deprecated
        toast.setView(viewLayout);

        toast.show();
    }

    public static void toastError(Context context, @NotNull Activity activity, String message){
        LayoutInflater inflater = activity.getLayoutInflater();
        View viewLayout = inflater.inflate(R.layout.layout_toast_error, activity.findViewById(R.id.parent_view));
        TextView toast_text = viewLayout.findViewById(R.id.toast_text);
        toast_text.setText(message);
        Toast toast = Toast.makeText(context,"",Toast.LENGTH_SHORT);

        //Deprecated
        toast.setView(viewLayout);

        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();
    }
}