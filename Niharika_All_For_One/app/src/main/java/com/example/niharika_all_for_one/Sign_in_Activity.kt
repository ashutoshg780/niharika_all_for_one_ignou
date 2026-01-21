package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.niharika_all_for_one.network.AppPreferences


class Sign_in_Activity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var btnForgotPassword: TextView
    private lateinit var btnSignUp: TextView
    private lateinit var phoneEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        db = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        phoneEditText = findViewById(R.id.et_full_name) // Employee ID field (Phone Number)
        passwordEditText = findViewById(R.id.et_password)
        loginButton = findViewById(R.id.btn_login)
        btnForgotPassword = findViewById(R.id.tv_forgot_password)
        btnSignUp = findViewById(R.id.signuppg)
        progressBar = findViewById(R.id.progressBar)

        loginButton.setOnClickListener {
            loginUser()
        }

        btnForgotPassword.setOnClickListener {
            startActivity(Intent(this, Forget_Password_Activity::class.java))
        }

        btnSignUp.setOnClickListener {
            startActivity(Intent(this, Sign_up_Activity::class.java))
        }
    }

//    private fun loginUser() {
//        val phoneNumber = "+91" + phoneEditText.text.toString().trim()
//        val enteredPassword = passwordEditText.text.toString().trim()
//
//        if (phoneNumber.isEmpty() || enteredPassword.isEmpty()) {
//            Toast.makeText(this, "Please enter phone number and password", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        progressBar.visibility = View.VISIBLE
//
//        db.collection("Users").document(phoneNumber)
//            .get()
//            .addOnSuccessListener { document ->
//                progressBar.visibility = View.GONE
//                if (document.exists()) {
//                    val storedPassword = document.getString("password")
//                    if (storedPassword == enteredPassword) {
//                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
//
//                        // ✅ Save login session into AppPreferences
//                        val prefs = AppPreferences(this)
//                        prefs.setIsLogin(true)                // user is logged in
//                        prefs.setPhone(phoneNumber)           // phone used as userId in Firestore
//                        prefs.setName(document.getString("fullName"))
//                        prefs.setUserRole(document.getString("role"))
//                        prefs.setEmail(document.getString("email"))
//                        prefs.setStatus(document.getString("status")) // "Active" / "Pending"
//
//                        // ✅ Store phone number in SharedPreferences
////                        getSharedPreferences("UserPrefs", MODE_PRIVATE)
////                            .edit()
////                            .putString("phone", phoneNumber)
////                            .apply()
//
//                        Handler(Looper.getMainLooper()).postDelayed({
//                            checkRole()
//                        }, 300)
//                    } else {
//                        Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show()
//                    }
//                } else {
//                    Toast.makeText(this, "Account does not exist!", Toast.LENGTH_SHORT).show()
//                }
//            }
//            .addOnFailureListener { e ->
//                progressBar.visibility = View.GONE
//                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//                Log.e("LoginError", "Error logging in: ${e.message}")
//            }
//    }

    private fun loginUser() {
        val phoneNumber = "+91" + phoneEditText.text.toString().trim()
        val enteredPassword = passwordEditText.text.toString().trim()

        if (phoneNumber.isEmpty() || enteredPassword.isEmpty()) {
            Toast.makeText(this, "Please enter phone number and password", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        db.collection("Users").document(phoneNumber)
            .get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                if (document.exists()) {
                    val storedPassword = document.getString("password")
                    if (storedPassword == enteredPassword) {
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                        // ✅ Save login session into AppPreferences
                        val prefs = AppPreferences(this)
                        val userName = document.getString("fullName")
                        val userRole = document.getString("role")
                        val userEmail = document.getString("email")
                        val userStatus = document.getString("status")

                        // ✅ Debug logs - yeh dekho kya save ho raha hai
                        Log.d("LoginDebug", "Saving data:")
                        Log.d("LoginDebug", "Phone: $phoneNumber")
                        Log.d("LoginDebug", "Name: $userName")
                        Log.d("LoginDebug", "Role: $userRole")
                        Log.d("LoginDebug", "Email: $userEmail")
                        Log.d("LoginDebug", "Status: $userStatus")

                        prefs.setIsLogin(true)
                        prefs.setPhone(phoneNumber)
                        prefs.setName(userName)
                        prefs.setUserRole(userRole)
                        prefs.setEmail(userEmail)
                        prefs.setStatus(userStatus)

                        // ✅ Verify karo save hua ya nahi
                        Log.d("LoginDebug", "Verification:")
                        Log.d("LoginDebug", "Saved Phone: ${prefs.getPhone()}")
                        Log.d("LoginDebug", "Saved Role: ${prefs.getUserRole()}")
                        Log.d("LoginDebug", "Saved Name: ${prefs.getName()}")

                        Handler(Looper.getMainLooper()).postDelayed({
                            checkRole()
                        }, 300)
                    } else {
                        Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Account does not exist!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("LoginError", "Error logging in: ${e.message}")
            }
    }

//    private fun checkRole() {
//        progressBar.visibility = View.VISIBLE
//
//        // ✅ Retrieve phone number from SharedPreferences
//        val phoneNumber = getSharedPreferences("UserPrefs", MODE_PRIVATE)
//            .getString("phone", null)
//
//        if (phoneNumber.isNullOrEmpty()) {
//            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
//            progressBar.visibility = View.GONE
//            return
//        }
//
//        db.collection("Users").document(phoneNumber)
//            .get()
//            .addOnSuccessListener { document ->
//                progressBar.visibility = View.GONE
//                if (document.exists()) {
//                    val role = document.getString("role")
//                    Log.d("LoginDebug", "User role: $role for $phoneNumber")
//
////                    when (role) {
////                        "Manager" -> {
////                            startActivity(Intent(this, Dashboard_Manager_Activity::class.java))
////                            finish()
////                        }
////                        "Engineer (Workshop)" -> {
////                            startActivity(Intent(this, Dashboard_Engineer_Workshop_Activity::class.java))
////                            finish()
////                        }
////                        "Engineer (Field)" -> {
////                            startActivity(Intent(this, Dashboard_Engineer_Field_Activity::class.java))
////                            finish()
////                        }
////                        "Admin" -> {
////                            startActivity(Intent(this, Dashboard_Admin_Activity::class.java))
////                            finish()
////                        }
////                        "Super Admin", "Owner" -> {
////                            startActivity(Intent(this, Dashboard_SuperAdmin_or_Owner_Activity::class.java))
////                            finish()
////                        }
////                        else -> {
////                            Toast.makeText(this, "Unknown Role", Toast.LENGTH_SHORT).show()
////                        }
////                    }
//                    startActivity(Intent(this, Dashboard::class.java))
//                    finish()
//
//                } else {
//                    Toast.makeText(this, "User document not found!", Toast.LENGTH_SHORT).show()
//                }
//            }
//            .addOnFailureListener { e ->
//                progressBar.visibility = View.GONE
//                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//                Log.e("RoleCheckError", "Error fetching role: ${e.message}")
//            }
//    }

    private fun checkRole() {
        progressBar.visibility = View.VISIBLE

        // ✅ Retrieve phone number from AppPreferences
        val prefs = AppPreferences(this)
        val phoneNumber = prefs.getPhone()

        if (phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            return
        }

        db.collection("Users").document(phoneNumber)
            .get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                if (document.exists()) {
                    val role = document.getString("role")
                    Log.d("LoginDebug", "User role: $role for $phoneNumber")

                    startActivity(Intent(this, Dashboard::class.java))
                    finish()

                } else {
                    Toast.makeText(this, "User document not found!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("RoleCheckError", "Error fetching role: ${e.message}")
            }
    }
}
