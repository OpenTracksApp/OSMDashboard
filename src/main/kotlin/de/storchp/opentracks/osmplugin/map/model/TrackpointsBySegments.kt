package de.storchp.opentracks.osmplugin.map.model

import de.storchp.opentracks.osmplugin.utils.TrackpointsDebug

data class TrackpointsBySegments(
    val segments: List<List<Trackpoint>>,
    val debug: TrackpointsDebug,
) : List<List<Trackpoint>> by segments {

    fun calcAverageSpeed() = speeds().average()

    fun calcMaxSpeed() = speeds().maxOrNull() ?: 0.0

    private fun speeds() = segments
        .flatMap { it }
        .map { it.speed ?: 0.0 }
        .filter { it > 0 }

}
