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
        final long trackid = intent.getExtras().getLong(Constants.TRACKID);
        readData(data, trackid);

        getContentResolver().registerContentObserver(data, true, new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                readData(data, trackid);
            }
        });

    }

    private void readData(Uri data, long trackId) {
        // A "projection" defines the columns that will be returned for each row
        String[] projection =
                {
                        Constants._ID,
                        Constants.TRACKID,
                        Constants.LATITUDE,
                        Constants.LONGITUDE,
                        Constants.TIME
                };

        // Defines a string to contain the selection clause
        String selectionClause = Constants.TRACKID + " = ?";

        // Initializes an array to contain selection arguments
        String[] selectionArgs = {String.valueOf(trackId)};

        Log.i(TAG, "Loading track for " + trackId);

        // Does a query against the table and returns a Cursor object
        final Cursor mCursor = getContentResolver().query(
                data,
                projection,
                selectionClause,
                selectionArgs,
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
