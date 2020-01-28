package de.storchp.opentracks.osmplugin;

import android.net.Uri;

import java.util.ArrayList;

public class Constants {

    // NOTE: Needs to be used in AndroidManifest.xml!
    public static final String ACTION_DASHBOARD = "Intent.OpenTracks-Dashboard";

    public static final String ACTION_DASHBOARD_PAYLOAD = ACTION_DASHBOARD + ".Payload";

    public static class Trackpoints {
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
    }

    // Track columns
    public static class Track {
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
    }

    /**
     * Checks if a given location is a valid (i.e. physically possible) location on Earth.
     * Note: The special separator locations (which have latitude = 100) will not qualify as valid.
     * Neither will locations with lat=0 and lng=0 as these are most likely "bad" measurements which often cause trouble.
     *
     * @return true if the location is a valid location.
     */
    public static boolean isValidLocation(double latitude, double longitude) {
        return Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180 && longitude != Trackpoints.PAUSE_LATITUDE;
    }

    public static Uri getTracksUri(ArrayList<Uri> uris) {
        return uris.get(0);
    }

    public static Uri getTrackPointsUri(ArrayList<Uri> uris) {
        return uris.get(1);
    }

}
