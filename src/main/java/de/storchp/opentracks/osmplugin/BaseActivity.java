package de.storchp.opentracks.osmplugin;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuCompat;

import de.storchp.opentracks.osmplugin.databinding.CompassSmoothingDialogBinding;
import de.storchp.opentracks.osmplugin.databinding.StrokeWidthDialogBinding;
import de.storchp.opentracks.osmplugin.databinding.TrackSmoothingDialogBinding;
import de.storchp.opentracks.osmplugin.utils.ArrowMode;
import de.storchp.opentracks.osmplugin.utils.MapMode;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

abstract class BaseActivity extends AppCompatActivity {

    protected MenuItem mapConsent;
    protected MenuItem pipMode;
    protected MenuItem multiThreadMapRendering;
    protected MenuItem persistentTileCache;

    public boolean onCreateOptionsMenu(final Menu menu, final boolean showInfo) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.maps, menu);

        MenuCompat.setGroupDividerEnabled(menu, true);

        menu.findItem(R.id.map_info).setVisible(showInfo);

        mapConsent = menu.findItem(R.id.map_online_consent);
        mapConsent.setChecked(PreferencesUtils.getOnlineMapConsent());

        if (BuildConfig.offline) {
            mapConsent.setVisible(false);
            menu.findItem(R.id.download_map).setVisible(false);
        }

        multiThreadMapRendering = menu.findItem(R.id.multi_thread_map_rendering);
        multiThreadMapRendering.setChecked(PreferencesUtils.getMultiThreadMapRendering());

        persistentTileCache = menu.findItem(R.id.persistent_tilecache);
        persistentTileCache.setChecked(PreferencesUtils.getPersistentTileCache());

        pipMode = menu.findItem(R.id.pip_mode);
        pipMode.setChecked(PreferencesUtils.isPipEnabled());

        menu.findItem(R.id.arrow_mode).setTitle(PreferencesUtils.getArrowMode().getMessageId());
        menu.findItem(R.id.map_mode).setTitle(PreferencesUtils.getMapMode().getMessageId());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.map_online_consent) {
            item.setChecked(!item.isChecked());
            PreferencesUtils.setOnlineMapConsent(item.isChecked());
            onOnlineMapConsentChanged(item.isChecked());
        } else if (itemId == R.id.track_smoothing) {
            showTrackSmoothingDialog();
        } else if (itemId == R.id.compass_smoothing) {
            showCompassSmoothingDialog();
        } else if (itemId == R.id.stroke_width) {
            showStrokeWidthDialog();
        } else if (itemId == R.id.multi_thread_map_rendering) {
            item.setChecked(!item.isChecked());
            PreferencesUtils.setMultiThreadMapRendering(item.isChecked());
        } else if (itemId == R.id.persistent_tilecache) {
            item.setChecked(!item.isChecked());
            PreferencesUtils.setPersistentTileCache(item.isChecked());
        } else if (itemId == R.id.pip_mode) {
            item.setChecked(!item.isChecked());
            PreferencesUtils.setPipEnabled(item.isChecked());
        } else if (itemId == R.id.map_selection) {
            startActivity(new Intent(this, MapSelectionActivity.class));
        } else if (itemId == R.id.theme_selection) {
            startActivity(new Intent(this, ThemeSelectionActivity.class));
        } else if (itemId == R.id.map_folder) {
            openDirectory(mapDirectoryLauncher);
        } else if (itemId == R.id.theme_folder) {
            openDirectory(themeDirectoryLauncher);
        } else if (itemId == R.id.download_map) {
            startActivity(new Intent(this, DownloadMapSelectionActivity.class));
        } else if (itemId == R.id.arrow_mode) {
            var arrowMode = PreferencesUtils.getArrowMode();
            arrowMode = arrowMode.next();
            item.setTitle(arrowMode.getMessageId());
            PreferencesUtils.setArrowMode(arrowMode);
            changeArrowMode(arrowMode);
        } else if (itemId == R.id.map_mode) {
            var mapMode = PreferencesUtils.getMapMode();
            mapMode = mapMode.next();
            item.setTitle(mapMode.getMessageId());
            PreferencesUtils.setMapMode(mapMode);
            changeMapMode(mapMode);
        }

        return super.onOptionsItemSelected(item);
    }

    protected abstract void changeMapMode(final MapMode mapMode);

    protected abstract void changeArrowMode(final ArrowMode arrowMode);

    private void showTrackSmoothingDialog() {
        final var binding = TrackSmoothingDialogBinding.inflate(LayoutInflater.from(this));
        binding.etTolerance.setText(String.valueOf(PreferencesUtils.getTrackSmoothingTolerance()));

        final var alertDialog = new AlertDialog.Builder(this)
                .setView(binding.getRoot())
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            final var newTolerance = binding.etTolerance.getText().toString().trim();
            if (newTolerance.length() > 0 && TextUtils.isDigitsOnly(newTolerance)) {
                PreferencesUtils.setTrackSmoothingTolerance(Integer.parseInt(newTolerance));
                alertDialog.dismiss();
            } else {
                Toast.makeText(BaseActivity.this, R.string.only_digits, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showCompassSmoothingDialog() {
        final var binding = CompassSmoothingDialogBinding.inflate(LayoutInflater.from(this));
        final int smoothing = PreferencesUtils.getCompassSmoothing();
        binding.tvCompassSmoothing.setText(String.valueOf(smoothing));
        binding.sbCompassSmoothing.setProgress(smoothing);
        binding.sbCompassSmoothing.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int i, final boolean b) {
                binding.tvCompassSmoothing.setText(String.valueOf(Math.max(binding.sbCompassSmoothing.getProgress(), 1)));
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {

            }
        });

        final var alertDialog = new AlertDialog.Builder(this)
                .setView(binding.getRoot())
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            PreferencesUtils.setCompassSmoothing(binding.sbCompassSmoothing.getProgress());
            alertDialog.dismiss();
        });
    }

    private void showStrokeWidthDialog() {
        final StrokeWidthDialogBinding binding = StrokeWidthDialogBinding.inflate(LayoutInflater.from(this));
        final int currentStrokeWidth = PreferencesUtils.getStrokeWidth();
        binding.tvStrokeWidth.setText(String.valueOf(currentStrokeWidth));
        binding.sbStrokeWidth.setProgress(currentStrokeWidth);
        binding.sbStrokeWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int i, final boolean b) {
                binding.tvStrokeWidth.setText(String.valueOf(Math.max(binding.sbStrokeWidth.getProgress(), 1)));
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {

            }
        });

        final var alertDialog = new AlertDialog.Builder(this)
                .setView(binding.getRoot())
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            PreferencesUtils.setStrokeWidth(binding.sbStrokeWidth.getProgress());
            alertDialog.dismiss();
        });
    }

    protected abstract void onOnlineMapConsentChanged(boolean consent);

    protected final ActivityResultLauncher<Intent> mapDirectoryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    changeMapDirectory(result.getData().getData(), result.getData());
                }
            });

    protected final ActivityResultLauncher<Intent> themeDirectoryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    changeThemeDirectory(result.getData().getData(), result.getData());
                }
            });

    protected void openDirectory(final ActivityResultLauncher<Intent> launcher) {
        try {
            launcher.launch(createOpenDocumentIntent());
        } catch (final ActivityNotFoundException exception) {
            Toast.makeText(BaseActivity.this, R.string.no_file_manager_found, Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    private Intent createOpenDocumentIntent() {
        final var intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        return intent;
    }

    private void releaseOldPermissions() {
        getContentResolver().getPersistedUriPermissions().stream()
                .map(UriPermission::getUri)
                .filter(uri -> !uri.equals(PreferencesUtils.getMapDirectoryUri())
                        && !uri.equals(PreferencesUtils.getMapThemeDirectoryUri()))
                .forEach(uri -> getContentResolver().releasePersistableUriPermission(uri, 0));
    }

    protected void changeThemeDirectory(final Uri uri, final Intent resultData) {
        takePersistableUriPermission(uri, resultData);
        PreferencesUtils.setMapThemeDirectoryUri(uri);
        releaseOldPermissions();
    }

    protected void changeMapDirectory(final Uri uri, final Intent resultData) {
        takePersistableUriPermission(uri, resultData);
        PreferencesUtils.setMapDirectoryUri(uri);
        releaseOldPermissions();
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
