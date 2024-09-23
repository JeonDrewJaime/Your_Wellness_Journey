package com.example.yourjourney.views.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import com.example.yourjourney.R
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class Home : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    private lateinit var exerciseProgress: TextView
    private lateinit var medicationProgress: TextView
    private lateinit var calendarView: CalendarView
    private lateinit var doExercisesButton: Button
    private lateinit var takeMedicineButton: Button
    private lateinit var takeQuizButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var dateToday: TextView

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
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val currentUser = auth.currentUser
        currentUser?.let {
            val uid = currentUser.uid
            loadUserProfile(uid)
        }


        calendarView = view.findViewById(R.id.calendarView)
        exerciseProgress = view.findViewById(R.id.exerciseProgress)
        medicationProgress = view.findViewById(R.id.medicationProgress)
        doExercisesButton = view.findViewById(R.id.exerciseButton)
        takeMedicineButton = view.findViewById(R.id.medicineButton)
        takeQuizButton = view.findViewById(R.id.quizButton)
        dateToday = view.findViewById(R.id.dateToday)

        val selectedDateInMillis = calendarView.date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDate = dateFormat.format(Date(selectedDateInMillis))
        dateToday.text = selectedDate

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            updateProgressTexts(year, month, dayOfMonth)
        }

        doExercisesButton.setOnClickListener {

            val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNavigationView?.selectedItemId = R.id.navigation_exercise
        }

        takeMedicineButton.setOnClickListener {
            val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNavigationView?.selectedItemId = R.id.navigation_medication
        }

        takeQuizButton.setOnClickListener {
            val intent = Intent(activity, Medication::class.java)
            startActivity(intent)
        }
        return view
    }

    private fun loadUserProfile(uid: String) {
        // Reference to the user's data in the database
        val userRef = database.child(uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val disease = dataSnapshot.child("disease").getValue(String::class.java)
                view?.findViewById<TextView>(R.id.disease)?.text = disease
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors
            }
        })
    }
    private fun updateProgressTexts(year: Int, month: Int, dayOfMonth: Int) {

        val selectedDate = String.format("%d-%d-%d", year, month + 1, dayOfMonth)
        val exerciseProgressText = "80%"
        val medicationProgressText = "50%"
        exerciseProgress.text = exerciseProgressText
        medicationProgress.text = medicationProgressText
        dateToday.text = selectedDate
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