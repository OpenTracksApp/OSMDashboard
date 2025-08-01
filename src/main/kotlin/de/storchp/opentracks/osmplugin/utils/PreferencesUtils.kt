package de.storchp.opentracks.osmplugin.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Resources
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.map.MapMode
import de.storchp.opentracks.osmplugin.map.SpeedColors
import de.storchp.opentracks.osmplugin.map.TrackColorMode
import de.storchp.opentracks.osmplugin.map.toMapMode

@Suppress("SameParameterValue")
object PreferencesUtils {
    private val TAG: String = PreferencesUtils::class.java.getSimpleName()
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var resources: Resources

    fun initPreferences(context: Context) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        resources = context.resources
    }

    private fun getKey(@StringRes keyId: Int) = resources.getString(keyId)

    fun getTrackColors() =
        listOf(
            getInt(
                R.string.APP_PREF_TRACK_COLOR_1,
                resources.getColor(R.color.track_color_1_default, null)
            ),
            getInt(
                R.string.APP_PREF_TRACK_COLOR_2,
                resources.getColor(R.color.track_color_2_default, null)
            ),
            getInt(
                R.string.APP_PREF_TRACK_COLOR_3,
                resources.getColor(R.color.track_color_3_default, null)
            ),
            getInt(
                R.string.APP_PREF_TRACK_COLOR_4,
                resources.getColor(R.color.track_color_4_default, null)
            ),
            getInt(
                R.string.APP_PREF_TRACK_COLOR_5,
                resources.getColor(R.color.track_color_5_default, null)
            ),
        )

    fun getTrackSpeedColors() =
        SpeedColors(
            getInt(
                R.string.APP_PREF_TRACK_COLOR_SPEED_LOW,
                resources.getColor(R.color.track_color_speed_low_default, null)
            ),
            getInt(
                R.string.APP_PREF_TRACK_COLOR_SPEED_AVERAGE,
                resources.getColor(R.color.track_color_speed_average_default, null)
            ),
            getInt(
                R.string.APP_PREF_TRACK_COLOR_SPEED_HIGH,
                resources.getColor(R.color.track_color_speed_high_default, null)
            )
        )

    fun getMapUris(): Set<Uri> {
        return getUris(getKey(R.string.APP_PREF_MAP_FILES))
    }

    fun setMapUris(mapUris: Set<Uri>) {
        setUris(R.string.APP_PREF_MAP_FILES, mapUris)
    }

    fun getMapDirectoryUri(): Uri? {
        return getUri(getKey(R.string.APP_PREF_MAP_DIRECTORY))
    }

    fun setMapDirectoryUri(mapDirectory: Uri?) {
        setUri(R.string.APP_PREF_MAP_DIRECTORY, mapDirectory)
    }

    fun getMapThemeDirectoryUri() = getUri(getKey(R.string.APP_PREF_MAP_THEME_DIRECTORY))

    fun setMapThemeDirectoryUri(mapThemeDirectory: Uri?) {
        setUri(R.string.APP_PREF_MAP_THEME_DIRECTORY, mapThemeDirectory)
    }

    fun getMapThemeUri() = getUri(getKey(R.string.APP_PREF_MAP_THEME))

    fun setMapThemeUri(mapTheme: Uri?) {
        setUri(R.string.APP_PREF_MAP_THEME, mapTheme)
    }

    fun getOnlineMapConsent() =
        getBoolean(
            R.string.APP_PREF_ONLINE_MAP_CONSENT,
            resources.getBoolean(R.bool.online_map_consent_default)
        )

    fun setOnlineMapConsent(onlineMapConsent: Boolean) {
        setBoolean(R.string.APP_PREF_ONLINE_MAP_CONSENT, onlineMapConsent)
    }

    fun isShowPauseMarkers() =
        getBoolean(
            R.string.APP_PREF_SHOW_PAUSE_MARKERS,
            resources.getBoolean(R.bool.show_pause_markers_default)
        )

    private fun getUris(keyId: String) =
        sharedPrefs.getStringSet(
            keyId,
            emptySet<String>()
        )!!
            .mapNotNull(::parseUri)
            .toSet()

    private fun getUri(keyId: String): Uri? {
        return parseUri(sharedPrefs.getString(keyId, null))
    }

    private fun parseUri(value: String?) =
        try {
            value?.toUri()
        } catch (_: Exception) {
            Log.e(TAG, "can't read Uri string $value")
            null
        }

    private fun setUri(keyId: Int, uri: Uri?) {
        setString(keyId, uri?.toString())
    }

    private fun setUris(keyId: Int, uris: Set<Uri>) {
        setStringSet(
            keyId,
            uris
                .map { it.toString() }
                .toSet())
    }

    private fun getString(keyId: Int, defaultValue: String?) =
        sharedPrefs.getString(
            getKey(keyId),
            defaultValue
        )

    private fun setString(keyId: Int, value: String?) {
        sharedPrefs.edit {
            putString(getKey(keyId), value)
        }
    }

    private fun setStringSet(keyId: Int, values: Set<String>) {
        sharedPrefs.edit {
            putStringSet(getKey(keyId), values)
        }
    }

    private fun getStringSet(keyId: Int, defaultValue: Set<String>) =
        sharedPrefs.getStringSet(
            getKey(keyId),
            defaultValue
        )!!

    private fun getBoolean(keyId: Int, defaultValue: Boolean) =
        sharedPrefs.getBoolean(
            getKey(keyId),
            defaultValue
        )

    private fun setBoolean(keyId: Int, value: Boolean) {
        sharedPrefs.edit {
            putBoolean(getKey(keyId), value)
        }
    }

    private fun getInt(keyId: Int, defaultValue: Int) =
        sharedPrefs.getInt(getKey(keyId), defaultValue)

    fun getTrackSmoothingTolerance() =
        getInt(
            R.string.APP_PREF_TRACK_SMOOTHING_TOLERANCE,
            resources.getInteger(R.integer.track_smoothing_tolerance_default)
        )

    fun isPipEnabled() =
        getBoolean(
            R.string.APP_PREF_PIP_ENABLED,
            resources.getBoolean(R.bool.pip_enabled_default)
        )

    fun getMapMode() =
        getString(
            R.string.APP_PREF_MAP_MODE,
            MapMode.NORTH.name
        )!!.toMapMode(
            MapMode.valueOf(resources.getString(R.string.map_mode_default))
        )

    fun getStrokeWidth() =
        getInt(
            R.string.APP_PREF_STROKE_WIDTH,
            resources.getInteger(R.integer.stroke_width_default)
        )

    fun getStatisticElements() =
        getStringSet(
            R.string.APP_PREF_STATISTIC_ELEMENTS,
            setOf(*resources.getStringArray(R.array.statistic_elements_defaults))
        )
            .mapNotNull(String::toStatisticElement)
            .toSet()

    fun isDebugTrackPoints() =
        getBoolean(
            R.string.APP_PREF_DEBUG_TRACKPOPINTS,
            resources.getBoolean(R.bool.debug_trackpoints_default)
        )

    fun getTrackColorMode(): TrackColorMode {
        val trackColorMode = getString(
            R.string.APP_PREF_TRACK_COLOR_MODE,
            resources.getString(R.string.track_color_mode_default)
        )!!
        return TrackColorMode.valueOf(trackColorMode)
    }

    fun registerOnSharedPreferenceChangeListener(changeListener: OnSharedPreferenceChangeListener) {
        sharedPrefs.registerOnSharedPreferenceChangeListener(changeListener)
        changeListener.onSharedPreferenceChanged(sharedPrefs, null)
    }

    fun unregisterOnSharedPreferenceChangeListener(changeListener: OnSharedPreferenceChangeListener?) {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(changeListener)
    }

    /**
     * Compares if keyId and key belong to the same shared preference key.
     *
     * @param keyId The resource id of the key
     * @param key   The key of the preference
     * @return true if key == null or key belongs to keyId
     */
    fun isKey(keyId: Int, key: String?) = key == null || key == getKey(keyId)

    fun shouldUseDynamicColors(): Boolean {
        val default = resources.getBoolean(R.bool.settings_ui_dynamic_colors_default)
        return getBoolean(R.string.settings_ui_dynamic_colors_key, default)
    }

    /**
     * @return [AppCompatDelegate].MODE_*
     */
    fun getDefaultNightMode(): Int {
        val defaultValue = getKey(R.string.night_mode_default)
        val value = getString(R.string.night_mode_key, defaultValue)

        return value!!.toInt()
    }

    fun applyNightMode() {
        AppCompatDelegate.setDefaultNightMode(getDefaultNightMode())
    }
}
