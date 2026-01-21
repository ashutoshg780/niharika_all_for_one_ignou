package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class Forget_Password_Activity : AppCompatActivity() {

    private lateinit var phoneInput: EditText
    private lateinit var otpInput: EditText
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var getOtpButton: Button
    private lateinit var verifyOtpButton: Button
    private lateinit var updatePasswordButton: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private var storedVerificationId: String? = null
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        phoneInput = findViewById(R.id.et_phone)
        otpInput = findViewById(R.id.et_otp)
        newPasswordInput = findViewById(R.id.et_new_password)
        confirmPasswordInput = findViewById(R.id.et_confirm_password)
        getOtpButton = findViewById(R.id.btn_get_otp)
        verifyOtpButton = findViewById(R.id.btn_verify_otp)
        updatePasswordButton = findViewById(R.id.btn_update_password)
        progressBar = findViewById(R.id.progressBar)

        // Initially disable OTP, password fields, and verify/update buttons
        otpInput.isEnabled = false
        verifyOtpButton.isEnabled = false
        newPasswordInput.isEnabled = false
        confirmPasswordInput.isEnabled = false
        updatePasswordButton.isEnabled = false

        getOtpButton.setOnClickListener {
            checkIfPhoneExists()
        }

        verifyOtpButton.setOnClickListener {
            verifyOtp()
        }

        updatePasswordButton.setOnClickListener {
            resetPassword()
        }
    }

    private fun checkIfPhoneExists() {
        val phoneNumber = "+91${phoneInput.text.toString().trim()}"

        progressBar.visibility = View.VISIBLE
        getOtpButton.isEnabled = false // 🚀 Disable "Get OTP" button to prevent multiple taps

        db.collection("Users")
            .document(phoneNumber)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    sendOtp()
                } else {
                    progressBar.visibility = View.GONE
                    getOtpButton.isEnabled = true // ✅ Re-enable button if phone doesn't exist
                    Toast.makeText(applicationContext, "Phone number is not linked to any account", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                getOtpButton.isEnabled = true // ✅ Re-enable button on failure
                Toast.makeText(applicationContext, "Database error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun sendOtp() {
        val phoneNumber = "+91${phoneInput.text.toString().trim()}"

        progressBar.visibility = View.VISIBLE
        getOtpButton.isEnabled = false // Disable while sending OTP

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    otpInput.setText(credential.smsCode)
                    verifyOtp()
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    progressBar.visibility = View.GONE
                    getOtpButton.isEnabled = true // Re-enable if failure
                    Toast.makeText(applicationContext, "OTP Error: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    storedVerificationId = verificationId
                    resendToken = token
                    progressBar.visibility = View.GONE

                    phoneInput.isEnabled = false
                    otpInput.isEnabled = true
                    verifyOtpButton.isEnabled = true
                    getOtpButton.text = "Resend OTP"
                    getOtpButton.isEnabled = true // Enable for Resend OTP
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    private fun verifyOtp() {
        val otpCode = otpInput.text.toString().trim()

        if (otpCode.isEmpty() || otpCode.length < 6) {
            otpInput.error = "Enter valid OTP"
            return
        }

        progressBar.visibility = View.VISIBLE
        verifyOtpButton.isEnabled = false // Disable while verifying

        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, otpCode)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    otpInput.isEnabled = false
                    verifyOtpButton.isEnabled = false
                    newPasswordInput.isEnabled = true
                    confirmPasswordInput.isEnabled = true
                    updatePasswordButton.isEnabled = true
                } else {
                    Toast.makeText(applicationContext, "Invalid OTP", Toast.LENGTH_SHORT).show()
                    verifyOtpButton.isEnabled = true // Re-enable if failed
                }
            }
    }


    private fun resetPassword() {
        val newPassword = newPasswordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()
        val phoneNumber = "+91${phoneInput.text.toString().trim()}"

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(applicationContext, "Password fields cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(applicationContext, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        updatePasswordButton.isEnabled = false // Prevent multiple taps

        db.collection("Users").document(phoneNumber)
            .update("password", newPassword)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(applicationContext, "Password Updated Successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Sign_in_Activity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                updatePasswordButton.isEnabled = true // Allow retry
                Toast.makeText(applicationContext, "Failed to update password: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
