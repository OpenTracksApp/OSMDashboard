package de.storchp.opentracks.osmplugin.utils;

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
     * Formats the elapsed timed in the form "MM:SS" or "H:MM:SS".
     */
    public static String formatElapsedTime(final int seconds) {
        return DateUtils.formatElapsedTime(seconds);
    }

    /**
     * Formats the elapsed time in the form "H:MM:SS".
     */
    public static String formatElapsedTimeWithHour(final int millis) {
        final String value = formatElapsedTime(millis/1000);
        return TextUtils.split(value, ":").length == 2 ? "0:" + value : value;
    }

    /**
     * Formats the distance in meters.
     *
     * @param context     the context
     * @param distanceMeter    the distance
     */
    public static String formatDistance(final Context context, final float distanceMeter) {
        return context.getString(R.string.distance_with_unit, formatDecimal(distanceMeter / 1000), context.getString(R.string.unit_kilometer));
    }

    private static String formatDecimal(final double value) {
        return StringUtils.formatDecimal(value, 2);
    }

    /**
     * Format a decimal number while removing trailing zeros of the decimal part (if present).
     */
    public static String formatDecimal(final double value, final int decimalPlaces) {
        final DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(decimalPlaces);
        df.setMaximumFractionDigits(decimalPlaces);
        df.setRoundingMode(RoundingMode.HALF_EVEN);
        return df.format(value);
    }

    public static String formatAltitudeChange(final Context context, final float altitude_m) {
        return context.getString(R.string.altitude_with_unit, String.valueOf((int) altitude_m), context.getString(R.string.unit_meter));
    }

}
