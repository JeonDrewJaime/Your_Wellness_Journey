package com.example.yourjourney.views.activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.yourjourney.R
import com.example.yourjourney.adapters.CustomSpinnerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupSpinner(R.id.genderSpinner, resources.getStringArray(R.array.genders))
        setupSpinner(R.id.monthSpinner, resources.getStringArray(R.array.months))
        setupSpinner(R.id.daySpinner, resources.getStringArray(R.array.days))
        setupSpinner(R.id.yearSpinner, resources.getStringArray(R.array.years))
        setupSpinner(R.id.diseaseSpinner, resources.getStringArray(R.array.diseases))

        findViewById<Button>(R.id.signUpButton).setOnClickListener {
            val email = findViewById<EditText>(R.id.emailEditText).text.toString()
            val password = findViewById<EditText>(R.id.passwordEditText).text.toString()
            val confirmPassword = findViewById<EditText>(R.id.confirmPasswordEditText).text.toString()

            if (password == confirmPassword) {
                createUser(email, password)
            } else {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // User is created, store additional data
                    val userId = auth.currentUser?.uid
                    val firstName = findViewById<EditText>(R.id.firstNameEditText).text.toString()
                    val middleName = findViewById<EditText>(R.id.middleNameEditText).text.toString()
                    val lastName = findViewById<EditText>(R.id.lastNameEditText).text.toString()
                    val gender = findViewById<Spinner>(R.id.genderSpinner).selectedItem.toString()
                    val birthMonth = findViewById<Spinner>(R.id.monthSpinner).selectedItem.toString()
                    val birthDay = findViewById<Spinner>(R.id.daySpinner).selectedItem.toString()
                    val birthYear = findViewById<Spinner>(R.id.yearSpinner).selectedItem.toString()
                    val disease = findViewById<Spinner>(R.id.diseaseSpinner).selectedItem.toString()

                    val user = User(firstName, middleName, lastName, gender,birthMonth,birthDay,birthYear,disease)
                    userId?.let {
                        database.child("users").child(it).setValue(user)
                            .addOnSuccessListener {

                                Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to register user: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setupSpinner(spinnerId: Int, data: Array<String>) {
        val spinner: Spinner = findViewById(spinnerId)
        val adapter = CustomSpinnerAdapter(
            this,
            R.layout.spinner_item,
            data,
            R.font.cronus
        )
        spinner.adapter = adapter
    }

    data class User(
        val firstName: String,
        val middleName: String,
        val lastName: String,
        val gender: String,
        val birthMonth: String,
        val birthDay: String,
        val birthYear: String,
        val disease: String,
    )
}
