package de.storchp.opentracks.osmplugin.map.reader

import android.util.Log
import de.storchp.opentracks.osmplugin.map.model.TRACKPOINT_TYPE_TRACKPOINT
import de.storchp.opentracks.osmplugin.map.model.Track
import de.storchp.opentracks.osmplugin.map.model.Trackpoint
import de.storchp.opentracks.osmplugin.map.model.TrackpointsBySegments
import de.storchp.opentracks.osmplugin.map.model.Waypoint
import de.storchp.opentracks.osmplugin.utils.TrackpointsDebug
import org.xml.sax.Attributes
import org.xml.sax.Locator
import org.xml.sax.helpers.DefaultHandler
import java.lang.Double
import java.time.OffsetDateTime
import java.time.ZoneOffset

private val TAG: String = GpxParser::class.java.getSimpleName()

private const val TAG_DESCRIPTION = "desc"
private const val TAG_GPX = "gpx"
private const val TAG_NAME = "name"
private const val TAG_TIME = "time"
private const val TAG_TRACK = "trk"
private const val TAG_TRACKPOINT = "trkpt"
private const val TAG_TRACKSEGMENT = "trkseg"
private const val TAG_TYPE = "type"
private const val TAG_OT_TYPE_TRANSLATED = "opentracks:typeTranslated"
private const val TAG_WAYPOINT = "wpt"
private const val TAG_OT_TRACK_UUID = "opentracks:trackid"
private const val ATTRIBUTE_LAT = "lat"
private const val ATTRIBUTE_LON = "lon"

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

    private var name: String? = null
    private var description: String? = null
    private var activityType: String? = null
    private var activityTypeLocalized: String? = null
    private var latitude: String? = null
    private var longitude: String? = null
    private var time: String? = null
    private var speed: String? = null
    private var markerType: String? = null
    private var photoUrl: String? = null
    private var trackUuid: String? = null
    private var trackId: Long = 0

    private val debug = TrackpointsDebug()
    private val segments = mutableListOf<MutableList<Trackpoint>>()
    private var segment: MutableList<Trackpoint>? = null

    val waypoints = mutableListOf<Waypoint>()
    val tracks = mutableListOf<Track>()
    val tracksBySegments: TrackpointsBySegments
        get() = TrackpointsBySegments(segments, debug)

    override fun startElement(uri: String, localName: String, tag: String, attributes: Attributes) {
        when (tag) {
            TAG_WAYPOINT -> onWaypointStart(attributes)
            TAG_TRACK -> {
                trackId++
            }

            TAG_TRACKSEGMENT -> {
                segment = mutableListOf()
            }

            TAG_TRACKPOINT -> onTrackpointStart(attributes)
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
            TAG_TRACK -> onTrackEnd()
            TAG_TRACKSEGMENT -> onTracksegmentEnd()
            TAG_TRACKPOINT -> onTrackpointEnd()

            TAG_NAME -> {
                name = content.trim()
            }

            TAG_DESCRIPTION -> {
                description = content.trim()
            }

            TAG_TYPE -> { //Track or Marker/WPT
                // In older  version this might be localized content.
                activityType = content.trim()
                markerType = content.trim()
            }

            TAG_OT_TYPE_TRANSLATED -> {
                activityTypeLocalized = content.trim()
            }

            TAG_TIME -> {
                time = content.trim()
            }

            TAG_EXTENSION_SPEED, TAG_EXTENSION_SPEED_COMPAT -> {
                speed = content.trim()
            }

            TAG_OT_TRACK_UUID -> {
                trackUuid = content.trim()
            }
        }

        content = ""
    }

    private fun onTracksegmentEnd() {
        if (segment == null || segment!!.isEmpty() == true) {
            Log.w(TAG, "No Trackpoints in current segment.")
            return
        }

        segments.add(segment!!)
        segment = mutableListOf()
    }

    private fun onTrackpointEnd() {
        val trackpoint = createTrackpoint()
        if (trackpoint.latLong != null) {
            segment!!.add(trackpoint)
            debug.trackpointsReceived++
        } else {
            debug.trackpointsInvalid++
        }
    }

    private fun createTrackpoint(): Trackpoint {
        try {
            val parsedTime = OffsetDateTime.parse(time)
            if (zoneOffset == null) {
                zoneOffset = parsedTime.offset
            }

            val latitudeDouble = latitude?.let { Double.parseDouble(it) } ?: 0.0
            val longitudeDouble = longitude?.let { Double.parseDouble(it) } ?: 0.0
            val speedDouble = speed?.let { Double.parseDouble(it) } ?: 0.0

            return Trackpoint(
                latitude = latitudeDouble,
                longitude = longitudeDouble,
                type = TRACKPOINT_TYPE_TRACKPOINT,
                speed = speedDouble,
                time = parsedTime.toInstant()
            )
        } catch (e: Exception) {
            throw RuntimeException("Unable to parse time: $time", e)
        }
    }

    private fun onTrackpointStart(attributes: Attributes) {
        latitude = attributes.getValue(ATTRIBUTE_LAT)
        longitude = attributes.getValue(ATTRIBUTE_LON)
        time = null
        speed = null
    }

    private fun onWaypointStart(attributes: Attributes) {
        name = null
        description = null
        photoUrl = null
        latitude = attributes.getValue(ATTRIBUTE_LAT)
        longitude = attributes.getValue(ATTRIBUTE_LON)
        time = null
        markerType = null
    }

    private fun onWaypointEnd() {
        val trackpoint = createTrackpoint()
        if (trackpoint.latLong == null) {
            Log.w(TAG, "Marker with invalid coordinates ignored: $trackpoint")
            return
        }

        waypoints.add(
            Waypoint(
                name = name,
                description = description,
                category = markerType,
                latLong = trackpoint.latLong,
                photoUrl = photoUrl
            )
        )
    }

    private fun onTrackEnd() {
        if (activityTypeLocalized == null) {
            // Backward compatibility
            activityTypeLocalized = activityType
        }
        tracks.add(
            Track(
                id = trackId,
                trackname = name,
                description = description,
                category = activityTypeLocalized,
                startTimeEpochMillis = 0,
                stopTimeEpochMillis = 0,
                totalDistanceMeter = 0f,
                totalTimeMillis = 0,
                movingTimeMillis = 0,
                avgSpeedMeterPerSecond = 0f,
                avgMovingSpeedMeterPerSecond = 0f,
                maxSpeedMeterPerSecond = 0f,
                minElevationMeter = 0f,
                maxElevationMeter = 0f,
                elevationGainMeter = 0f,
            )
        )
        zoneOffset = null
        debug.segments = segments.size
    }

    override fun setDocumentLocator(locator: Locator) {
        this.locator = locator
    }

}