package de.storchp.opentracks.osmplugin;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Set;

public class BaseApplication extends Application {

    private static BaseApplication instance;

    public static final String PREF_FILE = "APP_PREF_FILE";

    private SharedPreferences preferences;

    public BaseApplication() {
        setInstance(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    private static void setInstance(@NonNull final BaseApplication application) {
        instance = application;
    }

    public static BaseApplication getInstance() {
        return instance;
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

    public String getMapFileName() {
        return preferences.getString(getString(R.string.MAP_FILE), null);
    }

    public void setMapFileName(String mapFileName) {
        putString(R.string.MAP_FILE, mapFileName);
    }

    public String getMapDirectory() {
        return preferences.getString(getString(R.string.MAP_DIRECTORY), null);
    }

    public void setMapDirectory(String mapDirectory) {
        putString(R.string.MAP_DIRECTORY, mapDirectory);
    }

    public String getMapThemeDirectory() {
        return preferences.getString(getString(R.string.MAP_THEME_DIRECTORY), null);
    }

    public void setMapThemeDirectory(String mapThemeDirectory) {
        putString(R.string.MAP_THEME_DIRECTORY, mapThemeDirectory);
    }

    public String getMapTheme() {
        return preferences.getString(getString(R.string.MAP_THEME), null);
    }

    public void setMapTheme(String mapTheme) {
        putString(R.string.MAP_THEME, mapTheme);
    }

}
