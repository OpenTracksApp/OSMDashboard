package de.storchp.opentracks.osmplugin.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

/**
 * Derived from <a href="https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/shared/sensors/GravitySensor.kt">...</a>
 */
public class GravitySensor extends AbstractLowPassSensor {

    public GravitySensor(Context context)  {
        super(context, Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_GAME, 0.03f);
    }

}
