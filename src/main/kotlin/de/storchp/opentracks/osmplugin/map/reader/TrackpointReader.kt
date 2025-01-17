package de.storchp.opentracks.osmplugin.map.reader

import android.content.ContentResolver
import android.net.Uri
import de.storchp.opentracks.osmplugin.map.MapUtils
import de.storchp.opentracks.osmplugin.map.model.TRACKPOINT_TYPE_PAUSE
import de.storchp.opentracks.osmplugin.map.model.TRACKPOINT_TYPE_TRACKPOINT
import de.storchp.opentracks.osmplugin.map.model.Trackpoint
import de.storchp.opentracks.osmplugin.map.model.TrackpointsBySegments
import de.storchp.opentracks.osmplugin.map.model.TrackpointsDebug
import org.oscim.core.GeoPoint
import java.time.Instant

private const val PAUSE_LATITUDE: Double = 100.0

object TrackpointReader {
    const val ID = "_id"
    const val TRACKID = "trackid"
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val TIME = "time"
    const val TYPE = "type"
    const val SPEED = "speed"

    val PROJECTION_V1 = arrayOf(
        ID,
        TRACKID,
        LATITUDE,
        LONGITUDE,
        TIME,
        SPEED
    )

    val PROJECTION_V2 = arrayOf(
        ID,
        TRACKID,
        LATITUDE,
        LONGITUDE,
        TIME,
        TYPE,
        SPEED
    )

    /**
     * Reads the Trackpoints from the Content Uri and split by segments.
     * Pause Trackpoints and different Track IDs split the segments.
     */
    fun readTrackpointsBySegments(
        resolver: ContentResolver,
        data: Uri,
        lastId: Long?,
        protocolVersion: Int
    ): TrackpointsBySegments {
        val debug = TrackpointsDebug()
        var segment = mutableListOf<Trackpoint>()
        val segments = mutableListOf<MutableList<Trackpoint>>()
        var projection = PROJECTION_V2
        var typeQuery = " AND $TYPE IN (-2, -1, 0, 1, 3)"
        if (protocolVersion < 2) { // fallback to old Dashboard API
            projection = PROJECTION_V1
            typeQuery = ""
        }
        resolver.query(
            data,
            projection,
            "$ID> ?$typeQuery",
            arrayOf<String>((lastId ?: 0L).toString()),
            null
        ).use { cursor ->
            var lastTrackpoint: Trackpoint? = null
            while (cursor!!.moveToNext()) {
                debug.trackpointsReceived++
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(ID))
                val trackId = cursor.getLong(cursor.getColumnIndexOrThrow(TRACKID))
                val latitude =
                    cursor.getInt(cursor.getColumnIndexOrThrow(LATITUDE)) / APIConstants.LAT_LON_FACTOR
                val longitude =
                    cursor.getInt(cursor.getColumnIndexOrThrow(LONGITUDE)) / APIConstants.LAT_LON_FACTOR
                val speed = cursor.getDouble(cursor.getColumnIndexOrThrow(SPEED))
                val time = Instant.ofEpochMilli(cursor.getLong(cursor.getColumnIndexOrThrow(TIME)))

                var type: Int = TRACKPOINT_TYPE_TRACKPOINT
                val typeIndex = cursor.getColumnIndex(TYPE)
                if (typeIndex > -1) {
                    type = cursor.getInt(typeIndex)
                }

                if (lastTrackpoint == null || lastTrackpoint.trackId != trackId) {
                    if (segment.isNotEmpty()) {
                        segments.add(segment)
                    }
                    segment = mutableListOf()
                }

                val latLong = if (MapUtils.isValid(latitude, longitude)) {
                    GeoPoint(latitude, longitude)
                } else {
                    null
                }

                if (latLong != null) {
                    lastTrackpoint = Trackpoint(trackId, id, latLong, type, speed, time)
                    segment.add(lastTrackpoint)
                } else if (type == TRACKPOINT_TYPE_PAUSE || latitude == PAUSE_LATITUDE) {
                    debug.trackpointsPause++
                    if (segment.isNotEmpty()) {
                        val previousTrackpoint = segment.last()
                        segment.add(
                            Trackpoint(
                                trackId = trackId,
                                id = id,
                                latLong = previousTrackpoint.latLong,
                                type = TRACKPOINT_TYPE_PAUSE,
                                speed = speed,
                                time = time,
                            )
                        )
                    }
                    lastTrackpoint = null
                } else {
                    debug.trackpointsInvalid++
                }
            }
        }
        if (segment.isNotEmpty()) {
            segments.add(segment)
        }
        debug.segments = segments.size

        return TrackpointsBySegments(segments, debug)
    }
}