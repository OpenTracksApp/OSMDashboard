package de.storchp.opentracks.osmplugin.utils;

import android.net.Uri;

import androidx.annotation.NonNull;

public record FileItem(String name, Uri uri) {

    @NonNull
    @Override
    public String toString() {
        return name();
    }

}
