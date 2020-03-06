package de.storchp.opentracks.osmplugin;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.lang.ref.WeakReference;

import static android.view.Menu.NONE;

abstract class BaseActivity extends AppCompatActivity {

    protected static final int REQUEST_MAP_DIRECTORY = 1;
    protected static final int REQUEST_THEME_DIRECTORY = 2;

    private static final String TAG = BaseActivity.class.getSimpleName();

    protected BaseApplication baseApplication;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        baseApplication = (BaseApplication) getApplication();
    }

    public boolean onCreateOptionsMenu(final Menu menu, final boolean showInfo) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.maps, menu);

        final Uri mapUri = baseApplication.getMapUri();

        MenuItem osmMapnick = menu.findItem(R.id.osm_mapnik);
        osmMapnick.setChecked(mapUri == null);
        osmMapnick.setOnMenuItemClickListener(new MapMenuListener(this, baseApplication, null));

        SubMenu mapSubmenu = menu.findItem(R.id.maps_submenu).getSubMenu();

        final Uri mapDirectory = baseApplication.getMapDirectoryUri();
        if (mapDirectory != null) {
            DocumentFile documentsTree = getDocumentFileFromTreeUri(mapDirectory);
            if (documentsTree != null) {
                for (DocumentFile file : documentsTree.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".map")) {
                        MenuItem mapItem = mapSubmenu.add(R.id.maps_group, NONE, NONE, file.getName());
                        mapItem.setChecked(file.getUri().equals(mapUri));
                        mapItem.setOnMenuItemClickListener(new MapMenuListener(this, baseApplication, file.getUri()));
                    }
                }
            }
        }
        mapSubmenu.setGroupCheckable(R.id.maps_group, true, true);

        MenuItem mapFolder = mapSubmenu.add(R.string.map_folder);
        mapFolder.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                openMapDirectoryChooser();
                return false;
            }
        });

        final Uri mapTheme = baseApplication.getMapThemeUri();
        final Uri mapThemeDirectory = baseApplication.getMapThemeDirectoryUri();

        MenuItem defaultTheme = menu.findItem(R.id.default_theme);
        defaultTheme.setChecked(mapTheme == null);
        defaultTheme.setOnMenuItemClickListener(new MapThemeMenuListener(this, baseApplication, null));
        SubMenu themeSubmenu = menu.findItem(R.id.themes_submenu).getSubMenu();

        if (mapThemeDirectory != null) {
            DocumentFile documentsTree = getDocumentFileFromTreeUri(mapThemeDirectory);
            if (documentsTree != null) {
                for (DocumentFile file : documentsTree.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".xml")) {
                        String themeName = file.getName();
                        MenuItem themeItem = themeSubmenu.add(R.id.themes_group, NONE, NONE, themeName);
                        themeItem.setChecked(file.getUri().equals(mapTheme));
                        themeItem.setOnMenuItemClickListener(new MapThemeMenuListener(this, baseApplication, file.getUri()));
                    } else if (file.isDirectory()) {
                        DocumentFile childFile = file.findFile(file.getName() + ".xml");
                        if (childFile != null) {
                            String themeName = file.getName();
                            MenuItem themeItem = themeSubmenu.add(R.id.themes_group, NONE, NONE, themeName);
                            themeItem.setChecked(childFile.getUri().equals(mapTheme));
                            themeItem.setOnMenuItemClickListener(new MapThemeMenuListener(this, baseApplication, childFile.getUri()));
                        }
                    }
                }
            }
        }
        themeSubmenu.setGroupCheckable(R.id.themes_group, true, true);

        MenuItem themeFolder = themeSubmenu.add(R.string.theme_folder);
        themeFolder.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                openThemeDirectoryChooser();
                return false;
            }
        });

        MenuItem mapInfo = menu.findItem(R.id.map_info);
        mapInfo.setVisible(showInfo);

        return true;
    }

    private DocumentFile getDocumentFileFromTreeUri(Uri uri) {
        try {
            return DocumentFile.fromTreeUri(getApplication(), uri);
        } catch (Exception e) {
            Log.w(TAG, "Error getting DocumentFile from Uri: " + uri);
        }
        return null;
    }

    public void openDirectory(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }


    private void openMapDirectoryChooser() {
        openDirectory(REQUEST_MAP_DIRECTORY);
    }

    private void openThemeDirectoryChooser() {
        openDirectory(REQUEST_THEME_DIRECTORY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            Uri uri = resultData.getData();
            if (uri != null) {
                getContentResolver().takePersistableUriPermission(
                        resultData.getData(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                if (requestCode == REQUEST_MAP_DIRECTORY) {
                    baseApplication.setMapDirectoryUri(resultData.getData());
                    recreate();
                } else if (requestCode == REQUEST_THEME_DIRECTORY) {
                    baseApplication.setMapThemeDirectoryUri(resultData.getData());
                    recreate();
                }
            }
        }
    }

    private static class MapMenuListener implements MenuItem.OnMenuItemClickListener {

        private WeakReference<BaseActivity> activityRef;

        private BaseApplication baseApplication;

        private Uri mapUri;

        private MapMenuListener(final BaseActivity activity, final BaseApplication baseApplication, final Uri mapUri) {
            this.activityRef = new WeakReference<>(activity);
            this.baseApplication = baseApplication;
            this.mapUri = mapUri;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            item.setChecked(true);
            if (item.getItemId() == R.id.osm_mapnik) { // default Mapnik online tiles
                baseApplication.setMapUri(null);
            } else {
                baseApplication.setMapUri(mapUri);
            }

            BaseActivity activity = activityRef.get();
            if (activity != null) {
                activity.recreate();
            }
            return false;
        }
    }

    private static class MapThemeMenuListener implements MenuItem.OnMenuItemClickListener {

        private WeakReference<BaseActivity> activityRef;

        private BaseApplication baseApplication;

        private Uri mapThemeUri;

        private MapThemeMenuListener(final BaseActivity activity, final BaseApplication baseApplication, final Uri mapThemeUri) {
            this.activityRef = new WeakReference<>(activity);
            this.baseApplication = baseApplication;
            this.mapThemeUri = mapThemeUri;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            item.setChecked(true);
            if (item.getItemId() == R.id.default_theme) { // default theme
                baseApplication.setMapThemeUri(null);
            } else {
                baseApplication.setMapThemeUri(mapThemeUri);
            }

            BaseActivity activity = activityRef.get();
            if (activity != null) {
                activity.recreate();
            }
            return false;
        }
    }

}
