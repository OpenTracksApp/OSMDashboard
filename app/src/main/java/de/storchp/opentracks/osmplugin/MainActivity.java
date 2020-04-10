package de.storchp.opentracks.osmplugin;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.maps_toolbar);
        setSupportActionBar(toolbar);

        TextView info = findViewById(R.id.app_info);
        info.setPadding(50, 50, 50, 50);
        info.setMovementMethod(LinkMovementMethod.getInstance());
        info.setLinkTextColor(getResources().getColor(R.color.colorAccent));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu, false);
    }

    @Override
    void recreateMap(boolean menuNeedsUpdate) {
        // main activity doesn't have a map, just invalidate the menu
        if (menuNeedsUpdate) {
            invalidateOptionsMenu();
        }
    }

}
