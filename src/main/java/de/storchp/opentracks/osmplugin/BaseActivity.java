package de.storchp.opentracks.osmplugin;

import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.SeekBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuCompat;

import java.util.Locale;

import de.storchp.opentracks.osmplugin.databinding.TilecacheCapacityFactorDialogBinding;
import de.storchp.opentracks.osmplugin.settings.SettingsActivity;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = BaseActivity.class.getSimpleName();

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
        } else if (itemId == R.id.tilecache_capacity_factor) {
            showTileCacheCapacityFactorDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showTileCacheCapacityFactorDialog() {
        TilecacheCapacityFactorDialogBinding binding = TilecacheCapacityFactorDialogBinding.inflate(LayoutInflater.from(this));
        float currentTileCacheCapacityFactor = PreferencesUtils.getTileCacheCapacityFactor();

        binding.tvTilecacheCapacityFactor.setText(String.format(Locale.getDefault(), "%.2f", currentTileCacheCapacityFactor));
        binding.sbTilecacheCapacityFactor.setProgress((int) ((currentTileCacheCapacityFactor * 100) - 100) / 3);
        binding.sbTilecacheCapacityFactor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                binding.tvTilecacheCapacityFactor.setText(String.format(Locale.getDefault(), "%.2f", getTileCacheCapacityFactorFromProgress(binding)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        var alertDialog = new AlertDialog.Builder(this)
                .setView(binding.getRoot())
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            float tileCacheCapacityFactor = getTileCacheCapacityFactorFromProgress(binding);
            PreferencesUtils.setTileCacheCapacityFactor(tileCacheCapacityFactor);
            Log.i(TAG, "New tileCacheCapacityFactor: " + tileCacheCapacityFactor);
            alertDialog.dismiss();
        });
    }

    private float getTileCacheCapacityFactorFromProgress(final TilecacheCapacityFactorDialogBinding binding) {
        int progress = binding.sbTilecacheCapacityFactor.getProgress();
        if (progress == 0) {
            return 1;
        }
        return (progress * 3 / (float) 100) + 1;
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
