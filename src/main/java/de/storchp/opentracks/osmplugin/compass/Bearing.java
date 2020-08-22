package de.storchp.opentracks.osmplugin.compass;

/**
 * Derived from https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/navigation/domain/compass/Bearing.kt
 */
public class Bearing {

    private final float value;

    Bearing(final float value){
        this.value = AngleMath.normalizeAngle(value);
    }

    public float getCurrentDegrees() {
        return -value;
    }

}
