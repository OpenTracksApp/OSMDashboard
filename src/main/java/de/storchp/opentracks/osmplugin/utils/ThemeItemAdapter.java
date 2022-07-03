package de.storchp.opentracks.osmplugin.utils;

import android.app.Activity;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;

import androidx.annotation.NonNull;

import java.util.List;

import de.storchp.opentracks.osmplugin.R;
import de.storchp.opentracks.osmplugin.databinding.ThemeItemBinding;

public class ThemeItemAdapter extends ArrayAdapter<FileItem> {

    private final Activity context;
    private final List<FileItem> items;
    private Uri selected;
    private final boolean onlineMapSelected;

    public ThemeItemAdapter(@NonNull Activity context, List<FileItem> items, Uri selected, boolean onlineMapSelected) {
        super(context, R.layout.map_item, items);
        this.context = context;
        this.items = items;
        this.selected = selected;
        this.onlineMapSelected = onlineMapSelected;
    }

    public boolean areAllItemsEnabled() {
        return false;
    }

    public boolean isEnabled(int position) {
        return !onlineMapSelected || position == 0;
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        var rowView = convertView;
        // reuse views
        if (rowView == null) {
            var binding = ThemeItemBinding.inflate(context.getLayoutInflater(), parent, false);
            rowView = binding.getRoot();
            rowView.setTag(binding);
        }

        // fill data
        var binding = (ThemeItemBinding) rowView.getTag();
        var item = this.items.get(position);
        binding.name.setText(item.getName());
        binding.name.setEnabled(isEnabled(position));
        binding.radiobutton.setChecked(position == 0 ? selected == null : item.getUri() != null && item.getUri().equals(selected));
        binding.radiobutton.setOnClickListener(onStateChangedListener(binding.radiobutton, position));
        binding.radiobutton.setEnabled(isEnabled(position));

        return rowView;
    }

    private View.OnClickListener onStateChangedListener(RadioButton radioButton, int position) {
        return v -> {
            var fileItem = items.get(position);
            if (radioButton.isChecked()) {
                if (fileItem.getUri() == null) { // default theme
                    selected = null;
                } else {
                    selected = fileItem.getUri();
                }
            } else {
                if (fileItem.getUri() != null) { // offline theme
                    selected = null;
                }
            }
            notifyDataSetChanged();
        };
    }

    public Uri getSelectedUri() {
        return selected;
    }

    public void setSelectedUri(Uri selected) {
        this.selected = selected;
    }

}
