package de.storchp.opentracks.osmplugin.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.lang.Exception

object FileUtil {
    private val TAG: String = FileUtil::class.java.getSimpleName()

    fun getDocumentFileFromTreeUri(context: Context, uri: Uri) =
        try {
            DocumentFile.fromTreeUri(context, uri)
        } catch (_: Exception) {
            Log.w(TAG, "Error getting DocumentFile from Uri: $uri")
            null
        }

    fun getFilenameFromUri(context: Context, uri: Uri): String? {
        if ("file" == uri.scheme) {
            val file = File(uri.path!!)
            if (file.exists()) {
                return file.getName()
            }
        } else {
            val documentFile = getDocumentFileFromTreeUri(context, uri)
            if (documentFile != null && documentFile.exists()) {
                return documentFile.name
            }
        }
        return null
    }

    fun createBinaryDocumentFile(context: Context, destinationDir: Uri, filename: String) =
        getDocumentFileFromTreeUri(context, destinationDir)
            ?.createFile("application/binary", filename)
            ?: throw RuntimeException("Unable to create file: $filename")

}
