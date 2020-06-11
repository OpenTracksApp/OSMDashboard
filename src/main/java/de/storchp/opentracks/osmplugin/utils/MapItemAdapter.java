package de.storchp.opentracks.osmplugin.utils;

import android.app.Activity;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Set;

import de.storchp.opentracks.osmplugin.R;

public class MapItemAdapter extends ArrayAdapter<FileItem> {

    private final Activity context;
    private final List<FileItem> items;
    private final Set<Uri> selected;

    public MapItemAdapter(@NonNull final Activity context, final List<FileItem> items, final Set<Uri> selected) {
        super(context, R.layout.map_item, items);
        this.context = context;
        this.items = items;
        this.selected = selected;
    }

    @Override
    public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
        View rowView = convertView;
        // reuse views
        if (rowView == null) {
            final LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.map_item, parent, false);

            // configure view holder
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.checkBox = rowView.findViewById(R.id.checkbox);
            viewHolder.name = rowView.findViewById(R.id.name);
            rowView.setTag(viewHolder);
        }

        // fill data
        final ViewHolder holder = (ViewHolder) rowView.getTag();
        final FileItem item = this.items.get(position);
        holder.name.setText(item.getName());
        holder.checkBox.setChecked(position == 0 ? selected.isEmpty() : selected.contains(item.getUri()));
        holder.checkBox.setOnClickListener(onStateChangedListener(holder.checkBox, position));

        return rowView;
    }

    private View.OnClickListener onStateChangedListener(final CheckBox checkBox, final int position) {
        return v -> {
            final FileItem fileItem = items.get(position);
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

    public static class ViewHolder {
        public CheckBox checkBox;
        public TextView name;
    }

}
