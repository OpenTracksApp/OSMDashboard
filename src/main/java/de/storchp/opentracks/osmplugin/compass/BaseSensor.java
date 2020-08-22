package de.storchp.opentracks.osmplugin.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Derived from https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/shared/sensors/BaseSensor.kt
 */
public abstract class BaseSensor extends AbstractSensor {

    private final SensorManager sensorManager;
    protected final Context context;
    protected final int sensorType;
    protected final int sensorDelay;

    public BaseSensor(final Context context, final int sensorType, final int sensorDelay) {
        this.context = context;
        this.sensorType = sensorType;
        this.sensorDelay = sensorDelay;
        this.sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
    }

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(final SensorEvent event) {
            handleSensorEvent(event);
            notifyListeners();
        }

        @Override
        public void onAccuracyChanged(final Sensor sensor, final int newAccuracy) {
        }

    };

    protected void startImpl() {
        final Sensor sensor = sensorManager.getDefaultSensor(sensorType);
        if (sensor != null) {
            sensorManager.registerListener(
                sensorListener,
                sensor,
                sensorDelay
            );
        }
    }

    protected void stopImpl() {
        sensorManager.unregisterListener(sensorListener);
    }

    protected abstract void handleSensorEvent(final SensorEvent event);

}
