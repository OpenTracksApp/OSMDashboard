package de.storchp.opentracks.osmplugin.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MapUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "1.0, 1.0, true",
            "-1.0, -1.0, true",
            "1.0, 0.0, true",
            "0.0, 0.0, false",
            "91.0, 1.0, false",
            "-91.0, 1.0, false",
            "1.0, 181.0, false",
            "1.0, -181.0, false"
    })
    void isValid(double latitude, double longitude, boolean valid) {
        assertThat(MapUtils.isValid(latitude, longitude)).isEqualTo(valid);
    }
}