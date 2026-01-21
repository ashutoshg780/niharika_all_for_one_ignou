package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.niharika_all_for_one.network.AppPreferences


class Splash_Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()
        analytics = FirebaseAnalytics.getInstance(this)

        Log.d("SplashDebug", "Splash screen started. Firebase currentUser: ${auth.currentUser?.uid}")

        Handler().postDelayed({
            Log.d("SplashDebug", "Starting checkUser() after splash delay.")
            checkUser()
        }, 2500) // 2.5 second wait
    }

    private fun checkUser() {
        // 🔐 Log Firebase Auth status
        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.d("SplashDebug", "Auth currentUser: $currentUser")

        if (currentUser == null) {
            Log.w("SplashDebug", "Session expired: FirebaseAuth.getInstance().currentUser is null.")
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
            return
        } else {
            Log.d("SplashDebug", "User is logged in: UID = ${currentUser.uid}, Phone = ${currentUser.phoneNumber}")
        }

//        val sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
//        val phoneNumber = sharedPrefs.getString("phone", null)
        val prefs = AppPreferences(this)
        if (prefs.getIsLogin() && prefs.getStatus() == "Active") {
            // Already logged in → go straight to Dashboard
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        } else {
            // Not logged in / pending / no session
            startActivity(Intent(this, Sign_in_Activity::class.java))
            finish()
        }


//        Log.d("SplashDebug", "Loaded phone from SharedPreferences: $phoneNumber")

//        if (phoneNumber.isNullOrEmpty()) {
//            Log.w("SplashDebug", "No phone number found in SharedPreferences. Redirecting to login.")
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//            return
//        }

        val db = FirebaseFirestore.getInstance()
//        val userRef = db.collection("Users").document(phoneNumber)

//        Log.d("SplashDebug", "Attempting to fetch user document for phone: $phoneNumber")
//        userRef.get()
//            .addOnSuccessListener { document ->
//                Log.d("SplashDebug", "User document fetch success: exists=${document.exists()}")
//                if (document.exists()) {
//                    val role = document.getString("role")
//                    Log.d("SplashDebug", "User role from Firestore: $role")
//                    when (role) {
//                        "Manager" -> {
//                            Log.d("SplashDebug", "Opening Dashboard_Manager_Activity")
//                            startActivity(Intent(this, Dashboard_Manager_Activity::class.java))
//                        }
//                        "Engineer (Workshop)" -> {
//                            Log.d("SplashDebug", "Opening Dashboard_Engineer_Workshop_Activity")
//                            startActivity(Intent(this, Dashboard_Engineer_Workshop_Activity::class.java))
//                        }
//                        "Engineer (Field)" -> {
//                            Log.d("SplashDebug", "Opening Dashboard_Engineer_Field_Activity")
//                            startActivity(Intent(this, Dashboard_Engineer_Field_Activity::class.java))
//                        }
//                        "Admin" -> {
//                            Log.d("SplashDebug", "Opening Dashboard_Admin_Activity")
//                            startActivity(Intent(this, Dashboard_Admin_Activity::class.java))
//                        }
//                        "Super Admin", "Owner" -> {
//                            Log.d("SplashDebug", "Opening Dashboard_SuperAdmin_or_Owner_Activity")
//                            startActivity(Intent(this, Dashboard_SuperAdmin_or_Owner_Activity::class.java))
//                        }
//                        else -> {
//                            Log.w("SplashDebug", "Unknown role encountered: $role. Logging out user.")
//                            Toast.makeText(this, "Unknown Role", Toast.LENGTH_SHORT).show()
//                            FirebaseAuth.getInstance().signOut()
//                            startActivity(Intent(this, Start_Screen_Activity::class.java))
//                        }
//                    }
//                    finish()
//                } else {
//                    Log.e("SplashDebug", "User data not found in Firestore for phone: $phoneNumber")
//                    Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show()
//                    FirebaseAuth.getInstance().signOut()
//                    startActivity(Intent(this, Start_Screen_Activity::class.java))
//                    finish()
//                }
//            }
//            .addOnFailureListener { exception ->
//                Log.e("SplashDebug", "Failed to fetch user data: ${exception.message}")
//                Toast.makeText(this, "Failed to fetch user data!", Toast.LENGTH_SHORT).show()
//                FirebaseAuth.getInstance().signOut()
//                startActivity(Intent(this, Start_Screen_Activity::class.java))
//                finish()
//            }
    }
}
