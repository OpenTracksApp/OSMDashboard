package de.storchp.opentracks.osmplugin;

import android.content.Context;
import android.database.Cursor;
import android.provider.SyncStateContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;

public class TrackCursorAdapter extends CursorAdapter {

    private final LayoutInflater mInflater;

    public TrackCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.item, parent, false);
        ViewHolder holder = new ViewHolder();
        holder.coords = view.findViewById(R.id.coords);
        holder.time = view.findViewById(R.id.time);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.coords.setText(cursor.getString(cursor.getColumnIndex(ViewTrackActivity.LATITUDE)) + "," + cursor.getString(cursor.getColumnIndex(ViewTrackActivity.LONGITUDE)));
        holder.time.setText(new Date(cursor.getLong(cursor.getColumnIndex(ViewTrackActivity.TIME))).toString());
    }

    static class ViewHolder {
        TextView coords;
        TextView time;
    }

}
