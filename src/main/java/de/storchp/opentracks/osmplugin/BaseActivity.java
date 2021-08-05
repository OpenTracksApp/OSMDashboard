package de.storchp.opentracks.osmplugin;

import android.app.Activity;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuCompat;

import java.util.List;

import de.storchp.opentracks.osmplugin.databinding.CompassSmoothingDialogBinding;
import de.storchp.opentracks.osmplugin.databinding.StrokeWidthDialogBinding;
import de.storchp.opentracks.osmplugin.databinding.TrackSmoothingDialogBinding;
import de.storchp.opentracks.osmplugin.utils.ArrowMode;
import de.storchp.opentracks.osmplugin.utils.MapMode;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

abstract class BaseActivity extends AppCompatActivity {

    protected static final int REQUEST_MAP_DIRECTORY = 1;
    protected static final int REQUEST_THEME_DIRECTORY = 2;
    protected static final int REQUEST_DOWNLOAD_MAP = 3;
    protected static final int REQUEST_MAP_DIRECTORY_FOR_DOWNLOAD = 4;
    protected static final int REQUEST_MAP_SELECTION = 5;
    protected static final int REQUEST_THEME_SELECTION = 6;
    protected static final int REQUEST_THEME_DIRECTORY_FOR_DOWNLOAD = 7;

    protected MenuItem mapConsent;
    protected MenuItem pipMode;
    protected MenuItem multiThreadMapRendering;

    public boolean onCreateOptionsMenu(final Menu menu, final boolean showInfo) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.maps, menu);

        MenuCompat.setGroupDividerEnabled(menu, true);

        final MenuItem mapInfo = menu.findItem(R.id.map_info);
        mapInfo.setVisible(showInfo);

        mapConsent = menu.findItem(R.id.map_online_consent);
        mapConsent.setChecked(PreferencesUtils.getOnlineMapConsent());

        if (BuildConfig.offline) {
            mapConsent.setVisible(false);
            menu.findItem(R.id.download_map).setVisible(false);
        }

        multiThreadMapRendering = menu.findItem(R.id.multi_thread_map_rendering);
        multiThreadMapRendering.setChecked(PreferencesUtils.getMultiThreadMapRendering());

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
        } else if (itemId == R.id.pip_mode) {
            item.setChecked(!item.isChecked());
            PreferencesUtils.setPipEnabled(item.isChecked());
        } else if (itemId == R.id.map_selection) {
            startActivityForResult(new Intent(this, MapSelectionActivity.class), REQUEST_MAP_SELECTION);
        } else if (itemId == R.id.theme_selection) {
            startActivityForResult(new Intent(this, ThemeSelectionActivity.class), REQUEST_THEME_SELECTION);
        } else if (itemId == R.id.map_folder) {
            openMapDirectoryChooser();
        } else if (itemId == R.id.theme_folder) {
            openThemeDirectoryChooser();
        } else if (itemId == R.id.download_map) {
            startActivityForResult(new Intent(this, DownloadMapSelectionActivity.class), REQUEST_DOWNLOAD_MAP);
        } else if (itemId == R.id.arrow_mode) {
            ArrowMode arrowMode = PreferencesUtils.getArrowMode();
            arrowMode = arrowMode.next();
            item.setTitle(arrowMode.getMessageId());
            PreferencesUtils.setArrowMode(arrowMode);
            changeArrowMode(arrowMode);
        } else if (itemId == R.id.map_mode) {
            MapMode mapMode = PreferencesUtils.getMapMode();
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
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final TrackSmoothingDialogBinding binding = TrackSmoothingDialogBinding.inflate(LayoutInflater.from(this));
        binding.etTolerance.setText(String.valueOf(PreferencesUtils.getTrackSmoothingTolerance()));

        builder.setView(binding.getRoot())
               .setIcon(R.drawable.ic_logo_color_24dp)
               .setTitle(R.string.app_name)
                .setCancelable(false)
               .setPositiveButton(android.R.string.ok, null)
               .setNegativeButton(android.R.string.cancel, null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            final String newTolerance = binding.etTolerance.getText().toString().trim();
            if (newTolerance.length() > 0 && TextUtils.isDigitsOnly(newTolerance)) {
                PreferencesUtils.setTrackSmoothingTolerance(Integer.parseInt(newTolerance));
                alertDialog.dismiss();
            } else {
                Toast.makeText(BaseActivity.this, R.string.only_digits, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showCompassSmoothingDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final CompassSmoothingDialogBinding binding = CompassSmoothingDialogBinding.inflate(LayoutInflater.from(this));
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

        builder.setView(binding.getRoot())
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            final int compassSmoothing = binding.sbCompassSmoothing.getProgress();
            PreferencesUtils.setCompassSmoothing(compassSmoothing);
            alertDialog.dismiss();
        });
    }

    private void showStrokeWidthDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

        builder.setView(binding.getRoot())
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            final int strokeWidth = binding.sbStrokeWidth.getProgress();
            PreferencesUtils.setStrokeWidth(strokeWidth);
            alertDialog.dismiss();
        });
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
                } else if (requestCode == REQUEST_THEME_DIRECTORY || requestCode == REQUEST_THEME_DIRECTORY_FOR_DOWNLOAD) {
                    changeThemeDirectory(uri, requestCode, resultData);
                }
            }
        }

        // release old permissions
        final Uri mapDirectoryUri = PreferencesUtils.getMapDirectoryUri();
        final Uri themeDirectoryUri = PreferencesUtils.getMapThemeDirectoryUri();
        final List<UriPermission> persistedUriPermissions = getContentResolver().getPersistedUriPermissions();
        for (final UriPermission permission : persistedUriPermissions) {
            final Uri uri = permission.getUri();
            if (!uri.equals(mapDirectoryUri) && !uri.equals(themeDirectoryUri)) {
                getContentResolver().releasePersistableUriPermission(uri, 0);
            }
        }
    }

    protected void changeThemeDirectory(final Uri uri, final int requestCode, final Intent resultData) {
        takePersistableUriPermission(uri, resultData);
        PreferencesUtils.setMapThemeDirectoryUri(uri);
    }

    protected void changeMapDirectory(final Uri uri, final int requestCode, final Intent resultData) {
        takePersistableUriPermission(uri, resultData);
        PreferencesUtils.setMapDirectoryUri(uri);
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
