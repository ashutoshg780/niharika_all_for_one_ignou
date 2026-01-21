package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class Sign_up_Activity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var spinnerRole: Spinner
    private lateinit var btnSignUp: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize UI Components
        etFullName = findViewById(R.id.et_full_name)
        etPhoneNumber = findViewById(R.id.et_email) // Using email field for phone input
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        spinnerRole = findViewById(R.id.spinner_role)
        btnSignUp = findViewById(R.id.btn_signup)
        progressBar = findViewById(R.id.progressBar)

        btnSignUp.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val fullName = etFullName.text.toString().trim()
        val phoneNumber = "+91" + etPhoneNumber.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val role = spinnerRole.selectedItem.toString()

        if (fullName.isEmpty() || phoneNumber.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // Check if phone number already exists
        db.collection("Users").document(phoneNumber)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    showLoading(false)
                    Toast.makeText(this, "Phone number already registered!", Toast.LENGTH_SHORT).show()
                } else {
                    createAccount(fullName, phoneNumber, password, role)
                }
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Error checking phone number!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createAccount(fullName: String, phoneNumber: String, password: String, role: String) {
        val userData = hashMapOf(
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "password" to password, // ⚠️ Hash the password in production!
            "role" to role
        )

        db.collection("Users").document(phoneNumber)
            .set(userData)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Signup Successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Sign_in_Activity::class.java))
                finish()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Error saving user data!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSignUp.isEnabled = !isLoading
    }
}
