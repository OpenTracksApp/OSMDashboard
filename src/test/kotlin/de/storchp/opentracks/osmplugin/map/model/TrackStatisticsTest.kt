package de.storchp.opentracks.osmplugin.map.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class TrackStatisticsTest {

    @Test
    fun createSingleStatistic() {
        val stats = TrackStatistics(listOf(track1))

        assertThat(stats.category).isEqualTo(track1.category)
        assertThat(stats.startTime).isEqualTo(track1.startTime)
        assertThat(stats.stopTime).isEqualTo(track1.stopTime)
        assertThat(stats.totalDistanceMeter).isEqualTo(track1.totalDistanceMeter)
        assertThat(stats.movingTime!!).isEqualTo(track1.movingTime!!)
        assertThat(stats.avgSpeedMeterPerSecond).isEqualTo(track1.avgSpeedMeterPerSecond)
        assertThat(stats.avgMovingSpeedMeterPerSecond).isEqualTo(track1.avgMovingSpeedMeterPerSecond)
        assertThat(stats.maxSpeedMeterPerSecond).isEqualTo(track1.maxSpeedMeterPerSecond)
        assertThat(stats.minElevationMeter).isEqualTo(track1.minElevationMeter)
        assertThat(stats.maxElevationMeter).isEqualTo(track1.maxElevationMeter)
        assertThat(stats.elevationGainMeter).isEqualTo(track1.elevationGainMeter)
    }

    @Test
    fun createCombinedStatistic() {
        val stats = TrackStatistics(listOf(track1, track2))

        assertThat(stats.category).isEqualTo("mixed")
        assertThat(stats.startTime).isEqualTo(Instant.parse("1970-01-01T00:10:00Z"))
        assertThat(stats.stopTime).isEqualTo(Instant.parse("1970-01-01T00:35:00Z"))
        assertThat(stats.totalDistanceMeter).isEqualTo(1100.0)
        assertThat(stats.movingTime).isEqualTo(1500.seconds)
        assertThat(stats.avgSpeedMeterPerSecond).isEqualTo(5.0)
        assertThat(stats.avgMovingSpeedMeterPerSecond).isEqualTo(3.0)
        assertThat(stats.maxSpeedMeterPerSecond).isEqualTo(7.0)
        assertThat(stats.minElevationMeter).isEqualTo(10.0)
        assertThat(stats.maxElevationMeter).isEqualTo(40.0)
        assertThat(stats.elevationGainMeter).isEqualTo(42.0)
    }
}

private val track1 = Track(
    id = 1,
    category = "walking",
    startTime = Instant.parse("1970-01-01T00:10:00Z"),
    stopTime = Instant.parse("1970-01-01T00:20:00Z"),
    totalDistanceMeter = 100.0,
    totalTime = 600.seconds,
    movingTime = 500.seconds,
    avgSpeedMeterPerSecond = 4.0,
    avgMovingSpeedMeterPerSecond = 2.0,
    maxSpeedMeterPerSecond = 5.0,
    minElevationMeter = 10.0,
    maxElevationMeter = 20.0,
    elevationGainMeter = 10.0,
)

private val track2 = Track(
    id = 2,
    category = "running",
    startTime = Instant.parse("1970-01-01T00:15:00Z"),
    stopTime = Instant.parse("1970-01-01T00:35:00Z"),
    totalDistanceMeter = 1000.0,
    totalTime = 1200.seconds,
    movingTime = 1000.seconds,
    avgSpeedMeterPerSecond = 6.0,
    avgMovingSpeedMeterPerSecond = 4.0,
    maxSpeedMeterPerSecond = 7.0,
    minElevationMeter = 12.0,
    maxElevationMeter = 40.0,
    elevationGainMeter = 32.0,
)
