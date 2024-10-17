package de.storchp.opentracks.osmplugin.download

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.databinding.DownloadItemBinding

class DownloadMapItemAdapter(
    private val context: Activity,
    private val items: MutableList<DownloadMapItem>
) : ArrayAdapter<DownloadMapItem?>(context, R.layout.map_item, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var rowView = convertView
        // reuse views
        if (rowView == null) {
            val binding = DownloadItemBinding.inflate(context.layoutInflater, parent, false)
            rowView = binding.getRoot()
            rowView.tag = binding
        }

        // fill data
        val binding = rowView.tag as DownloadItemBinding
        val item = this.items[position]
        binding.name.text = item.name
        binding.date.text = item.date
        binding.size.text = item.size
        binding.mapIcon.setImageDrawable(
            ContextCompat.getDrawable(context, item.downloadItemType.iconResId)
        )

        return rowView
    }
}
