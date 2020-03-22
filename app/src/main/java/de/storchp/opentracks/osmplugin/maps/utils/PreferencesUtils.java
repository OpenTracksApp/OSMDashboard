package de.storchp.opentracks.osmplugin.maps.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import de.storchp.opentracks.osmplugin.R;

public class PreferencesUtils {

    private final static String TAG = PreferencesUtils.class.getSimpleName();

    private PreferencesUtils() {
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static Uri getMapUri(Context context) {
        return getUri(context, context.getString(R.string.MAP_FILE));
    }

    public static void setMapUri(Context context, Uri map) {
        setUri(context, R.string.MAP_FILE, map);
    }

    public static Uri getMapDirectoryUri(Context context) {
        return getUri(context, context.getString(R.string.MAP_DIRECTORY));
    }

    public static void setMapDirectoryUri(Context context, Uri mapDirectory) {
        setUri(context, R.string.MAP_DIRECTORY, mapDirectory);
    }

    public static Uri getMapThemeDirectoryUri(Context context) {
        return getUri(context, context.getString(R.string.MAP_THEME_DIRECTORY));
    }

    public static void setMapThemeDirectoryUri(Context context, Uri mapThemeDirectory) {
        setUri(context, R.string.MAP_THEME_DIRECTORY, mapThemeDirectory);
    }

    public static Uri getMapThemeUri(Context context) {
        return getUri(context, context.getString(R.string.MAP_THEME));
    }

    public static void setMapThemeUri(Context context, Uri mapTheme) {
        setUri(context, R.string.MAP_THEME, mapTheme);
    }


    private static Uri getUri(Context context, String keyId) {
        String value = getSharedPreferences(context).getString(keyId, null);
        try {
            return Uri.parse(value);
        } catch (Exception ignored) {
            Log.e(TAG, "can't read Uri string " + value);
        }
        return null;
    }

    private static void setUri(Context context, int keyId, Uri uri) {
        setString(context, keyId, uri != null ? uri.toString() : null);
    }

    /**
     * Gets a string preference value.
     *
     * @param context      the context
     * @param keyId        the key id
     * @param defaultValue default value
     */
    private static String getString(Context context, int keyId, String defaultValue) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getString(getKey(context, keyId), defaultValue);
    }

    public static void setString(Context context, int keyId, String value) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getKey(context, keyId), value);
        editor.apply();
    }

    /**
     * Gets a preference key
     *
     * @param context the context
     * @param keyId   the key id
     */
    private static String getKey(Context context, int keyId) {
        return context.getString(keyId);
    }
}
