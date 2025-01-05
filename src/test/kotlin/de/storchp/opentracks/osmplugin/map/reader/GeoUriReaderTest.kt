package de.storchp.opentracks.osmplugin.map.reader

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.oscim.core.GeoPoint

internal class GeoUriReaderTest {

    @Test
    fun fromGeoUriWithName() {
        val waypoint = fromGeoUri("geo:0,0?q=50.123,-5.456(Marker 0)")
        assertThat(waypoint.latLong).isEqualTo(GeoPoint(50.123, -5.456))
        assertThat(waypoint.name).isEqualTo("Marker 0")
    }

    @Test
    fun fromGeoUriWithoutName() {
        val waypoint = fromGeoUri("geo:0,0?q=50.123,-5.456")
        assertThat(waypoint.latLong).isEqualTo(GeoPoint(50.123, -5.456))
        assertThat(waypoint.name).isNull()
    }

    @Test
    fun fromGeoUriWithoutQueryPart() {
        val waypoint = fromGeoUri("geo:50.123,-5.456")
        assertThat(waypoint.latLong).isEqualTo(GeoPoint(50.123, -5.456))
        assertThat(waypoint.name).isNull()
    }
}