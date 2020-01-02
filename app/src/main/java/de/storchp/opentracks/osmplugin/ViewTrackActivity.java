package de.storchp.opentracks.osmplugin;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Shows a simple list of the track points for debugging only
 */
public class ViewTrackActivity extends AppCompatActivity {

    private static final String TAG = ViewTrackActivity.class.getSimpleName();

    private TrackCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_track);

        // Get the intent that started this activity
        Intent intent = getIntent();
        final Uri data = intent.getData();
        readData(data);

        getContentResolver().registerContentObserver(data, true, new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                readData(data);
            }
        });
    }

    private void readData(Uri data) {
        // A "projection" defines the columns that will be returned for each row
        String[] projection =
                {
                        Constants.Trackpoints._ID,
                        Constants.Trackpoints.LATITUDE,
                        Constants.Trackpoints.LONGITUDE,
                        Constants.Trackpoints.TIME
                };

        Log.i(TAG, "Loading track from " + data);

        // Does a query against the table and returns a Cursor object
        final Cursor mCursor = getContentResolver().query(
                data,
                projection,
                null,
                null,
                null);

        if (adapter == null) {
            adapter = new TrackCursorAdapter(this, mCursor, 0);
            ListView listView = findViewById(R.id.list_track_points);
            listView.setAdapter(adapter);
        } else {
            adapter.swapCursor(mCursor);
        }
    }

}
