package de.storchp.opentracks.osmplugin.download;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.storchp.opentracks.osmplugin.BaseActivity;
import de.storchp.opentracks.osmplugin.R;
import de.storchp.opentracks.osmplugin.databinding.ActivityDownloadBinding;
import de.storchp.opentracks.osmplugin.settings.DirectoryChooserActivity;
import de.storchp.opentracks.osmplugin.utils.FileUtil;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

public class DownloadActivity extends BaseActivity {

    private static final String TAG = DownloadActivity.class.getSimpleName();
    private static final int UPDATE_DOWNLOAD_PROGRESS = 1;

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
    private Long downloadId = null;
    private DownloadManager downloadManager;
    private DownloadBroadcastReceiver downloadBroadcastReceiver;
    private final ExecutorService executor = Executors.newFixedThreadPool(1);

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

            downloadBroadcastReceiver = new DownloadBroadcastReceiver();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(downloadBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(downloadBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }

            startDownload();
        } else {
            binding.downloadInfo.setText(R.string.no_download_uri_found);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            public void handleOnBackPressed() {
                navigateUp();
            }
        });
    }

    protected final ActivityResultLauncher<Intent> directoryIntentLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    startDownload();
                }
            });

    public void startDownload() {
        var directoryUri = downloadType.getDirectoryUri();
        if (directoryUri != null) {
            var directoryFile = FileUtil.getDocumentFileFromTreeUri(this, directoryUri);
            if (directoryFile != null && !directoryFile.canWrite()) {
                directoryIntentLauncher.launch(new Intent(this, downloadType.getDirectoryChooser()));
                return;
            }
        }

        var filesDir = getExternalFilesDir(null).toPath();
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
                        dialog.dismiss();
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
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadId = downloadManager.enqueue(request);
        observeDownload();
    }

    private void observeDownload() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.progressBar.setIndeterminate(false);
        binding.progressBar.setMax(100);
        binding.progressBar.setProgress(0);

        executor.execute(() -> {
            int progress = 0;
            boolean isDownloadFinished = false;
            while (!isDownloadFinished) {
                try (Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId))) {
                    if (cursor.moveToFirst()) {
                        int downloadStatus = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                        switch (downloadStatus) {
                            case DownloadManager.STATUS_RUNNING:
                                long totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                                if (totalBytes > 0) {
                                    long downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                    progress = (int) (downloadedBytes * 100 / totalBytes);
                                }

                                break;
                            case DownloadManager.STATUS_SUCCESSFUL:
                                progress = 100;
                                isDownloadFinished = true;
                                break;
                            case DownloadManager.STATUS_PAUSED:
                            case DownloadManager.STATUS_PENDING:
                                break;
                            case DownloadManager.STATUS_FAILED:
                                isDownloadFinished = true;
                                break;
                        }
                        var message = Message.obtain();
                        message.what = UPDATE_DOWNLOAD_PROGRESS;
                        message.arg1 = progress;
                        mainHandler.sendMessage(message);
                    }
                }
            }
        });
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == UPDATE_DOWNLOAD_PROGRESS) {
                int downloadProgress = msg.arg1;
                binding.progressBar.setProgress(downloadProgress);
            }
            return true;
        }
    });

    private class DownloadBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            var id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId != id) {
                return;
            }
            try (var cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId))) {
                if (cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        var uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                        downloadEnded(Uri.parse(uri));
                    }
                }
            }
        }
    }

    private void downloadEnded(Uri downloadedUri) {
        executor.shutdown();
        mainHandler.removeCallbacksAndMessages(null);
        var destinationDir = downloadType.getDirectoryUri();
        if (destinationDir == null) {
            if (downloadType.isExtractMapFromZIP()) {
                try (var inputStream = getContentResolver().openInputStream(downloadedUri)) {
                    var zis = new ZipInputStream(inputStream);
                    ZipEntry ze;
                    boolean foundMapInZip = false;
                    while (!foundMapInZip && (ze = zis.getNextEntry()) != null) {
                        var filename = ze.getName();
                        if (filename.endsWith(".map")) {
                            var downloadedFile = new File(downloadedUri.getPath());
                            var targetFile = new File(downloadedFile.getParent(), filename);
                            copy(zis, new FileOutputStream(targetFile));
                            foundMapInZip = true;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    new File(downloadedUri.getPath()).delete();
                }
            }
        } else {
            try (var inputStream = getContentResolver().openInputStream(downloadedUri)) {
                if (downloadType.isExtractMapFromZIP()) {
                    var zis = new ZipInputStream(inputStream);
                    ZipEntry ze;
                    boolean foundMapInZip = false;
                    while (!foundMapInZip && (ze = zis.getNextEntry()) != null) {
                        var filename = ze.getName();
                        if (filename.endsWith(".map")) {
                            var targetFile = createBinaryDocumentFile(destinationDir, filename);
                            copy(zis, getContentResolver().openOutputStream(targetFile.getUri(), "wt"));
                            foundMapInZip = true;
                        }
                    }
                } else {
                    var filename = downloadedUri.getLastPathSegment();
                    var targetFile = createBinaryDocumentFile(destinationDir, filename);
                    copy(inputStream, getContentResolver().openOutputStream(targetFile.getUri(), "wt"));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                new File(downloadedUri.getPath()).delete();
            }
        }

        binding.progressBar.setVisibility(View.GONE);
        downloadId = null;
        Toast.makeText(this, downloadType.getSuccessMessageId(), Toast.LENGTH_LONG).show();
    }

    private @NonNull DocumentFile createBinaryDocumentFile(Uri destinationDir, String filename) {
        var directoryFile = FileUtil.getDocumentFileFromTreeUri(this, destinationDir);
        var targetFile = directoryFile.createFile("application/binary", filename);
        if (targetFile == null) {
            throw new RuntimeException("Unable to create file: " + filename);
        }
        return targetFile;
    }

    private void copy(InputStream input, OutputStream output) throws IOException {
        var data = new byte[4096];
        int count;
        while ((count = input.read(data)) != -1) {
            output.write(data, 0, count);
        }
        input.close();
        output.close();
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

    public enum DownloadType {

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
