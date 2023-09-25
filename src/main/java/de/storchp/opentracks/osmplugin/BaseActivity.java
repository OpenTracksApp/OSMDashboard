package de.storchp.opentracks.osmplugin;

import android.content.Intent;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuCompat;

import de.storchp.opentracks.osmplugin.settings.SettingsActivity;

abstract class BaseActivity extends AppCompatActivity {

    public void onCreateOptionsMenu(Menu menu, boolean showInfo) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.maps, menu);

        MenuCompat.setGroupDividerEnabled(menu, true);
        menu.findItem(R.id.map_info).setVisible(showInfo);
    }

    ActivityResultLauncher<Intent> settingsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> recreate());

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            settingsActivityResultLauncher.launch(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void keepScreenOn(boolean keepScreenOn) {
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    protected void showOnLockScreen(boolean showOnLockScreen) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(showOnLockScreen);
        } else if (showOnLockScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

}
