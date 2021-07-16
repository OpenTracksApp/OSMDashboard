package de.storchp.opentracks.osmplugin.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import de.storchp.opentracks.osmplugin.R;

public class PreferencesUtils {

    private final static String TAG = PreferencesUtils.class.getSimpleName();
    private static SharedPreferences sharedPrefs;
    private static Resources mRes;

    public static void initPreferences(final Context context, final Resources res) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mRes = res;
    }

    private static String getKey(@StringRes final int keyId) {
        return mRes.getString(keyId);
    }

    public static Set<Uri> getMapUris() {
        return getUris(getKey(R.string.MAP_FILES));
    }

    public static void setMapUris(final Set<Uri> mapUris) {
        setUris(R.string.MAP_FILES, mapUris);
    }

    public static Uri getMapDirectoryUri() {
        return getUri(getKey(R.string.MAP_DIRECTORY));
    }

    public static void setMapDirectoryUri(final Uri mapDirectory) {
        setUri(R.string.MAP_DIRECTORY, mapDirectory);
    }

    public static Uri getMapThemeDirectoryUri() {
        return getUri(getKey(R.string.MAP_THEME_DIRECTORY));
    }

    public static void setMapThemeDirectoryUri(final Uri mapThemeDirectory) {
        setUri(R.string.MAP_THEME_DIRECTORY, mapThemeDirectory);
    }

    public static Uri getMapThemeUri() {
        return getUri(getKey(R.string.MAP_THEME));
    }

    public static void setMapThemeUri(final Uri mapTheme) {
        setUri(R.string.MAP_THEME, mapTheme);
    }

    public static boolean getOnlineMapConsent() {
        return getBoolean(R.string.ONLINE_MAP_CONSENT, false);
    }

    public static void setOnlineMapConsent(final boolean onlineMapConsent) {
        setBoolean(R.string.ONLINE_MAP_CONSENT, onlineMapConsent);
    }

    public static String getLastDownloadUrl(final String defaultDownloadUrl) {
        return getString(R.string.LAST_DOWNLOAD_URL, defaultDownloadUrl);
    }

    public static void setLastDownloadUrl(final String lastDownloadUrl) {
        setString(R.string.LAST_DOWNLOAD_URL, lastDownloadUrl);
    }

    private static Set<Uri> getUris(final String keyId) {
        final Set<String> values = sharedPrefs.getStringSet(keyId, null);
        final Set<Uri> uris = new HashSet<>();
        if (values != null) {
            for (final String value : values) {
                try {
                    uris.add(Uri.parse(value));
                } catch (final Exception ignored) {
                    Log.e(TAG, "can't read Uri string " + value);
                }
            }
        }
        return uris;
    }

    private static Uri getUri(final String keyId) {
        final String value = sharedPrefs.getString(keyId, null);
        try {
            return Uri.parse(value);
        } catch (final Exception ignored) {
            Log.e(TAG, "can't read Uri string " + value);
        }
        return null;
    }

    private static void setUri(final int keyId, final Uri uri) {
        setString(keyId, uri != null ? uri.toString() : null);
    }

    private static void setUris(final int keyId, final Set<Uri> uris) {
        final Set<String> values = new HashSet<>();
        if (uris != null) {
            for (final Uri uri : uris) {
                values.add(uri.toString());
            }
        }
        setStringSet(keyId, values);
    }

    private static String getString(final int keyId, final String defaultValue) {
        return sharedPrefs.getString(getKey(keyId), defaultValue);
    }

    private static void setString(final int keyId, final String value) {
        sharedPrefs.edit()
                .putString(getKey(keyId), value)
                .apply();
    }

    private static void setStringSet(final int keyId, final Set<String> values) {
        sharedPrefs.edit()
                .putStringSet(getKey(keyId), values)
                .apply();
    }

    private static boolean getBoolean(final int keyId, final boolean defaultValue) {
        return sharedPrefs.getBoolean(getKey(keyId), defaultValue);
    }

    private static void setBoolean(final int keyId, final boolean value) {
        sharedPrefs.edit()
                .putBoolean(getKey(keyId), value)
                .apply();
    }

    private static int getInt(final int keyId, final int defaultValue) {
        return sharedPrefs.getInt(getKey(keyId), defaultValue);
    }

    private static void setInt(final int keyId, final int value) {
        sharedPrefs.edit()
                .putInt(getKey(keyId), value)
                .apply();
    }

    public static int getTrackSmoothingTolerance() {
        return getInt(R.string.TRACK_SMOOTHING_TOLERANCE, 10);
    }

    public static void setTrackSmoothingTolerance(final int value) {
        setInt(R.string.TRACK_SMOOTHING_TOLERANCE, value);
    }

    public static boolean isPipEnabled() {
        return getBoolean(R.string.PIP_ENABLED, true);
    }

    public static void setPipEnabled(final boolean enabled) {
        setBoolean(R.string.PIP_ENABLED, enabled);
    }

    public static ArrowMode getArrowMode() {
        return ArrowMode.valueOf(getString(R.string.ARROW_MODE, ArrowMode.DIRECTION.name()), ArrowMode.DIRECTION);
    }

    public static void setArrowMode(final ArrowMode arrowMode) {
        setString(R.string.ARROW_MODE, arrowMode.name());
    }

    public static MapMode getMapMode() {
        return MapMode.valueOf(getString(R.string.MAP_MODE, MapMode.NORTH.name()), MapMode.NORTH);
    }

    public static void setMapMode(final MapMode mapMode) {
        setString(R.string.MAP_MODE, mapMode.name());
    }

    public static int getCompassSmoothing() {
        return getInt(R.string.COMPASS_SMOOTHING, 2);
    }

    public static void setCompassSmoothing(final int value) {
        setInt(R.string.COMPASS_SMOOTHING, value);
    }

    public static boolean getMultiThreadMapRendering() {
        return getBoolean(R.string.MAP_MULTI_THREAD_RENDERING, true);
    }

    public static void setMultiThreadMapRendering(final boolean multiThread) {
        setBoolean(R.string.MAP_MULTI_THREAD_RENDERING, multiThread);
    }

    public static int getStrokeWidth() {
        return getInt(R.string.STROKE_WIDTH, 4);
    }

    public static void setStrokeWidth(final int value) {
        setInt(R.string.STROKE_WIDTH, value);
    }
}
