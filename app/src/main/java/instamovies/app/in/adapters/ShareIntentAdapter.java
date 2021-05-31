package instamovies.app.in.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import instamovies.app.in.R;
import instamovies.app.in.models.ShareIntentModel;

public class ShareIntentAdapter extends RecyclerView.Adapter<ShareIntentAdapter.MyViewHolder> {

    private final ArrayList<ShareIntentModel> intentList;

    public ShareIntentAdapter(ArrayList<ShareIntentModel> intentList) {
        this.intentList = intentList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_menu_webview, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.name.setText(intentList.get(position).getName());
        if (intentList.get(position).getIcon() != null) {
            holder.icon.setImageDrawable(intentList.get(position).getIcon());
        } else if (intentList.get(position).getIconId() != 0) {
            holder.icon.setImageResource(intentList.get(position).getIconId());
        }
    }

    @Override
    public int getItemCount() {
        return intentList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;
        private final ImageView icon;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            icon = itemView.findViewById(R.id.icon);
        }
    }
}