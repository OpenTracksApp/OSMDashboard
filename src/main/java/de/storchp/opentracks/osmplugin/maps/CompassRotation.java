package de.storchp.opentracks.osmplugin.maps;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import static android.content.Context.SENSOR_SERVICE;

public class CompassRotation implements SensorEventListener {

    private SensorManager sensorManager;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private boolean lastAccelerometerSet = false;
    private boolean lastMagnetometerSet = false;
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private int lastDegreePos = -1;
    private final float[] lastDegrees = new float[5];
    private RotationListener listener = null;
    private float currentDegrees = 0;

    public CompassRotation(final Context context) {
        createSensorManager(context);
    }

    private void createSensorManager(final Context context) {
        this.sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
    }

    public void onStart() {
        final Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        final Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void onStop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
            lastAccelerometerSet = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
            lastMagnetometerSet = true;
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            final float azimuthInRadians = orientationAngles[0];
            final float azimuthInDegress = (float)(Math.toDegrees(azimuthInRadians)+360)%360;
            float newDegrees = -azimuthInDegress;
            if (lastDegreePos == -1) {
                initLastDegrees(newDegrees);
                lastDegreePos = 1;
            } else {
                lastDegreePos++;
                lastDegreePos %=lastDegrees.length;
                lastDegrees[lastDegreePos] = newDegrees;
                newDegrees = averageLastDegrees();
            }
            currentDegrees = newDegrees;
            if (listener != null) {
                listener.onRotationUpdate(currentDegrees);
            }
        }
    }

    private void initLastDegrees(final float newDegree) {
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
    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {

    }

    public void setRotationListener(final RotationListener listener) {
        this.listener = listener;
    }

    public float getCurrentDegrees() {
        return currentDegrees;
    }

}
