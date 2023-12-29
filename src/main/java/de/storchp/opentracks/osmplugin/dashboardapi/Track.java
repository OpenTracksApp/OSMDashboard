package de.storchp.opentracks.osmplugin.dashboardapi;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public record Track(long id, String trackname, String description, String category,
                    int startTimeEpochMillis, int stopTimeEpochMillis, float totalDistanceMeter,
                    int totalTimeMillis, int movingTimeMillis, float avgSpeedMeterPerSecond,
                    float avgMovingSpeedMeterPerSecond, float maxSpeedMeterPerSecond,
                    float minElevationMeter, float maxElevationMeter, float elevationGainMeter) {

    private static final String TAG = Track.class.getSimpleName();

    public static final String _ID = "_id";
    public static final String NAME = "name"; // track name
    public static final String DESCRIPTION = "description"; // track description
    public static final String CATEGORY = "category"; // track activity type
    public static final String STARTTIME = "starttime"; // track start time
    public static final String STOPTIME = "stoptime"; // track stop time
    public static final String TOTALDISTANCE = "totaldistance"; // total distance
    public static final String TOTALTIME = "totaltime"; // total time
    public static final String MOVINGTIME = "movingtime"; // moving time
    public static final String AVGSPEED = "avgspeed"; // average speed
    public static final String AVGMOVINGSPEED = "avgmovingspeed"; // average moving speed
    public static final String MAXSPEED = "maxspeed"; // maximum speed
    public static final String MINELEVATION = "minelevation"; // minimum elevation
    public static final String MAXELEVATION = "maxelevation"; // maximum elevation
    public static final String ELEVATIONGAIN = "elevationgain"; // elevation gain

    public static final String[] PROJECTION = {
            _ID,
            NAME,
            DESCRIPTION,
            CATEGORY,
            STARTTIME,
            STOPTIME,
            TOTALDISTANCE,
            TOTALTIME,
            MOVINGTIME,
            AVGSPEED,
            AVGMOVINGSPEED,
            MAXSPEED,
            MINELEVATION,
            MAXELEVATION,
            ELEVATIONGAIN
    };

    /**
     * Reads the Tracks from the Content Uri
     */
    public static List<Track> readTracks(ContentResolver resolver, Uri data) {
        Log.i(TAG, "Loading track(s) from " + data);

        var tracks = new ArrayList<Track>();
        try (Cursor cursor = resolver.query(data, Track.PROJECTION, null, null, null)) {
            while (cursor.moveToNext()) {
                var id = cursor.getLong(cursor.getColumnIndexOrThrow(Track._ID));
                var trackname = cursor.getString(cursor.getColumnIndexOrThrow(Track.NAME));
                var description = cursor.getString(cursor.getColumnIndexOrThrow(Track.DESCRIPTION));
                var category = cursor.getString(cursor.getColumnIndexOrThrow(Track.CATEGORY));
                var startTimeEpochMillis = cursor.getInt(cursor.getColumnIndexOrThrow(Track.STARTTIME));
                var stopTimeEpochMillis = cursor.getInt(cursor.getColumnIndexOrThrow(Track.STOPTIME));
                var totalDistanceMeter = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.TOTALDISTANCE));
                var totalTimeMillis = cursor.getInt(cursor.getColumnIndexOrThrow(Track.TOTALTIME));
                var movingTimeMillis = cursor.getInt(cursor.getColumnIndexOrThrow(Track.MOVINGTIME));
                var avgSpeedMeterPerSecond = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.AVGSPEED));
                var avgMovingSpeedMeterPerSecond = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.AVGMOVINGSPEED));
                var maxSpeedMeterPerSecond = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.MAXSPEED));
                var minElevationMeter = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.MINELEVATION));
                var maxElevationMeter = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.MAXELEVATION));
                var elevationGainMeter = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.ELEVATIONGAIN));

                tracks.add(new Track(id, trackname, description, category, startTimeEpochMillis, stopTimeEpochMillis,
                        totalDistanceMeter, totalTimeMillis, movingTimeMillis, avgSpeedMeterPerSecond, avgMovingSpeedMeterPerSecond, maxSpeedMeterPerSecond,
                        minElevationMeter, maxElevationMeter, elevationGainMeter));
            }
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to read track");
        } catch (Exception e) {
            Log.e(TAG, "Reading track failed", e);
        }
        return tracks;
    }
}
