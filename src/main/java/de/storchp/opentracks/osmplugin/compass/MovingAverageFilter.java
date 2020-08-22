package de.storchp.opentracks.osmplugin.compass;

import java.util.ArrayList;
import java.util.List;

/**
 * Derived from https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/weather/domain/MovingAverageFilter.kt
 */
public class MovingAverageFilter {

    private final int size;
    private final List<Double> window = new ArrayList<>();

    MovingAverageFilter(final int size) {
        this.size = size;
    }

    public double filter(final double measurement) {
        window.add(measurement);
        if (window.size() > size){
            window.remove(0);
        }
        return window.stream().mapToDouble(e->e).average().getAsDouble();
    }

}
