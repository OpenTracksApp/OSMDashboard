package de.storchp.opentracks.osmplugin.utils;

import android.content.Context;

public enum StatisticElement {
    CATEGORY{
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return statistics.getCategory();
        }
    },
    TOTAL_TIME{
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatElapsedTimeHoursMinutes(statistics.getTotalTimeMillis());
        }
    },
    MOVING_TIME{
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatElapsedTimeHoursMinutes(statistics.getMovingTimeMillis());
        }
    },
    DISTANCE{
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatDistanceInKm(context, statistics.getTotalDistanceMeter());
        }
    },
    ELEVATION_GAIN_METER{
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatAltitudeChange(context, statistics.getElevationGainMeter());
        }
    },
    SPEED_KM_H{
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatSpeedInKmPerHour(context, statistics.getAvgMovingSpeedMeterPerSecond());
        }
    },
    PACE_MIN_KM{
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatPaceMinPerKm(context, statistics.getAvgMovingSpeedMeterPerSecond());
        }
    };

    public abstract String getText(Context context, TrackStatistics statistics);

}
