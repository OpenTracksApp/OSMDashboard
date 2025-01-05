package de.storchp.opentracks.osmplugin.map.reader

import android.util.Log
import de.storchp.opentracks.osmplugin.map.DEFAULT_TRACK_COLOR_MORE
import de.storchp.opentracks.osmplugin.map.MapData
import de.storchp.opentracks.osmplugin.map.MapUtils
import de.storchp.opentracks.osmplugin.map.StyleColorCreator
import de.storchp.opentracks.osmplugin.map.TrackColorMode
import de.storchp.opentracks.osmplugin.map.TrackStatistics
import de.storchp.opentracks.osmplugin.map.model.Track
import de.storchp.opentracks.osmplugin.map.model.TrackpointsBySegments
import de.storchp.opentracks.osmplugin.map.model.Waypoint
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils
import de.storchp.opentracks.osmplugin.utils.TrackpointsDebug
import org.oscim.core.GeoPoint
import kotlin.collections.forEach

private val TAG: String = MapDataReader::class.java.getSimpleName()

abstract class MapDataReader(
    private val mapData: MapData,
    private val updateTrackStatistics: UpdateTrackStatistics,
    private val updateTrackpointsDebug: UpdateTrackpointsDebug,
) {

    private val colorCreator = StyleColorCreator()
    private var trackColor = colorCreator.nextColor()
    private var trackpointsDebug = TrackpointsDebug()
    var lastTrackId: Long? = null
        protected set
    var keepScreenOn = false
        protected set
    var showOnLockScreen = false
        protected set
    var showFullscreen = false
        protected set
    var isOpenTracksRecordingThisTrack = false
        protected set

    protected fun readTrackpoints(trackpointsBySegments: TrackpointsBySegments, update: Boolean) {
        val showPauseMarkers = PreferencesUtils.isShowPauseMarkers()
        val latLongs = mutableListOf<GeoPoint>()
        val tolerance = PreferencesUtils.getTrackSmoothingTolerance()

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
            trackpoints.filter { it.latLong != null }
                .forEach { trackpoint ->
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