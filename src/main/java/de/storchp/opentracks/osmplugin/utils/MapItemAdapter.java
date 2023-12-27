package de.storchp.opentracks.osmplugin.utils;

import android.app.Activity;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Set;

import de.storchp.opentracks.osmplugin.BuildConfig;
import de.storchp.opentracks.osmplugin.R;
import de.storchp.opentracks.osmplugin.databinding.MapItemBinding;

public class MapItemAdapter extends ArrayAdapter<FileItem> {

    private final Activity context;
    private final List<FileItem> items;
    private final Set<Uri> selected;

    public MapItemAdapter(@NonNull Activity context, List<FileItem> items, Set<Uri> selected) {
        super(context, R.layout.map_item, items);
        this.context = context;
        this.items = items;
        this.selected = selected;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        var rowView = convertView;
        // reuse views
        if (rowView == null) {
            var binding = MapItemBinding.inflate(context.getLayoutInflater(), parent, false);
            rowView = binding.getRoot();
            rowView.setTag(binding);
        }

        // fill data
        var binding = (MapItemBinding) rowView.getTag();
        var item = this.items.get(position);
        binding.name.setText(item.getName());
        binding.checkbox.setChecked(position == 0 && !BuildConfig.offline ? selected.isEmpty() : selected.contains(item.getUri()));
        binding.checkbox.setOnClickListener(onStateChangedListener(binding.checkbox, position));

        return rowView;
    }

    private View.OnClickListener onStateChangedListener(CheckBox checkBox, int position) {
        return v -> {
            var fileItem = items.get(position);
            if (checkBox.isChecked()) {
                if (fileItem.getUri() == null) { // online map
                    selected.clear();
                } else {
                    selected.add(fileItem.getUri());
                }
            } else {
                if (fileItem.getUri() != null) { // offline map
                    selected.remove(fileItem.getUri());
                }
            }
            notifyDataSetChanged();
        };
    }

    public Set<Uri> getSelectedUris() {
        return selected;
    }

}
