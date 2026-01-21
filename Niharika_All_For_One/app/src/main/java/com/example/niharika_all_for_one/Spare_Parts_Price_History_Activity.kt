package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.niharika_all_for_one.network.AppPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Spare_Parts_Price_History_Activity : AppCompatActivity() {

    // Firebase references
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    // Header and UI elements
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Back: ImageView
    private lateinit var priceHistoryTable: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_spare_parts_price_history)

        // === Header UI ===
        auth = FirebaseAuth.getInstance()
        RoleTitle = findViewById(R.id.profileRole)
        LogOut = findViewById(R.id.logoutButton)
        Back = findViewById(R.id.backButton)
        priceHistoryTable = findViewById(R.id.priceHistoryTable)

        // === Set up header and user info ===
        checkUser()

        // === Load the full price history table ===
        loadPriceHistory()

        // === Logout logic ===
//        LogOut.setOnClickListener {
//            FirebaseAuth.getInstance().signOut()
//            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//        }

        LogOut.setOnClickListener {
            val prefs = AppPreferences(this)
            prefs.clearPreferences()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
        }


        // === Back button logic ===
        Back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // === Load user's role and name in the header ===
//    private fun checkUser() {
//        val phoneNumber = getSharedPreferences("UserPrefs", MODE_PRIVATE)
//            .getString("phone", null)
//        if (phoneNumber.isNullOrEmpty()) {
//            Toast.makeText(this, "Phone number not found!", Toast.LENGTH_SHORT).show()
//            FirebaseAuth.getInstance().signOut()
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//            return
//        }
//
//        db.collection("Users").document(phoneNumber)
//            .get()
//            .addOnSuccessListener {
//                val role = it.getString("role") ?: ""
//                val name = it.getString("fullName") ?: ""
//                RoleTitle.text = "$role: $name"
//            }
//    }

    private fun checkUser() {
        val prefs = AppPreferences(this)
        val phoneNumber = prefs.getPhone()

        if (phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Phone number not found!", Toast.LENGTH_SHORT).show()
            prefs.clearPreferences()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
            return
        }

        db.collection("Users").document(phoneNumber)
            .get()
            .addOnSuccessListener {
                val role = it.getString("role") ?: ""
                val name = it.getString("fullName") ?: ""
                RoleTitle.text = "$role: $name"
            }
    }

    // === Fetch and show price update history in the table ===
    private fun loadPriceHistory() {
        // Remove previous rows except header (assume first row is header)
        val childCount = priceHistoryTable.childCount
        if (childCount > 1) {
            priceHistoryTable.removeViews(1, childCount - 1)
        }

        db.collection("Spare_Parts_Price_History")
            .orderBy("updatedOn", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                for (document in result.documents) {
                    val itemCode = document.id.substringBefore("_SPH_")
                    val itemName = document.getString("itemName") ?: ""
                    val price = document.getString("price") ?: ""
                    val updatedBy = document.getString("updatedBy") ?: ""
                    val updatedOn = document.getString("updatedOn") ?: ""

                    addHistoryRow(itemCode, itemName, price, updatedBy, updatedOn)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load price history!", Toast.LENGTH_SHORT).show()
                Log.e("SPHIST", "Error loading price history: ${it.message}")
            }
    }

    // === Helper: Add a single row to the table ===
    private fun addHistoryRow(code: String, name: String, price: String, updatedBy: String, updatedOn: String) {
        val row = TableRow(this)

        val codeView = TextView(this).apply {
            text = code
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            setTextColor(resources.getColor(android.R.color.white))
        }

        val nameView = TextView(this).apply {
            text = name
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            setTextColor(resources.getColor(android.R.color.white))
        }

        val priceView = TextView(this).apply {
            text = price
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            setTextColor(resources.getColor(android.R.color.white))
        }

        val byView = TextView(this).apply {
            text = updatedBy
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            setTextColor(resources.getColor(android.R.color.white))
        }

        val onView = TextView(this).apply {
            text = updatedOn
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            setTextColor(resources.getColor(android.R.color.white))
        }

        row.addView(codeView)
        row.addView(nameView)
        row.addView(priceView)
        row.addView(byView)
        row.addView(onView)
        priceHistoryTable.addView(row)
    }
}
