package de.storchp.opentracks.osmplugin;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;

import de.storchp.opentracks.osmplugin.databinding.ActivityDownloadBinding;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

public class DownloadActivity extends BaseActivity {

    private static final String TAG = DownloadActivity.class.getSimpleName();

    private static final String OPENANDROMAPS_MAP_HOST = "ftp.gwdg.de";
    private static final String OPENANDROMAPS_THEME_HOST = "www.openandromaps.org";
    private static final String OPENANDROMAPS_MAP_DOWNLOAD_URL = "https://" + OPENANDROMAPS_MAP_HOST + "/pub/misc/openstreetmap/openandromaps/mapsV4/";
    private static final String OPENANDROMAPS_THEME_DOWNLOAD_URL = "https://" + OPENANDROMAPS_THEME_HOST + "/wp-content/users/tobias/";
    private static final String FREIZEITKARTE_HOST = "download.freizeitkarte-osm.de";

    private static final String MF_V4_MAP_SCHEME = "mf-v4-map";
    private static final String MF_THEME_SCHEME = "mf-theme";

    private Uri downloadUri;
    private DownloadType downloadType = DownloadType.MAP;
    private ActivityDownloadBinding binding;
    private long downloadID;
    private DownloadBroadcastReceiver downloadBroadcastReceiver;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDownloadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.mapsToolbar.setTitle(R.string.download_map);
        setSupportActionBar(binding.toolbar.mapsToolbar);

        var uri = getIntent().getData();
        if (uri != null) {
            var scheme = uri.getScheme();
            var host = uri.getHost();
            var path = uri.getPath();
            Log.i(TAG, "scheme=" + scheme + ",host=" + host + ", path=" + path + ", lastPathSegment=" + uri.getLastPathSegment());
            downloadUri = uri;

            if (MF_V4_MAP_SCHEME.equals(scheme)) {
                if (host.equals("download.openandromaps.org") && path.startsWith("/mapsV4/") && path.endsWith(".zip")) {
                    // OpenAndroMaps URIs need to be remapped - mf-v4-map://download.openandromaps.org/mapsV4/Germany/bayern.zip
                    downloadUri = Uri.parse(OPENANDROMAPS_MAP_DOWNLOAD_URL + path.substring(8));
                    downloadType = DownloadType.MAP_ZIP;
                } else {
                    // try to replace MF_V4_MAP_SCHEME with https for unknown sources
                    downloadUri = uri.buildUpon().scheme("https").build();
                    downloadType = path.endsWith(".zip") ? DownloadType.MAP_ZIP : DownloadType.MAP;
                }
            } else if (MF_THEME_SCHEME.equals(scheme)) {
                if (host.equals("download.openandromaps.org") && path.startsWith("/themes/") && path.endsWith(".zip")) {
                    // no remapping, as they have themes only on their homepage, not on their ftp site
                    downloadUri = Uri.parse(OPENANDROMAPS_THEME_DOWNLOAD_URL + path.substring(8));
                } else {
                    // try to replace MF_THEME_SCHEME with https for unknown sources
                    downloadUri = uri.buildUpon().scheme("https").build();
                }
                downloadType = DownloadType.THEME;
                binding.toolbar.mapsToolbar.setTitle(R.string.download_theme);
            } else if (FREIZEITKARTE_HOST.equals(host)) {
                if (path.endsWith(".map.zip")) {
                    downloadType = DownloadType.MAP_ZIP;
                } else if (path.endsWith(".zip")) {
                    downloadType = DownloadType.THEME;
                    binding.toolbar.mapsToolbar.setTitle(R.string.download_theme);
                }
            } else if (OPENANDROMAPS_MAP_HOST.equals(host) && path.endsWith(".zip")) {
                downloadType = DownloadType.MAP_ZIP;
            } else if (OPENANDROMAPS_THEME_HOST.equals(host) && path.endsWith(".zip")) {
                downloadType = DownloadType.THEME;
            }

            Log.i(TAG, "downloadUri=" + downloadUri + ", downloadType=" + downloadType);

            binding.downloadInfo.setText(downloadUri.toString());
            binding.startDownloadButton.setOnClickListener((view) -> {
                startDownload();
            });
        } else {
            binding.downloadInfo.setText(R.string.no_download_uri_found);
            binding.startDownloadButton.setEnabled(false);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            public void handleOnBackPressed() {
                navigateUp();
            }
        });

        downloadBroadcastReceiver = new DownloadBroadcastReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(downloadBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    public void startDownload() {
        var filesDir = getFilesDir().toPath();
        var downloadDir = filesDir.resolve(downloadType.subdir);
        downloadDir.toFile().mkdir();
        var filename = downloadUri.getLastPathSegment();
        var file = downloadDir.resolve(filename);
        if (file.toFile().exists()) {
            new AlertDialog.Builder(DownloadActivity.this)
                    .setIcon(R.drawable.ic_logo_color_24dp)
                    .setTitle(R.string.app_name)
                    .setMessage(getString(downloadType.getOverwriteMessageId(), filename))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        file.toFile().delete();
                        startDownload();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
            return;
        }

        var request = new DownloadManager.Request(downloadUri)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(this, null, downloadType.subdir + "/" + filename)
                .setTitle(filename)
                .setDescription(getString(downloadType.downloadMessageId))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true);
        var downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadID = downloadManager.enqueue(request);
        observeProgress(downloadManager, downloadID);
    }

    private void observeProgress(DownloadManager downloadManager, long downloadId) {
        var query = new DownloadManager.Query().setFilterById(downloadId);
        var downloading = true;
        binding.progressBar.setIndeterminate(false);
        binding.progressBar.setMax(100);

        while (downloading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            try (var cursor = downloadManager.query(query)) {
                if (cursor.moveToFirst()) {
                    var statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    var progressIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    var totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                    int status = cursor.getInt(statusIndex);
                    long downloaded = cursor.getLong(progressIndex);
                    long total = cursor.getLong(totalIndex);
                    int progress = (int) ((downloaded * 100L) / total);

                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        downloading = false;
                    }

                    if (total >= 0) {
                        binding.progressBar.setProgress(progress);
                    }
                }
            }

            try {
                Thread.sleep(1000); // Wait for 1 second before querying again
            } catch (InterruptedException ignored) {

            }
        }
        binding.progressBar.setVisibility(View.GONE);
    }

    private void downloadEnded() {
        binding.progressBar.setVisibility(View.GONE);
        Toast.makeText(this, downloadType.getSuccessMessageId(), Toast.LENGTH_LONG).show();
        // TODO: unzip if necessary
    }

    private class DownloadBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadID == id) {
                downloadEnded();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloadBroadcastReceiver != null) {
            unregisterReceiver(downloadBroadcastReceiver);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            navigateUp();
            return true;
        }
        return false;
    }

    public void navigateUp() {
        finish();
    }

    private enum DownloadType {

        MAP(R.string.download_map, R.string.overwrite_map_question, R.string.download_success, R.string.download_failed, false, "maps") {
            @Override
            public Uri getDirectoryUri() {
                return PreferencesUtils.getMapDirectoryUri();
            }

            @Override
            public Class<? extends DirectoryChooserActivity> getDirectoryChooser() {
                return DirectoryChooserActivity.MapDirectoryChooserActivity.class;
            }
        },
        MAP_ZIP(R.string.download_map, R.string.overwrite_map_question, R.string.download_success, R.string.download_failed, true, "maps") {
            @Override
            public Uri getDirectoryUri() {
                return PreferencesUtils.getMapDirectoryUri();
            }

            @Override
            public Class<? extends DirectoryChooserActivity> getDirectoryChooser() {
                return DirectoryChooserActivity.MapDirectoryChooserActivity.class;
            }
        },
        THEME(R.string.download_theme, R.string.overwrite_theme_question, R.string.download_theme_success, R.string.download_theme_failed, false, "themes") {
            @Override
            public Uri getDirectoryUri() {
                return PreferencesUtils.getMapThemeDirectoryUri();
            }

            @Override
            public Class<? extends DirectoryChooserActivity> getDirectoryChooser() {
                return DirectoryChooserActivity.ThemeDirectoryChooserActivity.class;
            }
        };

        private final int downloadMessageId;
        private final int overwriteMessageId;
        private final int successMessageId;
        private final int failedMessageId;
        private final boolean extractMapFromZIP;
        private final String subdir;

        DownloadType(int downloadMessageId, int overwriteMessageId, int successMessageId, int failedMessageId, boolean extractMapFromZIP, String subdir) {
            this.downloadMessageId = downloadMessageId;
            this.overwriteMessageId = overwriteMessageId;
            this.successMessageId = successMessageId;
            this.failedMessageId = failedMessageId;
            this.extractMapFromZIP = extractMapFromZIP;
            this.subdir = subdir;
        }

        abstract public Uri getDirectoryUri();

        public int getOverwriteMessageId() {
            return overwriteMessageId;
        }

        public int getSuccessMessageId() {
            return successMessageId;
        }

        public int getFailedMessageId() {
            return failedMessageId;
        }

        public boolean isExtractMapFromZIP() {
            return extractMapFromZIP;
        }

        public abstract Class<? extends DirectoryChooserActivity> getDirectoryChooser();

        public int getDownloadMessageId() {
            return downloadMessageId;
        }

        public String getSubdir() {
            return subdir;
        }
    }

}
