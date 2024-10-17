package de.storchp.opentracks.osmplugin.dashboardapi

import android.net.Uri
import java.util.ArrayList

object APIConstants {
    const val LAT_LON_FACTOR = 1E6

    // NOTE: Needs to be used in AndroidManifest.xml!
    const val ACTION_DASHBOARD = "Intent.OpenTracks-Dashboard"

    const val ACTION_DASHBOARD_PAYLOAD = "$ACTION_DASHBOARD.Payload"

    fun getTracksUri(uris: ArrayList<Uri>) = uris[0]

    fun getTrackpointsUri(uris: ArrayList<Uri>) = uris[1]

    /**
     * Waypoints are only available in newer versions of OpenTracks.
     */
    fun getWaypointsUri(uris: ArrayList<Uri>) = uris.getOrNull(2)
}
