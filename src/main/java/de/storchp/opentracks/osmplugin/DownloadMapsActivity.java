package de.storchp.opentracks.osmplugin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import de.storchp.opentracks.osmplugin.utils.FileUtil;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

public class DownloadMapsActivity extends BaseActivity {

    private static final String TAG = DownloadMapsActivity.class.getSimpleName();

    private static final String MAPS_V_5 = "https://ftp-stud.hs-esslingen.de/pub/Mirrors/download.mapsforge.org/maps/v5/";

    private Uri downloadMapUri;
    private ProgressBar progressBar;
    private DownloadTask downloadTask;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_maps);

        final Toolbar toolbar = findViewById(R.id.maps_toolbar);
        toolbar.setTitle(R.string.choose_map_to_download);
        setSupportActionBar(toolbar);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setIndeterminate(true);

        final WebView webView = findViewById(R.id.webview);
        final WebViewClient webClient = new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                Log.d(TAG, "URL: " + url);
                if (!url.startsWith(MAPS_V_5)) {
                    return true; // don't load URLs outside the base URL
                }
                final Uri uri = Uri.parse(url);
                final String lastPathSegment = uri.getLastPathSegment();
                if (lastPathSegment != null && lastPathSegment.endsWith(".map")) {
                    if (isDownloadInProgress()) {
                        Toast.makeText(DownloadMapsActivity.this.getApplicationContext(), R.string.download_in_progress, Toast.LENGTH_LONG).show();
                        return true;
                    }
                    new AlertDialog.Builder(DownloadMapsActivity.this)
                        .setIcon(R.drawable.ic_logo_color_24dp)
                        .setTitle(R.string.app_name)
                        .setMessage(getString(R.string.download_map_question, lastPathSegment))
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            downloadMapUri = uri;
                            startMapDownload();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .create().show();
                    return true;
                }
                PreferencesUtils.setLastDownloadUrl(DownloadMapsActivity.this, url);
                return false;
            }

        };
        webView.setWebViewClient(webClient);
        webView.loadUrl(PreferencesUtils.getLastDownloadUrl(this, MAPS_V_5));
    }

    private boolean isDownloadInProgress() {
        return downloadTask != null;
    }

    private void startMapDownload() {
        final Uri mapDirectoryUri = PreferencesUtils.getMapDirectoryUri(this);
        if (mapDirectoryUri == null) {
            openDirectory(REQUEST_MAP_DIRECTORY_FOR_DOWNLOAD);
            return;
        }
        final DocumentFile mapDirectoryFile = FileUtil.getDocumentFileFromTreeUri(this, mapDirectoryUri);
        if (!mapDirectoryFile.canWrite()) {
            openDirectory(REQUEST_MAP_DIRECTORY_FOR_DOWNLOAD);
            return;
        }
        final String mapName = downloadMapUri.getLastPathSegment();

        final DocumentFile file = mapDirectoryFile.findFile(mapName);
        if (file != null) {
            new AlertDialog.Builder(DownloadMapsActivity.this)
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.overwrite_map_question, mapName))
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    file.delete();
                    startMapDownload();
                })
                .setNegativeButton(R.string.cancel, null)
                .create().show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);

        keepScreenOn(true);
        downloadTask = new DownloadTask(this, downloadMapUri, mapDirectoryFile.createFile("application/binary", mapName).getUri());
        downloadTask.start();

        Log.d(TAG, "Started map download of '" + mapName + "'");
    }

    private void downloadEnded(final boolean success, final boolean canceled) {
        progressBar.setVisibility(View.GONE);
        keepScreenOn(false);
        final Uri targetMapUri = downloadTask.targetMapUri;
        downloadTask = null;
        if (canceled) {
            final DocumentFile documentFile = FileUtil.getDocumentFileFromTreeUri(this, targetMapUri);
            if (documentFile != null) {
                documentFile.delete();
            }
            onBackPressed();
            return;
        }
        if (success) {
            Toast.makeText(this, R.string.download_success, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.download_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void changeMapDirectory(final Uri uri, final int requestCode, final Intent resultData) {
        super.changeMapDirectory(uri, requestCode, resultData);
        if (requestCode == REQUEST_MAP_DIRECTORY_FOR_DOWNLOAD) {
            startMapDownload();
        }
    }

    @Override
    protected void onOnlineMapConsentChanged(final boolean consent) {
        // nothing to do
    }

    private static class DownloadTask extends Thread {
        private final WeakReference<DownloadMapsActivity> ref;
        private final Uri downloadMapUri;
        private final Uri targetMapUri;
        private int contentLength = -1;
        private boolean success = false;
        private boolean canceled = false;

        public DownloadTask(final DownloadMapsActivity activity, final Uri downloadMapUri, final Uri targetMapUri) {
            ref = new WeakReference<>(activity);
            this.downloadMapUri = downloadMapUri;
            this.targetMapUri = targetMapUri;
        }

        protected void publishProgress(final int progress) {
            final DownloadMapsActivity activity = ref.get();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    activity.progressBar.setProgress(progress);
                    if (contentLength > 0 && activity.progressBar.isIndeterminate()) {
                        // we have a content length, so switch to determinate progress
                        activity.progressBar.setIndeterminate(false);
                        activity.progressBar.setMax(contentLength);
                    }
                });
            }
        }

        protected void end() {
            final DownloadMapsActivity activity = ref.get();
            if (activity != null) {
                activity.runOnUiThread(() -> activity.downloadEnded(success, canceled));
            }
        }

        @Override
        public void run() {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            int bytesWritten = 0;
            try {
                final URL sUrl = new URL(downloadMapUri.toString());
                connection = (HttpURLConnection) sUrl.openConnection();
                connection.connect();
                contentLength = connection.getContentLength();

                input = connection.getInputStream();
                output = ref.get().getContentResolver().openOutputStream(targetMapUri);

                final byte[] data = new byte[4096];
                int count;
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
                success = true;
            } catch (final Exception e) {
                Log.e(TAG, "Download failed", e);
            } finally {
                try {
                    if (output != null) {
                        output.close();
                    }
                    if (input != null) {
                        input.close();
                    }
                } catch (final IOException ignored) {
                }

                if (connection != null) {
                    connection.disconnect();
                }
            }
            end();
        }

        public void cancelDownload() {
            canceled = true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (isDownloadInProgress()) {
            new AlertDialog.Builder(DownloadMapsActivity.this)
                .setIcon(R.drawable.ic_logo_color_24dp)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.cancel_download_question))
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    downloadTask.cancelDownload();
                })
                .setNegativeButton(R.string.cancel, null)
                .create().show();
        } else {
            super.onBackPressed();
        }
    }

}
