package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.niharika_all_for_one.network.AppPreferences

class Spare_Parts_Order_History_Activity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var logoutButton: ImageView
    private lateinit var headerRole: TextView
    private lateinit var orderHistoryTable: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spare_parts_order_history)

        // === Initialize UI elements ===
        backButton = findViewById(R.id.backButton)
        logoutButton = findViewById(R.id.logoutButton)
        headerRole = findViewById(R.id.profileRole)
        orderHistoryTable = findViewById(R.id.orderHistoryTable)

        // === Check user role and name ===
        checkUser()

        // === Back button functionality ===
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // === Logout button functionality ===
        logoutButton.setOnClickListener {
            val prefs = AppPreferences(this)
            prefs.clearPreferences()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        // === Load order history from Firestore ===
        loadOrderHistory()
    }

    // === Check user authentication & load profile ===
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

        FirebaseFirestore.getInstance().collection("Users").document(phoneNumber)
            .get()
            .addOnSuccessListener {
                val role = it.getString("role") ?: ""
                val name = it.getString("fullName") ?: ""
                headerRole.text = "$role: $name"
            }
    }

    // === Load documents from Spare_Parts_Order_History collection ===
    private fun loadOrderHistory() {
        val db = FirebaseFirestore.getInstance()
        db.collection("Spare_Parts_Order_History")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val itemCode = document.getString("itemCode") ?: ""
                    val itemName = document.getString("itemName") ?: ""
                    val orderedQty = document.getString("orderedQty") ?: ""
                    val status = document.getString("status") ?: ""
                    val orderedOn = document.getString("orderedOn") ?: ""

                    val row = TableRow(this)
                    row.addView(makeTextView(itemCode))
                    row.addView(makeTextView(itemName))
                    row.addView(makeTextView(orderedQty))
                    row.addView(makeTextView(status))
                    row.addView(makeTextView(orderedOn))
                    orderHistoryTable.addView(row)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch order history", Toast.LENGTH_SHORT).show()
            }
    }

    // === Helper: Create TextView for a table cell ===
    private fun makeTextView(value: String): TextView {
        return TextView(this).apply {
            text = value
            setPadding(8, 4, 8, 4)
            textSize = 14f
        }
    }
}