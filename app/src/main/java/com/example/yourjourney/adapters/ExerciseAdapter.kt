package com.example.yourjourney.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.yourjourney.R
import com.example.yourjourney.data.plan.Exercises
import com.google.android.material.bottomnavigation.BottomNavigationView

class ExerciseAdapter(
    private val context: Context,
    private var exercises: List<Exercises>,
    private val bottomNavigationView: BottomNavigationView
) : ArrayAdapter<Exercises>(context, 0, exercises) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val exercise = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_exercises, parent, false)

        val nameTextView: TextView = view.findViewById(R.id.exerciseName)
        val exerciseImageView: ImageView = view.findViewById(R.id.exerciseImage)
        val performButton: Button = view.findViewById(R.id.performButton) // Button to perform exercise

        // Set exercise name
        nameTextView.text = exercise?.name

        // Load image using Glide
        Glide.with(context)
            .load(exercise?.imageUrl)
            .into(exerciseImageView)

        // Set the button text
        performButton.text = "Perform"

        // Check if the exercise is marked as done
        if (exercise?.isDone == true) {
            // Disable the button and change its background color
            performButton.isEnabled = false
            performButton.setBackgroundColor(context.getColor(R.color.gray)) // Change to your desired color
        } else {
            // Enable the button and reset the background color (to the default)
            performButton.isEnabled = true
            performButton.setBackgroundColor(context.getColor(R.color.default_color)) // Change to your default button color
        }

        // Set the button click listener to navigate to the Workout Fragment
        performButton.setOnClickListener {
            // Navigate to the Workout Fragment when the button is pressed
            bottomNavigationView.selectedItemId = R.id.navigation_workout // Use the correct ID for the Workout Fragment
        }

        return view
    }
}
