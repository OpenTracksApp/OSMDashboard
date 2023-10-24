package de.storchp.opentracks.osmplugin;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.storchp.opentracks.osmplugin.databinding.ActivityDownloadBinding;
import de.storchp.opentracks.osmplugin.utils.FileUtil;
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
    private DownloadTask downloadTask;
    private ActivityDownloadBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDownloadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.mapsToolbar.setTitle(R.string.download_map);
        setSupportActionBar(binding.toolbar.mapsToolbar);

        binding.progressBar.setIndeterminate(true);

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
            binding.startDownloadButton.setOnClickListener((view) -> startDownload());
        } else {
            binding.downloadInfo.setText(R.string.no_download_uri_found);
            binding.startDownloadButton.setEnabled(false);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            public void handleOnBackPressed() {
                navigateUp();
            }
        });
    }

    private boolean isDownloadInProgress() {
        return downloadTask != null;
    }

    protected final ActivityResultLauncher<Intent> directoryIntentLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    startDownload();
                }
            });

    public void startDownload() {
        var directoryUri = downloadType.getDirectoryUri();
        if (directoryUri == null) {
            directoryIntentLauncher.launch(new Intent(this, downloadType.getDirectoryChooser()));
            return;
        }

        var directoryFile = FileUtil.getDocumentFileFromTreeUri(this, directoryUri);
        if (directoryFile == null || !directoryFile.canWrite()) {
            directoryIntentLauncher.launch(new Intent(this, downloadType.getDirectoryChooser()));
            return;
        }

        var fileName = downloadUri.getLastPathSegment();
        var file = directoryFile.findFile(fileName);
        if (file != null) {
            new AlertDialog.Builder(DownloadActivity.this)
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setMessage(getString(downloadType.getOverwriteMessageId(), fileName))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    file.delete();
                    startDownload();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.progressBar.setIndeterminate(true);

        keepScreenOn(true);
        downloadTask = new DownloadTask(this, downloadUri, directoryFile, fileName, downloadType);
        downloadTask.start();

        Log.d(TAG, "Started download of '" + fileName + "'");
    }

    private void downloadEnded(boolean success, boolean canceled) {
        binding.progressBar.setVisibility(View.GONE);
        keepScreenOn(false);
        Uri targetUri = downloadTask != null ? downloadTask.targetUri : null;
        downloadTask = null;
        if (canceled) {
            if (targetUri != null) {
                var documentFile = FileUtil.getDocumentFileFromTreeUri(this, targetUri);
                if (documentFile != null) {
                    documentFile.delete();
                }
            }
            navigateUp();
            return;
        }
        if (success) {
            Toast.makeText(this, downloadType.getSuccessMessageId(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, downloadType.getFailedMessageId(), Toast.LENGTH_LONG).show();
        }
    }

    private static class DownloadTask extends Thread {
        private final WeakReference<DownloadActivity> ref;
        private final Uri downloadUri;
        private final DocumentFile directoryFile;
        private final DownloadType downloadType;
        private final String filename;
        private Uri targetUri;
        private int contentLength = -1;
        private boolean success = false;
        private boolean canceled = false;

        public DownloadTask(DownloadActivity activity, Uri downloadUri, DocumentFile directoryFile, String filename, DownloadType downloadType) {
            ref = new WeakReference<>(activity);
            this.downloadUri = downloadUri;
            this.directoryFile = directoryFile;
            this.downloadType = downloadType;
            this.filename = filename;
        }

        protected void publishProgress(int progress) {
            var activity = ref.get();
            if (activity != null) {
                activity.runOnUiThread(() -> activity.updateProgress(contentLength, progress));
            }
        }

        protected void end() {
            var activity = ref.get();
            if (activity != null) {
                activity.runOnUiThread(() -> activity.downloadEnded(success, canceled));
            }
        }

        @Override
        public void run() {
            InputStream input = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(downloadUri.toString()).openConnection();
                connection.connect();
                contentLength = connection.getContentLength();

                input = connection.getInputStream();
                if (downloadType.isExtractMapFromZIP()) {
                    var zis = new ZipInputStream(input);
                    ZipEntry ze;
                    boolean foundMapInZip = false;
                    while (!foundMapInZip && (ze = zis.getNextEntry()) != null) {
                        if (ze.getName().endsWith(".map")) {
                            contentLength = (int) ze.getSize();
                            copy(zis, ze.getName(), directoryFile);
                            foundMapInZip = true;
                        }
                    }
                } else {
                    copy(input, filename, directoryFile);
                }

                success = true;
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException ignored) {
                }

                if (connection != null) {
                    connection.disconnect();
                }
            }
            end();
        }

        private void copy(InputStream input, String filename, DocumentFile directoryFile) throws IOException {
            var file = directoryFile.createFile("application/binary", filename);
            if (file == null) {
                throw new IOException("Unable to create file: " + filename);
            }
            targetUri = file.getUri();
            var output = ref.get().getContentResolver().openOutputStream(targetUri);

            var data = new byte[4096];
            int count;
            int bytesWritten = 0;
            while ((count = input.read(data)) != -1) {
                if (canceled) {
                    input.close();
                    output.close();
                    end();
                    return;
                }
                output.write(data, 0, count);
                bytesWritten += count;
                publishProgress(bytesWritten);
            }
            output.close();
        }

        public void cancelDownload() {
            canceled = true;
        }
    }

    private void updateProgress(int contentLength, int progress) {
        binding.progressBar.setProgress(progress);
        if (contentLength > 0 && binding.progressBar.isIndeterminate()) {
            // we have a content length, so switch to determinate progress
            binding.progressBar.setIndeterminate(false);
            binding.progressBar.setMax(contentLength);
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
        if (isDownloadInProgress()) {
            new AlertDialog.Builder(DownloadActivity.this)
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.cancel_download_question))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> downloadTask.cancelDownload())
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
        } else {
            finish();
        }
    }

    private enum DownloadType {

        MAP(R.string.overwrite_map_question, R.string.download_success, R.string.download_failed, false) {
            @Override
            public Uri getDirectoryUri() {
                return  PreferencesUtils.getMapDirectoryUri();
            }

            @Override
            public Class<? extends DirectoryChooserActivity> getDirectoryChooser() {
                return DirectoryChooserActivity.MapDirectoryChooserActivity.class;
            }
        },
        MAP_ZIP(R.string.overwrite_map_question, R.string.download_success, R.string.download_failed, true) {
            @Override
            public Uri getDirectoryUri() {
                return  PreferencesUtils.getMapDirectoryUri();
            }
            @Override
            public Class<? extends DirectoryChooserActivity> getDirectoryChooser() {
                return DirectoryChooserActivity.MapDirectoryChooserActivity.class;
            }
        },
        THEME(R.string.overwrite_theme_question, R.string.download_theme_success, R.string.download_theme_failed, false) {
            @Override
            public Uri getDirectoryUri() {
                return  PreferencesUtils.getMapThemeDirectoryUri();
            }
            @Override
            public Class<? extends DirectoryChooserActivity> getDirectoryChooser() {
                return DirectoryChooserActivity.ThemeDirectoryChooserActivity.class;
            }
        };

        private final int overwriteMessageId;
        private final int successMessageId;
        private final int failedMessageId;
        private final boolean extractMapFromZIP;

        DownloadType(int overwriteMessageId, int successMessageId, int failedMessageId, boolean extractMapFromZIP) {
            this.overwriteMessageId = overwriteMessageId;
            this.successMessageId = successMessageId;
            this.failedMessageId = failedMessageId;
            this.extractMapFromZIP = extractMapFromZIP;
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
    }

}
