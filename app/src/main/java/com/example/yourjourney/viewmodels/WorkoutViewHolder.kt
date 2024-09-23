package com.example.yourjourney.viewmodels

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.yourjourney.R

/**
 * ViewHolder for displaying workout details in a RecyclerView.
 * @param itemView The view for each item in the RecyclerView.
 */
class WorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val exerciseNameTextView: TextView = itemView.findViewById(R.id.exerciseNameTextView)
    val repetitionsTextView: TextView = itemView.findViewById(R.id.repetitionsTextView)
    val isCompleteTextView: TextView = itemView.findViewById(R.id.isCompleteTextView)
}

