package de.storchp.opentracks.osmplugin.compass;

/**
 * Derived from https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/weather/domain/LowPassFilter.kt
 */
public class LowPassFilter {

    private final float alpha;
    private float estimate;

    public LowPassFilter(float alpha, float estimate) {
        this.alpha = alpha;
        this.estimate = estimate;
    }

    public LowPassFilter(float alpha) {
        this(alpha, 0f);
    }

    public float filter(float measurement) {
        estimate = (1 - alpha) * estimate + alpha * measurement;
        return estimate;
    }

}
