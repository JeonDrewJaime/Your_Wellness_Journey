package com.example.yourjourney.views.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import com.example.yourjourney.R
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
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

        // Set the current date and load progress for today
        val calendar = Calendar.getInstance()
        updateProgressTexts(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            auth,
            database
        )

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            updateProgressTexts(year, month, dayOfMonth, auth, database)
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
        val userRef = database.child(uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val disease = dataSnapshot.child("disease").getValue(String::class.java)
                view?.findViewById<TextView>(R.id.disease)?.text = disease
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Error loading user profile: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateProgressTexts(year: Int, month: Int, dayOfMonth: Int, auth: FirebaseAuth, database: DatabaseReference) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDate = dateFormat.format(calendar.time)
        dateToday.text = selectedDate

        val currentUser = auth.currentUser
        currentUser?.uid?.let { uid ->
            val medicationRef = database.child(uid)
                .child("medications").child(selectedDate).child("totalScore")
            val exerciseRef = database.child(uid)
                .child("exercises").child(selectedDate).child("totalScore")

            // Fetching medication total score
            medicationRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val medicationTotalScore = if (dataSnapshot.exists()) {
                        dataSnapshot.getValue(Int::class.java) ?: 0
                    } else {
                        0
                    }
                    medicationProgress.text = "$medicationTotalScore%"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    medicationProgress.text = "Error"
                    Toast.makeText(context, "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })

            // Fetching exercise total score
            exerciseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val exerciseTotalScore = if (dataSnapshot.exists()) {
                        dataSnapshot.getValue(Int::class.java) ?: 0
                    } else {
                        0
                    }
                    exerciseProgress.text = "$exerciseTotalScore%"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    exerciseProgress.text = "Error"
                    Toast.makeText(context, "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

}
