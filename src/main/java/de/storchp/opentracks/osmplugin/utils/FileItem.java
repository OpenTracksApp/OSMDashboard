package de.storchp.opentracks.osmplugin.utils;

import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

public class FileItem {
    private final DocumentFile file;

    public FileItem(final DocumentFile file) {
        this.file = file;
    }

    public DocumentFile getFile() {
        return file;
    }

    public String getName() {
        return file.getName();
    }

    public Uri getUri() {
        return file.getUri();
    }

    @Override
    public String toString() {
        return getName();
    }

}
