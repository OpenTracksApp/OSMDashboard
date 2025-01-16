package de.storchp.opentracks.osmplugin.map.reader

import de.storchp.opentracks.osmplugin.map.model.Track
import de.storchp.opentracks.osmplugin.map.model.Trackpoint
import de.storchp.opentracks.osmplugin.map.model.Waypoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.oscim.core.GeoPoint
import java.time.Instant
import javax.xml.parsers.SAXParserFactory
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
                totalDistanceMeter = 203.4179809008669,
                minElevationMeter = 0.0,
                maxElevationMeter = 0.0,
            )
        )
        assertThat(sut.tracksBySegments.segments).hasSize(1)
        val segment = sut.tracksBySegments.segments.first()
        assertThat(segment).hasSize(7)
        assertThat(segment.first()).isEqualTo(
            Trackpoint(
                latLong = GeoPoint(52.765593, 13.279984),
                elevation = 0.0,
                name = "RPT001"
            )
        )
        assertThat(segment.last()).isEqualTo(
            Trackpoint(
                latLong = GeoPoint(52.766719, 13.281122),
                elevation = 0.0,
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
                totalDistanceMeter = 468.38468683124336,
            )
        )
        assertThat(sut.tracksBySegments.segments).hasSize(1)
        val segment = sut.tracksBySegments.segments.first()
        assertThat(segment).hasSize(7)
        assertThat(segment.first()).isEqualTo(
            Trackpoint(
                latLong = GeoPoint(52.505294657, 13.560877026),
                name = "RPT001"
            )
        )
        assertThat(segment.last()).isEqualTo(
            Trackpoint(
                latLong = GeoPoint(52.504075066, 13.566812754),
                name = "RPT018",
            )
        )
    }

    @Test
    fun parseAATGpxTrack() {
        val sut = GpxParser()
        SAXParserFactory.newInstance().newSAXParser()
            .parse(GpxParserTest::class.java.getResourceAsStream("/aat.gpx"), sut)

        assertThat(sut.tracks).containsExactly(
            Track(
                id = 1,
                startTime = Instant.parse("2024-04-27T12:30:46Z"),
                stopTime = Instant.parse("2024-04-27T12:32:11Z"),
                totalDistanceMeter = 73.7530943518343,
                minElevationMeter = 70.5,
                maxElevationMeter = 74.5,
            )
        )
        assertThat(sut.tracksBySegments.segments).hasSize(1)
        val segment = sut.tracksBySegments.segments.first()
        assertThat(segment).hasSize(19)
        assertThat(segment.first()).isEqualTo(
            Trackpoint(
                latLong = GeoPoint(52.773146, 13.337713),
                elevation = 74.5,
                time = Instant.parse("2024-04-27T12:30:46Z"),
            )
        )
        assertThat(segment.last()).isEqualTo(
            Trackpoint(
                latLong = GeoPoint(52.772648, 13.338123),
                elevation = 73.4,
                time = Instant.parse("2024-04-27T12:32:11Z"),
            )
        )
    }

}