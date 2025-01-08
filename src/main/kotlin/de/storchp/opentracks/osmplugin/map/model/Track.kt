package de.storchp.opentracks.osmplugin.map.model

import java.time.Instant

data class Track(
    val id: Long,
    val trackname: String?,
    val description: String?,
    val category: String?,
    val startTime: Instant?,
    val stopTime: Instant?,
    val totalDistanceMeter: Double,
    val totalTimeMillis: Long,
    val movingTimeMillis: Long,
    val avgSpeedMeterPerSecond: Double,
    val avgMovingSpeedMeterPerSecond: Double,
    val maxSpeedMeterPerSecond: Double,
    val minElevationMeter: Double,
    val maxElevationMeter: Double,
    val elevationGainMeter: Double
)
