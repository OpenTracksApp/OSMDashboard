package de.storchp.opentracks.osmplugin.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

/**
 * Derived from https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/shared/sensors/Magnetometer.kt
 */
public class Magnetometer extends AbstractLowPassSensor {

    public Magnetometer(final Context context) {
        super(context, Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_GAME, 0.03f);
    }

}
