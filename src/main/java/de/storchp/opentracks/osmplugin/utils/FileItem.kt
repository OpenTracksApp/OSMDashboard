package de.storchp.opentracks.osmplugin.utils

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

data class FileItem(
    val name: String,
    val uri: Uri?,
    val file: File? = null,
    val documentFile: DocumentFile? = null
) {
    override fun toString() = this.name

}
