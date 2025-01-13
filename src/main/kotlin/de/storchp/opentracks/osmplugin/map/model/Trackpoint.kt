package de.storchp.opentracks.osmplugin.map.model

import org.oscim.core.GeoPoint
import java.time.Instant

const val TRACKPOINT_TYPE_TRACKPOINT = 0
const val TRACKPOINT_TYPE_PAUSE = 3

data class Trackpoint(
    val trackId: Long? = null,
    val id: Long? = null,
    val latLong: GeoPoint,
    val type: Int,
    val speed: Double?,
    val time: Instant?,
    val elevation: Double? = null,
    val name: String? = null,
) {
    val isPause = type == TRACKPOINT_TYPE_PAUSE
}
