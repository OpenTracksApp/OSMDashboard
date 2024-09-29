package de.storchp.opentracks.osmplugin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import de.storchp.opentracks.osmplugin.databinding.ActivityDownloadMapSelectionBinding;
import de.storchp.opentracks.osmplugin.utils.PreferencesUtils;

public class DownloadMapSelectionActivity extends BaseActivity {

    private static final String TAG = DownloadMapSelectionActivity.class.getSimpleName();

    private static final String MAPS_V_5 = "https://ftp-stud.hs-esslingen.de/pub/Mirrors/download.mapsforge.org/maps/v5/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        var binding = ActivityDownloadMapSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.mapsToolbar.setTitle(R.string.choose_map_to_download);
        setSupportActionBar(binding.toolbar.mapsToolbar);

        new Thread(() -> {
            try {
                Document doc = Jsoup.connect(MAPS_V_5 + "europe").get();
                Elements rows = doc.select("tr");
                for (Element element : rows) {
                    Elements cells = element.select("td");
                    if (cells.size() >= 4) {
                        String type = cells.get(0).select("img").get(0).attr("alt");
                        Element link = cells.get(1).select("a").get(0);
                        String href = link.attr("href");
                        String linkText = link.text();
                        String date = cells.get(2).text();
                        String size = cells.get(3).text();
                        Log.d(TAG, "Link: " + linkText + "./" + href + " " + date + " " + size);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        // TODO: build listview instead of webview

        WebView webView = findViewById(R.id.webview);
        var webClient = new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                var uri = request.getUrl();
                Log.d(TAG, "URL: " + uri);
                if (!uri.toString().startsWith(MAPS_V_5)) {
                    return true; // don't load URLs outside the base URL
                }
                var lastPathSegment = uri.getLastPathSegment();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

}
