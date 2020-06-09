package de.storchp.opentracks.osmplugin.utils;

import android.net.Uri;

public class FileItem {
    private final String name;

    private final Uri uri;

    public FileItem(final String name, final Uri uri) {
        this.name = name;
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public Uri getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return getName();
    }

}
