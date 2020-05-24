package de.storchp.opentracks.osmplugin.maps.utils;

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

    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static Set<Uri> getMapUris(Context context) {
        return getUris(context, context.getString(R.string.MAP_FILES));
    }

    public static void setMapUris(Context context, Set<Uri> mapUris) {
        setUris(context, R.string.MAP_FILES, mapUris);
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
        return getUri(context, getKey(context, R.string.MAP_THEME));
    }

    public static void setMapThemeUri(Context context, Uri mapTheme) {
        setUri(context, R.string.MAP_THEME, mapTheme);
    }

    public static boolean getOnlineMapConsent(Context context) {
        return getBoolean(context, R.string.ONLINE_MAP_CONSENT, false);
    }

    public static void setOnlineMapConsent(Context context, boolean onlineMapConsent) {
        setBoolean(context, R.string.ONLINE_MAP_CONSENT, onlineMapConsent);
    }

    private static Set<Uri> getUris(Context context, String keyId) {
        Set<String> values = getSharedPreferences(context).getStringSet(keyId, null);
        Set<Uri> uris = new HashSet<>();
        if (values != null) {
            for (String value : values) {
                try {
                    uris.add(Uri.parse(value));
                } catch (Exception ignored) {
                    Log.e(TAG, "can't read Uri string " + value);
                }
            }
        }
        return uris;
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

    private static void setUris(Context context, int keyId, Set<Uri> uris) {
        Set<String> values = new HashSet<>();
        if (uris != null) {
            for (Uri uri : uris) {
                values.add(uri.toString());
            }
        }
        setStringSet(context, keyId, values);
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

    private static void setString(Context context, int keyId, String value) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getKey(context, keyId), value);
        editor.apply();
    }

    private static void setStringSet(Context context, int keyId, Set<String> values) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(getKey(context, keyId), values);
        editor.apply();
    }

    private static boolean getBoolean(Context context, int keyId, boolean defaultValue) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(getKey(context, keyId), defaultValue);
    }

    private static void setBoolean(Context context, int keyId, boolean value) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getKey(context, keyId), value);
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
