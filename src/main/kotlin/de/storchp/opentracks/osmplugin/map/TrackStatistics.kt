package de.storchp.opentracks.osmplugin.map

import de.storchp.opentracks.osmplugin.map.model.Track
import kotlin.math.max
import kotlin.math.min

/**
 * @noinspection unused
 */
class TrackStatistics(tracks: List<Track>) {
    var category = "unknown"
        private set
    var startTimeEpochMillis: Long = 0
        private set
    var stopTimeEpochMillis: Long = 0
        private set
    var totalDistanceMeter: Double = 0.0
        private set
    var totalTimeMillis: Long = 0
        private set
    var movingTimeMillis: Long = 0
        private set
    var avgSpeedMeterPerSecond: Double = 0.0
        private set
    var avgMovingSpeedMeterPerSecond: Double = 0.0
        private set
    var maxSpeedMeterPerSecond: Double = 0.0
        private set
    var minElevationMeter: Double = 0.0
        private set
    var maxElevationMeter: Double = 0.0
        private set
    var elevationGainMeter: Double = 0.0
        private set

    init {
        if (tracks.isNotEmpty()) {
            val first = tracks[0]
            first.category?.let { category = it }
            startTimeEpochMillis = first.startTimeEpochMillis
            stopTimeEpochMillis = first.stopTimeEpochMillis
            totalDistanceMeter = first.totalDistanceMeter
            totalTimeMillis = first.totalTimeMillis
            movingTimeMillis = first.movingTimeMillis
            avgSpeedMeterPerSecond = first.avgSpeedMeterPerSecond
            avgMovingSpeedMeterPerSecond = first.avgMovingSpeedMeterPerSecond
            maxSpeedMeterPerSecond = first.maxSpeedMeterPerSecond
            minElevationMeter = first.minElevationMeter
            maxElevationMeter = first.maxElevationMeter
            elevationGainMeter = first.elevationGainMeter

            if (tracks.size > 1) {
                var totalAvgSpeedMeterPerSecond = avgSpeedMeterPerSecond
                var totalAvgMovingSpeedMeterPerSecond = avgMovingSpeedMeterPerSecond
                for (track in tracks.subList(1, tracks.size)) {
                    if (category != track.category) {
                        category = "mixed"
                    }
                    startTimeEpochMillis =
                        min(startTimeEpochMillis, track.startTimeEpochMillis)
                    stopTimeEpochMillis =
                        max(stopTimeEpochMillis, track.stopTimeEpochMillis)
                    totalDistanceMeter += track.totalDistanceMeter
                    totalTimeMillis += track.totalTimeMillis
                    movingTimeMillis += track.movingTimeMillis
                    totalAvgSpeedMeterPerSecond += track.avgSpeedMeterPerSecond
                    totalAvgMovingSpeedMeterPerSecond += track.avgMovingSpeedMeterPerSecond
                    maxSpeedMeterPerSecond =
                        max(maxSpeedMeterPerSecond, track.maxSpeedMeterPerSecond)
                    minElevationMeter =
                        min(minElevationMeter, track.minElevationMeter)
                    maxElevationMeter =
                        max(maxElevationMeter, track.maxElevationMeter)
                    elevationGainMeter += track.elevationGainMeter
                }

                avgSpeedMeterPerSecond = totalAvgSpeedMeterPerSecond / tracks.size
                avgMovingSpeedMeterPerSecond = totalAvgMovingSpeedMeterPerSecond / tracks.size
            }
        }
    }

}
