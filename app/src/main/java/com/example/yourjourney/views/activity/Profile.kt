package com.example.yourjourney.views.activity

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.yourjourney.R
class Profile : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        val currentUser = auth.currentUser
        currentUser?.let {
            val uid = currentUser.uid
            loadUserProfile(uid)
        }
    }

    private fun loadUserProfile(uid: String) {
        // Reference to the user's data in the database
        val userRef = database.child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val firstName = dataSnapshot.child("firstName").getValue(String::class.java)?.capitalize() ?: ""
                val middleName = dataSnapshot.child("middleName").getValue(String::class.java)
                val lastName = dataSnapshot.child("lastName").getValue(String::class.java)?.capitalize() ?: ""
                val middleInitial = middleName?.firstOrNull()?.uppercase()?.plus(".") ?: ""
                val email = dataSnapshot.child("email").getValue(String::class.java)
                val gender = dataSnapshot.child("gender").getValue(String::class.java)
                val birthDay = dataSnapshot.child("birthDay").getValue(String::class.java)
                val birthMonth = dataSnapshot.child("birthMonth").getValue(String::class.java)
                val birthYear = dataSnapshot.child("birthYear").getValue(String::class.java)
                val disease = dataSnapshot.child("disease").getValue(String::class.java)
                val fullName = "$firstName $middleInitial $lastName"
                val birthdate = "$birthMonth $birthDay, $birthYear"
                findViewById<TextView>(R.id.fullname).text = fullName
                findViewById<TextView>(R.id.email).text = email
                findViewById<TextView>(R.id.gender).text = gender
                findViewById<TextView>(R.id.birthdate).text = birthdate
                findViewById<TextView>(R.id.disease).text = disease
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }
}