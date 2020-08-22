package de.storchp.opentracks.osmplugin.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

/**
 * Derived from https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/shared/sensors/LowPassAccelerometer.kt
 */
public class LowPassAccelerometer extends BaseSensor implements Accelerometer {

    private final float filterSize = 0.05f;

    private final LowPassFilter[] filters = new LowPassFilter[] {
        new LowPassFilter(filterSize),
        new LowPassFilter(filterSize),
        new LowPassFilter(filterSize)
    };

    private Vector3 acceleration = Vector3.ZERO;

    public LowPassAccelerometer(final Context context)  {
        super(context, Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void handleSensorEvent(final SensorEvent event) {
        acceleration = new Vector3(
            filters[0].filter(event.values[0]),
            filters[1].filter(event.values[1]),
            filters[2].filter(event.values[2])
        );
    }

    @Override
    public Vector3 getAcceleration() {
        return acceleration;
    }
}
