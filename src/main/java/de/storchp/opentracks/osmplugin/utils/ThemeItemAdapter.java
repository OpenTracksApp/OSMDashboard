package de.storchp.opentracks.osmplugin.utils;

import android.app.Activity;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Set;

import de.storchp.opentracks.osmplugin.R;

public class ThemeItemAdapter extends ArrayAdapter<FileItem> {

    private final Activity context;
    private final List<FileItem> items;
    private Uri selected;

    public ThemeItemAdapter(@NonNull final Activity context, final List<FileItem> items, final Uri selected) {
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
            rowView = inflater.inflate(R.layout.theme_item, parent, false);

            // configure view holder
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.radioButton = rowView.findViewById(R.id.radiobutton);
            viewHolder.name = rowView.findViewById(R.id.name);
            rowView.setTag(viewHolder);
        }

        // fill data
        final ViewHolder holder = (ViewHolder) rowView.getTag();
        final FileItem item = this.items.get(position);
        holder.name.setText(item.getName());
        holder.radioButton.setChecked(position == 0 ? selected == null : item.getUri() != null && item.getUri().equals(selected));
        holder.radioButton.setOnClickListener(onStateChangedListener(holder.radioButton, position));

        return rowView;
    }

    private View.OnClickListener onStateChangedListener(final RadioButton radioButton, final int position) {
        return new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final FileItem fileItem = items.get(position);
                if (radioButton.isChecked()) {
                    if (fileItem.getUri() == null) { // online map
                        selected = null;
                    } else {
                        selected = fileItem.getUri();
                    }
                } else {
                    if (fileItem.getUri() != null) { // offline map
                        selected = null;
                    }
                }
                notifyDataSetChanged();
            }
        };
    }

    public Uri getSelectedUri() {
        return selected;
    }

    public void setSelectedUri(final Uri selected) {
        this.selected = selected;
    }

    public static class ViewHolder {
        public RadioButton radioButton;
        public TextView name;
    }

}
