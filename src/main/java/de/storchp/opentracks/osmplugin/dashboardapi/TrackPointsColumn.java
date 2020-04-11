package de.storchp.opentracks.osmplugin.dashboardapi;

public class TrackPointsColumn {

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

    /**
     * Checks if a given location is a valid (i.e. physically possible) location on Earth.
     * Note: The special separator locations (which have latitude = 100) will not qualify as valid.
     * Neither will locations with lat=0 and lng=0 as these are most likely "bad" measurements which often cause trouble.
     *
     * @return true if the location is a valid location.
     */
    public static boolean isValidLocation(double latitude, double longitude) {
        return Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180 && longitude != PAUSE_LATITUDE;
    }
}
