package de.storchp.opentracks.osmplugin.dashboardapi

import android.content.ContentResolver
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.util.Log
import de.storchp.opentracks.osmplugin.map.DEFAULT_TRACK_COLOR_MORE
import de.storchp.opentracks.osmplugin.map.MapData
import de.storchp.opentracks.osmplugin.map.MapUtils
import de.storchp.opentracks.osmplugin.map.StyleColorCreator
import de.storchp.opentracks.osmplugin.map.TrackColorMode
import de.storchp.opentracks.osmplugin.map.TrackStatistics
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils
import de.storchp.opentracks.osmplugin.utils.TrackpointsDebug
import org.oscim.core.GeoPoint
import java.lang.Exception

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
    private val mapData: MapData,
    private val updateTrackStatistics: UpdateTrackStatistics,
    private val updateTrackpointsDebug: UpdateTrackpointsDebug,
) {

    private val colorCreator = StyleColorCreator()
    private var trackColor = colorCreator.nextColor()
    private var trackpointsDebug = TrackpointsDebug()
    private var lastWaypointId = 0L
    private var lastTrackPointId = 0L
    private var contentObserver: OpenTracksContentObserver? = null
    var lastTrackId = 0L
        private set
    val keepScreenOn: Boolean
    val showOnLockScreen: Boolean
    val showFullscreen: Boolean
    val isOpenTracksRecordingThisTrack: Boolean
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

        val showPauseMarkers = PreferencesUtils.isShowPauseMarkers()
        val latLongs = mutableListOf<GeoPoint>()
        val tolerance = PreferencesUtils.getTrackSmoothingTolerance()

        try {
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

            val average = trackpointsBySegments.calcAverageSpeed()
            val maxSpeed = trackpointsBySegments.calcMaxSpeed()
            val averageToMaxSpeed = maxSpeed - average
            var trackColorMode = PreferencesUtils.getTrackColorMode()
            if (isOpenTracksRecordingThisTrack && !trackColorMode.supportsLiveTrack) {
                trackColorMode = DEFAULT_TRACK_COLOR_MORE
            }

            trackpointsBySegments.segments.map { trackpoints ->
                if (!update) {
                    mapData.cutPolyline()
                    if (tolerance > 0) { // smooth track
                        return@map MapUtils.decimate(tolerance, trackpoints)
                    }
                }
                return@map trackpoints
            }.forEach { trackpoints ->
                trackpoints.filter { it.latLong != null }.forEach { trackpoint ->
                    lastTrackPointId = trackpoint.id

                    if (trackpoint.trackId != lastTrackId) {
                        if (trackColorMode == TrackColorMode.BY_TRACK) {
                            trackColor = colorCreator.nextColor()
                        }
                        lastTrackId = trackpoint.trackId
                        mapData.resetCurrentPolyline()
                    }

                    if (trackColorMode == TrackColorMode.BY_SPEED) {
                        trackColor = MapUtils.getTrackColorBySpeed(
                            average,
                            averageToMaxSpeed,
                            trackpoint
                        )
                        mapData.addNewPolyline(trackColor, trackpoint.latLong!!)
                    } else {
                        mapData.extendPolyline(trackColor, trackpoint.latLong!!)
                    }

                    if (trackpoint.isPause && showPauseMarkers) {
                        mapData.addPauseMarker(trackpoint.latLong)
                    }

                    if (!update) {
                        mapData.endPos?.let { latLongs.add(it) }
                    }


                }
                trackpointsBySegments.debug.trackpointsDrawn += trackpoints.size
            }
            trackpointsDebug.add(trackpointsBySegments.debug)
        } catch (e: Exception) {
            throw RuntimeException("Error reading trackpoints", e)
        }

        mapData.setEndMarker()

        if (update && mapData.endPos != null) {
            mapData.updateMapPositionAndRotation(mapData.endPos!!)
            mapData.renderMap()
        } else if (latLongs.isNotEmpty()) {
            mapData.createBoundingBox(latLongs)
            mapData.boundingBox?.let { mapData.updateMapPositionAndRotation(it.centerPoint) }
        }

        updateTrackpointsDebug(trackpointsDebug)
    }

    private fun readWaypoints(data: Uri) {
        try {
            WaypointReader.readWaypoints(contentResolver, data, lastWaypointId).forEach {
                lastWaypointId = it.id
                mapData.addWaypointMarker(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reading waypoints failed", e)
        }
    }

    private fun readTracks(data: Uri) {
        val tracks = TrackReader.readTracks(contentResolver, data)
        val trackStatistics = if (tracks.isNotEmpty()) TrackStatistics(tracks) else null
        updateTrackStatistics(trackStatistics)
    }

    fun hasTrackId() = lastTrackId > 0

    fun startContentObserver() {
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

    fun unregisterContentObserver() {
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