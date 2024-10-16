package de.storchp.opentracks.osmplugin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.View;

import androidx.annotation.NonNull;

import de.storchp.opentracks.osmplugin.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        de.storchp.opentracks.osmplugin.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar.mapsToolbar);

        binding.usageInfo.setMovementMethod(LinkMovementMethod.getInstance());
        binding.osmInfo.setMovementMethod(LinkMovementMethod.getInstance());
        binding.offlineMaps.setMovementMethod(LinkMovementMethod.getInstance());
        binding.versionInfo.setText(Html.fromHtml(getString(R.string.version_info, BuildConfig.BUILD_TYPE, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), Html.FROM_HTML_MODE_COMPACT));

        if (BuildConfig.offline) {
            binding.offlineVersionInfo.setVisibility(View.VISIBLE);
            binding.offlineMapInfo.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu, false);
        return true;
    }

}
