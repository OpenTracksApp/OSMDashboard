package de.storchp.opentracks.osmplugin.compass;

/**
 * Derived from https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/shared/math/AngleMath.kt
 */
public class AngleMath {

    /**
     * Converts an angle to between 0 and 360
     * @param angle the angle in degrees
     * @return the normalized angle
     */
    public static float normalizeAngle(final float angle) {
        float outputAngle = angle;
        while (outputAngle < 0) {
            outputAngle += 360;
        }
        return outputAngle % 360;
    }

    public static float deltaAngle(final float angle1, final float angle2) {
        float delta = angle2 - angle1;
        delta += 180;
        delta -= Math.floor(delta / 360) * 360;
        delta -= 180;
        if (Math.abs(Math.abs(delta) - 180) <= Float.MIN_VALUE) {
            delta = 180f;
        }
        return delta;
    }

}
