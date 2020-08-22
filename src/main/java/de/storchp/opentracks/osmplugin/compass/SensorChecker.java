package de.storchp.opentracks.osmplugin.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import java.util.List;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Derived from https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/shared/sensors/SensorChecker.kt
 */
public class SensorChecker {

    public static boolean hasGravity(final Context context) {
        final SensorManager sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        final List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_GRAVITY);
        return sensors != null && !sensors.isEmpty();
    }

}
