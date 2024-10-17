package de.storchp.opentracks.osmplugin.dashboardapi

import de.storchp.opentracks.osmplugin.utils.TrackPointsDebug

data class TrackpointsBySegments(
    val segments: List<List<TrackPoint>>,
    val debug: TrackPointsDebug,
) {
    fun isEmpty() = segments.isEmpty()

    fun calcAverageSpeed() = trackPointsWithSpeed().average()

    fun calcMaxSpeed() = trackPointsWithSpeed().maxOrNull() ?: 0.0

    private fun trackPointsWithSpeed() = segments
        .flatMap { it }
        .map { it.speed }
        .filter { it > 0 }

}
