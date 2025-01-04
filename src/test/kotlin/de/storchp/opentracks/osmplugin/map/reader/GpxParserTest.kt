package de.storchp.opentracks.osmplugin.map.reader

import de.storchp.opentracks.osmplugin.map.model.Track
import de.storchp.opentracks.osmplugin.map.model.Trackpoint
import de.storchp.opentracks.osmplugin.map.model.Waypoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.oscim.core.GeoPoint
import java.time.Instant
import javax.xml.parsers.SAXParserFactory

internal class GpxParserTest {

    @Test
    fun parseGpx() {
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
                trackname = "2023-12-29T09:37+01",
                description = "",
                category = "walking",
                startTimeEpochMillis = 0,
                stopTimeEpochMillis = 0,
                totalDistanceMeter = 0f,
                totalTimeMillis = 0,
                movingTimeMillis = 0,
                avgSpeedMeterPerSecond = 0f,
                avgMovingSpeedMeterPerSecond = 0f,
                maxSpeedMeterPerSecond = 0f,
                minElevationMeter = 0f,
                maxElevationMeter = 0f,
                elevationGainMeter = 0f,
            )
        )
        assertThat(sut.tracksBySegments.segments).hasSize(1)
        val segment = sut.tracksBySegments.segments.first()
        assertThat(segment).hasSize(85)
        assertThat(segment.first()).isEqualTo(
            Trackpoint(
                latitude = 53.559632,
                longitude = 9.989175,
                type = 0,
                speed = 0.0,
                time = Instant.parse("2023-12-29T08:37:46.375Z")
            )
        )
        assertThat(segment.last()).isEqualTo(
            Trackpoint(
                latitude = 53.565765,
                longitude = 9.982826,
                type = 0,
                speed = 1.34,
                time = Instant.parse("2023-12-29T08:47:41.518Z")
            )
        )
    }

}