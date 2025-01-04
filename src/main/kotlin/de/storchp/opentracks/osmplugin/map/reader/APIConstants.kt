package de.storchp.opentracks.osmplugin.map.reader

import android.net.Uri

object APIConstants {
    const val LAT_LON_FACTOR = 1E6

    // NOTE: Needs to be used in AndroidManifest.xml!
    const val ACTION_DASHBOARD = "Intent.OpenTracks-Dashboard"

    const val ACTION_DASHBOARD_PAYLOAD = "$ACTION_DASHBOARD.Payload"

    fun getTracksUri(uris: List<Uri>) = uris[0]

    fun getTrackpointsUri(uris: List<Uri>) = uris[1]

    /**
     * Waypoints are only available in newer versions of OpenTracks.
     */
    fun getWaypointsUri(uris: List<Uri>) = uris.getOrNull(2)
}
