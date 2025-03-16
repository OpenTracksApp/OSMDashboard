package de.storchp.opentracks.osmplugin.map.reader

import android.util.Log
import de.storchp.opentracks.osmplugin.map.DEFAULT_TRACK_COLOR_MODE
import de.storchp.opentracks.osmplugin.map.MapData
import de.storchp.opentracks.osmplugin.map.MapUtils
import de.storchp.opentracks.osmplugin.map.TrackColorMode
import de.storchp.opentracks.osmplugin.map.model.Track
import de.storchp.opentracks.osmplugin.map.model.TrackStatistics
import de.storchp.opentracks.osmplugin.map.model.TrackpointsBySegments
import de.storchp.opentracks.osmplugin.map.model.TrackpointsDebug
import de.storchp.opentracks.osmplugin.map.model.Waypoint
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils
import org.oscim.core.GeoPoint

private val TAG: String = MapDataReader::class.java.getSimpleName()

abstract class MapDataReader(
    private val mapData: MapData,
    private val updateTrackStatistics: UpdateTrackStatistics,
    private val updateTrackpointsDebug: UpdateTrackpointsDebug,
) {

    private val trackSpeedColors = PreferencesUtils.getTrackSpeedColors()
    private val trackColors = PreferencesUtils.getTrackColors()
    private var trackColor = trackColors.first()
    private var trackpointsDebug = TrackpointsDebug()
    var lastTrackId: Long? = null
        protected set
    var keepScreenOn = false
        protected set
    var showOnLockScreen = false
        protected set
    var showFullscreen = false
        protected set
    var isRecording = false
        protected set

    protected fun readTrackpoints(
        trackpointsBySegments: TrackpointsBySegments,
        update: Boolean,
        isRecording: Boolean
    ) {
        val showPauseMarkers = PreferencesUtils.isShowPauseMarkers()
        val latLongs = mutableListOf<GeoPoint>()
        val tolerance = PreferencesUtils.getTrackSmoothingTolerance()

        if (trackpointsBySegments.isEmpty()) {
            Log.d(TAG, "No new trackpoints received")
            return
        }

        val average = trackpointsBySegments.calcAverageSpeed()
        val maxSpeed = trackpointsBySegments.calcMaxSpeed()
        var trackColorMode = PreferencesUtils.getTrackColorMode()
        if (isRecording && !trackColorMode.supportsLiveTrack) {
            trackColorMode = DEFAULT_TRACK_COLOR_MODE
        }
        var trackIndex = 0

        trackpointsBySegments.segments.map { trackpoints ->
            if (!update) {
                mapData.cutPolyline()
                if (tolerance > 0) { // smooth track
                    return@map MapUtils.decimate(tolerance, trackpoints)
                }
            }
            return@map trackpoints
        }.forEach { trackpoints ->
            trackpoints.forEach { trackpoint ->
                if (trackpoint.trackId != lastTrackId) {
                    if (trackColorMode == TrackColorMode.BY_TRACK) {
                        trackColor = trackColors[trackIndex % trackColors.size]
                        trackIndex++
                    }
                    lastTrackId = trackpoint.trackId
                    mapData.resetCurrentPolyline()
                }

                if (trackColorMode == TrackColorMode.BY_SPEED) {
                    trackColor = MapUtils.getTrackColorBySpeed(
                        average,
                        maxSpeed,
                        trackpoint,
                        trackSpeedColors,
                    )
                    mapData.addNewPolyline(trackColor, trackpoint)
                } else {
                    mapData.extendPolyline(trackColor, trackpoint)
                }

                if (trackpoint.isPause && showPauseMarkers) {
                    mapData.addPauseMarker(trackpoint)
                }

                if (!update) {
                    mapData.endPoint?.latLong?.let { latLongs.add(it) }
                }

            }
            trackpointsBySegments.debug.trackpointsDrawn += trackpoints.size
        }
        trackpointsDebug.add(trackpointsBySegments.debug)

        mapData.setEndMarker(isRecording)

        if (update && mapData.endPoint != null) {
            mapData.updateMapPositionAndRotation(mapData.endPoint!!.latLong)
            mapData.renderMap()
        } else if (latLongs.isNotEmpty()) {
            mapData.createBoundingBox(latLongs)
            mapData.boundingBox?.let { mapData.updateMapPositionAndRotation(it.centerPoint) }
        }

        updateTrackpointsDebug(trackpointsDebug)
    }

    protected fun readWaypoints(waypoints: List<Waypoint>) {
        waypoints.forEach {
            mapData.addWaypointMarker(it)
        }
    }

    protected fun readTracks(tracks: List<Track>) {
        val trackStatistics = if (tracks.isNotEmpty()) TrackStatistics(tracks) else null
        updateTrackStatistics(trackStatistics)
    }

    open fun startContentObserver() {
    }

    open fun unregisterContentObserver() {
    }

}