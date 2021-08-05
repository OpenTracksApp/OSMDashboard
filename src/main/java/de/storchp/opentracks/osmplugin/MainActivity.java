package de.storchp.opentracks.osmplugin;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;

import de.storchp.opentracks.osmplugin.databinding.ActivityMainBinding;
import de.storchp.opentracks.osmplugin.utils.ArrowMode;
import de.storchp.opentracks.osmplugin.utils.MapMode;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar.mapsToolbar);

        binding.usageInfo.setMovementMethod(LinkMovementMethod.getInstance());
        binding.osmInfo.setMovementMethod(LinkMovementMethod.getInstance());
        binding.offlineMaps.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        return super.onCreateOptionsMenu(menu, false);
    }

    @Override
    protected void changeMapMode(final MapMode mapMode) {
        // nothing to do
    }

    @Override
    protected void changeArrowMode(final ArrowMode arrowMode) {
        // nothing to do
    }

    @Override
    protected void onOnlineMapConsentChanged(final boolean consent) {
        // nothing to do
    }

}
