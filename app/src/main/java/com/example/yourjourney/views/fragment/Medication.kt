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
import com.example.yourjourney.adapters.MedicationAdapter
import com.example.yourjourney.data.plan.Medicines
import com.example.yourjourney.util.restrictedMedicationsByDiseaseAndAge
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Medication : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var dayToday: TextView
    private lateinit var adapter: MedicationAdapter
    private val medicationsList = mutableListOf<Medicines>()

    // Define argument keys
    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Medication().apply {
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
        val view = inflater.inflate(R.layout.fragment_medication, container, false)
        dayToday = view.findViewById(R.id.day)
        val medicationList: ListView = view.findViewById(R.id.medicationList)
        adapter = MedicationAdapter(requireContext(), medicationsList) { medicationName ->
            markMedicationAsDone(medicationName)
        }
        medicationList.adapter = adapter


        fetchMedications()

        return view
    }

    private fun fetchMedications() {
        val currentUser = auth.currentUser

        currentUser?.let {
            val userId = it.uid
            val userRef = database.getReference("users").child(userId)

            // Fetch user's age
            userRef.child("age").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(ageSnapshot: DataSnapshot) {
                    val age = ageSnapshot.getValue(Int::class.java) ?: 0 // Default age if not found

                    // Fetch user's disease
                    userRef.child("disease").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(diseaseSnapshot: DataSnapshot) {
                            val disease = diseaseSnapshot.getValue(String::class.java) ?: return

                            // Define restricted medications based on both disease and age


                            userRef.child("stage").addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(stageSnapshot: DataSnapshot) {
                                    val stage = stageSnapshot.getValue(String::class.java)

                                    if (disease != null && stage != null) {
                                        // Get today's date
                                        val calendar = Calendar.getInstance()
                                        val dayString = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time) // Get day name
                                        dayToday.text = dayString // Set day in TextView

                                        // Reference to today's medications
                                        val medicationsRef = database.getReference("diseases").child(disease).child(stage).child("Medicine").child(dayString)

                                        medicationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(medSnapshot: DataSnapshot) {
                                                if (medSnapshot.exists()) {
                                                    for (med in medSnapshot.children) {
                                                        val name = med.key ?: continue
                                                        val img = med.child("image").getValue<String>() ?: continue
                                                        val dose = med.child("dosage").getValue<String>() ?: continue

                                                        // Check if this medication is restricted based on disease and age
                                                        val diseaseRestrictions = restrictedMedicationsByDiseaseAndAge[disease]
                                                        val isRestricted = diseaseRestrictions?.any { (ageRange, medNames) ->
                                                            age in ageRange && medNames.any { medName -> name.equals(medName, ignoreCase = true) }
                                                        } ?: false

                                                        if (!isRestricted) {
                                                            // Check if the medication has been marked as done
                                                            val doneRef = database.getReference("users").child(userId).child("medications")
                                                                .child(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)).child(name).child("done")

                                                            doneRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                                                override fun onDataChange(doneSnapshot: DataSnapshot) {

                                                                    val isDone = doneSnapshot.exists() && doneSnapshot.getValue(Boolean::class.java) == true
                                                                        val medication = Medicines(name, dose, img,isDone)
                                                                        medicationsList.add(medication)
                                                                        adapter.notifyDataSetChanged()

                                                                }

                                                                override fun onCancelled(error: DatabaseError) {
                                                                    // Handle errors
                                                                }
                                                            })
                                                        }
                                                    }
                                                } else {
                                                    // Handle case where no medications are found
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                // Handle errors fetching medications
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
                            // Handle errors fetching user data
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle errors fetching age data
                }
            })
        }
    }

    private fun markMedicationAsDone(medicationName: String) {
        val currentUser = auth.currentUser
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(calendar.time)

        currentUser?.let {
            val userId = it.uid

            // Fetch the user's disease and stage
            val userRef = database.getReference("users").child(userId)
            userRef.child("disease").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(diseaseSnapshot: DataSnapshot) {
                    val disease = diseaseSnapshot.getValue(String::class.java) ?: return

                    userRef.child("stage").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(stageSnapshot: DataSnapshot) {
                            val stage = stageSnapshot.getValue(String::class.java) ?: return

                            // Get today's day of the week
                            val dayString = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)

                            // Reference to the medication's score in the diseases collection
                            val scoreRef = database.getReference("diseases")
                                .child(disease)
                                .child(stage)
                                .child("Medicine")
                                .child(dayString)
                                .child(medicationName)
                                .child("score")

                            // Fetch the score for the medication
                            scoreRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(scoreSnapshot: DataSnapshot) {
                                    if (scoreSnapshot.exists()) {
                                        val medicationScore = scoreSnapshot.getValue(Int::class.java) ?: 0 // Default score if not found

                                        // Now mark the medication as done
                                        val doneRef = database.getReference("users")
                                            .child(userId)
                                            .child("medications")
                                            .child(currentDate)
                                            .child(medicationName)

                                        // Check if already marked as done
                                        doneRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                if (!snapshot.exists()) {
                                                    // Mark as done and set the score for the specific medication
                                                    val medicationData = mapOf(
                                                        "done" to true,
                                                        "score" to medicationScore // Add score only for the specific medication
                                                    )

                                                    // Update the medication as done
                                                    doneRef.setValue(medicationData).addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            // Now update the total score for the date
                                                            updateTotalScore(userId, currentDate, medicationScore)
                                                        } else {
                                                            Toast.makeText(requireContext(), "Failed to mark $medicationName as done.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } else {
                                                    Toast.makeText(requireContext(), "$medicationName is already marked as done for today.", Toast.LENGTH_SHORT).show()
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        })
                                    } else {
                                        Toast.makeText(requireContext(), "Score not found for $medicationName.", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(requireContext(), "Error fetching medication score: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(requireContext(), "Error fetching stage: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error fetching disease: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun updateTotalScore(userId: String, currentDate: String, medicationScore: Int) {
        val totalScoreRef = database.getReference("users")
            .child(userId)
            .child("medications")
            .child(currentDate)
            .child("totalScore")

        // Fetch the existing total score for the date
        totalScoreRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(totalScoreSnapshot: DataSnapshot) {
                val currentTotalScore = totalScoreSnapshot.getValue(Int::class.java) ?: 0
                val newTotalScore = currentTotalScore + medicationScore

                // Update the total score for the specific date
                totalScoreRef.setValue(newTotalScore).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Total score updated to $newTotalScore for $currentDate.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to update total score for $currentDate.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching total score: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


}
