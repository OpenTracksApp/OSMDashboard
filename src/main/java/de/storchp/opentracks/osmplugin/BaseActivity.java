package de.storchp.opentracks.osmplugin;

import android.app.Activity;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuCompat;

import java.util.List;

import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

abstract class BaseActivity extends AppCompatActivity {

    protected static final int REQUEST_MAP_DIRECTORY = 1;
    protected static final int REQUEST_THEME_DIRECTORY = 2;
    protected static final int REQUEST_DOWNLOAD_MAP = 3;
    protected static final int REQUEST_MAP_DIRECTORY_FOR_DOWNLOAD = 4;
    protected static final int REQUEST_MAP_SELECTION = 5;
    protected static final int REQUEST_THEME_SELECTION = 6;

    protected MenuItem mapConsent;

    public boolean onCreateOptionsMenu(final Menu menu, final boolean showInfo) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.maps, menu);

        MenuCompat.setGroupDividerEnabled(menu, true);

        final MenuItem mapInfo = menu.findItem(R.id.map_info);
        mapInfo.setVisible(showInfo);

        mapConsent = menu.findItem(R.id.map_online_consent);
        mapConsent.setChecked(PreferencesUtils.getOnlineMapConsent(this));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.map_online_consent :
                item.setChecked(!item.isChecked());
                PreferencesUtils.setOnlineMapConsent(this, item.isChecked());
                onOnlineMapConsentChanged(item.isChecked());
                break;
            case R.id.map_selection :
                startActivityForResult(new Intent(this, MapSelectionActivity.class), REQUEST_MAP_SELECTION);
                break;
            case R.id.theme_selection :
                startActivityForResult(new Intent(this, ThemeSelectionActivity.class), REQUEST_THEME_SELECTION);
                break;
            case R.id.map_folder :
                openMapDirectoryChooser();
                break;
            case R.id.theme_folder :
                openThemeDirectoryChooser();
                break;
            case R.id.download_map :
                startActivityForResult(new Intent(this, DownloadMapsActivity.class), REQUEST_DOWNLOAD_MAP);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    protected abstract void onOnlineMapConsentChanged(boolean consent);

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
                takePersistableUriPermission(uri, resultData);
                if (requestCode == REQUEST_MAP_DIRECTORY || requestCode == REQUEST_MAP_DIRECTORY_FOR_DOWNLOAD) {
                    changeMapDirectory(uri, requestCode, resultData);
                } else if (requestCode == REQUEST_THEME_DIRECTORY) {
                    changeThemeDirectory(uri, resultData);
                }
            }
        }

        // release old permissions
        final Uri mapDirectoryUri = PreferencesUtils.getMapDirectoryUri(this);
        final Uri themeDirectoryUri = PreferencesUtils.getMapThemeDirectoryUri(this);
        final List<UriPermission> persistedUriPermissions = getContentResolver().getPersistedUriPermissions();
        for (final UriPermission permission : persistedUriPermissions) {
            final Uri uri = permission.getUri();
            if (!uri.equals(mapDirectoryUri) && !uri.equals(themeDirectoryUri)) {
                getContentResolver().releasePersistableUriPermission(uri, 0);
            }
        }
    }

    private void changeThemeDirectory(final Uri uri, final Intent resultData) {
        takePersistableUriPermission(uri, resultData);
        PreferencesUtils.setMapThemeDirectoryUri(this, uri);
    }

    protected void changeMapDirectory(final Uri uri, final int requestCode, final Intent resultData) {
        takePersistableUriPermission(uri, resultData);
        PreferencesUtils.setMapDirectoryUri(this, uri);
    }

    private void takePersistableUriPermission(final Uri uri, final Intent intent) {
        int takeFlags = intent.getFlags();
        takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getContentResolver().takePersistableUriPermission(uri, takeFlags);
    }

    protected void keepScreenOn(final boolean keepScreenOn) {
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    protected void showOnLockScreen(final boolean showOnLockScreen) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(showOnLockScreen);
        } else if (showOnLockScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

}
