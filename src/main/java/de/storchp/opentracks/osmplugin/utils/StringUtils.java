package de.storchp.opentracks.osmplugin.utils;

import static de.storchp.opentracks.osmplugin.utils.UnitConversions.MS_TO_KMH;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.math.RoundingMode;
import java.text.DecimalFormat;

import de.storchp.opentracks.osmplugin.R;

/**
 * Various string manipulation methods.
 */
public class StringUtils {

    private StringUtils() {
    }

    /**
     * Formats the elapsed time in the form "H:MM".
     */
    public static String formatElapsedTimeHoursMinutes(int millis) {
        return DateUtils.formatElapsedTime(millis / 1000);
    }

    public static String formatDistanceInKm(Context context, float distanceMeter) {
        return context.getString(R.string.distance_with_unit, formatDecimal(distanceMeter / 1000), context.getString(R.string.unit_kilometer));
    }

    public static String formatSpeedInKmPerHour(Context context, float meterPerSeconds) {
        return context.getString(R.string.distance_with_unit, formatDecimal(meterPerSeconds * MS_TO_KMH), context.getString(R.string.unit_kilometer_per_hour));
    }

    public static String formatPaceMinPerKm(Context context, float meterPerSeconds) {
        if (meterPerSeconds == 0) {
            return "0:00";
        }
        float kmPerSecond = meterPerSeconds / 1000;
        int secondsPerKm = (int)(1 / kmPerSecond);
        int minutes = secondsPerKm / 60;
        int seconds = secondsPerKm % 60;

        return context.getString(R.string.distance_with_unit, context.getString(R.string.time, minutes, seconds), context.getString(R.string.unit_minute_per_kilometer));
    }

    private static String formatDecimal(double value) {
        return StringUtils.formatDecimal(value, 2);
    }

    /**
     * Format a decimal number while removing trailing zeros of the decimal part (if present).
     */
    public static String formatDecimal(double value, int decimalPlaces) {
        var df = new DecimalFormat();
        df.setMinimumFractionDigits(decimalPlaces);
        df.setMaximumFractionDigits(decimalPlaces);
        df.setRoundingMode(RoundingMode.HALF_EVEN);
        return df.format(value);
    }

    public static String formatAltitudeChange(Context context, float altitude_m) {
        return context.getString(R.string.altitude_with_unit, String.valueOf((int) altitude_m), context.getString(R.string.unit_meter));
    }

}
