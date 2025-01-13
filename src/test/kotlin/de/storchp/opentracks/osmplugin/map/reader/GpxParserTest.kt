package de.storchp.opentracks.osmplugin.map.reader

import de.storchp.opentracks.osmplugin.map.model.Track
import de.storchp.opentracks.osmplugin.map.model.Trackpoint
import de.storchp.opentracks.osmplugin.map.model.Waypoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.oscim.core.GeoPoint
import java.time.Instant
import javax.xml.parsers.SAXParserFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class GpxParserTest {

    @Test
    fun parseGpxTrack() {
        val sut = GpxParser()
        SAXParserFactory.newInstance().newSAXParser()
            .parse(GpxParserTest::class.java.getResourceAsStream("/track.gpx"), sut)

        assertThat(sut.waypoints).containsExactlyInAnyOrder(
            Waypoint(
                name = "Marker 0",
                description = "",
                category = "",
                latLong = GeoPoint(53.559962, 9.989374)
            ),
            Waypoint(
                name = "Marker 1",
                description = "",
                category = "",
                latLong = GeoPoint(53.56363, 9.986106)
            ),
        )
        assertThat(sut.tracks).containsExactly(
            Track(
                id = 1,
                name = "2023-12-29T09:37+01",
                description = "",
                category = "walking",
                startTime = Instant.ofEpochMilli(1703839066375),
                stopTime = Instant.ofEpochMilli(1703839661518),
                totalDistanceMeter = 809.648193359375,
                totalTime = 598.seconds,
                movingTime = 583.seconds,
                avgSpeedMeterPerSecond = 1.4905882352941182,
                avgMovingSpeedMeterPerSecond = 1.5451219512195127,
                maxSpeedMeterPerSecond = 2.13,
                minElevationMeter = 43.2,
                maxElevationMeter = 56.3,
                elevationGainMeter = 3.0,
            )
        )
        assertThat(sut.tracksBySegments.segments).hasSize(1)
        val segment = sut.tracksBySegments.segments.first()
        assertThat(segment).hasSize(85)
        assertThat(segment.first()).isEqualTo(
            Trackpoint(
                latLong = GeoPoint(53.559632, 9.989175),
                type = 0,
                speed = 0.0,
                elevation = 55.9,
                time = Instant.parse("2023-12-29T08:37:46.375Z")
            )
        )
        assertThat(segment.last()).isEqualTo(
            Trackpoint(
                latLong = GeoPoint(53.565765, 9.982826),
                type = 0,
                speed = 1.34,
                elevation = 55.3,
                time = Instant.parse("2023-12-29T08:47:41.518Z")
            )
        )
    }

    @Test
    fun parseGpxRoute() {
        val sut = GpxParser()
        SAXParserFactory.newInstance().newSAXParser()
            .parse(GpxParserTest::class.java.getResourceAsStream("/route.gpx"), sut)

        assertThat(sut.tracks).containsExactly(
            Track(
                id = 1,
                name = "lenitzsee-zusatz-",
                description = "Generated from track lenitzsee-zusatz-",
                category = null,
                startTime = null,
                stopTime = null,
                totalDistanceMeter = 0.0,
                totalTime = Duration.ZERO,
                movingTime = Duration.ZERO,
                avgSpeedMeterPerSecond = 0.0,
                avgMovingSpeedMeterPerSecond = 0.0,
                maxSpeedMeterPerSecond = 0.0,
                minElevationMeter = 0.0,
                maxElevationMeter = 0.0,
                elevationGainMeter = 0.0,
            )
        )
        assertThat(sut.tracksBySegments.segments).hasSize(1)
        val segment = sut.tracksBySegments.segments.first()
        assertThat(segment).hasSize(7)
        assertThat(segment.first()).isEqualTo(
            Trackpoint(
                latLong = GeoPoint(52.765593, 13.279984),
                type = 0,
                speed = 0.0,
                elevation = 0.0,
                time = null,
                name = "RPT001"
            )
        )
        assertThat(segment.last()).isEqualTo(
            Trackpoint(
                latLong = GeoPoint(52.766719, 13.281122),
                type = 0,
                speed = 0.0,
                elevation = 0.0,
                time = null,
                name = "RPT007"
            )
        )
    }

    @Test
    fun parseGpxRouteNoElevation() {
        val sut = GpxParser()
        SAXParserFactory.newInstance().newSAXParser()
            .parse(GpxParserTest::class.java.getResourceAsStream("/route-noelevation.gpx"), sut)

        assertThat(sut.tracks).containsExactly(
            Track(
                id = 1,
                name = "RPT015",
                description = "Generated from track biesdorf",
                category = null,
                startTime = null,
                stopTime = null,
                totalDistanceMeter = 0.0,
                totalTime = Duration.ZERO,
                movingTime = Duration.ZERO,
                avgSpeedMeterPerSecond = 0.0,
                avgMovingSpeedMeterPerSecond = 0.0,
                maxSpeedMeterPerSecond = 0.0,
                minElevationMeter = 0.0,
                maxElevationMeter = 0.0,
                elevationGainMeter = 0.0,
            )
        )
        assertThat(sut.tracksBySegments.segments).hasSize(1)
        val segment = sut.tracksBySegments.segments.first()
        assertThat(segment).hasSize(7)
        assertThat(segment.first()).isEqualTo(
            Trackpoint(
                latLong = GeoPoint(52.505294657, 13.560877026),
                type = 0,
                speed = 0.0,
                elevation = null,
                time = null,
                name = "RPT001"
            )
        )
        assertThat(segment.last()).isEqualTo(
            Trackpoint(
                latLong = GeoPoint(52.504075066, 13.566812754),
                type = 0,
                speed = 0.0,
                elevation = null,
                time = null,
                name = "RPT018"
            )
        )
    }

}