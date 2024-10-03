package de.storchp.opentracks.osmplugin.download;

import android.net.Uri;

import androidx.annotation.NonNull;

public record DownloadMapItem(DownloadItemType downloadItemType, String name, String date,
                              String size, Uri uri) implements Comparable<DownloadMapItem> {

    @NonNull
    @Override
    public String toString() {
        return name();
    }

    @Override
    public int compareTo(DownloadMapItem o) {
        if (downloadItemType != o.downloadItemType) {
            return downloadItemType.compareTo(o.downloadItemType);
        }
        return name.compareTo(o.name);
    }
}
