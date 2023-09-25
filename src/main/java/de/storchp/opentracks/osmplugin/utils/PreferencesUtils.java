package de.storchp.opentracks.osmplugin.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;
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

    public static String getLastDownloadUrl(String defaultDownloadUrl) {
        return getString(R.string.APP_PREF_LAST_DOWNLOAD_URL, defaultDownloadUrl);
    }

    public static void setLastDownloadUrl(String lastDownloadUrl) {
        setString(R.string.APP_PREF_LAST_DOWNLOAD_URL, lastDownloadUrl);
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

    private static float getFloat(int keyId, float defaultValue) {
        return sharedPrefs.getFloat(getKey(keyId), defaultValue);
    }

    private static void setFloat(int keyId, float value) {
        sharedPrefs.edit()
                .putFloat(getKey(keyId), value)
                .apply();
    }

    public static int getTrackSmoothingTolerance() {
        return getInt(R.string.APP_PREF_TRACK_SMOOTHING_TOLERANCE, resources.getInteger(R.integer.track_smoothing_tolerance_default));
    }

    public static boolean isPipEnabled() {
        return getBoolean(R.string.APP_PREF_PIP_ENABLED, resources.getBoolean(R.bool.pip_enabled_default));
    }

    public static ArrowMode getArrowMode() {
        return ArrowMode.valueOf(getString(R.string.APP_PREF_ARROW_MODE, ArrowMode.DIRECTION.name()), ArrowMode.valueOf(resources.getString(R.string.arrow_mode_default)));
    }

    public static MapMode getMapMode() {
        return MapMode.valueOf(getString(R.string.APP_PREF_MAP_MODE, MapMode.NORTH.name()), MapMode.valueOf(resources.getString(R.string.map_mode_default)));
    }

    public static int getCompassSmoothing() {
        return getInt(R.string.APP_PREF_COMPASS_SMOOTHING, resources.getInteger(R.integer.compass_smoothing_default));
    }

    public static boolean getMultiThreadMapRendering() {
        return getBoolean(R.string.APP_PREF_MAP_MULTI_THREAD_RENDERING, resources.getBoolean(R.bool.map_multi_thread_rendering_default));
    }

    public static boolean getPersistentTileCache() {
        return getBoolean(R.string.APP_PREF_MAP_PERSISTENT_TILECACHE, resources.getBoolean(R.bool.map_persistent_tilecache_default));
    }

    public static int getStrokeWidth() {
        return getInt(R.string.APP_PREF_STROKE_WIDTH, resources.getInteger(R.integer.stroke_width_default));
    }

    public static double getOverdrawFactor() {
        return getFloat(R.string.APP_PREF_MAP_OVERDRAW_FACTOR, ResourcesCompat.getFloat(resources, R.dimen.map_overdraw_factor_default));
    }

    public static void setOverdrawFactor(double overdrawFactor) {
        setFloat(R.string.APP_PREF_MAP_OVERDRAW_FACTOR, (float) overdrawFactor);
    }

    public static float getTileCacheCapacityFactor() {
        return getFloat(R.string.APP_PREF_MAP_TILE_CACHE_CAPACITY_FACTOR, ResourcesCompat.getFloat(resources, R.dimen.tile_cache_capacity_factor_default));
    }

    public static void setTileCacheCapacityFactor(float tileCacheCapacityFactor) {
        setFloat(R.string.APP_PREF_MAP_TILE_CACHE_CAPACITY_FACTOR, tileCacheCapacityFactor);
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

}
