package de.storchp.opentracks.osmplugin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class DownloadMapsActivity extends AppCompatActivity {

    private static final String TAG = DownloadMapsActivity.class.getSimpleName();
    public static final String MAPS_V_5 = "https://ftp-stud.hs-esslingen.de/pub/Mirrors/download.mapsforge.org/maps/v5/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_maps);

        final Toolbar toolbar = findViewById(R.id.maps_toolbar);
        toolbar.setTitle(R.string.choose_map_to_download);
        setSupportActionBar(toolbar);

        final WebView webView = findViewById(R.id.webview);
        final WebViewClient webClient = new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "URL: " + url);
                if (!url.startsWith(MAPS_V_5)) {
                    return true; // don't load URLs outside the base URL
                }
                final String lastPathSegment = Uri.parse(url).getLastPathSegment();
                if (lastPathSegment != null && lastPathSegment.endsWith(".map")) {
                    Log.d(TAG, "Download Map: " + lastPathSegment);
                    // TODO: download the map in the background (e.g. with a Service)
                    return true;
                }
                return false;
            }

        };
        webView.setWebViewClient(webClient);
        webView.loadUrl(MAPS_V_5);
    }

}
