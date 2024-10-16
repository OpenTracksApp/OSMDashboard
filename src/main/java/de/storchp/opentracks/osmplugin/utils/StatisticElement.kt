package de.storchp.opentracks.osmplugin.utils

import android.content.Context
import de.storchp.opentracks.osmplugin.R
import java.lang.IllegalArgumentException

enum class StatisticElement {
    CATEGORY {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return context.getString(R.string.stat_category, statistics.getCategory())
        }
    },
    TOTAL_TIME {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatElapsedTimeHoursMinutes(
                context,
                statistics.getTotalTimeMillis()
            )
        }
    },
    MOVING_TIME {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatElapsedTimeHoursMinutes(
                context,
                statistics.getMovingTimeMillis()
            )
        }
    },
    DISTANCE_KM {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatDistanceInKm(context, statistics.getTotalDistanceMeter())
        }
    },
    DISTANCE_MI {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatDistanceInMi(context, statistics.getTotalDistanceMeter())
        }
    },
    SPEED_KM_H {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatSpeedInKmPerHour(
                context,
                statistics.getAvgMovingSpeedMeterPerSecond()
            )
        }
    },
    PACE_MIN_KM {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatPaceMinPerKm(
                context,
                statistics.getAvgMovingSpeedMeterPerSecond()
            )
        }
    },
    SPEED_MI_H {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatSpeedInMiPerHour(
                context,
                statistics.getAvgMovingSpeedMeterPerSecond()
            )
        }
    },
    PACE_MIN_MI {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatPaceMinPerMi(
                context,
                statistics.getAvgMovingSpeedMeterPerSecond()
            )
        }
    },
    ELEVATION_GAIN_METER {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatAltitudeChangeInMeter(
                context,
                statistics.getElevationGainMeter()
            )
        }
    },
    ELEVATION_GAIN_FEET {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatAltitudeChangeInFeet(
                context,
                statistics.getElevationGainMeter()
            )
        }
    };

    abstract fun getText(context: Context, statistics: TrackStatistics): String?

    companion object {
        fun of(name: String?) =
            try {
                StatisticElement.valueOf(name!!)
            } catch (_: IllegalArgumentException) {
                null
            }
    }
}
