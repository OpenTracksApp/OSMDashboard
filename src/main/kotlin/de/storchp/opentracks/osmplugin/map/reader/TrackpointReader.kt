package de.storchp.opentracks.osmplugin.map.reader

import android.content.ContentResolver
import android.net.Uri
import de.storchp.opentracks.osmplugin.map.model.TRACKPOINT_TYPE_TRACKPOINT
import de.storchp.opentracks.osmplugin.map.model.Trackpoint
import de.storchp.opentracks.osmplugin.map.model.TrackpointsBySegments
import de.storchp.opentracks.osmplugin.utils.TrackpointsDebug
import java.time.Instant

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
                val time = Instant.ofEpochMilli(cursor.getLong(cursor.getColumnIndexOrThrow(TIME)))

                var type: Int = TRACKPOINT_TYPE_TRACKPOINT
                val typeIndex = cursor.getColumnIndex(TYPE)
                if (typeIndex > -1) {
                    type = cursor.getInt(typeIndex)
                }

                if (lastTrackpoint == null || lastTrackpoint.trackId != trackId) {
                    segment = mutableListOf()
                    segments.add(segment)
                }

                lastTrackpoint =
                    Trackpoint(trackId, id, latitude, longitude, type, speed, time)
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
                                        speed = speed,
                                        time = time,
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