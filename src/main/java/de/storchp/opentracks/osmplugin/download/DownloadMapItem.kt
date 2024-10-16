package de.storchp.opentracks.osmplugin.download

import android.net.Uri

data class DownloadMapItem(
    val downloadItemType: DownloadItemType,
    val name: String,
    val date: String?,
    val size: String,
    val uri: Uri,
) : Comparable<DownloadMapItem> {
    override fun toString() = this.name

    override fun compareTo(o: DownloadMapItem) =
        if (downloadItemType != o.downloadItemType) {
            downloadItemType.compareTo(o.downloadItemType)
        } else {
            name.compareTo(o.name)
        }

}
