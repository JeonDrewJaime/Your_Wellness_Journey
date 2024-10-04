package com.example.yourjourney.views.activity
import android.app.AlertDialog // Import the AlertDialog class
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.yourjourney.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

class PostAssessment : AppCompatActivity() {

    private lateinit var questionsLayout: LinearLayout // Layout to hold all questions
    private lateinit var submitButton: Button
    private var questionsList = ArrayList<Question>()
    private lateinit var currentUserId: String
    private lateinit var disease: String
    private lateinit var stage: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_post_assessment)

        // Initialize views
        questionsLayout = findViewById(R.id.questionsLayout)
        submitButton = findViewById(R.id.submitButton)

        // Enable edge-to-edge and apply padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Fetch the currently logged-in user ID
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUserId = currentUser.uid
            fetchUserDetails()
        } else {
            // Handle user not logged in (e.g., redirect to login screen)
        }

        submitButton.setOnClickListener {
            // Handle answers submission
            checkAnswers()
        }
    }

    private fun fetchUserDetails() {
        val userReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("users/$currentUserId")
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get user's disease and stage
                disease = dataSnapshot.child("disease").getValue(String::class.java) ?: ""
                stage = dataSnapshot.child("stage").getValue(String::class.java) ?: ""
                fetchQuestionsForUser(disease, stage)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun fetchQuestionsForUser(disease: String, stage: String) {
        val questionsReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("diseases/$disease/$stage/Post")
        questionsReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                questionsList.clear()
                for (snapshot in dataSnapshot.children) {
                    val questionText = snapshot.child("text").getValue(String::class.java) ?: ""
                    val options = snapshot.child("options").value as Map<String, String>
                    val correctAnswer = snapshot.child("correctAnswer").getValue(String::class.java) ?: ""
                    questionsList.add(Question(questionText, options, correctAnswer))
                }
                displayQuestions()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun displayQuestions() {
        questionsLayout.removeAllViews() // Clear any existing views

        // Load the custom font from the resources
        val customFont = ResourcesCompat.getFont(this, R.font.cronus) // Change R.font.cronus to your desired font
        questionsList.forEach { question ->
            val questionView = layoutInflater.inflate(R.layout.question_item, questionsLayout, false)
            val questionText = questionView.findViewById<TextView>(R.id.questionText)
            val optionsGroup = questionView.findViewById<RadioGroup>(R.id.optionsGroup)
            val textColor = ContextCompat.getColor(this, R.color.darker_color) // Use a color from resources

            // Set question text
            questionText.text = question.text

            // Dynamically add RadioButtons for each option
            question.options.forEach { option ->
                val radioButton = RadioButton(this).apply {
                    setTextColor(textColor) // Set custom text color
                    text = option.value
                    typeface = customFont // Set custom font for each radio button
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                }
                optionsGroup.addView(radioButton)
            }

            // Add the question view to the parent layout
            questionsLayout.addView(questionView)
        }
    }
    private fun checkAnswers() {
        var score = 0
        val totalQuestions = questionsList.size  // Get total number of questions

        questionsList.forEachIndexed { index, question ->
            val questionView = questionsLayout.getChildAt(index) as View
            val optionsGroup = questionView.findViewById<RadioGroup>(R.id.optionsGroup)
            val selectedOptionId = optionsGroup.checkedRadioButtonId
            if (selectedOptionId != -1) {
                val selectedRadioButton = questionView.findViewById<RadioButton>(selectedOptionId)
                val selectedOption = selectedRadioButton.text.toString()
                if (question.correctAnswer == selectedOption) {
                    score++
                }
            }
        }

        // Store the score in the database
        storeScore(score, totalQuestions)  // Pass total number of questions
    }

    private fun storeScore(score: Int, totalQuestions: Int) {
        val currentDate = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val scoreReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("users/$currentUserId/Post/$currentDate/score")

        scoreReference.setValue(score).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Score stored successfully: $score/$totalQuestions", Toast.LENGTH_SHORT).show()
                // Show custom dialog with score out of total questions
                showScoreDialog(score, totalQuestions)
            } else {
                Toast.makeText(this, "Error storing score", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showScoreDialog(score: Int, totalQuestions: Int) {
        // Create a new Dialog
        val dialog = Dialog(this)

        // Inflate the custom layout
        val view: View = LayoutInflater.from(this).inflate(R.layout.score_dialog, null)

        // Initialize the views in the custom layout
        val dialogTitle: TextView = view.findViewById(R.id.dialogTitle)
        val dialogMessage: TextView = view.findViewById(R.id.dialogMessage)
        val closeButton: Button = view.findViewById(R.id.closeButton)

        // Set the dialog title and message
        dialogTitle.text = "Assessment Score"
        dialogMessage.text = "$score/$totalQuestions"  // Display score out of total questions

        // Set the custom layout to the dialog
        dialog.setContentView(view)

        // Set the close button click listener
        closeButton.setOnClickListener {
            val intent = Intent(this, Dashboard::class.java) // Replace with your target activity
            startActivity(intent)
            dialog.dismiss() // Dismiss the dialog
        }

        dialog.show()
    }
    data class Question(
        val text: String,
        val options: Map<String, String>,
        val correctAnswer: String
    )
}
