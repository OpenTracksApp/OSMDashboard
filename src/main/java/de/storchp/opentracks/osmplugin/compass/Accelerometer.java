package de.storchp.opentracks.osmplugin.compass;

/**
 * Derived from https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/shared/sensors/IAccelerometer.kt
 */
public interface Accelerometer extends Sensor {

    Vector3 getAcceleration();

}
