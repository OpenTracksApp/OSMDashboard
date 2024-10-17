package de.storchp.opentracks.osmplugin.dashboardapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.oscim.core.GeoPoint

internal class WaypointTest {

    @Test
    fun fromGeoUriWithName() {
        val waypoint = WaypointReader.fromGeoUri("geo:0,0?q=50.123,-5.456(Marker 0)")
        assertThat(waypoint?.latLong).isEqualTo(GeoPoint(50.123, -5.456))
        assertThat(waypoint?.name).isEqualTo("Marker 0")
    }

    @Test
    fun fromGeoUriWithoutName() {
        val waypoint = WaypointReader.fromGeoUri("geo:0,0?q=50.123,-5.456")
        assertThat(waypoint?.latLong).isEqualTo(GeoPoint(50.123, -5.456))
        assertThat(waypoint?.name).isNull()
    }

    @Test
    fun fromGeoUriWithoutQueryPart() {
        val waypoint = WaypointReader.fromGeoUri("geo:50.123,-5.456")
        assertThat(waypoint?.latLong).isEqualTo(GeoPoint(50.123, -5.456))
        assertThat(waypoint?.name).isNull()
    }
}