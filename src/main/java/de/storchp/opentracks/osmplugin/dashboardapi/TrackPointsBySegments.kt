package de.storchp.opentracks.osmplugin.dashboardapi

import de.storchp.opentracks.osmplugin.utils.TrackPointsDebug

data class TrackPointsBySegments(
    val segments: List<List<TrackPoint>>,
    val debug: TrackPointsDebug,
) {
    fun isEmpty(): Boolean {
        return segments.isEmpty()
    }

    fun calcAverageSpeed(): Double {
        return streamTrackPointsWithSpeed().average()
    }

    fun calcMaxSpeed(): Double {
        return streamTrackPointsWithSpeed().maxOrNull() ?: 0.0
    }

    private fun streamTrackPointsWithSpeed(): List<Double> {
        return segments
            .flatMap { it }
            .map { it.speed }
            .filter { it > 0 }
    }

}
