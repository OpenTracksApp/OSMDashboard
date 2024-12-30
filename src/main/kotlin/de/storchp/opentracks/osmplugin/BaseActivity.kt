package de.storchp.opentracks.osmplugin

import android.content.Intent
import android.os.Build
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuCompat
import de.storchp.opentracks.osmplugin.settings.SettingsActivity

abstract class BaseActivity : AppCompatActivity() {
    fun onCreateOptionsMenu(menu: Menu, showInfo: Boolean) {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.map, menu)

        MenuCompat.setGroupDividerEnabled(menu, true)
        menu.findItem(R.id.map_info).isVisible = showInfo
    }

    val settingsActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult<Intent, ActivityResult>(
            StartActivityForResult(),
            ActivityResultCallback { result: ActivityResult? -> recreate() })

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.action_settings) {
            settingsActivityResultLauncher.launch(Intent(this, SettingsActivity::class.java))
            return true
        }

        return super.onOptionsItemSelected(item)
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
