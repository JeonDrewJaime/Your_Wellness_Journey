package com.example.yourwellnessjourney

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class Home : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    private lateinit var exerciseProgress: TextView
    private lateinit var medicationProgress: TextView
    private lateinit var calendarView: CalendarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Access CalendarView and TextViews
        calendarView = view.findViewById(R.id.calendarView)
        exerciseProgress = view.findViewById(R.id.exerciseProgress)
        medicationProgress = view.findViewById(R.id.medicationProgress)

        // Set up CalendarView listener
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            updateProgressTexts(year, month, dayOfMonth)
        }
        return view
    }

    private fun updateProgressTexts(year: Int, month: Int, dayOfMonth: Int) {

        val selectedDate = String.format("%d-%d-%d", year, month + 1, dayOfMonth)
        val exerciseProgressText = "80%"
        val medicationProgressText = "50%"
        exerciseProgress.text = exerciseProgressText
        medicationProgress.text = medicationProgressText
    }
    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Home().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}