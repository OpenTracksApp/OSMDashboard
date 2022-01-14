package de.storchp.opentracks.osmplugin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import de.storchp.opentracks.osmplugin.databinding.ActivityDownloadMapSelectionBinding;
import de.storchp.opentracks.osmplugin.utils.ArrowMode;
import de.storchp.opentracks.osmplugin.utils.MapMode;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

public class DownloadMapSelectionActivity extends BaseActivity {

    private static final String TAG = DownloadMapSelectionActivity.class.getSimpleName();

    private static final String MAPS_V_5 = "https://ftp-stud.hs-esslingen.de/pub/Mirrors/download.mapsforge.org/maps/v5/";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final var binding = ActivityDownloadMapSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.mapsToolbar.setTitle(R.string.choose_map_to_download);
        setSupportActionBar(binding.toolbar.mapsToolbar);

        final WebView webView = findViewById(R.id.webview);
        final var webClient = new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final WebResourceRequest request) {
                final var uri = request.getUrl();
                Log.d(TAG, "URL: " + uri);
                if (!uri.toString().startsWith(MAPS_V_5)) {
                    return true; // don't load URLs outside the base URL
                }
                final var lastPathSegment = uri.getLastPathSegment();
                if (lastPathSegment != null && lastPathSegment.endsWith(".map")) {
                    startActivity(new Intent(Intent.ACTION_DEFAULT, uri, DownloadMapSelectionActivity.this, DownloadActivity.class));
                    return true;
                }
                PreferencesUtils.setLastDownloadUrl(uri.toString());
                return false;
            }

        };
        webView.setWebViewClient(webClient);
        webView.loadUrl(PreferencesUtils.getLastDownloadUrl(MAPS_V_5));
    }

    @Override
    protected void onOnlineMapConsentChanged(final boolean consent) {
        // nothing to do
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
    protected void changeMapMode(final MapMode mapMode) {
        // nothing to do
    }

    @Override
    protected void changeArrowMode(final ArrowMode arrowMode) {
        // nothing to do
    }

}
