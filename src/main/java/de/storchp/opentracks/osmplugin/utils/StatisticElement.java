package de.storchp.opentracks.osmplugin.utils;

import android.content.Context;

import de.storchp.opentracks.osmplugin.R;

public enum StatisticElement {
    CATEGORY(R.string.category) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return statistics.getCategory();
        }
    },
    TOTAL_TIME(R.string.total_time) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatElapsedTimeHoursMinutes(statistics.getTotalTimeMillis());
        }
    },
    MOVING_TIME(R.string.moving_time) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatElapsedTimeHoursMinutes(statistics.getMovingTimeMillis());
        }
    },
    DISTANCE(R.string.distance_km) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatDistanceInKm(context, statistics.getTotalDistanceMeter());
        }
    },
    ELEVATION_GAIN_METER(R.string.elevation_meter) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatAltitudeChange(context, statistics.getElevationGainMeter());
        }
    },
    SPEED_KM_H(R.string.speed_km_h) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatSpeedInKmPerHour(context, statistics.getAvgMovingSpeedMeterPerSecond());
        }
    },
    PACE_MIN_KM(R.string.pace_min_km) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatPaceMinPerKm(context, statistics.getAvgMovingSpeedMeterPerSecond());
        }
    };

    private final int labelResId;

    StatisticElement(final int labelResId) {
        this.labelResId = labelResId;
    }

    public abstract String getText(Context context, TrackStatistics statistics);

    public int getLabelResId() {
        return labelResId;
    }
}
