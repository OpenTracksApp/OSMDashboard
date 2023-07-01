package de.storchp.opentracks.osmplugin.dashboardapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mapsforge.core.model.LatLong;

class WaypointTest {

    @Test
    void fromGeoUriWithName() {
        var waypoint = Waypoint.fromGeoUri("geo:0,0?q=50.123,-5.456(Marker 0)");
        assertThat(waypoint).hasValueSatisfying(s -> {
            assertThat(s.getLatLong()).isEqualTo(new LatLong(50.123, -5.456));
            assertThat(s.getName()).isEqualTo("Marker 0");
        });
    }

    @Test
    void fromGeoUriWithoutName() {
        var waypoint = Waypoint.fromGeoUri("geo:0,0?q=50.123,-5.456");
        assertThat(waypoint).hasValueSatisfying(s -> {
            assertThat(s.getLatLong()).isEqualTo(new LatLong(50.123, -5.456));
            assertThat(s.getName()).isNull();
        });
    }

    @Test
    void fromGeoUriWithoutQueryPart() {
        var waypoint = Waypoint.fromGeoUri("geo:50.123,-5.456");
        assertThat(waypoint).hasValueSatisfying(s -> {
            assertThat(s.getLatLong()).isEqualTo(new LatLong(50.123, -5.456));
            assertThat(s.getName()).isNull();
        });
    }

 }