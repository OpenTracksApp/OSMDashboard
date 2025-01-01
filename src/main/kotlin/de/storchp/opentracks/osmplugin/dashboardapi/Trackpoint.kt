package de.storchp.opentracks.osmplugin.dashboardapi

import android.content.ContentResolver
import android.net.Uri
import de.storchp.opentracks.osmplugin.map.MapUtils
import de.storchp.opentracks.osmplugin.utils.TrackpointsDebug
import org.oscim.core.GeoPoint

data class Trackpoint(
    val trackId: Long,
    val id: Long,
    private val latitude: Double,
    private val longitude: Double,
    val type: Int?,
    val speed: Double,
) {
    val latLong = if (MapUtils.isValid(latitude, longitude)) {
        GeoPoint(latitude, longitude)
    } else {
        null
    }

    val isPause = if (type != null) type == 3 else latitude == PAUSE_LATITUDE
}

private const val PAUSE_LATITUDE: Double = 100.0

object TrackpointReader {
    const val ID = "_id"
    const val TRACKID = "trackid"
    const val LONGITUDE = "longitude"
    const val LATITUDE = "latitude"
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
        lastId: Long,
        protocolVersion: Int
    ): TrackpointsBySegments {
        val debug = TrackpointsDebug()
        val segments: MutableList<MutableList<Trackpoint>> = mutableListOf()
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
            arrayOf<String>(lastId.toString()),
            null
        ).use { cursor ->
            var lastTrackpoint: Trackpoint? = null
            var segment: MutableList<Trackpoint>? = null
            while (cursor!!.moveToNext()) {
                debug.trackpointsReceived++
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(ID))
                val trackId = cursor.getLong(cursor.getColumnIndexOrThrow(TRACKID))
                val latitude =
                    cursor.getInt(cursor.getColumnIndexOrThrow(LATITUDE)) / APIConstants.LAT_LON_FACTOR
                val longitude =
                    cursor.getInt(cursor.getColumnIndexOrThrow(LONGITUDE)) / APIConstants.LAT_LON_FACTOR
                val speed = cursor.getDouble(cursor.getColumnIndexOrThrow(SPEED))

                var type: Int? = null
                val typeIndex = cursor.getColumnIndex(TYPE)
                if (typeIndex > -1) {
                    type = cursor.getInt(typeIndex)
                }

                if (lastTrackpoint == null || lastTrackpoint.trackId != trackId) {
                    segment = mutableListOf()
                    segments.add(segment)
                }

                lastTrackpoint =
                    Trackpoint(trackId, id, latitude, longitude, type, speed)
                if (lastTrackpoint.latLong != null) {
                    segment!!.add(lastTrackpoint)
                } else if (!lastTrackpoint.isPause) {
                    debug.trackpointsInvalid++
                }
                if (lastTrackpoint.isPause) {
                    debug.trackpointsPause++
                    if (lastTrackpoint.latLong == null) {
                        if (segment!!.isNotEmpty()) {
                            val previousTrackpoint = segment[segment.size - 1]
                            previousTrackpoint.latLong?.let {
                                segment.add(
                                    Trackpoint(
                                        trackId = trackId,
                                        id = id,
                                        latitude = it.latitude,
                                        longitude = it.longitude,
                                        type = type,
                                        speed = speed
                                    )
                                )
                            }
                        }
                    }
                    lastTrackpoint = null
                }
            }
        }
        debug.segments = segments.size

        return TrackpointsBySegments(segments, debug)
    }
}
