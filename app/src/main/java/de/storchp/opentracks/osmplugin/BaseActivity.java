package de.storchp.opentracks.osmplugin;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.lang.ref.WeakReference;

import static android.view.Menu.NONE;

abstract class BaseActivity extends AppCompatActivity implements DirectoryChooserFragment.OnFragmentInteractionListener {

    protected static final int REQUEST_MAP_DIRECTORY = 1;
    protected static final int REQUEST_THEME_DIRECTORY = 2;

    private static final String TAG = BaseActivity.class.getSimpleName();
    private static final String TAG_MAP_DIR = MapsActivity.class.getSimpleName() + ".MapDirChooser";
    private static final String TAG_THEME_DIR = MapsActivity.class.getSimpleName() + ".ThemeDirChooser";

    protected BaseApplication baseApplication;
    private DirectoryChooserFragment mDirectoryChooser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        baseApplication = (BaseApplication) getApplication();
    }

    public boolean onCreateOptionsMenu(final Menu menu, final boolean showInfo) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.maps, menu);

        final String mapFileName = baseApplication.getMapFileName();

        MenuItem osmMapnick = menu.findItem(R.id.osm_mapnik);
        osmMapnick.setChecked(mapFileName == null);
        osmMapnick.setOnMenuItemClickListener(new MapsActivity.MapMenuListener(this, baseApplication, null));

        SubMenu mapSubmenu = menu.findItem(R.id.maps_submenu).getSubMenu();

        final String mapDirectory = baseApplication.getMapDirectory();
        if (mapDirectory != null) {
            final File mapDirectoryFile = new File(mapDirectory);
            if (mapDirectoryFile.canRead() && mapDirectoryFile.isDirectory()) {
                File[] maps = mapDirectoryFile.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".map");
                    }
                });
                for (File map : maps) {
                    MenuItem mapItem = mapSubmenu.add(R.id.maps_group, NONE, NONE, map.getName());
                    mapItem.setChecked(map.getAbsolutePath().equals(mapFileName));
                    mapItem.setOnMenuItemClickListener(new MapsActivity.MapMenuListener(this, baseApplication, mapDirectoryFile));
                }
            }
        }
        mapSubmenu.setGroupCheckable(R.id.maps_group, true, true);

        MenuItem mapFolder = mapSubmenu.add(R.string.map_folder);
        mapFolder.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(BaseActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_MAP_DIRECTORY);
                } else {
                    openMapDirectoryChooser();
                }
                return false;
            }
        });

        final String mapTheme = baseApplication.getMapTheme();
        final String mapThemeDirectory = baseApplication.getMapThemeDirectory();

        MenuItem defaultTheme = menu.findItem(R.id.default_theme);
        defaultTheme.setChecked(mapTheme == null);
        defaultTheme.setOnMenuItemClickListener(new MapsActivity.MapThemeMenuListener(this, baseApplication, null));
        SubMenu themeSubmenu = menu.findItem(R.id.themes_submenu).getSubMenu();

        if (mapThemeDirectory != null) {
            final File mapThemeDirectoryFile = new File(mapThemeDirectory);
            if (mapThemeDirectoryFile.canRead() && mapThemeDirectoryFile.isDirectory()) {
                File[] themes = mapThemeDirectoryFile.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        if (file.isFile() && file.getName().endsWith(".xml")) {
                            return true;
                        }
                        if (file.isDirectory()) {
                            File theme = new File(file, file.getName() + ".xml");
                            return theme.exists() && theme.canRead();
                        }
                        return false;
                    }

                });
                for (File theme : themes) {
                    String themeName = theme.getName();
                    MenuItem themeItem = themeSubmenu.add(R.id.themes_group, NONE, NONE, themeName);
                    themeItem.setChecked(theme.getAbsolutePath().equals(mapTheme));
                    themeItem.setOnMenuItemClickListener(new MapsActivity.MapThemeMenuListener(this, baseApplication, mapThemeDirectoryFile));
                }
            }
        }
        themeSubmenu.setGroupCheckable(R.id.themes_group, true, true);

        MenuItem themeFolder = themeSubmenu.add(R.string.theme_folder);
        themeFolder.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(BaseActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_MAP_DIRECTORY);
                } else {
                    openThemeDirectoryChooser();
                }
                return false;
            }
        });

        MenuItem mapInfo = menu.findItem(R.id.map_info);
        mapInfo.setVisible(showInfo);

        return true;
    }

    private void openDirectoryChooser(String dir, String tag) {
        if (dir == null) {
            dir = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .newDirectoryName("")
                .allowNewDirectoryNameModification(true)
                .allowReadOnlyDirectory(true)
                .initialDirectory(dir)
                .build();
        mDirectoryChooser = DirectoryChooserFragment.newInstance(config);

        mDirectoryChooser.show(getFragmentManager(), tag);
    }

    protected void openMapDirectoryChooser() {
        openDirectoryChooser(baseApplication.getMapDirectory(), TAG_MAP_DIR);
    }

    protected void openThemeDirectoryChooser() {
        openDirectoryChooser(baseApplication.getMapThemeDirectory(), TAG_THEME_DIR);
    }

    public void onSelectDirectory(@NonNull String path) {
        if (mDirectoryChooser.getTag().equals(TAG_MAP_DIR)) {
            baseApplication.setMapDirectory(path);
        } else if (mDirectoryChooser.getTag().equals(TAG_THEME_DIR)) {
            baseApplication.setMapThemeDirectory(path);
        }
        mDirectoryChooser.dismiss();
        recreate();
    }

    public void onCancelChooser() {
        mDirectoryChooser.dismiss();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_MAP_DIRECTORY) {
            Log.i(TAG, "Received response for external file permission request.");

            // Check if the required permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission has been granted
                openMapDirectoryChooser();
            } else {
                //Permission not granted
                Toast.makeText(getApplicationContext(), R.string.grant_external_storage, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_THEME_DIRECTORY) {
            Log.i(TAG, "Received response for external file permission request.");

            // Check if the required permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission has been granted
                openThemeDirectoryChooser();
            } else {
                //Permission not granted
                Toast.makeText(getApplicationContext(), R.string.grant_external_storage, Toast.LENGTH_LONG).show();
            }
        }
    }

    protected static class MapMenuListener implements MenuItem.OnMenuItemClickListener {

        private WeakReference<Activity> activityRef;

        private BaseApplication baseApplication;

        private File mapDirectory;

        private MapMenuListener(final Activity activity, final BaseApplication baseApplication, final File mapDirectory) {
            this.activityRef = new WeakReference<>(activity);
            this.baseApplication = baseApplication;
            this.mapDirectory = mapDirectory;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            item.setChecked(true);
            if (item.getItemId() == R.id.osm_mapnik) { // default Mapnik online tiles
                baseApplication.setMapFileName(null);
            } else {
                baseApplication.setMapFileName(new File(mapDirectory, item.getTitle().toString()).getAbsolutePath());
            }

            final Activity mapsActivity = activityRef.get();
            if (mapsActivity != null) {
                mapsActivity.recreate();
            }
            return false;
        }
    }

    protected static class MapThemeMenuListener implements MenuItem.OnMenuItemClickListener {

        private WeakReference<Activity> activityRef;

        private BaseApplication baseApplication;

        private File mapThemeDirectory;

        private MapThemeMenuListener(final Activity activity, final BaseApplication baseApplication, final File mapThemeDirectory) {
            this.activityRef = new WeakReference<>(activity);
            this.baseApplication = baseApplication;
            this.mapThemeDirectory = mapThemeDirectory;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            item.setChecked(true);
            if (item.getItemId() == R.id.default_theme) {
                baseApplication.setMapTheme(null);
            } else {
                baseApplication.setMapTheme(new File(mapThemeDirectory, item.getTitle().toString()).getAbsolutePath());
            }

            final Activity activity = activityRef.get();
            if (activity != null) {
                activity.recreate();
            }
            return false;
        }
    }

}
