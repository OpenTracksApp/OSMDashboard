package de.storchp.opentracks.osmplugin.utils

import de.storchp.opentracks.osmplugin.dashboardapi.Track
import kotlin.math.max
import kotlin.math.min

/**
 * @noinspection unused
 */
class TrackStatistics(tracks: List<Track>) {
    private var category = "unknown"
    private var startTimeEpochMillis: Int = 0
    private var stopTimeEpochMillis: Int = 0
    private var totalDistanceMeter: Float = 0f
    private var totalTimeMillis: Int = 0
    private var movingTimeMillis: Int = 0
    private var avgSpeedMeterPerSecond: Float = 0f
    private var avgMovingSpeedMeterPerSecond: Float = 0f
    private var maxSpeedMeterPerSecond: Float = 0f
    private var minElevationMeter: Float = 0f
    private var maxElevationMeter: Float = 0f
    private var elevationGainMeter: Float = 0f

    init {
        if (!tracks.isEmpty()) {
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

    fun getCategory(): String {
        return category
    }

    fun getStartTimeEpochMillis(): Int {
        return startTimeEpochMillis
    }

    fun getStopTimeEpochMillis(): Int {
        return stopTimeEpochMillis
    }

    fun getTotalDistanceMeter(): Float {
        return totalDistanceMeter
    }

    fun getTotalTimeMillis(): Int {
        return totalTimeMillis
    }

    fun getMovingTimeMillis(): Int {
        return movingTimeMillis
    }

    fun getAvgSpeedMeterPerSecond(): Float {
        return avgSpeedMeterPerSecond
    }

    fun getAvgMovingSpeedMeterPerSecond(): Float {
        return avgMovingSpeedMeterPerSecond
    }

    fun getMaxSpeedMeterPerSecond(): Float {
        return maxSpeedMeterPerSecond
    }

    fun getMinElevationMeter(): Float {
        return minElevationMeter
    }

    fun getMaxElevationMeter(): Float {
        return maxElevationMeter
    }

    fun getElevationGainMeter(): Float {
        return elevationGainMeter
    }
}
