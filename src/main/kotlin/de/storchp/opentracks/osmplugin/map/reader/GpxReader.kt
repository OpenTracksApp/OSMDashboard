package de.storchp.opentracks.osmplugin.map.reader

import android.content.ContentResolver
import android.util.Log
import androidx.documentfile.provider.DocumentFile
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
import javax.xml.parsers.SAXParserFactory

private val TAG: String = GpxReader::class.java.getSimpleName()

class GpxReader(
    files: List<DocumentFile>,
    contentResolver: ContentResolver,
    private val mapData: MapData,
    private val updateTrackStatistics: UpdateTrackStatistics,
    private val updateTrackpointsDebug: UpdateTrackpointsDebug,
) {

    private val colorCreator = StyleColorCreator()
    private var trackColor = colorCreator.nextColor()
    private var trackpointsDebug = TrackpointsDebug()

    init {
        val gpxParser = GpxParser()
        files.forEach { file ->
            contentResolver.openInputStream(file.uri).use { inputStream ->
                SAXParserFactory.newInstance().newSAXParser()
                    .parse(inputStream, gpxParser)
            }
        }
        readTrackpoints(gpxParser.tracksBySegments)
        readWaypoints(gpxParser.waypoints)
        readTracks(gpxParser.tracks)
    }

    fun readTrackpoints(trackpointsBySegments: TrackpointsBySegments) {
        val latLongs = mutableListOf<GeoPoint>()
        val tolerance = PreferencesUtils.getTrackSmoothingTolerance()

        if (trackpointsBySegments.isEmpty()) {
            Log.d(TAG, "No trackpoints received")
            return
        }

        val average = trackpointsBySegments.calcAverageSpeed()
        val maxSpeed = trackpointsBySegments.calcMaxSpeed()
        val averageToMaxSpeed = maxSpeed - average
        var trackColorMode = PreferencesUtils.getTrackColorMode()

        trackpointsBySegments.segments.map { trackpoints ->
            mapData.cutPolyline()
            if (tolerance > 0) { // smooth track
                return@map MapUtils.decimate(tolerance, trackpoints)
            }

            return@map trackpoints
        }.forEach { segment ->
            if (trackColorMode == TrackColorMode.BY_TRACK) {
                trackColor = colorCreator.nextColor()
            }
            mapData.resetCurrentPolyline()

            segment.filter { it.latLong != null }
                .forEach { trackpoint ->

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

                    mapData.endPos?.let { latLongs.add(it) }
                }
            trackpointsBySegments.debug.trackpointsDrawn += segment.size
        }
        trackpointsDebug.add(trackpointsBySegments.debug)

        mapData.setEndMarker()

        if (latLongs.isNotEmpty()) {
            mapData.createBoundingBox(latLongs)
            mapData.boundingBox?.let { mapData.updateMapPositionAndRotation(it.centerPoint) }
        }

        updateTrackpointsDebug(trackpointsDebug)
    }

    private fun readWaypoints(waypoints: List<Waypoint>) {
        waypoints.forEach {
            mapData.addWaypointMarker(it)
        }
    }

    private fun readTracks(tracks: List<Track>) {
        val trackStatistics = if (tracks.isNotEmpty()) TrackStatistics(tracks) else null
        updateTrackStatistics(trackStatistics)
    }

}
