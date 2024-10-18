package de.storchp.opentracks.osmplugin.settings

import android.app.Activity
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import de.storchp.opentracks.osmplugin.R
import de.storchp.opentracks.osmplugin.databinding.ThemeItemBinding

class ThemeItemAdapter(
    private val context: Activity,
    var selectedUri: Uri?,
    private val onlineMapSelected: Boolean
) : ArrayAdapter<FileItem>(context, R.layout.map_item, mutableListOf()) {

    override fun areAllItemsEnabled(): Boolean {
        return false
    }

    override fun isEnabled(position: Int): Boolean {
        return !onlineMapSelected || position == 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var rowView = convertView
        // reuse views
        if (rowView == null) {
            val binding = ThemeItemBinding.inflate(context.layoutInflater, parent, false)
            rowView = binding.getRoot()
            rowView.tag = binding
        }

        // fill data
        val binding = rowView.tag as ThemeItemBinding
        getItem(position)?.let { item ->
            binding.name.text = item.name
            binding.name.setEnabled(isEnabled(position))
            binding.radiobutton.setChecked(if (position == 0) selectedUri == null else item.uri != null && item.uri == selectedUri)
            binding.radiobutton.setOnClickListener(
                onClickListener(
                    binding.radiobutton,
                    position
                )
            )
            binding.radiobutton.setEnabled(isEnabled(position))
        }

        return rowView
    }

    private fun onClickListener(
        radioButton: RadioButton,
        position: Int
    ) = View.OnClickListener {
        getItem(position)?.let { fileItem ->
            if (radioButton.isChecked) {
                selectedUri = fileItem.uri
            } else {
                if (fileItem.uri != null) { // offline theme
                    selectedUri = null
                }
            }
            notifyDataSetChanged()
        }
    }

}
