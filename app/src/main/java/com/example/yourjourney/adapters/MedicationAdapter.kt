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
import com.example.yourjourney.data.plan.Medicines

class MedicationAdapter(
    private val context: Context,
    private var medications: List<Medicines>,
    private val markAsDoneCallback: (String) -> Unit // Callback to handle marking done
) : ArrayAdapter<Medicines>(context, 0, medications) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val medication = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_medication, parent, false)

        val nameTextView: TextView = view.findViewById(R.id.medicationName)
        val doseTextView: TextView = view.findViewById(R.id.medicationDosage)
        val medicationImageView: ImageView = view.findViewById(R.id.medicationImage)
        val medicationButton: Button = view.findViewById(R.id.medicationButton)

        // Set medication name and dosage
        nameTextView.text = medication?.name
        doseTextView.text = "Dose: ${medication?.dosage}"

        // Load image using Glide
        Glide.with(context)
            .load(medication?.imageUrl) // Load the image URL
            .into(medicationImageView)

        // Check if the medication is done
        if (medication?.isDone == true) {
            // Medication is done, update button text and disable it
            medicationButton.text = "Done"
            medicationButton.isEnabled = false
            medicationButton.setBackgroundColor(context.getColor(R.color.gray)) // Optional: change button color to indicate done
        } else {
            // Medication is not done, set the button to be clickable
            medicationButton.text = "Done"
            medicationButton.isEnabled = true
            medicationButton.setBackgroundColor(context.getColor(R.color.green)) // Set default color for active button

            // Set the button click listener
            medicationButton.setOnClickListener {
                if (medication != null) {
                    medication.name?.let { name ->
                        // Call the callback to handle marking as done
                        markAsDoneCallback(name)

                        // Immediately disable the button after clicking
                        medicationButton.text = "Done"
                        medicationButton.isEnabled = false
                        medicationButton.setBackgroundColor(context.getColor(R.color.gray)) // Optional: change button color to indicate done
                    }
                }
            }
        }

        return view
    }
}
