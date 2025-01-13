package de.storchp.opentracks.osmplugin.map

import de.storchp.opentracks.osmplugin.map.model.Track
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration

/**
 * @noinspection unused
 */
class TrackStatistics(tracks: List<Track>) {
    var category = "unknown"
        private set
    var startTime: Instant? = null
        private set
    var stopTime: Instant? = null
        private set
    var totalDistanceMeter: Double = 0.0
        private set
    var totalTime: Duration = Duration.ZERO
        private set
    var movingTime: Duration = Duration.ZERO
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
            startTime = first.startTime
            stopTime = first.stopTime
            totalDistanceMeter = first.totalDistanceMeter
            totalTime = first.totalTime
            movingTime = first.movingTime
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
                    if (startTime == null || (track.startTime != null && track.startTime < startTime)) {
                        startTime = track.startTime
                    }
                    if (startTime == null || (track.stopTime != null && track.stopTime > stopTime)) {
                        stopTime = track.startTime
                    }
                    totalDistanceMeter += track.totalDistanceMeter
                    totalTime += track.totalTime
                    movingTime += track.movingTime
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
