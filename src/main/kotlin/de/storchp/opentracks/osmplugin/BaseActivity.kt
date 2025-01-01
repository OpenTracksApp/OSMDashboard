package de.storchp.opentracks.osmplugin

import android.content.Intent
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import de.storchp.opentracks.osmplugin.settings.SettingsActivity

abstract class BaseActivity : AppCompatActivity() {

    val settingsActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult<Intent, ActivityResult>(
            StartActivityForResult(),
            ActivityResultCallback { result: ActivityResult? -> recreate() })

    @Suppress("unused")
    fun openSettings(view: View?) {
        settingsActivityResultLauncher.launch(Intent(this, SettingsActivity::class.java))
    }

    protected fun keepScreenOn(keepScreenOn: Boolean) {
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @Suppress("DEPRECATION")
    protected fun showOnLockScreen(showOnLockScreen: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(showOnLockScreen)
        } else if (showOnLockScreen) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
    }
}
