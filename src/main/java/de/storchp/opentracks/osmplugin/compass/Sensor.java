package de.storchp.opentracks.osmplugin.compass;

/**
 * https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/shared/sensors/ISensor.kt
 */
public interface Sensor {

    void start(SensorListener listener);

    void stop(SensorListener listener);

}
