package com.example.yourjourney.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

class CustomSpinnerAdapter(
    context: Context,
    private val layoutResource: Int,
    private val items: Array<String>,
    private val fontResource: Int
) : ArrayAdapter<String>(context, layoutResource, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        setCustomFont(view)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        setCustomFont(view)
        return view
    }

    private fun setCustomFont(view: View) {
        val textView = view as TextView
        val typeface = ResourcesCompat.getFont(context, fontResource)
        textView.typeface = typeface
    }
}
