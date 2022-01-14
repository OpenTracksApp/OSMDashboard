package de.storchp.opentracks.osmplugin.dashboardapi;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Track {

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

    private final long id;
    private final String trackname;
    private final String description;
    private final String category;
    private final int startTimeEpochMillis;
    private final int stopTimeEpochMillis;
    private final float totalDistanceMeter;
    private final int totalTimeMillis;
    private final int movingTimeMillis;
    private final float avgSpeedMeterPerSecond;
    private final float avgMovingSpeedMeterPerSecond;
    private final float maxSpeedMeterPerSecond;
    private final float minElevationMeter;
    private final float maxElevationMeter;
    private final float elevationGainMeter;

    public Track(final long id, final String trackname, final String description, final String category, final int startTimeEpochMillis, final int stopTimeEpochMillis, final float totalDistanceMeter, final int totalTimeMillis, final int movingTimeMillis, final float avgSpeedMeterPerSecond, final float avgMovingSpeedMeterPerSecond, final float maxSpeedMeterPerSecond, final float minElevationMeter, final float maxElevationMeter, final float elevationGainMeter) {
        this.id = id;
        this.trackname = trackname;
        this.description = description;
        this.category = category;
        this.startTimeEpochMillis = startTimeEpochMillis;
        this.stopTimeEpochMillis = stopTimeEpochMillis;
        this.totalDistanceMeter = totalDistanceMeter;
        this.totalTimeMillis = totalTimeMillis;
        this.movingTimeMillis = movingTimeMillis;
        this.avgSpeedMeterPerSecond = avgSpeedMeterPerSecond;
        this.avgMovingSpeedMeterPerSecond = avgMovingSpeedMeterPerSecond;
        this.maxSpeedMeterPerSecond = maxSpeedMeterPerSecond;
        this.minElevationMeter = minElevationMeter;
        this.maxElevationMeter = maxElevationMeter;
        this.elevationGainMeter = elevationGainMeter;
    }

    /**
     * Reads the Tracks from the Content Uri
     */
    public static List<Track> readTracks(final ContentResolver resolver, final Uri data, final int protocolVersion) {
        Log.i(TAG, "Loading track(s) from " + data);

        final var tracks = new ArrayList<Track>();
        try (final Cursor cursor = resolver.query(data, Track.PROJECTION, null, null, null)) {
            while (cursor.moveToNext()) {
                final var id = cursor.getLong(cursor.getColumnIndexOrThrow(Track._ID));
                final var trackname = cursor.getString(cursor.getColumnIndexOrThrow(Track.NAME));
                final var description = cursor.getString(cursor.getColumnIndexOrThrow(Track.DESCRIPTION));
                final var category = cursor.getString(cursor.getColumnIndexOrThrow(Track.CATEGORY));
                final var startTimeEpochMillis = cursor.getInt(cursor.getColumnIndexOrThrow(Track.STARTTIME));
                final var stopTimeEpochMillis = cursor.getInt(cursor.getColumnIndexOrThrow(Track.STOPTIME));
                final var totalDistanceMeter = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.TOTALDISTANCE));
                final var totalTimeMillis = cursor.getInt(cursor.getColumnIndexOrThrow(Track.TOTALTIME));
                final var movingTimeMillis = cursor.getInt(cursor.getColumnIndexOrThrow(Track.MOVINGTIME));
                final var avgSpeedMeterPerSecond = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.AVGSPEED));
                final var avgMovingSpeedMeterPerSecond = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.AVGMOVINGSPEED));
                final var maxSpeedMeterPerSecond = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.MAXSPEED));
                final var minElevationMeter = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.MINELEVATION));
                final var maxElevationMeter = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.MAXELEVATION));
                final var elevationGainMeter = cursor.getFloat(cursor.getColumnIndexOrThrow(Track.ELEVATIONGAIN));

                tracks.add(new Track(id, trackname, description, category, startTimeEpochMillis, stopTimeEpochMillis,
                        totalDistanceMeter, totalTimeMillis, movingTimeMillis, avgSpeedMeterPerSecond, avgMovingSpeedMeterPerSecond, maxSpeedMeterPerSecond,
                        minElevationMeter, maxElevationMeter, elevationGainMeter));
            }
        } catch (final SecurityException e) {
            Log.w(TAG, "No permission to read track");
        } catch (final Exception e) {
            Log.e(TAG, "Reading track failed", e);
        }
        return tracks;
    }

    public float getElevationGainMeter() {
        return elevationGainMeter;
    }

    public float getMaxElevationMeter() {
        return maxElevationMeter;
    }

    public float getMinElevationMeter() {
        return minElevationMeter;
    }

    public float getMaxSpeedMeterPerSecond() {
        return maxSpeedMeterPerSecond;
    }

    public float getAvgMovingSpeedMeterPerSecond() {
        return avgMovingSpeedMeterPerSecond;
    }

    public float getAvgSpeedMeterPerSecond() {
        return avgSpeedMeterPerSecond;
    }

    public int getMovingTimeMillis() {
        return movingTimeMillis;
    }

    public int getTotalTimeMillis() {
        return totalTimeMillis;
    }

    public float getTotalDistanceMeter() {
        return totalDistanceMeter;
    }

    public int getStopTimeEpochMillis() {
        return stopTimeEpochMillis;
    }

    public int getStartTimeEpochMillis() {
        return startTimeEpochMillis;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getTrackname() {
        return trackname;
    }

    public long getId() {
        return id;
    }
}
