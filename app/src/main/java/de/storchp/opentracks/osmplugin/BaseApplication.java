package de.storchp.opentracks.osmplugin;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

public class BaseApplication extends Application {

    private static final String TAG = BaseApplication.class.getSimpleName();
    public static final String PREF_FILE = "APP_PREF_FILE";

    private SharedPreferences preferences;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
    }

    private void putString(int key, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(getString(key), value);
        editor.apply();
    }

    /**
     * @return the default starting zoom level if nothing is encoded in the map file.
     */
    public byte getZoomLevelDefault() {
        return (byte) 12;
    }

    public Uri getMapUri() {
        return getUri(getString(R.string.MAP_FILE));
    }

    public void setMapUri(Uri map) {
        putUri(R.string.MAP_FILE, map);
    }

    private void putUri(int key, Uri uri) {
        putString(key, uri != null ? uri.toString() : null);
    }

    public Uri getMapDirectoryUri() {
        return getUri(getString(R.string.MAP_DIRECTORY));
    }

    private Uri getUri(String key) {
        String value = preferences.getString(key, null);
        try {
            return Uri.parse(value);
        } catch (Exception ignored) {
            Log.e(TAG, "can't read Uri string " + value);
        }
        return null;
    }

    public void setMapDirectoryUri(Uri mapDirectory) {
        putUri(R.string.MAP_DIRECTORY, mapDirectory);
    }

    public Uri getMapThemeDirectoryUri() {
        return getUri(getString(R.string.MAP_THEME_DIRECTORY));
    }

    public void setMapThemeDirectoryUri(Uri mapThemeDirectory) {
        putUri(R.string.MAP_THEME_DIRECTORY, mapThemeDirectory);
    }

    public Uri getMapThemeUri() {
        return getUri(getString(R.string.MAP_THEME));
    }

    public void setMapThemeUri(Uri mapTheme) {
        putUri(R.string.MAP_THEME, mapTheme);
    }

}
