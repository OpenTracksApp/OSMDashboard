package de.storchp.opentracks.osmplugin.utils

import android.content.Context
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.map.TrackStatistics
import java.lang.IllegalArgumentException

enum class StatisticElement {
    CATEGORY {
        override fun getText(context: Context, statistics: TrackStatistics): String? {
            return statistics.category?.let { context.getString(R.string.stat_category, it) }
        }
    },
    TOTAL_TIME {
        override fun getText(context: Context, statistics: TrackStatistics): String? {
            return statistics.totalTime?.let {
                StringUtils.formatElapsedTimeHoursMinutes(context, it)
            }
        }
    },
    MOVING_TIME {
        override fun getText(context: Context, statistics: TrackStatistics): String? {
            return statistics.movingTime?.let {
                StringUtils.formatElapsedTimeHoursMinutes(context, it)
            }
        }
    },
    DISTANCE_KM {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatDistanceInKm(context, statistics.totalDistanceMeter)
        }
    },
    DISTANCE_MI {
        override fun getText(context: Context, statistics: TrackStatistics): String {
            return StringUtils.formatDistanceInMi(context, statistics.totalDistanceMeter)
        }
    },
    SPEED_KM_H {
        override fun getText(context: Context, statistics: TrackStatistics): String? {
            return statistics.avgMovingSpeedMeterPerSecond?.let {
                StringUtils.formatSpeedInKmPerHour(context, it)
            }
        }
    },
    PACE_MIN_KM {
        override fun getText(context: Context, statistics: TrackStatistics): String? {
            return statistics.avgMovingSpeedMeterPerSecond?.let {
                StringUtils.formatPaceMinPerKm(context, it)
            }
        }
    },
    SPEED_MI_H {
        override fun getText(context: Context, statistics: TrackStatistics): String? {
            return statistics.avgMovingSpeedMeterPerSecond?.let {
                StringUtils.formatSpeedInMiPerHour(context, it)
            }
        }
    },
    PACE_MIN_MI {
        override fun getText(context: Context, statistics: TrackStatistics): String? {
            return statistics.avgMovingSpeedMeterPerSecond?.let {
                StringUtils.formatPaceMinPerMi(context, it)
            }
        }
    },
    ELEVATION_GAIN_METER {
        override fun getText(context: Context, statistics: TrackStatistics): String? {
            return statistics.elevationGainMeter?.let {
                StringUtils.formatAltitudeChangeInMeter(context, it)
            }
        }
    },
    ELEVATION_GAIN_FEET {
        override fun getText(context: Context, statistics: TrackStatistics): String? {
            return statistics.elevationGainMeter?.let {
                StringUtils.formatAltitudeChangeInFeet(context, it)
            }
        }
    };

    abstract fun getText(context: Context, statistics: TrackStatistics): String?

}

fun String.toStatisticElement() =
    try {
        StatisticElement.valueOf(this)
    } catch (_: IllegalArgumentException) {
        null
    }