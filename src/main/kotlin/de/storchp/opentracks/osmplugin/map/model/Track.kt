package de.storchp.opentracks.osmplugin.map.model

data class Track(
    val id: Long,
    val trackname: String?,
    val description: String?,
    val category: String?,
    val startTimeEpochMillis: Long,
    val stopTimeEpochMillis: Long,
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
