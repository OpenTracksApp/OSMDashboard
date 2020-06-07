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

public class FileItemAdapter extends ArrayAdapter<FileItem> {

    private final Activity context;
    private final List<FileItem> items;
    private final Set<Uri> selected;

    public FileItemAdapter(@NonNull final Activity context, final List<FileItem> items, final Set<Uri> selected) {
        super(context, R.layout.file_item, items);
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
            rowView = inflater.inflate(R.layout.file_item, parent, false);

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
        holder.checkBox.setChecked(selected.contains(item.getUri()));
        holder.checkBox.setOnClickListener(onStateChangedListener(holder.checkBox, position));

        return rowView;
    }

    private View.OnClickListener onStateChangedListener(final CheckBox checkBox, final int position) {
        return new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final FileItem fileItem = items.get(position);
                if (checkBox.isChecked()) {
                    selected.add(fileItem.getUri());
                } else {
                    selected.remove(fileItem.getUri());
                }
            }
        };
    }

    public Set<Uri> getSelectedUris() {
        return selected;
    }

    private static class ViewHolder {
        public CheckBox checkBox;
        public TextView name;
    }

}
