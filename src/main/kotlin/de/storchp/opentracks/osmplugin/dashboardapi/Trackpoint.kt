package de.storchp.opentracks.osmplugin.dashboardapi

import android.content.ContentResolver
import android.net.Uri
import de.storchp.opentracks.osmplugin.utils.MapUtils
import de.storchp.opentracks.osmplugin.utils.TrackPointsDebug
import org.oscim.core.GeoPoint

data class TrackPoint(
    val trackId: Long,
    val trackPointId: Long,
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
    const val _ID: String = "_id"
    const val TRACKID: String = "trackid"
    const val LONGITUDE: String = "longitude"
    const val LATITUDE: String = "latitude"
    const val TIME: String = "time"
    const val TYPE: String = "type"
    const val SPEED: String = "speed"


    val PROJECTION_V1 = arrayOf(
        _ID,
        TRACKID,
        LATITUDE,
        LONGITUDE,
        TIME,
        SPEED
    )

    val PROJECTION_V2 = arrayOf(
        _ID,
        TRACKID,
        LATITUDE,
        LONGITUDE,
        TIME,
        TYPE,
        SPEED
    )

    /**
     * Reads the TrackPoints from the Content Uri and split by segments.
     * Pause TrackPoints and different Track IDs split the segments.
     */
    fun readTrackPointsBySegments(
        resolver: ContentResolver,
        data: Uri,
        lastTrackPointId: Long,
        protocolVersion: Int
    ): TrackpointsBySegments {
        val debug = TrackPointsDebug()
        val segments: MutableList<MutableList<TrackPoint>> = mutableListOf()
        var projection = PROJECTION_V2
        var typeQuery = " AND $TYPE IN (-2, -1, 0, 1, 3)"
        if (protocolVersion < 2) { // fallback to old Dashboard API
            projection = PROJECTION_V1
            typeQuery = ""
        }
        resolver.query(
            data,
            projection,
            "$_ID> ?$typeQuery",
            arrayOf<String>(lastTrackPointId.toString()),
            null
        ).use { cursor ->
            var lastTrackPoint: TrackPoint? = null
            var segment: MutableList<TrackPoint>? = null
            while (cursor!!.moveToNext()) {
                debug.trackpointsReceived++
                val trackPointId = cursor.getLong(cursor.getColumnIndexOrThrow(_ID))
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

                if (lastTrackPoint == null || lastTrackPoint.trackId != trackId) {
                    segment = mutableListOf()
                    segments.add(segment)
                }

                lastTrackPoint =
                    TrackPoint(trackId, trackPointId, latitude, longitude, type, speed)
                if (lastTrackPoint.latLong != null) {
                    segment!!.add(lastTrackPoint)
                } else if (!lastTrackPoint.isPause) {
                    debug.trackpointsInvalid++
                }
                if (lastTrackPoint.isPause) {
                    debug.trackpointsPause++
                    if (lastTrackPoint.latLong == null) {
                        if (segment!!.isNotEmpty()) {
                            val previousTrackpoint = segment[segment.size - 1]
                            previousTrackpoint.latLong?.let {
                                segment.add(
                                    TrackPoint(
                                        trackId = trackId,
                                        trackPointId = trackPointId,
                                        latitude = it.latitude,
                                        longitude = it.longitude,
                                        type = type,
                                        speed = speed
                                    )
                                )
                            }
                        }
                    }
                    lastTrackPoint = null
                }
            }
        }
        debug.segments = segments.size

        return TrackpointsBySegments(segments, debug)
    }
}
