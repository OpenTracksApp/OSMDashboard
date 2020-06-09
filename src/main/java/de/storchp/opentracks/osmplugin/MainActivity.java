package de.storchp.opentracks.osmplugin;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Toolbar toolbar = findViewById(R.id.maps_toolbar);
        setSupportActionBar(toolbar);

        final TextView info = findViewById(R.id.app_info);
        info.setPadding(50, 50, 50, 50);
        info.setMovementMethod(LinkMovementMethod.getInstance());
        info.setLinkTextColor(getResources().getColor(R.color.holo_orange_dark));
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        return super.onCreateOptionsMenu(menu, false);
    }

    @Override
    protected void onOnlineMapConsentChanged(boolean consent) {
        // nothing to do
    }

    @Override
    void recreateMap(final boolean menuNeedsUpdate) {
        // main activity doesn't have a map, just invalidate the menu
        if (menuNeedsUpdate) {
            invalidateOptionsMenu();
        }
    }

}
