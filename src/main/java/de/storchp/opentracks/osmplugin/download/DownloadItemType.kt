package de.storchp.opentracks.osmplugin.download

import de.storchp.opentracks.osmplugin.R
import java.util.Arrays
import java.util.Optional

enum class DownloadItemType(iconResId: Int, alt: String) {
    SUBDIR(R.drawable.baseline_folder_24, "[DIR]"), MAP(R.drawable.baseline_map_24, "[   ]");

    private val iconResId: Int
    private val alt: String

    init {
        this.iconResId = iconResId
        this.alt = alt
    }

    fun getIconResId(): Int {
        return iconResId
    }

    companion object {
        fun ofAlt(alt: String?): Optional<DownloadItemType?> {
            return Arrays.stream<DownloadItemType?>(DownloadItemType.entries.toTypedArray())
                .filter { downloadItemType: DownloadItemType? -> downloadItemType!!.alt == alt }
                .findFirst()
        }
    }
}
