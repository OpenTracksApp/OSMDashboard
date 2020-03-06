package de.storchp.opentracks.osmplugin;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class Compass  implements SensorEventListener {

    private SensorManager sensorManager;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private boolean lastAccelerometerSet = false;
    private boolean lastMagnetometerSet = false;
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private ImageView compass;
    private float currentDegree = 0;
    private long lastCompassUpdate = 0;

    public Compass(SensorManager sensorManager, ImageView compass) {
        this.sensorManager = sensorManager;
        this.compass = compass;
    }

    public void onResume() {
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
    }

    protected void onPause() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
            lastAccelerometerSet = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
            lastMagnetometerSet = true;
        }

        if (lastAccelerometerSet && lastMagnetometerSet && compass != null && (System.currentTimeMillis() - lastCompassUpdate > 250)) {
            lastCompassUpdate = System.currentTimeMillis();
            SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            float azimuthInRadians = orientationAngles[0];
            float azimuthInDegress = (float)(Math.toDegrees(azimuthInRadians)+360)%360;
            RotateAnimation ra = new RotateAnimation(
                    currentDegree,
                    -azimuthInDegress,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);
            // how long the animation will take place
            ra.setDuration(0);

            // set the animation after the end of the reservation status
            ra.setFillAfter(true);

            // Start the animation
            compass.startAnimation(ra);
            currentDegree = -azimuthInDegress;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
