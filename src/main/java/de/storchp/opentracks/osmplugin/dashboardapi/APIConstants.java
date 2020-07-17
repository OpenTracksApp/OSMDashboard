package de.storchp.opentracks.osmplugin.dashboardapi;

import android.net.Uri;

import java.util.ArrayList;

public class APIConstants {

    // NOTE: Needs to be used in AndroidManifest.xml!
    public static final String ACTION_DASHBOARD = "Intent.OpenTracks-Dashboard";

    public static final String ACTION_DASHBOARD_PAYLOAD = ACTION_DASHBOARD + ".Payload";


    public static Uri getTracksUri(final ArrayList<Uri> uris) {
        return uris.get(0);
    }

    public static Uri getTrackPointsUri(final ArrayList<Uri> uris) {
        return uris.get(1);
    }

    /**
     * Waypoints are only available in newer versions of OpenTracks.
     */
    public static Uri getWaypointsUri(final ArrayList<Uri> uris) {
        return uris.size() > 2 ? uris.get(2) : null;
    }

}
