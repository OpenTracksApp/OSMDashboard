package de.storchp.opentracks.osmplugin.dashboardapi;

import android.net.Uri;

import java.util.ArrayList;

public class APIConstants {

    // NOTE: Needs to be used in AndroidManifest.xml!
    public static final String ACTION_DASHBOARD = "Intent.OpenTracks-Dashboard";

    public static final String ACTION_DASHBOARD_PAYLOAD = ACTION_DASHBOARD + ".Payload";


    public static Uri getTracksUri(ArrayList<Uri> uris) {
        return uris.get(0);
    }

    public static Uri getTrackPointsUri(ArrayList<Uri> uris) {
        return uris.get(1);
    }

}
