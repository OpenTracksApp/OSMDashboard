package de.storchp.opentracks.osmplugin.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import de.storchp.opentracks.osmplugin.R;

public class PreferencesUtils {

    private final static String TAG = PreferencesUtils.class.getSimpleName();
    private static SharedPreferences sharedPrefs;
    private static Resources resources;

    public static void initPreferences(Context context) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        resources = context.getResources();
    }

    private static String getKey(@StringRes int keyId) {
        return resources.getString(keyId);
    }

    public static Set<Uri> getMapUris() {
        return getUris(getKey(R.string.APP_PREF_MAP_FILES));
    }

    public static void setMapUris(Set<Uri> mapUris) {
        setUris(R.string.APP_PREF_MAP_FILES, mapUris);
    }

    public static Uri getMapDirectoryUri() {
        return getUri(getKey(R.string.APP_PREF_MAP_DIRECTORY));
    }

    public static void setMapDirectoryUri(Uri mapDirectory) {
        setUri(R.string.APP_PREF_MAP_DIRECTORY, mapDirectory);
    }

    public static Uri getMapThemeDirectoryUri() {
        return getUri(getKey(R.string.APP_PREF_MAP_THEME_DIRECTORY));
    }

    public static void setMapThemeDirectoryUri(Uri mapThemeDirectory) {
        setUri(R.string.APP_PREF_MAP_THEME_DIRECTORY, mapThemeDirectory);
    }

    public static Uri getMapThemeUri() {
        return getUri(getKey(R.string.APP_PREF_MAP_THEME));
    }

    public static void setMapThemeUri(Uri mapTheme) {
        setUri(R.string.APP_PREF_MAP_THEME, mapTheme);
    }

    public static boolean getOnlineMapConsent() {
        return getBoolean(R.string.APP_PREF_ONLINE_MAP_CONSENT, resources.getBoolean(R.bool.online_map_consent_default));
    }

    public static void setOnlineMapConsent(boolean onlineMapConsent) {
        setBoolean(R.string.APP_PREF_ONLINE_MAP_CONSENT, onlineMapConsent);
    }

    public static boolean isShowPauseMarkers() {
        return getBoolean(R.string.APP_PREF_SHOW_PAUSE_MARKERS, resources.getBoolean(R.bool.show_pause_markers_default));
    }

    private static Set<Uri> getUris(String keyId) {
        return sharedPrefs.getStringSet(keyId, Collections.emptySet()).stream()
                .map(PreferencesUtils::parseUri)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static Uri getUri(String keyId) {
        return parseUri(sharedPrefs.getString(keyId, null));
    }

    private static Uri parseUri(String value) {
        try {
            return Uri.parse(value);
        } catch (Exception ignored) {
            Log.e(TAG, "can't read Uri string " + value);
        }
        return null;
    }

    private static void setUri(int keyId, Uri uri) {
        setString(keyId, uri != null ? uri.toString() : null);
    }

    private static void setUris(int keyId, Set<Uri> uris) {
        setStringSet(keyId,
                uris.stream()
                        .map(Uri::toString)
                        .collect(Collectors.toSet()));
    }

    private static String getString(int keyId, String defaultValue) {
        return sharedPrefs.getString(getKey(keyId), defaultValue);
    }

    private static void setString(int keyId, String value) {
        sharedPrefs.edit()
                .putString(getKey(keyId), value)
                .apply();
    }

    private static void setStringSet(int keyId, Set<String> values) {
        sharedPrefs.edit()
                .putStringSet(getKey(keyId), values)
                .apply();
    }

    private static Set<String> getStringSet(int keyId, Set<String> defaultValue) {
        return sharedPrefs.getStringSet(getKey(keyId), defaultValue);
    }

    private static boolean getBoolean(int keyId, boolean defaultValue) {
        return sharedPrefs.getBoolean(getKey(keyId), defaultValue);
    }

    private static void setBoolean(int keyId, boolean value) {
        sharedPrefs.edit()
                .putBoolean(getKey(keyId), value)
                .apply();
    }

    private static int getInt(int keyId, int defaultValue) {
        return sharedPrefs.getInt(getKey(keyId), defaultValue);
    }

    public static int getTrackSmoothingTolerance() {
        return getInt(R.string.APP_PREF_TRACK_SMOOTHING_TOLERANCE, resources.getInteger(R.integer.track_smoothing_tolerance_default));
    }

    public static boolean isPipEnabled() {
        return getBoolean(R.string.APP_PREF_PIP_ENABLED, resources.getBoolean(R.bool.pip_enabled_default));
    }

    public static MapMode getMapMode() {
        return MapMode.valueOf(getString(R.string.APP_PREF_MAP_MODE, MapMode.NORTH.name()), MapMode.valueOf(resources.getString(R.string.map_mode_default)));
    }

    public static int getStrokeWidth() {
        return getInt(R.string.APP_PREF_STROKE_WIDTH, resources.getInteger(R.integer.stroke_width_default));
    }

    public static Set<StatisticElement> getStatisticElements() {
        return getStringSet(R.string.APP_PREF_STATISTIC_ELEMENTS, Set.of(resources.getStringArray(R.array.statistic_elements_defaults))).stream()
                .map(StatisticElement::of)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static boolean isDebugTrackPoints() {
        return getBoolean(R.string.APP_PREF_DEBUG_TRACKPOPINTS, resources.getBoolean(R.bool.debug_trackpoints_default));
    }

    public static TrackColorMode getTrackColorMode() {
        var trackColorMode = getString(R.string.APP_PREF_TRACK_COLOR_MODE, resources.getString(R.string.track_color_mode_default));
        return TrackColorMode.valueOf(trackColorMode);
    }

    public static void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener changeListener) {
        sharedPrefs.registerOnSharedPreferenceChangeListener(changeListener);
        changeListener.onSharedPreferenceChanged(sharedPrefs, null);
    }

    public static void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener changeListener) {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(changeListener);
    }

    /**
     * Compares if keyId and key belong to the same shared preference key.
     *
     * @param keyId The resource id of the key
     * @param key   The key of the preference
     * @return true if key == null or key belongs to keyId
     */
    public static boolean isKey(int keyId, String key) {
        return key == null || key.equals(getKey(keyId));
    }

    public static boolean shouldUseDynamicColors() {
        final boolean DEFAULT = resources.getBoolean(R.bool.settings_ui_dynamic_colors_default);
        return getBoolean(R.string.settings_ui_dynamic_colors_key, DEFAULT);
    }

    /**
     * @return {@link androidx.appcompat.app.AppCompatDelegate}.MODE_*
     */
    public static int getDefaultNightMode() {
        final String defaultValue = getKey(R.string.night_mode_default);
        final String value = getString(R.string.night_mode_key, defaultValue);

        return Integer.parseInt(value);
    }

    public static void applyNightMode() {
        AppCompatDelegate.setDefaultNightMode(PreferencesUtils.getDefaultNightMode());
    }

}
