package de.storchp.opentracks.osmplugin;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.util.Set;

import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

import static android.view.Menu.NONE;

abstract class BaseActivity extends AppCompatActivity {

    protected static final int REQUEST_MAP_DIRECTORY = 1;
    protected static final int REQUEST_THEME_DIRECTORY = 2;
    protected static final int REQUEST_DOWNLOAD_MAP = 3;
    protected static final int REQUEST_MAP_DIRECTORY_FOR_DOWNLOAD = 4;
    protected static final int REQUEST_MAP_SELECTION = 5;

    private static final String TAG = BaseActivity.class.getSimpleName();
    private SubMenu mapSubmenu;
    protected MenuItem mapConsent;

    public boolean onCreateOptionsMenu(final Menu menu, final boolean showInfo) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.maps, menu);

        final Set<Uri> mapUris = PreferencesUtils.getMapUris(this);

        final MenuItem osmMapnick = menu.findItem(R.id.osm_mapnik);
        osmMapnick.setChecked(mapUris.isEmpty());
        osmMapnick.setOnMenuItemClickListener(new MapMenuListener(null));

        mapSubmenu = menu.findItem(R.id.maps_submenu).getSubMenu();

/*        final Uri mapDirectory = PreferencesUtils.getMapDirectoryUri(this);
        if (mapDirectory != null) {
            final DocumentFile documentsTree = getDocumentFileFromTreeUri(mapDirectory);
            if (documentsTree != null) {
                for (final DocumentFile file : documentsTree.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".map")) {
                        final MenuItem mapItem = mapSubmenu.add(R.id.maps_group, NONE, NONE, file.getName());
                        mapItem.setChecked(mapUris.contains(file.getUri()));
                        mapItem.setOnMenuItemClickListener(new MapMenuListener(file.getUri()));
                    }
                }
            }
        }
        mapSubmenu.setGroupCheckable(R.id.maps_group, true, false);
*/

        final MenuItem mapSelection = mapSubmenu.add(R.string.map_selection);
        mapSelection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                final Intent intent = new Intent(BaseActivity.this, MapSelectionActivity.class);
                startActivityForResult(intent, REQUEST_MAP_SELECTION);
                return false;
            }
        });

        final MenuItem mapFolder = mapSubmenu.add(R.string.map_folder);
        mapFolder.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                openMapDirectoryChooser();
                return false;
            }
        });

        final MenuItem downloadMap = mapSubmenu.add(R.string.download_map);
        downloadMap.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                final Intent intent = new Intent(BaseActivity.this, DownloadMapsActivity.class);
                startActivityForResult(intent, REQUEST_DOWNLOAD_MAP);
                return false;
            }
        });

        final Uri mapTheme = PreferencesUtils.getMapThemeUri(this);
        final Uri mapThemeDirectory = PreferencesUtils.getMapThemeDirectoryUri(this);

        final MenuItem defaultTheme = menu.findItem(R.id.default_theme);
        defaultTheme.setChecked(mapTheme == null);
        defaultTheme.setOnMenuItemClickListener(new MapThemeMenuListener(null));
        final SubMenu themeSubmenu = menu.findItem(R.id.themes_submenu).getSubMenu();

        if (mapThemeDirectory != null) {
            final DocumentFile documentsTree = getDocumentFileFromTreeUri(mapThemeDirectory);
            if (documentsTree != null) {
                for (final DocumentFile file : documentsTree.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".xml")) {
                        final String themeName = file.getName();
                        final MenuItem themeItem = themeSubmenu.add(R.id.themes_group, NONE, NONE, themeName);
                        themeItem.setChecked(file.getUri().equals(mapTheme));
                        themeItem.setOnMenuItemClickListener(new MapThemeMenuListener(file.getUri()));
                    } else if (file.isDirectory()) {
                        final DocumentFile childFile = file.findFile(file.getName() + ".xml");
                        if (childFile != null) {
                            final String themeName = file.getName();
                            final MenuItem themeItem = themeSubmenu.add(R.id.themes_group, NONE, NONE, themeName);
                            themeItem.setChecked(childFile.getUri().equals(mapTheme));
                            themeItem.setOnMenuItemClickListener(new MapThemeMenuListener(childFile.getUri()));
                        }
                    }
                }
            }
        }
        themeSubmenu.setGroupCheckable(R.id.themes_group, true, true);

        final MenuItem themeFolder = themeSubmenu.add(R.string.theme_folder);
        themeFolder.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                openThemeDirectoryChooser();
                return false;
            }
        });

        final MenuItem mapInfo = menu.findItem(R.id.map_info);
        mapInfo.setVisible(showInfo);

        mapConsent = menu.findItem(R.id.map_online_consent);
        mapConsent.setChecked(PreferencesUtils.getOnlineMapConsent(this));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == R.id.map_online_consent) {
            item.setChecked(!item.isChecked());
            PreferencesUtils.setOnlineMapConsent(this, item.isChecked());
            onOnlineMapConsentChanged(item.isChecked());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected abstract void onOnlineMapConsentChanged(boolean consent);

    protected DocumentFile getDocumentFileFromTreeUri(final Uri uri) {
        try {
            return DocumentFile.fromTreeUri(getApplication(), uri);
        } catch (final Exception e) {
            Log.w(TAG, "Error getting DocumentFile from Uri: " + uri);
        }
        return null;
    }

    protected void openDirectory(final int requestCode) {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }

    protected void openMapDirectoryChooser() {
        openDirectory(REQUEST_MAP_DIRECTORY);
    }

    protected void openThemeDirectoryChooser() {
        openDirectory(REQUEST_THEME_DIRECTORY);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            final Uri uri = resultData.getData();
            if (uri != null) {
                takePersistableUriPermission(uri);
                if (requestCode == REQUEST_MAP_DIRECTORY || requestCode == REQUEST_MAP_DIRECTORY_FOR_DOWNLOAD) {
                    changeMapDirectory(uri, requestCode);
                } else if (requestCode == REQUEST_THEME_DIRECTORY) {
                    changeThemeDirectory(uri);
                }
            }
        }
    }

    private void changeThemeDirectory(final Uri uri) {
        takePersistableUriPermission(uri);
        PreferencesUtils.setMapThemeDirectoryUri(this, uri);
        recreateMap(true);
    }

    protected void changeMapDirectory(final Uri uri, final int requestCode) {
        takePersistableUriPermission(uri);
        PreferencesUtils.setMapDirectoryUri(this, uri);
        recreateMap(true);
    }

    private void takePersistableUriPermission(final Uri uri) {
        getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    abstract void recreateMap(boolean menuNeedsUpdate);

    private class MapMenuListener implements MenuItem.OnMenuItemClickListener {

        private final Uri mapUri;

        private MapMenuListener(final Uri mapUri) {
            this.mapUri = mapUri;
        }

        @Override
        public boolean onMenuItemClick(final MenuItem item) {
            item.setChecked(!item.isChecked());
            final Set<Uri> mapUris = PreferencesUtils.getMapUris(BaseActivity.this);
            if (item.isChecked()) {
                if (item.getItemId() == R.id.osm_mapnik) { // default Mapnik online tiles
                    mapUris.clear();
                    for (int i = 0; i < mapSubmenu.size(); i++) {
                        final MenuItem submenuItem = mapSubmenu.getItem(i);
                        if (submenuItem != item) {
                            submenuItem.setChecked(false);
                        }
                    }
                } else {
                    mapUris.add(mapUri);
                    for (int i = 0; i < mapSubmenu.size(); i++) {
                        final MenuItem submenuItem = mapSubmenu.getItem(i);
                        if (submenuItem.getItemId() == R.id.osm_mapnik) {
                            submenuItem.setChecked(false);
                        }
                    }
                }
            } else {
                if (mapUri != null) {
                    mapUris.remove(mapUri);
                }
            }
            PreferencesUtils.setMapUris(BaseActivity.this, mapUris);
            recreateMap(false);

            return false;
        }
    }

    private class MapThemeMenuListener implements MenuItem.OnMenuItemClickListener {

        private final Uri mapThemeUri;

        private MapThemeMenuListener(final Uri mapThemeUri) {
            this.mapThemeUri = mapThemeUri;
        }

        @Override
        public boolean onMenuItemClick(final MenuItem item) {
            item.setChecked(true);
            if (item.getItemId() == R.id.default_theme) { // default theme
                PreferencesUtils.setMapThemeUri(BaseActivity.this, null);
            } else {
                PreferencesUtils.setMapThemeUri(BaseActivity.this, mapThemeUri);
            }

            recreateMap(false);

            return false;
        }
    }

}
