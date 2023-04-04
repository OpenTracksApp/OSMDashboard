package de.storchp.opentracks.osmplugin.utils;

import android.content.Context;

import de.storchp.opentracks.osmplugin.R;

public enum StatisticElement {
    CATEGORY(R.string.category) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return context.getString(R.string.stat_category, statistics.getCategory());
        }
    },
    TOTAL_TIME(R.string.total_time) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatElapsedTimeHoursMinutes(context, statistics.getTotalTimeMillis());
        }
    },
    MOVING_TIME(R.string.moving_time) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatElapsedTimeHoursMinutes(context, statistics.getMovingTimeMillis());
        }
    },
    DISTANCE_KM(R.string.distance_km) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatDistanceInKm(context, statistics.getTotalDistanceMeter());
        }
    },
    DISTANCE_MI(R.string.distance_mi) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatDistanceInMi(context, statistics.getTotalDistanceMeter());
        }
    },
    ELEVATION_GAIN_METER(R.string.elevation_meter) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatAltitudeChangeInMeter(context, statistics.getElevationGainMeter());
        }
    },
    ELEVATION_GAIN_FEET(R.string.elevation_feet) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatAltitudeChangeInFeet(context, statistics.getElevationGainMeter());
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
    },
    SPEED_MI_H(R.string.speed_mi_h) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatSpeedInMiPerHour(context, statistics.getAvgMovingSpeedMeterPerSecond());
        }
    },
    PACE_MIN_MI(R.string.pace_min_mi) {
        @Override
        public String getText(Context context, TrackStatistics statistics) {
            return StringUtils.formatPaceMinPerMi(context, statistics.getAvgMovingSpeedMeterPerSecond());
        }
    };

    private final int labelResId;

    StatisticElement(int labelResId) {
        this.labelResId = labelResId;
    }

    public static StatisticElement of(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public abstract String getText(Context context, TrackStatistics statistics);

    public int getLabelResId() {
        return labelResId;
    }
}
