package de.storchp.opentracks.osmplugin.dashboardapi;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.mapsforge.core.model.LatLong;

import java.util.ArrayList;
import java.util.List;

import de.storchp.opentracks.osmplugin.utils.LatLongUtils;

public class TrackPoint {

    public static final String _ID = "_id";
    public static final String TRACKID = "trackid";
    public static final String LONGITUDE = "longitude";
    public static final String LATITUDE = "latitude";
    public static final String TIME = "time";

    public static final double PAUSE_LATITUDE = 100.0;

    public static final String[] PROJECTION = {
            _ID,
            TRACKID,
            LATITUDE,
            LONGITUDE,
            TIME
    };

    private final long trackPointId;
    private final long trackId;
    private final LatLong latLong;
    private final boolean pause;

    public TrackPoint(final long trackId, final long trackPointId, final double latitude, final double longitude) {
        this.trackId = trackId;
        this.trackPointId = trackPointId;
        if (LatLongUtils.isValid(latitude, longitude)) {
            this.latLong = new LatLong(latitude, longitude);
        } else {
            latLong = null;
        }
        this.pause = latitude == PAUSE_LATITUDE;
    }

    public boolean hasValidLocation() {
        return latLong != null && !isPause();
    }

    public boolean isPause() {
        return pause;
    }

    /**
     * Reads the TrackPoints from the Content Uri and split by segments.
     * Pause TrackPoints and different Track IDs split the segments.
     */
    public static List<List<TrackPoint>> readTrackPointsBySegments(final ContentResolver resolver, final Uri data, final long lastTrackPointId) {
        final List<List<TrackPoint>> segments = new ArrayList<>();
        try (final Cursor cursor = resolver.query(data, TrackPoint.PROJECTION, null, null, null)) {
            TrackPoint lastTrackPoint = null;
            List<TrackPoint> segment = null;
            while (cursor.moveToNext()) {
                final long trackPointId = cursor.getLong(cursor.getColumnIndex(TrackPoint._ID));
                if (lastTrackPointId > 0 && lastTrackPointId >= trackPointId) { // skip trackpoints we already have
                    continue;
                }
                final long trackId = cursor.getLong(cursor.getColumnIndex(TrackPoint.TRACKID));
                final double latitude = cursor.getInt(cursor.getColumnIndex(TrackPoint.LATITUDE)) / 1E6;
                final double longitude = cursor.getInt(cursor.getColumnIndex(TrackPoint.LONGITUDE)) / 1E6;

                if (lastTrackPoint == null || lastTrackPoint.trackId != trackId) {
                    segment = new ArrayList<>();
                    segments.add(segment);
                }

                lastTrackPoint = new TrackPoint(trackId, trackPointId, latitude, longitude);
                if (lastTrackPoint.hasValidLocation()) {
                    segment.add(lastTrackPoint);
                }
                if (lastTrackPoint.isPause()) {
                    lastTrackPoint = null;
                }
            }
        }
        return segments;
    }

    public long getTrackPointId() {
        return trackPointId;
    }

    public long getTrackId() {
        return trackId;
    }

    public LatLong getLatLong() {
        return latLong;
    }
}
