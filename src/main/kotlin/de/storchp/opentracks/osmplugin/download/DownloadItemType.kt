package de.storchp.opentracks.osmplugin.download

import de.storchp.opentracks.osmplugin.R

enum class DownloadItemType(val iconResId: Int, val alt: String) {
    SUBDIR(R.drawable.baseline_folder_24, "[DIR]"),
    MAP(R.drawable.baseline_map_24, "[   ]");
}

fun String.toDownloadItemType() =
    DownloadItemType.entries.firstOrNull { it.alt == this }
