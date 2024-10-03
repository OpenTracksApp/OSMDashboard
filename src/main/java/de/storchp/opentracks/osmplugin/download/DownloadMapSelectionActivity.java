package de.storchp.opentracks.osmplugin.download;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

import de.storchp.opentracks.osmplugin.BaseActivity;
import de.storchp.opentracks.osmplugin.R;
import de.storchp.opentracks.osmplugin.databinding.ActivityDownloadMapSelectionBinding;

public class DownloadMapSelectionActivity extends BaseActivity {

    public static final String MAPS_V_5 = "https://ftp-stud.hs-esslingen.de/pub/Mirrors/download.mapsforge.org/maps/v5/";

    private DownloadMapItemAdapter adapter;
    private Uri pageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        var binding = ActivityDownloadMapSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pageUri = Uri.parse(MAPS_V_5);
        var intent = getIntent();
        if (intent != null) {
            var mapDownloadUri = intent.getData();
            if (mapDownloadUri != null) {
                pageUri = mapDownloadUri;
            }
        }
        binding.downloadProviderInfo.setText(getString(R.string.download_page_info, pageUri));

        binding.toolbar.mapsToolbar.setTitle(R.string.choose_map_to_download);
        setSupportActionBar(binding.toolbar.mapsToolbar);

        adapter = new DownloadMapItemAdapter(this, new ArrayList<>());
        binding.mapDownloadList.setAdapter(adapter);
        binding.mapDownloadList.setOnItemClickListener((listview, view, position, id) -> {
            var item = adapter.getItem(position);
            if (item != null) {
                if (item.downloadItemType() == DownloadItemType.MAP) {
                    startActivity(new Intent(Intent.ACTION_DEFAULT, item.uri(), DownloadMapSelectionActivity.this, DownloadActivity.class));
                } else if (item.downloadItemType() == DownloadItemType.SUBDIR) {
                    startActivity(new Intent(Intent.ACTION_DEFAULT, item.uri(), DownloadMapSelectionActivity.this, DownloadMapSelectionActivity.class));
                }
            }
        });

        new Thread(() -> {
            try {
                var doc = Jsoup.connect(pageUri.toString()).get();
                var rows = doc.select("tr");
                var items = new ArrayList<DownloadMapItem>();
                for (Element element : rows) {
                    Elements cells = element.select("td");
                    if (cells.size() >= 4) {
                        String alt = cells.get(0).select("img").get(0).attr("alt");
                        Element link = cells.get(1).select("a").get(0);
                        String href = link.attr("href");
                        String linkText = link.text();
                        String date = cells.get(2).text();
                        String size = cells.get(3).text();
                        DownloadItemType.ofAlt(alt)
                                .ifPresent(type -> items.add(new DownloadMapItem(type, linkText, date, size, pageUri.buildUpon().appendEncodedPath(href).build())));
                    }
                }
                items.sort(Comparator.naturalOrder());
                runOnUiThread(() -> adapter.addAll(items));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

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
