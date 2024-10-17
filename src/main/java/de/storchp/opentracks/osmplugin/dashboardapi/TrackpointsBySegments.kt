package de.storchp.opentracks.osmplugin.dashboardapi

import de.storchp.opentracks.osmplugin.utils.TrackPointsDebug

data class TrackpointsBySegments(
    val segments: List<List<TrackPoint>>,
    val debug: TrackPointsDebug,
) : List<List<TrackPoint>> by segments {

    fun calcAverageSpeed() = speeds().average()

    fun calcMaxSpeed() = speeds().maxOrNull() ?: 0.0

    private fun speeds() = segments
        .flatMap { it }
        .map { it.speed }
        .filter { it > 0 }

}
