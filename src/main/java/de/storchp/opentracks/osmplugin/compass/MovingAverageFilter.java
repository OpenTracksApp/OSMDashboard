package de.storchp.opentracks.osmplugin.compass;

import java.util.ArrayList;
import java.util.List;

/**
 * Derived from <a href="https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/weather/domain/MovingAverageFilter.kt">...</a>
 */
public class MovingAverageFilter {

    private final int size;
    private final List<Double> window = new ArrayList<>();

    MovingAverageFilter(int size) {
        this.size = size;
    }

    public double filter(double measurement) {
        window.add(measurement);
        if (window.size() > size){
            window.remove(0);
        }
        return window.stream().mapToDouble(e->e).average().orElse(0);
    }

}
