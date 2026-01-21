package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.niharika_all_for_one.network.AppPreferences

class Approvals_Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Back: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_approvals)

        auth = FirebaseAuth.getInstance()
        RoleTitle = findViewById(R.id.profileRole)

        checkUser()

        LogOut = findViewById(R.id.logoutButton)
        Back = findViewById(R.id.backButton)

        LogOut.setOnClickListener {
            val prefs = AppPreferences(this)
            prefs.clearPreferences()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        Back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun checkUser() {
        val prefs = AppPreferences(this)
        val phoneNumber = prefs.getPhone()

        if (phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Phone number not available!", Toast.LENGTH_SHORT).show()
            prefs.clearPreferences()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
            return
        }

        FirebaseFirestore.getInstance().collection("Users").document(phoneNumber)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role")
                    val name = document.getString("fullName")
                    RoleTitle.text = "$role: $name"
                } else {
                    Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show()
                    prefs.clearPreferences()
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, Start_Screen_Activity::class.java))
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch user data!", Toast.LENGTH_SHORT).show()
                Log.e("ApprovalsError", "Firestore error: ${it.message}")
                prefs.clearPreferences()
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, Start_Screen_Activity::class.java))
                finish()
            }
    }
}