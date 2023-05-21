package de.storchp.opentracks.osmplugin.compass;

import de.storchp.opentracks.osmplugin.utils.MapUtils;

/**
 * Derived from <a href="https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/navigation/domain/compass/Bearing.kt">...</a>
 */
public class Bearing {

    private final float value;

    Bearing(float value){
        this.value = MapUtils.normalizeAngle(value);
    }

    public float getValue() {
        return value;
    }

}
