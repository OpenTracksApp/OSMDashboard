package de.storchp.opentracks.osmplugin.maps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class CompassListener implements SensorEventListener {

    private SensorManager sensorManager;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private boolean lastAccelerometerSet = false;
    private boolean lastMagnetometerSet = false;
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private ImageView compassView;
    private int lastDegreePos = -1;
    private float[] lastDegrees = new float[5];
    private float currentDegree = 0;

    public CompassListener(SensorManager sensorManager, ImageView compassView) {
        this.sensorManager = sensorManager;
        this.compassView = compassView;
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

    public void onPause() {
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

        if (lastAccelerometerSet && lastMagnetometerSet && compassView != null) {
            SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            float azimuthInRadians = orientationAngles[0];
            float azimuthInDegress = (float)(Math.toDegrees(azimuthInRadians)+360)%360;
            float newDegree = -azimuthInDegress;
            if (lastDegreePos == -1) {
                initLastDegrees(newDegree);
                lastDegreePos = 1;
            } else {
                lastDegreePos++;
                lastDegreePos %=lastDegrees.length;
                lastDegrees[lastDegreePos] = newDegree;
                newDegree = averageLastDegrees();
            }
            RotateAnimation ra = new RotateAnimation(
                    currentDegree,
                    newDegree,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);
            // how long the animation will take place
            ra.setDuration(0);

            // set the animation after the end of the reservation status
            ra.setFillAfter(true);

            // Start the animation
            compassView.startAnimation(ra);
            currentDegree = newDegree;
        }
    }

    private void initLastDegrees(float newDegree) {
        for(int i= 1;i <lastDegrees.length;i++){
            lastDegrees[i] = newDegree;
        }
    }

    private float averageLastDegrees() {
        int difference = 0;
        for(int i= 1;i <lastDegrees.length;i++){
            difference += ( (lastDegrees[i]- lastDegrees[0] + 180 + 360 ) % 360 ) - 180;
        }
        return (360 + lastDegrees[0] + ( difference / (float)lastDegrees.length ) ) % 360;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
