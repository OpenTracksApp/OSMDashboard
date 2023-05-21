package de.storchp.opentracks.osmplugin.compass;

import static android.content.Context.SENSOR_SERVICE;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

/**
 * Derived from <a href="https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/shared/sensors/SensorChecker.kt">...</a>
 */
public class SensorChecker {

    public static boolean hasGravity(Context context) {
        var sensors = ((SensorManager) context.getSystemService(SENSOR_SERVICE)).getSensorList(Sensor.TYPE_GRAVITY);
        return sensors != null && !sensors.isEmpty();
    }

}
