package de.storchp.opentracks.osmplugin.utils;

import android.net.Uri;

import androidx.annotation.NonNull;

public class FileItem {
    private final String name;

    private final Uri uri;

    public FileItem(String name, Uri uri) {
        this.name = name;
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public Uri getUri() {
        return uri;
    }

    @NonNull
    @Override
    public String toString() {
        return getName();
    }

}
