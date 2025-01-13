package de.storchp.opentracks.osmplugin.map.model

import java.time.Instant
import kotlin.time.Duration

data class Track(
    val id: Long,
    val name: String?,
    val description: String?,
    val category: String?,
    val startTime: Instant?,
    val stopTime: Instant?,
    val totalDistanceMeter: Double,
    val totalTime: Duration,
    val movingTime: Duration,
    val avgSpeedMeterPerSecond: Double,
    val avgMovingSpeedMeterPerSecond: Double,
    val maxSpeedMeterPerSecond: Double,
    val minElevationMeter: Double,
    val maxElevationMeter: Double,
    val elevationGainMeter: Double
)
