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
    var startTimeEpochMillis: Int = 0
        private set
    var stopTimeEpochMillis: Int = 0
        private set
    var totalDistanceMeter: Float = 0f
        private set
    var totalTimeMillis: Int = 0
        private set
    var movingTimeMillis: Int = 0
        private set
    var avgSpeedMeterPerSecond: Float = 0f
        private set
    var avgMovingSpeedMeterPerSecond: Float = 0f
        private set
    var maxSpeedMeterPerSecond: Float = 0f
        private set
    var minElevationMeter: Float = 0f
        private set
    var maxElevationMeter: Float = 0f
        private set
    var elevationGainMeter: Float = 0f
        private set

    init {
        if (tracks.isNotEmpty()) {
            val first = tracks[0]
            category = first.category.toString()
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
