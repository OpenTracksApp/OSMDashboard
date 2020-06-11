package de.storchp.opentracks.osmplugin.maps;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import androidx.annotation.Nullable;

import static android.content.Context.SENSOR_SERVICE;

public class CompassView extends androidx.appcompat.widget.AppCompatImageView implements SensorEventListener {

    private SensorManager sensorManager;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private boolean lastAccelerometerSet = false;
    private boolean lastMagnetometerSet = false;
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private int lastDegreePos = -1;
    private final float[] lastDegrees = new float[5];
    private float currentDegree = 0;

    public CompassView(final Context context) {
        super(context);
        createSensorManager(context);
    }

    public CompassView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        createSensorManager(context);
    }

    public CompassView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        createSensorManager(context);
    }

    private void createSensorManager(final Context context) {
        this.sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

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

    @Override
    protected void onDetachedFromWindow() {
        sensorManager.unregisterListener(this);
        super.onDetachedFromWindow();
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
            final RotateAnimation ra = new RotateAnimation(
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
            startAnimation(ra);
            currentDegree = newDegree;
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

}
