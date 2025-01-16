package de.storchp.opentracks.osmplugin.map.model

import java.time.Instant
import kotlin.time.Duration

data class Track(
    val id: Long,
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val startTime: Instant? = null,
    val stopTime: Instant? = null,
    val totalDistanceMeter: Double,
    val totalTime: Duration? = null,
    val movingTime: Duration? = null,
    val avgSpeedMeterPerSecond: Double? = null,
    val avgMovingSpeedMeterPerSecond: Double? = null,
    val maxSpeedMeterPerSecond: Double? = null,
    val minElevationMeter: Double? = null,
    val maxElevationMeter: Double? = null,
    val elevationGainMeter: Double? = null,
)
