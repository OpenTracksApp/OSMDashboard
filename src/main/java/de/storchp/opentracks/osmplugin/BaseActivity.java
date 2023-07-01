package de.storchp.opentracks.osmplugin;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
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

import java.util.Arrays;
import java.util.Locale;

import de.storchp.opentracks.osmplugin.databinding.CompassSmoothingDialogBinding;
import de.storchp.opentracks.osmplugin.databinding.OverdrawFactorDialogBinding;
import de.storchp.opentracks.osmplugin.databinding.StrokeWidthDialogBinding;
import de.storchp.opentracks.osmplugin.databinding.TilecacheCapacityFactorDialogBinding;
import de.storchp.opentracks.osmplugin.databinding.TrackSmoothingDialogBinding;
import de.storchp.opentracks.osmplugin.utils.ArrowMode;
import de.storchp.opentracks.osmplugin.utils.MapMode;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;
import de.storchp.opentracks.osmplugin.utils.StatisticElement;
import de.storchp.opentracks.osmplugin.utils.TrackColorMode;

abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    protected MenuItem mapConsent;

    public boolean onCreateOptionsMenu(Menu menu, boolean showInfo) {
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

        var multiThreadMapRendering = menu.findItem(R.id.multi_thread_map_rendering);
        multiThreadMapRendering.setChecked(PreferencesUtils.getMultiThreadMapRendering());

        var persistentTileCache = menu.findItem(R.id.persistent_tilecache);
        persistentTileCache.setChecked(PreferencesUtils.getPersistentTileCache());

        var pipMode = menu.findItem(R.id.pip_mode);
        pipMode.setChecked(PreferencesUtils.isPipEnabled());

        var debugTrackPoints = menu.findItem(R.id.debug_trackpoints);
        debugTrackPoints.setChecked(PreferencesUtils.isDebugTrackPoints());


        menu.findItem(R.id.arrow_mode).setTitle(PreferencesUtils.getArrowMode().getMessageId());
        menu.findItem(R.id.map_mode).setTitle(PreferencesUtils.getMapMode().getMessageId());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.map_online_consent) {
            item.setChecked(!item.isChecked());
            PreferencesUtils.setOnlineMapConsent(item.isChecked());
            onOnlineMapConsentChanged(item.isChecked());
        } else if (itemId == R.id.track_color) {
            showTrackColorDialog();
        } else if (itemId == R.id.configure_statistic) {
            showConfigureStatisticDialog();
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
        } else if (itemId == R.id.overdraw_factor) {
            showOverdrawFactorDialog();
        } else if (itemId == R.id.tilecache_capacity_factor) {
            showTileCacheCapacityFactorDialog();
        } else if (itemId == R.id.debug_trackpoints) {
            item.setChecked(!item.isChecked());
            PreferencesUtils.setDebugTrackPoints(item.isChecked());
            updateDebugTrackPoints();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showTrackColorDialog() {
        var trackColorModes = TrackColorMode.values();
        var currentTrackColorMode = PreferencesUtils.getTrackColorMode();

        new android.app.AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.track_color)
                .setSingleChoiceItems(Arrays.stream(trackColorModes).map(trackColorMode -> getString(trackColorMode.getLabelResId())).toArray(String[]::new),
                        currentTrackColorMode.ordinal(),
                        (dialog, which) -> {
                            PreferencesUtils.setTrackColorMode(trackColorModes[which]);
                            dialog.dismiss();
                            recreate();
                        })
                .create().show();
    }

    private void showConfigureStatisticDialog() {
        var availableStatisticElements = StatisticElement.values();
        var selectedStatisticElements = PreferencesUtils.getStatisticElements();
        var selected = new boolean[availableStatisticElements.length];
        for (int i = 0; i < selected.length; i++) {
            selected[i] = selectedStatisticElements.contains(availableStatisticElements[i]);
        }

        new android.app.AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.configure_statistic)
                .setMultiChoiceItems(Arrays.stream(availableStatisticElements).map(se -> getString(se.getLabelResId())).toArray(String[]::new),
                        selected,
                        (dialog, which, isChecked) -> {
                            if (isChecked) {
                                selectedStatisticElements.add(availableStatisticElements[which]);
                            } else {
                                selectedStatisticElements.remove(availableStatisticElements[which]);
                            }
                            PreferencesUtils.setStatisticElements(selectedStatisticElements);
                        })
                .setOnDismissListener(dialog -> this.recreate())
                .create().show();
    }

    protected abstract void changeMapMode(MapMode mapMode);

    public void updateDebugTrackPoints() {
        // override in subclasses
    }

    protected abstract void changeArrowMode(ArrowMode arrowMode);

    private void showTrackSmoothingDialog() {
        var binding = TrackSmoothingDialogBinding.inflate(LayoutInflater.from(this));
        binding.etTolerance.setText(String.valueOf(PreferencesUtils.getTrackSmoothingTolerance()));

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
            var newTolerance = binding.etTolerance.getText().toString().trim();
            if (newTolerance.length() > 0 && TextUtils.isDigitsOnly(newTolerance)) {
                PreferencesUtils.setTrackSmoothingTolerance(Integer.parseInt(newTolerance));
                alertDialog.dismiss();
                this.recreate();
            } else {
                Toast.makeText(BaseActivity.this, R.string.only_digits, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showCompassSmoothingDialog() {
        var binding = CompassSmoothingDialogBinding.inflate(LayoutInflater.from(this));
        int smoothing = PreferencesUtils.getCompassSmoothing();
        binding.tvCompassSmoothing.setText(String.valueOf(smoothing));
        binding.sbCompassSmoothing.setProgress(smoothing);
        binding.sbCompassSmoothing.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                binding.tvCompassSmoothing.setText(String.valueOf(Math.max(binding.sbCompassSmoothing.getProgress(), 1)));
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
            PreferencesUtils.setCompassSmoothing(binding.sbCompassSmoothing.getProgress());
            alertDialog.dismiss();
        });
    }

    private void showOverdrawFactorDialog() {
        OverdrawFactorDialogBinding binding = OverdrawFactorDialogBinding.inflate(LayoutInflater.from(this));
        double currentOverdrawFactor = PreferencesUtils.getOverdrawFactor();

        binding.tvOverdrawFactor.setText(String.format(Locale.getDefault(), "%.2f", currentOverdrawFactor));
        binding.sbOverdrawFactor.setProgress((int) (100 * currentOverdrawFactor) - 100);
        binding.sbOverdrawFactor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                binding.tvOverdrawFactor.setText(String.format(Locale.getDefault(), "%.2f", getOverdrawFactorFromProgress(binding)));
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
            double overdrawFactor = getOverdrawFactorFromProgress(binding);
            PreferencesUtils.setOverdrawFactor(overdrawFactor);
            Log.i(TAG, "New overdrawFactor: " + overdrawFactor);
            alertDialog.dismiss();
        });
    }

    private double getOverdrawFactorFromProgress(final OverdrawFactorDialogBinding binding) {
        int progress = binding.sbOverdrawFactor.getProgress();
        if (progress == 0) {
            return 1;
        }
        return (progress / (double) 100) + 1;
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

    private void showStrokeWidthDialog() {
        StrokeWidthDialogBinding binding = StrokeWidthDialogBinding.inflate(LayoutInflater.from(this));
        int currentStrokeWidth = PreferencesUtils.getStrokeWidth();
        binding.tvStrokeWidth.setText(String.valueOf(currentStrokeWidth));
        binding.sbStrokeWidth.setProgress(currentStrokeWidth);
        binding.sbStrokeWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                binding.tvStrokeWidth.setText(String.valueOf(Math.max(binding.sbStrokeWidth.getProgress(), 1)));
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
            PreferencesUtils.setStrokeWidth(binding.sbStrokeWidth.getProgress());
            alertDialog.dismiss();
            this.recreate();
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

    protected void openDirectory(ActivityResultLauncher<Intent> launcher) {
        try {
            launcher.launch(createOpenDocumentIntent());
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(BaseActivity.this, R.string.no_file_manager_found, Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    private Intent createOpenDocumentIntent() {
        var intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
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

    protected void changeThemeDirectory(Uri uri, Intent resultData) {
        takePersistableUriPermission(uri, resultData);
        PreferencesUtils.setMapThemeDirectoryUri(uri);
        releaseOldPermissions();
    }

    protected void changeMapDirectory(Uri uri, Intent resultData) {
        takePersistableUriPermission(uri, resultData);
        PreferencesUtils.setMapDirectoryUri(uri);
        releaseOldPermissions();
    }

    private void takePersistableUriPermission(Uri uri, Intent intent) {
        int takeFlags = intent.getFlags();
        takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getContentResolver().takePersistableUriPermission(uri, takeFlags);
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
