package de.storchp.opentracks.osmplugin.map.reader

import android.util.Log
import de.storchp.opentracks.osmplugin.map.model.TRACKPOINT_TYPE_TRACKPOINT
import de.storchp.opentracks.osmplugin.map.model.Track
import de.storchp.opentracks.osmplugin.map.model.Trackpoint
import de.storchp.opentracks.osmplugin.map.model.TrackpointsBySegments
import de.storchp.opentracks.osmplugin.map.model.Waypoint
import de.storchp.opentracks.osmplugin.utils.TrackpointsDebug
import org.oscim.core.GeoPoint
import org.xml.sax.Attributes
import org.xml.sax.Locator
import org.xml.sax.helpers.DefaultHandler
import java.lang.Double.parseDouble
import java.lang.Long.parseLong
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val TAG: String = GpxParser::class.java.getSimpleName()

private const val TAG_DESCRIPTION = "desc"
private const val TAG_GPX = "gpx"
private const val TAG_NAME = "name"
private const val TAG_TIME = "time"
private const val TAG_ELEVATION = "ele"
private const val TAG_TRACK = "trk"
private const val TAG_TRACKPOINT = "trkpt"
private const val TAG_TRACKSEGMENT = "trkseg"
private const val TAG_TYPE = "type"
private const val TAG_OT_TYPE_TRANSLATED = "opentracks:typeTranslated"
private const val TAG_WAYPOINT = "wpt"
private const val TAG_OT_TRACK_UUID = "opentracks:trackid"
private const val ATTRIBUTE_LAT = "lat"
private const val ATTRIBUTE_LON = "lon"
private const val TAG_ROUTE = "rte"
private const val TAG_ROUTEPOINT = "rtept"

private const val TAG_EXTENSION_DISTANCE = "gpxtrkx:Distance"
private const val TAG_EXTENSION_TIMER_TIME = "gpxtrkx:TimerTime"
private const val TAG_EXTENSION_MOVING_TIME = "gpxtrkx:MovingTime"
private const val TAG_EXTENSION_ASCENT = "gpxtrkx:Ascent"
private const val TAG_EXTENSION_SPEED = "gpxtpx:speed"

/**
 * Often speed is exported without the proper namespace.
 */
private const val TAG_EXTENSION_SPEED_COMPAT = "speed"

class GpxParser() : DefaultHandler() {

    private var locator: Locator? = null
    private var zoneOffset: ZoneOffset? = null

    // The current element content
    private var content = ""

    private var trackpointContext: Boolean = false
    private var trackName: String? = null
    private var trackpointName: String? = null
    private var trackDescription: String? = null
    private var trackpointDescription: String? = null
    private var trackType: String? = null
    private var typeLocalized: String? = null
    private var latitude: String? = null
    private var longitude: String? = null
    private var time: String? = null
    private var elevation: String? = null
    private var speed: String? = null
    private var distance: String? = null
    private var timerTime: String? = null
    private var movingTime: String? = null
    private var ascent: String? = null
    private var startTime: Instant? = null
    private var stopTime: Instant? = null
    private var maxSpeedMeterPerSecond: Double? = null
    private var minElevationMeter: Double? = null
    private var maxElevationMeter: Double? = null
    private val speedMeterPerSecondList = mutableListOf<Double>()
    private val movingSpeedMeterPerSecondList = mutableListOf<Double>()
    private var trackpointType: String? = null
    private var photoUrl: String? = null
    private var trackUuid: String? = null
    private var trackId: Long = 0

    private val debug = TrackpointsDebug()
    private val segments = mutableListOf<List<Trackpoint>>()
    private val segment = mutableListOf<Trackpoint>()

    val waypoints = mutableListOf<Waypoint>()
    val tracks = mutableListOf<Track>()
    val tracksBySegments: TrackpointsBySegments
        get() = TrackpointsBySegments(segments, debug)

    override fun startElement(uri: String, localName: String, tag: String, attributes: Attributes) {
        when (tag) {
            TAG_TRACK, TAG_ROUTE -> {
                trackId++
                distance = null
                timerTime = null
                movingTime = null
                ascent = null
                startTime = null
                stopTime = null
                maxSpeedMeterPerSecond = null
                minElevationMeter = null
                maxElevationMeter = null
                speedMeterPerSecondList.clear()
                movingSpeedMeterPerSecondList.clear()
                segment.clear()
            }

            TAG_TRACKSEGMENT -> {
                segment.clear()
            }

            TAG_WAYPOINT, TAG_TRACKPOINT, TAG_ROUTEPOINT -> onTrackpointStart(attributes)
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        content += String(ch, start, length)
    }

    override fun endElement(uri: String, localName: String, tag: String) {
        when (tag) {
            TAG_GPX -> {
                // nothing to do
            }

            TAG_WAYPOINT -> onWaypointEnd()
            TAG_TRACK, TAG_ROUTE -> onTrackEnd()
            TAG_TRACKSEGMENT -> onTracksegmentEnd()
            TAG_TRACKPOINT, TAG_ROUTEPOINT -> onTrackpointEnd()
            TAG_NAME -> {
                if (trackpointContext) trackpointName = content.trim()
                else trackName = content.trim()
            }

            TAG_DESCRIPTION -> {
                if (trackpointContext) trackpointDescription = content.trim()
                else trackDescription = content.trim()
            }

            TAG_TYPE -> {
                if (trackpointContext) trackpointType = content.trim()
                else trackType = content.trim()
            }

            TAG_OT_TYPE_TRANSLATED -> typeLocalized = content.trim()
            TAG_TIME -> time = content.trim()
            TAG_EXTENSION_SPEED, TAG_EXTENSION_SPEED_COMPAT -> speed = content.trim()
            TAG_ELEVATION -> elevation = content.trim()
            TAG_OT_TRACK_UUID -> trackUuid = content.trim()
            TAG_EXTENSION_DISTANCE -> distance = content.trim()
            TAG_EXTENSION_TIMER_TIME -> timerTime = content.trim()
            TAG_EXTENSION_MOVING_TIME -> movingTime = content.trim()
            TAG_EXTENSION_ASCENT -> ascent = content.trim()
        }

        content = ""
    }

    private fun onTracksegmentEnd() {
        if (segment.isEmpty() == true) {
            Log.w(TAG, "No Trackpoints in current segment.")
            return
        }

        segments.add(segment.toList())
        segment.clear()
    }

    private fun onTrackpointEnd() {
        val trackpoint = createTrackpoint()
        if (trackpoint == null) {
            debug.trackpointsInvalid++
            return
        }
        
        segment.add(trackpoint)
        debug.trackpointsReceived++

        if (startTime == null) {
            startTime = trackpoint.time
        }
        trackpoint.time?.let { time ->
            if (stopTime == null || time > stopTime!!) {
                stopTime = time
            }
        }
        trackpoint.speed?.let { speed ->
            if (maxSpeedMeterPerSecond == null || speed > maxSpeedMeterPerSecond!!) {
                maxSpeedMeterPerSecond = speed
            }
        }
        trackpoint.elevation?.let { elevation ->
            if (minElevationMeter == null || elevation < minElevationMeter!!) {
                minElevationMeter = elevation
            }
            if (maxElevationMeter == null || elevation > maxElevationMeter!!) {
                maxElevationMeter = elevation
            }
        }
        val speed = trackpoint.speed ?: 0.0
        speedMeterPerSecondList.add(speed)
        if (speed > 0) movingSpeedMeterPerSecondList.add(speed)

        trackpointContext = false
    }

    private fun createTrackpoint(): Trackpoint? {
        try {
            val parsedTime = time?.let { OffsetDateTime.parse(it) }
            if (zoneOffset == null && parsedTime != null) {
                zoneOffset = parsedTime.offset
            }

            val latitudeDouble = latitude?.let { parseDouble(it) }
            val longitudeDouble = longitude?.let { parseDouble(it) }
            if (latitudeDouble == null || longitudeDouble == null) {
                return null
            }

            val speedDouble = speed?.let { parseDouble(it) } ?: 0.0
            val elevationDouble = elevation?.let { parseDouble(it) }

            return Trackpoint(
                latLong = GeoPoint(latitudeDouble, longitudeDouble),
                type = TRACKPOINT_TYPE_TRACKPOINT,
                speed = speedDouble,
                time = parsedTime?.toInstant(),
                elevation = elevationDouble,
                name = trackpointName,
            )
        } catch (e: Exception) {
            throw RuntimeException("Unable to create Trackpoint", e)
        }
    }

    private fun onTrackpointStart(attributes: Attributes) {
        trackpointContext = true
        latitude = attributes.getValue(ATTRIBUTE_LAT)
        longitude = attributes.getValue(ATTRIBUTE_LON)
        time = null
        elevation = null
        speed = null
        trackpointName = null
        trackpointDescription = null
        photoUrl = null
        trackpointType = null
    }

    private fun onWaypointEnd() {
        val trackpoint = createTrackpoint()
        if (trackpoint == null) {
            return
        }

        waypoints.add(
            Waypoint(
                name = trackpointName,
                description = trackpointDescription,
                category = trackpointType,
                latLong = trackpoint.latLong,
                photoUrl = photoUrl
            )
        )
        trackpointContext = false
    }

    private fun onTrackEnd() {
        if (typeLocalized == null) {
            // Backward compatibility
            typeLocalized = trackType
        }
        tracks.add(
            Track(
                id = trackId,
                name = trackName,
                description = trackDescription,
                category = typeLocalized,
                totalDistanceMeter = distance?.let { parseDouble(it) } ?: 0.0,
                totalTime = timerTime?.let { parseLong(it).seconds } ?: Duration.ZERO,
                movingTime = movingTime?.let { parseLong(it).seconds } ?: Duration.ZERO,
                elevationGainMeter = ascent?.let { parseDouble(it) } ?: 0.0,
                startTime = startTime,
                stopTime = stopTime,
                maxSpeedMeterPerSecond = maxSpeedMeterPerSecond ?: 0.0,
                minElevationMeter = minElevationMeter ?: 0.0,
                maxElevationMeter = maxElevationMeter ?: 0.0,
                avgSpeedMeterPerSecond = if (speedMeterPerSecondList.isNotEmpty()) speedMeterPerSecondList.average() else 0.0,
                avgMovingSpeedMeterPerSecond = if (movingSpeedMeterPerSecondList.isNotEmpty()) movingSpeedMeterPerSecondList.average() else 0.0,
            )
        )
        zoneOffset = null

        // Add the last missing segment
        if (segments.isEmpty() && segment.isNotEmpty()) {
            segments.add(segment.toList())
            segment.clear()
        }

        debug.segments = segments.size
    }

    override fun setDocumentLocator(locator: Locator) {
        this.locator = locator
    }

}