package com.example.yourwellnessjourney

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Dashboard : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var userRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        val currentUser: FirebaseUser? = auth.currentUser

        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set Navigation Drawer Item Selected Listener
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    val profileIntent = Intent(this, Profile::class.java)
                    startActivity(profileIntent)
                }
                R.id.nav_logout -> {
                    auth.signOut() // Sign out the user from Firebase Auth
                    val intent = Intent(this, MainActivity::class.java) // MainActivity should be your entry activity
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Set Bottom Navigation Item Selected Listener
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(Home())
                    true
                }
                R.id.navigation_exercise -> {
                    replaceFragment(Exercise())
                    true
                }
                R.id.navigation_diet -> {
                    replaceFragment(Diet())
                    true
                }
                R.id.navigation_medication -> {
                    replaceFragment(Medication())
                    true
                }
                else -> false
            }
        }

        // Set default fragment
        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.navigation_home
        }

        // Fetch and display the user's profile information in the navigation header
        if (currentUser != null) {
            setupNavigationHeader(navigationView, currentUser)
        }
    }

    private fun setupNavigationHeader(navigationView: NavigationView, currentUser: FirebaseUser) {
        // Get header view and find TextViews
        val headerView: View = navigationView.getHeaderView(0)
        profileName = headerView.findViewById(R.id.profile_name)
        profileEmail = headerView.findViewById(R.id.profile_email)

        // Set the user's email
        profileEmail.text = currentUser.email

        // Reference to the user's data in Firebase Realtime Database
        val userId = currentUser.uid
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        // Fetch user data
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {

                    val firstName = dataSnapshot.child("firstName").getValue(String::class.java)?.capitalize() ?: ""
                    val middleName = dataSnapshot.child("middleName").getValue(String::class.java)
                    val lastName = dataSnapshot.child("lastName").getValue(String::class.java)?.capitalize() ?: ""
                    val middleInitial = middleName?.firstOrNull()?.uppercase()?.plus(".") ?: ""


                    val fullName = "$firstName $middleInitial $lastName"
                    profileName.text = fullName
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors
                Log.e("FirebaseError", "Failed to read user data", databaseError.toException())
            }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
