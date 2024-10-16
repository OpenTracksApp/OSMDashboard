package de.storchp.opentracks.osmplugin.utils;

import static de.storchp.opentracks.osmplugin.utils.UnitConversions.KM_TO_M;
import static de.storchp.opentracks.osmplugin.utils.UnitConversions.KM_TO_MI;
import static de.storchp.opentracks.osmplugin.utils.UnitConversions.MIN_TO_S;
import static de.storchp.opentracks.osmplugin.utils.UnitConversions.MS_TO_KMH;
import static de.storchp.opentracks.osmplugin.utils.UnitConversions.MS_TO_S;
import static de.storchp.opentracks.osmplugin.utils.UnitConversions.M_TO_KM;
import static de.storchp.opentracks.osmplugin.utils.UnitConversions.M_TO_MI;

import android.content.Context;
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
    public static String formatElapsedTimeHoursMinutes(Context context, int millis) {
        return context.getString(R.string.stat_time, DateUtils.formatElapsedTime((long)(millis * MS_TO_S)));
    }

    public static String formatDistanceInKm(Context context, float distanceMeter) {
        return context.getString(R.string.stat_distance_with_unit, formatDecimal(distanceMeter * M_TO_KM), context.getString(R.string.unit_kilometer));
    }

    public static String formatDistanceInMi(Context context, float distanceMeter) {
        return context.getString(R.string.stat_distance_with_unit, formatDecimal(distanceMeter * M_TO_MI), context.getString(R.string.unit_mile));
    }

    public static String formatSpeedInKmPerHour(Context context, float meterPerSeconds) {
        return context.getString(R.string.stat_distance_with_unit, formatDecimal(meterPerSeconds * MS_TO_KMH), context.getString(R.string.unit_kilometer_per_hour));
    }

    public static String formatSpeedInMiPerHour(Context context, float meterPerSeconds) {
        return context.getString(R.string.stat_distance_with_unit, formatDecimal(meterPerSeconds * MS_TO_KMH * KM_TO_MI), context.getString(R.string.unit_mile_per_hour));
    }

    public static String formatPaceMinPerKm(Context context, float meterPerSeconds) {
        if (meterPerSeconds == 0) {
            return "0:00";
        }
        float kmPerSecond = meterPerSeconds / (float)KM_TO_M;
        int secondsPerKm = (int)(1 / kmPerSecond);
        int minutes = (int)(secondsPerKm / MIN_TO_S);
        int seconds = secondsPerKm % (int)MIN_TO_S;

        return context.getString(R.string.stat_distance_with_unit, context.getString(R.string.stat_minute_seconds, minutes, seconds), context.getString(R.string.unit_minute_per_kilometer));
    }

    public static String formatPaceMinPerMi(Context context, float meterPerSeconds) {
        if (meterPerSeconds == 0) {
            return "0:00";
        }
        double kmPerSecond = (meterPerSeconds / KM_TO_M) * KM_TO_MI;
        int secondsPerKm = (int)(1 / kmPerSecond);
        int minutes = (int)(secondsPerKm / MIN_TO_S);
        int seconds = secondsPerKm % (int)MIN_TO_S;

        return context.getString(R.string.stat_distance_with_unit, context.getString(R.string.stat_minute_seconds, minutes, seconds), context.getString(R.string.unit_minute_per_mile));
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

    public static String formatAltitudeChangeInMeter(Context context, float altitudeInMeter) {
        return context.getString(R.string.stat_altitude_with_unit, String.valueOf((int) altitudeInMeter), context.getString(R.string.unit_meter));
    }

    public static String formatAltitudeChangeInFeet(Context context, float altitudeInMeter) {
        return context.getString(R.string.stat_altitude_with_unit, String.valueOf((int)(altitudeInMeter * UnitConversions.M_TO_FT)), context.getString(R.string.unit_feet));
    }

}
