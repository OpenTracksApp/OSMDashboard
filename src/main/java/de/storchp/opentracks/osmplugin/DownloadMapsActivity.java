package de.storchp.opentracks.osmplugin;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.snackbar.Snackbar;

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
                    if (progressBar.getVisibility() == View.VISIBLE) {
                        Toast.makeText(DownloadMapsActivity.this.getApplicationContext(), R.string.download_in_progress, Toast.LENGTH_LONG).show();
                        return true;
                    }
                    new AlertDialog.Builder(DownloadMapsActivity.this)
                        .setIcon(R.drawable.ic_logo_color_24dp)
                        .setTitle(R.string.app_name)
                        .setMessage(getString(R.string.download_map_question, lastPathSegment))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                downloadMapUri = uri;
                                startMapDownload();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .create().show();
                    return true;
                }
                return false;
            }

        };
        webView.setWebViewClient(webClient);
        webView.loadUrl(MAPS_V_5);
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
        final Uri targetMapUri = mapDirectoryFile.createFile("application/binary", mapName).getUri();

        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);

        new Thread(new DownloadTask(this, downloadMapUri, targetMapUri)).start();

        Log.d(TAG, "Started map download of '" + mapName + "'");
    }

    private void downloadEnded(final boolean success) {
        progressBar.setVisibility(View.GONE);
        if (success) {
            Toast.makeText(this, R.string.download_success, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.download_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void changeMapDirectory(final Uri uri, final int requestCode) {
        super.changeMapDirectory(uri, requestCode);
        if (requestCode == REQUEST_MAP_DIRECTORY_FOR_DOWNLOAD) {
            startMapDownload();
        }
    }

    @Override
    protected void onOnlineMapConsentChanged(final boolean consent) {
        // nothing to do
    }

    private static class DownloadTask implements Runnable {
        private final WeakReference<DownloadMapsActivity> ref;
        private final Uri downloadMapUri;
        private final Uri targetMapUri;
        private int contentLength = -1;
        private boolean success = false;

        public DownloadTask(final DownloadMapsActivity activity, final Uri downloadMapUri, final Uri targetMapUri) {
            ref = new WeakReference<>(activity);
            this.downloadMapUri = downloadMapUri;
            this.targetMapUri = targetMapUri;
        }

        protected void publishProgress(final int progress) {
            final DownloadMapsActivity activity = ref.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.progressBar.setProgress(progress);
                        if (contentLength > 0 && activity.progressBar.isIndeterminate()) {
                            // we have a content length, so switch to determinate progress
                            activity.progressBar.setIndeterminate(false);
                            activity.progressBar.setMax(contentLength);
                        }
                    }
                });
            }
        }

        protected void end() {
            final DownloadMapsActivity activity = ref.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.downloadEnded(success);
                    }
                });
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
    }

}
