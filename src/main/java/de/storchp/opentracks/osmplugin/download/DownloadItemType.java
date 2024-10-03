package de.storchp.opentracks.osmplugin.download;

import java.util.Arrays;
import java.util.Optional;

import de.storchp.opentracks.osmplugin.R;

public enum DownloadItemType {
    SUBDIR(R.drawable.baseline_folder_24, "[DIR]"), MAP(R.drawable.baseline_map_24, "[   ]");

    private final int iconResId;
    private final String alt;

    DownloadItemType(int iconResId, String alt) {
        this.iconResId = iconResId;
        this.alt = alt;
    }

    public static Optional<DownloadItemType> ofAlt(String alt) {
        return Arrays.stream(DownloadItemType.values()).filter(downloadItemType -> downloadItemType.alt.equals(alt)).findFirst();
    }

    public int getIconResId() {
        return iconResId;
    }
}
