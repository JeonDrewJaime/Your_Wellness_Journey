package com.example.yourjourney.views.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.yourjourney.R
import com.example.yourjourney.adapters.ExerciseAdapter
import com.example.yourjourney.data.plan.Exercises
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Exercise : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var dayToday: TextView
    private lateinit var adapter: ExerciseAdapter
    private val exercisesList = mutableListOf<Exercises>()

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Exercise().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

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
        val view = inflater.inflate(R.layout.fragment_exercise, container, false)
        dayToday = view.findViewById(R.id.day)
        val exerciseList: ListView = view.findViewById(R.id.exerciseList)
        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation) // Ensure you have the correct ID
        // Initialize adapter without the markExerciseAsDone callback
        adapter = ExerciseAdapter(requireContext(), exercisesList, bottomNavigationView)
        exerciseList.adapter = adapter

        fetchExercises()

        return view
    }

    private fun fetchExercises() {
        val currentUser = auth.currentUser

        currentUser?.let {
            val userId = it.uid
            val userRef = database.getReference("users").child(userId)

            userRef.child("age").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(ageSnapshot: DataSnapshot) {
                    val age = ageSnapshot.getValue(Int::class.java) ?: 0

                    userRef.child("disease").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(diseaseSnapshot: DataSnapshot) {
                            val disease = diseaseSnapshot.getValue(String::class.java) ?: return
                            val restrictedExercisesByDiseaseAndAge = mapOf(
                                "Acute Lymphoblastic Leukemia" to mapOf(
                                    4..5 to listOf("Jumping Jacks", "Push-Ups"),
                                    2..6 to listOf("Running")
                                ),
                                "diseaseB" to mapOf(
                                    3..5 to listOf("Cycling")
                                )
                            )

                            userRef.child("stage").addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(stageSnapshot: DataSnapshot) {
                                    val stage = stageSnapshot.getValue(String::class.java)

                                    if (disease != null && stage != null) {
                                        val calendar = Calendar.getInstance()
                                        val dayString = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
                                        dayToday.text = dayString

                                        // Reference to today's exercises
                                        val exercisesRef = database.getReference("diseases").child(disease).child(stage).child("Exercise").child(dayString)

                                        exercisesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(exerciseSnapshot: DataSnapshot) {
                                                if (exerciseSnapshot.exists()) {
                                                    for (exercise in exerciseSnapshot.children) {
                                                        val name = exercise.key ?: continue
                                                        val img = exercise.child("image").getValue<String>() ?: continue

                                                        // Check if this exercise is restricted based on disease and age
                                                        val exerciseRestrictions = restrictedExercisesByDiseaseAndAge[disease]
                                                        val isRestricted = exerciseRestrictions?.any { (ageRange, exerciseNames) ->
                                                            age in ageRange && exerciseNames.any { exerciseName -> name.equals(exerciseName, ignoreCase = true) }
                                                        } ?: false

                                                        // Check if the exercise has been marked as done
                                                        val doneRef = userRef.child("exercises")
                                                            .child(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time))
                                                            .child(name)
                                                            .child("done")

                                                        doneRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                                            override fun onDataChange(doneSnapshot: DataSnapshot) {
                                                                // Determine if the exercise is marked as done
                                                                val isDone = doneSnapshot.exists() && doneSnapshot.getValue(Boolean::class.java) == true

                                                                // Only add exercise if it's not restricted and not done
                                                                if (!isRestricted) {
                                                                    // Create Exercises object with done status
                                                                    val exerciseItem = Exercises(name, img, isDone)
                                                                    exercisesList.add(exerciseItem)
                                                                    adapter.notifyDataSetChanged()
                                                                }
                                                            }

                                                            override fun onCancelled(error: DatabaseError) {
                                                                // Handle errors fetching done status
                                                            }
                                                        })
                                                    }
                                                } else {
                                                    // Handle case where no exercises are found
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                // Handle errors fetching exercises
                                            }
                                        })
                                    } else {
                                        // Handle case where disease or stage is not found
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    // Handle errors fetching stage data
                                }
                            })
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle errors fetching disease data
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle errors fetching age data
                }
            })
        }
    }

    // Removed the markExerciseAsDone method
}
