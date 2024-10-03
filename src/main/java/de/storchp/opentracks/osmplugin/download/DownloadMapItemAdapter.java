package de.storchp.opentracks.osmplugin.download;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.List;

import de.storchp.opentracks.osmplugin.R;
import de.storchp.opentracks.osmplugin.databinding.DownloadItemBinding;

public class DownloadMapItemAdapter extends ArrayAdapter<DownloadMapItem> {

    private final Activity context;
    private final List<DownloadMapItem> items;

    public DownloadMapItemAdapter(@NonNull Activity context, List<DownloadMapItem> items) {
        super(context, R.layout.map_item, items);
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        var rowView = convertView;
        // reuse views
        if (rowView == null) {
            var binding = DownloadItemBinding.inflate(context.getLayoutInflater(), parent, false);
            rowView = binding.getRoot();
            rowView.setTag(binding);
        }

        // fill data
        var binding = (DownloadItemBinding) rowView.getTag();
        var item = this.items.get(position);
        binding.name.setText(item.name());
        binding.date.setText(item.date());
        binding.size.setText(item.size());
        binding.mapIcon.setImageDrawable(ContextCompat.getDrawable(context, item.downloadItemType().getIconResId()));

        return rowView;
    }

}
