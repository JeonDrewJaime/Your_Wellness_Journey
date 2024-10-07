package com.example.yourjourney.views.fragment

import android.os.Bundle
import android.view.LayoutInflater
import com.example.yourjourney.R
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class Diet : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    // Firebase instances
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
        val view = inflater.inflate(R.layout.fragment_diet, container, false)

        // Reference to the TextView for avoid details
        val avoidTextView: TextView = view.findViewById(R.id.avoid)  // Assuming you have a TextView with id 'avoid'
        val dietTextView: TextView = view.findViewById(R.id.diet)
        // Fetch the disease avoid details from Realtime Database
        fetchDiseaseAvoidDetails(avoidTextView, dietTextView)

        return view
    }

    // Function to fetch the disease avoid details from Realtime Database
    private fun fetchDiseaseAvoidDetails(avoidTextView: TextView, dietTextView: TextView) {
        val currentUser = auth.currentUser

        currentUser?.let {
            val userId = it.uid
            val userRef = database.getReference("users").child(userId)

            userRef.child("disease").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(diseaseSnapshot: DataSnapshot) {
                    val disease = diseaseSnapshot.getValue(String::class.java)

                    userRef.child("stage")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(stageSnapshot: DataSnapshot) {
                                val stage = stageSnapshot.getValue(String::class.java)

                                if (disease != null && stage != null) {
                                    // Set the user's disease in the diet TextView
                                    dietTextView.text = disease

                                    val diseaseRef =
                                        database.getReference("diseases").child(disease)
                                            .child(stage).child("Diet").child("Avoid")

                                    diseaseRef.addListenerForSingleValueEvent(object :
                                        ValueEventListener {
                                        override fun onDataChange(diseaseSnapshot: DataSnapshot) {
                                            if (diseaseSnapshot.exists()) {
                                                // Assume avoid details are stored as a list of strings
                                                val avoidList = diseaseSnapshot.children.map {
                                                    it.getValue(String::class.java)
                                                }
                                                val bulletPoints = avoidList.filterNotNull()
                                                    .joinToString("\n") { "• $it" }
                                                avoidTextView.text = bulletPoints
                                            } else {
                                                avoidTextView.text = "Avoid details not found"
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            avoidTextView.text = "Failed to retrieve avoid details"
                                        }
                                    })
                                } else {
                                    avoidTextView.text = "Disease or stage not found"
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                avoidTextView.text = "Failed to retrieve stage data"
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    avoidTextView.text = "Failed to retrieve user data"
                }
            })
        }
    }
    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Diet().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}