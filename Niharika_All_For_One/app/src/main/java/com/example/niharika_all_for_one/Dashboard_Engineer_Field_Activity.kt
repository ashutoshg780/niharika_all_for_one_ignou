package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Dashboard_Engineer_Field_Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Profile: ImageView

    private lateinit var cardJobsPending: CardView
    private lateinit var cardJobsCompleted: CardView
    private lateinit var btnJobsList: Button
    private lateinit var btnNewJob: Button
    private lateinit var btnSpairPartsBag: Button
    private lateinit var btnSpairParts: Button
    private lateinit var btnCompletedJob: Button
    private lateinit var btnStartEndDay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard_engineer_field)

        auth = FirebaseAuth.getInstance()
        RoleTitle = findViewById(R.id.profileRole)

        checkUser()

        // Initialize buttons
        LogOut = findViewById(R.id.logoutButton)
        Profile = findViewById(R.id.profileBtn)

        cardJobsPending = findViewById(R.id.cardJobsPending)
        cardJobsCompleted = findViewById(R.id.cardJobsCompleted)
        btnJobsList = findViewById(R.id.btnJobsList)
        btnNewJob = findViewById(R.id.btnNewJob)
        btnSpairPartsBag = findViewById(R.id.btnSpairPartsBag)
        btnSpairParts = findViewById(R.id.btnSpairParts)
        btnCompletedJob = findViewById(R.id.btnCompleteJob)
        btnStartEndDay = findViewById(R.id.btnStartEndDay)

        //Logouts the page and opens start page
        LogOut.setOnClickListener {
            // Clear Firebase session
            FirebaseAuth.getInstance().signOut()

            // Clear SharedPreferences (stored phone number)
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()

            // Navigate to Start Screen
            val intent = Intent(this, Start_Screen_Activity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // opens the Profile page
        Profile.setOnClickListener {
            startActivity(Intent(this,Profile_Activity::class.java))
        }


        cardJobsPending.setOnClickListener {
            startActivity(Intent(this, Jobs_List_Activity::class.java))
        }

        cardJobsCompleted.setOnClickListener {
            startActivity(Intent(this, Jobs_List_Activity::class.java))
        }

        btnJobsList.setOnClickListener{
            startActivity(Intent(this, Jobs_List_Activity::class.java))
        }

        btnNewJob.setOnClickListener{
            startActivity(Intent(this, New_Job_Activity::class.java))
        }

        btnSpairPartsBag.setOnClickListener {
            startActivity(Intent(this, Spare_Parts_Job_Activity::class.java))
        }

        btnSpairParts.setOnClickListener {
            startActivity(Intent(this, Spare_Parts_Stock_Activity::class.java))
        }

        btnCompletedJob.setOnClickListener {
            startActivity(Intent(this, Jobs_List_Activity::class.java))
        }

        btnStartEndDay.setOnClickListener {
            Toast.makeText(this, "Start/End Day Clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUser() {
        // Get phone number from SharedPreferences
        val phoneNumber = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("phone", null)

        if (phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Phone number not available!", Toast.LENGTH_SHORT).show()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("Users").document(phoneNumber)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role")
                    val name = document.getString("fullName")
                    RoleTitle.text = "$role: $name"

                    if (role == "Engineer (Field)") {
                        RoleTitle.text = "$role: $name"
                        Log.d("DashboardDebug", "Fetched role: $role and name: $name")
                    } else {
                        Toast.makeText(this, "Access Denied: Role = $role", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                        startActivity(Intent(this, Start_Screen_Activity::class.java))
                        finish()
                    }

                } else {
                    Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show()
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, Start_Screen_Activity::class.java))
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch user data!", Toast.LENGTH_SHORT).show()
                Log.e("DashboardError", "Firestore error: ${it.message}")
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, Start_Screen_Activity::class.java))
                finish()
            }
    }
}
