package de.storchp.opentracks.osmplugin.utils

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
    private val items: List<FileItem>,
    var selectedUri: Uri?,
    private val onlineMapSelected: Boolean
) : ArrayAdapter<FileItem>(context, R.layout.map_item, items) {

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
        val item = this.items[position]
        binding.name.text = item.name
        binding.name.setEnabled(isEnabled(position))
        binding.radiobutton.setChecked(if (position == 0) selectedUri == null else item.uri != null && item.uri == selectedUri)
        binding.radiobutton.setOnClickListener(
            onStateChangedListener(
                binding.radiobutton,
                position
            )
        )
        binding.radiobutton.setEnabled(isEnabled(position))

        return rowView
    }

    private fun onStateChangedListener(
        radioButton: RadioButton,
        position: Int
    ): View.OnClickListener {
        return View.OnClickListener { v: View? ->
            val fileItem = items[position]
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
