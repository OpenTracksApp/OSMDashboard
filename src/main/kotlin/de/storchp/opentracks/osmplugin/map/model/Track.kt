package de.storchp.opentracks.osmplugin.map.model

data class Track(
    val id: Long,
    val trackname: String?,
    val description: String?,
    val category: String?,
    val startTimeEpochMillis: Int,
    val stopTimeEpochMillis: Int,
    val totalDistanceMeter: Float,
    val totalTimeMillis: Int,
    val movingTimeMillis: Int,
    val avgSpeedMeterPerSecond: Float,
    val avgMovingSpeedMeterPerSecond: Float,
    val maxSpeedMeterPerSecond: Float,
    val minElevationMeter: Float,
    val maxElevationMeter: Float,
    val elevationGainMeter: Float
)
