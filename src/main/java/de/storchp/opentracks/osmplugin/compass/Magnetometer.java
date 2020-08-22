package de.storchp.opentracks.osmplugin.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

/**
 * Derived from https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/shared/sensors/Magnetometer.kt
 */
public class Magnetometer extends BaseSensor {

    private final float filterSize = 0.03f;
    private final LowPassFilter[] filters = {
            new LowPassFilter(filterSize),
            new LowPassFilter(filterSize),
            new LowPassFilter(filterSize)};

    private Vector3 magneticField = Vector3.ZERO;

    public Magnetometer(final Context context) {
        super(context, Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void handleSensorEvent(final SensorEvent event) {
        magneticField = new Vector3(
            filters[0].filter(event.values[0]),
            filters[1].filter(event.values[1]),
            filters[2].filter(event.values[2])
        );
    }

    public Vector3 getMagneticField() {
        return magneticField;
    }

}
