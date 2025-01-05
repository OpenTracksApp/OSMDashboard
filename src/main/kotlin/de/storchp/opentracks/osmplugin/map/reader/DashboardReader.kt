package de.storchp.opentracks.osmplugin.map.reader

import android.content.ContentResolver
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.util.Log
import de.storchp.opentracks.osmplugin.map.MapData
import de.storchp.opentracks.osmplugin.map.TrackStatistics
import de.storchp.opentracks.osmplugin.map.model.TrackpointsBySegments
import de.storchp.opentracks.osmplugin.utils.TrackpointsDebug

private val TAG: String = DashboardReader::class.java.getSimpleName()

private const val EXTRAS_PROTOCOL_VERSION = "PROTOCOL_VERSION"
private const val EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK =
    "EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK"
private const val EXTRAS_SHOULD_KEEP_SCREEN_ON = "EXTRAS_SHOULD_KEEP_SCREEN_ON"
private const val EXTRAS_SHOW_WHEN_LOCKED = "EXTRAS_SHOULD_KEEP_SCREEN_ON"
private const val EXTRAS_SHOW_FULLSCREEN = "EXTRAS_SHOULD_FULLSCREEN"

typealias UpdateTrackStatistics = (TrackStatistics?) -> Unit
typealias UpdateTrackpointsDebug = (TrackpointsDebug) -> Unit

class DashboardReader(
    intent: Intent,
    private val contentResolver: ContentResolver,
    mapData: MapData,
    updateTrackStatistics: UpdateTrackStatistics,
    updateTrackpointsDebug: UpdateTrackpointsDebug,
) : MapDataReader(mapData, updateTrackStatistics, updateTrackpointsDebug) {

    private var trackpointsDebug = TrackpointsDebug()
    private var lastWaypointId: Long? = null
    private var lastTrackPointId: Long? = null
    private var contentObserver: OpenTracksContentObserver? = null
    val tracksUri: Uri
    val trackpointsUri: Uri
    val waypointsUri: Uri?
    val protocolVersion: Int

    init {
        require(intent.isDashboardAction())
        val uris =
            intent.getParcelableArrayListExtra<Uri>(APIConstants.ACTION_DASHBOARD_PAYLOAD)!!
        protocolVersion = intent.getIntExtra(EXTRAS_PROTOCOL_VERSION, 1)
        tracksUri = APIConstants.getTracksUri(uris)
        trackpointsUri = APIConstants.getTrackpointsUri(uris)
        waypointsUri = APIConstants.getWaypointsUri(uris)
        keepScreenOn = intent.getBooleanExtra(EXTRAS_SHOULD_KEEP_SCREEN_ON, false)
        showOnLockScreen = intent.getBooleanExtra(EXTRAS_SHOW_WHEN_LOCKED, false)
        showFullscreen = intent.getBooleanExtra(EXTRAS_SHOW_FULLSCREEN, false)
        isOpenTracksRecordingThisTrack =
            intent.getBooleanExtra(EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK, false)

        trackpointsDebug.protocolVersion = protocolVersion
        readTrackpoints(trackpointsUri, false, protocolVersion)
        readTracks(tracksUri)
        waypointsUri?.let { readWaypoints(it) }
    }

    fun readTrackpoints(data: Uri, update: Boolean, protocolVersion: Int) {
        Log.i(TAG, "Loading trackpoints from $data")
        val trackpointsBySegments: TrackpointsBySegments =
            TrackpointReader.readTrackpointsBySegments(
                contentResolver,
                data,
                lastTrackPointId,
                protocolVersion
            )
        if (trackpointsBySegments.isEmpty()) {
            Log.d(TAG, "No new trackpoints received")
            return
        }
        if (trackpointsBySegments.isNotEmpty() && trackpointsBySegments.last().isNotEmpty()) {
            lastTrackPointId = trackpointsBySegments.last().last().id
        }
        readTrackpoints(trackpointsBySegments, update)
    }

    private fun readWaypoints(data: Uri) {
        val waypoints = WaypointReader.readWaypoints(contentResolver, data, lastWaypointId)
        if (waypoints.isNotEmpty()) {
            lastWaypointId = waypoints.last().id
        }
        readWaypoints(waypoints)
    }

    private fun readTracks(data: Uri) {
        readTracks(TrackReader.readTracks(contentResolver, data))
    }

    override fun startContentObserver() {
        contentObserver = OpenTracksContentObserver(
            tracksUri,
            trackpointsUri,
            waypointsUri,
            protocolVersion
        )

        contentResolver.registerContentObserver(tracksUri, false, contentObserver!!)
        contentResolver.registerContentObserver(
            trackpointsUri,
            false,
            contentObserver!!
        )
        if (waypointsUri != null) {
            contentResolver.registerContentObserver(
                waypointsUri,
                false,
                contentObserver!!
            )
        }

    }

    override fun unregisterContentObserver() {
        contentObserver?.let { contentResolver.unregisterContentObserver(it) }
        contentObserver = null
    }

    private inner class OpenTracksContentObserver(
        private val tracksUri: Uri,
        private val trackpointsUri: Uri,
        private val waypointsUri: Uri?,
        private val protocolVersion: Int
    ) : ContentObserver(Handler()) {

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (uri == null) {
                return  // nothing can be done without an uri
            }
            if (tracksUri.toString().startsWith(uri.toString())) {
                readTracks(tracksUri)
            } else if (trackpointsUri.toString().startsWith(uri.toString())) {
                readTrackpoints(trackpointsUri, true, protocolVersion)
            } else if (waypointsUri?.toString()?.startsWith(uri.toString()) == true) {
                readWaypoints(waypointsUri)
            }
        }
    }

}

fun Intent.isDashboardAction(): Boolean {
    return APIConstants.ACTION_DASHBOARD == action
}