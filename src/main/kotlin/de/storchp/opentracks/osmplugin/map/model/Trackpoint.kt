package de.storchp.opentracks.osmplugin.map.model

import de.storchp.opentracks.osmplugin.map.MapUtils
import org.oscim.core.GeoPoint
import java.time.Instant

private const val PAUSE_LATITUDE: Double = 100.0
const val TRACKPOINT_TYPE_TRACKPOINT = 0
const val TRACKPOINT_TYPE_PAUSE = 3

data class Trackpoint(
    val trackId: Long? = null,
    val id: Long? = null,
    private val latitude: Double,
    private val longitude: Double,
    val type: Int,
    val speed: Double?,
    val time: Instant?,
    val elevation: Double? = null,
    val name: String? = null,
) {
    val latLong = if (MapUtils.isValid(latitude, longitude)) {
        GeoPoint(latitude, longitude)
    } else {
        null
    }

    val isPause = type == TRACKPOINT_TYPE_PAUSE || latitude == PAUSE_LATITUDE
}
