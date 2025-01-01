package de.storchp.opentracks.osmplugin.settings

import android.app.Activity
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import de.storchp.opentracks.osmplugin.BuildConfig
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.databinding.MapItemBinding

class MapItemAdapter(
    private val context: Activity,
    private val items: List<FileItem>,
    private var selected: Set<Uri>
) : ArrayAdapter<FileItem>(context, R.layout.map_item, items.toMutableList()) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var rowView = convertView
        // reuse views
        if (rowView == null) {
            val binding = MapItemBinding.inflate(context.layoutInflater, parent, false)
            rowView = binding.getRoot()
            rowView.tag = binding
        }

        // fill data
        val binding = rowView.tag as MapItemBinding
        val item = getItem(position)
        binding.name.text = item?.name
        binding.checkbox.setChecked(
            @Suppress("KotlinConstantConditions")
            if (position == 0 && !BuildConfig.offline) selected.isEmpty() else selected.contains(
                item?.uri
            )
        )
        binding.checkbox.setOnClickListener(onStateChangedListener(binding.checkbox, position))

        return rowView
    }

    private fun onStateChangedListener(checkBox: CheckBox, position: Int): View.OnClickListener {
        return View.OnClickListener {
            getItem(position)?.let { fileItem ->
                if (checkBox.isChecked) {
                    selected = if (fileItem.uri == null) { // online map
                        setOf()
                    } else {
                        selected + fileItem.uri
                    }
                } else if (fileItem.uri != null) { // offline map
                    selected = selected - fileItem.uri
                }
                notifyDataSetChanged()
            }
        }
    }

    fun getSelectedUris() =
        selected.filter { uri ->
            items.any { item -> uri == item.uri }
        }.toSet()
}
