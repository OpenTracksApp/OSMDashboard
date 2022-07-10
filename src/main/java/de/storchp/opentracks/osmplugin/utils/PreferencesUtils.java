package de.storchp.opentracks.osmplugin.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import de.storchp.opentracks.osmplugin.R;

public class PreferencesUtils {

    private final static String TAG = PreferencesUtils.class.getSimpleName();
    private static SharedPreferences sharedPrefs;
    private static Resources mRes;

    public static void initPreferences(Context context) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mRes = context.getResources();
    }

    private static String getKey(@StringRes int keyId) {
        return mRes.getString(keyId);
    }

    public static Set<Uri> getMapUris() {
        return getUris(getKey(R.string.MAP_FILES));
    }

    public static void setMapUris(Set<Uri> mapUris) {
        setUris(R.string.MAP_FILES, mapUris);
    }

    public static Uri getMapDirectoryUri() {
        return getUri(getKey(R.string.MAP_DIRECTORY));
    }

    public static void setMapDirectoryUri(Uri mapDirectory) {
        setUri(R.string.MAP_DIRECTORY, mapDirectory);
    }

    public static Uri getMapThemeDirectoryUri() {
        return getUri(getKey(R.string.MAP_THEME_DIRECTORY));
    }

    public static void setMapThemeDirectoryUri(Uri mapThemeDirectory) {
        setUri(R.string.MAP_THEME_DIRECTORY, mapThemeDirectory);
    }

    public static Uri getMapThemeUri() {
        return getUri(getKey(R.string.MAP_THEME));
    }

    public static void setMapThemeUri(Uri mapTheme) {
        setUri(R.string.MAP_THEME, mapTheme);
    }

    public static boolean getOnlineMapConsent() {
        return getBoolean(R.string.ONLINE_MAP_CONSENT, false);
    }

    public static void setOnlineMapConsent(boolean onlineMapConsent) {
        setBoolean(R.string.ONLINE_MAP_CONSENT, onlineMapConsent);
    }

    public static String getLastDownloadUrl(String defaultDownloadUrl) {
        return getString(R.string.LAST_DOWNLOAD_URL, defaultDownloadUrl);
    }

    public static void setLastDownloadUrl(String lastDownloadUrl) {
        setString(R.string.LAST_DOWNLOAD_URL, lastDownloadUrl);
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

    private static void setInt(int keyId, int value) {
        sharedPrefs.edit()
                .putInt(getKey(keyId), value)
                .apply();
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
        return getInt(R.string.TRACK_SMOOTHING_TOLERANCE, 10);
    }

    public static void setTrackSmoothingTolerance(int value) {
        setInt(R.string.TRACK_SMOOTHING_TOLERANCE, value);
    }

    public static boolean isPipEnabled() {
        return getBoolean(R.string.PIP_ENABLED, true);
    }

    public static void setPipEnabled(boolean enabled) {
        setBoolean(R.string.PIP_ENABLED, enabled);
    }

    public static ArrowMode getArrowMode() {
        return ArrowMode.valueOf(getString(R.string.ARROW_MODE, ArrowMode.DIRECTION.name()), ArrowMode.DIRECTION);
    }

    public static void setArrowMode(ArrowMode arrowMode) {
        setString(R.string.ARROW_MODE, arrowMode.name());
    }

    public static MapMode getMapMode() {
        return MapMode.valueOf(getString(R.string.MAP_MODE, MapMode.NORTH.name()), MapMode.NORTH);
    }

    public static void setMapMode(MapMode mapMode) {
        setString(R.string.MAP_MODE, mapMode.name());
    }

    public static int getCompassSmoothing() {
        return getInt(R.string.COMPASS_SMOOTHING, 2);
    }

    public static void setCompassSmoothing(int value) {
        setInt(R.string.COMPASS_SMOOTHING, value);
    }

    public static boolean getMultiThreadMapRendering() {
        return getBoolean(R.string.MAP_MULTI_THREAD_RENDERING, true);
    }

    public static void setMultiThreadMapRendering(boolean multiThread) {
        setBoolean(R.string.MAP_MULTI_THREAD_RENDERING, multiThread);
    }

    public static boolean getPersistentTileCache() {
        return getBoolean(R.string.MAP_PERSISTENT_TILECACHE, true);
    }

    public static void setPersistentTileCache(boolean persistentTileCache) {
        setBoolean(R.string.MAP_PERSISTENT_TILECACHE, persistentTileCache);
    }

    public static int getStrokeWidth() {
        return getInt(R.string.STROKE_WIDTH, 4);
    }

    public static void setStrokeWidth(int value) {
        setInt(R.string.STROKE_WIDTH, value);
    }

    public static double getOverdrawFactor() {
        return getFloat(R.string.MAP_OVERDRAW_FACTOR, 1.2f);
    }

    public static void setOverdrawFactor(double overdrawFactor) {
        setFloat(R.string.MAP_OVERDRAW_FACTOR, (float) overdrawFactor);
    }

    public static float getTileCacheCapacityFactor() {
        return getFloat(R.string.MAP_TILE_CACHE_CAPACITY_FACTOR, 2f);
    }

    public static void setTileCacheCapacityFactor(float tileCacheCapacityFactor) {
        setFloat(R.string.MAP_TILE_CACHE_CAPACITY_FACTOR, tileCacheCapacityFactor);
    }

}
