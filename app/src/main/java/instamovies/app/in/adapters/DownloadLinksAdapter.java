package instamovies.app.in.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import instamovies.app.in.R;
import instamovies.app.in.models.DownloadLinksModel;

public class DownloadLinksAdapter extends BaseAdapter {

    private final ArrayList<DownloadLinksModel> downloadLinksModels;

    public DownloadLinksAdapter(ArrayList<DownloadLinksModel> modelArrayList) {
        downloadLinksModels = modelArrayList;
    }

    @Override
    public int getCount() {
        return downloadLinksModels.size();
    }

    @Override
    public Object getItem(int position) {
        return downloadLinksModels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_download_options, parent, false);
        }

        TextView title = convertView.findViewById(R.id.title);
        TextView subTitle = convertView.findViewById(R.id.sub_title);

        title.setText(downloadLinksModels.get(position).getTitle());
        subTitle.setText(downloadLinksModels.get(position).getSubTitle());
        return convertView;
    }
}