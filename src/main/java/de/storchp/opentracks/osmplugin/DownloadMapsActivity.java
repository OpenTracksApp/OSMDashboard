package de.storchp.opentracks.osmplugin;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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

import de.storchp.opentracks.osmplugin.maps.utils.PreferencesUtils;

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
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "URL: " + url);
                if (!url.startsWith(MAPS_V_5)) {
                    return true; // don't load URLs outside the base URL
                }
                final Uri uri = Uri.parse(url);
                final String lastPathSegment = uri.getLastPathSegment();
                if (lastPathSegment != null && lastPathSegment.endsWith(".map")) {
                    if (progressBar.getVisibility() == View.VISIBLE) {
                        Toast.makeText(DownloadMapsActivity.this, R.string.download_in_progress, Toast.LENGTH_LONG).show();
                        return true;
                    }
                    new AlertDialog.Builder(DownloadMapsActivity.this)
                        .setIcon(R.drawable.ic_logo_color_24dp)
                        .setTitle(R.string.app_name)
                        .setMessage(getString(R.string.download_map_question, lastPathSegment))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
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
        final DocumentFile mapDirectoryFile = getDocumentFileFromTreeUri(mapDirectoryUri);
        if (!mapDirectoryFile.canWrite()) {
            openDirectory(REQUEST_MAP_DIRECTORY_FOR_DOWNLOAD);
            return;
        }
        final String mapName = downloadMapUri.getLastPathSegment();
        final Uri targetMapUri = mapDirectoryFile.createFile("application/binary", mapName).getUri();
        new DownloadTask(this).execute(downloadMapUri, targetMapUri);
        Log.d(TAG, "Started map download of '" + mapName + "'");
    }

    @Override
    protected void changeMapDirectory(Uri uri, int requestCode) {
        super.changeMapDirectory(uri, requestCode);
        if (requestCode == REQUEST_MAP_DIRECTORY_FOR_DOWNLOAD) {
            startMapDownload();
        }
    }

    @Override
    protected void onOnlineMapConsentChanged(boolean consent) {
        // nothing to do
    }

    @Override
    void recreateMap(boolean menuNeedsUpdate) {
        if (menuNeedsUpdate) {
            invalidateOptionsMenu();
        }
    }

    static class DownloadTask extends AsyncTask<Uri, Integer, String> {
        private final WeakReference<DownloadMapsActivity> ref;

        public DownloadTask(final DownloadMapsActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(Uri... params) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            String result = "";
            try {
                final URL sUrl = new URL(params[0].toString());
                connection = (HttpURLConnection) sUrl.openConnection();
                connection.connect();

                // download the file
                input = connection.getInputStream();
                output = ref.get().getContentResolver().openOutputStream(params[1]);

                final byte[] data = new byte[4096];
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }

                    output.write(data, 0, count);
                }
                result = "OK";
            } catch (final Exception e) {
                Log.e(TAG, "Download failed", e);
                result = "FAILED";
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null) {
                    connection.disconnect();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(final String result) {
            final DownloadMapsActivity activity = ref.get();
            if (activity != null) {
                activity.progressBar.setVisibility(View.GONE);
                if ("OK".equals(result)) {
                    activity.invalidateOptionsMenu();
                }
            }
            Log.d(TAG, "Download finished: " + result);
        }

        @Override
        protected void onPreExecute() {
            final DownloadMapsActivity activity = ref.get();
            if (activity != null) {
                activity.progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onProgressUpdate(final Integer... values) {
        }

    }

}
