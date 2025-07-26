package de.storchp.opentracks.osmplugin.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils

abstract class DirectoryChooserActivity : AppCompatActivity() {
    protected val directoryIntentLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                onActivityResultOk(result.data!!)
            } else {
                onActivityResultCancel()
            }
            finish()
        }

    abstract fun onActivityResultOk(resultData: Intent)

    abstract fun onActivityResultCancel()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)

        try {
            directoryIntentLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_file_manager_found, Toast.LENGTH_LONG).show()
        }
    }

    protected fun takePersistableUriPermission(uri: Uri) {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    protected fun releaseOldPermissions() {
        contentResolver.persistedUriPermissions
            .map { it.uri }
            .filter { uri -> uri != PreferencesUtils.getMapDirectoryUri() && uri != PreferencesUtils.getMapThemeDirectoryUri() }
            .forEach { uri -> contentResolver.releasePersistableUriPermission(uri, 0) }
    }

    class MapDirectoryChooserActivity : DirectoryChooserActivity() {
        override fun onActivityResultOk(resultData: Intent) {
            takePersistableUriPermission(resultData.data!!)
            PreferencesUtils.setMapDirectoryUri(resultData.data)
            releaseOldPermissions()
        }

        override fun onActivityResultCancel() {
            PreferencesUtils.setMapDirectoryUri(null)
            releaseOldPermissions()
        }
    }

    class ThemeDirectoryChooserActivity : DirectoryChooserActivity() {
        override fun onActivityResultOk(resultData: Intent) {
            takePersistableUriPermission(resultData.data!!)
            PreferencesUtils.setMapThemeDirectoryUri(resultData.data)
            releaseOldPermissions()
        }

        override fun onActivityResultCancel() {
            PreferencesUtils.setMapThemeDirectoryUri(null)
            releaseOldPermissions()
        }
    }
}
