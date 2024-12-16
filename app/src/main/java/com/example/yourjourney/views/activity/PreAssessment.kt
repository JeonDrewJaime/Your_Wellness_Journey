package com.example.yourjourney.views.activity

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.yourjourney.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PreAssessment : AppCompatActivity() {

    private lateinit var questionsLayout: LinearLayout
    private lateinit var submitButton: Button
    private var questionsList = ArrayList<Question>()
    private lateinit var currentUserId: String
    private lateinit var disease: String
    private lateinit var stage: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pre_assessment)

        questionsLayout = findViewById(R.id.questionsLayout)
        submitButton = findViewById(R.id.submitButton)

        // Handle edge-to-edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Fetch the logged-in user ID
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUserId = currentUser.uid
            fetchUserDetails()
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }

        submitButton.setOnClickListener {

            collectAndStoreAnswers()
        }
    }
    private fun fetchUserDetails() {
        val userReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("users/$currentUserId")
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                disease = dataSnapshot.child("disease").getValue(String::class.java) ?: ""
                stage = dataSnapshot.child("stage").getValue(String::class.java) ?: ""
                Log.d("PreAssessment", "Disease: $disease, Stage: $stage")
                fetchQuestionsForUser(disease, stage)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@PreAssessment, "Error fetching user details", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchQuestionsForUser(disease: String, stage: String) {
        val questionsReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("diseases/$disease/$stage/Pre")
        questionsReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                questionsList.clear()
                for (snapshot in dataSnapshot.children) {
                    val questionText = snapshot.child("text").getValue(String::class.java) ?: ""
                    val optionsMap = snapshot.child("options").value as Map<String, String>? ?: emptyMap()
                    if (questionText.isNotEmpty()) {
                        questionsList.add(Question(questionText, optionsMap))
                    }
                }
                displayQuestions()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@PreAssessment, "Error fetching questions", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayQuestions() {
        questionsLayout.removeAllViews() // Clear existing views

        val customFont = ResourcesCompat.getFont(this, R.font.cronus)
        val textColor = ContextCompat.getColor(this, R.color.darker_color)

        questionsList.forEach { question ->
            val questionView = layoutInflater.inflate(R.layout.question_item, questionsLayout, false)
            val questionText = questionView.findViewById<TextView>(R.id.questionText)
            val optionsGroup = questionView.findViewById<RadioGroup>(R.id.optionsGroup)

            questionText.text = question.text

            // Dynamically add options
            question.options.forEach { option ->
                val radioButton = RadioButton(this).apply {
                    text = option.value
                    setTextColor(textColor)
                    typeface = customFont
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                }
                optionsGroup.addView(radioButton)
            }

            questionsLayout.addView(questionView)
        }
    }

    private fun collectAndStoreAnswers() {
        val answersMap = mutableMapOf<String, String>()
        var allAnswered = true  // Flag to track if all questions are answered

        questionsList.forEachIndexed { index, question ->
            val questionView = questionsLayout.getChildAt(index)
            val optionsGroup = questionView.findViewById<RadioGroup>(R.id.optionsGroup)
            val selectedOptionId = optionsGroup.checkedRadioButtonId

            if (selectedOptionId != -1) {
                val selectedRadioButton = questionView.findViewById<RadioButton>(selectedOptionId)
                answersMap[question.text] = selectedRadioButton.text.toString()
            } else {
                answersMap[question.text] = "No answer selected"
                allAnswered = false // Mark as not all questions are answered
            }
        }

        // Check if all questions are answered
        if (!allAnswered) {
            Toast.makeText(this, "Please answer all questions before submitting.", Toast.LENGTH_SHORT).show()
        } else {
            // Log the answers to debug
            Log.d("PreAssessment", "Collected answers: $answersMap")
            storeAnswers(answersMap)
        }
    }

    private fun storeAnswers(answers: Map<String, String>) {
        val currentDate = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val answersReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("users/$currentUserId/Pre/$currentDate/answers")

        Log.d("PreAssessment", "Storing answers at: users/$currentUserId/Pre/$currentDate/answers")

        answersReference.setValue(answers).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Store the date of completion to track if the assessment was taken
                val dateReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("users/$currentUserId/Pre/$currentDate")
                dateReference.setValue(true) // Mark this day as completed for Pre-Assessment

                Toast.makeText(this, "Answers stored successfully", Toast.LENGTH_SHORT).show()
                finish() // Close the activity
            } else {
                Toast.makeText(this, "Error storing answers", Toast.LENGTH_SHORT).show()
            }
        }
    }

    data class Question(
        val text: String,
        val options: Map<String, String>
    )
}
