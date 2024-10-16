package de.storchp.opentracks.osmplugin.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;

public class FileUtil {

    private static final String TAG = FileUtil.class.getSimpleName();

    public static DocumentFile getDocumentFileFromTreeUri(Context context, Uri uri) {
        try {
            return DocumentFile.fromTreeUri(context, uri);
        } catch (Exception e) {
            Log.w(TAG, "Error getting DocumentFile from Uri: " + uri);
        }
        return null;
    }

    public static String getFilenameFromUri(Context context, Uri uri) {
        if ("file".equals(uri.getScheme())) {
            var file = new File(uri.getPath());
            if (file.exists()) {
                return file.getName();
            }
        } else {
            var documentFile = getDocumentFileFromTreeUri(context, uri);
            if (documentFile != null && documentFile.exists()) {
                return documentFile.getName();
            }
        }
        return null;
    }

}
