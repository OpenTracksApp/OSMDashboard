package de.storchp.opentracks.osmplugin.utils;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;

public record FileItem(String name, File file, DocumentFile documentFile) {

    @NonNull
    @Override
    public String toString() {
        return name();
    }

    public Uri uri() {
        return file != null ? Uri.fromFile(file) : documentFile != null ? documentFile.getUri() : null;
    }
}
