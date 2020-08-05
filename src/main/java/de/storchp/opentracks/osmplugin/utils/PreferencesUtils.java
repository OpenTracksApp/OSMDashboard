package de.storchp.opentracks.osmplugin.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import de.storchp.opentracks.osmplugin.R;

public class PreferencesUtils {

    private final static String TAG = PreferencesUtils.class.getSimpleName();

    private PreferencesUtils() {
    }

    private static SharedPreferences getSharedPreferences(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static Set<Uri> getMapUris(final Context context) {
        return getUris(context, context.getString(R.string.MAP_FILES));
    }

    public static void setMapUris(final Context context, final Set<Uri> mapUris) {
        setUris(context, R.string.MAP_FILES, mapUris);
    }

    public static Uri getMapDirectoryUri(final Context context) {
        return getUri(context, context.getString(R.string.MAP_DIRECTORY));
    }

    public static void setMapDirectoryUri(final Context context, final Uri mapDirectory) {
        setUri(context, R.string.MAP_DIRECTORY, mapDirectory);
    }

    public static Uri getMapThemeDirectoryUri(final Context context) {
        return getUri(context, context.getString(R.string.MAP_THEME_DIRECTORY));
    }

    public static void setMapThemeDirectoryUri(final Context context, final Uri mapThemeDirectory) {
        setUri(context, R.string.MAP_THEME_DIRECTORY, mapThemeDirectory);
    }

    public static Uri getMapThemeUri(final Context context) {
        return getUri(context, getKey(context, R.string.MAP_THEME));
    }

    public static void setMapThemeUri(final Context context, final Uri mapTheme) {
        setUri(context, R.string.MAP_THEME, mapTheme);
    }

    public static boolean getOnlineMapConsent(final Context context) {
        return getBoolean(context, R.string.ONLINE_MAP_CONSENT, false);
    }

    public static void setOnlineMapConsent(final Context context, final boolean onlineMapConsent) {
        setBoolean(context, R.string.ONLINE_MAP_CONSENT, onlineMapConsent);
    }

    public static String getLastDownloadUrl(final Context context, final String defaultDownloadUrl) {
        return getString(context, R.string.LAST_DOWNLOAD_URL, defaultDownloadUrl);
    }

    public static void setLastDownloadUrl(final Context context, final String lastDownloadUrl) {
        setString(context, R.string.LAST_DOWNLOAD_URL, lastDownloadUrl);
    }

    private static Set<Uri> getUris(final Context context, final String keyId) {
        final Set<String> values = getSharedPreferences(context).getStringSet(keyId, null);
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

    private static Uri getUri(final Context context, final String keyId) {
        final String value = getSharedPreferences(context).getString(keyId, null);
        try {
            return Uri.parse(value);
        } catch (final Exception ignored) {
            Log.e(TAG, "can't read Uri string " + value);
        }
        return null;
    }

    private static void setUri(final Context context, final int keyId, final Uri uri) {
        setString(context, keyId, uri != null ? uri.toString() : null);
    }

    private static void setUris(final Context context, final int keyId, final Set<Uri> uris) {
        final Set<String> values = new HashSet<>();
        if (uris != null) {
            for (final Uri uri : uris) {
                values.add(uri.toString());
            }
        }
        setStringSet(context, keyId, values);
    }

    private static String getString(final Context context, final int keyId, final String defaultValue) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getString(getKey(context, keyId), defaultValue);
    }

    private static void setString(final Context context, final int keyId, final String value) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getKey(context, keyId), value);
        editor.apply();
    }

    private static void setStringSet(final Context context, final int keyId, final Set<String> values) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(getKey(context, keyId), values);
        editor.apply();
    }

    private static boolean getBoolean(final Context context, final int keyId, final boolean defaultValue) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(getKey(context, keyId), defaultValue);
    }

    private static void setBoolean(final Context context, final int keyId, final boolean value) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getKey(context, keyId), value);
        editor.apply();
    }

    private static int getInt(final Context context, final int keyId, final int defaultValue) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getInt(getKey(context, keyId), defaultValue);
    }

    private static void setInt(final Context context, final int keyId, final int value) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getKey(context, keyId), value);
        editor.apply();
    }

    private static String getKey(final Context context, final int keyId) {
        return context.getString(keyId);
    }

    public static int getTrackSmoothingTolerance(final Context context) {
        return getInt(context, R.string.TRACK_SMOOTHING_TOLERANCE, 10);
    }

    public static void setTrackSmoothingTolerance(final Context context, final int value) {
        setInt(context, R.string.TRACK_SMOOTHING_TOLERANCE, value);
    }

    public static boolean isPipEnabled(final Context context) {
        return getBoolean(context, R.string.PIP_ENABLED, true);
    }

    public static void setPipEnabled(final Context context, final boolean enabled) {
        setBoolean(context, R.string.PIP_ENABLED, enabled);
    }

}
