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

    override fun compareTo(other: DownloadMapItem) =
        if (downloadItemType != other.downloadItemType) {
            downloadItemType.compareTo(other.downloadItemType)
        } else {
            name.compareTo(other.name)
        }

}
