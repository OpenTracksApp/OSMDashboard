package de.storchp.opentracks.osmplugin.utils;

import android.content.Context;

import de.storchp.opentracks.osmplugin.R;

public enum StatisticElement {
    CATEGORY() {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return context.getString(R.string.stat_category, statistics.getCategory());
        }
    },
    TOTAL_TIME() {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatElapsedTimeHoursMinutes(context, statistics.getTotalTimeMillis());
        }
    },
    MOVING_TIME() {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatElapsedTimeHoursMinutes(context, statistics.getMovingTimeMillis());
        }
    },
    DISTANCE_KM() {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatDistanceInKm(context, statistics.getTotalDistanceMeter());
        }
    },
    DISTANCE_MI() {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatDistanceInMi(context, statistics.getTotalDistanceMeter());
        }
    },
    SPEED_KM_H() {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatSpeedInKmPerHour(context, statistics.getAvgMovingSpeedMeterPerSecond());
        }
    },
    PACE_MIN_KM() {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatPaceMinPerKm(context, statistics.getAvgMovingSpeedMeterPerSecond());
        }
    },
    SPEED_MI_H() {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatSpeedInMiPerHour(context, statistics.getAvgMovingSpeedMeterPerSecond());
        }
    },
    PACE_MIN_MI() {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatPaceMinPerMi(context, statistics.getAvgMovingSpeedMeterPerSecond());
        }
    },
    ELEVATION_GAIN_METER() {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatAltitudeChangeInMeter(context, statistics.getElevationGainMeter());
        }
    },
    ELEVATION_GAIN_FEET() {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatAltitudeChangeInFeet(context, statistics.getElevationGainMeter());
        }
    };

    public static StatisticElement of(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public abstract String getText(Context context, TrackStatistics statistics);

}
