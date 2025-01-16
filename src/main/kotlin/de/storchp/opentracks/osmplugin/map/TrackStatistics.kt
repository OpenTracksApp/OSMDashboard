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
    var category: String? = null
        private set
    var startTime: Instant? = null
        private set
    var stopTime: Instant? = null
        private set
    var totalDistanceMeter: Double = 0.0
        private set
    var totalTime: Duration? = null
        private set
    var movingTime: Duration? = null
        private set
    var avgSpeedMeterPerSecond: Double? = null
        private set
    var avgMovingSpeedMeterPerSecond: Double? = null
        private set
    var maxSpeedMeterPerSecond: Double? = null
        private set
    var minElevationMeter: Double? = null
        private set
    var maxElevationMeter: Double? = null
        private set
    var elevationGainMeter: Double? = null
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
                    if (category == null) {
                        category = track.category
                    } else if (category != track.category) {
                        category = "mixed"
                    }
                    if (startTime == null || (track.startTime != null && track.startTime < startTime)) {
                        startTime = track.startTime
                    }
                    if (startTime == null || (track.stopTime != null && track.stopTime > stopTime)) {
                        stopTime = track.startTime
                    }
                    totalDistanceMeter += track.totalDistanceMeter
                    totalTime = totalTime?.let { it + (track.totalTime ?: Duration.ZERO) }
                        ?: track.totalTime
                    movingTime = movingTime?.let { it + (track.movingTime ?: Duration.ZERO) }
                        ?: track.movingTime
                    totalAvgSpeedMeterPerSecond = totalAvgSpeedMeterPerSecond?.let {
                        it + (track.avgSpeedMeterPerSecond ?: 0.0)
                    } ?: track.avgSpeedMeterPerSecond
                    totalAvgMovingSpeedMeterPerSecond = totalAvgMovingSpeedMeterPerSecond?.let {
                        it + (track.avgMovingSpeedMeterPerSecond ?: 0.0)
                    } ?: track.avgMovingSpeedMeterPerSecond
                    maxSpeedMeterPerSecond = if (maxSpeedMeterPerSecond == null) {
                        track.maxSpeedMeterPerSecond
                    } else {
                        max(
                            maxSpeedMeterPerSecond!!,
                            track.maxSpeedMeterPerSecond ?: maxSpeedMeterPerSecond!!
                        )
                    }
                    minElevationMeter = if (minElevationMeter == null) {
                        track.minElevationMeter
                    } else {
                        min(minElevationMeter!!, track.minElevationMeter ?: minElevationMeter!!)
                    }
                    maxElevationMeter = if (maxElevationMeter == null) {
                        track.maxElevationMeter
                    } else {
                        max(maxElevationMeter!!, track.maxElevationMeter ?: maxElevationMeter!!)
                    }
                    elevationGainMeter =
                        elevationGainMeter?.let { it + (track.elevationGainMeter ?: 0.0) }
                            ?: track.elevationGainMeter
                }

                avgSpeedMeterPerSecond = totalAvgSpeedMeterPerSecond?.div(tracks.size)
                avgMovingSpeedMeterPerSecond = totalAvgMovingSpeedMeterPerSecond?.div(tracks.size)
            }
        }
    }

}
