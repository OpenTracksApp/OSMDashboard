package de.storchp.opentracks.osmplugin;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

public abstract class DirectoryChooserActivity extends AppCompatActivity {

    protected final ActivityResultLauncher<Intent> directoryIntentLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    onActivityResultOk(result.getData());
                } else {
                    onActivityResultCancel();
                }
                finish();
            });

    abstract void onActivityResultOk(@NonNull Intent resultData);

    abstract void onActivityResultCancel();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        try {
            directoryIntentLauncher.launch(intent);
        } catch (final ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.no_file_manager_found, Toast.LENGTH_LONG).show();
        }
    }

    protected void takePersistableUriPermission(Uri uri, Intent intent) {
        int takeFlags = intent.getFlags();
        takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getContentResolver().takePersistableUriPermission(uri, takeFlags);
    }

    protected void releaseOldPermissions() {
        getContentResolver().getPersistedUriPermissions().stream()
                .map(UriPermission::getUri)
                .filter(uri -> !uri.equals(PreferencesUtils.getMapDirectoryUri())
                        && !uri.equals(PreferencesUtils.getMapThemeDirectoryUri()))
                .forEach(uri -> getContentResolver().releasePersistableUriPermission(uri, 0));
    }

    public static class MapDirectoryChooserActivity extends DirectoryChooserActivity {

        @Override
        void onActivityResultOk(@NonNull final Intent resultData) {
            takePersistableUriPermission(resultData.getData(), resultData);
            PreferencesUtils.setMapDirectoryUri(resultData.getData());
            releaseOldPermissions();
        }

        @Override
        void onActivityResultCancel() {
            PreferencesUtils.setMapDirectoryUri(null);
            releaseOldPermissions();
        }
    }

    public static class ThemeDirectoryChooserActivity extends DirectoryChooserActivity {

        @Override
        void onActivityResultOk(@NonNull final Intent resultData) {
            takePersistableUriPermission(resultData.getData(), resultData);
            PreferencesUtils.setMapThemeDirectoryUri(resultData.getData());
            releaseOldPermissions();
        }

        @Override
        void onActivityResultCancel() {
            PreferencesUtils.setMapThemeDirectoryUri(null);
            releaseOldPermissions();
        }
    }

}
